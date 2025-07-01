package com.gsr.shared.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Utility class for generating consistent file names across platforms.
 * This ensures that all output files follow the same naming convention,
 * making it easier to correlate data from multiple devices.
 */
public class FileNamingUtils {
    
    // File type constants
    public static final String TYPE_RGB_VIDEO = "rgb";
    public static final String TYPE_THERMAL_VIDEO = "thermal";
    public static final String TYPE_GSR_DATA = "gsr";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_MANIFEST = "manifest";
    
    // Date format for session IDs
    private static final SimpleDateFormat SESSION_DATE_FORMAT = 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
    
    /**
     * Generate a unique session ID based on the current timestamp.
     * 
     * @return A unique session ID in the format "Session_YYYYMMDD_HHMMSS"
     */
    public static String generateSessionId() {
        return "Session_" + SESSION_DATE_FORMAT.format(new Date());
    }
    
    /**
     * Generate a file name for a data file.
     * 
     * @param sessionId The session ID
     * @param deviceId The device ID
     * @param fileType The type of file (use class constants)
     * @param extension The file extension (e.g., "mp4", "csv")
     * @return A file name in the format "SessionID_DeviceID_FileType.Extension"
     */
    public static String generateFileName(String sessionId, String deviceId, 
                                         String fileType, String extension) {
        return String.format("%s_%s_%s.%s", sessionId, deviceId, fileType, extension);
    }
    
    /**
     * Generate a directory path for a session.
     * 
     * @param baseDir The base directory
     * @param sessionId The session ID
     * @return A directory path in the format "BaseDir/SessionID"
     */
    public static String generateSessionDirectory(String baseDir, String sessionId) {
        return baseDir + "/" + sessionId;
    }
    
    /**
     * Generate a manifest file name for a session.
     * 
     * @param sessionId The session ID
     * @return A file name in the format "SessionID_manifest.json"
     */
    public static String generateManifestFileName(String sessionId) {
        return generateFileName(sessionId, "info", TYPE_MANIFEST, "json");
    }
}