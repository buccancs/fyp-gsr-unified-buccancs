package com.gsrmultimodal.android

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.StatFs
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Enhanced NetworkHandler implementing JSON protocol for PC communication.
 * Supports device registration, heartbeat monitoring, and command processing.
 */
class NetworkHandler(private val activity: MainActivity) {

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val isConnected = AtomicBoolean(false)
    private val isRunning = AtomicBoolean(false)

    private var connectionJob: Job? = null
    private var heartbeatJob: Job? = null
    private var listenerJob: Job? = null

    private val deviceId = "android_device_${System.currentTimeMillis()}"
    private var serverTimeOffset: Long = 0
    private var lastHeartbeatTime: Long = 0

    companion object {
        private const val TAG = "NetworkHandler"
        private const val HEARTBEAT_INTERVAL = 30000L // 30 seconds
        private const val CONNECTION_TIMEOUT = 5000 // 5 seconds
        private const val READ_TIMEOUT = 60000 // 60 seconds
    }

    /**
     * Connect to the PC server with device registration.
     */
    fun connect(serverIp: String, serverPort: Int) {
        if (isConnected.get()) {
            Log.w(TAG, "Already connected to server")
            return
        }

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Attempting to connect to $serverIp:$serverPort")

                socket = Socket().apply {
                    soTimeout = READ_TIMEOUT
                    connect(java.net.InetSocketAddress(serverIp, serverPort), CONNECTION_TIMEOUT)
                }

                outputStream = socket?.getOutputStream()
                inputStream = socket?.getInputStream()

                if (socket?.isConnected == true) {
                    isConnected.set(true)
                    isRunning.set(true)

                    Log.d(TAG, "Connected to server, starting registration")

                    // Send device registration
                    sendDeviceRegistration()

                    // Start message listener
                    startMessageListener()

                    // Start heartbeat
                    startHeartbeat()

                    withContext(Dispatchers.Main) {
                        activity.updateStatus("Connected to PC")
                    }
                } else {
                    throw Exception("Failed to establish connection")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to server", e)
                cleanup()
                withContext(Dispatchers.Main) {
                    activity.updateStatus("Connection failed: ${e.message}")
                }
            }
        }
    }

    /**
     * Send device registration message to PC.
     */
    private suspend fun sendDeviceRegistration() {
        val deviceInfo = JSONObject().apply {
            put("device_model", android.os.Build.MODEL)
            put("android_version", android.os.Build.VERSION.RELEASE)
            put("app_version", "1.0")
            put("capabilities", JSONObject().apply {
                put("rgb_camera", true)
                put("thermal_camera", true)
                put("gsr_sensor", true)
                put("audio_recording", true)
            })
            put("ip_address", activity.getDeviceIPAddress())
        }

        val registrationMessage = JSONObject().apply {
            put("command", "register_device")
            put("device_id", deviceId)
            put("timestamp", System.currentTimeMillis() / 1000.0)
            put("data", deviceInfo)
        }

        sendMessage(registrationMessage)
        Log.d(TAG, "Device registration sent")
    }

    /**
     * Start the message listener coroutine.
     */
    private fun startMessageListener() {
        listenerJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(4096)

            try {
                while (isRunning.get() && socket?.isConnected == true) {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val messageStr = String(buffer, 0, bytesRead, Charsets.UTF_8)
                        processIncomingMessage(messageStr)
                    } else if (bytesRead == -1) {
                        Log.w(TAG, "Server closed connection")
                        break
                    }
                }
            } catch (e: Exception) {
                if (isRunning.get()) {
                    Log.e(TAG, "Error in message listener", e)
                }
            }

            if (isRunning.get()) {
                withContext(Dispatchers.Main) {
                    activity.updateStatus("Connection lost")
                }
                cleanup()
            }
        }
    }

    /**
     * Process incoming JSON messages from PC.
     */
    private suspend fun processIncomingMessage(messageStr: String) {
        try {
            val message = JSONObject(messageStr)
            val command = message.getString("command")
            val timestamp = message.optDouble("timestamp", 0.0)
            val data = message.optJSONObject("data") ?: JSONObject()

            Log.d(TAG, "Received command: $command")

            when (command) {
                "registration_confirmed" -> handleRegistrationConfirmed(data)
                "heartbeat" -> handleHeartbeat(data)
                "heartbeat_response" -> handleHeartbeatResponse(data)
                "start_recording" -> handleStartRecording(data)
                "stop_recording" -> handleStopRecording(data)
                "get_status" -> handleGetStatus(data)
                "start_video_streaming" -> handleStartVideoStreaming(data)
                "stop_video_streaming" -> handleStopVideoStreaming(data)
                "sync_time" -> handleTimeSync(data)
                else -> {
                    Log.w(TAG, "Unknown command received: $command")
                }
            }

            // Update server time offset if timestamp is provided
            if (timestamp > 0) {
                val currentTime = System.currentTimeMillis() / 1000.0
                serverTimeOffset = ((timestamp - currentTime) * 1000).toLong()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing message: $messageStr", e)
        }
    }

    /**
     * Handle registration confirmation from PC.
     */
    private suspend fun handleRegistrationConfirmed(data: JSONObject) {
        val serverTime = data.optDouble("server_time", 0.0)
        val deviceRegistered = data.optBoolean("device_registered", false)

        if (deviceRegistered) {
            Log.d(TAG, "Device registration confirmed by server")
            withContext(Dispatchers.Main) {
                activity.updateStatus("Registered with PC")
            }
        }

        // Sync time with server
        if (serverTime > 0) {
            val currentTime = System.currentTimeMillis() / 1000.0
            serverTimeOffset = ((serverTime - currentTime) * 1000).toLong()
            Log.d(TAG, "Time synchronized with server, offset: ${serverTimeOffset}ms")
        }
    }

    /**
     * Handle heartbeat from PC.
     */
    private suspend fun handleHeartbeat(data: JSONObject) {
        lastHeartbeatTime = System.currentTimeMillis()

        // Send heartbeat response
        val response = JSONObject().apply {
            put("command", "heartbeat_response")
            put("device_id", deviceId)
            put("timestamp", getSynchronizedTime())
            put("data", JSONObject().apply {
                put("device_status", "connected")
                put("recording_status", if (activity.isRecording) "recording" else "idle")
                put("gsr_connected", activity.gsrConnected)
                put("thermal_connected", activity.thermalCameraConnected)
            })
        }

        sendMessage(response)
    }

    /**
     * Handle heartbeat response from PC.
     */
    private suspend fun handleHeartbeatResponse(data: JSONObject) {
        val serverTime = data.optDouble("server_time", 0.0)
        if (serverTime > 0) {
            val currentTime = System.currentTimeMillis() / 1000.0
            serverTimeOffset = ((serverTime - currentTime) * 1000).toLong()
        }
    }

    /**
     * Handle start recording command from PC.
     */
    private suspend fun handleStartRecording(data: JSONObject) {
        val sessionId = data.optString("session_id", "")
        val recordingParams = data.optJSONObject("recording_params") ?: JSONObject()

        withContext(Dispatchers.Main) {
            activity.handleCommand("start_recording:$sessionId")
        }

        // Send acknowledgment
        val response = JSONObject().apply {
            put("command", "recording_started")
            put("device_id", deviceId)
            put("timestamp", getSynchronizedTime())
            put("data", JSONObject().apply {
                put("session_id", sessionId)
                put("status", "started")
            })
        }

        sendMessage(response)
    }

    /**
     * Handle stop recording command from PC.
     */
    private suspend fun handleStopRecording(data: JSONObject) {
        val sessionId = data.optString("session_id", "")

        withContext(Dispatchers.Main) {
            activity.handleCommand("stop_recording:$sessionId")
        }

        // Send acknowledgment
        val response = JSONObject().apply {
            put("command", "recording_stopped")
            put("device_id", deviceId)
            put("timestamp", getSynchronizedTime())
            put("data", JSONObject().apply {
                put("session_id", sessionId)
                put("status", "stopped")
            })
        }

        sendMessage(response)
    }

    /**
     * Handle get status command from PC.
     */
    private suspend fun handleGetStatus(data: JSONObject) {
        val response = JSONObject().apply {
            put("command", "status_update")
            put("device_id", deviceId)
            put("timestamp", getSynchronizedTime())
            put("data", JSONObject().apply {
                put("device_status", "connected")
                put("recording_status", if (activity.isRecording) "recording" else "idle")
                put("gsr_connected", activity.gsrConnected)
                put("thermal_connected", activity.thermalCameraConnected)
                put("battery_level", getBatteryLevel())
                put("storage_available", isStorageAvailable())
                put("storage_free_gb", String.format("%.2f", getAvailableStorageGB()))
            })
        }

        sendMessage(response)
    }

    /**
     * Handle start video streaming command from PC.
     */
    private suspend fun handleStartVideoStreaming(data: JSONObject) {
        withContext(Dispatchers.Main) {
            activity.handleCommand("start_video_streaming")
        }
    }

    /**
     * Handle stop video streaming command from PC.
     */
    private suspend fun handleStopVideoStreaming(data: JSONObject) {
        withContext(Dispatchers.Main) {
            activity.handleCommand("stop_video_streaming")
        }
    }

    /**
     * Handle time synchronization command from PC.
     */
    private suspend fun handleTimeSync(data: JSONObject) {
        val serverTime = data.optDouble("server_time", 0.0)
        if (serverTime > 0) {
            val currentTime = System.currentTimeMillis() / 1000.0
            serverTimeOffset = ((serverTime - currentTime) * 1000).toLong()
            Log.d(TAG, "Time synchronized, offset: ${serverTimeOffset}ms")
        }
    }

    /**
     * Start heartbeat monitoring.
     */
    private fun startHeartbeat() {
        heartbeatJob = CoroutineScope(Dispatchers.IO).launch {
            while (isRunning.get() && isConnected.get()) {
                try {
                    val heartbeat = JSONObject().apply {
                        put("command", "heartbeat")
                        put("device_id", deviceId)
                        put("timestamp", getSynchronizedTime())
                        put("data", JSONObject().apply {
                            put("device_status", "connected")
                        })
                    }

                    sendMessage(heartbeat)
                    delay(HEARTBEAT_INTERVAL)

                } catch (e: Exception) {
                    Log.e(TAG, "Error sending heartbeat", e)
                    break
                }
            }
        }
    }

    /**
     * Send a JSON message to the PC.
     */
    suspend fun sendMessage(message: JSONObject) {
        try {
            if (isConnected.get() && outputStream != null) {
                val messageStr = message.toString()
                val messageBytes = messageStr.toByteArray(Charsets.UTF_8)

                outputStream?.write(messageBytes)
                outputStream?.flush()

                Log.d(TAG, "Sent message: ${message.getString("command")}")
            } else {
                Log.w(TAG, "Cannot send message - not connected")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message", e)
            cleanup()
        }
    }

    /**
     * Send data stream (GSR, thermal, etc.) to PC.
     */
    fun sendDataStream(dataType: String, data: JSONObject) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val message = JSONObject().apply {
                    put("command", "data_stream")
                    put("device_id", deviceId)
                    put("timestamp", getSynchronizedTime())
                    put("data", JSONObject().apply {
                        put("data_type", dataType)
                        put("payload", data)
                    })
                }

                sendMessage(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending data stream", e)
            }
        }
    }

    /**
     * Get synchronized time with server offset.
     */
    private fun getSynchronizedTime(): Double {
        return (System.currentTimeMillis() + serverTimeOffset) / 1000.0
    }

    /**
     * Get current battery level as percentage (0-100).
     */
    private fun getBatteryLevel(): Int {
        return try {
            val batteryIntent = activity.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100).toInt()
            } else {
                -1 // Unknown battery level
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting battery level", e)
            -1
        }
    }

    /**
     * Check if sufficient storage is available (at least 1GB free).
     */
    private fun isStorageAvailable(): Boolean {
        return try {
            val externalFilesDir = activity.getExternalFilesDir(null)
            if (externalFilesDir != null) {
                val stat = StatFs(externalFilesDir.path)
                val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
                val availableGB = availableBytes / (1024 * 1024 * 1024)
                availableGB >= 1 // At least 1GB free
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking storage availability", e)
            false
        }
    }

    /**
     * Get available storage in GB.
     */
    private fun getAvailableStorageGB(): Double {
        return try {
            val externalFilesDir = activity.getExternalFilesDir(null)
            if (externalFilesDir != null) {
                val stat = StatFs(externalFilesDir.path)
                val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
                availableBytes / (1024.0 * 1024.0 * 1024.0)
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting storage info", e)
            0.0
        }
    }

    /**
     * Check if connected to PC.
     */
    fun isConnected(): Boolean = isConnected.get()

    /**
     * Get device ID.
     */
    fun getDeviceId(): String = deviceId

    /**
     * Disconnect from PC and cleanup resources.
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from server")
        cleanup()
    }

    /**
     * Cleanup all resources and connections.
     */
    private fun cleanup() {
        isRunning.set(false)
        isConnected.set(false)

        // Cancel all coroutines
        connectionJob?.cancel()
        heartbeatJob?.cancel()
        listenerJob?.cancel()

        // Close streams and socket
        try {
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }

        outputStream = null
        inputStream = null
        socket = null

        Log.d(TAG, "Cleanup completed")
    }
}
