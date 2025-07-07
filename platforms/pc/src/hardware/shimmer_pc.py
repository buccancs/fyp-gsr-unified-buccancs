"""
Shimmer GSR sensor driver for PC.

This module implements the concrete driver for Shimmer GSR sensors connected
to the PC via Bluetooth/COM port using the pyshimmer library.
"""

import configparser
import csv
import os
import threading
import time
from typing import Optional

from PySide6.QtCore import QThread, Signal

from hardware.pc_sensor import PCConnectedSensor
from utils.logger import get_logger

try:
    import pyshimmer
except ImportError:
    pyshimmer = None

try:
    import _hardware_backend
except ImportError:
    _hardware_backend = None


class ShimmerDataThread(QThread):
    """
    Dedicated thread for reading Shimmer sensor data.

    This thread polls the C++ NativeShimmer backend's non-blocking getData() method
    to retrieve high-precision timestamped data.
    """

    data_received = Signal(dict)
    error_occurred = Signal(str)

    def __init__(self, shimmer_device, use_cpp_backend=True, parent=None):
        super().__init__(parent)
        self.shimmer_device = shimmer_device
        self.use_cpp_backend = use_cpp_backend
        self.running = False
        self.logger = get_logger(self.__class__.__name__)

    def run(self):
        """Main thread loop for reading Shimmer data."""
        self.running = True
        self.logger.info("Shimmer data thread started")

        while self.running:
            try:
                if self.shimmer_device:
                    if self.use_cpp_backend and hasattr(self.shimmer_device, 'is_running') and self.shimmer_device.is_running():
                        # Use C++ backend - non-blocking getData()
                        timestamped_data_list = self.shimmer_device.get_data()

                        if timestamped_data_list:
                            for timestamped_data in timestamped_data_list:
                                # Parse the C++ timestamped data and emit signal
                                parsed_data = self._parse_cpp_shimmer_data(timestamped_data)
                                if parsed_data:
                                    self.data_received.emit(parsed_data)

                        # Small delay to prevent overwhelming the system
                        self.msleep(5)  # 5ms delay for C++ backend

                    elif not self.use_cpp_backend and hasattr(self.shimmer_device, 'is_connected') and self.shimmer_device.is_connected():
                        # Fallback to pyshimmer - blocking read_data()
                        data = self.shimmer_device.read_data()

                        if data:
                            # Parse the data and emit signal
                            parsed_data = self._parse_shimmer_data(data)
                            if parsed_data:
                                self.data_received.emit(parsed_data)

                        # Small delay to prevent overwhelming the system
                        self.msleep(10)  # 10ms delay for pyshimmer
                    else:
                        # If not connected/running, wait longer before checking again
                        self.msleep(100)
                else:
                    # No device, wait before checking again
                    self.msleep(100)

            except Exception as e:
                self.logger.error(f"Error reading Shimmer data: {e}")
                self.error_occurred.emit(str(e))
                self.msleep(1000)  # Wait 1 second before retrying

    def stop(self):
        """Stop the data reading thread."""
        self.running = False
        self.logger.info("Stopping Shimmer data thread")

    def _parse_cpp_shimmer_data(self, timestamped_data):
        """
        Parse C++ timestamped Shimmer data into a structured dictionary.

        Args:
            timestamped_data: TimestampedData object from C++ backend

        Returns:
            dict: Parsed data with GSR and PPG values
        """
        try:
            # Convert C++ timestamp to Python timestamp
            cpp_timestamp = timestamped_data.timestamp
            # Convert steady_clock timestamp to epoch time
            timestamp = time.time()  # For now, use current time - could be improved

            # Get raw data packet
            data_packet = timestamped_data.data_packet

            # Parse the raw data packet (this is a simplified example)
            # In a real implementation, you would parse the actual Shimmer protocol
            parsed = {
                'timestamp': timestamp,
                'gsr': 0.0,
                'ppg': 0.0
            }

            # Example parsing - adjust based on actual Shimmer data format
            if len(data_packet) >= 8:  # Assuming at least 8 bytes for GSR + PPG
                # Convert bytes to values (example - adjust for actual protocol)
                gsr_bytes = data_packet[0:4]
                ppg_bytes = data_packet[4:8]

                # Simple conversion (in reality, this would be more complex)
                if len(gsr_bytes) == 4:
                    gsr_value = int.from_bytes(gsr_bytes, byteorder='little', signed=False)
                    parsed['gsr'] = gsr_value / 1000.0  # Example scaling

                if len(ppg_bytes) == 4:
                    ppg_value = int.from_bytes(ppg_bytes, byteorder='little', signed=False)
                    parsed['ppg'] = ppg_value / 1000.0  # Example scaling

            return parsed

        except Exception as e:
            self.logger.error(f"Error parsing C++ Shimmer data: {e}")
            return None

    def _parse_shimmer_data(self, raw_data):
        """
        Parse raw Shimmer data into a structured dictionary.

        Args:
            raw_data: Raw data from pyshimmer

        Returns:
            dict: Parsed data with GSR and PPG values
        """
        try:
            # This is a simplified parser - actual implementation depends on
            # the structure of data returned by pyshimmer
            parsed = {
                'timestamp': time.time(),
                'gsr': 0.0,
                'ppg': 0.0
            }

            # Parse GSR data (example - adjust based on actual pyshimmer API)
            if hasattr(raw_data, 'gsr'):
                parsed['gsr'] = float(raw_data.gsr)
            elif isinstance(raw_data, dict) and 'gsr' in raw_data:
                parsed['gsr'] = float(raw_data['gsr'])

            # Parse PPG data (example - adjust based on actual pyshimmer API)
            if hasattr(raw_data, 'ppg'):
                parsed['ppg'] = float(raw_data.ppg)
            elif isinstance(raw_data, dict) and 'ppg' in raw_data:
                parsed['ppg'] = float(raw_data['ppg'])

            return parsed

        except Exception as e:
            self.logger.error(f"Error parsing Shimmer data: {e}")
            return None


class ShimmerPC(PCConnectedSensor):
    """
    Concrete implementation of Shimmer GSR sensor driver for PC.

    This class uses the pyshimmer library to communicate with Shimmer sensors
    connected via Bluetooth/COM port.
    """

    def __init__(self, com_port=None, parent=None):
        super().__init__(parent)
        self.com_port = com_port or self._load_com_port_from_config()
        self.shimmer_device = None
        self.data_thread = None
        self.csv_writer = None
        self.csv_file = None
        self.use_cpp_backend = _hardware_backend is not None
        self.logger = get_logger(self.__class__.__name__)

        if self.use_cpp_backend:
            self.logger.info("Using C++ hardware backend for high-precision timing")
        elif pyshimmer is None:
            self.logger.error("Neither C++ backend nor pyshimmer library available")
        else:
            self.logger.info("Using pyshimmer library (fallback mode)")

    def _load_com_port_from_config(self):
        """Load COM port from configuration file."""
        try:
            config = configparser.ConfigParser()
            config_path = os.path.join(os.path.dirname(__file__), '..', '..', 'config.ini')

            if os.path.exists(config_path):
                config.read(config_path)
                return config.get('Hardware', 'shimmer_com_port', fallback='COM5')
            else:
                self.logger.warning("Config file not found, using default COM5")
                return 'COM5'
        except Exception as e:
            self.logger.error(f"Error loading config: {e}")
            return 'COM5'

    def connect(self):
        """Connect to the Shimmer sensor."""
        if self.use_cpp_backend and _hardware_backend:
            try:
                self.logger.info(f"Connecting to Shimmer on {self.com_port} using C++ backend")

                # Initialize C++ NativeShimmer device
                self.shimmer_device = _hardware_backend.NativeShimmer(self.com_port)

                # The C++ backend doesn't have a separate connect step - connection happens on start
                self._is_connected = True
                self.logger.info("C++ Shimmer backend initialized successfully")
                self.connected.emit()

            except Exception as e:
                self.logger.error(f"Error initializing C++ Shimmer backend: {e}")
                self.error.emit(str(e))

        elif pyshimmer is not None:
            try:
                self.logger.info(f"Connecting to Shimmer on {self.com_port} using pyshimmer")

                # Initialize Shimmer device
                self.shimmer_device = pyshimmer.ShimmerBluetooth(self.com_port)

                # Connect to the device
                if self.shimmer_device.connect():
                    self._is_connected = True
                    self.logger.info("Successfully connected to Shimmer")
                    self.connected.emit()
                else:
                    self.error.emit(f"Failed to connect to Shimmer on {self.com_port}")

            except Exception as e:
                self.logger.error(f"Error connecting to Shimmer: {e}")
                self.error.emit(str(e))
        else:
            self.error.emit("No Shimmer backend available (neither C++ nor pyshimmer)")

    def start_streaming(self):
        """Start streaming data from the Shimmer sensor."""
        if not self._is_connected or not self.shimmer_device:
            self.error.emit("Shimmer not connected")
            return

        try:
            if self.use_cpp_backend:
                # Start C++ backend streaming
                if self.shimmer_device.start():
                    self._is_streaming = True

                    # Start the data reading thread with C++ backend
                    self.data_thread = ShimmerDataThread(self.shimmer_device, use_cpp_backend=True, parent=self)
                    self.data_thread.data_received.connect(self._on_data_received)
                    self.data_thread.error_occurred.connect(self._on_thread_error)
                    self.data_thread.start()

                    self.logger.info("Started C++ Shimmer data streaming")
                else:
                    self.error.emit("Failed to start C++ Shimmer streaming")
            else:
                # Start pyshimmer streaming
                if self.shimmer_device.start_streaming():
                    self._is_streaming = True

                    # Start the data reading thread with pyshimmer
                    self.data_thread = ShimmerDataThread(self.shimmer_device, use_cpp_backend=False, parent=self)
                    self.data_thread.data_received.connect(self._on_data_received)
                    self.data_thread.error_occurred.connect(self._on_thread_error)
                    self.data_thread.start()

                    self.logger.info("Started pyshimmer data streaming")
                else:
                    self.error.emit("Failed to start pyshimmer streaming")

        except Exception as e:
            self.logger.error(f"Error starting Shimmer streaming: {e}")
            self.error.emit(str(e))

    def stop_streaming(self):
        """Stop streaming data from the Shimmer sensor."""
        try:
            self._is_streaming = False

            # Stop the data thread
            if self.data_thread:
                self.data_thread.stop()
                self.data_thread.wait(3000)  # Wait up to 3 seconds
                self.data_thread = None

            # Stop streaming on the device
            if self.shimmer_device:
                if self.use_cpp_backend:
                    self.shimmer_device.stop()
                else:
                    self.shimmer_device.stop_streaming()

            # Close CSV file if open
            self._close_csv_file()

            self.logger.info("Stopped Shimmer data streaming")

        except Exception as e:
            self.logger.error(f"Error stopping Shimmer streaming: {e}")
            self.error.emit(str(e))

    def disconnect(self):
        """Disconnect from the Shimmer sensor."""
        try:
            # Stop streaming first
            if self._is_streaming:
                self.stop_streaming()

            # Disconnect from device
            if self.shimmer_device:
                self.shimmer_device.disconnect()
                self.shimmer_device = None

            self._is_connected = False
            self.logger.info("Disconnected from Shimmer")
            self.disconnected.emit()

        except Exception as e:
            self.logger.error(f"Error disconnecting from Shimmer: {e}")
            self.error.emit(str(e))

    def start_recording(self, output_file):
        """
        Start recording data to a CSV file.

        Args:
            output_file (str): Path to the output CSV file
        """
        try:
            self.csv_file = open(output_file, 'w', newline='')
            self.csv_writer = csv.writer(self.csv_file)

            # Write CSV header
            self.csv_writer.writerow(['timestamp', 'gsr', 'ppg'])

            self.logger.info(f"Started recording to {output_file}")

        except Exception as e:
            self.logger.error(f"Error starting recording: {e}")
            self.error.emit(str(e))

    def stop_recording(self):
        """Stop recording data."""
        self._close_csv_file()
        self.logger.info("Stopped recording")

    def _close_csv_file(self):
        """Close the CSV file if open."""
        if self.csv_writer:
            self.csv_writer = None
        if self.csv_file:
            self.csv_file.close()
            self.csv_file = None

    def _on_data_received(self, data):
        """Handle data received from the data thread."""
        # Write to CSV if recording
        if self.csv_writer:
            self.csv_writer.writerow([
                data['timestamp'],
                data['gsr'],
                data['ppg']
            ])
            self.csv_file.flush()  # Ensure data is written immediately

        # Emit the data signal
        self.data_received.emit(data)

    def _on_thread_error(self, error_msg):
        """Handle errors from the data thread."""
        self.error.emit(error_msg)
