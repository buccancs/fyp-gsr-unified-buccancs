# Synchronised GSR & Dual-Video Recording System

This repository contains the unified codebase for the GSR & Dual-Video Recording System, including the Android app, Windows app, and shared components.

## Repository Structure

```
.
├── android/              # Android application code
│   ├── app/              # Android app module
│   └── build.gradle      # Android build configuration
├── windows/              # Windows application code
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

2. **Thermal Video Capture**: Implemented in `ThermalCameraManager.kt`. Interfaces with the Topdon TC001 thermal camera via USB-C to capture infrared video frames.

3. **GSR Sensor Data Capture**: Implemented in `GsrSensorManager.kt`. Connects to a Shimmer3 GSR+ sensor over Bluetooth Low Energy to stream GSR readings at 128 Hz.

4. **Audio Recording**: Implemented in `AudioRecorder.kt`. Captures synchronized audio via the phone's microphone at 44.1 kHz stereo, saved to WAV format.

5. **Real-time Preview & Mode Toggle**: Implemented in `MainActivity.kt`. Provides live preview of both RGB and thermal camera feeds with the ability to switch between views.

6. **Unified Recording Control**: Implemented in `RecordingController.kt`. Provides a simple one-touch interface to start and stop recordings for all data streams simultaneously.

7. **Local Data Storage**: Implemented across all manager classes. Saves captured data locally with a consistent naming scheme based on session ID and timestamp.

8. **Bluetooth Sensor Pairing & Status**: Implemented in `GsrSensorManager.kt`. Manages pairing and connection to the Shimmer GSR sensor with status feedback.

9. **Sensor Status and Feedback UI**: Implemented in `MainActivity.kt`. Displays visual indicators for each data source's status and real-time values.

10. **Timestamping & Synchronization**: Implemented in `TimeManager.kt`. Uses a unified clock (SystemClock.elapsedRealtimeNanos) to timestamp all sensor readings and video frames.

11. **Network Remote Control Client**: Implemented in `NetworkClient.kt`. Allows the app to be remote-controlled by the Windows PC controller.

#### Extended Features

1. **PPG/Heart Rate Derivation**: Implemented in `GsrSensorManager.kt`. Computes real-time heart rate from the PPG signal provided by the Shimmer GSR+ sensor.

#### Architecture

The app follows a modular architecture with clear separation of concerns:

- **MainActivity**: Main UI and entry point
- **RecordingController**: Coordinates all recording components
- **Camera Package**: Handles RGB and thermal video capture
- **Sensor Package**: Manages GSR sensor connection and data
- **Audio Package**: Handles audio recording
- **Network Package**: Manages communication with the PC controller
- **Utils Package**: Provides utility classes like TimeManager

## Building and Running

### Android App

1. Open the `android` directory in Android Studio
2. Build and run the app on a device or emulator

### Windows App

1. Install Python 3.8 or higher
2. Navigate to the `windows` directory
3. Install dependencies: `pip install -e .`
4. Run the app: `python -m app.main`

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

## 2. Windows PC Controller App

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
