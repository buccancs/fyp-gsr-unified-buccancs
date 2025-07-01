# GSR Multimodal System - Testing Guide

This guide provides instructions for testing the GSR Multimodal System communication between Android devices and the PC controller.

## Prerequisites

### Hardware Requirements
- 1-2 Android devices with USB-C port (API 26+)
- Windows PC with WiFi connectivity
- All devices on the same WiFi network

### Software Requirements
- Android app installed on device(s)
- Python 3.8+ on PC
- Required Python packages (see `pc-app/requirements.txt`)

## Setup Instructions

### 1. Android App Setup

1. **Build and Install Android App**:
   ```bash
   # In IntelliJ IDEA
   # 1. Open the project
   # 2. Select "Android App" run configuration
   # 3. Build and install on connected device
   ```

2. **Grant Permissions**:
   - Camera permission
   - Microphone permission
   - Storage permissions
   - Bluetooth permissions (for GSR sensor)
   - Location permissions (for Bluetooth scanning)

3. **Network Configuration**:
   - Ensure Android device is connected to WiFi
   - Note the device's IP address (Settings > WiFi > Network details)
   - Ensure the network allows device-to-device communication

### 2. PC Controller Setup

1. **Install Dependencies**:
   ```bash
   cd pc-app
   pip install -r requirements.txt
   ```

2. **Network Configuration**:
   - Ensure PC is on the same WiFi network as Android devices
   - Configure Windows Firewall to allow connections on port 8080
   - Note the PC's IP address

## Testing Procedures

### 1. Basic Communication Test

Run the automated test script:

```bash
cd pc-app
python test_communication.py
```

**Expected Output**:
```
Starting GSR Multimodal System Communication Test
============================================================
Testing device discovery...
Found 1 devices:
  - android_device_123 at 192.168.1.100:8080
Testing device connections...
Attempting to connect to android_device_123...
Successfully connected to android_device_123
Testing recording commands...
Sending start recording command...
Start recording successful on android_device_123
Sending stop recording command...
Stop recording successful on android_device_123
Testing GSR handler...
Latest GSR value: 19.5
Recent data points: 10
Communication test completed successfully!
```

### 2. Manual GUI Testing

1. **Start PC Controller**:
   ```bash
   cd pc-app
   python main.py
   ```

2. **Test Device Discovery**:
   - Click "Scan for Devices" button
   - Verify Android devices appear in the device list
   - Check connection status (✓ for connected, ✗ for failed)

3. **Test Recording Control**:
   - Select output directory
   - Click "Start Recording"
   - Verify recording starts on Android devices
   - Check real-time GSR values update
   - Click "Stop Recording"
   - Verify recording stops on all devices

### 3. Android App Testing

1. **Launch Android App**:
   - Open the GSR Multimodal app on Android device
   - Check status shows "Camera ready"
   - Verify network server starts

2. **Test Local Recording**:
   - Tap "Start Recording" button
   - Verify camera preview is active
   - Check GSR simulation values update
   - Tap "Stop Recording"
   - Verify files are created in device storage

3. **Test Remote Control**:
   - Keep Android app open
   - Use PC controller to start/stop recording
   - Verify Android app responds to remote commands
   - Check status updates in Android app

## Troubleshooting

### Common Issues

#### 1. No Devices Found
**Symptoms**: PC controller shows "Found 0 devices"

**Solutions**:
- Verify Android app is running and shows "Network server started"
- Check both devices are on same WiFi network
- Verify firewall settings allow port 8080
- Try different IP range in device discovery (e.g., 192.168.0, 10.0.0)

#### 2. Connection Failed
**Symptoms**: Devices found but connection fails (✗ status)

**Solutions**:
- Restart Android app
- Check Android device firewall/security settings
- Verify port 8080 is not blocked
- Try connecting to device IP manually

#### 3. Recording Commands Fail
**Symptoms**: Commands sent but Android doesn't respond

**Solutions**:
- Check Android app logs for errors
- Verify JSON command format
- Restart both applications
- Check network stability

#### 4. GSR Values Not Updating
**Symptoms**: GSR display shows "-- µS"

**Solutions**:
- Verify Android app is sending GSR data
- Check data callback registration
- Restart PC controller
- Check for data parsing errors in logs

### Network Configuration

#### Windows Firewall Setup
1. Open Windows Defender Firewall
2. Click "Allow an app or feature through Windows Defender Firewall"
3. Click "Change Settings" → "Allow another app"
4. Browse to Python executable
5. Check both "Private" and "Public" networks
6. Click "OK"

#### Alternative: Disable Firewall Temporarily
```cmd
# Run as Administrator
netsh advfirewall set allprofiles state off
# Remember to re-enable after testing:
netsh advfirewall set allprofiles state on
```

#### Router Configuration
- Ensure AP isolation is disabled
- Check if guest network restricts device communication
- Verify port forwarding is not interfering

## Performance Testing

### 1. Latency Testing
- Measure command response time
- Check data streaming latency
- Verify synchronization accuracy

### 2. Stability Testing
- Run extended recording sessions
- Test with multiple devices
- Monitor memory usage and performance

### 3. Data Integrity Testing
- Verify file creation and format
- Check timestamp synchronization
- Validate data completeness

## Advanced Testing

### 1. Multi-Device Testing
- Connect 2 Android devices simultaneously
- Test synchronized recording
- Verify independent control

### 2. Error Recovery Testing
- Disconnect devices during recording
- Test network interruption recovery
- Verify graceful error handling

### 3. Load Testing
- Test with high-frequency data streams
- Monitor system resource usage
- Verify UI responsiveness

## Test Results Documentation

### Success Criteria
- [ ] Device discovery finds all Android devices
- [ ] All devices connect successfully
- [ ] Recording commands work reliably
- [ ] Real-time data updates function
- [ ] Files are created correctly
- [ ] Error handling works properly

### Performance Benchmarks
- Device discovery time: < 30 seconds
- Connection establishment: < 5 seconds
- Command response time: < 1 second
- Data update frequency: Real-time (< 100ms delay)

## Reporting Issues

When reporting issues, include:
1. System specifications (Android version, PC OS)
2. Network configuration details
3. Complete error logs from both applications
4. Steps to reproduce the issue
5. Expected vs actual behavior

## Next Steps

After successful testing:
1. Proceed with actual sensor integration (Shimmer GSR, Topdon thermal camera)
2. Implement advanced features (data visualization, session management)
3. Add production-ready error handling and recovery
4. Optimize performance for extended recording sessions