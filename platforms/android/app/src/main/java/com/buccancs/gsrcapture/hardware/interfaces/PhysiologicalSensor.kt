package com.buccancs.gsrcapture.hardware.interfaces

import java.io.File

/**
 * Generic interface for physiological sensors (GSR, PPG, etc.)
 * This interface abstracts hardware-specific implementations to allow
 * support for multiple sensor types beyond just Shimmer devices.
 */
interface PhysiologicalSensor {
    /**
     * Data class representing physiological sensor readings
     */
    data class SensorReading(
        val timestamp: Long,
        val gsrValue: Float? = null,
        val ppgValue: Float? = null,
        val heartRate: Int? = null,
        val additionalData: Map<String, Any> = emptyMap(),
    )

    /**
     * Callback interface for sensor events
     */
    interface SensorCallback {
        fun onDataReceived(reading: SensorReading)

        fun onConnectionStateChanged(isConnected: Boolean)

        fun onError(
            error: String,
            exception: Throwable?,
        )
    }

    /**
     * Initialize the sensor hardware
     * @return true if initialization was successful
     */
    fun initialize(): Boolean

    /**
     * Connect to the sensor device
     * @param deviceAddress Optional device address/identifier
     * @return true if connection was successful
     */
    fun connect(deviceAddress: String? = null): Boolean

    /**
     * Disconnect from the sensor device
     */
    fun disconnect()

    /**
     * Check if the sensor is currently connected
     * @return true if connected
     */
    fun isConnected(): Boolean

    /**
     * Start streaming data from the sensor
     * @param callback Callback to receive sensor data
     * @return true if streaming started successfully
     */
    fun startStreaming(callback: SensorCallback): Boolean

    /**
     * Stop streaming data from the sensor
     */
    fun stopStreaming()

    /**
     * Start recording sensor data to file
     * @param outputDir Directory to save recordings
     * @param sessionId Unique session identifier
     * @return true if recording started successfully
     */
    fun startRecording(
        outputDir: File,
        sessionId: String,
    ): Boolean

    /**
     * Stop recording sensor data
     */
    fun stopRecording()

    /**
     * Get the sensor's hardware information
     * @return Map containing hardware details (model, version, etc.)
     */
    fun getHardwareInfo(): Map<String, String>

    /**
     * Configure sensor-specific settings
     * @param settings Map of configuration parameters
     * @return true if configuration was successful
     */
    fun configure(settings: Map<String, Any>): Boolean

    /**
     * Get available configuration options for this sensor
     * @return Map of available settings and their possible values
     */
    fun getAvailableSettings(): Map<String, List<Any>>

    /**
     * Shutdown the sensor and release resources
     */
    fun shutdown()
}
