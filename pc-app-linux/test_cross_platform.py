"""
Simple test script to verify cross-platform compatibility changes.
This script tests basic imports and initialization without requiring external dependencies.
"""

import importlib
import platform
import sys
from pathlib import Path


def test_basic_imports():
    """Test that basic modules can be imported without platform-specific issues."""
    print("Testing basic imports...")

    try:
        # Test pathlib usage (cross-platform path handling)
        test_path = Path("test") / "subdir" / "file.txt"
        print(f"✓ pathlib works: {test_path}")

        # Test platform detection
        system = platform.system()
        print(f"✓ Platform detection: {system}")

        # Test that our modules can be imported
        from app.sensors.gsr_handler import GSRHandler

        print("✓ GSRHandler import successful")

        from app.sensors.webcam_handler import WebcamHandler

        print("✓ WebcamHandler import successful")

        from app.network.device_manager import DeviceManager

        print("✓ DeviceManager import successful")

        return True

    except Exception as e:
        print(f"✗ Import error: {e}")
        return False


def test_gsr_handler_initialization():
    """Test GSRHandler initialization with missing optional dependencies."""
    print("\nTesting GSRHandler initialization...")

    try:
        from app.sensors.gsr_handler import GSRHandler

        # This should work even without optional dependencies
        handler = GSRHandler()
        print("✓ GSRHandler initialized successfully")

        # Test that it defaults to simulation mode
        assert handler.capture_mode == "simulation"
        print("✓ Default capture mode is simulation")

        # Test that it handles missing dependencies gracefully
        try:
            handler.set_capture_mode("simulation")
            print("✓ Simulation mode works without dependencies")
        except Exception as e:
            print(f"✗ Simulation mode failed: {e}")
            return False

        return True

    except Exception as e:
        print(f"✗ GSRHandler initialization failed: {e}")
        return False


def test_webcam_handler_initialization():
    """Test WebcamHandler initialization."""
    print("\nTesting WebcamHandler initialization...")

    try:
        from app.sensors.webcam_handler import WebcamHandler

        # This should work with basic imports
        handler = WebcamHandler()
        print("✓ WebcamHandler initialized successfully")

        return True

    except Exception as e:
        print(f"✗ WebcamHandler initialization failed: {e}")
        return False


def test_platform_specific_requirements():
    """Test platform-specific requirements handling."""
    print("\nTesting platform-specific requirements...")

    system = platform.system().lower()
    print(f"Detected platform: {system}")

    # Test that we can detect the platform correctly
    if system == "windows":
        print("✓ Windows platform detected")
        print("  - Should use pybluez for Bluetooth")
    elif system == "darwin":
        print("✓ macOS platform detected")
        print("  - Should use bleak for Bluetooth")
    elif system == "linux":
        print("✓ Linux platform detected")
        print("  - Should use pybluez for Bluetooth")
    else:
        print(f"⚠ Unknown platform: {system}")

    return True


def test_file_operations():
    """Test cross-platform file operations."""
    print("\nTesting cross-platform file operations...")

    try:
        # Test pathlib usage
        test_dir = Path("test_temp")
        test_file = test_dir / "test.txt"

        # Create directory
        test_dir.mkdir(exist_ok=True)
        print("✓ Directory creation works")

        # Write file
        test_file.write_text("test content")
        print("✓ File writing works")

        # Read file
        content = test_file.read_text()
        assert content == "test content"
        print("✓ File reading works")

        # Clean up
        test_file.unlink()
        test_dir.rmdir()
        print("✓ File cleanup works")

        return True

    except Exception as e:
        print(f"✗ File operations failed: {e}")
        return False


def main():
    """Run all cross-platform compatibility tests."""
    print("Cross-Platform Compatibility Test")
    print("=" * 40)
    print(f"Python version: {sys.version}")
    print(f"Platform: {platform.platform()}")
    print("=" * 40)

    tests = [
        test_basic_imports,
        test_gsr_handler_initialization,
        test_webcam_handler_initialization,
        test_platform_specific_requirements,
        test_file_operations,
    ]

    passed = 0
    total = len(tests)

    for test in tests:
        try:
            if test():
                passed += 1
            else:
                print(f"✗ {test.__name__} failed")
        except Exception as e:
            print(f"✗ {test.__name__} crashed: {e}")

    print("\n" + "=" * 40)
    print(f"Results: {passed}/{total} tests passed")

    if passed == total:
        print("✅ All cross-platform compatibility tests passed!")
        return True
    else:
        print("❌ Some tests failed")
        return False


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
