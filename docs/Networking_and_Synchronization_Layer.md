# Networking and Synchronization Layer

This document describes the implementation of the networking and synchronization layer for the GSR & Dual-Video Recording System. It provides bi-directional communication between the PC controller and Android devices, time synchronization, and error detection/recovery mechanisms.

## Architecture Overview

The networking layer is designed with the following principles:

1. **Transport Abstraction**: The `NetworkTransport` interface abstracts the underlying transport mechanism (TCP/IP, Bluetooth, etc.), allowing for different implementations without changing the higher-level code.

2. **Command Protocol**: A well-defined set of commands and message formats for control and synchronization.

3. **Time Synchronization**: An NTP-like mechanism for aligning timestamps between devices.

4. **Error Detection and Recovery**: Heartbeat-based connection monitoring and automatic reconnection.

5. **Multi-Device Scalability**: Support for multiple Android devices connecting to a single PC controller.

## Key Components

### Common Components (shared between PC and Android)

- **CommandProtocol**: Defines the command types, status codes, and message formats used for communication.
- **NetworkTransport**: Interface for bi-directional communication, implemented by different transport mechanisms.
- **TimeSync**: Implements time synchronization between devices using an NTP-like algorithm.
- **SyncMarker**: Provides utilities for creating and handling synchronization markers during recording.
- **ConnectionMonitor**: Monitors connection health and handles error detection/recovery.

### Windows PC Components

- **TcpServerTransport**: Implements the NetworkTransport interface using TCP/IP sockets for the PC server.
- **NetworkManager**: Coordinates multiple device connections and ensures commands are properly distributed.

### Android Components

- **TcpClientTransport**: Implements the NetworkTransport interface using TCP/IP sockets for the Android client.

## Command Protocol

The command protocol defines the following message types:

1. **Control Commands**:
   - `CMD_START`: Start recording on device
   - `CMD_STOP`: Stop recording on device
   - `CMD_STATUS`: Query device status

2. **Synchronization Commands**:
   - `SYNC_PING`: Time synchronization ping
   - `SYNC_PONG`: Time synchronization response
   - `SYNC_MARKER`: Synchronization event marker

3. **Connection Management**:
   - `CONNECT`: Initial connection request
   - `DISCONNECT`: Graceful disconnection
   - `HEARTBEAT`: Keep-alive message

4. **Error and Acknowledgment**:
   - `ACK`: Acknowledge receipt of command
   - `NACK`: Negative acknowledgment
   - `ERROR`: Error notification

## Time Synchronization

Time synchronization is implemented using a simplified NTP-like algorithm:

1. The PC server sends a `SYNC_PING` message with its current timestamp (t0).
2. The Android client receives the ping, records its local time (t1), and sends a `SYNC_PONG` response with both timestamps.
3. The PC receives the pong and records its local time (t3).
4. The clock offset is calculated as: `offset = ((t1 - t0) + (t2 - t3)) / 2`
5. The round-trip time is calculated as: `rtt = (t3 - t0) - (t2 - t1)`

This allows timestamps from different devices to be converted to a common time base.

## Error Detection and Recovery

Error detection and recovery is implemented using:

1. **Heartbeat Messages**: Periodic messages to verify connection health.
2. **Connection Monitoring**: Tracking of last heartbeat times and detection of timeouts.
3. **Automatic Reconnection**: Attempts to reconnect if a connection is lost.
4. **Error Notification**: Notification of connection issues to higher-level components.

## Multi-Device Scalability

The PC server can handle multiple Android client connections simultaneously:

1. Each client has a unique device ID.
2. The server maintains separate connections and synchronization state for each client.
3. Commands can be sent to individual devices or broadcast to all connected devices.
4. Synchronization is performed with each device independently.

## Usage Examples

### PC Server

```java
// Create a TCP server transport
NetworkTransport transport = new TcpServerTransport(8080);

// Create a network manager
NetworkManager networkManager = new NetworkManager("server", transport, new NetworkManager.Listener() {
    @Override
    public void onDeviceConnected(String deviceId) {
        System.out.println("Device connected: " + deviceId);
    }
    
    // Implement other listener methods...
});

// Start the network manager
networkManager.start();

// Send a command to a specific device
networkManager.sendCommand(CommandProtocol.CommandType.CMD_START, "device1", "session123");

// Broadcast a command to all connected devices
networkManager.broadcastCommand(CommandProtocol.CommandType.CMD_START, "session123");

// Synchronize with all devices
networkManager.syncWithAllDevices();
```

### Android Client

```java
// Create a TCP client transport
NetworkTransport transport = new TcpClientTransport("192.168.1.100", 8080);

// Initialize the transport
transport.initialize("device1", new NetworkTransport.Listener() {
    @Override
    public void onConnected(String deviceId) {
        System.out.println("Connected to server");
    }
    
    @Override
    public void onMessageReceived(CommandProtocol.Message message) {
        // Handle received messages
        if (message.getType() == CommandProtocol.CommandType.CMD_START) {
            // Start recording
        }
    }
    
    // Implement other listener methods...
});

// Start the transport
transport.start();

// Send a status update to the server
CommandProtocol.ResponseMessage statusMessage = new CommandProtocol.ResponseMessage(
    CommandProtocol.CommandType.CMD_STATUS,
    "device1",
    CommandProtocol.StatusCode.OK,
    "Status update",
    "100%", // Battery level
    "5GB",  // Storage remaining
    "rgb:true,thermal:true,gsr:true" // Active streams
);
transport.sendMessage(statusMessage, "server");
```

## Implementation Notes

- The current implementation uses TCP/IP sockets for communication, but the architecture allows for other transport mechanisms (e.g., Bluetooth) to be added in the future.
- Serialization of messages is currently a placeholder and would need to be implemented with a proper serialization format (e.g., JSON, Protocol Buffers).
- Error handling and recovery mechanisms are designed to be robust, but real-world testing is needed to verify their effectiveness.
- Time synchronization accuracy depends on network conditions and should be monitored during operation.

## Requirements Satisfied

This implementation satisfies the following requirements from the project specification:

1. **Bi-Directional Communication Channel (Core)** - Implemented using TCP/IP sockets with the NetworkTransport interface.
2. **Command Protocol (Core)** - Defined in the CommandProtocol class with various command types and message formats.
3. **Time Synchronization & Clock Alignment (Core)** - Implemented using an NTP-like algorithm in the TimeSync class.
4. **Synchronization Events & Markers (Core)** - Provided by the SyncMarker class for verification of synchronization during recording.
5. **Error Detection and Recovery (Core)** - Implemented in the ConnectionMonitor class with heartbeat-based monitoring.
6. **Multi-Device Scalability (Core)** - Supported by the NetworkManager class on the PC side and the transport abstraction.
7. **Transport Flexibility (Extended)** - Enabled by the NetworkTransport interface, allowing different transport implementations.

## Future Enhancements

Potential future enhancements to the networking layer include:

1. **Bluetooth Transport Implementation** - Adding a Bluetooth transport implementation for scenarios where Wi-Fi is not available.
2. **Improved Serialization** - Implementing a more efficient serialization format (e.g., Protocol Buffers) for message exchange.
3. **Enhanced Security** - Adding authentication and encryption for secure communication.
4. **Adaptive Synchronization** - Implementing more sophisticated synchronization algorithms that adapt to changing network conditions.
5. **Performance Optimization** - Optimizing the networking layer for high-throughput scenarios (e.g., video streaming).