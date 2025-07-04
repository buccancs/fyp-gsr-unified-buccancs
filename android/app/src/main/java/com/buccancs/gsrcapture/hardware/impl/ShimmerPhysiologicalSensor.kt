package com.buccancs.gsrcapture.hardware.impl

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.buccancs.gsrcapture.hardware.interfaces.PhysiologicalSensor
import com.buccancs.gsrcapture.utils.TimeManager
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice
import java.io.File
import java.io.FileWriter
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

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
    
    private val handler = Handler(Looper.getMainLooper())
    
    override fun initialize(): Boolean {
        return try {
            shimmerBluetoothManager = ShimmerBluetoothManagerAndroid(context, handler)
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
            
            // Set up data callback
            shimmerDevice?.setDataProcessingCallback { objectCluster ->
                processShimmerData(objectCluster)
            }
            
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
            "firmwareVersion" to (shimmerDevice?.getFirmwareVersionFullName() ?: "Unknown"),
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
                    enabledSensors = enabledSensors or Configuration.Shimmer3.SensorBitmap.SENSOR_INT_ADC_A12
                }
                
                device.setEnabledSensors(enabledSensors)
                device.writeConfiguration()
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
        // In a real implementation, you would scan for available Shimmer devices
        // and connect to the first one found
        return false
    }
    
    private fun configureSensor() {
        shimmerDevice?.let { device ->
            try {
                // Set default configuration
                device.setSamplingRateShimmer(DEFAULT_SAMPLING_RATE)
                
                // Enable GSR and PPG sensors
                val enabledSensors = Configuration.Shimmer3.SensorBitmap.SENSOR_GSR or
                        Configuration.Shimmer3.SensorBitmap.SENSOR_INT_ADC_A12
                
                device.setEnabledSensors(enabledSensors)
                device.writeConfiguration()
                
                Log.d(TAG, "Shimmer device configured with default settings")
            } catch (e: Exception) {
                Log.e(TAG, "Error configuring Shimmer device", e)
            }
        }
    }
    
    private fun processShimmerData(objectCluster: ObjectCluster) {
        executor.execute {
            try {
                val timestamp = TimeManager.getCurrentTimestamp()
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
        // Simplified heart rate calculation
        // In a real implementation, you would use proper signal processing
        // to detect peaks and calculate BPM
        return null
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