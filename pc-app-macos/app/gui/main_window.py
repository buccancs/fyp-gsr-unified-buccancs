import logging
import sys
import time

from PySide6.QtCore import QSize, Qt, QTimer
from PySide6.QtGui import QFont, QIcon, QImage, QPixmap
from PySide6.QtWidgets import (
    QApplication,
    QFrame,
    QGridLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QListWidget,
    QMainWindow,
    QPushButton,
    QStatusBar,
    QTabWidget,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

from app.gui.data_visualization import GSRPlotWidget
from app.gui.device_panel import DevicePanel
from app.network.device_manager import DeviceManager
from app.sensors.gsr_handler import GSRHandler
from app.sensors.webcam_handler import WebcamHandler
from app.session.session_manager import SessionManager
from app.transfer.file_transfer_manager import FileTransferManager

logger = logging.getLogger(__name__)


class MainWindow(QMainWindow):
    def __init__(
        self,
        device_manager: DeviceManager,
        gsr_handler: GSRHandler,
        webcam_handler: WebcamHandler,
        session_manager: SessionManager,
        file_transfer_manager: FileTransferManager,
    ):
        super().__init__()
        self.device_manager = device_manager
        self.gsr_handler = gsr_handler
        self.webcam_handler = webcam_handler
        self.session_manager = session_manager
        self.file_transfer_manager = file_transfer_manager

        self.device_panels = {}
        self.is_recording = False

        self.setWindowTitle("GSR Multimodal System Controller")
        self.setGeometry(100, 100, 1600, 900)
        self._apply_stylesheet()

        self.central_widget = QWidget()
        self.setCentralWidget(self.central_widget)
        self.main_layout = QHBoxLayout(self.central_widget)

        self._init_ui()
        self._init_timers()
        self._setup_callbacks()

    def _apply_stylesheet(self):
        """A modern, dark stylesheet for a professional look."""
        self.setStyleSheet(
            """
            QMainWindow, QWidget {
                background-color: #2E2E2E;
                color: #E0E0E0;
                font-family: 'Segoe UI', Arial, sans-serif;
            }
            QGroupBox {
                font-size: 14px;
                font-weight: bold;
                border: 1px solid #555;
                border-radius: 5px;
                margin-top: 1ex;
            }
            QGroupBox::title {
                subcontrol-origin: margin;
                subcontrol-position: top left;
                padding: 0 3px;
            }
            QPushButton {
                background-color: #555;
                color: #E0E0E0;
                border: 1px solid #666;
                padding: 8px;
                border-radius: 4px;
                font-size: 14px;
            }
            QPushButton:hover {
                background-color: #666;
            }
            QPushButton:pressed {
                background-color: #444;
            }
            QTextEdit, QListWidget {
                background-color: #252525;
                border: 1px solid #555;
                border-radius: 4px;
            }
            QTabWidget::pane {
                border: 1px solid #555;
            }
            QTabBar::tab {
                background: #444;
                color: #E0E0E0;
                padding: 8px;
                border-top-left-radius: 4px;
                border-top-right-radius: 4px;
            }
            QTabBar::tab:selected {
                background: #555;
                font-weight: bold;
            }
            QStatusBar {
                font-size: 12px;
            }
        """
        )

    def _init_ui(self):
        # --- Left Column: Session & Controls ---
        left_widget = QWidget()
        left_layout = QVBoxLayout(left_widget)
        left_widget.setFixedWidth(300)

        # Session Management
        session_group = QGroupBox("Session Control")
        session_layout = QVBoxLayout(session_group)
        self.start_session_button = QPushButton("Start New Session")
        self.start_session_button.clicked.connect(self._start_session)
        self.stop_session_button = QPushButton("End Session")
        self.stop_session_button.clicked.connect(self._stop_session)
        self.stop_session_button.setEnabled(False)
        self.session_status_label = QLabel("Status: No Active Session")
        session_layout.addWidget(self.start_session_button)
        session_layout.addWidget(self.stop_session_button)
        session_layout.addWidget(self.session_status_label)
        left_layout.addWidget(session_group)

        # Global Recording Controls
        record_group = QGroupBox("Global Recording")
        record_layout = QVBoxLayout(record_group)
        self.start_all_button = QPushButton("Start All Recording")
        self.start_all_button.setStyleSheet("background-color: #4CAF50;")
        self.start_all_button.clicked.connect(self._start_all_recording)
        self.stop_all_button = QPushButton("Stop All Recording")
        self.stop_all_button.setStyleSheet("background-color: #f44336;")
        self.stop_all_button.clicked.connect(self._stop_all_recording)
        self.start_all_button.setEnabled(False)
        self.stop_all_button.setEnabled(False)
        record_layout.addWidget(self.start_all_button)
        record_layout.addWidget(self.stop_all_button)
        left_layout.addWidget(record_group)

        # PC Webcam
        pc_webcam_group = QGroupBox("PC Webcam")
        pc_webcam_layout = QVBoxLayout(pc_webcam_group)
        self.webcam_label = QLabel("PC Webcam Feed")
        self.webcam_label.setAlignment(Qt.AlignCenter)
        self.webcam_label.setFixedSize(280, 210)
        self.webcam_label.setStyleSheet(
            "background-color: #111; border: 1px solid #444;"
        )
        self.start_webcam_button = QPushButton("Start Webcam")
        self.start_webcam_button.clicked.connect(self._start_pc_webcam)
        pc_webcam_layout.addWidget(self.webcam_label)
        pc_webcam_layout.addWidget(self.start_webcam_button)
        left_layout.addWidget(pc_webcam_group)

        left_layout.addStretch()
        self.main_layout.addWidget(left_widget)

        # --- Center Column: Main Display (GSR Plot) ---
        center_widget = QWidget()
        center_layout = QVBoxLayout(center_widget)
        self.gsr_plot_widget = GSRPlotWidget()
        center_layout.addWidget(self.gsr_plot_widget)
        self.main_layout.addWidget(center_widget)

        # --- Right Column: Device Management ---
        right_widget = QWidget()
        right_layout = QVBoxLayout(right_widget)
        right_widget.setFixedWidth(450)

        # Device Discovery
        discovery_group = QGroupBox("Connected Devices")
        discovery_layout = QVBoxLayout(discovery_group)
        self.device_list_label = QLabel("No devices connected.")
        discovery_layout.addWidget(self.device_list_label)
        right_layout.addWidget(discovery_group)

        # Device Tabs
        self.device_tabs = QTabWidget()
        right_layout.addWidget(self.device_tabs)
        self.main_layout.addWidget(right_widget)

        # --- Status Bar ---
        self.status_bar = QStatusBar()
        self.setStatusBar(self.status_bar)
        self.status_bar.showMessage("System Initialized. Waiting for connections.")

    def _init_timers(self):
        self.webcam_timer = QTimer(self)
        self.webcam_timer.timeout.connect(self._update_webcam_feed)

        self.device_status_timer = QTimer(self)
        self.device_status_timer.timeout.connect(self._update_device_list_label)
        self.device_status_timer.start(2000)  # Update every 2 seconds

    def _setup_callbacks(self):
        self.device_manager.add_message_callback(self._handle_device_message)
        self.gsr_handler.add_data_callback(self._handle_pc_gsr_data)
        if self.file_transfer_manager:
            self.file_transfer_manager.add_transfer_callback(
                self._handle_transfer_update
            )

    def _start_session(self):
        session_id = self.session_manager.start_session()
        self.status_bar.showMessage(f"Session '{session_id}' started.")
        self.session_status_label.setText(f"<b>Active Session:</b><br>{session_id}")
        self.start_session_button.setEnabled(False)
        self.stop_session_button.setEnabled(True)
        self.start_all_button.setEnabled(True)

    def _stop_session(self):
        if self.is_recording:
            self._stop_all_recording()
        self.session_manager.stop_session()
        self.status_bar.showMessage("Session ended.")
        self.session_status_label.setText("Status: No Active Session")
        self.start_session_button.setEnabled(True)
        self.stop_session_button.setEnabled(False)
        self.start_all_button.setEnabled(False)
        self.stop_all_button.setEnabled(False)

    def _refresh_device_list(self):
        """Refresh the list of connected devices."""
        self.device_list.clear()
        connected_devices = self.device_manager.get_connected_devices()
        for device_id in connected_devices:
            device_info = self.device_manager.get_device_info(device_id)
            if device_info:
                display_name = device_info.get("info", {}).get("device_name", device_id)
                self.device_list.addItem(f"{device_id} ({display_name})")

        self.status_text.append(f"Found {len(connected_devices)} connected devices")

    def _handle_device_message(self, device_id: str, message: dict):
        """Handle incoming messages from Android devices."""
        message_type = message.get("type", "unknown")

        if message_type == "device_connected":
            self._add_device_panel(device_id, message.get("data", {}))
        elif message_type == "device_disconnected":
            self._remove_device_panel(device_id)
        elif message_type == "gsr_data":
            self._update_device_gsr(device_id, message.get("data", {}))
        elif message_type == "frame_data":
            self._update_device_preview(device_id, message.get("data", {}))
        elif message_type == "status_update":
            self._update_device_status(device_id, message.get("data", {}))

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

        device_name = device_info.get("device_name", "Unknown Device")
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
        start_device_button.clicked.connect(
            lambda: self._start_device_recording(device_id)
        )

        stop_device_button = QPushButton(f"Stop {device_id}")
        stop_device_button.clicked.connect(
            lambda: self._stop_device_recording(device_id)
        )

        controls_layout.addWidget(start_device_button)
        controls_layout.addWidget(stop_device_button)

        device_layout.addWidget(controls_group)

        # Store references to UI elements for updates
        panel_data = {
            "widget": device_widget,
            "status_label": status_label,
            "camera_label": camera_label,
            "thermal_label": thermal_label,
            "gsr_label": gsr_value_label,
            "start_button": start_device_button,
            "stop_button": stop_device_button,
        }

        self.device_panels[device_id] = panel_data

        # Add tab to device tabs
        display_name = device_info.get("device_name", device_id)
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
        panel_widget = self.device_panels[device_id]["widget"]
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

        gsr_value = gsr_data.get("value", 0)
        gsr_label = self.device_panels[device_id]["gsr_label"]
        gsr_label.setText(f"GSR: {gsr_value:.1f} μS")

    def _update_device_preview(self, device_id: str, frame_data: dict):
        """Update video preview for a specific device."""
        if device_id not in self.device_panels:
            return

        # Handle camera frame
        if "camera_frame" in frame_data:
            try:
                camera_label = self.device_panels[device_id]["camera_label"]

                # Decode base64 JPEG data
                if isinstance(frame_data["camera_frame"], str):
                    # Base64 encoded JPEG
                    jpeg_data = base64.b64decode(frame_data["camera_frame"])
                else:
                    # Raw bytes
                    jpeg_data = frame_data["camera_frame"]

                # Convert JPEG bytes to PIL Image
                image = Image.open(BytesIO(jpeg_data))

                # Convert PIL Image to QPixmap
                image_array = np.array(image)
                if len(image_array.shape) == 3:  # RGB image
                    height, width, channel = image_array.shape
                    bytes_per_line = 3 * width
                    q_image = QImage(
                        image_array.data,
                        width,
                        height,
                        bytes_per_line,
                        QImage.Format_RGB888,
                    )
                else:  # Grayscale image
                    height, width = image_array.shape
                    bytes_per_line = width
                    q_image = QImage(
                        image_array.data,
                        width,
                        height,
                        bytes_per_line,
                        QImage.Format_Grayscale8,
                    )

                # Scale image to fit label while maintaining aspect ratio
                pixmap = QPixmap.fromImage(q_image)
                scaled_pixmap = pixmap.scaled(
                    camera_label.size(), Qt.KeepAspectRatio, Qt.SmoothTransformation
                )
                camera_label.setPixmap(scaled_pixmap)

            except Exception as e:
                logging.error(
                    f"Error decoding camera frame for device {device_id}: {e}"
                )
                camera_label.setText("Camera Frame Error")

        # Handle thermal frame
        if "thermal_frame" in frame_data:
            try:
                thermal_label = self.device_panels[device_id]["thermal_label"]

                # Decode base64 JPEG data
                if isinstance(frame_data["thermal_frame"], str):
                    # Base64 encoded JPEG
                    jpeg_data = base64.b64decode(frame_data["thermal_frame"])
                else:
                    # Raw bytes
                    jpeg_data = frame_data["thermal_frame"]

                # Convert JPEG bytes to PIL Image
                image = Image.open(BytesIO(jpeg_data))

                # Convert PIL Image to QPixmap
                image_array = np.array(image)
                if (
                    len(image_array.shape) == 3
                ):  # RGB image (thermal with color palette)
                    height, width, channel = image_array.shape
                    bytes_per_line = 3 * width
                    q_image = QImage(
                        image_array.data,
                        width,
                        height,
                        bytes_per_line,
                        QImage.Format_RGB888,
                    )
                else:  # Grayscale image
                    height, width = image_array.shape
                    bytes_per_line = width
                    q_image = QImage(
                        image_array.data,
                        width,
                        height,
                        bytes_per_line,
                        QImage.Format_Grayscale8,
                    )

                # Scale image to fit label while maintaining aspect ratio
                pixmap = QPixmap.fromImage(q_image)
                scaled_pixmap = pixmap.scaled(
                    thermal_label.size(), Qt.KeepAspectRatio, Qt.SmoothTransformation
                )
                thermal_label.setPixmap(scaled_pixmap)

            except Exception as e:
                logging.error(
                    f"Error decoding thermal frame for device {device_id}: {e}"
                )
                thermal_label.setText("Thermal Frame Error")

    def _update_device_status(self, device_id: str, status_data: dict):
        """Update device status display."""
        if device_id not in self.device_panels:
            return

        status = status_data.get("status", "unknown")
        status_label = self.device_panels[device_id]["status_label"]

        if status == "recording":
            status_label.setText("Recording")
            status_label.setStyleSheet("color: red; font-weight: bold;")
        elif status == "connected":
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
        if not session_info or not session_info.get("session_id"):
            self.status_text.append("No active session. Please start a session first.")
            return

        command_data = {
            "session_id": session_info["session_id"],
            "timestamp": time.time(),
        }

        self.device_manager.send_command(device_id, "start_recording", command_data)
        self.status_text.append(f"Start recording command sent to {device_id}")

    def _stop_device_recording(self, device_id: str):
        """Stop recording on a specific device."""
        command_data = {"stop_timestamp": time.time()}

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
            qt_image = QImage(
                frame.data, w, h, bytes_per_line, QImage.Format_RGB888
            ).rgbSwapped()
            self.webcam_label.setPixmap(QPixmap.fromImage(qt_image))

    def _start_all_recording(self):
        if not self.session_manager.is_session_active():
            self.status_bar.showMessage(
                "Error: No active session. Please start a session first."
            )
            return

        self.is_recording = True
        self.start_all_button.setEnabled(False)
        self.stop_all_button.setEnabled(True)
        self.status_bar.showMessage("Recording started on all devices...")

        # Start PC components
        if self.webcam_handler.is_capturing:
            session_dir = self.session_manager.current_session_dir
            webcam_path = session_dir / "pc_webcam.mp4"
            self.webcam_handler.start_recording(str(webcam_path))
        self.gsr_handler.start_gsr_capture()

        # Send command to Android devices
        command_data = {"session_id": self.session_manager.session_id}
        self.device_manager.send_command_to_all("start_recording", command_data)

    def _stop_all_recording(self):
        self.is_recording = False
        self.start_all_button.setEnabled(True)
        self.stop_all_button.setEnabled(False)
        self.status_bar.showMessage("Stopping recording and starting file transfer...")

        # Stop PC components
        self.webcam_handler.stop_recording()
        self.gsr_handler.stop_gsr_capture()

        # Send command to Android devices
        command_data = {"session_id": self.session_manager.session_id}
        self.device_manager.send_command_to_all("stop_recording", command_data)

        # Initiate file transfer
        time.sleep(1)  # Give devices a moment to finalize files
        for device_id in self.device_panels.keys():
            device_dir = self.session_manager.get_device_directory(device_id)
            if device_dir and self.file_transfer_manager:
                self.file_transfer_manager.start_file_transfer(
                    self.device_manager, device_id, str(device_dir)
                )

    def _start_pc_webcam(self):
        if self.webcam_handler.start_capture():
            self.webcam_timer.start(33)  # ~30 FPS
            self.status_bar.showMessage("PC Webcam started.")
            self.start_webcam_button.setText("Stop Webcam")
            self.start_webcam_button.clicked.disconnect()
            self.start_webcam_button.clicked.connect(self._stop_pc_webcam)
        else:
            self.status_bar.showMessage("Failed to start PC Webcam.")

    def _stop_pc_webcam(self):
        self.webcam_timer.stop()
        self.webcam_handler.shutdown()
        self.webcam_label.clear()
        self.webcam_label.setStyleSheet(
            "background-color: #111; border: 1px solid #444;"
        )
        self.webcam_label.setText("PC Webcam Feed")
        self.status_bar.showMessage("PC Webcam stopped.")
        self.start_webcam_button.setText("Start Webcam")
        self.start_webcam_button.clicked.disconnect()
        self.start_webcam_button.clicked.connect(self._start_pc_webcam)

    def _update_webcam_feed(self):
        frame = self.webcam_handler.get_latest_frame()
        if frame is not None:
            h, w, ch = frame.shape
            qt_image = QImage(
                frame.data, w, h, ch * w, QImage.Format_RGB888
            ).rgbSwapped()
            pixmap = QPixmap.fromImage(qt_image)
            self.webcam_label.setPixmap(
                pixmap.scaled(280, 210, Qt.KeepAspectRatio, Qt.SmoothTransformation)
            )

    def _update_device_list_label(self):
        devices = self.device_manager.get_connected_devices()
        if not devices:
            self.device_list_label.setText("No devices connected.")
        else:
            self.device_list_label.setText(f"{len(devices)} device(s) connected.")

    def _handle_device_message(self, device_id: str, message: dict):
        msg_type = message.get("command") or message.get("type")
        data = message.get("data", {})

        if msg_type == "register_device":
            if device_id not in self.device_panels:
                self._add_device_panel(device_id, data)
        elif device_id in self.device_panels:
            panel = self.device_panels[device_id]
            if msg_type == "status_update":
                panel.update_status(data)
            elif msg_type == "data_stream" and data.get("data_type") == "gsr":
                panel.update_gsr(data.get("payload", {}))
                self.gsr_plot_widget.add_data_point(
                    device_id, time.time(), data.get("payload", {}).get("value", 0)
                )
            elif msg_type == "data_stream" and data.get("data_type") == "video_frame":
                panel.update_previews(data.get("payload", {}))

    def _add_device_panel(self, device_id: str, device_info: dict):
        panel = DevicePanel(device_id, device_info)
        self.device_panels[device_id] = panel
        self.device_tabs.addTab(panel, device_info.get("device_name", device_id))
        self.status_bar.showMessage(
            f"Device '{device_info.get('device_name', device_id)}' connected."
        )

    def _handle_pc_gsr_data(self, gsr_data: dict):
        self.gsr_plot_widget.add_data_point(
            "PC_GSR", gsr_data["timestamp"], gsr_data["conductance"]
        )

    def _handle_transfer_update(self, device_id: str, event: str, data: dict):
        if device_id in self.device_panels:
            self.device_panels[device_id].update_transfer_progress(data)
        self.status_bar.showMessage(f"Device {device_id}: File transfer {event}.")

    def closeEvent(self, event):
        """Ensure cleanup on window close."""
        self._stop_session()
        self.webcam_handler.shutdown()
        self.device_manager.stop_server()
        super().closeEvent(event)


if __name__ == "__main__":
    app = QApplication(sys.argv)
    # This is for testing the UI in isolation. In main.py, the real managers are passed.
    mock_session = SessionManager()
    mock_device = DeviceManager(mock_session)
    mock_gsr = GSRHandler()
    mock_webcam = WebcamHandler()
    mock_transfer = FileTransferManager(mock_session)
    window = MainWindow(mock_device, mock_gsr, mock_webcam, mock_session, mock_transfer)
    window.show()
    sys.exit(app.exec())
