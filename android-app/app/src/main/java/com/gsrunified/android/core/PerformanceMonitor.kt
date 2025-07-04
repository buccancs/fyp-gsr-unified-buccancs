package com.gsrunified.android.core

import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance monitoring utility for tracking system metrics and performance indicators.
 * Provides real-time monitoring of frame rates, data throughput, memory usage, and other metrics.
 */
class PerformanceMonitor {
    private val metrics = ConcurrentHashMap<String, MetricTracker>()
    private val isMonitoring = AtomicBoolean(false)
    private var monitoringJob: Job? = null

    // Callback interface for performance updates
    interface PerformanceCallback {
        fun onMetricsUpdated(metrics: Map<String, MetricData>)

        fun onPerformanceAlert(
            metric: String,
            value: Double,
            threshold: Double,
        )
    }

    private var callback: PerformanceCallback? = null

    fun setCallback(callback: PerformanceCallback) {
        this.callback = callback
    }

    /**
     * Start performance monitoring with specified update interval.
     */
    fun startMonitoring(updateIntervalMs: Long = 5000) {
        if (isMonitoring.get()) {
            Log.w(TAG, "Performance monitoring already running")
            return
        }

        isMonitoring.set(true)
        monitoringJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isMonitoring.get()) {
                    try {
                        updateMetrics()
                        delay(updateIntervalMs)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in performance monitoring", e)
                    }
                }
            }

        Log.i(TAG, "Performance monitoring started")
    }

    /**
     * Stop performance monitoring.
     */
    fun stopMonitoring() {
        isMonitoring.set(false)
        monitoringJob?.cancel()
        Log.i(TAG, "Performance monitoring stopped")
    }

    /**
     * Record a metric value.
     */
    fun recordMetric(
        name: String,
        value: Double,
    ) {
        val tracker = metrics.getOrPut(name) { MetricTracker(name) }
        tracker.addValue(value)
    }

    /**
     * Increment a counter metric.
     */
    fun incrementCounter(name: String) {
        val tracker = metrics.getOrPut(name) { MetricTracker(name, MetricType.COUNTER) }
        tracker.increment()
    }

    /**
     * Record frame rate for a specific component.
     */
    fun recordFrameRate(component: String) {
        val metricName = "${component}_fps"
        val tracker = metrics.getOrPut(metricName) { MetricTracker(metricName, MetricType.FRAME_RATE) }
        tracker.recordFrame()
    }

    /**
     * Record data throughput in bytes.
     */
    fun recordDataThroughput(
        component: String,
        bytes: Long,
    ) {
        val metricName = "${component}_throughput_bps"
        val tracker = metrics.getOrPut(metricName) { MetricTracker(metricName, MetricType.THROUGHPUT) }
        tracker.addBytes(bytes)
    }

    /**
     * Get current metrics snapshot.
     */
    fun getMetrics(): Map<String, MetricData> = metrics.mapValues { it.value.getMetricData() }

    /**
     * Get specific metric value.
     */
    fun getMetric(name: String): MetricData? = metrics[name]?.getMetricData()

    /**
     * Set performance threshold for alerts.
     */
    fun setThreshold(
        metricName: String,
        threshold: Double,
    ) {
        metrics[metricName]?.setThreshold(threshold)
    }

    private fun updateMetrics() {
        val currentMetrics = getMetrics()
        callback?.onMetricsUpdated(currentMetrics)

        // Check for threshold violations
        currentMetrics.forEach { (name, data) ->
            val tracker = metrics[name]
            tracker?.threshold?.let { threshold ->
                if (data.current > threshold) {
                    callback?.onPerformanceAlert(name, data.current, threshold)
                }
            }
        }
    }

    /**
     * Reset all metrics.
     */
    fun reset() {
        metrics.clear()
        Log.i(TAG, "Performance metrics reset")
    }

    /**
     * Get performance summary as formatted string.
     */
    fun getPerformanceSummary(): String {
        val metrics = getMetrics()
        return buildString {
            appendLine("=== Performance Summary ===")
            metrics.forEach { (name, data) ->
                appendLine(
                    "$name: ${String.format("%.2f", data.current)} (avg: ${
                        String.format(
                            "%.2f",
                            data.average,
                        )
                    }, max: ${String.format("%.2f", data.maximum)})",
                )
            }
        }
    }

    enum class MetricType {
        GAUGE, // Single value metrics
        COUNTER, // Incrementing counters
        FRAME_RATE, // Frames per second
        THROUGHPUT, // Bytes per second
    }

    data class MetricData(
        val name: String,
        val type: MetricType,
        val current: Double,
        val average: Double,
        val minimum: Double,
        val maximum: Double,
        val count: Long,
        val lastUpdated: Long,
    )

    private class MetricTracker(
        val name: String,
        val type: MetricType = MetricType.GAUGE,
    ) {
        private val values = mutableListOf<Double>()
        private val timestamps = mutableListOf<Long>()
        private val counter = AtomicLong(0)
        private val frameCount = AtomicInteger(0)
        private val bytesTransferred = AtomicLong(0)
        private var lastFrameTime = 0L
        private var lastThroughputTime = 0L
        var threshold: Double? = null

        @Synchronized
        fun addValue(value: Double) {
            val now = System.currentTimeMillis()
            values.add(value)
            timestamps.add(now)

            // Keep only last 100 values for memory efficiency
            if (values.size > 100) {
                values.removeAt(0)
                timestamps.removeAt(0)
            }
        }

        fun increment() {
            counter.incrementAndGet()
        }

        fun recordFrame() {
            frameCount.incrementAndGet()
            val now = System.currentTimeMillis()
            if (lastFrameTime > 0) {
                val fps = 1000.0 / (now - lastFrameTime)
                addValue(fps)
            }
            lastFrameTime = now
        }

        fun addBytes(bytes: Long) {
            bytesTransferred.addAndGet(bytes)
            val now = System.currentTimeMillis()
            if (lastThroughputTime > 0) {
                val timeDiff = (now - lastThroughputTime) / 1000.0
                if (timeDiff > 0) {
                    val bps = bytes / timeDiff
                    addValue(bps)
                }
            }
            lastThroughputTime = now
        }

        fun setThreshold(threshold: Double) {
            this.threshold = threshold
        }

        @Synchronized
        fun getMetricData(): MetricData {
            val now = System.currentTimeMillis()

            return when (type) {
                MetricType.COUNTER -> {
                    val current = counter.get().toDouble()
                    MetricData(name, type, current, current, current, current, 1, now)
                }

                MetricType.FRAME_RATE, MetricType.THROUGHPUT, MetricType.GAUGE -> {
                    if (values.isEmpty()) {
                        MetricData(name, type, 0.0, 0.0, 0.0, 0.0, 0, now)
                    } else {
                        val current = values.lastOrNull() ?: 0.0
                        val average = values.average()
                        val minimum = values.minOrNull() ?: 0.0
                        val maximum = values.maxOrNull() ?: 0.0
                        MetricData(name, type, current, average, minimum, maximum, values.size.toLong(), now)
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "PerformanceMonitor"

        // Singleton instance
        @Volatile
        private var INSTANCE: PerformanceMonitor? = null

        fun getInstance(): PerformanceMonitor =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: PerformanceMonitor().also { INSTANCE = it }
            }

        // Static convenience methods for easier access
        fun startTimer(name: String) {
            getInstance().recordMetric("${name}_start_time", System.currentTimeMillis().toDouble())
        }

        fun stopTimer(name: String) {
            val instance = getInstance()
            val startTime = instance.getMetric("${name}_start_time")?.current ?: 0.0
            if (startTime > 0) {
                val duration = System.currentTimeMillis() - startTime
                instance.recordMetric("${name}_duration", duration)
            }
        }

        fun incrementCounter(name: String) {
            getInstance().incrementCounter(name)
        }

        fun recordMetric(
            name: String,
            value: Double,
        ) {
            getInstance().recordMetric(name, value)
        }
    }
}
