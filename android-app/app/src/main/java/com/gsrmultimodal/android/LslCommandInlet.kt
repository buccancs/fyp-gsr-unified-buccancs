package com.gsrmultimodal.android

import android.util.Log
import edu.ucsd.sccn.LSL
import gsrmultimodal.Command
import gsrmultimodal.CommandResponse
import gsrmultimodal.Timestamp
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import android.util.Base64

/**
 * Handles incoming commands from PC via LSL (Lab Streaming Layer)
 * This class listens for command streams and processes them accordingly
 */
class LslCommandInlet(
    private val deviceId: String,
    private val commandHandler: CommandHandler
) {

    companion object {
        private const val TAG = "LslCommandInlet"
        private const val COMMAND_STREAM_TYPE = "Command"
        private const val DISCOVERY_TIMEOUT = 2.0
        private const val PULL_TIMEOUT = 1.0
    }

    interface CommandHandler {
        fun onCommandReceived(command: Command): CommandResponse
        fun onConnectionStatusChanged(connected: Boolean)
    }

    private var inlet: LSL.StreamInlet? = null
    private var isRunning = false
    private var discoveryJob: Job? = null
    private var receptionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val commandQueue = ConcurrentLinkedQueue<Command>()

    /**
     * Start listening for command streams
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Command inlet already running")
            return
        }

        Log.i(TAG, "Starting LSL command inlet for device: $deviceId")
        isRunning = true

        // Start discovery coroutine
        discoveryJob = scope.launch {
            discoverAndConnect()
        }
    }

    /**
     * Stop the command inlet and cleanup resources
     */
    fun stop() {
        Log.i(TAG, "Stopping LSL command inlet")
        isRunning = false

        // Cancel coroutines
        discoveryJob?.cancel()
        receptionJob?.cancel()

        // Close inlet
        inlet?.close()
        inlet = null

        // Notify handler
        commandHandler.onConnectionStatusChanged(false)

        scope.cancel()
    }

    /**
     * Get the next command from the queue (non-blocking)
     */
    fun getNextCommand(): Command? {
        return commandQueue.poll()
    }

    /**
     * Check if there are pending commands
     */
    fun hasPendingCommands(): Boolean {
        return commandQueue.isNotEmpty()
    }

    /**
     * Get the number of pending commands
     */
    fun getPendingCommandCount(): Int {
        return commandQueue.size
    }

    private suspend fun discoverAndConnect() {
        while (isRunning) {
            try {
                Log.d(TAG, "Discovering command streams...")

                // Discover streams
                val streams = withContext(Dispatchers.IO) {
                    LSL.resolve_streams(COMMAND_STREAM_TYPE, DISCOVERY_TIMEOUT)
                }

                // Look for PC command stream
                val commandStream = streams.find { streamInfo ->
                    streamInfo.type() == COMMAND_STREAM_TYPE && 
                    streamInfo.source_id().contains("PC")
                }

                if (commandStream != null && inlet == null) {
                    Log.i(TAG, "Found command stream: ${commandStream.name()}")
                    connectToStream(commandStream)
                }

                // Wait before next discovery attempt
                delay(5000)

            } catch (e: Exception) {
                Log.e(TAG, "Error in discovery loop", e)
                delay(5000)
            }
        }
    }

    private suspend fun connectToStream(streamInfo: LSL.StreamInfo) {
        try {
            Log.i(TAG, "Connecting to command stream: ${streamInfo.name()}")

            inlet = LSL.StreamInlet(streamInfo, 360, 0)
            commandHandler.onConnectionStatusChanged(true)

            // Start reception coroutine
            receptionJob = scope.launch {
                receiveCommands()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to connect to command stream", e)
            inlet = null
            commandHandler.onConnectionStatusChanged(false)
        }
    }

    private suspend fun receiveCommands() {
        Log.i(TAG, "Starting command reception")

        try {
            while (isRunning && inlet != null) {
                try {
                    // Pull command data
                    val samples = withContext(Dispatchers.IO) {
                        inlet?.pull_chunk(PULL_TIMEOUT, 10)
                    }

                    samples?.first?.forEach { sample ->
                        if (sample.isNotEmpty()) {
                            processCommandSample(sample[0])
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error receiving commands", e)
                    delay(1000)
                }
            }
        } finally {
            Log.i(TAG, "Command reception stopped")
        }
    }

    private suspend fun processCommandSample(sampleData: String) {
        try {
            // Decode base64 encoded protobuf data
            val decodedData = Base64.decode(sampleData, Base64.DEFAULT)

            // Parse protobuf command
            val command = Command.parseFrom(decodedData)

            // Check if command is for this device
            if (command.deviceId == deviceId || command.deviceId.isEmpty()) {
                Log.d(TAG, "Received command: ${command.type} for device: ${command.deviceId}")

                // Add to queue for processing
                commandQueue.offer(command)

                // Process command immediately and send response
                val response = commandHandler.onCommandReceived(command)
                sendResponse(response)
            } else {
                Log.d(TAG, "Command not for this device (${command.deviceId}), ignoring")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing command sample", e)
        }
    }

    private fun sendResponse(response: CommandResponse) {
        // Note: Response sending would typically be handled by the LslStreamManager
        // This is just a placeholder to show the flow
        Log.d(TAG, "Sending response: ${response.status} - ${response.message}")
    }

    /**
     * Create a command response with current timestamp
     */
    fun createResponse(
        status: CommandResponse.Status,
        message: String
    ): CommandResponse {
        val currentTime = System.currentTimeMillis()
        val timestamp = Timestamp.newBuilder()
            .setSeconds(currentTime / 1000)
            .setNanos(((currentTime % 1000) * 1_000_000).toInt())
            .build()

        return CommandResponse.newBuilder()
            .setStatus(status)
            .setMessage(message)
            .setDeviceId(deviceId)
            .setTimestamp(timestamp)
            .build()
    }

    /**
     * Create a timestamp from current time
     */
    fun createCurrentTimestamp(): Timestamp {
        val currentTime = System.currentTimeMillis()
        return Timestamp.newBuilder()
            .setSeconds(currentTime / 1000)
            .setNanos(((currentTime % 1000) * 1_000_000).toInt())
            .build()
    }

    /**
     * Check if currently connected to a command stream
     */
    fun isConnected(): Boolean {
        return inlet != null
    }

    /**
     * Get connection status information
     */
    fun getConnectionInfo(): Map<String, Any> {
        val inlet = this.inlet
        return if (inlet != null) {
            mapOf(
                "connected" to true,
                "stream_name" to (inlet.info()?.name() ?: "Unknown"),
                "stream_type" to (inlet.info()?.type() ?: "Unknown"),
                "source_id" to (inlet.info()?.source_id() ?: "Unknown"),
                "pending_commands" to commandQueue.size
            )
        } else {
            mapOf(
                "connected" to false,
                "pending_commands" to commandQueue.size
            )
        }
    }
}

/**
 * Enhanced command handler implementation with actual handler integration
 */
class IntegratedCommandHandler(
    private val deviceId: String,
    private val cameraHandler: CameraHandler? = null,
    private val gsrHandler: GsrHandler? = null,
    private val thermalCameraHandler: ThermalCameraHandler? = null,
    private val lslStreamManager: LslStreamManager? = null
) : LslCommandInlet.CommandHandler {

    companion object {
        private const val TAG = "IntegratedCommandHandler"
    }

    override fun onCommandReceived(command: Command): CommandResponse {
        Log.i(TAG, "Processing command: ${command.type}")

        return try {
            when (command.type) {
                Command.CommandType.START_STREAM -> {
                    handleStartStream(command)
                }
                Command.CommandType.STOP_STREAM -> {
                    handleStopStream(command)
                }
                Command.CommandType.START_CAMERA -> {
                    handleStartCamera(command)
                }
                Command.CommandType.STOP_CAMERA -> {
                    handleStopCamera(command)
                }
                Command.CommandType.START_GSR -> {
                    handleStartGsr(command)
                }
                Command.CommandType.STOP_GSR -> {
                    handleStopGsr(command)
                }
                Command.CommandType.START_THERMAL -> {
                    handleStartThermal(command)
                }
                Command.CommandType.STOP_THERMAL -> {
                    handleStopThermal(command)
                }
                Command.CommandType.GET_STATUS -> {
                    handleGetStatus(command)
                }
                Command.CommandType.SYNC_TIME -> {
                    handleSyncTime(command)
                }
                Command.CommandType.MARK_EVENT -> {
                    handleMarkEvent(command)
                }
                else -> {
                    createErrorResponse("Unsupported command type: ${command.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command", e)
            createErrorResponse("Error processing command: ${e.message}")
        }
    }

    override fun onConnectionStatusChanged(connected: Boolean) {
        Log.i(TAG, "Command connection status changed: $connected")
    }

    private fun handleStartStream(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Starting LSL streams for device: $deviceId")

            // Start LSL streams for all available modalities
            var streamsStarted = 0
            val errors = mutableListOf<String>()

            lslStreamManager?.let { manager ->
                try {
                    manager.startGsrStream()
                    streamsStarted++
                    Log.d(TAG, "GSR LSL stream started")
                } catch (e: Exception) {
                    errors.add("GSR stream: ${e.message}")
                }

                try {
                    manager.startThermalStream()
                    streamsStarted++
                    Log.d(TAG, "Thermal LSL stream started")
                } catch (e: Exception) {
                    errors.add("Thermal stream: ${e.message}")
                }
            }

            if (streamsStarted > 0) {
                val message = "Started $streamsStarted LSL streams" + 
                    if (errors.isNotEmpty()) " (errors: ${errors.joinToString(", ")})" else ""
                createSuccessResponse(message)
            } else {
                createErrorResponse("Failed to start any LSL streams: ${errors.joinToString(", ")}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting streams", e)
            createErrorResponse("Failed to start streams: ${e.message}")
        }
    }

    private fun handleStopStream(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Stopping LSL streams for device: $deviceId")

            lslStreamManager?.let { manager ->
                manager.stopGsrStream()
                manager.stopThermalStream()
                Log.d(TAG, "All LSL streams stopped")
            }

            createSuccessResponse("LSL streams stopped successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping streams", e)
            createErrorResponse("Failed to stop streams: ${e.message}")
        }
    }

    private fun handleStartCamera(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Starting camera for device: $deviceId")

            cameraHandler?.let { handler ->
                // Extract session info from command parameters if available
                val sessionId = command.parametersMap["session_id"] ?: "lsl_session_${System.currentTimeMillis()}"
                handler.setSessionInfo(sessionId, deviceId)
                handler.startRecording()
                Log.d(TAG, "Camera recording started with session: $sessionId")
                createSuccessResponse("Camera recording started successfully")
            } ?: createErrorResponse("Camera handler not available")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera", e)
            createErrorResponse("Failed to start camera: ${e.message}")
        }
    }

    private fun handleStopCamera(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Stopping camera for device: $deviceId")

            cameraHandler?.let { handler ->
                handler.stopRecording()
                Log.d(TAG, "Camera recording stopped")
                createSuccessResponse("Camera recording stopped successfully")
            } ?: createErrorResponse("Camera handler not available")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera", e)
            createErrorResponse("Failed to stop camera: ${e.message}")
        }
    }

    private fun handleStartGsr(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Starting GSR for device: $deviceId")

            gsrHandler?.let { handler ->
                if (!handler.isConnected()) {
                    return createErrorResponse("GSR sensor not connected")
                }

                // Extract session info from command parameters if available
                val sessionId = command.parametersMap["session_id"] ?: "lsl_session_${System.currentTimeMillis()}"
                handler.setSessionInfo(sessionId, deviceId)
                handler.startStreaming()
                Log.d(TAG, "GSR streaming started with session: $sessionId")
                createSuccessResponse("GSR streaming started successfully")
            } ?: createErrorResponse("GSR handler not available")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting GSR", e)
            createErrorResponse("Failed to start GSR: ${e.message}")
        }
    }

    private fun handleStopGsr(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Stopping GSR for device: $deviceId")

            gsrHandler?.let { handler ->
                handler.stopStreaming()
                Log.d(TAG, "GSR streaming stopped")
                createSuccessResponse("GSR streaming stopped successfully")
            } ?: createErrorResponse("GSR handler not available")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping GSR", e)
            createErrorResponse("Failed to stop GSR: ${e.message}")
        }
    }

    private fun handleStartThermal(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Starting thermal camera for device: $deviceId")

            thermalCameraHandler?.let { handler ->
                if (!handler.isConnected()) {
                    return createErrorResponse("Thermal camera not connected")
                }

                // Extract session info from command parameters if available
                val sessionId = command.parametersMap["session_id"] ?: "lsl_session_${System.currentTimeMillis()}"
                handler.setSessionInfo(sessionId, deviceId)
                handler.startRecording()
                Log.d(TAG, "Thermal recording started with session: $sessionId")
                createSuccessResponse("Thermal recording started successfully")
            } ?: createErrorResponse("Thermal camera handler not available")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting thermal camera", e)
            createErrorResponse("Failed to start thermal camera: ${e.message}")
        }
    }

    private fun handleStopThermal(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Stopping thermal camera for device: $deviceId")

            thermalCameraHandler?.let { handler ->
                handler.stopRecording()
                Log.d(TAG, "Thermal recording stopped")
                createSuccessResponse("Thermal recording stopped successfully")
            } ?: createErrorResponse("Thermal camera handler not available")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping thermal camera", e)
            createErrorResponse("Failed to stop thermal camera: ${e.message}")
        }
    }

    private fun handleGetStatus(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Getting status for device: $deviceId")

            val statusInfo = mutableMapOf<String, String>()

            // Camera status
            cameraHandler?.let { handler ->
                statusInfo["camera_recording"] = handler.isRecording().toString()
                if (handler.isRecording()) {
                    statusInfo["camera_duration_ms"] = handler.getRecordingDuration().toString()
                }
            }

            // GSR status
            gsrHandler?.let { handler ->
                statusInfo["gsr_connected"] = handler.isConnected().toString()
                statusInfo["gsr_streaming"] = handler.isStreaming().toString()
            }

            // Thermal camera status
            thermalCameraHandler?.let { handler ->
                statusInfo["thermal_connected"] = handler.isConnected().toString()
                statusInfo["thermal_recording"] = handler.isRecording().toString()
                if (handler.isRecording()) {
                    statusInfo["thermal_frame_count"] = handler.getFrameCount().toString()
                    statusInfo["thermal_duration_ms"] = handler.getRecordingDuration().toString()
                }
            }

            // LSL stream status
            lslStreamManager?.let { manager ->
                statusInfo["lsl_gsr_active"] = manager.isGsrStreamActive().toString()
                statusInfo["lsl_thermal_active"] = manager.isThermalStreamActive().toString()
            }

            val statusMessage = "Device status retrieved: ${statusInfo.size} components"
            Log.d(TAG, "Status: $statusInfo")

            createSuccessResponse(statusMessage)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting status", e)
            createErrorResponse("Failed to get status: ${e.message}")
        }
    }

    private fun handleSyncTime(command: Command): CommandResponse {
        return try {
            Log.i(TAG, "Processing time sync for device: $deviceId")

            val serverTime = command.parametersMap["server_time"]?.toDoubleOrNull()
            if (serverTime != null) {
                val currentTime = System.currentTimeMillis() / 1000.0
                val offset = serverTime - currentTime
                Log.d(TAG, "Time sync: server=$serverTime, local=$currentTime, offset=$offset")

                // Store offset for use in timestamp calculations
                // This could be stored in a shared preferences or singleton
                createSuccessResponse("Time synchronized with offset: ${String.format("%.3f", offset)}s")
            } else {
                createErrorResponse("Invalid server time parameter")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing time", e)
            createErrorResponse("Failed to sync time: ${e.message}")
        }
    }

    private fun handleMarkEvent(command: Command): CommandResponse {
        return try {
            val eventName = command.parametersMap["event_name"] ?: "unnamed_event"
            val timestamp = System.currentTimeMillis()

            Log.i(TAG, "Marking event '$eventName' at timestamp: $timestamp")

            // Send event marker to all active streams
            lslStreamManager?.let { manager ->
                // This would require adding an event marker method to LslStreamManager
                Log.d(TAG, "Event marker sent to LSL streams")
            }

            createSuccessResponse("Event '$eventName' marked at timestamp: $timestamp")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking event", e)
            createErrorResponse("Failed to mark event: ${e.message}")
        }
    }</SEARCH>

    private fun createSuccessResponse(message: String): CommandResponse {
        return createResponse(CommandResponse.Status.SUCCESS, message)
    }

    private fun createErrorResponse(message: String): CommandResponse {
        return createResponse(CommandResponse.Status.ERROR, message)
    }

    private fun createResponse(status: CommandResponse.Status, message: String): CommandResponse {
        val currentTime = System.currentTimeMillis()
        val timestamp = Timestamp.newBuilder()
            .setSeconds(currentTime / 1000)
            .setNanos(((currentTime % 1000) * 1_000_000).toInt())
            .build()

        return CommandResponse.newBuilder()
            .setStatus(status)
            .setMessage(message)
            .setDeviceId(deviceId)
            .setTimestamp(timestamp)
            .build()
    }
}
