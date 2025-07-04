"""
Data Visualization Widgets for GSR Multimodal System
Contains specialized widgets for displaying GSR data, thermal images, and analysis results.
"""

import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional

import matplotlib.dates as mdates
import matplotlib.pyplot as plt
import numpy as np
from matplotlib.backends.backend_qt5agg import FigureCanvasQTAgg as FigureCanvas
from matplotlib.figure import Figure
from PySide6.QtCore import Qt, QTimer, Signal
from PySide6.QtGui import QColor, QFont, QPainter, QPen, QPixmap
from PySide6.QtWidgets import (
    QCheckBox,
    QComboBox,
    QDoubleSpinBox,
    QGridLayout,
    QGroupBox,
    QHBoxLayout,
    QLabel,
    QProgressBar,
    QPushButton,
    QSlider,
    QSpinBox,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

logger = logging.getLogger(__name__)


class GSRPlotWidget(QWidget):
    """Widget for real-time GSR data plotting."""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setup_ui()
        self.gsr_data = {}  # device_id -> list of (timestamp, value) tuples
        self.max_points = 1000  # Maximum points to display
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self.update_plot)
        self.update_timer.start(100)  # Update every 100ms

    def setup_ui(self):
        """Set up the plotting interface."""
        layout = QVBoxLayout(self)

        # Create matplotlib figure and canvas
        self.figure = Figure(figsize=(12, 6))
        self.canvas = FigureCanvas(self.figure)
        layout.addWidget(self.canvas)

        # Control panel
        control_panel = QHBoxLayout()

        # Time range selector
        control_panel.addWidget(QLabel("Time Range:"))
        self.time_range_combo = QComboBox()
        self.time_range_combo.addItems(
            ["10 seconds", "30 seconds", "1 minute", "5 minutes", "All"]
        )
        self.time_range_combo.setCurrentText("30 seconds")
        self.time_range_combo.currentTextChanged.connect(self.on_time_range_changed)
        control_panel.addWidget(self.time_range_combo)

        control_panel.addStretch()

        # Auto-scale checkbox
        self.auto_scale_checkbox = QCheckBox("Auto Scale")
        self.auto_scale_checkbox.setChecked(True)
        control_panel.addWidget(self.auto_scale_checkbox)

        # Clear data button
        clear_button = QPushButton("Clear Data")
        clear_button.clicked.connect(self.clear_data)
        control_panel.addWidget(clear_button)

        layout.addLayout(control_panel)

        # Initialize plot
        self.ax = self.figure.add_subplot(111)
        self.ax.set_title("Real-time GSR Data")
        self.ax.set_xlabel("Time")
        self.ax.set_ylabel("GSR (µS)")
        self.ax.grid(True, alpha=0.3)

        # Format x-axis for time display
        self.ax.xaxis.set_major_formatter(mdates.DateFormatter("%H:%M:%S"))
        self.figure.autofmt_xdate()

        self.canvas.draw()

    def add_data_point(self, device_id: str, timestamp: float, value: float):
        """Add a new GSR data point."""
        if device_id not in self.gsr_data:
            self.gsr_data[device_id] = []

        # Convert timestamp to datetime
        dt = datetime.fromtimestamp(timestamp)
        self.gsr_data[device_id].append((dt, value))

        # Limit data points
        if len(self.gsr_data[device_id]) > self.max_points:
            self.gsr_data[device_id] = self.gsr_data[device_id][-self.max_points :]

    def update_plot(self):
        """Update the plot with current data."""
        if not self.gsr_data:
            return

        self.ax.clear()
        self.ax.set_title("Real-time GSR Data")
        self.ax.set_xlabel("Time")
        self.ax.set_ylabel("GSR (µS)")
        self.ax.grid(True, alpha=0.3)

        # Get time range
        time_range_text = self.time_range_combo.currentText()
        if time_range_text != "All":
            current_time = datetime.now()
            if "seconds" in time_range_text:
                seconds = int(time_range_text.split()[0])
                cutoff_time = current_time - timedelta(seconds=seconds)
            elif "minute" in time_range_text:
                minutes = int(time_range_text.split()[0])
                cutoff_time = current_time - timedelta(minutes=minutes)
            else:
                cutoff_time = None
        else:
            cutoff_time = None

        # Plot data for each device
        colors = ["blue", "red", "green", "orange", "purple"]
        for i, (device_id, data) in enumerate(self.gsr_data.items()):
            if not data:
                continue

            # Filter data by time range
            if cutoff_time:
                filtered_data = [(dt, val) for dt, val in data if dt >= cutoff_time]
            else:
                filtered_data = data

            if not filtered_data:
                continue

            times, values = zip(*filtered_data)
            color = colors[i % len(colors)]
            self.ax.plot(
                times, values, color=color, label=f"Device {device_id}", linewidth=1.5
            )

        # Auto-scale if enabled
        if self.auto_scale_checkbox.isChecked():
            self.ax.relim()
            self.ax.autoscale()

        # Format x-axis
        self.ax.xaxis.set_major_formatter(mdates.DateFormatter("%H:%M:%S"))
        self.figure.autofmt_xdate()

        # Add legend if multiple devices
        if len(self.gsr_data) > 1:
            self.ax.legend()

        self.canvas.draw()

    def on_time_range_changed(self):
        """Handle time range selection change."""
        self.update_plot()

    def clear_data(self):
        """Clear all GSR data."""
        self.gsr_data.clear()
        self.update_plot()


class ThermalVisualizationWidget(QWidget):
    """Widget for thermal camera data visualization."""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.setup_ui()
        self.thermal_data = {}  # device_id -> latest thermal data

    def setup_ui(self):
        """Set up the thermal visualization interface."""
        layout = QVBoxLayout(self)

        # Control panel
        control_group = QGroupBox("Thermal Display Controls")
        control_layout = QGridLayout(control_group)

        # Color palette selector
        control_layout.addWidget(QLabel("Color Palette:"), 0, 0)
        self.palette_combo = QComboBox()
        self.palette_combo.addItems(["Ironbow", "Rainbow", "Grayscale", "Hot", "Cool"])
        control_layout.addWidget(self.palette_combo, 0, 1)

        # Temperature range controls
        control_layout.addWidget(QLabel("Min Temp (°C):"), 1, 0)
        self.min_temp_spin = QDoubleSpinBox()
        self.min_temp_spin.setRange(-50, 100)
        self.min_temp_spin.setValue(20)
        control_layout.addWidget(self.min_temp_spin, 1, 1)

        control_layout.addWidget(QLabel("Max Temp (°C):"), 2, 0)
        self.max_temp_spin = QDoubleSpinBox()
        self.max_temp_spin.setRange(0, 200)
        self.max_temp_spin.setValue(40)
        control_layout.addWidget(self.max_temp_spin, 2, 1)

        # Auto-range checkbox
        self.auto_range_checkbox = QCheckBox("Auto Range")
        self.auto_range_checkbox.setChecked(True)
        control_layout.addWidget(self.auto_range_checkbox, 3, 0, 1, 2)

        layout.addWidget(control_group)

        # Thermal display area
        display_group = QGroupBox("Thermal Images")
        display_layout = QGridLayout(display_group)

        # Device 1 thermal display
        display_layout.addWidget(QLabel("Device 1:"), 0, 0)
        self.thermal_display_1 = QLabel("No thermal data")
        self.thermal_display_1.setMinimumSize(256, 192)
        self.thermal_display_1.setStyleSheet(
            "border: 1px solid gray; background-color: #f0f0f0;"
        )
        self.thermal_display_1.setAlignment(Qt.AlignCenter)
        display_layout.addWidget(self.thermal_display_1, 1, 0)

        # Device 1 temperature info
        self.temp_info_1 = QLabel("Min: -- °C, Max: -- °C, Avg: -- °C")
        display_layout.addWidget(self.temp_info_1, 2, 0)

        # Device 2 thermal display
        display_layout.addWidget(QLabel("Device 2:"), 0, 1)
        self.thermal_display_2 = QLabel("No thermal data")
        self.thermal_display_2.setMinimumSize(256, 192)
        self.thermal_display_2.setStyleSheet(
            "border: 1px solid gray; background-color: #f0f0f0;"
        )
        self.thermal_display_2.setAlignment(Qt.AlignCenter)
        display_layout.addWidget(self.thermal_display_2, 1, 1)

        # Device 2 temperature info
        self.temp_info_2 = QLabel("Min: -- °C, Max: -- °C, Avg: -- °C")
        display_layout.addWidget(self.temp_info_2, 2, 1)

        layout.addWidget(display_group)

    def update_thermal_data(self, device_id: str, thermal_data: Dict):
        """Update thermal data for a device."""
        self.thermal_data[device_id] = thermal_data

        # Update display based on device
        if device_id.endswith("1") or len(self.thermal_data) == 1:
            self.update_thermal_display(
                self.thermal_display_1, self.temp_info_1, thermal_data
            )
        else:
            self.update_thermal_display(
                self.thermal_display_2, self.temp_info_2, thermal_data
            )

    def update_thermal_display(
        self, display_label: QLabel, info_label: QLabel, thermal_data: Dict
    ):
        """Update a thermal display widget."""
        try:
            min_temp = thermal_data.get("min_temp", 0)
            max_temp = thermal_data.get("max_temp", 0)
            avg_temp = (min_temp + max_temp) / 2

            # Update temperature info
            info_label.setText(
                f"Min: {min_temp:.1f}°C, Max: {max_temp:.1f}°C, Avg: {avg_temp:.1f}°C"
            )

            # For now, just show text info (actual thermal image would require the image data)
            display_label.setText(
                f"Thermal Frame\n{thermal_data.get('width', 256)}x{thermal_data.get('height', 192)}\nFrame: {thermal_data.get('frame_number', 0)}"
            )

        except Exception as e:
            logger.error(f"Error updating thermal display: {e}")


class DataAnalysisWidget(QWidget):
    """Widget for real-time data analysis and statistics."""

    def __init__(self, parent=None):
        super().__init__(parent)
        self.gsr_handler = None
        self.setup_ui()
        self.analysis_timer = QTimer()
        self.analysis_timer.timeout.connect(self.update_analysis)
        self.analysis_timer.start(1000)  # Update every second

    def setup_ui(self):
        """Set up the analysis interface."""
        layout = QVBoxLayout(self)

        # GSR Statistics
        gsr_stats_group = QGroupBox("GSR Statistics")
        gsr_stats_layout = QGridLayout(gsr_stats_group)

        # Current values
        gsr_stats_layout.addWidget(QLabel("Current GSR:"), 0, 0)
        self.current_gsr_label = QLabel("-- µS")
        self.current_gsr_label.setStyleSheet("font-weight: bold; font-size: 14px;")
        gsr_stats_layout.addWidget(self.current_gsr_label, 0, 1)

        # Statistics
        gsr_stats_layout.addWidget(QLabel("Mean (10s):"), 1, 0)
        self.mean_gsr_label = QLabel("-- µS")
        gsr_stats_layout.addWidget(self.mean_gsr_label, 1, 1)

        gsr_stats_layout.addWidget(QLabel("Std Dev (10s):"), 2, 0)
        self.std_gsr_label = QLabel("-- µS")
        gsr_stats_layout.addWidget(self.std_gsr_label, 2, 1)

        gsr_stats_layout.addWidget(QLabel("Range (10s):"), 3, 0)
        self.range_gsr_label = QLabel("-- µS")
        gsr_stats_layout.addWidget(self.range_gsr_label, 3, 1)

        layout.addWidget(gsr_stats_group)

        # Data Quality
        quality_group = QGroupBox("Data Quality")
        quality_layout = QGridLayout(quality_group)

        quality_layout.addWidget(QLabel("Signal Quality:"), 0, 0)
        self.signal_quality_bar = QProgressBar()
        self.signal_quality_bar.setRange(0, 100)
        self.signal_quality_bar.setValue(0)
        quality_layout.addWidget(self.signal_quality_bar, 0, 1)

        quality_layout.addWidget(QLabel("Artifacts Detected:"), 1, 0)
        self.artifacts_label = QLabel("0%")
        quality_layout.addWidget(self.artifacts_label, 1, 1)

        quality_layout.addWidget(QLabel("Data Points (10s):"), 2, 0)
        self.data_points_label = QLabel("0")
        quality_layout.addWidget(self.data_points_label, 2, 1)

        layout.addWidget(quality_group)

        # Analysis Results
        analysis_group = QGroupBox("Analysis Results")
        analysis_layout = QVBoxLayout(analysis_group)

        self.analysis_text = QTextEdit()
        self.analysis_text.setMaximumHeight(150)
        self.analysis_text.setReadOnly(True)
        analysis_layout.addWidget(self.analysis_text)

        # Analysis controls
        controls_layout = QHBoxLayout()

        self.auto_analysis_checkbox = QCheckBox("Auto Analysis")
        self.auto_analysis_checkbox.setChecked(True)
        controls_layout.addWidget(self.auto_analysis_checkbox)

        analyze_button = QPushButton("Analyze Now")
        analyze_button.clicked.connect(self.perform_analysis)
        controls_layout.addWidget(analyze_button)

        clear_analysis_button = QPushButton("Clear Results")
        clear_analysis_button.clicked.connect(self.clear_analysis)
        controls_layout.addWidget(clear_analysis_button)

        controls_layout.addStretch()
        analysis_layout.addLayout(controls_layout)

        layout.addWidget(analysis_group)

        layout.addStretch()

    def set_gsr_handler(self, gsr_handler):
        """Set the GSR handler for data access."""
        self.gsr_handler = gsr_handler

    def update_analysis(self):
        """Update analysis displays."""
        if not self.gsr_handler:
            return

        try:
            # Get device status
            device_status = self.gsr_handler.get_device_status()

            if not device_status:
                self.current_gsr_label.setText("-- µS")
                return

            # Update for first available device
            device_id = list(device_status.keys())[0]
            latest_value = self.gsr_handler.get_latest_value(device_id)

            if latest_value is not None:
                self.current_gsr_label.setText(f"{latest_value:.2f} µS")

                # Get recent data for statistics
                timestamps, values = self.gsr_handler.get_data_array(
                    device_id, duration_seconds=10.0
                )

                if len(values) > 0:
                    mean_val = np.mean(values)
                    std_val = np.std(values)
                    range_val = np.max(values) - np.min(values)

                    self.mean_gsr_label.setText(f"{mean_val:.2f} µS")
                    self.std_gsr_label.setText(f"{std_val:.2f} µS")
                    self.range_gsr_label.setText(f"{range_val:.2f} µS")
                    self.data_points_label.setText(str(len(values)))

                    # Simple quality assessment
                    quality = max(
                        0, min(100, 100 - (std_val * 10))
                    )  # Lower std = higher quality
                    self.signal_quality_bar.setValue(int(quality))

                    # Artifact detection (simple threshold-based)
                    if len(values) > 10:
                        z_scores = (
                            np.abs((values - mean_val) / std_val)
                            if std_val > 0
                            else np.zeros_like(values)
                        )
                        artifacts = np.sum(z_scores > 3)
                        artifact_percentage = (artifacts / len(values)) * 100
                        self.artifacts_label.setText(f"{artifact_percentage:.1f}%")

            # Auto analysis
            if self.auto_analysis_checkbox.isChecked():
                self.perform_analysis()

        except Exception as e:
            logger.error(f"Error updating analysis: {e}")

    def perform_analysis(self):
        """Perform detailed GSR analysis."""
        if not self.gsr_handler:
            return

        try:
            device_status = self.gsr_handler.get_device_status()

            if not device_status:
                return

            analysis_results = []

            for device_id in device_status.keys():
                analysis = self.gsr_handler.analyze_device_data(device_id)

                if analysis.get("status") == "analyzed":
                    features = analysis.get("features", {})

                    result_text = f"Device {device_id}:\n"
                    result_text += f"  Data points: {analysis.get('data_points', 0)}\n"
                    result_text += f"  Mean: {features.get('mean', 0):.2f} µS\n"
                    result_text += f"  Std: {features.get('std', 0):.2f} µS\n"
                    result_text += f"  Range: {features.get('range', 0):.2f} µS\n"
                    result_text += (
                        f"  Artifacts: {analysis.get('artifact_percentage', 0):.1f}%\n"
                    )

                    analysis_results.append(result_text)

            if analysis_results:
                timestamp = datetime.now().strftime("%H:%M:%S")
                full_result = (
                    f"[{timestamp}] Analysis Results:\n"
                    + "\n".join(analysis_results)
                    + "\n"
                )
                self.analysis_text.append(full_result)

        except Exception as e:
            logger.error(f"Error performing analysis: {e}")

    def clear_analysis(self):
        """Clear analysis results."""
        self.analysis_text.clear()
