"""Logitech Brio 4K webcam driver for PC.

This module implements the concrete driver for Logitech Brio 4K webcam
connected to the PC via USB using opencv-python.
"""

import configparser
import os
from typing import Optional

import cv2
import numpy as np
from PySide6.QtCore import QThread, Signal

from hardware.pc_sensor import PCConnectedCamera
from utils.logger import get_logger

try:
    import _hardware_backend
except ImportError:
    _hardware_backend = None


class WebcamCaptureThread(QThread):
    """Dedicated thread for capturing webcam frames.

    This thread polls the C++ NativeWebcam backend's non-blocking getData() method
    to retrieve high-precision timestamped frames.
    """

    frame_received = Signal(object)  # NumPy array
    error_occurred = Signal(str)

    def __init__(self, webcam_device, use_cpp_backend=True, parent=None) -> None:
        super().__init__(parent)
        self.webcam_device = webcam_device
        self.use_cpp_backend = use_cpp_backend
        self.running = False
        self.cap = None
        self.logger = get_logger(self.__class__.__name__)

    def run(self) -> None:
        """Main thread loop for capturing frames."""
        self.running = True
        self.logger.info("Webcam capture thread started")

        while self.running:
            try:
                if self.webcam_device:
                    if self.use_cpp_backend and hasattr(self.webcam_device, 'is_running') and self.webcam_device.is_running():
                        # Use C++ backend - non-blocking getData()
                        timestamped_frames_list = self.webcam_device.get_data()

                        if timestamped_frames_list:
                            for timestamped_frame in timestamped_frames_list:
                                # Parse the C++ timestamped frame and emit signal
                                frame = self._parse_cpp_webcam_frame(timestamped_frame)
                                if frame is not None:
                                    self.frame_received.emit(frame)

                        # Small delay to prevent overwhelming the system
                        self.msleep(10)  # 10ms delay for C++ backend

                    elif not self.use_cpp_backend and hasattr(self.webcam_device, 'isOpened') and self.webcam_device.isOpened():
                        # Fallback to OpenCV - blocking read()
                        ret, frame = self.webcam_device.read()

                        if ret and frame is not None:
                            # Emit the frame
                            self.frame_received.emit(frame)
                        else:
                            self.logger.warning("Failed to read frame from camera")
                            self.msleep(100)  # Wait before retrying

                        # Small delay to control frame rate
                        self.msleep(33)  # ~30 FPS for OpenCV
                    else:
                        # If not connected/running, wait longer before checking again
                        self.msleep(100)
                else:
                    # No device, wait before checking again
                    self.msleep(100)

            except Exception as e:
                self.logger.error(f"Error in webcam capture thread: {e}")
                self.error_occurred.emit(str(e))
                self.msleep(1000)  # Wait 1 second before retrying

    def stop(self) -> None:
        """Stop the capture thread."""
        self.running = False
        self.logger.info("Stopping webcam capture thread")

    def _parse_cpp_webcam_frame(self, timestamped_frame) -> None:
        """Parse C++ timestamped webcam frame into a NumPy array.

        Args:
            timestamped_frame: TimestampedFrame object from C++ backend

        Returns:
            numpy.ndarray: Frame data as NumPy array, or None if parsing fails
        """
        try:
            # Get the frame data from the C++ timestamped frame
            # The frame property should return a numpy array
            frame_data = timestamped_frame.frame

            if frame_data is not None and frame_data.size > 0:
                return frame_data
            else:
                return None

        except Exception as e:
            self.logger.error(f"Error parsing C++ webcam frame: {e}")
            return None

    def set_resolution(self, width, height) -> None:
        """Set camera resolution."""
        if self.cap and self.cap.isOpened():
            self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, width)
            self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, height)
            self.logger.info(f"Set camera resolution to {width}x{height}")


class WebcamPC(PCConnectedCamera):
    """Concrete implementation of Logitech Brio 4K webcam driver for PC.

    This class uses the C++ NativeWebcam backend for high-precision timing,
    with fallback to opencv-python for compatibility.
    """

    def __init__(self, camera_index=None, parent=None) -> None:
        super().__init__(parent)
        self.camera_index = camera_index or self._load_camera_index_from_config()
        self.webcam_device = None
        self.capture_thread = None
        self.video_writer = None
        self.recording_file = None
        self.use_cpp_backend = _hardware_backend is not None
        self.logger = get_logger(self.__class__.__name__)

        if self.use_cpp_backend:
            self.logger.info("Using C++ hardware backend for high-precision timing")
        else:
            self.logger.info("Using OpenCV fallback mode")
            # Check if OpenCV is available
            try:
                cv2.VideoCapture(0)
            except Exception as e:
                self.logger.error(f"OpenCV not available: {e}")

    def _load_camera_index_from_config(self) -> None:
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

    def connect(self) -> None:
        """Connect to the webcam."""
        if self.use_cpp_backend and _hardware_backend:
            try:
                self.logger.info(f"Connecting to webcam at index {self.camera_index} using C++ backend")

                # Initialize C++ NativeWebcam device
                self.webcam_device = _hardware_backend.NativeWebcam(self.camera_index)

                # Set 4K resolution for Logitech Brio before starting
                self.webcam_device.set_resolution(3840, 2160)

                # The C++ backend doesn't have a separate connect step - connection happens on start
                self._is_connected = True
                self.logger.info("C++ Webcam backend initialized successfully")
                self.connected.emit()

            except Exception as e:
                self.logger.error(f"Error initializing C++ Webcam backend: {e}")
                self.error.emit(str(e))

        else:
            try:
                self.logger.info(f"Connecting to webcam at index {self.camera_index} using OpenCV")

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

    def start_streaming(self) -> None:
        """Start streaming frames from the webcam."""
        if not self._is_connected or not self.webcam_device:
            self.error.emit("Webcam not connected")
            return

        try:
            if self.use_cpp_backend:
                # Start C++ backend streaming
                if self.webcam_device.start():
                    self._is_streaming = True

                    # Start the capture thread with C++ backend
                    self.capture_thread = WebcamCaptureThread(self.webcam_device, use_cpp_backend=True, parent=self)
                    self.capture_thread.frame_received.connect(self._on_frame_received)
                    self.capture_thread.error_occurred.connect(self._on_thread_error)
                    self.capture_thread.start()

                    self.logger.info("Started C++ Webcam streaming")
                else:
                    self.error.emit("Failed to start C++ Webcam streaming")
            else:
                # Start OpenCV streaming
                # Create a new VideoCapture for the thread
                opencv_device = cv2.VideoCapture(self.camera_index)

                if opencv_device.isOpened():
                    # Set 4K resolution for Logitech Brio
                    opencv_device.set(cv2.CAP_PROP_FRAME_WIDTH, 3840)
                    opencv_device.set(cv2.CAP_PROP_FRAME_HEIGHT, 2160)
                    opencv_device.set(cv2.CAP_PROP_FPS, 30)
                    opencv_device.set(cv2.CAP_PROP_FOURCC, cv2.VideoWriter_fourcc('M', 'J', 'P', 'G'))

                    self._is_streaming = True

                    # Start the capture thread with OpenCV
                    self.capture_thread = WebcamCaptureThread(opencv_device, use_cpp_backend=False, parent=self)
                    self.capture_thread.frame_received.connect(self._on_frame_received)
                    self.capture_thread.error_occurred.connect(self._on_thread_error)
                    self.capture_thread.start()

                    self.logger.info("Started OpenCV webcam streaming")
                else:
                    self.error.emit("Failed to start OpenCV webcam streaming")

        except Exception as e:
            self.logger.error(f"Error starting webcam streaming: {e}")
            self.error.emit(str(e))

    def stop_streaming(self) -> None:
        """Stop streaming frames from the webcam."""
        try:
            self._is_streaming = False

            # Stop the capture thread
            if self.capture_thread:
                self.capture_thread.stop()
                self.capture_thread.wait(3000)  # Wait up to 3 seconds
                self.capture_thread = None

            # Stop streaming on the device
            if self.webcam_device:
                if self.use_cpp_backend:
                    self.webcam_device.stop()
                # For OpenCV, the device is handled by the thread

            # Close video file if recording
            self._close_video_file()

            self.logger.info("Stopped webcam streaming")

        except Exception as e:
            self.logger.error(f"Error stopping webcam streaming: {e}")
            self.error.emit(str(e))

    def disconnect(self) -> None:
        """Disconnect from the webcam."""
        try:
            # Stop streaming first
            if self._is_streaming:
                self.stop_streaming()

            # Disconnect from device
            if self.webcam_device:
                if self.use_cpp_backend:
                    # C++ backend handles cleanup automatically
                    pass
                else:
                    # For OpenCV, release if it's still open
                    if hasattr(self.webcam_device, 'release'):
                        self.webcam_device.release()

                self.webcam_device = None

            self._is_connected = False
            self.logger.info("Disconnected from webcam")
            self.disconnected.emit()

        except Exception as e:
            self.logger.error(f"Error disconnecting from webcam: {e}")
            self.error.emit(str(e))

    def set_resolution(self, width, height) -> None:
        """Set the camera resolution."""
        if self.capture_thread:
            self.capture_thread.set_resolution(width, height)
        self.logger.info(f"Set webcam resolution to {width}x{height}")

    def start_recording(self, output_file, fps=30) -> None:
        """Start recording video to a file.

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

    def stop_recording(self) -> None:
        """Stop recording video."""
        self._close_video_file()
        self.logger.info("Stopped recording")

    def _close_video_file(self) -> None:
        """Close the video file if open."""
        if self.video_writer:
            self.video_writer.release()
            self.video_writer = None
            self.recording_file = None

    def _on_frame_received(self, frame) -> None:
        """Handle frame received from the capture thread."""
        # Write to video file if recording
        if self.video_writer and self.video_writer.isOpened():
            self.video_writer.write(frame)

        # Emit the frame signal
        self.frame_received.emit(frame)

    def _on_thread_error(self, error_msg) -> None:
        """Handle errors from the capture thread."""
        self.error.emit(error_msg)

    def get_camera_info(self) -> None:
        """Get camera information.

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
