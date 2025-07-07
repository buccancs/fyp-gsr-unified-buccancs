# PC Controller App (Cross-Platform) - Hybrid Orchestrator + Participant

This is the PC Controller App for the GSR & Dual-Video Recording System. It features a modern PySide6-based GUI that operates as both an orchestrator for remote Android devices AND a first-class data acquisition participant using local PC hardware (Shimmer GSR sensor and Logitech Brio 4K webcam).

**Cross-Platform Support**: Windows, macOS, and Linux with consistent modern interface
**GUI Framework**: PySide6 (Official Qt binding for Python)
**Interface**: Modern flat design with professional color scheme and enhanced user experience
**Architecture**: Hybrid Orchestrator + Participant model with local hardware integration

## Features

### Core Features

- **Hybrid Device Management**: Manage both remote Android devices AND local PC hardware as unified data acquisition nodes. The PC appears as "Local-PC" device alongside remote devices, with the same interface and controls.

- **Local Hardware Integration**: Direct integration with PC-connected hardware:
  - **Shimmer GSR Sensor**: Bluetooth/COM port connection for GSR and PPG data capture
  - **Logitech Brio 4K Webcam**: USB connection for high-resolution video recording (3840x2160)
  - **Hardware Abstraction Layer**: Clean, extensible architecture for adding new PC sensors

- **Dynamic Sensor Selection**: Per-device sensor selection UI allowing users to enable/disable specific sensors for each recording session:
  - Local PC: GSR/PPG, Brio 4K
  - Android devices: GSR/PPG, Thermal Camera
  - Real-time sensor status monitoring and control

- **Unified Recording Control**: Single interface to start/stop recording on ALL devices (local and remote) simultaneously with sensor-specific control. The system intelligently handles local file creation and remote device coordination.

- **Multi-Device Connection Management**: Discover and connect to multiple Android capture devices via Bluetooth or Wi-Fi, while automatically managing local PC hardware as a first-class participant.

- **Live Video Preview from All Sources**: Display live feeds from both remote Android devices (RGB/thermal) and local PC webcam with unified preview interface.

- **Comprehensive Status Dashboard**: Real-time monitoring of all devices (local and remote) including connection health, recording status, and sensor-specific indicators with hardware-level status reporting.

- **Intelligent File Management**: Automatic local file creation for PC hardware with smart collection from remote devices. Local files remain on PC while remote files are transferred as needed.

- **Session Manifest & Metadata Generation**: Enhanced manifest generation including local PC hardware metadata, sensor selections, and unified session information across all participating devices.

### Extended Features

- **Live GSR/PPG Plotting**: Graphical real-time plot of physiological signals from both local PC sensors and remote devices (display GSR readings and PPG-derived heart rate in real time during recording).

- **Advanced Configuration Management**: Settings interface to configure parameters like video resolution, frame rate, sample rates, output formats, and hardware-specific settings for both local and remote devices.

- **Multi-Camera Synchronization**: Advanced synchronization features for coordinating multiple video sources (local Brio + remote Android cameras) with precise timestamp alignment.

- **Export and Analysis Tools**: Built-in tools for data export, format conversion, and basic analysis of collected physiological and video data.

## PC Hardware Setup

### Shimmer GSR Sensor Setup

1. **Pair the Shimmer Device**:
   - Enable Bluetooth on your PC
   - Put the Shimmer device in pairing mode
   - Go to Windows Bluetooth Settings → More Bluetooth options → COM Ports
   - Note the COM port assigned to your Shimmer device (e.g., COM5)

2. **Configure the Application**:
   - Copy `config.ini.template` to `config.ini`
   - Update the `shimmer_com_port` setting with your COM port:
     ```ini
     [Hardware]
     shimmer_com_port = COM5
     ```

### Logitech Brio 4K Webcam Setup

1. **Connect the Camera**:
   - Connect your Logitech Brio via USB
   - Ensure it's recognized by Windows (check Device Manager)
   - Note the camera index (usually 0 for the first camera)

2. **Configure the Application**:
   - Update the `brio_camera_index` setting in `config.ini`:
     ```ini
     [Hardware]
     brio_camera_index = 0
     ```

3. **Verify 4K Support**:
   - The application automatically configures the Brio for 4K recording (3840x2160)
   - Ensure your system has sufficient USB bandwidth and processing power for 4K capture

### Hardware Requirements

- **Shimmer GSR Sensor**: Bluetooth-enabled Shimmer device with GSR and PPG capabilities
- **Logitech Brio 4K Webcam**: USB 3.0 connection recommended for 4K recording
- **System Requirements**: 
  - USB 3.0 ports for high-bandwidth video capture
  - Bluetooth adapter for Shimmer connectivity
  - Sufficient processing power for real-time 4K video processing

## Architecture

The PC Controller App implements a **Hybrid Orchestrator + Participant** architecture using Python and **PySide6**. The application serves as both a central orchestrator for remote Android devices AND a first-class data acquisition participant using local PC hardware.

### Hybrid Architecture Design

- **Unified Device Interface**: Both local PC hardware and remote Android devices implement the same interface, allowing seamless management through a single API
- **LocalDevice Abstraction**: PC hardware is encapsulated as a "LocalDevice" that appears identical to remote devices in the UI and control logic
- **Hardware Abstraction Layer**: Clean separation between hardware drivers and application logic, enabling easy extension for new sensors
- **Dynamic Sensor Management**: Per-device sensor selection with real-time status monitoring across all participants

### Modern GUI Features

- **Professional Styling**: Blue and gray color scheme with rounded corners and flat design
- **Enhanced Buttons**: Larger buttons with emojis (🔴 Start, ⏹️ Stop) and hover effects
- **Dynamic Sensor Selection UI**: Checkboxes for each device's available sensors with Select All/Deselect All controls
- **Unified Device Panels**: Consistent interface for both local and remote devices with hardware-specific status indicators
- **Responsive Design**: Better scaling on different screen sizes (1400x900 minimum)
- **Color-Coded Indicators**: Real-time status visualization with intuitive color coding

### Modular Architecture

- **Main**: Application entry point with PySide6 initialization and high-DPI support
- **UI**: Modern user interface components with professional styling:
  - Main window with sensor selection and unified device management
  - Device panels with dynamic sensor checkboxes and real-time status
  - Video previews supporting both local and remote sources
  - Status dashboard with hardware-level monitoring
  - Advanced log viewer with filtering capabilities
- **Hardware**: PC hardware abstraction layer:
  - Abstract base classes for sensors and cameras (`pc_sensor.py`)
  - Shimmer GSR driver with threaded data acquisition (`shimmer_pc.py`)
  - Logitech Brio 4K webcam driver with threaded capture (`webcam_pc.py`)
- **LocalDevice**: Unified local hardware management (`local_device.py`)
- **Network**: Enhanced device management with local/remote device support
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
