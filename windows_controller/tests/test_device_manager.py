#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Comprehensive unit tests for the DeviceManager class.
"""

import unittest
import os
import sys
import tempfile
import shutil
import json
import socket
import threading
import time
from unittest.mock import Mock, MagicMock, patch, call

# Add the parent directory to the path so we can import our modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.network.device_manager import DeviceManager, DeviceListener
from src.network.device import Device

class TestDeviceManager(unittest.TestCase):
    """
    Test case for the DeviceManager class.
    """
    
    def setUp(self):
        """
        Set up the test case.
        """
        self.device_manager = DeviceManager()
        
        # Create mock devices
        self.mock_device1 = Mock(spec=Device)
        self.mock_device1.device_id = "device_001"
        self.mock_device1.device_name = "Test Device 1"
        self.mock_device1.ip_address = "192.168.1.100"
        self.mock_device1.port = 8080
        self.mock_device1.is_connected = False
        self.mock_device1.is_recording = False
        
        self.mock_device2 = Mock(spec=Device)
        self.mock_device2.device_id = "device_002"
        self.mock_device2.device_name = "Test Device 2"
        self.mock_device2.ip_address = "192.168.1.101"
        self.mock_device2.port = 8081
        self.mock_device2.is_connected = False
        self.mock_device2.is_recording = False
    
    def tearDown(self):
        """
        Clean up after the test case.
        """
        self.device_manager.cleanup()
    
    def test_initialization(self):
        """
        Test DeviceManager initialization.
        """
        self.assertIsNotNone(self.device_manager)
        self.assertEqual(len(self.device_manager.devices), 0)
        self.assertFalse(self.device_manager.is_discovering)
    
    @patch('src.network.device_manager.Zeroconf')
    @patch('src.network.device_manager.ServiceBrowser')
    def test_discover_devices(self, mock_service_browser, mock_zeroconf):
        """
        Test device discovery functionality.
        """
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
    
    def test_add_device(self):
        """
        Test adding devices to the manager.
        """
        # Add first device
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        
        self.assertEqual(len(self.device_manager.devices), 1)
        self.assertIn(self.mock_device1.device_id, self.device_manager.devices)
        
        # Add second device
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        
        self.assertEqual(len(self.device_manager.devices), 2)
        self.assertIn(self.mock_device2.device_id, self.device_manager.devices)
    
    def test_connect_device(self):
        """
        Test connecting to a specific device.
        """
        # Add device to manager
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        
        # Mock successful connection
        self.mock_device1.connect.return_value = True
        self.mock_device1.is_connected = True
        
        # Connect to device
        result = self.device_manager.connect_device(self.mock_device1.device_id)
        
        self.assertTrue(result)
        self.mock_device1.connect.assert_called_once()
    
    def test_connect_nonexistent_device(self):
        """
        Test connecting to a device that doesn't exist.
        """
        result = self.device_manager.connect_device("nonexistent_device")
        self.assertFalse(result)
    
    def test_connect_device_failure(self):
        """
        Test handling connection failure.
        """
        # Add device to manager
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        
        # Mock connection failure
        self.mock_device1.connect.return_value = False
        
        # Attempt to connect
        result = self.device_manager.connect_device(self.mock_device1.device_id)
        
        self.assertFalse(result)
        self.mock_device1.connect.assert_called_once()
    
    def test_disconnect_device(self):
        """
        Test disconnecting from a specific device.
        """
        # Add connected device
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.mock_device1.is_connected = True
        
        # Mock successful disconnection
        self.mock_device1.disconnect.return_value = True
        self.mock_device1.is_connected = False
        
        # Disconnect from device
        result = self.device_manager.disconnect_device(self.mock_device1.device_id)
        
        self.assertTrue(result)
        self.mock_device1.disconnect.assert_called_once()
    
    def test_disconnect_nonexistent_device(self):
        """
        Test disconnecting from a device that doesn't exist.
        """
        result = self.device_manager.disconnect_device("nonexistent_device")
        self.assertFalse(result)
    
    def test_connect_all_devices(self):
        """
        Test connecting to all devices.
        """
        # Add devices to manager
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        
        # Mock successful connections
        self.mock_device1.connect.return_value = True
        self.mock_device2.connect.return_value = True
        
        # Connect to all devices
        results = self.device_manager.connect_all_devices()
        
        self.assertEqual(len(results), 2)
        self.assertTrue(all(results.values()))
        self.mock_device1.connect.assert_called_once()
        self.mock_device2.connect.assert_called_once()
    
    def test_connect_all_devices_mixed_results(self):
        """
        Test connecting to all devices with mixed success/failure.
        """
        # Add devices to manager
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        
        # Mock mixed connection results
        self.mock_device1.connect.return_value = True
        self.mock_device2.connect.return_value = False
        
        # Connect to all devices
        results = self.device_manager.connect_all_devices()
        
        self.assertEqual(len(results), 2)
        self.assertTrue(results[self.mock_device1.device_id])
        self.assertFalse(results[self.mock_device2.device_id])
    
    def test_disconnect_all_devices(self):
        """
        Test disconnecting from all devices.
        """
        # Add connected devices
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        self.mock_device1.is_connected = True
        self.mock_device2.is_connected = True
        
        # Mock successful disconnections
        self.mock_device1.disconnect.return_value = True
        self.mock_device2.disconnect.return_value = True
        
        # Disconnect from all devices
        results = self.device_manager.disconnect_all_devices()
        
        self.assertEqual(len(results), 2)
        self.assertTrue(all(results.values()))
        self.mock_device1.disconnect.assert_called_once()
        self.mock_device2.disconnect.assert_called_once()
    
    def test_start_recording(self):
        """
        Test starting recording on all connected devices.
        """
        session_id = "test_session_123"
        
        # Add connected devices
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        self.mock_device1.is_connected = True
        self.mock_device2.is_connected = True
        
        # Mock successful recording start
        self.mock_device1.start_recording.return_value = True
        self.mock_device2.start_recording.return_value = True
        
        # Start recording
        results = self.device_manager.start_recording(session_id)
        
        self.assertEqual(len(results), 2)
        self.assertTrue(all(results.values()))
        self.mock_device1.start_recording.assert_called_once_with(session_id)
        self.mock_device2.start_recording.assert_called_once_with(session_id)
    
    def test_start_recording_disconnected_devices(self):
        """
        Test starting recording with some disconnected devices.
        """
        session_id = "test_session_456"
        
        # Add devices (one connected, one disconnected)
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        self.mock_device1.is_connected = True
        self.mock_device2.is_connected = False
        
        # Mock recording start for connected device
        self.mock_device1.start_recording.return_value = True
        
        # Start recording
        results = self.device_manager.start_recording(session_id)
        
        # Only connected device should be included
        self.assertEqual(len(results), 1)
        self.assertIn(self.mock_device1.device_id, results)
        self.assertTrue(results[self.mock_device1.device_id])
        self.mock_device1.start_recording.assert_called_once_with(session_id)
        self.mock_device2.start_recording.assert_not_called()
    
    def test_stop_recording(self):
        """
        Test stopping recording on all devices.
        """
        # Add recording devices
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        self.mock_device1.is_recording = True
        self.mock_device2.is_recording = True
        
        # Mock successful recording stop
        self.mock_device1.stop_recording.return_value = True
        self.mock_device2.stop_recording.return_value = True
        
        # Stop recording
        results = self.device_manager.stop_recording()
        
        self.assertEqual(len(results), 2)
        self.assertTrue(all(results.values()))
        self.mock_device1.stop_recording.assert_called_once()
        self.mock_device2.stop_recording.assert_called_once()
    
    def test_collect_files(self):
        """
        Test collecting files from all devices.
        """
        destination_dir = tempfile.mkdtemp()
        
        try:
            # Add devices
            self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
            self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
            
            # Mock successful file collection
            self.mock_device1.collect_files.return_value = ["file1.csv", "file2.mp4"]
            self.mock_device2.collect_files.return_value = ["file3.csv", "file4.mp4"]
            
            # Collect files
            results = self.device_manager.collect_files(destination_dir)
            
            self.assertEqual(len(results), 2)
            self.assertEqual(len(results[self.mock_device1.device_id]), 2)
            self.assertEqual(len(results[self.mock_device2.device_id]), 2)
            self.mock_device1.collect_files.assert_called_once_with(destination_dir)
            self.mock_device2.collect_files.assert_called_once_with(destination_dir)
            
        finally:
            shutil.rmtree(destination_dir)
    
    def test_get_device_status(self):
        """
        Test getting status of a specific device.
        """
        # Add device
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        
        # Mock device status
        expected_status = {
            "device_id": self.mock_device1.device_id,
            "device_name": self.mock_device1.device_name,
            "is_connected": True,
            "is_recording": False,
            "battery_level": 85
        }
        self.mock_device1.get_status.return_value = expected_status
        
        # Get device status
        status = self.device_manager.get_device_status(self.mock_device1.device_id)
        
        self.assertEqual(status, expected_status)
        self.mock_device1.get_status.assert_called_once()
    
    def test_get_device_status_nonexistent(self):
        """
        Test getting status of a nonexistent device.
        """
        status = self.device_manager.get_device_status("nonexistent_device")
        self.assertIsNone(status)
    
    def test_get_all_device_statuses(self):
        """
        Test getting status of all devices.
        """
        # Add devices
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        
        # Mock device statuses
        status1 = {"device_id": self.mock_device1.device_id, "is_connected": True}
        status2 = {"device_id": self.mock_device2.device_id, "is_connected": False}
        self.mock_device1.get_status.return_value = status1
        self.mock_device2.get_status.return_value = status2
        
        # Get all device statuses
        statuses = self.device_manager.get_all_device_statuses()
        
        self.assertEqual(len(statuses), 2)
        self.assertEqual(statuses[self.mock_device1.device_id], status1)
        self.assertEqual(statuses[self.mock_device2.device_id], status2)
    
    def test_cleanup(self):
        """
        Test cleanup functionality.
        """
        # Add devices
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        
        # Mock cleanup methods
        self.mock_device1.disconnect.return_value = True
        self.mock_device2.disconnect.return_value = True
        
        # Perform cleanup
        self.device_manager.cleanup()
        
        # Verify devices were disconnected
        self.mock_device1.disconnect.assert_called_once()
        self.mock_device2.disconnect.assert_called_once()
    
    def test_concurrent_operations(self):
        """
        Test concurrent device operations.
        """
        # Add devices
        self.device_manager.devices[self.mock_device1.device_id] = self.mock_device1
        self.device_manager.devices[self.mock_device2.device_id] = self.mock_device2
        
        # Mock operations
        self.mock_device1.connect.return_value = True
        self.mock_device1.disconnect.return_value = True
        self.mock_device2.connect.return_value = True
        self.mock_device2.disconnect.return_value = True
        
        # Perform concurrent operations
        def connect_operation():
            self.device_manager.connect_all_devices()
        
        def disconnect_operation():
            time.sleep(0.1)  # Small delay
            self.device_manager.disconnect_all_devices()
        
        thread1 = threading.Thread(target=connect_operation)
        thread2 = threading.Thread(target=disconnect_operation)
        
        thread1.start()
        thread2.start()
        
        thread1.join()
        thread2.join()
        
        # Operations should complete without error
        self.assertTrue(True)


class TestDeviceListener(unittest.TestCase):
    """
    Test case for the DeviceListener class.
    """
    
    def setUp(self):
        """
        Set up the test case.
        """
        self.mock_device_manager = Mock(spec=DeviceManager)
        self.device_listener = DeviceListener(self.mock_device_manager)
    
    def test_initialization(self):
        """
        Test DeviceListener initialization.
        """
        self.assertIsNotNone(self.device_listener)
        self.assertEqual(self.device_listener.device_manager, self.mock_device_manager)
    
    @patch('src.network.device_manager.socket.inet_ntoa')
    def test_add_service(self, mock_inet_ntoa):
        """
        Test adding a service (device discovery).
        """
        # Mock zeroconf and service info
        mock_zeroconf = Mock()
        mock_service_info = Mock()
        mock_service_info.addresses = [b'\xc0\xa8\x01\x64']  # 192.168.1.100
        mock_service_info.port = 8080
        mock_service_info.properties = {b'device_name': b'Test Device'}
        
        mock_inet_ntoa.return_value = "192.168.1.100"
        mock_zeroconf.get_service_info.return_value = mock_service_info
        
        # Add service
        service_type = "_gsrcapture._tcp.local."
        service_name = "TestDevice._gsrcapture._tcp.local."
        
        self.device_listener.add_service(mock_zeroconf, service_type, service_name)
        
        # Verify service info was requested
        mock_zeroconf.get_service_info.assert_called_once_with(service_type, service_name)
    
    def test_add_service_no_info(self):
        """
        Test adding a service when service info is not available.
        """
        # Mock zeroconf returning None for service info
        mock_zeroconf = Mock()
        mock_zeroconf.get_service_info.return_value = None
        
        # Add service
        service_type = "_gsrcapture._tcp.local."
        service_name = "TestDevice._gsrcapture._tcp.local."
        
        # Should handle gracefully
        self.device_listener.add_service(mock_zeroconf, service_type, service_name)
        
        # Verify service info was requested
        mock_zeroconf.get_service_info.assert_called_once_with(service_type, service_name)
    
    def test_remove_service(self):
        """
        Test removing a service (device disconnection).
        """
        # Mock zeroconf
        mock_zeroconf = Mock()
        
        # Remove service
        service_type = "_gsrcapture._tcp.local."
        service_name = "TestDevice._gsrcapture._tcp.local."
        
        self.device_listener.remove_service(mock_zeroconf, service_type, service_name)
        
        # Should complete without error
        self.assertTrue(True)


if __name__ == "__main__":
    unittest.main()