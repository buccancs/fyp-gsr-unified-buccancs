package com.buccancs.gsr.common.network;

/**
 * SyncMarker provides utilities for creating and handling synchronization markers.
 * These markers are used to verify synchronization during recording by broadcasting
 * events to all devices and logging their receipt times.
 */
public class SyncMarker {
    
    /**
     * Types of synchronization markers.
     */
    public enum MarkerType {
        START_RECORDING,    // Marks the start of a recording session
        STOP_RECORDING,     // Marks the end of a recording session
        PERIODIC,           // Regular sync check during recording
        MANUAL,             // Manually triggered sync marker
        CALIBRATION         // Special marker for calibration purposes
    }
    
    /**
     * A recorded sync marker event with its timestamp.
     */
    public static class MarkerEvent {
        private final String markerId;
        private final MarkerType type;
        private final long localTimestamp;
        private final long masterTimestamp;
        private final String deviceId;
        
        /**
         * Creates a new MarkerEvent.
         * 
         * @param markerId The unique identifier for this marker
         * @param type The type of marker
         * @param localTimestamp The timestamp in the local device's time domain
         * @param masterTimestamp The timestamp in the master clock's time domain (if known, 0 otherwise)
         * @param deviceId The ID of the device that recorded this event
         */
        public MarkerEvent(String markerId, MarkerType type, long localTimestamp, long masterTimestamp, String deviceId) {
            this.markerId = markerId;
            this.type = type;
            this.localTimestamp = localTimestamp;
            this.masterTimestamp = masterTimestamp;
            this.deviceId = deviceId;
        }
        
        /**
         * Gets the marker ID.
         * 
         * @return The marker ID
         */
        public String getMarkerId() {
            return markerId;
        }
        
        /**
         * Gets the marker type.
         * 
         * @return The marker type
         */
        public MarkerType getType() {
            return type;
        }
        
        /**
         * Gets the local timestamp.
         * 
         * @return The timestamp in the local device's time domain
         */
        public long getLocalTimestamp() {
            return localTimestamp;
        }
        
        /**
         * Gets the master timestamp.
         * 
         * @return The timestamp in the master clock's time domain
         */
        public long getMasterTimestamp() {
            return masterTimestamp;
        }
        
        /**
         * Gets the device ID.
         * 
         * @return The ID of the device that recorded this event
         */
        public String getDeviceId() {
            return deviceId;
        }
        
        @Override
        public String toString() {
            return "MarkerEvent{" +
                   "markerId='" + markerId + '\'' +
                   ", type=" + type +
                   ", localTimestamp=" + localTimestamp +
                   ", masterTimestamp=" + masterTimestamp +
                   ", deviceId='" + deviceId + '\'' +
                   '}';
        }
    }
    
    /**
     * Creates a sync marker message to be sent to devices.
     * 
     * @param markerId The unique identifier for this marker
     * @param type The type of marker
     * @param deviceId The ID of the device sending the marker
     * @param sessionId The current session ID
     * @return A CommandMessage with the SYNC_MARKER command
     */
    public static CommandProtocol.CommandMessage createMarkerMessage(String markerId, MarkerType type, String deviceId, String sessionId) {
        return new CommandProtocol.CommandMessage(
            CommandProtocol.CommandType.SYNC_MARKER,
            deviceId,
            sessionId,
            markerId,
            type.name(),
            String.valueOf(System.currentTimeMillis())
        );
    }
    
    /**
     * Records a marker event when a marker message is received.
     * 
     * @param message The received marker message
     * @param timeSync The TimeSync instance for timestamp conversion
     * @param deviceId The ID of this device
     * @return A MarkerEvent recording the receipt of the marker
     */
    public static MarkerEvent recordMarkerEvent(CommandProtocol.CommandMessage message, TimeSync timeSync, String deviceId) {
        String markerId = message.getParameters()[0];
        MarkerType type = MarkerType.valueOf(message.getParameters()[1]);
        long senderTimestamp = Long.parseLong(message.getParameters()[2]);
        
        long localTimestamp = System.currentTimeMillis();
        long masterTimestamp = timeSync.localToMasterTime(localTimestamp);
        
        return new MarkerEvent(markerId, type, localTimestamp, masterTimestamp, deviceId);
    }
    
    /**
     * Generates a unique marker ID.
     * 
     * @param prefix A prefix for the marker ID
     * @return A unique marker ID
     */
    public static String generateMarkerId(String prefix) {
        return prefix + "_" + System.currentTimeMillis();
    }
    
    /**
     * Calculates the time difference between two marker events.
     * This can be used to verify synchronization accuracy.
     * 
     * @param event1 The first marker event
     * @param event2 The second marker event
     * @return The time difference in milliseconds
     */
    public static long calculateTimeDifference(MarkerEvent event1, MarkerEvent event2) {
        // If master timestamps are available, use those for comparison
        if (event1.getMasterTimestamp() > 0 && event2.getMasterTimestamp() > 0) {
            return event1.getMasterTimestamp() - event2.getMasterTimestamp();
        }
        
        // Otherwise, use local timestamps (less accurate)
        return event1.getLocalTimestamp() - event2.getLocalTimestamp();
    }
}