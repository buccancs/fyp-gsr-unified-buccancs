# Android Device Setup Guide

## Resolving "No target device found" Error

If you encounter the "No target device found" error when trying to run the Android app, it means there are no Android devices (physical or virtual) available for deployment. This guide will help you set up an Android Virtual Device (AVD) to resolve this issue.

## Prerequisites

1. Ensure the Android SDK is properly installed by running:
   ```bash
   # Windows
   environments\setup_android.bat
   
   # Linux/macOS
   environments/setup_android.sh
   ```

2. Verify the Android environment is set up correctly:
   ```bash
   gradlew :platforms:android:verifyAndroidEnv
   ```

## Solution 1: Create and Start an Android Virtual Device (AVD)

### Step 1: Create an AVD

Run the AVD creation script:
```bash
# Windows
environments\create_avd.bat

# Linux/macOS (create similar script if needed)
# Use the Android SDK tools directly
```

This script will:
- Install the required Android system image (API 35)
- Create a Pixel 7 AVD named "Pixel_7_API_35"
- Configure the AVD for optimal performance

### Step 2: Start the Emulator

Run the emulator start script:
```bash
# Windows
environments\start_emulator.bat
```

This will:
- Check if the AVD exists
- Start the Android emulator
- Keep it running for app deployment

### Step 3: Verify Device Detection

Once the emulator is running, you can verify it's detected by running:
```bash
# Check connected devices
environments\android\platform-tools\adb.exe devices
```

You should see output similar to:
```
List of devices attached
emulator-5554   device
```

## Solution 2: Connect a Physical Android Device

### Step 1: Enable Developer Options

1. Go to **Settings** > **About phone**
2. Tap **Build number** 7 times to enable Developer Options
3. Go back to **Settings** > **Developer options**
4. Enable **USB debugging**

### Step 2: Connect Device

1. Connect your Android device via USB
2. Allow USB debugging when prompted on the device
3. Verify connection:
   ```bash
   environments\android\platform-tools\adb.exe devices
   ```

## Solution 3: Manual AVD Creation (Alternative)

If the automated scripts don't work, you can create an AVD manually:

### Step 1: Install System Image
```bash
environments\android\cmdline-tools\latest\bin\sdkmanager.bat "system-images;android-35;google_apis;x86_64"
```

### Step 2: Create AVD
```bash
environments\android\cmdline-tools\latest\bin\avdmanager.bat create avd -n "MyAVD" -k "system-images;android-35;google_apis;x86_64"
```

### Step 3: Start Emulator
```bash
environments\android\emulator\emulator.exe -avd MyAVD
```

## Troubleshooting

### Common Issues and Solutions

#### 1. "AVD Manager not found"
- **Cause**: Android SDK not properly installed
- **Solution**: Run `environments\setup_android.bat` first

#### 2. "System image not available"
- **Cause**: Required Android system image not downloaded
- **Solution**: The create_avd.bat script automatically downloads it, or run:
  ```bash
  environments\android\cmdline-tools\latest\bin\sdkmanager.bat "system-images;android-35;google_apis;x86_64"
  ```

#### 3. "Emulator won't start"
- **Cause**: Hardware acceleration issues or insufficient resources
- **Solutions**:
  - Ensure Hyper-V is disabled on Windows
  - Enable Intel HAXM or AMD-V in BIOS
  - Try software rendering: add `-gpu swiftshader_indirect` to emulator command

#### 4. "Device not detected after connecting"
- **Cause**: USB debugging not enabled or drivers missing
- **Solutions**:
  - Enable USB debugging in Developer Options
  - Install device-specific USB drivers
  - Try different USB cable/port

### Performance Tips

1. **Allocate sufficient RAM**: Edit AVD settings to allocate 2-4GB RAM
2. **Enable hardware acceleration**: Ensure Intel HAXM or AMD-V is installed
3. **Use x86_64 images**: Faster than ARM images on x86 computers
4. **Close unnecessary applications**: Free up system resources

## Verification

After setting up your device/emulator, verify everything works:

1. **Check device detection**:
   ```bash
   environments\android\platform-tools\adb.exe devices
   ```

2. **Build and deploy the app**:
   ```bash
   gradlew :platforms:android:app:installDebug
   ```

3. **Run the app**:
   ```bash
   gradlew :platforms:android:app:run
   ```

## Quick Reference Commands

| Task | Windows Command |
|------|----------------|
| Setup Android SDK | `environments\setup_android.bat` |
| Create AVD | `environments\create_avd.bat` |
| Start Emulator | `environments\start_emulator.bat` |
| List devices | `environments\android\platform-tools\adb.exe devices` |
| List AVDs | `environments\android\cmdline-tools\latest\bin\avdmanager.bat list avd` |

## Next Steps

Once you have a target device available:

1. Open the project in Android Studio or your preferred IDE
2. Select your device/emulator as the deployment target
3. Build and run the Android application

The "No target device found" error should now be resolved, and you can successfully deploy and test your Android application.