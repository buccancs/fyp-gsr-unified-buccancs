"""
Cross-platform installation script for GSR Multimodal System - PC Controller
This script handles platform-specific dependencies and provides installation guidance.
"""

import os
import platform
import subprocess
import sys
from pathlib import Path


def get_platform_info():
    """Get detailed platform information."""
    system = platform.system().lower()
    machine = platform.machine().lower()
    python_version = sys.version_info

    return {
        "system": system,
        "machine": machine,
        "python_version": python_version,
        "platform_name": platform.platform(),
    }


def install_requirements():
    """Install requirements with platform-specific handling."""
    print("Installing GSR Multimodal System - PC Controller...")
    print(f"Python version: {sys.version}")
    print(f"Platform: {platform.platform()}")
    print("-" * 50)

    platform_info = get_platform_info()

    # Install base requirements
    print("Installing base requirements...")
    try:
        subprocess.check_call(
            [sys.executable, "-m", "pip", "install", "-r", "requirements.txt"]
        )
        print("✓ Base requirements installed successfully")
    except subprocess.CalledProcessError as e:
        print(f"✗ Error installing base requirements: {e}")
        return False

    # Handle platform-specific Bluetooth dependencies
    print("\nHandling platform-specific Bluetooth dependencies...")

    if platform_info["system"] == "darwin":  # macOS
        print("macOS detected - Installing bleak for Bluetooth support...")
        try:
            subprocess.check_call(
                [sys.executable, "-m", "pip", "install", "bleak>=0.19.0"]
            )
            print("✓ bleak installed successfully for macOS Bluetooth support")
        except subprocess.CalledProcessError as e:
            print(f"⚠ Warning: Could not install bleak: {e}")
            print("  Bluetooth functionality may be limited")

    elif platform_info["system"] in ["linux", "windows"]:
        print(
            f"{platform_info['system'].title()} detected - Installing pybluez for Bluetooth support..."
        )
        try:
            if platform_info["system"] == "linux":
                print("Note: On Linux, you may need to install system dependencies:")
                print("  Ubuntu/Debian: sudo apt-get install libbluetooth-dev")
                print("  CentOS/RHEL: sudo yum install bluez-libs-devel")
                print("  Arch: sudo pacman -S bluez-libs")

            subprocess.check_call(
                [sys.executable, "-m", "pip", "install", "pybluez>=0.23"]
            )
            print("✓ pybluez installed successfully")
        except subprocess.CalledProcessError as e:
            print(f"⚠ Warning: Could not install pybluez: {e}")
            print("  Bluetooth functionality may be limited")
            if platform_info["system"] == "linux":
                print("  Make sure you have installed the required system dependencies")

    # Additional platform-specific setup
    print("\nPerforming platform-specific setup...")

    if platform_info["system"] == "linux":
        print("Linux-specific notes:")
        print("- For webcam access, ensure your user is in the 'video' group")
        print("- For serial port access, ensure your user is in the 'dialout' group")
        print("- Run: sudo usermod -a -G video,dialout $USER")
        print("- You may need to log out and back in for group changes to take effect")

    elif platform_info["system"] == "darwin":  # macOS
        print("macOS-specific notes:")
        print("- Camera and microphone permissions may be required")
        print("- Grant permissions in System Preferences > Security & Privacy")
        print("- For Bluetooth access, ensure Bluetooth is enabled")

    elif platform_info["system"] == "windows":
        print("Windows-specific notes:")
        print("- Ensure Windows Camera app permissions are granted")
        print("- For Bluetooth devices, pair them in Windows Settings first")

    print("\n" + "=" * 50)
    print("Installation completed!")
    print("You can now run the application with: python main.py")
    return True


def check_dependencies():
    """Check if all required dependencies are available."""
    print("Checking dependencies...")

    required_modules = [
        "PySide6",
        "opencv-python",
        "numpy",
        "requests",
        "websockets",
        "pandas",
        "matplotlib",
        "scipy",
        "pylsl",
        "protobuf",
        "pyserial",
        "PyYAML",
        "python-dotenv",
        "loguru",
    ]

    optional_modules = {
        "pybluez": "Bluetooth support (Windows/Linux)",
        "bleak": "Bluetooth support (macOS)",
        "pyshimmer": "Shimmer device support",
    }

    missing_required = []
    missing_optional = []

    for module in required_modules:
        try:
            __import__(module.replace("-", "_"))
            print(f"✓ {module}")
        except ImportError:
            print(f"✗ {module}")
            missing_required.append(module)

    for module, description in optional_modules.items():
        try:
            __import__(module.replace("-", "_"))
            print(f"✓ {module} ({description})")
        except ImportError:
            print(f"⚠ {module} ({description}) - Optional")
            missing_optional.append(module)

    if missing_required:
        print(f"\nMissing required dependencies: {', '.join(missing_required)}")
        return False

    if missing_optional:
        print(f"\nMissing optional dependencies: {', '.join(missing_optional)}")
        print("Some features may be limited")

    print("\nAll required dependencies are available!")
    return True


def main():
    """Main installation function."""
    if len(sys.argv) > 1 and sys.argv[1] == "--check":
        return check_dependencies()

    print("GSR Multimodal System - PC Controller Installation")
    print("=" * 50)

    # Change to the script directory
    script_dir = Path(__file__).parent
    os.chdir(script_dir)

    if not Path("requirements.txt").exists():
        print("Error: requirements.txt not found!")
        print("Make sure you're running this script from the pc-app directory")
        return False

    success = install_requirements()

    if success:
        print("\nRunning dependency check...")
        check_dependencies()

    return success


if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
