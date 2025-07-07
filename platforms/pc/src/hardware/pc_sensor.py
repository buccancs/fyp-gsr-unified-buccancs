"""
Abstract base classes for PC-connected hardware sensors and cameras.

This module defines the hardware abstraction layer for PC-connected devices,
providing common interfaces and signals for sensors and cameras.
"""

import logging
from abc import ABC, abstractmethod

from PySide6.QtCore import QObject, Signal

from utils.logger import get_logger


class PCConnectedSensor(QObject, ABC):
    """
    Abstract base class for PC-connected sensors.
    
    This class defines the common interface for all PC-connected sensors
    such as GSR, PPG, accelerometer, etc.
    """
    
    # Common signals for all PC sensors
    connected = Signal()
    disconnected = Signal()
    data_received = Signal(dict)  # Dictionary containing sensor data
    error = Signal(str)  # Error message
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.logger = get_logger(self.__class__.__name__)
        self._is_connected = False
        self._is_streaming = False
    
    @property
    def is_connected(self):
        """Return True if sensor is connected."""
        return self._is_connected
    
    @property
    def is_streaming(self):
        """Return True if sensor is actively streaming data."""
        return self._is_streaming
    
    @abstractmethod
    def connect(self):
        """
        Connect to the sensor.
        
        Should emit connected signal on success or error signal on failure.
        """
        pass
    
    @abstractmethod
    def start_streaming(self):
        """
        Start streaming data from the sensor.
        
        Should emit data_received signals with sensor data.
        """
        pass
    
    @abstractmethod
    def stop_streaming(self):
        """Stop streaming data from the sensor."""
        pass
    
    @abstractmethod
    def disconnect(self):
        """
        Disconnect from the sensor.
        
        Should emit disconnected signal.
        """
        pass


class PCConnectedCamera(QObject, ABC):
    """
    Abstract base class for PC-connected cameras.
    
    This class defines the common interface for all PC-connected cameras
    such as webcams, thermal cameras, etc.
    """
    
    # Common signals for all PC cameras
    connected = Signal()
    disconnected = Signal()
    frame_received = Signal(object)  # Raw frame data (e.g., NumPy array)
    error = Signal(str)  # Error message
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.logger = get_logger(self.__class__.__name__)
        self._is_connected = False
        self._is_streaming = False
    
    @property
    def is_connected(self):
        """Return True if camera is connected."""
        return self._is_connected
    
    @property
    def is_streaming(self):
        """Return True if camera is actively streaming frames."""
        return self._is_streaming
    
    @abstractmethod
    def connect(self):
        """
        Connect to the camera.
        
        Should emit connected signal on success or error signal on failure.
        """
        pass
    
    @abstractmethod
    def start_streaming(self):
        """
        Start streaming frames from the camera.
        
        Should emit frame_received signals with frame data.
        """
        pass
    
    @abstractmethod
    def stop_streaming(self):
        """Stop streaming frames from the camera."""
        pass
    
    @abstractmethod
    def disconnect(self):
        """
        Disconnect from the camera.
        
        Should emit disconnected signal.
        """
        pass
    
    @abstractmethod
    def set_resolution(self, width, height):
        """
        Set the camera resolution.
        
        Args:
            width (int): Frame width in pixels
            height (int): Frame height in pixels
        """
        pass