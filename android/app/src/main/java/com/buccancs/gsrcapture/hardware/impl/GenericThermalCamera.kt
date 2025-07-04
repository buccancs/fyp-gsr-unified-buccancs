package com.buccancs.gsrcapture.hardware.impl

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import android.view.TextureView
import com.buccancs.gsrcapture.hardware.interfaces.ThermalCamera
import com.buccancs.gsrcapture.utils.TimeManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Generic thermal camera implementation for unknown or third-party thermal cameras
 * Provides basic thermal camera functionality with configurable parameters
 */
class GenericThermalCamera(
    private val context: Context,
    private val executor: ExecutorService,
    private val deviceInfo: Map<String, Any>
) : ThermalCamera {
    
    companion object {
        private const val TAG = "GenericThermalCamera"
        
        // Default specifications (can be overridden via deviceInfo)
        private const val DEFAULT_WIDTH = 80
        private const val DEFAULT_HEIGHT = 60
        private const val DEFAULT_FRAME_RATE = 8.0f
        private const val DEFAULT_MIN_TEMP = -40.0f
        private const val DEFAULT_MAX_TEMP = 300.0f
        
        // USB communication parameters
        private const val DEFAULT_BAUD_RATE = 9600
        private const val DATA_BITS = 8
        private const val STOP_BITS = UsbSerialPort.STOPBITS_1
        private const val PARITY = UsbSerialPort.PARITY_NONE
    }
    
    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private var usbSerialPort: UsbSerialPort? = null
    
    private val isConnected = AtomicBoolean(false)
    private val isStreaming = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)
    
    private var currentCallback: ThermalCamera.ThermalCameraCallback? = null
    private var frameCallback: ((ThermalCamera.ThermalFrame) -> Unit)? = null
    private var textureView: TextureView? = null
    private var recordingDir: File? = null
    private var sessionId: String? = null
    
    // Device specifications (configurable)
    private val thermalWidth = deviceInfo["width"] as? Int ?: DEFAULT_WIDTH
    private val thermalHeight = deviceInfo["height"] as? Int ?: DEFAULT_HEIGHT
    private val frameRate = deviceInfo["frameRate"] as? Float ?: DEFAULT_FRAME_RATE
    private val minTemperature = deviceInfo["minTemp"] as? Float ?: DEFAULT_MIN_TEMP
    private val maxTemperature = deviceInfo["maxTemp"] as? Float ?: DEFAULT_MAX_TEMP
    private val vendorId = deviceInfo["vendorId"] as? Int
    private val productId = deviceInfo["productId"] as? Int
    private val baudRate = deviceInfo["baudRate"] as? Int ?: DEFAULT_BAUD_RATE
    
    override fun initialize(): Boolean {
        return try {
            usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            Log.d(TAG, "Generic thermal camera initialized with specs: ${thermalWidth}x${thermalHeight}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize generic thermal camera", e)
            false
        }
    }
    
    override fun connect(deviceIdentifier: String?): Boolean {
        if (isConnected.get()) {
            Log.d(TAG, "Already connected to generic thermal camera")
            return true
        }
        
        return try {
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            val driver = findCompatibleDriver(availableDrivers, deviceIdentifier)
            
            if (driver == null) {
                Log.w(TAG, "Compatible thermal camera not found")
                return false
            }
            
            usbDevice = driver.device
            usbConnection = usbManager?.openDevice(usbDevice)
            
            if (usbConnection == null) {
                Log.e(TAG, "Failed to open USB connection")
                return false
            }
            
            usbSerialPort = driver.ports[0]
            usbSerialPort?.open(usbConnection)
            usbSerialPort?.setParameters(baudRate, DATA_BITS, STOP_BITS, PARITY)
            
            isConnected.set(true)
            currentCallback?.onConnectionStateChanged(true)
            
            Log.d(TAG, "Connected to generic thermal camera: ${usbDevice?.deviceName}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to generic thermal camera", e)
            currentCallback?.onError("Connection failed", e)
            false
        }
    }
    
    override fun disconnect() {
        try {
            stopStreaming()
            stopRecording()
            
            usbSerialPort?.close()
            usbConnection?.close()
            
            usbSerialPort = null
            usbConnection = null
            usbDevice = null
            
            isConnected.set(false)
            currentCallback?.onConnectionStateChanged(false)
            Log.d(TAG, "Disconnected from generic thermal camera")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from generic thermal camera", e)
        }
    }
    
    override fun isConnected(): Boolean = isConnected.get()
    
    override fun startStreaming(callback: ThermalCamera.ThermalCameraCallback): Boolean {
        if (!isConnected.get()) {
            callback.onError("Device not connected", null)
            return false
        }
        
        if (isStreaming.get()) {
            Log.d(TAG, "Already streaming")
            return true
        }
        
        return try {
            currentCallback = callback
            isStreaming.set(true)
            
            // Start reading thermal data in background
            executor.execute { readThermalData() }
            
            Log.d(TAG, "Started streaming from generic thermal camera")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting streaming", e)
            callback.onError("Failed to start streaming", e)
            false
        }
    }
    
    override fun stopStreaming() {
        if (!isStreaming.get()) return
        
        try {
            isStreaming.set(false)
            currentCallback = null
            Log.d(TAG, "Stopped streaming from generic thermal camera")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping streaming", e)
        }
    }
    
    override fun setPreviewView(view: TextureView) {
        textureView = view
    }
    
    override fun startRecording(outputDir: File, sessionId: String): Boolean {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot start recording - device not connected")
            return false
        }
        
        return try {
            outputDir.mkdirs()
            recordingDir = outputDir
            this.sessionId = sessionId
            isRecording.set(true)
            
            Log.d(TAG, "Started recording thermal frames to $outputDir")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            false
        }
    }
    
    override fun stopRecording() {
        if (!isRecording.get()) return
        
        try {
            isRecording.set(false)
            recordingDir = null
            sessionId = null
            Log.d(TAG, "Stopped recording thermal frames")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }
    
    override fun getCameraSpecs(): ThermalCamera.CameraSpecs {
        return ThermalCamera.CameraSpecs(
            width = thermalWidth,
            height = thermalHeight,
            temperatureRange = Pair(minTemperature, maxTemperature),
            frameRate = frameRate.toDouble(),
            additionalSpecs = mapOf(
                "type" to "Generic",
                "configurable" to "true",
                "dataFormat" to "Raw bytes",
                "connectionType" to "USB Serial"
            )
        )
    }
    
    override fun getHardwareInfo(): Map<String, String> {
        return mapOf(
            "manufacturer" to (deviceInfo["manufacturer"] as? String ?: "Unknown"),
            "model" to (deviceInfo["model"] as? String ?: "Generic Thermal Camera"),
            "type" to "Thermal Camera",
            "resolution" to "${thermalWidth}x${thermalHeight}",
            "connectionType" to "USB",
            "firmwareVersion" to "Unknown",
            "serialNumber" to (usbDevice?.serialNumber ?: "Unknown"),
            "deviceId" to (usbDevice?.deviceName ?: "Unknown"),
            "vendorId" to (vendorId?.toString() ?: "Unknown"),
            "productId" to (productId?.toString() ?: "Unknown")
        )
    }
    
    override fun configure(settings: Map<String, Any>): Boolean {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot configure - device not connected")
            return false
        }
        
        return try {
            // Generic configuration - send simple commands
            settings.forEach { (key, value) ->
                sendConfigurationCommand(key, value.toString())
            }
            
            Log.d(TAG, "Configured generic thermal camera with settings: $settings")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring device", e)
            false
        }
    }
    
    override fun getAvailableSettings(): Map<String, List<Any>> {
        return mapOf(
            "emissivity" to listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 0.95f, 1.0f),
            "temperatureUnit" to listOf("Celsius", "Fahrenheit", "Kelvin"),
            "colorPalette" to listOf("Grayscale", "Iron", "Rainbow", "Hot", "Cool"),
            "imageFormat" to listOf("RAW", "JPEG", "PNG"),
            "autoGain" to listOf(true, false),
            "shutterMode" to listOf("Auto", "Manual")
        )
    }
    
    override fun captureFrame(): ThermalCamera.ThermalFrame? {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot capture frame - device not connected")
            return null
        }
        
        return try {
            val buffer = ByteArray(thermalWidth * thermalHeight * 2) // Assume 2 bytes per pixel
            val len = usbSerialPort?.read(buffer, 2000) ?: 0
            
            if (len > 0) {
                processThermalData(buffer, len)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing frame", e)
            null
        }
    }
    
    override fun setFrameCallback(callback: (ThermalCamera.ThermalFrame) -> Unit) {
        frameCallback = callback
    }
    
    override fun shutdown() {
        disconnect()
        usbManager = null
        Log.d(TAG, "Generic thermal camera shutdown complete")
    }
    
    // Private helper methods
    
    private fun findCompatibleDriver(
        availableDrivers: List<UsbSerialDriver>,
        deviceIdentifier: String?
    ): UsbSerialDriver? {
        for (driver in availableDrivers) {
            val device = driver.device
            
            // If specific vendor/product IDs are provided, match them
            if (vendorId != null && productId != null) {
                if (device.vendorId == vendorId && device.productId == productId) {
                    if (deviceIdentifier == null || device.deviceName == deviceIdentifier) {
                        return driver
                    }
                }
            } else {
                // If no specific IDs, try to match by device identifier or use first available
                if (deviceIdentifier == null || device.deviceName == deviceIdentifier) {
                    return driver
                }
            }
        }
        
        // If no specific match found and no vendor/product IDs specified, return first driver
        if (vendorId == null && productId == null && availableDrivers.isNotEmpty()) {
            return availableDrivers[0]
        }
        
        return null
    }
    
    private fun readThermalData() {
        val expectedFrameSize = thermalWidth * thermalHeight * 2 // Assume 2 bytes per pixel
        val buffer = ByteArray(expectedFrameSize)
        
        while (isStreaming.get() && isConnected.get()) {
            try {
                val len = usbSerialPort?.read(buffer, 1000) ?: 0
                if (len > 0) {
                    val thermalFrame = processThermalData(buffer, len)
                    if (thermalFrame != null) {
                        // Send to callback
                        currentCallback?.onFrameReceived(thermalFrame)
                        frameCallback?.invoke(thermalFrame)
                        
                        // Update preview
                        updatePreview(thermalFrame.bitmap)
                        
                        // Save frame if recording
                        if (isRecording.get()) {
                            saveFrame(thermalFrame)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading thermal data", e)
                currentCallback?.onError("Data reading error", e)
                break
            }
        }
    }
    
    private fun processThermalData(buffer: ByteArray, len: Int): ThermalCamera.ThermalFrame? {
        return try {
            val timestamp = TimeManager.getCurrentTimestamp()
            
            // Parse thermal data using generic format
            val temperatureData = parseGenericThermalData(buffer, len)
            val bitmap = createThermalBitmap(temperatureData)
            
            ThermalCamera.ThermalFrame(
                timestamp = timestamp,
                bitmap = bitmap,
                temperatureData = temperatureData,
                metadata = mapOf(
                    "deviceId" to (usbDevice?.deviceName ?: "unknown"),
                    "frameSize" to len,
                    "expectedSize" to (thermalWidth * thermalHeight * 2),
                    "minTemp" to (temperatureData?.flatten()?.minOrNull() ?: 0.0f),
                    "maxTemp" to (temperatureData?.flatten()?.maxOrNull() ?: 0.0f)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing thermal data", e)
            null
        }
    }
    
    private fun parseGenericThermalData(buffer: ByteArray, len: Int): Array<FloatArray>? {
        return try {
            val temperatureData = Array(thermalHeight) { FloatArray(thermalWidth) }
            val bytesPerPixel = 2 // Assume 16-bit data
            
            var bufferIndex = 0
            for (y in 0 until thermalHeight) {
                for (x in 0 until thermalWidth) {
                    if (bufferIndex + bytesPerPixel - 1 < len) {
                        // Convert raw bytes to temperature (generic approach)
                        val rawValue = if (bytesPerPixel == 2) {
                            // 16-bit little-endian
                            ((buffer[bufferIndex + 1].toInt() and 0xFF) shl 8) or 
                            (buffer[bufferIndex].toInt() and 0xFF)
                        } else {
                            // 8-bit
                            buffer[bufferIndex].toInt() and 0xFF
                        }
                        
                        // Convert raw value to temperature using linear mapping
                        val normalizedValue = rawValue.toFloat() / 65535.0f // Normalize to 0-1
                        temperatureData[y][x] = minTemperature + normalizedValue * (maxTemperature - minTemperature)
                        
                        bufferIndex += bytesPerPixel
                    } else {
                        // Default temperature if not enough data
                        temperatureData[y][x] = (minTemperature + maxTemperature) / 2.0f
                    }
                }
            }
            
            temperatureData
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing generic thermal data", e)
            null
        }
    }
    
    private fun createThermalBitmap(temperatureData: Array<FloatArray>?): Bitmap {
        val bitmap = Bitmap.createBitmap(thermalWidth, thermalHeight, Bitmap.Config.ARGB_8888)
        
        if (temperatureData != null) {
            // Find temperature range for normalization
            val flatData = temperatureData.flatten()
            val minTemp = flatData.minOrNull() ?: minTemperature
            val maxTemp = flatData.maxOrNull() ?: maxTemperature
            val tempRange = maxTemp - minTemp
            
            for (y in 0 until thermalHeight) {
                for (x in 0 until thermalWidth) {
                    val temp = temperatureData[y][x]
                    val normalizedTemp = if (tempRange > 0) {
                        ((temp - minTemp) / tempRange).coerceIn(0.0f, 1.0f)
                    } else {
                        0.5f
                    }
                    
                    // Apply grayscale palette (simple and universal)
                    val color = applyGrayscalePalette(normalizedTemp)
                    bitmap.setPixel(x, y, color)
                }
            }
        } else {
            // Create a default pattern if no data
            for (y in 0 until thermalHeight) {
                for (x in 0 until thermalWidth) {
                    val intensity = ((x + y) % 256)
                    val color = 0xFF000000.toInt() or (intensity shl 16) or (intensity shl 8) or intensity
                    bitmap.setPixel(x, y, color)
                }
            }
        }
        
        return bitmap
    }
    
    private fun applyGrayscalePalette(normalizedValue: Float): Int {
        val intensity = (normalizedValue * 255).toInt().coerceIn(0, 255)
        return 0xFF000000.toInt() or (intensity shl 16) or (intensity shl 8) or intensity
    }
    
    private fun updatePreview(bitmap: Bitmap) {
        textureView?.post {
            val canvas = textureView?.lockCanvas()
            canvas?.let {
                // Scale bitmap to fit texture view
                val scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap, 
                    textureView?.width ?: bitmap.width, 
                    textureView?.height ?: bitmap.height, 
                    false
                )
                it.drawBitmap(scaledBitmap, 0f, 0f, null)
                textureView?.unlockCanvasAndPost(it)
            }
        }
    }
    
    private fun saveFrame(frame: ThermalCamera.ThermalFrame) {
        if (recordingDir == null || sessionId == null) return
        
        executor.execute {
            try {
                val filename = "generic_thermal_${sessionId}_${frame.timestamp}.png"
                val file = File(recordingDir, filename)
                
                FileOutputStream(file).use { fos ->
                    frame.bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                }
                
                Log.v(TAG, "Saved thermal frame: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving thermal frame", e)
            }
        }
    }
    
    private fun sendConfigurationCommand(command: String, value: String): Boolean {
        return try {
            val commandString = "$command=$value\n"
            val bytes = commandString.toByteArray()
            val written = usbSerialPort?.write(bytes, 1000) ?: 0
            written == bytes.size
        } catch (e: Exception) {
            Log.e(TAG, "Error sending configuration command", e)
            false
        }
    }
}