package com.buccancs.gsrcapture.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import com.buccancs.gsrcapture.utils.TimeManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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
        private const val SERVICE_TYPE = "_gsrcapture._tcp."
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
            
            // Send initial status
            sendStatus("CONNECTED")
            
            // Start time synchronization
            synchronizeTime()
            
            // Listen for commands
            networkExecutor.execute {
                try {
                    var line: String?
                    while (isConnected.get() && `in`?.readLine().also { line = it } != null) {
                        line?.let { handleCommand(it) }
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
     * Sends a status update to the client.
     * @param status Status to send
     */
    fun sendStatus(status: String) {
        if (!isConnected.get()) {
            return
        }
        
        try {
            val json = JSONObject()
            json.put("type", "STATUS")
            json.put("status", status)
            json.put("timestamp", System.currentTimeMillis())
            
            out?.println(json.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Error sending status", e)
        }
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
        
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            port = PORT
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
        
        // Shutdown executor
        networkExecutor.shutdown()
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
}