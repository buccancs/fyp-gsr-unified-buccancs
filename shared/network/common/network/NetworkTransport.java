package com.buccancs.gsr.common.network;

import java.io.IOException;

/**
 * NetworkTransport defines the interface for bi-directional communication
 * between devices. This abstraction allows for different transport implementations
 * (TCP/IP, Bluetooth, etc.) while maintaining a consistent API.
 */
public interface NetworkTransport {
    
    /**
     * Listener interface for network events.
     */
    interface Listener {
        /**
         * Called when a connection is established.
         * 
         * @param deviceId The ID of the connected device
         */
        void onConnected(String deviceId);
        
        /**
         * Called when a connection is lost.
         * 
         * @param deviceId The ID of the disconnected device
         * @param reason The reason for disconnection
         */
        void onDisconnected(String deviceId, String reason);
        
        /**
         * Called when a message is received.
         * 
         * @param message The received message
         */
        void onMessageReceived(CommandProtocol.Message message);
        
        /**
         * Called when an error occurs.
         * 
         * @param deviceId The ID of the device where the error occurred
         * @param error The error message
         * @param exception The exception that caused the error, if any
         */
        void onError(String deviceId, String error, Exception exception);
    }
    
    /**
     * Initializes the transport layer.
     * 
     * @param deviceId The unique identifier for this device
     * @param listener The listener for network events
     * @throws IOException If initialization fails
     */
    void initialize(String deviceId, Listener listener) throws IOException;
    
    /**
     * Starts the transport layer.
     * For servers, this begins listening for connections.
     * For clients, this attempts to connect to the server.
     * 
     * @throws IOException If starting fails
     */
    void start() throws IOException;
    
    /**
     * Stops the transport layer.
     * This closes all connections and releases resources.
     */
    void stop();
    
    /**
     * Sends a message to a specific device.
     * 
     * @param message The message to send
     * @param targetDeviceId The ID of the target device
     * @return true if the message was sent successfully, false otherwise
     * @throws IOException If sending fails
     */
    boolean sendMessage(CommandProtocol.Message message, String targetDeviceId) throws IOException;
    
    /**
     * Broadcasts a message to all connected devices.
     * 
     * @param message The message to broadcast
     * @return The number of devices the message was sent to
     * @throws IOException If broadcasting fails
     */
    int broadcastMessage(CommandProtocol.Message message) throws IOException;
    
    /**
     * Gets the connection status for a specific device.
     * 
     * @param deviceId The ID of the device to check
     * @return true if connected, false otherwise
     */
    boolean isConnected(String deviceId);
    
    /**
     * Gets the IDs of all connected devices.
     * 
     * @return An array of device IDs
     */
    String[] getConnectedDevices();
    
    /**
     * Gets the transport type (e.g., "TCP", "Bluetooth").
     * 
     * @return The transport type
     */
    String getTransportType();
}