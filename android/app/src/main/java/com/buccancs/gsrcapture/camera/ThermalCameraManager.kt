package com.buccancs.gsrcapture.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import android.view.Surface
import android.view.TextureView
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
    private val cameraExecutor: ExecutorService
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
    private var textureView: TextureView? = null
    private var frameCallback: ((Bitmap) -> Unit)? = null

    // Recording state
    private var outputDirectory: File? = null
    private var sessionId: String? = null
    private var frameCount: Int = 0

    /**
     * Initializes the thermal camera manager.
     */
    fun initialize() {
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
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
    private fun findTopdonDriver(availableDrivers: List<UsbSerialDriver>): UsbSerialDriver? {
        // In a real implementation, you would check for the specific vendor ID and product ID
        // of the Topdon TC001 thermal camera
        // For now, we'll just return the first available driver for demonstration purposes
        return if (availableDrivers.isNotEmpty()) availableDrivers[0] else null
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
    private fun processThermalData(buffer: ByteArray, len: Int): Bitmap {
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
    private fun updatePreview(thermalFrame: Bitmap) {
        // Update the texture view on the main thread
        textureView?.post {
            val canvas = textureView?.lockCanvas()
            canvas?.drawBitmap(thermalFrame, 0f, 0f, null)
            textureView?.unlockCanvasAndPost(canvas)

            // Call the frame callback if set
            frameCallback?.invoke(thermalFrame)
        }
    }

    /**
     * Sets the texture view for preview.
     * @param view TextureView to display the thermal preview
     */
    fun setPreviewView(view: TextureView) {
        textureView = view

        // Set up the texture view
        view.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                // Surface is ready for drawing
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                // Surface size changed
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return true
            }

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
     * Starts recording thermal frames.
     * @param outputDir Directory where frames will be saved
     * @param sessionId Unique identifier for the recording session
     * @return True if recording started successfully, false otherwise
     */
    fun startRecording(outputDir: File, sessionId: String): Boolean {
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
        val thermalDir = File(outputDir, "thermal_${sessionId}")
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
    private fun saveFrame(frame: Bitmap) {
        val dir = outputDirectory ?: return
        val sid = sessionId ?: return

        try {
            val timestamp = TimeManager.getCurrentTimestampNanos()
            val frameFile = File(dir, "thermal_${sid}/frame_${timestamp}.jpg")

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
    }
}
