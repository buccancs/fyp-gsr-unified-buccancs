#!/usr/bin/env python3
"""
Test script to verify LocalDevice functionality for PC-connected hardware.
"""

import sys
import os

# Add the src directory to the path
sys.path.append(os.path.join(os.path.dirname(__file__), 'src'))

from local_device import LocalDevice
from PySide6.QtCore import QCoreApplication
import time

def test_local_device():
    """Test LocalDevice functionality."""
    print("Testing LocalDevice functionality...")
    
    # Create Qt application (required for signals)
    app = QCoreApplication(sys.argv)
    
    # Create LocalDevice instance
    local_device = LocalDevice()
    
    # Test device properties
    print(f"Device ID: {local_device.id}")
    print(f"Device Name: {local_device.name}")
    print(f"Device Type: {local_device.device_type}")
    print(f"Capabilities: {local_device.capabilities}")
    
    # Test connection
    print("\nTesting connection...")
    try:
        local_device.connect()
        print(f"Connection status: {local_device.is_connected}")
        
        if local_device.is_connected:
            print("✅ LocalDevice connected successfully!")
            
            # Test hardware status
            print(f"Shimmer connected: {local_device.shimmer.is_connected}")
            print(f"Webcam connected: {local_device.webcam.is_connected}")
            
            # Test recording functionality
            print("\nTesting recording functionality...")
            local_device.start_recording("test_session", ["GSR", "Webcam"])
            print(f"Recording status: {local_device.is_recording}")
            
            # Wait a moment
            time.sleep(2)
            
            # Stop recording
            local_device.stop_recording()
            print(f"Recording stopped: {not local_device.is_recording}")
            
            # Disconnect
            local_device.disconnect()
            print(f"Disconnected: {not local_device.is_connected}")
            
        else:
            print("⚠️  LocalDevice connection failed (hardware may not be available)")
            
    except Exception as e:
        print(f"❌ Error testing LocalDevice: {e}")
    
    print("\nLocalDevice test completed.")
    return True

if __name__ == "__main__":
    test_local_device()