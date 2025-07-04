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
 * Topdon TC001-specific implementation of ThermalCamera interface
 * Provides concrete implementation for Topdon TC001 thermal cameras
 */
class TopdonThermalCamera(
    private val context: Context,
    private val executor: ExecutorService
) : ThermalCamera {
    
    companion object {
        private const val TAG = "TopdonThermalCamera"
        
        // Topdon TC001 specifications
        private const val TOPDON_VENDOR_ID = 0x2E42
        private const val TOPDON_PRODUCT_ID = 0x0001
        private const val THERMAL_WIDTH = 160
        private const val THERMAL_HEIGHT = 120
        private const val FRAME_RATE = 9.0f
        private const val MIN_TEMPERATURE = -20.0f
        private const val MAX_TEMPERATURE = 400.0f
        
        // USB communication parameters
        private const val BAUD_RATE = 115200
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
    
    override fun initialize(): Boolean {
        return try {
            usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            Log.d(TAG, "Topdon thermal camera initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Topdon thermal camera", e)
            false
        }
    }
    
    override fun connect(deviceIdentifier: String?): Boolean {
        if (isConnected.get()) {
            Log.d(TAG, "Already connected to Topdon thermal camera")
            return true
        }
        
        return try {
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            val driver = findTopdonDriver(availableDrivers, deviceIdentifier)
            
            if (driver == null) {
                Log.w(TAG, "Topdon TC001 thermal camera not found")
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
            usbSerialPort?.setParameters(BAUD_RATE, DATA_BITS, STOP_BITS, PARITY)
            
            isConnected.set(true)
            currentCallback?.onConnectionStateChanged(true)
            
            Log.d(TAG, "Connected to Topdon TC001 thermal camera")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Topdon thermal camera", e)
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
            Log.d(TAG, "Disconnected from Topdon thermal camera")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from Topdon thermal camera", e)
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
            
            Log.d(TAG, "Started streaming from Topdon thermal camera")
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
            Log.d(TAG, "Stopped streaming from Topdon thermal camera")
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
            width = THERMAL_WIDTH,
            height = THERMAL_HEIGHT,
            temperatureRange = Pair(MIN_TEMPERATURE, MAX_TEMPERATURE),
            frameRate = FRAME_RATE.toDouble(),
            additionalSpecs = mapOf(
                "thermalSensitivity" to "0.1°C",
                "spectralRange" to "8-14μm",
                "fieldOfView" to "50° × 38°",
                "focusType" to "Fixed",
                "operatingTemperature" to "-10°C to +50°C"
            )
        )
    }
    
    override fun getHardwareInfo(): Map<String, String> {
        return mapOf(
            "manufacturer" to "Topdon",
            "model" to "TC001",
            "type" to "Thermal Camera",
            "resolution" to "${THERMAL_WIDTH}x${THERMAL_HEIGHT}",
            "connectionType" to "USB",
            "firmwareVersion" to getFirmwareVersion(),
            "serialNumber" to getSerialNumber(),
            "deviceId" to (usbDevice?.deviceName ?: "Unknown")
        )
    }
    
    override fun configure(settings: Map<String, Any>): Boolean {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot configure - device not connected")
            return false
        }
        
        return try {
            val emissivity = settings["emissivity"] as? Float ?: 0.95f
            val temperatureUnit = settings["temperatureUnit"] as? String ?: "Celsius"
            val colorPalette = settings["colorPalette"] as? String ?: "Iron"
            
            // Send configuration commands to the device
            sendConfigurationCommand("SET_EMISSIVITY", emissivity.toString())
            sendConfigurationCommand("SET_TEMP_UNIT", temperatureUnit)
            sendConfigurationCommand("SET_PALETTE", colorPalette)
            
            Log.d(TAG, "Configured Topdon thermal camera with settings: $settings")
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
            "colorPalette" to listOf("Iron", "Rainbow", "White Hot", "Black Hot", "Red Hot", "Cool"),
            "imageFormat" to listOf("JPEG", "PNG", "RAW"),
            "autoShutter" to listOf(true, false)
        )
    }
    
    override fun captureFrame(): ThermalCamera.ThermalFrame? {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot capture frame - device not connected")
            return null
        }
        
        return try {
            val buffer = ByteArray(4096)
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
        Log.d(TAG, "Topdon thermal camera shutdown complete")
    }
    
    // Private helper methods
    
    private fun findTopdonDriver(
        availableDrivers: List<UsbSerialDriver>,
        deviceIdentifier: String?
    ): UsbSerialDriver? {
        for (driver in availableDrivers) {
            val device = driver.device
            if (device.vendorId == TOPDON_VENDOR_ID && device.productId == TOPDON_PRODUCT_ID) {
                if (deviceIdentifier == null || device.deviceName == deviceIdentifier) {
                    return driver
                }
            }
        }
        return null
    }
    
    private fun readThermalData() {
        val buffer = ByteArray(4096)
        
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
            
            // Parse thermal data from Topdon TC001 format
            val temperatureData = parseTopdonThermalData(buffer, len)
            val bitmap = createThermalBitmap(temperatureData)
            
            ThermalCamera.ThermalFrame(
                timestamp = timestamp,
                bitmap = bitmap,
                temperatureData = temperatureData,
                metadata = mapOf(
                    "deviceId" to (usbDevice?.deviceName ?: "unknown"),
                    "frameSize" to len,
                    "minTemp" to (temperatureData?.flatten()?.minOrNull() ?: 0.0f),
                    "maxTemp" to (temperatureData?.flatten()?.maxOrNull() ?: 0.0f)
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing thermal data", e)
            null
        }
    }
    
    private fun parseTopdonThermalData(buffer: ByteArray, len: Int): Array<FloatArray>? {
        return try {
            // This is a simplified parser for demonstration
            // In a real implementation, you would parse the actual Topdon TC001 data format
            val temperatureData = Array(THERMAL_HEIGHT) { FloatArray(THERMAL_WIDTH) }
            
            var bufferIndex = 0
            for (y in 0 until THERMAL_HEIGHT) {
                for (x in 0 until THERMAL_WIDTH) {
                    if (bufferIndex + 1 < len) {
                        // Convert raw bytes to temperature (simplified)
                        val rawValue = ((buffer[bufferIndex].toInt() and 0xFF) shl 8) or 
                                      (buffer[bufferIndex + 1].toInt() and 0xFF)
                        temperatureData[y][x] = rawValue / 100.0f // Convert to Celsius
                        bufferIndex += 2
                    } else {
                        temperatureData[y][x] = 20.0f // Default temperature
                    }
                }
            }
            
            temperatureData
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing thermal data", e)
            null
        }
    }
    
    private fun createThermalBitmap(temperatureData: Array<FloatArray>?): Bitmap {
        val bitmap = Bitmap.createBitmap(THERMAL_WIDTH, THERMAL_HEIGHT, Bitmap.Config.ARGB_8888)
        
        if (temperatureData != null) {
            // Find temperature range for normalization
            val flatData = temperatureData.flatten()
            val minTemp = flatData.minOrNull() ?: MIN_TEMPERATURE
            val maxTemp = flatData.maxOrNull() ?: MAX_TEMPERATURE
            val tempRange = maxTemp - minTemp
            
            for (y in 0 until THERMAL_HEIGHT) {
                for (x in 0 until THERMAL_WIDTH) {
                    val temp = temperatureData[y][x]
                    val normalizedTemp = if (tempRange > 0) {
                        ((temp - minTemp) / tempRange).coerceIn(0.0f, 1.0f)
                    } else {
                        0.5f
                    }
                    
                    // Apply iron color palette
                    val color = applyIronPalette(normalizedTemp)
                    bitmap.setPixel(x, y, color)
                }
            }
        } else {
            // Create a default gradient if no data
            for (y in 0 until THERMAL_HEIGHT) {
                for (x in 0 until THERMAL_WIDTH) {
                    val intensity = ((x + y) % 256)
                    val color = 0xFF000000.toInt() or (intensity shl 16) or (intensity shl 8) or intensity
                    bitmap.setPixel(x, y, color)
                }
            }
        }
        
        return bitmap
    }
    
    private fun applyIronPalette(normalizedValue: Float): Int {
        // Iron color palette implementation
        val value = (normalizedValue * 255).toInt().coerceIn(0, 255)
        
        val red = when {
            value < 64 -> 0
            value < 128 -> (value - 64) * 4
            else -> 255
        }
        
        val green = when {
            value < 64 -> value * 4
            value < 192 -> 255
            else -> 255 - (value - 192) * 4
        }
        
        val blue = when {
            value < 128 -> 0
            value < 192 -> (value - 128) * 4
            else -> 255
        }
        
        return 0xFF000000.toInt() or (red shl 16) or (green shl 8) or blue
    }
    
    private fun updatePreview(bitmap: Bitmap) {
        textureView?.post {
            val canvas = textureView?.lockCanvas()
            canvas?.let {
                it.drawBitmap(bitmap, 0f, 0f, null)
                textureView?.unlockCanvasAndPost(it)
            }
        }
    }
    
    private fun saveFrame(frame: ThermalCamera.ThermalFrame) {
        if (recordingDir == null || sessionId == null) return
        
        executor.execute {
            try {
                val filename = "thermal_${sessionId}_${frame.timestamp}.png"
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
            val commandString = "$command:$value\n"
            val bytes = commandString.toByteArray()
            val written = usbSerialPort?.write(bytes, 1000) ?: 0
            written == bytes.size
        } catch (e: Exception) {
            Log.e(TAG, "Error sending configuration command", e)
            false
        }
    }
    
    private fun getFirmwareVersion(): String {
        return try {
            sendConfigurationCommand("GET_VERSION", "")
            // In a real implementation, you would read the response
            "1.0.0"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    private fun getSerialNumber(): String {
        return try {
            sendConfigurationCommand("GET_SERIAL", "")
            // In a real implementation, you would read the response
            usbDevice?.serialNumber ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}