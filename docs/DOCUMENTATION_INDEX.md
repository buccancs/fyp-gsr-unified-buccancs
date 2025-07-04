# Documentation Index - GSR & Dual-Video Recording System

Welcome to the comprehensive documentation for the GSR & Dual-Video Recording System. This index will help you find the right documentation for your needs.

## 📚 Documentation Overview

This system provides multiple levels of documentation to serve different user types and use cases:

### 🚀 Getting Started
- **[Setup and Connection Guide](SETUP_AND_CONNECTION_GUIDE.md)** - Complete hardware setup and connection instructions
- **[App Usage Guide](APP_USAGE_GUIDE.md)** - Step-by-step user guide for both Android app and PC controller

### 🔧 Technical Documentation
- **[Python API Guide](PYTHON_API_GUIDE.md)** - Comprehensive API reference for programmatic control
- **[Development Setup Guide](../DEVELOPMENT_SETUP.md)** - Development environment configuration
- **[Networking and Synchronization](Networking_and_Synchronization_Layer.md)** - Technical implementation details

### 📖 Reference Documentation
- **[Main README](../README.md)** - System overview, features, and technical specifications
- **[Changelog](../CHANGELOG.md)** - Version history and major changes
- **[Windows Controller README](../windows_controller/README.md)** - PC controller specific documentation

## 🎯 Choose Your Path

### I'm New to the System
**Start Here**: [Setup and Connection Guide](SETUP_AND_CONNECTION_GUIDE.md)

This guide will walk you through:
- Hardware requirements and setup
- Software installation
- Network configuration
- First recording session
- Troubleshooting common issues

### I Want to Use the Apps
**Go To**: [App Usage Guide](APP_USAGE_GUIDE.md)

This guide covers:
- Android app interface and controls
- PC controller features and workflows
- Recording processes and data management
- Advanced features and best practices

### I Want to Control the System Programmatically
**Go To**: [Python API Guide](PYTHON_API_GUIDE.md)

This guide provides:
- Complete API reference
- Code examples and integration patterns
- Advanced automation techniques
- Integration with research tools (PsychoPy, LSL)

### I'm a Developer
**Start With**: [Development Setup Guide](../DEVELOPMENT_SETUP.md)

Then explore:
- [Networking and Synchronization](Networking_and_Synchronization_Layer.md)
- [Windows Controller README](../windows_controller/README.md)
- Source code in `android/` and `windows_controller/` directories

## 📋 Quick Reference

### Common Tasks

| Task | Documentation |
|------|---------------|
| First-time setup | [Setup and Connection Guide](SETUP_AND_CONNECTION_GUIDE.md) |
| Recording a session | [App Usage Guide](APP_USAGE_GUIDE.md#recording-workflows) |
| Connecting GSR sensor | [Setup Guide - Hardware Setup](SETUP_AND_CONNECTION_GUIDE.md#hardware-setup) |
| Troubleshooting connections | [Setup Guide - Troubleshooting](SETUP_AND_CONNECTION_GUIDE.md#troubleshooting) |
| Using Python API | [Python API Guide](PYTHON_API_GUIDE.md#getting-started) |
| Multi-device recording | [App Usage Guide](APP_USAGE_GUIDE.md#multi-device-recording) |
| Data export and analysis | [App Usage Guide](APP_USAGE_GUIDE.md#data-management) |
| Development environment | [Development Setup Guide](../DEVELOPMENT_SETUP.md) |

### Hardware-Specific Guides

| Hardware Component | Setup Instructions | Troubleshooting |
|-------------------|-------------------|-----------------|
| Shimmer3 GSR+ Sensor | [Hardware Setup - GSR Sensor](SETUP_AND_CONNECTION_GUIDE.md#step-1-shimmer3-gsr-sensor-setup) | [GSR Sensor Won't Connect](APP_USAGE_GUIDE.md#gsr-sensor-wont-connect) |
| Topdon TC001 Thermal Camera | [Hardware Setup - Thermal Camera](SETUP_AND_CONNECTION_GUIDE.md#step-2-topdon-tc001-thermal-camera-setup) | [Thermal Camera Not Detected](APP_USAGE_GUIDE.md#thermal-camera-not-detected) |
| Android Device | [Hardware Setup - Android Device](SETUP_AND_CONNECTION_GUIDE.md#step-3-android-device-preparation) | [Device Connection Issues](APP_USAGE_GUIDE.md#common-issues) |

### Software Components

| Component | User Guide | API Reference | Development |
|-----------|------------|---------------|-------------|
| Android App | [Android App Usage](APP_USAGE_GUIDE.md#android-app-usage) | N/A | [Development Setup](../DEVELOPMENT_SETUP.md#android-app) |
| PC Controller | [PC Controller Usage](APP_USAGE_GUIDE.md#pc-controller-usage) | [Python API Guide](PYTHON_API_GUIDE.md) | [Development Setup](../DEVELOPMENT_SETUP.md#windows-controller) |
| Network Layer | [Network Configuration](SETUP_AND_CONNECTION_GUIDE.md#network-configuration) | [Device Management API](PYTHON_API_GUIDE.md#device-management) | [Networking Documentation](Networking_and_Synchronization_Layer.md) |

## 🔍 Documentation by User Type

### 🧑‍🔬 Researchers and End Users
**Primary Documentation**:
1. [Setup and Connection Guide](SETUP_AND_CONNECTION_GUIDE.md) - Essential for first-time setup
2. [App Usage Guide](APP_USAGE_GUIDE.md) - Daily usage and workflows

**When You Need More**:
- [Python API Guide](PYTHON_API_GUIDE.md) - For automation and integration
- [Main README](../README.md) - For system overview and capabilities

### 👨‍💻 Developers and Integrators
**Primary Documentation**:
1. [Development Setup Guide](../DEVELOPMENT_SETUP.md) - Development environment
2. [Python API Guide](PYTHON_API_GUIDE.md) - Programmatic control
3. [Networking and Synchronization](Networking_and_Synchronization_Layer.md) - Technical details

**Reference Materials**:
- [Setup and Connection Guide](SETUP_AND_CONNECTION_GUIDE.md) - Understanding system architecture
- [App Usage Guide](APP_USAGE_GUIDE.md) - Understanding user workflows

### 🔧 System Administrators
**Primary Documentation**:
1. [Setup and Connection Guide](SETUP_AND_CONNECTION_GUIDE.md) - Network and system setup
2. [Development Setup Guide](../DEVELOPMENT_SETUP.md) - Installation and configuration

**Troubleshooting Resources**:
- [App Usage Guide - Troubleshooting](APP_USAGE_GUIDE.md#troubleshooting)
- [Setup Guide - Troubleshooting](SETUP_AND_CONNECTION_GUIDE.md#troubleshooting)

### 📊 Data Analysts
**Primary Documentation**:
1. [App Usage Guide - Data Management](APP_USAGE_GUIDE.md#data-management) - Data formats and export
2. [Python API Guide - Data Analysis](PYTHON_API_GUIDE.md#custom-data-analysis-integration) - Programmatic analysis

**Integration Guides**:
- [Python API Guide - Integration Examples](PYTHON_API_GUIDE.md#integration-examples) - MATLAB, Python, R integration

## 🆘 Getting Help

### Documentation Issues
If you find errors or gaps in the documentation:
1. Check if the information exists in another guide
2. Search the [main README](../README.md) for additional details
3. Create an issue on the GitHub repository

### Technical Support
For technical issues:
1. **First**: Check the troubleshooting sections in relevant guides
2. **Then**: Review log files and error messages
3. **Finally**: Create a detailed issue report with:
   - Your setup configuration
   - Steps to reproduce the problem
   - Log files and error messages
   - Expected vs. actual behavior

### Feature Requests
For new features or improvements:
1. Review the [main README](../README.md) for planned features
2. Check existing GitHub issues for similar requests
3. Create a new feature request with detailed requirements

## 📝 Documentation Maintenance

### Keeping Documentation Current
This documentation is maintained alongside the codebase. When using the system:
- Report any outdated information
- Suggest improvements for clarity
- Share common use cases not covered

### Contributing to Documentation
To contribute to the documentation:
1. Follow the existing structure and style
2. Include practical examples and code snippets
3. Test all instructions before submitting
4. Update this index when adding new documentation

## 🔗 External Resources

### Hardware Documentation
- [Shimmer3 GSR+ User Guide](https://shimmersensing.com/support/wireless-sensor-networks-documentation/)
- [Topdon TC001 Manual](https://www.topdon.com/products/tc001)
- [Android Developer Documentation](https://developer.android.com/docs)

### Software Dependencies
- [PySide6 Documentation](https://doc.qt.io/qtforpython/)
- [OpenCV Documentation](https://docs.opencv.org/)
- [PsychoPy Documentation](https://www.psychopy.org/documentation.html)
- [Lab Streaming Layer](https://labstreaminglayer.readthedocs.io/)

### Research and Academic Resources
- [GSR Signal Processing Guidelines](https://www.biopac.com/knowledge-base/gsr-eda-analysis/)
- [Multimodal Data Collection Best Practices](https://www.frontiersin.org/articles/10.3389/fpsyg.2019.01849/full)

---

**Last Updated**: December 2024  
**Version**: 1.0  
**Maintainer**: GSR & Dual-Video Recording System Team

For the most current information, always refer to the latest version of the documentation in the repository.
