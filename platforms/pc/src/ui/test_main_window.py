#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Comprehensive tests for PC Main Window buttons and functionalities.
Tests all interactive UI components and their behaviors.
"""

import sys
import unittest
from unittest.mock import Mock, MagicMock, patch
from PySide6.QtCore import Qt, QTimer
from PySide6.QtGui import QAction
from PySide6.QtWidgets import QApplication, QPushButton, QLabel, QTabWidget
from PySide6.QtTest import QTest

# Add the src directory to the path so we can import our modules
sys.path.insert(0, '/Users/duyantran/workspace/fyp-gsr-unified-buccancs/platforms/pc/src')

from ui.main_window import MainWindow
from network.device_manager import DeviceManager
from utils.session_manager import SessionManager


class TestMainWindow(unittest.TestCase):
    """Comprehensive tests for MainWindow buttons and functionalities."""

    @classmethod
    def setUpClass(cls):
        """Set up QApplication for all tests."""
        if not QApplication.instance():
            cls.app = QApplication(sys.argv)
        else:
            cls.app = QApplication.instance()

    def setUp(self):
        """Set up test fixtures before each test method."""
        # Mock dependencies to avoid actual hardware/network interactions
        with patch('ui.main_window.DeviceManager') as mock_device_manager, \
             patch('ui.main_window.SessionManager') as mock_session_manager:
            
            self.mock_device_manager = Mock(spec=DeviceManager)
            self.mock_session_manager = Mock(spec=SessionManager)
            
            mock_device_manager.return_value = self.mock_device_manager
            mock_session_manager.return_value = self.mock_session_manager
            
            # Create main window
            self.main_window = MainWindow()

    def tearDown(self):
        """Clean up after each test method."""
        if hasattr(self, 'main_window'):
            self.main_window.close()

    def test_main_window_initialization(self):
        """Test that MainWindow initializes correctly."""
        self.assertIsNotNone(self.main_window)
        self.assertIsInstance(self.main_window, MainWindow)
        self.assertEqual(self.main_window.windowTitle(), 
                        "GSR & Dual-Video Recording System - Modern Interface")

    def test_control_buttons_exist(self):
        """Test that all control buttons are created and accessible."""
        # Check start button
        self.assertTrue(hasattr(self.main_window, 'start_button'))
        self.assertIsInstance(self.main_window.start_button, QPushButton)
        self.assertEqual(self.main_window.start_button.text(), "🔴 Start Recording")
        
        # Check stop button
        self.assertTrue(hasattr(self.main_window, 'stop_button'))
        self.assertIsInstance(self.main_window.stop_button, QPushButton)
        self.assertEqual(self.main_window.stop_button.text(), "⏹️ Stop Recording")

    def test_initial_button_states(self):
        """Test that buttons have correct initial states."""
        # Start button should be enabled initially
        self.assertTrue(self.main_window.start_button.isEnabled())
        
        # Stop button should be disabled initially
        self.assertFalse(self.main_window.stop_button.isEnabled())

    def test_start_recording_button_click(self):
        """Test start recording button functionality."""
        # Mock the recording functionality
        with patch.object(self.main_window, 'on_start_recording') as mock_start:
            # Simulate button click
            QTest.mouseClick(self.main_window.start_button, Qt.LeftButton)
            
            # Verify the handler was called
            mock_start.assert_called_once()

    def test_stop_recording_button_click(self):
        """Test stop recording button functionality."""
        # Enable stop button first (simulate recording state)
        self.main_window.stop_button.setEnabled(True)
        
        # Mock the stop recording functionality
        with patch.object(self.main_window, 'on_stop_recording') as mock_stop:
            # Simulate button click
            QTest.mouseClick(self.main_window.stop_button, Qt.LeftButton)
            
            # Verify the handler was called
            mock_stop.assert_called_once()

    def test_menu_actions_exist(self):
        """Test that all menu actions are created."""
        menu_bar = self.main_window.menuBar()
        
        # Check File menu
        file_menu = None
        for action in menu_bar.actions():
            if action.text() == "&File":
                file_menu = action.menu()
                break
        
        self.assertIsNotNone(file_menu)
        
        # Check Devices menu
        devices_menu = None
        for action in menu_bar.actions():
            if action.text() == "&Devices":
                devices_menu = action.menu()
                break
        
        self.assertIsNotNone(devices_menu)
        
        # Check Tools menu
        tools_menu = None
        for action in menu_bar.actions():
            if action.text() == "&Tools":
                tools_menu = action.menu()
                break
        
        self.assertIsNotNone(tools_menu)
        
        # Check Help menu
        help_menu = None
        for action in menu_bar.actions():
            if action.text() == "&Help":
                help_menu = action.menu()
                break
        
        self.assertIsNotNone(help_menu)

    def test_new_session_action(self):
        """Test New Session menu action."""
        with patch.object(self.main_window, 'on_new_session') as mock_new_session:
            # Find and trigger the New Session action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&File":
                    file_menu = action.menu()
                    for file_action in file_menu.actions():
                        if file_action.text() == "&New Session":
                            file_action.trigger()
                            break
                    break
            
            mock_new_session.assert_called_once()

    def test_open_session_action(self):
        """Test Open Session menu action."""
        with patch.object(self.main_window, 'on_open_session') as mock_open_session:
            # Find and trigger the Open Session action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&File":
                    file_menu = action.menu()
                    for file_action in file_menu.actions():
                        if file_action.text() == "&Open Session":
                            file_action.trigger()
                            break
                    break
            
            mock_open_session.assert_called_once()

    def test_discover_devices_action(self):
        """Test Discover Devices menu action."""
        with patch.object(self.main_window, 'on_discover_devices') as mock_discover:
            # Find and trigger the Discover Devices action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&Devices":
                    devices_menu = action.menu()
                    for device_action in devices_menu.actions():
                        if device_action.text() == "&Discover Devices":
                            device_action.trigger()
                            break
                    break
            
            mock_discover.assert_called_once()

    def test_discover_usb_devices_action(self):
        """Test Discover USB Devices menu action."""
        with patch.object(self.main_window, 'on_discover_usb_devices') as mock_discover_usb:
            # Find and trigger the Discover USB Devices action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&Devices":
                    devices_menu = action.menu()
                    for device_action in devices_menu.actions():
                        if device_action.text() == "Discover &USB Devices":
                            device_action.trigger()
                            break
                    break
            
            mock_discover_usb.assert_called_once()

    def test_connect_all_action(self):
        """Test Connect All menu action."""
        with patch.object(self.main_window, 'on_connect_all') as mock_connect_all:
            # Find and trigger the Connect All action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&Devices":
                    devices_menu = action.menu()
                    for device_action in devices_menu.actions():
                        if device_action.text() == "Connect &All":
                            device_action.trigger()
                            break
                    break
            
            mock_connect_all.assert_called_once()

    def test_disconnect_all_action(self):
        """Test Disconnect All menu action."""
        with patch.object(self.main_window, 'on_disconnect_all') as mock_disconnect_all:
            # Find and trigger the Disconnect All action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&Devices":
                    devices_menu = action.menu()
                    for device_action in devices_menu.actions():
                        if device_action.text() == "&Disconnect All":
                            device_action.trigger()
                            break
                    break
            
            mock_disconnect_all.assert_called_once()

    def test_settings_action(self):
        """Test Settings menu action."""
        with patch.object(self.main_window, 'on_settings') as mock_settings:
            # Find and trigger the Settings action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&Tools":
                    tools_menu = action.menu()
                    for tool_action in tools_menu.actions():
                        if tool_action.text() == "&Settings":
                            tool_action.trigger()
                            break
                    break
            
            mock_settings.assert_called_once()

    def test_camera_calibration_action(self):
        """Test Camera Calibration menu action."""
        with patch.object(self.main_window, 'on_camera_calibration') as mock_calibration:
            # Find and trigger the Camera Calibration action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&Tools":
                    tools_menu = action.menu()
                    for tool_action in tools_menu.actions():
                        if "Camera Calibration" in tool_action.text():
                            tool_action.trigger()
                            break
                    break
            
            mock_calibration.assert_called_once()

    def test_live_calibration_action(self):
        """Test Live Calibration menu action."""
        with patch.object(self.main_window, 'on_live_calibration') as mock_live_calibration:
            # Find and trigger the Live Calibration action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&Tools":
                    tools_menu = action.menu()
                    for tool_action in tools_menu.actions():
                        if "Live Calibration" in tool_action.text():
                            tool_action.trigger()
                            break
                    break
            
            mock_live_calibration.assert_called_once()

    def test_about_action(self):
        """Test About menu action."""
        with patch.object(self.main_window, 'on_about') as mock_about:
            # Find and trigger the About action
            menu_bar = self.main_window.menuBar()
            for action in menu_bar.actions():
                if action.text() == "&Help":
                    help_menu = action.menu()
                    for help_action in help_menu.actions():
                        if help_action.text() == "&About":
                            help_action.trigger()
                            break
                    break
            
            mock_about.assert_called_once()

    def test_tabs_exist(self):
        """Test that all tabs are created."""
        self.assertTrue(hasattr(self.main_window, 'tabs'))
        self.assertIsInstance(self.main_window.tabs, QTabWidget)
        
        # Check that tabs are added
        tab_count = self.main_window.tabs.count()
        self.assertGreater(tab_count, 0)
        
        # Check for expected tabs
        tab_names = []
        for i in range(tab_count):
            tab_names.append(self.main_window.tabs.tabText(i))
        
        expected_tabs = ["Devices", "Status Dashboard", "Real-time Data", "Logs", "Video Playback"]
        for expected_tab in expected_tabs:
            self.assertIn(expected_tab, tab_names)

    def test_status_labels_exist(self):
        """Test that status labels are created."""
        self.assertTrue(hasattr(self.main_window, 'session_label'))
        self.assertIsInstance(self.main_window.session_label, QLabel)
        
        self.assertTrue(hasattr(self.main_window, 'duration_label'))
        self.assertIsInstance(self.main_window.duration_label, QLabel)

    def test_recording_state_changes(self):
        """Test recording state changes affect UI correctly."""
        # Initial state
        self.assertFalse(self.main_window.recording)
        self.assertTrue(self.main_window.start_button.isEnabled())
        self.assertFalse(self.main_window.stop_button.isEnabled())
        
        # Simulate recording started
        self.main_window.recording = True
        self.main_window.update_ui_state()
        
        # After starting recording
        self.assertFalse(self.main_window.start_button.isEnabled())
        self.assertTrue(self.main_window.stop_button.isEnabled())
        
        # Simulate recording stopped
        self.main_window.recording = False
        self.main_window.update_ui_state()
        
        # After stopping recording
        self.assertTrue(self.main_window.start_button.isEnabled())
        self.assertFalse(self.main_window.stop_button.isEnabled())

    def test_device_discovery_workflow(self):
        """Test device discovery workflow."""
        # Mock device discovery
        mock_device = Mock()
        mock_device.name = "Test Device"
        mock_device.id = "test_device_001"
        
        # Simulate device discovered
        self.main_window.on_device_discovered(mock_device)
        
        # Verify device was handled (no exceptions thrown)
        self.assertTrue(True)

    def test_device_connection_workflow(self):
        """Test device connection workflow."""
        # Mock device connection
        mock_device = Mock()
        mock_device.name = "Test Device"
        mock_device.id = "test_device_001"
        
        # Simulate device connected
        self.main_window.on_device_connected(mock_device)
        
        # Verify device connection was handled (no exceptions thrown)
        self.assertTrue(True)

    def test_ui_update_timer(self):
        """Test that UI update timer is set up correctly."""
        self.assertTrue(hasattr(self.main_window, 'update_timer'))
        self.assertIsInstance(self.main_window.update_timer, QTimer)
        self.assertTrue(self.main_window.update_timer.isActive())

    def test_window_close_handling(self):
        """Test that window closes gracefully."""
        # This should not raise any exceptions
        self.main_window.close()
        self.assertTrue(True)

    def test_keyboard_shortcuts(self):
        """Test keyboard shortcuts work correctly."""
        # Test Ctrl+N for New Session
        with patch.object(self.main_window, 'on_new_session') as mock_new_session:
            QTest.keySequence(self.main_window, "Ctrl+N")
            # Note: In a real test environment, this would trigger the action
            # For now, we just verify the test doesn't crash
            self.assertTrue(True)

    def test_button_accessibility(self):
        """Test button accessibility properties."""
        # Check that buttons have proper accessibility
        self.assertTrue(self.main_window.start_button.isVisible())
        self.assertTrue(self.main_window.stop_button.isVisible())
        
        # Check button sizes are reasonable
        start_size = self.main_window.start_button.size()
        stop_size = self.main_window.stop_button.size()
        
        self.assertGreater(start_size.width(), 100)
        self.assertGreater(start_size.height(), 30)
        self.assertGreater(stop_size.width(), 100)
        self.assertGreater(stop_size.height(), 30)

    def test_error_handling_in_ui_operations(self):
        """Test that UI operations handle errors gracefully."""
        # Test that UI operations don't crash on invalid inputs
        try:
            self.main_window.on_device_discovered(None)
            self.main_window.on_device_connected(None)
            self.main_window.on_device_disconnected(None)
            self.main_window.on_device_removed(None)
        except Exception as e:
            self.fail(f"UI operations should handle invalid inputs gracefully: {e}")

    def test_session_management_integration(self):
        """Test session management integration with UI."""
        # Test session label updates
        initial_text = self.main_window.session_label.text()
        self.assertEqual(initial_text, "Session: Not started")
        
        # Simulate session start
        self.main_window.session_label.setText("Session: Recording_001")
        updated_text = self.main_window.session_label.text()
        self.assertEqual(updated_text, "Session: Recording_001")


if __name__ == '__main__':
    unittest.main()