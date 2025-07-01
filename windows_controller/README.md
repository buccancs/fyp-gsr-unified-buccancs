# Windows PC Controller App

This is the Windows PC Controller App for the GSR & Dual-Video Recording System. It allows you to control multiple Android capture devices simultaneously, view live video feeds, and manage recording sessions.

## Features

### Core Features

- **Multi-Device Connection Management**: Discover and connect to multiple Android capture devices (e.g. two phones) either via Bluetooth or Wi-Fi. The user can pair with each phone and see a list of connected devices.

- **Synchronized Start/Stop Control**: One central control to start and stop recording simultaneously on all connected devices (and on the PC itself if it's capturing data). When the user hits "Start", the PC sends a CMD_START to each device to initiate recording in unison. Similarly, a stop command halts all recordings.

- **Live Video Preview from Devices**: Display the live RGB and thermal video feeds from each connected Android device in the PC application's UI. This allows the operator to monitor framing, focus, and ensure data is coming in.

- **Device Status Dashboard**: The GUI shows real-time status of each connected device: e.g. connection health, battery level (if obtainable), storage remaining, and whether each sensor stream is active. There are indicators for each modality per device (camera on, thermal ready, GSR streaming).

- **Data Logging and Monitoring**: The PC app logs important events and metadata during a session. This includes timestamps of when recording started/stopped for each device, any sync adjustments, dropped packets, etc., and a live log view for the operator.

- **Session Manifest & Metadata Generation**: Upon completing a recording session, the PC compiles a manifest (session metadata) that describes the session and all data files. For example, generating a JSON file that lists all participant devices, sensor types, file names, start times, durations, and any calibration info.

### Extended Features (Planned)

- **Local Webcam Integration**: Ability to capture video from a USB or built-in webcam on the PC as an additional video source.

- **Optional PC-GSR Sensor Recording**: Support recording GSR/PPG data via a sensor directly connected to the PC (e.g. a Shimmer connected over USB/Bluetooth to the PC).

- **File Aggregation Utility**: A convenient feature to collect all data files from the Android devices to the PC after recording (e.g. via automatic file transfer).

- **Live GSR/PPG Plotting**: Graphical real-time plot of physiological signals on the PC (display GSR readings and PPG-derived heart rate in real time during recording).

- **Configuration Management UI**: A settings interface on PC to configure parameters like video resolution, frame rate, sample rates, output formats, etc., for the devices (if applicable).

## Architecture

The Windows PC Controller App is built using Python and PyQt5 for the GUI. It follows a modular architecture with the following components:

- **Main**: The main entry point for the application.
- **UI**: The user interface components, including the main window, device panels, video previews, status dashboard, and log viewer.
- **Network**: The networking components for device discovery, connection, and communication.
- **Utils**: Utility functions and classes, including logging and session management.

## Project Structure

```
windows_controller/
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

- Python 3.8 or higher
- PyQt5
- Other dependencies listed in `requirements.txt`

## Installation

1. Clone the repository:
   ```
   git clone https://github.com/your-username/fyp-gsr-unified-buccancs.git
   ```

2. Navigate to the windows_controller directory:
   ```
   cd fyp-gsr-unified-buccancs/windows_controller
   ```

3. Install the required dependencies:
   ```
   pip install -r requirements.txt
   ```

## Usage

1. Start the application:
   ```
   python src/main/main.py
   ```

2. Use the "Devices" menu to discover and connect to Android capture devices.

3. Once devices are connected, use the "Start Recording" button to start recording on all devices simultaneously.

4. Use the "Stop Recording" button to stop recording on all devices.

5. After stopping, you can generate a session manifest and collect files from the devices.

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