package com.buccancs.gsrcapture.recording

/**
 * Enum representing different recording modes available in the application
 */
enum class RecordingMode(
    val displayName: String,
    val description: String,
    val requiresScheduling: Boolean = false
) {
    /**
     * Standard continuous recording mode
     */
    CONTINUOUS(
        displayName = "Continuous Recording",
        description = "Records continuously until manually stopped",
        requiresScheduling = false
    ),

    /**
     * Time-lapse recording mode - captures frames at specified intervals
     */
    TIME_LAPSE(
        displayName = "Time-lapse Recording",
        description = "Captures frames at specified intervals to create time-lapse videos",
        requiresScheduling = false
    ),

    /**
     * Burst recording mode - captures multiple frames/samples rapidly
     */
    BURST(
        displayName = "Burst Recording",
        description = "Captures multiple frames/samples in rapid succession",
        requiresScheduling = false
    ),

    /**
     * Scheduled recording mode - starts and stops at predetermined times
     */
    SCHEDULED(
        displayName = "Scheduled Recording",
        description = "Automatically starts and stops recording at scheduled times",
        requiresScheduling = true
    ),

    /**
     * Motion-triggered recording mode
     */
    MOTION_TRIGGERED(
        displayName = "Motion Triggered",
        description = "Starts recording when motion is detected",
        requiresScheduling = false
    ),

    /**
     * Sensor-triggered recording mode
     */
    SENSOR_TRIGGERED(
        displayName = "Sensor Triggered",
        description = "Starts recording when sensor values exceed thresholds",
        requiresScheduling = false
    ),

    /**
     * Interval recording mode - records for specified durations with breaks
     */
    INTERVAL(
        displayName = "Interval Recording",
        description = "Records for specified durations with breaks in between",
        requiresScheduling = false
    )
}

/**
 * Data class representing recording mode configuration
 */
data class RecordingModeConfig(
    val mode: RecordingMode,
    val duration: Long = 0, // Recording duration in milliseconds (0 = unlimited)
    val interval: Long = 1000, // Interval between captures in milliseconds (for time-lapse/interval modes)
    val burstCount: Int = 10, // Number of captures in burst mode
    val scheduledStartTime: Long = 0, // Scheduled start time (timestamp)
    val scheduledEndTime: Long = 0, // Scheduled end time (timestamp)
    val motionSensitivity: Float = 0.5f, // Motion detection sensitivity (0.0 - 1.0)
    val sensorThreshold: Float = 0.0f, // Sensor trigger threshold
    val intervalDuration: Long = 60000, // Duration of each recording interval in milliseconds
    val intervalBreak: Long = 30000, // Break duration between intervals in milliseconds
    val autoStop: Boolean = false, // Whether to automatically stop recording
    val customSettings: Map<String, Any> = emptyMap()
)

/**
 * Data class representing recording session statistics
 */
data class RecordingStats(
    val sessionId: String,
    val mode: RecordingMode,
    val startTime: Long,
    val endTime: Long = 0,
    val duration: Long = 0,
    val framesCaptured: Int = 0,
    val samplesCaptured: Int = 0,
    val fileSize: Long = 0,
    val averageFrameRate: Double = 0.0,
    val averageSampleRate: Double = 0.0,
    val batteryUsed: Float = 0.0f,
    val storageUsed: Long = 0,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)