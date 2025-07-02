package com.fpygsrunified.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.android.guiUtilities.ShimmerBluetoothDialog
import com.shimmerresearch.bluetooth.ShimmerBluetooth
import com.shimmerresearch.driver.CallbackObject
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.FormatCluster
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.exceptions.ShimmerException
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class GsrHandler(private val activity: AppCompatActivity, private val gsrValueText: TextView) {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var shimmer: Shimmer? = null
    private var gsrConnected = false
    private var isStreaming = false
    private var sessionId: String? = null
    private var deviceId: String = "unknown"
    private var gsrDataWriter: BufferedWriter? = null
    private var sessionStartTime: Long = 0

    // Callback interface for GSR data events
    interface GsrDataCallback {
        fun onGsrDataReceived(conductance: Double, resistance: Double, timestamp: Long)
        fun onConnectionStateChanged(connected: Boolean, deviceAddress: String?)
        fun onStreamingStateChanged(streaming: Boolean)
        fun onError(error: String)
    }

    private var gsrCallback: GsrDataCallback? = null

    fun setGsrCallback(callback: GsrDataCallback) {
        this.gsrCallback = callback
    }

    fun setSessionInfo(sessionId: String, deviceId: String) {
        this.sessionId = sessionId
        this.deviceId = deviceId
    }

    fun initialize() {
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(activity, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            gsrCallback?.onError("Bluetooth not supported")
            return
        }

        if (bluetoothAdapter?.isEnabled == false) {
            Toast.makeText(activity, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            gsrCallback?.onError("Bluetooth not enabled")
        } else {
            // Initialize Shimmer with handler
            shimmer = Shimmer(shimmerHandler, activity)
            Log.d(TAG, "Shimmer GSR handler initialized")
        }
    }

    fun showDeviceSelectionDialog() {
        val intent = Intent(activity, ShimmerBluetoothDialog::class.java)
        activity.startActivityForResult(intent, ShimmerBluetoothDialog.REQUEST_CONNECT_SHIMMER)
    }

    fun connectToDevice(deviceAddress: String) {
        shimmer?.let { shimmerDevice ->
            try {
                shimmerDevice.connect(deviceAddress, "default")
                Log.d(TAG, "Attempting to connect to Shimmer device: $deviceAddress")
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to Shimmer device", e)
                gsrCallback?.onError("Failed to connect to device: ${e.message}")
            }
        }
    }

    fun startStreaming() {
        shimmer?.let { shimmerDevice ->
            try {
                if (gsrConnected) {
                    // Configure GSR sensor
                    shimmerDevice.setSensorEnabledState(Configuration.Shimmer3.SENSOR_ID.SHIMMER_GSR, true)
                    shimmerDevice.setSamplingRateShimmer(128.0) // 128 Hz as specified in requirements

                    // Start data logging if session is active
                    sessionId?.let { session ->
                        startDataLogging(session)
                    }

                    shimmerDevice.startStreaming()
                    isStreaming = true
                    sessionStartTime = System.currentTimeMillis()
                    gsrCallback?.onStreamingStateChanged(true)
                    Log.d(TAG, "GSR streaming started")
                } else {
                    gsrCallback?.onError("Device not connected")
                }
            } catch (e: ShimmerException) {
                Log.e(TAG, "Error starting GSR streaming", e)
                gsrCallback?.onError("Failed to start streaming: ${e.message}")
            }
        }
    }

    fun stopStreaming() {
        shimmer?.let { shimmerDevice ->
            try {
                shimmerDevice.stopStreaming()
                isStreaming = false
                gsrCallback?.onStreamingStateChanged(false)
                stopDataLogging()
                Log.d(TAG, "GSR streaming stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping GSR streaming", e)
                gsrCallback?.onError("Failed to stop streaming: ${e.message}")
            }
        }
    }

    private fun startDataLogging(sessionId: String) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "${sessionId}_${deviceId}_gsr_data.csv"
            val file = File(activity.getExternalFilesDir(null), filename)
            gsrDataWriter = BufferedWriter(FileWriter(file))
            gsrDataWriter?.write("timestamp,conductance_us,resistance_kohm,system_time_ms\n")
            Log.d(TAG, "Started GSR data logging to: ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e(TAG, "Error starting GSR data logging", e)
            gsrCallback?.onError("Failed to start data logging: ${e.message}")
        }
    }

    private fun stopDataLogging() {
        try {
            gsrDataWriter?.close()
            gsrDataWriter = null
            Log.d(TAG, "GSR data logging stopped")
        } catch (e: IOException) {
            Log.e(TAG, "Error stopping GSR data logging", e)
        }
    }

    fun disconnect() {
        shimmer?.disconnect()
    }

    fun release() {
        stopStreaming()
        disconnect()
        stopDataLogging()
        shimmer = null
    }

    fun isConnected(): Boolean = gsrConnected
    fun isStreaming(): Boolean = isStreaming

    /**
     * Handler for receiving messages from Shimmer device
     */
    private val shimmerHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET -> {
                    if (msg.obj is ObjectCluster) {
                        handleGsrData(msg.obj as ObjectCluster)
                    }
                }

                Shimmer.MESSAGE_TOAST -> {
                    val toastMsg = msg.data.getString(Shimmer.TOAST)
                    Toast.makeText(activity, toastMsg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Shimmer message: $toastMsg")
                }

                ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE -> {
                    handleStateChange(msg)
                }
            }
        }
    }

    private fun handleGsrData(objectCluster: ObjectCluster) {
        try {
            // Extract timestamp
            val timestampFormats =
                objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP)
            val timestampCluster = ObjectCluster.returnFormatCluster(timestampFormats, "CAL") as? FormatCluster
            val shimmerTimestamp = timestampCluster?.mData ?: 0.0

            // Extract GSR conductance
            val gsrFormats =
                objectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE)
            val gsrCluster = ObjectCluster.returnFormatCluster(gsrFormats, "CAL") as? FormatCluster
            val conductance = gsrCluster?.mData ?: 0.0

            // Calculate resistance (1/conductance)
            val resistance = if (conductance > 0) 1.0 / conductance else 0.0

            val systemTimestamp = System.currentTimeMillis()

            // Update UI
            activity.runOnUiThread {
                gsrValueText.text = String.format("GSR: %.2f μS (%.2f kΩ)", conductance, resistance)
            }

            // Log data to file
            gsrDataWriter?.let { writer ->
                writer.write("$shimmerTimestamp,$conductance,$resistance,$systemTimestamp\n")
                writer.flush()
            }

            // Notify callback
            gsrCallback?.onGsrDataReceived(conductance, resistance, systemTimestamp)

        } catch (e: Exception) {
            Log.e(TAG, "Error processing GSR data", e)
        }
    }

    private fun handleStateChange(msg: Message) {
        var state: ShimmerBluetooth.BT_STATE? = null
        var macAddress = ""

        when (msg.obj) {
            is ObjectCluster -> {
                state = (msg.obj as ObjectCluster).mState
                macAddress = (msg.obj as ObjectCluster).macAddress
            }

            is CallbackObject -> {
                state = (msg.obj as CallbackObject).mState
                macAddress = (msg.obj as CallbackObject).mBluetoothAddress
            }
        }

        when (state) {
            ShimmerBluetooth.BT_STATE.CONNECTED -> {
                gsrConnected = true
                gsrCallback?.onConnectionStateChanged(true, macAddress)
                Log.d(TAG, "Shimmer GSR device connected: $macAddress")
            }

            ShimmerBluetooth.BT_STATE.DISCONNECTED -> {
                gsrConnected = false
                isStreaming = false
                gsrCallback?.onConnectionStateChanged(false, macAddress)
                stopDataLogging()
                Log.d(TAG, "Shimmer GSR device disconnected: $macAddress")
            }

            ShimmerBluetooth.BT_STATE.CONNECTING -> {
                Log.d(TAG, "Connecting to Shimmer GSR device: $macAddress")
            }

            else -> {
                Log.d(TAG, "Shimmer state change: $state")
            }
        }
    }

    companion object {
        private const val TAG = "GsrHandler"
    }
}
