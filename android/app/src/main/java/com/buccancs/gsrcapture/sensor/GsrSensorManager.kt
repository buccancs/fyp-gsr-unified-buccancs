package com.buccancs.gsrcapture.sensor

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.buccancs.gsrcapture.utils.TimeManager
import java.io.File
import java.io.FileWriter
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages GSR sensor operations including connection, data streaming, and recording.
 * This class interfaces with the Shimmer3 GSR+ sensor over Bluetooth Low Energy.
 */
class GsrSensorManager(
    private val context: Context,
    private val sensorExecutor: ExecutorService
) {
    private val TAG = "GsrSensorManager"
    
    // Bluetooth components
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var shimmerDevice: BluetoothDevice? = null
    private var shimmerConnection: ShimmerConnection? = null
    
    // GSR sensor state
    private val isConnected = AtomicBoolean(false)
    private val isRecording = AtomicBoolean(false)
    private var gsrCallback: ((Float) -> Unit)? = null
    private var heartRateCallback: ((Int) -> Unit)? = null
    private var connectionStateCallback: ((Boolean) -> Unit)? = null
    
    // Recording state
    private var outputDirectory: File? = null
    private var sessionId: String? = null
    private var csvWriter: FileWriter? = null
    private var sampleCount: Int = 0
    
    // Shimmer GSR+ specific constants
    companion object {
        // Shimmer GSR+ service and characteristic UUIDs
        // Note: These are placeholder UUIDs and would need to be replaced with actual Shimmer UUIDs
        private val SHIMMER_SERVICE_UUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb")
        private val GSR_CHARACTERISTIC_UUID = UUID.fromString("00002345-0000-1000-8000-00805f9b34fb")
        private val PPG_CHARACTERISTIC_UUID = UUID.fromString("00003456-0000-1000-8000-00805f9b34fb")
        
        // Shimmer GSR+ sampling rate (Hz)
        private const val SAMPLING_RATE_HZ = 128
    }
    
    /**
     * Initializes the GSR sensor manager.
     */
    fun initialize() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }
    
    /**
     * Scans for and connects to the Shimmer GSR+ sensor.
     * @param deviceAddress MAC address of the Shimmer device (optional)
     * @return True if connection was successful, false otherwise
     */
    fun connectToSensor(deviceAddress: String? = null): Boolean {
        if (isConnected.get()) {
            Log.d(TAG, "Already connected to GSR sensor")
            return true
        }
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported on this device")
            return false
        }
        
        if (!bluetoothAdapter!!.isEnabled) {
            Log.e(TAG, "Bluetooth is not enabled")
            return false
        }
        
        try {
            // If device address is provided, connect directly
            if (deviceAddress != null) {
                shimmerDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                if (shimmerDevice == null) {
                    Log.e(TAG, "Could not find Shimmer device with address $deviceAddress")
                    return false
                }
            } else {
                // Scan for Shimmer devices
                // In a real implementation, you would use BluetoothLeScanner to scan for devices
                // For now, we'll simulate finding a device
                shimmerDevice = findShimmerDevice()
                if (shimmerDevice == null) {
                    Log.e(TAG, "No Shimmer GSR+ sensor found")
                    return false
                }
            }
            
            // Create and establish connection
            shimmerConnection = ShimmerConnection(shimmerDevice!!)
            val connected = shimmerConnection?.connect() ?: false
            
            if (connected) {
                isConnected.set(true)
                connectionStateCallback?.invoke(true)
                
                // Configure the sensor
                configureSensor()
                
                // Start data streaming
                startDataStreaming()
                
                Log.d(TAG, "Connected to Shimmer GSR+ sensor")
                return true
            } else {
                Log.e(TAG, "Failed to connect to Shimmer GSR+ sensor")
                disconnect()
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to GSR sensor", e)
            disconnect()
            return false
        }
    }
    
    /**
     * Simulates finding a Shimmer device.
     * In a real implementation, this would scan for BLE devices and filter for Shimmer devices.
     * @return A Shimmer BluetoothDevice, or null if not found
     */
    private fun findShimmerDevice(): BluetoothDevice? {
        // In a real implementation, you would scan for devices and filter for Shimmer devices
        // For now, we'll just return null to simulate no device found
        return null
    }
    
    /**
     * Configures the Shimmer GSR+ sensor.
     */
    private fun configureSensor() {
        // In a real implementation, you would configure the sensor settings
        // such as sampling rate, enabled sensors, etc.
        shimmerConnection?.setSamplingRate(SAMPLING_RATE_HZ)
        shimmerConnection?.enableGsrSensor(true)
        shimmerConnection?.enablePpgSensor(true)
    }
    
    /**
     * Starts streaming data from the Shimmer GSR+ sensor.
     */
    private fun startDataStreaming() {
        shimmerConnection?.startStreaming()
        
        // Set up data listeners
        shimmerConnection?.setGsrDataListener { gsrValue ->
            // Process GSR data
            processGsrData(gsrValue)
        }
        
        shimmerConnection?.setPpgDataListener { ppgValue ->
            // Process PPG data
            processPpgData(ppgValue)
        }
    }
    
    /**
     * Processes GSR data from the sensor.
     * @param gsrValue GSR value in microSiemens
     */
    private fun processGsrData(gsrValue: Float) {
        // Timestamp the data
        val timestampedData = TimeManager.timestampData(gsrValue)
        
        // Call the callback if set
        gsrCallback?.invoke(gsrValue)
        
        // Save data if recording
        if (isRecording.get()) {
            saveGsrData(timestampedData)
        }
    }
    
    /**
     * Processes PPG data from the sensor.
     * @param ppgValue PPG value
     */
    private fun processPpgData(ppgValue: Float) {
        // In a real implementation, you would process the PPG data to derive heart rate
        // For now, we'll just simulate a heart rate calculation
        val heartRate = calculateHeartRate(ppgValue)
        
        // Call the callback if set
        heartRateCallback?.invoke(heartRate)
    }
    
    /**
     * Calculates heart rate from PPG data.
     * @param ppgValue PPG value
     * @return Calculated heart rate in BPM
     */
    private fun calculateHeartRate(ppgValue: Float): Int {
        // In a real implementation, you would implement a heart rate calculation algorithm
        // For now, we'll just return a simulated heart rate
        return (60 + (Math.random() * 20).toInt())
    }
    
    /**
     * Sets a callback to receive GSR data.
     * @param callback Function to call with each new GSR value
     */
    fun setGsrCallback(callback: (Float) -> Unit) {
        gsrCallback = callback
    }
    
    /**
     * Sets a callback to receive heart rate data.
     * @param callback Function to call with each new heart rate value
     */
    fun setHeartRateCallback(callback: (Int) -> Unit) {
        heartRateCallback = callback
    }
    
    /**
     * Sets a callback to receive connection state changes.
     * @param callback Function to call when connection state changes
     */
    fun setConnectionStateCallback(callback: (Boolean) -> Unit) {
        connectionStateCallback = callback
    }
    
    /**
     * Starts recording GSR data.
     * @param outputDir Directory where data will be saved
     * @param sessionId Unique identifier for the recording session
     * @return True if recording started successfully, false otherwise
     */
    fun startRecording(outputDir: File, sessionId: String): Boolean {
        if (!isConnected.get()) {
            Log.e(TAG, "Cannot start recording: not connected to GSR sensor")
            return false
        }
        
        if (isRecording.get()) {
            Log.d(TAG, "Already recording")
            return true
        }
        
        outputDirectory = outputDir
        this.sessionId = sessionId
        sampleCount = 0
        
        try {
            // Create CSV file for GSR data
            val timestamp = TimeManager.getCurrentTimestamp()
            val csvFile = File(outputDir, "GSR_${sessionId}_${timestamp}.csv")
            
            csvWriter = FileWriter(csvFile)
            
            // Write CSV header
            csvWriter?.write("timestamp_nanos,session_offset_nanos,gsr_microsiemens\n")
            
            isRecording.set(true)
            Log.d(TAG, "Started recording GSR data to ${csvFile.absolutePath}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting GSR recording", e)
            csvWriter?.close()
            csvWriter = null
            return false
        }
    }
    
    /**
     * Stops recording GSR data.
     */
    fun stopRecording() {
        if (isRecording.getAndSet(false)) {
            try {
                csvWriter?.close()
                Log.d(TAG, "Stopped recording GSR data. Total samples: $sampleCount")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing GSR data file", e)
            } finally {
                csvWriter = null
            }
        }
    }
    
    /**
     * Saves GSR data to the CSV file.
     * @param data Timestamped GSR data
     */
    private fun saveGsrData(data: TimeManager.TimestampedData<Float>) {
        try {
            csvWriter?.write("${data.timestampNanos},${data.sessionOffsetNanos},${data.data}\n")
            csvWriter?.flush()
            sampleCount++
        } catch (e: Exception) {
            Log.e(TAG, "Error saving GSR data", e)
        }
    }
    
    /**
     * Disconnects from the GSR sensor.
     */
    fun disconnect() {
        stopRecording()
        
        try {
            shimmerConnection?.stopStreaming()
            shimmerConnection?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from GSR sensor", e)
        }
        
        shimmerConnection = null
        shimmerDevice = null
        
        isConnected.set(false)
        connectionStateCallback?.invoke(false)
        
        Log.d(TAG, "Disconnected from GSR sensor")
    }
    
    /**
     * Releases all resources.
     */
    fun shutdown() {
        disconnect()
        gsrCallback = null
        heartRateCallback = null
        connectionStateCallback = null
    }
    
    /**
     * Inner class to handle Shimmer device connection and data streaming.
     * In a real implementation, this would use the Shimmer API.
     */
    private inner class ShimmerConnection(private val device: BluetoothDevice) {
        private val isStreaming = AtomicBoolean(false)
        private var gsrDataListener: ((Float) -> Unit)? = null
        private var ppgDataListener: ((Float) -> Unit)? = null
        
        /**
         * Connects to the Shimmer device.
         * @return True if connection was successful, false otherwise
         */
        fun connect(): Boolean {
            // In a real implementation, you would use the Shimmer API to connect to the device
            // For now, we'll just simulate a successful connection
            return true
        }
        
        /**
         * Disconnects from the Shimmer device.
         */
        fun disconnect() {
            stopStreaming()
            // In a real implementation, you would use the Shimmer API to disconnect from the device
        }
        
        /**
         * Sets the sampling rate for the Shimmer device.
         * @param samplingRate Sampling rate in Hz
         */
        fun setSamplingRate(samplingRate: Int) {
            // In a real implementation, you would use the Shimmer API to set the sampling rate
        }
        
        /**
         * Enables or disables the GSR sensor.
         * @param enable True to enable, false to disable
         */
        fun enableGsrSensor(enable: Boolean) {
            // In a real implementation, you would use the Shimmer API to enable/disable the GSR sensor
        }
        
        /**
         * Enables or disables the PPG sensor.
         * @param enable True to enable, false to disable
         */
        fun enablePpgSensor(enable: Boolean) {
            // In a real implementation, you would use the Shimmer API to enable/disable the PPG sensor
        }
        
        /**
         * Starts streaming data from the Shimmer device.
         */
        fun startStreaming() {
            if (isStreaming.getAndSet(true)) {
                return
            }
            
            // In a real implementation, you would use the Shimmer API to start streaming
            // For now, we'll simulate data streaming with a background thread
            sensorExecutor.execute {
                var lastGsrValue = 0.5f
                var lastPpgValue = 0.0f
                var direction = 1
                
                while (isStreaming.get()) {
                    try {
                        // Simulate GSR data (0.5 to 20 microSiemens)
                        lastGsrValue += (Math.random() * 0.1f - 0.05f) * direction
                        if (lastGsrValue > 20f) {
                            lastGsrValue = 20f
                            direction = -1
                        } else if (lastGsrValue < 0.5f) {
                            lastGsrValue = 0.5f
                            direction = 1
                        }
                        
                        // Simulate PPG data (sine wave)
                        lastPpgValue += 0.1f
                        val ppgValue = Math.sin(lastPpgValue).toFloat() * 500 + 500
                        
                        // Call listeners
                        gsrDataListener?.invoke(lastGsrValue)
                        ppgDataListener?.invoke(ppgValue)
                        
                        // Sleep to simulate sampling rate
                        Thread.sleep((1000 / SAMPLING_RATE_HZ).toLong())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in simulated data streaming", e)
                        break
                    }
                }
            }
        }
        
        /**
         * Stops streaming data from the Shimmer device.
         */
        fun stopStreaming() {
            isStreaming.set(false)
            // In a real implementation, you would use the Shimmer API to stop streaming
        }
        
        /**
         * Sets a listener for GSR data.
         * @param listener Function to call with each new GSR value
         */
        fun setGsrDataListener(listener: (Float) -> Unit) {
            gsrDataListener = listener
        }
        
        /**
         * Sets a listener for PPG data.
         * @param listener Function to call with each new PPG value
         */
        fun setPpgDataListener(listener: (Float) -> Unit) {
            ppgDataListener = listener
        }
    }
}