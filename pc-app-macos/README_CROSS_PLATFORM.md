# GSR Multimodal System - PC Controller
## Cross-Platform Installation and Usage Guide

This guide provides instructions for installing and running the GSR Multimodal System PC Controller on Windows, macOS, and Linux.

## System Requirements

### All Platforms
- Python 3.8 or higher
- At least 4GB RAM
- USB ports for sensor connections
- Camera access for webcam functionality
- Network connectivity for device communication

### Platform-Specific Requirements

#### Windows
- Windows 10 or higher
- Visual C++ Redistributable (usually included with Python)
- Windows Camera app permissions

#### macOS
- macOS 10.15 (Catalina) or higher
- Xcode Command Line Tools: `xcode-select --install`
- Camera and microphone permissions in System Preferences

#### Linux
- Ubuntu 18.04+, CentOS 7+, or equivalent
- Development packages for Bluetooth support
- User permissions for camera and serial devices

## Installation

### Quick Installation (Recommended)

1. **Clone or download the project**
   ```bash
   cd pc-app
   ```

2. **Run the cross-platform installer**
   ```bash
   python install.py
   ```

   The installer will:
   - Detect your operating system
   - Install platform-appropriate dependencies
   - Provide platform-specific setup instructions
   - Verify the installation

3. **Check installation**
   ```bash
   python install.py --check
   ```

### Manual Installation

If you prefer to install manually or the automatic installer fails:

#### 1. Install Python Dependencies

**All Platforms:**
```bash
pip install -r requirements.txt
```

#### 2. Platform-Specific Bluetooth Support

**Windows/Linux:**
```bash
pip install pybluez>=0.23
```

**macOS:**
```bash
pip install bleak>=0.19.0
```

#### 3. System Dependencies

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install libbluetooth-dev python3-dev
sudo usermod -a -G video,dialout $USER
```

**CentOS/RHEL:**
```bash
sudo yum install bluez-libs-devel python3-devel
sudo usermod -a -G video,dialout $USER
```

**Arch Linux:**
```bash
sudo pacman -S bluez-libs python
sudo usermod -a -G video,dialout $USER
```

**macOS:**
```bash
# Install Homebrew if not already installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install system dependencies (optional, for additional features)
brew install python-tk
```

## Running the Application

### Standard Launch
```bash
python main.py
```

### Platform-Specific Notes

#### Windows
- Ensure Windows Defender or antivirus software allows Python network access
- Grant camera permissions when prompted
- For Bluetooth devices, pair them in Windows Settings first

#### macOS
- Grant camera and microphone permissions in System Preferences > Security & Privacy
- Allow network connections when prompted by macOS firewall
- Ensure Bluetooth is enabled for Shimmer device connectivity

#### Linux
- Log out and back in after installation to apply group permissions
- For camera issues, check: `ls -l /dev/video*`
- For serial port issues, check: `ls -l /dev/ttyUSB* /dev/ttyACM*`
- Some distributions may require additional permissions:
  ```bash
  sudo chmod 666 /dev/video0  # Temporary camera access
  ```

## Troubleshooting

### Common Issues

#### Import Errors
```bash
# Check which dependencies are missing
python install.py --check

# Reinstall specific packages
pip install --upgrade <package-name>
```

#### Camera Access Issues

**Windows:**
- Check Windows Camera app permissions
- Restart the application as administrator (if needed)

**macOS:**
- System Preferences > Security & Privacy > Camera
- Add Python or Terminal to allowed applications

**Linux:**
- Check user groups: `groups $USER`
- Should include 'video' group
- Try: `sudo usermod -a -G video $USER` and log out/in

#### Bluetooth Connection Issues

**All Platforms:**
- Ensure Bluetooth is enabled
- For Shimmer devices, pair them first using system Bluetooth settings
- Check device compatibility with your Bluetooth version

**Linux Specific:**
```bash
# Check Bluetooth service
sudo systemctl status bluetooth

# Restart Bluetooth service
sudo systemctl restart bluetooth
```

#### Network/Firewall Issues
- Allow Python through firewall
- Default ports used: 8080 (device communication), 8081 (file transfer)
- Configure firewall to allow these ports if needed

### Performance Optimization

#### All Platforms
- Close unnecessary applications
- Ensure adequate disk space for recordings
- Use SSD storage for better performance

#### Linux
- For better real-time performance:
  ```bash
  # Increase process priority (optional)
  sudo nice -n -10 python main.py
  ```

## Feature Availability by Platform

| Feature | Windows | macOS | Linux |
|---------|---------|-------|-------|
| GUI Interface | ✅ | ✅ | ✅ |
| Webcam Capture | ✅ | ✅ | ✅ |
| Network Communication | ✅ | ✅ | ✅ |
| Serial Communication | ✅ | ✅ | ✅ |
| Bluetooth (pybluez) | ✅ | ❌ | ✅ |
| Bluetooth (bleak) | ❌ | ✅ | ❌ |
| LSL Streaming | ✅ | ✅ | ✅ |
| File Transfer | ✅ | ✅ | ✅ |

## Development and Testing

### Running Tests
```bash
# Install test dependencies
pip install pytest pytest-qt

# Run all tests
python -m pytest tests/

# Run specific test
python -m pytest tests/test_webcam_handler.py
```

### Development Setup
```bash
# Install development dependencies
pip install -r requirements.txt
pip install pytest pytest-qt black flake8

# Format code
black .

# Lint code
flake8 .
```

## Support and Contributing

### Getting Help
1. Check this documentation first
2. Run `python install.py --check` to verify installation
3. Check the logs in `gsr_multimodal.log`
4. Review platform-specific troubleshooting above

### Reporting Issues
When reporting issues, please include:
- Operating system and version
- Python version (`python --version`)
- Error messages and logs
- Steps to reproduce the issue

### Contributing
- Follow the existing code style
- Add tests for new features
- Update documentation for changes
- Test on multiple platforms when possible

## License

This project is licensed under the terms specified in the main project LICENSE file.