package com.buccancs.gsr.common.network;

/**
 * TimeSync provides utilities for time synchronization between devices.
 * It implements a simplified NTP-like algorithm to estimate clock offsets
 * and align timestamps across multiple devices.
 */
public class TimeSync {
    
    private String deviceId;
    private String masterId;
    private long offsetToMaster = 0; // Offset in milliseconds to the master clock
    private long roundTripTime = 0;  // Last measured round-trip time in milliseconds
    private long lastSyncTime = 0;   // Last time synchronization was performed
    private int syncInterval = 30000; // Default sync interval: 30 seconds
    private int maxAllowedError = 50; // Maximum allowed timestamp error in milliseconds
    
    /**
     * Creates a new TimeSync instance.
     * 
     * @param deviceId The unique identifier for this device
     * @param masterId The identifier of the master clock device (usually the PC)
     */
    public TimeSync(String deviceId, String masterId) {
        this.deviceId = deviceId;
        this.masterId = masterId;
    }
    
    /**
     * Creates a SYNC_PING message to initiate time synchronization.
     * 
     * @return A SyncMessage with the SYNC_PING command
     */
    public CommandProtocol.SyncMessage createSyncPing() {
        return new CommandProtocol.SyncMessage(
            CommandProtocol.CommandType.SYNC_PING,
            deviceId,
            System.currentTimeMillis(),
            0 // No receive timestamp for initial ping
        );
    }
    
    /**
     * Creates a SYNC_PONG response to a received SYNC_PING.
     * 
     * @param pingMessage The received SYNC_PING message
     * @return A SyncMessage with the SYNC_PONG command
     */
    public CommandProtocol.SyncMessage createSyncPong(CommandProtocol.SyncMessage pingMessage) {
        return new CommandProtocol.SyncMessage(
            CommandProtocol.CommandType.SYNC_PONG,
            deviceId,
            pingMessage.getOriginTimestamp(),
            System.currentTimeMillis()
        );
    }
    
    /**
     * Processes a received SYNC_PONG message to calculate clock offset.
     * This should be called by the master device (usually the PC) when it receives
     * a SYNC_PONG response from a client device.
     * 
     * @param pongMessage The received SYNC_PONG message
     * @return The calculated offset in milliseconds
     */
    public long processSyncPong(CommandProtocol.SyncMessage pongMessage) {
        long t0 = pongMessage.getOriginTimestamp();    // Time when ping was sent
        long t1 = pongMessage.getReceiveTimestamp();   // Time when ping was received
        long t2 = pongMessage.getTransmitTimestamp();  // Time when pong was sent
        long t3 = System.currentTimeMillis();          // Time when pong was received
        
        // Calculate round-trip time
        roundTripTime = (t3 - t0) - (t2 - t1);
        
        // Calculate offset (simplified NTP formula)
        // offset = ((t1 - t0) + (t2 - t3)) / 2
        long offset = ((t1 - t0) + (t2 - t3)) / 2;
        
        // Update the offset to master if this is a client device
        if (!deviceId.equals(masterId)) {
            offsetToMaster = offset;
        }
        
        lastSyncTime = System.currentTimeMillis();
        
        return offset;
    }
    
    /**
     * Converts a local timestamp to the master clock's time domain.
     * 
     * @param localTimestamp The local timestamp to convert
     * @return The equivalent timestamp in the master clock's time domain
     */
    public long localToMasterTime(long localTimestamp) {
        return localTimestamp + offsetToMaster;
    }
    
    /**
     * Converts a master clock timestamp to the local time domain.
     * 
     * @param masterTimestamp The master timestamp to convert
     * @return The equivalent timestamp in the local clock's time domain
     */
    public long masterToLocalTime(long masterTimestamp) {
        return masterTimestamp - offsetToMaster;
    }
    
    /**
     * Checks if synchronization is needed based on the last sync time.
     * 
     * @return true if synchronization should be performed, false otherwise
     */
    public boolean isSyncNeeded() {
        return System.currentTimeMillis() - lastSyncTime > syncInterval;
    }
    
    /**
     * Gets the estimated accuracy of the current synchronization.
     * This is a simplified estimate based on half the round-trip time.
     * 
     * @return The estimated synchronization error in milliseconds
     */
    public long getEstimatedSyncError() {
        return roundTripTime / 2;
    }
    
    /**
     * Checks if the current synchronization meets the maximum allowed error threshold.
     * 
     * @return true if the synchronization is within acceptable limits, false otherwise
     */
    public boolean isSyncAccurate() {
        return getEstimatedSyncError() <= maxAllowedError;
    }
    
    /**
     * Gets the offset to the master clock in milliseconds.
     * 
     * @return The current offset to the master clock
     */
    public long getOffsetToMaster() {
        return offsetToMaster;
    }
    
    /**
     * Gets the last measured round-trip time in milliseconds.
     * 
     * @return The round-trip time
     */
    public long getRoundTripTime() {
        return roundTripTime;
    }
    
    /**
     * Gets the time of the last successful synchronization.
     * 
     * @return The timestamp of the last sync
     */
    public long getLastSyncTime() {
        return lastSyncTime;
    }
    
    /**
     * Sets the synchronization interval.
     * 
     * @param interval The interval in milliseconds
     */
    public void setSyncInterval(int interval) {
        this.syncInterval = interval;
    }
    
    /**
     * Sets the maximum allowed synchronization error.
     * 
     * @param maxError The maximum error in milliseconds
     */
    public void setMaxAllowedError(int maxError) {
        this.maxAllowedError = maxError;
    }
}