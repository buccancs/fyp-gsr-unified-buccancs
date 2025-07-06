"""
Logitech Brio 4K webcam driver for PC.

This module implements the concrete driver for Logitech Brio 4K webcam
connected to the PC via USB using opencv-python.
"""

import configparser
import os
import threading
import time
from typing import Optional

import cv2
import numpy as np
from PySide6.QtCore import QThread, Signal

from hardware.pc_sensor import PCConnectedCamera
from utils.logger import get_logger


class WebcamCaptureThread(QThread):
    """
    Dedicated thread for capturing webcam frames.
    
    This thread continuously reads frames from the camera to avoid
    blocking the UI.
    """
    
    frame_received = Signal(object)  # NumPy array
    error_occurred = Signal(str)
    
    def __init__(self, camera_index, parent=None):
        super().__init__(parent)
        self.camera_index = camera_index
        self.running = False
        self.cap = None
        self.logger = get_logger(self.__class__.__name__)
    
    def run(self):
        """Main thread loop for capturing frames."""
        self.running = True
        self.logger.info("Webcam capture thread started")
        
        try:
            # Initialize camera
            self.cap = cv2.VideoCapture(self.camera_index)
            
            if not self.cap.isOpened():
                self.error_occurred.emit(f"Failed to open camera {self.camera_index}")
                return
            
            # Set 4K resolution for Logitech Brio
            self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, 3840)
            self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 2160)
            
            # Set other properties for better quality
            self.cap.set(cv2.CAP_PROP_FPS, 30)
            self.cap.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc('M', 'J', 'P', 'G'))
            
            self.logger.info(f"Camera initialized with resolution: "
                           f"{int(self.cap.get(cv2.CAP_PROP_FRAME_WIDTH))}x"
                           f"{int(self.cap.get(cv2.CAP_PROP_FRAME_HEIGHT))}")
            
            while self.running:
                ret, frame = self.cap.read()
                
                if ret and frame is not None:
                    # Emit the frame
                    self.frame_received.emit(frame)
                else:
                    self.logger.warning("Failed to read frame from camera")
                    self.msleep(100)  # Wait before retrying
                
                # Small delay to control frame rate
                self.msleep(33)  # ~30 FPS
                
        except Exception as e:
            self.logger.error(f"Error in webcam capture thread: {e}")
            self.error_occurred.emit(str(e))
        
        finally:
            if self.cap:
                self.cap.release()
                self.cap = None
    
    def stop(self):
        """Stop the capture thread."""
        self.running = False
        self.logger.info("Stopping webcam capture thread")
    
    def set_resolution(self, width, height):
        """Set camera resolution."""
        if self.cap and self.cap.isOpened():
            self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, width)
            self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, height)
            self.logger.info(f"Set camera resolution to {width}x{height}")


class WebcamPC(PCConnectedCamera):
    """
    Concrete implementation of Logitech Brio 4K webcam driver for PC.
    
    This class uses opencv-python to capture frames from the webcam
    connected via USB.
    """
    
    def __init__(self, camera_index=None, parent=None):
        super().__init__(parent)
        self.camera_index = camera_index or self._load_camera_index_from_config()
        self.capture_thread = None
        self.video_writer = None
        self.recording_file = None
        self.logger = get_logger(self.__class__.__name__)
        
        # Check if OpenCV is available
        try:
            cv2.VideoCapture(0)
        except Exception as e:
            self.logger.error(f"OpenCV not available: {e}")
    
    def _load_camera_index_from_config(self):
        """Load camera index from configuration file."""
        try:
            config = configparser.ConfigParser()
            config_path = os.path.join(os.path.dirname(__file__), '..', '..', 'config.ini')
            
            if os.path.exists(config_path):
                config.read(config_path)
                return config.getint('Hardware', 'brio_camera_index', fallback=0)
            else:
                self.logger.warning("Config file not found, using default camera index 0")
                return 0
        except Exception as e:
            self.logger.error(f"Error loading config: {e}")
            return 0
    
    def connect(self):
        """Connect to the webcam."""
        try:
            self.logger.info(f"Connecting to webcam at index {self.camera_index}")
            
            # Test camera connection
            test_cap = cv2.VideoCapture(self.camera_index)
            
            if test_cap.isOpened():
                # Get camera info
                width = int(test_cap.get(cv2.CAP_PROP_FRAME_WIDTH))
                height = int(test_cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
                fps = int(test_cap.get(cv2.CAP_PROP_FPS))
                
                self.logger.info(f"Camera connected: {width}x{height} @ {fps}fps")
                
                test_cap.release()
                self._is_connected = True
                self.connected.emit()
            else:
                self.error.emit(f"Failed to connect to camera at index {self.camera_index}")
                
        except Exception as e:
            self.logger.error(f"Error connecting to webcam: {e}")
            self.error.emit(str(e))
    
    def start_streaming(self):
        """Start streaming frames from the webcam."""
        if not self._is_connected:
            self.error.emit("Webcam not connected")
            return
        
        try:
            # Start the capture thread
            self.capture_thread = WebcamCaptureThread(self.camera_index, self)
            self.capture_thread.frame_received.connect(self._on_frame_received)
            self.capture_thread.error_occurred.connect(self._on_thread_error)
            self.capture_thread.start()
            
            self._is_streaming = True
            self.logger.info("Started webcam streaming")
            
        except Exception as e:
            self.logger.error(f"Error starting webcam streaming: {e}")
            self.error.emit(str(e))
    
    def stop_streaming(self):
        """Stop streaming frames from the webcam."""
        try:
            self._is_streaming = False
            
            # Stop the capture thread
            if self.capture_thread:
                self.capture_thread.stop()
                self.capture_thread.wait(3000)  # Wait up to 3 seconds
                self.capture_thread = None
            
            # Close video file if recording
            self._close_video_file()
            
            self.logger.info("Stopped webcam streaming")
            
        except Exception as e:
            self.logger.error(f"Error stopping webcam streaming: {e}")
            self.error.emit(str(e))
    
    def disconnect(self):
        """Disconnect from the webcam."""
        try:
            # Stop streaming first
            if self._is_streaming:
                self.stop_streaming()
            
            self._is_connected = False
            self.logger.info("Disconnected from webcam")
            self.disconnected.emit()
            
        except Exception as e:
            self.logger.error(f"Error disconnecting from webcam: {e}")
            self.error.emit(str(e))
    
    def set_resolution(self, width, height):
        """Set the camera resolution."""
        if self.capture_thread:
            self.capture_thread.set_resolution(width, height)
        self.logger.info(f"Set webcam resolution to {width}x{height}")
    
    def start_recording(self, output_file, fps=30):
        """
        Start recording video to a file.
        
        Args:
            output_file (str): Path to the output video file
            fps (int): Frames per second for recording
        """
        try:
            # Define codec and create VideoWriter
            fourcc = cv2.VideoWriter_fourcc(*'mp4v')
            
            # Use 4K resolution for Brio
            self.video_writer = cv2.VideoWriter(
                output_file, fourcc, fps, (3840, 2160)
            )
            
            if not self.video_writer.isOpened():
                self.error.emit(f"Failed to create video writer for {output_file}")
                return
            
            self.recording_file = output_file
            self.logger.info(f"Started recording to {output_file}")
            
        except Exception as e:
            self.logger.error(f"Error starting recording: {e}")
            self.error.emit(str(e))
    
    def stop_recording(self):
        """Stop recording video."""
        self._close_video_file()
        self.logger.info("Stopped recording")
    
    def _close_video_file(self):
        """Close the video file if open."""
        if self.video_writer:
            self.video_writer.release()
            self.video_writer = None
            self.recording_file = None
    
    def _on_frame_received(self, frame):
        """Handle frame received from the capture thread."""
        # Write to video file if recording
        if self.video_writer and self.video_writer.isOpened():
            self.video_writer.write(frame)
        
        # Emit the frame signal
        self.frame_received.emit(frame)
    
    def _on_thread_error(self, error_msg):
        """Handle errors from the capture thread."""
        self.error.emit(error_msg)
    
    def get_camera_info(self):
        """
        Get camera information.
        
        Returns:
            dict: Camera information including resolution, fps, etc.
        """
        if not self._is_connected:
            return {}
        
        try:
            test_cap = cv2.VideoCapture(self.camera_index)
            if test_cap.isOpened():
                info = {
                    'width': int(test_cap.get(cv2.CAP_PROP_FRAME_WIDTH)),
                    'height': int(test_cap.get(cv2.CAP_PROP_FRAME_HEIGHT)),
                    'fps': int(test_cap.get(cv2.CAP_PROP_FPS)),
                    'fourcc': int(test_cap.get(cv2.CAP_PROP_FOURCC)),
                    'backend': test_cap.getBackendName()
                }
                test_cap.release()
                return info
            else:
                return {}
        except Exception as e:
            self.logger.error(f"Error getting camera info: {e}")
            return {}