"""
Simple test script to verify PC-Android communication
Tests the device discovery and basic communication functionality.
"""

import sys
import logging
import time
from app.network.device_manager import DeviceManager
from app.sensors.gsr_handler import GSRHandler

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def test_device_discovery():
    """Test device discovery functionality."""
    logger.info("Testing device discovery...")
    
    device_manager = DeviceManager()
    
    # Discover devices on local network
    devices = device_manager.discover_devices(ip_range="192.168.1", port=8080)
    
    logger.info(f"Found {len(devices)} devices:")
    for device in devices:
        logger.info(f"  - {device.device_id} at {device.ip_address}:{device.port}")
    
    return devices

def test_device_connection(devices):
    """Test connecting to discovered devices."""
    logger.info("Testing device connections...")
    
    device_manager = DeviceManager()
    connected_devices = []
    
    for device in devices:
        logger.info(f"Attempting to connect to {device.device_id}...")
        if device_manager.connect_device(device):
            connected_devices.append(device)
            logger.info(f"Successfully connected to {device.device_id}")
        else:
            logger.warning(f"Failed to connect to {device.device_id}")
    
    return device_manager, connected_devices

def test_recording_commands(device_manager, connected_devices):
    """Test recording start/stop commands."""
    if not connected_devices:
        logger.warning("No connected devices to test recording commands")
        return
    
    logger.info("Testing recording commands...")
    
    # Test start recording
    logger.info("Sending start recording command...")
    start_results = device_manager.start_recording_all()
    for device_id, success in start_results.items():
        if success:
            logger.info(f"Start recording successful on {device_id}")
        else:
            logger.warning(f"Start recording failed on {device_id}")
    
    # Wait a bit
    time.sleep(3)
    
    # Test stop recording
    logger.info("Sending stop recording command...")
    stop_results = device_manager.stop_recording_all()
    for device_id, success in stop_results.items():
        if success:
            logger.info(f"Stop recording successful on {device_id}")
        else:
            logger.warning(f"Stop recording failed on {device_id}")

def test_gsr_handler():
    """Test GSR handler functionality."""
    logger.info("Testing GSR handler...")
    
    gsr_handler = GSRHandler()
    
    # Add a test device
    gsr_handler.add_device("test_device", sampling_rate=128.0)
    
    # Add some test data points
    import time
    current_time = time.time()
    for i in range(10):
        gsr_value = 15.0 + (i * 0.5)  # Simulate increasing GSR values
        gsr_handler.add_data_point("test_device", current_time + i, gsr_value)
    
    # Get latest value
    latest_value = gsr_handler.get_latest_value("test_device")
    logger.info(f"Latest GSR value: {latest_value}")
    
    # Get recent data
    recent_data = gsr_handler.get_recent_data("test_device", duration_seconds=5.0)
    logger.info(f"Recent data points: {len(recent_data)}")
    
    return gsr_handler

def main():
    """Main test function."""
    logger.info("Starting GSR Multimodal System Communication Test")
    logger.info("=" * 60)
    
    try:
        # Test 1: Device Discovery
        devices = test_device_discovery()
        
        if not devices:
            logger.warning("No devices found. Make sure Android app is running and on the same network.")
            logger.info("To test with Android app:")
            logger.info("1. Install and run the Android app on a device")
            logger.info("2. Ensure both PC and Android device are on the same WiFi network")
            logger.info("3. Check that port 8080 is not blocked by firewall")
            return
        
        # Test 2: Device Connection
        device_manager, connected_devices = test_device_connection(devices)
        
        # Test 3: Recording Commands
        test_recording_commands(device_manager, connected_devices)
        
        # Test 4: GSR Handler
        gsr_handler = test_gsr_handler()
        
        # Cleanup
        logger.info("Cleaning up...")
        device_manager.shutdown()
        gsr_handler.shutdown()
        
        logger.info("Communication test completed successfully!")
        
    except Exception as e:
        logger.error(f"Test failed with error: {e}")
        return 1
    
    return 0

if __name__ == "__main__":
    sys.exit(main())