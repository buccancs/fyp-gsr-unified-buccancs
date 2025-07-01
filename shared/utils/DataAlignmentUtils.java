package com.gsr.shared.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gsr.shared.models.SessionInfo;
import com.gsr.shared.models.SessionInfo.DeviceInfo;
import com.gsr.shared.models.SessionInfo.SyncEvent;

/**
 * Utility class for aligning data from multiple devices.
 * This ensures that data from different sources can be merged for analysis.
 */
public class DataAlignmentUtils {

    /**
     * Calculate time offsets for all devices in a session based on sync events.
     * This updates the timeOffsetMs field in each DeviceInfo.
     * 
     * @param session The session info
     * @param referenceDeviceId The ID of the reference device (e.g., the PC)
     */
    public static void calculateTimeOffsets(SessionInfo session, String referenceDeviceId) {
        List<SyncEvent> syncEvents = session.getSyncEvents();
        if (syncEvents.isEmpty()) {
            return;
        }

        // Group sync events by type
        Map<String, List<SyncEvent>> eventsByType = new HashMap<>();
        for (SyncEvent event : syncEvents) {
            String type = event.getType();
            if (!eventsByType.containsKey(type)) {
                eventsByType.put(type, new ArrayList<>());
            }
            eventsByType.get(type).add(event);
        }

        // Find the reference device
        DeviceInfo referenceDevice = null;
        for (DeviceInfo device : session.getDevices()) {
            if (device.getDeviceId().equals(referenceDeviceId)) {
                referenceDevice = device;
                break;
            }
        }

        if (referenceDevice == null) {
            // If reference device not found, use the first device
            if (!session.getDevices().isEmpty()) {
                referenceDevice = session.getDevices().get(0);
            } else {
                return;
            }
        }

        // Set the reference device's offset to 0
        referenceDevice.setTimeOffsetMs(0);

        // Calculate offsets for other devices
        for (DeviceInfo device : session.getDevices()) {
            if (device == referenceDevice) {
                continue;
            }

            // Use the "start" event if available
            if (eventsByType.containsKey("start")) {
                long referenceStartTime = findEventTimeForDevice(eventsByType.get("start"), referenceDeviceId);
                long deviceStartTime = findEventTimeForDevice(eventsByType.get("start"), device.getDeviceId());

                if (referenceStartTime > 0 && deviceStartTime > 0) {
                    long offset = referenceStartTime - deviceStartTime;
                    device.setTimeOffsetMs(offset);
                    continue;
                }
            }

            // If no start event, use the average of all sync events
            long totalOffset = 0;
            int count = 0;

            for (Map.Entry<String, List<SyncEvent>> entry : eventsByType.entrySet()) {
                long referenceTime = findEventTimeForDevice(entry.getValue(), referenceDeviceId);
                long deviceTime = findEventTimeForDevice(entry.getValue(), device.getDeviceId());

                if (referenceTime > 0 && deviceTime > 0) {
                    totalOffset += (referenceTime - deviceTime);
                    count++;
                }
            }

            if (count > 0) {
                device.setTimeOffsetMs(totalOffset / count);
            }
        }
    }

    /**
     * Find the timestamp of an event for a specific device.
     * 
     * @param events The list of events
     * @param deviceId The device ID
     * @return The timestamp, or -1 if not found
     */
    private static long findEventTimeForDevice(List<SyncEvent> events, String deviceId) {
        for (SyncEvent event : events) {
            if (event.getType().contains(deviceId)) {
                return event.getTimestamp();
            }
        }
        return -1;
    }

    /**
     * Convert a timestamp from a device's local time to the reference timeline.
     * 
     * @param localTimestamp The local timestamp
     * @param deviceInfo The device info with the time offset
     * @return The timestamp in the reference timeline
     */
    public static long convertToReferenceTime(long localTimestamp, DeviceInfo deviceInfo) {
        return localTimestamp + deviceInfo.getTimeOffsetMs();
    }

    /**
     * Generate a CSV file with aligned timestamps for all sync events.
     * 
     * @param session The session info
     * @return A CSV string with aligned timestamps
     */
    public static String generateAlignedTimestampsCsv(SessionInfo session) {
        StringBuilder csv = new StringBuilder();

        // Add header
        csv.append("event_type");
        for (DeviceInfo device : session.getDevices()) {
            csv.append(",").append(device.getDeviceId());
        }
        csv.append("\n");

        // Group sync events by type
        Map<String, List<SyncEvent>> eventsByType = new HashMap<>();
        for (SyncEvent event : session.getSyncEvents()) {
            String type = event.getType();
            if (!eventsByType.containsKey(type)) {
                eventsByType.put(type, new ArrayList<>());
            }
            eventsByType.get(type).add(event);
        }

        // Add rows for each event type
        for (Map.Entry<String, List<SyncEvent>> entry : eventsByType.entrySet()) {
            String eventType = entry.getKey();
            csv.append(eventType);

            for (DeviceInfo device : session.getDevices()) {
                long timestamp = findEventTimeForDevice(entry.getValue(), device.getDeviceId());
                if (timestamp > 0) {
                    // Convert to reference time
                    timestamp = convertToReferenceTime(timestamp, device);
                }
                csv.append(",").append(timestamp > 0 ? timestamp : "");
            }

            csv.append("\n");
        }

        return csv.toString();
    }
}
