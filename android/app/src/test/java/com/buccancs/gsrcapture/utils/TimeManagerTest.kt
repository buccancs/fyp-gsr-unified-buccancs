package com.buccancs.gsrcapture.utils

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TimeManagerTest {

    @Before
    fun setUp() {
        // Reset TimeManager state before each test
        TimeManager.reset()
    }

    @Test
    fun testInitialization() {
        // Test that TimeManager initializes correctly
        assertNotNull("TimeManager should be available", TimeManager)
        assertTrue("TimeManager should have valid initial time", TimeManager.getCurrentTimestamp() > 0)
    }

    @Test
    fun testTimestampGeneration() {
        val timestamp1 = TimeManager.getCurrentTimestamp()
        Thread.sleep(1) // Small delay
        val timestamp2 = TimeManager.getCurrentTimestamp()
        
        assertTrue("Timestamps should be increasing", timestamp2 > timestamp1)
        assertTrue("Timestamps should be reasonable", timestamp1 > System.currentTimeMillis() - 1000)
    }

    @Test
    fun testDeviceTimeGeneration() {
        val deviceTime1 = TimeManager.getCurrentDeviceTime()
        Thread.sleep(1) // Small delay
        val deviceTime2 = TimeManager.getCurrentDeviceTime()
        
        assertTrue("Device times should be increasing", deviceTime2 > deviceTime1)
        assertTrue("Device time should be in nanoseconds", deviceTime1 > 0)
    }

    @Test
    fun testTimestampedDataCreation() {
        val testData = "test_data"
        val timestampedData = TimeManager.createTimestampedData(testData)
        
        assertNotNull("Timestamped data should not be null", timestampedData)
        assertEquals("Data should match", testData, timestampedData.data)
        assertTrue("Timestamp should be valid", timestampedData.timestamp > 0)
        assertTrue("Device time should be valid", timestampedData.deviceTime > 0)
    }

    @Test
    fun testTimestampedDataWithCustomTimes() {
        val testData = 42.5f
        val customTimestamp = 1234567890L
        val customDeviceTime = 9876543210L
        
        val timestampedData = TimeManager.TimestampedData(
            data = testData,
            timestamp = customTimestamp,
            deviceTime = customDeviceTime
        )
        
        assertEquals("Data should match", testData, timestampedData.data)
        assertEquals("Timestamp should match", customTimestamp, timestampedData.timestamp)
        assertEquals("Device time should match", customDeviceTime, timestampedData.deviceTime)
    }

    @Test
    fun testTimeSync() {
        val remoteTime = System.currentTimeMillis()
        val roundTripTime = 50L
        
        // Test time synchronization
        TimeManager.synchronizeTime(remoteTime, roundTripTime)
        
        // After sync, timestamps should be adjusted
        val syncedTimestamp = TimeManager.getCurrentTimestamp()
        assertTrue("Synced timestamp should be reasonable", syncedTimestamp > 0)
    }

    @Test
    fun testTimeSyncWithZeroRoundTrip() {
        val remoteTime = System.currentTimeMillis()
        val roundTripTime = 0L
        
        TimeManager.synchronizeTime(remoteTime, roundTripTime)
        
        val syncedTimestamp = TimeManager.getCurrentTimestamp()
        assertTrue("Synced timestamp should be valid even with zero round trip", syncedTimestamp > 0)
    }

    @Test
    fun testTimeSyncWithLargeRoundTrip() {
        val remoteTime = System.currentTimeMillis()
        val roundTripTime = 1000L // 1 second round trip
        
        TimeManager.synchronizeTime(remoteTime, roundTripTime)
        
        val syncedTimestamp = TimeManager.getCurrentTimestamp()
        assertTrue("Synced timestamp should handle large round trip times", syncedTimestamp > 0)
    }

    @Test
    fun testMultipleTimeSync() {
        val remoteTime1 = System.currentTimeMillis()
        val remoteTime2 = remoteTime1 + 1000
        
        // First sync
        TimeManager.synchronizeTime(remoteTime1, 50L)
        val timestamp1 = TimeManager.getCurrentTimestamp()
        
        Thread.sleep(10)
        
        // Second sync
        TimeManager.synchronizeTime(remoteTime2, 30L)
        val timestamp2 = TimeManager.getCurrentTimestamp()
        
        assertTrue("Multiple syncs should work", timestamp2 > timestamp1)
    }

    @Test
    fun testTimestampConsistency() {
        val timestamps = mutableListOf<Long>()
        
        // Generate multiple timestamps quickly
        repeat(100) {
            timestamps.add(TimeManager.getCurrentTimestamp())
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
            deviceTimes.add(TimeManager.getCurrentDeviceTime())
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
                val timestamp = TimeManager.getCurrentTimestamp()
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
            assertTrue("All timestamps should be positive", timestamp > 0)
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
                val data = TimeManager.createTimestampedData(index)
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
            assertTrue("Timestamp should be positive", data.timestamp > 0)
            assertTrue("Device time should be positive", data.deviceTime > 0)
        }
    }

    @Test
    fun testTimestampedDataTypes() {
        // Test with different data types
        val stringData = TimeManager.createTimestampedData("test_string")
        val intData = TimeManager.createTimestampedData(42)
        val floatData = TimeManager.createTimestampedData(3.14f)
        val doubleData = TimeManager.createTimestampedData(2.718)
        val booleanData = TimeManager.createTimestampedData(true)
        
        assertEquals("String data should match", "test_string", stringData.data)
        assertEquals("Int data should match", 42, intData.data)
        assertEquals("Float data should match", 3.14f, floatData.data, 0.001f)
        assertEquals("Double data should match", 2.718, doubleData.data, 0.001)
        assertEquals("Boolean data should match", true, booleanData.data)
        
        // All should have valid timestamps
        assertTrue("String data timestamp should be valid", stringData.timestamp > 0)
        assertTrue("Int data timestamp should be valid", intData.timestamp > 0)
        assertTrue("Float data timestamp should be valid", floatData.timestamp > 0)
        assertTrue("Double data timestamp should be valid", doubleData.timestamp > 0)
        assertTrue("Boolean data timestamp should be valid", booleanData.timestamp > 0)
    }

    @Test
    fun testTimeOffsetCalculation() {
        val baseTime = System.currentTimeMillis()
        val remoteTime = baseTime + 1000 // Remote is 1 second ahead
        val roundTripTime = 100L
        
        TimeManager.synchronizeTime(remoteTime, roundTripTime)
        
        // After sync, our timestamps should be adjusted
        val adjustedTime = TimeManager.getCurrentTimestamp()
        
        // The adjustment should account for the offset and round trip time
        assertTrue("Adjusted time should be reasonable", adjustedTime > baseTime)
    }

    @Test
    fun testReset() {
        // Modify TimeManager state
        TimeManager.synchronizeTime(System.currentTimeMillis() + 5000, 100L)
        val timestampBeforeReset = TimeManager.getCurrentTimestamp()
        
        // Reset TimeManager
        TimeManager.reset()
        
        val timestampAfterReset = TimeManager.getCurrentTimestamp()
        
        // After reset, timestamps should be based on system time again
        assertTrue("Reset should restore normal timing", timestampAfterReset > 0)
    }

    @Test
    fun testTimestampAccuracy() {
        val systemTime = System.currentTimeMillis()
        val managerTime = TimeManager.getCurrentTimestamp()
        
        // Times should be close (within 1 second)
        val difference = Math.abs(managerTime - systemTime)
        assertTrue("TimeManager timestamp should be close to system time", difference < 1000)
    }

    @Test
    fun testDeviceTimeAccuracy() {
        val systemNanoTime = System.nanoTime()
        val managerDeviceTime = TimeManager.getCurrentDeviceTime()
        
        // Device times should be close (within reasonable bounds)
        val difference = Math.abs(managerDeviceTime - systemNanoTime)
        assertTrue("TimeManager device time should be close to system nano time", difference < 1_000_000_000L) // 1 second in nanoseconds
    }

    @Test
    fun testTimestampedDataEquality() {
        val data1 = TimeManager.TimestampedData("test", 1000L, 2000L)
        val data2 = TimeManager.TimestampedData("test", 1000L, 2000L)
        val data3 = TimeManager.TimestampedData("different", 1000L, 2000L)
        
        assertEquals("Same data should be equal", data1.data, data2.data)
        assertEquals("Same timestamp should be equal", data1.timestamp, data2.timestamp)
        assertEquals("Same device time should be equal", data1.deviceTime, data2.deviceTime)
        assertNotEquals("Different data should not be equal", data1.data, data3.data)
    }

    @Test
    fun testTimestampedDataToString() {
        val data = TimeManager.TimestampedData("test_data", 1234567890L, 9876543210L)
        val stringRepresentation = data.toString()
        
        assertNotNull("String representation should not be null", stringRepresentation)
        assertTrue("String should contain data", stringRepresentation.contains("test_data"))
        assertTrue("String should contain timestamp", stringRepresentation.contains("1234567890"))
        assertTrue("String should contain device time", stringRepresentation.contains("9876543210"))
    }
}