#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Device Manager for the PC Controller App.
Cross-platform support: Windows, macOS, Linux.
"""

import logging
import threading
import time
import socket
import json
from PySide6.QtCore import QObject, Signal, Slot
from zeroconf import ServiceBrowser, Zeroconf

from network.device import Device
from utils.logger import get_logger

class DeviceManager(QObject):
    """
    Device Manager class for handling device discovery, connection, and communication.
    """

    # Define signals
    device_discovered = Signal(object)
    device_removed = Signal(object)
    device_connected = Signal(object)
    device_disconnected = Signal(object)

    def __init__(self):
        """
        Initialize the device manager.
        """
        super().__init__()

        # Set up logging
        self.logger = get_logger(__name__)
        self.logger.info("Initializing device manager")

        # Initialize device list
        self.devices = {}  # Dictionary of devices by ID
        self.discovered_devices = {}  # Dictionary of discovered devices by ID

        # Initialize zeroconf for device discovery
        self.zeroconf = None
        self.browser = None

        # Initialize lock for thread safety
        self.lock = threading.Lock()

        self.logger.info("Device manager initialized")

    def discover_devices(self):
        """
        Discover devices on the network.
        """
        self.logger.info("Starting device discovery")

        # Clear discovered devices
        with self.lock:
            self.discovered_devices.clear()

        # Initialize zeroconf
        if self.zeroconf is None:
            self.zeroconf = Zeroconf()

        # Create a listener for service discovery
        listener = DeviceListener(self)

        # Browse for GSR capture services (matching Android app service type)
        self.browser = ServiceBrowser(self.zeroconf, "_gsrcapture._tcp.local.", listener)

        self.logger.info("Device discovery started")
        return True


    def connect_device(self, device_id):
        """
        Connect to a device.

        Args:
            device_id: The ID of the device to connect to

        Returns:
            True if the connection was successful, False otherwise
        """
        self.logger.info(f"Connecting to device: {device_id}")

        # Check if the device is already connected
        with self.lock:
            if device_id in self.devices:
                self.logger.warning(f"Device {device_id} is already connected")
                return True

            # Check if the device is discovered
            if device_id not in self.discovered_devices:
                self.logger.error(f"Device {device_id} not found in discovered devices")
                return False

            # Get the device
            device = self.discovered_devices[device_id]

        # Connect to the device
        success = device.connect()
        if success:
            # Add the device to the connected devices
            with self.lock:
                self.devices[device_id] = device

            # Emit signal
            self.device_connected.emit(device)

            self.logger.info(f"Connected to device: {device_id}")
            return True
        else:
            self.logger.error(f"Failed to connect to device: {device_id}")
            return False

    def disconnect_device(self, device_id):
        """
        Disconnect from a device.

        Args:
            device_id: The ID of the device to disconnect from

        Returns:
            True if the disconnection was successful, False otherwise
        """
        self.logger.info(f"Disconnecting from device: {device_id}")

        # Check if the device is connected
        with self.lock:
            if device_id not in self.devices:
                self.logger.warning(f"Device {device_id} is not connected")
                return True

            # Get the device
            device = self.devices[device_id]

        # Disconnect from the device
        success = device.disconnect()
        if success:
            # Remove the device from the connected devices
            with self.lock:
                del self.devices[device_id]

            # Emit signal
            self.device_disconnected.emit(device)

            self.logger.info(f"Disconnected from device: {device_id}")
            return True
        else:
            self.logger.error(f"Failed to disconnect from device: {device_id}")
            return False

    def connect_all_devices(self):
        """
        Connect to all discovered devices.

        Returns:
            True if all connections were successful, False otherwise
        """
        self.logger.info("Connecting to all devices")

        # Get the list of discovered devices
        with self.lock:
            device_ids = list(self.discovered_devices.keys())

        # Connect to each device
        success = True
        for device_id in device_ids:
            if not self.connect_device(device_id):
                success = False

        return success

    def disconnect_all_devices(self):
        """
        Disconnect from all connected devices.

        Returns:
            True if all disconnections were successful, False otherwise
        """
        self.logger.info("Disconnecting from all devices")

        # Get the list of connected devices
        with self.lock:
            device_ids = list(self.devices.keys())

        # Disconnect from each device
        success = True
        for device_id in device_ids:
            if not self.disconnect_device(device_id):
                success = False

        return success

    def start_recording(self, session_id):
        """
        Start recording on all connected devices.

        Args:
            session_id: The ID of the session to start

        Returns:
            True if all devices started recording successfully, False otherwise
        """
        self.logger.info(f"Starting recording on all devices with session ID: {session_id}")

        # Get the list of connected devices
        with self.lock:
            device_ids = list(self.devices.keys())

        # Start recording on each device
        success = True
        for device_id in device_ids:
            with self.lock:
                device = self.devices[device_id]

            if not device.start_recording(session_id):
                success = False

        return success

    def stop_recording(self):
        """
        Stop recording on all connected devices.

        Returns:
            True if all devices stopped recording successfully, False otherwise
        """
        self.logger.info("Stopping recording on all devices")

        # Get the list of connected devices
        with self.lock:
            device_ids = list(self.devices.keys())

        # Stop recording on each device
        success = True
        for device_id in device_ids:
            with self.lock:
                device = self.devices[device_id]

            if not device.stop_recording():
                success = False

        return success

    def collect_files(self, destination_dir):
        """
        Collect files from all connected devices.

        Args:
            destination_dir: The directory to save the files to

        Returns:
            True if all files were collected successfully, False otherwise
        """
        self.logger.info(f"Collecting files from all devices to: {destination_dir}")

        # Get the list of connected devices
        with self.lock:
            device_ids = list(self.devices.keys())

        # Collect files from each device
        success = True
        for device_id in device_ids:
            with self.lock:
                device = self.devices[device_id]

            if not device.collect_files(destination_dir):
                success = False

        return success

    def get_device_status(self, device_id):
        """
        Get the status of a device.

        Args:
            device_id: The ID of the device to get the status of

        Returns:
            The status of the device, or None if the device is not connected
        """
        # Check if the device is connected
        with self.lock:
            if device_id not in self.devices:
                self.logger.warning(f"Device {device_id} is not connected")
                return None

            # Get the device
            device = self.devices[device_id]

        # Get the status of the device
        return device.get_status()

    def get_all_device_statuses(self):
        """
        Get the status of all connected devices.

        Returns:
            A dictionary of device statuses by device ID
        """
        # Get the list of connected devices
        with self.lock:
            device_ids = list(self.devices.keys())

        # Get the status of each device
        statuses = {}
        for device_id in device_ids:
            status = self.get_device_status(device_id)
            if status is not None:
                statuses[device_id] = status

        return statuses

    def cleanup(self):
        """
        Clean up resources.
        """
        self.logger.info("Cleaning up device manager")

        # Disconnect from all devices
        self.disconnect_all_devices()

        # Close zeroconf
        if self.zeroconf is not None:
            self.zeroconf.close()
            self.zeroconf = None

        self.logger.info("Device manager cleaned up")


class DeviceListener:
    """
    Listener for zeroconf service discovery.
    """

    def __init__(self, device_manager):
        """
        Initialize the listener.

        Args:
            device_manager: The device manager to notify of discovered devices
        """
        self.device_manager = device_manager
        self.logger = get_logger(__name__)

    def add_service(self, zeroconf, service_type, name):
        """
        Called when a service is discovered.

        Args:
            zeroconf: The zeroconf instance
            service_type: The type of service
            name: The name of the service
        """
        try:
            info = zeroconf.get_service_info(service_type, name)
            if info:
                self.logger.info(f"Service discovered: {name}")

                # Validate that we have required information
                if not info.addresses or len(info.addresses) == 0:
                    self.logger.warning(f"Service {name} has no addresses, skipping")
                    return

                if info.port is None or info.port <= 0:
                    self.logger.warning(f"Service {name} has invalid port {info.port}, skipping")
                    return

                # Parse the service info
                address = socket.inet_ntoa(info.addresses[0])
                port = info.port

                # Parse the properties safely
                properties = {}
                if info.properties:
                    for key, value in info.properties.items():
                        try:
                            properties[key.decode('utf-8')] = value.decode('utf-8')
                        except (UnicodeDecodeError, AttributeError) as e:
                            self.logger.warning(f"Failed to decode property {key}: {e}")

                # Create device ID - use service name if no ID property
                device_id = properties.get('id', name.split('.')[0])  # Remove domain part
                device_name = properties.get('name', name)

                # Parse capabilities
                capabilities_str = properties.get('capabilities', '')
                capabilities = [cap.strip() for cap in capabilities_str.split(',') if cap.strip()]

                # Create a device
                device = Device(
                    id=device_id,
                    name=device_name,
                    address=address,
                    port=port,
                    device_type=properties.get('type', 'android'),
                    capabilities=capabilities
                )

                # Check if device already exists
                with self.device_manager.lock:
                    if device_id in self.device_manager.discovered_devices:
                        self.logger.info(f"Device {device_id} already discovered, updating info")

                    self.device_manager.discovered_devices[device_id] = device

                # Emit signal
                self.device_manager.device_discovered.emit(device)
                self.logger.info(f"Device added: {device_name} ({device_id}) at {address}:{port}")

            else:
                self.logger.warning(f"Could not get service info for {name}")

        except Exception as e:
            self.logger.error(f"Error processing discovered service {name}: {e}")

    def remove_service(self, zeroconf, service_type, name):
        """
        Called when a service is removed.

        Args:
            zeroconf: The zeroconf instance
            service_type: The type of service
            name: The name of the service
        """
        self.logger.info(f"Service removed: {name}")

        # Find and remove the device from discovered devices
        device_to_remove = None
        with self.device_manager.lock:
            # Find the device by service name or other identifier
            for device_id, device in list(self.device_manager.discovered_devices.items()):
                # Match by service name or device name
                if device.name == name or device_id == name:
                    device_to_remove = device
                    del self.device_manager.discovered_devices[device_id]
                    break

        # If device was found and removed, emit signal
        if device_to_remove:
            self.device_manager.device_removed.emit(device_to_remove)
            self.logger.info(f"Device removed from discovered devices: {device_to_remove.name}")
        else:
            self.logger.warning(f"Could not find device to remove for service: {name}")
