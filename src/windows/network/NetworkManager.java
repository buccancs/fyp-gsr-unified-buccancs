package com.buccancs.gsr.windows.network;

import com.buccancs.gsr.common.network.CommandProtocol;
import com.buccancs.gsr.common.network.ConnectionMonitor;
import com.buccancs.gsr.common.network.NetworkTransport;
import com.buccancs.gsr.common.network.TimeSync;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * NetworkManager coordinates multiple device connections and ensures commands
 * are properly distributed to all connected devices. It handles the multi-device
 * scalability requirement of the system.
 */
public class NetworkManager implements NetworkTransport.Listener, ConnectionMonitor.Listener {
    
    /**
     * Listener interface for network manager events.
     */
    public interface Listener {
        /**
         * Called when a device connects.
         * 
         * @param deviceId The ID of the connected device
         */
        void onDeviceConnected(String deviceId);
        
        /**
         * Called when a device disconnects.
         * 
         * @param deviceId The ID of the disconnected device
         * @param reason The reason for disconnection
         */
        void onDeviceDisconnected(String deviceId, String reason);
        
        /**
         * Called when a message is received from a device.
         * 
         * @param message The received message
         * @param deviceId The ID of the device that sent the message
         */
        void onMessageReceived(CommandProtocol.Message message, String deviceId);
        
        /**
         * Called when a synchronization event occurs.
         * 
         * @param deviceId The ID of the device
         * @param offset The calculated clock offset in milliseconds
         * @param roundTripTime The measured round-trip time in milliseconds
         */
        void onSyncEvent(String deviceId, long offset, long roundTripTime);
        
        /**
         * Called when an error occurs.
         * 
         * @param deviceId The ID of the device where the error occurred
         * @param error The error message
         * @param exception The exception that caused the error, if any
         */
        void onError(String deviceId, String error, Exception exception);
    }
    
    private final String serverId;
    private final NetworkTransport transport;
    private final Listener listener;
    private final ExecutorService executorService;
    private final ConnectionMonitor connectionMonitor;
    
    // Map of device IDs to their TimeSync instances
    private final Map<String, TimeSync> timeSyncs = new ConcurrentHashMap<>();
    
    // Map of device IDs to their last known status
    private final Map<String, DeviceStatus> deviceStatuses = new ConcurrentHashMap<>();
    
    // Flag to indicate if the manager is running
    private boolean running;
    
    /**
     * Creates a new NetworkManager.
     * 
     * @param serverId The ID of this server
     * @param transport The network transport to use
     * @param listener The listener for network manager events
     */
    public NetworkManager(String serverId, NetworkTransport transport, Listener listener) {
        this.serverId = serverId;
        this.transport = transport;
        this.listener = listener;
        this.executorService = Executors.newCachedThreadPool();
        this.connectionMonitor = new ConnectionMonitor(transport, serverId, this);
    }
    
    /**
     * Starts the network manager.
     * 
     * @throws IOException If starting fails
     */
    public void start() throws IOException {
        if (running) {
            return;
        }
        
        running = true;
        
        // Initialize and start the transport
        transport.initialize(serverId, this);
        transport.start();
        
        // Start the connection monitor
        connectionMonitor.start();
        
        System.out.println("Network manager started");
    }
    
    /**
     * Stops the network manager.
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        
        // Stop the connection monitor
        connectionMonitor.stop();
        
        // Stop the transport
        transport.stop();
        
        // Shutdown the executor service
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        // Clear all maps
        timeSyncs.clear();
        deviceStatuses.clear();
        
        System.out.println("Network manager stopped");
    }
    
    /**
     * Sends a command to a specific device.
     * 
     * @param command The command type
     * @param deviceId The ID of the target device
     * @param sessionId The current session ID
     * @param parameters Additional command parameters
     * @return true if the command was sent successfully, false otherwise
     * @throws IOException If sending fails
     */
    public boolean sendCommand(CommandProtocol.CommandType command, String deviceId, String sessionId, String... parameters) throws IOException {
        CommandProtocol.CommandMessage message = new CommandProtocol.CommandMessage(command, serverId, sessionId, parameters);
        return transport.sendMessage(message, deviceId);
    }
    
    /**
     * Broadcasts a command to all connected devices.
     * 
     * @param command The command type
     * @param sessionId The current session ID
     * @param parameters Additional command parameters
     * @return The number of devices the command was sent to
     * @throws IOException If broadcasting fails
     */
    public int broadcastCommand(CommandProtocol.CommandType command, String sessionId, String... parameters) throws IOException {
        CommandProtocol.CommandMessage message = new CommandProtocol.CommandMessage(command, serverId, sessionId, parameters);
        return transport.broadcastMessage(message);
    }
    
    /**
     * Initiates time synchronization with a specific device.
     * 
     * @param deviceId The ID of the device to synchronize with
     * @throws IOException If sending the sync ping fails
     */
    public void syncWithDevice(String deviceId) throws IOException {
        TimeSync timeSync = getOrCreateTimeSync(deviceId);
        CommandProtocol.SyncMessage pingMessage = timeSync.createSyncPing();
        transport.sendMessage(pingMessage, deviceId);
    }
    
    /**
     * Initiates time synchronization with all connected devices.
     * 
     * @throws IOException If sending the sync pings fails
     */
    public void syncWithAllDevices() throws IOException {
        for (String deviceId : transport.getConnectedDevices()) {
            syncWithDevice(deviceId);
        }
    }
    
    /**
     * Gets the clock offset for a specific device.
     * 
     * @param deviceId The ID of the device
     * @return The clock offset in milliseconds, or 0 if the device is not synchronized
     */
    public long getClockOffset(String deviceId) {
        TimeSync timeSync = timeSyncs.get(deviceId);
        return timeSync != null ? timeSync.getOffsetToMaster() : 0;
    }
    
    /**
     * Gets the status of a specific device.
     * 
     * @param deviceId The ID of the device
     * @return The device status, or null if the device is not connected
     */
    public DeviceStatus getDeviceStatus(String deviceId) {
        return deviceStatuses.get(deviceId);
    }
    
    /**
     * Gets the IDs of all connected devices.
     * 
     * @return A list of device IDs
     */
    public List<String> getConnectedDevices() {
        return new ArrayList<>(deviceStatuses.keySet());
    }
    
    /**
     * Checks if a device is connected.
     * 
     * @param deviceId The ID of the device
     * @return true if the device is connected, false otherwise
     */
    public boolean isDeviceConnected(String deviceId) {
        return deviceStatuses.containsKey(deviceId) && transport.isConnected(deviceId);
    }
    
    /**
     * Gets or creates a TimeSync instance for a device.
     * 
     * @param deviceId The ID of the device
     * @return The TimeSync instance
     */
    private TimeSync getOrCreateTimeSync(String deviceId) {
        return timeSyncs.computeIfAbsent(deviceId, id -> new TimeSync(serverId, id));
    }
    
    // NetworkTransport.Listener implementation
    
    @Override
    public void onConnected(String deviceId) {
        // Register the device with the connection monitor
        connectionMonitor.registerDevice(deviceId);
        
        // Create a new device status
        deviceStatuses.put(deviceId, new DeviceStatus(deviceId));
        
        // Notify the listener
        if (listener != null) {
            listener.onDeviceConnected(deviceId);
        }
        
        // Initiate time synchronization
        executorService.submit(() -> {
            try {
                Thread.sleep(1000); // Wait a bit for the connection to stabilize
                syncWithDevice(deviceId);
            } catch (Exception e) {
                System.err.println("Error syncing with device " + deviceId + ": " + e.getMessage());
            }
        });
    }
    
    @Override
    public void onDisconnected(String deviceId, String reason) {
        // Unregister the device from the connection monitor
        connectionMonitor.unregisterDevice(deviceId);
        
        // Remove the device status
        deviceStatuses.remove(deviceId);
        
        // Remove the TimeSync instance
        timeSyncs.remove(deviceId);
        
        // Notify the listener
        if (listener != null) {
            listener.onDeviceDisconnected(deviceId, reason);
        }
    }
    
    @Override
    public void onMessageReceived(CommandProtocol.Message message) {
        String deviceId = message.getDeviceId();
        
        // Update the heartbeat time
        connectionMonitor.updateHeartbeat(deviceId);
        
        // Handle the message based on its type
        if (message.getType() == CommandProtocol.CommandType.HEARTBEAT) {
            // Just update the heartbeat, no need to notify the listener
            return;
        } else if (message.getType() == CommandProtocol.CommandType.SYNC_PONG) {
            // Process the sync pong message
            TimeSync timeSync = getOrCreateTimeSync(deviceId);
            long offset = timeSync.processSyncPong((CommandProtocol.SyncMessage) message);
            
            // Notify the listener
            if (listener != null) {
                listener.onSyncEvent(deviceId, offset, timeSync.getRoundTripTime());
            }
            
            return;
        }
        
        // For other message types, notify the listener
        if (listener != null) {
            listener.onMessageReceived(message, deviceId);
        }
        
        // If it's a status message, update the device status
        if (message.getType() == CommandProtocol.CommandType.CMD_STATUS && message instanceof CommandProtocol.ResponseMessage) {
            CommandProtocol.ResponseMessage responseMessage = (CommandProtocol.ResponseMessage) message;
            DeviceStatus status = deviceStatuses.get(deviceId);
            if (status != null) {
                status.updateFromResponse(responseMessage);
            }
        }
    }
    
    @Override
    public void onError(String deviceId, String error, Exception exception) {
        // Notify the listener
        if (listener != null) {
            listener.onError(deviceId, error, exception);
        }
    }
    
    // ConnectionMonitor.Listener implementation
    
    @Override
    public void onConnectionLost(String deviceId) {
        // Update the device status
        DeviceStatus status = deviceStatuses.get(deviceId);
        if (status != null) {
            status.setConnected(false);
        }
        
        // Notify the listener
        if (listener != null) {
            listener.onError(deviceId, "Connection lost, attempting to recover", null);
        }
    }
    
    @Override
    public void onConnectionRecovered(String deviceId) {
        // Update the device status
        DeviceStatus status = deviceStatuses.get(deviceId);
        if (status != null) {
            status.setConnected(true);
        }
        
        // Notify the listener
        if (listener != null) {
            listener.onDeviceConnected(deviceId);
        }
        
        // Initiate time synchronization
        executorService.submit(() -> {
            try {
                syncWithDevice(deviceId);
            } catch (Exception e) {
                System.err.println("Error syncing with device " + deviceId + ": " + e.getMessage());
            }
        });
    }
    
    @Override
    public void onConnectionPermanentlyLost(String deviceId) {
        // Remove the device status
        deviceStatuses.remove(deviceId);
        
        // Remove the TimeSync instance
        timeSyncs.remove(deviceId);
        
        // Notify the listener
        if (listener != null) {
            listener.onDeviceDisconnected(deviceId, "Connection permanently lost");
        }
    }
    
    /**
     * Represents the status of a connected device.
     */
    public static class DeviceStatus {
        private final String deviceId;
        private boolean connected;
        private String batteryLevel;
        private String storageRemaining;
        private Map<String, Boolean> activeStreams = new HashMap<>();
        private long lastUpdateTime;
        
        /**
         * Creates a new DeviceStatus.
         * 
         * @param deviceId The ID of the device
         */
        public DeviceStatus(String deviceId) {
            this.deviceId = deviceId;
            this.connected = true;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        /**
         * Updates the status from a response message.
         * 
         * @param response The response message
         */
        public void updateFromResponse(CommandProtocol.ResponseMessage response) {
            if (response.getData().length >= 3) {
                this.batteryLevel = response.getData()[0];
                this.storageRemaining = response.getData()[1];
                
                // Parse active streams (format: "stream1:true,stream2:false,...")
                String streamsStr = response.getData()[2];
                String[] streams = streamsStr.split(",");
                for (String stream : streams) {
                    String[] parts = stream.split(":");
                    if (parts.length == 2) {
                        activeStreams.put(parts[0], Boolean.parseBoolean(parts[1]));
                    }
                }
            }
            
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        /**
         * Gets the device ID.
         * 
         * @return The device ID
         */
        public String getDeviceId() {
            return deviceId;
        }
        
        /**
         * Checks if the device is connected.
         * 
         * @return true if connected, false otherwise
         */
        public boolean isConnected() {
            return connected;
        }
        
        /**
         * Sets the connected status.
         * 
         * @param connected The connected status
         */
        public void setConnected(boolean connected) {
            this.connected = connected;
        }
        
        /**
         * Gets the battery level.
         * 
         * @return The battery level
         */
        public String getBatteryLevel() {
            return batteryLevel;
        }
        
        /**
         * Gets the storage remaining.
         * 
         * @return The storage remaining
         */
        public String getStorageRemaining() {
            return storageRemaining;
        }
        
        /**
         * Checks if a stream is active.
         * 
         * @param streamName The name of the stream
         * @return true if active, false otherwise
         */
        public boolean isStreamActive(String streamName) {
            Boolean active = activeStreams.get(streamName);
            return active != null && active;
        }
        
        /**
         * Gets the last update time.
         * 
         * @return The last update time
         */
        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
        
        /**
         * Gets a map of all active streams.
         * 
         * @return A map of stream names to their active status
         */
        public Map<String, Boolean> getActiveStreams() {
            return new HashMap<>(activeStreams);
        }
    }
}