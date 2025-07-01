package com.gsr.shared.utils;

/**
 * Utility class for defining standardized data formats across platforms.
 * This ensures that all data is stored and formatted consistently,
 * making it easier to parse and analyze.
 */
public class DataFormatUtils {
    
    // Video format constants
    public static final String VIDEO_CONTAINER_FORMAT = "mp4";
    public static final String VIDEO_CODEC = "h264";
    public static final int DEFAULT_VIDEO_WIDTH = 1920;
    public static final int DEFAULT_VIDEO_HEIGHT = 1080;
    public static final int DEFAULT_VIDEO_FPS = 30;
    public static final int DEFAULT_THERMAL_WIDTH = 384;
    public static final int DEFAULT_THERMAL_HEIGHT = 288;
    public static final int DEFAULT_THERMAL_FPS = 25;
    
    // Audio format constants
    public static final String AUDIO_FORMAT = "wav";
    public static final int DEFAULT_AUDIO_SAMPLE_RATE = 44100;
    public static final int DEFAULT_AUDIO_CHANNELS = 2;
    
    // GSR data format constants
    public static final String GSR_DATA_FORMAT = "csv";
    public static final int DEFAULT_GSR_SAMPLE_RATE = 128;
    
    // Timestamp format constants
    public static final String TIMESTAMP_FORMAT = "unix_epoch_ms";
    
    // Manifest format constants
    public static final String MANIFEST_FORMAT = "json";
    
    /**
     * Get the file extension for a specific data type.
     * 
     * @param dataType The type of data (e.g., "video", "audio", "gsr")
     * @return The file extension for the data type
     */
    public static String getFileExtension(String dataType) {
        switch (dataType) {
            case "rgb_video":
            case "thermal_video":
                return VIDEO_CONTAINER_FORMAT;
            case "audio":
                return AUDIO_FORMAT;
            case "gsr":
                return GSR_DATA_FORMAT;
            case "manifest":
                return MANIFEST_FORMAT;
            default:
                return "dat";
        }
    }
    
    /**
     * Get the CSV header for GSR data.
     * 
     * @return The CSV header for GSR data
     */
    public static String getGsrCsvHeader() {
        return "timestamp_ms,gsr_microsiemens,ppg_raw,heart_rate_bpm";
    }
    
    /**
     * Format a GSR data point as a CSV line.
     * 
     * @param timestamp The timestamp in milliseconds
     * @param gsrValue The GSR value in microsiemens
     * @param ppgValue The PPG raw value
     * @param heartRate The heart rate in BPM (can be -1 if not available)
     * @return A CSV line with the GSR data
     */
    public static String formatGsrCsvLine(long timestamp, float gsrValue, 
                                         int ppgValue, float heartRate) {
        return String.format("%d,%.2f,%d,%.1f", 
                            timestamp, gsrValue, ppgValue, 
                            heartRate < 0 ? -1.0f : heartRate);
    }
    
    /**
     * Get the JSON schema for the session manifest.
     * 
     * @return A template for the session manifest JSON
     */
    public static String getManifestTemplate() {
        return "{\n" +
               "  \"session_id\": \"\",\n" +
               "  \"start_time\": 0,\n" +
               "  \"end_time\": 0,\n" +
               "  \"devices\": [\n" +
               "    {\n" +
               "      \"device_id\": \"\",\n" +
               "      \"device_type\": \"\",\n" +
               "      \"time_offset_ms\": 0,\n" +
               "      \"files\": [\n" +
               "        {\n" +
               "          \"file_path\": \"\",\n" +
               "          \"file_type\": \"\",\n" +
               "          \"format\": \"\",\n" +
               "          \"duration_ms\": 0\n" +
               "        }\n" +
               "      ]\n" +
               "    }\n" +
               "  ],\n" +
               "  \"sync_events\": [\n" +
               "    {\n" +
               "      \"timestamp\": 0,\n" +
               "      \"type\": \"start\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"metadata\": {\n" +
               "    \"experiment_name\": \"\",\n" +
               "    \"participant_id\": \"\",\n" +
               "    \"notes\": \"\"\n" +
               "  }\n" +
               "}";
    }
}