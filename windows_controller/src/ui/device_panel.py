#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Device Panel for the PC Controller App.
Cross-platform support: Windows, macOS, Linux.
"""

import logging
from PySide6.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel, 
                           QPushButton, QGroupBox, QGridLayout, QProgressBar)
from PySide6.QtCore import Qt, Slot, Signal
from PySide6.QtGui import QPixmap, QImage

from ui.video_preview import VideoPreview
from utils.logger import get_logger

class DevicePanel(QWidget):
    """
    Device Panel class for displaying a connected device in the UI.
    """

    # Define signals
    disconnect_requested = Signal(str)  # device_id

    def __init__(self, device):
        """
        Initialize the device panel.

        Args:
            device: The device to display
        """
        super().__init__()

        # Set up logging
        self.logger = get_logger(__name__)

        # Store device
        self.device = device

        # Set up UI
        self.setup_ui()

        # Connect signals
        self.connect_signals()

        # Update UI
        self.update_ui()

        self.logger.info(f"Device panel initialized for device: {device.name} ({device.id})")

    def setup_ui(self):
        """
        Set up the UI for the device panel.
        """
        # Create main layout
        self.main_layout = QVBoxLayout(self)

        # Create device header
        self.header_layout = QHBoxLayout()

        # Device name and status
        self.device_name_label = QLabel(self.device.name)
        self.device_name_label.setStyleSheet("font-weight: bold; font-size: 14px;")
        self.header_layout.addWidget(self.device_name_label)

        self.device_status_label = QLabel("Not Connected")
        self.header_layout.addWidget(self.device_status_label)

        self.header_layout.addStretch(1)

        # Disconnect button
        self.disconnect_button = QPushButton("Disconnect")
        self.disconnect_button.clicked.connect(self.on_disconnect_clicked)
        self.header_layout.addWidget(self.disconnect_button)

        self.main_layout.addLayout(self.header_layout)

        # Create video preview group
        self.video_group = QGroupBox("Video Preview")
        self.video_layout = QVBoxLayout()

        # Create video preview widgets
        self.rgb_preview = VideoPreview("RGB")
        self.thermal_preview = VideoPreview("Thermal")

        # Add video previews to layout
        self.video_layout.addWidget(self.rgb_preview)
        self.video_layout.addWidget(self.thermal_preview)

        self.video_group.setLayout(self.video_layout)
        self.main_layout.addWidget(self.video_group)

        # Create status group
        self.status_group = QGroupBox("Device Status")
        self.status_layout = QGridLayout()

        # Add status indicators
        self.status_layout.addWidget(QLabel("Connection:"), 0, 0)
        self.connection_status_label = QLabel("Not Connected")
        self.status_layout.addWidget(self.connection_status_label, 0, 1)

        self.status_layout.addWidget(QLabel("Recording:"), 1, 0)
        self.recording_status_label = QLabel("Not Recording")
        self.status_layout.addWidget(self.recording_status_label, 1, 1)

        self.status_layout.addWidget(QLabel("Battery:"), 2, 0)
        self.battery_progress = QProgressBar()
        self.battery_progress.setRange(0, 100)
        self.battery_progress.setValue(0)
        self.status_layout.addWidget(self.battery_progress, 2, 1)

        self.status_layout.addWidget(QLabel("Storage:"), 3, 0)
        self.storage_progress = QProgressBar()
        self.storage_progress.setRange(0, 100)
        self.storage_progress.setValue(0)
        self.status_layout.addWidget(self.storage_progress, 3, 1)

        # Add sensor status indicators
        self.status_layout.addWidget(QLabel("RGB Camera:"), 0, 2)
        self.rgb_status_label = QLabel("Inactive")
        self.status_layout.addWidget(self.rgb_status_label, 0, 3)

        self.status_layout.addWidget(QLabel("Thermal Camera:"), 1, 2)
        self.thermal_status_label = QLabel("Inactive")
        self.status_layout.addWidget(self.thermal_status_label, 1, 3)

        self.status_layout.addWidget(QLabel("GSR Sensor:"), 2, 2)
        self.gsr_status_label = QLabel("Inactive")
        self.status_layout.addWidget(self.gsr_status_label, 2, 3)

        self.status_layout.addWidget(QLabel("Error:"), 3, 2)
        self.error_label = QLabel("None")
        self.status_layout.addWidget(self.error_label, 3, 3)

        self.status_group.setLayout(self.status_layout)
        self.main_layout.addWidget(self.status_group)

    def connect_signals(self):
        """
        Connect signals and slots.
        """
        # Connect device signals
        self.device.status_updated.connect(self.on_device_status_updated)
        self.device.video_frame_received.connect(self.on_video_frame_received)

    def update_ui(self):
        """
        Update the UI with current device information.
        """
        # Get device status
        status = self.device.get_status()

        # Update connection status
        if status["connected"]:
            self.connection_status_label.setText("Connected")
            self.connection_status_label.setStyleSheet("color: green;")
            self.device_status_label.setText("Connected")
            self.device_status_label.setStyleSheet("color: green;")
        else:
            self.connection_status_label.setText("Not Connected")
            self.connection_status_label.setStyleSheet("color: red;")
            self.device_status_label.setText("Not Connected")
            self.device_status_label.setStyleSheet("color: red;")

        # Update recording status
        if status["recording"]:
            self.recording_status_label.setText("Recording")
            self.recording_status_label.setStyleSheet("color: red;")
        else:
            self.recording_status_label.setText("Not Recording")
            self.recording_status_label.setStyleSheet("")

        # Update battery and storage
        self.battery_progress.setValue(status["battery_level"])
        self.storage_progress.setValue(status["storage_remaining"])

        # Update sensor status
        if status["rgb_camera_active"]:
            self.rgb_status_label.setText("Active")
            self.rgb_status_label.setStyleSheet("color: green;")
        else:
            self.rgb_status_label.setText("Inactive")
            self.rgb_status_label.setStyleSheet("")

        if status["thermal_camera_active"]:
            self.thermal_status_label.setText("Active")
            self.thermal_status_label.setStyleSheet("color: green;")
        else:
            self.thermal_status_label.setText("Inactive")
            self.thermal_status_label.setStyleSheet("")

        if status["gsr_sensor_active"]:
            self.gsr_status_label.setText("Active")
            self.gsr_status_label.setStyleSheet("color: green;")
        else:
            self.gsr_status_label.setText("Inactive")
            self.gsr_status_label.setStyleSheet("")

        # Update error
        if status["error"]:
            self.error_label.setText(status["error"])
            self.error_label.setStyleSheet("color: red;")
        else:
            self.error_label.setText("None")
            self.error_label.setStyleSheet("")

    @Slot()
    def on_disconnect_clicked(self):
        """
        Handle the disconnect button click.
        """
        self.logger.info(f"Disconnect button clicked for device: {self.device.id}")
        self.disconnect_requested.emit(self.device.id)

    @Slot(object)
    def on_device_status_updated(self, device):
        """
        Handle device status updates.

        Args:
            device: The device that was updated
        """
        if device.id == self.device.id:
            self.update_ui()

    @Slot(object, str)
    def on_video_frame_received(self, device, frame_type):
        """
        Handle video frame updates.

        Args:
            device: The device that sent the frame
            frame_type: The type of frame (rgb/thermal)
        """
        if device.id != self.device.id:
            return

        # In a real implementation, we would update the video preview with the actual frame
        # For now, just update the preview with a placeholder
        if frame_type == "rgb":
            self.rgb_preview.update_status("Receiving")
        elif frame_type == "thermal":
            self.thermal_preview.update_status("Receiving")
