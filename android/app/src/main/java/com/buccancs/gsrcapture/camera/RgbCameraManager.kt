package com.buccancs.gsrcapture.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
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
    private var recording: Recording? = null
    
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
                .setQualitySelector(VideoCapture.QualitySelector.from(VideoCapture.QualitySelector.QUALITY_FHD))
                .build()
            videoCapture = VideoCapture.withOutput(recorder)
            
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()
                
                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture
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
     * Releases all camera resources.
     */
    fun shutdown() {
        recording?.stop()
        recording = null
        cameraProvider?.unbindAll()
    }
}