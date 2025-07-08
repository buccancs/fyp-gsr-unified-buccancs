#!/usr/bin/env python3
"""Test script to verify the compiled C++ hardware backend.

This script tests that the C++ extension module can be imported
and that the basic functionality is accessible.
"""

import sys
import os

# Add the src directory to Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src'))

def test_module_import() -> None:
    """Test that the C++ module can be imported."""
    try:
        import _hardware_backend
from typing import Any, Dict, List, Optional, Union
        print("✓ C++ hardware backend module imported successfully")
        print(f"  Module version: {_hardware_backend.__version__}")
        print(f"  Module author: {_hardware_backend.__author__}")
        return _hardware_backend
    except ImportError as e:
        print(f"✗ Failed to import C++ hardware backend: {e}")
        return None

def test_module_functions(backend) -> None:
    """Test basic module functions."""
    try:
        version = backend.get_version()
        test_msg = backend.test_connection()
        print(f"✓ Module functions work:")
        print(f"  - get_version(): {version}")
        print(f"  - test_connection(): {test_msg}")
        return True
    except Exception as e:
        print(f"✗ Module functions failed: {e}")
        return False

def test_shimmer_class(backend) -> None:
    """Test NativeShimmer class instantiation."""
    try:
        shimmer = backend.NativeShimmer("COM3")  # Dummy port
        print(f"✓ NativeShimmer instantiated: {shimmer}")
        print(f"  - COM port: {shimmer.get_com_port()}")
        print(f"  - Connected: {shimmer.is_connected()}")
        print(f"  - Running: {shimmer.is_running()}")
        return True
    except Exception as e:
        print(f"✗ NativeShimmer instantiation failed: {e}")
        return False

def test_webcam_class(backend) -> None:
    """Test NativeWebcam class instantiation."""
    try:
        webcam = backend.NativeWebcam(0)  # Default camera
        print(f"✓ NativeWebcam instantiated: {webcam}")
        print(f"  - Camera index: {webcam.get_camera_index()}")
        print(f"  - Connected: {webcam.is_connected()}")
        print(f"  - Running: {webcam.is_running()}")
        print(f"  - Resolution: {webcam.get_frame_width()}x{webcam.get_frame_height()}")
        return True
    except Exception as e:
        print(f"✗ NativeWebcam instantiation failed: {e}")
        return False

def main() -> None:
    """Run all tests."""
    print("=== C++ Hardware Backend Test ===\n")
    
    # Test module import
    backend = test_module_import()
    if not backend:
        print("\n✗ Cannot proceed without successful module import")
        return False
    
    print()
    
    # Test module functions
    functions_ok = test_module_functions(backend)
    print()
    
    # Test Shimmer class
    shimmer_ok = test_shimmer_class(backend)
    print()
    
    # Test Webcam class
    webcam_ok = test_webcam_class(backend)
    print()
    
    # Summary
    print("=== Summary ===")
    if functions_ok and shimmer_ok and webcam_ok:
        print("✓ All C++ backend tests passed!")
        print("\nThe C++ hardware backend is ready for integration.")
        print("Next steps:")
        print("- Test actual hardware connections")
        print("- Integrate with existing Python hardware layer")
        print("- Implement Phase 3: Python layer refactoring")
    else:
        print("✗ Some tests failed. Please check the issues above.")
    
    return functions_ok and shimmer_ok and webcam_ok

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)