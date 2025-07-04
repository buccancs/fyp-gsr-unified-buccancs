package com.buccancs.gsrcapture.controller

import android.content.Context
import android.os.Environment
import android.util.Log
import com.buccancs.gsrcapture.audio.AudioRecorder
import com.buccancs.gsrcapture.camera.RgbCameraManager
import com.buccancs.gsrcapture.camera.ThermalCameraManager
import com.buccancs.gsrcapture.sensor.GsrSensorManager
import com.buccancs.gsrcapture.utils.TimeManager
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Coordinates all recording components and provides a unified interface for
 * starting and stopping recordings.
 */
class RecordingController(private val context: Context) {
    private val TAG = "RecordingController"

    // Recording components
    private lateinit var rgbCameraManager: RgbCameraManager
    private lateinit var thermalCameraManager: ThermalCameraManager
    private lateinit var gsrSensorManager: GsrSensorManager
    private lateinit var audioRecorder: AudioRecorder

    // Executors for background tasks
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val sensorExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val audioExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Recording state
    private val isRecording = AtomicBoolean(false)
    private var currentSessionId: String? = null
    private var outputDirectory: File? = null

    // Callbacks
    private var recordingStateCallback: ((Boolean) -> Unit)? = null
    private var gsrValueCallback: ((Float) -> Unit)? = null
    private var heartRateCallback: ((Int) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    /**
     * Initializes the recording controller and all its components.
     */
    fun initialize() {
        Log.d(TAG, "Initializing RecordingController")

        // Initialize TimeManager
        TimeManager.initSession()

        // Create output directory
        createOutputDirectory()

        // Initialize components
        initializeComponents()
    }

    /**
     * Creates the output directory for storing recorded data.
     */
    private fun createOutputDirectory() {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: context.filesDir

        val appDir = File(baseDir, "GSRCapture")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }

        outputDirectory = appDir
        Log.d(TAG, "Output directory: ${outputDirectory?.absolutePath}")
    }

    /**
     * Initializes all recording components.
     */
    private fun initializeComponents() {
        // Initialize RGB camera manager
        rgbCameraManager = RgbCameraManager(context, context as androidx.lifecycle.LifecycleOwner, cameraExecutor)

        // Initialize thermal camera manager
        thermalCameraManager = ThermalCameraManager(context, cameraExecutor)
        thermalCameraManager.initialize()

        // Initialize GSR sensor manager
        gsrSensorManager = GsrSensorManager(context, sensorExecutor)
        gsrSensorManager.initialize()

        // Set up GSR callbacks
        gsrSensorManager.setGsrCallback { value ->
            gsrValueCallback?.invoke(value)
        }

        gsrSensorManager.setHeartRateCallback { value ->
            heartRateCallback?.invoke(value)
        }

        // Initialize audio recorder
        audioRecorder = AudioRecorder(context, audioExecutor)
        audioRecorder.initialize()
    }

    /**
     * Connects to the thermal camera.
     * @return True if connection was successful, false otherwise
     */
    fun connectThermalCamera(): Boolean {
        return thermalCameraManager.connectToCamera()
    }

    /**
     * Connects to the GSR sensor.
     * @param deviceAddress MAC address of the GSR sensor (optional)
     * @return True if connection was successful, false otherwise
     */
    fun connectGsrSensor(deviceAddress: String? = null): Boolean {
        return gsrSensorManager.connectToSensor(deviceAddress)
    }

    /**
     * Sets the preview view for the RGB camera.
     * @param previewView PreviewView for displaying the RGB camera feed
     */
    fun setRgbPreviewView(previewView: androidx.camera.view.PreviewView) {
        rgbCameraManager.startCamera(previewView)
    }

    /**
     * Sets the preview view for the thermal camera.
     * @param textureView TextureView for displaying the thermal camera feed
     */
    fun setThermalPreviewView(textureView: android.view.TextureView) {
        thermalCameraManager.setPreviewView(textureView)
    }

    /**
     * Starts recording all data streams.
     * @return True if recording started successfully, false otherwise
     */
    fun startRecording(): Boolean {
        if (isRecording.get()) {
            Log.d(TAG, "Already recording")
            return true
        }

        // Generate a new session ID
        currentSessionId = generateSessionId()

        // Create session directory
        val sessionDir = File(outputDirectory, currentSessionId!!)
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        Log.d(TAG, "Starting recording session: $currentSessionId")

        // Reset TimeManager for this session
        TimeManager.initSession()

        // Start all recordings
        var success = true

        // Start RGB video recording
        if (!rgbCameraManager.startRecording(sessionDir, currentSessionId!!)) {
            Log.e(TAG, "Failed to start RGB video recording")
            success = false
        }

        // Start RGB raw image capture
        if (!rgbCameraManager.startRawImageCapture(sessionDir, currentSessionId!!)) {
            Log.e(TAG, "Failed to start RGB raw image capture")
            success = false
        }

        // Start thermal video recording
        if (!thermalCameraManager.startRecording(sessionDir, currentSessionId!!)) {
            Log.e(TAG, "Failed to start thermal video recording")
            success = false
        }

        // Start GSR recording
        if (!gsrSensorManager.startRecording(sessionDir, currentSessionId!!)) {
            Log.e(TAG, "Failed to start GSR recording")
            success = false
        }

        // Start audio recording
        if (!audioRecorder.startRecording(sessionDir, currentSessionId!!)) {
            Log.e(TAG, "Failed to start audio recording")
            success = false
        }

        // Create session metadata file
        createSessionMetadata(sessionDir)

        if (success) {
            isRecording.set(true)
            recordingStateCallback?.invoke(true)
            Log.d(TAG, "Recording started successfully")
        } else {
            // If any component failed to start, stop all recordings
            stopRecording()
            errorCallback?.invoke("Failed to start recording")
        }

        return success
    }

    /**
     * Stops recording all data streams.
     */
    fun stopRecording() {
        if (!isRecording.getAndSet(false)) {
            return
        }

        Log.d(TAG, "Stopping recording session: $currentSessionId")

        // Stop all recordings
        rgbCameraManager.stopRecording()
        rgbCameraManager.stopRawImageCapture()
        thermalCameraManager.stopRecording()
        gsrSensorManager.stopRecording()
        audioRecorder.stopRecording()

        recordingStateCallback?.invoke(false)
        Log.d(TAG, "Recording stopped")
    }

    /**
     * Generates a unique session ID.
     * @return Session ID string
     */
    private fun generateSessionId(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val randomPart = UUID.randomUUID().toString().substring(0, 8)
        return "session_${timestamp}_$randomPart"
    }

    /**
     * Creates a metadata file for the recording session.
     * @param sessionDir Directory for the current session
     */
    private fun createSessionMetadata(sessionDir: File) {
        try {
            val metadataFile = File(sessionDir, "session_metadata.json")
            val writer = FileWriter(metadataFile)

            // Create JSON metadata
            val metadata = """
                {
                    "sessionId": "$currentSessionId",
                    "timestamp": "${TimeManager.getCurrentTimestamp()}",
                    "timestampEpochMillis": ${System.currentTimeMillis()},
                    "sessionStartTimeNanos": ${TimeManager.getCurrentTimestampNanos()},
                    "device": {
                        "manufacturer": "${android.os.Build.MANUFACTURER}",
                        "model": "${android.os.Build.MODEL}",
                        "androidVersion": "${android.os.Build.VERSION.RELEASE}"
                    },
                    "components": {
                        "rgbCamera": true,
                        "thermalCamera": ${thermalCameraManager.isConnected()},
                        "gsrSensor": ${gsrSensorManager.isConnected()},
                        "audio": true
                    },
                    "settings": {
                        "rgbVideoQuality": "1080p",
                        "audioSampleRate": 44100,
                        "gsrSampleRate": 128
                    }
                }
            """.trimIndent()

            writer.write(metadata)
            writer.close()

            Log.d(TAG, "Created session metadata file: ${metadataFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating session metadata", e)
        }
    }

    /**
     * Sets a callback for recording state changes.
     * @param callback Function to call when recording state changes
     */
    fun setRecordingStateCallback(callback: (Boolean) -> Unit) {
        recordingStateCallback = callback
    }

    /**
     * Sets a callback for GSR value updates.
     * @param callback Function to call with each new GSR value
     */
    fun setGsrValueCallback(callback: (Float) -> Unit) {
        gsrValueCallback = callback
    }

    /**
     * Sets a callback for heart rate updates.
     * @param callback Function to call with each new heart rate value
     */
    fun setHeartRateCallback(callback: (Int) -> Unit) {
        heartRateCallback = callback
    }

    /**
     * Sets a callback for error notifications.
     * @param callback Function to call when an error occurs
     */
    fun setErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    /**
     * Releases all resources.
     */
    fun shutdown() {
        stopRecording()

        rgbCameraManager.shutdown()
        thermalCameraManager.shutdown()
        gsrSensorManager.shutdown()
        audioRecorder.shutdown()

        cameraExecutor.shutdown()
        sensorExecutor.shutdown()
        audioExecutor.shutdown()

        Log.d(TAG, "RecordingController shut down")
    }

    /**
     * Extension function to check if the thermal camera is connected.
     */
    private fun ThermalCameraManager.isConnected(): Boolean {
        // This is a placeholder. In a real implementation, you would have a method
        // in ThermalCameraManager to check if the camera is connected.
        return true
    }

    /**
     * Extension function to check if the GSR sensor is connected.
     */
    private fun GsrSensorManager.isConnected(): Boolean {
        // This is a placeholder. In a real implementation, you would have a method
        // in GsrSensorManager to check if the sensor is connected.
        return true
    }
}
