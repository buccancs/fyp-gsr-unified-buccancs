import json
import logging
import queue
import random
import threading
import time
from datetime import datetime
from typing import Any, Callable, Dict, List, Optional

logger = logging.getLogger(__name__)

try:
    import serial
    import serial.tools.list_ports

    SERIAL_AVAILABLE = True
except ImportError:
    SERIAL_AVAILABLE = False
    logger.warning("pyserial not available - Shimmer direct connection disabled")

try:
    import pylsl

    LSL_AVAILABLE = True
except ImportError:
    LSL_AVAILABLE = False
    logger.warning("pylsl not available - LSL stream reception disabled")

try:
    import pyshimmer

    PYSHIMMER_AVAILABLE = True
except ImportError:
    PYSHIMMER_AVAILABLE = False
    logger.warning("pyshimmer not available - Shimmer API connection disabled")


class GSRHandler:
    """
    Enhanced GSR handler supporting multiple data sources:
    1. Direct Shimmer device connection via serial/Bluetooth
    2. LSL stream reception from Android devices
    3. PyShimmer API connection for Shimmer devices
    4. Simulation mode for testing
    """

    def __init__(self):
        self.is_capturing = False
        self.capture_thread = None
        self.data_queue = queue.Queue(maxsize=1000)
        self.latest_data = None
        self.data_callbacks = []

        # Configuration
        self.sampling_rate = 128  # Hz
        self.capture_mode = "simulation"  # "simulation", "serial", "lsl", "pyshimmer"

        # Serial connection (for direct Shimmer connection)
        self.serial_connection = None
        self.serial_port = None
        self.serial_baudrate = 115200

        # LSL stream
        self.lsl_inlet = None
        self.lsl_stream_info = None

        # PyShimmer connection
        self.pyshimmer_device = None
        self.pyshimmer_mac_address = None

        # Statistics
        self.total_samples = 0
        self.start_time = None
        self.last_sample_time = None

        logger.info("Enhanced GSRHandler initialized with multiple data source support")

    def add_data_callback(self, callback: Callable[[Dict[str, Any]], None]):
        """Add a callback function to be called when new GSR data is received."""
        self.data_callbacks.append(callback)

    def remove_data_callback(self, callback: Callable[[Dict[str, Any]], None]):
        """Remove a data callback function."""
        if callback in self.data_callbacks:
            self.data_callbacks.remove(callback)

    def set_capture_mode(self, mode: str):
        """Set the capture mode: 'simulation', 'serial', 'lsl', or 'pyshimmer'."""
        if mode not in ["simulation", "serial", "lsl", "pyshimmer"]:
            raise ValueError(
                "Invalid capture mode. Must be 'simulation', 'serial', 'lsl', or 'pyshimmer'"
            )

        if mode == "serial" and not SERIAL_AVAILABLE:
            raise RuntimeError("Serial mode not available - pyserial not installed")

        if mode == "lsl" and not LSL_AVAILABLE:
            raise RuntimeError("LSL mode not available - pylsl not installed")

        if mode == "pyshimmer" and not PYSHIMMER_AVAILABLE:
            raise RuntimeError("PyShimmer mode not available - pyshimmer not installed")

        self.capture_mode = mode
        logger.info(f"GSR capture mode set to: {mode}")

    def configure_serial(self, port: str = None, baudrate: int = 115200):
        """Configure serial connection parameters."""
        if port:
            self.serial_port = port
        self.serial_baudrate = baudrate
        logger.info(
            f"Serial configuration: port={self.serial_port}, baudrate={self.serial_baudrate}"
        )

    def configure_pyshimmer(self, mac_address: str = None):
        """Configure PyShimmer connection parameters."""
        if mac_address:
            self.pyshimmer_mac_address = mac_address
        logger.info(
            f"PyShimmer configuration: mac_address={self.pyshimmer_mac_address}"
        )

    def discover_shimmer_devices(self) -> List[str]:
        """Discover available Shimmer devices on serial ports."""
        if not SERIAL_AVAILABLE:
            logger.warning("Serial not available for device discovery")
            return []

        shimmer_ports = []
        try:
            ports = serial.tools.list_ports.comports()
            for port in ports:
                # Look for Shimmer-like devices (adjust VID/PID as needed)
                if port.description and (
                    "shimmer" in port.description.lower()
                    or "bluetooth" in port.description.lower()
                ):
                    shimmer_ports.append(port.device)
                    logger.info(
                        f"Found potential Shimmer device: {port.device} - {port.description}"
                    )
        except Exception as e:
            logger.error(f"Error discovering Shimmer devices: {e}")

        return shimmer_ports

    def discover_lsl_streams(self) -> List[Dict[str, str]]:
        """Discover available GSR LSL streams."""
        if not LSL_AVAILABLE:
            logger.warning("LSL not available for stream discovery")
            return []

        try:
            logger.info("Discovering GSR LSL streams...")
            streams = pylsl.resolve_stream("type", "GSR", timeout=2.0)

            stream_info = []
            for stream in streams:
                info = {
                    "name": stream.name(),
                    "type": stream.type(),
                    "source_id": stream.source_id(),
                    "hostname": stream.hostname(),
                    "channel_count": stream.channel_count(),
                    "sampling_rate": stream.nominal_srate(),
                }
                stream_info.append(info)
                logger.info(f"Found GSR stream: {info}")

            return stream_info
        except Exception as e:
            logger.error(f"Error discovering LSL streams: {e}")
            return []

    def start_gsr_capture(self):
        """Start GSR data capture based on configured mode."""
        if self.is_capturing:
            logger.warning("GSR capture already running")
            return False

        logger.info(f"Starting GSR capture in {self.capture_mode} mode")

        try:
            if self.capture_mode == "serial":
                return self._start_serial_capture()
            elif self.capture_mode == "lsl":
                return self._start_lsl_capture()
            elif self.capture_mode == "pyshimmer":
                return self._start_pyshimmer_capture()
            else:  # simulation
                return self._start_simulation_capture()
        except Exception as e:
            logger.error(f"Failed to start GSR capture: {e}")
            return False

    def _start_serial_capture(self) -> bool:
        """Start serial-based GSR capture from Shimmer device."""
        if not SERIAL_AVAILABLE:
            logger.error("Serial not available")
            return False

        try:
            # Auto-discover if no port specified
            if not self.serial_port:
                ports = self.discover_shimmer_devices()
                if ports:
                    self.serial_port = ports[0]
                    logger.info(f"Auto-selected serial port: {self.serial_port}")
                else:
                    logger.error("No Shimmer devices found")
                    return False

            # Open serial connection
            self.serial_connection = serial.Serial(
                port=self.serial_port, baudrate=self.serial_baudrate, timeout=1.0
            )

            logger.info(f"Connected to Shimmer device on {self.serial_port}")

            # Start capture thread
            self.is_capturing = True
            self.start_time = time.time()
            self.capture_thread = threading.Thread(
                target=self._serial_capture_loop, daemon=True
            )
            self.capture_thread.start()

            return True

        except Exception as e:
            logger.error(f"Failed to start serial capture: {e}")
            return False

    def _start_lsl_capture(self) -> bool:
        """Start LSL-based GSR capture from Android devices."""
        if not LSL_AVAILABLE:
            logger.error("LSL not available")
            return False

        try:
            # Discover GSR streams
            streams = pylsl.resolve_stream("type", "GSR", timeout=5.0)
            if not streams:
                logger.error("No GSR LSL streams found")
                return False

            # Use the first available stream
            self.lsl_stream_info = streams[0]
            self.lsl_inlet = pylsl.StreamInlet(self.lsl_stream_info)

            logger.info(f"Connected to GSR LSL stream: {self.lsl_stream_info.name()}")

            # Start capture thread
            self.is_capturing = True
            self.start_time = time.time()
            self.capture_thread = threading.Thread(
                target=self._lsl_capture_loop, daemon=True
            )
            self.capture_thread.start()

            return True

        except Exception as e:
            logger.error(f"Failed to start LSL capture: {e}")
            return False

    def _start_simulation_capture(self) -> bool:
        """Start simulation-based GSR capture."""
        logger.info("Starting GSR simulation")

        self.is_capturing = True
        self.start_time = time.time()
        self.capture_thread = threading.Thread(
            target=self._simulation_capture_loop, daemon=True
        )
        self.capture_thread.start()

        return True

    def _start_pyshimmer_capture(self) -> bool:
        """Start PyShimmer-based GSR capture from Shimmer device."""
        if not PYSHIMMER_AVAILABLE:
            logger.error("PyShimmer not available")
            return False

        try:
            # Initialize PyShimmer device
            if not self.pyshimmer_mac_address:
                logger.error("No MAC address specified for PyShimmer device")
                return False

            # Connect to Shimmer device using PyShimmer
            self.pyshimmer_device = pyshimmer.ShimmerBluetooth(
                self.pyshimmer_mac_address
            )
            self.pyshimmer_device.connect()

            # Configure GSR sensor
            self.pyshimmer_device.set_sensors([pyshimmer.SENSOR_GSR])
            self.pyshimmer_device.set_sampling_rate(self.sampling_rate)

            logger.info(
                f"Connected to Shimmer device via PyShimmer: {self.pyshimmer_mac_address}"
            )

            # Start capture thread
            self.is_capturing = True
            self.start_time = time.time()
            self.capture_thread = threading.Thread(
                target=self._pyshimmer_capture_loop, daemon=True
            )
            self.capture_thread.start()

            return True

        except Exception as e:
            logger.error(f"Failed to start PyShimmer capture: {e}")
            return False

    def _serial_capture_loop(self):
        """Serial capture loop for Shimmer device."""
        logger.info("Starting serial GSR capture loop")

        try:
            while self.is_capturing and self.serial_connection:
                try:
                    # Read data from Shimmer device
                    # This is a simplified example - actual Shimmer protocol is more complex
                    line = self.serial_connection.readline().decode("utf-8").strip()

                    if line:
                        # Parse Shimmer data format (adjust based on actual format)
                        try:
                            parts = line.split(",")
                            if len(parts) >= 2:
                                timestamp = float(parts[0])
                                gsr_value = float(parts[1])

                                data = {
                                    "timestamp": time.time(),
                                    "shimmer_timestamp": timestamp,
                                    "conductance": gsr_value,
                                    "resistance": (
                                        1.0 / gsr_value if gsr_value > 0 else 0
                                    ),
                                    "unit": "microsiemens",
                                    "source": "shimmer_serial",
                                    "quality": 100,
                                }

                                self._process_gsr_data(data)
                        except (ValueError, IndexError) as e:
                            logger.debug(f"Failed to parse Shimmer data: {line} - {e}")

                except Exception as e:
                    logger.error(f"Error in serial capture loop: {e}")
                    time.sleep(0.1)

        except Exception as e:
            logger.error(f"Serial capture loop error: {e}")
        finally:
            logger.info("Serial GSR capture loop stopped")

    def _lsl_capture_loop(self):
        """LSL capture loop for Android device streams."""
        logger.info("Starting LSL GSR capture loop")

        try:
            while self.is_capturing and self.lsl_inlet:
                try:
                    # Pull sample from LSL stream
                    sample, timestamp = self.lsl_inlet.pull_sample(timeout=1.0)

                    if sample:
                        # Parse LSL GSR data (adjust based on actual format)
                        data = {
                            "timestamp": time.time(),
                            "lsl_timestamp": timestamp,
                            "conductance": sample[0] if len(sample) > 0 else 0,
                            "resistance": sample[1] if len(sample) > 1 else 0,
                            "quality": sample[2] if len(sample) > 2 else 100,
                            "unit": "microsiemens",
                            "source": "android_lsl",
                        }

                        self._process_gsr_data(data)

                except Exception as e:
                    logger.error(f"Error in LSL capture loop: {e}")
                    time.sleep(0.1)

        except Exception as e:
            logger.error(f"LSL capture loop error: {e}")
        finally:
            logger.info("LSL GSR capture loop stopped")

    def _simulation_capture_loop(self):
        """Simulation capture loop for testing."""
        logger.info("Starting GSR simulation loop")

        try:
            while self.is_capturing:
                try:
                    # Generate realistic GSR simulation
                    current_time = time.time()
                    elapsed = current_time - self.start_time

                    # Realistic GSR simulation with physiological patterns
                    baseline = 15.0
                    slow_drift = (
                        2.0
                        * (0.5 + 0.5 * random.random())
                        * (1 + 0.3 * random.random())
                    )
                    heart_rate = 0.5 * random.random()
                    noise = 0.8 * (random.random() - 0.5)

                    # Occasional skin conductance responses
                    scr = 8.0 * random.random() if random.random() < 0.002 else 0

                    conductance = max(
                        0.1, baseline + slow_drift + heart_rate + noise + scr
                    )
                    resistance = 1.0 / conductance if conductance > 0 else 0

                    data = {
                        "timestamp": current_time,
                        "conductance": conductance,
                        "resistance": resistance,
                        "unit": "microsiemens",
                        "source": "simulation",
                        "quality": 95 + 5 * random.random(),
                    }

                    self._process_gsr_data(data)

                    # Maintain sampling rate
                    time.sleep(1.0 / self.sampling_rate)

                except Exception as e:
                    logger.error(f"Error in simulation loop: {e}")
                    time.sleep(0.1)

        except Exception as e:
            logger.error(f"Simulation loop error: {e}")
        finally:
            logger.info("GSR simulation loop stopped")

    def _pyshimmer_capture_loop(self):
        """PyShimmer capture loop for Shimmer device."""
        logger.info("Starting PyShimmer GSR capture loop")

        try:
            # Start streaming from Shimmer device
            self.pyshimmer_device.start_streaming()

            while self.is_capturing and self.pyshimmer_device:
                try:
                    # Get data from PyShimmer device
                    if self.pyshimmer_device.is_streaming():
                        # Read data packet from Shimmer
                        data_packet = self.pyshimmer_device.read_data_packet()

                        if data_packet:
                            # Extract GSR data from packet
                            gsr_data = data_packet.get("GSR", {})

                            if gsr_data:
                                conductance = gsr_data.get("conductance", 0)
                                resistance = gsr_data.get("resistance", 0)

                                data = {
                                    "timestamp": time.time(),
                                    "shimmer_timestamp": data_packet.get(
                                        "timestamp", time.time()
                                    ),
                                    "conductance": conductance,
                                    "resistance": resistance,
                                    "unit": "microsiemens",
                                    "source": "pyshimmer",
                                    "quality": 100,
                                }

                                self._process_gsr_data(data)

                    # Maintain sampling rate
                    time.sleep(1.0 / self.sampling_rate)

                except Exception as e:
                    logger.error(f"Error in PyShimmer capture loop: {e}")
                    time.sleep(0.1)

        except Exception as e:
            logger.error(f"PyShimmer capture loop error: {e}")
        finally:
            # Stop streaming and disconnect
            try:
                if self.pyshimmer_device:
                    self.pyshimmer_device.stop_streaming()
                    self.pyshimmer_device.disconnect()
            except Exception as e:
                logger.error(f"Error stopping PyShimmer device: {e}")
            logger.info("PyShimmer GSR capture loop stopped")

    def _process_gsr_data(self, data: Dict[str, Any]):
        """Process and distribute GSR data."""
        try:
            # Update statistics
            self.total_samples += 1
            self.last_sample_time = data["timestamp"]
            self.latest_data = data

            # Add to queue (non-blocking)
            try:
                self.data_queue.put_nowait(data)
            except queue.Full:
                # Remove oldest sample if queue is full
                try:
                    self.data_queue.get_nowait()
                    self.data_queue.put_nowait(data)
                except queue.Empty:
                    pass

            # Call callbacks
            for callback in self.data_callbacks:
                try:
                    callback(data)
                except Exception as e:
                    logger.error(f"Error in GSR data callback: {e}")

        except Exception as e:
            logger.error(f"Error processing GSR data: {e}")

    def stop_gsr_capture(self):
        """Stop GSR data capture."""
        if not self.is_capturing:
            logger.warning("GSR capture not running")
            return

        logger.info("Stopping GSR capture")
        self.is_capturing = False

        # Wait for capture thread to finish
        if self.capture_thread and self.capture_thread.is_alive():
            self.capture_thread.join(timeout=2.0)

        # Close connections
        if self.serial_connection:
            try:
                self.serial_connection.close()
            except Exception as e:
                logger.error(f"Error closing serial connection: {e}")
            self.serial_connection = None

        if self.lsl_inlet:
            try:
                self.lsl_inlet.close_stream()
            except Exception as e:
                logger.error(f"Error closing LSL inlet: {e}")
            self.lsl_inlet = None

        if self.pyshimmer_device:
            try:
                self.pyshimmer_device.stop_streaming()
                self.pyshimmer_device.disconnect()
            except Exception as e:
                logger.error(f"Error disconnecting PyShimmer device: {e}")
            self.pyshimmer_device = None

        logger.info("GSR capture stopped")

    def get_latest_gsr_data(self) -> Optional[Dict[str, Any]]:
        """Get the most recent GSR data sample."""
        return self.latest_data

    def get_gsr_data_batch(self, max_samples: int = 100) -> List[Dict[str, Any]]:
        """Get a batch of GSR data samples from the queue."""
        samples = []
        for _ in range(max_samples):
            try:
                sample = self.data_queue.get_nowait()
                samples.append(sample)
            except queue.Empty:
                break
        return samples

    def get_capture_statistics(self) -> Dict[str, Any]:
        """Get capture statistics."""
        current_time = time.time()
        duration = current_time - self.start_time if self.start_time else 0

        return {
            "is_capturing": self.is_capturing,
            "capture_mode": self.capture_mode,
            "total_samples": self.total_samples,
            "duration_seconds": duration,
            "average_sample_rate": self.total_samples / duration if duration > 0 else 0,
            "last_sample_time": self.last_sample_time,
            "queue_size": self.data_queue.qsize(),
            "serial_port": self.serial_port,
            "lsl_stream_name": (
                self.lsl_stream_info.name() if self.lsl_stream_info else None
            ),
            "pyshimmer_mac_address": self.pyshimmer_mac_address,
            "pyshimmer_connected": self.pyshimmer_device is not None,
        }

    def is_capturing_data(self) -> bool:
        """Check if currently capturing GSR data."""
        return self.is_capturing

    def __del__(self):
        """Cleanup when object is destroyed."""
        try:
            self.stop_gsr_capture()
        except:
            pass
