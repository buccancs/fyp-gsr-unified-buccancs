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
import androidx.camera.video.Recording
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.buccancs.gsrcapture.utils.TimeManager
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages RGB camera operations including preview and recording.
 */
class RgbCameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val cameraExecutor: ExecutorService
) {
    private val TAG = "RgbCameraManager"

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var imageCapture: ImageCapture? = null
    private var recording: Recording? = null

    // Raw image capture state
    private val isCapturingRawImages = AtomicBoolean(false)
    private var rawImageOutputDirectory: File? = null
    private var rawImageSessionId: String? = null
    private var rawImageFrameCount: Int = 0

    /**
     * Starts the camera and sets up the preview.
     */
    fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Video Capture
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.FHD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)

            // Image Capture for raw images
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture, imageCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Starts video recording.
     * @param outputDirectory Directory where the video will be saved
     * @param sessionId Unique identifier for the recording session
     * @return True if recording started successfully, false otherwise
     */
    fun startRecording(outputDirectory: File, sessionId: String): Boolean {
        val videoCapture = videoCapture ?: return false

        // Create output file
        val timestamp = TimeManager.getCurrentTimestamp()
        val videoFile = File(outputDirectory, "RGB_${sessionId}_${timestamp}.mp4")

        // Create output options object
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        // Configure Recorder
        recording = videoCapture.output
            .prepareRecording(context, outputOptions)
            .apply {
                // Enable audio recording
                withAudioEnabled()
            }
            .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                when(recordEvent) {
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
    fun startRawImageCapture(outputDirectory: File, sessionId: String): Boolean {
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
        val rawImageDir = File(outputDirectory, "raw_rgb_${sessionId}")
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
            val imageFile = File(outputDir, "raw_rgb_${sessionId}/frame_${timestamp}.jpg")

            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()

            imageCapture.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        rawImageFrameCount++
                        Log.v(TAG, "Raw image saved: ${imageFile.absolutePath}")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Error capturing raw image", exception)
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in captureRawFrame", e)
        }
    }

    /**
     * Releases all camera resources.
     */
    fun shutdown() {
        stopRawImageCapture()
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
    }
}
