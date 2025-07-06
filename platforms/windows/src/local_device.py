"""
LocalDevice class for managing PC hardware as a first-class data acquisition node.

This module implements the LocalDevice class that makes the PC's hardware
look and act exactly like a remote Android device, providing a unified
interface for the device manager.
"""

import csv
import os
import time
from typing import Dict, List, Optional

import cv2
from PySide6.QtCore import QObject, Signal

from hardware.shimmer_pc import ShimmerPC
from hardware.webcam_pc import WebcamPC
from utils.logger import get_logger


class LocalDevice(QObject):
    """
    LocalDevice class that manages PC hardware as a unified device.
    
    This class provides the same public interface as the remote Device class,
    allowing the DeviceManager to treat local PC hardware identically to
    remote Android devices.
    """
    
    # Signals matching the Device class interface
    status_updated = Signal(object)  # Device status updates
    connected = Signal()
    disconnected = Signal()
    recording_started = Signal()
    recording_stopped = Signal()
    error_occurred = Signal(str)
    
    def __init__(self, parent=None):
        super().__init__(parent)
        
        # Device identification (matching Device class interface)
        self.id = "Local-PC"
        self.name = "Local PC"
        self.address = "localhost"
        self.port = 0
        self.device_type = "pc"
        self.capabilities = ["GSR/PPG", "Brio 4K"]
        
        # Hardware drivers
        self.shimmer = ShimmerPC(parent=self)
        self.webcam = WebcamPC(parent=self)
        
        # Recording state
        self._is_connected = False
        self._is_recording = False
        self._session_id = None
        self._output_directory = None
        self._enabled_sensors = []  # List of enabled sensors for current session
        
        # File writers
        self._gsr_file = None
        self._video_file = None
        
        self.logger = get_logger(self.__class__.__name__)
        
        # Connect hardware signals
        self._connect_hardware_signals()
    
    def _connect_hardware_signals(self):
        """Connect signals from hardware drivers to local device signals."""
        # Shimmer signals
        self.shimmer.connected.connect(self._on_shimmer_connected)
        self.shimmer.disconnected.connect(self._on_shimmer_disconnected)
        self.shimmer.data_received.connect(self._on_shimmer_data)
        self.shimmer.error.connect(self._on_shimmer_error)
        
        # Webcam signals
        self.webcam.connected.connect(self._on_webcam_connected)
        self.webcam.disconnected.connect(self._on_webcam_disconnected)
        self.webcam.frame_received.connect(self._on_webcam_frame)
        self.webcam.error.connect(self._on_webcam_error)
    
    @property
    def is_connected(self):
        """Return True if device is connected."""
        return self._is_connected
    
    @property
    def is_recording(self):
        """Return True if device is recording."""
        return self._is_recording
    
    def connect(self):
        """Connect to all PC hardware."""
        try:
            self.logger.info("Connecting to local PC hardware")
            
            # Connect to Shimmer sensor
            self.shimmer.connect()
            
            # Connect to webcam
            self.webcam.connect()
            
            # Check if at least one device connected
            if self.shimmer.is_connected or self.webcam.is_connected:
                self._is_connected = True
                self.logger.info("Local PC hardware connected")
                self.connected.emit()
                self._emit_status_update()
            else:
                self.error_occurred.emit("Failed to connect to any local hardware")
                
        except Exception as e:
            self.logger.error(f"Error connecting to local hardware: {e}")
            self.error_occurred.emit(str(e))
    
    def disconnect(self):
        """Disconnect from all PC hardware."""
        try:
            self.logger.info("Disconnecting from local PC hardware")
            
            # Stop recording if active
            if self._is_recording:
                self.stop_recording()
            
            # Disconnect hardware
            self.shimmer.disconnect()
            self.webcam.disconnect()
            
            self._is_connected = False
            self.logger.info("Local PC hardware disconnected")
            self.disconnected.emit()
            self._emit_status_update()
            
        except Exception as e:
            self.logger.error(f"Error disconnecting from local hardware: {e}")
            self.error_occurred.emit(str(e))
    
    def start_recording(self, session_id, enabled_sensors=None):
        """
        Start recording from enabled sensors.
        
        Args:
            session_id (str): Unique session identifier
            enabled_sensors (list): List of enabled sensor names
        """
        if not self._is_connected:
            self.error_occurred.emit("Local device not connected")
            return
        
        if self._is_recording:
            self.error_occurred.emit("Local device already recording")
            return
        
        try:
            self._session_id = session_id
            self._enabled_sensors = enabled_sensors or self.capabilities
            self._output_directory = self._create_session_directory(session_id)
            
            self.logger.info(f"Starting recording for session {session_id} with sensors: {self._enabled_sensors}")
            
            # Start recording for enabled sensors
            if "GSR/PPG" in self._enabled_sensors and self.shimmer.is_connected:
                gsr_file = os.path.join(self._output_directory, f"{session_id}_gsr_data.csv")
                self.shimmer.start_recording(gsr_file)
                self.shimmer.start_streaming()
            
            if "Brio 4K" in self._enabled_sensors and self.webcam.is_connected:
                video_file = os.path.join(self._output_directory, f"{session_id}_brio_video.mp4")
                self.webcam.start_recording(video_file)
                self.webcam.start_streaming()
            
            self._is_recording = True
            self.logger.info(f"Started recording to {self._output_directory}")
            self.recording_started.emit()
            self._emit_status_update()
            
        except Exception as e:
            self.logger.error(f"Error starting recording: {e}")
            self.error_occurred.emit(str(e))
    
    def stop_recording(self):
        """Stop recording from all sensors."""
        if not self._is_recording:
            return
        
        try:
            self.logger.info("Stopping recording")
            
            # Stop Shimmer recording
            if self.shimmer.is_streaming:
                self.shimmer.stop_streaming()
                self.shimmer.stop_recording()
            
            # Stop webcam recording
            if self.webcam.is_streaming:
                self.webcam.stop_streaming()
                self.webcam.stop_recording()
            
            self._is_recording = False
            self._session_id = None
            self._enabled_sensors = []
            
            self.logger.info("Stopped recording")
            self.recording_stopped.emit()
            self._emit_status_update()
            
        except Exception as e:
            self.logger.error(f"Error stopping recording: {e}")
            self.error_occurred.emit(str(e))
    
    def collect_files(self, destination_dir):
        """
        Collect recorded files (no-op for local device since files are already local).
        
        Args:
            destination_dir (str): Destination directory (ignored for local device)
            
        Returns:
            list: List of local file paths
        """
        if not self._output_directory or not os.path.exists(self._output_directory):
            return []
        
        # Return list of files in the output directory
        files = []
        for filename in os.listdir(self._output_directory):
            file_path = os.path.join(self._output_directory, filename)
            if os.path.isfile(file_path):
                files.append(file_path)
        
        self.logger.info(f"Local files available: {files}")
        return files
    
    def get_status(self):
        """
        Get device status information.
        
        Returns:
            dict: Device status information
        """
        return {
            'id': self.id,
            'name': self.name,
            'address': self.address,
            'device_type': self.device_type,
            'is_connected': self._is_connected,
            'is_recording': self._is_recording,
            'capabilities': self.capabilities,
            'enabled_sensors': self._enabled_sensors,
            'session_id': self._session_id,
            'hardware_status': {
                'shimmer': {
                    'connected': self.shimmer.is_connected,
                    'streaming': self.shimmer.is_streaming
                },
                'webcam': {
                    'connected': self.webcam.is_connected,
                    'streaming': self.webcam.is_streaming
                }
            }
        }
    
    def _create_session_directory(self, session_id):
        """
        Create directory for session files.
        
        Args:
            session_id (str): Session identifier
            
        Returns:
            str: Path to session directory
        """
        # Create sessions directory if it doesn't exist
        sessions_dir = os.path.join(os.path.dirname(__file__), '..', 'sessions')
        os.makedirs(sessions_dir, exist_ok=True)
        
        # Create session-specific directory
        session_dir = os.path.join(sessions_dir, f"session_{session_id}")
        os.makedirs(session_dir, exist_ok=True)
        
        return session_dir
    
    def _emit_status_update(self):
        """Emit status update signal."""
        self.status_updated.emit(self)
    
    # Hardware signal handlers
    def _on_shimmer_connected(self):
        """Handle Shimmer connection."""
        self.logger.info("Shimmer sensor connected")
        self._emit_status_update()
    
    def _on_shimmer_disconnected(self):
        """Handle Shimmer disconnection."""
        self.logger.info("Shimmer sensor disconnected")
        self._emit_status_update()
    
    def _on_shimmer_data(self, data):
        """Handle Shimmer data received."""
        # Data is automatically written to CSV by the ShimmerPC driver
        pass
    
    def _on_shimmer_error(self, error_msg):
        """Handle Shimmer error."""
        self.logger.error(f"Shimmer error: {error_msg}")
        self.error_occurred.emit(f"Shimmer: {error_msg}")
    
    def _on_webcam_connected(self):
        """Handle webcam connection."""
        self.logger.info("Webcam connected")
        self._emit_status_update()
    
    def _on_webcam_disconnected(self):
        """Handle webcam disconnection."""
        self.logger.info("Webcam disconnected")
        self._emit_status_update()
    
    def _on_webcam_frame(self, frame):
        """Handle webcam frame received."""
        # Frames are automatically written to video by the WebcamPC driver
        pass
    
    def _on_webcam_error(self, error_msg):
        """Handle webcam error."""
        self.logger.error(f"Webcam error: {error_msg}")
        self.error_occurred.emit(f"Webcam: {error_msg}")
    
    def __str__(self):
        """String representation of the device."""
        return f"LocalDevice(id={self.id}, connected={self._is_connected}, recording={self._is_recording})"