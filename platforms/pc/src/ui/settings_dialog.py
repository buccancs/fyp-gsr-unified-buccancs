"""
Settings Dialog for the GSR & Dual-Video Recording System PC Controller.

This dialog provides a comprehensive interface for configuring all aspects
of the system including network settings, hardware preferences, recording
parameters, and security options.
"""

import json
import os
from typing import Dict, Any, Optional

from PySide6.QtCore import Qt, Signal, QSettings
from PySide6.QtGui import QFont, QIcon
from PySide6.QtWidgets import (
    QDialog, QVBoxLayout, QHBoxLayout, QTabWidget, QWidget,
    QGroupBox, QFormLayout, QLineEdit, QSpinBox, QDoubleSpinBox,
    QCheckBox, QComboBox, QPushButton, QLabel, QTextEdit,
    QFileDialog, QMessageBox, QSlider, QProgressBar,
    QListWidget, QListWidgetItem, QSplitter, QFrame
)

from utils.logger import get_logger


class SettingsDialog(QDialog):
    """
    Comprehensive settings dialog for the GSR system.
    
    Features:
    - Network and connection settings
    - Hardware configuration
    - Recording parameters
    - Security settings
    - UI preferences
    - Data management
    - Advanced options
    """
    
    # Signal emitted when settings are applied
    settings_applied = Signal(dict)
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.logger = get_logger(__name__)
        self.settings = QSettings('BuccaNCS', 'GSR_Controller')
        
        self.setWindowTitle("GSR System Settings")
        self.setWindowIcon(QIcon(":/icons/settings.png"))
        self.setModal(True)
        self.resize(800, 600)
        
        # Initialize UI
        self.setup_ui()
        self.load_settings()
        
        # Connect signals
        self.connect_signals()
        
    def setup_ui(self):
        """Set up the user interface."""
        layout = QVBoxLayout(self)
        
        # Create tab widget
        self.tab_widget = QTabWidget()
        layout.addWidget(self.tab_widget)
        
        # Create tabs
        self.create_network_tab()
        self.create_hardware_tab()
        self.create_recording_tab()
        self.create_security_tab()
        self.create_ui_tab()
        self.create_data_tab()
        self.create_advanced_tab()
        
        # Button layout
        button_layout = QHBoxLayout()
        
        # Reset to defaults button
        self.reset_button = QPushButton("Reset to Defaults")
        self.reset_button.clicked.connect(self.reset_to_defaults)
        button_layout.addWidget(self.reset_button)
        
        button_layout.addStretch()
        
        # Standard dialog buttons
        self.cancel_button = QPushButton("Cancel")
        self.cancel_button.clicked.connect(self.reject)
        button_layout.addWidget(self.cancel_button)
        
        self.apply_button = QPushButton("Apply")
        self.apply_button.clicked.connect(self.apply_settings)
        button_layout.addWidget(self.apply_button)
        
        self.ok_button = QPushButton("OK")
        self.ok_button.clicked.connect(self.accept_settings)
        self.ok_button.setDefault(True)
        button_layout.addWidget(self.ok_button)
        
        layout.addLayout(button_layout)
        
    def create_network_tab(self):
        """Create the network settings tab."""
        tab = QWidget()
        layout = QVBoxLayout(tab)
        
        # Connection Settings
        conn_group = QGroupBox("Connection Settings")
        conn_layout = QFormLayout(conn_group)
        
        self.server_port = QSpinBox()
        self.server_port.setRange(1024, 65535)
        self.server_port.setValue(8080)
        conn_layout.addRow("Server Port:", self.server_port)
        
        self.max_connections = QSpinBox()
        self.max_connections.setRange(1, 50)
        self.max_connections.setValue(10)
        conn_layout.addRow("Max Connections:", self.max_connections)
        
        self.connection_timeout = QSpinBox()
        self.connection_timeout.setRange(5, 300)
        self.connection_timeout.setValue(30)
        self.connection_timeout.setSuffix(" seconds")
        conn_layout.addRow("Connection Timeout:", self.connection_timeout)
        
        self.auto_discovery = QCheckBox("Enable automatic device discovery")
        self.auto_discovery.setChecked(True)
        conn_layout.addRow(self.auto_discovery)
        
        layout.addWidget(conn_group)
        
        # Network Interface Settings
        interface_group = QGroupBox("Network Interface")
        interface_layout = QFormLayout(interface_group)
        
        self.bind_address = QLineEdit("0.0.0.0")
        interface_layout.addRow("Bind Address:", self.bind_address)
        
        self.network_interface = QComboBox()
        self.network_interface.addItems(["Auto", "Ethernet", "WiFi", "All Interfaces"])
        interface_layout.addRow("Preferred Interface:", self.network_interface)
        
        layout.addWidget(interface_group)
        
        # Quality of Service
        qos_group = QGroupBox("Quality of Service")
        qos_layout = QFormLayout(qos_group)
        
        self.enable_qos = QCheckBox("Enable QoS prioritization")
        qos_layout.addRow(self.enable_qos)
        
        self.bandwidth_limit = QSpinBox()
        self.bandwidth_limit.setRange(1, 1000)
        self.bandwidth_limit.setValue(100)
        self.bandwidth_limit.setSuffix(" Mbps")
        qos_layout.addRow("Bandwidth Limit:", self.bandwidth_limit)
        
        layout.addWidget(qos_group)
        
        layout.addStretch()
        self.tab_widget.addTab(tab, "Network")
        
    def create_hardware_tab(self):
        """Create the hardware settings tab."""
        tab = QWidget()
        layout = QVBoxLayout(tab)
        
        # Sensor Settings
        sensor_group = QGroupBox("Sensor Configuration")
        sensor_layout = QFormLayout(sensor_group)
        
        self.default_sampling_rate = QDoubleSpinBox()
        self.default_sampling_rate.setRange(1.0, 1000.0)
        self.default_sampling_rate.setValue(128.0)
        self.default_sampling_rate.setSuffix(" Hz")
        sensor_layout.addRow("Default Sampling Rate:", self.default_sampling_rate)
        
        self.sensor_timeout = QSpinBox()
        self.sensor_timeout.setRange(1, 60)
        self.sensor_timeout.setValue(10)
        self.sensor_timeout.setSuffix(" seconds")
        sensor_layout.addRow("Sensor Timeout:", self.sensor_timeout)
        
        self.auto_calibration = QCheckBox("Enable automatic sensor calibration")
        self.auto_calibration.setChecked(True)
        sensor_layout.addRow(self.auto_calibration)
        
        layout.addWidget(sensor_group)
        
        # Camera Settings
        camera_group = QGroupBox("Camera Configuration")
        camera_layout = QFormLayout(camera_group)
        
        self.default_resolution = QComboBox()
        self.default_resolution.addItems([
            "640x480", "800x600", "1024x768", "1280x720", 
            "1920x1080", "2560x1440", "3840x2160"
        ])
        self.default_resolution.setCurrentText("1280x720")
        camera_layout.addRow("Default Resolution:", self.default_resolution)
        
        self.default_framerate = QSpinBox()
        self.default_framerate.setRange(1, 120)
        self.default_framerate.setValue(30)
        self.default_framerate.setSuffix(" FPS")
        camera_layout.addRow("Default Frame Rate:", self.default_framerate)
        
        self.camera_format = QComboBox()
        self.camera_format.addItems(["MJPEG", "H.264", "H.265", "RAW"])
        camera_layout.addRow("Video Format:", self.camera_format)
        
        layout.addWidget(camera_group)
        
        # Hardware Detection
        detection_group = QGroupBox("Hardware Detection")
        detection_layout = QFormLayout(detection_group)
        
        self.auto_detect_hardware = QCheckBox("Automatically detect connected hardware")
        self.auto_detect_hardware.setChecked(True)
        detection_layout.addRow(self.auto_detect_hardware)
        
        self.detection_interval = QSpinBox()
        self.detection_interval.setRange(1, 60)
        self.detection_interval.setValue(5)
        self.detection_interval.setSuffix(" seconds")
        detection_layout.addRow("Detection Interval:", self.detection_interval)
        
        layout.addWidget(detection_group)
        
        layout.addStretch()
        self.tab_widget.addTab(tab, "Hardware")
        
    def create_recording_tab(self):
        """Create the recording settings tab."""
        tab = QWidget()
        layout = QVBoxLayout(tab)
        
        # Recording Paths
        paths_group = QGroupBox("Recording Paths")
        paths_layout = QFormLayout(paths_group)
        
        # Output directory
        output_layout = QHBoxLayout()
        self.output_directory = QLineEdit()
        self.output_directory.setText(os.path.expanduser("~/GSR_Recordings"))
        output_layout.addWidget(self.output_directory)
        
        browse_output_btn = QPushButton("Browse...")
        browse_output_btn.clicked.connect(self.browse_output_directory)
        output_layout.addWidget(browse_output_btn)
        
        paths_layout.addRow("Output Directory:", output_layout)
        
        # Temporary directory
        temp_layout = QHBoxLayout()
        self.temp_directory = QLineEdit()
        self.temp_directory.setText(os.path.expanduser("~/GSR_Temp"))
        temp_layout.addWidget(self.temp_directory)
        
        browse_temp_btn = QPushButton("Browse...")
        browse_temp_btn.clicked.connect(self.browse_temp_directory)
        temp_layout.addWidget(browse_temp_btn)
        
        paths_layout.addRow("Temporary Directory:", temp_layout)
        
        layout.addWidget(paths_group)
        
        # Recording Options
        options_group = QGroupBox("Recording Options")
        options_layout = QFormLayout(options_group)
        
        self.auto_start_recording = QCheckBox("Auto-start recording when devices connect")
        options_layout.addRow(self.auto_start_recording)
        
        self.auto_stop_recording = QCheckBox("Auto-stop recording on device disconnect")
        options_layout.addRow(self.auto_stop_recording)
        
        self.max_recording_duration = QSpinBox()
        self.max_recording_duration.setRange(1, 1440)
        self.max_recording_duration.setValue(60)
        self.max_recording_duration.setSuffix(" minutes")
        options_layout.addRow("Max Recording Duration:", self.max_recording_duration)
        
        self.split_recordings = QCheckBox("Split recordings into segments")
        options_layout.addRow(self.split_recordings)
        
        self.segment_duration = QSpinBox()
        self.segment_duration.setRange(1, 60)
        self.segment_duration.setValue(10)
        self.segment_duration.setSuffix(" minutes")
        options_layout.addRow("Segment Duration:", self.segment_duration)
        
        layout.addWidget(options_group)
        
        # File Management
        file_group = QGroupBox("File Management")
        file_layout = QFormLayout(file_group)
        
        self.auto_cleanup = QCheckBox("Automatically clean up old recordings")
        file_layout.addRow(self.auto_cleanup)
        
        self.cleanup_days = QSpinBox()
        self.cleanup_days.setRange(1, 365)
        self.cleanup_days.setValue(30)
        self.cleanup_days.setSuffix(" days")
        file_layout.addRow("Keep recordings for:", self.cleanup_days)
        
        self.compress_recordings = QCheckBox("Compress recordings after completion")
        file_layout.addRow(self.compress_recordings)
        
        layout.addWidget(file_group)
        
        layout.addStretch()
        self.tab_widget.addTab(tab, "Recording")
        
    def create_security_tab(self):
        """Create the security settings tab."""
        tab = QWidget()
        layout = QVBoxLayout(tab)
        
        # Authentication
        auth_group = QGroupBox("Authentication")
        auth_layout = QFormLayout(auth_group)
        
        self.enable_authentication = QCheckBox("Enable device authentication")
        self.enable_authentication.setChecked(True)
        auth_layout.addRow(self.enable_authentication)
        
        self.auth_timeout = QSpinBox()
        self.auth_timeout.setRange(10, 300)
        self.auth_timeout.setValue(30)
        self.auth_timeout.setSuffix(" seconds")
        auth_layout.addRow("Authentication Timeout:", self.auth_timeout)
        
        self.max_auth_attempts = QSpinBox()
        self.max_auth_attempts.setRange(1, 10)
        self.max_auth_attempts.setValue(3)
        auth_layout.addRow("Max Authentication Attempts:", self.max_auth_attempts)
        
        layout.addWidget(auth_group)
        
        # Encryption
        encryption_group = QGroupBox("Encryption")
        encryption_layout = QFormLayout(encryption_group)
        
        self.enable_encryption = QCheckBox("Enable message encryption")
        self.enable_encryption.setChecked(True)
        encryption_layout.addRow(self.enable_encryption)
        
        self.encryption_algorithm = QComboBox()
        self.encryption_algorithm.addItems(["AES-256", "AES-128", "ChaCha20"])
        encryption_layout.addRow("Encryption Algorithm:", self.encryption_algorithm)
        
        self.key_rotation_interval = QSpinBox()
        self.key_rotation_interval.setRange(1, 24)
        self.key_rotation_interval.setValue(1)
        self.key_rotation_interval.setSuffix(" hours")
        encryption_layout.addRow("Key Rotation Interval:", self.key_rotation_interval)
        
        layout.addWidget(encryption_group)
        
        # Access Control
        access_group = QGroupBox("Access Control")
        access_layout = QFormLayout(access_group)
        
        self.whitelist_enabled = QCheckBox("Enable device whitelist")
        access_layout.addRow(self.whitelist_enabled)
        
        self.allowed_devices = QTextEdit()
        self.allowed_devices.setMaximumHeight(100)
        self.allowed_devices.setPlaceholderText("Enter device IDs, one per line")
        access_layout.addRow("Allowed Devices:", self.allowed_devices)
        
        layout.addWidget(access_group)
        
        # Security Logging
        logging_group = QGroupBox("Security Logging")
        logging_layout = QFormLayout(logging_group)
        
        self.log_security_events = QCheckBox("Log security events")
        self.log_security_events.setChecked(True)
        logging_layout.addRow(self.log_security_events)
        
        self.log_failed_attempts = QCheckBox("Log failed authentication attempts")
        self.log_failed_attempts.setChecked(True)
        logging_layout.addRow(self.log_failed_attempts)
        
        layout.addWidget(logging_group)
        
        layout.addStretch()
        self.tab_widget.addTab(tab, "Security")
        
    def create_ui_tab(self):
        """Create the UI preferences tab."""
        tab = QWidget()
        layout = QVBoxLayout(tab)
        
        # Appearance
        appearance_group = QGroupBox("Appearance")
        appearance_layout = QFormLayout(appearance_group)
        
        self.theme = QComboBox()
        self.theme.addItems(["System", "Light", "Dark", "High Contrast"])
        appearance_layout.addRow("Theme:", self.theme)
        
        self.font_size = QSpinBox()
        self.font_size.setRange(8, 24)
        self.font_size.setValue(10)
        self.font_size.setSuffix(" pt")
        appearance_layout.addRow("Font Size:", self.font_size)
        
        self.show_tooltips = QCheckBox("Show tooltips")
        self.show_tooltips.setChecked(True)
        appearance_layout.addRow(self.show_tooltips)
        
        layout.addWidget(appearance_group)
        
        # Dashboard
        dashboard_group = QGroupBox("Dashboard")
        dashboard_layout = QFormLayout(dashboard_group)
        
        self.refresh_interval = QSpinBox()
        self.refresh_interval.setRange(100, 5000)
        self.refresh_interval.setValue(1000)
        self.refresh_interval.setSuffix(" ms")
        dashboard_layout.addRow("Refresh Interval:", self.refresh_interval)
        
        self.show_performance_metrics = QCheckBox("Show performance metrics")
        self.show_performance_metrics.setChecked(True)
        dashboard_layout.addRow(self.show_performance_metrics)
        
        self.animate_charts = QCheckBox("Animate charts and graphs")
        self.animate_charts.setChecked(True)
        dashboard_layout.addRow(self.animate_charts)
        
        layout.addWidget(dashboard_group)
        
        # Notifications
        notifications_group = QGroupBox("Notifications")
        notifications_layout = QFormLayout(notifications_group)
        
        self.enable_notifications = QCheckBox("Enable system notifications")
        self.enable_notifications.setChecked(True)
        notifications_layout.addRow(self.enable_notifications)
        
        self.notification_sound = QCheckBox("Play notification sounds")
        self.notification_sound.setChecked(True)
        notifications_layout.addRow(self.notification_sound)
        
        self.show_connection_notifications = QCheckBox("Show device connection notifications")
        self.show_connection_notifications.setChecked(True)
        notifications_layout.addRow(self.show_connection_notifications)
        
        layout.addWidget(notifications_group)
        
        layout.addStretch()
        self.tab_widget.addTab(tab, "Interface")
        
    def create_data_tab(self):
        """Create the data management tab."""
        tab = QWidget()
        layout = QVBoxLayout(tab)
        
        # Export Settings
        export_group = QGroupBox("Data Export")
        export_layout = QFormLayout(export_group)
        
        self.default_export_format = QComboBox()
        self.default_export_format.addItems(["CSV", "JSON", "MATLAB", "HDF5", "Excel"])
        export_layout.addRow("Default Export Format:", self.default_export_format)
        
        self.include_metadata = QCheckBox("Include metadata in exports")
        self.include_metadata.setChecked(True)
        export_layout.addRow(self.include_metadata)
        
        self.compress_exports = QCheckBox("Compress exported files")
        self.compress_exports.setChecked(True)
        export_layout.addRow(self.compress_exports)
        
        layout.addWidget(export_group)
        
        # Data Processing
        processing_group = QGroupBox("Data Processing")
        processing_layout = QFormLayout(processing_group)
        
        self.auto_process_data = QCheckBox("Automatically process data after recording")
        processing_layout.addRow(self.auto_process_data)
        
        self.apply_filters = QCheckBox("Apply default filters to sensor data")
        processing_layout.addRow(self.apply_filters)
        
        self.filter_cutoff = QDoubleSpinBox()
        self.filter_cutoff.setRange(0.1, 100.0)
        self.filter_cutoff.setValue(10.0)
        self.filter_cutoff.setSuffix(" Hz")
        processing_layout.addRow("Low-pass Filter Cutoff:", self.filter_cutoff)
        
        layout.addWidget(processing_group)
        
        # Backup Settings
        backup_group = QGroupBox("Backup")
        backup_layout = QFormLayout(backup_group)
        
        self.enable_backup = QCheckBox("Enable automatic backup")
        backup_layout.addRow(self.enable_backup)
        
        # Backup directory
        backup_dir_layout = QHBoxLayout()
        self.backup_directory = QLineEdit()
        self.backup_directory.setText(os.path.expanduser("~/GSR_Backup"))
        backup_dir_layout.addWidget(self.backup_directory)
        
        browse_backup_btn = QPushButton("Browse...")
        browse_backup_btn.clicked.connect(self.browse_backup_directory)
        backup_dir_layout.addWidget(browse_backup_btn)
        
        backup_layout.addRow("Backup Directory:", backup_dir_layout)
        
        self.backup_interval = QComboBox()
        self.backup_interval.addItems(["Daily", "Weekly", "Monthly"])
        backup_layout.addRow("Backup Frequency:", self.backup_interval)
        
        layout.addWidget(backup_group)
        
        layout.addStretch()
        self.tab_widget.addTab(tab, "Data")
        
    def create_advanced_tab(self):
        """Create the advanced settings tab."""
        tab = QWidget()
        layout = QVBoxLayout(tab)
        
        # Performance
        performance_group = QGroupBox("Performance")
        performance_layout = QFormLayout(performance_group)
        
        self.thread_pool_size = QSpinBox()
        self.thread_pool_size.setRange(1, 32)
        self.thread_pool_size.setValue(4)
        performance_layout.addRow("Thread Pool Size:", self.thread_pool_size)
        
        self.buffer_size = QSpinBox()
        self.buffer_size.setRange(1, 1000)
        self.buffer_size.setValue(100)
        self.buffer_size.setSuffix(" MB")
        performance_layout.addRow("Buffer Size:", self.buffer_size)
        
        self.enable_gpu_acceleration = QCheckBox("Enable GPU acceleration (if available)")
        performance_layout.addRow(self.enable_gpu_acceleration)
        
        layout.addWidget(performance_group)
        
        # Logging
        logging_group = QGroupBox("Logging")
        logging_layout = QFormLayout(logging_group)
        
        self.log_level = QComboBox()
        self.log_level.addItems(["DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"])
        self.log_level.setCurrentText("INFO")
        logging_layout.addRow("Log Level:", self.log_level)
        
        self.max_log_size = QSpinBox()
        self.max_log_size.setRange(1, 1000)
        self.max_log_size.setValue(10)
        self.max_log_size.setSuffix(" MB")
        logging_layout.addRow("Max Log File Size:", self.max_log_size)
        
        self.log_rotation_count = QSpinBox()
        self.log_rotation_count.setRange(1, 50)
        self.log_rotation_count.setValue(5)
        logging_layout.addRow("Log Rotation Count:", self.log_rotation_count)
        
        layout.addWidget(logging_group)
        
        # Development
        dev_group = QGroupBox("Development")
        dev_layout = QFormLayout(dev_group)
        
        self.debug_mode = QCheckBox("Enable debug mode")
        dev_layout.addRow(self.debug_mode)
        
        self.enable_profiling = QCheckBox("Enable performance profiling")
        dev_layout.addRow(self.enable_profiling)
        
        self.mock_devices = QCheckBox("Use mock devices for testing")
        dev_layout.addRow(self.mock_devices)
        
        layout.addWidget(dev_group)
        
        layout.addStretch()
        self.tab_widget.addTab(tab, "Advanced")
        
    def connect_signals(self):
        """Connect widget signals to handlers."""
        # Enable/disable dependent controls
        self.enable_authentication.toggled.connect(
            lambda checked: self.toggle_auth_controls(checked)
        )
        self.enable_encryption.toggled.connect(
            lambda checked: self.toggle_encryption_controls(checked)
        )
        self.whitelist_enabled.toggled.connect(
            lambda checked: self.allowed_devices.setEnabled(checked)
        )
        self.split_recordings.toggled.connect(
            lambda checked: self.segment_duration.setEnabled(checked)
        )
        self.auto_cleanup.toggled.connect(
            lambda checked: self.cleanup_days.setEnabled(checked)
        )
        self.enable_backup.toggled.connect(
            lambda checked: self.toggle_backup_controls(checked)
        )
        
    def toggle_auth_controls(self, enabled: bool):
        """Enable/disable authentication controls."""
        self.auth_timeout.setEnabled(enabled)
        self.max_auth_attempts.setEnabled(enabled)
        
    def toggle_encryption_controls(self, enabled: bool):
        """Enable/disable encryption controls."""
        self.encryption_algorithm.setEnabled(enabled)
        self.key_rotation_interval.setEnabled(enabled)
        
    def toggle_backup_controls(self, enabled: bool):
        """Enable/disable backup controls."""
        self.backup_directory.setEnabled(enabled)
        self.backup_interval.setEnabled(enabled)
        
    def browse_output_directory(self):
        """Browse for output directory."""
        directory = QFileDialog.getExistingDirectory(
            self, "Select Output Directory", self.output_directory.text()
        )
        if directory:
            self.output_directory.setText(directory)
            
    def browse_temp_directory(self):
        """Browse for temporary directory."""
        directory = QFileDialog.getExistingDirectory(
            self, "Select Temporary Directory", self.temp_directory.text()
        )
        if directory:
            self.temp_directory.setText(directory)
            
    def browse_backup_directory(self):
        """Browse for backup directory."""
        directory = QFileDialog.getExistingDirectory(
            self, "Select Backup Directory", self.backup_directory.text()
        )
        if directory:
            self.backup_directory.setText(directory)
            
    def load_settings(self):
        """Load settings from QSettings."""
        try:
            # Network settings
            self.server_port.setValue(self.settings.value("network/server_port", 8080, int))
            self.max_connections.setValue(self.settings.value("network/max_connections", 10, int))
            self.connection_timeout.setValue(self.settings.value("network/connection_timeout", 30, int))
            self.auto_discovery.setChecked(self.settings.value("network/auto_discovery", True, bool))
            self.bind_address.setText(self.settings.value("network/bind_address", "0.0.0.0", str))
            
            # Hardware settings
            self.default_sampling_rate.setValue(self.settings.value("hardware/sampling_rate", 128.0, float))
            self.sensor_timeout.setValue(self.settings.value("hardware/sensor_timeout", 10, int))
            self.auto_calibration.setChecked(self.settings.value("hardware/auto_calibration", True, bool))
            self.default_resolution.setCurrentText(self.settings.value("hardware/resolution", "1280x720", str))
            self.default_framerate.setValue(self.settings.value("hardware/framerate", 30, int))
            
            # Recording settings
            self.output_directory.setText(self.settings.value("recording/output_dir", 
                                                            os.path.expanduser("~/GSR_Recordings"), str))
            self.temp_directory.setText(self.settings.value("recording/temp_dir", 
                                                           os.path.expanduser("~/GSR_Temp"), str))
            self.max_recording_duration.setValue(self.settings.value("recording/max_duration", 60, int))
            
            # Security settings
            self.enable_authentication.setChecked(self.settings.value("security/enable_auth", True, bool))
            self.enable_encryption.setChecked(self.settings.value("security/enable_encryption", True, bool))
            self.auth_timeout.setValue(self.settings.value("security/auth_timeout", 30, int))
            
            # UI settings
            self.theme.setCurrentText(self.settings.value("ui/theme", "System", str))
            self.font_size.setValue(self.settings.value("ui/font_size", 10, int))
            self.refresh_interval.setValue(self.settings.value("ui/refresh_interval", 1000, int))
            
            # Advanced settings
            self.log_level.setCurrentText(self.settings.value("advanced/log_level", "INFO", str))
            self.thread_pool_size.setValue(self.settings.value("advanced/thread_pool_size", 4, int))
            
            self.logger.info("Settings loaded successfully")
            
        except Exception as e:
            self.logger.error(f"Error loading settings: {e}")
            QMessageBox.warning(self, "Settings Error", 
                              f"Error loading settings: {e}\nUsing default values.")
            
    def save_settings(self):
        """Save settings to QSettings."""
        try:
            # Network settings
            self.settings.setValue("network/server_port", self.server_port.value())
            self.settings.setValue("network/max_connections", self.max_connections.value())
            self.settings.setValue("network/connection_timeout", self.connection_timeout.value())
            self.settings.setValue("network/auto_discovery", self.auto_discovery.isChecked())
            self.settings.setValue("network/bind_address", self.bind_address.text())
            
            # Hardware settings
            self.settings.setValue("hardware/sampling_rate", self.default_sampling_rate.value())
            self.settings.setValue("hardware/sensor_timeout", self.sensor_timeout.value())
            self.settings.setValue("hardware/auto_calibration", self.auto_calibration.isChecked())
            self.settings.setValue("hardware/resolution", self.default_resolution.currentText())
            self.settings.setValue("hardware/framerate", self.default_framerate.value())
            
            # Recording settings
            self.settings.setValue("recording/output_dir", self.output_directory.text())
            self.settings.setValue("recording/temp_dir", self.temp_directory.text())
            self.settings.setValue("recording/max_duration", self.max_recording_duration.value())
            
            # Security settings
            self.settings.setValue("security/enable_auth", self.enable_authentication.isChecked())
            self.settings.setValue("security/enable_encryption", self.enable_encryption.isChecked())
            self.settings.setValue("security/auth_timeout", self.auth_timeout.value())
            
            # UI settings
            self.settings.setValue("ui/theme", self.theme.currentText())
            self.settings.setValue("ui/font_size", self.font_size.value())
            self.settings.setValue("ui/refresh_interval", self.refresh_interval.value())
            
            # Advanced settings
            self.settings.setValue("advanced/log_level", self.log_level.currentText())
            self.settings.setValue("advanced/thread_pool_size", self.thread_pool_size.value())
            
            self.settings.sync()
            self.logger.info("Settings saved successfully")
            
        except Exception as e:
            self.logger.error(f"Error saving settings: {e}")
            QMessageBox.critical(self, "Settings Error", 
                               f"Error saving settings: {e}")
            
    def get_settings_dict(self) -> Dict[str, Any]:
        """Get all settings as a dictionary."""
        return {
            "network": {
                "server_port": self.server_port.value(),
                "max_connections": self.max_connections.value(),
                "connection_timeout": self.connection_timeout.value(),
                "auto_discovery": self.auto_discovery.isChecked(),
                "bind_address": self.bind_address.text(),
            },
            "hardware": {
                "sampling_rate": self.default_sampling_rate.value(),
                "sensor_timeout": self.sensor_timeout.value(),
                "auto_calibration": self.auto_calibration.isChecked(),
                "resolution": self.default_resolution.currentText(),
                "framerate": self.default_framerate.value(),
            },
            "recording": {
                "output_dir": self.output_directory.text(),
                "temp_dir": self.temp_directory.text(),
                "max_duration": self.max_recording_duration.value(),
            },
            "security": {
                "enable_auth": self.enable_authentication.isChecked(),
                "enable_encryption": self.enable_encryption.isChecked(),
                "auth_timeout": self.auth_timeout.value(),
            },
            "ui": {
                "theme": self.theme.currentText(),
                "font_size": self.font_size.value(),
                "refresh_interval": self.refresh_interval.value(),
            },
            "advanced": {
                "log_level": self.log_level.currentText(),
                "thread_pool_size": self.thread_pool_size.value(),
            }
        }
        
    def reset_to_defaults(self):
        """Reset all settings to default values."""
        reply = QMessageBox.question(
            self, "Reset Settings", 
            "Are you sure you want to reset all settings to their default values?",
            QMessageBox.Yes | QMessageBox.No,
            QMessageBox.No
        )
        
        if reply == QMessageBox.Yes:
            # Clear all settings
            self.settings.clear()
            
            # Reload default values
            self.load_settings()
            
            self.logger.info("Settings reset to defaults")
            QMessageBox.information(self, "Settings Reset", 
                                  "All settings have been reset to their default values.")
            
    def apply_settings(self):
        """Apply settings without closing the dialog."""
        self.save_settings()
        settings_dict = self.get_settings_dict()
        self.settings_applied.emit(settings_dict)
        
        QMessageBox.information(self, "Settings Applied", 
                              "Settings have been applied successfully.")
        
    def accept_settings(self):
        """Apply settings and close the dialog."""
        self.apply_settings()
        self.accept()
        
    def validate_settings(self) -> bool:
        """Validate current settings."""
        # Check if directories exist or can be created
        directories = [
            self.output_directory.text(),
            self.temp_directory.text(),
            self.backup_directory.text() if self.enable_backup.isChecked() else None
        ]
        
        for directory in directories:
            if directory and not os.path.exists(directory):
                try:
                    os.makedirs(directory, exist_ok=True)
                except OSError as e:
                    QMessageBox.warning(
                        self, "Invalid Directory", 
                        f"Cannot create directory: {directory}\nError: {e}"
                    )
                    return False
                    
        # Validate port range
        if not (1024 <= self.server_port.value() <= 65535):
            QMessageBox.warning(
                self, "Invalid Port", 
                "Server port must be between 1024 and 65535."
            )
            return False
            
        return True
        
    def closeEvent(self, event):
        """Handle dialog close event."""
        if self.validate_settings():
            event.accept()
        else:
            event.ignore()