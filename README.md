# GSR Multimodal System - Unified Repository

A comprehensive multimodal data collection system for synchronized GSR (Galvanic Skin Response), dual-video (RGB + thermal), and audio recording using Android devices and PC coordination.

## Project Overview

This monorepo contains both Android and PC applications for a synchronized multimodal data collection system:

- **Android App** (`android-app/`): Kotlin-based mobile application for data capture
- **PC Controller** (`pc-app/`): Python-based desktop application for device coordination

## Features

### Android Application
- **RGB Video Recording**: High-quality video capture with CameraX
- **Thermal Camera Integration**: Topdon TC001 thermal camera support via USB-C
- **GSR Sensor Data**: Shimmer3 GSR+ sensor integration via Bluetooth
- **Audio Recording**: Synchronized ambient audio capture
- **Real-time Preview**: Live preview of all data streams
- **Remote Control**: PC-controlled start/stop recording
- **Data Synchronization**: Timestamp-based multi-modal sync

### PC Controller Application
- **Device Management**: Discovery and connection to multiple Android devices
- **Synchronized Recording**: Coordinated start/stop across all devices
- **Live Monitoring**: Real-time preview of camera feeds and GSR data
- **Data Analysis**: Real-time GSR signal processing and analysis
- **Session Management**: Organized data storage and session tracking
- **Multi-device Support**: Support for up to 2 Android devices simultaneously

## System Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │    │   Android App   │    │  PC Controller  │
│    (Device 1)   │    │    (Device 2)   │    │                 │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • RGB Camera    │    │ • RGB Camera    │    │ • Device Mgmt   │
│ • Thermal Cam   │    │ • Thermal Cam   │    │ • Live Monitor  │
│ • GSR Sensor    │    │ • GSR Sensor    │    │ • Data Analysis │
│ • Audio Rec     │    │ • Audio Rec     │    │ • Sync Control  │
│ • WiFi/BT Comm  │    │ • WiFi/BT Comm  │    │ • Session Mgmt  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │   WiFi Network  │
                    │  (TCP/IP Comm)  │
                    └─────────────────┘
```

## Quick Start

### Prerequisites
- **Hardware**: 
  - 1-2 Android phones with USB-C port
  - Windows PC with Bluetooth and WiFi
  - Topdon TC001 thermal camera(s)
  - Shimmer3 GSR+ sensor(s)
- **Software**:
  - IntelliJ IDEA Ultimate Edition
  - Android SDK (API 26+)
  - Python 3.8+

### Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd fyp-gsr-unified-buccancs
   ```

2. **Open in IntelliJ IDEA**:
   - File → Open → Select project root folder
   - IntelliJ will detect both Android and Python modules

3. **Configure Android Module**:
   - Sync Gradle project
   - Connect Android device
   - Build and install app

4. **Configure Python Module**:
   ```bash
   cd pc-app
   pip install -r requirements.txt
   python main.py
   ```

5. **Network Setup**:
   - Ensure all devices are on the same WiFi network
   - Configure firewall to allow communication on port 8080

## Project Structure

```
fyp-gsr-unified-buccancs/
├── android-app/                 # Android application module
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/gsrmultimodal/android/
│   │   │   │   └── MainActivity.kt
│   │   │   ├── res/layout/
│   │   │   │   └── activity_main.xml
│   │   │   └── AndroidManifest.xml
│   │   ├── build.gradle.kts
│   │   └── libs/                # Topdon SDK files
│   ├── build.gradle.kts
│   └── settings.gradle.kts
├── pc-app/                      # Python PC controller module
│   ├── main.py                  # Application entry point
│   ├── requirements.txt         # Python dependencies
│   └── app/
│       ├── gui/                 # GUI components
│       │   ├── __init__.py
│       │   └── main_window.py
│       ├── network/             # Device communication
│       │   ├── __init__.py
│       │   └── device_manager.py
│       └── sensors/             # Sensor data processing
│           ├── __init__.py
│           └── gsr_handler.py
├── docs/                        # Documentation
│   └── README.md               # Hardware requirements & architecture
├── README.md                   # This file
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

Each recording session generates:
- **RGB Videos**: MP4 files with embedded audio
- **Thermal Data**: Image sequences or encoded video
- **GSR Data**: CSV files with timestamps and values
- **Session Metadata**: JSON files with configuration and timing

## Contributing

1. Follow the existing code style and patterns
2. Add comprehensive tests for new features
3. Update documentation for any API changes
4. Ensure cross-platform compatibility

## License

[Add appropriate license information]

## Support

For technical support and documentation, see the `docs/` directory or contact the development team.
