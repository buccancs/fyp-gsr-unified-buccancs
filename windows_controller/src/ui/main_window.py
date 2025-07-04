#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Main window for the PC Controller App.
Cross-platform support: Windows, macOS, Linux.
"""

import os
import sys
import logging
from PySide6.QtWidgets import (QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, 
                            QPushButton, QLabel, QTabWidget, QGroupBox, 
                            QGridLayout, QStatusBar, QAction, QMenu, QMessageBox,
                            QFileDialog, QSplitter)
from PySide6.QtCore import Qt, QTimer, Slot, Signal
from PySide6.QtGui import QIcon, QPixmap

# Import our modules
from ui.device_panel import DevicePanel
from ui.video_preview import VideoPreview
from ui.video_playback_window import VideoPlaybackWindow
from ui.status_dashboard import StatusDashboard
from ui.log_viewer import LogViewer
from network.device_manager import DeviceManager
from utils.logger import get_logger
from utils.session_manager import SessionManager

class MainWindow(QMainWindow):
    """
    Main window class for the PC Controller App.
    Cross-platform support: Windows, macOS, Linux.
    """

    def __init__(self):
        """
        Initialize the main window.
        """
        super().__init__()

        # Set up logging
        self.logger = get_logger(__name__)
        self.logger.info("Initializing main window")

        # Initialize components
        self.device_manager = DeviceManager()
        self.session_manager = SessionManager()

        # Set up modern UI
        self.setWindowTitle("GSR & Dual-Video Recording System - Modern Interface")
        self.setMinimumSize(1400, 900)

        # Apply modern styling
        self.setStyleSheet("""
            QMainWindow {
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

    def create_menu_bar(self):
        """
        Create the menu bar for the application.
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

        connect_all_action = QAction("Connect &All", self)
        connect_all_action.triggered.connect(self.on_connect_all)
        devices_menu.addAction(connect_all_action)

        disconnect_all_action = QAction("&Disconnect All", self)
        disconnect_all_action.triggered.connect(self.on_disconnect_all)
        devices_menu.addAction(disconnect_all_action)

        # Tools menu
        tools_menu = self.menuBar().addMenu("&Tools")

        settings_action = QAction("&Settings", self)
        settings_action.triggered.connect(self.on_settings)
        tools_menu.addAction(settings_action)

        # Help menu
        help_menu = self.menuBar().addMenu("&Help")

        about_action = QAction("&About", self)
        about_action.triggered.connect(self.on_about)
        help_menu.addAction(about_action)

    def create_control_panel(self):
        """
        Create the control panel for the application.
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

    def create_device_panels(self):
        """
        Create the device panels for the application.
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

        # Log Viewer tab
        self.log_viewer = LogViewer()
        self.tabs.addTab(self.log_viewer, "Logs")

        # Video Playback tab
        self.video_playback_window = VideoPlaybackWindow()
        self.tabs.addTab(self.video_playback_window, "Video Playback")

        self.content_splitter.addWidget(self.tabs)
        self.main_layout.addWidget(self.content_splitter, 1)  # 1 = stretch factor

    def create_status_bar(self):
        """
        Create the status bar for the application.
        """
        self.statusBar().showMessage("Ready")

        # Add device count label
        self.device_count_label = QLabel("Devices: 0")
        self.statusBar().addPermanentWidget(self.device_count_label)

        # Add recording status label
        self.recording_status_label = QLabel("Not Recording")
        self.statusBar().addPermanentWidget(self.recording_status_label)

    def connect_signals(self):
        """
        Connect signals and slots.
        """
        # Connect device manager signals
        self.device_manager.device_discovered.connect(self.on_device_discovered)
        self.device_manager.device_removed.connect(self.on_device_removed)
        self.device_manager.device_connected.connect(self.on_device_connected)
        self.device_manager.device_disconnected.connect(self.on_device_disconnected)

        # Connect video playback window signals
        self.video_playback_window.video_changed.connect(self.on_video_changed)
        self.video_playback_window.annotation_added.connect(self.on_annotation_added)

        # Set up timer for updating UI
        self.update_timer = QTimer(self)
        self.update_timer.timeout.connect(self.update_ui)
        self.update_timer.start(1000)  # Update every second

    def update_ui_state(self):
        """
        Update the UI state based on the current application state.
        """
        # Update button states
        self.start_button.setEnabled(not self.recording and len(self.device_panels) > 0)
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

    def update_ui(self):
        """
        Update the UI with current information.
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
    def on_new_session(self):
        """
        Handle the New Session action.
        """
        self.logger.info("Creating new session")
        # In a real implementation, we would show a dialog to configure the session
        # For now, just create a new session with a default name
        success = self.session_manager.create_new_session()
        if success:
            self.session_label.setText(f"Session: {self.session_manager.get_current_session_id()}")
            self.statusBar().showMessage("New session created", 3000)
        else:
            QMessageBox.warning(self, "Error", "Failed to create new session")

    @Slot()
    def on_open_session(self):
        """
        Handle the Open Session action.
        """
        self.logger.info("Opening session")
        # In a real implementation, we would show a dialog to select a session
        # For now, just show a file dialog
        session_dir = QFileDialog.getExistingDirectory(self, "Open Session Directory")
        if session_dir:
            success = self.session_manager.open_session(session_dir)
            if success:
                self.session_label.setText(f"Session: {self.session_manager.get_current_session_id()}")
                self.statusBar().showMessage("Session opened", 3000)
            else:
                QMessageBox.warning(self, "Error", "Failed to open session")

    @Slot()
    def on_discover_devices(self):
        """
        Handle the Discover Devices action.
        """
        self.logger.info("Discovering devices")
        self.statusBar().showMessage("Discovering devices...")
        self.device_manager.discover_devices()

    @Slot()
    def on_connect_all(self):
        """
        Handle the Connect All action.
        """
        self.logger.info("Connecting to all devices")
        self.statusBar().showMessage("Connecting to all devices...")
        self.device_manager.connect_all_devices()

    @Slot()
    def on_disconnect_all(self):
        """
        Handle the Disconnect All action.
        """
        self.logger.info("Disconnecting from all devices")
        self.statusBar().showMessage("Disconnecting from all devices...")
        self.device_manager.disconnect_all_devices()

    @Slot()
    def on_settings(self):
        """
        Handle the Settings action.
        """
        self.logger.info("Opening settings")
        # In a real implementation, we would show a settings dialog
        QMessageBox.information(self, "Settings", "Settings dialog not implemented yet")

    @Slot()
    def on_about(self):
        """
        Handle the About action.
        """
        QMessageBox.about(self, "About", 
                         "GSR & Dual-Video Recording System\n\n"
                         "A system for synchronized recording of GSR and dual-video data\n"
                         "from multiple Android devices.\n\n"
                         "© 2023 BuccaNCS")

    @Slot()
    def on_start_recording(self):
        """
        Handle the Start Recording button click.
        """
        self.logger.info("Starting recording")

        # Check if we have a session
        if not self.session_manager.has_current_session():
            # Create a new session if we don't have one
            success = self.session_manager.create_new_session()
            if not success:
                QMessageBox.warning(self, "Error", "Failed to create new session")
                return
            self.session_label.setText(f"Session: {self.session_manager.get_current_session_id()}")

        # Check if we have connected devices
        if len(self.device_panels) == 0:
            QMessageBox.warning(self, "Error", "No devices connected")
            return

        # Start recording on all devices
        success = self.device_manager.start_recording(self.session_manager.get_current_session_id())
        if success:
            self.recording = True

            # Notify video playback window about recording start
            import time
            self.video_playback_window.set_recording_status(True, time.time())

            self.update_ui_state()
            self.statusBar().showMessage("Recording started", 3000)
        else:
            QMessageBox.warning(self, "Error", "Failed to start recording")

    @Slot()
    def on_stop_recording(self):
        """
        Handle the Stop Recording button click.
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

            # Ask if user wants to collect files
            response = QMessageBox.question(self, "Collect Files", 
                                          "Do you want to collect files from devices?",
                                          QMessageBox.Yes | QMessageBox.No)
            if response == QMessageBox.Yes:
                self.device_manager.collect_files(self.session_manager.get_session_directory())
        else:
            QMessageBox.warning(self, "Error", "Failed to stop recording")

    @Slot(object)
    def on_device_discovered(self, device):
        """
        Handle a device being discovered.

        Args:
            device: The discovered device
        """
        self.logger.info(f"Device discovered: {device.name} ({device.id})")
        self.statusBar().showMessage(f"Device discovered: {device.name}", 3000)

        # In a real implementation, we would add the device to the UI
        # For now, just update the device count
        self.update_ui_state()

    @Slot(object)
    def on_device_removed(self, device):
        """
        Handle a device being removed from the network.

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
    def on_device_connected(self, device):
        """
        Handle a device being connected.

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

        # Update UI state
        self.update_ui_state()

    @Slot(object)
    def on_device_disconnected(self, device):
        """
        Handle a device being disconnected.

        Args:
            device: The disconnected device
        """
        self.logger.info(f"Device disconnected: {device.name} ({device.id})")
        self.statusBar().showMessage(f"Device disconnected: {device.name}", 3000)

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
    def on_video_changed(self, video_path, annotation_data):
        """
        Handle video change events from the video playback window.

        Args:
            video_path: Path to the new video
            annotation_data: Data about the video and annotations
        """
        self.logger.info(f"Video changed to: {annotation_data.get('video_name', 'Unknown')}")

        # Update status bar with video info
        video_name = annotation_data.get('video_name', 'Unknown')
        annotations_count = annotation_data.get('annotations_count', 0)
        self.statusBar().showMessage(f"Video: {video_name} ({annotations_count} annotations)", 5000)

    @Slot(str, str, dict)
    def on_annotation_added(self, video_path, timestamp, annotation):
        """
        Handle annotation added events from the video playback window.

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

    def closeEvent(self, event):
        """
        Handle the window close event.

        Args:
            event: The close event
        """
        # Check if we're recording
        if self.recording:
            response = QMessageBox.question(self, "Exit", 
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
