"""Common test fixtures and test data.

This module provides reusable test fixtures, mock data, and test configurations
that can be shared across different test modules.
"""

import os
import tempfile
import unittest
from typing import Any, Dict, List, Optional, Union
from unittest.mock import Mock, MagicMock


class BaseTestCase(unittest.TestCase):
    """Base test case with common setup and teardown functionality."""
    
    def setUp(self) -> None:
        """Set up common test fixtures."""
        self.temp_dir = tempfile.mkdtemp()
        self.test_data_dir = os.path.join(self.temp_dir, 'test_data')
        os.makedirs(self.test_data_dir, exist_ok=True)
        
    def tearDown(self) -> None:
        """Clean up test fixtures."""
        import shutil
        if os.path.exists(self.temp_dir):
            shutil.rmtree(self.temp_dir)


class MockDevice:
    """Mock device for testing."""
    
    def __init__(self, device_id: str = "test_device", device_type: str = "shimmer"):
        self.device_id = device_id
        self.device_type = device_type
        self.connected = False
        self.recording = False
        self.data = []
        
    def connect(self) -> bool:
        """Mock connect method."""
        self.connected = True
        return True
        
    def disconnect(self) -> bool:
        """Mock disconnect method."""
        self.connected = False
        return True
        
    def start_recording(self) -> bool:
        """Mock start recording method."""
        if self.connected:
            self.recording = True
            return True
        return False
        
    def stop_recording(self) -> bool:
        """Mock stop recording method."""
        self.recording = False
        return True
        
    def get_data(self) -> List[Dict[str, Any]]:
        """Mock get data method."""
        return self.data.copy()


class TestDataGenerator:
    """Generate test data for various scenarios."""
    
    @staticmethod
    def create_shimmer_data(num_samples: int = 100) -> List[Dict[str, Any]]:
        """Create mock Shimmer sensor data."""
        import time
        data = []
        base_time = time.time()
        
        for i in range(num_samples):
            sample = {
                'timestamp': base_time + i * 0.01,  # 100Hz sampling
                'accel_x': 0.1 * i,
                'accel_y': 0.2 * i,
                'accel_z': 9.8 + 0.1 * i,
                'gyro_x': 0.05 * i,
                'gyro_y': 0.03 * i,
                'gyro_z': 0.02 * i,
                'gsr': 1000 + 10 * i
            }
            data.append(sample)
        
        return data
    
    @staticmethod
    def create_webcam_frame(width: int = 640, height: int = 480) -> bytes:
        """Create mock webcam frame data."""
        # Create a simple test pattern
        frame_size = width * height * 3  # RGB
        return b'\x00' * frame_size
    
    @staticmethod
    def create_device_info(device_id: str = "test_device") -> Dict[str, Any]:
        """Create mock device information."""
        return {
            'id': device_id,
            'name': f'Test Device {device_id}',
            'type': 'shimmer',
            'ip_address': '192.168.1.100',
            'port': 8080,
            'status': 'disconnected',
            'capabilities': ['gsr', 'accelerometer', 'gyroscope'],
            'firmware_version': '1.0.0',
            'battery_level': 85
        }


# Common test configurations
TEST_CONFIG = {
    'shimmer': {
        'com_port': 'COM3',
        'baud_rate': 115200,
        'sampling_rate': 100,
        'sensors': ['gsr', 'accel', 'gyro']
    },
    'webcam': {
        'camera_index': 0,
        'width': 640,
        'height': 480,
        'fps': 30
    },
    'network': {
        'discovery_timeout': 5.0,
        'connection_timeout': 10.0,
        'retry_attempts': 3
    }
}

# Common test data paths
TEST_PATHS = {
    'config_file': 'test_config.ini',
    'log_file': 'test.log',
    'data_dir': 'test_data',
    'output_dir': 'test_output'
}