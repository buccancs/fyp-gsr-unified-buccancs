package com.buccancs.gsrcapture.utils

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class TimeManagerTest {

    @Before
    fun setUp() {
        // Initialize TimeManager session before each test
        TimeManager.initSession()
    }

    @Test
    fun testInitialization() {
        // Test that TimeManager initializes correctly
        assertNotNull("TimeManager should be available", TimeManager)
        assertTrue("TimeManager should have valid initial time", TimeManager.getCurrentTimestamp().isNotEmpty())
    }

    @Test
    fun testTimestampGeneration() {
        val timestamp1 = TimeManager.getCurrentTimestampMillis()
        Thread.sleep(1) // Small delay
        val timestamp2 = TimeManager.getCurrentTimestampMillis()

        assertTrue("Timestamps should be increasing", timestamp2 > timestamp1)
        assertTrue("Timestamps should be reasonable", timestamp1 >= 0)
    }

    @Test
    fun testDeviceTimeGeneration() {
        val deviceTime1 = TimeManager.getCurrentTimestampNanos()
        Thread.sleep(1) // Small delay
        val deviceTime2 = TimeManager.getCurrentTimestampNanos()

        assertTrue("Device times should be increasing", deviceTime2 > deviceTime1)
        assertTrue("Device time should be in nanoseconds", deviceTime1 >= 0)
    }

    @Test
    fun testTimestampedDataCreation() {
        val testData = "test_data"
        val timestampedData = TimeManager.timestampData(testData)

        assertNotNull("Timestamped data should not be null", timestampedData)
        assertEquals("Data should match", testData, timestampedData.data)
        assertTrue("Timestamp should be valid", timestampedData.timestampNanos > 0)
        assertTrue("Session offset should be valid", timestampedData.sessionOffsetNanos >= 0)
    }

    @Test
    fun testTimestampedDataWithCustomTimes() {
        val testData = 42.5f
        val customTimestamp = 1234567890L
        val customSessionOffset = 9876543210L

        val timestampedData = TimeManager.TimestampedData(
            data = testData,
            timestampNanos = customTimestamp,
            sessionOffsetNanos = customSessionOffset
        )

        assertEquals("Data should match", testData, timestampedData.data)
        assertEquals("Timestamp should match", customTimestamp, timestampedData.timestampNanos)
        assertEquals("Session offset should match", customSessionOffset, timestampedData.sessionOffsetNanos)
    }

    @Test
    fun testTimeSync() {
        val remoteTime = System.currentTimeMillis()
        val roundTripTime = 50L

        // Test time synchronization
        TimeManager.synchronizeWithNetwork(remoteTime, roundTripTime)

        // After sync, synchronized time should be available
        val syncedTime = TimeManager.getSynchronizedTimeMillis()
        assertTrue("Synced time should be reasonable", syncedTime > 0)
    }

    @Test
    fun testTimeSyncWithZeroRoundTrip() {
        val remoteTime = System.currentTimeMillis()
        val roundTripTime = 0L

        TimeManager.synchronizeWithNetwork(remoteTime, roundTripTime)

        val syncedTime = TimeManager.getSynchronizedTimeMillis()
        assertTrue("Synced time should be valid even with zero round trip", syncedTime > 0)
    }

    @Test
    fun testTimeSyncWithLargeRoundTrip() {
        val remoteTime = System.currentTimeMillis()
        val roundTripTime = 1000L // 1 second round trip

        TimeManager.synchronizeWithNetwork(remoteTime, roundTripTime)

        val syncedTime = TimeManager.getSynchronizedTimeMillis()
        assertTrue("Synced time should handle large round trip times", syncedTime > 0)
    }

    @Test
    fun testMultipleTimeSync() {
        val remoteTime1 = System.currentTimeMillis()
        val remoteTime2 = remoteTime1 + 1000

        // First sync
        TimeManager.synchronizeWithNetwork(remoteTime1, 50L)
        val timestamp1 = TimeManager.getSynchronizedTimeMillis()

        Thread.sleep(10)

        // Second sync
        TimeManager.synchronizeWithNetwork(remoteTime2, 30L)
        val timestamp2 = TimeManager.getSynchronizedTimeMillis()

        assertTrue("Multiple syncs should work", timestamp2 >= timestamp1)
    }

    @Test
    fun testTimestampConsistency() {
        val timestamps = mutableListOf<Long>()

        // Generate multiple timestamps quickly
        repeat(100) {
            timestamps.add(TimeManager.getCurrentTimestampMillis())
        }

        // Check that timestamps are monotonically increasing or equal
        for (i in 1 until timestamps.size) {
            assertTrue(
                "Timestamps should be monotonically increasing: ${timestamps[i-1]} <= ${timestamps[i]}",
                timestamps[i-1] <= timestamps[i]
            )
        }
    }

    @Test
    fun testDeviceTimeConsistency() {
        val deviceTimes = mutableListOf<Long>()

        // Generate multiple device times quickly
        repeat(100) {
            deviceTimes.add(TimeManager.getCurrentTimestampNanos())
        }

        // Check that device times are monotonically increasing
        for (i in 1 until deviceTimes.size) {
            assertTrue(
                "Device times should be monotonically increasing: ${deviceTimes[i-1]} <= ${deviceTimes[i]}",
                deviceTimes[i-1] <= deviceTimes[i]
            )
        }
    }

    @Test
    fun testConcurrentTimestampGeneration() {
        val timestampCount = 1000
        val timestamps = mutableListOf<Long>()
        val latch = CountDownLatch(timestampCount)

        // Generate timestamps concurrently
        repeat(timestampCount) {
            Thread {
                val timestamp = TimeManager.getCurrentTimestampMillis()
                synchronized(timestamps) {
                    timestamps.add(timestamp)
                }
                latch.countDown()
            }.start()
        }

        // Wait for all threads to complete
        assertTrue("All timestamp generation should complete", latch.await(10, TimeUnit.SECONDS))
        assertEquals("Should generate all timestamps", timestampCount, timestamps.size)

        // Check that all timestamps are valid
        timestamps.forEach { timestamp ->
            assertTrue("All timestamps should be non-negative", timestamp >= 0)
        }
    }

    @Test
    fun testConcurrentTimestampedDataCreation() {
        val dataCount = 500
        val timestampedDataList = mutableListOf<TimeManager.TimestampedData<Int>>()
        val latch = CountDownLatch(dataCount)

        // Create timestamped data concurrently
        repeat(dataCount) { index ->
            Thread {
                val data = TimeManager.timestampData(index)
                synchronized(timestampedDataList) {
                    timestampedDataList.add(data)
                }
                latch.countDown()
            }.start()
        }

        // Wait for all threads to complete
        assertTrue("All data creation should complete", latch.await(10, TimeUnit.SECONDS))
        assertEquals("Should create all timestamped data", dataCount, timestampedDataList.size)

        // Check that all data is valid
        timestampedDataList.forEach { data ->
            assertTrue("Data should be valid", data.data >= 0)
            assertTrue("Timestamp should be positive", data.timestampNanos > 0)
            assertTrue("Session offset should be non-negative", data.sessionOffsetNanos >= 0)
        }
    }

    @Test
    fun testTimestampedDataTypes() {
        // Test with different data types
        val stringData = TimeManager.timestampData("test_string")
        val intData = TimeManager.timestampData(42)
        val floatData = TimeManager.timestampData(3.14f)
        val doubleData = TimeManager.timestampData(2.718)
        val booleanData = TimeManager.timestampData(true)

        assertEquals("String data should match", "test_string", stringData.data)
        assertEquals("Int data should match", 42, intData.data)
        assertEquals("Float data should match", 3.14f, floatData.data, 0.001f)
        assertEquals("Double data should match", 2.718, doubleData.data, 0.001)
        assertEquals("Boolean data should match", true, booleanData.data)

        // All should have valid timestamps
        assertTrue("String data timestamp should be valid", stringData.timestampNanos > 0)
        assertTrue("Int data timestamp should be valid", intData.timestampNanos > 0)
        assertTrue("Float data timestamp should be valid", floatData.timestampNanos > 0)
        assertTrue("Double data timestamp should be valid", doubleData.timestampNanos > 0)
        assertTrue("Boolean data timestamp should be valid", booleanData.timestampNanos > 0)
    }

    @Test
    fun testTimeOffsetCalculation() {
        val baseTime = System.currentTimeMillis()
        val remoteTime = baseTime + 1000 // Remote is 1 second ahead
        val roundTripTime = 100L

        TimeManager.synchronizeWithNetwork(remoteTime, roundTripTime)

        // After sync, our synchronized time should be adjusted
        val adjustedTime = TimeManager.getSynchronizedTimeMillis()

        // The adjustment should account for the offset and round trip time
        assertTrue("Adjusted time should be reasonable", adjustedTime > baseTime)
    }

    @Test
    fun testSessionInitialization() {
        // Test that session initialization works
        val sessionStart1 = TimeManager.initSession()
        Thread.sleep(1)
        val sessionStart2 = TimeManager.initSession()

        // Session start times should be different
        assertTrue("Session start times should be different", sessionStart2 > sessionStart1)

        // After initialization, timestamps should start from 0
        val timestamp = TimeManager.getCurrentTimestampNanos()
        assertTrue("Timestamp should be small after session init", timestamp < 1_000_000_000L) // Less than 1 second
    }

    @Test
    fun testTimestampStringFormat() {
        val timestampString = TimeManager.getCurrentTimestamp()

        // Should be a valid timestamp string format
        assertNotNull("Timestamp string should not be null", timestampString)
        assertTrue("Timestamp string should not be empty", timestampString.isNotEmpty())
        assertTrue("Timestamp string should have expected format", timestampString.matches(Regex("\\d{8}_\\d{6}")))
    }

    @Test
    fun testTimestampNanosAccuracy() {
        // Test that timestamp nanos are reasonable relative to session start
        val timestamp1 = TimeManager.getCurrentTimestampNanos()
        Thread.sleep(1)
        val timestamp2 = TimeManager.getCurrentTimestampNanos()

        // Should be increasing and reasonable
        assertTrue("Timestamps should be increasing", timestamp2 > timestamp1)
        assertTrue("Timestamps should be non-negative", timestamp1 >= 0)
        assertTrue("Timestamp difference should be reasonable", (timestamp2 - timestamp1) < 1_000_000_000L) // Less than 1 second
    }

    @Test
    fun testTimestampedDataEquality() {
        val data1 = TimeManager.TimestampedData("test", 1000L, 2000L)
        val data2 = TimeManager.TimestampedData("test", 1000L, 2000L)
        val data3 = TimeManager.TimestampedData("different", 1000L, 2000L)

        assertEquals("Same data should be equal", data1.data, data2.data)
        assertEquals("Same timestamp should be equal", data1.timestampNanos, data2.timestampNanos)
        assertEquals("Same session offset should be equal", data1.sessionOffsetNanos, data2.sessionOffsetNanos)
        assertNotEquals("Different data should not be equal", data1.data, data3.data)
        assertEquals("Same objects should be equal", data1, data2)
        assertNotEquals("Different objects should not be equal", data1, data3)
    }

    @Test
    fun testTimestampedDataToString() {
        val data = TimeManager.TimestampedData("test_data", 1234567890L, 9876543210L)
        val stringRepresentation = data.toString()

        assertNotNull("String representation should not be null", stringRepresentation)
        assertTrue("String should contain data", stringRepresentation.contains("test_data"))
        assertTrue("String should contain timestamp", stringRepresentation.contains("1234567890"))
        assertTrue("String should contain session offset", stringRepresentation.contains("9876543210"))
    }
}
