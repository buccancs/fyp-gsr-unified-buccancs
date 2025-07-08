package com.buccancs.gsrcapture.integration

import android.content.Context
import android.util.Log
import com.buccancs.gsrcapture.export.Hdf5Exporter
import com.buccancs.gsrcapture.export.MatlabExporter
import com.buccancs.gsrcapture.export.interfaces.DataExporter
import com.buccancs.gsrcapture.hardware.HardwareFactory
import com.buccancs.gsrcapture.hardware.interfaces.PhysiologicalSensor
import com.buccancs.gsrcapture.hardware.interfaces.ThermalCamera
import com.buccancs.gsrcapture.security.SecureNetworkWrapper
import com.buccancs.gsrcapture.security.interfaces.AuthenticationProvider
import com.buccancs.gsrcapture.security.interfaces.EncryptionProvider
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Integration example demonstrating how to use the new hardware abstraction,
 * security features, and data export capabilities together.
 *
 * This class shows a complete workflow from hardware detection to secure
 * data collection and export.
 */
class SystemIntegrationExample(
    private val context: Context,
) {
    companion object {
        private const val TAG = "SystemIntegrationExample"
    }

    private val executor: ExecutorService = Executors.newCachedThreadPool()
    private val hardwareFactory = HardwareFactory(context)

    // Hardware instances
    private var physiologicalSensor: PhysiologicalSensor? = null
    private var thermalCamera: ThermalCamera? = null

    // Security components
    private var secureNetworkWrapper: SecureNetworkWrapper? = null

    // Data collection
    private val collectedData = mutableListOf<PhysiologicalSensor.SensorReading>()
    private val collectedFrames = mutableListOf<ThermalCamera.ThermalFrame>()

    /**
     * Complete system integration workflow
     */
    fun runIntegrationExample() {
        Log.d(TAG, "Starting system integration example...")

        try {
            // Step 1: Hardware Detection and Setup
            setupHardware()

            // Step 2: Security Setup
            setupSecurity()

            // Step 3: Data Collection
            startDataCollection()

            // Step 4: Simulate data collection period
            Thread.sleep(5000) // Collect for 5 seconds

            // Step 5: Stop data collection
            stopDataCollection()

            // Step 6: Export collected data
            exportCollectedData()

            // Step 7: Cleanup
            cleanup()

            Log.d(TAG, "System integration example completed successfully!")
        } catch (e: Exception) {
            Log.e(TAG, "Error in system integration example", e)
        }
    }

    /**
     * Step 1: Detect and setup hardware using the abstraction layer
     */
    private fun setupHardware() {
        Log.d(TAG, "Setting up hardware...")

        // Detect all available hardware
        val allHardware = hardwareFactory.detectAllHardware()
        Log.d(TAG, "Detected ${allHardware.size} hardware devices")

        // Setup physiological sensors
        val sensors = hardwareFactory.detectPhysiologicalSensors()
        if (sensors.isNotEmpty()) {
            val sensorHardware = sensors.first()
            physiologicalSensor = hardwareFactory.createPhysiologicalSensor(sensorHardware, executor)

            if (physiologicalSensor?.connect(sensorHardware.identifier) == true) {
                Log.d(TAG, "Connected to physiological sensor: ${sensorHardware.name}")

                // Configure sensor
                val sensorSettings =
                    mapOf(
                        "samplingRate" to 51.2,
                        "enableGsr" to true,
                        "enablePpg" to true,
                    )
                physiologicalSensor?.configure(sensorSettings)
            }
        }

        // Setup thermal cameras
        val cameras = hardwareFactory.detectThermalCameras()
        if (cameras.isNotEmpty()) {
            val cameraHardware = cameras.first()
            thermalCamera = hardwareFactory.createThermalCamera(cameraHardware, executor)

            if (thermalCamera?.connect(cameraHardware.identifier) == true) {
                Log.d(TAG, "Connected to thermal camera: ${cameraHardware.name}")

                // Configure camera
                val cameraSettings =
                    mapOf(
                        "emissivity" to 0.95f,
                        "temperatureUnit" to "Celsius",
                        "colorPalette" to "Iron",
                    )
                thermalCamera?.configure(cameraSettings)
            }
        }

        // Print hardware information
        physiologicalSensor?.let { sensor ->
            val info = sensor.getHardwareInfo()
            Log.d(TAG, "Physiological Sensor Info: $info")
        }

        thermalCamera?.let { camera ->
            val info = camera.getHardwareInfo()
            val specs = camera.getCameraSpecs()
            Log.d(TAG, "Thermal Camera Info: $info")
            Log.d(TAG, "Thermal Camera Specs: $specs")
        }
    }

    /**
     * Step 2: Setup security components (demonstration)
     */
    private fun setupSecurity() {
        Log.d(TAG, "Setting up security...")

        // Create mock authentication and encryption providers
        val authProvider = createMockAuthProvider()
        val encryptionProvider = createMockEncryptionProvider()

        // Create secure network wrapper
        secureNetworkWrapper = SecureNetworkWrapper(authProvider, encryptionProvider)

        // Set up security callbacks
        secureNetworkWrapper?.setCallback(
            object : SecureNetworkWrapper.SecureNetworkCallback {
                override fun onSecureMessageReceived(
                    deviceId: String,
                    message: String,
                ) {
                    Log.d(TAG, "Secure message received from $deviceId: $message")
                }

                override fun onAuthenticationSuccess(deviceId: String) {
                    Log.d(TAG, "Authentication successful for device: $deviceId")
                }

                override fun onAuthenticationFailed(
                    deviceId: String,
                    reason: String,
                ) {
                    Log.w(TAG, "Authentication failed for device $deviceId: $reason")
                }

                override fun onConnectionSecured(deviceId: String) {
                    Log.d(TAG, "Connection secured for device: $deviceId")
                }

                override fun onSecurityError(
                    deviceId: String,
                    error: String,
                ) {
                    Log.e(TAG, "Security error for device $deviceId: $error")
                }
            },
        )

        Log.d(TAG, "Security setup completed")
    }

    /**
     * Step 3: Start data collection from all connected devices
     */
    private fun startDataCollection() {
        Log.d(TAG, "Starting data collection...")

        val outputDir = File(context.getExternalFilesDir(null), "recordings")
        val sessionId = "integration_test_${System.currentTimeMillis()}"

        // Start physiological sensor data collection
        physiologicalSensor?.let { sensor ->
            sensor.startRecording(outputDir, sessionId)
            sensor.startStreaming(
                object : PhysiologicalSensor.SensorCallback {
                    override fun onDataReceived(reading: PhysiologicalSensor.SensorReading) {
                        collectedData.add(reading)
                        Log.v(TAG, "Received sensor data: GSR=${reading.gsrValue}, PPG=${reading.ppgValue}")
                    }

                    override fun onConnectionStateChanged(isConnected: Boolean) {
                        Log.d(TAG, "Sensor connection state changed: $isConnected")
                    }

                    override fun onError(
                        error: String,
                        exception: Throwable?,
                    ) {
                        Log.e(TAG, "Sensor error: $error", exception)
                    }
                },
            )
        }

        // Start thermal camera data collection
        thermalCamera?.let { camera ->
            camera.startRecording(outputDir, sessionId)
            camera.startStreaming(
                object : ThermalCamera.ThermalCameraCallback {
                    override fun onFrameReceived(frame: ThermalCamera.ThermalFrame) {
                        collectedFrames.add(frame)
                        Log.v(TAG, "Received thermal frame: ${frame.timestamp}")
                    }

                    override fun onConnectionStateChanged(isConnected: Boolean) {
                        Log.d(TAG, "Camera connection state changed: $isConnected")
                    }

                    override fun onError(
                        error: String,
                        exception: Throwable?,
                    ) {
                        Log.e(TAG, "Camera error: $error", exception)
                    }
                },
            )
        }

        Log.d(TAG, "Data collection started")
    }

    /**
     * Step 5: Stop data collection
     */
    private fun stopDataCollection() {
        Log.d(TAG, "Stopping data collection...")

        physiologicalSensor?.stopStreaming()
        physiologicalSensor?.stopRecording()

        thermalCamera?.stopStreaming()
        thermalCamera?.stopRecording()

        Log.d(TAG, "Data collection stopped. Collected ${collectedData.size} sensor readings and ${collectedFrames.size} thermal frames")
    }

    /**
     * Step 6: Export collected data using the new export system
     */
    private fun exportCollectedData() {
        Log.d(TAG, "Exporting collected data...")

        if (collectedData.isEmpty()) {
            Log.w(TAG, "No data to export")
            return
        }

        try {
            // Prepare data for export
            val dataSeries = createDataSeries()
            val videoData = createVideoData()

            val dataset =
                DataExporter.ExportDataset(
                    sessionId = "integration_test_${System.currentTimeMillis()}",
                    startTime = collectedData.minOfOrNull { it.timestamp } ?: 0L,
                    endTime = collectedData.maxOfOrNull { it.timestamp } ?: 0L,
                    dataSeries = dataSeries,
                    videoData = videoData,
                    metadata =
                        mapOf(
                            "exportType" to "integration_test",
                            "deviceCount" to 2,
                            "sensorReadings" to collectedData.size,
                            "thermalFrames" to collectedFrames.size,
                        ),
                )

            val exportDir = File(context.getExternalFilesDir(null), "exports")
            exportDir.mkdirs()

            // Export to MATLAB format
            exportToMatlab(dataset, exportDir)

            // Export to HDF5 format
            exportToHdf5(dataset, exportDir)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting data", e)
        }
    }

    /**
     * Step 7: Cleanup resources
     */
    private fun cleanup() {
        Log.d(TAG, "Cleaning up resources...")

        physiologicalSensor?.shutdown()
        thermalCamera?.shutdown()

        executor.shutdown()

        Log.d(TAG, "Cleanup completed")
    }

    // Helper methods

    private fun createDataSeries(): List<DataExporter.DataSeries> {
        val series = mutableListOf<DataExporter.DataSeries>()

        // Create GSR data series
        val gsrTimestamps = collectedData.mapNotNull { if (it.gsrValue != null) it.timestamp else null }
        val gsrValues = collectedData.mapNotNull { it.gsrValue?.toDouble() }

        if (gsrTimestamps.isNotEmpty() && gsrValues.isNotEmpty()) {
            series.add(
                DataExporter.DataSeries(
                    name = "GSR",
                    timestamps = gsrTimestamps,
                    values = gsrValues,
                    unit = "microsiemens",
                    metadata =
                        mapOf(
                            "sensorType" to "Shimmer3",
                            "samplingRate" to "51.2Hz",
                        ),
                ),
            )
        }

        // Create PPG data series
        val ppgTimestamps = collectedData.mapNotNull { if (it.ppgValue != null) it.timestamp else null }
        val ppgValues = collectedData.mapNotNull { it.ppgValue?.toDouble() }

        if (ppgTimestamps.isNotEmpty() && ppgValues.isNotEmpty()) {
            series.add(
                DataExporter.DataSeries(
                    name = "PPG",
                    timestamps = ppgTimestamps,
                    values = ppgValues,
                    unit = "arbitrary_units",
                    metadata =
                        mapOf(
                            "sensorType" to "Shimmer3",
                            "channel" to "A12",
                        ),
                ),
            )
        }

        return series
    }

    private fun createVideoData(): List<DataExporter.VideoData> {
        // For thermal frames, we would typically save them as a video file
        // For this example, we'll just create metadata
        return if (collectedFrames.isNotEmpty()) {
            listOf(
                DataExporter.VideoData(
                    name = "ThermalVideo",
                    filePath = "thermal_recording.mp4",
                    frameRate = 9.0,
                    duration = collectedFrames.size * 111L, // ~9 FPS
                    resolution = Pair(160, 120),
                    metadata =
                        mapOf(
                            "cameraType" to "Topdon TC001",
                            "frameCount" to collectedFrames.size,
                        ),
                ),
            )
        } else {
            emptyList()
        }
    }

    private fun exportToMatlab(
        dataset: DataExporter.ExportDataset,
        exportDir: File,
    ) {
        val matlabExporter = MatlabExporter()
        val matlabFile = File(exportDir, "integration_test_${System.currentTimeMillis()}.mat")

        val config =
            DataExporter.ExportConfig(
                includeRawData = true,
                includeMetadata = true,
                compressionLevel = 6,
            )

        val result = matlabExporter.exportData(dataset, matlabFile, config)

        if (result.success) {
            Log.d(TAG, "MATLAB export successful: ${matlabFile.absolutePath} (${result.fileSize} bytes)")
        } else {
            Log.e(TAG, "MATLAB export failed: ${result.errorMessage}")
        }
    }

    private fun exportToHdf5(
        dataset: DataExporter.ExportDataset,
        exportDir: File,
    ) {
        val hdf5Exporter = Hdf5Exporter()
        val hdf5File = File(exportDir, "integration_test_${System.currentTimeMillis()}.h5")

        val config =
            DataExporter.ExportConfig(
                includeRawData = true,
                includeMetadata = true,
                compressionLevel = 6,
                customOptions =
                    mapOf(
                        "chunkSize" to 32768,
                        "useCompression" to true,
                    ),
            )

        val result = hdf5Exporter.exportData(dataset, hdf5File, config)

        if (result.success) {
            Log.d(TAG, "HDF5 export successful: ${hdf5File.absolutePath} (${result.fileSize} bytes)")
        } else {
            Log.e(TAG, "HDF5 export failed: ${result.errorMessage}")
        }
    }

    // Mock implementations for demonstration

    private fun createMockAuthProvider(): AuthenticationProvider =
        object : AuthenticationProvider {
            override fun authenticate(credentials: AuthenticationProvider.Credentials): AuthenticationProvider.AuthenticationResult =
                AuthenticationProvider.AuthenticationResult(
                    isAuthenticated = true,
                    sessionToken = "mock_session_${System.currentTimeMillis()}",
                    expirationTime = System.currentTimeMillis() + 3600000, // 1 hour
                )

            override fun validateSession(
                sessionToken: String,
                deviceId: String,
            ): Boolean = true

            override fun generateSessionToken(deviceId: String): String = "mock_token_$deviceId"

            override fun revokeSession(
                sessionToken: String,
                deviceId: String,
            ) {}

            override fun isDeviceAuthorized(deviceId: String): Boolean = true

            override fun authorizeDevice(
                deviceId: String,
                deviceName: String,
            ): Boolean = true

            override fun deauthorizeDevice(deviceId: String): Boolean = true

            override fun getAuthorizedDevices(): Map<String, String> = mapOf("device1" to "Test Device")

            override fun getAuthenticationMethod(): String = "Mock Authentication"
        }

    private fun createMockEncryptionProvider(): EncryptionProvider {
        return object : EncryptionProvider {
            override fun generateKeyPair(): EncryptionProvider.KeyPair {
                // Generate actual RSA key pair for mock implementation
                try {
                    val keyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
                    keyPairGenerator.initialize(2048)
                    val keyPair = keyPairGenerator.generateKeyPair()

                    return EncryptionProvider.KeyPair(
                        publicKey = keyPair.public,
                        privateKey = keyPair.private
                    )
                } catch (e: Exception) {
                    // Fallback to mock keys if generation fails
                    Log.w(TAG, "Failed to generate real key pair, using mock keys: ${e.message}")

                    // Create mock keys for testing purposes
                    val mockKeyPairGenerator = java.security.KeyPairGenerator.getInstance("RSA")
                    mockKeyPairGenerator.initialize(1024) // Smaller key size for mock
                    val mockKeyPair = mockKeyPairGenerator.generateKeyPair()

                    return EncryptionProvider.KeyPair(
                        publicKey = mockKeyPair.public,
                        privateKey = mockKeyPair.private
                    )
                }
            }

            override fun encryptSymmetric(
                data: ByteArray,
                key: ByteArray,
            ): EncryptionProvider.EncryptedData {
                return EncryptionProvider.EncryptedData(data) // Mock - no actual encryption
            }

            override fun decryptSymmetric(
                encryptedData: EncryptionProvider.EncryptedData,
                key: ByteArray,
            ): ByteArray {
                return encryptedData.data // Mock - no actual decryption
            }

            override fun encryptAsymmetric(
                data: ByteArray,
                publicKey: java.security.PublicKey,
            ): EncryptionProvider.EncryptedData {
                return EncryptionProvider.EncryptedData(data) // Mock
            }

            override fun decryptAsymmetric(
                encryptedData: EncryptionProvider.EncryptedData,
                privateKey: java.security.PrivateKey,
            ): ByteArray {
                return encryptedData.data // Mock
            }

            override fun generateSymmetricKey(keySize: Int): ByteArray = ByteArray(keySize / 8)

            override fun deriveKeyFromPassword(
                password: String,
                salt: ByteArray,
                iterations: Int,
                keyLength: Int,
            ): ByteArray = ByteArray(keyLength / 8)

            override fun generateSalt(length: Int): ByteArray = ByteArray(length)

            override fun createHash(data: ByteArray): ByteArray = data.copyOf(32) // Mock hash

            override fun verifyHash(
                data: ByteArray,
                hash: ByteArray,
            ): Boolean = true

            override fun getAlgorithmName(): String = "Mock Encryption"

            override fun getMaxAsymmetricDataSize(): Int = 245
        }
    }
}
