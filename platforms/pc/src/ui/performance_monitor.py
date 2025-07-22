"""Performance Monitoring Dashboard for FYP-GSR System

This module provides a comprehensive performance monitoring dashboard that tracks
system performance, resource usage, data throughput, and device health in real-time.

Features:
- Real-time CPU, memory, and network monitoring
- Device-specific performance metrics
- Data throughput visualization
- Performance alerts and notifications
- Historical performance data logging
- Export capabilities for performance analysis

Author: FYP-GSR Team
"""

import logging
import os
import sys
import time
import psutil
import threading
from collections import deque
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple

from PySide6.QtCore import Qt, QTimer, Signal, QThread, QObject
from PySide6.QtGui import QFont, QIcon, QPalette, QColor
from PySide6.QtWidgets import (
    QWidget, QVBoxLayout, QHBoxLayout, QGridLayout, QGroupBox,
    QLabel, QPushButton, QProgressBar, QTabWidget, QTextEdit,
    QTableWidget, QTableWidgetItem, QHeaderView, QSplitter,
    QScrollArea, QFrame, QCheckBox, QSpinBox, QComboBox,
    QMessageBox, QFileDialog
)

# Import plotting capabilities
try:
    import matplotlib.pyplot as plt
    from matplotlib.backends.backend_qt5agg import FigureCanvasQTAgg as FigureCanvas
    from matplotlib.figure import Figure
    import matplotlib.dates as mdates
    MATPLOTLIB_AVAILABLE = True
except ImportError:
    MATPLOTLIB_AVAILABLE = False

from utils.logger import get_logger


class PerformanceMetrics:
    """Container for performance metrics data."""
    
    def __init__(self):
        self.timestamp = datetime.now()
        self.cpu_percent = 0.0
        self.memory_percent = 0.0
        self.memory_used_mb = 0.0
        self.memory_total_mb = 0.0
        self.disk_usage_percent = 0.0
        self.network_bytes_sent = 0
        self.network_bytes_recv = 0
        self.device_count = 0
        self.active_recordings = 0
        self.data_throughput_mbps = 0.0
        self.frame_rate = 0.0
        self.dropped_frames = 0
        self.latency_ms = 0.0


class PerformanceCollector(QThread):
    """Background thread for collecting performance metrics."""
    
    metrics_updated = Signal(object)  # PerformanceMetrics object
    
    def __init__(self):
        super().__init__()
        self.running = False
        self.collection_interval = 1.0  # seconds
        self.logger = get_logger(__name__)
        self.last_network_stats = None
        
    def set_collection_interval(self, interval: float):
        """Set the metrics collection interval in seconds."""
        self.collection_interval = max(0.1, interval)
        
    def run(self):
        """Main collection loop."""
        self.running = True
        self.logger.info("Performance collector started")
        
        while self.running:
            try:
                metrics = self._collect_metrics()
                self.metrics_updated.emit(metrics)
                time.sleep(self.collection_interval)
            except Exception as e:
                self.logger.error(f"Error collecting performance metrics: {e}")
                time.sleep(self.collection_interval)
                
    def stop(self):
        """Stop the collection thread."""
        self.running = False
        self.logger.info("Performance collector stopped")
        
    def _collect_metrics(self) -> PerformanceMetrics:
        """Collect current system performance metrics."""
        metrics = PerformanceMetrics()
        
        # CPU usage
        metrics.cpu_percent = psutil.cpu_percent(interval=None)
        
        # Memory usage
        memory = psutil.virtual_memory()
        metrics.memory_percent = memory.percent
        metrics.memory_used_mb = memory.used / (1024 * 1024)
        metrics.memory_total_mb = memory.total / (1024 * 1024)
        
        # Disk usage
        disk = psutil.disk_usage('/')
        metrics.disk_usage_percent = (disk.used / disk.total) * 100
        
        # Network usage
        network = psutil.net_io_counters()
        metrics.network_bytes_sent = network.bytes_sent
        metrics.network_bytes_recv = network.bytes_recv
        
        # Calculate network throughput if we have previous data
        if self.last_network_stats:
            time_diff = (metrics.timestamp - self.last_network_stats[0]).total_seconds()
            if time_diff > 0:
                sent_diff = metrics.network_bytes_sent - self.last_network_stats[1]
                recv_diff = metrics.network_bytes_recv - self.last_network_stats[2]
                total_bytes = sent_diff + recv_diff
                metrics.data_throughput_mbps = (total_bytes / time_diff) / (1024 * 1024)
        
        self.last_network_stats = (metrics.timestamp, metrics.network_bytes_sent, metrics.network_bytes_recv)
        
        return metrics


class PerformanceChart(QWidget):
    """Real-time performance chart widget."""
    
    def __init__(self, title: str, max_points: int = 100):
        super().__init__()
        self.title = title
        self.max_points = max_points
        self.data_points = deque(maxlen=max_points)
        self.timestamps = deque(maxlen=max_points)
        
        if MATPLOTLIB_AVAILABLE:
            self._setup_matplotlib_chart()
        else:
            self._setup_fallback_chart()
            
    def _setup_matplotlib_chart(self):
        """Setup matplotlib-based chart."""
        self.figure = Figure(figsize=(8, 4), dpi=100)
        self.canvas = FigureCanvas(self.figure)
        self.axes = self.figure.add_subplot(111)
        
        layout = QVBoxLayout()
        layout.addWidget(self.canvas)
        self.setLayout(layout)
        
        # Configure axes
        self.axes.set_title(self.title)
        self.axes.set_ylabel('Value')
        self.axes.grid(True, alpha=0.3)
        
    def _setup_fallback_chart(self):
        """Setup fallback text-based display when matplotlib is not available."""
        layout = QVBoxLayout()
        
        title_label = QLabel(self.title)
        title_label.setFont(QFont("Arial", 12, QFont.Bold))
        layout.addWidget(title_label)
        
        self.value_label = QLabel("No data")
        self.value_label.setFont(QFont("Arial", 24))
        self.value_label.setAlignment(Qt.AlignCenter)
        layout.addWidget(self.value_label)
        
        self.setLayout(layout)
        
    def add_data_point(self, value: float, timestamp: datetime = None):
        """Add a new data point to the chart."""
        if timestamp is None:
            timestamp = datetime.now()
            
        self.data_points.append(value)
        self.timestamps.append(timestamp)
        
        if MATPLOTLIB_AVAILABLE:
            self._update_matplotlib_chart()
        else:
            self._update_fallback_chart(value)
            
    def _update_matplotlib_chart(self):
        """Update matplotlib chart."""
        self.axes.clear()
        
        if len(self.data_points) > 1:
            self.axes.plot(list(self.timestamps), list(self.data_points), 'b-', linewidth=2)
            
        self.axes.set_title(self.title)
        self.axes.set_ylabel('Value')
        self.axes.grid(True, alpha=0.3)
        
        # Format x-axis for time
        if len(self.timestamps) > 0:
            self.axes.xaxis.set_major_formatter(mdates.DateFormatter('%H:%M:%S'))
            self.figure.autofmt_xdate()
            
        self.canvas.draw()
        
    def _update_fallback_chart(self, value: float):
        """Update fallback text display."""
        self.value_label.setText(f"{value:.2f}")


class DevicePerformanceWidget(QWidget):
    """Widget for displaying device-specific performance metrics."""
    
    def __init__(self):
        super().__init__()
        self.device_metrics = {}
        self._setup_ui()
        
    def _setup_ui(self):
        """Setup the device performance UI."""
        layout = QVBoxLayout()
        
        # Title
        title = QLabel("Device Performance")
        title.setFont(QFont("Arial", 14, QFont.Bold))
        layout.addWidget(title)
        
        # Device table
        self.device_table = QTableWidget()
        self.device_table.setColumnCount(6)
        self.device_table.setHorizontalHeaderLabels([
            "Device", "Status", "CPU %", "Memory MB", "Throughput MB/s", "Latency ms"
        ])
        
        # Make table headers stretch
        header = self.device_table.horizontalHeader()
        header.setSectionResizeMode(QHeaderView.Stretch)
        
        layout.addWidget(self.device_table)
        self.setLayout(layout)
        
    def update_device_metrics(self, device_name: str, metrics: Dict):
        """Update metrics for a specific device."""
        self.device_metrics[device_name] = metrics
        self._refresh_table()
        
    def _refresh_table(self):
        """Refresh the device table with current metrics."""
        self.device_table.setRowCount(len(self.device_metrics))
        
        for row, (device_name, metrics) in enumerate(self.device_metrics.items()):
            self.device_table.setItem(row, 0, QTableWidgetItem(device_name))
            self.device_table.setItem(row, 1, QTableWidgetItem(metrics.get('status', 'Unknown')))
            self.device_table.setItem(row, 2, QTableWidgetItem(f"{metrics.get('cpu_percent', 0):.1f}"))
            self.device_table.setItem(row, 3, QTableWidgetItem(f"{metrics.get('memory_mb', 0):.1f}"))
            self.device_table.setItem(row, 4, QTableWidgetItem(f"{metrics.get('throughput_mbps', 0):.2f}"))
            self.device_table.setItem(row, 5, QTableWidgetItem(f"{metrics.get('latency_ms', 0):.1f}"))


class PerformanceAlerts(QWidget):
    """Widget for managing performance alerts and thresholds."""
    
    def __init__(self):
        super().__init__()
        self.alert_thresholds = {
            'cpu_percent': 80.0,
            'memory_percent': 85.0,
            'disk_percent': 90.0,
            'latency_ms': 100.0
        }
        self.active_alerts = set()
        self._setup_ui()
        
    def _setup_ui(self):
        """Setup the alerts UI."""
        layout = QVBoxLayout()
        
        # Title
        title = QLabel("Performance Alerts")
        title.setFont(QFont("Arial", 14, QFont.Bold))
        layout.addWidget(title)
        
        # Threshold settings
        thresholds_group = QGroupBox("Alert Thresholds")
        thresholds_layout = QGridLayout()
        
        self.threshold_spinboxes = {}
        row = 0
        for metric, default_value in self.alert_thresholds.items():
            label = QLabel(metric.replace('_', ' ').title() + ":")
            spinbox = QSpinBox()
            spinbox.setRange(1, 100)
            spinbox.setValue(int(default_value))
            spinbox.setSuffix(" %" if 'percent' in metric else " ms" if 'ms' in metric else "")
            
            thresholds_layout.addWidget(label, row, 0)
            thresholds_layout.addWidget(spinbox, row, 1)
            self.threshold_spinboxes[metric] = spinbox
            row += 1
            
        thresholds_group.setLayout(thresholds_layout)
        layout.addWidget(thresholds_group)
        
        # Active alerts display
        alerts_group = QGroupBox("Active Alerts")
        alerts_layout = QVBoxLayout()
        
        self.alerts_text = QTextEdit()
        self.alerts_text.setMaximumHeight(150)
        self.alerts_text.setReadOnly(True)
        alerts_layout.addWidget(self.alerts_text)
        
        alerts_group.setLayout(alerts_layout)
        layout.addWidget(alerts_group)
        
        self.setLayout(layout)
        
    def check_alerts(self, metrics: PerformanceMetrics):
        """Check metrics against thresholds and generate alerts."""
        current_alerts = set()
        
        # Check CPU
        cpu_threshold = self.threshold_spinboxes['cpu_percent'].value()
        if metrics.cpu_percent > cpu_threshold:
            current_alerts.add(f"High CPU usage: {metrics.cpu_percent:.1f}% (threshold: {cpu_threshold}%)")
            
        # Check Memory
        memory_threshold = self.threshold_spinboxes['memory_percent'].value()
        if metrics.memory_percent > memory_threshold:
            current_alerts.add(f"High memory usage: {metrics.memory_percent:.1f}% (threshold: {memory_threshold}%)")
            
        # Check Disk
        disk_threshold = self.threshold_spinboxes['disk_percent'].value()
        if metrics.disk_usage_percent > disk_threshold:
            current_alerts.add(f"High disk usage: {metrics.disk_usage_percent:.1f}% (threshold: {disk_threshold}%)")
            
        # Check Latency
        latency_threshold = self.threshold_spinboxes['latency_ms'].value()
        if metrics.latency_ms > latency_threshold:
            current_alerts.add(f"High latency: {metrics.latency_ms:.1f}ms (threshold: {latency_threshold}ms)")
            
        # Update active alerts
        self.active_alerts = current_alerts
        self._update_alerts_display()
        
    def _update_alerts_display(self):
        """Update the alerts display."""
        if self.active_alerts:
            alert_text = "\n".join([f"⚠️ {alert}" for alert in self.active_alerts])
            self.alerts_text.setPlainText(alert_text)
            self.alerts_text.setStyleSheet("QTextEdit { background-color: #ffeeee; }")
        else:
            self.alerts_text.setPlainText("✅ No active alerts")
            self.alerts_text.setStyleSheet("QTextEdit { background-color: #eeffee; }")


class PerformanceMonitorWidget(QWidget):
    """Main performance monitoring dashboard widget."""
    
    def __init__(self):
        super().__init__()
        self.logger = get_logger(__name__)
        self.performance_collector = PerformanceCollector()
        self.metrics_history = deque(maxlen=1000)  # Store last 1000 metrics
        
        self._setup_ui()
        self._setup_connections()
        
        # Start performance collection
        self.performance_collector.start()
        
    def _setup_ui(self):
        """Setup the main UI."""
        layout = QVBoxLayout()
        
        # Control panel
        control_panel = self._create_control_panel()
        layout.addWidget(control_panel)
        
        # Main content in tabs
        self.tab_widget = QTabWidget()
        
        # Overview tab
        overview_tab = self._create_overview_tab()
        self.tab_widget.addTab(overview_tab, "Overview")
        
        # Charts tab
        charts_tab = self._create_charts_tab()
        self.tab_widget.addTab(charts_tab, "Charts")
        
        # Devices tab
        self.device_widget = DevicePerformanceWidget()
        self.tab_widget.addTab(self.device_widget, "Devices")
        
        # Alerts tab
        self.alerts_widget = PerformanceAlerts()
        self.tab_widget.addTab(self.alerts_widget, "Alerts")
        
        layout.addWidget(self.tab_widget)
        self.setLayout(layout)
        
    def _create_control_panel(self) -> QWidget:
        """Create the control panel."""
        panel = QFrame()
        panel.setFrameStyle(QFrame.StyledPanel)
        layout = QHBoxLayout()
        
        # Collection interval
        layout.addWidget(QLabel("Update Interval:"))
        self.interval_spinbox = QSpinBox()
        self.interval_spinbox.setRange(1, 60)
        self.interval_spinbox.setValue(1)
        self.interval_spinbox.setSuffix(" sec")
        layout.addWidget(self.interval_spinbox)
        
        layout.addStretch()
        
        # Export button
        export_btn = QPushButton("Export Data")
        export_btn.clicked.connect(self._export_performance_data)
        layout.addWidget(export_btn)
        
        # Clear history button
        clear_btn = QPushButton("Clear History")
        clear_btn.clicked.connect(self._clear_history)
        layout.addWidget(clear_btn)
        
        panel.setLayout(layout)
        return panel
        
    def _create_overview_tab(self) -> QWidget:
        """Create the overview tab."""
        widget = QWidget()
        layout = QGridLayout()
        
        # System metrics group
        system_group = QGroupBox("System Metrics")
        system_layout = QGridLayout()
        
        # CPU
        system_layout.addWidget(QLabel("CPU Usage:"), 0, 0)
        self.cpu_progress = QProgressBar()
        self.cpu_label = QLabel("0%")
        system_layout.addWidget(self.cpu_progress, 0, 1)
        system_layout.addWidget(self.cpu_label, 0, 2)
        
        # Memory
        system_layout.addWidget(QLabel("Memory Usage:"), 1, 0)
        self.memory_progress = QProgressBar()
        self.memory_label = QLabel("0 MB / 0 MB")
        system_layout.addWidget(self.memory_progress, 1, 1)
        system_layout.addWidget(self.memory_label, 1, 2)
        
        # Disk
        system_layout.addWidget(QLabel("Disk Usage:"), 2, 0)
        self.disk_progress = QProgressBar()
        self.disk_label = QLabel("0%")
        system_layout.addWidget(self.disk_progress, 2, 1)
        system_layout.addWidget(self.disk_label, 2, 2)
        
        system_group.setLayout(system_layout)
        layout.addWidget(system_group, 0, 0)
        
        # Network metrics group
        network_group = QGroupBox("Network Metrics")
        network_layout = QGridLayout()
        
        network_layout.addWidget(QLabel("Data Throughput:"), 0, 0)
        self.throughput_label = QLabel("0.00 MB/s")
        network_layout.addWidget(self.throughput_label, 0, 1)
        
        network_layout.addWidget(QLabel("Bytes Sent:"), 1, 0)
        self.bytes_sent_label = QLabel("0 MB")
        network_layout.addWidget(self.bytes_sent_label, 1, 1)
        
        network_layout.addWidget(QLabel("Bytes Received:"), 2, 0)
        self.bytes_recv_label = QLabel("0 MB")
        network_layout.addWidget(self.bytes_recv_label, 2, 1)
        
        network_group.setLayout(network_layout)
        layout.addWidget(network_group, 0, 1)
        
        # Application metrics group
        app_group = QGroupBox("Application Metrics")
        app_layout = QGridLayout()
        
        app_layout.addWidget(QLabel("Connected Devices:"), 0, 0)
        self.devices_label = QLabel("0")
        app_layout.addWidget(self.devices_label, 0, 1)
        
        app_layout.addWidget(QLabel("Active Recordings:"), 1, 0)
        self.recordings_label = QLabel("0")
        app_layout.addWidget(self.recordings_label, 1, 1)
        
        app_layout.addWidget(QLabel("Frame Rate:"), 2, 0)
        self.framerate_label = QLabel("0.0 fps")
        app_layout.addWidget(self.framerate_label, 2, 1)
        
        app_group.setLayout(app_layout)
        layout.addWidget(app_group, 1, 0)
        
        # Performance summary
        summary_group = QGroupBox("Performance Summary")
        summary_layout = QVBoxLayout()
        
        self.summary_text = QTextEdit()
        self.summary_text.setMaximumHeight(100)
        self.summary_text.setReadOnly(True)
        summary_layout.addWidget(self.summary_text)
        
        summary_group.setLayout(summary_layout)
        layout.addWidget(summary_group, 1, 1)
        
        widget.setLayout(layout)
        return widget
        
    def _create_charts_tab(self) -> QWidget:
        """Create the charts tab."""
        widget = QWidget()
        layout = QGridLayout()
        
        # Create performance charts
        self.cpu_chart = PerformanceChart("CPU Usage (%)")
        self.memory_chart = PerformanceChart("Memory Usage (%)")
        self.throughput_chart = PerformanceChart("Data Throughput (MB/s)")
        self.latency_chart = PerformanceChart("Latency (ms)")
        
        layout.addWidget(self.cpu_chart, 0, 0)
        layout.addWidget(self.memory_chart, 0, 1)
        layout.addWidget(self.throughput_chart, 1, 0)
        layout.addWidget(self.latency_chart, 1, 1)
        
        widget.setLayout(layout)
        return widget
        
    def _setup_connections(self):
        """Setup signal connections."""
        self.performance_collector.metrics_updated.connect(self._update_metrics)
        self.interval_spinbox.valueChanged.connect(self._update_collection_interval)
        
    def _update_collection_interval(self, interval: int):
        """Update the performance collection interval."""
        self.performance_collector.set_collection_interval(float(interval))
        
    def _update_metrics(self, metrics: PerformanceMetrics):
        """Update UI with new performance metrics."""
        # Store metrics in history
        self.metrics_history.append(metrics)
        
        # Update overview tab
        self._update_overview_metrics(metrics)
        
        # Update charts
        self._update_charts(metrics)
        
        # Check alerts
        self.alerts_widget.check_alerts(metrics)
        
    def _update_overview_metrics(self, metrics: PerformanceMetrics):
        """Update the overview tab metrics."""
        # CPU
        self.cpu_progress.setValue(int(metrics.cpu_percent))
        self.cpu_label.setText(f"{metrics.cpu_percent:.1f}%")
        
        # Memory
        self.memory_progress.setValue(int(metrics.memory_percent))
        self.memory_label.setText(f"{metrics.memory_used_mb:.0f} MB / {metrics.memory_total_mb:.0f} MB")
        
        # Disk
        self.disk_progress.setValue(int(metrics.disk_usage_percent))
        self.disk_label.setText(f"{metrics.disk_usage_percent:.1f}%")
        
        # Network
        self.throughput_label.setText(f"{metrics.data_throughput_mbps:.2f} MB/s")
        self.bytes_sent_label.setText(f"{metrics.network_bytes_sent / (1024*1024):.1f} MB")
        self.bytes_recv_label.setText(f"{metrics.network_bytes_recv / (1024*1024):.1f} MB")
        
        # Application
        self.devices_label.setText(str(metrics.device_count))
        self.recordings_label.setText(str(metrics.active_recordings))
        self.framerate_label.setText(f"{metrics.frame_rate:.1f} fps")
        
        # Performance summary
        self._update_performance_summary(metrics)
        
    def _update_performance_summary(self, metrics: PerformanceMetrics):
        """Update the performance summary text."""
        summary_lines = []
        
        # Overall system health
        if metrics.cpu_percent > 80 or metrics.memory_percent > 85:
            summary_lines.append("⚠️ System under high load")
        elif metrics.cpu_percent < 20 and metrics.memory_percent < 50:
            summary_lines.append("✅ System running efficiently")
        else:
            summary_lines.append("ℹ️ System running normally")
            
        # Data flow status
        if metrics.data_throughput_mbps > 10:
            summary_lines.append(f"📊 High data throughput: {metrics.data_throughput_mbps:.1f} MB/s")
        elif metrics.data_throughput_mbps > 1:
            summary_lines.append(f"📈 Moderate data flow: {metrics.data_throughput_mbps:.1f} MB/s")
        else:
            summary_lines.append("📉 Low data activity")
            
        # Recording status
        if metrics.active_recordings > 0:
            summary_lines.append(f"🔴 {metrics.active_recordings} active recording(s)")
        else:
            summary_lines.append("⏸️ No active recordings")
            
        self.summary_text.setPlainText("\n".join(summary_lines))
        
    def _update_charts(self, metrics: PerformanceMetrics):
        """Update the performance charts."""
        timestamp = metrics.timestamp
        
        self.cpu_chart.add_data_point(metrics.cpu_percent, timestamp)
        self.memory_chart.add_data_point(metrics.memory_percent, timestamp)
        self.throughput_chart.add_data_point(metrics.data_throughput_mbps, timestamp)
        self.latency_chart.add_data_point(metrics.latency_ms, timestamp)
        
    def _export_performance_data(self):
        """Export performance data to CSV file."""
        if not self.metrics_history:
            QMessageBox.information(self, "Export", "No performance data to export.")
            return
            
        filename, _ = QFileDialog.getSaveFileName(
            self, "Export Performance Data", 
            f"performance_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.csv",
            "CSV Files (*.csv)"
        )
        
        if filename:
            try:
                with open(filename, 'w') as f:
                    # Write header
                    f.write("timestamp,cpu_percent,memory_percent,memory_used_mb,memory_total_mb,"
                           "disk_usage_percent,network_bytes_sent,network_bytes_recv,"
                           "data_throughput_mbps,device_count,active_recordings,frame_rate,"
                           "dropped_frames,latency_ms\n")
                    
                    # Write data
                    for metrics in self.metrics_history:
                        f.write(f"{metrics.timestamp.isoformat()},"
                               f"{metrics.cpu_percent},"
                               f"{metrics.memory_percent},"
                               f"{metrics.memory_used_mb},"
                               f"{metrics.memory_total_mb},"
                               f"{metrics.disk_usage_percent},"
                               f"{metrics.network_bytes_sent},"
                               f"{metrics.network_bytes_recv},"
                               f"{metrics.data_throughput_mbps},"
                               f"{metrics.device_count},"
                               f"{metrics.active_recordings},"
                               f"{metrics.frame_rate},"
                               f"{metrics.dropped_frames},"
                               f"{metrics.latency_ms}\n")
                               
                QMessageBox.information(self, "Export", f"Performance data exported to {filename}")
                self.logger.info(f"Performance data exported to {filename}")
                
            except Exception as e:
                QMessageBox.critical(self, "Export Error", f"Failed to export data: {str(e)}")
                self.logger.error(f"Failed to export performance data: {e}")
                
    def _clear_history(self):
        """Clear performance history."""
        reply = QMessageBox.question(
            self, "Clear History", 
            "Are you sure you want to clear all performance history?",
            QMessageBox.Yes | QMessageBox.No
        )
        
        if reply == QMessageBox.Yes:
            self.metrics_history.clear()
            QMessageBox.information(self, "Clear History", "Performance history cleared.")
            self.logger.info("Performance history cleared")
            
    def closeEvent(self, event):
        """Handle widget close event."""
        self.performance_collector.stop()
        self.performance_collector.wait()
        event.accept()


# Convenience function for creating the performance monitor
def create_performance_monitor() -> PerformanceMonitorWidget:
    """Create and return a performance monitor widget."""
    return PerformanceMonitorWidget()