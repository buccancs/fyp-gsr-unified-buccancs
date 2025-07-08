#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Comprehensive unit tests for the DeviceManager class.

This module contains unit tests for the DeviceManager class, testing device
discovery, connection management, and recording functionality.
"""

import os
import sys
import tempfile
import threading
import time
import unittest
from unittest.mock import Mock, patch
from typing import Any, Dict, List, Optional, Union

# Add the src directory to the path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src'))

# Add the tests directory to the path for common utilities
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..'))

# Import common test utilities
try:
    from common.fixtures import BaseTestCase, MockDevice, TestDataGenerator
    from common.mocks import MockDeviceManager, create_mock_device_manager
    from common.utils import setup_test_environment, cleanup_test_environment
except ImportError:
    # Fallback to basic unittest if common utilities are not available
    BaseTestCase = unittest.TestCase

    class MockDevice:
        def __init__(self, device_id="test", device_type="shimmer"):
            self.device_id = device_id
            self.device_type = device_type
            self.connected = False
            self.recording = False

        def connect(self):
            self.connected = True
            return True

        def disconnect(self):
            self.connected = False
            return True

        def start_recording(self):
            if self.connected:
                self.recording = True
                return True
            return False

        def stop_recording(self):
            self.recording = False
            return True

# Import the modules to test
from src.network.device_manager import DeviceListener, DeviceManager
from src.network.device import Device


class TestDeviceManager(BaseTestCase):
    """Test case for the DeviceManager class."""

    def setUp(self) -> None:
        """Set up the test case."""
        super().setUp()
        self.device_manager = DeviceManager()

        # Create mock devices using common utilities
        self.mock_device1 = MockDevice("device_001", "shimmer")
        self.mock_device1.device_name = "Test Device 1"
        self.mock_device1.ip_address = "192.168.1.100"
        self.mock_device1.port = 8080

        self.mock_device2 = MockDevice("device_002", "webcam")
        self.mock_device2.device_name = "Test Device 2"
        self.mock_device2.ip_address = "192.168.1.101"
        self.mock_device2.port = 8081

    def tearDown(self) -> None:
        """Clean up after the test case."""
        self.device_manager.cleanup()
        super().tearDown()

    def test_initialization(self) -> None:
        """Test DeviceManager initialization."""
        self.assertIsNotNone(self.device_manager)
        self.assertEqual(len(self.device_manager.devices), 0)
        self.assertFalse(self.device_manager.is_discovering)

    @patch('src.network.device_manager.Zeroconf')
    @patch('src.network.device_manager.ServiceBrowser')
    def test_discover_devices(self, mock_service_browser, mock_zeroconf) -> None:
        """Test device discovery functionality."""
        # Mock zeroconf and service browser
        mock_zeroconf_instance = Mock()
        mock_zeroconf.return_value = mock_zeroconf_instance

        mock_browser_instance = Mock()
        mock_service_browser.return_value = mock_browser_instance

        # Start discovery
        result = self.device_manager.discover_devices()

        self.assertTrue(result)
        self.assertTrue(self.device_manager.is_discovering)

        # Verify zeroconf and service browser were created
        mock_zeroconf.assert_called_once()
        mock_service_browser.assert_called_once()

    def test_add_device(self) -> None:
        """Test adding devices to the manager."""
        # Add first device
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1

        self.assertEqual(len(self.device_manager.devices), 1)
        self.assertIn(self.mock_device1.device_id, self.device_manager.devices)

        # Add second device
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2

        self.assertEqual(len(self.device_manager.devices), 2)
        self.assertIn(self.mock_device2.device_id, self.device_manager.devices)

    def test_connect_device(self) -> None:
        """Test connecting to a specific device."""
        # Add device to manager
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1

        # Connect to device (using MockDevice which returns True)
        result = self.device_manager.connect_device(self.mock_device1.device_id)

        self.assertTrue(result)
        self.assertTrue(self.mock_device1.connected)

    def test_connect_nonexistent_device(self) -> None:
        """Test connecting to a device that doesn't exist."""
        result = self.device_manager.connect_device("nonexistent_device")
        self.assertFalse(result)

    def test_connect_device_failure(self) -> None:
        """Test handling connection failure."""
        # Add device to manager
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1

        # Mock connection failure
        original_connect = self.mock_device1.connect
        self.mock_device1.connect = Mock(return_value=False)

        result = self.device_manager.connect_device(self.mock_device1.device_id)

        self.assertFalse(result)
        self.assertFalse(self.mock_device1.connected)

        # Restore original method
        self.mock_device1.connect = original_connect

    def test_disconnect_device(self) -> None:
        """Test disconnecting from a device."""
        # Add and connect device
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.mock_device1.connect()

        # Disconnect device
        result = self.device_manager.disconnect_device(self.mock_device1.device_id)

        self.assertTrue(result)
        self.assertFalse(self.mock_device1.connected)

    def test_disconnect_nonexistent_device(self) -> None:
        """Test disconnecting from a device that doesn't exist."""
        result = self.device_manager.disconnect_device("nonexistent_device")
        self.assertFalse(result)

    def test_connect_all_devices(self) -> None:
        """Test connecting to all devices."""
        # Add devices to manager
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2

        # Connect all devices
        results = self.device_manager.connect_all_devices()

        self.assertEqual(len(results), 2)
        self.assertTrue(all(results.values()))
        self.assertTrue(self.mock_device1.connected)
        self.assertTrue(self.mock_device2.connected)

    def test_disconnect_all_devices(self) -> None:
        """Test disconnecting from all devices."""
        # Add and connect devices
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        self.mock_device1.connect()
        self.mock_device2.connect()

        # Disconnect all devices
        results = self.device_manager.disconnect_all_devices()

        self.assertEqual(len(results), 2)
        self.assertTrue(all(results.values()))
        self.assertFalse(self.mock_device1.connected)
        self.assertFalse(self.mock_device2.connected)

    def test_start_recording(self) -> None:
        """Test starting recording on devices."""
        # Add and connect device
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.mock_device1.connect()

        # Start recording
        result = self.device_manager.start_recording(self.mock_device1.device_id)

        self.assertTrue(result)
        self.assertTrue(self.mock_device1.recording)

    def test_start_recording_disconnected_devices(self) -> None:
        """Test starting recording on disconnected devices."""
        # Add device but don't connect
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1

        # Try to start recording
        result = self.device_manager.start_recording(self.mock_device1.device_id)

        self.assertFalse(result)
        self.assertFalse(self.mock_device1.recording)

    def test_stop_recording(self) -> None:
        """Test stopping recording on devices."""
        # Add, connect, and start recording
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.mock_device1.connect()
        self.mock_device1.start_recording()

        # Stop recording
        result = self.device_manager.stop_recording(self.mock_device1.device_id)

        self.assertTrue(result)
        self.assertFalse(self.mock_device1.recording)

    def test_get_device_status(self) -> None:
        """Test getting device status."""
        # Add and connect device
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.mock_device1.connect()

        # Get status
        status = self.device_manager.get_device_status(self.mock_device1.device_id)

        self.assertIsNotNone(status)
        self.assertEqual(status['device_id'], self.mock_device1.device_id)
        self.assertTrue(status['connected'])

    def test_get_device_status_nonexistent(self) -> None:
        """Test getting status for nonexistent device."""
        status = self.device_manager.get_device_status("nonexistent_device")
        self.assertIsNone(status)

    def test_cleanup(self) -> None:
        """Test cleanup functionality."""
        # Add and connect devices
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        self.mock_device1.connect()
        self.mock_device2.connect()

        # Cleanup
        self.device_manager.cleanup()

        # Verify all devices are disconnected
        self.assertFalse(self.mock_device1.connected)
        self.assertFalse(self.mock_device2.connected)


class TestDeviceListener(BaseTestCase):
    """Test case for the DeviceListener class."""

    def setUp(self) -> None:
        """Set up the test case."""
        super().setUp()
        self.device_manager = MockDeviceManager()
        self.device_listener = DeviceListener(self.device_manager)

    def test_initialization(self) -> None:
        """Test DeviceListener initialization."""
        self.assertIsNotNone(self.device_listener)
        self.assertEqual(self.device_listener.device_manager, self.device_manager)

    @patch('socket.inet_ntoa')
    def test_add_service(self, mock_inet_ntoa) -> None:
        """Test adding a service through the listener."""
        # Mock the inet_ntoa function
        mock_inet_ntoa.return_value = "192.168.1.100"

        # Create mock service info
        mock_info = Mock()
        mock_info.addresses = [b'\xc0\xa8\x01\x64']  # 192.168.1.100
        mock_info.port = 8080
        mock_info.properties = {b'name': b'Test Device'}

        # Add service
        self.device_listener.add_service(None, "_http._tcp.local.", "test_service", mock_info)

        # Verify device was added
        devices = self.device_manager.discover_devices()
        self.assertGreater(len(devices), 0)

    def test_remove_service(self) -> None:
        """Test removing a service through the listener."""
        # This is a simple test since remove_service typically just logs
        # In a real implementation, you might track removed services
        self.device_listener.remove_service(None, "_http._tcp.local.", "test_service")

        # Test passes if no exception is raised
        self.assertTrue(True)


if __name__ == '__main__':
    unittest.main()
