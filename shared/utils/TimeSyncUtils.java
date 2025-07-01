package com.gsr.shared.utils;

/**
 * Utility class for timestamp synchronization across devices.
 * This ensures that timestamps from different devices can be aligned
 * to a common timeline.
 */
public class TimeSyncUtils {
    
    /**
     * Calculate the offset between a local timestamp and a reference timestamp.
     * This offset can be used to convert local timestamps to the reference timeline.
     * 
     * @param localTimestamp The local device timestamp
     * @param referenceTimestamp The reference timestamp (e.g., from the PC)
     * @return The offset to add to local timestamps to get reference timestamps
     */
    public static long calculateOffset(long localTimestamp, long referenceTimestamp) {
        return referenceTimestamp - localTimestamp;
    }
    
    /**
     * Convert a local timestamp to a reference timestamp using an offset.
     * 
     * @param localTimestamp The local device timestamp
     * @param offset The offset calculated using calculateOffset()
     * @return The timestamp in the reference timeline
     */
    public static long convertToReferenceTime(long localTimestamp, long offset) {
        return localTimestamp + offset;
    }
    
    /**
     * Calculate the round-trip time for a ping-pong synchronization.
     * This can be used to estimate the network latency.
     * 
     * @param pingTimestamp The timestamp when the ping was sent
     * @param pongTimestamp The timestamp when the pong was received
     * @return The round-trip time in milliseconds
     */
    public static long calculateRoundTripTime(long pingTimestamp, long pongTimestamp) {
        return pongTimestamp - pingTimestamp;
    }
    
    /**
     * Estimate the one-way network latency based on the round-trip time.
     * This assumes symmetric network conditions.
     * 
     * @param roundTripTime The round-trip time calculated using calculateRoundTripTime()
     * @return The estimated one-way latency in milliseconds
     */
    public static long estimateOneWayLatency(long roundTripTime) {
        return roundTripTime / 2;
    }
    
    /**
     * Calculate a more accurate offset by accounting for network latency.
     * 
     * @param localTimestamp The local device timestamp
     * @param referenceTimestamp The reference timestamp (e.g., from the PC)
     * @param oneWayLatency The estimated one-way latency
     * @return The offset adjusted for network latency
     */
    public static long calculateAdjustedOffset(long localTimestamp, 
                                              long referenceTimestamp,
                                              long oneWayLatency) {
        return referenceTimestamp - localTimestamp - oneWayLatency;
    }
}