# App Usage Guide - GSR & Dual-Video Recording System

This guide provides step-by-step instructions for using the GSR & Dual-Video Recording System for data collection. It covers both the Android capture app and PC controller interface from a user perspective.

## 📋 Table of Contents

1. [Quick Start](#quick-start)
2. [Android App Usage](#android-app-usage)
3. [PC Controller Usage](#pc-controller-usage)
4. [Recording Workflows](#recording-workflows)
5. [Data Management](#data-management)
6. [Advanced Features](#advanced-features)
7. [Tips and Best Practices](#tips-and-best-practices)
8. [Common Issues](#common-issues)

## 🚀 Quick Start

### Prerequisites
- Android device with GSR & Dual-Video app installed
- PC with controller software running
- Shimmer3 GSR+ sensor (charged and ready)
- Topdon TC001 thermal camera
- Both devices connected to same WiFi network

### 5-Minute Setup
1. **Power on GSR sensor** (hold power button for 3 seconds)
2. **Connect thermal camera** to Android device via USB-C
3. **Launch Android app** and wait for sensors to connect
4. **Start PC controller** and discover Android device
5. **Connect devices** and verify all status indicators are green
6. **Start recording** from either PC or Android app

## 📱 Android App Usage

### App Interface Overview

The Android app features a clean, intuitive interface with real-time status indicators:

```
┌─────────────────────────────────────┐
│  GSR & Dual-Video Capture          │
├─────────────────────────────────────┤
│  📹 Camera Preview                  │
│  [RGB] [Thermal] Toggle             │
│                                     │
│  📊 Real-time Data:                 │
│  GSR: 2.45 μS                      │
│  Heart Rate: 72 BPM                 │
│                                     │
│  🔴 [RECORD] Button                 │
│                                     │
│  Status Indicators:                 │
│  🟢 GSR Sensor: Connected           │
│  🟢 Thermal Camera: Ready           │
│  🟢 Network: Connected              │
│  🟢 Storage: 15.2 GB Available     │
└─────────────────────────────────────┘
```

### Starting the App

1. **Launch the App**:
   - Tap the GSR Capture app icon
   - Wait for the app to initialize (2-3 seconds)
   - Grant permissions when prompted

2. **Check Status Indicators**:
   - **Green**: Component ready and functioning
   - **Yellow**: Component connecting or warning
   - **Red**: Component error or disconnected

### Connecting Sensors

#### GSR Sensor Connection

1. **Automatic Detection**:
   - App automatically scans for Shimmer3 GSR+ sensors
   - Previously paired sensors connect automatically
   - Connection status appears in real-time

2. **Manual Connection** (if needed):
   - Tap "Connect GSR Sensor" button
   - Select "Shimmer3 GSR+" from Bluetooth device list
   - Wait for "Connected" status (usually 5-10 seconds)

3. **Verify Connection**:
   - Real-time GSR values should appear (0.1-20+ μS range)
   - Heart rate should be calculated and displayed
   - Status indicator should be green

#### Thermal Camera Connection

1. **Physical Connection**:
   - Connect Topdon TC001 to Android device via USB-C
   - Camera powers on automatically when connected
   - Android shows "USB device connected" notification

2. **Grant Permissions**:
   - Tap "Allow" when USB permission dialog appears
   - App automatically detects and initializes camera
   - Thermal preview becomes available

3. **Verify Connection**:
   - Thermal camera status indicator turns green
   - Live thermal preview available in camera view
   - Temperature readings visible in thermal mode

### Camera Preview and Controls

#### Switching Camera Modes

1. **RGB Mode**:
   - Shows standard color camera feed
   - 1080p resolution at 30fps
   - Auto-focus and exposure control
   - Tap [RGB] button to activate

2. **Thermal Mode**:
   - Shows infrared thermal imaging
   - Temperature mapping with color scale
   - Real-time thermal data overlay
   - Tap [Thermal] button to activate

#### Camera Controls

- **Focus**: Tap on preview to focus on specific area
- **Exposure**: Automatic exposure adjustment
- **Zoom**: Pinch to zoom in/out (RGB mode only)
- **Orientation**: App handles rotation automatically

### Recording Process

#### Pre-Recording Checklist

Before starting recording, verify:
- ✅ GSR sensor connected (green status)
- ✅ Thermal camera connected (green status)
- ✅ Network connected to PC (if using remote control)
- ✅ Sufficient storage space (>1GB recommended)
- ✅ Battery level adequate (>20% recommended)

#### Starting Recording

1. **Local Recording** (from Android app):
   ```
   1. Tap the red [RECORD] button
   2. Button changes to show recording status
   3. All data streams start simultaneously:
      • RGB video recording
      • Raw RGB frame capture
      • Thermal frame capture
      • GSR data streaming
      • Audio recording
   4. Recording timer appears
   ```

2. **Remote Recording** (from PC controller):
   ```
   1. PC controller sends start command
   2. App receives command and starts recording
   3. Confirmation message appears
   4. All modalities begin recording
   ```

#### During Recording

**Monitor Status**:
- Recording timer shows elapsed time
- Real-time GSR values continue updating
- Storage remaining decreases gradually
- All status indicators remain green

**What's Being Recorded**:
- **RGB Video**: 1080p MP4 file
- **Raw RGB Images**: Individual JPEG frames with nanosecond timestamps
- **Thermal Frames**: Individual thermal images with timestamps
- **GSR Data**: CSV file with 128Hz sampling rate
- **Audio**: 44.1kHz stereo WAV file

**Quality Indicators**:
- Stable GSR readings (not flatlined)
- Clear camera preview (not blurry)
- No error messages or warnings
- Consistent frame rates

#### Stopping Recording

1. **Local Stop**:
   - Tap the [STOP] button
   - Wait for "Saving files..." message
   - All streams stop simultaneously
   - Files saved to device storage

2. **Remote Stop**:
   - PC controller sends stop command
   - App stops recording and saves files
   - Confirmation sent back to PC

### Data Output and Storage

#### File Organization

Recorded data is organized in session folders:

```
/Android/data/com.buccancs.gsrcapture/files/Movies/GSRCapture/
└── session_YYYYMMDD_HHMMSS_xxxxxxxx/
    ├── raw_rgb_session_YYYYMMDD_HHMMSS_xxxxxxxx/
    │   ├── frame_1234567890123456789.jpg
    │   ├── frame_1234567890123456790.jpg
    │   └── ...
    ├── thermal_session_YYYYMMDD_HHMMSS_xxxxxxxx/
    │   ├── frame_1234567890123456789.jpg
    │   ├── frame_1234567890123456790.jpg
    │   └── ...
    ├── RGB_session_YYYYMMDD_HHMMSS_xxxxxxxx.mp4
    ├── GSR_session_YYYYMMDD_HHMMSS_xxxxxxxx.csv
    ├── Audio_session_YYYYMMDD_HHMMSS_xxxxxxxx.wav
    └── session_metadata.json
```

#### File Formats

- **RGB Video**: H.264 encoded MP4
- **Raw Images**: JPEG with EXIF timestamp data
- **Thermal Images**: JPEG with temperature mapping
- **GSR Data**: CSV with columns: timestamp, gsr_value, heart_rate
- **Audio**: Uncompressed WAV stereo
- **Metadata**: JSON with session information

### App Settings and Configuration

#### Accessing Settings

1. Tap the menu button (three dots) in top-right corner
2. Select "Settings" from dropdown menu
3. Configure recording parameters as needed

#### Key Settings

**Recording Quality**:
- Video resolution: 720p, 1080p, 4K
- Video frame rate: 24, 30, 60 fps
- Audio quality: 44.1kHz, 48kHz

**Sensor Configuration**:
- GSR sampling rate: 64Hz, 128Hz, 256Hz
- Heart rate calculation: Enabled/Disabled
- Sensor calibration options

**Storage Options**:
- Storage location: Internal/External SD
- Auto-cleanup old sessions: Enabled/Disabled
- Maximum session duration: 30min, 1hr, 2hr, Unlimited

**Network Settings**:
- PC controller discovery: Automatic/Manual
- Connection timeout: 5s, 10s, 30s
- Status update frequency: 1s, 5s, 10s

## 💻 PC Controller Usage

### Interface Overview

The PC Controller features a modern tabbed interface:

```
┌─────────────────────────────────────────────────────────┐
│ GSR & Dual-Video Recording System                       │
├─────────────────────────────────────────────────────────┤
│ [🏠 Dashboard] [📱 Devices] [📊 Status] [📝 Logs] [🎬 Playback] │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  🔴 Start Recording    ⏹️ Stop Recording               │
│                                                         │
│  Connected Devices: 2                                   │
│  ┌─────────────────┐  ┌─────────────────┐              │
│  │ Phone 1         │  │ Phone 2         │              │
│  │ 🟢 Connected    │  │ 🟢 Connected    │              │
│  │ Battery: 85%    │  │ Battery: 92%    │              │
│  │ Storage: 12GB   │  │ Storage: 8GB    │              │
│  └─────────────────┘  └─────────────────┘              │
│                                                         │
│  Session: experiment_001                                │
│  Duration: 00:02:45                                     │
│  Status: Recording                                      │
└─────────────────────────────────────────────────────────┘
```

### Starting the PC Controller

1. **Launch Application**:
   ```bash
   cd windows_controller
   python src/main/main.py
   ```

2. **Interface Initialization**:
   - Modern PySide6 interface loads
   - All tabs become accessible
   - System status indicators appear
   - Ready for device connections

### Device Management Tab

#### Discovering Devices

1. **Automatic Discovery**:
   - Click "🔍 Discover Devices" button
   - System scans network using Zeroconf/mDNS
   - Available Android devices appear in list
   - Discovery typically takes 5-10 seconds

2. **Manual Device Addition**:
   - Click "➕ Add Device Manually"
   - Enter device IP address and port
   - Specify device name and type
   - Click "Add" to include in device list

#### Connecting to Devices

1. **Individual Connection**:
   - Select device from discovered list
   - Click "🔗 Connect" button
   - Wait for connection confirmation
   - Device status updates to "Connected"

2. **Bulk Connection**:
   - Click "🔗 Connect All" button
   - System attempts to connect to all discovered devices
   - Progress shown for each device
   - Summary of successful/failed connections

#### Device Information

Each connected device shows:
- **Device Name**: User-friendly identifier
- **IP Address**: Network location
- **Connection Status**: Connected/Disconnected/Error
- **Battery Level**: Current charge percentage
- **Storage Available**: Remaining storage space
- **Sensor Status**: GSR, Thermal, RGB availability
- **Last Update**: Time of last status update

### Dashboard Tab

#### Recording Controls

**Start Recording**:
1. Ensure all desired devices are connected
2. Click "🔴 Start Recording" button
3. Enter session ID (or use auto-generated)
4. Confirm recording parameters
5. All connected devices begin recording simultaneously

**Stop Recording**:
1. Click "⏹️ Stop Recording" button
2. Confirmation dialog appears
3. All devices stop recording
4. Session manifest generated automatically
5. Files remain on individual devices

#### Session Management

**Current Session Info**:
- Session ID and description
- Start time and elapsed duration
- Number of connected devices
- Recording status (Recording/Stopped/Error)
- Estimated total file size

**Session History**:
- List of recent recording sessions
- Session duration and file counts
- Quick access to session manifests
- Option to delete old sessions

### Status Dashboard Tab

#### Real-time Monitoring

**Device Status Grid**:
```
┌─────────────┬─────────────┬─────────────┬─────────────┐
│ Device      │ Connection  │ Battery     │ Storage     │
├─────────────┼─────────────┼─────────────┼─────────────┤
│ Phone_001   │ 🟢 Connected│ 85% 🟢      │ 12.5GB 🟢   │
│ Phone_002   │ 🟢 Connected│ 45% 🟡      │ 2.1GB 🟡    │
│ Phone_003   │ 🔴 Error    │ Unknown     │ Unknown     │
└─────────────┴─────────────┴─────────────┴─────────────┘
```

**Sensor Status**:
- GSR sensor connection and readings
- Thermal camera status and temperature
- RGB camera availability and settings
- Audio recording capability

**Network Health**:
- Connection latency to each device
- Data transfer rates
- Network stability indicators
- Error counts and recovery attempts

#### Alerts and Warnings

**Automatic Monitoring**:
- Low battery warnings (< 20%)
- Low storage alerts (< 1GB)
- Connection timeout notifications
- Sensor disconnection alerts

**Alert Actions**:
- Visual notifications in interface
- Optional audio alerts
- Email notifications (if configured)
- Automatic recording pause (if enabled)

### Logs Tab

#### Log Viewing

**Log Categories**:
- **System**: Application startup, shutdown, errors
- **Network**: Device connections, disconnections, timeouts
- **Recording**: Session start/stop, file operations
- **Sensors**: GSR readings, camera status, calibration

**Log Filtering**:
- Filter by log level: Debug, Info, Warning, Error
- Filter by time range: Last hour, day, week
- Filter by device: Show logs for specific devices
- Search by keyword or message content

#### Log Export

**Export Options**:
- Export filtered logs to text file
- Include timestamps and device information
- Choose date range for export
- Compress large log files automatically

### Playback Tab

#### Video Playback

**Loading Videos**:
1. Click "📁 Load Video" button
2. Browse to session folder
3. Select RGB or thermal video file
4. Video loads in playback window

**Playback Controls**:
- Play/Pause/Stop buttons
- Seek bar for navigation
- Speed control (0.25x to 4x)
- Frame-by-frame stepping
- Full-screen mode

**Multi-Video Playback**:
- Load multiple videos simultaneously
- Synchronized playback across devices
- Side-by-side comparison view
- Timeline alignment tools

#### Annotation System

**Adding Annotations**:
1. Pause video at desired timestamp
2. Click "📝 Add Annotation" button
3. Enter annotation text and category
4. Annotation saved with precise timestamp

**Annotation Management**:
- View all annotations in timeline
- Edit or delete existing annotations
- Export annotations to CSV/JSON
- Import annotations from external tools

**Annotation Categories**:
- Behavioral observations
- Experimental events
- Technical notes
- Quality assessments

## 🎬 Recording Workflows

### Single Device Recording

**Use Case**: Simple data collection with one Android device

**Workflow**:
1. Set up hardware (GSR sensor, thermal camera)
2. Launch Android app and verify connections
3. Start recording from Android app
4. Monitor real-time data during session
5. Stop recording and verify file creation
6. Transfer files to PC for analysis

**Best For**:
- Pilot studies
- Individual participant sessions
- Quick data collection
- Testing and validation

### Multi-Device Recording

**Use Case**: Synchronized recording across multiple devices

**Workflow**:
1. Set up multiple Android devices with sensors
2. Launch PC controller and discover all devices
3. Connect to all devices and verify status
4. Start synchronized recording from PC
5. Monitor all devices during session
6. Stop recording and collect session manifest
7. Gather files from all devices

**Best For**:
- Multi-participant studies
- Different camera angles
- Redundant data collection
- Large-scale experiments

### Remote Controlled Recording

**Use Case**: Operator controls recording from separate location

**Workflow**:
1. Set up Android devices in recording location
2. Operator uses PC controller from control room
3. Monitor device status remotely
4. Start/stop recording based on experimental protocol
5. Receive real-time status updates
6. Handle any issues remotely

**Best For**:
- Controlled laboratory experiments
- Minimal participant distraction
- Standardized protocols
- Professional research settings

### Automated Recording

**Use Case**: Scheduled or triggered recording sessions

**Workflow**:
1. Configure recording parameters in advance
2. Set up automated triggers (time, external signal)
3. System automatically starts recording
4. Monitor progress through status dashboard
5. Automatic stop based on duration or trigger
6. Files organized and manifests generated

**Best For**:
- Longitudinal studies
- Unattended data collection
- Standardized session durations
- High-throughput experiments

## 📊 Data Management

### File Organization

#### Session Structure

Each recording session creates a structured folder hierarchy:

```
Sessions/
├── session_20241201_143022_abc123/
│   ├── manifest.json                    # Session metadata
│   ├── devices/
│   │   ├── phone_001/
│   │   │   ├── RGB_video.mp4
│   │   │   ├── raw_rgb/
│   │   │   │   ├── frame_*.jpg
│   │   │   ├── thermal/
│   │   │   │   ├── frame_*.jpg
│   │   │   ├── GSR_data.csv
│   │   │   └── audio.wav
│   │   └── phone_002/
│   │       └── [similar structure]
│   └── analysis/
│       ├── annotations.json
│       ├── sync_report.txt
│       └── quality_metrics.json
```

#### Naming Conventions

**Session IDs**: `session_YYYYMMDD_HHMMSS_randomID`
- Date and time of session start
- Random identifier for uniqueness
- Consistent across all devices

**File Names**: Include session ID and data type
- `RGB_session_20241201_143022_abc123.mp4`
- `GSR_session_20241201_143022_abc123.csv`
- `frame_1234567890123456789.jpg` (nanosecond timestamp)

### Data Collection

#### Automatic File Collection

**From PC Controller**:
1. Go to Dashboard tab after recording
2. Click "📥 Collect Files" button
3. Select destination folder on PC
4. System downloads files from all devices
5. Progress shown for each device
6. Verification of file integrity

**Manual Collection**:
1. Connect Android device to PC via USB
2. Navigate to app storage folder
3. Copy session folders to PC
4. Verify all files transferred correctly

#### Data Verification

**Automatic Checks**:
- File size validation
- Timestamp consistency
- Data format verification
- Synchronization quality assessment

**Manual Verification**:
- Play video files to check quality
- Open CSV files to verify GSR data
- Check audio files for clarity
- Review session manifest for completeness

### Data Export

#### Export Formats

**Video Data**:
- Original MP4 files
- Frame extraction to image sequences
- Compressed versions for sharing
- Thumbnail generation for quick review

**Sensor Data**:
- Original CSV format
- MATLAB .mat files
- HDF5 for large datasets
- JSON for web applications

**Combined Exports**:
- ZIP archives of complete sessions
- Synchronized data packages
- Analysis-ready formats
- Research publication packages

#### Export Tools

**Built-in Export**:
- Session export wizard in PC controller
- Batch export for multiple sessions
- Format conversion options
- Compression and encryption

**External Tools**:
- MATLAB integration scripts
- Python analysis notebooks
- R data import functions
- Custom export plugins

## 🔧 Advanced Features

### Synchronization

#### Time Synchronization

**Automatic Sync**:
- NTP-like protocol between devices
- Sub-millisecond accuracy
- Continuous drift correction
- Sync quality monitoring

**Manual Sync Markers**:
- Insert sync events during recording
- Visual/audio cues for alignment
- Post-processing sync correction
- Sync quality assessment tools

#### Multi-Modal Alignment

**Data Stream Alignment**:
- GSR data aligned with video frames
- Audio synchronized with all streams
- Thermal frames matched to RGB video
- Timestamp-based correlation

**Quality Metrics**:
- Sync accuracy measurements
- Drift detection and correction
- Missing data identification
- Alignment confidence scores

### Integration with Research Tools

#### PsychoPy Integration

**Experiment Control**:
- Start/stop recording from PsychoPy scripts
- Send event markers to data streams
- Synchronize with experimental timeline
- Automated session management

**Data Integration**:
- Export data in PsychoPy-compatible formats
- Combine behavioral and physiological data
- Automated analysis pipelines
- Result visualization tools

#### Lab Streaming Layer (LSL)

**Real-time Streaming**:
- Stream GSR data to LSL network
- Receive event markers from other systems
- Synchronize with EEG, eye tracking, etc.
- Multi-lab data sharing

**Data Format**:
- Standard LSL data types
- Metadata preservation
- Timestamp synchronization
- Quality indicators

### Custom Analysis

#### Built-in Analysis Tools

**GSR Analysis**:
- Peak detection algorithms
- Baseline correction
- Response amplitude calculation
- Statistical summaries

**Video Analysis**:
- Motion detection
- Face detection and tracking
- Thermal analysis tools
- Frame difference calculations

#### External Analysis

**MATLAB Integration**:
- Data import functions
- Analysis toolboxes
- Visualization tools
- Statistical analysis

**Python Integration**:
- Pandas data frames
- OpenCV video processing
- SciPy signal processing
- Machine learning tools

## 💡 Tips and Best Practices

### Hardware Setup

**GSR Sensor**:
- Clean electrode sites with alcohol
- Ensure good skin contact
- Avoid movement artifacts
- Monitor battery level regularly

**Thermal Camera**:
- Allow warm-up time (2-3 minutes)
- Avoid direct sunlight
- Check USB connection stability
- Calibrate if needed

**Android Device**:
- Use devices with adequate storage
- Keep battery charged (>50%)
- Close unnecessary apps
- Enable airplane mode with WiFi

### Recording Quality

**Environment**:
- Stable lighting conditions
- Minimal electromagnetic interference
- Consistent temperature
- Quiet environment for audio

**Participant Preparation**:
- Explain recording process
- Ensure comfort with sensors
- Minimize movement during recording
- Provide clear instructions

**Technical Checks**:
- Test all connections before recording
- Verify data quality in real-time
- Monitor storage space
- Check network stability

### Data Management

**Organization**:
- Use descriptive session IDs
- Maintain consistent folder structure
- Document experimental conditions
- Back up data regularly

**Quality Control**:
- Review data immediately after recording
- Check for missing files or corruption
- Verify synchronization quality
- Document any issues

**Security**:
- Encrypt sensitive data
- Use secure network connections
- Follow institutional data policies
- Implement access controls

### Troubleshooting

**Connection Issues**:
- Restart WiFi on both devices
- Check firewall settings
- Verify network connectivity
- Try manual IP connection

**Sensor Problems**:
- Check battery levels
- Restart Bluetooth
- Re-pair sensors if needed
- Verify hardware connections

**Recording Issues**:
- Check available storage
- Verify all permissions
- Restart applications
- Monitor system resources

## ❓ Common Issues

### "GSR Sensor Won't Connect"

**Symptoms**: GSR status shows red, no readings displayed

**Solutions**:
1. Check sensor power (LED should blink blue)
2. Restart Bluetooth on Android device
3. Clear Bluetooth cache in Android settings
4. Re-pair sensor in Bluetooth settings
5. Check sensor battery level
6. Restart GSR sensor

### "Thermal Camera Not Detected"

**Symptoms**: Thermal status red, no thermal preview

**Solutions**:
1. Check USB-C cable connection
2. Grant USB permissions when prompted
3. Try different USB-C port/hub
4. Restart Android app
5. Check device USB host support
6. Try different USB cable

### "Devices Not Discovered by PC"

**Symptoms**: PC controller shows no devices in discovery

**Solutions**:
1. Ensure both devices on same WiFi network
2. Check firewall settings on PC
3. Restart WiFi on both devices
4. Try manual device addition
5. Check network subnet configuration
6. Restart both applications

### "Recording Fails to Start"

**Symptoms**: Recording button pressed but recording doesn't begin

**Solutions**:
1. Check storage space on Android device
2. Verify all sensors connected
3. Restart both applications
4. Check network connection
5. Ensure all permissions granted
6. Check battery levels

### "Poor Data Quality"

**Symptoms**: Noisy GSR data, blurry video, audio issues

**Solutions**:
1. Check sensor electrode contact
2. Minimize participant movement
3. Improve lighting conditions
4. Check camera focus
5. Reduce electromagnetic interference
6. Use higher quality settings

### "Synchronization Issues"

**Symptoms**: Data streams not aligned, timing errors

**Solutions**:
1. Ensure stable network connection
2. Restart recording session
3. Check for network interference
4. Use wired connection if possible
5. Reduce network traffic
6. Check system clock accuracy

---

**Need More Help?**

- Check the troubleshooting section in the main README
- Review log files for error messages
- Create an issue on the GitHub repository
- Consult the Python API guide for programmatic control
- Refer to the setup and connection guide for initial configuration

**Remember**: This system is designed for research purposes and requires specific hardware. Always test your setup before important data collection sessions.