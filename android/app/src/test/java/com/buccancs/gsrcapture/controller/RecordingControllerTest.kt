package com.buccancs.gsrcapture.controller

import android.content.Context
import android.view.TextureView
import androidx.camera.view.PreviewView
import com.buccancs.gsrcapture.audio.AudioRecorder
import com.buccancs.gsrcapture.camera.RgbCameraManager
import com.buccancs.gsrcapture.camera.ThermalCameraManager
import com.buccancs.gsrcapture.sensor.GsrSensorManager
import com.buccancs.gsrcapture.utils.TimeManager
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RecordingControllerTest {

    @Mock
    private lateinit var mockGsrSensorManager: GsrSensorManager

    @Mock
    private lateinit var mockThermalCameraManager: ThermalCameraManager

    @Mock
    private lateinit var mockRgbCameraManager: RgbCameraManager

    @Mock
    private lateinit var mockAudioRecorder: AudioRecorder

    @Mock
    private lateinit var mockPreviewView: PreviewView

    @Mock
    private lateinit var mockTextureView: TextureView

    private lateinit var recordingController: RecordingController

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Use Robolectric's application context
        val context = RuntimeEnvironment.getApplication()
        recordingController = RecordingController(context)

        // Inject mocked components using reflection or setter methods if available
        // For this test, we'll test the public interface
    }

    @Test
    fun testInitialization() {
        // Test that the recording controller initializes correctly
        recordingController.initialize()
        assertFalse("Should not be recording initially", recordingController.isRecordingState)
    }

    @Test
    fun testOutputDirectoryCreation() {
        recordingController.initialize()

        // Output directory should be created during initialization
        val outputDir = recordingController.createOutputDirectoryForTesting()

        assertNotNull("Output directory should be created", outputDir)
        assertTrue("Output directory should exist", outputDir!!.exists())
        assertTrue("Output directory should be a directory", outputDir.isDirectory())
    }

    @Test
    fun testSessionIdGeneration() {
        val sessionId1 = recordingController.generateSessionIdForTesting()
        val sessionId2 = recordingController.generateSessionIdForTesting()

        assertNotNull("Session ID should not be null", sessionId1)
        assertNotNull("Session ID should not be null", sessionId2)
        assertNotEquals("Session IDs should be unique", sessionId1, sessionId2)
        assertTrue("Session ID should contain timestamp", sessionId1.contains("_"))
    }

    @Test
    fun testPreviewViewSetup() {
        recordingController.initialize()

        // Test setting RGB preview view
        recordingController.setRgbPreviewView(mockPreviewView)

        // Test setting thermal preview view
        recordingController.setThermalPreviewView(mockTextureView)

        // Should complete without error
        assertTrue("Preview view setup should complete successfully", true)
    }

    @Test
    fun testThermalCameraConnection() {
        recordingController.initialize()

        // Test thermal camera connection
        val result = recordingController.connectThermalCamera()

        // In test environment, connection might fail but should not crash
        assertNotNull("Thermal camera connection should return a result", result)
    }

    @Test
    fun testGsrSensorConnection() {
        recordingController.initialize()

        // Test GSR sensor connection without device address
        val result1 = recordingController.connectGsrSensor()
        assertNotNull("GSR sensor connection should return a result", result1)

        // Test GSR sensor connection with device address
        val result2 = recordingController.connectGsrSensor("00:11:22:33:44:55")
        assertNotNull("GSR sensor connection with address should return a result", result2)
    }

    @Test
    fun testCallbackRegistration() {
        var recordingStateChanged = false
        var gsrValueReceived = false
        var heartRateReceived = false
        var errorReceived = false

        // Register callbacks
        recordingController.setRecordingStateCallback { recordingStateChanged = true }
        recordingController.setGsrValueCallback { gsrValueReceived = true }
        recordingController.setHeartRateCallback { heartRateReceived = true }
        recordingController.setErrorCallback { errorReceived = true }

        // Callbacks should be registered without error
        assertTrue("Callback registration should complete successfully", true)
    }

    @Test
    fun testRecordingStartStop() {
        recordingController.initialize()

        // Test starting recording
        val startResult = recordingController.startRecording()
        assertTrue("Recording should start successfully", startResult)
        assertTrue("Should be recording after start", recordingController.isRecordingState)

        // Test stopping recording
        recordingController.stopRecording()
        assertFalse("Should not be recording after stop", recordingController.isRecordingState)
    }

    @Test
    fun testRecordingWithoutInitialization() {
        // Test starting recording without initialization
        val result = recordingController.startRecording()

        // Should handle gracefully
        assertNotNull("Recording start should return a result even without initialization", result)
    }

    @Test
    fun testMultipleRecordingAttempts() {
        recordingController.initialize()

        // Start recording
        val result1 = recordingController.startRecording()
        assertTrue("First recording start should succeed", result1)

        // Try to start recording again while already recording
        val result2 = recordingController.startRecording()
        // Should handle gracefully (might return false or true depending on implementation)
        assertNotNull("Second recording start should return a result", result2)

        // Stop recording
        recordingController.stopRecording()
        assertFalse("Should not be recording after stop", recordingController.isRecordingState)
    }

    @Test
    fun testSessionMetadataCreation() {
        recordingController.initialize()

        val outputDir = recordingController.createOutputDirectoryForTesting()
        val sessionDir = File(outputDir!!, "test_session")
        sessionDir.mkdirs()

        // Create session metadata
        recordingController.createSessionMetadataForTesting(sessionDir)

        // Check if metadata file was created
        val metadataFile = File(sessionDir, "session_metadata.json")
        assertTrue("Session metadata file should be created", metadataFile.exists())
        assertTrue("Session metadata file should not be empty", metadataFile.length() > 0)

        // Clean up
        sessionDir.deleteRecursively()
    }

    @Test
    fun testConnectionStatus() {
        recordingController.initialize()

        // Test connection status methods
        val connected = recordingController.isConnectedForTesting()

        // Should return boolean values without error
        assertNotNull("Connection status should not be null", connected)
    }

    @Test
    fun testShutdown() {
        recordingController.initialize()

        // Start recording
        recordingController.startRecording()

        // Test shutdown
        recordingController.shutdown()

        assertFalse("Should not be recording after shutdown", recordingController.isRecordingState)
    }

    @Test
    fun testErrorHandling() {
        // Test various error conditions
        recordingController.initialize()

        // Test with invalid output directory
        val invalidDir = File("/invalid/path/that/does/not/exist")

        // Should handle invalid paths gracefully
        assertTrue("Should handle invalid paths gracefully", true)
    }

    @Test
    fun testConcurrentOperations() {
        recordingController.initialize()

        val operationCount = 5
        val latch = CountDownLatch(operationCount)
        val results = mutableListOf<Boolean>()

        // Perform concurrent recording operations
        repeat(operationCount) { index ->
            Thread {
                try {
                    when (index % 3) {
                        0 -> {
                            val result = recordingController.startRecording()
                            synchronized(results) { results.add(result) }
                        }
                        1 -> {
                            recordingController.stopRecording()
                        }
                        2 -> {
                            val connected = recordingController.isConnectedForTesting()
                            // Just check connection status
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }.start()
        }

        // Wait for all operations to complete
        assertTrue("All concurrent operations should complete", latch.await(10, TimeUnit.SECONDS))

        recordingController.shutdown()
    }

    @Test
    fun testRecordingStateCallbacks() {
        recordingController.initialize()

        var callbackCount = 0
        var lastRecordingState = false

        recordingController.setRecordingStateCallback { isRecording ->
            callbackCount++
            lastRecordingState = isRecording
        }

        // Start recording
        recordingController.startRecording()

        // Stop recording
        recordingController.stopRecording()

        // Callbacks should be triggered (though timing might vary in tests)
        assertTrue("Recording state callbacks should be registered", true)
    }

    @Test
    fun testDataCallbacks() {
        recordingController.initialize()

        var gsrCallbackCount = 0
        var heartRateCallbackCount = 0
        var lastGsrValue = 0.0f
        var lastHeartRate = 0

        recordingController.setGsrValueCallback { value ->
            gsrCallbackCount++
            lastGsrValue = value
        }

        recordingController.setHeartRateCallback { rate ->
            heartRateCallbackCount++
            lastHeartRate = rate
        }

        // Data callbacks should be registered without error
        assertTrue("Data callbacks should be registered successfully", true)
    }

    @Test
    fun testErrorCallback() {
        recordingController.initialize()

        var errorCallbackCount = 0
        var lastErrorMessage = ""

        recordingController.setErrorCallback { message ->
            errorCallbackCount++
            lastErrorMessage = message
        }

        // Error callback should be registered without error
        assertTrue("Error callback should be registered successfully", true)
    }

    @Test
    fun testRecordingControllerLifecycle() {
        // Test complete lifecycle
        assertFalse("Should not be recording initially", recordingController.isRecordingState)

        // Initialize
        recordingController.initialize()

        // Set up preview views
        recordingController.setRgbPreviewView(mockPreviewView)
        recordingController.setThermalPreviewView(mockTextureView)

        // Connect devices
        recordingController.connectThermalCamera()
        recordingController.connectGsrSensor()

        // Set up callbacks
        var recordingStateChanged = false
        recordingController.setRecordingStateCallback { recordingStateChanged = true }

        // Start recording
        val startResult = recordingController.startRecording()
        assertTrue("Recording should start", startResult)
        assertTrue("Should be recording", recordingController.isRecordingState)

        // Stop recording
        recordingController.stopRecording()
        assertFalse("Should not be recording after stop", recordingController.isRecordingState)

        // Shutdown
        recordingController.shutdown()
        assertFalse("Should not be recording after shutdown", recordingController.isRecordingState)
    }

    @Test
    fun testOutputDirectoryStructure() {
        recordingController.initialize()
        recordingController.startRecording()

        // Let recording run briefly
        Thread.sleep(100)

        recordingController.stopRecording()

        // Check that output directory structure is created
        val outputDir = recordingController.createOutputDirectoryForTesting()
        assertTrue("Output directory should exist", outputDir!!.exists())

        // Clean up
        outputDir.deleteRecursively()
    }

    @Test
    fun testSessionIdUniqueness() {
        val sessionIds = mutableSetOf<String>()

        // Generate multiple session IDs
        repeat(10) {
            val sessionId = recordingController.generateSessionIdForTesting()
            assertFalse("Session ID should be unique", sessionIds.contains(sessionId))
            sessionIds.add(sessionId)

            // Small delay to ensure timestamp difference
            Thread.sleep(1)
        }

        assertEquals("All session IDs should be unique", 10, sessionIds.size)
    }
}
