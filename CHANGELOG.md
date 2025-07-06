# Changelog

All notable changes to the GSR & Dual-Video Recording System project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Complete Networking Layer Implementation**: Fully functional device communication system
  - Real device discovery using Zeroconf/mDNS (no longer simulated)
  - Live GSR/PPG data streaming from Android to PC controller
  - Automatic file transfer capabilities after recording sessions
  - Complete command protocol with binary serialization/deserialization
  - Functional handshake protocol with ACK/NACK responses
  - Periodic time synchronization with drift detection and correction
  - Connection monitoring with heartbeat and automatic recovery
  - Device status reporting with battery, storage, and stream information

### Fixed
- **Documentation Updates**: Corrected outdated information about missing features
  - Updated README.md to reflect current implementation status
  - Revised missing_features.txt to show completed implementations
  - Updated priority roadmaps and limitation sections

## [2.0.0] - 2024-12-XX

### Added
- **Raw RGB Image Capture**: Frame-by-frame capture before ISP processing using CameraX ImageCapture
  - ~30 FPS capture rate with nanosecond timestamp naming
  - Organized storage in dedicated subdirectories
  - Seamless integration with unified recording control
- **Comprehensive Documentation**: Complete rewrite and enhancement of all documentation
  - Professional-grade documentation suitable for research and commercial use
  - Enhanced visual organization with emojis and clear structure
  - Comprehensive troubleshooting and setup guides
- **Modern PySide6 GUI Framework**: Upgraded PC controller interface
  - Professional flat design with blue/gray color scheme
  - Enhanced buttons with emojis and hover effects
  - Cross-platform support (Windows, macOS, Linux)
  - High-DPI support for crisp rendering

### Changed
- **Java Version**: Upgraded from Java 1.8 to Java 24
  - Access to latest language features and performance improvements
  - Better tooling support and security enhancements
- **Kotlin Version**: Upgraded from 1.8.0 to 2.0.0
  - Enhanced compiler performance and new language features
  - Kotlin Multiplatform improvements
- **Android SDK**: Upgraded from API 33 to API 34 (Android 14)
  - Access to latest Android features and security controls
  - Improved performance optimizations
- **Gradle**: Updated to version 8.14 for better performance
- **AndroidX Libraries**: Updated all libraries to latest versions
  - androidx.core:core-ktx: 1.9.0 → 1.12.0
  - androidx.appcompat:appcompat: 1.6.1 → 1.7.0
  - com.google.android.material:material: 1.8.0 → 1.11.0
  - androidx.constraintlayout:constraintlayout: 2.1.4 → 2.2.0
  - androidx.lifecycle libraries: 2.5.1 → 2.7.0
- **CameraX**: Updated from 1.2.2 to 1.3.1
- **Third-Party Dependencies**:
  - no.nordicsemi.android:ble: 2.5.1 → 2.7.4
  - usb-serial-for-android: 3.4.6 → 3.7.0
  - com.squareup.okhttp3:okhttp: 4.10.0 → 4.12.0
  - kotlinx-coroutines: 1.6.4 → 1.7.3

### Improved
- **Development Environment**: Self-sufficient repository with comprehensive IDE configurations
  - IntelliJ IDEA/Android Studio run configurations
  - VS Code launch and task configurations
  - Automated Python environment setup script
- **Build System**: Optimized Gradle settings with parallel builds and caching
- **Documentation Quality**: Enhanced technical accuracy and user experience
- **Testing Framework**: Professional-grade testing procedures and automation

### Technical Specifications
- **Timestamp Precision**: Nanosecond-level timing using SystemClock.elapsedRealtimeNanos()
- **File Organization**: Structured directory layout for all data types
- **Synchronization**: Enhanced multi-device coordination
- **Compatibility**: Maintained backward compatibility while adding new features

### Build Verification
- ✅ All dependencies are compatible
- ✅ No compilation errors
- ✅ No compatibility issues detected
- ✅ Successful build on Java 24, Kotlin 2.0, Android SDK 34

### Compatibility Notes
- **Minimum SDK**: Still supports Android 7.0 (API 24)
- **Target SDK**: Now targets Android 14 (API 34)
- **Java**: Now uses Java 24 features
- **Kotlin**: Uses Kotlin 2.0.0 features

## [1.0.0] - 2024-XX-XX

### Added
- Initial release of GSR & Dual-Video Recording System
- Android capture app with multi-modal recording capabilities
- PC controller with device management
- GSR sensor integration (Shimmer3 GSR+)
- Thermal camera support (Topdon TC001)
- RGB video recording with CameraX
- Audio recording capabilities
- Network synchronization between devices
- Real-time monitoring and control

### Features
- **Multi-Modal Recording**: RGB video, thermal imaging, GSR sensors, and audio
- **Real-Time Synchronization**: Sub-millisecond timing accuracy across devices
- **Cross-Platform**: Android app + PC controller
- **Professional Interface**: Modern GUI with real-time monitoring
- **Research-Ready**: Comprehensive data export and analysis tools

---

**Note**: This changelog consolidates information from previous meta-documents including COMPREHENSIVE_DOCUMENTATION_UPDATE.md, DOCUMENTATION_UPDATE_SUMMARY.md, UPGRADE_SUMMARY.md, CONFIGURATION_SUMMARY.md, and IMPLEMENTATION_SUMMARY.md, which have been removed to eliminate duplication.
