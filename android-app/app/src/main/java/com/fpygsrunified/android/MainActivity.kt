package com.fpygsrunified.android

// Shimmer SDK imports (when available)
// import com.shimmerresearch.android.Shimmer
// import com.shimmerresearch.android.manager.ShimmerBluetoothManagerAndroid
// import com.shimmerresearch.driver.Configuration
// import com.shimmerresearch.driver.ObjectCluster

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.fpygsrunified.android.camera.CameraHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var thermalImageView: ImageView
    private lateinit var gsrValueText: TextView
    private lateinit var statusText: TextView
    private lateinit var recordButton: Button
    private lateinit var stopButton: Button
    private lateinit var analyzeHandsButton: Button
    private lateinit var stopAnalysisButton: Button

    private lateinit var cameraHandler: CameraHandler
    private lateinit var thermalCameraHandler: ThermalCameraHandler
    private lateinit var gsrHandler: GsrHandler
    private lateinit var networkHandler: NetworkHandler
    private lateinit var handAnalysisHandler: HandAnalysisHandler

    private lateinit var cameraExecutor: ExecutorService

    // Video frame streaming variables
    private var isFrameStreamingEnabled = false
    private var lastFrameSentTime = 0L
    private val frameStreamingInterval = 1000L // 1 second = 1 FPS
    private var frameCounter = 0

    // GSR and thermal camera related variables
    private var gsrValue: Double = 0.0
    private var sessionStartTime: Long = 0

    // Device and recording state variables
    private var deviceId: String = "android_device_${System.currentTimeMillis()}"
    var isRecording = false
    var gsrConnected = false
    var thermalCameraConnected = false

    // Session management
    private var currentSessionId: String? = null

    // USB and Bluetooth managers
    private lateinit var usbManager: UsbManager
    private lateinit var bluetoothManager: BluetoothManager
    private var bluetoothAdapter: BluetoothAdapter? = null

    // Broadcast receivers
    private lateinit var usbReceiver: BroadcastReceiver
    private var bluetoothDiscoveryReceiver: BroadcastReceiver? = null

    // GSR recording variables
    private var isGSRRecording = false
    private var gsrRecordingThread: Thread? = null
    private val gsrDataBuffer = mutableListOf<GSRDataPoint>()
    private val gsrSamplingRate = 128 // Hz

    // Thermal recording variables
    private var isThermalRecording = false
    private var thermalRecordingThread: Thread? = null
    private val thermalFrameBuffer = mutableListOf<ThermalFrameData>()

    // Shimmer device variables
    private var shimmerDevice: Any? = null
    private var shimmerMacAddress: String? = null

    // Data classes
    data class GSRDataPoint(
        val timestamp: Long,
        val gsrValue: Double,
        val quality: Int
    )

    // Frame analyzer for video streaming


    companion object {
        private const val TAG = "fpygsrunified"
        private const val ACTION_USB_PERMISSION = "com.fpygsrunified.android.USB_PERMISSION"

        // Topdon TC001 USB identifiers (these may need to be adjusted based on actual device)
        private const val TOPDON_VENDOR_ID = 0x1234  // Replace with actual vendor ID
        private const val TOPDON_PRODUCT_ID = 0x5678  // Replace with actual product ID

        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()

        // Initialize USB manager
        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        // Initialize Bluetooth manager
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        // Initialize USB receiver
        usbReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        Log.d(TAG, "USB device attached")
                        updateStatus("USB device attached")
                    }

                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                        Log.d(TAG, "USB device detached")
                        updateStatus("USB device detached")
                    }

                    ACTION_USB_PERMISSION -> {
                        Log.d(TAG, "USB permission received")
                    }
                }
            }
        }

        // Register USB receiver
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(ACTION_USB_PERMISSION)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(usbReceiver, filter)
        }

        // Register Bluetooth discovery receiver
        setupBluetoothDiscoveryReceiver()

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        setupButtonListeners()

        // Initialize handlers with callbacks
        initializeHandlers()

        networkHandler = NetworkHandler(this)
        networkHandler.connect("192.168.1.100", 8080) // Replace with actual server IP and port
    }

    fun handleCommand(command: String) {
        try {
            Log.d(TAG, "Received command: $command")

            // Parse command and optional parameters
            val commandParts = command.split(":")
            val commandType = commandParts[0]
            val sessionId = if (commandParts.size > 1) commandParts[1] else ""

            when (commandType) {
                "start_recording" -> {
                    Log.d(TAG, "Starting recording for session: $sessionId")
                    runOnUiThread {
                        startRecording()
                        updateStatus("Recording started - Session: $sessionId")
                    }
                }

                "stop_recording" -> {
                    Log.d(TAG, "Stopping recording for session: $sessionId")
                    runOnUiThread {
                        stopRecording()
                        updateStatus("Recording stopped - Session: $sessionId")
                    }
                }

                "start_video_streaming" -> {
                    isFrameStreamingEnabled = true
                    Log.d(TAG, "Video streaming enabled")
                    runOnUiThread {
                        updateStatus("Video streaming started")
                    }
                }

                "stop_video_streaming" -> {
                    isFrameStreamingEnabled = false
                    Log.d(TAG, "Video streaming disabled")
                    runOnUiThread {
                        updateStatus("Video streaming stopped")
                    }
                }

                "get_status" -> {
                    Log.d(TAG, "Status requested")
                    // Status is handled automatically by NetworkHandler heartbeat
                }

                "get_device_info" -> {
                    Log.d(TAG, "Device info requested")
                    // Device info is sent during registration
                }

                "sync_time" -> {
                    Log.d(TAG, "Time sync requested")
                    // Time sync is handled automatically by NetworkHandler
                }

                "get_recorded_files" -> {
                    Log.d(TAG, "Recorded files list requested")
                    // This would trigger file list generation and sending
                }

                "start_file_transfer" -> {
                    Log.d(TAG, "File transfer requested")
                    // This would trigger file transfer process
                }

                "heartbeat" -> {
                    Log.d(TAG, "Heartbeat received")
                    // Heartbeat is handled automatically by NetworkHandler
                }

                else -> {
                    Log.w(TAG, "Unknown command: $commandType")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling command: $command", e)
        }
    }


    private fun initializeViews() {
        previewView = findViewById(R.id.previewView)
        thermalImageView = findViewById(R.id.thermalImageView)
        gsrValueText = findViewById(R.id.gsrValueText)
        statusText = findViewById(R.id.statusText)
        recordButton = findViewById(R.id.recordButton)
        stopButton = findViewById(R.id.stopButton)
        // Note: analyzeHandsButton and stopAnalysisButton are not in the layout
        // analyzeHandsButton = findViewById(R.id.analyzeHandsButton)
        // stopAnalysisButton = findViewById(R.id.stopAnalysisButton)

        statusText.text = "Initializing..."

        // Initially disable analysis buttons until recording is complete
        // analyzeHandsButton.isEnabled = false
        // stopAnalysisButton.isEnabled = false
    }

    private fun setupButtonListeners() {
        recordButton.setOnClickListener {
            startRecording()
        }

        stopButton.setOnClickListener {
            stopRecording()
        }

        // Note: analyzeHandsButton and stopAnalysisButton are not in the layout
        // analyzeHandsButton.setOnClickListener {
        //     startHandAnalysis()
        // }

        // stopAnalysisButton.setOnClickListener {
        //     stopHandAnalysis()
        // }
    }

    private fun initializeHandlers() {
        // Initialize camera handler
        cameraHandler = CameraHandler(this, previewView, this)
        cameraHandler.setRecordingCallback(object : CameraHandler.RecordingCallback {
            override fun onRecordingStarted(filePath: String) {
                runOnUiThread {
                    updateStatus("RGB video recording started: $filePath")
                }
            }

            override fun onRecordingStopped(filePath: String) {
                runOnUiThread {
                    updateStatus("RGB video recording stopped: $filePath")
                }
            }

            override fun onRecordingError(error: String) {
                runOnUiThread {
                    updateStatus("RGB video recording error: $error")
                }
            }
        })

        // Set raw frame callback for RGB frame capture
        cameraHandler.setRawFrameCallback(object : CameraHandler.RawFrameCallback {
            override fun onFrameCaptureStarted() {
                runOnUiThread {
                    updateStatus("RGB frame capture started")
                }
            }

            override fun onFrameCaptured(bitmap: Bitmap, timestamp: Long, frameNumber: Int) {
                // Raw RGB frame processing can be added here if needed
                // For now, just log frame reception for debugging
                if (frameNumber % 30 == 0) { // Log every 30th frame to avoid spam
                    Log.d(
                        "MainActivity",
                        "RGB frame captured: ${bitmap.width}x${bitmap.height}, frame #${frameNumber}"
                    )
                }
            }

            override fun onFrameCaptureStopped() {
                runOnUiThread {
                    updateStatus("RGB frame capture stopped")
                }
            }

            override fun onFrameCaptureError(error: String) {
                runOnUiThread {
                    updateStatus("RGB frame capture error: $error")
                }
            }
        })

        // Initialize GSR handler
        gsrHandler = GsrHandler(this, gsrValueText)
        gsrHandler.setGsrCallback(object : GsrHandler.GsrDataCallback {
            override fun onGsrDataReceived(conductance: Double, resistance: Double, timestamp: Long) {
                // GSR data is already handled by the handler's UI update
            }

            override fun onConnectionStateChanged(connected: Boolean, deviceAddress: String?) {
                gsrConnected = connected
                runOnUiThread {
                    if (connected) {
                        updateStatus("GSR sensor connected: $deviceAddress")
                    } else {
                        updateStatus("GSR sensor disconnected")
                    }
                }
            }

            override fun onStreamingStateChanged(streaming: Boolean) {
                runOnUiThread {
                    updateStatus("GSR streaming: ${if (streaming) "started" else "stopped"}")
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    updateStatus("GSR error: $error")
                }
            }
        })
        gsrHandler.initialize()

        // Initialize thermal camera handler
        thermalCameraHandler = ThermalCameraHandler(this, thermalImageView)
        thermalCameraHandler.setThermalCallback(object : ThermalCameraHandler.ThermalDataCallback {
            override fun onThermalFrameReceived(
                frameData: ByteArray,
                width: Int,
                height: Int,
                timestamp: Long,
                frameNumber: Int
            ) {
                // Thermal frame processing can be added here if needed
            }

            override fun onConnectionStateChanged(connected: Boolean, deviceInfo: String?) {
                thermalCameraConnected = connected
                runOnUiThread {
                    if (connected) {
                        updateStatus("Thermal camera connected: $deviceInfo")
                    } else {
                        updateStatus("Thermal camera disconnected")
                    }
                }
            }

            override fun onRecordingStateChanged(recording: Boolean) {
                runOnUiThread {
                    updateStatus("Thermal recording: ${if (recording) "started" else "stopped"}")
                }
            }

            override fun onError(error: String) {
                runOnUiThread {
                    updateStatus("Thermal camera error: $error")
                }
            }
        })
        thermalCameraHandler.initialize()

        // Initialize hand analysis handler
        handAnalysisHandler = HandAnalysisHandler(this)
        handAnalysisHandler.setAnalysisCallback(object : HandAnalysisHandler.HandAnalysisCallback {
            override fun onAnalysisStarted(videoPath: String, timestamp: Long) {
                runOnUiThread {
                    updateStatus("Hand analysis started on: $videoPath")
                    analyzeHandsButton.isEnabled = false
                    stopAnalysisButton.isEnabled = true
                }
            }

            override fun onAnalysisProgress(progress: Float, frameNumber: Int, totalFrames: Int) {
                runOnUiThread {
                    val progressPercent = (progress * 100).toInt()
                    updateStatus("Hand analysis progress: $progressPercent% (frame $frameNumber/$totalFrames)")
                }
            }

            override fun onHandDetected(
                frameNumber: Int,
                timestamp: Long,
                handLandmarks: List<HandAnalysisHandler.HandLandmark>
            ) {
                runOnUiThread {
                    updateStatus("Hand detected in frame $frameNumber with ${handLandmarks.size} landmarks")
                }
            }

            override fun onPoseDetected(frameNumber: Int, timestamp: Long, pose: HandAnalysisHandler.PoseData) {
                runOnUiThread {
                    updateStatus("Pose detected in frame $frameNumber with confidence ${pose.confidence}")
                }
            }

            override fun onAnalysisCompleted(videoPath: String, resultsPath: String, timestamp: Long) {
                runOnUiThread {
                    updateStatus("Hand analysis completed. Results saved to: $resultsPath")
                    analyzeHandsButton.isEnabled = true
                    stopAnalysisButton.isEnabled = false
                }
            }

            override fun onAnalysisError(error: String, timestamp: Long) {
                runOnUiThread {
                    updateStatus("Hand analysis error: $error")
                    analyzeHandsButton.isEnabled = true
                    stopAnalysisButton.isEnabled = false
                }
            }
        })
        handAnalysisHandler.initialize()
    }

    private fun startRecording() {
        if (isRecording) {
            updateStatus("Recording already in progress")
            return
        }

        try {
            // Generate session ID
            currentSessionId = "session_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
            sessionStartTime = System.currentTimeMillis()

            // Set session info for all handlers
            val outputDir = File(getExternalFilesDir(null), currentSessionId!!)
            cameraHandler.setSessionInfo(currentSessionId!!, outputDir)
            gsrHandler.setSessionInfo(currentSessionId!!, deviceId)
            thermalCameraHandler.setSessionInfo(currentSessionId!!, deviceId)

            // Start frame capture for raw RGB frames
            cameraHandler.startFrameCapture()

            // Trigger sync marker at recording start
            cameraHandler.addTimestampMarker("RECORDING_START")

            // Wait a moment then trigger visual sync marker
            Handler(Looper.getMainLooper()).postDelayed({
                // Note: triggerCombinedSyncMarker method doesn't exist in CameraHandler
                // cameraHandler.triggerCombinedSyncMarker(200) // 200ms flash duration
            }, 500) // 500ms delay to ensure all systems are recording

            // Start recording on all handlers
            cameraHandler.startRecording()

            if (gsrConnected) {
                gsrHandler.startStreaming()
            }

            if (thermalCameraConnected) {
                thermalCameraHandler.startRecording()
            }

            isRecording = true
            updateStatus("Recording started - Session: $currentSessionId")
            updateStatus("RGB frame capture and sync markers enabled")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            updateStatus("Error starting recording: ${e.message}")
        }
    }

    private fun stopRecording() {
        if (!isRecording) {
            updateStatus("No recording in progress")
            return
        }

        try {
            // Trigger final sync marker before stopping
            cameraHandler.addTimestampMarker("RECORDING_STOP")
            // Note: triggerCombinedSyncMarker method doesn't exist in CameraHandler
            // cameraHandler.triggerCombinedSyncMarker(200) // 200ms flash duration

            // Wait a moment for sync marker to complete, then stop recording
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    // Stop recording on all handlers
                    cameraHandler.stopRecording()
                    gsrHandler.stopStreaming()
                    thermalCameraHandler.stopRecording()

                    // Stop frame capture
                    cameraHandler.stopFrameCapture()

                    isRecording = false
                    val sessionId = currentSessionId
                    currentSessionId = null

                    updateStatus("Recording stopped - Session: $sessionId")
                    updateStatus("RGB frame capture stopped")

                    // Enable hand analysis button after recording is complete
                    analyzeHandsButton.isEnabled = true

                } catch (e: Exception) {
                    Log.e(TAG, "Error in delayed stop recording", e)
                    updateStatus("Error stopping recording: ${e.message}")
                }
            }, 300) // 300ms delay to allow sync marker to complete

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            updateStatus("Error stopping recording: ${e.message}")
        }
    }

    private fun startHandAnalysis() {
        try {
            // Find the most recent video file for analysis
            val videoPath = findMostRecentVideoFile()
            if (videoPath != null) {
                // Set session info for hand analysis
                val sessionId =
                    currentSessionId ?: "session_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
                handAnalysisHandler.setSessionInfo(sessionId, deviceId)

                // Start analysis
                handAnalysisHandler.analyzeVideoFile(videoPath)
                updateStatus("Starting hand analysis on: $videoPath")
            } else {
                updateStatus("No video file found for hand analysis. Please record a video first.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting hand analysis", e)
            updateStatus("Error starting hand analysis: ${e.message}")
        }
    }

    private fun stopHandAnalysis() {
        try {
            handAnalysisHandler.stopAnalysis()
            updateStatus("Hand analysis stopped")
            analyzeHandsButton.isEnabled = true
            stopAnalysisButton.isEnabled = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping hand analysis", e)
            updateStatus("Error stopping hand analysis: ${e.message}")
        }
    }

    private fun findMostRecentVideoFile(): String? {
        try {
            val externalFilesDir = getExternalFilesDir(null)
            if (externalFilesDir != null && externalFilesDir.exists()) {
                val videoFiles = externalFilesDir.listFiles { file ->
                    file.name.endsWith(".mp4") && file.name.contains("rgb_video")
                }

                if (videoFiles != null && videoFiles.isNotEmpty()) {
                    // Sort by last modified time and return the most recent
                    val mostRecent = videoFiles.maxByOrNull { it.lastModified() }
                    return mostRecent?.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding video file", e)
        }
        return null
    }

    private fun updateStatus(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val statusMessage = "[$timestamp] $message"
        statusText.append("$statusMessage\n")
        Log.d(TAG, statusMessage)
    }

    private fun setupBluetoothDiscoveryReceiver() {
        bluetoothDiscoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            Log.d(TAG, "Bluetooth device found: ${it.name} - ${it.address}")
                            updateStatus("Bluetooth device found: ${it.name}")
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        Log.d(TAG, "Bluetooth discovery started")
                        updateStatus("Bluetooth discovery started")
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Log.d(TAG, "Bluetooth discovery finished")
                        updateStatus("Bluetooth discovery finished")
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothDiscoveryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(bluetoothDiscoveryReceiver, filter)
        }
    }

    private fun startCamera() {
        // Camera handler is now initialized in initializeHandlers()
        // Just start the camera preview
        if (::cameraHandler.isInitialized) {
            CoroutineScope(Dispatchers.Main).launch {
                cameraHandler.initialize()
            }
        }
    }


    private fun startShimmerDataStreaming() {
        if (!gsrConnected) return

        Thread {
            Log.d(TAG, "Starting Shimmer GSR data streaming")
            var sampleCount = 0

            while (gsrConnected) {
                try {
                    // TODO: Replace with actual Shimmer SDK data reading
                    // Example: 
                    // val gsrData = (shimmerDevice as Shimmer).getLatestReceivedData()
                    // val gsrValue = gsrData.getFormatClusterValue(Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE)

                    // Enhanced simulation with more realistic GSR patterns
                    val timestamp = System.currentTimeMillis()
                    val simulatedGSR = generateRealisticGSRValue(timestamp)

                    val dataPoint = GSRDataPoint(timestamp, simulatedGSR, 100) // 100% quality

                    // Update current GSR value
                    gsrValue = simulatedGSR

                    // Add to buffer
                    synchronized(gsrDataBuffer) {
                        gsrDataBuffer.add(dataPoint)

                        // Keep buffer size manageable (last 1000 samples ~8 seconds at 128Hz)
                        if (gsrDataBuffer.size > 1000) {
                            gsrDataBuffer.removeAt(0)
                        }
                    }

                    // Send data to PC if connected
                    sendGSRDataToPC(dataPoint)

                    // Update UI every 10 samples to reduce overhead
                    if (sampleCount % 10 == 0) {
                        runOnUiThread {
                            gsrValueText.text = String.format("GSR: %.2f µS", simulatedGSR)
                        }
                    }

                    sampleCount++

                    // Sleep to maintain sampling rate (128 Hz = ~7.8ms per sample)
                    Thread.sleep(8)

                } catch (e: Exception) {
                    Log.e(TAG, "Error in Shimmer GSR data streaming", e)
                    break
                }
            }

            Log.d(TAG, "Shimmer GSR data streaming stopped")
        }.start()
    }

    private fun generateRealisticGSRValue(timestamp: Long): Double {
        // More realistic GSR simulation with physiological patterns
        val baseline = 15.0 // microsiemens baseline
        val timeSeconds = (timestamp / 1000.0) % 3600 // Hour cycle

        // Slow drift (breathing, temperature changes)
        val slowDrift = Math.sin(timeSeconds * 0.1) * 2.0

        // Medium frequency variations (heart rate influence)
        val heartRateInfluence = Math.sin(timeSeconds * 1.2) * 0.5

        // High frequency noise
        val noise = (Math.random() - 0.5) * 0.8

        // Occasional skin conductance responses (SCRs)
        val scrProbability = 0.002 // 0.2% chance per sample
        val scr = if (Math.random() < scrProbability) {
            Math.random() * 8.0 // SCR amplitude 0-8 µS
        } else 0.0

        return Math.max(0.1, baseline + slowDrift + heartRateInfluence + noise + scr)
    }

    private fun sendGSRDataToPC(dataPoint: GSRDataPoint) {
        // Send GSR data to PC using NetworkHandler
        if (::networkHandler.isInitialized && networkHandler.isConnected()) {
            try {
                val gsrData = JSONObject().apply {
                    put("timestamp", dataPoint.timestamp)
                    put("gsr_value", dataPoint.gsrValue)
                    put("quality", dataPoint.quality)
                    put("sampling_rate", 128)
                    put("unit", "microsiemens")
                }

                networkHandler.sendDataStream("gsr", gsrData)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send GSR data to PC", e)
            }
        }
    }

    private fun sendFrameToPC(imageProxy: ImageProxy) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Send video frame data to PC using NetworkHandler
                if (::networkHandler.isInitialized && networkHandler.isConnected()) {
                    // Convert ImageProxy to Bitmap
                    val bitmap = imageProxyToBitmap(imageProxy)
                    if (bitmap != null) {
                        // Compress to JPEG
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream) // 50% quality for smaller size
                        val jpegBytes = stream.toByteArray()

                        // Create frame metadata
                        val frameData = JSONObject().apply {
                            put("timestamp", System.currentTimeMillis())
                            put("frame_number", frameCounter)
                            put("frame_size", jpegBytes.size)
                            put("format", "jpeg")
                            put("quality", 50)
                            put("resolution", "${bitmap.width}x${bitmap.height}")
                        }

                        networkHandler.sendDataStream("video_frame", frameData)
                        frameCounter++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing frame: ${e.message}")
            }
        }
    }

    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        return try {
            val buffer: ByteBuffer = imageProxy.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting ImageProxy to Bitmap: ${e.message}")
            null
        }
    }

    private fun onShimmerDataReceived(data: Any) {
        // TODO: Implement actual Shimmer data parsing
        // This method would be called by the Shimmer SDK when new data arrives

        try {
            // Example data parsing (adjust based on actual Shimmer SDK):
            // val gsrReading = data.getGSRValue() // in microsiemens
            // val timestamp = data.getTimestamp()
            // val quality = data.getSignalQuality()

            // For now, this is handled by the simulation

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Shimmer data", e)
        }
    }


    private fun startThermalFrameCapture() {
        if (!thermalCameraConnected) return

        // Start a background thread to capture thermal frames
        Thread {
            Log.d(TAG, "Starting thermal frame capture thread")
            var frameCount = 0

            while (thermalCameraConnected) {
                try {
                    // TODO: Replace with actual Topdon SDK frame capture when available
                    // Example based on Topdon TC001 SDK:
                    // val thermalFrame = topdonCamera.captureFrame()
                    // val temperatureMatrix = thermalFrame.getTemperatureMatrix()
                    // val thermalBitmap = thermalFrame.getThermalBitmap()
                    // val minTemp = thermalFrame.getMinTemperature()
                    // val maxTemp = thermalFrame.getMaxTemperature()

                    // Enhanced thermal simulation with realistic temperature data
                    val thermalData = generateRealisticThermalFrame(frameCount)
                    val thermalBitmap = createThermalBitmapFromData(thermalData)

                    // Update UI with thermal image
                    runOnUiThread {
                        thermalImageView.setImageBitmap(thermalBitmap)
                    }

                    // Send thermal data to PC if connected
                    sendThermalDataToPC(thermalData, frameCount)

                    frameCount++

                    // Capture at ~25 FPS (40ms interval)
                    Thread.sleep(40)

                } catch (e: Exception) {
                    Log.e(TAG, "Error capturing thermal frame", e)
                    break
                }
            }

            Log.d(TAG, "Thermal frame capture thread stopped")
        }.start()
    }

    private fun generateRealisticThermalFrame(frameCount: Int): ThermalFrameData {
        // Generate realistic thermal data (256x192 resolution for TC001)
        val width = 256
        val height = 192
        val temperatureMatrix = Array(height) { FloatArray(width) }

        val time = frameCount * 0.04 // Time in seconds (25 FPS)

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Base temperature around room temperature
                var temp = 22.0f + (Math.random() * 2.0f - 1.0f).toFloat() // 21-23°C base

                // Add some spatial patterns (simulate heat sources)
                val centerX = width / 2
                val centerY = height / 2
                val distanceFromCenter =
                    Math.sqrt(((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toDouble())

                // Simulate a warm object in the center
                if (distanceFromCenter < 30) {
                    temp += (30 - distanceFromCenter).toFloat() * 0.3f
                }

                // Add temporal variation
                temp += Math.sin(time * 0.5 + x * 0.1 + y * 0.1).toFloat() * 0.5f

                temperatureMatrix[y][x] = temp
            }
        }

        return ThermalFrameData(
            temperatureMatrix = temperatureMatrix,
            width = width,
            height = height,
            minTemp = temperatureMatrix.flatMap { it.toList() }.minOrNull() ?: 20.0f,
            maxTemp = temperatureMatrix.flatMap { it.toList() }.maxOrNull() ?: 30.0f,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createThermalBitmapFromData(thermalData: ThermalFrameData): Bitmap {
        val bitmap = Bitmap.createBitmap(thermalData.width, thermalData.height, Bitmap.Config.ARGB_8888)

        val tempRange = thermalData.maxTemp - thermalData.minTemp

        for (y in 0 until thermalData.height) {
            for (x in 0 until thermalData.width) {
                val temp = thermalData.temperatureMatrix[y][x]
                val normalizedTemp = if (tempRange > 0) (temp - thermalData.minTemp) / tempRange else 0.5f

                // Convert temperature to color (ironbow palette simulation)
                val color = temperatureToColor(normalizedTemp)
                bitmap.setPixel(x, y, color)
            }
        }

        return bitmap
    }

    private fun temperatureToColor(normalizedTemp: Float): Int {
        // Simple ironbow color palette implementation
        val temp = Math.max(0.0f, Math.min(1.0f, normalizedTemp))

        return when {
            temp < 0.25f -> {
                val t = temp * 4
                android.graphics.Color.rgb((t * 255).toInt(), 0, (255 * (1 - t)).toInt())
            }

            temp < 0.5f -> {
                val t = (temp - 0.25f) * 4
                android.graphics.Color.rgb(255, (t * 255).toInt(), 0)
            }

            temp < 0.75f -> {
                val t = (temp - 0.5f) * 4
                android.graphics.Color.rgb(255, 255, (t * 255).toInt())
            }

            else -> {
                val t = (temp - 0.75f) * 4
                android.graphics.Color.rgb(255, 255, 255)
            }
        }
    }

    private fun sendThermalDataToPC(thermalData: ThermalFrameData, frameNumber: Int) {
        // Send thermal data to PC using NetworkHandler
        if (::networkHandler.isInitialized && networkHandler.isConnected()) {
            try {
                val thermalMessage = JSONObject().apply {
                    put("timestamp", thermalData.timestamp)
                    put("frame_number", frameNumber)
                    put("min_temp", thermalData.minTemp)
                    put("max_temp", thermalData.maxTemp)
                    put("width", thermalData.width)
                    put("height", thermalData.height)
                    put("camera_model", "Topdon TC001")
                    put("resolution", "${thermalData.width}x${thermalData.height}")
                    put("temperature_unit", "celsius")
                }

                networkHandler.sendDataStream("thermal", thermalMessage)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send thermal data to PC", e)
            }
        }
    }

    // Data class for thermal frame information
    data class ThermalFrameData(
        val temperatureMatrix: Array<FloatArray>,
        val width: Int,
        val height: Int,
        val minTemp: Float,
        val maxTemp: Float,
        val timestamp: Long
    )


    private fun startGSRRecording() {
        if (!gsrConnected) {
            Log.w(TAG, "Cannot start GSR recording - sensor not connected")
            return
        }

        isGSRRecording = true
        Log.d(TAG, "Starting GSR recording")

        // Start GSR recording thread
        gsrRecordingThread = Thread {
            val sessionDir = createGSRSessionDirectory()
            val csvFile = File(sessionDir, "gsr_data.csv")
            var recordedSamples = 0

            try {
                csvFile.bufferedWriter().use { writer ->
                    // Write CSV header
                    writer.write("timestamp,gsr_value_us,quality,session_time_ms\n")

                    val recordingStartTime = System.currentTimeMillis()

                    while (isGSRRecording && gsrConnected) {
                        try {
                            // Get recent GSR data from buffer
                            val dataToRecord = mutableListOf<GSRDataPoint>()
                            synchronized(gsrDataBuffer) {
                                // Copy new data points that haven't been recorded yet
                                if (gsrDataBuffer.isNotEmpty()) {
                                    dataToRecord.addAll(gsrDataBuffer.takeLast(10)) // Take last 10 samples
                                }
                            }

                            // Write data points to CSV
                            for (dataPoint in dataToRecord) {
                                val sessionTime = dataPoint.timestamp - recordingStartTime
                                writer.write("${dataPoint.timestamp},${dataPoint.gsrValue},${dataPoint.quality},$sessionTime\n")
                                recordedSamples++
                            }

                            // Flush data periodically
                            if (recordedSamples % 100 == 0) {
                                writer.flush()
                            }

                            // Sleep briefly to avoid overwhelming the file system
                            Thread.sleep(50)

                        } catch (e: Exception) {
                            Log.e(TAG, "Error writing GSR data", e)
                            break
                        }
                    }

                    writer.flush()
                }

                Log.d(TAG, "GSR recording completed. Recorded $recordedSamples samples to ${csvFile.absolutePath}")

            } catch (e: Exception) {
                Log.e(TAG, "Error during GSR recording", e)
            }
        }

        gsrRecordingThread?.start()
    }

    private fun stopGSRRecording() {
        Log.d(TAG, "Stopping GSR recording")

        isGSRRecording = false

        // Wait for recording thread to finish
        gsrRecordingThread?.let { thread ->
            try {
                thread.join(2000) // Wait up to 2 seconds
            } catch (e: InterruptedException) {
                Log.w(TAG, "Interrupted while waiting for GSR recording thread to stop")
            }
        }

        gsrRecordingThread = null

        Log.d(TAG, "GSR recording stopped")
    }

    private fun createGSRSessionDirectory(): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val sessionDir = File(getExternalFilesDir(null), "gsr_session_$timestamp")

        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        // Create metadata file
        val metadataFile = File(sessionDir, "session_metadata.json")
        try {
            metadataFile.writeText(
                """
                {
                    "session_start": ${System.currentTimeMillis()},
                    "sampling_rate_hz": $gsrSamplingRate,
                    "device_type": "Shimmer3_GSR+",
                    "data_format": "CSV",
                    "columns": ["timestamp", "gsr_value_us", "quality", "session_time_ms"]
                }
            """.trimIndent()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create GSR metadata file", e)
        }

        return sessionDir
    }

    private fun startThermalRecording() {
        if (!thermalCameraConnected) {
            Log.w(TAG, "Cannot start thermal recording - camera not connected")
            return
        }

        isThermalRecording = true
        thermalFrameBuffer.clear()

        Log.d(TAG, "Starting thermal recording")

        // Start thermal recording thread
        thermalRecordingThread = Thread {
            val sessionDir = createThermalSessionDirectory()
            var frameCount = 0

            while (isThermalRecording && thermalCameraConnected) {
                try {
                    // TODO: Replace with actual thermal frame capture from SDK when available
                    // For now, use enhanced thermal simulation
                    val thermalData = generateRealisticThermalFrame(frameCount)
                    val thermalBitmap = createThermalBitmapFromData(thermalData)

                    // Save frame to buffer
                    synchronized(thermalFrameBuffer) {
                        thermalFrameBuffer.add(thermalData)

                        // Keep buffer size manageable (last 100 frames)
                        if (thermalFrameBuffer.size > 100) {
                            thermalFrameBuffer.removeAt(0)
                        }
                    }

                    // Save thermal frame and temperature data to files
                    saveThermalFrameWithData(thermalBitmap, thermalData, sessionDir, frameCount)
                    frameCount++

                    // Record at ~25 FPS
                    Thread.sleep(40)

                } catch (e: Exception) {
                    Log.e(TAG, "Error during thermal recording", e)
                    break
                }
            }

            Log.d(TAG, "Thermal recording thread stopped. Recorded $frameCount frames")
        }

        thermalRecordingThread?.start()
    }

    private fun createThermalSessionDirectory(): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val sessionDir = File(getExternalFilesDir(null), "thermal_session_$timestamp")

        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        // Create thermal session metadata file
        val metadataFile = File(sessionDir, "thermal_metadata.json")
        try {
            metadataFile.writeText(
                """
                {
                    "session_start": ${System.currentTimeMillis()},
                    "camera_model": "Topdon TC001",
                    "resolution": "256x192",
                    "frame_rate": 25,
                    "temperature_range": "-20 to 550°C",
                    "data_format": "PNG + CSV",
                    "color_palette": "ironbow"
                }
            """.trimIndent()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create thermal metadata file", e)
        }

        return sessionDir
    }

    private fun saveThermalFrameWithData(
        thermalBitmap: Bitmap,
        thermalData: ThermalFrameData,
        sessionDir: File,
        frameNumber: Int
    ) {
        try {
            // Save thermal image
            val imageFilename = String.format("thermal_frame_%06d.png", frameNumber)
            val imageFile = File(sessionDir, imageFilename)

            FileOutputStream(imageFile).use { out ->
                thermalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Save temperature data as CSV
            val dataFilename = String.format("thermal_data_%06d.csv", frameNumber)
            val dataFile = File(sessionDir, dataFilename)

            dataFile.bufferedWriter().use { writer ->
                writer.write("x,y,temperature_celsius\n")
                for (y in 0 until thermalData.height) {
                    for (x in 0 until thermalData.width) {
                        writer.write("$x,$y,${thermalData.temperatureMatrix[y][x]}\n")
                    }
                }
            }

            // Save frame summary
            if (frameNumber % 25 == 0) { // Every second at 25 FPS
                val summaryFile = File(sessionDir, "frame_summary.csv")
                val isNewFile = !summaryFile.exists()

                summaryFile.appendText(
                    if (isNewFile) "frame_number,timestamp,min_temp,max_temp,avg_temp\n" else ""
                )

                val avgTemp = thermalData.temperatureMatrix.flatMap { it.toList() }.average()
                summaryFile.appendText(
                    "$frameNumber,${thermalData.timestamp},${thermalData.minTemp},${thermalData.maxTemp},$avgTemp\n"
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save thermal frame $frameNumber", e)
        }
    }

    private fun stopThermalRecording() {
        Log.d(TAG, "Stopping thermal recording")

        isThermalRecording = false

        // Wait for recording thread to finish
        thermalRecordingThread?.let { thread ->
            try {
                thread.join(1000) // Wait up to 1 second
            } catch (e: InterruptedException) {
                Log.w(TAG, "Interrupted while waiting for thermal recording thread to stop")
            }
        }

        thermalRecordingThread = null

        // Optionally convert frame sequence to video
        // convertThermalFramesToVideo()
    }

    private fun createSessionDirectory(): File {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val sessionDir = File(getExternalFilesDir(null), "thermal_session_$timestamp")

        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }

        return sessionDir
    }

    private fun saveThermalFrame(frame: Bitmap, sessionDir: File, frameNumber: Int) {
        try {
            val filename = String.format("thermal_frame_%06d.png", frameNumber)
            val file = File(sessionDir, filename)

            FileOutputStream(file).use { out ->
                frame.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save thermal frame $frameNumber", e)
        }
    }


    private fun getRecordedFilesList(): JSONArray {
        val fileList = JSONArray()
        try {
            val externalFilesDir = getExternalFilesDir(null)
            if (externalFilesDir != null && externalFilesDir.exists()) {
                val files = externalFilesDir.listFiles()
                files?.forEach { file ->
                    if (file.isFile) {
                        val fileInfo = JSONObject().apply {
                            put("filename", file.name)
                            put("size", file.length())
                            put("last_modified", file.lastModified())
                            put("file_type", getFileType(file.name))
                        }
                        fileList.put(fileInfo)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recorded files list", e)
        }
        return fileList
    }

    private fun calculateTotalFileSize(fileList: JSONArray): Long {
        var totalSize = 0L
        try {
            for (i in 0 until fileList.length()) {
                val fileInfo = fileList.getJSONObject(i)
                totalSize += fileInfo.getLong("size")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating total file size", e)
        }
        return totalSize
    }

    private fun getFileType(filename: String): String {
        return when {
            filename.endsWith(".mp4") -> "video"
            filename.endsWith(".csv") -> "gsr"
            filename.endsWith(".png") -> "thermal"
            filename.endsWith(".jpg") || filename.endsWith(".jpeg") -> "image"
            else -> "unknown"
        }
    }

    private fun startFileTransfer(transferPort: Int, sessionId: String) {
        Thread {
            try {
                Log.d(TAG, "Starting file transfer to PC on port $transferPort")

                val socket = Socket("192.168.1.100", transferPort) // Replace with actual PC IP
                val outputStream = socket.getOutputStream()

                val fileList = getRecordedFilesList()

                // Send each file
                for (i in 0 until fileList.length()) {
                    val fileInfo = fileList.getJSONObject(i)
                    val filename = fileInfo.getString("filename")
                    val fileSize = fileInfo.getLong("size")
                    val fileType = fileInfo.getString("file_type")

                    val file = File(getExternalFilesDir(null), filename)
                    if (file.exists()) {
                        // Send file header
                        val header = JSONObject().apply {
                            put("type", "file")
                            put("filename", filename)
                            put("size", fileSize)
                            put("file_type", fileType)
                            put("session_id", sessionId)
                        }

                        val headerBytes = header.toString().padEnd(1024, '\u0000').toByteArray()
                        outputStream.write(headerBytes)

                        // Send file data
                        FileInputStream(file).use { fileInput ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (fileInput.read(buffer).also { bytesRead = it } != -1) {
                                outputStream.write(buffer, 0, bytesRead)
                            }
                        }

                        Log.d(TAG, "Sent file: $filename ($fileSize bytes)")
                    }
                }

                // Send completion header
                val completionHeader = JSONObject().apply {
                    put("type", "complete")
                    put("session_id", sessionId)
                }
                val completionBytes = completionHeader.toString().padEnd(1024, '\u0000').toByteArray()
                outputStream.write(completionBytes)

                socket.close()
                Log.d(TAG, "File transfer completed successfully")

            } catch (e: Exception) {
                Log.e(TAG, "Error in file transfer", e)
            }
        }.start()
    }

    private fun sendResponseToPC(response: String) {
        // This would be implemented by the NetworkHandler
        // For now, just log the response
        Log.d(TAG, "Response to PC: $response")
    }

    private fun getDeviceIPAddress(): String {
        try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress

            return String.format(
                "%d.%d.%d.%d",
                ipAddress and 0xff,
                ipAddress shr 8 and 0xff,
                ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device IP address", e)
            return "unknown"
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()

        // Stop GSR recording and data streaming
        isGSRRecording = false
        gsrConnected = false

        // Wait for GSR threads to finish
        gsrRecordingThread?.let { thread ->
            try {
                thread.join(2000)
            } catch (e: InterruptedException) {
                Log.w(TAG, "Interrupted while waiting for GSR recording thread")
            }
        }

        // Stop thermal recording and frame capture
        isThermalRecording = false
        thermalCameraConnected = false

        // Wait for thermal threads to finish
        thermalRecordingThread?.let { thread ->
            try {
                thread.join(2000)
            } catch (e: InterruptedException) {
                Log.w(TAG, "Interrupted while waiting for thermal recording thread")
            }
        }

        // Disconnect Shimmer device if connected
        try {
            // TODO: Add actual Shimmer SDK disconnect call when available
            // (shimmerDevice as? Shimmer)?.let { device ->
            //     device.stopStreaming()
            //     device.disconnect()
            // }
            shimmerDevice = null
            shimmerMacAddress = null
        } catch (e: Exception) {
            Log.w(TAG, "Error disconnecting Shimmer device", e)
        }

        // Stop Bluetooth discovery if running
        try {
            bluetoothAdapter?.let { adapter ->
                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Bluetooth discovery", e)
        }

        // Stop network server
        if (::networkHandler.isInitialized) {
            networkHandler.disconnect()
        }

        // Unregister receivers
        try {
            unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering USB receiver", e)
        }

        try {
            bluetoothDiscoveryReceiver?.let { receiver ->
                unregisterReceiver(receiver)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering Bluetooth discovery receiver", e)
        }

        // Release hand analysis handler
        try {
            handAnalysisHandler.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing hand analysis handler", e)
        }

        // Shutdown camera executor
        cameraExecutor.shutdown()

        Log.d(TAG, "MainActivity destroyed and resources cleaned up")
    }
}
