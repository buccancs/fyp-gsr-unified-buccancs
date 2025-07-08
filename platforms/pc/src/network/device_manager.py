#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Device Manager for the PC Controller App.
Cross-platform support: Windows, macOS, Linux.
"""

import json
import logging
import socket
import threading
import time

from PySide6.QtCore import QObject, Signal, Slot
from zeroconf import ServiceBrowser, Zeroconf

try:
    from ppadb.client import Client as AdbClient
    from ppadb.device import Device as AdbDevice
    ADB_AVAILABLE = True
except ImportError:
    ADB_AVAILABLE = False

from network.device import Device
from local_device import LocalDevice
from utils.logger import get_logger
from typing import Any, Dict, List, Optional, Union


class DeviceManager(QObject):
    """Device Manager class for handling device discovery, connection, and communication.
    """

    # Define signals
    device_discovered = Signal(object)
    device_removed = Signal(object)
    device_connected = Signal(object)
    device_disconnected = Signal(object)

    def __init__(self) -> None:
        """Initialize the device manager.
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

        # Initialize local device
        self.local_device = LocalDevice(parent=self)
        self._initialize_local_device()

        self.logger.info("Device manager initialized")

    def _initialize_local_device(self) -> None:
        """Initialize the local device and add it to the devices list.
        """
        try:
            # Connect to local hardware
            self.local_device.connect()

            # Add local device to the devices dictionary
            with self.lock:
                self.devices[self.local_device.id] = self.local_device

            # Connect signals
            self.local_device.connected.connect(
                lambda: self.device_connected.emit(self.local_device))
            self.local_device.disconnected.connect(
                lambda: self.device_disconnected.emit(self.local_device))

            self.logger.info("Local device initialized and added to device list")

        except Exception as e:
            self.logger.error(f"Failed to initialize local device: {e}")

    def discover_devices(self) -> None:
        """Discover devices on the network.
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
        self.browser = ServiceBrowser(
            self.zeroconf, "_gsrcapture._tcp.local.", listener)

        self.logger.info("Device discovery started")
        return True

    def discover_usb_devices(self) -> None:
        """Discover Android devices connected via USB using ADB.

        Returns:
            True if USB discovery was successful, False otherwise
        """
        if not ADB_AVAILABLE:
            self.logger.error("ADB library not available. Install pure-python-adb to use USB discovery.")
            return False

        self.logger.info("Starting USB device discovery")

        try:
            # Connect to ADB server
            adb_client = AdbClient(host="127.0.0.1", port=5037)

            # Get list of connected devices
            adb_devices = adb_client.devices()

            if not adb_devices:
                self.logger.info("No USB devices found")
                return True

            # Process each USB device
            for adb_device in adb_devices:
                try:
                    device_serial = adb_device.serial
                    self.logger.info(f"Found USB device: {device_serial}")

                    # Set up reverse port forwarding (Android port 5000 -> PC port 5000)
                    forward_port = 5000
                    result = adb_device.reverse(f"tcp:{forward_port}", f"tcp:{forward_port}")

                    if result:
                        self.logger.info(f"Port forwarding established for device {device_serial}")

                        # Create Device object for USB connection
                        device = Device(
                            id=device_serial,
                            name=f"USB Device ({device_serial})",
                            address="127.0.0.1",  # Localhost due to port forwarding
                            port=forward_port,
                            device_type="usb_phone",
                            capabilities=["gsr", "video", "thermal", "audio"]
                        )

                        # Add to discovered devices
                        with self.lock:
                            self.discovered_devices[device_serial] = device

                        # Emit discovery signal
                        self.device_discovered.emit(device)

                        self.logger.info(f"USB device {device_serial} added to discovered devices")

                    else:
                        self.logger.error(f"Failed to set up port forwarding for device {device_serial}")

                except Exception as e:
                    self.logger.error(f"Error processing USB device {device_serial}: {e}")
                    continue

            self.logger.info("USB device discovery completed")
            return True

        except Exception as e:
            self.logger.error(f"USB device discovery failed: {e}")
            return False

    def connect_device(self, device_id) -> None:
        """Connect to a device.

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
                self.logger.error(
                    f"Device {device_id} not found in discovered devices")
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

    def disconnect_device(self, device_id) -> None:
        """Disconnect from a device.

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

    def connect_all_devices(self) -> None:
        """Connect to all discovered devices.

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

    def disconnect_all_devices(self) -> None:
        """Disconnect from all connected devices.

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

    def start_recording(self, session_id, sensor_map=None) -> None:
        """Start recording on all connected devices with specified sensors.

        Args:
            session_id: The ID of the session to start
            sensor_map: Dictionary mapping device IDs to lists of enabled sensors
                       e.g., {"Local-PC": ["GSR/PPG", "Brio 4K"], "Android-Device-1": ["Thermal Cam"]}

        Returns:
            True if all devices started recording successfully, False otherwise
        """
        self.logger.info(
            f"Starting recording on all devices with session ID: {session_id}")

        if sensor_map:
            self.logger.info(f"Sensor map: {sensor_map}")

        # Get the list of connected devices
        with self.lock:
            device_ids = list(self.devices.keys())

        # Start recording on each device
        success = True
        for device_id in device_ids:
            with self.lock:
                device = self.devices[device_id]

            # Get enabled sensors for this device
            enabled_sensors = None
            if sensor_map and device_id in sensor_map:
                enabled_sensors = sensor_map[device_id]

            # Start recording with appropriate parameters
            if hasattr(device, 'start_recording'):
                if device_id == "Local-PC":
                    # LocalDevice supports sensor selection
                    if not device.start_recording(session_id, enabled_sensors):
                        success = False
                else:
                    # Remote devices use the original interface
                    if not device.start_recording(session_id):
                        success = False

        return success

    def stop_recording(self) -> None:
        """Stop recording on all connected devices.

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

    def collect_files(self, destination_dir) -> None:
        """Collect files from all connected devices.

        Args:
            destination_dir: The directory to save the files to

        Returns:
            True if all files were collected successfully, False otherwise
        """
        self.logger.info(
            f"Collecting files from all devices to: {destination_dir}")

        # Get the list of connected devices
        with self.lock:
            device_ids = list(self.devices.keys())

        # Collect files from each device
        success = True
        for device_id in device_ids:
            with self.lock:
                device = self.devices[device_id]

            # Skip LocalDevice since its files are already on the PC
            if device_id == "Local-PC":
                self.logger.info(f"Skipping file collection for {device_id} - files are already local")
                # Get list of local files for logging
                local_files = device.collect_files(destination_dir)
                if local_files:
                    self.logger.info(f"Local files available: {local_files}")
                continue

            # Collect files from remote devices
            if not device.collect_files(destination_dir):
                success = False

        return success

    def get_device_status(self, device_id) -> None:
        """Get the status of a device.

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

    def get_all_device_statuses(self) -> None:
        """Get the status of all connected devices.

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

    def cleanup(self) -> None:
        """Clean up resources.
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
    """Listener for zeroconf service discovery.
    """

    def __init__(self, device_manager) -> None:
        """Initialize the listener.

        Args:
            device_manager: The device manager to notify of discovered devices
        """
        self.device_manager = device_manager
        self.logger = get_logger(__name__)

    def add_service(self, zeroconf, service_type, name) -> None:
        """Called when a service is discovered.

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
                    self.logger.warning(
                        f"Service {name} has no addresses, skipping")
                    return

                if info.port is None or info.port <= 0:
                    self.logger.warning(
                        f"Service {name} has invalid port {info.port}, skipping")
                    return

                # Parse the service info
                address = socket.inet_ntoa(info.addresses[0])
                port = info.port

                # Parse the properties safely
                properties = {}
                if info.properties:
                    for key, value in info.properties.items():
                        try:
                            properties[key.decode(
                                'utf-8')] = value.decode('utf-8')
                        except (UnicodeDecodeError, AttributeError) as e:
                            self.logger.warning(
                                f"Failed to decode property {key}: {e}")

                # Create device ID - use service name if no ID property
                device_id = properties.get(
                    'id', name.split('.')[0])  # Remove domain part
                device_name = properties.get('name', name)

                # Parse capabilities
                capabilities_str = properties.get('capabilities', '')
                capabilities = [
                    cap.strip() for cap in capabilities_str.split(',') if cap.strip()]

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
                        self.logger.info(
                            f"Device {device_id} already discovered, updating info")

                    self.device_manager.discovered_devices[device_id] = device

                # Emit signal
                self.device_manager.device_discovered.emit(device)
                self.logger.info(
                    f"Device added: {device_name} ({device_id}) at {address}:{port}")

            else:
                self.logger.warning(f"Could not get service info for {name}")

        except Exception as e:
            self.logger.error(
                f"Error processing discovered service {name}: {e}")

    def remove_service(self, zeroconf, service_type, name) -> None:
        """Called when a service is removed.

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
            for device_id, device in list(
                    self.device_manager.discovered_devices.items()):
                # Match by service name or device name
                if device.name == name or device_id == name:
                    device_to_remove = device
                    del self.device_manager.discovered_devices[device_id]
                    break

        # If device was found and removed, emit signal
        if device_to_remove:
            self.device_manager.device_removed.emit(device_to_remove)
            self.logger.info(
                f"Device removed from discovered devices: {device_to_remove.name}")
        else:
            self.logger.warning(
                f"Could not find device to remove for service: {name}")
