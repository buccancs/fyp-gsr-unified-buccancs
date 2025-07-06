# Samsung Device Deployment Guide

This guide explains how to deploy and debug the GSR Capture Android application on Samsung devices using the configured run/debug configurations.

## Prerequisites

### 1. Samsung Device Setup
- Enable Developer Options on your Samsung device:
  1. Go to Settings > About phone
  2. Tap "Build number" 7 times
  3. Go back to Settings > Developer options
  4. Enable "USB debugging"
  5. Enable "Stay awake" (recommended for development)

### 2. USB Connection
- Connect your Samsung device to your computer via USB
- When prompted on the device, allow USB debugging for your computer
- Verify device connection: `adb devices` should show your device

### 3. Development Environment Setup
- **Java 17 or higher**: Android Gradle plugin requires Java 17+
  - Check current Java version: `java -version`
  - Find Java installations on macOS: `/usr/libexec/java_home -V`
  - Configure in `platforms/android/gradle.properties` if needed
- **Android SDK**: Properly configured (already set in local.properties)
- **Android Studio/IntelliJ IDEA**: Latest version recommended
- **Samsung USB drivers**: Install if needed for device recognition
- **ADB**: Verify ADB can detect your Samsung device (`adb devices`)

## Available Run Configurations

### 1. Android App - Samsung Deploy
**Purpose**: Basic deployment and launch configuration
- Deploys the app to connected Samsung device
- Launches the main activity
- Basic debugging support
- Use for: Quick testing and deployment

### 2. Android App - Samsung Debug (Recommended)
**Purpose**: Enhanced debugging configuration optimized for Samsung devices
- **Features**:
  - Automatic logcat display
  - Clears app storage on each run for clean testing
  - Hybrid debugger (Java/Kotlin + Native)
  - Advanced profiling enabled
  - Enhanced debugging options
- Use for: Development, debugging, and performance analysis

### 3. Supporting Configurations
- **Android App - Build**: Builds the debug APK
- **Android App - Clean**: Cleans build artifacts
- **Android App - Install**: Installs app without launching
- **Android App - Test**: Runs unit and instrumentation tests

## How to Use

### Method 1: Using Run Configurations
1. Open the project in Android Studio/IntelliJ IDEA
2. Select "Android App - Samsung Debug" from the run configuration dropdown
3. Ensure your Samsung device is selected in the device dropdown
4. Click the Run (▶) or Debug (🐛) button

### Method 2: Using Gradle Commands
```bash
# Navigate to Android platform directory
cd platforms/android

# Build and install
./gradlew installDebug

# Run tests
./gradlew test connectedAndroidTest
```

## Samsung-Specific Considerations

### 1. Permissions
The app requires several permissions that may need manual approval on Samsung devices:
- Camera access
- Storage access (for saving recordings)
- Bluetooth access (for GSR sensor connectivity)
- Microphone access (for audio recording)

### 2. Samsung Knox
- Some Samsung devices have Knox security features
- Ensure Knox doesn't block development features
- You may need to disable Knox for development

### 3. Battery Optimization
- Samsung devices may aggressively optimize battery usage
- Add the app to battery optimization whitelist during testing
- Go to Settings > Battery > App power management > Apps that won't be put to sleep

### 4. Samsung-Specific Features
- The app may behave differently on Samsung devices due to:
  - Samsung's Android customizations (One UI)
  - Different camera APIs
  - Samsung-specific Bluetooth implementations

## Troubleshooting

### Device Not Detected
1. Check USB cable and connection
2. Verify USB debugging is enabled
3. Try different USB ports
4. Install Samsung USB drivers
5. Run `adb kill-server && adb start-server`

### App Installation Fails
1. Ensure device has enough storage
2. Check if app is already installed (uninstall first)
3. Verify app permissions in AndroidManifest.xml
4. Check for Samsung-specific restrictions

### Debugging Issues
1. Use "Android App - Samsung Debug" configuration
2. Check logcat output for errors
3. Verify breakpoints are set correctly
4. Ensure debug build variant is selected

### Performance Issues
1. Enable advanced profiling in debug configuration
2. Monitor memory usage in Android Studio
3. Check for Samsung-specific performance optimizations
4. Test on different Samsung device models if available

## Application Features

The GSR Capture application includes:
- **Camera recording** with CameraX
- **GSR sensor data collection** via Bluetooth (Shimmer SDK)
- **Thermal camera support** (Topdon SDK)
- **Network communication** for data transmission
- **Multi-sensor data synchronization**

## Development Tips

1. **Use Samsung Debug configuration** for active development
2. **Clear app storage** between test runs for consistent behavior
3. **Monitor logcat** for Samsung-specific warnings or errors
4. **Test permissions** thoroughly on Samsung devices
5. **Verify sensor connectivity** (Bluetooth, camera, etc.)

## Support

For issues specific to Samsung device deployment:
1. Check Samsung Developer documentation
2. Verify Android compatibility matrix
3. Test on multiple Samsung device models
4. Consider Samsung-specific Android customizations
