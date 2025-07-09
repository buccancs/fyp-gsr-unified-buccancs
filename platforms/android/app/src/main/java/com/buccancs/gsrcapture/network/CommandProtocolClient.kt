package com.buccancs.gsrcapture.network

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.util.Log
import com.buccancs.gsr.android.network.TcpClientTransport
import com.buccancs.gsr.common.network.CommandProtocol
import com.buccancs.gsr.common.network.NetworkTransport
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CommandProtocolClient integrates the Android app with the Java CommandProtocol networking layer.
 * This replaces the JSON-based NetworkClient to provide compatibility with the PC controller.
 */
class CommandProtocolClient(
    private val context: Context,
) : NetworkTransport.Listener {
    private val TAG = "CommandProtocolClient"

    // Network transport
    private var transport: TcpClientTransport? = null
    private val isRunning = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)

    // Device identification
    private val deviceId = generateDeviceId()

    // Callbacks
    private var commandCallback: ((String) -> Unit)? = null
    private var connectionStateCallback: ((Boolean) -> Unit)? = null
    private var errorCallback: ((String, Exception?) -> Unit)? = null

    // Executors
    private val executorService: ExecutorService = Executors.newCachedThreadPool()
    private val scheduledExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(2)

    // Main thread handler for UI callbacks
    private val mainHandler = Handler(Looper.getMainLooper())

    // Status reporting
    private var statusReportingTask: java.util.concurrent.ScheduledFuture<*>? = null

    /**
     * Starts the client and attempts to connect to the PC controller.
     */
    fun start(
        serverAddress: String = "192.168.1.100",
        serverPort: Int = 8080,
    ) {
        if (isRunning.get()) {
            Log.w(TAG, "Client already running")
            return
        }

        Log.d(TAG, "Starting CommandProtocol client...")
        isRunning.set(true)

        // Run network operations on background thread to avoid NetworkOnMainThreadException
        executorService.execute {
            try {
                // Create and initialize transport
                transport = TcpClientTransport(serverAddress, serverPort)
                transport?.initialize(deviceId, this)

                // Start transport (this performs blocking network operations)
                transport?.start()

                Log.d(TAG, "CommandProtocol client started successfully")
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start client", e)

                // Post UI callback to main thread
                mainHandler.post {
                    errorCallback?.invoke("Failed to start client: ${e.message}", e)
                }

                isRunning.set(false)
            }
        }
    }

    /**
     * Stops the client and closes all connections.
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }

        Log.d(TAG, "Stopping CommandProtocol client...")

        // Stop status reporting
        stopStatusReporting()

        // Stop transport
        transport?.stop()
        transport = null

        // Shutdown executors
        scheduledExecutor.shutdown()
        executorService.shutdown()

        // Update connection state
        if (isConnected.getAndSet(false)) {
            connectionStateCallback?.invoke(false)
        }

        Log.d(TAG, "CommandProtocol client stopped")
    }

    /**
     * Sends a command response back to the PC controller.
     */
    fun sendResponse(
        commandType: CommandProtocol.CommandType,
        status: CommandProtocol.StatusCode,
        message: String,
        vararg data: String,
    ): Boolean {
        val transport = this.transport
        if (transport == null || !isConnected.get()) {
            Log.w(TAG, "Cannot send response - not connected")
            return false
        }

        try {
            val response =
                CommandProtocol.ResponseMessage(
                    commandType,
                    deviceId,
                    status,
                    message,
                    *data,
                )

            return transport.sendMessage(response, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send response", e)
            errorCallback?.invoke("Failed to send response: ${e.message}", e)
            return false
        }
    }

    /**
     * Sends device status to the PC controller.
     */
    fun sendDeviceStatus(): Boolean {
        val batteryLevel = getBatteryLevel()
        val storageRemaining = getStorageRemaining()
        val activeStreams = getActiveStreams()

        return sendResponse(
            CommandProtocol.CommandType.CMD_STATUS,
            CommandProtocol.StatusCode.OK,
            "Device status",
            "battery:$batteryLevel",
            "storage:$storageRemaining",
            "streams:$activeStreams",
        )
    }

    /**
     * Starts periodic status reporting to the PC controller.
     */
    fun startStatusReporting(intervalSeconds: Long = 30) {
        stopStatusReporting()

        statusReportingTask =
            scheduledExecutor.scheduleAtFixedRate({
                if (isConnected.get()) {
                    sendDeviceStatus()
                }
            }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS)

        Log.d(TAG, "Started status reporting every $intervalSeconds seconds")
    }

    /**
     * Stops periodic status reporting.
     */
    fun stopStatusReporting() {
        statusReportingTask?.cancel(false)
        statusReportingTask = null
    }

    /**
     * Sets the command callback for handling incoming commands.
     */
    fun setCommandCallback(callback: (String) -> Unit) {
        this.commandCallback = callback
    }

    /**
     * Sets the connection state callback.
     */
    fun setConnectionStateCallback(callback: (Boolean) -> Unit) {
        this.connectionStateCallback = callback
    }

    /**
     * Sets the error callback.
     */
    fun setErrorCallback(callback: (String, Exception?) -> Unit) {
        this.errorCallback = callback
    }

    /**
     * Checks if the client is running.
     */
    val running: Boolean
        get() = isRunning.get()

    /**
     * Checks if the client is connected to the server.
     */
    val connected: Boolean
        get() = isConnected.get()

    // NetworkTransport.Listener implementation

    override fun onConnected(deviceId: String) {
        Log.d(TAG, "Connected to server: $deviceId")
        isConnected.set(true)

        // Post UI callback to main thread
        mainHandler.post {
            connectionStateCallback?.invoke(true)
        }

        // Start status reporting when connected
        startStatusReporting()
    }

    override fun onDisconnected(
        deviceId: String,
        reason: String,
    ) {
        Log.d(TAG, "Disconnected from server: $deviceId, reason: $reason")
        if (isConnected.getAndSet(false)) {
            // Post UI callback to main thread
            mainHandler.post {
                connectionStateCallback?.invoke(false)
            }
        }

        // Stop status reporting when disconnected
        stopStatusReporting()
    }

    override fun onMessageReceived(message: CommandProtocol.Message) {
        Log.d(TAG, "Received message: ${message.type} from ${message.deviceId}")

        when (message) {
            is CommandProtocol.CommandMessage -> handleCommandMessage(message)
            is CommandProtocol.ResponseMessage -> handleResponseMessage(message)
            is CommandProtocol.SyncMessage -> handleSyncMessage(message)
        }
    }

    override fun onError(
        deviceId: String,
        error: String,
        exception: Exception?,
    ) {
        Log.e(TAG, "Network error for device $deviceId: $error", exception)

        // Post UI callback to main thread
        mainHandler.post {
            errorCallback?.invoke("Network error: $error", exception)
        }
    }

    // Private helper methods

    private fun handleCommandMessage(message: CommandProtocol.CommandMessage) {
        val commandType = message.type.name
        Log.d(TAG, "Handling command: $commandType")

        // Send ACK first
        sendResponse(
            CommandProtocol.CommandType.ACK,
            CommandProtocol.StatusCode.OK,
            "Command received: $commandType",
        )

        // Forward to callback on main thread
        mainHandler.post {
            commandCallback?.invoke(commandType)
        }
    }

    private fun handleResponseMessage(message: CommandProtocol.ResponseMessage) {
        Log.d(TAG, "Received response: ${message.type} - ${message.message}")
        // Response messages are typically handled by the transport layer
    }

    private fun handleSyncMessage(message: CommandProtocol.SyncMessage) {
        Log.d(TAG, "Received sync message: ${message.type}")

        when (message.type) {
            CommandProtocol.CommandType.SYNC_PING -> {
                // Respond with SYNC_PONG
                try {
                    val pongMessage =
                        CommandProtocol.SyncMessage(
                            CommandProtocol.CommandType.SYNC_PONG,
                            deviceId,
                            message.originTimestamp,
                            System.currentTimeMillis(),
                        )
                    transport?.sendMessage(pongMessage, null)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send SYNC_PONG", e)
                }
            }
            else -> {
                Log.d(TAG, "Unhandled sync message type: ${message.type}")
            }
        }
    }

    private fun generateDeviceId(): String = "android_${android.os.Build.MODEL.replace(" ", "_")}_${System.currentTimeMillis() % 10000}"

    private fun getBatteryLevel(): String =
        try {
            val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

            if (level >= 0 && scale > 0) {
                val batteryPct = (level * 100 / scale)
                "$batteryPct%"
            } else {
                "Unknown"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting battery level", e)
            "Unknown"
        }

    private fun getStorageRemaining(): String =
        try {
            val externalStorageDir = Environment.getExternalStorageDirectory()
            val stat = StatFs(externalStorageDir.path)
            val availableBytes = stat.availableBytes
            val totalBytes = stat.totalBytes
            val usedBytes = totalBytes - availableBytes
            val usedPercentage = (usedBytes * 100 / totalBytes).toInt()

            "${100 - usedPercentage}% free"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting storage info", e)
            "Unknown"
        }

    private fun getActiveStreams(): String {
        // This would be populated based on actual recording state
        // For now, return a placeholder
        return "rgb:false,thermal:false,gsr:false,audio:false"
    }

    /**
     * Streams GSR sensor data to the connected PC controller.
     * @param gsrValue GSR value in microSiemens
     * @param timestamp Timestamp of the measurement
     */
    fun streamGsrData(
        gsrValue: Float,
        timestamp: Long,
    ): Boolean {
        val transport = this.transport
        if (transport == null || !isConnected.get()) {
            Log.w(TAG, "Cannot stream GSR data - not connected")
            return false
        }

        try {
            val message =
                CommandProtocol.CommandMessage(
                    CommandProtocol.CommandType.DATA_GSR,
                    deviceId,
                    "", // No session ID needed for data streaming
                    "value:$gsrValue",
                    "timestamp:$timestamp",
                    "unit:microSiemens",
                )

            return transport.sendMessage(message, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stream GSR data", e)
            errorCallback?.invoke("Failed to stream GSR data: ${e.message}", e)
            return false
        }
    }

    /**
     * Streams heart rate data to the connected PC controller.
     * @param heartRate Heart rate in BPM
     * @param timestamp Timestamp of the measurement
     */
    fun streamHeartRateData(
        heartRate: Int,
        timestamp: Long,
    ): Boolean {
        val transport = this.transport
        if (transport == null || !isConnected.get()) {
            Log.w(TAG, "Cannot stream heart rate data - not connected")
            return false
        }

        try {
            val message =
                CommandProtocol.CommandMessage(
                    CommandProtocol.CommandType.DATA_HEART_RATE,
                    deviceId,
                    "", // No session ID needed for data streaming
                    "value:$heartRate",
                    "timestamp:$timestamp",
                    "unit:BPM",
                )

            return transport.sendMessage(message, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stream heart rate data", e)
            errorCallback?.invoke("Failed to stream heart rate data: ${e.message}", e)
            return false
        }
    }

    /**
     * Sends video frame data to the connected PC controller.
     * @param frameData JPEG compressed frame data
     * @param frameType Type of frame (rgb, thermal)
     * @param timestamp Frame timestamp
     */
    fun sendVideoFrame(
        frameData: ByteArray,
        frameType: String = "thermal",
        timestamp: Long = System.currentTimeMillis(),
    ): Boolean {
        val transport = this.transport
        if (transport == null || !isConnected.get()) {
            Log.w(TAG, "Cannot send video frame - not connected")
            return false
        }

        try {
            val message =
                CommandProtocol.CommandMessage(
                    CommandProtocol.CommandType.DATA_VIDEO_FRAME,
                    deviceId,
                    "", // No session ID needed for data streaming
                    "frame_type:$frameType",
                    "timestamp:$timestamp",
                    "size:${frameData.size}",
                    "data:${android.util.Base64.encodeToString(frameData, android.util.Base64.DEFAULT)}",
                )

            return transport.sendMessage(message, null)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send video frame", e)
            errorCallback?.invoke("Failed to send video frame: ${e.message}", e)
            return false
        }
    }
}
