package com.buccancs.gsrcapture.hardware.interfaces

import android.graphics.Bitmap
import android.view.TextureView
import java.io.File

/**
 * Generic interface for thermal cameras
 * This interface abstracts hardware-specific implementations to allow
 * support for multiple thermal camera types beyond just Topdon devices.
 */
interface ThermalCamera {
    /**
     * Data class representing thermal camera specifications
     */
    data class CameraSpecs(
        val width: Int,
        val height: Int,
        val temperatureRange: Pair<Float, Float>, // Min and max temperature in Celsius
        val frameRate: Float,
        val additionalSpecs: Map<String, Any> = emptyMap(),
    )

    /**
     * Data class representing thermal frame data
     */
    data class ThermalFrame(
        val timestamp: Long,
        val bitmap: Bitmap,
        val temperatureData: Array<FloatArray>? = null, // Raw temperature matrix
        val metadata: Map<String, Any> = emptyMap(),
    )

    /**
     * Callback interface for thermal camera events
     */
    interface ThermalCameraCallback {
        fun onFrameReceived(frame: ThermalFrame)

        fun onConnectionStateChanged(isConnected: Boolean)

        fun onError(
            error: String,
            exception: Throwable?,
        )
    }

    /**
     * Initialize the thermal camera hardware
     * @return true if initialization was successful
     */
    fun initialize(): Boolean

    /**
     * Connect to the thermal camera device
     * @param deviceIdentifier Optional device identifier (USB path, serial number, etc.)
     * @return true if connection was successful
     */
    fun connect(deviceIdentifier: String? = null): Boolean

    /**
     * Disconnect from the thermal camera device
     */
    fun disconnect()

    /**
     * Check if the camera is currently connected
     * @return true if connected
     */
    fun isConnected(): Boolean

    /**
     * Start streaming frames from the camera
     * @param callback Callback to receive thermal frames
     * @return true if streaming started successfully
     */
    fun startStreaming(callback: ThermalCameraCallback): Boolean

    /**
     * Stop streaming frames from the camera
     */
    fun stopStreaming()

    /**
     * Set the preview view for displaying thermal frames
     * @param view TextureView to display the thermal preview
     */
    fun setPreviewView(view: TextureView)

    /**
     * Start recording thermal frames to file
     * @param outputDir Directory to save recordings
     * @param sessionId Unique session identifier
     * @return true if recording started successfully
     */
    fun startRecording(
        outputDir: File,
        sessionId: String,
    ): Boolean

    /**
     * Stop recording thermal frames
     */
    fun stopRecording()

    /**
     * Get the camera's specifications
     * @return CameraSpecs containing camera capabilities
     */
    fun getCameraSpecs(): CameraSpecs

    /**
     * Get the camera's hardware information
     * @return Map containing hardware details (model, version, etc.)
     */
    fun getHardwareInfo(): Map<String, String>

    /**
     * Configure camera-specific settings
     * @param settings Map of configuration parameters
     * @return true if configuration was successful
     */
    fun configure(settings: Map<String, Any>): Boolean

    /**
     * Get available configuration options for this camera
     * @return Map of available settings and their possible values
     */
    fun getAvailableSettings(): Map<String, List<Any>>

    /**
     * Capture a single thermal frame
     * @return ThermalFrame or null if capture failed
     */
    fun captureFrame(): ThermalFrame?

    /**
     * Set frame callback for receiving individual frames
     * @param callback Callback to receive frames
     */
    fun setFrameCallback(callback: (ThermalFrame) -> Unit)

    /**
     * Shutdown the camera and release resources
     */
    fun shutdown()
}
