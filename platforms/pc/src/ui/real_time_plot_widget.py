#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Real-time Plot Widget for the PC Controller App.
Cross-platform support: Windows, macOS, Linux.
"""

import logging
from collections import deque
import time

from PySide6.QtCore import Qt, Signal, Slot, QTimer
from PySide6.QtWidgets import QVBoxLayout, QHBoxLayout, QLabel, QWidget, QGroupBox

try:
    import pyqtgraph as pg
    PYQTGRAPH_AVAILABLE = True
except ImportError:
    PYQTGRAPH_AVAILABLE = False
    pg = None

from utils.logger import get_logger


class RealTimePlotWidget(QWidget):
    """
    Real-time plotting widget for displaying GSR and heart rate data.
    """

    def __init__(self, parent=None):
        """
        Initialize the real-time plot widget.

        Args:
            parent: The parent widget (default: None)
        """
        super().__init__(parent)

        # Set up logging
        self.logger = get_logger(__name__)

        # Initialize data buffers
        self.max_points = 100  # Maximum number of points to display
        self.gsr_data = deque(maxlen=self.max_points)
        self.heart_rate_data = deque(maxlen=self.max_points)
        self.time_data = deque(maxlen=self.max_points)

        # Initialize current values
        self.current_gsr = 0.0
        self.current_heart_rate = 0

        # Set up UI
        self.setup_ui()

        # Set up update timer
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_plots)
        self.update_timer.start(100)  # Update every 100ms

        self.logger.info("Real-time plot widget initialized")

    def setup_ui(self):
        """
        Set up the UI for the plot widget.
        """
        # Create main layout
        self.main_layout = QVBoxLayout(self)

        # Create header with current values
        self.header_layout = QHBoxLayout()
        
        # Current GSR value
        self.gsr_group = QGroupBox("GSR (μS)")
        self.gsr_layout = QVBoxLayout()
        self.gsr_value_label = QLabel("0.00")
        self.gsr_value_label.setStyleSheet("font-size: 18px; font-weight: bold; color: blue;")
        self.gsr_layout.addWidget(self.gsr_value_label)
        self.gsr_group.setLayout(self.gsr_layout)
        self.header_layout.addWidget(self.gsr_group)

        # Current heart rate value
        self.hr_group = QGroupBox("Heart Rate (BPM)")
        self.hr_layout = QVBoxLayout()
        self.hr_value_label = QLabel("0")
        self.hr_value_label.setStyleSheet("font-size: 18px; font-weight: bold; color: red;")
        self.hr_layout.addWidget(self.hr_value_label)
        self.hr_group.setLayout(self.hr_layout)
        self.header_layout.addWidget(self.hr_group)

        self.main_layout.addLayout(self.header_layout)

        if PYQTGRAPH_AVAILABLE:
            # Create plot widgets
            self.setup_plots()
        else:
            # Show error message if pyqtgraph is not available
            error_label = QLabel("PyQtGraph not available. Please install it to enable real-time plotting.")
            error_label.setStyleSheet("color: red; font-weight: bold;")
            self.main_layout.addWidget(error_label)

    def setup_plots(self):
        """
        Set up the pyqtgraph plot widgets.
        """
        # GSR plot
        self.gsr_plot_widget = pg.PlotWidget(title="GSR Data (μS)")
        self.gsr_plot_widget.setLabel('left', 'GSR', units='μS')
        self.gsr_plot_widget.setLabel('bottom', 'Time', units='s')
        self.gsr_plot_widget.showGrid(x=True, y=True)
        self.gsr_plot_widget.setYRange(0, 50)  # Typical GSR range
        
        # Create GSR plot line
        self.gsr_line = self.gsr_plot_widget.plot(pen=pg.mkPen(color='b', width=2))
        
        self.main_layout.addWidget(self.gsr_plot_widget)

        # Heart rate plot
        self.hr_plot_widget = pg.PlotWidget(title="Heart Rate (BPM)")
        self.hr_plot_widget.setLabel('left', 'Heart Rate', units='BPM')
        self.hr_plot_widget.setLabel('bottom', 'Time', units='s')
        self.hr_plot_widget.showGrid(x=True, y=True)
        self.hr_plot_widget.setYRange(50, 150)  # Typical heart rate range
        
        # Create heart rate plot line
        self.hr_line = self.hr_plot_widget.plot(pen=pg.mkPen(color='r', width=2))
        
        self.main_layout.addWidget(self.hr_plot_widget)

    @Slot(float, int)
    def update_gsr_data(self, gsr_value, timestamp):
        """
        Update GSR data.

        Args:
            gsr_value: GSR value in microSiemens
            timestamp: Timestamp of the measurement
        """
        current_time = time.time()
        
        self.gsr_data.append(gsr_value)
        self.time_data.append(current_time)
        self.current_gsr = gsr_value

        # Update current value display
        self.gsr_value_label.setText(f"{gsr_value:.2f}")

        self.logger.debug(f"Updated GSR data: {gsr_value:.2f} μS")

    @Slot(int, int)
    def update_heart_rate_data(self, heart_rate, timestamp):
        """
        Update heart rate data.

        Args:
            heart_rate: Heart rate in BPM
            timestamp: Timestamp of the measurement
        """
        current_time = time.time()
        
        self.heart_rate_data.append(heart_rate)
        self.current_heart_rate = heart_rate

        # Update current value display
        self.hr_value_label.setText(f"{heart_rate}")

        self.logger.debug(f"Updated heart rate data: {heart_rate} BPM")

    def update_plots(self):
        """
        Update the plot displays.
        """
        if not PYQTGRAPH_AVAILABLE:
            return

        if len(self.time_data) > 1:
            # Convert time data to relative seconds
            base_time = self.time_data[0] if self.time_data else 0
            relative_times = [t - base_time for t in self.time_data]

            # Update GSR plot
            if len(self.gsr_data) > 0:
                self.gsr_line.setData(relative_times[-len(self.gsr_data):], list(self.gsr_data))

            # Update heart rate plot
            if len(self.heart_rate_data) > 0:
                self.hr_line.setData(relative_times[-len(self.heart_rate_data):], list(self.heart_rate_data))

    def clear_data(self):
        """
        Clear all plot data.
        """
        self.gsr_data.clear()
        self.heart_rate_data.clear()
        self.time_data.clear()
        
        self.current_gsr = 0.0
        self.current_heart_rate = 0
        
        # Update displays
        self.gsr_value_label.setText("0.00")
        self.hr_value_label.setText("0")

        if PYQTGRAPH_AVAILABLE:
            self.gsr_line.setData([], [])
            self.hr_line.setData([], [])

        self.logger.info("Cleared all plot data")

    def connect_device_signals(self, device):
        """
        Connect to device signals for real-time data updates.

        Args:
            device: The device to connect to
        """
        # Note: This would need to be implemented based on how GSR and heart rate
        # data signals are emitted from the device. For now, this is a placeholder.
        self.logger.info(f"Connected plot widget to device: {device.id}")