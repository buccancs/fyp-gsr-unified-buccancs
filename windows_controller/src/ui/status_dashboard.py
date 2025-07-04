#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Status Dashboard for the PC Controller App.
Cross-platform support: Windows, macOS, Linux.
"""

import logging
from PySide6.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel, 
                           QTableWidget, QTableWidgetItem, QHeaderView)
from PySide6.QtCore import Qt, Slot, Signal
from PySide6.QtGui import QColor, QBrush

from utils.logger import get_logger

class StatusDashboard(QWidget):
    """
    Status Dashboard class for displaying the status of all connected devices.
    """

    def __init__(self, parent=None):
        """
        Initialize the status dashboard.

        Args:
            parent: The parent widget (default: None)
        """
        super().__init__(parent)

        # Set up logging
        self.logger = get_logger(__name__)

        # Initialize state
        self.devices = {}  # Dictionary of devices by ID

        # Set up UI
        self.setup_ui()

        self.logger.info("Status dashboard initialized")

    def setup_ui(self):
        """
        Set up the UI for the status dashboard.
        """
        # Create main layout
        self.main_layout = QVBoxLayout(self)

        # Create header
        self.header_layout = QHBoxLayout()

        # Title
        self.title_label = QLabel("Device Status Dashboard")
        self.title_label.setStyleSheet("font-weight: bold; font-size: 16px;")
        self.header_layout.addWidget(self.title_label)

        # Status
        self.status_label = QLabel("No devices connected")
        self.header_layout.addWidget(self.status_label)

        self.header_layout.addStretch(1)

        self.main_layout.addLayout(self.header_layout)

        # Create table
        self.table = QTableWidget()
        self.table.setColumnCount(9)
        self.table.setHorizontalHeaderLabels([
            "Device ID", "Name", "Connection", "Recording", 
            "Battery", "Storage", "RGB Camera", "Thermal Camera", "GSR Sensor"
        ])
        self.table.horizontalHeader().setSectionResizeMode(QHeaderView.Stretch)
        self.table.verticalHeader().setVisible(False)
        self.main_layout.addWidget(self.table)

    def update_ui(self):
        """
        Update the UI with current device information.
        """
        # Update status label
        if not self.devices:
            self.status_label.setText("No devices connected")
        else:
            self.status_label.setText(f"{len(self.devices)} device(s) connected")

        # Update table
        self.table.setRowCount(len(self.devices))

        for i, (device_id, device) in enumerate(self.devices.items()):
            # Get device status
            status = device.get_status()

            # Device ID
            self.table.setItem(i, 0, QTableWidgetItem(device_id))

            # Name
            self.table.setItem(i, 1, QTableWidgetItem(device.name))

            # Connection
            connection_item = QTableWidgetItem("Connected" if status["connected"] else "Disconnected")
            connection_item.setForeground(QBrush(QColor("green" if status["connected"] else "red")))
            self.table.setItem(i, 2, connection_item)

            # Recording
            recording_item = QTableWidgetItem("Recording" if status["recording"] else "Not Recording")
            recording_item.setForeground(QBrush(QColor("red" if status["recording"] else "black")))
            self.table.setItem(i, 3, recording_item)

            # Battery
            battery_item = QTableWidgetItem(f"{status['battery_level']}%")
            if status["battery_level"] < 20:
                battery_item.setForeground(QBrush(QColor("red")))
            elif status["battery_level"] < 50:
                battery_item.setForeground(QBrush(QColor("orange")))
            else:
                battery_item.setForeground(QBrush(QColor("green")))
            self.table.setItem(i, 4, battery_item)

            # Storage
            storage_item = QTableWidgetItem(f"{status['storage_remaining']}%")
            if status["storage_remaining"] < 20:
                storage_item.setForeground(QBrush(QColor("red")))
            elif status["storage_remaining"] < 50:
                storage_item.setForeground(QBrush(QColor("orange")))
            else:
                storage_item.setForeground(QBrush(QColor("green")))
            self.table.setItem(i, 5, storage_item)

            # RGB Camera
            rgb_item = QTableWidgetItem("Active" if status["rgb_camera_active"] else "Inactive")
            rgb_item.setForeground(QBrush(QColor("green" if status["rgb_camera_active"] else "black")))
            self.table.setItem(i, 6, rgb_item)

            # Thermal Camera
            thermal_item = QTableWidgetItem("Active" if status["thermal_camera_active"] else "Inactive")
            thermal_item.setForeground(QBrush(QColor("green" if status["thermal_camera_active"] else "black")))
            self.table.setItem(i, 7, thermal_item)

            # GSR Sensor
            gsr_item = QTableWidgetItem("Active" if status["gsr_sensor_active"] else "Inactive")
            gsr_item.setForeground(QBrush(QColor("green" if status["gsr_sensor_active"] else "black")))
            self.table.setItem(i, 8, gsr_item)

    def add_device(self, device):
        """
        Add a device to the dashboard.

        Args:
            device: The device to add
        """
        self.logger.info(f"Adding device to dashboard: {device.id}")
        self.devices[device.id] = device
        self.update_ui()

    def remove_device(self, device_id):
        """
        Remove a device from the dashboard.

        Args:
            device_id: The ID of the device to remove
        """
        if device_id in self.devices:
            self.logger.info(f"Removing device from dashboard: {device_id}")
            del self.devices[device_id]
            self.update_ui()

    def clear_devices(self):
        """
        Clear all devices from the dashboard.
        """
        self.logger.info("Clearing all devices from dashboard")
        self.devices.clear()
        self.update_ui()
