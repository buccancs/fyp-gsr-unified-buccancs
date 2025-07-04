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

// Shimmer SDK imports
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.driver.ShimmerDevice

/**
 * Manages GSR sensor operations including connection, data streaming, and recording.
 * This class interfaces with the Shimmer3 GSR+ sensor over Bluetooth Low Energy.
 */
class GsrSensorManager(
    private val context: Context,
    private val sensorExecutor: ExecutorService
) {
    private val TAG = "GsrSensorManager"

    // Shimmer SDK components
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var shimmerBluetoothManager: ShimmerBluetoothManagerAndroid? = null
    private var shimmerDevice: Shimmer? = null

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
        // Shimmer GSR+ sampling rate (Hz)
        private const val SAMPLING_RATE_HZ = 128
    }

    /**
     * Initializes the GSR sensor manager.
     */
    fun initialize() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Initialize Shimmer Bluetooth Manager
        shimmerBluetoothManager = ShimmerBluetoothManagerAndroid(context, bluetoothAdapter)
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
                val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                if (bluetoothDevice == null) {
                    Log.e(TAG, "Could not find Shimmer device with address $deviceAddress")
                    return false
                }

                // Create Shimmer device using the SDK
                shimmerDevice = Shimmer(shimmerBluetoothManager)
                shimmerDevice?.setBluetoothRadio(bluetoothDevice)
            } else {
                // Scan for Shimmer devices using the SDK
                val availableDevices = shimmerBluetoothManager?.getShimmerDeviceList()
                if (availableDevices.isNullOrEmpty()) {
                    Log.e(TAG, "No Shimmer GSR+ sensor found")
                    return false
                }

                // Use the first available Shimmer device
                shimmerDevice = availableDevices[0]
            }

            // Set up data callback before connecting
            shimmerDevice?.setDataProcessingCallback { objectCluster ->
                processShimmerData(objectCluster)
            }

            // Connect to the device
            val connected = shimmerDevice?.connect() ?: false

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
     * Processes data from the Shimmer SDK ObjectCluster.
     * @param objectCluster Data cluster containing sensor readings
     */
    private fun processShimmerData(objectCluster: ObjectCluster) {
        // Extract GSR data
        val gsrData = objectCluster.getFormatClusterValue(Configuration.Shimmer3.ObjectClusterSensorName.GSR_SKIN_CONDUCTANCE)
        if (gsrData != null) {
            processGsrData(gsrData.data.toFloat())
        }

        // Extract PPG data
        val ppgData = objectCluster.getFormatClusterValue(Configuration.Shimmer3.ObjectClusterSensorName.PPG_A12)
        if (ppgData != null) {
            processPpgData(ppgData.data.toFloat())
        }
    }

    /**
     * Configures the Shimmer GSR+ sensor.
     */
    private fun configureSensor() {
        shimmerDevice?.let { device ->
            // Set sampling rate
            device.setSamplingRateShimmer(SAMPLING_RATE_HZ.toDouble())

            // Enable GSR sensor
            device.setEnabledSensors(
                Configuration.Shimmer3.SensorBitmap.SENSOR_GSR or
                Configuration.Shimmer3.SensorBitmap.SENSOR_PPG_A12
            )

            // Write configuration to device
            device.writeShimmerAndSensorConfiguration()
        }
    }

    /**
     * Starts streaming data from the Shimmer GSR+ sensor.
     */
    private fun startDataStreaming() {
        shimmerDevice?.startStreaming()
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
            shimmerDevice?.stopStreaming()
            shimmerDevice?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error disconnecting from GSR sensor", e)
        }

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

}
