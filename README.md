# GSR Multimodal System - Unified Repository

A comprehensive multimodal data collection system for synchronized GSR (Galvanic Skin Response), dual-video (RGB +
thermal), and hand analysis using Android devices and PC coordination with Lab Streaming Layer (LSL) integration.

## Project Overview

This monorepo contains both Android and PC applications for a synchronized multimodal data collection system:

- **Android App** (`android-app/`): Kotlin-based mobile application for comprehensive data capture
- **PC Controller** (`pc-app/`): Python-based desktop application for device coordination and analysis

## Features

### Android Application

#### Core Recording Capabilities

- **RGB Video Recording**: High-quality video capture with CameraX (1080p at 30fps)
- **Raw Frame Capture**: Synchronized RGB frame extraction with timestamp markers
- **Thermal Camera Integration**: Topdon TC001 thermal camera support via USB-C
- **GSR Sensor Data**: Shimmer3 GSR+ sensor integration via Bluetooth (128 Hz sampling)

#### Advanced Features

- **Hand Analysis**: Post-recording hand detection and pose estimation using ML Kit
- **Visual Sync Markers**: Combined timestamp and visual flash synchronization
- **LSL Integration**: Lab Streaming Layer support for real-time data streaming
- **Performance Monitoring**: Comprehensive performance metrics and logging
- **Network Communication**: TCP/IP and LSL-based remote control

#### Data Management

- **Session Management**: Organized data storage with consistent naming conventions
- **Multi-modal Synchronization**: Unified timestamp schema across all data streams
- **Real-time Preview**: Live preview of RGB and thermal camera feeds
- **Status Monitoring**: Real-time sensor status and connection feedback

### PC Controller Application

#### Device Coordination

- **Multi-Device Management**: Discovery and connection to multiple Android devices
- **Synchronized Recording**: Coordinated start/stop across all devices via LSL
- **Live Monitoring**: Real-time preview of camera feeds and sensor data
- **Command Distribution**: LSL-based command distribution to Android devices

#### Data Processing

- **GSR Analysis**: Real-time GSR signal processing and analysis
- **Session Orchestration**: Centralized session management and metadata generation
- **Data Aggregation**: Collection and organization of multi-device recordings
- **LSL Stream Management**: Central LSL stream coordination and monitoring

#### Integration Capabilities

- **LSL Ecosystem**: Full integration with Lab Streaming Layer infrastructure
- **Shimmer Integration**: Direct Shimmer device connection via multiple protocols
- **Multi-device Support**: Support for multiple Android devices simultaneously
- **Cross-platform Compatibility**: Windows, Linux, and macOS support

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Lab Streaming Layer (LSL)                        │
│                        Real-time Data Streaming & Commands                 │
└─────────────────────────────────────────────────────────────────────────────┘
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │    │   Android App   │    │  PC Controller  │
│    (Device 1)   │    │    (Device 2)   │    │                 │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • CameraHandler │    │ • CameraHandler │    │ • LSL Manager   │
│ • ThermalHandler│    │ • ThermalHandler│    │ • Device Coord  │
│ • GsrHandler    │    │ • GsrHandler    │    │ • Session Mgmt  │
│ • HandAnalysis  │    │ • HandAnalysis  │    │ • Data Analysis │
│ • LSL Streams   │    │ • LSL Streams   │    │ • GUI Interface │
│ • NetworkHandler│    │ • NetworkHandler│    │ • File Transfer │
│ • Sync Markers  │    │ • Sync Markers  │    │ • Multi-device  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   WiFi Network  │
                    │ (TCP/IP + LSL)  │
                    └─────────────────┘

Hardware Integration:
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  Shimmer3 GSR+  │    │  Topdon TC001   │    │   Android       │
│   (Bluetooth)   │    │  Thermal Camera │    │   RGB Camera    │
│                 │    │    (USB-C)      │    │   + Microphone  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Quick Start

### Prerequisites

- **Hardware**:
    - 1-2 Android phones with USB-C port (API 26+)
    - Windows/Linux/macOS PC with Bluetooth and WiFi
    - Topdon TC001 thermal camera(s) with USB-C connection
    - Shimmer3 GSR+ sensor(s) with Bluetooth capability
- **Software**:
    - IntelliJ IDEA Ultimate Edition (recommended) or Android Studio
    - Android SDK (API 26+, target API 34)
    - Python 3.8+ with pip
    - Lab Streaming Layer (LSL) runtime
    - Git for version control

### Setup Instructions

#### 1. Repository Setup

```bash
git clone <repository-url>
cd fyp-gsr-unified-buccancs
```

#### 2. Android Application Setup

```bash
# Open in IntelliJ IDEA or Android Studio
# File → Open → Select project root folder
# IntelliJ will detect both Android and Python modules

# Sync Gradle project (this will download all dependencies including LSL)
./gradlew build

# Connect Android device and enable USB debugging
# Build and install app
./gradlew installDebug
```

#### 3. PC Controller Setup

```bash
cd pc-app

# Create virtual environment (recommended)
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies including LSL
pip install -r requirements.txt

# Run the PC controller
python main.py
```

#### 4. LSL Environment Setup

```bash
# Install LSL runtime (if not included in requirements)
# Windows: Download from https://github.com/sccn/liblsl/releases
# Linux: sudo apt-get install liblsl
# macOS: brew install labstreaminglayer/tap/lsl

# Verify LSL installation
python -c "import pylsl; print('LSL version:', pylsl.library_version())"
```

#### 5. Network Configuration

- Ensure all devices are on the same WiFi network
- Configure firewall to allow:
    - TCP communication on port 8080 (legacy networking)
    - LSL multicast traffic (UDP ports 16571-16604)
- For advanced setups, configure LSL network settings

#### 6. Hardware Integration

**GSR Sensor Setup:**

```bash
# Pair Shimmer3 GSR+ device with Android via Bluetooth
# Configure in Android app settings or use PC direct connection
# Supports multiple modes: Shimmer SDK, LSL streams, or simulation
```

**Thermal Camera Setup:**

```bash
# Connect Topdon TC001 to Android device via USB-C
# Grant USB permissions when prompted
# Verify connection in Android app status display
```

## Project Structure

```
fyp-gsr-unified-buccancs/
├── android-app/                 # Android application module
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/gsrunified/android/
│   │   │   │   ├── MainActivity.kt           # Main activity with LSL integration
│   │   │   │   ├── GsrHandler.kt            # Shimmer GSR sensor management
│   │   │   │   ├── ThermalCameraHandler.kt  # Topdon thermal camera integration
│   │   │   │   ├── NetworkHandler.kt        # TCP/IP communication
│   │   │   │   ├── HandAnalysisHandler.kt   # Post-recording hand analysis
│   │   │   │   ├── LslStreamManager.kt      # LSL stream management
│   │   │   │   ├── LslCommandInlet.kt       # LSL command processing
│   │   │   │   ├── camera/
│   │   │   │   │   └── CameraHandler.kt     # Enhanced camera with sync markers
│   │   │   │   └── core/
│   │   │   │       ├── EnhancedLogger.kt    # Comprehensive logging
│   │   │   │       └── PerformanceMonitor.kt # Performance metrics
│   │   │   │   ├── proto/                   # Protocol buffer definitions
│   │   │   │   │   └── messages.proto       # LSL message schemas
│   │   │   │   ├── res/layout/
│   │   │   │   │   └── activity_main.xml
│   │   │   │   └── AndroidManifest.xml
│   │   │   ├── test/                        # Unit tests
│   │   │   └── androidTest/                 # Integration tests
│   │   ├── build.gradle.kts                 # Enhanced build with LSL dependencies
│   │   └── libs/                            # Third-party SDK files
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── pc-app/                      # Python PC controller module
│   ├── main.py                  # Application entry point
│   ├── requirements.txt         # Python dependencies with LSL
│   └── app/
│       ├── gui/                 # PySide6 GUI components
│       │   ├── __init__.py
│       │   ├── main_window.py   # Main application window
│       │   ├── device_widget.py # Device status widgets
│       │   └── session_widget.py # Session management UI
│       ├── lsl/                 # LSL integration
│       │   ├── __init__.py
│       │   ├── stream_manager.py # LSL stream coordination
│       │   └── command_outlet.py # Command distribution
│       ├── network/             # Device communication
│       │   ├── __init__.py
│       │   └── device_manager.py # Multi-device coordination
│       ├── sensors/             # Sensor data processing
│       │   ├── __init__.py
│       │   └── gsr_handler.py   # GSR analysis and processing
│       ├── session/             # Session management
│       │   ├── __init__.py
│       │   └── session_manager.py # Recording session coordination
│       └── transfer/            # Data transfer utilities
│           ├── __init__.py
│           └── file_manager.py  # Multi-device file collection
├── third_party/                 # Third-party SDKs and libraries
│   ├── shimmer-sdk/             # Shimmer sensor SDK
│   └── topdon-sdk/              # Topdon thermal camera SDK
├── proto/                       # Shared protocol definitions
│   └── messages.proto           # Cross-platform message schemas
├── docs/                        # Comprehensive documentation
│   ├── README.md               # Hardware requirements & setup
│   ├── API.md                  # API documentation
│   └── TROUBLESHOOTING.md      # Common issues and solutions
├── README.md                   # This file
├── IMPLEMENTATION_SUMMARY.md   # Current implementation status
├── TESTING.md                  # Testing procedures and results
└── LICENSE
```

## Development Workflow

### Android Development

- Use Kotlin for all Android code
- Follow Android Architecture Components patterns
- Implement proper permission handling
- Test on physical devices with sensors

### Python Development

- Use PySide6 for GUI development
- Follow async patterns for network communication
- Implement proper error handling and logging
- Use type hints for better code documentation

### Integration Testing

- Test device discovery and connection
- Verify synchronized recording across devices
- Validate data integrity and timestamps
- Test error recovery scenarios

## Data Output

Each recording session generates comprehensive multimodal data:

### Core Recording Data

- **RGB Videos**: MP4 files with H.264 encoding (1080p at 30fps)
- **Raw RGB Frames**: Individual frame captures with precise timestamps for analysis
- **Thermal Data**: Processed thermal frames with temperature metadata and visual representations
- **GSR Data**: High-frequency sensor data (128 Hz) with conductance, resistance, and quality metrics

### Analysis and Synchronization

- **Hand Analysis Results**: JSON files with detected hand landmarks and pose data from ML Kit
- **Timestamp Markers**: Synchronization events and visual markers for multi-device alignment
- **Performance Metrics**: Detailed logging of system performance and data quality
- **Session Metadata**: Comprehensive JSON manifests with device info, timing, and configuration

### LSL Stream Data

- **Real-time GSR Streams**: Live conductance and resistance data via LSL
- **Thermal Stream Data**: Frame metadata and temperature statistics
- **Command Responses**: Device status and command acknowledgments
- **Synchronization Events**: Cross-device timing markers and sync verification

### File Organization

```
session_YYYYMMDD_HHMMSS/
├── device_1/
│   ├── rgb_video_TIMESTAMP.mp4          # Main RGB video recording
│   ├── rgb_frames/                      # Raw frame captures
│   │   ├── frame_NNNN_TIMESTAMP.jpg
│   │   └── ...
│   ├── thermal_data_TIMESTAMP.csv       # Thermal frame metadata
│   ├── thermal_frames/                  # Thermal image sequences
│   │   ├── thermal_NNNN_TIMESTAMP.png
│   │   └── ...
│   ├── gsr_data_TIMESTAMP.csv          # GSR sensor readings
│   ├── hand_analysis_TIMESTAMP.json    # Post-recording hand analysis
│   ├── sync_markers_TIMESTAMP.json     # Synchronization events
│   └── performance_log_TIMESTAMP.json  # System performance data
├── device_2/
│   └── [similar structure]
├── lsl_streams/                         # LSL stream recordings (optional)
│   ├── gsr_stream_TIMESTAMP.xdf
│   ├── thermal_stream_TIMESTAMP.xdf
│   └── command_stream_TIMESTAMP.xdf
└── session_manifest.json               # Complete session metadata
```

### Data Formats and Standards

- **Video**: MP4 container with H.264 video codec
- **Images**: JPEG for RGB frames, PNG for thermal visualizations
- **Sensor Data**: CSV with ISO 8601 timestamps and metric units
- **Metadata**: JSON with comprehensive device and session information
- **LSL Streams**: XDF format for Lab Streaming Layer data (when recorded)

## Contributing

1. Follow the existing code style and patterns
2. Add comprehensive tests for new features
3. Update documentation for any API changes
4. Ensure cross-platform compatibility

## License

[Add appropriate license information]

## Support

For technical support and documentation, see the `docs/` directory or contact the development team.
