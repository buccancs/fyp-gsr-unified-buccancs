package com.gsrmultimodal.android

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class CameraHandler(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val window: Window? = null
) {

    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var recording: Recording? = null
    private var sessionId: String? = null
    private var deviceId: String = "unknown"
    private var recordingStartTime: Long = 0
    private var frameCount: Long = 0
    private var isCapturingFrames: Boolean = false
    private var frameDataWriter: BufferedWriter? = null
    private var camera: androidx.camera.core.Camera? = null
    private val syncHandler = Handler(Looper.getMainLooper())

    // Callback interface for recording events
    interface RecordingCallback {
        fun onRecordingStarted(filePath: String, timestamp: Long)
        fun onRecordingStopped(filePath: String, timestamp: Long)
        fun onRecordingError(error: String, timestamp: Long)
    }

    // Callback interface for raw frame events
    interface RawFrameCallback {
        fun onRawFrameReceived(
            frameData: ByteArray,
            width: Int,
            height: Int,
            timestamp: Long,
            frameNumber: Long
        )
        fun onFrameCaptureStarted(timestamp: Long)
        fun onFrameCaptureStopped(timestamp: Long)
        fun onFrameCaptureError(error: String, timestamp: Long)
    }

    private var recordingCallback: RecordingCallback? = null
    private var rawFrameCallback: RawFrameCallback? = null

    fun setRecordingCallback(callback: RecordingCallback) {
        this.recordingCallback = callback
    }

    fun setRawFrameCallback(callback: RawFrameCallback) {
        this.rawFrameCallback = callback
    }

    fun setSessionInfo(sessionId: String, deviceId: String) {
        this.sessionId = sessionId
        this.deviceId = deviceId
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Setup ImageAnalysis for raw frame capture
            imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(1920, 1080))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        processRawFrame(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture, imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("CameraHandler", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun startRecording() {
        val videoCapture = this.videoCapture ?: return

        // Generate unified filename with session ID and device ID
        val timestamp = System.currentTimeMillis()
        recordingStartTime = timestamp
        val sessionPrefix = sessionId ?: "session_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))}"
        val name = "${sessionPrefix}_${deviceId}_rgb_video"

        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        recording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .withAudioEnabled()
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        val startTimestamp = System.currentTimeMillis()
                        Log.d("CameraHandler", "Recording started at timestamp: $startTimestamp")
                        recordingCallback?.onRecordingStarted(name, startTimestamp)
                    }
                    is VideoRecordEvent.Finalize -> {
                        val endTimestamp = System.currentTimeMillis()
                        if (!recordEvent.hasError()) {
                            val filePath = recordEvent.outputResults.outputUri.toString()
                            Log.d("CameraHandler", "Video capture succeeded: $filePath")
                            recordingCallback?.onRecordingStopped(filePath, endTimestamp)
                        } else {
                            recording?.close()
                            recording = null
                            val errorMsg = "Video capture error: ${recordEvent.error}"
                            Log.e("CameraHandler", errorMsg)
                            recordingCallback?.onRecordingError(errorMsg, endTimestamp)
                        }
                    }
                }
            }
    }

    fun stopRecording() {
        recording?.let { activeRecording ->
            val stopTimestamp = System.currentTimeMillis()
            Log.d("CameraHandler", "Stopping recording at timestamp: $stopTimestamp")
            activeRecording.stop()
            recording = null
        }
    }

    fun isRecording(): Boolean {
        return recording != null
    }

    fun getRecordingDuration(): Long {
        return if (isRecording() && recordingStartTime > 0) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0
        }
    }

    /**
     * Start capturing raw frames and logging frame metadata
     */
    fun startFrameCapture() {
        if (isCapturingFrames) {
            Log.w("CameraHandler", "Frame capture already started")
            return
        }

        try {
            sessionId?.let { session ->
                startFrameDataLogging(session)
            }

            isCapturingFrames = true
            frameCount = 0
            val timestamp = System.currentTimeMillis()
            rawFrameCallback?.onFrameCaptureStarted(timestamp)
            Log.d("CameraHandler", "Frame capture started at timestamp: $timestamp")
        } catch (e: Exception) {
            Log.e("CameraHandler", "Error starting frame capture", e)
            rawFrameCallback?.onFrameCaptureError("Failed to start frame capture: ${e.message}", System.currentTimeMillis())
        }
    }

    /**
     * Stop capturing raw frames and close logging
     */
    fun stopFrameCapture() {
        if (!isCapturingFrames) {
            Log.w("CameraHandler", "Frame capture not started")
            return
        }

        try {
            isCapturingFrames = false
            stopFrameDataLogging()
            val timestamp = System.currentTimeMillis()
            rawFrameCallback?.onFrameCaptureStopped(timestamp)
            Log.d("CameraHandler", "Frame capture stopped at timestamp: $timestamp")
        } catch (e: Exception) {
            Log.e("CameraHandler", "Error stopping frame capture", e)
            rawFrameCallback?.onFrameCaptureError("Failed to stop frame capture: ${e.message}", System.currentTimeMillis())
        }
    }

    /**
     * Process raw frame from ImageAnalysis
     */
    private fun processRawFrame(imageProxy: ImageProxy) {
        try {
            val timestamp = System.currentTimeMillis()

            if (isCapturingFrames) {
                frameCount++

                // Extract frame data from ImageProxy
                val buffer: ByteBuffer = imageProxy.planes[0].buffer
                val frameData = ByteArray(buffer.remaining())
                buffer.get(frameData)

                val width = imageProxy.width
                val height = imageProxy.height

                // Log frame metadata if logging is active
                frameDataWriter?.let { writer ->
                    writer.write("$frameCount,$timestamp,$width,$height,${frameData.size}\n")
                    writer.flush()
                }

                // Notify callback with raw frame data
                rawFrameCallback?.onRawFrameReceived(frameData, width, height, timestamp, frameCount)
            }

        } catch (e: Exception) {
            Log.e("CameraHandler", "Error processing raw frame", e)
            rawFrameCallback?.onFrameCaptureError("Frame processing error: ${e.message}", System.currentTimeMillis())
        } finally {
            imageProxy.close()
        }
    }

    /**
     * Start logging frame metadata to CSV file
     */
    private fun startFrameDataLogging(sessionId: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "${sessionId}_${deviceId}_rgb_frames.csv"
            val file = File(context.getExternalFilesDir(null), filename)
            frameDataWriter = BufferedWriter(FileWriter(file))
            frameDataWriter?.write("frame_number,timestamp_ms,width,height,frame_size_bytes\n")
            Log.d("CameraHandler", "Started RGB frame data logging to: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("CameraHandler", "Error starting frame data logging", e)
            rawFrameCallback?.onFrameCaptureError("Failed to start frame logging: ${e.message}", System.currentTimeMillis())
        }
    }

    /**
     * Stop logging frame metadata
     */
    private fun stopFrameDataLogging() {
        try {
            frameDataWriter?.close()
            frameDataWriter = null
            Log.d("CameraHandler", "RGB frame data logging stopped")
        } catch (e: IOException) {
            Log.e("CameraHandler", "Error stopping frame data logging", e)
        }
    }

    /**
     * Check if frame capture is active
     */
    fun isCapturingFrames(): Boolean {
        return isCapturingFrames
    }

    /**
     * Get current frame count
     */
    fun getFrameCount(): Long {
        return frameCount
    }

    /**
     * Trigger a sync marker using screen flash
     * This creates a visual marker that can be detected in all camera feeds for synchronization
     */
    fun triggerScreenFlashSyncMarker(durationMs: Long = 100) {
        window?.let { win ->
            syncHandler.post {
                try {
                    val timestamp = System.currentTimeMillis()
                    Log.d("CameraHandler", "Screen flash sync marker triggered at timestamp: $timestamp")

                    // Store original brightness
                    val originalBrightness = win.attributes.screenBrightness
                    val originalFlags = win.attributes.flags

                    // Set screen to maximum brightness and white
                    val layoutParams = win.attributes
                    layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                    layoutParams.flags = layoutParams.flags or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    win.attributes = layoutParams

                    // Set background to white
                    win.decorView.setBackgroundColor(Color.WHITE)

                    // Log sync marker event
                    frameDataWriter?.let { writer ->
                        writer.write("SYNC_MARKER,SCREEN_FLASH,$timestamp,${durationMs}\n")
                        writer.flush()
                    }

                    // Restore original settings after duration
                    syncHandler.postDelayed({
                        try {
                            val restoreParams = win.attributes
                            restoreParams.screenBrightness = originalBrightness
                            restoreParams.flags = originalFlags
                            win.attributes = restoreParams
                            win.decorView.setBackgroundColor(Color.TRANSPARENT)

                            Log.d("CameraHandler", "Screen flash sync marker completed")
                        } catch (e: Exception) {
                            Log.e("CameraHandler", "Error restoring screen settings", e)
                        }
                    }, durationMs)

                } catch (e: Exception) {
                    Log.e("CameraHandler", "Error triggering screen flash sync marker", e)
                }
            }
        } ?: run {
            Log.w("CameraHandler", "Window not available for screen flash sync marker")
        }
    }

    /**
     * Trigger a sync marker using camera flash (if available)
     * This creates a light marker that can be detected in other camera feeds
     */
    fun triggerCameraFlashSyncMarker(durationMs: Long = 100) {
        camera?.let { cam ->
            try {
                val timestamp = System.currentTimeMillis()
                Log.d("CameraHandler", "Camera flash sync marker triggered at timestamp: $timestamp")

                // Check if torch is available
                val cameraInfo = cam.cameraInfo
                if (cameraInfo.hasFlashUnit()) {
                    // Enable torch
                    cam.cameraControl.enableTorch(true)

                    // Log sync marker event
                    frameDataWriter?.let { writer ->
                        writer.write("SYNC_MARKER,CAMERA_FLASH,$timestamp,${durationMs}\n")
                        writer.flush()
                    }

                    // Disable torch after duration
                    syncHandler.postDelayed({
                        try {
                            cam.cameraControl.enableTorch(false)
                            Log.d("CameraHandler", "Camera flash sync marker completed")
                        } catch (e: Exception) {
                            Log.e("CameraHandler", "Error disabling camera flash", e)
                        }
                    }, durationMs)

                } else {
                    Log.w("CameraHandler", "Camera flash not available for sync marker")
                }

            } catch (e: Exception) {
                Log.e("CameraHandler", "Error triggering camera flash sync marker", e)
            }
        } ?: run {
            Log.w("CameraHandler", "Camera not available for flash sync marker")
        }
    }

    /**
     * Trigger both screen and camera flash sync markers simultaneously
     * This provides maximum visibility across all recording devices
     */
    fun triggerCombinedSyncMarker(durationMs: Long = 100) {
        val timestamp = System.currentTimeMillis()
        Log.d("CameraHandler", "Combined sync marker triggered at timestamp: $timestamp")

        // Log combined sync marker event
        frameDataWriter?.let { writer ->
            writer.write("SYNC_MARKER,COMBINED,$timestamp,${durationMs}\n")
            writer.flush()
        }

        // Trigger both markers
        triggerScreenFlashSyncMarker(durationMs)
        triggerCameraFlashSyncMarker(durationMs)
    }

    /**
     * Add a timestamp marker to the frame log for external synchronization events
     */
    fun addTimestampMarker(markerType: String, description: String = "") {
        val timestamp = System.currentTimeMillis()
        frameDataWriter?.let { writer ->
            writer.write("TIMESTAMP_MARKER,$markerType,$timestamp,$description\n")
            writer.flush()
        }
        Log.d("CameraHandler", "Timestamp marker added: $markerType at $timestamp - $description")
    }
}
