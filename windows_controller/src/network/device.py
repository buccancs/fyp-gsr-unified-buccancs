#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Device class for the Windows PC Controller App.
This module represents a connected device and handles communication with it.
"""

import logging
import socket
import threading
import time
import json
import os
from PyQt5.QtCore import QObject, pyqtSignal, pyqtSlot

from utils.logger import get_logger

class Device(QObject):
    """
    Device class for representing a connected device and handling communication with it.
    """
    
    # Define signals
    status_updated = pyqtSignal(object)
    video_frame_received = pyqtSignal(object, str)  # device, frame_type (rgb/thermal)
    
    def __init__(self, id, name, address, port, device_type="phone", capabilities=None):
        """
        Initialize the device.
        
        Args:
            id: The unique ID of the device
            name: The name of the device
            address: The IP address of the device
            port: The port to connect to
            device_type: The type of device (e.g. "phone")
            capabilities: A list of capabilities the device has (e.g. ["rgb", "thermal", "gsr"])
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
    
    def connect(self):
        """
        Connect to the device.
        
        Returns:
            True if the connection was successful, False otherwise
        """
        self.logger.info(f"Connecting to device: {self.name} ({self.id})")
        
        # Check if already connected
        if self.connected:
            self.logger.warning(f"Device {self.id} is already connected")
            return True
        
        try:
            # In a real implementation, we would establish a socket connection
            # For now, just simulate a successful connection
            self.connected = True
            self.status["connected"] = True
            
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
            self.logger.error(f"Failed to connect to device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False
    
    def disconnect(self):
        """
        Disconnect from the device.
        
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
            
            self.logger.info(f"Disconnected from device: {self.name} ({self.id})")
            return True
        except Exception as e:
            self.logger.error(f"Failed to disconnect from device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False
    
    def start_recording(self, session_id):
        """
        Start recording on the device.
        
        Args:
            session_id: The ID of the session to start
        
        Returns:
            True if recording was started successfully, False otherwise
        """
        self.logger.info(f"Starting recording on device: {self.name} ({self.id}) with session ID: {session_id}")
        
        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot start recording on device {self.id}: not connected")
            return False
        
        # Check if already recording
        if self.recording:
            self.logger.warning(f"Device {self.id} is already recording")
            return True
        
        try:
            # In a real implementation, we would send a command to the device
            # For now, just simulate a successful start
            self.recording = True
            self.session_id = session_id
            
            # Update status
            self.status["recording"] = True
            self.status["rgb_camera_active"] = "rgb" in self.capabilities
            self.status["thermal_camera_active"] = "thermal" in self.capabilities
            self.status["gsr_sensor_active"] = "gsr" in self.capabilities
            
            # Emit status update
            self.status_updated.emit(self)
            
            self.logger.info(f"Recording started on device: {self.name} ({self.id})")
            return True
        except Exception as e:
            self.logger.error(f"Failed to start recording on device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False
    
    def stop_recording(self):
        """
        Stop recording on the device.
        
        Returns:
            True if recording was stopped successfully, False otherwise
        """
        self.logger.info(f"Stopping recording on device: {self.name} ({self.id})")
        
        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot stop recording on device {self.id}: not connected")
            return False
        
        # Check if not recording
        if not self.recording:
            self.logger.warning(f"Device {self.id} is not recording")
            return True
        
        try:
            # In a real implementation, we would send a command to the device
            # For now, just simulate a successful stop
            self.recording = False
            
            # Update status
            self.status["recording"] = False
            self.status["rgb_camera_active"] = False
            self.status["thermal_camera_active"] = False
            self.status["gsr_sensor_active"] = False
            
            # Emit status update
            self.status_updated.emit(self)
            
            self.logger.info(f"Recording stopped on device: {self.name} ({self.id})")
            return True
        except Exception as e:
            self.logger.error(f"Failed to stop recording on device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False
    
    def collect_files(self, destination_dir):
        """
        Collect files from the device.
        
        Args:
            destination_dir: The directory to save the files to
        
        Returns:
            True if files were collected successfully, False otherwise
        """
        self.logger.info(f"Collecting files from device: {self.name} ({self.id}) to: {destination_dir}")
        
        # Check if connected
        if not self.connected:
            self.logger.error(f"Cannot collect files from device {self.id}: not connected")
            return False
        
        try:
            # In a real implementation, we would request files from the device
            # For now, just simulate a successful collection
            
            # Create device directory
            device_dir = os.path.join(destination_dir, self.id)
            os.makedirs(device_dir, exist_ok=True)
            
            # Simulate creating some files
            if "rgb" in self.capabilities:
                with open(os.path.join(device_dir, f"{self.session_id}_{self.id}_rgb.mp4"), "w") as f:
                    f.write("Simulated RGB video file")
            
            if "thermal" in self.capabilities:
                with open(os.path.join(device_dir, f"{self.session_id}_{self.id}_thermal.mp4"), "w") as f:
                    f.write("Simulated thermal video file")
            
            if "gsr" in self.capabilities:
                with open(os.path.join(device_dir, f"{self.session_id}_{self.id}_gsr.csv"), "w") as f:
                    f.write("timestamp,gsr_value\n")
                    f.write("0,100\n")
                    f.write("1000,110\n")
                    f.write("2000,120\n")
            
            self.logger.info(f"Files collected from device: {self.name} ({self.id})")
            return True
        except Exception as e:
            self.logger.error(f"Failed to collect files from device {self.id}: {str(e)}")
            self.status["error"] = str(e)
            self.status_updated.emit(self)
            return False
    
    def get_status(self):
        """
        Get the status of the device.
        
        Returns:
            A dictionary with the device status
        """
        # In a real implementation, we would query the device for its status
        # For now, just return the current status
        return self.status
    
    def _reader_thread(self):
        """
        Thread for reading data from the device.
        """
        self.logger.info(f"Reader thread started for device: {self.name} ({self.id})")
        
        while self.running:
            try:
                # In a real implementation, we would read data from the socket
                # For now, just simulate receiving status updates and video frames
                
                # Simulate receiving a status update
                if self.connected:
                    # Update battery level and storage
                    self.status["battery_level"] = min(100, self.status["battery_level"] + 1)
                    self.status["storage_remaining"] = max(0, 100 - self.status["battery_level"])
                    
                    # Emit status update
                    self.status_updated.emit(self)
                
                # Simulate receiving video frames
                if self.connected and self.recording:
                    # Emit video frame signals
                    if "rgb" in self.capabilities:
                        # In a real implementation, we would receive actual frames
                        # For now, just emit a signal with None as the frame
                        self.video_frame_received.emit(self, "rgb")
                    
                    if "thermal" in self.capabilities:
                        # In a real implementation, we would receive actual frames
                        # For now, just emit a signal with None as the frame
                        self.video_frame_received.emit(self, "thermal")
                
                # Sleep to avoid busy waiting
                time.sleep(1.0)
            except Exception as e:
                self.logger.error(f"Error in reader thread for device {self.id}: {str(e)}")
                self.status["error"] = str(e)
                self.status_updated.emit(self)
                
                # Sleep before retrying
                time.sleep(1.0)
        
        self.logger.info(f"Reader thread stopped for device: {self.name} ({self.id})")
    
    def _writer_thread(self):
        """
        Thread for writing data to the device.
        """
        self.logger.info(f"Writer thread started for device: {self.name} ({self.id})")
        
        while self.running:
            try:
                # In a real implementation, we would write data to the socket
                # For now, just sleep to avoid busy waiting
                time.sleep(1.0)
            except Exception as e:
                self.logger.error(f"Error in writer thread for device {self.id}: {str(e)}")
                self.status["error"] = str(e)
                self.status_updated.emit(self)
                
                # Sleep before retrying
                time.sleep(1.0)
        
        self.logger.info(f"Writer thread stopped for device: {self.name} ({self.id})")
    
    def __str__(self):
        """
        Get a string representation of the device.
        
        Returns:
            A string representation of the device
        """
        return f"Device({self.id}, {self.name}, {self.address}:{self.port})"