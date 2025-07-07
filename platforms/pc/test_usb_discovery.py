#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Test script for USB device discovery functionality.
This script tests the USB discovery feature implemented in the DeviceManager.
"""

import sys
import os

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from network.device_manager import DeviceManager
from utils.logger import get_logger

def test_usb_discovery():
    """
    Test the USB device discovery functionality.
    """
    logger = get_logger(__name__)
    logger.info("Starting USB device discovery test")
    
    # Create device manager
    device_manager = DeviceManager()
    
    # Test USB discovery
    logger.info("Testing USB device discovery...")
    success = device_manager.discover_usb_devices()
    
    if success:
        logger.info("USB device discovery completed successfully")
        
        # Check discovered devices
        discovered_devices = device_manager.discovered_devices
        logger.info(f"Found {len(discovered_devices)} USB devices:")
        
        for device_id, device in discovered_devices.items():
            logger.info(f"  - Device ID: {device_id}")
            logger.info(f"    Name: {device.name}")
            logger.info(f"    Address: {device.address}")
            logger.info(f"    Port: {device.port}")
            logger.info(f"    Type: {device.device_type}")
            logger.info(f"    Capabilities: {device.capabilities}")
            
            # Test connection type detection
            connection_type = "USB" if device.address == "127.0.0.1" else "Wi-Fi"
            logger.info(f"    Connection Type: {connection_type}")
            
    else:
        logger.error("USB device discovery failed")
        
    logger.info("USB device discovery test completed")
    return success

if __name__ == "__main__":
    print("USB Device Discovery Test")
    print("=" * 40)
    print()
    print("This test will attempt to discover Android devices connected via USB.")
    print("Make sure you have:")
    print("1. An Android device connected via USB")
    print("2. USB debugging enabled on the device")
    print("3. ADB server running (or it will be started automatically)")
    print()
    
    try:
        success = test_usb_discovery()
        if success:
            print("\n✅ USB discovery test completed successfully!")
        else:
            print("\n❌ USB discovery test failed!")
            print("Check the logs for more details.")
    except Exception as e:
        print(f"\n❌ Test failed with exception: {e}")
        import traceback
        traceback.print_exc()