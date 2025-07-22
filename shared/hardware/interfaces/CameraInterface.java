package com.buccancs.gsr.shared.hardware.interfaces;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generic interface for all camera types in the GSR system.
 * This interface provides a common abstraction layer that allows the system
 * to work with different camera hardware without tight coupling.
 * 
 * Supported camera types include:
 * - Thermal cameras (e.g., Topdon TC001)
 * - RGB cameras (standard webcams)
 * - Infrared cameras
 * - Depth cameras
 * - Custom camera implementations
 */
public interface CameraInterface {
    
    /**
     * Camera types supported by the system.
     */
    enum CameraType {
        RGB("RGB Camera"),
        THERMAL("Thermal Camera"),
        INFRARED("Infrared Camera"),
        DEPTH("Depth Camera"),
        STEREO("Stereo Camera"),
        FISHEYE("Fisheye Camera"),
        CUSTOM("Custom Camera");
        
        private final String displayName;
        
        CameraType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Connection types supported by cameras.
     */
    enum ConnectionType {
        USB("USB"),
        WIFI("WiFi"),
        ETHERNET("Ethernet"),
        BLUETOOTH("Bluetooth"),
        HDMI("HDMI"),
        CSI("CSI"),
        CUSTOM("Custom");
        
        private final String displayName;
        
        ConnectionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Camera status enumeration.
     */
    enum CameraStatus {
        DISCONNECTED("Disconnected"),
        CONNECTING("Connecting"),
        CONNECTED("Connected"),
        STREAMING("Streaming"),
        RECORDING("Recording"),
        ERROR("Error"),
        CALIBRATING("Calibrating");
        
        private final String displayName;
        
        CameraStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Video resolution specification.
     */
    class Resolution {
        private final int width;
        private final int height;
        
        public Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }
        
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        
        @Override
        public String toString() {
            return width + "x" + height;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Resolution that = (Resolution) obj;
            return width == that.width && height == that.height;
        }
        
        @Override
        public int hashCode() {
            return width * 31 + height;
        }
    }
    
    /**
     * Camera frame data.
     */
    class FrameData {
        private final long timestamp;
        private final CameraType cameraType;
        private final byte[] imageData;
        private final String format; // e.g., "JPEG", "PNG", "RAW", "YUV"
        private final Resolution resolution;
        private final Map<String, Object> metadata;
        
        public FrameData(long timestamp, CameraType cameraType, byte[] imageData, 
                        String format, Resolution resolution, Map<String, Object> metadata) {
            this.timestamp = timestamp;
            this.cameraType = cameraType;
            this.imageData = imageData;
            this.format = format;
            this.resolution = resolution;
            this.metadata = metadata;
        }
        
        public long getTimestamp() { return timestamp; }
        public CameraType getCameraType() { return cameraType; }
        public byte[] getImageData() { return imageData; }
        public String getFormat() { return format; }
        public Resolution getResolution() { return resolution; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        @Override
        public String toString() {
            return String.format("FrameData{timestamp=%d, type=%s, format=%s, resolution=%s, size=%d bytes}", 
                timestamp, cameraType, format, resolution, imageData.length);
        }
    }
    
    /**
     * Camera configuration parameters.
     */
    class CameraConfiguration {
        private final Map<String, Object> parameters;
        
        public CameraConfiguration(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
        
        public Map<String, Object> getParameters() { return parameters; }
        
        public <T> T getParameter(String key, Class<T> type) {
            Object value = parameters.get(key);
            if (value != null && type.isInstance(value)) {
                return type.cast(value);
            }
            return null;
        }
        
        public <T> T getParameter(String key, Class<T> type, T defaultValue) {
            T value = getParameter(key, type);
            return value != null ? value : defaultValue;
        }
    }
    
    /**
     * Listener interface for camera events.
     */
    interface CameraEventListener {
        /**
         * Called when a new frame is available.
         */
        void onFrameReceived(FrameData frame);
        
        /**
         * Called when camera status changes.
         */
        void onStatusChanged(CameraStatus oldStatus, CameraStatus newStatus);
        
        /**
         * Called when a camera error occurs.
         */
        void onError(String errorMessage, Exception cause);
        
        /**
         * Called when camera connection is lost.
         */
        void onConnectionLost(String reason);
        
        /**
         * Called when recording starts.
         */
        void onRecordingStarted(String outputPath);
        
        /**
         * Called when recording stops.
         */
        void onRecordingStopped(String outputPath, long duration);
    }
    
    // Core camera operations
    
    /**
     * Get the unique identifier for this camera.
     */
    String getCameraId();
    
    /**
     * Get the human-readable name of this camera.
     */
    String getCameraName();
    
    /**
     * Get the type of this camera.
     */
    CameraType getCameraType();
    
    /**
     * Get the connection type used by this camera.
     */
    ConnectionType getConnectionType();
    
    /**
     * Get the current status of the camera.
     */
    CameraStatus getStatus();
    
    /**
     * Get camera capabilities and supported features.
     */
    Map<String, Object> getCapabilities();
    
    /**
     * Check if the camera is currently connected.
     */
    boolean isConnected();
    
    /**
     * Check if the camera is currently streaming.
     */
    boolean isStreaming();
    
    /**
     * Check if the camera is currently recording.
     */
    boolean isRecording();
    
    // Connection management
    
    /**
     * Connect to the camera asynchronously.
     * @return CompletableFuture that completes when connection is established
     */
    CompletableFuture<Boolean> connect();
    
    /**
     * Disconnect from the camera asynchronously.
     * @return CompletableFuture that completes when disconnection is finished
     */
    CompletableFuture<Boolean> disconnect();
    
    /**
     * Reconnect to the camera (disconnect then connect).
     * @return CompletableFuture that completes when reconnection is finished
     */
    default CompletableFuture<Boolean> reconnect() {
        return disconnect().thenCompose(success -> connect());
    }
    
    // Video streaming
    
    /**
     * Start streaming video from the camera.
     * @return CompletableFuture that completes when streaming starts
     */
    CompletableFuture<Boolean> startStreaming();
    
    /**
     * Stop streaming video from the camera.
     * @return CompletableFuture that completes when streaming stops
     */
    CompletableFuture<Boolean> stopStreaming();
    
    /**
     * Get the current frame rate in FPS.
     */
    double getFrameRate();
    
    /**
     * Set the frame rate in FPS.
     * @param fps The desired frame rate
     * @return CompletableFuture that completes when frame rate is set
     */
    CompletableFuture<Boolean> setFrameRate(double fps);
    
    /**
     * Get the supported frame rates.
     */
    List<Double> getSupportedFrameRates();
    
    // Resolution and format
    
    /**
     * Get the current resolution.
     */
    Resolution getResolution();
    
    /**
     * Set the resolution.
     * @param resolution The desired resolution
     * @return CompletableFuture that completes when resolution is set
     */
    CompletableFuture<Boolean> setResolution(Resolution resolution);
    
    /**
     * Get the supported resolutions.
     */
    List<Resolution> getSupportedResolutions();
    
    /**
     * Get the current image format.
     */
    String getImageFormat();
    
    /**
     * Set the image format.
     * @param format The desired format (e.g., "JPEG", "PNG", "RAW")
     * @return CompletableFuture that completes when format is set
     */
    CompletableFuture<Boolean> setImageFormat(String format);
    
    /**
     * Get the supported image formats.
     */
    List<String> getSupportedImageFormats();
    
    // Recording
    
    /**
     * Start recording video to a file.
     * @param outputPath The path where the video should be saved
     * @return CompletableFuture that completes when recording starts
     */
    CompletableFuture<Boolean> startRecording(String outputPath);
    
    /**
     * Stop recording video.
     * @return CompletableFuture that completes when recording stops
     */
    CompletableFuture<Boolean> stopRecording();
    
    /**
     * Get the current recording duration in milliseconds.
     */
    long getRecordingDuration();
    
    /**
     * Get the current recording file path.
     */
    String getRecordingPath();
    
    // Configuration
    
    /**
     * Get the current camera configuration.
     */
    CameraConfiguration getConfiguration();
    
    /**
     * Apply a new configuration to the camera.
     * @param configuration The new configuration
     * @return CompletableFuture that completes when configuration is applied
     */
    CompletableFuture<Boolean> configure(CameraConfiguration configuration);
    
    /**
     * Reset the camera to default configuration.
     * @return CompletableFuture that completes when reset is finished
     */
    CompletableFuture<Boolean> resetToDefaults();
    
    // Camera controls
    
    /**
     * Get the current brightness level (0.0 to 1.0).
     */
    double getBrightness();
    
    /**
     * Set the brightness level (0.0 to 1.0).
     * @param brightness The desired brightness level
     * @return CompletableFuture that completes when brightness is set
     */
    CompletableFuture<Boolean> setBrightness(double brightness);
    
    /**
     * Get the current contrast level (0.0 to 1.0).
     */
    double getContrast();
    
    /**
     * Set the contrast level (0.0 to 1.0).
     * @param contrast The desired contrast level
     * @return CompletableFuture that completes when contrast is set
     */
    CompletableFuture<Boolean> setContrast(double contrast);
    
    /**
     * Get the current saturation level (0.0 to 1.0).
     */
    double getSaturation();
    
    /**
     * Set the saturation level (0.0 to 1.0).
     * @param saturation The desired saturation level
     * @return CompletableFuture that completes when saturation is set
     */
    CompletableFuture<Boolean> setSaturation(double saturation);
    
    /**
     * Check if auto-exposure is enabled.
     */
    boolean isAutoExposureEnabled();
    
    /**
     * Enable or disable auto-exposure.
     * @param enabled Whether auto-exposure should be enabled
     * @return CompletableFuture that completes when setting is applied
     */
    CompletableFuture<Boolean> setAutoExposure(boolean enabled);
    
    /**
     * Get the current exposure time in milliseconds.
     */
    double getExposureTime();
    
    /**
     * Set the exposure time in milliseconds.
     * @param exposureMs The desired exposure time
     * @return CompletableFuture that completes when exposure is set
     */
    CompletableFuture<Boolean> setExposureTime(double exposureMs);
    
    // Calibration
    
    /**
     * Check if the camera supports calibration.
     */
    boolean supportsCalibration();
    
    /**
     * Start camera calibration process.
     * @return CompletableFuture that completes when calibration is finished
     */
    CompletableFuture<Boolean> calibrate();
    
    /**
     * Check if the camera is currently calibrated.
     */
    boolean isCalibrated();
    
    /**
     * Get the last calibration timestamp.
     */
    long getLastCalibrationTime();
    
    /**
     * Get camera calibration parameters (intrinsic matrix, distortion coefficients, etc.).
     */
    Map<String, Object> getCalibrationParameters();
    
    // Event handling
    
    /**
     * Add a listener for camera events.
     */
    void addListener(CameraEventListener listener);
    
    /**
     * Remove a listener for camera events.
     */
    void removeListener(CameraEventListener listener);
    
    /**
     * Remove all listeners.
     */
    void removeAllListeners();
    
    // Frame access
    
    /**
     * Capture a single frame (non-blocking).
     * @return CompletableFuture that completes with the captured frame
     */
    CompletableFuture<FrameData> captureFrame();
    
    /**
     * Get the latest frame (non-blocking).
     * @return The most recent frame, or null if none available
     */
    FrameData getLatestFrame();
    
    /**
     * Get buffered frames.
     * @param maxFrames Maximum number of frames to return
     * @return List of recent frames
     */
    List<FrameData> getBufferedFrames(int maxFrames);
    
    /**
     * Clear the internal frame buffer.
     */
    void clearBuffer();
    
    // Diagnostics
    
    /**
     * Run camera self-test.
     * @return CompletableFuture that completes with test results
     */
    CompletableFuture<Map<String, Object>> runSelfTest();
    
    /**
     * Get camera health information.
     */
    Map<String, Object> getHealthInfo();
    
    /**
     * Get camera firmware/software version.
     */
    String getVersion();
    
    /**
     * Get camera manufacturer information.
     */
    String getManufacturer();
    
    /**
     * Get camera model information.
     */
    String getModel();
    
    /**
     * Get camera serial number.
     */
    String getSerialNumber();
    
    // Thermal camera specific (if applicable)
    
    /**
     * Check if this camera supports temperature measurement.
     */
    default boolean supportsTemperatureMeasurement() {
        return getCameraType() == CameraType.THERMAL;
    }
    
    /**
     * Get temperature at a specific pixel coordinate (for thermal cameras).
     * @param x X coordinate
     * @param y Y coordinate
     * @return Temperature in Celsius, or null if not supported
     */
    default Double getTemperatureAt(int x, int y) {
        return null; // Override in thermal camera implementations
    }
    
    /**
     * Get the temperature range for thermal cameras.
     * @return Array with [min_temp, max_temp] in Celsius, or null if not supported
     */
    default double[] getTemperatureRange() {
        return null; // Override in thermal camera implementations
    }
    
    // Lifecycle
    
    /**
     * Initialize the camera (called once during setup).
     * @return CompletableFuture that completes when initialization is finished
     */
    CompletableFuture<Boolean> initialize();
    
    /**
     * Cleanup and release resources (called during shutdown).
     */
    void cleanup();
}