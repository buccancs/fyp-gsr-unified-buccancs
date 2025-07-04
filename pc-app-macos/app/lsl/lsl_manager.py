"""
LSL (Lab Streaming Layer) Manager for PC Application
Handles discovery and reception of LSL streams from Android devices
"""

import base64
import logging
import queue
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Callable, Dict, List, Optional

import pylsl

# Import generated protobuf classes
try:
    import messages_pb2

    PROTOBUF_AVAILABLE = True
except ImportError:
    PROTOBUF_AVAILABLE = False
    logging.warning(
        "Protobuf classes not available. Run 'protoc --python_out=. messages.proto' to generate them."
    )

# Import protobuf messages (will be generated from proto files)
# TODO: Update imports once protobuf Python files are generated
# from gsrunified import messages_pb2

logger = logging.getLogger(__name__)


class LslInletManager:
    """
    Manages LSL inlets for receiving data from Android devices
    """

    def __init__(self):
        self.inlets: Dict[str, pylsl.StreamInlet] = {}
        self.streams: Dict[str, pylsl.StreamInfo] = {}
        self.data_callbacks: Dict[str, Callable] = {}
        self.running = False
        self.discovery_thread: Optional[threading.Thread] = None
        self.data_threads: Dict[str, threading.Thread] = {}
        self.executor = ThreadPoolExecutor(max_workers=10)
        self.data_queue = queue.Queue()

    def start(self):
        """Start the LSL inlet manager"""
        if self.running:
            logger.warning("LSL Inlet Manager already running")
            return

        logger.info("Starting LSL Inlet Manager")
        self.running = True

        # Start stream discovery
        self.discovery_thread = threading.Thread(
            target=self._discovery_loop, daemon=True
        )
        self.discovery_thread.start()

    def stop(self):
        """Stop the LSL inlet manager and cleanup resources"""
        logger.info("Stopping LSL Inlet Manager")
        self.running = False

        # Stop all data threads
        for thread in self.data_threads.values():
            if thread.is_alive():
                thread.join(timeout=1.0)

        # Close all inlets
        for inlet in self.inlets.values():
            inlet.close_stream()

        self.inlets.clear()
        self.streams.clear()
        self.data_threads.clear()

        if self.discovery_thread and self.discovery_thread.is_alive():
            self.discovery_thread.join(timeout=2.0)

        self.executor.shutdown(wait=True)

    def register_data_callback(self, stream_type: str, callback: Callable):
        """Register a callback function for a specific stream type"""
        self.data_callbacks[stream_type] = callback
        logger.info(f"Registered callback for stream type: {stream_type}")

    def get_discovered_streams(self) -> List[Dict[str, Any]]:
        """Get list of discovered streams with their metadata"""
        streams = []
        for stream_name, stream_info in self.streams.items():
            streams.append(
                {
                    "name": stream_info.name(),
                    "type": stream_info.type(),
                    "source_id": stream_info.source_id(),
                    "channel_count": stream_info.channel_count(),
                    "nominal_srate": stream_info.nominal_srate(),
                    "connected": stream_name in self.inlets,
                }
            )
        return streams

    def connect_to_stream(self, stream_name: str) -> bool:
        """Connect to a specific stream"""
        if stream_name in self.inlets:
            logger.warning(f"Already connected to stream: {stream_name}")
            return True

        if stream_name not in self.streams:
            logger.error(f"Stream not found: {stream_name}")
            return False

        try:
            stream_info = self.streams[stream_name]
            inlet = pylsl.StreamInlet(stream_info, max_buflen=360, max_chunklen=0)
            self.inlets[stream_name] = inlet

            # Start data reception thread
            data_thread = threading.Thread(
                target=self._data_reception_loop,
                args=(stream_name, inlet, stream_info),
                daemon=True,
            )
            data_thread.start()
            self.data_threads[stream_name] = data_thread

            logger.info(f"Connected to stream: {stream_name}")
            return True

        except Exception as e:
            logger.error(f"Failed to connect to stream {stream_name}: {e}")
            return False

    def disconnect_from_stream(self, stream_name: str):
        """Disconnect from a specific stream"""
        if stream_name in self.inlets:
            self.inlets[stream_name].close_stream()
            del self.inlets[stream_name]

        if stream_name in self.data_threads:
            thread = self.data_threads[stream_name]
            if thread.is_alive():
                thread.join(timeout=1.0)
            del self.data_threads[stream_name]

        logger.info(f"Disconnected from stream: {stream_name}")

    def _discovery_loop(self):
        """Continuously discover new LSL streams"""
        logger.info("Starting stream discovery loop")

        while self.running:
            try:
                # Discover streams with timeout
                streams = pylsl.resolve_streams(wait_time=2.0)

                # Process discovered streams
                for stream_info in streams:
                    stream_name = stream_info.name()
                    stream_type = stream_info.type()

                    # Filter for our application streams
                    if any(
                        t in stream_type
                        for t in ["GSR", "Thermal", "Command", "Camera"]
                    ):
                        if stream_name not in self.streams:
                            self.streams[stream_name] = stream_info
                            logger.info(
                                f"Discovered new stream: {stream_name} (type: {stream_type})"
                            )

                            # Auto-connect to data streams
                            if stream_type in ["GSR", "Thermal"]:
                                self.connect_to_stream(stream_name)

                time.sleep(5.0)  # Discovery interval

            except Exception as e:
                logger.error(f"Error in discovery loop: {e}")
                time.sleep(5.0)

    def _data_reception_loop(
        self, stream_name: str, inlet: pylsl.StreamInlet, stream_info: pylsl.StreamInfo
    ):
        """Receive data from a specific stream"""
        logger.info(f"Starting data reception for stream: {stream_name}")
        stream_type = stream_info.type()

        try:
            while self.running and stream_name in self.inlets:
                if stream_type in ["GSR", "Thermal"]:
                    # Numeric data streams
                    samples, timestamps = inlet.pull_chunk(timeout=1.0, max_samples=100)

                    if samples:
                        for sample, timestamp in zip(samples, timestamps):
                            self._process_numeric_data(
                                stream_name, stream_type, sample, timestamp
                            )

                elif stream_type in ["CommandResponse"]:
                    # String data streams (serialized protobuf)
                    samples, timestamps = inlet.pull_chunk(timeout=1.0, max_samples=10)

                    if samples:
                        for sample, timestamp in zip(samples, timestamps):
                            self._process_string_data(
                                stream_name, stream_type, sample[0], timestamp
                            )

                else:
                    time.sleep(0.1)  # Unknown stream type

        except Exception as e:
            logger.error(f"Error in data reception for {stream_name}: {e}")
        finally:
            logger.info(f"Data reception stopped for stream: {stream_name}")

    def _process_numeric_data(
        self, stream_name: str, stream_type: str, sample: List[float], timestamp: float
    ):
        """Process numeric data samples"""
        try:
            data = {
                "stream_name": stream_name,
                "stream_type": stream_type,
                "timestamp": timestamp,
                "sample": sample,
            }

            if stream_type == "GSR" and len(sample) >= 3:
                data.update(
                    {
                        "conductance": sample[0],
                        "resistance": sample[1],
                        "quality": int(sample[2]),
                    }
                )

            elif stream_type == "Thermal" and len(sample) >= 6:
                data.update(
                    {
                        "width": int(sample[0]),
                        "height": int(sample[1]),
                        "min_temp": sample[2],
                        "max_temp": sample[3],
                        "avg_temp": sample[4],
                        "frame_number": int(sample[5]),
                    }
                )

            # Call registered callback
            if stream_type in self.data_callbacks:
                self.data_callbacks[stream_type](data)

            # Add to data queue for external processing
            self.data_queue.put(data)

        except Exception as e:
            logger.error(f"Error processing numeric data: {e}")

    def _process_string_data(
        self, stream_name: str, stream_type: str, sample: str, timestamp: float
    ):
        """Process string data samples (typically serialized protobuf)"""
        try:
            # Decode base64 encoded protobuf data
            decoded_data = base64.b64decode(sample)

            data = {
                "stream_name": stream_name,
                "stream_type": stream_type,
                "timestamp": timestamp,
                "raw_data": decoded_data,
                "sample": sample,
            }

            # Deserialize protobuf message if classes are available
            if PROTOBUF_AVAILABLE:
                try:
                    if stream_type == "CommandResponse":
                        response = messages_pb2.CommandResponse()
                        response.ParseFromString(decoded_data)
                        data.update(
                            {
                                "status": response.status,
                                "message": response.message,
                                "device_id": response.device_id,
                                "response_timestamp": {
                                    "seconds": response.timestamp.seconds,
                                    "nanos": response.timestamp.nanos,
                                },
                            }
                        )

                    elif stream_type == "GSRData":
                        gsr_data = messages_pb2.GSRData()
                        gsr_data.ParseFromString(decoded_data)
                        data.update(
                            {
                                "conductance": gsr_data.conductance,
                                "resistance": gsr_data.resistance,
                                "quality": gsr_data.quality,
                                "device_id": gsr_data.device_id,
                                "gsr_timestamp": {
                                    "seconds": gsr_data.timestamp.seconds,
                                    "nanos": gsr_data.timestamp.nanos,
                                },
                            }
                        )

                    elif stream_type == "ThermalData":
                        thermal_data = messages_pb2.ThermalData()
                        thermal_data.ParseFromString(decoded_data)
                        data.update(
                            {
                                "width": thermal_data.width,
                                "height": thermal_data.height,
                                "temperature_matrix": list(
                                    thermal_data.temperature_matrix
                                ),
                                "min_temp": thermal_data.min_temp,
                                "max_temp": thermal_data.max_temp,
                                "avg_temp": thermal_data.avg_temp,
                                "device_id": thermal_data.device_id,
                                "frame_number": thermal_data.frame_number,
                                "thermal_timestamp": {
                                    "seconds": thermal_data.timestamp.seconds,
                                    "nanos": thermal_data.timestamp.nanos,
                                },
                            }
                        )

                    elif stream_type == "CameraFrame":
                        camera_frame = messages_pb2.CameraFrame()
                        camera_frame.ParseFromString(decoded_data)
                        data.update(
                            {
                                "width": camera_frame.width,
                                "height": camera_frame.height,
                                "image_data": camera_frame.image_data,
                                "device_id": camera_frame.device_id,
                                "frame_number": camera_frame.frame_number,
                                "camera_timestamp": {
                                    "seconds": camera_frame.timestamp.seconds,
                                    "nanos": camera_frame.timestamp.nanos,
                                },
                            }
                        )

                    elif stream_type == "DeviceStatus":
                        device_status = messages_pb2.DeviceStatus()
                        device_status.ParseFromString(decoded_data)
                        data.update(
                            {
                                "device_id": device_status.device_id,
                                "gsr_connected": device_status.gsr_connected,
                                "thermal_connected": device_status.thermal_connected,
                                "camera_active": device_status.camera_active,
                                "recording": device_status.recording,
                                "battery_level": device_status.battery_level,
                                "status_message": device_status.status_message,
                                "status_timestamp": {
                                    "seconds": device_status.timestamp.seconds,
                                    "nanos": device_status.timestamp.nanos,
                                },
                            }
                        )

                    elif stream_type == "TimeSyncResponse":
                        sync_response = messages_pb2.TimeSyncResponse()
                        sync_response.ParseFromString(decoded_data)
                        data.update(
                            {
                                "device_id": sync_response.device_id,
                                "round_trip_time_ms": sync_response.round_trip_time_ms,
                                "pc_timestamp": {
                                    "seconds": sync_response.pc_timestamp.seconds,
                                    "nanos": sync_response.pc_timestamp.nanos,
                                },
                                "device_timestamp": {
                                    "seconds": sync_response.device_timestamp.seconds,
                                    "nanos": sync_response.device_timestamp.nanos,
                                },
                            }
                        )

                    else:
                        logger.warning(f"Unknown protobuf message type: {stream_type}")

                except Exception as protobuf_error:
                    logger.error(
                        f"Error deserializing protobuf for {stream_type}: {protobuf_error}"
                    )
                    # Continue with raw data if protobuf parsing fails
            else:
                logger.debug(
                    f"Protobuf classes not available, using raw data for {stream_type}"
                )

            # Call registered callback
            if stream_type in self.data_callbacks:
                self.data_callbacks[stream_type](data)

            # Add to data queue
            self.data_queue.put(data)

        except Exception as e:
            logger.error(f"Error processing string data: {e}")

    def get_latest_data(self, timeout: float = 0.1) -> Optional[Dict[str, Any]]:
        """Get the latest data from the queue"""
        try:
            return self.data_queue.get(timeout=timeout)
        except queue.Empty:
            return None

    def get_stream_info(self, stream_name: str) -> Optional[Dict[str, Any]]:
        """Get detailed information about a specific stream"""
        if stream_name not in self.streams:
            return None

        stream_info = self.streams[stream_name]
        return {
            "name": stream_info.name(),
            "type": stream_info.type(),
            "source_id": stream_info.source_id(),
            "channel_count": stream_info.channel_count(),
            "nominal_srate": stream_info.nominal_srate(),
            "channel_format": stream_info.channel_format(),
            "version": stream_info.version(),
            "created_at": stream_info.created_at(),
            "uid": stream_info.uid(),
            "session_id": stream_info.session_id(),
            "hostname": stream_info.hostname(),
            "connected": stream_name in self.inlets,
        }


class LslCommandSender:
    """
    Sends commands to Android devices via LSL
    """

    def __init__(self, device_id: str = "PC_Controller"):
        self.device_id = device_id
        self.command_outlet: Optional[pylsl.StreamOutlet] = None
        self._setup_command_outlet()

    def _setup_command_outlet(self):
        """Setup LSL outlet for sending commands"""
        try:
            stream_info = pylsl.StreamInfo(
                f"Command_{self.device_id}",
                "Command",
                1,  # single channel for serialized protobuf data
                pylsl.IRREGULAR_RATE,
                pylsl.cf_string,
                self.device_id,
            )

            self.command_outlet = pylsl.StreamOutlet(stream_info)
            logger.info(f"Created command outlet: Command_{self.device_id}")

        except Exception as e:
            logger.error(f"Failed to create command outlet: {e}")

    def send_command(self, command_data: str, timestamp: Optional[float] = None):
        """Send a command via LSL"""
        if not self.command_outlet:
            logger.error("Command outlet not available")
            return False

        try:
            if timestamp is None:
                timestamp = pylsl.local_clock()

            self.command_outlet.push_sample([command_data], timestamp)
            logger.debug(
                f"Sent command: {command_data[:100]}..."
            )  # Log first 100 chars
            return True

        except Exception as e:
            logger.error(f"Failed to send command: {e}")
            return False

    def cleanup(self):
        """Cleanup command sender resources"""
        if self.command_outlet:
            self.command_outlet.close()
            self.command_outlet = None
