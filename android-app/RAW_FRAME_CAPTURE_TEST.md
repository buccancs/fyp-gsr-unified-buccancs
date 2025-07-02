# RGB Raw Frame Capture and Sync Markers - Testing Guide

## Overview

This document provides testing procedures for the newly implemented RGB raw frame capture and visual sync marker
functionality in the GSR Multimodal Android app.

## New Features Implemented

### 1. RGB Raw Frame Capture

- **ImageAnalysis Use Case**: Added to CameraHandler.kt for real-time frame access
- **RawFrameCallback Interface**: Provides callbacks for frame processing events
- **Frame Metadata Logging**: CSV logging of frame data (frame number, timestamp, dimensions, size)
- **Concurrent Operation**: Works alongside existing video recording

### 2. Visual Sync Markers

- **Screen Flash Sync**: Full-screen white flash for visual synchronization
- **Camera Flash Sync**: Camera torch flash for cross-device synchronization
- **Combined Sync**: Both screen and camera flash simultaneously
- **Timestamp Markers**: Logged markers for external synchronization events

## Testing Procedures

### Test 1: Basic Raw Frame Capture

**Objective**: Verify RGB raw frame capture functionality

**Steps**:

1. Launch the GSR Multimodal app
2. Grant all required permissions (camera, storage, etc.)
3. Start recording by pressing the record button
4. Observe status messages for "RGB frame capture started"
5. Let recording run for 30 seconds
6. Stop recording
7. Check device storage for CSV file: `{sessionId}_{deviceId}_rgb_frames.csv`

**Expected Results**:

- Status shows "RGB frame capture started" and "RGB frame capture stopped"
- CSV file contains frame metadata with columns: frame_number, timestamp_ms, width, height, frame_size_bytes
- Frame numbers increment sequentially
- Timestamps are in milliseconds
- No frame drops or errors in logs

**Log Verification**:

```
adb logcat | grep "RGB raw frame received"
```

Should show periodic frame reception logs (every 30th frame).

### Test 2: Visual Sync Markers

**Objective**: Verify sync marker functionality

**Steps**:

1. Set up multiple recording devices (phones, cameras) pointing at the test device
2. Start recording on all devices
3. Start recording on the test device (triggers automatic sync markers)
4. Observe screen flash and camera flash after ~500ms
5. Wait 10 seconds
6. Stop recording (triggers final sync markers)
7. Observe final screen and camera flash

**Expected Results**:

- Screen flashes bright white for 200ms at start and stop
- Camera flash activates for 200ms at start and stop (if available)
- Sync markers are visible in recordings from other devices
- CSV file contains SYNC_MARKER entries

**CSV Sync Marker Entries**:

```
SYNC_MARKER,SCREEN_FLASH,{timestamp},200
SYNC_MARKER,CAMERA_FLASH,{timestamp},200
SYNC_MARKER,COMBINED,{timestamp},200
TIMESTAMP_MARKER,RECORDING_START,{timestamp},Session: {sessionId}
TIMESTAMP_MARKER,RECORDING_STOP,{timestamp},Session: {sessionId}
```

### Test 3: Multi-Modal Synchronization

**Objective**: Verify synchronization across all data streams

**Steps**:

1. Connect GSR sensor via Bluetooth
2. Connect thermal camera via USB-C
3. Start recording (all modalities)
4. Perform synchronized actions:
    - Clap hands (audio sync)
    - Wave hand in front of thermal camera
    - Touch GSR sensor
5. Stop recording

**Expected Results**:

- All data streams start within 100ms of each other
- Sync markers appear in all relevant data files
- Timestamps are aligned across modalities
- No data loss or corruption

### Test 4: Performance and Resource Usage

**Objective**: Verify system performance with raw frame capture

**Steps**:

1. Monitor CPU and memory usage before recording
2. Start recording with all features enabled
3. Record for 5 minutes
4. Monitor system performance during recording
5. Stop recording and check final file sizes

**Expected Results**:

- CPU usage remains reasonable (<80%)
- Memory usage stable (no leaks)
- Frame rate maintained (~30 FPS for raw frames)
- All recordings complete successfully
- File sizes are reasonable

**Performance Monitoring**:

```bash
# Monitor CPU usage
adb shell top | grep com.fpygsrunified.android

# Monitor memory usage
adb shell dumpsys meminfo com.fpygsrunified.android
```

### Test 5: Error Handling

**Objective**: Verify robust error handling

**Test Cases**:

1. **Storage Full**: Fill device storage, attempt recording
2. **Camera Busy**: Use camera in another app, then test
3. **Permission Denied**: Revoke camera permission during recording
4. **Low Battery**: Test with low battery conditions
5. **Network Issues**: Test with poor WiFi connectivity

**Expected Results**:

- Graceful error messages displayed
- No app crashes
- Partial data saved when possible
- System recovers after error resolution

## File Output Verification

### Expected Files After Recording

1. **Video File**: `{sessionId}_{deviceId}_rgb_video.mp4`
2. **Frame Metadata**: `{sessionId}_{deviceId}_rgb_frames.csv`
3. **GSR Data**: `{sessionId}_{deviceId}_gsr_data.csv`
4. **Thermal Data**: `{sessionId}_{deviceId}_thermal_data.csv`

### CSV File Format Examples

**RGB Frames CSV**:

```csv
frame_number,timestamp_ms,width,height,frame_size_bytes
1,1703123456789,1920,1080,6220800
2,1703123456823,1920,1080,6220800
SYNC_MARKER,SCREEN_FLASH,1703123457000,200
TIMESTAMP_MARKER,RECORDING_START,1703123456500,Session: session_20231221_143056
```

## Troubleshooting

### Common Issues

1. **No Raw Frames Captured**
    - Check camera permissions
    - Verify ImageAnalysis binding in logs
    - Ensure sufficient storage space

2. **Sync Markers Not Visible**
    - Check window parameter in CameraHandler initialization
    - Verify camera flash availability
    - Check screen brightness settings

3. **Performance Issues**
    - Reduce frame processing frequency
    - Check device specifications
    - Monitor background apps

### Debug Commands

```bash
# View all app logs
adb logcat | grep fpygsrunified

# Check file creation
adb shell ls -la /sdcard/Android/data/com.fpygsrunified.android/files/

# Monitor frame capture
adb logcat | grep "RGB raw frame\|Frame capture\|Sync marker"
```

## Success Criteria

The implementation is considered successful if:

1. ✅ Raw RGB frames are captured at ~30 FPS
2. ✅ Frame metadata is logged accurately
3. ✅ Visual sync markers are clearly visible
4. ✅ Synchronization accuracy is within 50ms
5. ✅ No significant performance degradation
6. ✅ Robust error handling
7. ✅ Integration with existing functionality maintained

## Next Steps

After successful testing:

1. **Optimization**: Fine-tune frame processing for better performance
2. **Advanced Sync**: Implement audio-based sync markers
3. **Analysis Tools**: Create post-processing scripts for sync verification
4. **Documentation**: Update user manual with new features