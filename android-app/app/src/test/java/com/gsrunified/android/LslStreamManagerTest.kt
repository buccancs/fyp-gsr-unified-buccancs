package com.gsrunified.android

import gsrunified.CommandResponse
import gsrunified.GSRData
import gsrunified.ThermalData
import gsrunified.Timestamp
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for LslStreamManager.
 *
 * Note: Since the underlying LSL library uses static methods and native code,
 * these tests focus on verifying the logic of the LslStreamManager class itself,
 * such as state management and method execution, rather than mocking the LSL library directly.
 * This is a "best-effort" test for a class with hard-to-mock dependencies.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class LslStreamManagerTest {
    private lateinit var lslStreamManager: LslStreamManager
    private val testDeviceId = "test-android-device-123"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        lslStreamManager = LslStreamManager(testDeviceId)
        // In a real test environment, we would mock the LSL library.
        // For now, we test that the methods can be called without crashing.
        lslStreamManager.initialize()
    }

    @After
    fun tearDown() {
        lslStreamManager.cleanup()
    }

    @Test
    fun `initialization should complete without errors`() {
        val manager = LslStreamManager("init-test")
        val result = manager.initialize()
        assertTrue("Initialization should return true", result)
    }

    @Test
    fun `createGsrStream should create and track the stream`() {
        val created = lslStreamManager.createGsrStream()
        assertTrue("GSR stream creation should succeed", created)
        assertTrue("GSR stream should be active", lslStreamManager.isStreamActive(LslStreamManager.STREAM_TYPE_GSR))
        assertTrue(
            "Active streams list should contain GSR",
            lslStreamManager.getActiveStreams().contains(LslStreamManager.STREAM_TYPE_GSR),
        )
    }

    @Test
    fun `createThermalStream should create and track the stream`() {
        val created = lslStreamManager.createThermalStream()
        assertTrue("Thermal stream creation should succeed", created)
        assertTrue(
            "Thermal stream should be active",
            lslStreamManager.isStreamActive(LslStreamManager.STREAM_TYPE_THERMAL),
        )
    }

    @Test
    fun `createCommandResponseStream should create and track the stream`() {
        val created = lslStreamManager.createCommandResponseStream()
        assertTrue("CommandResponse stream creation should succeed", created)
        assertTrue(
            "CommandResponse stream should be active",
            lslStreamManager.isStreamActive(LslStreamManager.STREAM_TYPE_RESPONSE),
        )
    }

    @Test
    fun `pushGsrData should not throw exceptions`() {
        lslStreamManager.createGsrStream()
        val gsrData =
            GSRData
                .newBuilder()
                .setConductance(1.23)
                .setResistance(0.81)
                .setQuality(100)
                .setTimestamp(
                    Timestamp
                        .newBuilder()
                        .setSeconds(12345)
                        .setNanos(6789)
                        .build(),
                ).build()

        // This test verifies that the method can be called without crashing.
        // A full integration test would be needed to verify the data on the other end.
        lslStreamManager.pushGsrData(gsrData)
        assertTrue(true) // Pass if no exception
    }

    @Test
    fun `pushThermalData should not throw exceptions`() {
        lslStreamManager.createThermalStream()
        val thermalData =
            ThermalData
                .newBuilder()
                .setWidth(256)
                .setHeight(192)
                .setMinTemp(25.5f)
                .setMaxTemp(35.5f)
                .setAvgTemp(30.0f)
                .setFrameNumber(1)
                .setTimestamp(Timestamp.newBuilder().setSeconds(12345).build())
                .build()

        lslStreamManager.pushThermalData(thermalData)
        assertTrue(true) // Pass if no exception
    }

    @Test
    fun `pushCommandResponse should not throw exceptions`() {
        lslStreamManager.createCommandResponseStream()
        val response =
            CommandResponse
                .newBuilder()
                .setStatus(CommandResponse.Status.SUCCESS)
                .setMessage("Command executed")
                .setTimestamp(Timestamp.newBuilder().setSeconds(12345).build())
                .build()

        lslStreamManager.pushCommandResponse(response)
        assertTrue(true) // Pass if no exception
    }

    @Test
    fun `cleanup should remove all active streams`() {
        lslStreamManager.createGsrStream()
        lslStreamManager.createThermalStream()
        assertTrue("Streams should be active before cleanup", lslStreamManager.getActiveStreams().isNotEmpty())

        lslStreamManager.cleanup()
        assertTrue("Streams should be empty after cleanup", lslStreamManager.getActiveStreams().isEmpty())
    }
}
