# GSR Multimodal System - Hardware Requirements & Architecture

This document outlines the hardware requirements, system architecture, and technical specifications for the GSR Multimodal Data Collection System.

## Hardware Requirements

### Android Devices
- **Minimum Requirements**:
  - Android 8.0 (API level 26) or higher
  - USB-C port for thermal camera connection
  - Bluetooth 4.0+ for GSR sensor connectivity
  - WiFi 802.11n for PC communication
  - Minimum 4GB RAM, 32GB storage
  - Rear-facing camera with autofocus
  - Microphone for audio recording

- **Recommended Specifications**:
  - Android 10+ for optimal performance
  - 6GB+ RAM for smooth multi-modal recording
  - 64GB+ storage for local data buffering
  - USB 3.0+ for high-speed thermal data transfer
  - Bluetooth 5.0+ for improved GSR connectivity

### PC Controller System
- **Operating System**: Windows 10/11 (64-bit)
- **Processor**: Intel i5-8th gen or AMD Ryzen 5 3600 (minimum)
- **Memory**: 8GB RAM (16GB recommended)
- **Storage**: 256GB SSD (1TB recommended for data storage)
- **Network**: WiFi 802.11ac or Gigabit Ethernet
- **Bluetooth**: Bluetooth 4.0+ adapter (internal or USB dongle)
- **Display**: 1920x1080 minimum resolution
- **USB Ports**: Multiple USB 3.0+ ports for device management

### Sensor Hardware

#### Topdon TC001 Thermal Camera
- **Resolution**: 256×192 thermal sensor
- **Temperature Range**: -20°C to +550°C
- **Accuracy**: ±2°C or ±2%
- **Frame Rate**: Up to 25 Hz
- **Interface**: USB-C connection to Android device
- **Power**: Bus-powered via USB-C
- **SDK**: Topdon proprietary SDK required

#### Shimmer3 GSR+ Sensor
- **Sensor Type**: Galvanic Skin Response (GSR/EDA)
- **Range**: 10kΩ to 4.7MΩ
- **Resolution**: 16-bit ADC
- **Sampling Rate**: Up to 512 Hz (128 Hz recommended)
- **Connectivity**: Bluetooth Classic (SPP profile)
- **Battery**: Rechargeable Li-ion, 8+ hours operation
- **Electrodes**: Disposable Ag/AgCl electrodes

## System Architecture

### Network Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        WiFi Network                             │
│                     (192.168.1.0/24)                          │
└─────────────────────────────────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        │                       │                       │
┌───────▼────────┐    ┌─────────▼────────┐    ┌────────▼────────┐
│ Android Device │    │ Android Device   │    │  PC Controller  │
│   (Phone 1)    │    │   (Phone 2)      │    │                 │
│ 192.168.1.100  │    │ 192.168.1.101    │    │ 192.168.1.10    │
│ Port: 8080     │    │ Port: 8080       │    │ Port: 8080      │
└────────────────┘    └──────────────────┘    └─────────────────┘
```

### Data Flow Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Data Sources  │    │   Processing    │    │   Storage &     │
│                 │    │                 │    │   Analysis      │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ RGB Camera      │───▶│ Video Encoding  │───▶│ MP4 Files       │
│ Thermal Camera  │───▶│ Thermal Proc.   │───▶│ Thermal Data    │
│ GSR Sensor      │───▶│ Signal Filter   │───▶│ CSV Files       │
│ Microphone      │───▶│ Audio Encoding  │───▶│ Audio Tracks    │
│ Timestamps      │───▶│ Synchronization │───▶│ Sync Metadata   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Software Architecture

#### Android Application Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    MainActivity                             │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────┐ │
│ │   Camera    │ │   Thermal   │ │     GSR     │ │ Network │ │
│ │  Manager    │ │   Manager   │ │   Manager   │ │ Manager │ │
│ └─────────────┘ └─────────────┘ └─────────────┘ └─────────┘ │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────┐ │
│ │   CameraX   │ │ Topdon SDK  │ │ Shimmer SDK │ │ OkHttp  │ │
│ │   Library   │ │             │ │             │ │ Client  │ │
│ └─────────────┘ └─────────────┘ └─────────────┘ └─────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    Android Framework                        │
└─────────────────────────────────────────────────────────────┘
```

#### PC Controller Architecture
```
┌─────────────────────────────────────────────────────────────┐
│                    Main Window (PySide6)                   │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────┐ │
│ │   Device    │ │     GUI     │ │   Session   │ │   Data  │ │
│ │  Manager    │ │  Controller │ │   Manager   │ │ Analysis│ │
│ └─────────────┘ └─────────────┘ └─────────────┘ └─────────┘ │
├─────────────────────────────────────────────────────────────┤
│ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────┐ │
│ │   Socket    │ │   OpenCV    │ │    NumPy    │ │ Pandas  │ │
│ │ Networking  │ │   Vision    │ │  Processing │ │ Analysis│ │
│ └─────────────┘ └─────────────┘ └─────────────┘ └─────────┘ │
├─────────────────────────────────────────────────────────────┤
│                    Python Runtime                          │
└─────────────────────────────────────────────────────────────┘
```

## Communication Protocols

### TCP/IP Communication
- **Protocol**: TCP over WiFi
- **Port**: 8080 (configurable)
- **Message Format**: JSON over TCP
- **Encoding**: UTF-8
- **Heartbeat**: 30-second intervals
- **Timeout**: 60 seconds

### Message Structure
```json
{
  "command": "start_recording|stop_recording|get_status|heartbeat",
  "timestamp": 1640995200.123,
  "device_id": "android_device_001",
  "data": {
    "session_id": "session_20231201_100000",
    "parameters": {}
  }
}
```

### Bluetooth Communication (GSR Sensor)
- **Profile**: Serial Port Profile (SPP)
- **Baud Rate**: 115200 bps
- **Data Format**: Binary packets (Shimmer format)
- **Sampling Rate**: 128 Hz
- **Packet Size**: Variable (typically 20-30 bytes)

## Data Synchronization

### Timestamp Strategy
- **Reference Clock**: PC system time (NTP synchronized)
- **Android Sync**: Regular time sync requests to PC
- **GSR Timestamps**: Device clock with offset correction
- **Video Timestamps**: MediaRecorder PTS (Presentation Time Stamps)
- **Thermal Timestamps**: Frame capture time with USB latency compensation

### Synchronization Accuracy
- **Target Accuracy**: ±10ms across all modalities
- **WiFi Latency**: Typically 1-5ms on local network
- **USB Latency**: 1-3ms for thermal camera
- **Bluetooth Latency**: 10-50ms for GSR sensor

## File Organization

### Session Directory Structure
```
sessions/
└── session_20231201_100000/
    ├── metadata.json
    ├── device_001/
    │   ├── rgb_video.mp4
    │   ├── thermal_data.mp4
    │   ├── gsr_data.csv
    │   └── device_info.json
    ├── device_002/
    │   ├── rgb_video.mp4
    │   ├── thermal_data.mp4
    │   ├── gsr_data.csv
    │   └── device_info.json
    └── analysis/
        ├── sync_report.json
        ├── gsr_analysis.csv
        └── session_summary.pdf
```

### File Formats

#### Video Files (RGB)
- **Format**: MP4 (H.264 video, AAC audio)
- **Resolution**: 1920x1080 @ 30fps (configurable)
- **Bitrate**: 8-12 Mbps (variable)
- **Audio**: 48kHz, 16-bit, stereo

#### Thermal Data
- **Format**: MP4 (H.264 encoded) or image sequence
- **Resolution**: 256x192 @ 25fps
- **Color Map**: Configurable (ironbow, rainbow, grayscale)
- **Temperature Data**: Separate CSV with raw values

#### GSR Data
- **Format**: CSV (timestamp, value, quality)
- **Sampling Rate**: 128 Hz
- **Units**: Microsiemens (µS)
- **Precision**: 0.001 µS

## Performance Specifications

### Recording Capacity
- **Simultaneous Devices**: Up to 2 Android phones
- **Recording Duration**: Limited by storage capacity
- **Data Rate per Device**: ~15-20 MB/minute
- **Network Bandwidth**: ~2-5 Mbps per device (preview streams)

### System Latency
- **Command Response**: <100ms
- **Preview Latency**: <200ms
- **Recording Start Sync**: <500ms across all devices
- **Data Processing**: Real-time for GSR, near real-time for video

## Troubleshooting

### Common Issues
1. **Device Discovery Fails**
   - Check WiFi network connectivity
   - Verify firewall settings (port 8080)
   - Ensure devices are on same subnet

2. **GSR Sensor Connection Issues**
   - Verify Bluetooth pairing
   - Check sensor battery level
   - Restart Bluetooth service

3. **Thermal Camera Not Detected**
   - Check USB-C connection
   - Verify USB host mode enabled
   - Install manufacturer drivers

4. **Synchronization Drift**
   - Check network latency
   - Verify NTP synchronization
   - Monitor system clock stability

### Performance Optimization
- Use SSD storage for better I/O performance
- Close unnecessary applications during recording
- Use 5GHz WiFi for reduced interference
- Ensure adequate cooling for extended sessions

## Security Considerations

### Network Security
- Use WPA3 encrypted WiFi networks
- Consider VPN for remote deployments
- Implement device authentication
- Regular security updates

### Data Privacy
- Encrypt stored data files
- Secure data transfer protocols
- Access control for sensitive data
- Compliance with data protection regulations

## Future Enhancements

### Planned Features
- Support for additional sensor types
- Cloud storage integration
- Real-time data streaming to external systems
- Advanced synchronization algorithms
- Mobile app for remote monitoring

### Scalability Considerations
- Support for more than 2 devices
- Distributed processing capabilities
- Integration with research databases
- API for third-party applications