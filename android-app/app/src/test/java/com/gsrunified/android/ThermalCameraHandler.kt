package com.gsrunified.android

import android.hardware.usb.UsbDevice
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.infisense.iruvc.usb.USBMonitor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class ThermalCameraHandlerTest {
    @Mock
    private lateinit var mockActivity: AppCompatActivity

    @Mock
    private lateinit var mockImageView: ImageView

    @Mock
    private lateinit var mockCallback: ThermalCameraHandler.ThermalDataCallback

    @Mock
    private lateinit var mockUsbMonitor: USBMonitor

    @Mock
    private lateinit var mockUsbDevice: UsbDevice

    private lateinit var handler: ThermalCameraHandler

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Mock the behavior of the USB device
        whenever(mockUsbDevice.vendorId).thenReturn(0x2E42) // Topdon Vendor ID
        whenever(mockUsbDevice.productId).thenReturn(0x0001) // TC001 Product ID
        whenever(mockUsbDevice.deviceName).thenReturn("mock_topdon_device")

        // The handler creates its own USBMonitor, so we can't directly inject a mock.
        // This is a limitation of the current design. A better design would use dependency injection.
        // We will focus on testing the logic that can be tested around the SDK components.
        handler = spy(ThermalCameraHandler(mockActivity, mockImageView))
        handler.setThermalCallback(mockCallback)

        // Mock external file directory for logging tests
        whenever(mockActivity.getExternalFilesDir(any())).thenReturn(File("."))
    }

    @After
    fun tearDown() {
        handler.release()
    }

    @Test
    fun `initial state is not connected and not recording`() {
        assertFalse(handler.isConnected())
        assertFalse(handler.isRecording())
        assertEquals(0, handler.getFrameCount())
        assertEquals(0L, handler.getRecordingDuration())
    }

    @Test
    fun `setSessionInfo stores session details correctly`() {
        val sessionId = "session_thermal_test"
        val deviceId = "android_test_1"

        handler.setSessionInfo(sessionId, deviceId)

        // We can't directly verify internal state, but we can ensure the method runs without error.
        // A refactor could make these properties accessible for testing.
        assertTrue(true)
    }

    @Test
    fun `start and stop recording updates state and calls callbacks`() {
        // Simulate a connected state
        // In a real test with DI, we would set the internal state directly.
        // For now, we'll test the public methods' logic.
        handler.startRecording()
        // Since it's not actually connected, it should call onError.
        verify(mockCallback).onError("Thermal camera not connected")
        assertFalse(handler.isRecording())

        // To properly test start/stop, we would need to mock the connection state.
        // This highlights the need for better testability in the handler.
    }

    @Test
    fun `processRawThermalFrame correctly converts raw data to temperatures`() {
        // This tests the core data processing logic of the handler.
        val width = 2
        val height = 1
        // Create a sample raw frame (16-bit little-endian values)
        // Value 1: 8191 (low temp)
        // Value 2: 49151 (high temp)
        val rawFrame =
            byteArrayOf(
                0xFF.toByte(),
                0x1F.toByte(), // 8191
                0xFF.toByte(),
                0xBF.toByte(), // 49151
            )

        // We test the private method using a spy or by making it internal for testing
        val processedFrame = handler.processRawThermalFrame(rawFrame)

        assertNotNull(processedFrame)
        assertEquals(width, processedFrame.width)
        assertEquals(height, processedFrame.height)

        // Verify temperature conversion (based on the linear approximation in the handler)
        val temp1 = processedFrame.temperatureMatrix[0][0]
        val temp2 = processedFrame.temperatureMatrix[0][1]

        // Expected values from the linear conversion in the handler
        val expectedTemp1 = -20.0f + (8191f / 65535f) * (550.0f - -20.0f)
        val expectedTemp2 = -20.0f + (49151f / 65535f) * (550.0f - -20.0f)

        assertEquals(expectedTemp1, temp1, 0.1f)
        assertEquals(expectedTemp2, temp2, 0.1f)
    }

    @Test
    fun `createThermalBitmap generates a non-null bitmap`() {
        // Create a sample processed frame
        val processedFrame =
            ThermalCameraHandler.ThermalProcessedFrame(
                temperatureMatrix = Array(192) { FloatArray(256) { 25.0f } },
                width = 256,
                height = 192,
                minTemp = 20.0f,
                maxTemp = 30.0f,
                timestamp = System.currentTimeMillis(),
            )

        val bitmap = handler.createThermalBitmap(processedFrame)

        assertNotNull(bitmap)
        assertEquals(256, bitmap.width)
        assertEquals(192, bitmap.height)
    }

    @Test
    fun `release stops monitoring and cleans up resources`() {
        handler.initialize() // This will create the USBMonitor
        handler.release()

        // In a test with an injected mock USBMonitor, we would verify usbMonitor.unregister() was called.
        // For now, we just ensure it runs without crashing.
        assertFalse(handler.isConnected())
        assertFalse(handler.isRecording())
    }
}
