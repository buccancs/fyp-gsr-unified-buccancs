#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Main window for the PC Controller App.
Cross-platform support: Windows, macOS, Linux.
"""

import logging
import os
import sys
import time

from PySide6.QtCore import Qt, QTimer, Signal, Slot
from PySide6.QtGui import QAction, QIcon, QPixmap
from PySide6.QtWidgets import (QDialog, QFileDialog, QGridLayout, QGroupBox,
                               QHBoxLayout, QLabel, QMainWindow, QMenu,
                               QMessageBox, QPushButton, QSplitter, QStatusBar,
                               QTabWidget, QVBoxLayout, QWidget)

# Import our modules
from network.device_manager import DeviceManager
from ui.calibration_dialog import CalibrationDialog
from ui.device_panel import DevicePanel
from ui.live_calibration_dialog import LiveCalibrationDialog
from ui.log_viewer import LogViewer
from ui.real_time_plot_widget import RealTimePlotWidget
from ui.settings_dialog import SettingsDialog
from ui.status_dashboard import StatusDashboard
from ui.video_playback_window import VideoPlaybackWindow
from ui.video_preview import VideoPreview
from utils.logger import get_logger
from utils.session_manager import SessionManager


class MainWindow(QMainWindow):
    """Main window class for the PC Controller App.
    Cross-platform support: Windows, macOS, Linux.
    """

    def __init__(self) -> None:
        """Initialize the main window.
        """
        super().__init__()

        # Set up logging
        self.logger = get_logger(__name__)
        self.logger.info("Initializing main window")

        # Initialize components
        self.device_manager = DeviceManager()
        self.session_manager = SessionManager()

        # Set up modern UI
        self.setWindowTitle(
            "GSR & Dual-Video Recording System - Modern Interface")
        self.setMinimumSize(1400, 900)

        # Apply modern styling
        self.setStyleSheet("""QMainWindow {
                background-color: #f5f5f5;
            }
            QGroupBox {
                font-weight: bold;
                border: 2px solid #cccccc;
                border-radius: 8px;
                margin-top: 1ex;
                padding-top: 10px;
                background-color: white;
            }
            QGroupBox::title {
                subcontrol-origin: margin;
                left: 10px;
                padding: 0 5px 0 5px;
                color: #2c3e50;
            }
            QPushButton {
                background-color: #3498db;
                border: none;
                color: white;
                padding: 8px 16px;
                border-radius: 4px;
                font-weight: bold;
                min-width: 80px;
            }
            QPushButton:hover {
                background-color: #2980b9;
            }
            QPushButton:pressed {
                background-color: #21618c;
            }
            QPushButton:disabled {
                background-color: #bdc3c7;
            }
            QPushButton#recordButton {
                background-color: #e74c3c;
            }
            QPushButton#recordButton:hover {
                background-color: #c0392b;
            }
            QPushButton#stopButton {
                background-color: #95a5a6;
            }
            QPushButton#stopButton:hover {
                background-color: #7f8c8d;
            }
            QLabel {
                color: #2c3e50;
            }
            QStatusBar {
                background-color: #34495e;
                color: white;
                border-top: 1px solid #2c3e50;
            }
        """)

        # Create central widget and main layout
        self.central_widget = QWidget()
        self.setCentralWidget(self.central_widget)
        self.main_layout = QVBoxLayout(self.central_widget)

        # Create menu bar
        self.create_menu_bar()

        # Create main UI components
        self.create_control_panel()
        self.create_device_panels()
        self.create_status_bar()

        # Connect signals and slots
        self.connect_signals()

        # Initialize state
        self.recording = False
        self.update_ui_state()

        self.logger.info("Main window initialized")

    def create_menu_bar(self) -> None:
        """Create the menu bar for the application.
        """
        # File menu
        file_menu = self.menuBar().addMenu("&File")

        new_session_action = QAction("&New Session", self)
        new_session_action.setShortcut("Ctrl+N")
        new_session_action.triggered.connect(self.on_new_session)
        file_menu.addAction(new_session_action)

        open_session_action = QAction("&Open Session", self)
        open_session_action.setShortcut("Ctrl+O")
        open_session_action.triggered.connect(self.on_open_session)
        file_menu.addAction(open_session_action)

        file_menu.addSeparator()

        exit_action = QAction("E&xit", self)
        exit_action.setShortcut("Ctrl+Q")
        exit_action.triggered.connect(self.close)
        file_menu.addAction(exit_action)

        # Devices menu
        devices_menu = self.menuBar().addMenu("&Devices")

        discover_action = QAction("&Discover Devices", self)
        discover_action.triggered.connect(self.on_discover_devices)
        devices_menu.addAction(discover_action)

        discover_usb_action = QAction("Discover &USB Devices", self)
        discover_usb_action.triggered.connect(self.on_discover_usb_devices)
        devices_menu.addAction(discover_usb_action)

        devices_menu.addSeparator()

        connect_all_action = QAction("Connect &All", self)
        connect_all_action.triggered.connect(self.on_connect_all)
        devices_menu.addAction(connect_all_action)

        disconnect_all_action = QAction("&Disconnect All", self)
        disconnect_all_action.triggered.connect(self.on_disconnect_all)
        devices_menu.addAction(disconnect_all_action)

        # Tools menu
        tools_menu = self.menuBar().addMenu("&Tools")

        # Camera calibration action
        calibration_action = QAction("📷 &Camera Calibration", self)
        calibration_action.setShortcut("Ctrl+Shift+C")
        calibration_action.triggered.connect(self.on_camera_calibration)
        tools_menu.addAction(calibration_action)

        # Live calibration action
        live_calibration_action = QAction("🎥 &Live Calibration", self)
        live_calibration_action.setShortcut("Ctrl+Shift+L")
        live_calibration_action.triggered.connect(self.on_live_calibration)
        tools_menu.addAction(live_calibration_action)

        tools_menu.addSeparator()

        settings_action = QAction("&Settings", self)
        settings_action.triggered.connect(self.on_settings)
        tools_menu.addAction(settings_action)

        # Help menu
        help_menu = self.menuBar().addMenu("&Help")

        about_action = QAction("&About", self)
        about_action.triggered.connect(self.on_about)
        help_menu.addAction(about_action)

    def create_control_panel(self) -> None:
        """Create the control panel for the application.
        """
        control_group = QGroupBox("Recording Control")
        control_layout = QHBoxLayout()

        # Session info
        session_layout = QVBoxLayout()
        self.session_label = QLabel("Session: Not started")
        session_layout.addWidget(self.session_label)

        self.duration_label = QLabel("Duration: 00:00:00")
        session_layout.addWidget(self.duration_label)

        control_layout.addLayout(session_layout)

        # Control buttons
        button_layout = QHBoxLayout()

        self.start_button = QPushButton("🔴 Start Recording")
        self.start_button.setObjectName("recordButton")
        self.start_button.setMinimumSize(140, 45)
        self.start_button.clicked.connect(self.on_start_recording)
        button_layout.addWidget(self.start_button)

        self.stop_button = QPushButton("⏹️ Stop Recording")
        self.stop_button.setObjectName("stopButton")
        self.stop_button.setMinimumSize(140, 45)
        self.stop_button.setEnabled(False)
        self.stop_button.clicked.connect(self.on_stop_recording)
        button_layout.addWidget(self.stop_button)

        control_layout.addLayout(button_layout)
        control_layout.addStretch(1)

        control_group.setLayout(control_layout)
        self.main_layout.addWidget(control_group)

    def create_device_panels(self) -> None:
        """Create the device panels for the application.
        """
        # Create a splitter for the main content area
        self.content_splitter = QSplitter(Qt.Vertical)

        # Create tabs for different views
        self.tabs = QTabWidget()

        # Devices tab
        self.devices_widget = QWidget()
        devices_layout = QVBoxLayout(self.devices_widget)

        # Device panels will be added dynamically
        self.device_panels_layout = QGridLayout()
        devices_layout.addLayout(self.device_panels_layout)

        # Add some placeholder device panels for now
        self.device_panels = []
        # We'll add actual device panels when devices are discovered

        self.tabs.addTab(self.devices_widget, "Devices")

        # Status Dashboard tab
        self.status_dashboard = StatusDashboard()
        self.tabs.addTab(self.status_dashboard, "Status Dashboard")

        # Real-time Plotting tab
        self.real_time_plot_widget = RealTimePlotWidget()
        self.tabs.addTab(self.real_time_plot_widget, "Real-time Data")

        # Log Viewer tab
        self.log_viewer = LogViewer()
        self.tabs.addTab(self.log_viewer, "Logs")

        # Video Playback tab
        self.video_playback_window = VideoPlaybackWindow()
        self.tabs.addTab(self.video_playback_window, "Video Playback")

        self.content_splitter.addWidget(self.tabs)
        self.main_layout.addWidget(
            self.content_splitter,
            1)  # 1 = stretch factor

    def create_status_bar(self) -> None:
        """Create the status bar for the application.
        """
        self.statusBar().showMessage("Ready")

        # Add device count label
        self.device_count_label = QLabel("Devices: 0")
        self.statusBar().addPermanentWidget(self.device_count_label)

        # Add recording status label
        self.recording_status_label = QLabel("Not Recording")
        self.statusBar().addPermanentWidget(self.recording_status_label)

    def connect_signals(self) -> None:
        """Connect signals and slots.
        """
        # Connect device manager signals
        self.device_manager.device_discovered.connect(
            self.on_device_discovered)
        self.device_manager.device_removed.connect(self.on_device_removed)
        self.device_manager.device_connected.connect(self.on_device_connected)
        self.device_manager.device_disconnected.connect(
            self.on_device_disconnected)

        # Connect video playback window signals
        self.video_playback_window.video_changed.connect(self.on_video_changed)
        self.video_playback_window.annotation_added.connect(
            self.on_annotation_added)

        # Set up timer for updating UI
        self.update_timer = QTimer(self)
        self.update_timer.timeout.connect(self.update_ui)
        self.update_timer.start(1000)  # Update every second

    def update_ui_state(self) -> None:
        """Update the UI state based on the current application state.
        """
        # Update button states
        self.start_button.setEnabled(
            not self.recording and len(
                self.device_panels) > 0)
        self.stop_button.setEnabled(self.recording)

        # Update status labels
        if self.recording:
            self.recording_status_label.setText("Recording")
            self.recording_status_label.setStyleSheet("color: red;")
        else:
            self.recording_status_label.setText("Not Recording")
            self.recording_status_label.setStyleSheet("")

        # Update device count
        self.device_count_label.setText(f"Devices: {len(self.device_panels)}")

    def update_ui(self) -> None:
        """Update the UI with current information.
        """
        # Update duration if recording
        if self.recording:
            # In a real implementation, we would get the actual duration from the session manager
            # For now, just update the label with a placeholder
            self.duration_label.setText("Duration: 00:00:00")

        # Update device panels
        for panel in self.device_panels:
            panel.update_ui()

        # Update status dashboard
        self.status_dashboard.update_ui()

    @Slot()
    def on_new_session(self) -> None:
        """Handle the New Session action.
        """
        self.logger.info("Creating new session")
        # In a real implementation, we would show a dialog to configure the session
        # For now, just create a new session with a default name
        success = self.session_manager.create_new_session()
        if success:
            self.session_label.setText(
                f"Session: {self.session_manager.get_current_session_id()}")
            self.statusBar().showMessage("New session created", 3000)
        else:
            QMessageBox.warning(self, "Error", "Failed to create new session")

    @Slot()
    def on_open_session(self) -> None:
        """Handle the Open Session action.
        """
        self.logger.info("Opening session")
        # In a real implementation, we would show a dialog to select a session
        # For now, just show a file dialog
        session_dir = QFileDialog.getExistingDirectory(
            self, "Open Session Directory")
        if session_dir:
            success = self.session_manager.open_session(session_dir)
            if success:
                self.session_label.setText(
                    f"Session: {self.session_manager.get_current_session_id()}")
                self.statusBar().showMessage("Session opened", 3000)
            else:
                QMessageBox.warning(self, "Error", "Failed to open session")

    @Slot()
    def on_discover_devices(self) -> None:
        """Handle the Discover Devices action.
        """
        self.logger.info("Discovering devices")
        self.statusBar().showMessage("Discovering devices...")
        self.device_manager.discover_devices()

    @Slot()
    def on_discover_usb_devices(self) -> None:
        """Handle the Discover USB Devices action.
        """
        self.logger.info("Discovering USB devices")
        self.statusBar().showMessage("Discovering USB devices...")
        success = self.device_manager.discover_usb_devices()
        if success:
            self.statusBar().showMessage("USB device discovery completed", 3000)
        else:
            self.statusBar().showMessage("USB device discovery failed", 3000)
            QMessageBox.warning(self, "USB Discovery", 
                              "Failed to discover USB devices. Make sure ADB is available and devices are connected with USB debugging enabled.")

    @Slot()
    def on_connect_all(self) -> None:
        """Handle the Connect All action.
        """
        self.logger.info("Connecting to all devices")
        self.statusBar().showMessage("Connecting to all devices...")
        self.device_manager.connect_all_devices()

    @Slot()
    def on_disconnect_all(self) -> None:
        """Handle the Disconnect All action.
        """
        self.logger.info("Disconnecting from all devices")
        self.statusBar().showMessage("Disconnecting from all devices...")
        self.device_manager.disconnect_all_devices()

    @Slot()
    def on_settings(self) -> None:
        """Handle the Settings action.
        """
        self.logger.info("Opening settings dialog")
        try:
            # Create and show the settings dialog
            settings_dialog = SettingsDialog(self)

            # Connect the settings applied signal to handle changes
            settings_dialog.settings_applied.connect(self.on_settings_applied)

            # Show the dialog
            result = settings_dialog.exec_()

            if result == QDialog.Accepted:
                self.logger.info("Settings dialog accepted")
            else:
                self.logger.info("Settings dialog cancelled")

        except Exception as e:
            self.logger.error(f"Failed to open settings dialog: {str(e)}")
            QMessageBox.critical(
                self, "Error",
                f"Failed to open settings dialog:\n\n{str(e)}"
            )

    @Slot()
    def on_settings_applied(self, settings_dict: dict) -> None:
        """Handle when settings are applied from the settings dialog.

        Args:
            settings_dict: Dictionary containing all the applied settings
        """
        self.logger.info("Settings applied, updating system configuration")

        try:
            # Update network settings if device manager exists
            if hasattr(self, 'device_manager') and self.device_manager:
                network_settings = settings_dict.get('network', {})
                if network_settings:
                    # Update server port if changed
                    if 'server_port' in network_settings:
                        self.logger.info(f"Server port updated to: {network_settings['server_port']}")

                    # Update connection timeout if changed
                    if 'connection_timeout' in network_settings:
                        self.logger.info(f"Connection timeout updated to: {network_settings['connection_timeout']}")

            # Update UI settings
            ui_settings = settings_dict.get('ui', {})
            if ui_settings:
                # Update refresh interval for dashboard
                if 'refresh_interval' in ui_settings and hasattr(self, 'status_dashboard'):
                    refresh_interval = ui_settings['refresh_interval']
                    self.logger.info(f"Dashboard refresh interval updated to: {refresh_interval}ms")
                    # Update the dashboard refresh timer if it exists
                    if hasattr(self.status_dashboard, 'set_refresh_interval'):
                        self.status_dashboard.set_refresh_interval(refresh_interval)

                # Update font size if changed
                if 'font_size' in ui_settings:
                    font_size = ui_settings['font_size']
                    self.logger.info(f"Font size updated to: {font_size}pt")
                    # Apply font size changes to the application
                    font = self.font()
                    font.setPointSize(font_size)
                    self.setFont(font)

            # Update recording settings
            recording_settings = settings_dict.get('recording', {})
            if recording_settings:
                if 'output_dir' in recording_settings:
                    self.logger.info(f"Output directory updated to: {recording_settings['output_dir']}")
                if 'max_duration' in recording_settings:
                    self.logger.info(f"Max recording duration updated to: {recording_settings['max_duration']} minutes")

            # Update security settings
            security_settings = settings_dict.get('security', {})
            if security_settings:
                if 'enable_auth' in security_settings:
                    auth_enabled = security_settings['enable_auth']
                    self.logger.info(f"Authentication {'enabled' if auth_enabled else 'disabled'}")
                if 'enable_encryption' in security_settings:
                    encryption_enabled = security_settings['enable_encryption']
                    self.logger.info(f"Encryption {'enabled' if encryption_enabled else 'disabled'}")

            # Update advanced settings
            advanced_settings = settings_dict.get('advanced', {})
            if advanced_settings:
                if 'log_level' in advanced_settings:
                    log_level = advanced_settings['log_level']
                    self.logger.info(f"Log level updated to: {log_level}")
                    # Update the logger level
                    logging.getLogger().setLevel(getattr(logging, log_level))

            # Show confirmation message
            self.statusBar().showMessage("Settings applied successfully", 3000)

        except Exception as e:
            self.logger.error(f"Error applying settings: {e}")
            QMessageBox.warning(
                self, "Settings Error", 
                f"Some settings could not be applied:\n\n{str(e)}"
            )

    @Slot()
    def on_camera_calibration(self) -> None:
        """Handle the Camera Calibration action.
        """
        self.logger.info("Opening camera calibration dialog")
        try:
            # Create and show the calibration dialog
            calibration_dialog = CalibrationDialog(self)
            calibration_dialog.exec_()
        except Exception as e:
            self.logger.error(f"Failed to open calibration dialog: {str(e)}")
            QMessageBox.critical(
                self, "Error",
                f"Failed to open camera calibration dialog:\n\n{str(e)}"
            )

    @Slot()
    def on_live_calibration(self) -> None:
        """Handle the Live Calibration action.
        """
        self.logger.info("Opening live calibration dialog")
        try:
            # Create and show the live calibration dialog
            live_calibration_dialog = LiveCalibrationDialog(self)
            live_calibration_dialog.exec_()
        except Exception as e:
            self.logger.error(
                f"Failed to open live calibration dialog: {str(e)}")
            QMessageBox.critical(
                self, "Error",
                f"Failed to open live calibration dialog:\n\n{str(e)}"
            )

    @Slot()
    def on_about(self) -> None:
        """Handle the About action.
        """
        QMessageBox.about(
            self, "About", "GSR & Dual-Video Recording System\n\n"
            "A system for synchronized recording of GSR and dual-video data\n"
            "from multiple Android devices.\n\n"
            "© 2023 BuccaNCS")

    @Slot()
    def on_start_recording(self) -> None:
        """Handle the Start Recording button click.
        """
        self.logger.info("Starting recording")

        # Check if we have a session
        if not self.session_manager.has_current_session():
            # Create a new session if we don't have one
            success = self.session_manager.create_new_session()
            if not success:
                QMessageBox.warning(
                    self, "Error", "Failed to create new session")
                return
            self.session_label.setText(
                f"Session: {self.session_manager.get_current_session_id()}")

        # Check if we have connected devices
        if len(self.device_panels) == 0:
            QMessageBox.warning(self, "Error", "No devices connected")
            return

        # Collect sensor selections from all device panels
        sensor_map = {}
        for device_id, panel in self.device_panels.items():
            if hasattr(panel, 'get_selected_sensors'):
                selected_sensors = panel.get_selected_sensors()
                if selected_sensors:  # Only include devices with selected sensors
                    sensor_map[device_id] = selected_sensors
                    self.logger.info(f"Device {device_id} selected sensors: {selected_sensors}")
                else:
                    self.logger.warning(f"Device {device_id} has no sensors selected")
            else:
                # Fallback for devices without sensor selection UI
                self.logger.info(f"Device {device_id} using default sensor configuration")

        # Check if any sensors are selected
        if not sensor_map:
            response = QMessageBox.question(
                self,
                "No Sensors Selected",
                "No sensors are selected for recording. Do you want to continue with default settings?",
                QMessageBox.Yes | QMessageBox.No)
            if response == QMessageBox.No:
                return
            sensor_map = None  # Use default settings

        # Start recording on all devices with sensor selection
        success = self.device_manager.start_recording(
            self.session_manager.get_current_session_id(), sensor_map)
        if success:
            self.recording = True

            # Notify video playback window about recording start
            self.video_playback_window.set_recording_status(True, time.time())

            self.update_ui_state()
            self.statusBar().showMessage("Recording started", 3000)
        else:
            QMessageBox.warning(self, "Error", "Failed to start recording")

    @Slot()
    def on_stop_recording(self) -> None:
        """Handle the Stop Recording button click.
        """
        self.logger.info("Stopping recording")

        # Stop recording on all devices
        success = self.device_manager.stop_recording()
        if success:
            self.recording = False

            # Notify video playback window about recording stop
            self.video_playback_window.set_recording_status(False)

            self.update_ui_state()
            self.statusBar().showMessage("Recording stopped", 3000)

            # Generate session manifest
            self.session_manager.generate_manifest()

            # --- MODIFY THIS SECTION ---
            # Ask if user wants to collect files
            response = QMessageBox.question(
                self,
                "Collect Files",
                "Recording stopped. Do you want to collect all recorded files from the remote devices now?",
                QMessageBox.Yes | QMessageBox.No,
                QMessageBox.Yes # Default to Yes
            )
            if response == QMessageBox.Yes:
                self.statusBar().showMessage("Collecting files from devices...", 5000)
                # The destination directory is managed by the SessionManager
                destination_dir = self.session_manager.get_session_directory()
                self.device_manager.collect_files(destination_dir)
                self.statusBar().showMessage("File collection complete.", 3000)
            # --- END OF MODIFICATION ---
        else:
            QMessageBox.warning(self, "Error", "Failed to stop recording")

    @Slot(object)
    def on_device_discovered(self, device) -> None:
        """Handle a device being discovered.

        Args:
            device: The discovered device
        """
        self.logger.info(f"Device discovered: {device.name} ({device.id})")
        self.statusBar().showMessage(f"Device discovered: {device.name}", 3000)

        # In a real implementation, we would add the device to the UI
        # For now, just update the device count
        self.update_ui_state()

    @Slot(object)
    def on_device_removed(self, device) -> None:
        """Handle a device being removed from the network.

        Args:
            device: The removed device
        """
        self.logger.info(f"Device removed: {device.name} ({device.id})")
        self.statusBar().showMessage(f"Device removed: {device.name}", 3000)

        # If the device was connected, it should be handled by on_device_disconnected
        # This handler is for devices that disappear from the network discovery
        # For now, just update the device count
        self.update_ui_state()

    @Slot(object)
    def on_device_connected(self, device) -> None:
        """Handle a device being connected.

        Args:
            device: The connected device
        """
        self.logger.info(f"Device connected: {device.name} ({device.id})")
        self.statusBar().showMessage(f"Device connected: {device.name}", 3000)

        # Create a device panel for the device
        panel = DevicePanel(device)
        self.device_panels.append(panel)

        # Add the panel to the grid layout
        row = (len(self.device_panels) - 1) // 2
        col = (len(self.device_panels) - 1) % 2
        self.device_panels_layout.addWidget(panel, row, col)

        # Connect device signals to real-time plot widget
        device.gsr_data_received.connect(self.real_time_plot_widget.update_gsr_data)
        device.heart_rate_data_received.connect(self.real_time_plot_widget.update_heart_rate_data)

        # Update UI state
        self.update_ui_state()

    @Slot(object)
    def on_device_disconnected(self, device) -> None:
        """Handle a device being disconnected.

        Args:
            device: The disconnected device
        """
        self.logger.info(f"Device disconnected: {device.name} ({device.id})")
        self.statusBar().showMessage(
            f"Device disconnected: {device.name}", 3000)

        # Remove the device panel
        for i, panel in enumerate(self.device_panels):
            if panel.device.id == device.id:
                self.device_panels_layout.removeWidget(panel)
                panel.deleteLater()
                self.device_panels.pop(i)
                break

        # Rearrange the remaining panels
        for i, panel in enumerate(self.device_panels):
            row = i // 2
            col = i % 2
            self.device_panels_layout.addWidget(panel, row, col)

        # Update UI state
        self.update_ui_state()

    @Slot(str, dict)
    def on_video_changed(self, video_path, annotation_data) -> None:
        """Handle video change events from the video playback window.

        Args:
            video_path: Path to the new video
            annotation_data: Data about the video and annotations
        """
        self.logger.info(
            f"Video changed to: {annotation_data.get('video_name', 'Unknown')}")

        # Update status bar with video info
        video_name = annotation_data.get('video_name', 'Unknown')
        annotations_count = annotation_data.get('annotations_count', 0)
        self.statusBar().showMessage(
            f"Video: {video_name} ({annotations_count} annotations)", 5000)

    @Slot(str, str, dict)
    def on_annotation_added(self, video_path, timestamp, annotation) -> None:
        """Handle annotation added events from the video playback window.

        Args:
            video_path: Path to the video
            timestamp: Timestamp of the annotation
            annotation: Annotation data
        """
        video_name = os.path.basename(video_path)
        annotation_text = annotation.get('text', '')
        auto_generated = annotation.get('auto_generated', False)

        log_message = f"Annotation added to {video_name} at {timestamp}: {annotation_text}"
        if auto_generated:
            log_message += " (auto-generated)"

        self.logger.info(log_message)

        # Update status bar
        self.statusBar().showMessage(f"Annotation added to {video_name}", 3000)

    def closeEvent(self, event) -> None:
        """Handle the window close event.

        Args:
            event: The close event
        """
        # Check if we're recording
        if self.recording:
            response = QMessageBox.question(
                self,
                "Exit",
                "Recording is in progress. Are you sure you want to exit?",
                QMessageBox.Yes | QMessageBox.No)
            if response == QMessageBox.No:
                event.ignore()
                return

            # Stop recording
            self.device_manager.stop_recording()

        # Disconnect all devices
        self.device_manager.disconnect_all_devices()

        # Accept the event
        event.accept()
