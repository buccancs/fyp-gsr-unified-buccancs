# Common Network Components

This directory contains the shared networking components used by both the Android app and PC controller for communication and synchronization.

## Components

### CommandProtocol.java
Defines the command types, status codes, and message formats used for communication between the PC controller and Android devices.

**Key Features:**
- Command types (CMD_START, CMD_STOP, CMD_STATUS, etc.)
- Status codes for responses
- Message serialization/deserialization
- Support for all data streams including raw RGB image capture

### NetworkTransport.java
Abstract interface for bi-directional communication that can be implemented by different transport mechanisms (TCP/IP, Bluetooth, etc.).

**Key Features:**
- Transport abstraction layer
- Message sending/receiving interface
- Connection lifecycle management
- Error handling callbacks

### TimeSync.java
Implements time synchronization between devices using an NTP-like algorithm for precise timestamp alignment.

**Key Features:**
- Clock offset calculation
- Round-trip time measurement
- Nanosecond precision synchronization
- Support for multiple device synchronization

### SyncMarker.java
Provides utilities for creating and handling synchronization markers during recording sessions.

**Key Features:**
- Synchronization event markers
- Timestamp verification
- Multi-device sync validation
- Recording session alignment

### ConnectionMonitor.java
Monitors connection health and handles error detection/recovery mechanisms.

**Key Features:**
- Heartbeat-based monitoring
- Connection timeout detection
- Automatic reconnection attempts
- Error notification system

## Usage

These components are designed to be used by both the Android app and PC controller to ensure consistent communication protocols and synchronization mechanisms across the entire system.

### Integration with Android App
The Android app uses these components through the NetworkClient implementation to:
- Receive recording commands from PC controller
- Send status updates and device information
- Participate in time synchronization
- Handle connection monitoring

### Integration with PC Controller
The PC controller uses these components to:
- Send commands to multiple Android devices
- Collect status information from devices
- Coordinate time synchronization across all devices
- Monitor connection health and handle errors

## Recent Updates

- Added support for raw RGB image capture status reporting
- Enhanced synchronization for nanosecond-precision timestamps
- Improved error handling and recovery mechanisms
- Updated for compatibility with Java 24 and latest Android SDK