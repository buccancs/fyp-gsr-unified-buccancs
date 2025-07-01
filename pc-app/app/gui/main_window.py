import sys
import logging
import json
import time
from PySide6.QtWidgets import QApplication, QMainWindow, QVBoxLayout, QHBoxLayout, QWidget, QPushButton, QLabel, QTextEdit
from PySide6.QtGui import QImage, QPixmap
from PySide6.QtCore import Qt, QTimer

from app.network.device_manager import DeviceManager
from app.sensors.gsr_handler import GSRHandler
from app.sensors.webcam_handler import WebcamHandler
from app.session.session_manager import SessionManager

logger = logging.getLogger(__name__)

class MainWindow(QMainWindow):
    def __init__(self, device_manager: DeviceManager, gsr_handler: GSRHandler, webcam_handler: WebcamHandler, session_manager: SessionManager, file_transfer_manager=None):
        super().__init__()
        self.device_manager = device_manager
        self.gsr_handler = gsr_handler
        self.webcam_handler = webcam_handler
        self.session_manager = session_manager
        self.file_transfer_manager = file_transfer_manager

        self.setWindowTitle("GSR Multimodal PC Controller")
        self.setGeometry(100, 100, 1200, 800)

        self.central_widget = QWidget()
        self.setCentralWidget(self.central_widget)
        self.main_layout = QHBoxLayout(self.central_widget)

        self._init_ui()
        self._init_timers()

    def _init_ui(self):
        # Left panel for PC webcam and controls
        left_panel = QVBoxLayout()
        self.webcam_label = QLabel("PC Webcam Feed")
        self.webcam_label.setAlignment(Qt.AlignCenter)
        self.webcam_label.setFixedSize(640, 480) # Standard webcam resolution
        left_panel.addWidget(self.webcam_label)

        self.start_webcam_button = QPushButton("Start PC Webcam")
        self.start_webcam_button.clicked.connect(self._start_pc_webcam)
        left_panel.addWidget(self.start_webcam_button)

        self.stop_webcam_button = QPushButton("Stop PC Webcam")
        self.stop_webcam_button.clicked.connect(self._stop_pc_webcam)
        left_panel.addWidget(self.stop_webcam_button)

        self.main_layout.addLayout(left_panel)

        # Right panel for Android device controls and status
        right_panel = QVBoxLayout()
        self.status_text = QTextEdit()
        self.status_text.setReadOnly(True)
        self.status_text.setPlaceholderText("System status and logs...")
        right_panel.addWidget(self.status_text)

        self.start_all_button = QPushButton("Start All Recording")
        self.start_all_button.clicked.connect(self._start_all_recording)
        right_panel.addWidget(self.start_all_button)

        self.stop_all_button = QPushButton("Stop All Recording")
        self.stop_all_button.clicked.connect(self._stop_all_recording)
        right_panel.addWidget(self.stop_all_button)

        self.main_layout.addLayout(right_panel)

    def _init_timers(self):
        self.webcam_timer = QTimer(self)
        self.webcam_timer.timeout.connect(self._update_webcam_feed)
        self.webcam_timer.start(33) # ~30 FPS

    def _start_pc_webcam(self):
        if self.webcam_handler.start_capture():
            self.status_text.append("PC Webcam started.")
        else:
            self.status_text.append("Failed to start PC Webcam.")

    def _stop_pc_webcam(self):
        self.webcam_handler.stop_capture()
        self.status_text.append("PC Webcam stopped.")

    def _update_webcam_feed(self):
        frame = self.webcam_handler.get_latest_frame()
        if frame is not None:
            h, w, ch = frame.shape
            bytes_per_line = ch * w
            qt_image = QImage(frame.data, w, h, bytes_per_line, QImage.Format_RGB888).rgbSwapped()
            self.webcam_label.setPixmap(QPixmap.fromImage(qt_image))

    def _start_all_recording(self):
        """Start recording on all connected devices with proper session management."""
        try:
            # Start a new session
            session_id = self.session_manager.start_session()
            self.status_text.append(f"Started new session: {session_id}")

            # Get connected devices
            connected_devices = self.device_manager.get_connected_devices()
            if not connected_devices:
                self.status_text.append("No devices connected. Cannot start recording.")
                return

            # Start PC webcam recording if available
            if self.webcam_handler:
                webcam_filename = f"pc_webcam_{session_id}.mp4"
                session_info = self.session_manager.get_session_info()
                webcam_path = f"{session_info['session_dir']}/{webcam_filename}"
                if self.webcam_handler.start_recording(webcam_path):
                    self.status_text.append("PC webcam recording started")
                else:
                    self.status_text.append("Failed to start PC webcam recording")

            # Start GSR capture on PC if available
            self.gsr_handler.start_gsr_capture()
            self.status_text.append("PC GSR capture started")

            # Send start recording command to all Android devices
            command_data = {
                "session_id": session_id,
                "timestamp": self.session_manager.session_start_time,
                "recording_params": {
                    "video_quality": "HD",
                    "gsr_sampling_rate": 128,
                    "thermal_fps": 25
                }
            }

            devices_reached = self.device_manager.send_command_to_all("start_recording", command_data)
            self.status_text.append(f"Start recording command sent to {devices_reached} devices")

            if devices_reached > 0:
                self.status_text.append("Recording started successfully on all devices")
            else:
                self.status_text.append("Warning: No devices received the start command")

        except Exception as e:
            logger.error(f"Error starting recording: {e}")
            self.status_text.append(f"Error starting recording: {str(e)}")

    def _stop_all_recording(self):
        """Stop recording on all connected devices and finalize session."""
        try:
            # Send stop recording command to all Android devices
            session_info = self.session_manager.get_session_info()
            command_data = {
                "session_id": session_info.get("session_id"),
                "stop_timestamp": time.time()
            }

            devices_reached = self.device_manager.send_command_to_all("stop_recording", command_data)
            self.status_text.append(f"Stop recording command sent to {devices_reached} devices")

            # Stop PC webcam recording
            if self.webcam_handler:
                self.webcam_handler.stop_recording()
                self.status_text.append("PC webcam recording stopped")

            # Stop PC GSR capture
            self.gsr_handler.stop_gsr_capture()
            self.status_text.append("PC GSR capture stopped")

            # Initiate file transfer from devices if file transfer manager is available
            if self.file_transfer_manager and devices_reached > 0:
                self.status_text.append("Initiating file transfer from devices...")
                connected_devices = self.device_manager.get_connected_devices()
                for device_id in connected_devices:
                    try:
                        device_dir = self.session_manager.get_device_directory(device_id)
                        if device_dir:
                            self.file_transfer_manager.start_file_transfer(
                                self.device_manager, device_id, str(device_dir)
                            )
                            self.status_text.append(f"File transfer started for {device_id}")
                    except Exception as e:
                        logger.error(f"Error starting file transfer for {device_id}: {e}")
                        self.status_text.append(f"File transfer error for {device_id}: {str(e)}")

            # Stop the session
            self.session_manager.stop_session()
            self.status_text.append("Session stopped and data saved")

        except Exception as e:
            logger.error(f"Error stopping recording: {e}")
            self.status_text.append(f"Error stopping recording: {str(e)}")

    def run(self):
        app = QApplication(sys.argv)
        self.show()
        sys.exit(app.exec())
