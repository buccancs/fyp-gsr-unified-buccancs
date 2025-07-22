package com.buccancs.gsr.shared.hardware.interfaces;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Generic interface for all sensor types in the GSR system.
 * This interface provides a common abstraction layer that allows the system
 * to work with different sensor hardware without tight coupling.
 * 
 * Supported sensor types include:
 * - GSR (Galvanic Skin Response) sensors
 * - Heart rate sensors
 * - Temperature sensors
 * - Accelerometer sensors
 * - Custom sensor implementations
 */
public interface SensorInterface {
    
    /**
     * Sensor types supported by the system.
     */
    enum SensorType {
        GSR("Galvanic Skin Response"),
        HEART_RATE("Heart Rate"),
        TEMPERATURE("Temperature"),
        ACCELEROMETER("Accelerometer"),
        GYROSCOPE("Gyroscope"),
        MAGNETOMETER("Magnetometer"),
        PRESSURE("Pressure"),
        CUSTOM("Custom Sensor");
        
        private final String displayName;
        
        SensorType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Connection types supported by sensors.
     */
    enum ConnectionType {
        BLUETOOTH("Bluetooth"),
        USB("USB"),
        WIFI("WiFi"),
        SERIAL("Serial"),
        I2C("I2C"),
        SPI("SPI"),
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
     * Sensor status enumeration.
     */
    enum SensorStatus {
        DISCONNECTED("Disconnected"),
        CONNECTING("Connecting"),
        CONNECTED("Connected"),
        STREAMING("Streaming"),
        ERROR("Error"),
        CALIBRATING("Calibrating");
        
        private final String displayName;
        
        SensorStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Data sample from a sensor.
     */
    class SensorData {
        private final long timestamp;
        private final SensorType sensorType;
        private final Map<String, Object> values;
        private final String units;
        private final double quality; // 0.0 to 1.0, where 1.0 is perfect quality
        
        public SensorData(long timestamp, SensorType sensorType, Map<String, Object> values, String units, double quality) {
            this.timestamp = timestamp;
            this.sensorType = sensorType;
            this.values = values;
            this.units = units;
            this.quality = Math.max(0.0, Math.min(1.0, quality)); // Clamp to [0,1]
        }
        
        public long getTimestamp() { return timestamp; }
        public SensorType getSensorType() { return sensorType; }
        public Map<String, Object> getValues() { return values; }
        public String getUnits() { return units; }
        public double getQuality() { return quality; }
        
        @Override
        public String toString() {
            return String.format("SensorData{timestamp=%d, type=%s, values=%s, units=%s, quality=%.2f}", 
                timestamp, sensorType, values, units, quality);
        }
    }
    
    /**
     * Sensor configuration parameters.
     */
    class SensorConfiguration {
        private final Map<String, Object> parameters;
        
        public SensorConfiguration(Map<String, Object> parameters) {
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
     * Listener interface for sensor events.
     */
    interface SensorEventListener {
        /**
         * Called when new sensor data is available.
         */
        void onDataReceived(SensorData data);
        
        /**
         * Called when sensor status changes.
         */
        void onStatusChanged(SensorStatus oldStatus, SensorStatus newStatus);
        
        /**
         * Called when a sensor error occurs.
         */
        void onError(String errorMessage, Exception cause);
        
        /**
         * Called when sensor connection is lost.
         */
        void onConnectionLost(String reason);
    }
    
    // Core sensor operations
    
    /**
     * Get the unique identifier for this sensor.
     */
    String getSensorId();
    
    /**
     * Get the human-readable name of this sensor.
     */
    String getSensorName();
    
    /**
     * Get the type of this sensor.
     */
    SensorType getSensorType();
    
    /**
     * Get the connection type used by this sensor.
     */
    ConnectionType getConnectionType();
    
    /**
     * Get the current status of the sensor.
     */
    SensorStatus getStatus();
    
    /**
     * Get sensor capabilities and supported features.
     */
    Map<String, Object> getCapabilities();
    
    /**
     * Check if the sensor is currently connected.
     */
    boolean isConnected();
    
    /**
     * Check if the sensor is currently streaming data.
     */
    boolean isStreaming();
    
    // Connection management
    
    /**
     * Connect to the sensor asynchronously.
     * @return CompletableFuture that completes when connection is established
     */
    CompletableFuture<Boolean> connect();
    
    /**
     * Disconnect from the sensor asynchronously.
     * @return CompletableFuture that completes when disconnection is finished
     */
    CompletableFuture<Boolean> disconnect();
    
    /**
     * Reconnect to the sensor (disconnect then connect).
     * @return CompletableFuture that completes when reconnection is finished
     */
    default CompletableFuture<Boolean> reconnect() {
        return disconnect().thenCompose(success -> connect());
    }
    
    // Data streaming
    
    /**
     * Start streaming data from the sensor.
     * @return CompletableFuture that completes when streaming starts
     */
    CompletableFuture<Boolean> startStreaming();
    
    /**
     * Stop streaming data from the sensor.
     * @return CompletableFuture that completes when streaming stops
     */
    CompletableFuture<Boolean> stopStreaming();
    
    /**
     * Get the current sampling rate in Hz.
     */
    double getSamplingRate();
    
    /**
     * Set the sampling rate in Hz.
     * @param rateHz The desired sampling rate
     * @return CompletableFuture that completes when rate is set
     */
    CompletableFuture<Boolean> setSamplingRate(double rateHz);
    
    /**
     * Get the supported sampling rates.
     */
    List<Double> getSupportedSamplingRates();
    
    // Configuration
    
    /**
     * Get the current sensor configuration.
     */
    SensorConfiguration getConfiguration();
    
    /**
     * Apply a new configuration to the sensor.
     * @param configuration The new configuration
     * @return CompletableFuture that completes when configuration is applied
     */
    CompletableFuture<Boolean> configure(SensorConfiguration configuration);
    
    /**
     * Reset the sensor to default configuration.
     * @return CompletableFuture that completes when reset is finished
     */
    CompletableFuture<Boolean> resetToDefaults();
    
    // Calibration
    
    /**
     * Check if the sensor supports calibration.
     */
    boolean supportsCalibration();
    
    /**
     * Start sensor calibration process.
     * @return CompletableFuture that completes when calibration is finished
     */
    CompletableFuture<Boolean> calibrate();
    
    /**
     * Check if the sensor is currently calibrated.
     */
    boolean isCalibrated();
    
    /**
     * Get the last calibration timestamp.
     */
    long getLastCalibrationTime();
    
    // Event handling
    
    /**
     * Add a listener for sensor events.
     */
    void addListener(SensorEventListener listener);
    
    /**
     * Remove a listener for sensor events.
     */
    void removeListener(SensorEventListener listener);
    
    /**
     * Remove all listeners.
     */
    void removeAllListeners();
    
    // Data access
    
    /**
     * Get the latest sensor data (non-blocking).
     * @return The most recent sensor data, or null if none available
     */
    SensorData getLatestData();
    
    /**
     * Get buffered sensor data.
     * @param maxSamples Maximum number of samples to return
     * @return List of recent sensor data samples
     */
    List<SensorData> getBufferedData(int maxSamples);
    
    /**
     * Clear the internal data buffer.
     */
    void clearBuffer();
    
    // Diagnostics
    
    /**
     * Run sensor self-test.
     * @return CompletableFuture that completes with test results
     */
    CompletableFuture<Map<String, Object>> runSelfTest();
    
    /**
     * Get sensor health information.
     */
    Map<String, Object> getHealthInfo();
    
    /**
     * Get sensor firmware/software version.
     */
    String getVersion();
    
    /**
     * Get sensor manufacturer information.
     */
    String getManufacturer();
    
    /**
     * Get sensor model information.
     */
    String getModel();
    
    /**
     * Get sensor serial number.
     */
    String getSerialNumber();
    
    // Lifecycle
    
    /**
     * Initialize the sensor (called once during setup).
     * @return CompletableFuture that completes when initialization is finished
     */
    CompletableFuture<Boolean> initialize();
    
    /**
     * Cleanup and release resources (called during shutdown).
     */
    void cleanup();
}