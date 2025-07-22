package com.buccancs.gsrcapture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.buccancs.gsrcapture.camera.CaptureStage
import com.buccancs.gsrcapture.controller.RecordingController
import java.io.File

/**
 * Settings Activity for configuring camera, recording, and sensor parameters
 */
class SettingsActivity : AppCompatActivity() {

    // UI Components
    private lateinit var cameraTypeSpinner: Spinner
    private lateinit var captureStageSpinner: Spinner
    private lateinit var fpsSeekBar: SeekBar
    private lateinit var fpsValueText: TextView
    private lateinit var resolutionSpinner: Spinner
    private lateinit var videoQualitySpinner: Spinner

    // Camera Parameters
    private lateinit var exposureSeekBar: SeekBar
    private lateinit var exposureValueText: TextView
    private lateinit var isoSeekBar: SeekBar
    private lateinit var isoValueText: TextView
    private lateinit var focusDistanceSeekBar: SeekBar
    private lateinit var focusDistanceValueText: TextView

    // Sensor Status
    private lateinit var gsrSensorStatus: TextView
    private lateinit var thermalCameraStatus: TextView
    private lateinit var rgbCameraStatus: TextView
    private lateinit var audioStatus: TextView

    // Folder Access
    private lateinit var outputFolderText: TextView
    private lateinit var selectFolderButton: Button
    private lateinit var openFolderButton: Button

    // Control Buttons
    private lateinit var saveButton: Button
    private lateinit var resetButton: Button
    private lateinit var backButton: Button

    // Recording Controller for accessing current settings
    private lateinit var recordingController: RecordingController

    companion object {
        private const val REQUEST_CODE_SELECT_FOLDER = 100
        private const val REQUEST_CODE_PERMISSIONS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        try {
            // Initialize recording controller
            recordingController = RecordingController(this)

            initializeViews()
            setupSpinners()
            setupSeekBars()
            setupButtons()
            loadCurrentSettings()
            updateSensorStatus()
        } catch (e: Exception) {
            // Handle initialization errors gracefully (e.g., in test environments)
            initializeViews()
            setupSpinners()
            setupSeekBars()
            setupButtons()
            setDefaultSettings()
            setDefaultSensorStatus()
        }
    }

    private fun initializeViews() {
        // Camera Settings
        cameraTypeSpinner = findViewById(R.id.cameraTypeSpinner)
        captureStageSpinner = findViewById(R.id.captureStageSpinner)
        fpsSeekBar = findViewById(R.id.fpsSeekBar)
        fpsValueText = findViewById(R.id.fpsValueText)
        resolutionSpinner = findViewById(R.id.resolutionSpinner)
        videoQualitySpinner = findViewById(R.id.videoQualitySpinner)

        // Camera Parameters
        exposureSeekBar = findViewById(R.id.exposureSeekBar)
        exposureValueText = findViewById(R.id.exposureValueText)
        isoSeekBar = findViewById(R.id.isoSeekBar)
        isoValueText = findViewById(R.id.isoValueText)
        focusDistanceSeekBar = findViewById(R.id.focusDistanceSeekBar)
        focusDistanceValueText = findViewById(R.id.focusDistanceValueText)

        // Sensor Status
        gsrSensorStatus = findViewById(R.id.gsrSensorStatus)
        thermalCameraStatus = findViewById(R.id.thermalCameraStatus)
        rgbCameraStatus = findViewById(R.id.rgbCameraStatus)
        audioStatus = findViewById(R.id.audioStatus)

        // Folder Access
        outputFolderText = findViewById(R.id.outputFolderText)
        selectFolderButton = findViewById(R.id.selectFolderButton)
        openFolderButton = findViewById(R.id.openFolderButton)

        // Control Buttons
        saveButton = findViewById(R.id.saveButton)
        resetButton = findViewById(R.id.resetButton)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupSpinners() {
        // Camera Type Spinner
        val cameraTypes = arrayOf(
            "RGB Camera (Main)",
            "Thermal Camera",
            "Front Camera",
            "Wide Camera",
            "Ultra Wide Camera"
        )
        val cameraAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, cameraTypes)
        cameraAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        cameraTypeSpinner.adapter = cameraAdapter

        // Capture Stage Spinner
        val captureStages = arrayOf(
            "Stage 1 - RAW Sensor (30 fps max)",
            "Stage 3 - YUV Processed (240 fps max)"
        )
        val stageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, captureStages)
        stageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        captureStageSpinner.adapter = stageAdapter

        // Resolution Spinner
        val resolutions = arrayOf(
            "4K (3840x2160)",
            "1080p (1920x1080)",
            "720p (1280x720)",
            "480p (854x480)"
        )
        val resolutionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, resolutions)
        resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        resolutionSpinner.adapter = resolutionAdapter

        // Video Quality Spinner
        val qualities = arrayOf(
            "High Quality",
            "Medium Quality",
            "Low Quality",
            "Custom"
        )
        val qualityAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, qualities)
        qualityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        videoQualitySpinner.adapter = qualityAdapter

        // Set up spinner listeners
        captureStageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateFpsLimits(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSeekBars() {
        // FPS SeekBar
        fpsSeekBar.max = 240
        fpsSeekBar.progress = 30
        fpsValueText.text = "30 fps"
        fpsSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                fpsValueText.text = "$progress fps"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Exposure SeekBar (-2.0 to +2.0 EV)
        exposureSeekBar.max = 40
        exposureSeekBar.progress = 20 // 0 EV
        exposureValueText.text = "0.0 EV"
        exposureSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val ev = (progress - 20) / 10.0
                exposureValueText.text = "${String.format("%.1f", ev)} EV"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // ISO SeekBar (100 to 3200)
        isoSeekBar.max = 32
        isoSeekBar.progress = 8 // ISO 800
        isoValueText.text = "ISO 800"
        isoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val iso = 100 + (progress * 100)
                isoValueText.text = "ISO $iso"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Focus Distance SeekBar (0.0 to 10.0 meters)
        focusDistanceSeekBar.max = 100
        focusDistanceSeekBar.progress = 0 // Auto focus
        focusDistanceValueText.text = "Auto Focus"
        focusDistanceSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (progress == 0) {
                    focusDistanceValueText.text = "Auto Focus"
                } else {
                    val distance = progress / 10.0
                    focusDistanceValueText.text = "${String.format("%.1f", distance)}m"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupButtons() {
        selectFolderButton.setOnClickListener {
            selectOutputFolder()
        }

        openFolderButton.setOnClickListener {
            openOutputFolder()
        }

        saveButton.setOnClickListener {
            saveSettings()
        }

        resetButton.setOnClickListener {
            resetToDefaults()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun updateFpsLimits(captureStagePosition: Int) {
        when (captureStagePosition) {
            0 -> { // Stage 1 - RAW
                fpsSeekBar.max = 30
                if (fpsSeekBar.progress > 30) {
                    fpsSeekBar.progress = 30
                    fpsValueText.text = "30 fps"
                }
            }
            1 -> { // Stage 3 - YUV
                fpsSeekBar.max = 240
            }
        }
    }

    private fun loadCurrentSettings() {
        // Load current capture stage
        val currentStage = recordingController.getCurrentCaptureStage()
        when (currentStage) {
            CaptureStage.STAGE_1_RAW_SENSOR -> captureStageSpinner.setSelection(0)
            CaptureStage.STAGE_3_YUV_PROCESSED -> captureStageSpinner.setSelection(1)
            else -> captureStageSpinner.setSelection(1) // Default to YUV
        }

        // Set default output folder
        val defaultFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GSRCapture")
        outputFolderText.text = defaultFolder.absolutePath
    }

    private fun updateSensorStatus() {
        // Check GSR sensor status
        val isConnected = try {
            recordingController.isConnectedForTesting()
        } catch (e: Exception) {
            false
        }

        gsrSensorStatus.text = if (isConnected) {
            "GSR Sensor: Connected"
        } else {
            "GSR Sensor: Disconnected"
        }
        gsrSensorStatus.setTextColor(
            if (isConnected) 
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            else 
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
        )

        // Check thermal camera status
        val thermalConnected = recordingController.connectThermalCamera()
        thermalCameraStatus.text = if (thermalConnected) {
            "Thermal Camera: Available"
        } else {
            "Thermal Camera: Not Available"
        }
        thermalCameraStatus.setTextColor(
            if (thermalConnected) 
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            else 
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
        )

        // RGB camera is assumed to be always available
        rgbCameraStatus.text = "RGB Camera: Available"
        rgbCameraStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))

        // Audio status
        val hasAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        audioStatus.text = if (hasAudioPermission) {
            "Audio Recording: Enabled"
        } else {
            "Audio Recording: Permission Required"
        }
        audioStatus.setTextColor(
            if (hasAudioPermission) 
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            else 
                ContextCompat.getColor(this, android.R.color.holo_orange_dark)
        )
    }

    private fun selectOutputFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        startActivityForResult(intent, REQUEST_CODE_SELECT_FOLDER)
    }

    private fun openOutputFolder() {
        val folderPath = outputFolderText.text.toString()
        val folder = File(folderPath)

        if (folder.exists()) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(folder), DocumentsContract.Document.MIME_TYPE_DIR)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open folder: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Folder does not exist", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        try {
            // Apply capture stage setting if recording controller is available
            if (::recordingController.isInitialized) {
                val selectedStage = when (captureStageSpinner.selectedItemPosition) {
                    0 -> CaptureStage.STAGE_1_RAW_SENSOR
                    1 -> CaptureStage.STAGE_3_YUV_PROCESSED
                    else -> CaptureStage.STAGE_3_YUV_PROCESSED
                }
                recordingController.setCaptureStage(selectedStage)
            }

            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()

            // Return result to MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("settings_changed", true)
            setResult(RESULT_OK, resultIntent)

        } catch (e: Exception) {
            Toast.makeText(this, "Error saving settings: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetToDefaults() {
        // Reset all controls to default values
        cameraTypeSpinner.setSelection(0) // RGB Camera
        captureStageSpinner.setSelection(1) // YUV Processed
        fpsSeekBar.progress = 30
        fpsValueText.text = "30 fps"
        resolutionSpinner.setSelection(1) // 1080p
        videoQualitySpinner.setSelection(0) // High Quality

        exposureSeekBar.progress = 20 // 0 EV
        exposureValueText.text = "0.0 EV"
        isoSeekBar.progress = 8 // ISO 800
        isoValueText.text = "ISO 800"
        focusDistanceSeekBar.progress = 0 // Auto Focus
        focusDistanceValueText.text = "Auto Focus"

        Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SELECT_FOLDER && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                // Take persistable permission
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                outputFolderText.text = uri.toString()
                Toast.makeText(this, "Output folder selected", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setDefaultSettings() {
        // Set default capture stage
        captureStageSpinner.setSelection(1) // Default to YUV

        // Set default output folder
        val defaultFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "GSRCapture")
        outputFolderText.text = defaultFolder.absolutePath
    }

    private fun setDefaultSensorStatus() {
        // Set default sensor status when RecordingController is not available
        gsrSensorStatus.text = "GSR Sensor: Unknown"
        gsrSensorStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        thermalCameraStatus.text = "Thermal Camera: Unknown"
        thermalCameraStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        rgbCameraStatus.text = "RGB Camera: Unknown"
        rgbCameraStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))

        audioStatus.text = "Audio Recording: Unknown"
        audioStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up recording controller if needed
        if (::recordingController.isInitialized) {
            try {
                recordingController.shutdown()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}
