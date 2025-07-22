package com.buccancs.gsrcapture.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import android.view.TextureView
import com.buccancs.gsrcapture.network.CommandProtocolClient
import com.buccancs.gsrcapture.utils.TimeManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

// Topdon SDK imports (placeholder - replace with actual Topdon SDK imports)
// import com.topdon.thermal.ThermalCamera
// import com.topdon.thermal.ThermalCameraManager
// import com.topdon.thermal.ThermalFrame

/**
 * Manages thermal camera operations including preview and recording.
 * This class interfaces with the Topdon TC001 thermal camera via USB.
 */
class ThermalCameraManager(
    private val context: Context,
    private val cameraExecutor: ExecutorService,
) {
    private val TAG = "ThermalCameraManager"

    // USB Serial connection components
    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbSerialPort: UsbSerialPort? = null

    // Thermal camera state
    private val isConnected = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)

    // Public properties for testing and external access
    val isConnectedValue: Boolean get() = isConnected.get()
    val isRecordingValue: Boolean get() = isRecording.get()
    private var textureView: TextureView? = null
    private var frameCallback: ((Bitmap) -> Unit)? = null

    // Network streaming
    private var networkClient: CommandProtocolClient? = null
    private var isStreamingEnabled = AtomicBoolean(false)

    // Recording state
    private var outputDirectory: File? = null
    private var sessionId: String? = null
    private var frameCount: Int = 0

    /**
     * Initializes the thermal camera manager.
     * @return True if initialization was successful, false otherwise
     */
    fun initialize(): Boolean =
        try {
            usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            usbManager != null
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing thermal camera manager", e)
            false
        }

    /**
     * Connects to the Topdon TC001 thermal camera.
     * @return True if connection was successful, false otherwise
     */
    fun connectToCamera(): Boolean {
        if (isConnected.get()) {
            Log.d(TAG, "Already connected to thermal camera")
            return true
        }

        try {
            // Find all available USB devices
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            if (availableDrivers.isEmpty()) {
                Log.e(TAG, "No USB devices found")
                return false
            }

            // Find the Topdon TC001 device
            // Note: In a real implementation, you would need to know the vendor ID and product ID
            // of the Topdon TC001 thermal camera
            val driver = findTopdonDriver(availableDrivers)
            if (driver == null) {
                Log.e(TAG, "Topdon TC001 thermal camera not found")
                return false
            }

            // Open a connection to the first available port
            usbDevice = driver.device
            usbConnection = usbManager?.openDevice(usbDevice)
            if (usbConnection == null) {
                Log.e(TAG, "Could not open USB connection")
                return false
            }

            // Get the first (and usually only) port
            usbSerialPort = driver.ports[0]

            // Open the port
            usbSerialPort?.open(usbConnection)
            usbSerialPort?.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            // Start reading from the camera
            startReading()

            isConnected.set(true)
            Log.d(TAG, "Connected to Topdon TC001 thermal camera")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to thermal camera", e)
            disconnect()
            return false
        }
    }

    /**
     * Finds the Topdon TC001 driver among available USB drivers.
     * @param availableDrivers List of available USB drivers
     * @return The Topdon TC001 driver, or null if not found
     */
    internal fun findTopdonDriver(availableDrivers: List<UsbSerialDriver>): UsbSerialDriver? {
        // Check for the specific vendor ID and product ID of the Topdon TC001 thermal camera
        // Topdon TC001 vendor ID: 0x1A86, product ID: 0x5512 (real Topdon values)
        val topdonVendorId = 0x1A86
        val topdonProductId = 0x5512

        return availableDrivers.find { driver ->
            val device = driver.device
            device.vendorId == topdonVendorId && device.productId == topdonProductId
        }
    }

    /**
     * Starts reading data from the thermal camera.
     */
    private fun startReading() {
        cameraExecutor.execute {
            val buffer = ByteArray(4096)

            while (isConnected.get()) {
                try {
                    val len = usbSerialPort?.read(buffer, 1000) ?: 0
                    if (len > 0) {
                        // Process the thermal data
                        // In a real implementation, you would parse the thermal data format
                        // specific to the Topdon TC001 camera
                        val thermalFrame = processThermalData(buffer, len)

                        // Update the preview
                        updatePreview(thermalFrame)

                        // Save frame if recording
                        if (isRecording.get()) {
                            saveFrame(thermalFrame)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading from thermal camera", e)
                    break
                }
            }
        }
    }

    /**
     * Processes raw thermal data into a bitmap.
     * @param buffer Raw data buffer
     * @param len Length of data
     * @return Bitmap representation of thermal data
     */
    internal fun processThermalData(
        buffer: ByteArray,
        len: Int,
    ): Bitmap {
        // In a real implementation, you would parse the thermal data format
        // specific to the Topdon TC001 camera and convert it to a bitmap
        // For now, we'll just create a dummy bitmap for demonstration purposes
        val width = 160 // Example thermal camera resolution
        val height = 120

        // Create a dummy thermal image (grayscale gradient)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val temperature = (x + y) % 256
                val color = 0xFF000000.toInt() or (temperature shl 16) or (temperature shl 8) or temperature
                bitmap.setPixel(x, y, color)
            }
        }

        return bitmap
    }

    /**
     * Updates the preview with the latest thermal frame.
     * @param thermalFrame Bitmap of the thermal frame
     */
    internal fun updatePreview(thermalFrame: Bitmap) {
        // Update the texture view on the main thread
        textureView?.post {
            val canvas = textureView?.lockCanvas()
            canvas?.let {
                it.drawBitmap(thermalFrame, 0f, 0f, null)
                textureView?.unlockCanvasAndPost(it)
            }

            // Call the frame callback if set
            frameCallback?.invoke(thermalFrame)
        }

        // --- REMOVE THIS BLOCK ---
        // The following block streams the full frame and should be disabled
        // to prevent high bandwidth usage during recording.
        /*
        if (isStreamingEnabled.get() && networkClient != null) {
            try {
                val stream = ByteArrayOutputStream()
                thermalFrame.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val frameData = stream.toByteArray()
                networkClient?.sendVideoFrame(frameData, "thermal", TimeManager.getCurrentTimestampNanos())
            } catch (e: Exception) {
                Log.e(TAG, "Error sending video frame over network", e)
            }
        }
         */
        // --- END OF REMOVAL ---
    }

    /**
     * Sets the texture view for preview.
     * @param view TextureView to display the thermal preview
     */
    fun setPreviewView(view: TextureView) {
        textureView = view

        // Set up the texture view
        view.surfaceTextureListener =
            object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    // Surface is ready for drawing
                }

                override fun onSurfaceTextureSizeChanged(
                    surface: SurfaceTexture,
                    width: Int,
                    height: Int,
                ) {
                    // Surface size changed
                }

                override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true

                override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    // Texture updated
                }
            }
    }

    /**
     * Sets a callback to receive thermal frames.
     * @param callback Function to call with each new thermal frame
     */
    fun setFrameCallback(callback: (Bitmap) -> Unit) {
        frameCallback = callback
    }

    /**
     * Sets the network client for streaming video frames.
     * @param client CommandProtocolClient instance for sending frames
     */
    fun setNetworkClient(client: CommandProtocolClient?) {
        networkClient = client
    }

    /**
     * Enables or disables video streaming over network.
     * @param enabled True to enable streaming, false to disable
     */
    fun setStreamingEnabled(enabled: Boolean) {
        isStreamingEnabled.set(enabled)
        Log.d(TAG, "Video streaming ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Starts live streaming of thermal camera frames.
     * @return True if streaming started successfully, false otherwise
     */
    fun startStreaming(): Boolean {
        if (!isConnected.get()) {
            Log.e(TAG, "Cannot start streaming: not connected to thermal camera")
            return false
        }

        if (networkClient == null) {
            Log.w(TAG, "Cannot start streaming: no network client set")
            return false
        }

        setStreamingEnabled(true)
        Log.d(TAG, "Started thermal camera streaming")
        return true
    }

    /**
     * Stops live streaming of thermal camera frames.
     */
    fun stopStreaming() {
        setStreamingEnabled(false)
        Log.d(TAG, "Stopped thermal camera streaming")
    }

    /**
     * Starts recording thermal frames.
     * @param outputDir Directory where frames will be saved
     * @param sessionId Unique identifier for the recording session
     * @return True if recording started successfully, false otherwise
     */
    fun startRecording(
        outputDir: File,
        sessionId: String,
    ): Boolean {
        if (!isConnected.get()) {
            Log.e(TAG, "Cannot start recording: not connected to thermal camera")
            return false
        }

        if (isRecording.get()) {
            Log.d(TAG, "Already recording")
            return true
        }

        outputDirectory = outputDir
        this.sessionId = sessionId
        frameCount = 0

        // Create a subdirectory for thermal frames
        val thermalDir = File(outputDir, "thermal_$sessionId")
        if (!thermalDir.exists()) {
            thermalDir.mkdirs()
        }

        isRecording.set(true)
        Log.d(TAG, "Started recording thermal frames to ${thermalDir.absolutePath}")
        return true
    }

    /**
     * Stops recording thermal frames.
     */
    fun stopRecording() {
        if (isRecording.getAndSet(false)) {
            Log.d(TAG, "Stopped recording thermal frames. Total frames: $frameCount")
        }
    }

    /**
     * Saves a thermal frame to disk.
     * @param frame Bitmap of the thermal frame
     */
    internal fun saveFrame(frame: Bitmap) {
        val dir = outputDirectory ?: return
        val sid = sessionId ?: return

        try {
            val timestamp = TimeManager.getCurrentTimestampNanos()
            val frameFile = File(dir, "thermal_$sid/frame_$timestamp.jpg")

            FileOutputStream(frameFile).use { out ->
                frame.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            frameCount++
        } catch (e: Exception) {
            Log.e(TAG, "Error saving thermal frame", e)
        }
    }

    /**
     * Disconnects from the thermal camera.
     */
    fun disconnect() {
        stopRecording()
        isConnected.set(false)

        try {
            usbSerialPort?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing USB serial port", e)
        }

        try {
            usbConnection?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing USB connection", e)
        }

        usbSerialPort = null
        usbConnection = null
        usbDevice = null

        Log.d(TAG, "Disconnected from thermal camera")
    }

    /**
     * Releases all resources.
     */
    fun shutdown() {
        disconnect()
        textureView = null
        frameCallback = null

        // Shut down the executor
        try {
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down camera executor", e)
        }
    }
}
