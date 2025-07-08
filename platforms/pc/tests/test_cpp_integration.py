#!/usr/bin/env python3
"""Test script to verify the C++ integration setup.

This script tests that the basic project structure is in place
and that the existing Python hardware layer is accessible.
"""

import sys
import os

# Add the src directory to Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

def test_python_hardware_layer() -> None:
    """Test that the existing Python hardware layer is accessible."""
    try:
        from hardware.shimmer_pc import ShimmerPC
        from hardware.webcam_pc import WebcamPC
from typing import Any, Dict, List, Optional, Union
        print("✓ Python hardware layer imports successful")
        
        # Test basic instantiation (without actual hardware)
        shimmer = ShimmerPC(com_port="COM3")  # Dummy port
        webcam = WebcamPC(camera_index=0)
        
        print("✓ Python hardware classes instantiated successfully")
        print(f"  - ShimmerPC: {shimmer}")
        print(f"  - WebcamPC: {webcam}")
        
        return True
    except Exception as e:
        print(f"✗ Python hardware layer test failed: {e}")
        return False

def test_cpp_structure() -> None:
    """Test that the C++ project structure is in place."""
    base_path = os.path.dirname(__file__)
    
    required_files = [
        "src/cpp/include/NativeShimmer.h",
        "src/cpp/include/NativeWebcam.h", 
        "src/cpp/main/NativeShimmer.cpp",
        "src/cpp/main/NativeWebcam.cpp",
        "src/cpp/main/bindings.cpp",
        "build.gradle.kts"
    ]
    
    all_exist = True
    for file_path in required_files:
        full_path = os.path.join(base_path, file_path)
        if os.path.exists(full_path):
            print(f"✓ {file_path}")
        else:
            print(f"✗ {file_path} - NOT FOUND")
            all_exist = False
    
    return all_exist

def test_gradle_configuration() -> None:
    """Test that Gradle configuration includes PC platform."""
    try:
        settings_path = os.path.join(os.path.dirname(__file__), "..", "..", "settings.gradle.kts")
        with open(settings_path, 'r') as f:
            content = f.read()
            
        if 'include(":pc")' in content:
            print("✓ PC platform included in settings.gradle.kts")
            return True
        else:
            print("✗ PC platform not found in settings.gradle.kts")
            return False
    except Exception as e:
        print(f"✗ Error checking Gradle configuration: {e}")
        return False

def main() -> None:
    """Run all tests."""
    print("=== C++ Integration Setup Test ===\n")
    
    print("1. Testing Python Hardware Layer:")
    python_ok = test_python_hardware_layer()
    print()
    
    print("2. Testing C++ Project Structure:")
    cpp_ok = test_cpp_structure()
    print()
    
    print("3. Testing Gradle Configuration:")
    gradle_ok = test_gradle_configuration()
    print()
    
    print("=== Summary ===")
    if python_ok and cpp_ok and gradle_ok:
        print("✓ All tests passed! Phase 1 setup is complete.")
        print("\nNext steps:")
        print("- Install pybind11 dependencies")
        print("- Install OpenCV development libraries") 
        print("- Configure CMake or update Gradle build for dependencies")
        print("- Implement Phase 2: Native Hardware Implementation")
    else:
        print("✗ Some tests failed. Please check the issues above.")
    
    return python_ok and cpp_ok and gradle_ok

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)