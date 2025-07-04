package com.buccancs.gsrcapture.network

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.BatteryManager
import android.os.StatFs
import android.os.Environment
import android.util.Log
import com.buccancs.gsrcapture.utils.TimeManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject

/**
 * Handles network communication with the Windows PC controller.
 * Implements the Network Remote Control Client feature.
 */
class NetworkClient(private val context: Context) {
    private val TAG = "NetworkClient"

    // Network components
    private val networkExecutor: ExecutorService = Executors.newCachedThreadPool()
    private val statusExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private var statusReportingTask: java.util.concurrent.ScheduledFuture<*>? = null
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    private var out: PrintWriter? = null
    private var `in`: BufferedReader? = null

    // Network service discovery
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null

    // Client state
    private val isRunning = AtomicBoolean(false)
    private val isConnected = AtomicBoolean(false)

    // Callbacks
    private var commandCallback: ((String) -> Unit)? = null
    private var connectionStateCallback: ((Boolean) -> Unit)? = null

    // Network constants
    companion object {
        private const val SERVICE_NAME = "GSRCapture"
        private const val SERVICE_TYPE = "_gsrcapture._tcp.local."
        private const val PORT = 5000
    }

    /**
     * Starts the network client.
     */
    fun start() {
        if (isRunning.getAndSet(true)) {
            Log.d(TAG, "Network client already running")
            return
        }

        Log.d(TAG, "Starting network client")

        // Start server socket
        startServer()

        // Register network service
        registerService()
    }

    /**
     * Starts the server socket to listen for incoming connections.
     */
    private fun startServer() {
        networkExecutor.execute {
            try {
                serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Server socket started on port $PORT")

                while (isRunning.get()) {
                    try {
                        // Wait for client connection
                        val socket = serverSocket?.accept()
                        if (socket != null) {
                            // Handle client connection
                            handleClientConnection(socket)
                        }
                    } catch (e: Exception) {
                        if (isRunning.get()) {
                            Log.e(TAG, "Error accepting client connection", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting server socket", e)
            }
        }
    }

    /**
     * Handles a client connection.
     * @param socket Client socket
     */
    private fun handleClientConnection(socket: Socket) {
        // Close existing connection
        closeConnection()

        clientSocket = socket

        try {
            out = PrintWriter(socket.getOutputStream(), true)
            `in` = BufferedReader(InputStreamReader(socket.getInputStream()))

            isConnected.set(true)
            connectionStateCallback?.invoke(true)

            Log.d(TAG, "Client connected: ${socket.inetAddress.hostAddress}")

            // Send initial device status
            sendDeviceStatus()

            // Start periodic status reporting
            startStatusReporting()

            // Start time synchronization
            synchronizeTime()

            // Listen for commands
            networkExecutor.execute {
                try {
                    while (isConnected.get()) {
                        val line = `in`?.readLine()
                        if (line != null) {
                            handleCommand(line)
                        } else {
                            break
                        }
                    }
                } catch (e: Exception) {
                    if (isConnected.get()) {
                        Log.e(TAG, "Error reading from client", e)
                        closeConnection()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up client connection", e)
            closeConnection()
        }
    }

    /**
     * Handles a command from the client.
     * @param commandJson JSON command string
     */
    private fun handleCommand(commandJson: String) {
        try {
            val json = JSONObject(commandJson)
            val command = json.getString("command")

            Log.d(TAG, "Received command: $command")

            // Handle time sync command
            if (command == "SYNC_TIME") {
                val remoteTime = json.getLong("timestamp")
                val roundTripStart = json.getLong("roundTripStart")
                val roundTripTime = System.currentTimeMillis() - roundTripStart

                // Synchronize time
                TimeManager.synchronizeWithNetwork(remoteTime, roundTripTime)

                // Send acknowledgment
                sendTimeSync(remoteTime, roundTripTime)
                return
            }

            // Handle file collection command
            if (command == "COLLECT_FILES") {
                handleFileCollection(json)
                return
            }

            // Forward command to callback
            commandCallback?.invoke(command)

            // Send acknowledgment
            sendAcknowledgment(command)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing command: $commandJson", e)
        }
    }

    /**
     * Sends an acknowledgment for a command.
     * @param command Command to acknowledge
     */
    private fun sendAcknowledgment(command: String) {
        try {
            val json = JSONObject()
            json.put("type", "ACK")
            json.put("command", command)
            json.put("timestamp", System.currentTimeMillis())

            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending acknowledgment", e)
        }
    }

    /**
     * Collects and sends device status to the PC controller.
     * Sends battery level, storage remaining, and active streams.
     */
    fun sendDeviceStatus() {
        if (!isConnected.get()) {
            return
        }

        try {
            val batteryLevel = getBatteryLevel()
            val storageRemaining = getStorageRemaining()
            val activeStreams = getActiveStreams()

            val json = JSONObject()
            json.put("type", "CMD_STATUS")
            json.put("deviceId", getDeviceId())
            json.put("timestamp", System.currentTimeMillis())
            json.put("batteryLevel", batteryLevel)
            json.put("storageRemaining", storageRemaining)
            json.put("activeStreams", activeStreams)

            out?.println(json.toString())
            Log.d(TAG, "Device status sent: Battery=$batteryLevel%, Storage=$storageRemaining, Streams=$activeStreams")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending device status", e)
        }
    }

    /**
     * Gets the current battery level as a percentage.
     */
    private fun getBatteryLevel(): String {
        return try {
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
    }

    /**
     * Gets the remaining storage space.
     */
    private fun getStorageRemaining(): String {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            val availableBytes = stat.availableBlocksLong * stat.blockSizeLong
            val availableGB = availableBytes / (1024 * 1024 * 1024)
            "${availableGB}GB"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting storage remaining", e)
            "Unknown"
        }
    }

    /**
     * Gets the status of active streams.
     */
    private fun getActiveStreams(): String {
        // This would be updated based on actual recording state
        // For now, return a placeholder format that matches what DeviceStatus expects
        return "gsr:false,rgb_video:false,thermal_video:false,audio:false"
    }

    /**
     * Gets the device ID.
     */
    private fun getDeviceId(): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
    }

    /**
     * Starts periodic device status reporting.
     */
    private fun startStatusReporting() {
        // Cancel any existing task
        stopStatusReporting()

        // Start new periodic status reporting
        statusReportingTask = statusExecutor.scheduleAtFixedRate({
            sendDeviceStatus()
        }, 5, 30, TimeUnit.SECONDS) // Send status every 30 seconds, starting after 5 seconds
    }

    /**
     * Stops periodic device status reporting.
     */
    private fun stopStatusReporting() {
        statusReportingTask?.cancel(false)
        statusReportingTask = null
    }

    /**
     * Sends time synchronization information to the client.
     * @param remoteTime Remote time in milliseconds
     * @param roundTripTime Round-trip time in milliseconds
     */
    private fun sendTimeSync(remoteTime: Long, roundTripTime: Long) {
        try {
            val json = JSONObject()
            json.put("type", "TIME_SYNC")
            json.put("remoteTime", remoteTime)
            json.put("localTime", System.currentTimeMillis())
            json.put("roundTripTime", roundTripTime)
            json.put("offset", TimeManager.getSynchronizedTimeMillis() - System.currentTimeMillis())

            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending time sync", e)
        }
    }

    /**
     * Initiates time synchronization with the client.
     */
    private fun synchronizeTime() {
        try {
            val json = JSONObject()
            json.put("type", "SYNC_REQUEST")
            json.put("timestamp", System.currentTimeMillis())

            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending sync request", e)
        }
    }

    /**
     * Registers the network service for discovery.
     */
    private fun registerService() {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

        // Generate unique device ID based on Android ID
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = PORT

            // Add service properties for device discovery
            setAttribute("id", deviceId)
            setAttribute("name", "${android.os.Build.MODEL}_$deviceId")
            setAttribute("type", "android")
            setAttribute("capabilities", "gsr,rgb_video,thermal_video,audio,raw_images")
            setAttribute("version", "1.0")
            setAttribute("status", "ready")
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${serviceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed: $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered: ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed: $errorCode")
            }
        }

        try {
            nsdManager?.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error registering service", e)
        }
    }

    /**
     * Unregisters the network service.
     */
    private fun unregisterService() {
        try {
            registrationListener?.let { listener ->
                nsdManager?.unregisterService(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering service", e)
        }

        registrationListener = null
        nsdManager = null
    }

    /**
     * Closes the current client connection.
     */
    private fun closeConnection() {
        if (isConnected.getAndSet(false)) {
            connectionStateCallback?.invoke(false)
        }

        // Stop periodic status reporting
        stopStatusReporting()

        try {
            out?.close()
            `in`?.close()
            clientSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing connection", e)
        }

        out = null
        `in` = null
        clientSocket = null
    }

    /**
     * Stops the network client.
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }

        Log.d(TAG, "Stopping network client")

        // Close connection
        closeConnection()

        // Unregister service
        unregisterService()

        // Close server socket
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing server socket", e)
        }

        serverSocket = null

        // Shutdown executors
        networkExecutor.shutdown()
        statusExecutor.shutdown()
    }

    /**
     * Sets a callback for receiving commands.
     * @param callback Function to call when a command is received
     */
    fun setCommandCallback(callback: (String) -> Unit) {
        commandCallback = callback
    }

    /**
     * Sets a callback for connection state changes.
     * @param callback Function to call when connection state changes
     */
    fun setConnectionStateCallback(callback: (Boolean) -> Unit) {
        connectionStateCallback = callback
    }

    /**
     * Streams GSR sensor data to the connected PC controller.
     * @param gsrValue GSR value in microSiemens
     * @param timestamp Timestamp of the measurement
     */
    fun streamGsrData(gsrValue: Float, timestamp: Long) {
        if (!isConnected.get()) {
            return
        }

        try {
            val json = JSONObject()
            json.put("type", "GSR_DATA")
            json.put("value", gsrValue)
            json.put("timestamp", timestamp)
            json.put("unit", "microSiemens")

            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming GSR data", e)
        }
    }

    /**
     * Streams heart rate data to the connected PC controller.
     * @param heartRate Heart rate in BPM
     * @param timestamp Timestamp of the measurement
     */
    fun streamHeartRateData(heartRate: Int, timestamp: Long) {
        if (!isConnected.get()) {
            return
        }

        try {
            val json = JSONObject()
            json.put("type", "HEART_RATE_DATA")
            json.put("value", heartRate)
            json.put("timestamp", timestamp)
            json.put("unit", "BPM")

            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming heart rate data", e)
        }
    }

    /**
     * Streams device status to the connected PC controller.
     * @param batteryLevel Battery level percentage
     * @param storageUsed Storage used percentage
     * @param isRecording Whether device is currently recording
     */
    fun streamDeviceStatus(batteryLevel: Int, storageUsed: Int, isRecording: Boolean) {
        if (!isConnected.get()) {
            return
        }

        try {
            val json = JSONObject()
            json.put("type", "DEVICE_STATUS")
            json.put("battery_level", batteryLevel)
            json.put("storage_used", storageUsed)
            json.put("is_recording", isRecording)
            json.put("timestamp", System.currentTimeMillis())

            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming device status", e)
        }
    }

    /**
     * Streams video frame metadata to the connected PC controller.
     * @param frameType Type of frame (rgb, thermal)
     * @param frameNumber Frame sequence number
     * @param timestamp Frame timestamp
     */
    fun streamVideoFrameMetadata(frameType: String, frameNumber: Long, timestamp: Long) {
        if (!isConnected.get()) {
            return
        }

        try {
            val json = JSONObject()
            json.put("type", "VIDEO_FRAME_METADATA")
            json.put("frame_type", frameType)
            json.put("frame_number", frameNumber)
            json.put("timestamp", timestamp)

            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error streaming video frame metadata", e)
        }
    }

    /**
     * Handles file collection request from PC controller.
     * @param commandJson JSON command containing collection parameters
     */
    private fun handleFileCollection(commandJson: JSONObject) {
        try {
            val sessionId = commandJson.optString("session_id", "unknown")
            Log.d(TAG, "Handling file collection for session: $sessionId")

            // Get the app's external files directory
            val appDir = context.getExternalFilesDir(null)
            if (appDir == null) {
                Log.e(TAG, "Cannot access external files directory")
                sendFileCollectionResponse(false, "Cannot access storage")
                return
            }

            // Find session directory
            val sessionDir = File(appDir, sessionId)
            if (!sessionDir.exists()) {
                Log.e(TAG, "Session directory not found: ${sessionDir.absolutePath}")
                sendFileCollectionResponse(false, "Session directory not found")
                return
            }

            // Get list of files to transfer
            val filesToTransfer = mutableListOf<File>()
            sessionDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    filesToTransfer.add(file)
                }
            }

            if (filesToTransfer.isEmpty()) {
                Log.w(TAG, "No files found in session directory")
                sendFileCollectionResponse(true, "No files to transfer")
                return
            }

            // Send file list first
            sendFileList(filesToTransfer)

            // Transfer each file
            var successCount = 0
            for (file in filesToTransfer) {
                if (transferFile(file)) {
                    successCount++
                } else {
                    Log.e(TAG, "Failed to transfer file: ${file.name}")
                }
            }

            val success = successCount == filesToTransfer.size
            val message = "Transferred $successCount of ${filesToTransfer.size} files"
            sendFileCollectionResponse(success, message)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling file collection", e)
            sendFileCollectionResponse(false, "Error: ${e.message}")
        }
    }

    /**
     * Sends the list of files that will be transferred.
     * @param files List of files to transfer
     */
    private fun sendFileList(files: List<File>) {
        try {
            val json = JSONObject()
            json.put("type", "FILE_LIST")
            json.put("count", files.size)

            val fileArray = org.json.JSONArray()
            files.forEach { file ->
                val fileInfo = JSONObject()
                fileInfo.put("name", file.name)
                fileInfo.put("size", file.length())
                fileInfo.put("modified", file.lastModified())
                fileArray.put(fileInfo)
            }
            json.put("files", fileArray)

            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file list", e)
        }
    }

    /**
     * Transfers a single file to the PC controller.
     * @param file File to transfer
     * @return True if transfer was successful
     */
    private fun transferFile(file: File): Boolean {
        try {
            Log.d(TAG, "Transferring file: ${file.name} (${file.length()} bytes)")

            // Send file header
            val headerJson = JSONObject()
            headerJson.put("type", "FILE_TRANSFER")
            headerJson.put("name", file.name)
            headerJson.put("size", file.length())
            headerJson.put("checksum", file.hashCode()) // Simple checksum
            out?.println(headerJson.toString())

            // Read and send file content in chunks
            val buffer = ByteArray(8192) // 8KB chunks
            file.inputStream().use { inputStream ->
                clientSocket?.getOutputStream()?.use { outputStream ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.flush()
                }
            }

            Log.d(TAG, "File transfer completed: ${file.name}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error transferring file: ${file.name}", e)
            return false
        }
    }

    /**
     * Sends file collection response to PC controller.
     * @param success Whether the collection was successful
     * @param message Status message
     */
    private fun sendFileCollectionResponse(success: Boolean, message: String) {
        try {
            val json = JSONObject()
            json.put("type", "FILE_COLLECTION_RESPONSE")
            json.put("success", success)
            json.put("message", message)
            json.put("timestamp", System.currentTimeMillis())

            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending file collection response", e)
        }
    }
}
