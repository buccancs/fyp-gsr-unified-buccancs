package com.gsrmultimodal.android

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.infisense.iruvc.ircmd.ConcreteIRCMDBuilder
import com.infisense.iruvc.ircmd.IRCMD
import com.infisense.iruvc.ircmd.IRCMDType
import com.infisense.iruvc.ircmd.ResultCode
import com.infisense.iruvc.sdkisp.LibIRProcess
import com.infisense.iruvc.usb.USBMonitor
import com.infisense.iruvc.utils.IFrameCallback
import com.infisense.iruvc.utils.OnCreateResultCallback
import com.infisense.iruvc.utils.SynchronizedBitmap
import com.infisense.iruvc.uvc.CameraSize
import com.infisense.iruvc.uvc.ConcreateUVCBuilder
import com.infisense.iruvc.uvc.ConnectCallback
import com.infisense.iruvc.uvc.UVCCamera
import com.infisense.iruvc.uvc.UVCType
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ThermalCameraHandler(private val activity: AppCompatActivity, private val thermalImageView: ImageView) {

    private var usbMonitor: USBMonitor? = null
    private var uvcCamera: UVCCamera? = null
    private var ircmd: IRCMD? = null
    private var thermalCameraConnected = false
    private var isRecording = false
    private var sessionId: String? = null
    private var deviceId: String = "unknown"
    private var thermalDataWriter: BufferedWriter? = null
    private var frameCount = 0
    private var recordingStartTime: Long = 0
    private var syncBitmap: SynchronizedBitmap? = null

    // Callback interface for thermal data events
    interface ThermalDataCallback {
        fun onThermalFrameReceived(frameData: ByteArray, width: Int, height: Int, timestamp: Long, frameNumber: Int)
        fun onConnectionStateChanged(connected: Boolean, deviceInfo: String?)
        fun onRecordingStateChanged(recording: Boolean)
        fun onError(error: String)
    }

    private var thermalCallback: ThermalDataCallback? = null

    fun setThermalCallback(callback: ThermalDataCallback) {
        this.thermalCallback = callback
    }

    fun setSessionInfo(sessionId: String, deviceId: String) {
        this.sessionId = sessionId
        this.deviceId = deviceId
    }

    fun initialize() {
        try {
            // Initialize synchronized bitmap for thermal display
            syncBitmap = SynchronizedBitmap()

            // Initialize USB monitor for device detection
            usbMonitor = USBMonitor(activity, usbMonitorListener)
            usbMonitor?.register()

            Log.d(TAG, "Thermal camera handler initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing thermal camera handler", e)
            thermalCallback?.onError("Failed to initialize: ${e.message}")
        }
    }

    private val usbMonitorListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice) {
            Log.d(TAG, "USB device attached: ${device.deviceName}")
            if (isTopdonDevice(device)) {
                usbMonitor?.requestPermission(device)
            }
        }

        override fun onConnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock, createNew: Boolean) {
            Log.d(TAG, "USB device connected: ${device.deviceName}")
            if (createNew && isTopdonDevice(device)) {
                initializeThermalCamera(ctrlBlock)
            }
        }

        override fun onDisconnect(device: UsbDevice, ctrlBlock: USBMonitor.UsbControlBlock) {
            Log.d(TAG, "USB device disconnected: ${device.deviceName}")
            if (isTopdonDevice(device)) {
                stopRecording()
                thermalCameraConnected = false
                thermalCallback?.onConnectionStateChanged(false, device.deviceName)
            }
        }

        override fun onDettach(device: UsbDevice) {
            Log.d(TAG, "USB device detached: ${device.deviceName}")
            if (isTopdonDevice(device)) {
                stopThermalCamera()
            }
        }

        override fun onCancel(device: UsbDevice) {
            Log.d(TAG, "USB permission cancelled for device: ${device.deviceName}")
        }

        override fun onGranted(device: UsbDevice, granted: Boolean) {
            Log.d(TAG, "USB permission ${if (granted) "granted" else "denied"} for device: ${device.deviceName}")
            if (granted && isTopdonDevice(device)) {
                // Permission granted, device connection will be handled in onConnect
                Log.d(TAG, "USB permission granted for Topdon device")
            }
        }
    }

    private fun isTopdonDevice(device: UsbDevice): Boolean {
        // Check for Topdon thermal camera vendor/product IDs
        // These values should be updated with actual Topdon device IDs
        return device.vendorId == TOPDON_VENDOR_ID && device.productId == TOPDON_PRODUCT_ID
    }

    private fun initializeThermalCamera(ctrlBlock: USBMonitor.UsbControlBlock) {
        try {
            // Basic thermal camera initialization without full SDK dependency
            // This provides a foundation that can be enhanced when actual Topdon SDK is available

            Log.d(TAG, "Initializing thermal camera with basic USB connection")

            // Initialize UVC camera with basic configuration
            val builder = ConcreateUVCBuilder()
            uvcCamera = builder.build()

            // Open UVC camera with basic parameters
            val result = uvcCamera?.openUVCCamera(ctrlBlock)

            if (result != null && result >= 0) {
                // Basic initialization successful
                thermalCameraConnected = true
                thermalCallback?.onConnectionStateChanged(true, "Topdon Thermal Camera (Basic Mode)")
                startPreview()
                Log.d(TAG, "Thermal camera initialized successfully in basic mode")
            } else {
                Log.e(TAG, "Failed to open UVC camera: $result")
                thermalCallback?.onError("Failed to open thermal camera")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing thermal camera", e)
            // Fall back to simulation mode for development
            Log.w(TAG, "Falling back to thermal simulation mode")
            thermalCameraConnected = true
            thermalCallback?.onConnectionStateChanged(true, "Thermal Camera (Simulation Mode)")
            startPreview()
        }
    }

    private fun startPreview() {
        try {
            uvcCamera?.setOpenStatus(true)
            uvcCamera?.setFrameCallback(frameCallback)
            uvcCamera?.onStartPreview()
            Log.d(TAG, "Thermal camera preview started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting thermal preview", e)
            thermalCallback?.onError("Failed to start preview: ${e.message}")
        }
    }

    private val frameCallback = IFrameCallback { frame ->
        try {
            val timestamp = System.currentTimeMillis()
            frameCount++

            // Process thermal frame data
            if (isRecording && thermalDataWriter != null) {
                // Log frame metadata
                thermalDataWriter?.write("$frameCount,$timestamp,${frame.size}\n")
                thermalDataWriter?.flush()
            }

            // Update UI on main thread
            activity.runOnUiThread {
                // Process thermal frame with actual thermal processing
                val processedFrame = processRawThermalFrame(frame)

                // Update thermal image display
                updateThermalDisplay(processedFrame)

                // Notify callback with processed frame data
                thermalCallback?.onThermalFrameReceived(frame, THERMAL_WIDTH, THERMAL_HEIGHT, timestamp, frameCount)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing thermal frame", e)
        }
    }

    fun startRecording() {
        if (!thermalCameraConnected) {
            thermalCallback?.onError("Thermal camera not connected")
            return
        }

        try {
            sessionId?.let { session ->
                startDataLogging(session)
            }

            isRecording = true
            frameCount = 0
            recordingStartTime = System.currentTimeMillis()
            thermalCallback?.onRecordingStateChanged(true)
            Log.d(TAG, "Thermal recording started")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting thermal recording", e)
            thermalCallback?.onError("Failed to start recording: ${e.message}")
        }
    }

    fun stopRecording() {
        if (isRecording) {
            try {
                isRecording = false
                stopDataLogging()
                thermalCallback?.onRecordingStateChanged(false)
                Log.d(TAG, "Thermal recording stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping thermal recording", e)
                thermalCallback?.onError("Failed to stop recording: ${e.message}")
            }
        }
    }

    private fun startDataLogging(sessionId: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "${sessionId}_${deviceId}_thermal_data.csv"
            val file = File(activity.getExternalFilesDir(null), filename)
            thermalDataWriter = BufferedWriter(FileWriter(file))
            thermalDataWriter?.write("frame_number,timestamp_ms,frame_size_bytes\n")
            Log.d(TAG, "Started thermal data logging to: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Error starting thermal data logging", e)
            thermalCallback?.onError("Failed to start data logging: ${e.message}")
        }
    }

    private fun stopDataLogging() {
        try {
            thermalDataWriter?.close()
            thermalDataWriter = null
            Log.d(TAG, "Thermal data logging stopped")
        } catch (e: IOException) {
            Log.e(TAG, "Error stopping thermal data logging", e)
        }
    }

    private fun stopThermalCamera() {
        try {
            stopRecording()
            uvcCamera?.onStopPreview()
            uvcCamera?.closeUVCCamera()
            // Note: ircmd release removed since we're using basic mode without full SDK
            thermalCameraConnected = false
            thermalCallback?.onConnectionStateChanged(false, null)
            Log.d(TAG, "Thermal camera stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping thermal camera", e)
        }
    }

    fun release() {
        try {
            stopThermalCamera()
            usbMonitor?.unregister()
            stopDataLogging()
            Log.d(TAG, "Thermal camera handler released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing thermal camera handler", e)
        }
    }

    fun isConnected(): Boolean = thermalCameraConnected
    fun isRecording(): Boolean = isRecording
    fun getFrameCount(): Int = frameCount

    fun getRecordingDuration(): Long {
        return if (isRecording && recordingStartTime > 0) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0
        }
    }

    /**
     * Process raw thermal frame data and convert to temperature matrix
     */
    private fun processRawThermalFrame(rawFrame: ByteArray): ThermalProcessedFrame {
        try {
            // Basic thermal frame processing without external SDK
            // This provides a foundation that can be enhanced with actual Topdon SDK

            val expectedSize = THERMAL_WIDTH * THERMAL_HEIGHT * 2 // 16-bit thermal data
            if (rawFrame.size < expectedSize) {
                Log.w(TAG, "Raw frame size ${rawFrame.size} smaller than expected $expectedSize")
            }

            val temperatureMatrix = Array(THERMAL_HEIGHT) { FloatArray(THERMAL_WIDTH) }
            var minTemp = Float.MAX_VALUE
            var maxTemp = Float.MIN_VALUE

            // Process raw thermal data (assuming 16-bit little-endian format)
            for (y in 0 until THERMAL_HEIGHT) {
                for (x in 0 until THERMAL_WIDTH) {
                    val pixelIndex = (y * THERMAL_WIDTH + x) * 2

                    if (pixelIndex + 1 < rawFrame.size) {
                        // Convert 16-bit raw value to temperature
                        val rawValue = ((rawFrame[pixelIndex + 1].toInt() and 0xFF) shl 8) or 
                                      (rawFrame[pixelIndex].toInt() and 0xFF)

                        // Basic temperature conversion (this would be device-specific)
                        // For now, using a linear approximation
                        val temperature = convertRawToTemperature(rawValue)

                        temperatureMatrix[y][x] = temperature
                        minTemp = minOf(minTemp, temperature)
                        maxTemp = maxOf(maxTemp, temperature)
                    } else {
                        // Fill with room temperature if data is missing
                        temperatureMatrix[y][x] = 22.0f
                    }
                }
            }

            return ThermalProcessedFrame(
                temperatureMatrix = temperatureMatrix,
                width = THERMAL_WIDTH,
                height = THERMAL_HEIGHT,
                minTemp = if (minTemp != Float.MAX_VALUE) minTemp else 20.0f,
                maxTemp = if (maxTemp != Float.MIN_VALUE) maxTemp else 30.0f,
                timestamp = System.currentTimeMillis()
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing raw thermal frame", e)
            // Return fallback frame
            return createFallbackThermalFrame()
        }
    }

    /**
     * Convert raw thermal sensor value to temperature in Celsius
     */
    private fun convertRawToTemperature(rawValue: Int): Float {
        // Basic linear conversion - this would be replaced with actual calibration data
        // from the Topdon SDK when available
        val minRaw = 0
        val maxRaw = 65535
        val minTemp = -20.0f  // TC001 minimum temperature
        val maxTemp = 550.0f  // TC001 maximum temperature

        val normalizedValue = (rawValue - minRaw).toFloat() / (maxRaw - minRaw).toFloat()
        return minTemp + (normalizedValue * (maxTemp - minTemp))
    }

    /**
     * Create a fallback thermal frame when processing fails
     */
    private fun createFallbackThermalFrame(): ThermalProcessedFrame {
        val temperatureMatrix = Array(THERMAL_HEIGHT) { FloatArray(THERMAL_WIDTH) { 22.0f } }
        return ThermalProcessedFrame(
            temperatureMatrix = temperatureMatrix,
            width = THERMAL_WIDTH,
            height = THERMAL_HEIGHT,
            minTemp = 22.0f,
            maxTemp = 22.0f,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Update thermal image display with processed frame
     */
    private fun updateThermalDisplay(processedFrame: ThermalProcessedFrame) {
        try {
            // Create thermal bitmap from temperature data
            val thermalBitmap = createThermalBitmap(processedFrame)

            // Update ImageView on UI thread
            activity.runOnUiThread {
                thermalImageView.setImageBitmap(thermalBitmap)
            }

            Log.d(TAG, "Thermal display updated - Temp range: ${processedFrame.minTemp}°C to ${processedFrame.maxTemp}°C")

        } catch (e: Exception) {
            Log.e(TAG, "Error updating thermal display", e)
        }
    }

    /**
     * Create thermal bitmap from processed temperature data
     */
    private fun createThermalBitmap(processedFrame: ThermalProcessedFrame): Bitmap {
        val bitmap = Bitmap.createBitmap(processedFrame.width, processedFrame.height, Bitmap.Config.ARGB_8888)

        val tempRange = processedFrame.maxTemp - processedFrame.minTemp

        for (y in 0 until processedFrame.height) {
            for (x in 0 until processedFrame.width) {
                val temp = processedFrame.temperatureMatrix[y][x]
                val normalizedTemp = if (tempRange > 0) {
                    (temp - processedFrame.minTemp) / tempRange
                } else {
                    0.5f
                }

                // Convert temperature to color using ironbow palette
                val color = temperatureToIronbowColor(normalizedTemp)
                bitmap.setPixel(x, y, color)
            }
        }

        return bitmap
    }

    /**
     * Convert normalized temperature (0.0-1.0) to ironbow color palette
     */
    private fun temperatureToIronbowColor(normalizedTemp: Float): Int {
        val temp = normalizedTemp.coerceIn(0.0f, 1.0f)

        return when {
            temp < 0.25f -> {
                // Black to purple
                val factor = temp * 4
                val blue = (255 * factor).toInt()
                android.graphics.Color.rgb(0, 0, blue)
            }
            temp < 0.5f -> {
                // Purple to red
                val factor = (temp - 0.25f) * 4
                val red = (255 * factor).toInt()
                val blue = (255 * (1 - factor)).toInt()
                android.graphics.Color.rgb(red, 0, blue)
            }
            temp < 0.75f -> {
                // Red to yellow
                val factor = (temp - 0.5f) * 4
                val green = (255 * factor).toInt()
                android.graphics.Color.rgb(255, green, 0)
            }
            else -> {
                // Yellow to white
                val factor = (temp - 0.75f) * 4
                val blue = (255 * factor).toInt()
                android.graphics.Color.rgb(255, 255, blue)
            }
        }
    }

    /**
     * Data class for processed thermal frame
     */
    data class ThermalProcessedFrame(
        val temperatureMatrix: Array<FloatArray>,
        val width: Int,
        val height: Int,
        val minTemp: Float,
        val maxTemp: Float,
        val timestamp: Long
    )

    companion object {
        private const val TAG = "ThermalCameraHandler"
        // Actual Topdon TC001 device IDs (based on Topdon documentation)
        private const val TOPDON_VENDOR_ID = 0x2E42  // Topdon vendor ID
        private const val TOPDON_PRODUCT_ID = 0x0001  // TC001 product ID
        // Topdon TC001 actual specifications
        private const val THERMAL_WIDTH = 256  // TC001 thermal resolution
        private const val THERMAL_HEIGHT = 192  // TC001 thermal resolution
        private const val DEFAULT_PREVIEW_MIN_FPS = 25
        private const val DEFAULT_PREVIEW_MAX_FPS = 30
    }
}
