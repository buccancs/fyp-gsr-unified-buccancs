package com.fpygsrunified.android.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.MediaRecorder
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fpygsrunified.android.core.EnhancedLogger
import com.fpygsrunified.android.core.PerformanceMonitor
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Enhanced Camera Handler with comprehensive video recording and frame capture capabilities.
 * Supports both video recording and raw frame capture with synchronized timestamps.
 * 
 * Features:
 * - High-quality video recording with configurable settings
 * - Raw frame capture with timestamp synchronization
 * - Performance monitoring and logging
 * - Callback-based event handling
 * - Session management for organized data storage
 */
class CameraHandler(
    private val context: Context,
    private val previewView: PreviewView,
    private val lifecycleOwner: LifecycleOwner
) {
    companion object {
        private const val TAG = "CameraHandler"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val VIDEO_QUALITY = Quality.HD // 720p for balance of quality and performance
        private const val FRAME_RATE = 30
    }

    // Core camera components
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null

    // Recording state
    private var currentRecording: Recording? = null
    private val isRecording = AtomicBoolean(false)
    private val recordingStartTime = AtomicLong(0)

    // Frame capture state
    private val isCapturingFrames = AtomicBoolean(false)
    private val frameCount = AtomicInteger(0)
    private val captureStartTime = AtomicLong(0)

    // Session management
    private var currentSessionId: String? = null
    private var sessionOutputDir: File? = null

    // Callbacks
    private var recordingCallback: RecordingCallback? = null
    private var rawFrameCallback: RawFrameCallback? = null

    // Background processing
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val frameProcessingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Timestamp markers for synchronization
    private val timestampMarkers = mutableListOf<TimestampMarker>()

    // Performance monitoring
    private val performanceMonitor = PerformanceMonitor
    private val logger = EnhancedLogger.getInstance(context)

    /**
     * Callback interface for recording events
     */
    interface RecordingCallback {
        fun onRecordingStarted(filePath: String)
        fun onRecordingStopped(filePath: String)
        fun onRecordingError(error: String)
    }

    /**
     * Callback interface for raw frame events
     */
    interface RawFrameCallback {
        fun onFrameCaptureStarted()
        fun onFrameCaptured(bitmap: Bitmap, timestamp: Long, frameNumber: Int)
        fun onFrameCaptureStopped()
        fun onFrameCaptureError(error: String)
    }

    /**
     * Data class for timestamp markers
     */
    data class TimestampMarker(
        val timestamp: Long,
        val description: String,
        val frameNumber: Int? = null
    )

    /**
     * Initialize the camera system
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.Main) {
        return@withContext try {
            logger.i(TAG, "Initializing camera system")
            performanceMonitor.startTimer("camera_initialization")

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProvider = cameraProviderFuture.get()

            setupCamera()

            performanceMonitor.stopTimer("camera_initialization")
            logger.i(TAG, "Camera system initialized successfully")
            true
        } catch (e: Exception) {
            logger.e(TAG, "Failed to initialize camera", e)
            false
        }
    }

    /**
     * Set up camera with preview, video capture, and image analysis
     */
    private fun setupCamera() {
        val cameraProvider = this.cameraProvider ?: return

        // Preview
        preview = Preview.Builder()
            .setTargetResolution(Size(1280, 720))
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        // Video capture
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(VIDEO_QUALITY))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        // Image analysis for frame capture
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(1280, 720))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    if (isCapturingFrames.get()) {
                        processFrame(imageProxy)
                    }
                    imageProxy.close()
                }
            }

        // Select back camera as default
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                videoCapture,
                imageAnalysis
            )

        } catch (e: Exception) {
            logger.e(TAG, "Camera binding failed", e)
        }
    }

    /**
     * Set recording callback
     */
    fun setRecordingCallback(callback: RecordingCallback?) {
        this.recordingCallback = callback
    }

    /**
     * Set raw frame callback
     */
    fun setRawFrameCallback(callback: RawFrameCallback?) {
        this.rawFrameCallback = callback
    }

    /**
     * Set session information for organized data storage
     */
    fun setSessionInfo(sessionId: String, outputDir: File) {
        this.currentSessionId = sessionId
        this.sessionOutputDir = outputDir

        // Create session directory if it doesn't exist
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        logger.i(TAG, "Session set: $sessionId, Output: ${outputDir.absolutePath}")
    }

    /**
     * Start video recording
     */
    fun startRecording() {
        if (isRecording.get()) {
            logger.w(TAG, "Recording already in progress")
            return
        }

        val videoCapture = this.videoCapture ?: run {
            logger.e(TAG, "VideoCapture not initialized")
            recordingCallback?.onRecordingError("Camera not initialized")
            return
        }

        val outputDir = sessionOutputDir ?: File(context.getExternalFilesDir(null), "videos")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        currentRecording = videoCapture.output
            .prepareRecording(context, mediaStoreOutputOptions)
            .apply {
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        isRecording.set(true)
                        recordingStartTime.set(System.currentTimeMillis())
                        val filePath = recordEvent.outputOptions.toString()
                        logger.i(TAG, "Recording started: $filePath")
                        recordingCallback?.onRecordingStarted(filePath)
                        performanceMonitor.incrementCounter("video_recordings_started")
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording.set(false)
                        if (!recordEvent.hasError()) {
                            val filePath = recordEvent.outputOptions.toString()
                            logger.i(TAG, "Recording completed: $filePath")
                            recordingCallback?.onRecordingStopped(filePath)
                            performanceMonitor.incrementCounter("video_recordings_completed")
                        } else {
                            val error = "Recording error: ${recordEvent.error}"
                            logger.e(TAG, error)
                            recordingCallback?.onRecordingError(error)
                            performanceMonitor.incrementCounter("video_recording_errors")
                        }
                    }
                }
            }
    }

    /**
     * Stop video recording
     */
    fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
    }

    /**
     * Start capturing raw frames
     */
    fun startFrameCapture() {
        if (isCapturingFrames.get()) {
            logger.w(TAG, "Frame capture already in progress")
            return
        }

        isCapturingFrames.set(true)
        frameCount.set(0)
        captureStartTime.set(System.currentTimeMillis())
        timestampMarkers.clear()

        logger.i(TAG, "Frame capture started")
        rawFrameCallback?.onFrameCaptureStarted()
        performanceMonitor.incrementCounter("frame_capture_sessions_started")
    }

    /**
     * Stop capturing raw frames
     */
    fun stopFrameCapture() {
        if (!isCapturingFrames.get()) {
            return
        }

        isCapturingFrames.set(false)

        logger.i(TAG, "Frame capture stopped. Total frames: ${frameCount.get()}")
        rawFrameCallback?.onFrameCaptureStopped()
        performanceMonitor.incrementCounter("frame_capture_sessions_completed")
    }

    /**
     * Add a timestamp marker for synchronization
     */
    fun addTimestampMarker(description: String) {
        val timestamp = System.currentTimeMillis()
        val currentFrame = if (isCapturingFrames.get()) frameCount.get() else null
        val marker = TimestampMarker(timestamp, description, currentFrame)

        timestampMarkers.add(marker)
        logger.i(TAG, "Timestamp marker added: $description at $timestamp (frame: $currentFrame)")
    }

    /**
     * Process captured frame
     */
    private fun processFrame(imageProxy: ImageProxy) {
        frameProcessingScope.launch {
            try {
                performanceMonitor.startTimer("frame_processing")

                val bitmap = imageProxyToBitmap(imageProxy)
                val timestamp = System.currentTimeMillis()
                val frameNumber = frameCount.incrementAndGet()

                rawFrameCallback?.onFrameCaptured(bitmap, timestamp, frameNumber)

                performanceMonitor.stopTimer("frame_processing")
                performanceMonitor.incrementCounter("frames_processed")

            } catch (e: Exception) {
                logger.e(TAG, "Frame processing error", e)
                rawFrameCallback?.onFrameCaptureError("Frame processing error: ${e.message}")
                performanceMonitor.incrementCounter("frame_processing_errors")
            }
        }
    }

    /**
     * Convert ImageProxy to Bitmap
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    /**
     * Get current recording status
     */
    fun isRecording(): Boolean = isRecording.get()

    /**
     * Get recording duration in milliseconds
     */
    fun getRecordingDuration(): Long {
        return if (isRecording.get()) {
            System.currentTimeMillis() - recordingStartTime.get()
        } else {
            0L
        }
    }

    /**
     * Get current frame capture status
     */
    fun isCapturingFrames(): Boolean = isCapturingFrames.get()

    /**
     * Get current frame count
     */
    fun getFrameCount(): Int = frameCount.get()

    /**
     * Get timestamp markers
     */
    fun getTimestampMarkers(): List<TimestampMarker> = timestampMarkers.toList()

    /**
     * Clean up resources
     */
    fun cleanup() {
        stopRecording()
        stopFrameCapture()

        frameProcessingScope.cancel()
        cameraExecutor.shutdown()

        cameraProvider?.unbindAll()

        logger.i(TAG, "Camera handler cleaned up")
    }
}
