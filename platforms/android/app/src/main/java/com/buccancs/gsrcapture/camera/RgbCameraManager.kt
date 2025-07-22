package com.buccancs.gsrcapture.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.buccancs.gsrcapture.utils.TimeManager
import com.buccancs.gsrcapture.network.CommandProtocolClient
import java.io.File
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages RGB camera operations including preview and recording.
 */
class RgbCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val cameraExecutor: ExecutorService,
) {
    private val TAG = "RgbCameraManager"

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var recording: Recording? = null

    // Camera selection state
    private var currentCameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isUsingFrontCamera: Boolean = false

    // Raw image capture state
    private val isCapturingRawImages = AtomicBoolean(false)
    private var rawImageOutputDirectory: File? = null
    private var rawImageSessionId: String? = null
    private var rawImageFrameCount: Int = 0

    // Network streaming state
    private var networkClient: CommandProtocolClient? = null
    private val isStreaming = AtomicBoolean(false)
    private var streamingFrameCount: Long = 0

    /**
     * Starts the camera and sets up the preview.
     */
    fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            preview =
                Preview
                    .Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

            // Video Capture
            val recorder =
                Recorder
                    .Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Image Capture for raw images
            imageCapture =
                ImageCapture
                    .Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    currentCameraSelector,
                    preview,
                    videoCapture,
                    imageCapture,
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, cameraExecutor)
    }

    /**
     * Starts video recording.
     * @param outputDirectory Directory where the video will be saved
     * @param sessionId Unique identifier for the recording session
     * @return True if recording started successfully, false otherwise
     */
    fun startRecording(
        outputDirectory: File,
        sessionId: String,
    ): Boolean {
        val videoCapture = videoCapture ?: return false

        // Create output file
        val timestamp = TimeManager.getCurrentTimestamp()
        val videoFile = File(outputDirectory, "RGB_${sessionId}_$timestamp.mp4")

        // Create output options object
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        // Configure Recorder
        recording =
            videoCapture.output
                .prepareRecording(context, outputOptions)
                .apply {
                    // Enable audio recording
                    withAudioEnabled()
                }.start(cameraExecutor) { recordEvent ->
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.d(TAG, "Recording started")
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (recordEvent.hasError()) {
                                Log.e(TAG, "Video recording error: ${recordEvent.error}")
                            } else {
                                Log.d(TAG, "Recording saved to ${videoFile.absolutePath}")
                            }
                        }
                    }
                }

        return true
    }

    /**
     * Stops the current recording.
     */
    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    /**
     * Starts capturing raw images frame by frame with timestamps.
     * @param outputDirectory Directory where raw images will be saved
     * @param sessionId Unique identifier for the recording session
     * @return True if raw image capture started successfully, false otherwise
     */
    fun startRawImageCapture(
        outputDirectory: File,
        sessionId: String,
    ): Boolean {
        if (imageCapture == null) {
            Log.e(TAG, "Cannot start raw image capture: ImageCapture not initialized")
            return false
        }

        if (isCapturingRawImages.get()) {
            Log.d(TAG, "Already capturing raw images")
            return true
        }

        rawImageOutputDirectory = outputDirectory
        rawImageSessionId = sessionId
        rawImageFrameCount = 0

        // Create a subdirectory for raw images
        val rawImageDir = File(outputDirectory, "raw_rgb_$sessionId")
        if (!rawImageDir.exists()) {
            rawImageDir.mkdirs()
        }

        isCapturingRawImages.set(true)

        // Start continuous frame capture
        startContinuousCapture()

        Log.d(TAG, "Started capturing raw RGB images to ${rawImageDir.absolutePath}")
        return true
    }

    /**
     * Stops capturing raw images.
     */
    fun stopRawImageCapture() {
        if (isCapturingRawImages.getAndSet(false)) {
            Log.d(TAG, "Stopped capturing raw RGB images. Total frames: $rawImageFrameCount")
        }
    }

    /**
     * Starts continuous frame capture in a background thread.
     */
    private fun startContinuousCapture() {
        cameraExecutor.execute {
            while (isCapturingRawImages.get()) {
                captureRawFrame()
                try {
                    // Capture at approximately 30 FPS (33ms interval)
                    Thread.sleep(33)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    /**
     * Captures a single raw frame and saves it with timestamp.
     */
    private fun captureRawFrame() {
        val imageCapture = imageCapture ?: return
        val outputDir = rawImageOutputDirectory ?: return
        val sessionId = rawImageSessionId ?: return

        try {
            val timestamp = TimeManager.getCurrentTimestampNanos()
            val imageFile = File(outputDir, "raw_rgb_$sessionId/frame_$timestamp.jpg")

            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

            imageCapture.takePicture(
                outputFileOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        rawImageFrameCount++
                        Log.v(TAG, "Raw image saved: ${imageFile.absolutePath}")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Error capturing raw image", exception)
                    }
                },
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in captureRawFrame", e)
        }
    }

    /**
     * Switches between front and rear camera.
     * @param previewView The preview view to update
     * @return True if camera was switched successfully, false otherwise
     */
    fun switchCamera(previewView: PreviewView): Boolean {
        return try {
            // Toggle camera selector
            currentCameraSelector = if (isUsingFrontCamera) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                CameraSelector.DEFAULT_FRONT_CAMERA
            }
            isUsingFrontCamera = !isUsingFrontCamera

            // Restart camera with new selector
            startCamera(previewView)

            Log.d(TAG, "Switched to ${if (isUsingFrontCamera) "front" else "rear"} camera")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to switch camera", e)
            false
        }
    }

    /**
     * Sets the camera to use front camera.
     * @param previewView The preview view to update
     * @return True if camera was set successfully, false otherwise
     */
    fun setFrontCamera(previewView: PreviewView): Boolean {
        return if (!isUsingFrontCamera) {
            switchCamera(previewView)
        } else {
            true // Already using front camera
        }
    }

    /**
     * Sets the camera to use rear camera.
     * @param previewView The preview view to update
     * @return True if camera was set successfully, false otherwise
     */
    fun setRearCamera(previewView: PreviewView): Boolean {
        return if (isUsingFrontCamera) {
            switchCamera(previewView)
        } else {
            true // Already using rear camera
        }
    }

    /**
     * Gets the current camera type.
     * @return True if using front camera, false if using rear camera
     */
    fun isUsingFrontCamera(): Boolean = isUsingFrontCamera

    /**
     * Sets the network client for streaming video frames.
     * @param client CommandProtocolClient instance for sending frames
     */
    fun setNetworkClient(client: CommandProtocolClient?) {
        networkClient = client
    }

    /**
     * Starts live video streaming to the network client.
     * @return True if streaming started successfully, false otherwise
     */
    fun startStreaming(): Boolean {
        if (networkClient == null) {
            Log.w(TAG, "Cannot start streaming: no network client set")
            return false
        }

        if (isStreaming.get()) {
            Log.d(TAG, "Already streaming")
            return true
        }

        isStreaming.set(true)
        streamingFrameCount = 0
        startStreamingCapture()
        Log.d(TAG, "RGB video streaming started")
        return true
    }

    /**
     * Stops live video streaming.
     */
    fun stopStreaming() {
        if (isStreaming.getAndSet(false)) {
            Log.d(TAG, "RGB video streaming stopped")
        }
    }

    /**
     * Starts continuous frame capture for streaming in a background thread.
     */
    private fun startStreamingCapture() {
        cameraExecutor.execute {
            while (isStreaming.get()) {
                captureStreamingFrame()
                try {
                    // Stream at approximately 15 FPS (67ms interval) to reduce bandwidth
                    Thread.sleep(67)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    /**
     * Captures a single frame for streaming and sends it to the network client.
     */
    private fun captureStreamingFrame() {
        val imageCapture = imageCapture ?: return
        val client = networkClient ?: return

        try {
            val timestamp = TimeManager.getCurrentTimestampNanos()

            // Create a temporary file for the frame
            val tempFile = File.createTempFile("rgb_stream_frame", ".jpg", context.cacheDir)
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()

            imageCapture.takePicture(
                outputFileOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            // Read the captured frame and send it
                            val frameData = tempFile.readBytes()
                            client.sendVideoFrame(frameData, "rgb", timestamp)
                            streamingFrameCount++

                            // Clean up temp file
                            tempFile.delete()

                            Log.v(TAG, "Streamed RGB frame #$streamingFrameCount (${frameData.size} bytes)")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sending streaming frame", e)
                            tempFile.delete()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Error capturing streaming frame", exception)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in captureStreamingFrame", e)
        }
    }

    /**
     * Releases all camera resources.
     */
    fun shutdown() {
        stopRawImageCapture()
        stopStreaming()
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
    }
}
