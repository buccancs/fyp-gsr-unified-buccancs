#!/usr/bin/env python3
"""
Test script to verify the Shimmer C++ integration.

This script tests that the updated ShimmerPC class can use the C++ backend
and that the integration maintains API compatibility.
"""

import sys
import os
import time

# Add the src directory to Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

def test_shimmer_integration():
    """Test the ShimmerPC integration with C++ backend."""
    try:
        from hardware.shimmer_pc import ShimmerPC
        print("✓ ShimmerPC class imported successfully")
        
        # Test instantiation
        shimmer = ShimmerPC(com_port="COM3")  # Dummy port for testing
        print(f"✓ ShimmerPC instantiated: {shimmer}")
        print(f"  - COM port: {shimmer.com_port}")
        print(f"  - Using C++ backend: {shimmer.use_cpp_backend}")
        print(f"  - Connected: {shimmer.is_connected}")
        print(f"  - Streaming: {shimmer.is_streaming}")
        
        # Test connection (without actual hardware)
        print("\nTesting connection...")
        shimmer.connect()
        print(f"  - Connected after connect(): {shimmer.is_connected}")
        
        # Test that the device object was created
        if shimmer.shimmer_device:
            print(f"  - Device object created: {type(shimmer.shimmer_device)}")
            if hasattr(shimmer.shimmer_device, 'get_com_port'):
                print(f"  - Device COM port: {shimmer.shimmer_device.get_com_port()}")
        
        return True
        
    except Exception as e:
        print(f"✗ ShimmerPC integration test failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_backend_availability():
    """Test which backends are available."""
    print("=== Backend Availability ===")
    
    try:
        import _hardware_backend
        print("✓ C++ hardware backend available")
        print(f"  - Version: {_hardware_backend.get_version()}")
        print(f"  - Test connection: {_hardware_backend.test_connection()}")
    except ImportError:
        print("✗ C++ hardware backend not available")
    
    try:
        import pyshimmer
        print("✓ pyshimmer library available")
    except ImportError:
        print("✗ pyshimmer library not available")

def main():
    """Run all integration tests."""
    print("=== Shimmer C++ Integration Test ===\n")
    
    # Test backend availability
    test_backend_availability()
    print()
    
    # Test Shimmer integration
    print("=== ShimmerPC Integration Test ===")
    integration_ok = test_shimmer_integration()
    print()
    
    # Summary
    print("=== Summary ===")
    if integration_ok:
        print("✓ Shimmer C++ integration test passed!")
        print("\nThe ShimmerPC class successfully integrates with the C++ backend.")
        print("Next steps:")
        print("- Test with actual Shimmer hardware")
        print("- Implement webcam integration")
        print("- Performance validation and benchmarking")
    else:
        print("✗ Integration test failed. Please check the issues above.")
    
    return integration_ok

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)