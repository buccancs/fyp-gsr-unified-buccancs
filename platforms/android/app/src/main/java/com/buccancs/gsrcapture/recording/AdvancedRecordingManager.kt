package com.buccancs.gsrcapture.recording

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.buccancs.gsrcapture.controller.RecordingController
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Advanced Recording Manager that handles different recording modes
 * and provides enhanced recording capabilities beyond basic start/stop
 */
class AdvancedRecordingManager(
    private val context: Context,
    private val recordingController: RecordingController
) {
    companion object {
        private const val TAG = "AdvancedRecordingManager"
    }

    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val isActive = AtomicBoolean(false)
    private val currentMode = AtomicReference<RecordingMode>(RecordingMode.CONTINUOUS)
    private val currentConfig = AtomicReference<RecordingModeConfig>()
    private val sessionStats = AtomicReference<RecordingStats>()

    // Counters for statistics
    private val frameCounter = AtomicInteger(0)
    private val sampleCounter = AtomicInteger(0)
    private val sessionStartTime = AtomicLong(0)

    // Callbacks
    private var modeChangeCallback: ((RecordingMode) -> Unit)? = null
    private var statsUpdateCallback: ((RecordingStats) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var scheduledStartCallback: (() -> Unit)? = null
    private var scheduledStopCallback: (() -> Unit)? = null

    /**
     * Start recording with the specified mode and configuration
     */
    fun startRecording(config: RecordingModeConfig): Boolean {
        if (isActive.get()) {
            Log.w(TAG, "Recording already active")
            return false
        }

        try {
            currentConfig.set(config)
            currentMode.set(config.mode)
            sessionStartTime.set(System.currentTimeMillis())

            // Initialize statistics
            val stats = RecordingStats(
                sessionId = generateSessionId(),
                mode = config.mode,
                startTime = sessionStartTime.get()
            )
            sessionStats.set(stats)

            // Reset counters
            frameCounter.set(0)
            sampleCounter.set(0)

            when (config.mode) {
                RecordingMode.CONTINUOUS -> startContinuousRecording(config)
                RecordingMode.TIME_LAPSE -> startTimeLapseRecording(config)
                RecordingMode.BURST -> startBurstRecording(config)
                RecordingMode.SCHEDULED -> startScheduledRecording(config)
                RecordingMode.MOTION_TRIGGERED -> startMotionTriggeredRecording(config)
                RecordingMode.SENSOR_TRIGGERED -> startSensorTriggeredRecording(config)
                RecordingMode.INTERVAL -> startIntervalRecording(config)
            }

            isActive.set(true)
            modeChangeCallback?.invoke(config.mode)

            Log.d(TAG, "Started ${config.mode.displayName} recording")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error starting advanced recording", e)
            errorCallback?.invoke("Failed to start ${config.mode.displayName}: ${e.message}")
            return false
        }
    }

    /**
     * Stop the current recording session
     */
    fun stopRecording() {
        if (!isActive.get()) {
            Log.w(TAG, "No active recording to stop")
            return
        }

        try {
            // Stop the underlying recording controller
            recordingController.stopRecording()

            // Cancel any scheduled tasks
            scheduler.shutdownNow()

            // Update final statistics
            updateFinalStats()

            isActive.set(false)
            Log.d(TAG, "Stopped ${currentMode.get().displayName} recording")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            errorCallback?.invoke("Error stopping recording: ${e.message}")
        }
    }

    private fun startContinuousRecording(config: RecordingModeConfig) {
        // Start normal recording
        recordingController.startRecording()

        // Set up auto-stop if duration is specified
        if (config.duration > 0) {
            scheduler.schedule({
                mainHandler.post { stopRecording() }
            }, config.duration, TimeUnit.MILLISECONDS)
        }
    }

    private fun startTimeLapseRecording(config: RecordingModeConfig) {
        // Start recording controller
        recordingController.startRecording()

        // Schedule periodic frame captures
        scheduler.scheduleAtFixedRate({
            captureFrame()
        }, 0, config.interval, TimeUnit.MILLISECONDS)

        // Set up auto-stop if duration is specified
        if (config.duration > 0) {
            scheduler.schedule({
                mainHandler.post { stopRecording() }
            }, config.duration, TimeUnit.MILLISECONDS)
        }
    }

    private fun startBurstRecording(config: RecordingModeConfig) {
        // Start recording controller
        recordingController.startRecording()

        // Capture burst frames
        for (i in 0 until config.burstCount) {
            scheduler.schedule({
                captureFrame()
                if (i == config.burstCount - 1) {
                    // Stop after last frame
                    mainHandler.post { stopRecording() }
                }
            }, i * 100L, TimeUnit.MILLISECONDS) // 100ms between burst frames
        }
    }

    private fun startScheduledRecording(config: RecordingModeConfig) {
        val currentTime = System.currentTimeMillis()
        val startDelay = config.scheduledStartTime - currentTime
        val duration = config.scheduledEndTime - config.scheduledStartTime

        if (startDelay > 0) {
            // Schedule start
            scheduler.schedule({
                mainHandler.post {
                    recordingController.startRecording()
                    scheduledStartCallback?.invoke()
                }
            }, startDelay, TimeUnit.MILLISECONDS)
        } else {
            // Start immediately
            recordingController.startRecording()
        }

        if (duration > 0) {
            // Schedule stop
            scheduler.schedule({
                mainHandler.post {
                    stopRecording()
                    scheduledStopCallback?.invoke()
                }
            }, maxOf(startDelay, 0) + duration, TimeUnit.MILLISECONDS)
        }
    }

    private fun startMotionTriggeredRecording(config: RecordingModeConfig) {
        // This would integrate with motion detection
        // For now, we'll simulate motion detection
        scheduler.scheduleAtFixedRate({
            if (detectMotion(config.motionSensitivity)) {
                mainHandler.post {
                    if (!recordingController.isRecordingState) {
                        recordingController.startRecording()
                        Log.d(TAG, "Motion detected - started recording")
                    }
                }
            }
        }, 0, 1000, TimeUnit.MILLISECONDS) // Check every second
    }

    private fun startSensorTriggeredRecording(config: RecordingModeConfig) {
        // This would integrate with sensor threshold monitoring
        scheduler.scheduleAtFixedRate({
            // Monitor sensor values and trigger recording when threshold is exceeded
            // This is a placeholder implementation
            val sensorValue = getCurrentSensorValue()
            if (sensorValue > config.sensorThreshold) {
                mainHandler.post {
                    if (!recordingController.isRecordingState) {
                        recordingController.startRecording()
                        Log.d(TAG, "Sensor threshold exceeded - started recording")
                    }
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS) // Check every 500ms
    }

    private fun startIntervalRecording(config: RecordingModeConfig) {
        scheduler.scheduleAtFixedRate({
            mainHandler.post {
                // Start recording for interval duration
                recordingController.startRecording()
                Log.d(TAG, "Started interval recording")

                // Schedule stop after interval duration
                scheduler.schedule({
                    mainHandler.post {
                        recordingController.stopRecording()
                        Log.d(TAG, "Stopped interval recording")
                    }
                }, config.intervalDuration, TimeUnit.MILLISECONDS)
            }
        }, 0, config.intervalDuration + config.intervalBreak, TimeUnit.MILLISECONDS)
    }

    private fun captureFrame() {
        frameCounter.incrementAndGet()
        updateStats()
    }

    private fun detectMotion(sensitivity: Float): Boolean {
        // Placeholder motion detection logic
        // In a real implementation, this would analyze camera frames
        return Math.random() > (1.0 - sensitivity)
    }

    private fun getCurrentSensorValue(): Float {
        // Placeholder sensor value retrieval
        // In a real implementation, this would get actual sensor readings
        return (Math.random() * 100).toFloat()
    }

    private fun updateStats() {
        val currentStats = sessionStats.get() ?: return
        val currentTime = System.currentTimeMillis()

        val updatedStats = currentStats.copy(
            duration = currentTime - sessionStartTime.get(),
            framesCaptured = frameCounter.get(),
            samplesCaptured = sampleCounter.get(),
            averageFrameRate = calculateAverageFrameRate(),
            averageSampleRate = calculateAverageSampleRate()
        )

        sessionStats.set(updatedStats)
        statsUpdateCallback?.invoke(updatedStats)
    }

    private fun updateFinalStats() {
        val currentStats = sessionStats.get() ?: return
        val endTime = System.currentTimeMillis()

        val finalStats = currentStats.copy(
            endTime = endTime,
            duration = endTime - sessionStartTime.get(),
            framesCaptured = frameCounter.get(),
            samplesCaptured = sampleCounter.get(),
            averageFrameRate = calculateAverageFrameRate(),
            averageSampleRate = calculateAverageSampleRate()
        )

        sessionStats.set(finalStats)
        statsUpdateCallback?.invoke(finalStats)
    }

    private fun calculateAverageFrameRate(): Double {
        val duration = System.currentTimeMillis() - sessionStartTime.get()
        return if (duration > 0) {
            (frameCounter.get() * 1000.0) / duration
        } else 0.0
    }

    private fun calculateAverageSampleRate(): Double {
        val duration = System.currentTimeMillis() - sessionStartTime.get()
        return if (duration > 0) {
            (sampleCounter.get() * 1000.0) / duration
        } else 0.0
    }

    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }

    // Callback setters
    fun setModeChangeCallback(callback: (RecordingMode) -> Unit) {
        modeChangeCallback = callback
    }

    fun setStatsUpdateCallback(callback: (RecordingStats) -> Unit) {
        statsUpdateCallback = callback
    }

    fun setErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    fun setScheduledStartCallback(callback: () -> Unit) {
        scheduledStartCallback = callback
    }

    fun setScheduledStopCallback(callback: () -> Unit) {
        scheduledStopCallback = callback
    }

    // Getters
    fun isRecording(): Boolean = isActive.get()
    fun getCurrentMode(): RecordingMode = currentMode.get()
    fun getCurrentStats(): RecordingStats? = sessionStats.get()
    fun getCurrentConfig(): RecordingModeConfig? = currentConfig.get()

    /**
     * Clean up resources
     */
    fun shutdown() {
        stopRecording()
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }
}
