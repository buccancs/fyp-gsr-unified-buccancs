package com.buccancs.gsrcapture.utils

import android.os.SystemClock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages time synchronization and timestamping for all data streams.
 * Uses the monotonic clock (SystemClock.elapsedRealtimeNanos) for precise timing.
 */
object TimeManager {
    private val TAG = "TimeManager"

    // Offset between system time and network time (if synchronized with PC)
    private val networkTimeOffset = AtomicLong(0)

    // Session start time in nanoseconds (monotonic clock)
    private var sessionStartTimeNanos: Long = 0

    // Date formatter for human-readable timestamps
    private val dateFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    // Check if running in test environment
    private val isTestEnvironment = try {
        Class.forName("org.robolectric.RobolectricTestRunner")
        true
    } catch (e: ClassNotFoundException) {
        false
    }

    /**
     * Initializes the time manager for a new recording session.
     * @return The session start time in nanoseconds
     */
    fun initSession(): Long {
        sessionStartTimeNanos = if (isTestEnvironment) {
            System.nanoTime()
        } else {
            SystemClock.elapsedRealtimeNanos()
        }
        return sessionStartTimeNanos
    }

    /**
     * Gets the current timestamp in nanoseconds since session start.
     * @return Nanoseconds since session start
     */
    fun getCurrentTimestampNanos(): Long {
        val currentTime = if (isTestEnvironment) {
            System.nanoTime()
        } else {
            SystemClock.elapsedRealtimeNanos()
        }
        return currentTime - sessionStartTimeNanos
    }

    /**
     * Gets the current timestamp in milliseconds since session start.
     * @return Milliseconds since session start
     */
    fun getCurrentTimestampMillis(): Long = getCurrentTimestampNanos() / 1_000_000

    /**
     * Gets a formatted timestamp string for file naming.
     * @return Timestamp string in format "yyyyMMdd_HHmmss"
     */
    fun getCurrentTimestamp(): String = dateFormatter.format(Date())

    /**
     * Synchronizes the local clock with a network time source (e.g., PC controller).
     * @param remoteTimeMillis The remote time in milliseconds
     * @param roundTripTimeMillis The round-trip network delay in milliseconds
     */
    fun synchronizeWithNetwork(
        remoteTimeMillis: Long,
        roundTripTimeMillis: Long,
    ) {
        // Estimate one-way network delay (half of round-trip time)
        val networkDelayMillis = roundTripTimeMillis / 2

        // Current local time
        val localTimeMillis = System.currentTimeMillis()

        // Calculate offset: remoteTime + networkDelay - localTime
        val offset = remoteTimeMillis + networkDelayMillis - localTimeMillis

        // Update the network time offset
        networkTimeOffset.set(offset)
    }

    /**
     * Gets the current network-synchronized time.
     * @return Current time adjusted by network offset
     */
    fun getSynchronizedTimeMillis(): Long = System.currentTimeMillis() + networkTimeOffset.get()

    /**
     * Converts a local timestamp to a synchronized timestamp.
     * @param localTimestampMillis Local timestamp in milliseconds
     * @return Synchronized timestamp in milliseconds
     */
    fun convertToSynchronizedTime(localTimestampMillis: Long): Long = localTimestampMillis + networkTimeOffset.get()

    /**
     * Adds a timestamp to a data sample.
     * @param sample The data sample to timestamp
     * @return The timestamped data sample
     */
    fun <T> timestampData(sample: T): TimestampedData<T> {
        val currentTime = if (isTestEnvironment) {
            System.nanoTime()
        } else {
            SystemClock.elapsedRealtimeNanos()
        }
        return TimestampedData(
            data = sample,
            timestampNanos = currentTime,
            sessionOffsetNanos = getCurrentTimestampNanos(),
        )
    }

    /**
     * Data class for timestamped data samples.
     */
    data class TimestampedData<T>(
        val data: T,
        val timestampNanos: Long,
        val sessionOffsetNanos: Long,
    )
}
