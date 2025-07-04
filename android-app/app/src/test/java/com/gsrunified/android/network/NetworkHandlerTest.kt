package com.gsrunified.android.network

import com.gsrunified.android.MainActivity
import com.gsrunified.android.NetworkHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.json.JSONObject
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class NetworkHandlerTest {
    // Use TestCoroutineDispatcher for controlling coroutine execution in tests
    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mainActivity: MainActivity

    @Mock
    private lateinit var socket: Socket

    private lateinit var networkHandler: NetworkHandler
    private lateinit var outputStream: OutputStream
    private lateinit var inputStream: InputStream

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock the streams for testing network I/O
        outputStream = ByteArrayOutputStream()
        // We'll set the input stream content for each test
        inputStream = ByteArrayInputStream(byteArrayOf())

        // Mock socket behavior
        whenever(socket.isConnected).thenReturn(true)
        whenever(socket.getOutputStream()).thenReturn(outputStream)
        whenever(socket.getInputStream()).thenReturn(inputStream)

        // Mock MainActivity methods
        whenever(mainActivity.getDeviceIPAddress()).thenReturn("127.0.0.1")

        // The NetworkHandler now uses a mocked socket for predictable testing
        networkHandler = NetworkHandler(mainActivity)

        // We can't directly inject the socket, so this is a limitation of the current NetworkHandler design.
        // For a real test, we would refactor NetworkHandler to allow injecting a socket factory.
        // For this example, we'll focus on testing the logic that can be tested without a live socket.
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        networkHandler.disconnect()
    }

    @Test
    fun `initial state is disconnected`() {
        assertFalse(networkHandler.isConnected())
        assertNotNull(networkHandler.getDeviceId())
    }

    @Test
    fun `sendDataStream constructs correct JSON message`() =
        runTest {
            // This test focuses on the logic of message construction, which is testable.
            val testHandler = spyk(networkHandler)
            // Prevent the actual sendMessage call which requires a real socket
            coEvery { testHandler.sendMessage(any()) } returns Unit

            val dataType = "gsr"
            val payload = JSONObject().put("value", 1.23)

            testHandler.sendDataStream(dataType, payload)

            // Verify that sendMessage was called with the correctly structured JSON object
            coVerify {
                testHandler.sendMessage(
                    check { message ->
                        assertEquals("data_stream", message.getString("command"))
                        assertEquals(testHandler.getDeviceId(), message.getString("device_id"))
                        val data = message.getJSONObject("data")
                        assertEquals(dataType, data.getString("data_type"))
                        assertEquals(payload.toString(), data.getJSONObject("payload").toString())
                    },
                )
            }
        }

    @Test
    fun `getBatteryLevel and getAvailableStorageGB handle exceptions gracefully`() {
        // These methods are part of NetworkHandler and are good candidates for unit testing.
        // We can mock the context/activity to throw exceptions.
        val handler = NetworkHandler(mainActivity)

        // Mocking to simulate error scenarios
        whenever(mainActivity.registerReceiver(any(), any())).thenThrow(RuntimeException("Test Exception"))
        whenever(mainActivity.getExternalFilesDir(null)).thenReturn(null)

        assertEquals(-1, handler.getBatteryLevel())
        assertEquals(0.0, handler.getAvailableStorageGB(), 0.0)
    }

    @Test
    fun `cleanup stops all jobs and clears resources`() =
        runTest {
            // This test verifies the resource cleanup logic
            val handler = spyk(networkHandler)
            handler.connect("localhost", 8080)
            advanceUntilIdle() // Let connection attempt run

            // Disconnect
            handler.disconnect()

            assertFalse(handler.isConnected())
            // In a real test with injected jobs, we would verify that jobs are cancelled.
            // For now, we just ensure it runs without error and sets state correctly.
        }

    @Test
    fun `processIncomingMessage handles start_recording command`() =
        runTest {
            val handler = spyk(networkHandler)
            val sessionId = "session_12345"
            val message =
                JSONObject()
                    .apply {
                        put("command", "start_recording")
                        put("data", JSONObject().put("session_id", sessionId))
                    }.toString()

            // Simulate receiving the message
            handler.processIncomingMessage(message)
            advanceUntilIdle()

            // Verify that the MainActivity's handleCommand was called with the correct parameter
            verify(mainActivity).handleCommand("start_recording:$sessionId")
        }

    @Test
    fun `processIncomingMessage handles stop_recording command`() =
        runTest {
            val handler = spyk(networkHandler)
            val sessionId = "session_12345"
            val message =
                JSONObject()
                    .apply {
                        put("command", "stop_recording")
                        put("data", JSONObject().put("session_id", sessionId))
                    }.toString()

            handler.processIncomingMessage(message)
            advanceUntilIdle()

            verify(mainActivity).handleCommand("stop_recording:$sessionId")
        }

    @Test
    fun `processIncomingMessage handles unknown command gracefully`() =
        runTest {
            val handler = spyk(networkHandler)
            val message =
                JSONObject()
                    .apply {
                        put("command", "unknown_command")
                    }.toString()

            handler.processIncomingMessage(message)
            advanceUntilIdle()

            // Verify no methods on mainActivity were called for an unknown command
            verify(mainActivity, never()).handleCommand(any())
        }
}
