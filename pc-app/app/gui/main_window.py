import sys
import logging
import json
import time
from PySide6.QtWidgets import (QApplication, QMainWindow, QVBoxLayout, QHBoxLayout, QWidget, 
                               QPushButton, QLabel, QTextEdit, QTabWidget, QSplitter, 
                               QGroupBox, QGridLayout, QListWidget, QFrame)
from PySide6.QtGui import QImage, QPixmap, QFont
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

        # Device panels for connected Android devices
        self.device_panels = {}
        self.device_preview_timers = {}

        self.setWindowTitle("GSR Multimodal PC Controller")
        self.setGeometry(100, 100, 1400, 900)

        self.central_widget = QWidget()
        self.setCentralWidget(self.central_widget)
        self.main_layout = QHBoxLayout(self.central_widget)

        self._init_ui()
        self._init_timers()
        self._setup_device_callbacks()

    def _init_ui(self):
        """Initialize the UI with a horizontal splitter layout as described in the issue."""
        # Create main horizontal splitter
        main_splitter = QSplitter(Qt.Horizontal)
        self.main_layout.addWidget(main_splitter)

        # Left side: PC webcam and controls
        left_widget = QWidget()
        left_layout = QVBoxLayout(left_widget)

        # PC Webcam section
        pc_group = QGroupBox("PC Webcam")
        pc_layout = QVBoxLayout(pc_group)

        self.webcam_label = QLabel("PC Webcam Feed")
        self.webcam_label.setAlignment(Qt.AlignCenter)
        self.webcam_label.setFixedSize(640, 480)
        self.webcam_label.setStyleSheet("border: 1px solid gray; background-color: black;")
        pc_layout.addWidget(self.webcam_label)

        # PC webcam controls
        webcam_controls = QHBoxLayout()
        self.start_webcam_button = QPushButton("Start PC Webcam")
        self.start_webcam_button.clicked.connect(self._start_pc_webcam)
        self.stop_webcam_button = QPushButton("Stop PC Webcam")
        self.stop_webcam_button.clicked.connect(self._stop_pc_webcam)
        webcam_controls.addWidget(self.start_webcam_button)
        webcam_controls.addWidget(self.stop_webcam_button)
        pc_layout.addLayout(webcam_controls)

        left_layout.addWidget(pc_group)

        # Global controls section
        controls_group = QGroupBox("Global Controls")
        controls_layout = QVBoxLayout(controls_group)

        self.start_all_button = QPushButton("Start All Recording")
        self.start_all_button.clicked.connect(self._start_all_recording)
        self.start_all_button.setStyleSheet("QPushButton { background-color: #4CAF50; color: white; font-weight: bold; }")

        self.stop_all_button = QPushButton("Stop All Recording")
        self.stop_all_button.clicked.connect(self._stop_all_recording)
        self.stop_all_button.setStyleSheet("QPushButton { background-color: #f44336; color: white; font-weight: bold; }")

        controls_layout.addWidget(self.start_all_button)
        controls_layout.addWidget(self.stop_all_button)
        left_layout.addWidget(controls_group)

        # Status section
        status_group = QGroupBox("System Status")
        status_layout = QVBoxLayout(status_group)
        self.status_text = QTextEdit()
        self.status_text.setReadOnly(True)
        self.status_text.setPlaceholderText("System status and logs...")
        self.status_text.setMaximumHeight(150)
        status_layout.addWidget(self.status_text)
        left_layout.addWidget(status_group)

        main_splitter.addWidget(left_widget)

        # Right side: Device tabs for Android devices
        right_widget = QWidget()
        right_layout = QVBoxLayout(right_widget)

        # Device discovery section
        discovery_group = QGroupBox("Device Discovery")
        discovery_layout = QHBoxLayout(discovery_group)

        self.device_list = QListWidget()
        self.device_list.setMaximumHeight(100)
        discovery_layout.addWidget(self.device_list)

        discovery_controls = QVBoxLayout()
        self.refresh_devices_button = QPushButton("Refresh Devices")
        self.refresh_devices_button.clicked.connect(self._refresh_device_list)
        discovery_controls.addWidget(self.refresh_devices_button)
        discovery_layout.addLayout(discovery_controls)

        right_layout.addWidget(discovery_group)

        # Device tabs for connected Android devices
        self.device_tabs = QTabWidget()
        self.device_tabs.setTabsClosable(False)
        right_layout.addWidget(self.device_tabs)

        main_splitter.addWidget(right_widget)

        # Set splitter proportions (60% left, 40% right)
        main_splitter.setSizes([800, 600])

    def _init_timers(self):
        self.webcam_timer = QTimer(self)
        self.webcam_timer.timeout.connect(self._update_webcam_feed)
        self.webcam_timer.start(33) # ~30 FPS

    def _setup_device_callbacks(self):
        """Setup callbacks for device manager to handle device connections."""
        self.device_manager.add_message_callback(self._handle_device_message)

    def _refresh_device_list(self):
        """Refresh the list of connected devices."""
        self.device_list.clear()
        connected_devices = self.device_manager.get_connected_devices()
        for device_id in connected_devices:
            device_info = self.device_manager.get_device_info(device_id)
            if device_info:
                display_name = device_info.get('info', {}).get('device_name', device_id)
                self.device_list.addItem(f"{device_id} ({display_name})")

        self.status_text.append(f"Found {len(connected_devices)} connected devices")

    def _handle_device_message(self, device_id: str, message: dict):
        """Handle incoming messages from Android devices."""
        message_type = message.get('type', 'unknown')

        if message_type == 'device_connected':
            self._add_device_panel(device_id, message.get('data', {}))
        elif message_type == 'device_disconnected':
            self._remove_device_panel(device_id)
        elif message_type == 'gsr_data':
            self._update_device_gsr(device_id, message.get('data', {}))
        elif message_type == 'frame_data':
            self._update_device_preview(device_id, message.get('data', {}))
        elif message_type == 'status_update':
            self._update_device_status(device_id, message.get('data', {}))

    def _add_device_panel(self, device_id: str, device_info: dict):
        """Add a new device panel tab for a connected Android device."""
        if device_id in self.device_panels:
            return  # Panel already exists

        # Create device panel widget
        device_widget = QWidget()
        device_layout = QVBoxLayout(device_widget)

        # Device info section
        info_group = QGroupBox(f"Device Info - {device_id}")
        info_layout = QGridLayout(info_group)

        device_name = device_info.get('device_name', 'Unknown Device')
        info_layout.addWidget(QLabel("Name:"), 0, 0)
        info_layout.addWidget(QLabel(device_name), 0, 1)

        # Connection status
        status_label = QLabel("Connected")
        status_label.setStyleSheet("color: green; font-weight: bold;")
        info_layout.addWidget(QLabel("Status:"), 1, 0)
        info_layout.addWidget(status_label, 1, 1)

        device_layout.addWidget(info_group)

        # Video preview section
        video_group = QGroupBox("Camera Preview")
        video_layout = QVBoxLayout(video_group)

        camera_label = QLabel("Camera Feed")
        camera_label.setAlignment(Qt.AlignCenter)
        camera_label.setFixedSize(320, 240)  # Smaller preview size
        camera_label.setStyleSheet("border: 1px solid gray; background-color: black;")
        video_layout.addWidget(camera_label)

        device_layout.addWidget(video_group)

        # Thermal preview section (if available)
        thermal_group = QGroupBox("Thermal Preview")
        thermal_layout = QVBoxLayout(thermal_group)

        thermal_label = QLabel("Thermal Feed")
        thermal_label.setAlignment(Qt.AlignCenter)
        thermal_label.setFixedSize(256, 192)  # Thermal camera resolution
        thermal_label.setStyleSheet("border: 1px solid gray; background-color: black;")
        thermal_layout.addWidget(thermal_label)

        device_layout.addWidget(thermal_group)

        # GSR data section
        gsr_group = QGroupBox("GSR Data")
        gsr_layout = QVBoxLayout(gsr_group)

        gsr_value_label = QLabel("GSR: -- μS")
        gsr_value_label.setStyleSheet("font-size: 14px; font-weight: bold;")
        gsr_layout.addWidget(gsr_value_label)

        device_layout.addWidget(gsr_group)

        # Individual device controls
        controls_group = QGroupBox("Device Controls")
        controls_layout = QHBoxLayout(controls_group)

        start_device_button = QPushButton(f"Start {device_id}")
        start_device_button.clicked.connect(lambda: self._start_device_recording(device_id))

        stop_device_button = QPushButton(f"Stop {device_id}")
        stop_device_button.clicked.connect(lambda: self._stop_device_recording(device_id))

        controls_layout.addWidget(start_device_button)
        controls_layout.addWidget(stop_device_button)

        device_layout.addWidget(controls_group)

        # Store references to UI elements for updates
        panel_data = {
            'widget': device_widget,
            'status_label': status_label,
            'camera_label': camera_label,
            'thermal_label': thermal_label,
            'gsr_label': gsr_value_label,
            'start_button': start_device_button,
            'stop_button': stop_device_button
        }

        self.device_panels[device_id] = panel_data

        # Add tab to device tabs
        display_name = device_info.get('device_name', device_id)
        self.device_tabs.addTab(device_widget, display_name)

        # Setup preview timer for this device
        preview_timer = QTimer(self)
        preview_timer.timeout.connect(lambda: self._request_device_preview(device_id))
        preview_timer.start(1000)  # Request preview every second
        self.device_preview_timers[device_id] = preview_timer

        self.status_text.append(f"Added device panel for {device_id}")
        self._refresh_device_list()

    def _remove_device_panel(self, device_id: str):
        """Remove device panel when device disconnects."""
        if device_id not in self.device_panels:
            return

        # Stop preview timer
        if device_id in self.device_preview_timers:
            self.device_preview_timers[device_id].stop()
            del self.device_preview_timers[device_id]

        # Find and remove tab
        panel_widget = self.device_panels[device_id]['widget']
        for i in range(self.device_tabs.count()):
            if self.device_tabs.widget(i) == panel_widget:
                self.device_tabs.removeTab(i)
                break

        # Clean up references
        del self.device_panels[device_id]

        self.status_text.append(f"Removed device panel for {device_id}")
        self._refresh_device_list()

    def _update_device_gsr(self, device_id: str, gsr_data: dict):
        """Update GSR display for a specific device."""
        if device_id not in self.device_panels:
            return

        gsr_value = gsr_data.get('value', 0)
        gsr_label = self.device_panels[device_id]['gsr_label']
        gsr_label.setText(f"GSR: {gsr_value:.1f} μS")

    def _update_device_preview(self, device_id: str, frame_data: dict):
        """Update video preview for a specific device."""
        if device_id not in self.device_panels:
            return

        # Handle camera frame
        if 'camera_frame' in frame_data:
            # TODO: Decode and display camera frame
            camera_label = self.device_panels[device_id]['camera_label']
            # For now, just indicate frame received
            camera_label.setText("Camera Frame Received")

        # Handle thermal frame
        if 'thermal_frame' in frame_data:
            # TODO: Decode and display thermal frame
            thermal_label = self.device_panels[device_id]['thermal_label']
            # For now, just indicate frame received
            thermal_label.setText("Thermal Frame Received")

    def _update_device_status(self, device_id: str, status_data: dict):
        """Update device status display."""
        if device_id not in self.device_panels:
            return

        status = status_data.get('status', 'unknown')
        status_label = self.device_panels[device_id]['status_label']

        if status == 'recording':
            status_label.setText("Recording")
            status_label.setStyleSheet("color: red; font-weight: bold;")
        elif status == 'connected':
            status_label.setText("Connected")
            status_label.setStyleSheet("color: green; font-weight: bold;")
        else:
            status_label.setText(status)
            status_label.setStyleSheet("color: orange; font-weight: bold;")

    def _request_device_preview(self, device_id: str):
        """Request preview frame from a specific device."""
        self.device_manager.send_command(device_id, "request_preview", {})

    def _start_device_recording(self, device_id: str):
        """Start recording on a specific device."""
        session_info = self.session_manager.get_session_info()
        if not session_info or not session_info.get('session_id'):
            self.status_text.append("No active session. Please start a session first.")
            return

        command_data = {
            "session_id": session_info['session_id'],
            "timestamp": time.time()
        }

        self.device_manager.send_command(device_id, "start_recording", command_data)
        self.status_text.append(f"Start recording command sent to {device_id}")

    def _stop_device_recording(self, device_id: str):
        """Stop recording on a specific device."""
        command_data = {
            "stop_timestamp": time.time()
        }

        self.device_manager.send_command(device_id, "stop_recording", command_data)
        self.status_text.append(f"Stop recording command sent to {device_id}")

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
