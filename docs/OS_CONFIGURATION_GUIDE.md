# OS-Specific Configuration Guide

This guide explains how to configure the GSR-Unified project for different operating systems (Windows, macOS, Linux), including both Android development and the new high-precision C++ backend requirements.

## Overview

The project supports multiple operating systems with OS-specific configurations for:

**Android Development:**
- `gradle.properties` (root and Android platform)
- `local.properties` (root and Android platform)  
- `gradle/libs.versions.toml` (root and Android platform)

**PC Platform C++ Backend:**
- CMake build system for cross-platform C++ compilation
- OpenCV development libraries for camera capture
- pybind11 for Python-C++ bindings
- Platform-specific compilers and build tools

## Quick Setup

### 1. Choose Your Operating System Configuration

#### Windows
1. Edit `gradle.properties` files:
   - Uncomment the Windows JDK path that matches your installation
   - Comment out the current macOS path

2. Edit `local.properties` files:
   - Uncomment the Windows SDK and JDK paths that match your installation
   - Comment out the current macOS paths

#### macOS
The project is currently configured for macOS. If the default paths don't match your installation:
1. Update the paths in `gradle.properties` and `local.properties` files
2. Or use the alternative macOS paths provided in the comments

#### Linux
1. Edit `gradle.properties` files:
   - Uncomment the Linux JDK path that matches your installation
   - Comment out the current macOS path

2. Edit `local.properties` files:
   - Uncomment the Linux SDK and JDK paths that match your installation
   - Comment out the current macOS paths

### 2. C++ Backend Setup (PC Platform)

#### Windows C++ Backend Setup
1. **Install Build Tools**:
   ```bash
   # Install Visual Studio 2019+ with C++ tools
   # Or install Build Tools for Visual Studio 2019+
   ```

2. **Install Dependencies**:
   ```bash
   # Install CMake
   winget install Kitware.CMake

   # Install OpenCV (via vcpkg recommended)
   git clone https://github.com/Microsoft/vcpkg.git
   cd vcpkg
   .\bootstrap-vcpkg.bat
   .\vcpkg install opencv4[contrib]:x64-windows
   ```

3. **Build C++ Backend**:
   ```bash
   cd platforms/pc
   mkdir build && cd build
   cmake .. -DCMAKE_TOOLCHAIN_FILE=path/to/vcpkg/scripts/buildsystems/vcpkg.cmake
   cmake --build . --config Release
   ```

#### macOS C++ Backend Setup
1. **Install Build Tools**:
   ```bash
   # Install Xcode Command Line Tools
   xcode-select --install

   # Install Homebrew (if not already installed)
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```

2. **Install Dependencies**:
   ```bash
   # Install CMake and OpenCV
   brew install cmake opencv

   # pybind11 is included as git submodule
   git submodule update --init --recursive
   ```

3. **Build C++ Backend**:
   ```bash
   cd platforms/pc
   mkdir build && cd build
   cmake ..
   make
   cp _hardware_backend*.so ../src/
   ```

#### Linux C++ Backend Setup
1. **Install Build Tools**:
   ```bash
   # Ubuntu/Debian
   sudo apt update
   sudo apt install build-essential cmake git

   # CentOS/RHEL/Fedora
   sudo yum groupinstall "Development Tools"
   sudo yum install cmake git
   ```

2. **Install Dependencies**:
   ```bash
   # Ubuntu/Debian
   sudo apt install libopencv-dev python3-dev

   # CentOS/RHEL/Fedora
   sudo yum install opencv-devel python3-devel

   # pybind11 is included as git submodule
   git submodule update --init --recursive
   ```

3. **Build C++ Backend**:
   ```bash
   cd platforms/pc
   mkdir build && cd build
   cmake ..
   make
   cp _hardware_backend*.so ../src/
   ```

## Detailed Configuration

### gradle.properties Files

Located at:
- `gradle.properties` (root)
- `platforms/android/gradle.properties`

These files contain OS-specific Java home paths for the Gradle daemon:

```properties
# macOS paths (uncomment one that matches your installation)
org.gradle.java.home=/Users/duyantran/Library/Java/JavaVirtualMachines/openjdk-24.0.1/Contents/Home
# org.gradle.java.home=/opt/homebrew/Cellar/openjdk@17/17.0.8/libexec/openjdk.jdk/Contents/Home
# org.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# Windows paths (uncomment one that matches your installation)
# org.gradle.java.home=C:/Program Files/Java/jdk-17
# org.gradle.java.home=C:/Program Files/Eclipse Adoptium/jdk-17.0.8.101-hotspot
# org.gradle.java.home=C:/Program Files/OpenJDK/openjdk-17

# Linux paths (uncomment one that matches your installation)
# org.gradle.java.home=/usr/lib/jvm/java-17-openjdk-amd64
# org.gradle.java.home=/usr/lib/jvm/default-java
# org.gradle.java.home=/opt/jdk-17
```

### local.properties Files

Located at:
- `local.properties` (root)
- `platforms/android/local.properties`

These files contain Android SDK and JDK paths:

```properties
# CURRENT ACTIVE CONFIGURATION (macOS)
jdk.dir=/opt/homebrew/Cellar/openjdk/24.0.1/libexec/openjdk.jdk/Contents/Home
sdk.dir=/Users/duyantran/Library/Android/sdk

# WINDOWS CONFIGURATIONS
# sdk.dir=C:/Users/%USERNAME%/AppData/Local/Android/Sdk
# jdk.dir=C:/Program Files/Java/jdk-17

# LINUX CONFIGURATIONS  
# sdk.dir=/home/%USERNAME%/Android/Sdk
# jdk.dir=/usr/lib/jvm/java-17-openjdk-amd64
```

### libs.versions.toml Files

Located at:
- `gradle/libs.versions.toml` (root)
- `platforms/android/gradle/libs.versions.toml`

These files contain version catalogs with optional OS-specific build tool versions:

```toml
[versions]
# Build Tools & Plugins
agp = "8+"
kotlin = "2+"
# OS-specific build tool versions (if needed for different platforms)
# agp-windows = "8.4.2"
# agp-macos = "8.4.2" 
# agp-linux = "8.4.2"
```

## Environment Variables Alternative

Instead of modifying the configuration files, you can set these environment variables:

### Windows
```cmd
set ANDROID_HOME=C:\Users\%USERNAME%\AppData\Local\Android\Sdk
set JAVA_HOME=C:\Program Files\Java\jdk-17
```

### macOS/Linux
```bash
export ANDROID_HOME=/Users/$USER/Library/Android/sdk  # macOS
export ANDROID_HOME=/home/$USER/Android/Sdk           # Linux
export JAVA_HOME=/path/to/your/jdk
```

Environment variables take precedence over local.properties settings.

## Common Installation Paths

### Android SDK

#### Windows
- `C:/Users/%USERNAME%/AppData/Local/Android/Sdk`
- `C:/Android/Sdk`

#### macOS
- `/Users/%USERNAME%/Library/Android/sdk`
- `/opt/android-sdk`

#### Linux
- `/home/%USERNAME%/Android/Sdk`
- `/opt/android-sdk`

### JDK

#### Windows
- `C:/Program Files/Java/jdk-17`
- `C:/Program Files/Eclipse Adoptium/jdk-17.0.8.101-hotspot`
- `C:/Program Files/OpenJDK/openjdk-17`

#### macOS
- `/opt/homebrew/Cellar/openjdk@17/17.0.8/libexec/openjdk.jdk/Contents/Home` (Homebrew)
- `/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home` (Oracle)

#### Linux
- `/usr/lib/jvm/java-17-openjdk-amd64` (Ubuntu/Debian)
- `/usr/lib/jvm/default-java`
- `/opt/jdk-17`

## Finding Your Installations

### macOS
```bash
# Find Java installations
/usr/libexec/java_home -V

# Find Android SDK (if installed via Android Studio)
ls ~/Library/Android/sdk
```

### Windows
```cmd
# Find Java installations
where java
dir "C:\Program Files\Java"

# Find Android SDK
dir "%LOCALAPPDATA%\Android\Sdk"
```

### Linux
```bash
# Find Java installations
update-alternatives --list java
ls /usr/lib/jvm/

# Find Android SDK
ls ~/Android/Sdk
```

## Troubleshooting

1. **Build fails with Java version error**: Ensure you're using Java 17 or higher
2. **Android SDK not found**: Verify the SDK path exists and contains the required components
3. **Permission errors**: Ensure the user has read/write access to the SDK and JDK directories
4. **Path with spaces**: Use quotes around paths containing spaces in environment variables

## Notes

- The project requires Java 17 or higher for Android Gradle Plugin compatibility
- Environment variables take precedence over local.properties settings
- Both root and Android platform configuration files should be updated consistently
- The current configuration is set up for macOS - modify as needed for your OS

