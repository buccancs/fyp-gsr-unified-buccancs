package com.buccancs.gsr.common.network;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * ConnectionMonitor provides error detection and recovery mechanisms for network connections.
 * It monitors connection health, detects lost connections, and attempts recovery.
 */
public class ConnectionMonitor {
    
    /**
     * Listener interface for connection events.
     */
    public interface Listener {
        /**
         * Called when a connection is lost and recovery is being attempted.
         * 
         * @param deviceId The ID of the device with the lost connection
         */
        void onConnectionLost(String deviceId);
        
        /**
         * Called when a connection is recovered after being lost.
         * 
         * @param deviceId The ID of the device with the recovered connection
         */
        void onConnectionRecovered(String deviceId);
        
        /**
         * Called when a connection is permanently lost after recovery attempts fail.
         * 
         * @param deviceId The ID of the device with the permanently lost connection
         */
        void onConnectionPermanentlyLost(String deviceId);
    }
    
    private static final int DEFAULT_HEARTBEAT_INTERVAL = 5000; // 5 seconds
    private static final int DEFAULT_HEARTBEAT_TIMEOUT = 15000; // 15 seconds
    private static final int DEFAULT_MAX_RETRY_COUNT = 3;
    
    private final NetworkTransport transport;
    private final Listener listener;
    private final String deviceId;
    private final int heartbeatInterval;
    private final int heartbeatTimeout;
    private final int maxRetryCount;
    
    private ScheduledExecutorService scheduler;
    private boolean running;
    
    // Map of device IDs to their last heartbeat times
    private final Map<String, Long> lastHeartbeatTimes = new ConcurrentHashMap<>();
    
    // Map of device IDs to their retry counts
    private final Map<String, Integer> retryCounters = new ConcurrentHashMap<>();
    
    /**
     * Creates a new ConnectionMonitor with default settings.
     * 
     * @param transport The network transport to monitor
     * @param deviceId The ID of this device
     * @param listener The listener for connection events
     */
    public ConnectionMonitor(NetworkTransport transport, String deviceId, Listener listener) {
        this(transport, deviceId, listener, DEFAULT_HEARTBEAT_INTERVAL, DEFAULT_HEARTBEAT_TIMEOUT, DEFAULT_MAX_RETRY_COUNT);
    }
    
    /**
     * Creates a new ConnectionMonitor with custom settings.
     * 
     * @param transport The network transport to monitor
     * @param deviceId The ID of this device
     * @param listener The listener for connection events
     * @param heartbeatInterval The interval between heartbeat messages in milliseconds
     * @param heartbeatTimeout The timeout for heartbeat responses in milliseconds
     * @param maxRetryCount The maximum number of retry attempts
     */
    public ConnectionMonitor(NetworkTransport transport, String deviceId, Listener listener, 
                             int heartbeatInterval, int heartbeatTimeout, int maxRetryCount) {
        this.transport = transport;
        this.deviceId = deviceId;
        this.listener = listener;
        this.heartbeatInterval = heartbeatInterval;
        this.heartbeatTimeout = heartbeatTimeout;
        this.maxRetryCount = maxRetryCount;
    }
    
    /**
     * Starts the connection monitor.
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        scheduler = Executors.newScheduledThreadPool(2);
        
        // Schedule the heartbeat sender
        scheduler.scheduleAtFixedRate(
            this::sendHeartbeats,
            heartbeatInterval,
            heartbeatInterval,
            TimeUnit.MILLISECONDS
        );
        
        // Schedule the connection checker
        scheduler.scheduleAtFixedRate(
            this::checkConnections,
            heartbeatInterval,
            heartbeatInterval,
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Stops the connection monitor.
     */
    public void stop() {
        running = false;
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                scheduler.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        lastHeartbeatTimes.clear();
        retryCounters.clear();
    }
    
    /**
     * Registers a device for monitoring.
     * 
     * @param deviceId The ID of the device to monitor
     */
    public void registerDevice(String deviceId) {
        lastHeartbeatTimes.put(deviceId, System.currentTimeMillis());
        retryCounters.put(deviceId, 0);
    }
    
    /**
     * Unregisters a device from monitoring.
     * 
     * @param deviceId The ID of the device to unregister
     */
    public void unregisterDevice(String deviceId) {
        lastHeartbeatTimes.remove(deviceId);
        retryCounters.remove(deviceId);
    }
    
    /**
     * Updates the heartbeat time for a device.
     * This should be called when a heartbeat is received from the device.
     * 
     * @param deviceId The ID of the device
     */
    public void updateHeartbeat(String deviceId) {
        lastHeartbeatTimes.put(deviceId, System.currentTimeMillis());
        retryCounters.put(deviceId, 0); // Reset retry counter on successful heartbeat
    }
    
    /**
     * Sends heartbeat messages to all connected devices.
     */
    private void sendHeartbeats() {
        if (!running) {
            return;
        }
        
        try {
            // Create a heartbeat message
            CommandProtocol.Message heartbeatMessage = new CommandProtocol.CommandMessage(
                CommandProtocol.CommandType.HEARTBEAT,
                deviceId,
                "",
                String.valueOf(System.currentTimeMillis())
            );
            
            // Send the heartbeat to all connected devices
            transport.broadcastMessage(heartbeatMessage);
        } catch (IOException e) {
            System.err.println("Error sending heartbeats: " + e.getMessage());
        }
    }
    
    /**
     * Checks the connection status of all monitored devices.
     */
    private void checkConnections() {
        if (!running) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, Long> entry : lastHeartbeatTimes.entrySet()) {
            String deviceId = entry.getKey();
            long lastHeartbeatTime = entry.getValue();
            
            // Check if the heartbeat has timed out
            if (currentTime - lastHeartbeatTime > heartbeatTimeout) {
                handleHeartbeatTimeout(deviceId);
            }
        }
    }
    
    /**
     * Handles a heartbeat timeout for a device.
     * 
     * @param deviceId The ID of the device with the timeout
     */
    private void handleHeartbeatTimeout(String deviceId) {
        int retryCount = retryCounters.getOrDefault(deviceId, 0);
        
        if (retryCount < maxRetryCount) {
            // Increment the retry counter
            retryCounters.put(deviceId, retryCount + 1);
            
            // Notify the listener
            if (listener != null && retryCount == 0) {
                listener.onConnectionLost(deviceId);
            }
            
            // Attempt to send a heartbeat directly to the device
            try {
                CommandProtocol.Message heartbeatMessage = new CommandProtocol.CommandMessage(
                    CommandProtocol.CommandType.HEARTBEAT,
                    this.deviceId,
                    "",
                    String.valueOf(System.currentTimeMillis())
                );
                
                transport.sendMessage(heartbeatMessage, deviceId);
            } catch (IOException e) {
                System.err.println("Error sending retry heartbeat to " + deviceId + ": " + e.getMessage());
            }
        } else {
            // Max retry count reached, consider the connection permanently lost
            if (listener != null) {
                listener.onConnectionPermanentlyLost(deviceId);
            }
            
            // Unregister the device
            unregisterDevice(deviceId);
        }
    }
    
    /**
     * Handles a successful message delivery to a device.
     * This resets the retry counter for the device.
     * 
     * @param deviceId The ID of the device
     */
    public void handleSuccessfulDelivery(String deviceId) {
        Integer previousRetryCount = retryCounters.put(deviceId, 0);
        
        // If the device was previously in a retry state, notify that it's recovered
        if (previousRetryCount != null && previousRetryCount > 0 && listener != null) {
            listener.onConnectionRecovered(deviceId);
        }
    }
    
    /**
     * Gets the current retry count for a device.
     * 
     * @param deviceId The ID of the device
     * @return The retry count, or 0 if the device is not being monitored
     */
    public int getRetryCount(String deviceId) {
        return retryCounters.getOrDefault(deviceId, 0);
    }
    
    /**
     * Checks if a device is considered connected (has recent heartbeats).
     * 
     * @param deviceId The ID of the device
     * @return true if the device is connected, false otherwise
     */
    public boolean isDeviceConnected(String deviceId) {
        Long lastHeartbeatTime = lastHeartbeatTimes.get(deviceId);
        if (lastHeartbeatTime == null) {
            return false;
        }
        
        return System.currentTimeMillis() - lastHeartbeatTime <= heartbeatTimeout;
    }
}