# LSL Usage for Android Data Timestamping Analysis

## Question
Is LSL (Lab Streaming Layer) used on the phone to timestamp the IR, GSR and built-in camera data?

## Answer
**No, LSL is not used on the Android phone to timestamp IR, GSR, and built-in camera data.**

## Detailed Analysis

### Android Timestamping Implementation
The Android application uses its own timestamping system implemented in `TimeManager.kt`:

1. **GSR Data**: Uses `TimeManager.timestampData()` which creates timestamps with:
   - `timestampNanos`: Current system time using `SystemClock.elapsedRealtimeNanos()`
   - `sessionOffsetNanos`: Time since session start
   - Data saved as: `timestamp_nanos,session_offset_nanos,gsr_microsiemens`

2. **IR Camera Data**: Uses `TimeManager.getCurrentTimestampNanos()` to timestamp thermal frames:
   - Frames saved with timestamp in filename: `frame_${timestamp}.jpg`

3. **Built-in Camera Data**: Uses `TimeManager.getCurrentTimestamp()` for video files:
   - Videos saved with timestamp in filename: `RGB_${sessionId}_${timestamp}.mp4`

### LSL Integration Location
LSL integration exists only on the **Windows controller side** (`windows_controller/src/integrations/lsl_integration.py`):
- Creates LSL streams for GSR and PPG data
- Uses `pylsl.local_clock()` for LSL timestamps
- Receives data from Android and re-streams it via LSL

### Network Communication
The Android device sends its own timestamps to the Windows controller:
- Uses `System.currentTimeMillis()` and `TimeManager.getSynchronizedTimeMillis()`
- No LSL timestamps are sent from Android
- Time synchronization occurs between Android and Windows, but not using LSL

### Conclusion
The Android phone uses its own high-precision timestamping system based on `SystemClock.elapsedRealtimeNanos()` for all sensor and camera data. LSL timestamping is applied later on the Windows controller side when the data is re-streamed for real-time analysis or integration with other research tools.