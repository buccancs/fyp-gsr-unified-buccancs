"""
Test suite for LocalDevice class.

This module contains unit tests for the LocalDevice class that manages
PC hardware as a unified device.
"""

import os
import tempfile
import unittest
from unittest.mock import MagicMock, Mock, patch

import sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from local_device import LocalDevice


class TestLocalDevice(unittest.TestCase):
    """Test cases for LocalDevice class."""
    
    def setUp(self):
        """Set up test fixtures."""
        # Mock the hardware drivers to avoid actual hardware dependencies
        with patch('local_device.ShimmerPC') as mock_shimmer, \
             patch('local_device.WebcamPC') as mock_webcam:
            
            self.mock_shimmer = Mock()
            self.mock_webcam = Mock()
            mock_shimmer.return_value = self.mock_shimmer
            mock_webcam.return_value = self.mock_webcam
            
            # Set up mock properties
            self.mock_shimmer.is_connected = False
            self.mock_shimmer.is_streaming = False
            self.mock_webcam.is_connected = False
            self.mock_webcam.is_streaming = False
            
            self.local_device = LocalDevice()
    
    def test_initialization(self):
        """Test LocalDevice initialization."""
        self.assertEqual(self.local_device.id, "Local-PC")
        self.assertEqual(self.local_device.name, "Local PC")
        self.assertEqual(self.local_device.address, "localhost")
        self.assertEqual(self.local_device.device_type, "pc")
        self.assertEqual(self.local_device.capabilities, ["GSR/PPG", "Brio 4K"])
        self.assertFalse(self.local_device.is_connected)
        self.assertFalse(self.local_device.is_recording)
    
    def test_connect_success(self):
        """Test successful connection to hardware."""
        # Mock successful hardware connections
        self.mock_shimmer.is_connected = True
        self.mock_webcam.is_connected = True
        
        # Connect should succeed if at least one device connects
        self.local_device.connect()
        
        # Verify connection calls were made
        self.mock_shimmer.connect.assert_called_once()
        self.mock_webcam.connect.assert_called_once()
    
    def test_connect_partial_success(self):
        """Test connection with only one hardware device available."""
        # Mock partial success (only shimmer connects)
        self.mock_shimmer.is_connected = True
        self.mock_webcam.is_connected = False
        
        self.local_device.connect()
        
        # Should still be considered connected if at least one device works
        self.assertTrue(self.local_device.is_connected)
    
    def test_connect_failure(self):
        """Test connection failure when no hardware is available."""
        # Mock connection failure
        self.mock_shimmer.is_connected = False
        self.mock_webcam.is_connected = False
        
        self.local_device.connect()
        
        # Should not be connected if no hardware is available
        self.assertFalse(self.local_device.is_connected)
    
    def test_disconnect(self):
        """Test disconnection from hardware."""
        # Set up connected state
        self.local_device._is_connected = True
        
        self.local_device.disconnect()
        
        # Verify disconnect calls were made
        self.mock_shimmer.disconnect.assert_called_once()
        self.mock_webcam.disconnect.assert_called_once()
        self.assertFalse(self.local_device.is_connected)
    
    @patch('local_device.os.makedirs')
    @patch('local_device.os.path.join')
    def test_start_recording_with_all_sensors(self, mock_join, mock_makedirs):
        """Test starting recording with all sensors enabled."""
        # Set up connected state
        self.local_device._is_connected = True
        self.mock_shimmer.is_connected = True
        self.mock_webcam.is_connected = True
        
        # Mock file path creation
        mock_join.return_value = "/fake/path/session_test_gsr_data.csv"
        
        session_id = "test_session"
        enabled_sensors = ["GSR/PPG", "Brio 4K"]
        
        self.local_device.start_recording(session_id, enabled_sensors)
        
        # Verify recording started on both devices
        self.mock_shimmer.start_recording.assert_called_once()
        self.mock_shimmer.start_streaming.assert_called_once()
        self.mock_webcam.start_recording.assert_called_once()
        self.mock_webcam.start_streaming.assert_called_once()
        
        self.assertTrue(self.local_device.is_recording)
        self.assertEqual(self.local_device._session_id, session_id)
        self.assertEqual(self.local_device._enabled_sensors, enabled_sensors)
    
    def test_start_recording_with_selected_sensors(self):
        """Test starting recording with only selected sensors."""
        # Set up connected state
        self.local_device._is_connected = True
        self.mock_shimmer.is_connected = True
        self.mock_webcam.is_connected = True
        
        session_id = "test_session"
        enabled_sensors = ["GSR/PPG"]  # Only GSR/PPG enabled
        
        with patch.object(self.local_device, '_create_session_directory') as mock_create_dir:
            mock_create_dir.return_value = "/fake/session/dir"
            
            self.local_device.start_recording(session_id, enabled_sensors)
        
        # Verify only shimmer recording started
        self.mock_shimmer.start_recording.assert_called_once()
        self.mock_shimmer.start_streaming.assert_called_once()
        self.mock_webcam.start_recording.assert_not_called()
        self.mock_webcam.start_streaming.assert_not_called()
    
    def test_start_recording_not_connected(self):
        """Test starting recording when not connected."""
        # Device not connected
        self.local_device._is_connected = False
        
        session_id = "test_session"
        
        self.local_device.start_recording(session_id)
        
        # Should not start recording
        self.assertFalse(self.local_device.is_recording)
        self.mock_shimmer.start_recording.assert_not_called()
        self.mock_webcam.start_recording.assert_not_called()
    
    def test_stop_recording(self):
        """Test stopping recording."""
        # Set up recording state
        self.local_device._is_recording = True
        self.mock_shimmer.is_streaming = True
        self.mock_webcam.is_streaming = True
        
        self.local_device.stop_recording()
        
        # Verify recording stopped on both devices
        self.mock_shimmer.stop_streaming.assert_called_once()
        self.mock_shimmer.stop_recording.assert_called_once()
        self.mock_webcam.stop_streaming.assert_called_once()
        self.mock_webcam.stop_recording.assert_called_once()
        
        self.assertFalse(self.local_device.is_recording)
        self.assertIsNone(self.local_device._session_id)
        self.assertEqual(self.local_device._enabled_sensors, [])
    
    @patch('local_device.os.path.exists')
    @patch('local_device.os.listdir')
    @patch('local_device.os.path.isfile')
    def test_collect_files(self, mock_isfile, mock_listdir, mock_exists):
        """Test collecting files from local device."""
        # Mock file system
        mock_exists.return_value = True
        mock_listdir.return_value = ['test_gsr_data.csv', 'test_brio_video.mp4']
        mock_isfile.return_value = True
        
        self.local_device._output_directory = "/fake/session/dir"
        
        files = self.local_device.collect_files("/destination")
        
        # Should return list of local files
        self.assertEqual(len(files), 2)
        self.assertIn('test_gsr_data.csv', files[0])
        self.assertIn('test_brio_video.mp4', files[1])
    
    def test_get_status(self):
        """Test getting device status."""
        # Set up some state
        self.local_device._is_connected = True
        self.local_device._is_recording = False
        self.local_device._session_id = "test_session"
        self.local_device._enabled_sensors = ["GSR/PPG"]
        
        # Mock hardware status
        self.mock_shimmer.is_connected = True
        self.mock_shimmer.is_streaming = False
        self.mock_webcam.is_connected = False
        self.mock_webcam.is_streaming = False
        
        status = self.local_device.get_status()
        
        # Verify status information
        self.assertEqual(status['id'], "Local-PC")
        self.assertEqual(status['name'], "Local PC")
        self.assertEqual(status['device_type'], "pc")
        self.assertTrue(status['is_connected'])
        self.assertFalse(status['is_recording'])
        self.assertEqual(status['capabilities'], ["GSR/PPG", "Brio 4K"])
        self.assertEqual(status['enabled_sensors'], ["GSR/PPG"])
        self.assertEqual(status['session_id'], "test_session")
        
        # Verify hardware status
        self.assertTrue(status['hardware_status']['shimmer']['connected'])
        self.assertFalse(status['hardware_status']['shimmer']['streaming'])
        self.assertFalse(status['hardware_status']['webcam']['connected'])
        self.assertFalse(status['hardware_status']['webcam']['streaming'])
    
    def test_signal_connections(self):
        """Test that hardware signals are properly connected."""
        # This test verifies that the signal connections are set up
        # In a real test, we would verify signal emissions, but for now
        # we just check that the methods exist
        self.assertTrue(hasattr(self.local_device, '_on_shimmer_connected'))
        self.assertTrue(hasattr(self.local_device, '_on_shimmer_disconnected'))
        self.assertTrue(hasattr(self.local_device, '_on_shimmer_data'))
        self.assertTrue(hasattr(self.local_device, '_on_shimmer_error'))
        self.assertTrue(hasattr(self.local_device, '_on_webcam_connected'))
        self.assertTrue(hasattr(self.local_device, '_on_webcam_disconnected'))
        self.assertTrue(hasattr(self.local_device, '_on_webcam_frame'))
        self.assertTrue(hasattr(self.local_device, '_on_webcam_error'))
    
    def test_string_representation(self):
        """Test string representation of LocalDevice."""
        self.local_device._is_connected = True
        self.local_device._is_recording = False
        
        str_repr = str(self.local_device)
        
        self.assertIn("Local-PC", str_repr)
        self.assertIn("connected=True", str_repr)
        self.assertIn("recording=False", str_repr)


if __name__ == '__main__':
    unittest.main()