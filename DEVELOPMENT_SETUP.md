# Development Setup Guide

This guide provides comprehensive instructions for setting up the development environment for the GSR & Dual-Video Recording System. The repository has been configured to be self-sufficient with all necessary run/debug configurations.

## Prerequisites

### Required Software
- **Java Development Kit (JDK) 24**
  - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
  - Set `JAVA_HOME` environment variable
  - **Note**: The project has been upgraded to use Java 24 features

- **Android SDK**
  - Download [Android Studio](https://developer.android.com/studio) (recommended) or standalone SDK
  - Set `ANDROID_HOME` environment variable
  - Install Android SDK Platform 34 (Android 14) and Build Tools 34.0.0
  - **Note**: Updated to target Android 14 (API 34)

- **Python 3.8 or higher**
  - Download from [python.org](https://www.python.org/downloads/)
  - Ensure `pip` is installed

- **Git**
  - Download from [git-scm.com](https://git-scm.com/)

### Optional but Recommended
- **IntelliJ IDEA** or **Android Studio** for Android/Java development
- **VS Code** for cross-platform development
- **PyCharm** for Python development

## Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd fyp-gsr-unified-buccancs
```

### 2. Set Up Python Environment
Run the automated setup script:
```bash
python3 setup_python_env.py
```

This will:
- Create a virtual environment in `venv/`
- Install all Python dependencies
- Set up the Windows controller environment

### 3. Set Up Android Environment
Navigate to the Android directory and build:
```bash
cd android
./gradlew assembleDebug
```

## IDE-Specific Setup

### IntelliJ IDEA / Android Studio

The repository includes pre-configured run configurations in `.idea/runConfigurations/`:

#### Available Configurations:
- **Android App - Build**: Builds the debug version of the Android app
- **Android App - Test**: Runs unit and instrumentation tests
- **Android App - Install**: Installs the app on a connected device
- **Android App - Clean**: Cleans the Android build
- **Windows Controller - Main**: Runs the Python Windows controller
- **Windows Controller - Tests**: Runs Python tests
- **Shared Java - Compile**: Compiles shared Java components

#### Setup Steps:
1. Open the project root directory in IntelliJ IDEA/Android Studio
2. Wait for Gradle sync to complete
3. Configure Python SDK:
   - Go to File → Project Structure → SDKs
   - Add Python SDK pointing to `venv/bin/python` (or `venv/Scripts/python.exe` on Windows)
4. Run configurations will be automatically available in the Run/Debug dropdown

### VS Code

The repository includes comprehensive VS Code configurations:

#### Available Configurations:
- **Launch Configurations** (`.vscode/launch.json`):
  - Windows Controller - Main
  - Windows Controller - Debug
  - Python: Current File
  - Python: Test Current File
  - Android: Attach Debugger

- **Build Tasks** (`.vscode/tasks.json`):
  - Android: Build Debug/Release
  - Android: Run Tests
  - Android: Clean
  - Python: Setup Environment
  - Python: Install Dependencies
  - Python: Run Tests
  - Java: Compile Shared Components

#### Setup Steps:
1. Open the project root directory in VS Code
2. Install recommended extensions (VS Code will prompt you)
3. Set up Python interpreter:
   - Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on macOS)
   - Type "Python: Select Interpreter"
   - Choose the interpreter in `./venv/bin/python`
4. Use `Ctrl+Shift+P` → "Tasks: Run Task" to access build tasks
5. Use `F5` or Run and Debug panel for debugging

## Component-Specific Setup

### Android App

#### Dependencies:
- **Android SDK 34** (Android 14) - Updated from SDK 33
- **Kotlin 2.0.0** - Updated from 1.8.0 for enhanced performance
- **CameraX 1.3.1** - Updated from 1.2.2 with enhanced capabilities
- **Java 24** - Updated from Java 1.8 for latest language features
- **Gradle 8.5** - Updated build system
- Various sensor and networking libraries (see `android/app/build.gradle`)

#### Build Commands:
```bash
cd android
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew test                   # Run unit tests
./gradlew connectedAndroidTest   # Run instrumentation tests
./gradlew installDebug           # Install on connected device
./gradlew clean                  # Clean build
```

#### Key Features:
- **RGB video capture** with 1080p/30fps recording
- **Raw RGB image capture** - NEW: Frame-by-frame capture before ISP processing with nanosecond timestamps
- **Thermal camera capture** with frame-by-frame saving and timestamp naming
- **GSR sensor integration** (Shimmer3 GSR+) with 128Hz sampling
- **Audio recording** with synchronized timestamps
- **Network communication** with Windows controller
- **Real-time data synchronization** with nanosecond precision
- **Unified recording control** for all data streams simultaneously

### Windows Controller

#### Dependencies:
- Python 3.8+
- **PySide6 6.6.1** for modern GUI framework (official Qt binding)
- OpenCV for video processing
- pyqtgraph for real-time plotting (PySide6 compatible)
- Zeroconf for device discovery
- Various sensor integration libraries (see `windows_controller/requirements.txt`)

#### Setup Commands:
```bash
# Activate virtual environment
source venv/bin/activate  # On macOS/Linux
# or
venv\Scripts\activate     # On Windows

# Install dependencies (includes PySide6)
pip install -r windows_controller/requirements.txt

# Run the application with modern GUI
python windows_controller/src/main/main.py

# Test the modern GUI interface
python windows_controller/test_modern_gui.py

# Run tests
python -m pytest windows_controller/tests/ -v
```

#### Key Features:
- **Modern PySide6 GUI** with professional styling and enhanced usability
- **Enhanced Interface**: Blue/gray color scheme, rounded corners, emoji buttons
- **Responsive Design**: Optimized for 1400x900 resolution with better scaling
- Multi-device management with real-time status indicators
- Advanced data visualization and plotting capabilities
- Comprehensive session management with metadata generation
- Integration with LSL, PsychoPy, and Shimmer sensors
- Network server for Android devices with Zeroconf discovery
- Video playback and annotation system for research analysis

### Shared Java Components

#### Purpose:
- Common data models (`DataEvent`, `SessionInfo`)
- Utility classes for data formatting, alignment, and synchronization
- Network protocol definitions

#### Compilation:
```bash
# Create build directory
mkdir -p build/classes

# Compile shared components
javac -d build/classes -cp . shared/models/*.java shared/utils/*.java src/common/network/*.java
```

## Environment Variables

Set the following environment variables for optimal development experience:

### macOS/Linux:
```bash
export JAVA_HOME=/path/to/your/jdk
export ANDROID_HOME=/path/to/your/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

### Windows:
```cmd
set JAVA_HOME=C:\path\to\your\jdk
set ANDROID_HOME=C:\path\to\your\android-sdk
set PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
```

## Testing

### Android Tests:
```bash
cd android
./gradlew test                    # Unit tests
./gradlew connectedAndroidTest    # Instrumentation tests (requires device)
```

### Python Tests:
```bash
cd windows_controller
python -m pytest tests/ -v       # All tests
python -m pytest tests/test_session_manager.py -v  # Specific test file
```

### Java Tests:
```bash
# Compile and run tests for shared components
javac -d build/test-classes -cp build/classes:. shared/test/*.java
java -cp build/classes:build/test-classes org.junit.runner.JUnitCore TestSuite
```

## Troubleshooting

### Common Issues:

1. **Gradle Build Fails**:
   - Ensure `JAVA_HOME` is set correctly
   - Check Android SDK installation
   - Run `./gradlew clean` and try again

2. **Python Import Errors**:
   - Ensure virtual environment is activated
   - Run `pip install -r windows_controller/requirements.txt`
   - Check `PYTHONPATH` includes `windows_controller/src`

3. **Android Device Not Detected**:
   - Enable USB debugging on device
   - Install appropriate USB drivers
   - Run `adb devices` to verify connection

4. **VS Code Python Interpreter Issues**:
   - Manually select interpreter: `Ctrl+Shift+P` → "Python: Select Interpreter"
   - Choose `./venv/bin/python` or `./venv/Scripts/python.exe`

5. **IntelliJ IDEA Gradle Sync Issues**:
   - File → Invalidate Caches and Restart
   - Reimport Gradle project
   - Check Gradle JVM settings

## Development Workflow

### Typical Development Session:

1. **Start Development**:
   ```bash
   # Activate Python environment
   source venv/bin/activate

   # Pull latest changes
   git pull origin main
   ```

2. **Android Development**:
   ```bash
   cd android
   ./gradlew assembleDebug
   ./gradlew installDebug  # Install on device
   ```

3. **Python Development**:
   ```bash
   # Run Windows controller
   python windows_controller/src/main/main.py

   # Run tests
   python -m pytest windows_controller/tests/ -v
   ```

4. **Testing Integration**:
   - Start Windows controller
   - Install and run Android app
   - Test communication between components

## Additional Resources

- [Android Developer Documentation](https://developer.android.com/docs)
- [Python Virtual Environments](https://docs.python.org/3/tutorial/venv.html)
- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)
- [VS Code Python Tutorial](https://code.visualstudio.com/docs/python/python-tutorial)
- [IntelliJ IDEA Documentation](https://www.jetbrains.com/help/idea/)

## Contributing

1. Follow the existing code style and conventions
2. Run tests before submitting changes
3. Update documentation for new features
4. Use the provided run/debug configurations for consistency

For questions or issues, please refer to the project documentation or create an issue in the repository.
