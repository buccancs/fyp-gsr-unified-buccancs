package com.buccancs.gsrcapture

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.TextureView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.buccancs.gsrcapture.controller.RecordingController
import com.buccancs.gsrcapture.network.NetworkClient
import com.buccancs.gsrcapture.network.CommandProtocolClient

/**
 * MainActivity for the GSR Capture app.
 * This activity handles the main UI and coordinates the different capture components.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var thermalView: TextureView
    private lateinit var recordButton: Button
    private lateinit var cameraSwitchButton: Button
    private lateinit var gsrStatusText: TextView
    private lateinit var heartRateText: TextView
    private lateinit var cameraTypeText: TextView
    private lateinit var recordingStatusText: TextView

    private var isRgbMode = true // true for RGB, false for Thermal

    // Recording controller
    private lateinit var recordingController: RecordingController

    // Network client for remote control
    private lateinit var networkClient: NetworkClient
    private lateinit var commandProtocolClient: CommandProtocolClient

    // Permission request codes
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        viewFinder = findViewById(R.id.viewFinder)
        // thermalView = findViewById(R.id.thermalView) // Uncomment when thermal view is added to layout
        recordButton = findViewById(R.id.recordButton)
        cameraSwitchButton = findViewById(R.id.cameraSwitchButton)
        gsrStatusText = findViewById(R.id.gsrStatusText)
        heartRateText = findViewById(R.id.heartRateText)
        cameraTypeText = findViewById(R.id.cameraTypeText)
        recordingStatusText = findViewById(R.id.recordingStatusText)

        // Initialize recording controller
        recordingController = RecordingController(this)

        // Initialize network client
        networkClient = NetworkClient(this)

        // Initialize CommandProtocol client for PC integration
        commandProtocolClient = CommandProtocolClient(this)

        // Request permissions
        if (allPermissionsGranted()) {
            initializeApp()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Set up button listeners
        recordButton.setOnClickListener {
            toggleRecording()
        }

        cameraSwitchButton.setOnClickListener {
            toggleCameraMode()
        }

        // Set up callbacks
        setupCallbacks()
    }

    private fun initializeApp() {
        // Initialize recording controller
        recordingController.initialize()

        // Set up RGB camera preview
        recordingController.setRgbPreviewView(viewFinder)

        // Connect to thermal camera
        // Uncomment when thermal view is added to layout
        // if (recordingController.connectThermalCamera()) {
        //     recordingController.setThermalPreviewView(thermalView)
        // } else {
        //     Toast.makeText(this, getString(R.string.error_thermal_camera_unavailable), Toast.LENGTH_SHORT).show()
        // }

        // Connect to GSR sensor
        if (!recordingController.connectGsrSensor()) {
            Toast.makeText(this, getString(R.string.error_gsr_sensor_unavailable), Toast.LENGTH_SHORT).show()
        }

        // Start network client
        networkClient.start()

        // Start CommandProtocol client for PC integration
        commandProtocolClient.start()
    }

    private fun setupCallbacks() {
        // Set up recording state callback
        recordingController.setRecordingStateCallback { isRecording ->
            updateRecordingUI(isRecording)
        }

        // Set up GSR value callback
        recordingController.setGsrValueCallback { value ->
            gsrStatusText.text = getString(R.string.gsr_value, value)
            // Stream GSR data to PC controller
            networkClient.streamGsrData(value, System.currentTimeMillis())
        }

        // Set up heart rate callback
        recordingController.setHeartRateCallback { value ->
            heartRateText.text = getString(R.string.heart_rate, value)
            // Stream heart rate data to PC controller
            networkClient.streamHeartRateData(value, System.currentTimeMillis())
        }

        // Set up error callback
        recordingController.setErrorCallback { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }

        // Set up network command callback
        networkClient.setCommandCallback { command ->
            handleNetworkCommand(command)
        }

        // Set up CommandProtocol client callbacks
        commandProtocolClient.setCommandCallback { command ->
            handleNetworkCommand(command)
        }

        commandProtocolClient.setConnectionStateCallback { isConnected ->
            // Update UI to show connection status
            runOnUiThread {
                if (isConnected) {
                    Toast.makeText(this, "Connected to PC Controller", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Disconnected from PC Controller", Toast.LENGTH_SHORT).show()
                }
            }
        }

        commandProtocolClient.setErrorCallback { error, exception ->
            runOnUiThread {
                Toast.makeText(this, "Network error: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleRecording() {
        if (recordingStatusText.text == getString(R.string.status_recording)) {
            // Stop recording
            recordingController.stopRecording()
        } else {
            // Start recording
            if (recordingController.startRecording()) {
                // Recording started successfully
            } else {
                Toast.makeText(this, getString(R.string.error_recording_failed), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun toggleCameraMode() {
        isRgbMode = !isRgbMode

        if (isRgbMode) {
            // Switch to RGB mode
            cameraTypeText.text = getString(R.string.rgb_camera)
            viewFinder.visibility = android.view.View.VISIBLE
            // thermalView.visibility = android.view.View.GONE // Uncomment when thermal view is added to layout
        } else {
            // Switch to Thermal mode
            cameraTypeText.text = getString(R.string.thermal_camera)
            viewFinder.visibility = android.view.View.GONE
            // thermalView.visibility = android.view.View.VISIBLE // Uncomment when thermal view is added to layout
        }
    }

    private fun updateRecordingUI(isRecording: Boolean) {
        if (isRecording) {
            recordButton.text = getString(R.string.stop_recording)
            recordingStatusText.text = getString(R.string.status_recording)
        } else {
            recordButton.text = getString(R.string.start_recording)
            recordingStatusText.text = getString(R.string.status_ready)
        }
    }

    private fun handleNetworkCommand(command: String) {
        when (command) {
            // JSON-based commands (existing)
            "START_RECORDING" -> {
                if (recordingStatusText.text != getString(R.string.status_recording)) {
                    recordingController.startRecording()
                }
            }
            "STOP_RECORDING" -> {
                if (recordingStatusText.text == getString(R.string.status_recording)) {
                    recordingController.stopRecording()
                }
            }
            "SWITCH_TO_RGB" -> {
                if (!isRgbMode) {
                    toggleCameraMode()
                }
            }
            "SWITCH_TO_THERMAL" -> {
                if (isRgbMode) {
                    toggleCameraMode()
                }
            }

            // CommandProtocol commands (new)
            "CMD_START" -> {
                if (recordingStatusText.text != getString(R.string.status_recording)) {
                    val success = recordingController.startRecording()
                    if (success) {
                        Toast.makeText(this, "Recording started via remote command", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to start recording", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "CMD_STOP" -> {
                if (recordingStatusText.text == getString(R.string.status_recording)) {
                    recordingController.stopRecording()
                    Toast.makeText(this, "Recording stopped via remote command", Toast.LENGTH_SHORT).show()
                }
            }
            "CMD_STATUS" -> {
                // Send device status back to the PC controller
                commandProtocolClient.sendDeviceStatus()
            }

            else -> {
                // Handle other commands
                Toast.makeText(this, "Unknown command: $command", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                initializeApp()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recordingController.shutdown()
        networkClient.stop()
        commandProtocolClient.stop()
    }
}
