"""
Lab Streaming Layer (LSL) integration for real-time data streaming.
This module provides functionality to stream GSR, PPG, and other sensor data
in real-time using the Lab Streaming Layer protocol.
"""

import time
import threading
from typing import Optional, Dict, Any, Callable
import logging

try:
    import pylsl
except ImportError:
    pylsl = None
    logging.warning("pylsl not available. LSL functionality will be disabled.")


class LSLStreamer:
    """
    Manages LSL streams for real-time sensor data broadcasting.
    """
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        self.streams: Dict[str, pylsl.StreamOutlet] = {}
        self.is_streaming = False
        self.stream_thread: Optional[threading.Thread] = None
        
    def create_gsr_stream(self, stream_name: str = "GSR_Stream", 
                         sampling_rate: float = 128.0) -> bool:
        """
        Creates an LSL stream for GSR data.
        
        Args:
            stream_name: Name of the LSL stream
            sampling_rate: Sampling rate in Hz
            
        Returns:
            True if stream was created successfully, False otherwise
        """
        if pylsl is None:
            self.logger.error("pylsl not available")
            return False
            
        try:
            # Create stream info
            info = pylsl.StreamInfo(
                name=stream_name,
                type='GSR',
                channel_count=1,
                nominal_srate=sampling_rate,
                channel_format=pylsl.cf_float32,
                source_id='gsr_sensor_001'
            )
            
            # Add metadata
            channels = info.desc().append_child("channels")
            ch = channels.append_child("channel")
            ch.append_child_value("label", "GSR")
            ch.append_child_value("unit", "microsiemens")
            ch.append_child_value("type", "GSR")
            
            # Create outlet
            outlet = pylsl.StreamOutlet(info)
            self.streams['gsr'] = outlet
            
            self.logger.info(f"Created GSR LSL stream: {stream_name}")
            return True
            
        except Exception as e:
            self.logger.error(f"Error creating GSR stream: {e}")
            return False
    
    def create_ppg_stream(self, stream_name: str = "PPG_Stream", 
                         sampling_rate: float = 128.0) -> bool:
        """
        Creates an LSL stream for PPG/heart rate data.
        
        Args:
            stream_name: Name of the LSL stream
            sampling_rate: Sampling rate in Hz
            
        Returns:
            True if stream was created successfully, False otherwise
        """
        if pylsl is None:
            self.logger.error("pylsl not available")
            return False
            
        try:
            # Create stream info
            info = pylsl.StreamInfo(
                name=stream_name,
                type='PPG',
                channel_count=2,  # PPG value and heart rate
                nominal_srate=sampling_rate,
                channel_format=pylsl.cf_float32,
                source_id='ppg_sensor_001'
            )
            
            # Add metadata
            channels = info.desc().append_child("channels")
            
            ch1 = channels.append_child("channel")
            ch1.append_child_value("label", "PPG")
            ch1.append_child_value("unit", "arbitrary")
            ch1.append_child_value("type", "PPG")
            
            ch2 = channels.append_child("channel")
            ch2.append_child_value("label", "HeartRate")
            ch2.append_child_value("unit", "BPM")
            ch2.append_child_value("type", "HeartRate")
            
            # Create outlet
            outlet = pylsl.StreamOutlet(info)
            self.streams['ppg'] = outlet
            
            self.logger.info(f"Created PPG LSL stream: {stream_name}")
            return True
            
        except Exception as e:
            self.logger.error(f"Error creating PPG stream: {e}")
            return False
    
    def create_marker_stream(self, stream_name: str = "Markers") -> bool:
        """
        Creates an LSL stream for event markers.
        
        Args:
            stream_name: Name of the LSL stream
            
        Returns:
            True if stream was created successfully, False otherwise
        """
        if pylsl is None:
            self.logger.error("pylsl not available")
            return False
            
        try:
            # Create stream info
            info = pylsl.StreamInfo(
                name=stream_name,
                type='Markers',
                channel_count=1,
                nominal_srate=pylsl.IRREGULAR_RATE,
                channel_format=pylsl.cf_string,
                source_id='marker_stream_001'
            )
            
            # Create outlet
            outlet = pylsl.StreamOutlet(info)
            self.streams['markers'] = outlet
            
            self.logger.info(f"Created marker LSL stream: {stream_name}")
            return True
            
        except Exception as e:
            self.logger.error(f"Error creating marker stream: {e}")
            return False
    
    def push_gsr_sample(self, gsr_value: float, timestamp: Optional[float] = None):
        """
        Pushes a GSR sample to the LSL stream.
        
        Args:
            gsr_value: GSR value in microsiemens
            timestamp: Optional timestamp (uses current time if None)
        """
        if 'gsr' in self.streams:
            try:
                if timestamp is None:
                    timestamp = pylsl.local_clock()
                self.streams['gsr'].push_sample([gsr_value], timestamp)
            except Exception as e:
                self.logger.error(f"Error pushing GSR sample: {e}")
    
    def push_ppg_sample(self, ppg_value: float, heart_rate: float, 
                       timestamp: Optional[float] = None):
        """
        Pushes a PPG sample to the LSL stream.
        
        Args:
            ppg_value: PPG value
            heart_rate: Heart rate in BPM
            timestamp: Optional timestamp (uses current time if None)
        """
        if 'ppg' in self.streams:
            try:
                if timestamp is None:
                    timestamp = pylsl.local_clock()
                self.streams['ppg'].push_sample([ppg_value, heart_rate], timestamp)
            except Exception as e:
                self.logger.error(f"Error pushing PPG sample: {e}")
    
    def push_marker(self, marker: str, timestamp: Optional[float] = None):
        """
        Pushes an event marker to the LSL stream.
        
        Args:
            marker: Marker string (e.g., "recording_start", "stimulus_onset")
            timestamp: Optional timestamp (uses current time if None)
        """
        if 'markers' in self.streams:
            try:
                if timestamp is None:
                    timestamp = pylsl.local_clock()
                self.streams['markers'].push_sample([marker], timestamp)
                self.logger.info(f"Pushed marker: {marker}")
            except Exception as e:
                self.logger.error(f"Error pushing marker: {e}")
    
    def start_streaming(self):
        """
        Starts the LSL streaming service.
        """
        if self.is_streaming:
            self.logger.warning("LSL streaming already active")
            return
            
        self.is_streaming = True
        self.logger.info("LSL streaming started")
    
    def stop_streaming(self):
        """
        Stops the LSL streaming service.
        """
        if not self.is_streaming:
            return
            
        self.is_streaming = False
        self.logger.info("LSL streaming stopped")
    
    def close_all_streams(self):
        """
        Closes all LSL streams and releases resources.
        """
        self.stop_streaming()
        
        for stream_name, outlet in self.streams.items():
            try:
                # LSL outlets are automatically cleaned up when they go out of scope
                self.logger.info(f"Closed LSL stream: {stream_name}")
            except Exception as e:
                self.logger.error(f"Error closing stream {stream_name}: {e}")
        
        self.streams.clear()


class LSLReceiver:
    """
    Receives data from LSL streams for analysis or recording.
    """
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        self.inlets: Dict[str, pylsl.StreamInlet] = {}
        self.is_receiving = False
        self.receive_thread: Optional[threading.Thread] = None
        self.data_callbacks: Dict[str, Callable] = {}
    
    def connect_to_stream(self, stream_name: str, stream_type: str = None) -> bool:
        """
        Connects to an LSL stream by name.
        
        Args:
            stream_name: Name of the stream to connect to
            stream_type: Optional stream type filter
            
        Returns:
            True if connection was successful, False otherwise
        """
        if pylsl is None:
            self.logger.error("pylsl not available")
            return False
            
        try:
            # Resolve stream
            streams = pylsl.resolve_stream('name', stream_name)
            if not streams:
                self.logger.error(f"No stream found with name: {stream_name}")
                return False
            
            # Create inlet
            inlet = pylsl.StreamInlet(streams[0])
            self.inlets[stream_name] = inlet
            
            self.logger.info(f"Connected to LSL stream: {stream_name}")
            return True
            
        except Exception as e:
            self.logger.error(f"Error connecting to stream {stream_name}: {e}")
            return False
    
    def set_data_callback(self, stream_name: str, callback: Callable):
        """
        Sets a callback function for received data.
        
        Args:
            stream_name: Name of the stream
            callback: Function to call with received data (sample, timestamp)
        """
        self.data_callbacks[stream_name] = callback
    
    def start_receiving(self):
        """
        Starts receiving data from connected streams.
        """
        if self.is_receiving:
            self.logger.warning("LSL receiving already active")
            return
            
        self.is_receiving = True
        self.receive_thread = threading.Thread(target=self._receive_loop)
        self.receive_thread.daemon = True
        self.receive_thread.start()
        
        self.logger.info("LSL receiving started")
    
    def _receive_loop(self):
        """
        Main receiving loop (runs in separate thread).
        """
        while self.is_receiving:
            for stream_name, inlet in self.inlets.items():
                try:
                    sample, timestamp = inlet.pull_sample(timeout=0.1)
                    if sample is not None:
                        callback = self.data_callbacks.get(stream_name)
                        if callback:
                            callback(sample, timestamp)
                except Exception as e:
                    self.logger.error(f"Error receiving from {stream_name}: {e}")
            
            time.sleep(0.001)  # Small delay to prevent excessive CPU usage
    
    def stop_receiving(self):
        """
        Stops receiving data from streams.
        """
        if not self.is_receiving:
            return
            
        self.is_receiving = False
        if self.receive_thread:
            self.receive_thread.join(timeout=1.0)
        
        self.logger.info("LSL receiving stopped")
    
    def close_all_inlets(self):
        """
        Closes all LSL inlets and releases resources.
        """
        self.stop_receiving()
        
        for stream_name, inlet in self.inlets.items():
            try:
                inlet.close_stream()
                self.logger.info(f"Closed LSL inlet: {stream_name}")
            except Exception as e:
                self.logger.error(f"Error closing inlet {stream_name}: {e}")
        
        self.inlets.clear()
        self.data_callbacks.clear()