package com.gsrunified.android

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.shimmerresearch.android.Shimmer
import com.shimmerresearch.bluetooth.ShimmerBluetooth
import com.shimmerresearch.driver.CallbackObject
import com.shimmerresearch.driver.Configuration
import com.shimmerresearch.driver.ObjectCluster
import com.shimmerresearch.exceptions.ShimmerException
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class GsrHandlerTest {
    @Mock
    private lateinit var mockActivity: AppCompatActivity

    @Mock
    private lateinit var mockTextView: TextView

    @Mock
    private lateinit var mockBluetoothManager: BluetoothManager

    @Mock
    private lateinit var mockBluetoothAdapter: BluetoothAdapter

    @Mock
    private lateinit var mockShimmer: Shimmer

    @Mock
    private lateinit var mockCallback: GsrHandler.GsrDataCallback

    @Captor
    private lateinit var messageCaptor: ArgumentCaptor<Message>

    private lateinit var gsrHandler: GsrHandler
    private lateinit var shimmerHandler: Handler

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Mock Android framework services
        whenever(mockActivity.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE)).thenReturn(mockBluetoothManager)
        whenever(mockBluetoothManager.adapter).thenReturn(mockBluetoothAdapter)
        whenever(mockBluetoothAdapter.isEnabled).thenReturn(true)
        // Mock the file system for Robolectric
        whenever(mockActivity.getExternalFilesDir(any())).thenReturn(File("./test_output/"))
        File("./test_output/").mkdirs()

        // With dependency injection, we can now pass our mock Shimmer object directly.
        gsrHandler = GsrHandler(mockActivity, mockTextView, mockShimmer)
        gsrHandler.setGsrCallback(mockCallback)

        // Use reflection to get the internal handler for testing message processing
        val handlerField = GsrHandler::class.java.getDeclaredField("shimmerHandler")
        handlerField.isAccessible = true
        shimmerHandler = handlerField.get(gsrHandler) as Handler
    }

    @After
    fun tearDown() {
        gsrHandler.release()
        File("./test_output/").deleteRecursively()
    }

    // --- Existing tests are still valid ---
    @Test
    fun `initialization sets up bluetooth and shimmer`() {
        gsrHandler.initialize()
        // Verify that Bluetooth checks are performed
        verify(mockBluetoothManager).adapter
        verify(mockBluetoothAdapter).isEnabled
    }

    @Test
    fun `startStreaming when not connected does not start and calls error callback`() {
        gsrHandler.startStreaming()
        assertFalse(gsrHandler.isStreaming())
        verify(mockCallback).onError("Device not connected")
    }

    // --- NEW AND IMPROVED TESTS ---

    @Test
    fun `connectToDevice when shimmer throws exception calls onError callback`() {
        val testAddress = "00:11:22:33:44:55"
        val exceptionMessage = "Connection failed"
        // Configure the mock Shimmer to throw an exception when connect is called
        whenever(mockShimmer.connect(any(), any())).thenThrow(ShimmerException(exceptionMessage))

        gsrHandler.connectToDevice(testAddress)

        // Verify that the onError callback was triggered with the correct message
        verify(mockCallback).onError("Failed to connect to device: $exceptionMessage")
    }

    @Test
    fun `handleGsrData updates UI with correctly formatted text`() {
        // Capture the Runnable passed to runOnUiThread
        val runnableCaptor = argumentCaptor<Runnable>()
        val conductance = 1.2345
        val resistance = 1.0 / conductance

        // Simulate receiving data
        val mockObjectCluster = mock<ObjectCluster>()
        val mockFormatCluster = mock<com.shimmerresearch.driver.FormatCluster>()
        whenever(mockObjectCluster.getCollectionOfFormatClusters(any())).thenReturn(listOf(mockFormatCluster))
        whenever(mockFormatCluster.mData).thenReturn(conductance)
        whenever(mockFormatCluster.mFormat).thenReturn("CAL")

        val message =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET
                obj = mockObjectCluster
            }
        shimmerHandler.handleMessage(message)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify runOnUiThread was called and capture the Runnable
        verify(mockActivity).runOnUiThread(runnableCaptor.capture())
        // Execute the captured Runnable to perform the UI update
        runnableCaptor.firstValue.run()

        // Verify that setText was called on the TextView with the correctly formatted string
        val expectedText = String.format("GSR: %.2f μS (%.2f kΩ)", conductance, resistance)
        verify(mockTextView).text = expectedText
    }

    @Test
    fun `handleGsrData should write correctly formatted data to CSV`() {
        // 1. Setup the session and start logging
        val sessionId = "session_test_123"
        val deviceId = "test_device_abc"
        gsrHandler.setSessionInfo(sessionId, deviceId)

        // Manually set connected state for the test
        val connectMessage =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE
                obj = CallbackObject(ShimmerBluetooth.BT_STATE.CONNECTED, "00:11:22:33:44:55", "")
            }
        shimmerHandler.handleMessage(connectMessage)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Start streaming, which creates the writer
        gsrHandler.startStreaming()

        // 2. Simulate receiving data
        val conductance = 1.23
        val resistance = 1.0 / conductance
        val shimmerTimestamp = 12345.67
        System.currentTimeMillis()

        val mockObjectCluster = mock<ObjectCluster>()
        val mockGsrFormat = mock<com.shimmerresearch.driver.FormatCluster>()
        val mockTsFormat = mock<com.shimmerresearch.driver.FormatCluster>()
        whenever(
            mockObjectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE),
        ).thenReturn(listOf(mockGsrFormat))
        whenever(
            mockObjectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP),
        ).thenReturn(listOf(mockTsFormat))
        whenever(mockGsrFormat.mData).thenReturn(conductance)
        whenever(mockGsrFormat.mFormat).thenReturn("CAL")
        whenever(mockTsFormat.mData).thenReturn(shimmerTimestamp)
        whenever(mockTsFormat.mFormat).thenReturn("CAL")

        val message =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET
                obj = mockObjectCluster
            }
        shimmerHandler.handleMessage(message)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // 3. Stop logging to flush and close the file
        gsrHandler.stopStreaming()

        // 4. Verify the file content
        val expectedFileName = "${sessionId}_${deviceId}_gsr_data.csv"
        val file = File("./test_output/", expectedFileName)
        assertTrue("CSV file should exist", file.exists())

        val lines = file.readLines()
        assertTrue("CSV should have a header and at least one data line", lines.size >= 2)
        assertEquals("CSV header is incorrect", "timestamp,conductance_us,resistance_kohm,system_time_ms", lines[0])

        // Check the data line (the second line)
        val dataParts = lines[1].split(",")
        assertEquals("Shimmer timestamp is incorrect in CSV", shimmerTimestamp.toString(), dataParts[0])
        assertEquals("Conductance value is incorrect in CSV", conductance.toString(), dataParts[1])
        assertEquals("Resistance value is incorrect in CSV", resistance.toString(), dataParts[2])
        // We can't check the exact system timestamp, but we can check it's a valid long
        assertTrue("System timestamp should be a valid long", dataParts[3].toLongOrNull() != null)
    }

    // --- ADDITIONAL COMPREHENSIVE TESTS ---

    @Test
    fun `initialization when bluetooth not supported calls onError callback`() {
        // Mock Bluetooth as not supported
        whenever(mockActivity.getSystemService(AppCompatActivity.BLUETOOTH_SERVICE)).thenReturn(mockBluetoothManager)
        whenever(mockBluetoothManager.adapter).thenReturn(null)

        gsrHandler.initialize()

        verify(mockCallback).onError("Bluetooth not supported")
    }

    @Test
    fun `initialization when bluetooth not enabled calls onError callback`() {
        // Mock Bluetooth as disabled
        whenever(mockBluetoothAdapter.isEnabled).thenReturn(false)

        gsrHandler.initialize()

        verify(mockCallback).onError("Bluetooth not enabled")
    }

    @Test
    fun `successful connection updates state and notifies callback`() {
        val macAddress = "00:11:22:33:44:55"
        val connectMessage =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE
                obj = CallbackObject(ShimmerBluetooth.BT_STATE.CONNECTED, macAddress, "")
            }

        shimmerHandler.handleMessage(connectMessage)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertTrue("Handler should report connected state", gsrHandler.isConnected())
        verify(mockCallback).onConnectionStateChanged(true, macAddress)
    }

    @Test
    fun `disconnection updates state and stops streaming`() {
        // First connect
        val macAddress = "00:11:22:33:44:55"
        val connectMessage =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE
                obj = CallbackObject(ShimmerBluetooth.BT_STATE.CONNECTED, macAddress, "")
            }
        shimmerHandler.handleMessage(connectMessage)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Start streaming
        gsrHandler.startStreaming()
        assertTrue("Should be streaming", gsrHandler.isStreaming())

        // Now disconnect
        val disconnectMessage =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE
                obj = CallbackObject(ShimmerBluetooth.BT_STATE.DISCONNECTED, macAddress, "")
            }
        shimmerHandler.handleMessage(disconnectMessage)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertFalse("Should not be connected", gsrHandler.isConnected())
        assertFalse("Should not be streaming", gsrHandler.isStreaming())
        verify(mockCallback).onConnectionStateChanged(false, macAddress)
    }

    @Test
    fun `connecting state is handled correctly`() {
        val macAddress = "00:11:22:33:44:55"
        val connectingMessage =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE
                obj = CallbackObject(ShimmerBluetooth.BT_STATE.CONNECTING, macAddress, "")
            }

        shimmerHandler.handleMessage(connectingMessage)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Should not be connected yet
        assertFalse("Should not be connected while connecting", gsrHandler.isConnected())
    }

    @Test
    fun `startStreaming when connected configures sensor and starts streaming`() {
        // First connect
        val connectMessage =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE
                obj = CallbackObject(ShimmerBluetooth.BT_STATE.CONNECTED, "00:11:22:33:44:55", "")
            }
        shimmerHandler.handleMessage(connectMessage)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        gsrHandler.startStreaming()

        assertTrue("Should be streaming", gsrHandler.isStreaming())
        verify(mockShimmer).setSensorEnabledState(Configuration.Shimmer3.SENSOR_ID.SHIMMER_GSR, true)
        verify(mockShimmer).setSamplingRateShimmer(128.0)
        verify(mockShimmer).startStreaming()
        verify(mockCallback).onStreamingStateChanged(true)
    }

    @Test
    fun `stopStreaming stops shimmer and updates state`() {
        // First connect and start streaming
        val connectMessage =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE
                obj = CallbackObject(ShimmerBluetooth.BT_STATE.CONNECTED, "00:11:22:33:44:55", "")
            }
        shimmerHandler.handleMessage(connectMessage)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        gsrHandler.startStreaming()

        gsrHandler.stopStreaming()

        assertFalse("Should not be streaming", gsrHandler.isStreaming())
        verify(mockShimmer).stopStreaming()
        verify(mockCallback).onStreamingStateChanged(false)
    }

    @Test
    fun `handleGsrData with missing conductance data handles gracefully`() {
        val mockObjectCluster = mock<ObjectCluster>()
        // Return empty list for GSR conductance (missing data)
        whenever(mockObjectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE))
            .thenReturn(emptyList())
        whenever(mockObjectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP))
            .thenReturn(emptyList())

        val message =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET
                obj = mockObjectCluster
            }

        // Should not throw exception
        shimmerHandler.handleMessage(message)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Verify callback is still called with default values
        verify(mockCallback).onGsrDataReceived(eq(0.0), eq(0.0), any())
    }

    @Test
    fun `handleGsrData with zero conductance calculates resistance as zero`() {
        val mockObjectCluster = mock<ObjectCluster>()
        val mockGsrFormat = mock<com.shimmerresearch.driver.FormatCluster>()
        whenever(mockObjectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.GSR_CONDUCTANCE))
            .thenReturn(listOf(mockGsrFormat))
        whenever(mockObjectCluster.getCollectionOfFormatClusters(Configuration.Shimmer3.ObjectClusterSensorName.TIMESTAMP))
            .thenReturn(emptyList())
        whenever(mockGsrFormat.mData).thenReturn(0.0) // Zero conductance
        whenever(mockGsrFormat.mFormat).thenReturn("CAL")

        val message =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_DATA_PACKET
                obj = mockObjectCluster
            }

        shimmerHandler.handleMessage(message)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Should handle division by zero gracefully
        verify(mockCallback).onGsrDataReceived(eq(0.0), eq(0.0), any())
    }

    @Test
    fun `toast message is handled correctly`() {
        val toastMessage = "Test toast message"
        val message =
            Message.obtain().apply {
                what = Shimmer.MESSAGE_TOAST
                data =
                    Bundle().apply {
                        putString(Shimmer.TOAST, toastMessage)
                    }
            }

        // Should not throw exception
        shimmerHandler.handleMessage(message)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Test passes if no exception is thrown
        assertTrue("Toast message handled successfully", true)
    }

    @Test
    fun `release method cleans up resources properly`() {
        gsrHandler.release()

        verify(mockShimmer).stopStreaming()
        verify(mockShimmer).disconnect()
    }

    @Test
    fun `setSessionInfo updates session and device information`() {
        val sessionId = "test_session_456"
        val deviceId = "test_device_789"

        gsrHandler.setSessionInfo(sessionId, deviceId)

        // Verify by starting streaming and checking file creation
        val connectMessage =
            Message.obtain().apply {
                what = ShimmerBluetooth.MSG_IDENTIFIER_STATE_CHANGE
                obj = CallbackObject(ShimmerBluetooth.BT_STATE.CONNECTED, "00:11:22:33:44:55", "")
            }
        shimmerHandler.handleMessage(connectMessage)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        gsrHandler.startStreaming()
        gsrHandler.stopStreaming()

        val expectedFileName = "${sessionId}_${deviceId}_gsr_data.csv"
        val file = File("./test_output/", expectedFileName)
        assertTrue("CSV file with correct session info should exist", file.exists())
    }
}
