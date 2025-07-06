# GSR & Dual-Video Recording System - Setup and Connection Guide

This comprehensive guide will walk you through setting up the complete GSR & Dual-Video Recording System, connecting all hardware components, and establishing communication between the Android app and PC controller.

## 📋 Table of Contents

1. [Hardware Requirements](#hardware-requirements)
2. [Software Requirements](#software-requirements)
3. [Hardware Setup](#hardware-setup)
4. [Software Installation](#software-installation)
5. [Network Configuration](#network-configuration)
6. [Device Connection](#device-connection)
7. [First Recording Session](#first-recording-session)
8. [Troubleshooting](#troubleshooting)

## 🔧 Hardware Requirements

### Essential Hardware
- **Android Device**: Android 7.0+ (API 24+) with USB-C port
- **PC/Laptop**: Windows, macOS, or Linux with Python 3.8+
- **Shimmer3 GSR+ Sensor**: For galvanic skin response measurement
- **Topdon TC001 Thermal Camera**: USB-C thermal imaging camera
- **WiFi Network**: For communication between devices

### Optional Hardware
- **USB-C Hub**: If your Android device has limited ports
- **Power Bank**: For extended recording sessions
- **Tripods**: For stable camera positioning

## 💻 Software Requirements

### PC Controller Requirements
- **Python 3.8 or higher**
- **PySide6 6.6.1+** (installed automatically)
- **Git** (for cloning the repository)

### Android App Requirements
- **Android 7.0+** (API level 24+)
- **USB Host Support** (for thermal camera)
- **Bluetooth LE Support** (for GSR sensor)

## 🔌 Hardware Setup

### Step 1: Shimmer3 GSR+ Sensor Setup

1. **Charge the Sensor**:
   ```
   - Connect Shimmer3 GSR+ to USB charger
   - Charge until LED shows solid green (fully charged)
   - Typical charging time: 2-3 hours
   ```

2. **Prepare GSR Electrodes**:
   ```
   - Clean electrode sites on fingers/palm with alcohol wipe
   - Attach GSR electrodes to index and middle finger
   - Ensure good skin contact for accurate readings
   ```

3. **Power On the Sensor**:
   ```
   - Press and hold power button for 3 seconds
   - LED should blink blue (discoverable mode)
   - Sensor is now ready for pairing
   ```

### Step 2: Topdon TC001 Thermal Camera Setup

1. **Connect to Android Device**:
   ```
   - Connect Topdon TC001 to Android device via USB-C cable
   - If using USB-C hub, ensure it supports USB host mode
   - Camera should power on automatically when connected
   ```

2. **Verify Connection**:
   ```
   - Android should show "USB device connected" notification
   - Grant USB permissions when prompted
   - Camera LED should indicate active status
   ```

### Step 3: Android Device Preparation

1. **Enable Developer Options**:
   ```
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times
   - Developer Options will appear in Settings
   ```

2. **Enable USB Debugging**:
   ```
   - Go to Settings > Developer Options
   - Enable "USB Debugging"
   - This allows PC communication for development
   ```

3. **Configure Permissions**:
   ```
   - Ensure location services are enabled (for Bluetooth)
   - Grant all app permissions when prompted
   - Keep screen on during recording sessions
   ```

## 📱 Software Installation

### Step 1: Install PC Controller

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/your-username/fyp-gsr-unified-buccancs.git
   cd fyp-gsr-unified-buccancs
   ```

2. **Set Up Python Environment**:
   ```bash
   # Run the automated setup script
   python3 setup_python_env.py
   
   # Or manually:
   python3 -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   pip install -r windows_controller/requirements.txt
   ```

3. **Test Installation**:
   ```bash
   cd windows_controller
   python src/main/main.py
   ```

### Step 2: Install Android App

1. **Build from Source** (Recommended):
   ```bash
   cd android
   ./gradlew assembleDebug
   ./gradlew installDebug  # Install on connected device
   ```

2. **Or Install Pre-built APK**:
   ```bash
   # If APK is provided
   adb install app-debug.apk
   ```

## 🌐 Network Configuration

### Step 1: WiFi Network Setup

1. **Connect Both Devices to Same Network**:
   ```
   - Connect PC to WiFi network
   - Connect Android device to the SAME WiFi network
   - Ensure both devices can communicate (same subnet)
   ```

2. **Check Network Connectivity**:
   ```bash
   # On PC, find your IP address
   # Windows:
   ipconfig
   
   # macOS/Linux:
   ifconfig
   
   # Note down your PC's IP address (e.g., 192.168.1.100)
   ```

3. **Configure Firewall** (if needed):
   ```
   - Allow Python applications through firewall
   - Open port 8080 for device communication
   - Disable any VPN that might block local network access
   ```

### Step 2: Device Discovery Configuration

The system uses Zeroconf/mDNS for automatic device discovery:

1. **PC Controller Setup**:
   ```python
   # The PC controller automatically advertises itself on the network
   # Default port: 8080
   # Service name: _gsrcapture._tcp.local.
   ```

2. **Android App Setup**:
   ```
   # The Android app automatically discovers PC controllers
   # No manual configuration required
   ```

## 🔗 Device Connection

### Step 1: Start PC Controller

1. **Launch the Application**:
   ```bash
   cd windows_controller
   python src/main/main.py
   ```

2. **Verify Interface**:
   ```
   - Modern PySide6 interface should appear
   - Check that all tabs are accessible:
     * Dashboard
     * Devices
     * Status
     * Logs
     * Playback
   ```

### Step 2: Connect GSR Sensor

1. **In Android App**:
   ```
   - Launch GSR Capture app
   - Tap "Connect GSR Sensor"
   - Select "Shimmer3 GSR+" from Bluetooth devices
   - Wait for "Connected" status (green indicator)
   ```

2. **Verify GSR Connection**:
   ```
   - Real-time GSR values should appear on screen
   - Values typically range from 0.1 to 20+ μS
   - Heart rate should also be displayed
   ```

### Step 3: Connect Thermal Camera

1. **Automatic Detection**:
   ```
   - Thermal camera should be detected automatically
   - "Thermal Camera: Connected" should appear
   - Live thermal preview should be available
   ```

2. **Manual Troubleshooting**:
   ```
   - If not detected, disconnect and reconnect USB-C cable
   - Check USB permissions in Android notifications
   - Restart app if necessary
   ```

### Step 4: Establish PC-Android Communication

1. **Device Discovery**:
   ```
   - In PC Controller, go to "Devices" tab
   - Click "Discover Devices"
   - Android devices should appear in the list
   ```

2. **Connect Devices**:
   ```
   - Select discovered Android device
   - Click "Connect"
   - Status should change to "Connected" (green)
   ```

3. **Verify Connection**:
   ```
   - Check "Status" tab for device information
   - Battery level, storage, and sensor status should be visible
   - All indicators should be green for ready state
   ```

## 🎬 First Recording Session

### Step 1: Pre-Recording Checklist

1. **Hardware Status**:
   ```
   ✓ GSR sensor connected and showing readings
   ✓ Thermal camera connected and showing preview
   ✓ Android device connected to PC controller
   ✓ All status indicators green
   ✓ Sufficient storage space on Android device
   ✓ Adequate battery levels on all devices
   ```

2. **Software Status**:
   ```
   ✓ PC Controller running and connected
   ✓ Android app running with all sensors active
   ✓ Network connection stable
   ✓ Recording destination configured
   ```

### Step 2: Start Recording

1. **From PC Controller**:
   ```
   - Go to "Dashboard" tab
   - Click "🔴 Start Recording"
   - All connected devices will start recording simultaneously
   - Monitor status in real-time
   ```

2. **From Android App** (Alternative):
   ```
   - Tap the "Record" button in Android app
   - All modalities start recording:
     * RGB video
     * Raw RGB images
     * Thermal frames
     * GSR data
     * Audio
   ```

### Step 3: During Recording

1. **Monitor Status**:
   ```
   - Watch real-time GSR values
   - Check thermal camera feed
   - Monitor storage usage
   - Verify all streams are active
   ```

2. **Quality Checks**:
   ```
   - Ensure stable GSR sensor connection
   - Check thermal camera positioning
   - Monitor for any error messages
   - Verify timestamp synchronization
   ```

### Step 4: Stop Recording

1. **Stop from PC Controller**:
   ```
   - Click "⏹️ Stop Recording"
   - Wait for all devices to complete file saving
   - Session manifest will be generated automatically
   ```

2. **Verify Data Output**:
   ```
   - Check Android device storage for recorded files
   - Verify session manifest on PC
   - Confirm all modalities were recorded
   ```

## 🔧 Troubleshooting

### Connection Issues

**Problem**: Android device not discovered by PC
```
Solutions:
1. Ensure both devices on same WiFi network
2. Restart WiFi on both devices
3. Check firewall settings on PC
4. Restart both applications
5. Try manual IP connection if available
```

**Problem**: GSR sensor won't connect
```
Solutions:
1. Ensure sensor is powered on (blinking blue LED)
2. Clear Bluetooth cache on Android
3. Restart Bluetooth on Android device
4. Re-pair sensor in Android Bluetooth settings
5. Check sensor battery level
```

**Problem**: Thermal camera not detected
```
Solutions:
1. Check USB-C cable connection
2. Grant USB permissions when prompted
3. Try different USB-C port/hub
4. Restart Android app
5. Check device USB host support
```

### Recording Issues

**Problem**: Recording fails to start
```
Solutions:
1. Check storage space on Android device
2. Verify all sensors are connected
3. Restart both applications
4. Check network connection stability
5. Ensure all permissions granted
```

**Problem**: Poor synchronization between devices
```
Solutions:
1. Ensure stable network connection
2. Restart recording session
3. Check for network interference
4. Use wired connection if possible
5. Reduce network traffic during recording
```

### Performance Issues

**Problem**: App crashes or freezes
```
Solutions:
1. Close other applications on devices
2. Ensure adequate RAM available
3. Check device temperature (overheating)
4. Restart devices if necessary
5. Update to latest app version
```

**Problem**: High battery drain
```
Solutions:
1. Connect devices to power during long sessions
2. Reduce screen brightness
3. Close unnecessary background apps
4. Use airplane mode with WiFi only
5. Consider external power banks
```

### Data Issues

**Problem**: Missing data files
```
Solutions:
1. Check Android device storage location
2. Verify recording completed successfully
3. Check session manifest for file references
4. Ensure sufficient storage during recording
5. Check file permissions
```

**Problem**: Timestamp synchronization errors
```
Solutions:
1. Ensure stable network during recording
2. Check system clocks on both devices
3. Restart synchronization process
4. Use shorter recording sessions
5. Check for network latency issues
```

## 📞 Getting Help

If you encounter issues not covered in this guide:

1. **Check the Logs**:
   ```
   - PC Controller: Check "Logs" tab for error messages
   - Android App: Use Android Studio logcat for detailed logs
   ```

2. **Review Documentation**:
   ```
   - README.md: Comprehensive system overview
   - DEVELOPMENT_SETUP.md: Development environment setup
   - Python API Guide: Programmatic control documentation
   ```

3. **Community Support**:
   ```
   - Create an issue in the GitHub repository
   - Include log files and error messages
   - Describe your setup and steps to reproduce
   ```

## 🎯 Next Steps

Once you have successfully completed your first recording session:

1. **Explore Advanced Features**:
   - Video playback and annotation
   - Session management and metadata
   - Data export and analysis tools

2. **Customize Your Setup**:
   - Configure recording parameters
   - Set up automated workflows
   - Integrate with research tools (LSL, PsychoPy)

3. **Scale Your System**:
   - Connect multiple Android devices
   - Set up multi-participant studies
   - Implement custom analysis pipelines

---

**Note**: This system is designed for research purposes and requires specific hardware components. Ensure you have all necessary equipment before beginning setup.