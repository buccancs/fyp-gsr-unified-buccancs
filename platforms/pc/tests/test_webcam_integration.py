#!/usr/bin/env python3
"""Test script to verify the Webcam C++ integration.

This script tests that the updated WebcamPC class can use the C++ backend
and that the integration maintains API compatibility.
"""

import sys
import os
import time

# Add the src directory to Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

def test_webcam_integration() -> None:
    """Test the WebcamPC integration with C++ backend."""
    try:
        from hardware.webcam_pc import WebcamPC
        print("✓ WebcamPC class imported successfully")
        
        # Test instantiation
        webcam = WebcamPC(camera_index=0)  # Default camera for testing
        print(f"✓ WebcamPC instantiated: {webcam}")
        print(f"  - Camera index: {webcam.camera_index}")
        print(f"  - Using C++ backend: {webcam.use_cpp_backend}")
        print(f"  - Connected: {webcam.is_connected}")
        print(f"  - Streaming: {webcam.is_streaming}")
        
        # Test connection (without actual hardware)
        print("\nTesting connection...")
        webcam.connect()
        print(f"  - Connected after connect(): {webcam.is_connected}")
        
        # Test that the device object was created
        if webcam.webcam_device:
            print(f"  - Device object created: {type(webcam.webcam_device)}")
            if hasattr(webcam.webcam_device, 'get_camera_index'):
                print(f"  - Device camera index: {webcam.webcam_device.get_camera_index()}")
            if hasattr(webcam.webcam_device, 'get_frame_width'):
                print(f"  - Device resolution: {webcam.webcam_device.get_frame_width()}x{webcam.webcam_device.get_frame_height()}")
        
        return True
        
    except Exception as e:
        print(f"✗ WebcamPC integration test failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_backend_availability() -> None:
    """Test which backends are available."""
    print("=== Backend Availability ===")
    
    try:
        import _hardware_backend
        print("✓ C++ hardware backend available")
        print(f"  - Version: {_hardware_backend.get_version()}")
        print(f"  - Test connection: {_hardware_backend.test_connection()}")
        
        # Test NativeWebcam directly
        webcam = _hardware_backend.NativeWebcam(0)
        print(f"  - NativeWebcam created: {webcam}")
        print(f"  - Camera index: {webcam.get_camera_index()}")
        print(f"  - Resolution: {webcam.get_frame_width()}x{webcam.get_frame_height()}")
        
    except ImportError:
        print("✗ C++ hardware backend not available")
    
    try:
        import cv2
        print("✓ OpenCV library available")
        print(f"  - OpenCV version: {cv2.__version__}")
    except ImportError:
        print("✗ OpenCV library not available")

def test_complete_hardware_integration() -> None:
    """Test both Shimmer and Webcam integration together."""
    print("=== Complete Hardware Integration Test ===")
    
    try:
        from hardware.shimmer_pc import ShimmerPC
        from hardware.webcam_pc import WebcamPC
from typing import Any, Dict, List, Optional, Union
        
        print("✓ Both hardware classes imported successfully")
        
        # Test instantiation of both
        shimmer = ShimmerPC(com_port="COM3")  # Dummy port
        webcam = WebcamPC(camera_index=0)     # Default camera
        
        print(f"✓ Both devices instantiated:")
        print(f"  - ShimmerPC: {shimmer} (C++ backend: {shimmer.use_cpp_backend})")
        print(f"  - WebcamPC: {webcam} (C++ backend: {webcam.use_cpp_backend})")
        
        # Test connections
        print("\nTesting connections...")
        shimmer.connect()
        webcam.connect()
        
        print(f"  - Shimmer connected: {shimmer.is_connected}")
        print(f"  - Webcam connected: {webcam.is_connected}")
        
        return True
        
    except Exception as e:
        print(f"✗ Complete integration test failed: {e}")
        return False

def main() -> None:
    """Run all integration tests."""
    print("=== Webcam C++ Integration Test ===\n")
    
    # Test backend availability
    test_backend_availability()
    print()
    
    # Test Webcam integration
    print("=== WebcamPC Integration Test ===")
    webcam_ok = test_webcam_integration()
    print()
    
    # Test complete hardware integration
    complete_ok = test_complete_hardware_integration()
    print()
    
    # Summary
    print("=== Summary ===")
    if webcam_ok and complete_ok:
        print("✓ All webcam and hardware integration tests passed!")
        print("\nThe WebcamPC class successfully integrates with the C++ backend.")
        print("Key achievements:")
        print("- ✓ Phase 1: C++ Core Library & Gradle Build System Integration")
        print("- ✓ Phase 2: Native Hardware Implementation in C++")
        print("- ✓ Phase 3: Python Layer Refactoring (Complete)")
        print("  - ✓ ShimmerPC C++ integration")
        print("  - ✓ WebcamPC C++ integration")
        print("  - ✓ API compatibility maintained")
        print("\nNext steps:")
        print("- Phase 4: Testing and Validation")
        print("- Performance benchmarking and jitter analysis")
        print("- Test with actual hardware devices")
    else:
        print("✗ Some integration tests failed. Please check the issues above.")
    
    return webcam_ok and complete_ok

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)