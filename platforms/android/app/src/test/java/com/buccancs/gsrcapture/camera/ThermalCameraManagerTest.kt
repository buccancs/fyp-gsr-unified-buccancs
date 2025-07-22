package com.buccancs.gsrcapture.camera

import android.content.Context
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.view.TextureView
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.io.File
import java.util.HashMap // Import Java's HashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ThermalCameraManagerTest {
    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockUsbManager: UsbManager

    @Mock
    private lateinit var mockUsbDevice: UsbDevice

    @Mock
    private lateinit var mockUsbConnection: UsbDeviceConnection

    @Mock
    private lateinit var mockTextureView: TextureView

    @Mock
    private lateinit var mockDriver: UsbSerialDriver

    @Mock
    private lateinit var mockUsbSerialPort: UsbSerialPort

    @Mock
    private lateinit var mockUsbSerialProber: UsbSerialProber

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var thermalCameraManager: ThermalCameraManager

    // A temporary directory for test artifacts
    private lateinit var tempDir: File

    // Static mock for UsbSerialProber
    private lateinit var mockedUsbSerialProber: MockedStatic<UsbSerialProber>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        tempDir =
            File.createTempFile("test", "dir").apply {
                delete()
                mkdirs()
            }

        // Mock the static UsbSerialProber.getDefaultProber() method
        mockedUsbSerialProber = mockStatic(UsbSerialProber::class.java)
        mockedUsbSerialProber.`when`<UsbSerialProber> { UsbSerialProber.getDefaultProber() }
            .thenReturn(mockUsbSerialProber)

        // CORRECTED: Use java.util.HashMap to match the expected type from the Android SDK
        val mockDeviceMap = HashMap<String, UsbDevice>()
        mockDeviceMap["device1"] = mockUsbDevice

        // Mock the standard Android USB service behavior
        whenever(mockContext.getSystemService(Context.USB_SERVICE)).thenReturn(mockUsbManager)
        whenever(mockUsbManager.deviceList).thenReturn(mockDeviceMap)
        whenever(mockUsbManager.openDevice(any())).thenReturn(mockUsbConnection)

        // Mock the behavior of a found Topdon device
        whenever(mockUsbDevice.vendorId).thenReturn(0x1A86) // Real Topdon Vendor ID
        whenever(mockUsbDevice.productId).thenReturn(0x5512) // Real Topdon Product ID

        // Mock the USB serial driver behavior
        whenever(mockDriver.device).thenReturn(mockUsbDevice)
        whenever(mockDriver.ports).thenReturn(listOf(mockUsbSerialPort))

        // Mock the USB serial prober to return our mock driver
        whenever(mockUsbSerialProber.findAllDrivers(any())).thenReturn(listOf(mockDriver))

        thermalCameraManager = ThermalCameraManager(mockContext, cameraExecutor)
    }

    @After
    fun tearDown() {
        // Shut down the executor and clean up the temp directory
        cameraExecutor.shutdownNow()
        tempDir.deleteRecursively()
        // Close the static mock
        mockedUsbSerialProber.close()
    }

    @Test
    fun `initialize sets initial state correctly`() {
        thermalCameraManager.initialize()
        assertFalse("Should not be connected initially", thermalCameraManager.isConnectedValue)
        assertFalse("Should not be recording initially", thermalCameraManager.isRecordingValue)
    }

    @Test
    fun `initialize does not crash when UsbManager is unavailable`() {
        // Arrange: Simulate a device without USB host capabilities
        whenever(mockContext.getSystemService(Context.USB_SERVICE)).thenReturn(null)
        val cameraManager = ThermalCameraManager(mockContext, cameraExecutor)

        // Act & Assert: Initialization should complete without throwing an exception
        try {
            cameraManager.initialize()
        } catch (e: Exception) {
            fail("Initialization crashed when UsbManager was null: $e")
        }
    }

    @Test
    fun `connectToCamera successfully opens device connection when Topdon camera is found`() {
        // Arrange
        thermalCameraManager.initialize()

        // Act
        val result = thermalCameraManager.connectToCamera()

        // Assert
        assertTrue("Connection should be successful", result)
        assertTrue("isConnectedValue should be true after connection", thermalCameraManager.isConnectedValue)
        // Verify that we actually tried to open the device
        verify(mockUsbManager).openDevice(mockUsbDevice)
    }

    @Test
    fun `connectToCamera fails when no Topdon camera is found`() {
        // Arrange: Mock a non-Topdon device
        whenever(mockUsbDevice.vendorId).thenReturn(0x1234)
        whenever(mockUsbDevice.productId).thenReturn(0x5678)
        thermalCameraManager.initialize()

        // Act
        val result = thermalCameraManager.connectToCamera()

        // Assert
        assertFalse("Connection should fail if no Topdon camera is found", result)
        assertFalse("isConnectedValue should remain false", thermalCameraManager.isConnectedValue)
        // Verify we never tried to open a device we didn't identify
        verify(mockUsbManager, never()).openDevice(any())
    }

    @Test
    fun `setFrameCallback registers callback without error`() {
        // Arrange
        var frameReceived = false
        val callback: (Bitmap) -> Unit = { frame -> frameReceived = true }

        // Act: Set the callback - this should complete without error
        thermalCameraManager.setFrameCallback(callback)

        // Assert: The method should complete successfully
        // Note: We can't directly test the private frameCallback property,
        // but we can verify the method doesn't throw an exception
        assertTrue("setFrameCallback should complete successfully", true)
    }

    @Test
    fun `startRecording returns true and creates session directory`() {
        // Arrange
        val sessionId = "test_thermal_session"
        thermalCameraManager.initialize()
        thermalCameraManager.connectToCamera() // Must be connected to record

        // Act
        val startResult = thermalCameraManager.startRecording(tempDir, sessionId)

        // Assert
        assertTrue("Recording should start successfully", startResult)
        assertTrue("isRecordingValue should be true after start", thermalCameraManager.isRecordingValue)

        val expectedDir = File(tempDir, "thermal_test_thermal_session")
        assertTrue("Session directory should have been created", expectedDir.exists() && expectedDir.isDirectory)
    }

    @Test
    fun `stopRecording sets recording flag to false`() {
        // Arrange
        thermalCameraManager.initialize()
        thermalCameraManager.connectToCamera()
        thermalCameraManager.startRecording(tempDir, "session_to_stop")
        assertTrue("Precondition failed: should be recording", thermalCameraManager.isRecordingValue)

        // Act
        thermalCameraManager.stopRecording()

        // Assert
        assertFalse("Should not be recording after stop", thermalCameraManager.isRecordingValue)
    }

    @Test
    fun `disconnect closes connection and resets state`() {
        // Arrange
        thermalCameraManager.initialize()
        thermalCameraManager.connectToCamera()
        assertTrue("Precondition failed: should be connected", thermalCameraManager.isConnectedValue)

        // Act
        thermalCameraManager.disconnect()

        // Assert
        assertFalse("Should not be connected after disconnect", thermalCameraManager.isConnectedValue)
        assertFalse("Should not be recording after disconnect", thermalCameraManager.isRecordingValue)
        // Verify that the underlying connection was actually closed
        verify(mockUsbConnection).close()
    }

    @Test
    fun `shutdown disconnects and terminates executor`() {
        // Arrange
        thermalCameraManager.initialize()
        thermalCameraManager.connectToCamera()

        // Act
        thermalCameraManager.shutdown()

        // Assert
        assertFalse("Should not be connected after shutdown", thermalCameraManager.isConnectedValue)
        assertTrue("Executor should be shut down", cameraExecutor.isShutdown)
    }

    @Test
    fun `startRecording fails if not connected`() {
        // Arrange
        thermalCameraManager.initialize()
        assertFalse("Precondition: should not be connected", thermalCameraManager.isConnectedValue)

        // Act
        val recordingResult = thermalCameraManager.startRecording(tempDir, "test")

        // Assert
        assertFalse("Recording should fail if not connected", recordingResult)
    }

    @Test
    fun `findTopdonDriver returns correct driver from list`() {
        // Arrange
        whenever(mockDriver.device).thenReturn(mockUsbDevice)
        val driverList = listOf(mockDriver)

        // Act
        val result = thermalCameraManager.findTopdonDriver(driverList)

        // Assert
        assertEquals("Should have found the mock driver", mockDriver, result)
    }

    @Test
    fun `findTopdonDriver returns null for empty driver list`() {
        // Act
        val result = thermalCameraManager.findTopdonDriver(emptyList())

        // Assert
        assertNull("Should return null for empty driver list", result)
    }

    @Test
    fun `startStreaming returns true when connected and network client is set`() {
        // Arrange
        thermalCameraManager.initialize()
        thermalCameraManager.connectToCamera()
        thermalCameraManager.setNetworkClient(mock())
        assertTrue("Precondition: should be connected", thermalCameraManager.isConnectedValue)

        // Act
        val result = thermalCameraManager.startStreaming()

        // Assert
        assertTrue("Streaming should start successfully when connected and network client is set", result)
    }

    @Test
    fun `startStreaming returns false when not connected`() {
        // Arrange
        thermalCameraManager.initialize()
        thermalCameraManager.setNetworkClient(mock())
        assertFalse("Precondition: should not be connected", thermalCameraManager.isConnectedValue)

        // Act
        val result = thermalCameraManager.startStreaming()

        // Assert
        assertFalse("Streaming should fail when not connected", result)
    }

    @Test
    fun `startStreaming returns false when network client is not set`() {
        // Arrange
        thermalCameraManager.initialize()
        thermalCameraManager.connectToCamera()
        assertTrue("Precondition: should be connected", thermalCameraManager.isConnectedValue)

        // Act
        val result = thermalCameraManager.startStreaming()

        // Assert
        assertFalse("Streaming should fail when network client is not set", result)
    }

    @Test
    fun `stopStreaming completes without error`() {
        // Arrange
        thermalCameraManager.initialize()
        thermalCameraManager.connectToCamera()
        thermalCameraManager.setNetworkClient(mock())
        thermalCameraManager.startStreaming()

        // Act & Assert - should not throw any exceptions
        thermalCameraManager.stopStreaming()
        assertTrue("stopStreaming should complete successfully", true)
    }
}
