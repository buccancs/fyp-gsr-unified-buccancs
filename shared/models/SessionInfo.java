package com.gsr.shared.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gsr.shared.utils.FileNamingUtils;

/**
 * SessionInfo represents all the information about a recording session.
 * This class is used to track session metadata, devices, and files.
 */
public class SessionInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String sessionId;
    private long startTime;
    private long endTime;
    private List<DeviceInfo> devices;
    private List<SyncEvent> syncEvents;
    private Map<String, String> metadata;
    
    /**
     * Create a new SessionInfo with a generated session ID.
     */
    public SessionInfo() {
        this(FileNamingUtils.generateSessionId());
    }
    
    /**
     * Create a new SessionInfo with the specified session ID.
     * 
     * @param sessionId The session ID
     */
    public SessionInfo(String sessionId) {
        this.sessionId = sessionId;
        this.startTime = 0;
        this.endTime = 0;
        this.devices = new ArrayList<>();
        this.syncEvents = new ArrayList<>();
        this.metadata = new HashMap<>();
    }
    
    /**
     * Get the session ID.
     * 
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Set the session start time.
     * 
     * @param startTime The start time in milliseconds (epoch time)
     */
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    /**
     * Get the session start time.
     * 
     * @return The start time in milliseconds (epoch time)
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Set the session end time.
     * 
     * @param endTime The end time in milliseconds (epoch time)
     */
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    /**
     * Get the session end time.
     * 
     * @return The end time in milliseconds (epoch time)
     */
    public long getEndTime() {
        return endTime;
    }
    
    /**
     * Add a device to the session.
     * 
     * @param device The device info
     */
    public void addDevice(DeviceInfo device) {
        devices.add(device);
    }
    
    /**
     * Get all devices in the session.
     * 
     * @return The list of devices
     */
    public List<DeviceInfo> getDevices() {
        return devices;
    }
    
    /**
     * Add a sync event to the session.
     * 
     * @param event The sync event
     */
    public void addSyncEvent(SyncEvent event) {
        syncEvents.add(event);
    }
    
    /**
     * Get all sync events in the session.
     * 
     * @return The list of sync events
     */
    public List<SyncEvent> getSyncEvents() {
        return syncEvents;
    }
    
    /**
     * Set a metadata value.
     * 
     * @param key The metadata key
     * @param value The metadata value
     */
    public void setMetadata(String key, String value) {
        metadata.put(key, value);
    }
    
    /**
     * Get a metadata value.
     * 
     * @param key The metadata key
     * @return The metadata value, or null if not found
     */
    public String getMetadata(String key) {
        return metadata.get(key);
    }
    
    /**
     * Get all metadata.
     * 
     * @return The metadata map
     */
    public Map<String, String> getAllMetadata() {
        return metadata;
    }
    
    /**
     * DeviceInfo represents information about a device in the session.
     */
    public static class DeviceInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String deviceId;
        private String deviceType;
        private long timeOffsetMs;
        private List<FileInfo> files;
        
        /**
         * Create a new DeviceInfo.
         * 
         * @param deviceId The device ID
         * @param deviceType The device type (e.g., "android", "pc")
         */
        public DeviceInfo(String deviceId, String deviceType) {
            this.deviceId = deviceId;
            this.deviceType = deviceType;
            this.timeOffsetMs = 0;
            this.files = new ArrayList<>();
        }
        
        /**
         * Get the device ID.
         * 
         * @return The device ID
         */
        public String getDeviceId() {
            return deviceId;
        }
        
        /**
         * Get the device type.
         * 
         * @return The device type
         */
        public String getDeviceType() {
            return deviceType;
        }
        
        /**
         * Set the time offset for this device.
         * 
         * @param timeOffsetMs The time offset in milliseconds
         */
        public void setTimeOffsetMs(long timeOffsetMs) {
            this.timeOffsetMs = timeOffsetMs;
        }
        
        /**
         * Get the time offset for this device.
         * 
         * @return The time offset in milliseconds
         */
        public long getTimeOffsetMs() {
            return timeOffsetMs;
        }
        
        /**
         * Add a file to this device.
         * 
         * @param file The file info
         */
        public void addFile(FileInfo file) {
            files.add(file);
        }
        
        /**
         * Get all files for this device.
         * 
         * @return The list of files
         */
        public List<FileInfo> getFiles() {
            return files;
        }
    }
    
    /**
     * FileInfo represents information about a file in the session.
     */
    public static class FileInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String filePath;
        private String fileType;
        private String format;
        private long durationMs;
        
        /**
         * Create a new FileInfo.
         * 
         * @param filePath The file path
         * @param fileType The file type (e.g., "rgb", "thermal", "gsr")
         * @param format The file format (e.g., "mp4", "csv")
         */
        public FileInfo(String filePath, String fileType, String format) {
            this.filePath = filePath;
            this.fileType = fileType;
            this.format = format;
            this.durationMs = 0;
        }
        
        /**
         * Get the file path.
         * 
         * @return The file path
         */
        public String getFilePath() {
            return filePath;
        }
        
        /**
         * Get the file type.
         * 
         * @return The file type
         */
        public String getFileType() {
            return fileType;
        }
        
        /**
         * Get the file format.
         * 
         * @return The file format
         */
        public String getFormat() {
            return format;
        }
        
        /**
         * Set the duration of the file.
         * 
         * @param durationMs The duration in milliseconds
         */
        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }
        
        /**
         * Get the duration of the file.
         * 
         * @return The duration in milliseconds
         */
        public long getDurationMs() {
            return durationMs;
        }
    }
    
    /**
     * SyncEvent represents a synchronization event in the session.
     */
    public static class SyncEvent implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private long timestamp;
        private String type;
        
        /**
         * Create a new SyncEvent.
         * 
         * @param timestamp The timestamp in milliseconds (epoch time)
         * @param type The event type (e.g., "start", "stop", "marker")
         */
        public SyncEvent(long timestamp, String type) {
            this.timestamp = timestamp;
            this.type = type;
        }
        
        /**
         * Get the timestamp.
         * 
         * @return The timestamp in milliseconds (epoch time)
         */
        public long getTimestamp() {
            return timestamp;
        }
        
        /**
         * Get the event type.
         * 
         * @return The event type
         */
        public String getType() {
            return type;
        }
    }
}