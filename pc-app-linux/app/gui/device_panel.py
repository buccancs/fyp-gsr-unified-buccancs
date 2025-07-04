import base64

from PySide6.QtCore import Qt
from PySide6.QtGui import QImage, QPixmap
from PySide6.QtWidgets import (
    QGridLayout,
    QGroupBox,
    QLabel,
    QProgressBar,
    QPushButton,
    QVBoxLayout,
    QWidget,
)


class DevicePanel(QWidget):
    """A dedicated widget to display information and controls for a single connected device."""

    def __init__(self, device_id: str, device_info: dict, parent=None):
        super().__init__(parent)
        self.device_id = device_id
        self.device_info = device_info
        self._init_ui()

    def _init_ui(self):
        layout = QVBoxLayout(self)
        layout.setContentsMargins(5, 5, 5, 5)

        # --- Device Info ---
        info_group = QGroupBox(
            f"Device: {self.device_info.get('device_name', self.device_id)}"
        )
        info_layout = QGridLayout(info_group)
        self.status_label = QLabel("<b style='color: green;'>Connected</b>")
        self.battery_label = QLabel("Battery: N/A")
        self.storage_label = QLabel("Storage: N/A")
        info_layout.addWidget(QLabel("Status:"), 0, 0)
        info_layout.addWidget(self.status_label, 0, 1)
        info_layout.addWidget(QLabel("Battery:"), 1, 0)
        info_layout.addWidget(self.battery_label, 1, 1)
        info_layout.addWidget(QLabel("Storage:"), 2, 0)
        info_layout.addWidget(self.storage_label, 2, 1)
        layout.addWidget(info_group)

        # --- Previews ---
        preview_group = QGroupBox("Live Previews")
        preview_layout = QGridLayout(preview_group)
        self.camera_preview = QLabel("Camera Feed")
        self.camera_preview.setFixedSize(320, 240)
        self.camera_preview.setAlignment(Qt.AlignCenter)
        self.camera_preview.setStyleSheet(
            "background-color: #111; border: 1px solid #444;"
        )
        self.thermal_preview = QLabel("Thermal Feed")
        self.thermal_preview.setFixedSize(256, 192)
        self.thermal_preview.setAlignment(Qt.AlignCenter)
        self.thermal_preview.setStyleSheet(
            "background-color: #111; border: 1px solid #444;"
        )
        preview_layout.addWidget(self.camera_preview, 0, 0)
        preview_layout.addWidget(self.thermal_preview, 0, 1)
        layout.addWidget(preview_group)

        # --- Sensor Data ---
        sensor_group = QGroupBox("Sensor Data")
        sensor_layout = QVBoxLayout(sensor_group)
        self.gsr_label = QLabel("GSR: -- μS")
        self.gsr_label.setStyleSheet("font-size: 16px; font-weight: bold;")
        sensor_layout.addWidget(self.gsr_label)
        layout.addWidget(sensor_group)

        # --- File Transfer ---
        transfer_group = QGroupBox("File Transfer")
        transfer_layout = QVBoxLayout(transfer_group)
        self.transfer_progress = QProgressBar()
        self.transfer_progress.setVisible(False)
        transfer_layout.addWidget(self.transfer_progress)
        layout.addWidget(transfer_group)

        layout.addStretch()

    def update_status(self, status_data: dict):
        """Update the status labels for battery, storage, and recording state."""
        battery = status_data.get("battery_level", -1)
        storage = status_data.get("storage_free_gb", -1.0)
        recording = status_data.get("recording_status", "idle")

        self.battery_label.setText(f"{battery}%" if battery != -1 else "N/A")
        self.storage_label.setText(f"{storage:.2f} GB Free" if storage >= 0 else "N/A")

        if recording == "recording":
            self.status_label.setText("<b style='color: red;'>Recording</b>")
        else:
            self.status_label.setText("<b style='color: green;'>Connected</b>")

    def update_gsr(self, gsr_data: dict):
        """Update the GSR value display."""
        value = gsr_data.get("value", 0.0)
        resistance = 1.0 / value if value > 0 else 0.0
        self.gsr_label.setText(f"GSR: {value:.2f} μS ({resistance:.2f} kΩ)")

    def update_previews(self, frame_data: dict):
        """Decode and display JPEG frames for camera and thermal previews."""
        if "camera_frame" in frame_data:
            try:
                jpg_bytes = base64.b64decode(frame_data["camera_frame"])
                image = QImage()
                image.loadFromData(jpg_bytes, "JPG")
                pixmap = QPixmap.fromImage(image)
                self.camera_preview.setPixmap(
                    pixmap.scaled(320, 240, Qt.KeepAspectRatio, Qt.SmoothTransformation)
                )
            except Exception as e:
                self.camera_preview.setText("Frame Error")

        if "thermal_frame" in frame_data:
            try:
                jpg_bytes = base64.b64decode(frame_data["thermal_frame"])
                image = QImage()
                image.loadFromData(jpg_bytes, "JPG")
                pixmap = QPixmap.fromImage(image)
                self.thermal_preview.setPixmap(
                    pixmap.scaled(256, 192, Qt.KeepAspectRatio, Qt.SmoothTransformation)
                )
            except Exception as e:
                self.thermal_preview.setText("Frame Error")

    def update_transfer_progress(self, progress_data: dict):
        """Update the file transfer progress bar."""
        status = progress_data.get("status")
        if status == "transferring":
            self.transfer_progress.setVisible(True)
            total_size = progress_data.get("total_size", 1)
            transferred = progress_data.get("transferred_size", 0)
            percent = int((transferred / total_size) * 100) if total_size > 0 else 0
            self.transfer_progress.setValue(percent)
        elif status in ["completed", "failed"]:
            self.transfer_progress.setVisible(False)
