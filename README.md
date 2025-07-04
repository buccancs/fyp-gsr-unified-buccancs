# GSR & Dual-Video Recording System

A comprehensive multimodal data capture system that synchronizes Galvanic Skin Response (GSR) sensors with RGB and thermal video recording across multiple Android devices, controlled by a cross-platform PC application.

## 🎯 Overview

This system enables researchers to capture synchronized physiological and visual data for behavioral studies, emotion recognition, and human-computer interaction research. The system consists of:

- **📱 Android Capture App**: Records GSR, RGB video, thermal video, raw images, and audio with nanosecond precision
- **💻 PC Controller**: Modern PySide6 interface for managing multiple devices, live monitoring, and coordinated recording sessions
- **🔗 Shared Components**: Common networking, synchronization, and data management utilities
- **📊 Analysis Tools**: Video playback, annotation system, and data export capabilities

## ✨ Key Features at a Glance

- **🎥 Multi-Modal Recording**: RGB video, thermal imaging, GSR sensors, raw image capture, and audio
- **⚡ Real-Time Synchronization**: Sub-millisecond timing accuracy across multiple devices
- **🌐 Cross-Platform**: Android app + PC controller (Windows/macOS/Linux)
- **🔧 Modern Tech Stack**: Java 24, Kotlin 2.0, Android 14, PySide6
- **📈 Professional Interface**: Modern GUI with real-time monitoring and control
- **🔬 Research-Ready**: Comprehensive data export and analysis tools

## 🚀 Current Capabilities

### 📱 Android Capture App - Comprehensive Multi-Modal Recording

#### Core Recording Capabilities
- **🎥 RGB Video Capture**:
  - High-definition recording (1080p @ 30fps) using CameraX 1.3.1
  - H.264 codec with optimized compression
  - Live preview with real-time focus and exposure control

- **📸 Raw RGB Image Capture** *(NEW)*:
  - Frame-by-frame capture before ISP processing
  - CameraX ImageCapture with minimize latency mode
  - ~30 FPS capture rate with nanosecond timestamp naming
  - Organized storage in dedicated subdirectories

- **🌡️ Thermal Video Capture**:
  - Topdon TC001 thermal camera integration (USB-C)
  - Infrared frame capture with temperature mapping
  - Frame-by-frame saving with timestamp synchronization
  - Real-time thermal preview and mode switching

- **📊 GSR Sensor Integration**:
  - Shimmer3 GSR+ sensor via Bluetooth LE
  - 128Hz sampling rate for high-precision data
  - Real-time GSR readings in microSiemens (μS)
  - PPG-derived heart rate calculation
  - Automatic reconnection and status monitoring

- **🎵 Audio Recording**:
  - 44.1kHz stereo WAV format
  - Synchronized with all other data streams
  - Built-in microphone with noise optimization

#### Advanced Features
- **⚡ Real-Time Monitoring**:
  - Live preview switching between RGB and thermal feeds
  - Real-time GSR values and heart rate display
  - Device status indicators with color-coded feedback
  - Connection health monitoring for all sensors

- **🎛️ Unified Control System**:
  - One-touch recording start/stop for all modalities
  - Synchronized timing across all data streams
  - Automatic session management and file organization
  - Error handling and recovery mechanisms

- **⏱️ Precision Timing & Synchronization**:
  - Nanosecond-precision timestamps using SystemClock.elapsedRealtimeNanos()
  - Frame-by-frame saving with timestamp-based naming
  - Synchronized start/stop across all recording modalities
  - Sub-millisecond accuracy for multi-device coordination

- **🌐 Network Integration**:
  - Remote control via PC controller
  - Real-time status reporting and command execution
  - TCP/IP communication with command protocol
  - Device discovery and automatic pairing

### 💻 PC Controller - Modern Cross-Platform Interface

#### 🖥️ Modern PySide6 GUI Framework
- **Professional Interface Design**:
  - Modern flat design with professional blue/gray color scheme
  - Enhanced buttons with emojis (🔴 Start, ⏹️ Stop) and hover effects
  - Rounded corners, improved typography, and consistent styling
  - Responsive design optimized for 1400x900+ displays
  - High-DPI support for crisp rendering on all screen types

- **Cross-Platform Compatibility**:
  - Native support for Windows, macOS, and Linux
  - Consistent user experience across all platforms
  - Official Qt binding (PySide6) for reliable performance
  - Modern Python 3.8+ compatibility

#### 📊 Advanced Multi-Device Management
- **Device Discovery & Connection**:
  - Zeroconf/mDNS automatic device discovery
  - Manual IP address entry for devices not auto-discovered
  - Multiple simultaneous device connections
  - Real-time connection health monitoring
  - Automatic reconnection and error recovery

- **Synchronized Control System**:
  - Central recording control for all connected devices
  - Simultaneous start/stop commands across devices
  - Real-time command acknowledgment and status feedback
  - Error handling and device-specific recovery

- **Real-Time Status Dashboard**:
  - Color-coded device status indicators
  - Battery level monitoring (when available)
  - Storage remaining on each device
  - Sensor stream status (camera, thermal, GSR, audio)
  - Connection quality and latency monitoring

#### 📹 Advanced Monitoring & Analysis
- **Live Video Preview System**:
  - Real-time RGB and thermal video feeds from devices
  - Multi-device video preview in organized layout
  - Frame rate and quality monitoring
  - Preview switching and full-screen modes

- **Session Management & Metadata**:
  - Automatic session ID generation and propagation
  - Comprehensive session manifest creation
  - Device information and configuration logging
  - File organization with consistent naming conventions
  - Metadata collection for research compliance

- **Data Logging & Monitoring**:
  - Advanced logging with multiple severity levels
  - Real-time log filtering and search capabilities
  - Export functionality for troubleshooting
  - System event tracking and performance monitoring

- **Video Playback & Annotation**:
  - Integrated video playback system with PySide6 multimedia
  - Real-time annotation capabilities during playback
  - Video playlist management for multi-file analysis
  - Export functionality for research analysis and reporting

### 🔧 Technical Specifications

#### 📱 Android Platform Requirements
- **Operating System**: Android 7.0+ (API 24) to Android 14 (API 34)
- **Hardware Requirements**:
  - USB-C port for thermal camera connectivity
  - Bluetooth LE support for GSR sensor
  - Minimum 4GB RAM, 8GB+ recommended
  - 32GB+ storage for recording sessions
  - Camera2 API support for advanced camera features

#### 💻 PC Platform Requirements
- **Operating Systems**: Windows 10+, macOS 10.15+, Ubuntu 18.04+
- **Hardware Requirements**:
  - Minimum 8GB RAM, 16GB+ recommended
  - 1400x900+ display resolution
  - Network connectivity (WiFi/Ethernet)
  - Optional: USB webcam for local recording

#### 🛠️ Development Stack (2024 Latest)
- **Android Development**:
  - **Java**: 24 (upgraded from Java 8)
  - **Kotlin**: 2.0.0 (enhanced compiler performance)
  - **Android SDK**: 34 (Android 14 support)
  - **Gradle**: 8.14 (latest build system)
  - **CameraX**: 1.3.1 (enhanced raw image capture)
  - **AndroidX Libraries**: All updated to latest versions

- **PC Development**:
  - **Python**: 3.8+ (3.11+ recommended)
  - **PySide6**: 6.6.1 (official Qt binding)
  - **OpenCV**: 4.8.0 (computer vision)
  - **pyqtgraph**: 0.13.7 (real-time plotting)
  - **Zeroconf**: 0.69.0 (device discovery)

#### 🔌 Hardware Integration
- **GSR Sensor**: Shimmer3 GSR+ with Bluetooth LE
- **Thermal Camera**: Topdon TC001 (USB-C connection)
- **Audio**: Built-in device microphones
- **Optional**: PC webcams via USB

#### 🌐 Networking & Communication
- **Protocols**: TCP/IP, Zeroconf/mDNS
- **Device Discovery**: Automatic network scanning
- **Command Protocol**: Custom binary protocol for device control
- **Data Transfer**: WiFi-based file transfer capabilities
- **Synchronization**: NTP-like time sync with sub-millisecond accuracy

#### ⚡ Performance Specifications
- **Timing Accuracy**: Nanosecond-precision timestamps
- **Video Recording**: 1080p @ 30fps (RGB), ~25fps (thermal)
- **GSR Sampling**: 128Hz continuous data stream
- **Audio Quality**: 44.1kHz stereo WAV
- **Raw Image Capture**: ~30 FPS with timestamp naming
- **Multi-Device Support**: Tested with up to 4 simultaneous devices

## 🔮 Future Improvements

### Short-Term (Next 3-6 months)
- **Enhanced Networking**:
  - Improve command protocol with authentication and encryption
  - Add live video streaming (full video frames) from Android to PC
  - Optimize network performance and reduce latency

- **Data Analysis Tools**:
  - Real-time GSR/PPG plotting on PC
  - Data export to MATLAB/HDF5 formats
  - Automated sync quality analysis

- **User Experience**:
  - Configuration management UI
  - Improved error messages and recovery
  - Device-specific settings interface

### Medium-Term (6-12 months)
- **Extended Hardware Support**:
  - Additional GSR sensor models
  - Multiple thermal camera brands
  - PC-connected sensors (USB/Bluetooth)

- **Advanced Features**:
  - Machine learning integration for real-time analysis
  - Cloud storage and backup options
  - Multi-session experiment management

### Long-Term (12+ months)
- **Platform Expansion**:
  - iOS capture app
  - Web-based controller interface
  - Integration with research platforms (PsychoPy, LSL)

- **Research Tools**:
  - Automated data preprocessing pipelines
  - Statistical analysis integration
  - Collaborative research features

## 🚀 Recently Implemented Features

### 🔧 Hardware Abstraction Layer
The system now includes a comprehensive hardware abstraction layer that allows support for multiple hardware types without code modification:

#### **Generic Interfaces**
- **PhysiologicalSensor Interface**: Standardized interface for GSR, PPG, and other physiological sensors
- **ThermalCamera Interface**: Unified interface for thermal cameras with configurable specifications
- **HardwareFactory**: Automatic hardware detection and instantiation

#### **Concrete Implementations**
- **ShimmerPhysiologicalSensor**: Full implementation for Shimmer3 GSR+ devices
- **TopdonThermalCamera**: Complete support for Topdon TC001 thermal cameras
- **GenericThermalCamera**: Configurable implementation for unknown/third-party thermal cameras

#### **Benefits**
- ✅ Easy addition of new hardware types
- ✅ Consistent API across different hardware
- ✅ Runtime hardware detection and configuration
- ✅ Graceful fallback for unsupported devices

### 🔐 Network Security Framework
A complete security framework has been implemented to secure device communications:

#### **Authentication System**
- **AuthenticationProvider Interface**: Pluggable authentication methods
- **Device Authorization**: Whitelist-based device access control
- **Session Management**: Token-based session handling with expiration

#### **Encryption System**
- **EncryptionProvider Interface**: Support for multiple encryption algorithms
- **Symmetric Encryption**: Fast encryption for data streams
- **Asymmetric Encryption**: Secure key exchange and authentication
- **SecureNetworkWrapper**: Complete secure communication layer

#### **Security Features**
- ✅ Device authentication and authorization
- ✅ Encrypted data transmission
- ✅ Session token management
- ✅ Secure handshake protocol
- ✅ Protection against unauthorized access

### 📊 Data Export System
Advanced data export capabilities for research and analysis:

#### **Export Formats**
- **MATLAB (.mat) Export**: Full compatibility with MATLAB analysis workflows
- **HDF5 (.h5) Export**: Scientific data format with compression and metadata
- **Configurable Options**: Compression levels, precision settings, metadata inclusion

#### **Export Features**
- ✅ Time series data export (GSR, PPG, heart rate)
- ✅ Metadata preservation (device info, session details, settings)
- ✅ Configurable compression and precision
- ✅ Size estimation and validation
- ✅ Batch export capabilities

#### **Research Integration**
- **MATLAB Compatibility**: Direct import into MATLAB for analysis
- **HDF5 Support**: Compatible with Python (h5py), R, and other scientific tools
- **Metadata Rich**: Comprehensive session and device information included

### 🔗 System Integration
Complete integration example demonstrating all new features working together:

#### **Integration Workflow**
1. **Hardware Detection**: Automatic discovery of available sensors and cameras
2. **Security Setup**: Authentication and encryption configuration
3. **Data Collection**: Synchronized data capture from multiple devices
4. **Export Processing**: Automatic export to multiple research formats
5. **Resource Cleanup**: Proper shutdown and resource management

#### **Example Usage**
```kotlin
val integrationExample = SystemIntegrationExample(context)
integrationExample.runIntegrationExample()
```

## ⚠️ Current Blockers & Limitations

### 🔧 Technical Implementation Limitations

#### Network & Communication
- **Live Video Streaming**: Full video frame streaming not yet implemented (GSR/PPG data streaming is functional)
- **Network Security**: ✅ **RESOLVED** - Authentication and encryption framework implemented

#### Data Management
- **Data Export**: ✅ **RESOLVED** - MATLAB (.mat) and HDF5 (.h5) export capabilities implemented
- **Data Validation**: Limited verification of transferred file integrity
- **Backup Systems**: No automatic backup or redundancy mechanisms

### 🔌 Hardware Constraints

#### Sensor Dependencies
- **Thermal Camera**: ✅ **IMPROVED** - Hardware abstraction layer now supports multiple thermal cameras
  - *Supported*: Topdon TC001 (native), Generic thermal cameras (configurable)
  - *Framework*: Generic ThermalCamera interface allows easy addition of new models
  - *Remaining limitation*: Each new camera type still requires specific implementation
- **GSR Sensor**: ✅ **IMPROVED** - Hardware abstraction layer implemented
  - *Supported*: Shimmer3 GSR+ (native), Generic physiological sensors (framework ready)
  - *Framework*: PhysiologicalSensor interface standardizes sensor integration
  - *Remaining limitation*: Non-Shimmer sensors require specific driver implementation

#### Device Compatibility
- **USB-C Requirement**: Thermal camera requires USB-C host support
  - *Impact*: Excludes older Android devices with micro-USB
  - *Workaround*: USB-C to micro-USB adapters (limited functionality)
- **Android Version**: CameraX limitations on older devices (API < 24)
  - *Impact*: No support for Android 6.0 and below
  - *Workaround*: Legacy Camera2 API implementation needed

### 💻 Software Dependencies & Compatibility

#### Platform Requirements
- **Android**: Strict API 24+ requirement (Android 7.0+)
- **PC**: Python 3.8+ with specific PySide6 version dependencies
- **Network**: Devices must be on same WiFi network (no cellular/mobile hotspot)
- **Permissions**: Extensive permission requirements may limit deployment

#### Development Environment
- **Java Version**: Now requires Java 24 (may limit developer adoption)
- **Build System**: Gradle 8.14+ required (compatibility with older IDEs)
- **Library Dependencies**: Specific version requirements for all components

### ⚡ Performance & Resource Limitations

#### System Performance
- **Battery Drain**: Intensive multi-modal recording significantly impacts battery life
  - *Impact*: Limited recording session duration on mobile devices
  - *Mitigation*: Power management optimizations needed
- **Storage Requirements**: High-resolution video and raw images require substantial storage
  - *Impact*: Limited session count on devices with low storage
  - *Mitigation*: Compression and cleanup strategies needed

#### Processing Constraints
- **Thermal Processing**: CPU-intensive thermal frame processing affects performance
- **Real-Time Processing**: Limited real-time analysis capabilities during recording
- **Memory Usage**: High memory consumption during multi-modal recording
- **Network Bandwidth**: Large file transfers may saturate network connections

### 🔄 Synchronization & Timing Issues

#### Timing Accuracy
- **Network Latency**: Variable network delays affect synchronization precision
  - *Current*: Sub-millisecond accuracy under ideal conditions
  - *Reality*: 10-50ms variance in real-world network conditions
- **Clock Drift**: Device clock differences accumulate over long sessions
- **Frame Dropping**: Occasional frame drops during high-intensity recording

#### Multi-Device Coordination
- **Scalability**: Tested with up to 4 devices - higher counts may introduce issues
- **Error Recovery**: Limited automatic recovery from device disconnections
- **Session Management**: Complex session state management across multiple devices

### 🛠️ Development & Deployment Challenges

#### Setup Complexity
- **Hardware Setup**: Requires specific hardware configuration and pairing
- **Network Configuration**: Manual network setup and device discovery
- **Permission Management**: Complex permission requirements across platforms
- **Calibration**: Manual sensor calibration and validation required

#### Maintenance & Support
- **Documentation**: Some implementation details still being documented
- **Testing**: Limited automated testing for multi-device scenarios
- **Error Handling**: Inconsistent error reporting across components
- **User Support**: Limited troubleshooting guides for common issues

### 📋 Priority Resolution Roadmap

#### ✅ Recently Completed
1. ✅ Implement actual device discovery (Zeroconf/mDNS)
2. ✅ Implement live data streaming (GSR/PPG data)
3. ✅ Add automatic file transfer capabilities
4. ✅ Complete command protocol implementation with full serialization/deserialization
5. ✅ Implement functional handshake protocol with ACK/NACK responses
6. ✅ Add periodic time synchronization with drift detection
7. ✅ Implement connection monitoring with heartbeat and recovery

#### ✅ Recently Implemented (Latest Update)
1. ✅ **Hardware Abstraction Layer**: Generic interfaces for physiological sensors and thermal cameras
2. ✅ **Network Security Framework**: Authentication and encryption interfaces with secure communication wrapper
3. ✅ **Data Export System**: MATLAB (.mat) and HDF5 (.h5) export capabilities for research analysis
4. ✅ **Concrete Hardware Implementations**: Support for Shimmer3 GSR+, Topdon TC001, and generic thermal cameras
5. ✅ **System Integration Example**: Complete workflow demonstration from hardware detection to data export

#### High Priority (Next 3 months)
1. Implement live video streaming (full video frames)
2. Improve error handling and recovery mechanisms
3. Optimize battery usage and performance
4. Add concrete authentication and encryption providers

#### Medium Priority (3-6 months)
1. Expand hardware support (additional thermal cameras, GSR sensors using the new abstraction layer)
2. Improve synchronization accuracy
3. Add data validation and integrity checking
4. Implement backup and redundancy systems

#### Low Priority (6+ months)
1. Add support for additional platforms (iOS)
2. Implement advanced data analysis features
3. Add cloud storage and backup options
4. Develop automated testing frameworks

## 🧪 How to Test - Comprehensive Testing Guide

### 📋 Prerequisites & Setup

#### Required Hardware
- **Android Device**: USB-C port, API 24+ (Android 7.0+), 4GB+ RAM
- **Shimmer3 GSR+ Sensor**: With charged battery and proper firmware
- **Topdon TC001 Thermal Camera**: USB-C connection cable
- **PC/Laptop**: Windows 10+/macOS 10.15+/Ubuntu 18.04+, 8GB+ RAM
- **Network**: Stable WiFi network for device communication

#### Software Prerequisites
- **Android Studio**: Latest version with Android SDK 34
- **Java Development Kit**: JDK 24
- **Python**: 3.8+ (3.11+ recommended)
- **Git**: For repository management

### 📱 Android App Testing

#### 🔧 Environment Setup
1. **Clone and Setup**:
   ```bash
   git clone <repository-url>
   cd fyp-gsr-unified-buccancs/android
   ```

2. **Build Environment**:
   ```bash
   # Clean and build project
   ./gradlew clean build

   # Verify dependencies
   ./gradlew dependencies

   # Check for updates
   ./gradlew dependencyUpdates
   ```

#### 🧪 Automated Testing
1. **Unit Tests** (Fast execution, no hardware required):
   ```bash
   # Run all unit tests
   ./gradlew test

   # Run specific component tests
   ./gradlew test --tests GsrSensorManagerTest
   ./gradlew test --tests ThermalCameraManagerTest
   ./gradlew test --tests NetworkClientTest
   ./gradlew test --tests RecordingControllerTest
   ./gradlew test --tests TimeManagerTest

   # Generate detailed coverage report
   ./gradlew testDebugUnitTestCoverage
   # Report available at: android/app/build/reports/coverage/test/debug/
   ```

2. **Integration Tests** (Requires connected device):
   ```bash
   # Run instrumentation tests
   ./gradlew connectedAndroidTest

   # Run specific integration tests
   ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.buccancs.gsrcapture.CameraIntegrationTest
   ```

#### 📲 Manual Testing Procedures

1. **Installation & Permissions**:
   ```bash
   # Install debug APK
   ./gradlew installDebug

   # Or install specific build
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

   - Launch app and grant all permissions:
     - Camera access
     - Microphone access
     - Bluetooth access
     - Storage access
     - Location access (for Bluetooth LE)

2. **Hardware Connection Testing**:
   - **GSR Sensor**: 
     - Turn on Shimmer3 GSR+ sensor
     - Pair via Bluetooth in app settings
     - Verify green connection indicator
     - Check real-time GSR readings display

   - **Thermal Camera**:
     - Connect Topdon TC001 via USB-C
     - Verify thermal preview appears
     - Test preview mode switching (RGB ↔ Thermal)

   - **Camera System**:
     - Verify RGB camera preview
     - Test focus and exposure controls
     - Check preview quality and frame rate

3. **Recording Functionality Testing**:
   ```bash
   # Test recording workflow
   1. Start app with all sensors connected
   2. Verify all status indicators are green
   3. Tap Record button
   4. Record for 30 seconds minimum
   5. Stop recording
   6. Verify file creation and structure
   ```

4. **Data Verification**:
   - Check file structure in device storage:
     ```
     /Android/data/com.buccancs.gsrcapture/files/Movies/GSRCapture/
     └── session_YYYYMMDD_HHMMSS_xxxxxxxx/
         ├── raw_rgb_session_YYYYMMDD_HHMMSS_xxxxxxxx/
         ├── thermal_session_YYYYMMDD_HHMMSS_xxxxxxxx/
         ├── RGB_session_YYYYMMDD_HHMMSS_xxxxxxxx.mp4
         ├── GSR_session_YYYYMMDD_HHMMSS_xxxxxxxx.csv
         └── Audio_session_YYYYMMDD_HHMMSS_xxxxxxxx.wav
     ```
   - Verify timestamp consistency across files
   - Check file sizes and content validity

### 💻 PC Controller Testing

#### 🔧 Environment Setup
1. **Repository Setup**:
   ```bash
   cd fyp-gsr-unified-buccancs/windows_controller

   # Create virtual environment (recommended)
   python -m venv venv

   # Activate virtual environment
   # Windows:
   venv\Scripts\activate
   # macOS/Linux:
   source venv/bin/activate
   ```

2. **Dependencies Installation**:
   ```bash
   # Install all dependencies
   pip install -r requirements.txt

   # Verify PySide6 installation
   python -c "import PySide6; print(f'PySide6 version: {PySide6.__version__}')"

   # Check all critical dependencies
   python -c "import cv2, zeroconf, pyqtgraph; print('All dependencies OK')"
   ```

#### 🧪 Automated Testing
1. **Unit Tests** (Component testing):
   ```bash
   # Run all tests with verbose output
   python -m pytest tests/ -v

   # Run specific component tests
   python -m pytest tests/test_device_manager.py -v
   python -m pytest tests/test_ui_components.py -v
   python -m pytest tests/test_session_manager.py -v
   python -m pytest tests/test_sdk_integrations.py -v

   # Generate comprehensive coverage report
   python -m pytest tests/ --cov=src --cov-report=html --cov-report=term
   # HTML report available at: htmlcov/index.html
   ```

2. **Integration Tests** (Cross-component testing):
   ```bash
   # Test device discovery and connection
   python -m pytest tests/test_device.py -v

   # Test UI integration
   python -m pytest tests/test_ui_components.py::TestMainWindowIntegration -v
   ```

#### 🖥️ GUI Testing & Validation
1. **Interface Testing**:
   ```bash
   # Test modern PySide6 interface
   python test_modern_gui.py

   # Test video playback functionality
   python test_video_playback.py

   # Test all UI components
   python -m pytest tests/test_ui_components.py -v
   ```

2. **Manual GUI Testing Checklist**:
   - **Application Launch**:
     - Verify application starts without errors
     - Check high-DPI scaling on different displays
     - Confirm modern blue/gray theme loads correctly

   - **Main Interface**:
     - Test all tabs (Dashboard, Devices, Status, Logs, Playback)
     - Verify emoji buttons (🔴 Start, ⏹️ Stop) display correctly
     - Check responsive design on different window sizes

   - **Device Management**:
     - Test device discovery simulation
     - Verify device connection/disconnection UI updates
     - Check status indicators and color coding

   - **Session Management**:
     - Test session creation and management
     - Verify manifest generation
     - Check log viewer functionality

#### 🔗 Network & Communication Testing
1. **Simulated Testing** (No hardware required):
   ```bash
   # Test device discovery simulation
   python src/main/main.py --test-mode

   # Test network protocols
   python -m pytest tests/test_device_manager.py::TestNetworkProtocol -v
   ```

2. **Hardware Integration Testing** (Requires Android device):
   ```bash
   # Test with actual Android device
   # 1. Start PC controller
   python src/main/main.py

   # 2. Launch Android app on device
   # 3. Test device discovery and connection
   # 4. Verify command protocol functionality
   ```

### 🔗 Integration Testing - End-to-End System Validation

#### 🌐 Network Communication Testing
1. **Device Discovery & Connection**:
   ```bash
   # Test Scenario 1: Basic Connection
   1. Start PC controller: python src/main/main.py
   2. Launch Android app on device
   3. Verify device appears in PC discovery list
   4. Test connection establishment
   5. Check status indicators on both sides

   # Test Scenario 2: Multiple Devices
   1. Connect 2-4 Android devices to same network
   2. Launch app on all devices
   3. Verify all devices discovered by PC
   4. Test simultaneous connections
   5. Monitor connection stability
   ```

2. **Command Protocol Validation**:
   ```bash
   # Test remote control commands
   1. Connect Android device to PC
   2. Send START command from PC
   3. Verify Android app begins recording
   4. Send STOP command from PC
   5. Verify Android app stops recording
   6. Check command acknowledgments
   ```

3. **Network Resilience Testing**:
   - Test connection recovery after network interruption
   - Verify behavior with poor network conditions
   - Test device reconnection after app restart
   - Validate error handling for network failures

#### 📹 Complete Recording Workflow Testing
1. **Hardware Setup Validation**:
   ```bash
   # Pre-recording checklist
   ✓ Shimmer3 GSR+ sensor paired and connected
   ✓ Topdon TC001 thermal camera connected via USB-C
   ✓ Android device on same WiFi as PC
   ✓ All permissions granted on Android
   ✓ PC controller connected to Android device
   ```

2. **Synchronized Recording Test**:
   ```bash
   # Complete workflow test
   1. Start PC controller
   2. Connect Android device(s)
   3. Verify all sensors connected (green indicators)
   4. Start recording from PC controller
   5. Record for 2-5 minutes
   6. Stop recording from PC controller
   7. Verify all devices stopped simultaneously
   ```

3. **Multi-Device Coordination**:
   ```bash
   # Test with multiple Android devices
   1. Connect 2+ Android devices to PC
   2. Ensure all have required hardware
   3. Start synchronized recording on all devices
   4. Monitor real-time status on PC dashboard
   5. Stop recording simultaneously
   6. Verify session consistency across devices
   ```

#### 📊 Data Validation & Quality Assurance
1. **File Structure Verification**:
   ```bash
   # Check output structure on each device
   /Android/data/com.buccancs.gsrcapture/files/Movies/GSRCapture/
   └── session_YYYYMMDD_HHMMSS_xxxxxxxx/
       ├── raw_rgb_session_YYYYMMDD_HHMMSS_xxxxxxxx/
       │   ├── frame_1234567890123456789.jpg
       │   └── ... (multiple frames)
       ├── thermal_session_YYYYMMDD_HHMMSS_xxxxxxxx/
       │   ├── frame_1234567890123456789.jpg
       │   └── ... (multiple frames)
       ├── RGB_session_YYYYMMDD_HHMMSS_xxxxxxxx.mp4
       ├── GSR_session_YYYYMMDD_HHMMSS_xxxxxxxx.csv
       ├── Audio_session_YYYYMMDD_HHMMSS_xxxxxxxx.wav
       └── session_metadata.json
   ```

2. **Timestamp Synchronization Analysis**:
   ```bash
   # Verify timing accuracy
   1. Check timestamp consistency across all files
   2. Validate nanosecond precision in frame names
   3. Verify session start/stop times match
   4. Calculate synchronization accuracy between devices
   5. Check for frame drops or timing gaps
   ```

3. **Data Quality Validation**:
   ```bash
   # Content verification
   1. Video files: Check resolution, frame rate, codec
   2. Audio files: Verify sample rate, channels, format
   3. GSR data: Check sampling rate, data continuity
   4. Raw images: Verify frame count and quality
   5. Thermal images: Check temperature mapping
   6. Metadata: Validate JSON structure and content
   ```

#### 🔄 Session Management Testing
1. **Session Manifest Validation**:
   ```bash
   # PC-generated session manifest
   1. Complete a recording session
   2. Check manifest generation on PC
   3. Verify device information accuracy
   4. Validate file references and paths
   5. Check metadata completeness
   ```

2. **Multi-Session Testing**:
   ```bash
   # Test session isolation
   1. Record multiple sessions back-to-back
   2. Verify unique session IDs
   3. Check file organization separation
   4. Validate no data cross-contamination
   ```

#### 🚨 Error Handling & Recovery Testing
1. **Hardware Disconnection Scenarios**:
   ```bash
   # Test robustness
   1. Disconnect GSR sensor during recording
   2. Unplug thermal camera mid-session
   3. Force-close Android app during recording
   4. Disconnect network during session
   5. Verify graceful error handling and recovery
   ```

2. **Resource Limitation Testing**:
   ```bash
   # Test under constraints
   1. Record with low device storage
   2. Test with low battery conditions
   3. Record with poor network connectivity
   4. Test with high CPU/memory usage
   ```

#### 📈 Performance & Load Testing
1. **Extended Session Testing**:
   ```bash
   # Long-duration recording
   1. Record for 30+ minutes continuously
   2. Monitor memory usage and performance
   3. Check for memory leaks or degradation
   4. Verify data integrity over time
   ```

2. **Stress Testing**:
   ```bash
   # Maximum load scenarios
   1. Connect maximum number of devices (4+)
   2. Record at highest quality settings
   3. Monitor system resource usage
   4. Test network bandwidth limits
   5. Verify system stability under load
   ```

## 📱 How to Use the Android App - Complete User Guide

### 🚀 Initial Setup & Installation

#### 📲 App Installation
1. **Download & Install**:
   ```bash
   # Option 1: Install from APK
   adb install app-debug.apk

   # Option 2: Build and install from source
   cd android
   ./gradlew installDebug
   ```

2. **Grant Essential Permissions**:
   Upon first launch, grant the following permissions:
   - **📷 Camera**: Required for RGB video recording and raw image capture
   - **🎤 Microphone**: Required for audio recording
   - **📶 Bluetooth**: Required for GSR sensor connectivity
   - **💾 Storage**: Required for saving recorded data
   - **📍 Location**: Required for Bluetooth LE device discovery
   - **🌐 Network**: Required for PC controller communication

#### 🔌 Hardware Connection Setup

1. **GSR Sensor (Shimmer3 GSR+)**:
   ```bash
   # Setup procedure
   1. Charge Shimmer3 GSR+ sensor fully
   2. Turn on sensor (LED should blink)
   3. Open Android app
   4. Go to Settings → Bluetooth Devices
   5. Scan for devices and select your Shimmer
   6. Pair and connect (LED turns solid when connected)
   7. Verify green status indicator in main app
   ```

2. **Thermal Camera (Topdon TC001)**:
   ```bash
   # Connection procedure
   1. Connect Topdon TC001 to device via USB-C cable
   2. Grant USB device permission when prompted
   3. Verify thermal preview appears in app
   4. Test preview mode switching (RGB ↔ Thermal)
   5. Check thermal status indicator turns green
   ```

3. **Audio System**:
   - Built-in microphone automatically detected
   - No additional setup required
   - Verify microphone permission granted

#### 🌐 Network Configuration
1. **WiFi Setup**:
   ```bash
   # Network requirements
   1. Connect Android device to same WiFi as PC controller
   2. Ensure network allows device-to-device communication
   3. Note device IP address: Settings → About Phone → Status
   4. Verify network connectivity indicator in app
   ```

2. **PC Controller Pairing**:
   - Launch PC controller application
   - Enable device discovery on PC
   - Android device should appear in PC device list
   - Establish connection from PC interface

### 🎬 Recording Process - Step-by-Step Guide

#### 🔍 Pre-Recording Checklist
1. **System Status Verification**:
   ```bash
   # Check all indicators before recording
   ✓ GSR Sensor: Green indicator (connected and streaming)
   ✓ Thermal Camera: Green indicator (connected and previewing)
   ✓ RGB Camera: Green indicator (preview active)
   ✓ Audio: Green indicator (microphone ready)
   ✓ Network: Green indicator (PC connected, if applicable)
   ✓ Storage: Sufficient space available (check status bar)
   ✓ Battery: >30% recommended for extended sessions
   ```

2. **Preview & Calibration**:
   - **Camera Preview**: Verify RGB camera shows clear image
   - **Thermal Preview**: Check thermal camera displays temperature data
   - **Preview Toggle**: Test switching between RGB ↔ Thermal views
   - **GSR Baseline**: Allow 30 seconds for GSR sensor to stabilize
   - **Focus & Exposure**: Tap screen to adjust camera focus if needed

3. **Session Configuration**:
   - **Session ID**: Note auto-generated session ID or set custom ID
   - **Recording Quality**: Verify settings (1080p RGB, thermal resolution)
   - **Duration Planning**: Estimate session length for storage planning

#### ▶️ Starting Recording Session
1. **Manual Recording Start**:
   ```bash
   # Local recording initiation
   1. Tap the large "Record" button (🔴)
   2. Button turns red and shows "Recording..."
   3. Timer starts showing elapsed time
   4. All status indicators should remain green
   ```

2. **Remote Recording Start** (PC-controlled):
   ```bash
   # PC-initiated recording
   1. PC controller sends START command
   2. Android app receives command
   3. Recording begins automatically
   4. Confirmation sent back to PC
   5. Status updates appear on both PC and Android
   ```

3. **Multi-Modal Activation**:
   When recording starts, all systems activate simultaneously:
   - **📹 RGB Video**: 1080p @ 30fps H.264 recording
   - **📸 Raw RGB Images**: ~30 FPS frame-by-frame capture
   - **🌡️ Thermal Frames**: Individual thermal images with timestamps
   - **📊 GSR Data**: 128Hz continuous sensor data logging
   - **🎵 Audio**: 44.1kHz stereo WAV recording

#### 🎥 During Recording - Monitoring & Management
1. **Real-Time Monitoring**:
   ```bash
   # Live data display
   - GSR Value: Current reading in microSiemens (μS)
   - Heart Rate: Calculated from PPG signal (BPM)
   - Recording Time: Elapsed session duration
   - Frame Count: Number of captured frames
   - Storage Used: Current session data size
   - Battery Level: Device power status
   ```

2. **Status Monitoring**:
   - **Connection Health**: All indicators should remain green
   - **Data Flow**: Verify real-time GSR values are updating
   - **Preview Quality**: Check camera preview remains stable
   - **Storage Space**: Monitor available storage (warning at <1GB)

3. **Best Practices During Recording**:
   - **Avoid Hardware Disconnection**: Keep all cables connected
   - **Maintain Network**: Keep WiFi connection stable
   - **Monitor Battery**: Watch for low battery warnings
   - **Minimize Interruptions**: Avoid incoming calls or notifications
   - **Stable Positioning**: Keep device and cameras steady

#### ⏹️ Stopping Recording Session
1. **Manual Recording Stop**:
   ```bash
   # Local recording termination
   1. Tap the "Stop" button (⏹️)
   2. Button returns to normal state
   3. "Saving..." indicator appears
   4. Wait for all files to be written to storage
   5. "Recording Complete" confirmation appears
   ```

2. **Remote Recording Stop** (PC-controlled):
   ```bash
   # PC-initiated stop
   1. PC controller sends STOP command
   2. Android app receives command
   3. Recording stops automatically
   4. File saving process begins
   5. Completion status sent to PC
   ```

3. **Post-Recording Process**:
   ```bash
   # Automatic file processing
   1. All data streams stop simultaneously
   2. Video files are finalized and saved
   3. Raw images are organized in directories
   4. GSR data is written to CSV file
   5. Audio file is finalized
   6. Session metadata JSON is generated
   7. File verification and integrity check
   ```

#### 📁 Data Output Verification
After recording completion, verify the following structure:
```
/Android/data/com.buccancs.gsrcapture/files/Movies/GSRCapture/
└── session_YYYYMMDD_HHMMSS_xxxxxxxx/
    ├── raw_rgb_session_YYYYMMDD_HHMMSS_xxxxxxxx/
    │   ├── frame_1234567890123456789.jpg
    │   ├── frame_1234567890123456790.jpg
    │   └── ... (hundreds of frames)
    ├── thermal_session_YYYYMMDD_HHMMSS_xxxxxxxx/
    │   ├── frame_1234567890123456789.jpg
    │   ├── frame_1234567890123456790.jpg
    │   └── ... (hundreds of frames)
    ├── RGB_session_YYYYMMDD_HHMMSS_xxxxxxxx.mp4
    ├── GSR_session_YYYYMMDD_HHMMSS_xxxxxxxx.csv
    ├── Audio_session_YYYYMMDD_HHMMSS_xxxxxxxx.wav
    └── session_metadata.json
```

### 🔧 Troubleshooting Common Issues

#### 📶 GSR Sensor Connection Problems
- **Sensor Won't Pair**:
  - Ensure Shimmer3 GSR+ is fully charged
  - Reset sensor by holding power button for 10 seconds
  - Clear Bluetooth cache: Settings → Apps → Bluetooth → Storage → Clear Cache
  - Try pairing from Android Bluetooth settings first

- **Connection Drops During Recording**:
  - Check sensor battery level
  - Ensure sensor is within 10 meters of device
  - Avoid interference from other Bluetooth devices
  - Restart both sensor and app if connection is unstable

#### 🌡️ Thermal Camera Issues
- **Camera Not Detected**:
  - Verify USB-C connection is secure
  - Check device supports USB host mode
  - Try different USB-C cable
  - Grant USB device permission when prompted
  - Restart app after connecting camera

- **Thermal Preview Not Showing**:
  - Disconnect and reconnect thermal camera
  - Check camera LED indicators
  - Verify camera firmware is compatible
  - Try connecting camera before launching app

#### 📱 Recording & Performance Issues
- **Recording Fails to Start**:
  - Check available storage space (minimum 2GB recommended)
  - Verify all permissions are granted
  - Ensure all sensors are connected (green indicators)
  - Restart app if any sensors show red status

- **Poor Performance/Lag**:
  - Close other running applications
  - Ensure device has sufficient RAM (4GB+ recommended)
  - Lower recording quality if performance issues persist
  - Check device temperature (avoid overheating)

#### 🌐 Network & Synchronization Issues
- **PC Controller Can't Find Device**:
  - Ensure both devices on same WiFi network
  - Check firewall settings on PC
  - Verify network allows device-to-device communication
  - Try manual IP address entry on PC

- **Poor Synchronization**:
  - Ensure stable network connection
  - Check network latency and bandwidth
  - Restart both Android app and PC controller
  - Verify time synchronization between devices

## 💻 How to Use the PC Controller - Complete User Guide

### 🚀 Installation & Setup

#### 📥 System Requirements & Installation
1. **Prerequisites Check**:
   ```bash
   # Verify Python version (3.8+ required, 3.11+ recommended)
   python --version

   # Check pip availability
   pip --version

   # Verify system compatibility
   # Windows 10+, macOS 10.15+, or Ubuntu 18.04+
   ```

2. **Repository Setup**:
   ```bash
   # Clone the repository
   git clone https://github.com/your-username/fyp-gsr-unified-buccancs.git
   cd fyp-gsr-unified-buccancs/windows_controller

   # Create virtual environment (recommended)
   python -m venv venv

   # Activate virtual environment
   # Windows:
   venv\Scripts\activate
   # macOS/Linux:
   source venv/bin/activate
   ```

3. **Dependencies Installation**:
   ```bash
   # Install all required packages
   pip install -r requirements.txt

   # Verify critical dependencies
   python -c "import PySide6; print(f'PySide6 {PySide6.__version__} installed')"
   python -c "import cv2; print(f'OpenCV {cv2.__version__} installed')"
   python -c "import zeroconf; print('Zeroconf installed')"
   ```

4. **First Launch**:
   ```bash
   # Launch the application
   python src/main/main.py

   # Alternative: Test mode for development
   python src/main/main.py --test-mode
   ```

#### 🖥️ Interface Overview & Navigation
Upon launching, you'll see the modern PySide6 interface with:
- **Professional Design**: Blue/gray color scheme with modern flat design
- **Tabbed Interface**: Organized sections for different functions
- **High-DPI Support**: Crisp rendering on all display types
- **Responsive Layout**: Adapts to different window sizes (minimum 1400x900)

### 🎛️ Main Interface Components

#### 📋 Tabbed Interface Layout
The PC Controller features a modern tabbed interface with the following sections:

1. **🏠 Dashboard Tab**:
   - System overview and status summary
   - Central recording controls (🔴 Start, ⏹️ Stop)
   - Session management and configuration
   - Quick access to essential functions

2. **📱 Devices Tab**:
   - Device discovery and connection management
   - Real-time device status panels
   - Connection health monitoring
   - Device-specific controls and settings

3. **📊 Status Tab**:
   - Comprehensive system status dashboard
   - Color-coded indicators for all components
   - Real-time performance monitoring
   - Network and synchronization status

4. **📝 Logs Tab**:
   - Advanced logging with multiple severity levels
   - Real-time log filtering and search
   - Export functionality for troubleshooting
   - System event tracking

5. **🎬 Playback Tab**:
   - Integrated video playback system
   - Multi-file playlist management
   - Real-time annotation capabilities
   - Analysis and export tools

### 📱 Device Management - Complete Workflow

#### 🔍 Device Discovery Process
1. **Automatic Discovery**:
   ```bash
   # Step-by-step discovery process
   1. Navigate to "Devices" tab in PC controller
   2. Click "🔍 Discover Devices" button
   3. PC scans local network using Zeroconf/mDNS
   4. Available Android devices appear in discovery list
   5. Each device shows: Name, IP address, status
   ```

2. **Manual Device Entry**:
   ```bash
   # For devices not auto-discovered
   1. Click "➕ Add Device Manually"
   2. Enter device IP address (e.g., 192.168.1.100)
   3. Specify port (default: 8080)
   4. Click "Connect" to establish connection
   ```

3. **Discovery Troubleshooting**:
   - **No Devices Found**: Check WiFi network, ensure devices on same subnet
   - **Partial Discovery**: Verify firewall settings, check network permissions
   - **Connection Timeout**: Increase discovery timeout in settings

#### 🔗 Device Connection Management
1. **Establishing Connections**:
   ```bash
   # Connection workflow
   1. Select device(s) from discovery list
   2. Click "🔗 Connect" for each device
   3. Monitor connection progress in status panel
   4. Verify green connection indicator
   5. Check device appears in "Connected Devices" list
   ```

2. **Connection Status Monitoring**:
   - **🟢 Green**: Connected and responsive
   - **🟡 Yellow**: Connected but slow response
   - **🔴 Red**: Connection failed or lost
   - **⚪ Gray**: Not connected

3. **Multi-Device Connection**:
   ```bash
   # Connecting multiple devices
   1. Discover all available devices
   2. Select multiple devices (Ctrl+Click)
   3. Click "🔗 Connect All Selected"
   4. Monitor individual connection status
   5. Verify all devices show green status
   ```

#### 📊 Real-Time Device Status Monitoring
1. **Status Dashboard Overview**:
   Navigate to **Status** tab to view comprehensive device information:

   ```bash
   # Device status information displayed
   - Device Name: Android device identifier
   - IP Address: Network location
   - Connection Health: Latency and stability
   - Battery Level: Current charge percentage
   - Storage Available: Free space for recordings
   - Sensor Status: Individual sensor connectivity
     ✓ RGB Camera: Ready/Recording/Error
     ✓ Thermal Camera: Connected/Disconnected
     ✓ GSR Sensor: Paired/Streaming/Error
     ✓ Audio: Ready/Recording
   ```

2. **Individual Device Panels**:
   Each connected device has a dedicated panel showing:
   - **Real-time metrics**: Battery, storage, network quality
   - **Sensor indicators**: Color-coded status for each sensor
   - **Control buttons**: Device-specific actions
   - **Performance graphs**: Connection quality over time

3. **Alert System**:
   - **Low Battery**: Warning when device battery <20%
   - **Storage Full**: Alert when available storage <1GB
   - **Connection Issues**: Notification for network problems
   - **Sensor Errors**: Immediate alerts for hardware disconnections

### Recording Sessions

1. **Session Setup**:
   - Ensure all desired devices are connected
   - Configure session parameters if needed
   - Verify all device sensors are ready

2. **Start Recording**:
   - Click **🔴 Start Recording** in Dashboard
   - All connected devices begin recording simultaneously
   - Monitor progress in real-time status display

3. **During Recording**:
   - Watch live video previews (when implemented)
   - Monitor device status for any issues
   - View real-time logs for system events

4. **Stop Recording**:
   - Click **⏹️ Stop Recording** in Dashboard
   - Wait for all devices to complete file saving
   - Session manifest is automatically generated

### Data Management

1. **Session Manifests**:
   - Generated automatically after each session
   - Contains metadata for all devices and files
   - Saved in `sessions/` directory

2. **File Collection**:
   - Use **Collect Files** feature (when implemented)
   - Manually transfer files from devices
   - Organize data using session manifests

3. **Video Playback**:
   - Use **Playback** tab for video analysis
   - Load multiple video files for comparison
   - Add annotations and export analysis

### Advanced Features

1. **Log Management**:
   - Filter logs by level (Debug, Info, Warning, Error)
   - Export logs for troubleshooting
   - Real-time log monitoring during sessions

2. **Configuration**:
   - Adjust recording parameters
   - Configure network settings
   - Set default file locations

3. **Integration**:
   - LSL (Lab Streaming Layer) support
   - PsychoPy experiment integration
   - Custom plugin development

### Troubleshooting

- **Devices Not Discovered**: Check WiFi network, restart discovery
- **Connection Failures**: Verify network connectivity, check firewall settings
- **Recording Issues**: Check device storage, verify sensor connections
- **Performance Problems**: Close unnecessary applications, check system resources

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📚 Comprehensive Documentation

This project includes extensive documentation to help you get started and make the most of the system:

### 🚀 New User? Start Here
- **[📖 Documentation Index](docs/DOCUMENTATION_INDEX.md)** - Complete guide to all available documentation
- **[🔧 Setup and Connection Guide](docs/SETUP_AND_CONNECTION_GUIDE.md)** - Step-by-step hardware setup and connection instructions
- **[📱 App Usage Guide](docs/APP_USAGE_GUIDE.md)** - Complete user guide for Android app and PC controller

### 🔧 Developers and Advanced Users
- **[🐍 Python API Guide](docs/PYTHON_API_GUIDE.md)** - Comprehensive API reference for programmatic control
- **[⚙️ Development Setup Guide](DEVELOPMENT_SETUP.md)** - Development environment configuration
- **[🌐 Networking Documentation](docs/Networking_and_Synchronization_Layer.md)** - Technical implementation details

### 📋 Quick Links
| I want to... | Go to... |
|--------------|----------|
| Set up the system for the first time | [Setup and Connection Guide](docs/SETUP_AND_CONNECTION_GUIDE.md) |
| Learn how to use the apps | [App Usage Guide](docs/APP_USAGE_GUIDE.md) |
| Control the system with Python code | [Python API Guide](docs/PYTHON_API_GUIDE.md) |
| Develop or modify the system | [Development Setup Guide](DEVELOPMENT_SETUP.md) |
| Find specific documentation | [Documentation Index](docs/DOCUMENTATION_INDEX.md) |

## 📞 Support

For technical support or questions:
- **First**: Check the [Documentation Index](docs/DOCUMENTATION_INDEX.md) for the right guide
- **Then**: Review troubleshooting sections in the relevant documentation
- **Finally**: Create an issue in the GitHub repository with detailed information

---

**Note**: This system is designed for research purposes and requires specific hardware components. Ensure you have the necessary equipment before attempting to use the system.

## Repository Structure

```
.
├── android/              # Android application code
│   ├── app/              # Android app module
│   └── build.gradle      # Android build configuration
├── windows_controller/   # Windows PC Controller application (PySide6)
│   ├── src/              # Source code
│   │   ├── ui/           # Modern GUI components
│   │   ├── network/      # Device management and networking
│   │   ├── utils/        # Utility functions and session management
│   │   └── main/         # Application entry point
│   ├── requirements.txt  # Python dependencies (PySide6-based)
│   └── README.md         # Windows controller documentation
├── windows/              # Legacy Windows application code
│   ├── app/              # Windows app module
│   └── setup.py          # Python package configuration
├── shared/               # Shared code used by both platforms
│   ├── models/           # Shared data models
│   └── utils/            # Shared utility classes
├── docs/                 # Documentation
└── README.md             # This file
```

## Implementation of Shared and Cross-Cutting Features

### Monorepo Code Integration

This repository maintains a unified codebase for the entire system (Android app, Windows app, and common libraries). This ensures consistency in definitions for data formats, network protocols, and easier project management.

Common data models (e.g., `DataEvent` class) are defined once in the `shared/models` directory and used by both Android and Windows applications. Changes to the protocol or format are synchronized across platforms.

Build processes for each platform (Gradle for Android, Python for Windows) reside in the same repository for coordinated versioning.

### Unified Naming Conventions

All output files and data streams follow a consistent naming scheme using the `FileNamingUtils` class. This includes using a session ID or timestamp in file names and folder structure so that data from multiple devices can be easily correlated.

Each device has an identifier in the filename (e.g., `Session123_DeviceA_rgb.mp4`). The PC generates or propagates the session ID to all devices at start, and the Android app incorporates this ID into its file naming.

### Standardized Data Formats

The system defines how each modality's data is stored and formatted across platforms using the `DataFormatUtils` class:

- Video files: MP4 with H.264 codec
- Sensor data: CSV with timestamp columns
- Audio: WAV
- Session metadata: JSON

Both Android and Windows adhere to these formats, ensuring that when data is aggregated, all streams can be parsed together by analysis tools.

### Consistent Timestamp Schema

The system establishes a scheme for timestamps that allows data from different devices to be merged on a common timeline using the `TimeSyncUtils` class. All recorded timestamps are in Unix epoch milliseconds.

The PC sends a "start time" reference to all devices which they use as zero-time for their logs, or each device's monotonic timestamps are offset to the PC's clock during synchronization.

### Session ID and Metadata Propagation

The system generates a unique session identifier for each recording session and uses it across all components. The PC creates this (e.g., "Session_2023-07-01_1300") and instructs each Android device to tag its data with this ID.

Metadata like participant info or experimental conditions are also included in the session manifest.

### Comprehensive Session Manifest

The PC produces a manifest (e.g., `session_info.json`) that compiles metadata: device IDs, start times, file paths, sensor types, firmware versions, etc. This is implemented using the `SessionInfo` class.

The Android app sends device-specific info to the PC (either at connect time or start of recording) so it can be logged. The manifest is critical for users to understand and verify the dataset after a session.

### Multi-Device Data Alignment and Aggregation

The system ensures that data from different sources can be merged for analysis using the `DataAlignmentUtils` class. After a session, one can align the time-series from each phone and the PC with minimal post-processing.

This is achieved via the shared timestamp schema and sync events. The system can also produce a combined timeline (e.g., a CSV of sync timestamps or an analysis report of sync quality).

## Implementation Details

### Android Capture App Implementation

The Android Capture App has been implemented with the following components:

#### Core Components

1. **RGB Video Capture**: Implemented using the CameraX API in `RgbCameraManager.kt`. Records high-definition color video at 1080p/30fps.

2. **Raw RGB Image Capture**: NEW - Implemented in `RgbCameraManager.kt`. Captures raw images before ISP processing using CameraX ImageCapture with minimize latency mode. Images are saved frame-by-frame with nanosecond timestamp naming at ~30 FPS for precise temporal analysis.

3. **Thermal Video Capture**: Implemented in `ThermalCameraManager.kt`. Interfaces with the Topdon TC001 thermal camera via USB-C to capture infrared video frames. Thermal frames are also saved individually with nanosecond timestamp naming for frame-by-frame analysis.

4. **GSR Sensor Data Capture**: Implemented in `GsrSensorManager.kt`. Connects to a Shimmer3 GSR+ sensor over Bluetooth Low Energy to stream GSR readings at 128 Hz.

5. **Audio Recording**: Implemented in `AudioRecorder.kt`. Captures synchronized audio via the phone's microphone at 44.1 kHz stereo, saved to WAV format.

6. **Real-time Preview & Mode Toggle**: Implemented in `MainActivity.kt`. Provides live preview of both RGB and thermal camera feeds with the ability to switch between views.

7. **Unified Recording Control**: Implemented in `RecordingController.kt`. Provides a simple one-touch interface to start and stop recordings for all data streams simultaneously, including the new raw image capture functionality.

8. **Local Data Storage**: Implemented across all manager classes. Saves captured data locally with a consistent naming scheme based on session ID and timestamp. Creates organized directory structure with separate folders for raw RGB images and thermal frames.

9. **Bluetooth Sensor Pairing & Status**: Implemented in `GsrSensorManager.kt`. Manages pairing and connection to the Shimmer GSR sensor with status feedback.

10. **Sensor Status and Feedback UI**: Implemented in `MainActivity.kt`. Displays visual indicators for each data source's status and real-time values.

11. **Timestamping & Synchronization**: Implemented in `TimeManager.kt`. Uses a unified clock (SystemClock.elapsedRealtimeNanos) to timestamp all sensor readings, video frames, and raw images with nanosecond precision.

12. **Network Remote Control Client**: Implemented in `NetworkClient.kt`. Allows the app to be remote-controlled by the PC controller.

#### Extended Features

1. **PPG/Heart Rate Derivation**: Implemented in `GsrSensorManager.kt`. Computes real-time heart rate from the PPG signal provided by the Shimmer GSR+ sensor.

#### Architecture

The app follows a modular architecture with clear separation of concerns:

- **MainActivity**: Main UI and entry point
- **RecordingController**: Coordinates all recording components
- **Camera Package**: Handles RGB and thermal video capture, including raw image capture
- **Sensor Package**: Manages GSR sensor connection and data
- **Audio Package**: Handles audio recording
- **Network Package**: Manages communication with the PC controller
- **Utils Package**: Provides utility classes like TimeManager

#### Recent Upgrades (2024)

The Android application has been comprehensively upgraded to the latest technologies:

**Platform Upgrades:**
- **Java 24**: Updated from Java 1.8 to Java 24 for access to latest language features and performance improvements
- **Kotlin 2.0.0**: Updated from Kotlin 1.8.0 to 2.0.0 for enhanced compiler performance and new language features
- **Android SDK 34**: Updated from API 33 to API 34 (Android 14) for latest Android features and security improvements
- **Gradle 8.14**: Updated build system for better performance and compatibility

**AndroidX Library Updates:**
- **Core KTX**: 1.9.0 → 1.12.0
- **AppCompat**: 1.6.1 → 1.7.0
- **Material Design**: 1.8.0 → 1.11.0
- **ConstraintLayout**: 2.1.4 → 2.2.0
- **Lifecycle Components**: 2.5.1 → 2.7.0
- **CameraX**: 1.2.2 → 1.3.1 (enables enhanced raw image capture capabilities)

**Third-Party Dependencies:**
- **Kotlin Coroutines**: 1.6.4 → 1.7.3
- **Nordic BLE Library**: 2.5.1 → 2.7.4
- **USB Serial**: 3.4.6 → 3.7.0
- **OkHttp**: 4.10.0 → 4.12.0

All upgrades maintain backward compatibility while providing enhanced performance, security, and access to latest Android features.

### PC Controller App Implementation (Cross-Platform)

The PC Controller App has been fully upgraded to PySide6 with a modern GUI interface and supports Windows, macOS, and Linux:

#### Modern GUI Framework (PySide6)

The application has been completely upgraded from PyQt5 to PySide6, providing:
- **Official Qt Support**: Using PySide6 (the official Qt binding for Python)
- **Modern Interface**: Professional blue and gray color scheme with rounded corners
- **Enhanced Usability**: Larger buttons with emojis, improved typography, and better spacing
- **Responsive Design**: Better scaling on different screen sizes (1400x900 minimum)
- **Cross-Platform Compatibility**: Consistent experience across Windows, macOS, and Linux

#### Core Components

1. **Multi-Device Connection Management**: Implemented in `device_manager.py`. Provides a framework for discovering and connecting to multiple Android capture devices using Zeroconf/mDNS discovery. Features modern device panels with real-time status indicators.

2. **Synchronized Start/Stop Control**: Implemented in `main_window.py` with modern control buttons featuring emojis (🔴 Start, ⏹️ Stop). Provides central control to start and stop recording simultaneously on all connected devices with enhanced visual feedback.

3. **Device Status Dashboard**: Implemented in `status_dashboard.py`. Shows real-time status of each connected device in a modern tabular interface with color-coded indicators for connection health, battery level, storage remaining, and sensor stream status.

4. **Data Logging and Monitoring**: Implemented in `log_viewer.py` with modern text formatting and color-coded log levels. Provides live log view with filtering capabilities and export functionality.

5. **Session Manifest & Metadata Generation**: Fully implemented in `session_manager.py`. Compiles comprehensive session manifests with device information, timestamps, and file references.

6. **Video Playback and Annotation**: Implemented in `video_playback_window.py` with PySide6 multimedia support. Features video playlist management, real-time annotation capabilities, and export functionality for research analysis.

#### Extended Features

1. **Live Video Preview from Devices**: UI framework implemented in `video_preview.py` with modern styling. Actual video streaming implementation is planned for future development.

2. **File Aggregation Utility**: Framework exists in `device_manager.py` with modern progress indicators. Actual file transfer implementation is currently simulated but ready for network protocol integration.

3. **Local Webcam Integration**: Planned feature for capturing video from PC webcams using OpenCV integration.

4. **Optional PC-GSR Sensor Recording**: Planned feature for direct GSR sensor connection to PC via USB/Bluetooth.

5. **Live GSR/PPG Plotting**: Planned feature using pyqtgraph for real-time physiological data visualization.

6. **Configuration Management UI**: Planned feature for device-specific settings and recording parameters.

#### Architecture

The app follows a modern modular architecture built on PySide6 with clear separation of concerns:

- **src/main/**: Application entry point (`main.py`) with PySide6 initialization and high-DPI support
- **src/ui/**: Modern GUI components with professional styling
  - `main_window.py`: Main application window with tabbed interface and modern controls
  - `device_panel.py`: Individual device management panels with real-time status
  - `status_dashboard.py`: System-wide status overview with color-coded indicators
  - `log_viewer.py`: Advanced log viewing with filtering and export capabilities
  - `video_preview.py`: Live video preview widgets with modern styling
  - `video_playback_window.py`: Video playback and annotation system
- **src/network/**: Device discovery and communication
  - `device_manager.py`: Multi-device connection management with Zeroconf discovery
  - `device.py`: Individual device communication and status monitoring
- **src/utils/**: Utility functions and session management
  - `session_manager.py`: Session lifecycle and metadata management
  - `logger.py`: Advanced logging with multiple output formats
- **src/integrations/**: External system integrations (LSL, PsychoPy, Shimmer)

## Building and Running

### Android App

1. Open the `android` directory in Android Studio
2. Build and run the app on a device or emulator

### PC Controller App (Cross-Platform)

The PC Controller App has been upgraded to PySide6 with a modern GUI interface.

#### Prerequisites

- Python 3.8 or higher
- PySide6 6.6.1 or higher

#### Installation

1. Navigate to the `windows_controller` directory:
   ```bash
   cd windows_controller
   ```

2. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

3. Run the application:
   ```bash
   python src/main/main.py
   ```

#### Testing the Modern GUI

To test the modernized interface:
```bash
python test_modern_gui.py
```

#### Key Dependencies

- **PySide6 6.6.1**: Official Qt binding for Python with modern GUI framework
- **pyqtgraph 0.13.7**: Real-time plotting (PySide6 compatible)
- **OpenCV 4.8.0**: Video processing and webcam integration
- **Zeroconf 0.69.0**: Device discovery on local network
- **Additional integrations**: LSL, PsychoPy, Shimmer sensor support

**Platform Support**: Windows, macOS, Linux with consistent modern interface

## Implementation Roadmap

The following roadmap outlines the next steps for completing the PC Controller App implementation:

### Phase 1: Core Networking Implementation

1. **Implement Actual Device Discovery**
   - Replace the simulated device discovery in `DeviceManager.py` with actual Zeroconf/mDNS discovery
   - Ensure proper handling of device appearance and disappearance on the network
   - Add support for manual IP address entry for devices not discovered automatically

2. **Implement Actual Device Connection**
   - Replace the simulated connection in `Device.py` with actual socket connection
   - Implement proper connection handshake and authentication if needed
   - Add error handling and reconnection logic

3. **Implement Command Protocol**
   - Define a clear command protocol for communication between PC and Android devices
   - Implement command serialization and deserialization
   - Replace simulated command sending with actual network communication

### Phase 2: Data Streaming and Collection

1. **Implement Live Video Streaming**
   - Add video frame receiving and decoding in `Device.py`
   - Update `VideoPreview.py` to display actual video frames
   - Implement efficient frame transport (possibly using RTSP or WebRTC)

2. **Implement Actual File Collection**
   - Replace simulated file collection with actual file transfer protocol
   - Add progress reporting for file transfers
   - Implement verification of transferred files

3. **Implement Status Updates**
   - Replace simulated status updates with actual device status polling
   - Add real-time status visualization in the dashboard

### Phase 3: Extended Features

1. **Implement Local Webcam Integration**
   - Add webcam capture using OpenCV
   - Integrate webcam feed into the recording session

2. **Implement PC-GSR Sensor Recording**
   - Add direct connection to GSR sensors from the PC
   - Integrate sensor data into the recording session

3. **Implement Live GSR/PPG Plotting**
   - Add real-time plotting of GSR and PPG data
   - Implement data visualization options

4. **Implement Configuration Management UI**
   - Add settings dialog for configuring app behavior
   - Implement device-specific configuration options

### Phase 4: Testing and Optimization

1. **Comprehensive Testing**
   - Test with multiple Android devices simultaneously
   - Test all recording scenarios and edge cases
   - Verify synchronization accuracy

2. **Performance Optimization**
   - Optimize video streaming for lower latency
   - Improve file transfer speeds
   - Reduce CPU and memory usage

3. **User Experience Improvements**
   - Refine UI based on user feedback
   - Add helpful tooltips and documentation
   - Improve error messages and recovery procedures

## Development

When making changes to shared components, ensure that both Android and Windows implementations are updated accordingly to maintain consistency across platforms.

## System Requirements

Below are detailed feature requirements for each component of the system. Each feature is marked as Core (must-have for basic functionality) or Extended (optional or nice-to-have), along with any interdependencies (hardware, other features, or cross-component integration).

##  1. Android Capture App

*   **RGB Video Capture (Core)** – Record high-definition color video using the phone’s built-in camera (e.g. 1080p at 30fps) ￼. Interdependencies: Requires Camera2 API support on the device; depends on preview UI and storage for saving video files.
*   **Thermal Video Capture (Core)** – Integrate with the Topdon TC001 thermal camera (via USB-C) to capture infrared video frames (~25–30 Hz) ￼. Interdependencies: Requires Topdon TC001 hardware and SDK support on Android; depends on USB host permission and thermal preview UI.
*   **GSR Sensor Data Capture (Core)** – Connect to a Shimmer3 GSR+ sensor over Bluetooth Low Energy to stream GSR readings at 128 Hz ￼. Interdependencies: Requires Shimmer GSR+ hardware and BLE pairing; depends on Bluetooth permissions and the sensor’s data protocol (Shimmer API).
*   **Audio Recording (Core)** – Capture synchronized audio via the phone’s microphone (e.g. 44.1 kHz stereo, saved to WAV) ￼. Interdependencies: Requires microphone access; synchronized start/stop should include audio track, and storage for audio file.
*   **Real-time Preview & Mode Toggle (Core)** – Provide live preview of the camera feed on the device screen, with the ability to switch between RGB and thermal views ￼ ￼. Interdependencies: Depends on active capture from both camera modules; requires UI toggle control and real-time rendering pipeline for both RGB and thermal streams.
*   **Unified Recording Control (Core)** – A simple one-touch interface to Start and Stop recordings for all data streams simultaneously ￼ ￼. Interdependencies: Tied to the Recording Controller logic that orchestrates all capture modules; requires that thermal, RGB, GSR, audio modules all respond to start/stop in sync (e.g. using a latch or barrier mechanism).
*   **Local Data Storage (Core)** – Save captured data locally on the phone with proper file management. Each modality should be recorded to a file (e.g. MP4 for videos, CSV for sensor data, WAV for audio) with a consistent naming scheme. Interdependencies: Requires storage permissions; depends on having a session identifier (from PC or user) to name files and enough device storage to buffer high-rate video data.
*   **Bluetooth Sensor Pairing & Status (Core)** – In-app management for pairing/connecting the Shimmer GSR sensor via BLE, with feedback on connection status. This includes auto-reconnection if link is lost. Interdependencies: Depends on Android BLE APIs and the Shimmer device; must be established before recording starts. Also interacts with the UI status indicators for sensor connectivity.
*   **Sensor Status and Feedback UI (Core)** – Visual indicators and readouts for each data source’s status (e.g. icons or text for camera active, thermal camera connected, GSR sensor connected) ￼. This includes displaying real-time values like current GSR level (μS) and possibly heart rate. Interdependencies: Requires data from each module (e.g. periodic GSR updates) and ties into the preview UI; depends on modules broadcasting status events.
*   **Timestamping & Synchronization Mechanism (Core)** – Use a unified clock on Android (monotonic clock) to timestamp all sensor readings and video frames for alignment ￼ ￼. All data events carry a common time reference (e.g. SystemClock.elapsedRealtimeNanos() on Android for sub-millisecond alignment ￼). Interdependencies: This is critical for multi-modal sync** – it underpins alignment between RGB frames, thermal frames, GSR samples, and audio. It also interdepends with the Networking layer to correlate with PC/other device clocks.
*   **Network Remote Control Client (Core)** – Ability for the app to be remote-controlled by the Windows PC controller. The Android app should listen for commands like Start/Stop from the PC and execute them, and send back status or acknowledgment. Interdependencies: Requires the Networking layer (WiFi or BT communication) to be active and a pairing/association with the PC. Depends on command protocol implementation (e.g. handling CMD_START, CMD_STOP commands from PC).
*   **Simulation Mode for Testing (Extended)** – A mode that simulates data sources when hardware is not available (e.g. generate fake thermal frames, dummy GSR signals) ￼. Interdependencies: Can be triggered when sensors are not connected or via a developer setting. Helps development of UI and networking without physical devices, but not needed in production use.
*   **PPG/Heart Rate Derivation (Extended)** – If the Shimmer GSR+ provides PPG data, compute real-time heart rate from the PPG signal and display it ￼ ￼. Interdependencies: Requires the GSR sensor’s PPG channel and additional processing; depends on GSR capture functioning. Not essential for basic recording (the raw PPG can be recorded regardless), but adds value for biofeedback.
*   **Additional Phone Sensors (Extended)** – Optionally capture other phone sensor data (e.g. accelerometer, gyroscope) to include with the session. Interdependencies: Requires accessing Android sensor APIs and adding new capture modules; would need to integrate with timestamping and storage just like other modalities.

## 2. PC Controller App (Cross-Platform)

*   **Multi-Device Connection Management (Core)** – A GUI to discover and connect to multiple Android capture devices (e.g. two phones) either via Bluetooth or Wi-Fi ￼. The user should be able to pair with each phone and see a list of connected devices. Interdependencies: Requires the Android apps to be running and advertising/awaiting connection; depends on the Networking layer’s device discovery and pairing protocol (Bluetooth scanning, Wi-Fi LAN or direct connections).
*   **Synchronized Start/Stop Control (Core)** – One central control to start and stop recording simultaneously on all connected devices (and on the PC itself if it’s capturing data). When the user hits “Start”, the PC sends a CMD_START to each device to initiate recording in unison ￼ ￼. Similarly, a stop command halts all recordings. Interdependencies: Relies on robust bi-directional communication with each Android (commands and acknowledgments). It also depends on each device’s ability to honor the command and use its local timestamp base such that all streams share a common start reference (see Networking/Sync features).
*   **Live Video Preview from Devices (Core)** – Display the live RGB and thermal video feeds from each connected Android device in the PC application’s UI ￼ ￼. This allows the operator to monitor framing, focus, and ensure data is coming in. Interdependencies: Requires that Android devices stream preview frames (likely at a lower frame rate or resolution for bandwidth) to the PC. Depends on the networking layer to transmit video data (could be via a separate stream or on request). Also depends on having a video rendering component in the PC GUI for each feed.
*   **Local Webcam Integration (Extended)** – Ability to capture video from a USB or built-in webcam on the PC as an additional video source. For example, the PC app could record a local RGB video (e.g. from a high-quality USB camera or built-in webcam) to include in the session. Interdependencies: Requires OpenCV or similar library on PC to interface with the webcam; needs to be synchronized with the overall session timeline (i.e. include PC’s own camera frames in the unified timestamp schema). This is optional if all cameras are on phones, but good for adding a PC viewpoint.
*   **Optional PC-GSR Sensor Recording (Extended)** – Support recording GSR/PPG data via a sensor directly connected to the PC (e.g. a Shimmer connected over USB/Bluetooth to the PC) ￼. This is useful if the PC is used as a standalone recorder or as a backup. Interdependencies: Requires serial/Bluetooth interface on PC for the sensor (and possibly using Shimmer’s API or an LSL stream). Would share similar data logging format as phone GSR data. If an Android device is already capturing GSR, this feature might be redundant, hence marked optional.
*   **Device Status Dashboard (Core)** – The GUI shows real-time status of each connected device: e.g. connection health, battery level (if obtainable), storage remaining, and whether each sensor stream is active. There should be indicators for each modality per device (camera on, thermal ready, GSR streaming) ￼. Interdependencies: Depends on periodic status updates from Android apps (the Android should send status events like “Thermal camera disconnected” or periodic heartbeats). Also requires UI elements for displaying these indicators, possibly in a multi-device overview panel.
*   **Data Logging and Monitoring (Core)** – The PC app logs important events and metadata during a session. This includes timestamps of when recording started/stopped for each device, any sync adjustments, dropped packets, etc., and possibly a live log view for the operator. Interdependencies: Relies on receiving acknowledgments and status from devices (network events) to log sync info. Also depends on a unified clock reference to log events in a comparable timeline.
*   **Session Manifest & Metadata Generation (Core)** – Upon completing a recording session, the PC compiles a manifest (session metadata) that describes the session and all data files. For example, generating a JSON or text file that lists all participant devices, sensor types, file names, start times, durations, and any calibration info ￼. Interdependencies: Requires consistent naming and timestamps from all devices. The PC needs to know or receive from each Android the file names and indices used (or instruct them ahead of time via session ID). This feature ties in with Shared features like unified naming conventions.
*   **File Aggregation Utility (Extended)** – A convenient feature to collect all data files from the Android devices to the PC after recording (e.g. via automatic file transfer). For instance, after stopping, the PC could instruct phones to upload their recorded files (videos, CSVs) to the PC over Wi-Fi or prompt the user to copy them. Interdependencies: Depends on network bandwidth and protocols (could use Wi-Fi for large video files). It also requires the phones to temporarily act as a server or client to send files. This is a nice-to-have for user convenience; if not implemented, the user would manually retrieve files from each device.
*   **Live GSR/PPG Plotting (Extended)** – Graphical real-time plot of physiological signals on the PC (display GSR readings and PPG-derived heart rate in real time during recording) ￼. Interdependencies: Requires continuous data streaming from the Android’s GSR sensor to the PC. Depends on a plotting library (PyQtGraph/matplotlib in the GUI) and may use LSL if integrated. This enhances monitoring but is not strictly required to record data.
*   **Configuration Management UI (Extended)** – A settings interface on PC to configure parameters like video resolution, frame rate, sample rates, output formats, etc., for the devices (if applicable). Interdependencies: Would require sending configuration commands to Android apps or applying settings on the PC capture components. Not essential for base functionality if defaults are used, but useful for flexibility.

## 3. Networking and Synchronization Layer

*   **Bi-Directional Communication Channel (Core)** – A robust networking layer that enables messages to flow between the PC controller and Android app(s) in both directions. This can be implemented over Wi-Fi (TCP/UDP sockets) or Bluetooth (RFCOMM or BLE GATT) as needed ￼. Interdependencies: Both PC and Android must implement the chosen transport protocol. If Wi-Fi is used, devices need to be on the same network or use Wi-Fi Direct; if Bluetooth, devices must be paired. This layer underpins all remote commands and status messaging in the system.
*   **Command Protocol (Core)** – Define a set of network commands and their formats for control and sync. Key commands include CMD_START (trigger recording start), CMD_STOP (stop recording), CMD_STATUS (query device status), and SYNC_PING (for time sync) among others. Each command will have an expected response or acknowledgment. Interdependencies: Requires a shared understanding (in a common library) on both PC and Android of these command types. The Android app’s Recording Controller should handle start/stop commands from PC ￼, and the PC needs to handle status responses or error codes.
*   **Time Synchronization & Clock Alignment (Core)** – Implement a mechanism to align timestamps between multiple devices and the PC. This could be done via a SYNC_PING message exchange: e.g. PC sends a timestamped ping, phone responds immediately, and round-trip time is used to estimate clock offset. Alternatively, integration with Lab Streaming Layer (LSL) can provide a common clock base ￼ ￼. Interdependencies: All devices must use a common epoch or reference once offset is determined. The Android uses its monotonic clock for internal timestamps ￼, and the PC may use its system clock or LSL-provided clock; the sync layer must reconcile these. This feature depends on stable network latency (the sync algorithm should account for jitter). Core requirement is to achieve a known maximum timestamp error (e.g. within 50 ms across devices). LSL integration is optional (extended) if a custom NTP/RTCP-like approach is used instead.
*   **Synchronization Events & Markers (Core)** – Support special network messages or data markers that allow verification of synchronization during recording. For example, broadcasting a sync marker event to all devices (or having devices log a received marker) to later check alignment ￼ ￼. Interdependencies: Depends on the command protocol and timestamp alignment above. Each device should log the receipt time of sync events in their own clock domain (which gets translated to unified timestamps). This feature helps in post-processing to ensure data alignment.
*   **Error Detection and Recovery (Core)** – The communication layer should detect lost connections or failed message delivery and attempt recovery. For example, if a CMD_START is sent but no ACK is received from a phone, the PC could retry or alert the user. If a phone disconnects mid-session, the PC should note the error and possibly attempt to reconnect or safely stop that device’s recording. Interdependencies: Requires a heartbeat or keep-alive mechanism (the Android could send periodic “I’m alive” messages). Also depends on the ability of the Recording Controller on each side to handle unexpected stops or resync. Robust error handling ensures the system is reliable beyond a single-device scenario.
*   **Multi-Device Scalability (Core)** – Support networking with multiple Android devices concurrently. The protocol should handle addressing each device (e.g. unique IDs or separate socket connections for each) so that commands can be sent to all or individual devices. Interdependencies: Depends on how connections are managed (e.g. one PC server socket handling multiple client connections from phones, or multiple BT links). Also requires that synchronization pings and start signals are orchestrated to minimize skew (e.g. sending start to 3 devices as close in time as possible). The system should be tested for at least 2 phones initially ￼, with an eye toward extending to more. This may also require a Master clock concept where one device (PC or a designated phone) acts as the time base.
*   **Transport Flexibility (Extended)** – Ability to use different transport methods based on scenario: e.g. Bluetooth for convenience vs Wi-Fi for high bandwidth. In extended use, the system might automatically choose the best available transport or even support a wired USB/Ethernet connection if available. Interdependencies: Requires abstracting the networking layer so that the command protocol is transport-agnostic. Both sides need implementations for each supported transport. One method (e.g. Wi-Fi TCP) will be the core default, and others can be added as optional for specific needs (Bluetooth as backup, etc.).
*   **Latency and Jitter Management (Extended)** – Enhance the sync protocol with jitter buffering or timestamp interpolation if needed. For example, if using live video preview streaming, implement buffering to smooth network jitter. Interdependencies: Relies on time sync being in place. This is more about quality improvement and may involve adjusting for network delays when aligning data, not strictly required for functional correctness in short recordings.
*   **Security and Pairing (Extended)** – If needed, implement authentication for device connections (to prevent unintended devices from connecting). This could involve pairing codes or verifying device IDs. Interdependencies: Relies on underlying transport security (Bluetooth pairing, Wi-Fi network security). Not fundamental to functionality in a controlled lab setting, but good for field use or if multiple systems are in proximity.

## 4. Shared and Cross-Cutting Features

*   **Monorepo Code Integration (Core)** – Maintain a unified codebase or repository for the entire system (Android app, Windows app, and common libraries). This ensures consistency in definitions for data formats, network protocols, and easier project management. Interdependencies: Common data models (e.g. a DataEvent class or message format) should be defined once and used by both Android and PC ￼. Changes to the protocol or format are synchronized across platforms. Build processes for each (Gradle for Android, Python/C++ for PC) reside in the same repository for coordinated versioning.
*   **Unified Naming Conventions (Core)** – All output files and data streams follow a consistent naming scheme. For example, using a session ID or timestamp in file names and folder structure so that data from multiple devices can be easily correlated ￼. Each device could have an identifier in the filename (e.g. Session123_DeviceA_rgb.mp4). Interdependencies: The PC likely generates or propagates the session ID to all devices at start. Android app needs to incorporate this ID into its file naming. The PC’s manifest generation depends on consistent naming to recognize files from each modality.
*   **Standardized Data Formats (Core)** – Define how each modality’s data is stored and formatted across the system. For instance, video files encoded in a common format (MP4 with H.264 codec), sensor data in CSV with timestamp columns, audio in WAV, and a JSON/YAML session metadata file ￼. Interdependencies: Both Android and PC must adhere to these formats. The Android might directly produce some of these files (video, raw sensor CSV), which the PC will then expect. A unified format ensures that when data is aggregated, all streams can be parsed together by analysis tools. This also ties into any export features on the PC side (which might convert or package data in different formats if needed).
*   **Consistent Timestamp Schema (Core)** – Establish a scheme for timestamps that allows data from different devices to be merged on a common timeline. For example, define that all recorded timestamps will be in Unix epoch milliseconds or a shared t=0 at session start. One approach is the PC sends a “start time” reference to all devices which they use as zero-time for their logs, or each device’s monotonic timestamps are offset to the PC’s clock during synchronization ￼ ￼. Interdependencies: Depends on the Networking sync mechanism to distribute or reconcile clocks. All data files (videos, sensor logs) should either be time-aligned or contain sufficient timestamp info to align post hoc. The session manifest can include the offset values used. This schema is fundamental for multi-device data alignment.
*   **Session ID and Metadata Propagation (Core)** – Generate a unique session identifier for each recording session and use it across all components. The PC could create this (e.g. “Session_2025-07-01_1300”) and instruct each Android to tag its data with this ID. Metadata like participant info or experimental conditions could also be included. Interdependencies: Tied to manifest and naming conventions. Android app may include the session ID in its UI or in file headers. The PC uses it for folder names and to collate files ￼. All devices need to agree on the active session context.
*   **Comprehensive Session Manifest (Core)** – As noted, the PC will produce a manifest (e.g. session_info.json) that compiles metadata: device IDs, start times, file paths, sensor types, firmware versions, etc. ￼. This is a cross-cutting feature since it involves contributions from all parts (Android provides some info like device model or sensor serial number). Interdependencies: Requires that the Android app sends device-specific info to the PC (either at connect time or start of recording) so it can be logged. Also relies on unified naming so the manifest can reference the correct filenames. The manifest is critical for users to understand and verify the dataset after a session.
*   **Multi-Device Data Alignment and Aggregation (Core)** – Ensure that data from different sources can be merged for analysis. In practice, this means after a session, one should be able to align the time-series from each phone and the PC with minimal post-processing. This is achieved via the shared timestamp schema and sync events. The system might also produce a combined timeline (e.g. a CSV of sync timestamps or an analysis report of sync quality) ￼. Interdependencies: This is the end-goal of the sync mechanism. It may involve cross-checking logs from each device to compute any residual offsets. If implemented, a sync analysis (Extended feature) could be generated (for example, calculating the drift or offset between device clocks and logging it) ￼.
*   **Cross-Platform Compatibility & Integration (Extended)** – While focusing on Android and Windows, design the system such that core components could be extended to other platforms (e.g. Linux support for the controller, or an iOS capture app). This involves abstracting platform-specific code. Interdependencies: Not immediate, but a forward-looking consideration. Monorepo setup and modular architecture facilitate adding new platforms with shared logic.
*   **Data Export and Conversion Tools (Extended)** – Provide tools to export or convert the session data into various formats for analysis (e.g. conversion of all data to a single archive or to formats like MATLAB or HDF5) ￼ ￼. Interdependencies: Depends on the data being well-structured and manifest available. Typically handled on the PC side after recording, this feature ensures researchers can easily consume the data. It’s optional for the capture functionality but enhances the usefulness of the system.
*   **Testing & Calibration Utilities (Extended)** – Include cross-cutting features for system validation, such as a calibration mode where all devices blink an LED or play a sound to mark a moment for sync verification. Interdependencies: Relies on coordination between PC and phones to generate a known simultaneous event. Useful for measuring true sync error (could integrate with sync markers feature). This is a nice-to-have for development and ensuring quality of synchronization.

Each of these components and features works in concert to deliver a cohesive multi-modal recording system. Core features represent the minimum viable product for synchronized dual-video + GSR capture, while extended features improve robustness, usability, and flexibility. Interdependencies highlight how the Android app, PC controller, and networking layer must be developed with a shared understanding of protocols, timing, and data formats to achieve reliable synchronization across all devices.

Sources: The above requirements are informed by the design and documentation of the GSR-RGBT capture system in the user’s repository, which details the multi-modal Android app capabilities ￼ ￼ and the PC controller features including multi-phone control and synchronization infrastructure ￼ 
