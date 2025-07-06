package com.buccancs.gsrcapture.hardware.impl

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.buccancs.gsrcapture.hardware.interfaces.PhysiologicalSensor
import com.buccancs.gsrcapture.utils.TimeManager
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.bluetooth.ShimmerBluetooth
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.FormatCluster
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Shimmer-specific implementation of PhysiologicalSensor interface
 * Provides concrete implementation for Shimmer3 GSR+ devices
 */
class ShimmerPhysiologicalSensor(
    private val context: Context,
    private val executor: ExecutorService
) : PhysiologicalSensor {

    companion object {
        private const val TAG = "ShimmerPhysiologicalSensor"
        private const val DEFAULT_SAMPLING_RATE = 51.2 // Hz
    }

    private var shimmerDevice: Shimmer? = null
    private var shimmerBluetoothManager: ShimmerBluetoothManagerAndroid? = null
    private val isConnected = AtomicBoolean(false)
    private val isStreaming = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)

    private var currentCallback: PhysiologicalSensor.SensorCallback? = null
    private var recordingWriter: FileWriter? = null
    private var recordingFile: File? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    // Heart rate calculation variables
    private val ppgBuffer = mutableListOf<Float>()
    private var lastHeartRateCalculation = 0L
    private val heartRateCalculationInterval = 5000L // 5 seconds

    /**
     * Handler to receive messages from Shimmer device including sensor data
     */
    private val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET -> {
                    if (msg.obj is ObjectCluster) {
                        processShimmerData(msg.obj as ObjectCluster)
                    }
                }
                ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE -> {
                    // Handle state changes if needed
                    Log.d(TAG, "Shimmer state changed")
                }
                else -> {
                    super.handleMessage(msg)
                }
            }
        }
    }

    override fun initialize(): Boolean {
        return try {
            shimmerBluetoothManager = ShimmerBluetoothManagerAndroid(context, handler)

            // Initialize Bluetooth adapter
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter

            Log.d(TAG, "Shimmer Bluetooth Manager initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Shimmer Bluetooth Manager", e)
            false
        }
    }

    override fun connect(deviceAddress: String?): Boolean {
        if (isConnected.get()) {
            Log.d(TAG, "Already connected to Shimmer device")
            return true
        }

        return try {
            shimmerDevice = Shimmer(handler, context)

            val connected = if (deviceAddress != null) {
                shimmerDevice?.connect(deviceAddress, "default") != null
            } else {
                // Try to connect to any available Shimmer device
                connectToAnyAvailableDevice()
            }

            if (connected) {
                isConnected.set(true)
                configureSensor()
                currentCallback?.onConnectionStateChanged(true)
                Log.d(TAG, "Successfully connected to Shimmer device")
                true
            } else {
                Log.w(TAG, "Failed to connect to Shimmer device")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Shimmer device", e)
            currentCallback?.onError("Connection failed", e)
            false
        }
    }

    override fun disconnect() {
        try {
            stopStreaming()
            stopRecording()

            shimmerDevice?.stop()
            shimmerDevice?.disconnect()
            shimmerDevice = null

            isConnected.set(false)
            currentCallback?.onConnectionStateChanged(false)
            Log.d(TAG, "Disconnected from Shimmer device")
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from Shimmer device", e)
        }
    }

    override fun isConnected(): Boolean = isConnected.get()

    override fun startStreaming(callback: PhysiologicalSensor.SensorCallback): Boolean {
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

            // Data will be received automatically via the handler's MSG_IDENTIFIER_DATA_PACKET message
            // Start streaming
            shimmerDevice?.startStreaming()
            isStreaming.set(true)

            Log.d(TAG, "Started streaming from Shimmer device")
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
            shimmerDevice?.stopStreaming()
            isStreaming.set(false)
            currentCallback = null
            Log.d(TAG, "Stopped streaming from Shimmer device")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping streaming", e)
        }
    }

    override fun startRecording(outputDir: File, sessionId: String): Boolean {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot start recording - device not connected")
            return false
        }

        return try {
            // Create recording file
            outputDir.mkdirs()
            recordingFile = File(outputDir, "shimmer_${sessionId}_${System.currentTimeMillis()}.csv")
            recordingWriter = FileWriter(recordingFile!!)

            // Write CSV header
            recordingWriter?.write("timestamp,gsr_value,ppg_value,heart_rate\n")

            isRecording.set(true)
            Log.d(TAG, "Started recording to ${recordingFile?.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            false
        }
    }

    override fun stopRecording() {
        if (!isRecording.get()) return

        try {
            recordingWriter?.close()
            recordingWriter = null
            isRecording.set(false)
            Log.d(TAG, "Stopped recording to ${recordingFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }

    override fun getHardwareInfo(): Map<String, String> {
        return mapOf(
            "manufacturer" to "Shimmer Research",
            "model" to "Shimmer3 GSR+",
            "type" to "Physiological Sensor",
            "capabilities" to "GSR, PPG, Heart Rate",
            "connectionType" to "Bluetooth",
            "firmwareVersion" to (shimmerDevice?.getFirmwareVersionParsed() ?: "Unknown"),
            "deviceId" to (shimmerDevice?.getBluetoothAddress() ?: "Unknown")
        )
    }

    override fun configure(settings: Map<String, Any>): Boolean {
        if (!isConnected.get()) {
            Log.w(TAG, "Cannot configure - device not connected")
            return false
        }

        return try {
            val samplingRate = settings["samplingRate"] as? Double ?: DEFAULT_SAMPLING_RATE
            val enableGsr = settings["enableGsr"] as? Boolean ?: true
            val enablePpg = settings["enablePpg"] as? Boolean ?: true

            shimmerDevice?.let { device ->
                // Configure sampling rate
                device.setSamplingRateShimmer(samplingRate)

                // Configure enabled sensors
                var enabledSensors = 0
                if (enableGsr) {
                    enabledSensors = enabledSensors or Configuration.Shimmer3.SensorBitmap.SENSOR_GSR
                }
                if (enablePpg) {
                    enabledSensors = enabledSensors or Configuration.Shimmer3.SensorBitmap.SENSOR_INT_A12
                }

                @Suppress("DEPRECATION")
                device.setEnabledSensors(enabledSensors.toLong())

                // Configuration is applied in memory and will be used when streaming starts
                Log.d(TAG, "Configuration applied to Shimmer device")
            }

            Log.d(TAG, "Configured Shimmer device with settings: $settings")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error configuring device", e)
            false
        }
    }

    override fun getAvailableSettings(): Map<String, List<Any>> {
        return mapOf(
            "samplingRate" to listOf(1.0, 10.24, 51.2, 128.0, 256.0, 512.0, 1024.0),
            "enableGsr" to listOf(true, false),
            "enablePpg" to listOf(true, false),
            "gsrRange" to listOf("AUTO", "40kOhm to 10MOhm", "10kOhm to 1MOhm", "3.9kOhm to 100kOhm"),
            "batteryMonitoring" to listOf(true, false)
        )
    }

    override fun shutdown() {
        disconnect()
        shimmerBluetoothManager = null
        Log.d(TAG, "Shimmer sensor shutdown complete")
    }

    // Private helper methods

    private fun connectToAnyAvailableDevice(): Boolean {
        return try {
            if (bluetoothAdapter == null) {
                Log.e(TAG, "Bluetooth adapter not available")
                return false
            }

            if (!bluetoothAdapter!!.isEnabled) {
                Log.e(TAG, "Bluetooth is not enabled")
                return false
            }

            // Get paired devices and look for Shimmer devices
            val pairedDevices = bluetoothAdapter!!.bondedDevices
            var shimmerDevice: BluetoothDevice? = null

            for (device in pairedDevices) {
                val deviceName = device.name
                if (deviceName != null && deviceName.startsWith("Shimmer", ignoreCase = true)) {
                    shimmerDevice = device
                    Log.d(TAG, "Found paired Shimmer device: ${device.name} (${device.address})")
                    break
                }
            }

            if (shimmerDevice != null) {
                // Try to connect to the found Shimmer device
                val connected = this.shimmerDevice?.connect(shimmerDevice.address, "default") != null
                if (connected) {
                    Log.d(TAG, "Successfully connected to Shimmer device: ${shimmerDevice.address}")
                    return true
                } else {
                    Log.w(TAG, "Failed to connect to Shimmer device: ${shimmerDevice.address}")
                }
            } else {
                Log.w(TAG, "No paired Shimmer devices found")
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning for Shimmer devices", e)
            false
        }
    }

    private fun configureSensor() {
        shimmerDevice?.let { device ->
            try {
                // Set default configuration
                device.setSamplingRateShimmer(DEFAULT_SAMPLING_RATE)

                // Enable GSR and PPG sensors
                val enabledSensors = Configuration.Shimmer3.SensorBitmap.SENSOR_GSR or
                        Configuration.Shimmer3.SensorBitmap.SENSOR_INT_A12

                @Suppress("DEPRECATION")
                device.setEnabledSensors(enabledSensors.toLong())

                // Configuration is applied in memory and will be used when streaming starts
                Log.d(TAG, "Shimmer device configured with default settings")
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring Shimmer device", e)
            }
        }
    }

    private fun processShimmerData(objectCluster: ObjectCluster) {
        executor.execute {
            try {
                val timestamp = TimeManager.getCurrentTimestampNanos()
                var gsrValue: Float? = null
                var ppgValue: Float? = null
                var heartRate: Int? = null

                // Extract GSR data
                try {
                    val gsrData = objectCluster.getFormatClusterValue("GSR", "CAL")
                    gsrValue = gsrData?.toFloat()
                } catch (e: Exception) {
                    Log.v(TAG, "Could not extract GSR data: ${e.message}")
                }

                // Extract PPG data
                try {
                    val ppgData = objectCluster.getFormatClusterValue("PPG_A12", "CAL")
                    ppgValue = ppgData?.toFloat()
                } catch (e: Exception) {
                    Log.v(TAG, "Could not extract PPG data: ${e.message}")
                }

                // Calculate heart rate from PPG if available
                if (ppgValue != null) {
                    heartRate = calculateHeartRate(ppgValue)
                }

                // Create sensor reading
                val reading = PhysiologicalSensor.SensorReading(
                    timestamp = timestamp,
                    gsrValue = gsrValue,
                    ppgValue = ppgValue,
                    heartRate = heartRate,
                    additionalData = mapOf(
                        "deviceId" to (shimmerDevice?.getBluetoothAddress() ?: "unknown"),
                        "rawData" to objectCluster.toString()
                    )
                )

                // Send to callback
                currentCallback?.onDataReceived(reading)

                // Record if recording is active
                if (isRecording.get()) {
                    recordData(reading)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing Shimmer data", e)
                currentCallback?.onError("Data processing error", e)
            }
        }
    }

    private fun calculateHeartRate(ppgValue: Float): Int? {
        // Add PPG value to buffer for analysis
        ppgBuffer.add(ppgValue)

        // Keep buffer size manageable (about 10 seconds of data at 51.2 Hz)
        val maxBufferSize = (DEFAULT_SAMPLING_RATE * 10).toInt()
        if (ppgBuffer.size > maxBufferSize) {
            ppgBuffer.removeAt(0)
        }

        // Only calculate heart rate every 5 seconds and if we have enough data
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastHeartRateCalculation < heartRateCalculationInterval || ppgBuffer.size < 256) {
            return null
        }

        lastHeartRateCalculation = currentTime

        return try {
            // Simple peak detection algorithm
            val peaks = detectPeaks(ppgBuffer.toList())

            if (peaks.size < 2) {
                return null
            }

            // Calculate average time between peaks
            val peakIntervals = mutableListOf<Float>()
            for (i in 1 until peaks.size) {
                val interval = (peaks[i] - peaks[i-1]) / DEFAULT_SAMPLING_RATE // Convert to seconds
                if (interval > 0.4 && interval < 2.0) { // Filter reasonable heart rate intervals (30-150 BPM)
                    peakIntervals.add(interval.toFloat())
                }
            }

            if (peakIntervals.isEmpty()) {
                return null
            }

            // Calculate BPM from average interval
            val avgInterval = peakIntervals.average()
            val bpm = (60.0 / avgInterval).toInt()

            // Validate BPM range
            if (bpm in 40..200) {
                Log.d(TAG, "Calculated heart rate: $bpm BPM")
                bpm
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error calculating heart rate", e)
            null
        }
    }

    /**
     * Simple peak detection algorithm for PPG signal
     */
    private fun detectPeaks(signal: List<Float>): List<Int> {
        val peaks = mutableListOf<Int>()
        val minPeakDistance = (DEFAULT_SAMPLING_RATE * 0.4).toInt() // Minimum 0.4 seconds between peaks

        if (signal.size < 3) return peaks

        // Simple derivative-based peak detection
        for (i in 1 until signal.size - 1) {
            val prev = signal[i - 1]
            val curr = signal[i]
            val next = signal[i + 1]

            // Check if current point is a local maximum
            if (curr > prev && curr > next) {
                // Check minimum distance from last peak
                if (peaks.isEmpty() || i - peaks.last() >= minPeakDistance) {
                    // Additional threshold check - peak should be significant
                    val localMean = signal.subList(maxOf(0, i - 10), minOf(signal.size, i + 10)).average()
                    if (curr > localMean + 0.1) { // Threshold above local mean
                        peaks.add(i)
                    }
                }
            }
        }

        return peaks
    }

    private fun recordData(reading: PhysiologicalSensor.SensorReading) {
        try {
            recordingWriter?.write(
                "${reading.timestamp},${reading.gsrValue ?: ""},${reading.ppgValue ?: ""},${reading.heartRate ?: ""}\n"
            )
            recordingWriter?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "Error recording data", e)
        }
    }
}
