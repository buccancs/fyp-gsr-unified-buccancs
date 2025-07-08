"""Test suite for PC hardware drivers.

This module contains unit tests for the ShimmerPC and WebcamPC drivers
that use mocked dependencies to avoid requiring actual hardware.
"""

import os
import tempfile
import unittest
from unittest.mock import MagicMock, Mock, patch, mock_open

import sys
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))


class TestShimmerPC(unittest.TestCase):
    """Test cases for ShimmerPC driver."""
    
    def setUp(self) -> None:
        """Set up test fixtures."""
        # Mock pyshimmer to avoid hardware dependency
        self.pyshimmer_mock = Mock()
        self.shimmer_device_mock = Mock()
        self.pyshimmer_mock.ShimmerBluetooth.return_value = self.shimmer_device_mock
        
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            from hardware.shimmer_pc import ShimmerPC
            self.ShimmerPC = ShimmerPC
    
    @patch('hardware.shimmer_pc.configparser.ConfigParser')
    @patch('hardware.shimmer_pc.os.path.exists')
    def test_initialization_with_config(self, mock_exists, mock_config_parser) -> None:
        """Test ShimmerPC initialization with config file."""
        # Mock config file exists and has COM port setting
        mock_exists.return_value = True
        mock_config = Mock()
        mock_config.get.return_value = 'COM3'
        mock_config_parser.return_value = mock_config
        
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC()
        
        self.assertEqual(shimmer.com_port, 'COM3')
        self.assertFalse(shimmer.is_connected)
        self.assertFalse(shimmer.is_streaming)
    
    @patch('hardware.shimmer_pc.configparser.ConfigParser')
    @patch('hardware.shimmer_pc.os.path.exists')
    def test_initialization_without_config(self, mock_exists, mock_config_parser) -> None:
        """Test ShimmerPC initialization without config file."""
        # Mock config file doesn't exist
        mock_exists.return_value = False
        
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC()
        
        self.assertEqual(shimmer.com_port, 'COM5')  # Default value
    
    def test_connect_success(self) -> None:
        """Test successful connection to Shimmer."""
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC(com_port='COM3')
        
        # Mock successful connection
        self.shimmer_device_mock.connect.return_value = True
        
        shimmer.connect()
        
        self.assertTrue(shimmer.is_connected)
        self.shimmer_device_mock.connect.assert_called_once()
    
    def test_connect_failure(self) -> None:
        """Test failed connection to Shimmer."""
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC(com_port='COM3')
        
        # Mock failed connection
        self.shimmer_device_mock.connect.return_value = False
        
        shimmer.connect()
        
        self.assertFalse(shimmer.is_connected)
    
    def test_connect_without_pyshimmer(self) -> None:
        """Test connection when pyshimmer is not available."""
        with patch.dict('sys.modules', {'pyshimmer': None}):
            from hardware.shimmer_pc import ShimmerPC
            shimmer = ShimmerPC(com_port='COM3')
        
        shimmer.connect()
        
        self.assertFalse(shimmer.is_connected)
    
    def test_start_streaming_success(self) -> None:
        """Test successful start of streaming."""
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC(com_port='COM3')
        
        # Set up connected state
        shimmer._is_connected = True
        shimmer.shimmer_device = self.shimmer_device_mock
        self.shimmer_device_mock.start_streaming.return_value = True
        
        with patch('hardware.shimmer_pc.ShimmerDataThread') as mock_thread_class:
            mock_thread = Mock()
            mock_thread_class.return_value = mock_thread
            
            shimmer.start_streaming()
        
        self.assertTrue(shimmer.is_streaming)
        self.shimmer_device_mock.start_streaming.assert_called_once()
        mock_thread.start.assert_called_once()
    
    def test_start_streaming_not_connected(self) -> None:
        """Test start streaming when not connected."""
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC(com_port='COM3')
        
        # Not connected
        shimmer._is_connected = False
        
        shimmer.start_streaming()
        
        self.assertFalse(shimmer.is_streaming)
    
    def test_stop_streaming(self) -> None:
        """Test stopping streaming."""
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC(com_port='COM3')
        
        # Set up streaming state
        shimmer._is_streaming = True
        shimmer.shimmer_device = self.shimmer_device_mock
        
        mock_thread = Mock()
        shimmer.data_thread = mock_thread
        
        shimmer.stop_streaming()
        
        self.assertFalse(shimmer.is_streaming)
        mock_thread.stop.assert_called_once()
        mock_thread.wait.assert_called_once_with(3000)
        self.shimmer_device_mock.stop_streaming.assert_called_once()
    
    def test_disconnect(self) -> None:
        """Test disconnection from Shimmer."""
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC(com_port='COM3')
        
        # Set up connected state
        shimmer._is_connected = True
        shimmer.shimmer_device = self.shimmer_device_mock
        
        shimmer.disconnect()
        
        self.assertFalse(shimmer.is_connected)
        self.shimmer_device_mock.disconnect.assert_called_once()
    
    @patch('builtins.open', new_callable=mock_open)
    @patch('hardware.shimmer_pc.csv.writer')
    def test_start_recording(self, mock_csv_writer, mock_file_open) -> None:
        """Test starting CSV recording."""
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC(com_port='COM3')
        
        mock_writer = Mock()
        mock_csv_writer.return_value = mock_writer
        
        shimmer.start_recording('/fake/path/test.csv')
        
        mock_file_open.assert_called_once_with('/fake/path/test.csv', 'w', newline='')
        mock_writer.writerow.assert_called_once_with(['timestamp', 'gsr', 'ppg'])
    
    def test_stop_recording(self) -> None:
        """Test stopping CSV recording."""
        with patch.dict('sys.modules', {'pyshimmer': self.pyshimmer_mock}):
            shimmer = self.ShimmerPC(com_port='COM3')
        
        # Set up recording state
        mock_file = Mock()
        shimmer.csv_file = mock_file
        shimmer.csv_writer = Mock()
        
        shimmer.stop_recording()
        
        mock_file.close.assert_called_once()
        self.assertIsNone(shimmer.csv_file)
        self.assertIsNone(shimmer.csv_writer)


class TestWebcamPC(unittest.TestCase):
    """Test cases for WebcamPC driver."""
    
    def setUp(self) -> None:
        """Set up test fixtures."""
        # Mock cv2 to avoid hardware dependency
        self.cv2_mock = Mock()
        self.cv2_mock.VideoCapture.return_value = Mock()
        
    @patch('hardware.webcam_pc.configparser.ConfigParser')
    @patch('hardware.webcam_pc.os.path.exists')
    def test_initialization_with_config(self, mock_exists, mock_config_parser) -> None:
        """Test WebcamPC initialization with config file."""
        # Mock config file exists and has camera index setting
        mock_exists.return_value = True
        mock_config = Mock()
        mock_config.getint.return_value = 1
        mock_config_parser.return_value = mock_config
        
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC()
        
        self.assertEqual(webcam.camera_index, 1)
        self.assertFalse(webcam.is_connected)
        self.assertFalse(webcam.is_streaming)
    
    @patch('hardware.webcam_pc.configparser.ConfigParser')
    @patch('hardware.webcam_pc.os.path.exists')
    def test_initialization_without_config(self, mock_exists, mock_config_parser) -> None:
        """Test WebcamPC initialization without config file."""
        # Mock config file doesn't exist
        mock_exists.return_value = False
        
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC()
        
        self.assertEqual(webcam.camera_index, 0)  # Default value
    
    def test_connect_success(self) -> None:
        """Test successful connection to webcam."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC(camera_index=0)
        
        # Mock successful camera connection
        mock_cap = Mock()
        mock_cap.isOpened.return_value = True
        mock_cap.get.side_effect = [1920, 1080, 30]  # width, height, fps
        self.cv2_mock.VideoCapture.return_value = mock_cap
        
        webcam.connect()
        
        self.assertTrue(webcam.is_connected)
        mock_cap.release.assert_called_once()
    
    def test_connect_failure(self) -> None:
        """Test failed connection to webcam."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC(camera_index=0)
        
        # Mock failed camera connection
        mock_cap = Mock()
        mock_cap.isOpened.return_value = False
        self.cv2_mock.VideoCapture.return_value = mock_cap
        
        webcam.connect()
        
        self.assertFalse(webcam.is_connected)
    
    def test_start_streaming_success(self) -> None:
        """Test successful start of streaming."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC(camera_index=0)
        
        # Set up connected state
        webcam._is_connected = True
        
        with patch('hardware.webcam_pc.WebcamCaptureThread') as mock_thread_class:
            mock_thread = Mock()
            mock_thread_class.return_value = mock_thread
            
            webcam.start_streaming()
        
        self.assertTrue(webcam.is_streaming)
        mock_thread.start.assert_called_once()
    
    def test_start_streaming_not_connected(self) -> None:
        """Test start streaming when not connected."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC(camera_index=0)
        
        # Not connected
        webcam._is_connected = False
        
        webcam.start_streaming()
        
        self.assertFalse(webcam.is_streaming)
    
    def test_stop_streaming(self) -> None:
        """Test stopping streaming."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC(camera_index=0)
        
        # Set up streaming state
        webcam._is_streaming = True
        
        mock_thread = Mock()
        webcam.capture_thread = mock_thread
        
        webcam.stop_streaming()
        
        self.assertFalse(webcam.is_streaming)
        mock_thread.stop.assert_called_once()
        mock_thread.wait.assert_called_once_with(3000)
    
    def test_disconnect(self) -> None:
        """Test disconnection from webcam."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC(camera_index=0)
        
        # Set up connected state
        webcam._is_connected = True
        
        webcam.disconnect()
        
        self.assertFalse(webcam.is_connected)
    
    def test_set_resolution(self) -> None:
        """Test setting camera resolution."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC(camera_index=0)
        
        mock_thread = Mock()
        webcam.capture_thread = mock_thread
        
        webcam.set_resolution(1920, 1080)
        
        mock_thread.set_resolution.assert_called_once_with(1920, 1080)
    
    def test_start_recording(self) -> None:
        """Test starting video recording."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC(camera_index=0)
        
        # Mock video writer
        mock_writer = Mock()
        mock_writer.isOpened.return_value = True
        self.cv2_mock.VideoWriter.return_value = mock_writer
        self.cv2_mock.VideoWriter_fourcc.return_value = 'mp4v'
        
        webcam.start_recording('/fake/path/test.mp4')
        
        self.cv2_mock.VideoWriter.assert_called_once_with(
            '/fake/path/test.mp4', 'mp4v', 30, (3840, 2160)
        )
        self.assertEqual(webcam.recording_file, '/fake/path/test.mp4')
    
    def test_stop_recording(self) -> None:
        """Test stopping video recording."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
            webcam = WebcamPC(camera_index=0)
        
        # Set up recording state
        mock_writer = Mock()
        webcam.video_writer = mock_writer
        webcam.recording_file = '/fake/path/test.mp4'
        
        webcam.stop_recording()
        
        mock_writer.release.assert_called_once()
        self.assertIsNone(webcam.video_writer)
        self.assertIsNone(webcam.recording_file)
    
    def test_get_camera_info(self) -> None:
        """Test getting camera information."""
        with patch.dict('sys.modules', {'cv2': self.cv2_mock}):
            from hardware.webcam_pc import WebcamPC
from typing import Any, Dict, List, Optional, Union
            webcam = WebcamPC(camera_index=0)
        
        # Set up connected state
        webcam._is_connected = True
        
        # Mock camera info
        mock_cap = Mock()
        mock_cap.isOpened.return_value = True
        mock_cap.get.side_effect = [1920, 1080, 30, 1234, 'DirectShow']
        mock_cap.getBackendName.return_value = 'DirectShow'
        self.cv2_mock.VideoCapture.return_value = mock_cap
        
        info = webcam.get_camera_info()
        
        self.assertEqual(info['width'], 1920)
        self.assertEqual(info['height'], 1080)
        self.assertEqual(info['fps'], 30)
        self.assertEqual(info['backend'], 'DirectShow')
        mock_cap.release.assert_called_once()


if __name__ == '__main__':
    unittest.main()