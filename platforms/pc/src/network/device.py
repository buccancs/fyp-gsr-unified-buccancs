#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Device class for the PC Controller App.

Cross-platform support: Windows, macOS, Linux.
"""

import json
import logging
import os
import socket
import threading
import time
from typing import Any, Dict, List, Optional, Union

from PySide6.QtCore import QObject, Signal, Slot

from utils.logger import get_logger


class Device(QObject):
    """Device class for representing a connected device and handling communication with it."""

    # Define signals
    status_updated = Signal(object)
    # device, frame_type (rgb/thermal)
    video_frame_received = Signal(object, str)
    # device, frame_data (bytes), frame_type, timestamp
    video_frame_data_received = Signal(object, bytes, str, int)
    # device, gsr_value, timestamp
    gsr_data_received = Signal(object, float, int)
    # device, heart_rate, timestamp
    heart_rate_data_received = Signal(object, int, int)

    def __init__(
            self,
            id: str,
            name: str,
            address: str,
            port: int,
            device_type: str = "phone",
            capabilities: Optional[List[str]] = None) -> None:
        """Initialize the device.

        Args:
            id: The unique ID of the device.
            name: The name of the device.
            address: The IP address of the device.
            port: The port to connect to.
            device_type: The type of device (e.g. "phone").
            capabilities: A list of capabilities the device has (e.g. ["rgb", "thermal", "gsr"]).
        """
        super().__init__()

        # Set up logging
        self.logger = get_logger(__name__)

        # Store device info
        self.id = id
        self.name = name
        self.address = address
        self.port = port
        self.device_type = device_type
        self.capabilities = capabilities or []

        # Initialize connection state
        self.connected = False
        self.socket = None
        self.reader_thread = None
        self.writer_thread = None
        self.running = False

        # Initialize recording state
        self.recording = False
        self.session_id = None

        # Initialize status
        self.status = {
            "connected": False,
            "recording": False,
            "battery_level": 0,
            "storage_remaining": 0,
            "rgb_camera_active": False,
            "thermal_camera_active": False,
            "gsr_sensor_active": False,
            "error": None
        }

        self.logger.info(f"Device initialized: {self.name} ({self.id})")

    def connect(self) -> None:
        """Connect to the device.

        Returns:
            True if the connection was successful, False otherwise
        """
        self.logger.info(f"Connecting to device: {self.name} ({self.id})")

        # Check if already connected
        if self.connected:
            self.logger.warning(f"Device {self.id} is already connected")
            return True

        try:
            # Establish a socket connection to the device
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.settimeout(10.0)  # 10 second timeout
            self.socket.connect((self.address, self.port))

            self.connected = True
            self.status["connected"] = True
            self.logger.info(f"Successfully connected to {self.name} at {self.address}:{self.port}")

            # Start reader and writer threads
            self.running = True
            self.reader_thread = threading.Thread(target=self._reader_thread)
            self.reader_thread.daemon = True
            self.reader_thread.start()

            self.writer_thread = threading.Thread(target=self._writer_thread)
            self.writer_thread.daemon = True
            self.writer_thread.start()

            # Emit status update
            self.status_updated.emit(self)

            self.logger.info(f"Connected to device: {self.name} ({self.id})")
            return True
        except Exception as e:
            self.logger.error(
                f"Failed to connect to device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False

    def disconnect(self) -> None:
        """Disconnect from the device.

        Returns:
            True if the disconnection was successful, False otherwise
        """
        self.logger.info(f"Disconnecting from device: {self.name} ({self.id})")

        # Check if already disconnected
        if not self.connected:
            self.logger.warning(f"Device {self.id} is already disconnected")
            return True

        try:
            # Stop recording if it's in progress
            if self.recording:
                self.stop_recording()

            # Stop threads
            self.running = False
            if self.reader_thread:
                self.reader_thread.join(timeout=1.0)
                self.reader_thread = None

            if self.writer_thread:
                self.writer_thread.join(timeout=1.0)
                self.writer_thread = None

            # Close socket
            if self.socket:
                self.socket.close()
                self.socket = None

            # Update state
            self.connected = False
            self.status["connected"] = False

            # Emit status update
            self.status_updated.emit(self)

            self.logger.info(
                f"Disconnected from device: {self.name} ({self.id})")
            return True
        except Exception as e:
            self.logger.error(
                f"Failed to disconnect from device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False

    def send_command(self, command, data=None, timeout=5.0) -> None:
        """Send a command to the device and wait for acknowledgment.

        Args:
            command: The command to send
            data: Optional data to include with the command
            timeout: Timeout in seconds to wait for acknowledgment

        Returns:
            True if command was sent and acknowledged successfully, False otherwise
        """
        if not self.connected or not self.socket:
            self.logger.error(
                f"Cannot send command to device {self.id}: not connected")
            return False

        try:
            # Create command JSON
            command_data = {
                "command": command,
                "timestamp": time.time(),
                "session_id": self.session_id
            }

            if data:
                command_data.update(data)

            # Send command
            command_json = json.dumps(command_data) + "\n"
            self.socket.send(command_json.encode('utf-8'))

            self.logger.debug(f"Sent command to device {self.id}: {command}")

            # For now, assume command was successful
            # In a real implementation, we would wait for acknowledgment
            return True

        except Exception as e:
            self.logger.error(
                f"Failed to send command to device {self.id}: {str(e)}")
            return False

    def start_recording(self, session_id) -> None:
        """Start recording on the device.

        Args:
            session_id: The ID of the session to start

        Returns:
            True if recording was started successfully, False otherwise
        """
        self.logger.info(
            f"Starting recording on device: {self.name} ({self.id}) with session ID: {session_id}")

        # Check if connected
        if not self.connected:
            self.logger.error(
                f"Cannot start recording on device {self.id}: not connected")
            return False

        # Check if already recording
        if self.recording:
            self.logger.warning(f"Device {self.id} is already recording")
            return True

        try:
            # Send start recording command to the device
            success = self.send_command(
                "START_RECORDING", {
                    "session_id": session_id})

            if success:
                self.recording = True
                self.session_id = session_id

                # Update status
                self.status["recording"] = True
                self.status["rgb_camera_active"] = "rgb" in self.capabilities
                self.status["thermal_camera_active"] = "thermal" in self.capabilities
                self.status["gsr_sensor_active"] = "gsr" in self.capabilities

                # Emit status update
                self.status_updated.emit(self)

                self.logger.info(
                    f"Recording started on device: {self.name} ({self.id})")
                return True
            else:
                self.logger.error(
                    f"Failed to send start recording command to device {self.id}")
                return False

        except Exception as e:
            self.logger.error(
                f"Failed to start recording on device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False

    def stop_recording(self) -> None:
        """Stop recording on the device.

        Returns:
            True if recording was stopped successfully, False otherwise
        """
        self.logger.info(
            f"Stopping recording on device: {self.name} ({self.id})")

        # Check if connected
        if not self.connected:
            self.logger.error(
                f"Cannot stop recording on device {self.id}: not connected")
            return False

        # Check if not recording
        if not self.recording:
            self.logger.warning(f"Device {self.id} is not recording")
            return True

        try:
            # Send stop recording command to the device
            success = self.send_command("STOP_RECORDING")

            if success:
                self.recording = False

                # Update status
                self.status["recording"] = False
                self.status["rgb_camera_active"] = False
                self.status["thermal_camera_active"] = False
                self.status["gsr_sensor_active"] = False

                # Emit status update
                self.status_updated.emit(self)

                self.logger.info(
                    f"Recording stopped on device: {self.name} ({self.id})")
                return True
            else:
                self.logger.error(
                    f"Failed to send stop recording command to device {self.id}")
                return False

        except Exception as e:
            self.logger.error(
                f"Failed to stop recording on device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False

    def switch_to_front_camera(self) -> bool:
        """Switch device to front camera.

        Returns:
            True if camera was switched successfully, False otherwise
        """
        self.logger.info(f"Switching to front camera on device: {self.name} ({self.id})")

        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot switch camera on device {self.id}: not connected")
            return False

        try:
            success = self.send_command("CMD_SWITCH_FRONT_CAMERA")
            if success:
                self.logger.info(f"Successfully switched to front camera on device {self.id}")
                return True
            else:
                self.logger.error(f"Failed to switch to front camera on device {self.id}")
                return False
        except Exception as e:
            self.logger.error(f"Error switching to front camera on device {self.id}: {str(e)}")
            return False

    def switch_to_rear_camera(self) -> bool:
        """Switch device to rear camera.

        Returns:
            True if camera was switched successfully, False otherwise
        """
        self.logger.info(f"Switching to rear camera on device: {self.name} ({self.id})")

        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot switch camera on device {self.id}: not connected")
            return False

        try:
            success = self.send_command("CMD_SWITCH_REAR_CAMERA")
            if success:
                self.logger.info(f"Successfully switched to rear camera on device {self.id}")
                return True
            else:
                self.logger.error(f"Failed to switch to rear camera on device {self.id}")
                return False
        except Exception as e:
            self.logger.error(f"Error switching to rear camera on device {self.id}: {str(e)}")
            return False

    def toggle_rgb_sensor(self) -> bool:
        """Toggle RGB video sensor on/off.

        Returns:
            True if sensor was toggled successfully, False otherwise
        """
        self.logger.info(f"Toggling RGB sensor on device: {self.name} ({self.id})")

        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot toggle RGB sensor on device {self.id}: not connected")
            return False

        try:
            success = self.send_command("CMD_TOGGLE_RGB_SENSOR")
            if success:
                self.logger.info(f"Successfully toggled RGB sensor on device {self.id}")
                return True
            else:
                self.logger.error(f"Failed to toggle RGB sensor on device {self.id}")
                return False
        except Exception as e:
            self.logger.error(f"Error toggling RGB sensor on device {self.id}: {str(e)}")
            return False

    def toggle_thermal_sensor(self) -> bool:
        """Toggle thermal video sensor on/off.

        Returns:
            True if sensor was toggled successfully, False otherwise
        """
        self.logger.info(f"Toggling thermal sensor on device: {self.name} ({self.id})")

        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot toggle thermal sensor on device {self.id}: not connected")
            return False

        try:
            success = self.send_command("CMD_TOGGLE_THERMAL_SENSOR")
            if success:
                self.logger.info(f"Successfully toggled thermal sensor on device {self.id}")
                return True
            else:
                self.logger.error(f"Failed to toggle thermal sensor on device {self.id}")
                return False
        except Exception as e:
            self.logger.error(f"Error toggling thermal sensor on device {self.id}: {str(e)}")
            return False

    def toggle_gsr_sensor(self) -> bool:
        """Toggle GSR sensor on/off.

        Returns:
            True if sensor was toggled successfully, False otherwise
        """
        self.logger.info(f"Toggling GSR sensor on device: {self.name} ({self.id})")

        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot toggle GSR sensor on device {self.id}: not connected")
            return False

        try:
            success = self.send_command("CMD_TOGGLE_GSR_SENSOR")
            if success:
                self.logger.info(f"Successfully toggled GSR sensor on device {self.id}")
                return True
            else:
                self.logger.error(f"Failed to toggle GSR sensor on device {self.id}")
                return False
        except Exception as e:
            self.logger.error(f"Error toggling GSR sensor on device {self.id}: {str(e)}")
            return False

    def toggle_audio_recording(self) -> bool:
        """Toggle audio recording on/off.

        Returns:
            True if audio recording was toggled successfully, False otherwise
        """
        self.logger.info(f"Toggling audio recording on device: {self.name} ({self.id})")

        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot toggle audio recording on device {self.id}: not connected")
            return False

        try:
            success = self.send_command("CMD_TOGGLE_AUDIO_RECORDING")
            if success:
                self.logger.info(f"Successfully toggled audio recording on device {self.id}")
                return True
            else:
                self.logger.error(f"Failed to toggle audio recording on device {self.id}")
                return False
        except Exception as e:
            self.logger.error(f"Error toggling audio recording on device {self.id}: {str(e)}")
            return False

    def get_sensor_status(self) -> bool:
        """Get current sensor status from device.

        Returns:
            True if status was requested successfully, False otherwise
        """
        self.logger.info(f"Getting sensor status from device: {self.name} ({self.id})")

        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot get sensor status from device {self.id}: not connected")
            return False

        try:
            success = self.send_command("CMD_GET_SENSOR_STATUS")
            if success:
                self.logger.info(f"Successfully requested sensor status from device {self.id}")
                return True
            else:
                self.logger.error(f"Failed to get sensor status from device {self.id}")
                return False
        except Exception as e:
            self.logger.error(f"Error getting sensor status from device {self.id}: {str(e)}")
            return False

    def collect_files(self, destination_dir) -> None:
        """Collect files from the device.

        Args:
            destination_dir: The directory to save the files to

        Returns:
            True if files were collected successfully, False otherwise
        """
        self.logger.info(
            f"Collecting files from device: {self.name} ({self.id}) to: {destination_dir}")

        # Check if connected
        if not self.connected:
            self.logger.error(
                f"Cannot collect files from device {self.id}: not connected")
            return False

        try:
            # Create device directory
            device_dir = os.path.join(destination_dir, self.id)
            os.makedirs(device_dir, exist_ok=True)

            # Send file collection request to the device
            success = self.send_command("COLLECT_FILES", {
                "session_id": self.session_id,
                "destination": destination_dir
            })

            if success:
                # Wait for and handle file transfer
                return self._handle_file_transfer(device_dir)
            else:
                self.logger.error(
                    f"Failed to send file collection command to device {self.id}")
                return False

        except Exception as e:
            self.logger.error(
                f"Failed to collect files from device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False

    def _handle_file_transfer(self, device_dir) -> None:
        """Handle file transfer from the device.

        Args:
            device_dir: Directory to save files to

        Returns:
            True if all files were transferred successfully
        """
        try:
            files_received = 0
            total_files = 0

            # Set a timeout for file transfer
            transfer_timeout = 300  # 5 minutes
            start_time = time.time()

            while time.time() - start_time < transfer_timeout:
                try:
                    # Read response from device
                    if self.socket and self.socket.fileno() != -1:
                        # 10 second timeout for each read
                        self.socket.settimeout(10.0)
                        response = self.socket.recv(4096).decode('utf-8')

                        if not response:
                            break

                        # Parse JSON response
                        data = json.loads(response)
                        msg_type = data.get("type")

                        if msg_type == "FILE_LIST":
                            total_files = data.get("count", 0)
                            files_info = data.get("files", [])
                            self.logger.info(
                                f"Expecting {total_files} files from device {self.id}")

                            for file_info in files_info:
                                self.logger.info(
                                    f"  - {file_info.get('name')} ({file_info.get('size')} bytes)")

                        elif msg_type == "FILE_TRANSFER":
                            # Receive file
                            filename = data.get("name")
                            file_size = data.get("size", 0)

                            if self._receive_file(
                                    device_dir, filename, file_size):
                                files_received += 1
                                self.logger.info(
                                    f"Received file {files_received}/{total_files}: {filename}")
                            else:
                                self.logger.error(
                                    f"Failed to receive file: {filename}")

                        elif msg_type == "FILE_COLLECTION_RESPONSE":
                            success = data.get("success", False)
                            message = data.get("message", "")
                            self.logger.info(
                                f"File collection completed: {message}")
                            return success and files_received == total_files

                except socket.timeout:
                    continue
                except json.JSONDecodeError as e:
                    self.logger.warning(f"Invalid JSON received: {e}")
                    continue
                except Exception as e:
                    self.logger.error(f"Error during file transfer: {e}")
                    break

            # Check if we received all expected files
            if total_files > 0 and files_received == total_files:
                self.logger.info(
                    f"Successfully received all {files_received} files from device {self.id}")
                return True
            else:
                self.logger.warning(
                    f"File transfer incomplete: received {files_received}/{total_files} files")
                return False

        except Exception as e:
            self.logger.error(
                f"Error handling file transfer from device {self.id}: {e}")
            return False

    def _receive_file(self, device_dir, filename, file_size) -> None:
        """Receive a single file from the device.

        Args:
            device_dir: Directory to save the file
            filename: Name of the file
            file_size: Expected size of the file

        Returns:
            True if file was received successfully
        """
        try:
            file_path = os.path.join(device_dir, filename)
            bytes_received = 0

            with open(file_path, 'wb') as f:
                while bytes_received < file_size:
                    chunk_size = min(8192, file_size - bytes_received)
                    chunk = self.socket.recv(chunk_size)

                    if not chunk:
                        break

                    f.write(chunk)
                    bytes_received += len(chunk)

            if bytes_received == file_size:
                self.logger.info(
                    f"Successfully received file: {filename} ({bytes_received} bytes)")
                return True
            else:
                self.logger.error(
                    f"File size mismatch for {filename}: expected {file_size}, got {bytes_received}")
                # Remove incomplete file
                if os.path.exists(file_path):
                    os.remove(file_path)
                return False

        except Exception as e:
            self.logger.error(f"Error receiving file {filename}: {e}")
            return False

    def get_status(self) -> None:
        """Get the status of the device.

        Returns:
            A dictionary with the device status
        """
        # In a real implementation, we would query the device for its status
        # For now, just return the current status
        return self.status

    def _reader_thread(self) -> None:
        """Thread for reading data from the device.
        """
        self.logger.info(f"Reader thread started for device: {self.name} ({self.id})")

        socket_file = None
        try:
            if self.socket:
                socket_file = self.socket.makefile('r')
        except Exception as e:
            self.logger.error(f"Error creating socket file: {e}")
            return

        while self.running and self.connected:
            try:
                if not socket_file:
                    break

                # Read JSON message line
                line = socket_file.readline()
                if not line:
                    self.logger.warning("Connection closed by device")
                    break

                line = line.strip()
                if not line:
                    continue

                # Parse JSON message
                try:
                    message = json.loads(line)
                    message_type = message.get("type", "")

                    if message_type == "GSR_DATA":
                        # Handle GSR data
                        gsr_value = message.get("value", 0)
                        timestamp = message.get("timestamp", 0)
                        self.logger.debug(f"Received GSR data: {gsr_value} at {timestamp}")
                        self.gsr_data_received.emit(self, float(gsr_value), int(timestamp))

                    elif message_type == "HEART_RATE_DATA":
                        # Handle heart rate data
                        heart_rate = message.get("value", 0)
                        timestamp = message.get("timestamp", 0)
                        self.logger.debug(f"Received heart rate: {heart_rate} BPM at {timestamp}")
                        self.heart_rate_data_received.emit(self, int(heart_rate), int(timestamp))

                    elif message_type == "DEVICE_STATUS":
                        # Handle device status update
                        self.status["battery_level"] = message.get("battery_level", 0)
                        self.status["storage_used"] = message.get("storage_used", 0)
                        self.status["is_recording"] = message.get("is_recording", False)
                        self.status_updated.emit(self)

                    elif message_type == "VIDEO_FRAME":
                        # Handle video frame data
                        frame_type = message.get("frame_type", "thermal")
                        data_length = message.get("data_length", 0)
                        timestamp = message.get("timestamp", 0)

                        if data_length > 0:
                            # Read binary frame data
                            frame_data = self.socket.recv(data_length)
                            if len(frame_data) == data_length:
                                self.logger.debug(f"Received {frame_type} frame: {data_length} bytes")
                                self.video_frame_data_received.emit(self, frame_data, frame_type, timestamp)
                            else:
                                self.logger.warning(f"Incomplete frame data: expected {data_length}, got {len(frame_data)}")

                    else:
                        self.logger.debug(f"Unknown message type: {message_type}")

                except json.JSONDecodeError as e:
                    self.logger.error(f"Error parsing JSON message: {e}")
                    continue

            except Exception as e:
                if self.running:
                    self.logger.error(f"Error in reader thread for device {self.id}: {str(e)}")
                    self.status["error"] = str(e)
                    self.status_updated.emit(self)
                break

        # Cleanup
        if socket_file:
            try:
                socket_file.close()
            except:
                pass

        self.logger.info(f"Reader thread stopped for device: {self.name} ({self.id})")

    def _writer_thread(self) -> None:
        """Thread for writing data to the device.
        """
        self.logger.info(
            f"Writer thread started for device: {self.name} ({self.id})")

        while self.running:
            try:
                # In a real implementation, we would write data to the socket
                # For now, just sleep to avoid busy waiting
                time.sleep(1.0)
            except Exception as e:
                self.logger.error(
                    f"Error in writer thread for device {self.id}: {str(e)}")
                self.status["error"] = str(e)
                self.status_updated.emit(self)

                # Sleep before retrying
                time.sleep(1.0)

        self.logger.info(
            f"Writer thread stopped for device: {self.name} ({self.id})")

    def __str__(self) -> None:
        """Get a string representation of the device.

        Returns:
            A string representation of the device
        """
        return f"Device({self.id}, {self.name}, {self.address}:{self.port})"
