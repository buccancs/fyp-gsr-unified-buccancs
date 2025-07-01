package com.gsrmultimodal.android

import android.util.Log
import edu.ucsd.sccn.LSL
import gsrmultimodal.GSRData
import gsrmultimodal.ThermalData
import gsrmultimodal.Command
import gsrmultimodal.CommandResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages LSL (Lab Streaming Layer) outlets for streaming sensor data and commands
 * This class handles the creation and management of LSL streams for different data types
 */
class LslStreamManager(private val deviceId: String) {
    
    companion object {
        private const val TAG = "LslStreamManager"
        
        // Stream types as defined in the blueprint
        const val STREAM_TYPE_GSR = "GSR"
        const val STREAM_TYPE_THERMAL = "Thermal"
        const val STREAM_TYPE_COMMAND = "Command"
        const val STREAM_TYPE_RESPONSE = "CommandResponse"
        const val STREAM_TYPE_CAMERA = "Camera"
    }
    
    private val outlets = ConcurrentHashMap<String, LSL.StreamOutlet>()
    private var isInitialized = false
    
    /**
     * Initialize the LSL stream manager
     */
    fun initialize(): Boolean {
        return try {
            Log.i(TAG, "Initializing LSL Stream Manager for device: $deviceId")
            isInitialized = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LSL Stream Manager", e)
            false
        }
    }
    
    /**
     * Create GSR data stream outlet
     */
    fun createGsrStream(): Boolean {
        return try {
            if (outlets.containsKey(STREAM_TYPE_GSR)) {
                Log.w(TAG, "GSR stream already exists")
                return true
            }
            
            val streamInfo = LSL.StreamInfo(
                "${STREAM_TYPE_GSR}_$deviceId",
                STREAM_TYPE_GSR,
                3, // channels: conductance, resistance, quality
                128.0, // nominal sampling rate (Hz)
                LSL.ChannelFormat.double64,
                deviceId
            )
            
            // Add channel descriptions
            val channels = streamInfo.desc().append_child("channels")
            channels.append_child("channel")
                .append_child_value("label", "conductance")
                .append_child_value("unit", "microsiemens")
                .append_child_value("type", "GSR")
            
            channels.append_child("channel")
                .append_child_value("label", "resistance")
                .append_child_value("unit", "ohms")
                .append_child_value("type", "GSR")
                
            channels.append_child("channel")
                .append_child_value("label", "quality")
                .append_child_value("unit", "percent")
                .append_child_value("type", "Quality")
            
            val outlet = LSL.StreamOutlet(streamInfo)
            outlets[STREAM_TYPE_GSR] = outlet
            
            Log.i(TAG, "Created GSR stream outlet: ${streamInfo.name()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create GSR stream", e)
            false
        }
    }
    
    /**
     * Create thermal data stream outlet
     */
    fun createThermalStream(): Boolean {
        return try {
            if (outlets.containsKey(STREAM_TYPE_THERMAL)) {
                Log.w(TAG, "Thermal stream already exists")
                return true
            }
            
            val streamInfo = LSL.StreamInfo(
                "${STREAM_TYPE_THERMAL}_$deviceId",
                STREAM_TYPE_THERMAL,
                6, // channels: width, height, min_temp, max_temp, avg_temp, frame_number
                25.0, // nominal sampling rate (Hz)
                LSL.ChannelFormat.float32,
                deviceId
            )
            
            // Add channel descriptions
            val channels = streamInfo.desc().append_child("channels")
            channels.append_child("channel")
                .append_child_value("label", "width")
                .append_child_value("unit", "pixels")
                .append_child_value("type", "Dimension")
                
            channels.append_child("channel")
                .append_child_value("label", "height")
                .append_child_value("unit", "pixels")
                .append_child_value("type", "Dimension")
                
            channels.append_child("channel")
                .append_child_value("label", "min_temp")
                .append_child_value("unit", "celsius")
                .append_child_value("type", "Temperature")
                
            channels.append_child("channel")
                .append_child_value("label", "max_temp")
                .append_child_value("unit", "celsius")
                .append_child_value("type", "Temperature")
                
            channels.append_child("channel")
                .append_child_value("label", "avg_temp")
                .append_child_value("unit", "celsius")
                .append_child_value("type", "Temperature")
                
            channels.append_child("channel")
                .append_child_value("label", "frame_number")
                .append_child_value("unit", "count")
                .append_child_value("type", "Index")
            
            val outlet = LSL.StreamOutlet(streamInfo)
            outlets[STREAM_TYPE_THERMAL] = outlet
            
            Log.i(TAG, "Created thermal stream outlet: ${streamInfo.name()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create thermal stream", e)
            false
        }
    }
    
    /**
     * Create command response stream outlet
     */
    fun createCommandResponseStream(): Boolean {
        return try {
            if (outlets.containsKey(STREAM_TYPE_RESPONSE)) {
                Log.w(TAG, "Command response stream already exists")
                return true
            }
            
            val streamInfo = LSL.StreamInfo(
                "${STREAM_TYPE_RESPONSE}_$deviceId",
                STREAM_TYPE_RESPONSE,
                1, // single channel for serialized protobuf data
                LSL.IRREGULAR_RATE, // irregular sampling rate
                LSL.ChannelFormat.string,
                deviceId
            )
            
            val outlet = LSL.StreamOutlet(streamInfo)
            outlets[STREAM_TYPE_RESPONSE] = outlet
            
            Log.i(TAG, "Created command response stream outlet: ${streamInfo.name()}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create command response stream", e)
            false
        }
    }
    
    /**
     * Push GSR data to LSL stream
     */
    fun pushGsrData(gsrData: GSRData) {
        try {
            val outlet = outlets[STREAM_TYPE_GSR]
            if (outlet == null) {
                Log.w(TAG, "GSR stream outlet not found")
                return
            }
            
            val sample = doubleArrayOf(
                gsrData.conductance,
                gsrData.resistance,
                gsrData.quality.toDouble()
            )
            
            // Convert protobuf timestamp to LSL timestamp
            val timestamp = gsrData.timestamp.seconds.toDouble() + gsrData.timestamp.nanos / 1e9
            outlet.push_sample(sample, timestamp)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push GSR data", e)
        }
    }
    
    /**
     * Push thermal data to LSL stream
     */
    fun pushThermalData(thermalData: ThermalData) {
        try {
            val outlet = outlets[STREAM_TYPE_THERMAL]
            if (outlet == null) {
                Log.w(TAG, "Thermal stream outlet not found")
                return
            }
            
            val sample = floatArrayOf(
                thermalData.width.toFloat(),
                thermalData.height.toFloat(),
                thermalData.minTemp,
                thermalData.maxTemp,
                thermalData.avgTemp,
                thermalData.frameNumber.toFloat()
            )
            
            // Convert protobuf timestamp to LSL timestamp
            val timestamp = thermalData.timestamp.seconds.toDouble() + thermalData.timestamp.nanos / 1e9
            outlet.push_sample(sample, timestamp)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push thermal data", e)
        }
    }
    
    /**
     * Push command response to LSL stream
     */
    fun pushCommandResponse(response: CommandResponse) {
        try {
            val outlet = outlets[STREAM_TYPE_RESPONSE]
            if (outlet == null) {
                Log.w(TAG, "Command response stream outlet not found")
                return
            }
            
            // Serialize protobuf message to byte array and then to base64 string
            val serializedData = android.util.Base64.encodeToString(
                response.toByteArray(),
                android.util.Base64.DEFAULT
            )
            
            val sample = arrayOf(serializedData)
            
            // Convert protobuf timestamp to LSL timestamp
            val timestamp = response.timestamp.seconds.toDouble() + response.timestamp.nanos / 1e9
            outlet.push_sample(sample, timestamp)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to push command response", e)
        }
    }
    
    /**
     * Close all streams and cleanup resources
     */
    fun cleanup() {
        try {
            Log.i(TAG, "Cleaning up LSL streams")
            outlets.values.forEach { outlet ->
                outlet.close()
            }
            outlets.clear()
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    /**
     * Get the list of active stream names
     */
    fun getActiveStreams(): List<String> {
        return outlets.keys.toList()
    }
    
    /**
     * Check if a specific stream type is active
     */
    fun isStreamActive(streamType: String): Boolean {
        return outlets.containsKey(streamType)
    }
}