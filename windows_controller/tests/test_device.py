#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Comprehensive unit tests for the Device class.
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

from src.network.device import Device

class TestDevice(unittest.TestCase):
    """
    Test case for the Device class.
    """
    
    def setUp(self):
        """
        Set up the test case.
        """
        self.device_id = "test_device_001"
        self.device_name = "Test Android Device"
        self.ip_address = "192.168.1.100"
        self.port = 8080
        
        self.device = Device(
            device_id=self.device_id,
            device_name=self.device_name,
            ip_address=self.ip_address,
            port=self.port
        )
    
    def tearDown(self):
        """
        Clean up after the test case.
        """
        if self.device.is_connected:
            self.device.disconnect()
    
    def test_initialization(self):
        """
        Test Device initialization.
        """
        self.assertEqual(self.device.device_id, self.device_id)
        self.assertEqual(self.device.device_name, self.device_name)
        self.assertEqual(self.device.ip_address, self.ip_address)
        self.assertEqual(self.device.port, self.port)
        self.assertFalse(self.device.is_connected)
        self.assertFalse(self.device.is_recording)
        self.assertIsNone(self.device.socket)
    
    @patch('socket.socket')
    def test_connect_success(self, mock_socket_class):
        """
        Test successful device connection.
        """
        # Mock socket
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        
        # Connect to device
        result = self.device.connect()
        
        self.assertTrue(result)
        self.assertTrue(self.device.is_connected)
        self.assertEqual(self.device.socket, mock_socket)
        mock_socket.connect.assert_called_once_with((self.ip_address, self.port))
    
    @patch('socket.socket')
    def test_connect_failure(self, mock_socket_class):
        """
        Test device connection failure.
        """
        # Mock socket connection failure
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.side_effect = ConnectionRefusedError("Connection refused")
        
        # Attempt to connect
        result = self.device.connect()
        
        self.assertFalse(result)
        self.assertFalse(self.device.is_connected)
        self.assertIsNone(self.device.socket)
    
    @patch('socket.socket')
    def test_connect_timeout(self, mock_socket_class):
        """
        Test device connection timeout.
        """
        # Mock socket timeout
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.side_effect = socket.timeout("Connection timed out")
        
        # Attempt to connect
        result = self.device.connect()
        
        self.assertFalse(result)
        self.assertFalse(self.device.is_connected)
        self.assertIsNone(self.device.socket)
    
    @patch('socket.socket')
    def test_disconnect(self, mock_socket_class):
        """
        Test device disconnection.
        """
        # Mock socket and establish connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        
        # Connect first
        self.device.connect()
        self.assertTrue(self.device.is_connected)
        
        # Disconnect
        result = self.device.disconnect()
        
        self.assertTrue(result)
        self.assertFalse(self.device.is_connected)
        self.assertIsNone(self.device.socket)
        mock_socket.close.assert_called_once()
    
    def test_disconnect_not_connected(self):
        """
        Test disconnecting when not connected.
        """
        # Attempt to disconnect when not connected
        result = self.device.disconnect()
        
        self.assertTrue(result)  # Should succeed gracefully
        self.assertFalse(self.device.is_connected)
    
    @patch('socket.socket')
    def test_send_command(self, mock_socket_class):
        """
        Test sending commands to device.
        """
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        
        # Mock socket send and receive
        mock_socket.send.return_value = 20  # bytes sent
        mock_socket.recv.return_value = b'{"status": "OK"}'
        
        # Connect and send command
        self.device.connect()
        result = self.device.send_command("START_RECORDING")
        
        self.assertTrue(result)
        mock_socket.send.assert_called()
        mock_socket.recv.assert_called()
    
    @patch('socket.socket')
    def test_send_command_not_connected(self, mock_socket_class):
        """
        Test sending command when not connected.
        """
        # Attempt to send command without connection
        result = self.device.send_command("START_RECORDING")
        
        self.assertFalse(result)
    
    @patch('socket.socket')
    def test_send_command_failure(self, mock_socket_class):
        """
        Test command sending failure.
        """
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        mock_socket.send.side_effect = BrokenPipeError("Broken pipe")
        
        # Connect and attempt to send command
        self.device.connect()
        result = self.device.send_command("START_RECORDING")
        
        self.assertFalse(result)
    
    @patch('socket.socket')
    def test_start_recording(self, mock_socket_class):
        """
        Test starting recording on device.
        """
        session_id = "test_session_123"
        
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        mock_socket.send.return_value = 50
        mock_socket.recv.return_value = b'{"status": "RECORDING_STARTED"}'
        
        # Connect and start recording
        self.device.connect()
        result = self.device.start_recording(session_id)
        
        self.assertTrue(result)
        self.assertTrue(self.device.is_recording)
        mock_socket.send.assert_called()
    
    @patch('socket.socket')
    def test_start_recording_not_connected(self, mock_socket_class):
        """
        Test starting recording when not connected.
        """
        session_id = "test_session_456"
        
        # Attempt to start recording without connection
        result = self.device.start_recording(session_id)
        
        self.assertFalse(result)
        self.assertFalse(self.device.is_recording)
    
    @patch('socket.socket')
    def test_stop_recording(self, mock_socket_class):
        """
        Test stopping recording on device.
        """
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        mock_socket.send.return_value = 30
        mock_socket.recv.return_value = b'{"status": "RECORDING_STOPPED"}'
        
        # Connect, start recording, then stop
        self.device.connect()
        self.device.is_recording = True  # Simulate recording state
        result = self.device.stop_recording()
        
        self.assertTrue(result)
        self.assertFalse(self.device.is_recording)
        mock_socket.send.assert_called()
    
    @patch('socket.socket')
    def test_stop_recording_not_recording(self, mock_socket_class):
        """
        Test stopping recording when not recording.
        """
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        
        # Connect but don't start recording
        self.device.connect()
        result = self.device.stop_recording()
        
        self.assertTrue(result)  # Should succeed gracefully
        self.assertFalse(self.device.is_recording)
    
    @patch('socket.socket')
    def test_get_status(self, mock_socket_class):
        """
        Test getting device status.
        """
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        mock_socket.send.return_value = 20
        
        # Mock status response
        status_response = {
            "device_id": self.device_id,
            "device_name": self.device_name,
            "is_connected": True,
            "is_recording": False,
            "battery_level": 85,
            "storage_available": "2.5GB"
        }
        mock_socket.recv.return_value = json.dumps(status_response).encode()
        
        # Connect and get status
        self.device.connect()
        status = self.device.get_status()
        
        self.assertIsNotNone(status)
        self.assertEqual(status["device_id"], self.device_id)
        self.assertEqual(status["battery_level"], 85)
        mock_socket.send.assert_called()
    
    @patch('socket.socket')
    def test_get_status_not_connected(self, mock_socket_class):
        """
        Test getting status when not connected.
        """
        # Attempt to get status without connection
        status = self.device.get_status()
        
        # Should return basic status
        self.assertIsNotNone(status)
        self.assertEqual(status["device_id"], self.device_id)
        self.assertEqual(status["is_connected"], False)
    
    @patch('socket.socket')
    def test_collect_files(self, mock_socket_class):
        """
        Test collecting files from device.
        """
        destination_dir = tempfile.mkdtemp()
        
        try:
            # Mock socket and connection
            mock_socket = Mock()
            mock_socket_class.return_value = mock_socket
            mock_socket.connect.return_value = None
            mock_socket.send.return_value = 30
            
            # Mock file list response
            file_list = ["session_123_gsr_data.csv", "session_123_thermal_video.mp4"]
            mock_socket.recv.return_value = json.dumps({"files": file_list}).encode()
            
            # Connect and collect files
            self.device.connect()
            collected_files = self.device.collect_files(destination_dir)
            
            self.assertIsNotNone(collected_files)
            self.assertIsInstance(collected_files, list)
            mock_socket.send.assert_called()
            
        finally:
            shutil.rmtree(destination_dir)
    
    @patch('socket.socket')
    def test_collect_files_not_connected(self, mock_socket_class):
        """
        Test collecting files when not connected.
        """
        destination_dir = tempfile.mkdtemp()
        
        try:
            # Attempt to collect files without connection
            collected_files = self.device.collect_files(destination_dir)
            
            self.assertEqual(collected_files, [])
            
        finally:
            shutil.rmtree(destination_dir)
    
    @patch('socket.socket')
    def test_ping(self, mock_socket_class):
        """
        Test pinging device.
        """
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        mock_socket.send.return_value = 10
        mock_socket.recv.return_value = b'{"status": "PONG"}'
        
        # Connect and ping
        self.device.connect()
        result = self.device.ping()
        
        self.assertTrue(result)
        mock_socket.send.assert_called()
    
    @patch('socket.socket')
    def test_ping_not_connected(self, mock_socket_class):
        """
        Test pinging when not connected.
        """
        # Attempt to ping without connection
        result = self.device.ping()
        
        self.assertFalse(result)
    
    def test_device_equality(self):
        """
        Test device equality comparison.
        """
        # Create another device with same ID
        device2 = Device(
            device_id=self.device_id,
            device_name="Different Name",
            ip_address="192.168.1.200",
            port=9090
        )
        
        # Create device with different ID
        device3 = Device(
            device_id="different_device",
            device_name=self.device_name,
            ip_address=self.ip_address,
            port=self.port
        )
        
        self.assertEqual(self.device, device2)  # Same device_id
        self.assertNotEqual(self.device, device3)  # Different device_id
    
    def test_device_string_representation(self):
        """
        Test device string representation.
        """
        device_str = str(self.device)
        
        self.assertIn(self.device_id, device_str)
        self.assertIn(self.device_name, device_str)
        self.assertIn(self.ip_address, device_str)
        self.assertIn(str(self.port), device_str)
    
    @patch('socket.socket')
    def test_concurrent_operations(self, mock_socket_class):
        """
        Test concurrent device operations.
        """
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        mock_socket.send.return_value = 20
        mock_socket.recv.return_value = b'{"status": "OK"}'
        
        # Connect device
        self.device.connect()
        
        # Define concurrent operations
        def ping_operation():
            for _ in range(5):
                self.device.ping()
                time.sleep(0.01)
        
        def status_operation():
            for _ in range(5):
                self.device.get_status()
                time.sleep(0.01)
        
        # Run operations concurrently
        thread1 = threading.Thread(target=ping_operation)
        thread2 = threading.Thread(target=status_operation)
        
        thread1.start()
        thread2.start()
        
        thread1.join()
        thread2.join()
        
        # Operations should complete without error
        self.assertTrue(True)
    
    @patch('socket.socket')
    def test_connection_recovery(self, mock_socket_class):
        """
        Test connection recovery after failure.
        """
        # Mock socket
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        
        # First connection succeeds
        mock_socket.connect.return_value = None
        self.assertTrue(self.device.connect())
        self.assertTrue(self.device.is_connected)
        
        # Simulate connection loss
        mock_socket.send.side_effect = BrokenPipeError("Connection lost")
        result = self.device.send_command("PING")
        self.assertFalse(result)
        
        # Reset mock for reconnection
        mock_socket.send.side_effect = None
        mock_socket.send.return_value = 10
        mock_socket.recv.return_value = b'{"status": "OK"}'
        
        # Reconnect should work
        self.device.disconnect()
        self.assertTrue(self.device.connect())
        self.assertTrue(self.device.send_command("PING"))
    
    @patch('socket.socket')
    def test_invalid_responses(self, mock_socket_class):
        """
        Test handling of invalid responses from device.
        """
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        mock_socket.send.return_value = 20
        
        # Test invalid JSON response
        mock_socket.recv.return_value = b'invalid json response'
        
        self.device.connect()
        result = self.device.send_command("GET_STATUS")
        
        # Should handle invalid response gracefully
        self.assertFalse(result)
    
    @patch('socket.socket')
    def test_large_data_transfer(self, mock_socket_class):
        """
        Test handling of large data transfers.
        """
        # Mock socket and connection
        mock_socket = Mock()
        mock_socket_class.return_value = mock_socket
        mock_socket.connect.return_value = None
        mock_socket.send.return_value = 50
        
        # Mock large response
        large_response = {"data": "x" * 10000}  # Large response
        mock_socket.recv.return_value = json.dumps(large_response).encode()
        
        self.device.connect()
        result = self.device.send_command("GET_LARGE_DATA")
        
        # Should handle large responses
        self.assertTrue(result)


if __name__ == "__main__":
    unittest.main()