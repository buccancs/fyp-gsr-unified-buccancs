package com.buccancs.gsrcapture.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.buccancs.gsrcapture.utils.TimeManager
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Camera2-based manager that supports capturing images from different pipeline stages.
 * Supports both RAW_SENSOR (Stage 1) and YUV_420_888 (Stage 3) capture modes.
 */
class Camera2CaptureManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val cameraExecutor: ExecutorService
) {
    private val TAG = "Camera2CaptureManager"

    private var cameraManager: CameraManager? = null
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null

    // Image readers for different formats
    private var rawImageReader: ImageReader? = null
    private var yuvImageReader: ImageReader? = null

    // Capture configuration
    private var currentCaptureStage: CaptureStage = CaptureStage.STAGE_3_YUV_PROCESSED
    private val isCapturing = AtomicBoolean(false)
    private var outputDirectory: File? = null
    private var sessionId: String? = null
    private var frameCount: Int = 0

    // Camera characteristics
    private var cameraId: String? = null
    private var cameraCharacteristics: CameraCharacteristics? = null

    /**
     * Sets the capture stage (RAW_SENSOR or YUV_420_888).
     */
    fun setCaptureStage(stage: CaptureStage) {
        if (isCapturing.get()) {
            Log.w(TAG, "Cannot change capture stage while capturing")
            return
        }
        currentCaptureStage = stage
        Log.d(TAG, "Capture stage set to: ${stage.displayName}")
    }

    /**
     * Gets the current capture stage.
     */
    fun getCurrentCaptureStage(): CaptureStage = currentCaptureStage

    /**
     * Initializes the camera manager and finds the best camera.
     */
    fun initialize(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted")
            return false
        }

        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cameraId = findBestCamera()

            if (cameraId == null) {
                Log.e(TAG, "No suitable camera found")
                return false
            }

            cameraCharacteristics = cameraManager?.getCameraCharacteristics(cameraId!!)
            startBackgroundThread()

            Log.d(TAG, "Camera2CaptureManager initialized with camera: $cameraId")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize camera", e)
            return false
        }
    }

    /**
     * Finds the best camera for capture (preferably back camera with RAW support).
     */
    private fun findBestCamera(): String? {
        try {
            val cameraManager = this.cameraManager ?: return null

            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)

                // Prefer back camera
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {

                    // Check if camera supports RAW capture
                    val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                    val supportsRaw = capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW) == true

                    Log.d(TAG, "Camera $cameraId - Back facing: true, RAW support: $supportsRaw")
                    return cameraId
                }
            }

            // Fallback to first available camera
            return cameraManager.cameraIdList.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Error finding camera", e)
            return null
        }
    }

    /**
     * Opens the camera and sets up capture session.
     */
    fun openCamera(): Boolean {
        try {
            val cameraManager = this.cameraManager ?: return false
            val cameraId = this.cameraId ?: return false

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Camera permission not granted")
                return false
            }

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession()
                    Log.d(TAG, "Camera opened successfully")
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    Log.d(TAG, "Camera disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    Log.e(TAG, "Camera error: $error")
                }
            }, backgroundHandler)

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open camera", e)
            return false
        }
    }

    /**
     * Creates the capture session with appropriate image readers.
     */
    private fun createCaptureSession() {
        try {
            val camera = cameraDevice ?: return
            val characteristics = cameraCharacteristics ?: return

            // Get available sizes for the current capture stage
            val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val surfaces = mutableListOf<Surface>()

            when (currentCaptureStage) {
                CaptureStage.STAGE_1_RAW_SENSOR -> {
                    // Setup RAW capture
                    val rawSizes = streamConfigMap?.getOutputSizes(ImageFormat.RAW_SENSOR)
                    if (rawSizes?.isNotEmpty() == true) {
                        val rawSize = rawSizes[0] // Use largest available size
                        rawImageReader = ImageReader.newInstance(
                            rawSize.width, rawSize.height, 
                            ImageFormat.RAW_SENSOR, 2
                        )
                        rawImageReader?.setOnImageAvailableListener(rawImageAvailableListener, backgroundHandler)
                        surfaces.add(rawImageReader!!.surface)
                        Log.d(TAG, "RAW capture setup: ${rawSize.width}x${rawSize.height}")
                    } else {
                        Log.e(TAG, "RAW_SENSOR format not supported")
                        return
                    }
                }

                CaptureStage.STAGE_3_YUV_PROCESSED -> {
                    // Setup YUV capture
                    val yuvSizes = streamConfigMap?.getOutputSizes(ImageFormat.YUV_420_888)
                    if (yuvSizes?.isNotEmpty() == true) {
                        val yuvSize = yuvSizes[0] // Use largest available size
                        yuvImageReader = ImageReader.newInstance(
                            yuvSize.width, yuvSize.height,
                            ImageFormat.YUV_420_888, 2
                        )
                        yuvImageReader?.setOnImageAvailableListener(yuvImageAvailableListener, backgroundHandler)
                        surfaces.add(yuvImageReader!!.surface)
                        Log.d(TAG, "YUV capture setup: ${yuvSize.width}x${yuvSize.height}")
                    } else {
                        Log.e(TAG, "YUV_420_888 format not supported")
                        return
                    }
                }
            }

            // Create capture session
            camera.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    Log.d(TAG, "Capture session configured for ${currentCaptureStage.displayName}")
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e(TAG, "Failed to configure capture session")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capture session", e)
        }
    }

    /**
     * Starts continuous image capture.
     */
    fun startCapture(outputDir: File, sessionId: String): Boolean {
        if (isCapturing.get()) {
            Log.d(TAG, "Already capturing")
            return true
        }

        // Check if camera is properly initialized
        if (cameraDevice == null) {
            Log.e(TAG, "Camera device not initialized. Attempting to open camera...")
            if (!openCamera()) {
                Log.e(TAG, "Failed to open camera device")
                return false
            }
            // Wait a bit for camera to initialize
            Thread.sleep(1000)
        }

        // Check if capture session is ready, if not try to create it
        if (captureSession == null) {
            Log.w(TAG, "Capture session not ready. Attempting to create session...")
            createCaptureSession()

            // Wait for session creation (with timeout)
            var waitTime = 0
            val maxWaitTime = 5000 // 5 seconds
            while (captureSession == null && waitTime < maxWaitTime) {
                Thread.sleep(100)
                waitTime += 100
            }

            if (captureSession == null) {
                Log.e(TAG, "Capture session not ready")
                return false
            }
        }

        this.outputDirectory = outputDir
        this.sessionId = sessionId
        this.frameCount = 0

        // Create subdirectory for this capture stage
        val stageDir = File(outputDir, "${currentCaptureStage.name.lowercase()}_$sessionId")
        if (!stageDir.exists()) {
            stageDir.mkdirs()
        }

        isCapturing.set(true)
        startRepeatingCapture()

        Log.d(TAG, "Started ${currentCaptureStage.displayName} capture")
        return true
    }

    /**
     * Starts repeating capture requests.
     */
    private fun startRepeatingCapture() {
        try {
            val session = captureSession ?: return
            val camera = cameraDevice ?: return

            val surface = when (currentCaptureStage) {
                CaptureStage.STAGE_1_RAW_SENSOR -> rawImageReader?.surface
                CaptureStage.STAGE_3_YUV_PROCESSED -> yuvImageReader?.surface
            }

            if (surface == null) {
                Log.e(TAG, "No surface available for capture")
                return
            }

            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureRequestBuilder.addTarget(surface)

            // Set capture parameters based on stage
            when (currentCaptureStage) {
                CaptureStage.STAGE_1_RAW_SENSOR -> {
                    // For RAW capture, minimize processing
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
                    captureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF)
                    captureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF)
                }
                CaptureStage.STAGE_3_YUV_PROCESSED -> {
                    // For YUV capture, use full ISP processing
                    captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                }
            }

            // Start repeating capture
            session.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start repeating capture", e)
        }
    }

    /**
     * Stops image capture.
     */
    fun stopCapture() {
        if (!isCapturing.getAndSet(false)) {
            return
        }

        try {
            captureSession?.stopRepeating()
            Log.d(TAG, "Stopped ${currentCaptureStage.displayName} capture. Total frames: $frameCount")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping capture", e)
        }
    }

    /**
     * Image available listener for RAW images.
     */
    private val rawImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        if (!isCapturing.get()) return@OnImageAvailableListener

        val image = reader.acquireLatestImage()
        image?.let {
            saveRawImage(it)
            it.close()
        }
    }

    /**
     * Image available listener for YUV images.
     */
    private val yuvImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        if (!isCapturing.get()) return@OnImageAvailableListener

        val image = reader.acquireLatestImage()
        image?.let {
            saveYuvImage(it)
            it.close()
        }
    }

    /**
     * Saves a RAW image to file.
     */
    private fun saveRawImage(image: Image) {
        try {
            val outputDir = this.outputDirectory ?: return
            val sessionId = this.sessionId ?: return
            val timestamp = TimeManager.getCurrentTimestampNanos()

            val stageDir = File(outputDir, "${currentCaptureStage.name.lowercase()}_$sessionId")
            val imageFile = File(stageDir, "raw_frame_${timestamp}.dng")

            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            FileOutputStream(imageFile).use { output ->
                output.write(bytes)
            }

            frameCount++
            Log.v(TAG, "RAW image saved: ${imageFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Error saving RAW image", e)
        }
    }

    /**
     * Saves a YUV image to file.
     */
    private fun saveYuvImage(image: Image) {
        try {
            val outputDir = this.outputDirectory ?: return
            val sessionId = this.sessionId ?: return
            val timestamp = TimeManager.getCurrentTimestampNanos()

            val stageDir = File(outputDir, "${currentCaptureStage.name.lowercase()}_$sessionId")
            val imageFile = File(stageDir, "yuv_frame_${timestamp}.yuv")

            // YUV_420_888 has 3 planes: Y, U, V
            FileOutputStream(imageFile).use { output ->
                for (plane in image.planes) {
                    val buffer = plane.buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    output.write(bytes)
                }
            }

            frameCount++
            Log.v(TAG, "YUV image saved: ${imageFile.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Error saving YUV image", e)
        }
    }

    /**
     * Starts the background thread for camera operations.
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("Camera2Background").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    /**
     * Stops the background thread.
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    /**
     * Closes the camera and releases resources.
     */
    fun shutdown() {
        stopCapture()

        captureSession?.close()
        captureSession = null

        cameraDevice?.close()
        cameraDevice = null

        rawImageReader?.close()
        rawImageReader = null

        yuvImageReader?.close()
        yuvImageReader = null

        stopBackgroundThread()

        Log.d(TAG, "Camera2CaptureManager shutdown complete")
    }
}
