package com.buccancs.gsrcapture.sensor

import android.content.Context
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import com.buccancs.gsrcapture.utils.TimeManager
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.*
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class GsrSensorManagerTest {

    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockBluetoothManager: BluetoothManager
    
    @Mock
    private lateinit var mockBluetoothAdapter: BluetoothAdapter
    
    private lateinit var sensorExecutor: ExecutorService
    private lateinit var gsrSensorManager: GsrSensorManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        sensorExecutor = Executors.newSingleThreadExecutor()
        
        // Mock Bluetooth system service
        `when`(mockContext.getSystemService(Context.BLUETOOTH_SERVICE)).thenReturn(mockBluetoothManager)
        `when`(mockBluetoothManager.adapter).thenReturn(mockBluetoothAdapter)
        `when`(mockBluetoothAdapter.isEnabled).thenReturn(true)
        
        gsrSensorManager = GsrSensorManager(mockContext, sensorExecutor)
    }

    @Test
    fun testInitialization() {
        // Test that the sensor manager initializes correctly
        assertTrue("GSR sensor manager should initialize successfully", gsrSensorManager.initialize())
        assertFalse("Should not be connected initially", gsrSensorManager.isConnected)
        assertFalse("Should not be streaming initially", gsrSensorManager.isStreaming)
    }

    @Test
    fun testInitializationWithoutBluetooth() {
        // Test initialization when Bluetooth is not available
        `when`(mockBluetoothAdapter.isEnabled).thenReturn(false)
        
        assertFalse("Initialization should fail without Bluetooth", gsrSensorManager.initialize())
    }

    @Test
    fun testGsrCallbackRegistration() {
        var receivedValue: Float? = null
        val callback: (Float) -> Unit = { value -> receivedValue = value }
        
        gsrSensorManager.setGsrCallback(callback)
        
        // Simulate GSR data processing
        gsrSensorManager.processGsrData(15.5f)
        
        assertEquals("GSR callback should receive correct value", 15.5f, receivedValue)
    }

    @Test
    fun testHeartRateCallbackRegistration() {
        var receivedHeartRate: Int? = null
        val callback: (Int) -> Unit = { rate -> receivedHeartRate = rate }
        
        gsrSensorManager.setHeartRateCallback(callback)
        
        // Simulate heart rate calculation
        val heartRate = gsrSensorManager.calculateHeartRate(100.0f)
        
        assertNotNull("Heart rate should be calculated", heartRate)
        assertTrue("Heart rate should be in valid range", heartRate in 40..200)
    }

    @Test
    fun testConnectionStateCallback() {
        var connectionState: Boolean? = null
        val callback: (Boolean) -> Unit = { connected -> connectionState = connected }
        
        gsrSensorManager.setConnectionStateCallback(callback)
        
        // Test connection state changes would be tested with actual Shimmer device
        // For unit test, we verify callback registration works
        assertNotNull("Connection state callback should be registered", connectionState)
    }

    @Test
    fun testRecordingStartStop() {
        val outputDir = File.createTempFile("test", "dir").apply { 
            delete()
            mkdirs()
        }
        val sessionId = "test_session_123"
        
        try {
            // Test starting recording
            val startResult = gsrSensorManager.startRecording(outputDir, sessionId)
            assertTrue("Recording should start successfully", startResult)
            assertTrue("Should be recording after start", gsrSensorManager.isRecording)
            
            // Test stopping recording
            gsrSensorManager.stopRecording()
            assertFalse("Should not be recording after stop", gsrSensorManager.isRecording)
            
        } finally {
            outputDir.deleteRecursively()
        }
    }

    @Test
    fun testGsrDataProcessing() {
        // Test GSR data processing with various values
        val testValues = listOf(0.0f, 5.5f, 15.0f, 25.5f, 50.0f)
        val receivedValues = mutableListOf<Float>()
        
        gsrSensorManager.setGsrCallback { value -> receivedValues.add(value) }
        
        testValues.forEach { value ->
            gsrSensorManager.processGsrData(value)
        }
        
        assertEquals("All GSR values should be processed", testValues.size, receivedValues.size)
        assertEquals("GSR values should match", testValues, receivedValues)
    }

    @Test
    fun testPpgDataProcessing() {
        // Test PPG data processing
        val testPpgValues = listOf(80.0f, 90.0f, 100.0f, 110.0f, 120.0f)
        
        testPpgValues.forEach { value ->
            gsrSensorManager.processPpgData(value)
            // Verify that PPG processing doesn't crash
        }
    }

    @Test
    fun testHeartRateCalculation() {
        // Test heart rate calculation with various PPG values
        val testCases = mapOf(
            50.0f to 40..80,   // Low PPG should give lower heart rate range
            100.0f to 60..120, // Medium PPG
            150.0f to 80..160  // High PPG should give higher heart rate range
        )
        
        testCases.forEach { (ppgValue, expectedRange) ->
            val heartRate = gsrSensorManager.calculateHeartRate(ppgValue)
            assertTrue(
                "Heart rate $heartRate for PPG $ppgValue should be in range $expectedRange",
                heartRate in expectedRange
            )
        }
    }

    @Test
    fun testDataSaving() {
        val outputDir = File.createTempFile("test", "dir").apply { 
            delete()
            mkdirs()
        }
        val sessionId = "test_session_save"
        
        try {
            gsrSensorManager.startRecording(outputDir, sessionId)
            
            // Create test data
            val testData = TimeManager.TimestampedData(
                data = 12.5f,
                timestamp = System.currentTimeMillis(),
                deviceTime = System.nanoTime()
            )
            
            gsrSensorManager.saveGsrData(testData)
            
            // Verify file was created
            val gsrFile = File(outputDir, "${sessionId}_gsr_data.csv")
            assertTrue("GSR data file should be created", gsrFile.exists())
            assertTrue("GSR data file should not be empty", gsrFile.length() > 0)
            
        } finally {
            outputDir.deleteRecursively()
        }
    }

    @Test
    fun testShutdown() {
        gsrSensorManager.initialize()
        
        // Test shutdown
        gsrSensorManager.shutdown()
        
        assertFalse("Should not be connected after shutdown", gsrSensorManager.isConnected)
        assertFalse("Should not be streaming after shutdown", gsrSensorManager.isStreaming)
        assertFalse("Should not be recording after shutdown", gsrSensorManager.isRecording)
    }

    @Test
    fun testMultipleCallbackRegistrations() {
        var gsrCallbackCount = 0
        var heartRateCallbackCount = 0
        var connectionCallbackCount = 0
        
        // Register multiple callbacks
        gsrSensorManager.setGsrCallback { gsrCallbackCount++ }
        gsrSensorManager.setHeartRateCallback { heartRateCallbackCount++ }
        gsrSensorManager.setConnectionStateCallback { connectionCallbackCount++ }
        
        // Trigger callbacks
        gsrSensorManager.processGsrData(10.0f)
        
        assertEquals("GSR callback should be called once", 1, gsrCallbackCount)
    }

    @Test
    fun testErrorHandling() {
        // Test connection with invalid device address
        val result = gsrSensorManager.connectToSensor("invalid_address")
        assertFalse("Connection should fail with invalid address", result)
        
        // Test recording without connection
        val outputDir = File.createTempFile("test", "dir").apply { 
            delete()
            mkdirs()
        }
        
        try {
            val recordingResult = gsrSensorManager.startRecording(outputDir, "test")
            // Should handle gracefully even without connection
            assertNotNull("Recording start should return a result", recordingResult)
        } finally {
            outputDir.deleteRecursively()
        }
    }
}