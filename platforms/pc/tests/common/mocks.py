"""Common mock objects and mock factories.

This module provides reusable mock objects that can be shared across different test modules.
"""

from unittest.mock import Mock, MagicMock, patch
from typing import Any, Dict, List, Optional, Union


class MockShimmerDevice:
    """Mock Shimmer device for testing."""
    
    def __init__(self, com_port: str = "COM3"):
        self.com_port = com_port
        self.connected = False
        self.streaming = False
        self.recording = False
        self.data_buffer = []
        
    def connect(self) -> bool:
        """Mock connect to Shimmer device."""
        self.connected = True
        return True
        
    def disconnect(self) -> bool:
        """Mock disconnect from Shimmer device."""
        self.connected = False
        self.streaming = False
        self.recording = False
        return True
        
    def start_streaming(self) -> bool:
        """Mock start streaming data."""
        if self.connected:
            self.streaming = True
            return True
        return False
        
    def stop_streaming(self) -> bool:
        """Mock stop streaming data."""
        self.streaming = False
        return True
        
    def start_recording(self, filename: str = "test.csv") -> bool:
        """Mock start recording data."""
        if self.connected:
            self.recording = True
            return True
        return False
        
    def stop_recording(self) -> bool:
        """Mock stop recording data."""
        self.recording = False
        return True
        
    def get_data(self) -> List[Dict[str, Any]]:
        """Mock get buffered data."""
        return self.data_buffer.copy()


class MockWebcamDevice:
    """Mock webcam device for testing."""
    
    def __init__(self, camera_index: int = 0):
        self.camera_index = camera_index
        self.connected = False
        self.streaming = False
        self.recording = False
        self.width = 640
        self.height = 480
        self.fps = 30
        
    def connect(self) -> bool:
        """Mock connect to webcam."""
        self.connected = True
        return True
        
    def disconnect(self) -> bool:
        """Mock disconnect from webcam."""
        self.connected = False
        self.streaming = False
        self.recording = False
        return True
        
    def start_streaming(self) -> bool:
        """Mock start streaming video."""
        if self.connected:
            self.streaming = True
            return True
        return False
        
    def stop_streaming(self) -> bool:
        """Mock stop streaming video."""
        self.streaming = False
        return True
        
    def start_recording(self, filename: str = "test.mp4") -> bool:
        """Mock start recording video."""
        if self.connected:
            self.recording = True
            return True
        return False
        
    def stop_recording(self) -> bool:
        """Mock stop recording video."""
        self.recording = False
        return True
        
    def get_frame(self) -> Optional[bytes]:
        """Mock get current frame."""
        if self.streaming:
            # Return mock frame data
            frame_size = self.width * self.height * 3
            return b'\x00' * frame_size
        return None
        
    def set_resolution(self, width: int, height: int) -> bool:
        """Mock set camera resolution."""
        self.width = width
        self.height = height
        return True


class MockDeviceManager:
    """Mock device manager for testing."""
    
    def __init__(self):
        self.devices = {}
        self.connected_devices = set()
        self.recording_devices = set()
        
    def discover_devices(self) -> List[Dict[str, Any]]:
        """Mock device discovery."""
        return [
            {
                'id': 'shimmer_001',
                'type': 'shimmer',
                'name': 'Shimmer Device 001',
                'ip_address': '192.168.1.100',
                'port': 8080
            },
            {
                'id': 'webcam_001',
                'type': 'webcam',
                'name': 'USB Camera 001',
                'device_index': 0
            }
        ]
        
    def add_device(self, device_info: Dict[str, Any]) -> bool:
        """Mock add device."""
        device_id = device_info['id']
        self.devices[device_id] = device_info
        return True
        
    def connect_device(self, device_id: str) -> bool:
        """Mock connect to device."""
        if device_id in self.devices:
            self.connected_devices.add(device_id)
            return True
        return False
        
    def disconnect_device(self, device_id: str) -> bool:
        """Mock disconnect from device."""
        self.connected_devices.discard(device_id)
        self.recording_devices.discard(device_id)
        return True
        
    def start_recording(self, device_id: str) -> bool:
        """Mock start recording on device."""
        if device_id in self.connected_devices:
            self.recording_devices.add(device_id)
            return True
        return False
        
    def stop_recording(self, device_id: str) -> bool:
        """Mock stop recording on device."""
        self.recording_devices.discard(device_id)
        return True
        
    def get_device_status(self, device_id: str) -> Dict[str, Any]:
        """Mock get device status."""
        if device_id not in self.devices:
            return {}
            
        return {
            'id': device_id,
            'connected': device_id in self.connected_devices,
            'recording': device_id in self.recording_devices,
            'status': 'active' if device_id in self.connected_devices else 'inactive'
        }


class MockCppBackend:
    """Mock C++ backend for testing."""
    
    def __init__(self):
        self.initialized = True
        
    def get_version(self) -> str:
        """Mock get backend version."""
        return "1.0.0-test"
        
    def test_connection(self) -> str:
        """Mock test connection."""
        return "Connection OK"
        
    def create_shimmer(self, com_port: str) -> MockShimmerDevice:
        """Mock create Shimmer device."""
        return MockShimmerDevice(com_port)
        
    def create_webcam(self, camera_index: int) -> MockWebcamDevice:
        """Mock create webcam device."""
        return MockWebcamDevice(camera_index)


# Mock factory functions
def create_mock_shimmer_device(**kwargs) -> MockShimmerDevice:
    """Create a mock Shimmer device with optional parameters."""
    return MockShimmerDevice(**kwargs)


def create_mock_webcam_device(**kwargs) -> MockWebcamDevice:
    """Create a mock webcam device with optional parameters."""
    return MockWebcamDevice(**kwargs)


def create_mock_device_manager(**kwargs) -> MockDeviceManager:
    """Create a mock device manager with optional parameters."""
    return MockDeviceManager(**kwargs)


def create_mock_cpp_backend(**kwargs) -> MockCppBackend:
    """Create a mock C++ backend with optional parameters."""
    return MockCppBackend(**kwargs)


# Common mock patches
def patch_shimmer_import():
    """Patch Shimmer import for testing."""
    return patch('pyshimmer.ShimmerBluetooth', return_value=create_mock_shimmer_device())


def patch_opencv_import():
    """Patch OpenCV import for testing."""
    mock_cv2 = MagicMock()
    mock_cv2.VideoCapture.return_value = create_mock_webcam_device()
    return patch.dict('sys.modules', {'cv2': mock_cv2})


def patch_cpp_backend_import():
    """Patch C++ backend import for testing."""
    return patch('_hardware_backend', create_mock_cpp_backend())