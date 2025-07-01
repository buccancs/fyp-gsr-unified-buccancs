package com.gsr.shared.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * DataEvent represents a common data format for events across platforms.
 * This class is used by both Android and Windows applications to ensure
 * consistent data handling.
 */
public class DataEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // Event type constants
    public static final String TYPE_GSR = "gsr";
    public static final String TYPE_RGB_FRAME = "rgb_frame";
    public static final String TYPE_THERMAL_FRAME = "thermal_frame";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_SYNC_MARKER = "sync_marker";
    
    private String type;
    private long timestamp;  // Timestamp in milliseconds (epoch time)
    private String deviceId; // Unique identifier for the source device
    private Map<String, Object> data;
    
    public DataEvent(String type, long timestamp, String deviceId) {
        this.type = type;
        this.timestamp = timestamp;
        this.deviceId = deviceId;
        this.data = new HashMap<>();
    }
    
    public String getType() {
        return type;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void addData(String key, Object value) {
        data.put(key, value);
    }
    
    public Object getValue(String key) {
        return data.get(key);
    }
    
    @Override
    public String toString() {
        return "DataEvent{" +
                "type='" + type + '\'' +
                ", timestamp=" + timestamp +
                ", deviceId='" + deviceId + '\'' +
                ", data=" + data +
                '}';
    }
}