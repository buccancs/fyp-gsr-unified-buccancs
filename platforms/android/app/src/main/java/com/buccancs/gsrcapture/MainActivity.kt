package com.buccancs.gsrcapture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.TextureView
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.buccancs.gsrcapture.controller.RecordingController
import com.buccancs.gsrcapture.network.CommandProtocolClient
import com.buccancs.gsr.common.network.CommandProtocol

/**
 * MainActivity for the GSR Capture app.
 * This activity handles the main UI and coordinates the different capture components.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var viewFinder: PreviewView
    private lateinit var thermalView: TextureView
    private lateinit var recordButton: Button
    private lateinit var cameraSwitchButton: Button
    private lateinit var settingsButton: Button
    private lateinit var gsrStatusText: TextView
    private lateinit var heartRateText: TextView
    private lateinit var cameraTypeText: TextView
    private lateinit var recordingStatusText: TextView

    // Sensor toggle checkboxes
    private lateinit var rgbVideoCheckBox: CheckBox
    private lateinit var thermalVideoCheckBox: CheckBox
    private lateinit var gsrSensorCheckBox: CheckBox
    private lateinit var audioRecordingCheckBox: CheckBox

    private var isRgbMode = true // true for RGB, false for Thermal

    // Recording controller
    private lateinit var recordingController: RecordingController

    // Network client for remote control
    private lateinit var commandProtocolClient: CommandProtocolClient

    // Permission request codes
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 11
        private const val REQUEST_CODE_SETTINGS = 12

        private fun getRequiredPermissions(): Array<String> {
            val permissions = mutableListOf<String>()

            // Core permissions needed on all API levels
            permissions.addAll(listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
            ))

            // Storage permissions based on API level
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+ - For Android 11+, READ_EXTERNAL_STORAGE is automatically denied
                // when MANAGE_EXTERNAL_STORAGE is declared in manifest
                // WRITE_EXTERNAL_STORAGE is automatically denied, so we don't request it
                // MANAGE_EXTERNAL_STORAGE is handled separately through Settings
                // Don't request READ_EXTERNAL_STORAGE on API 30+ as it will be denied
            } else {
                // Pre-API 30 - Legacy storage permissions
                permissions.addAll(listOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ))
            }

            // Bluetooth permissions based on API level
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ - New Bluetooth permissions for nearby devices
                permissions.addAll(listOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                ))
            } else {
                // Pre-API 31 - Legacy Bluetooth permissions
                permissions.addAll(listOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ))
            }

            // Location permissions (needed for Bluetooth scanning on older devices)
            permissions.addAll(listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))

            // Notification permission for API 33+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            return permissions.toTypedArray()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        viewFinder = findViewById(R.id.viewFinder)
        // thermalView = findViewById(R.id.thermalView) // Uncomment when thermal view is added to layout
        recordButton = findViewById(R.id.recordButton)
        cameraSwitchButton = findViewById(R.id.cameraSwitchButton)
        settingsButton = findViewById(R.id.settingsButton)
        gsrStatusText = findViewById(R.id.gsrStatusText)
        heartRateText = findViewById(R.id.heartRateText)
        cameraTypeText = findViewById(R.id.cameraTypeText)
        recordingStatusText = findViewById(R.id.recordingStatusText)

        // Initialize sensor toggle checkboxes
        rgbVideoCheckBox = findViewById(R.id.rgbVideoCheckBox)
        thermalVideoCheckBox = findViewById(R.id.thermalVideoCheckBox)
        gsrSensorCheckBox = findViewById(R.id.gsrSensorCheckBox)
        audioRecordingCheckBox = findViewById(R.id.audioRecordingCheckBox)

        // Initialize recording controller
        recordingController = RecordingController(this)

        // Initialize CommandProtocol client for PC integration
        commandProtocolClient = CommandProtocolClient(this)

        // Request permissions
        if (allPermissionsGranted()) {
            initializeApp()
        } else {
            ActivityCompat.requestPermissions(
                this,
                getRequiredPermissions(),
                REQUEST_CODE_PERMISSIONS,
            )
        }

        // Set up button listeners
        recordButton.setOnClickListener {
            toggleRecording()
        }

        cameraSwitchButton.setOnClickListener {
            toggleCameraMode()
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SETTINGS)
        }

        // Set up sensor toggle listeners
        setupSensorToggles()

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

        // Start CommandProtocol client for PC integration on background thread
        Thread {
            try {
                commandProtocolClient.start()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to start network client: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()

        // Connect network client to thermal camera for video streaming
        recordingController.setNetworkClient(commandProtocolClient)
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
            commandProtocolClient.streamGsrData(value, System.currentTimeMillis())
        }

        // Set up heart rate callback
        recordingController.setHeartRateCallback { value ->
            heartRateText.text = getString(R.string.heart_rate, value)
            // Stream heart rate data to PC controller
            commandProtocolClient.streamHeartRateData(value, System.currentTimeMillis())
        }

        // Set up error callback
        recordingController.setErrorCallback { errorMessage ->
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
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

    private fun setupSensorToggles() {
        // Set up RGB video checkbox listener
        rgbVideoCheckBox.setOnCheckedChangeListener { _, isChecked ->
            recordingController.setSensorEnabled("rgb_video", isChecked)

            // Update visual feedback
            updateSensorToggleUI()

            // If recording is active, show warning that changes will take effect on next recording
            if (recordingStatusText.text == getString(R.string.status_recording)) {
                Toast.makeText(this, "Sensor changes will take effect on next recording", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up thermal video checkbox listener
        thermalVideoCheckBox.setOnCheckedChangeListener { _, isChecked ->
            recordingController.setSensorEnabled("thermal_video", isChecked)

            // Update visual feedback
            updateSensorToggleUI()

            // If recording is active, show warning that changes will take effect on next recording
            if (recordingStatusText.text == getString(R.string.status_recording)) {
                Toast.makeText(this, "Sensor changes will take effect on next recording", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up GSR sensor checkbox listener
        gsrSensorCheckBox.setOnCheckedChangeListener { _, isChecked ->
            recordingController.setSensorEnabled("gsr_sensor", isChecked)

            // Update visual feedback
            updateSensorToggleUI()

            // If recording is active, show warning that changes will take effect on next recording
            if (recordingStatusText.text == getString(R.string.status_recording)) {
                Toast.makeText(this, "Sensor changes will take effect on next recording", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up audio recording checkbox listener
        audioRecordingCheckBox.setOnCheckedChangeListener { _, isChecked ->
            recordingController.setSensorEnabled("audio_recording", isChecked)

            // Update visual feedback
            updateSensorToggleUI()

            // If recording is active, show warning that changes will take effect on next recording
            if (recordingStatusText.text == getString(R.string.status_recording)) {
                Toast.makeText(this, "Sensor changes will take effect on next recording", Toast.LENGTH_SHORT).show()
            }
        }

        // Initial UI update
        updateSensorToggleUI()
    }

    private fun updateSensorToggleUI() {
        // Disable checkboxes during recording to prevent confusion
        val isRecording = recordingStatusText.text == getString(R.string.status_recording)

        rgbVideoCheckBox.isEnabled = !isRecording
        thermalVideoCheckBox.isEnabled = !isRecording
        gsrSensorCheckBox.isEnabled = !isRecording
        audioRecordingCheckBox.isEnabled = !isRecording

        // Update visual feedback based on checkbox states
        // You could add visual indicators here (e.g., change text colors, add icons)
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
            "CMD_SWITCH_FRONT_CAMERA" -> {
                if (::recordingController.isInitialized) {
                    // Switch to front camera if not already using it
                    if (recordingController.setFrontCamera(viewFinder)) {
                        Toast.makeText(this, "Switched to front camera", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to switch to front camera", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "CMD_SWITCH_REAR_CAMERA" -> {
                if (::recordingController.isInitialized) {
                    // Switch to rear camera if not already using it
                    if (recordingController.setRearCamera(viewFinder)) {
                        Toast.makeText(this, "Switched to rear camera", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to switch to rear camera", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "CMD_TOGGLE_RGB_SENSOR" -> {
                val currentState = recordingController.isSensorEnabled("rgb_video")
                recordingController.setSensorEnabled("rgb_video", !currentState)
                rgbVideoCheckBox.isChecked = !currentState
                Toast.makeText(this, "RGB video ${if (!currentState) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
            }
            "CMD_TOGGLE_THERMAL_SENSOR" -> {
                val currentState = recordingController.isSensorEnabled("thermal_video")
                recordingController.setSensorEnabled("thermal_video", !currentState)
                thermalVideoCheckBox.isChecked = !currentState
                Toast.makeText(this, "Thermal video ${if (!currentState) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
            }
            "CMD_TOGGLE_GSR_SENSOR" -> {
                val currentState = recordingController.isSensorEnabled("gsr_sensor")
                recordingController.setSensorEnabled("gsr_sensor", !currentState)
                gsrSensorCheckBox.isChecked = !currentState
                Toast.makeText(this, "GSR sensor ${if (!currentState) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
            }
            "CMD_TOGGLE_AUDIO_RECORDING" -> {
                val currentState = recordingController.isSensorEnabled("audio_recording")
                recordingController.setSensorEnabled("audio_recording", !currentState)
                audioRecordingCheckBox.isChecked = !currentState
                Toast.makeText(this, "Audio recording ${if (!currentState) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
            }
            "CMD_GET_SENSOR_STATUS" -> {
                // Send current sensor status back to PC
                val enabledSensors = recordingController.getEnabledSensors()
                val statusMessage = "Enabled sensors: ${enabledSensors.joinToString(", ")}"
                Toast.makeText(this, statusMessage, Toast.LENGTH_LONG).show()

                // Send sensor status back to PC via network
                commandProtocolClient.sendResponse(
                    CommandProtocol.CommandType.CMD_STATUS,
                    CommandProtocol.StatusCode.OK,
                    "Sensor status response",
                    "enabled_sensors:${enabledSensors.joinToString(",")}"
                )
            }

            else -> {
                // Handle other commands
                Toast.makeText(this, "Unknown command: $command", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        // Check regular permissions
        val regularPermissionsGranted = getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

        // For API 30+, also check MANAGE_EXTERNAL_STORAGE permission
        val storagePermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // Not needed for older versions
        }

        return regularPermissionsGranted && storagePermissionGranted
    }

    private fun requestManageExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE_MANAGE_EXTERNAL_STORAGE)
                Toast.makeText(this, "Please grant 'All files access' permission for the app to work properly", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivityForResult(intent, REQUEST_CODE_MANAGE_EXTERNAL_STORAGE)
                Toast.makeText(this, "Please find and enable 'All files access' for this app", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            // Check regular permissions first
            val regularPermissionsGranted = getRequiredPermissions().all {
                ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
            }

            if (regularPermissionsGranted) {
                // Regular permissions granted, now check storage permission for API 30+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    // Need to request MANAGE_EXTERNAL_STORAGE permission through Settings
                    requestManageExternalStoragePermission()
                } else {
                    // All permissions granted
                    initializeApp()
                }
            } else {
                // Debug: Log which permissions were denied
                val deniedPermissions = mutableListOf<String>()
                for (i in permissions.indices) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissions.add(permissions[i])
                    }
                }

                val deniedPermissionsList = deniedPermissions.joinToString(", ")
                android.util.Log.e("MainActivity", "Denied permissions: $deniedPermissionsList")

                Toast
                    .makeText(
                        this,
                        "Required permissions not granted. Denied: ${deniedPermissions.size} permissions",
                        Toast.LENGTH_LONG,
                    ).show()
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // Permission granted, initialize the app
                    initializeApp()
                } else {
                    // Permission denied
                    Toast.makeText(this, "All files access permission is required for the app to work properly", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        } else if (requestCode == REQUEST_CODE_SETTINGS && resultCode == RESULT_OK) {
            // Settings were changed, update UI if needed
            data?.let {
                val settingsChanged = it.getBooleanExtra("settings_changed", false)
                if (settingsChanged) {
                    Toast.makeText(this, "Settings updated", Toast.LENGTH_SHORT).show()
                    // You can add more specific UI updates here if needed
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recordingController.shutdown()
        commandProtocolClient.stop()
    }
}
