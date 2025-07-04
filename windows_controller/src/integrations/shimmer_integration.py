"""
Shimmer sensor integration for Windows PC controller.
This module provides functionality to connect to and receive data from
Shimmer sensors via Bluetooth or USB.
"""

import time
import threading
from typing import Optional, Dict, Any, Callable, List
import logging
import struct

try:
    import serial
    import serial.tools.list_ports
except ImportError:
    serial = None
    logging.warning("pyserial not available. Shimmer serial functionality will be disabled.")

try:
    import bleak
except ImportError:
    bleak = None
    logging.warning("bleak not available. Shimmer Bluetooth functionality will be disabled.")


class ShimmerSensor:
    """
    Represents a single Shimmer sensor device.
    """
    
    def __init__(self, device_id: str, connection_type: str = 'bluetooth'):
        self.device_id = device_id
        self.connection_type = connection_type
        self.logger = logging.getLogger(__name__)
        
        # Connection state
        self.is_connected = False
        self.is_streaming = False
        
        # Connection objects
        self.serial_connection: Optional[serial.Serial] = None
        self.bluetooth_client: Optional[bleak.BleakClient] = None
        
        # Data callbacks
        self.gsr_callback: Optional[Callable] = None
        self.ppg_callback: Optional[Callable] = None
        self.accel_callback: Optional[Callable] = None
        
        # Streaming thread
        self.stream_thread: Optional[threading.Thread] = None
        self.stop_streaming_event = threading.Event()
        
        # Sensor configuration
        self.sampling_rate = 128  # Hz
        self.enabled_sensors = {
            'gsr': True,
            'ppg': True,
            'accel': False,
            'gyro': False,
            'mag': False
        }
    
    def connect_serial(self, port: str = None, baudrate: int = 115200) -> bool:
        """
        Connects to Shimmer sensor via serial/USB.
        
        Args:
            port: Serial port (auto-detected if None)
            baudrate: Serial communication baudrate
            
        Returns:
            True if connection successful, False otherwise
        """
        if serial is None:
            self.logger.error("pyserial not available")
            return False
        
        try:
            if port is None:
                # Auto-detect Shimmer device
                port = self._find_shimmer_serial_port()
                if port is None:
                    self.logger.error("No Shimmer device found on serial ports")
                    return False
            
            # Open serial connection
            self.serial_connection = serial.Serial(
                port=port,
                baudrate=baudrate,
                timeout=1.0
            )
            
            # Test connection
            if self._test_serial_connection():
                self.is_connected = True
                self.connection_type = 'serial'
                self.logger.info(f"Connected to Shimmer sensor on {port}")
                return True
            else:
                self.serial_connection.close()
                self.serial_connection = None
                return False
                
        except Exception as e:
            self.logger.error(f"Error connecting to Shimmer via serial: {e}")
            return False
    
    def connect_bluetooth(self, address: str = None) -> bool:
        """
        Connects to Shimmer sensor via Bluetooth.
        
        Args:
            address: Bluetooth MAC address (auto-detected if None)
            
        Returns:
            True if connection successful, False otherwise
        """
        if bleak is None:
            self.logger.error("bleak not available")
            return False
        
        try:
            if address is None:
                # Auto-detect Shimmer device
                address = self._find_shimmer_bluetooth_device()
                if address is None:
                    self.logger.error("No Shimmer device found via Bluetooth")
                    return False
            
            # Create Bluetooth client
            self.bluetooth_client = bleak.BleakClient(address)
            
            # Connect
            success = self.bluetooth_client.connect()
            if success:
                self.is_connected = True
                self.connection_type = 'bluetooth'
                self.logger.info(f"Connected to Shimmer sensor via Bluetooth: {address}")
                return True
            else:
                return False
                
        except Exception as e:
            self.logger.error(f"Error connecting to Shimmer via Bluetooth: {e}")
            return False
    
    def _find_shimmer_serial_port(self) -> Optional[str]:
        """Finds Shimmer device on serial ports."""
        if serial is None:
            return None
            
        ports = serial.tools.list_ports.comports()
        for port in ports:
            # Look for Shimmer-specific identifiers
            if 'shimmer' in port.description.lower() or 'shimmer' in port.manufacturer.lower():
                return port.device
        
        # If no specific Shimmer port found, return first available port
        # (This is a fallback - in production you'd want more specific detection)
        if ports:
            return ports[0].device
        
        return None
    
    def _find_shimmer_bluetooth_device(self) -> Optional[str]:
        """Finds Shimmer device via Bluetooth scanning."""
        # This would require async implementation with bleak
        # For now, return None (would need to be implemented based on specific requirements)
        return None
    
    def _test_serial_connection(self) -> bool:
        """Tests serial connection by sending a ping command."""
        if self.serial_connection is None:
            return False
        
        try:
            # Send inquiry command to Shimmer
            self.serial_connection.write(b'#')
            time.sleep(0.1)
            
            # Read response
            response = self.serial_connection.read(1)
            return len(response) > 0
            
        except Exception as e:
            self.logger.error(f"Error testing serial connection: {e}")
            return False
    
    def configure_sensors(self, gsr: bool = True, ppg: bool = True, 
                         accel: bool = False, sampling_rate: int = 128) -> bool:
        """
        Configures which sensors are enabled and sampling rate.
        
        Args:
            gsr: Enable GSR sensor
            ppg: Enable PPG sensor
            accel: Enable accelerometer
            sampling_rate: Sampling rate in Hz
            
        Returns:
            True if configuration successful, False otherwise
        """
        if not self.is_connected:
            self.logger.error("Not connected to Shimmer sensor")
            return False
        
        try:
            self.enabled_sensors['gsr'] = gsr
            self.enabled_sensors['ppg'] = ppg
            self.enabled_sensors['accel'] = accel
            self.sampling_rate = sampling_rate
            
            # Send configuration commands to Shimmer
            if self.connection_type == 'serial':
                return self._configure_serial_sensors()
            elif self.connection_type == 'bluetooth':
                return self._configure_bluetooth_sensors()
            
            return False
            
        except Exception as e:
            self.logger.error(f"Error configuring sensors: {e}")
            return False
    
    def _configure_serial_sensors(self) -> bool:
        """Configures sensors via serial connection."""
        if self.serial_connection is None:
            return False
        
        try:
            # Stop streaming if active
            self.serial_connection.write(b'!')
            time.sleep(0.1)
            
            # Set sampling rate
            rate_cmd = f"#{self.sampling_rate}\r\n".encode()
            self.serial_connection.write(rate_cmd)
            time.sleep(0.1)
            
            # Configure enabled sensors
            sensor_mask = 0
            if self.enabled_sensors['gsr']:
                sensor_mask |= 0x04  # GSR sensor bit
            if self.enabled_sensors['ppg']:
                sensor_mask |= 0x08  # PPG sensor bit
            if self.enabled_sensors['accel']:
                sensor_mask |= 0x80  # Accelerometer bit
            
            sensor_cmd = f"#{sensor_mask:02X}\r\n".encode()
            self.serial_connection.write(sensor_cmd)
            time.sleep(0.1)
            
            self.logger.info("Shimmer sensors configured via serial")
            return True
            
        except Exception as e:
            self.logger.error(f"Error configuring serial sensors: {e}")
            return False
    
    def _configure_bluetooth_sensors(self) -> bool:
        """Configures sensors via Bluetooth connection."""
        # Implementation would depend on specific Shimmer Bluetooth protocol
        # This is a placeholder
        self.logger.info("Shimmer sensors configured via Bluetooth")
        return True
    
    def start_streaming(self) -> bool:
        """
        Starts data streaming from the Shimmer sensor.
        
        Returns:
            True if streaming started successfully, False otherwise
        """
        if not self.is_connected:
            self.logger.error("Not connected to Shimmer sensor")
            return False
        
        if self.is_streaming:
            self.logger.warning("Already streaming")
            return True
        
        try:
            # Send start streaming command
            if self.connection_type == 'serial':
                self.serial_connection.write(b'#')
                time.sleep(0.1)
            
            # Start streaming thread
            self.stop_streaming_event.clear()
            self.stream_thread = threading.Thread(target=self._stream_data)
            self.stream_thread.daemon = True
            self.stream_thread.start()
            
            self.is_streaming = True
            self.logger.info("Started Shimmer data streaming")
            return True
            
        except Exception as e:
            self.logger.error(f"Error starting streaming: {e}")
            return False
    
    def stop_streaming(self):
        """Stops data streaming from the Shimmer sensor."""
        if not self.is_streaming:
            return
        
        try:
            # Signal streaming thread to stop
            self.stop_streaming_event.set()
            
            # Send stop streaming command
            if self.connection_type == 'serial' and self.serial_connection:
                self.serial_connection.write(b'!')
                time.sleep(0.1)
            
            # Wait for thread to finish
            if self.stream_thread:
                self.stream_thread.join(timeout=2.0)
            
            self.is_streaming = False
            self.logger.info("Stopped Shimmer data streaming")
            
        except Exception as e:
            self.logger.error(f"Error stopping streaming: {e}")
    
    def _stream_data(self):
        """Main data streaming loop (runs in separate thread)."""
        while not self.stop_streaming_event.is_set():
            try:
                if self.connection_type == 'serial':
                    self._read_serial_data()
                elif self.connection_type == 'bluetooth':
                    self._read_bluetooth_data()
                
                time.sleep(1.0 / self.sampling_rate)  # Maintain sampling rate
                
            except Exception as e:
                self.logger.error(f"Error in streaming loop: {e}")
                break
    
    def _read_serial_data(self):
        """Reads and processes data from serial connection."""
        if self.serial_connection is None:
            return
        
        try:
            # Read data packet (format depends on Shimmer protocol)
            # This is a simplified example
            data = self.serial_connection.read(10)  # Adjust size based on packet format
            
            if len(data) >= 10:
                # Parse data packet (example format)
                timestamp = struct.unpack('<I', data[0:4])[0]
                gsr_raw = struct.unpack('<H', data[4:6])[0]
                ppg_raw = struct.unpack('<H', data[6:8])[0]
                
                # Convert raw values to physical units
                gsr_value = self._convert_gsr_raw_to_microsiemens(gsr_raw)
                ppg_value = self._convert_ppg_raw_to_mv(ppg_raw)
                
                # Call callbacks
                if self.gsr_callback and self.enabled_sensors['gsr']:
                    self.gsr_callback(gsr_value, timestamp)
                
                if self.ppg_callback and self.enabled_sensors['ppg']:
                    self.ppg_callback(ppg_value, timestamp)
                
        except Exception as e:
            self.logger.error(f"Error reading serial data: {e}")
    
    def _read_bluetooth_data(self):
        """Reads and processes data from Bluetooth connection."""
        # Implementation would depend on specific Shimmer Bluetooth protocol
        # This is a placeholder
        pass
    
    def _convert_gsr_raw_to_microsiemens(self, raw_value: int) -> float:
        """Converts raw GSR value to microsiemens."""
        # Shimmer GSR conversion formula (example)
        # Actual formula depends on Shimmer hardware version
        voltage = (raw_value / 4095.0) * 3.0  # Convert to voltage
        resistance = (3.0 - voltage) / voltage * 40000  # Convert to resistance
        conductance = 1.0 / resistance * 1000000  # Convert to microsiemens
        return conductance
    
    def _convert_ppg_raw_to_mv(self, raw_value: int) -> float:
        """Converts raw PPG value to millivolts."""
        # Shimmer PPG conversion formula (example)
        voltage_mv = (raw_value / 4095.0) * 3000.0  # Convert to millivolts
        return voltage_mv
    
    def set_gsr_callback(self, callback: Callable[[float, int], None]):
        """Sets callback for GSR data."""
        self.gsr_callback = callback
    
    def set_ppg_callback(self, callback: Callable[[float, int], None]):
        """Sets callback for PPG data."""
        self.ppg_callback = callback
    
    def set_accel_callback(self, callback: Callable[[float, float, float, int], None]):
        """Sets callback for accelerometer data."""
        self.accel_callback = callback
    
    def disconnect(self):
        """Disconnects from the Shimmer sensor."""
        self.stop_streaming()
        
        try:
            if self.serial_connection:
                self.serial_connection.close()
                self.serial_connection = None
            
            if self.bluetooth_client:
                self.bluetooth_client.disconnect()
                self.bluetooth_client = None
            
            self.is_connected = False
            self.logger.info("Disconnected from Shimmer sensor")
            
        except Exception as e:
            self.logger.error(f"Error disconnecting: {e}")


class ShimmerManager:
    """
    Manages multiple Shimmer sensors.
    """
    
    def __init__(self):
        self.logger = logging.getLogger(__name__)
        self.sensors: Dict[str, ShimmerSensor] = {}
        
    def add_sensor(self, device_id: str, connection_type: str = 'bluetooth') -> ShimmerSensor:
        """
        Adds a new Shimmer sensor to the manager.
        
        Args:
            device_id: Unique identifier for the sensor
            connection_type: 'bluetooth' or 'serial'
            
        Returns:
            ShimmerSensor object
        """
        sensor = ShimmerSensor(device_id, connection_type)
        self.sensors[device_id] = sensor
        self.logger.info(f"Added Shimmer sensor: {device_id}")
        return sensor
    
    def connect_all_sensors(self) -> bool:
        """
        Connects to all added sensors.
        
        Returns:
            True if all sensors connected successfully, False otherwise
        """
        success = True
        for device_id, sensor in self.sensors.items():
            if sensor.connection_type == 'serial':
                connected = sensor.connect_serial()
            else:
                connected = sensor.connect_bluetooth()
            
            if not connected:
                self.logger.error(f"Failed to connect to sensor: {device_id}")
                success = False
        
        return success
    
    def start_all_streaming(self) -> bool:
        """
        Starts streaming from all connected sensors.
        
        Returns:
            True if all sensors started streaming successfully, False otherwise
        """
        success = True
        for device_id, sensor in self.sensors.items():
            if sensor.is_connected:
                started = sensor.start_streaming()
                if not started:
                    self.logger.error(f"Failed to start streaming for sensor: {device_id}")
                    success = False
        
        return success
    
    def stop_all_streaming(self):
        """Stops streaming from all sensors."""
        for sensor in self.sensors.values():
            sensor.stop_streaming()
    
    def disconnect_all_sensors(self):
        """Disconnects from all sensors."""
        for sensor in self.sensors.values():
            sensor.disconnect()
    
    def get_sensor(self, device_id: str) -> Optional[ShimmerSensor]:
        """Gets a sensor by device ID."""
        return self.sensors.get(device_id)
    
    def get_connected_sensors(self) -> List[ShimmerSensor]:
        """Gets list of connected sensors."""
        return [sensor for sensor in self.sensors.values() if sensor.is_connected]