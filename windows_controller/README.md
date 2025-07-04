# PC Controller App (Cross-Platform) - Modern PySide6 Interface

This is the PC Controller App for the GSR & Dual-Video Recording System. It features a modern PySide6-based GUI that allows you to control multiple Android capture devices simultaneously, view live video feeds, and manage recording sessions with professional styling and enhanced usability.

**Cross-Platform Support**: Windows, macOS, and Linux with consistent modern interface
**GUI Framework**: PySide6 (Official Qt binding for Python)
**Interface**: Modern flat design with professional color scheme and enhanced user experience

## Features

### Core Features

- **Multi-Device Connection Management**: Discover and connect to multiple Android capture devices (e.g. two phones) either via Bluetooth or Wi-Fi. The user can pair with each phone and see a list of connected devices.

- **Synchronized Start/Stop Control**: One central control to start and stop recording simultaneously on all connected devices (and on the PC itself if it's capturing data). When the user hits "Start", the PC sends a CMD_START to each device to initiate recording in unison. Similarly, a stop command halts all recordings.

- **Live Video Preview from Devices**: Display the live RGB and thermal video feeds from each connected Android device in the PC application's UI. This allows the operator to monitor framing, focus, and ensure data is coming in.

- **Device Status Dashboard**: The GUI shows real-time status of each connected device: e.g. connection health, battery level (if obtainable), storage remaining, and whether each sensor stream is active. There are indicators for each modality per device (camera on, thermal ready, GSR streaming).

- **Data Logging and Monitoring**: The PC app logs important events and metadata during a session. This includes timestamps of when recording started/stopped for each device, any sync adjustments, dropped packets, etc., and a live log view for the operator.

- **Session Manifest & Metadata Generation**: Upon completing a recording session, the PC compiles a manifest (session metadata) that describes the session and all data files. For example, generating a JSON file that lists all participant devices, sensor types, file names, start times, durations, and any calibration info. Now includes support for the new raw RGB image capture functionality from Android devices.

### Extended Features (Planned)

- **Local Webcam Integration**: Ability to capture video from a USB or built-in webcam on the PC as an additional video source.

- **Optional PC-GSR Sensor Recording**: Support recording GSR/PPG data via a sensor directly connected to the PC (e.g. a Shimmer connected over USB/Bluetooth to the PC).

- **File Aggregation Utility**: A convenient feature to collect all data files from the Android devices to the PC after recording (e.g. via automatic file transfer).

- **Live GSR/PPG Plotting**: Graphical real-time plot of physiological signals on the PC (display GSR readings and PPG-derived heart rate in real time during recording).

- **Configuration Management UI**: A settings interface on PC to configure parameters like video resolution, frame rate, sample rates, output formats, etc., for the devices (if applicable).

## Architecture

The PC Controller App is built using Python and **PySide6** for the modern GUI framework, providing cross-platform compatibility across Windows, macOS, and Linux. The application has been completely upgraded from PyQt5 to PySide6 with a professional modern interface.

### Modern GUI Features

- **Professional Styling**: Blue and gray color scheme with rounded corners and flat design
- **Enhanced Buttons**: Larger buttons with emojis (🔴 Start, ⏹️ Stop) and hover effects
- **Improved Typography**: Bold fonts and consistent text colors throughout
- **Responsive Design**: Better scaling on different screen sizes (1400x900 minimum)
- **Color-Coded Indicators**: Real-time status visualization with intuitive color coding

### Modular Architecture

- **Main**: Application entry point with PySide6 initialization and high-DPI support
- **UI**: Modern user interface components with professional styling:
  - Main window with tabbed interface and modern controls
  - Device panels with real-time status indicators
  - Video previews with modern styling
  - Status dashboard with color-coded indicators
  - Advanced log viewer with filtering capabilities
  - Video playback and annotation system
- **Network**: Device discovery and communication with Zeroconf/mDNS support
- **Utils**: Utility functions including session management and advanced logging
- **Integrations**: External system integrations (LSL, PsychoPy, Shimmer)

## Project Structure

```
pc_controller/
├── README.md
├── requirements.txt
├── sessions/
├── logs/
└── src/
    ├── main/
    │   ├── __init__.py
    │   └── main.py
    ├── ui/
    │   ├── __init__.py
    │   ├── main_window.py
    │   ├── device_panel.py
    │   ├── video_preview.py
    │   ├── status_dashboard.py
    │   └── log_viewer.py
    ├── network/
    │   ├── __init__.py
    │   ├── device_manager.py
    │   └── device.py
    └── utils/
        ├── __init__.py
        ├── logger.py
        └── session_manager.py
```

## Requirements

### Prerequisites

- **Python 3.8 or higher**
- **PySide6 6.6.1 or higher** (Official Qt binding for Python)

### Key Dependencies

- **PySide6 6.6.1**: Modern GUI framework with official Qt support
- **pyqtgraph 0.13.7**: Real-time plotting (PySide6 compatible)
- **OpenCV 4.8.0**: Video processing and webcam integration
- **Zeroconf 0.69.0**: Device discovery on local network
- **websockets 11.0.3**: Network communication
- **pylsl 1.16.2**: Lab Streaming Layer integration
- **psychopy 2023.2.3**: Psychological experiments integration
- **Additional dependencies**: Listed in `requirements.txt`

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/fyp-gsr-unified-buccancs.git
   ```

2. Navigate to the windows_controller directory:
   ```bash
   cd fyp-gsr-unified-buccancs/windows_controller
   ```

3. Install the required dependencies:
   ```bash
   pip install -r requirements.txt
   ```

   This will install PySide6 and all other required dependencies for the modern GUI interface.

## Usage

### Starting the Application

1. Start the modern PySide6 application:
   ```bash
   python src/main/main.py
   ```

   The application will launch with the modern interface featuring:
   - Professional blue and gray color scheme
   - Enhanced buttons with emojis and hover effects
   - Responsive design optimized for 1400x900 resolution
   - Tabbed interface for better organization

### Testing the Modern GUI

To test the modernized interface without connecting devices:
```bash
python test_modern_gui.py
```

### Basic Workflow

1. **Device Discovery**: Use the "Devices" menu to discover and connect to Android capture devices
2. **Device Management**: View connected devices in the modern device panels with real-time status indicators
3. **Recording Control**: Use the enhanced recording buttons:
   - 🔴 **Start Recording**: Begin recording on all connected devices simultaneously
   - ⏹️ **Stop Recording**: Stop recording on all devices
4. **Status Monitoring**: Monitor device status in the color-coded dashboard
5. **Session Management**: Generate session manifests and collect files from devices
6. **Video Playback**: Use the integrated video playback and annotation system for analysis

## Development

### Adding a New Feature

1. Identify the appropriate module for your feature.
2. Implement the feature in the module.
3. Update the UI to expose the feature to the user.
4. Add tests for the feature.
5. Update the documentation.

### Running Tests

```
python -m unittest discover -s tests
```

## License

This project is licensed under the MIT License - see the LICENSE file for details.
