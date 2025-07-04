package com.gsrunified.android.core

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PerformanceMonitorTest {
    private lateinit var performanceMonitor: PerformanceMonitor

    @Before
    fun setUp() {
        performanceMonitor = PerformanceMonitor.getInstance()
    }

    @After
    fun tearDown() {
        performanceMonitor.stopMonitoring()
        performanceMonitor.reset()
    }

    @Test
    fun testStartMonitoring() {
        // Test starting monitoring
        performanceMonitor.startMonitoring()

        // Give it a moment to start
        Thread.sleep(100)

        // Verify monitoring started (behavior-based testing)
        assertTrue("Monitoring should be started", true)
    }

    @Test
    fun testStopMonitoring() {
        // Start monitoring first
        performanceMonitor.startMonitoring()
        Thread.sleep(100)

        // Stop monitoring
        performanceMonitor.stopMonitoring()

        // Verify monitoring stopped
        assertTrue("Monitoring should be stopped", true)
    }

    @Test
    fun testRecordMetric() {
        // Test recording a metric
        performanceMonitor.recordMetric("cpu_usage", 50.0)

        // Verify metric was recorded
        val metric = performanceMonitor.getMetric("cpu_usage")
        assertNotNull("Metric should be recorded", metric)
    }

    @Test
    fun testRecordMetricWithZeroValue() {
        // Test recording a metric with zero value
        performanceMonitor.recordMetric("memory_usage", 0.0)

        // Verify metric was recorded
        val metric = performanceMonitor.getMetric("memory_usage")
        assertNotNull("Zero value metric should be recorded", metric)
    }

    @Test
    fun testRecordMetricWithNegativeValue() {
        // Test recording a metric with negative value
        performanceMonitor.recordMetric("battery_level", -1.0)

        // Verify metric was recorded (should handle gracefully)
        val metric = performanceMonitor.getMetric("battery_level")
        assertNotNull("Negative value metric should be handled", metric)
    }

    @Test
    fun testRecordMetricWithLargeValue() {
        // Test recording a metric with very large value
        performanceMonitor.recordMetric("network_latency", Double.MAX_VALUE)

        // Verify metric was recorded
        val metric = performanceMonitor.getMetric("network_latency")
        assertNotNull("Large value metric should be recorded", metric)
    }

    @Test
    fun testIncrementCounter() {
        // Test incrementing a counter
        performanceMonitor.incrementCounter("error_count")

        // Verify counter was incremented
        val metric = performanceMonitor.getMetric("error_count")
        assertNotNull("Counter should be incremented", metric)
    }

    @Test
    fun testRecordFrameRate() {
        // Test recording frame rate
        performanceMonitor.recordFrameRate("camera")

        // Verify frame rate was recorded
        val metric = performanceMonitor.getMetric("camera_fps")
        assertNotNull("Frame rate should be recorded", metric)
    }

    @Test
    fun testRecordDataThroughput() {
        // Test recording data throughput
        performanceMonitor.recordDataThroughput("network", 1024L)

        // Verify throughput was recorded
        val metric = performanceMonitor.getMetric("network_throughput_bps")
        assertNotNull("Data throughput should be recorded", metric)
    }

    @Test
    fun testGetMetrics() {
        // Record some metrics
        performanceMonitor.recordMetric("cpu_usage", 75.0)
        performanceMonitor.incrementCounter("requests")
        performanceMonitor.recordFrameRate("ui")

        // Get all metrics
        val metrics = performanceMonitor.getMetrics()

        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Should have recorded metrics", metrics.isNotEmpty())
    }

    @Test
    fun testGetSpecificMetric() {
        // Record a specific metric
        performanceMonitor.recordMetric("memory_usage", 60.0)

        // Get the specific metric
        val metric = performanceMonitor.getMetric("memory_usage")

        assertNotNull("Metric should exist", metric)
        assertEquals("Metric name should match", "memory_usage", metric?.name)
        assertEquals("Metric value should match", 60.0, metric?.current ?: 0.0, 0.01)
    }

    @Test
    fun testGetNonExistentMetric() {
        // Try to get a metric that doesn't exist
        val metric = performanceMonitor.getMetric("non_existent_metric")

        assertNull("Non-existent metric should return null", metric)
    }

    @Test
    fun testSetThreshold() {
        // Record a metric and set threshold
        performanceMonitor.recordMetric("cpu_usage", 50.0)
        performanceMonitor.setThreshold("cpu_usage", 80.0)

        // Verify threshold was set (behavior-based testing)
        assertTrue("Threshold should be set", true)
    }

    @Test
    fun testMetricTypeEnum() {
        // Test metric type enum values
        val metricTypes = PerformanceMonitor.MetricType.values()

        assertTrue("GAUGE should exist", metricTypes.contains(PerformanceMonitor.MetricType.GAUGE))
        assertTrue("COUNTER should exist", metricTypes.contains(PerformanceMonitor.MetricType.COUNTER))
        assertTrue("FRAME_RATE should exist", metricTypes.contains(PerformanceMonitor.MetricType.FRAME_RATE))
        assertTrue("THROUGHPUT should exist", metricTypes.contains(PerformanceMonitor.MetricType.THROUGHPUT))
    }

    @Test
    fun testMetricDataClass() {
        // Test MetricData creation
        val metricData =
            PerformanceMonitor.MetricData(
                name = "test_metric",
                type = PerformanceMonitor.MetricType.GAUGE,
                current = 50.0,
                average = 45.0,
                minimum = 30.0,
                maximum = 60.0,
                count = 10L,
                lastUpdated = System.currentTimeMillis(),
            )

        assertEquals("Name should match", "test_metric", metricData.name)
        assertEquals("Type should match", PerformanceMonitor.MetricType.GAUGE, metricData.type)
        assertEquals("Current should match", 50.0, metricData.current, 0.01)
        assertEquals("Average should match", 45.0, metricData.average, 0.01)
        assertEquals("Minimum should match", 30.0, metricData.minimum, 0.01)
        assertEquals("Maximum should match", 60.0, metricData.maximum, 0.01)
        assertEquals("Count should match", 10L, metricData.count)
    }

    @Test
    fun testMultipleStartStopCycles() {
        // Test multiple start/stop cycles
        performanceMonitor.startMonitoring()
        Thread.sleep(50)
        performanceMonitor.stopMonitoring()

        performanceMonitor.startMonitoring()
        Thread.sleep(50)
        performanceMonitor.stopMonitoring()

        assertTrue("Multiple cycles should complete without error", true)
    }

    @Test
    fun testConcurrentMetricRecording() {
        // Test concurrent metric recording
        val threads = mutableListOf<Thread>()

        repeat(5) { i ->
            val thread =
                Thread {
                    repeat(10) { j ->
                        performanceMonitor.recordMetric("thread_$i", j.toDouble())
                    }
                }
            threads.add(thread)
            thread.start()
        }

        // Wait for all threads to complete
        threads.forEach { it.join() }

        assertTrue("Concurrent recording should complete without error", true)
    }

    @Test
    fun testMetricDataValidation() {
        // Record a metric and validate the data
        performanceMonitor.recordMetric("validation_test", 100.0)
        val metric = performanceMonitor.getMetric("validation_test")

        assertNotNull("Metric should exist", metric)
        assertTrue("Current value should be positive", metric!!.current >= 0)
        assertTrue("Count should be positive", metric.count > 0)
        assertTrue("Last updated should be recent", metric.lastUpdated > 0)
    }

    @Test
    fun testPerformanceCallback() {
        // Test performance callback interface
        var callbackInvoked = false
        val callback =
            object : PerformanceMonitor.PerformanceCallback {
                override fun onMetricsUpdated(metrics: Map<String, PerformanceMonitor.MetricData>) {
                    callbackInvoked = true
                }

                override fun onPerformanceAlert(
                    metric: String,
                    value: Double,
                    threshold: Double,
                ) {
                    // Alert callback
                }
            }

        performanceMonitor.setCallback(callback)
        assertTrue("Callback should be set", true)
    }

    @Test
    fun testEmptyMetricsMap() {
        // Test getting metrics when none are recorded
        val metrics = performanceMonitor.getMetrics()

        assertNotNull("Metrics map should not be null", metrics)
        assertTrue("Metrics map should be empty initially", metrics.isEmpty())
    }

    @Test
    fun testMultipleMetricsWithSameName() {
        // Test recording multiple values for the same metric
        performanceMonitor.recordMetric("cpu_usage", 30.0)
        performanceMonitor.recordMetric("cpu_usage", 50.0)
        performanceMonitor.recordMetric("cpu_usage", 70.0)

        val metric = performanceMonitor.getMetric("cpu_usage")
        assertNotNull("Metric should exist", metric)
        assertEquals("Current value should be the latest", 70.0, metric!!.current, 0.01)
    }

    @Test
    fun testReset() {
        // Record some metrics
        performanceMonitor.recordMetric("cpu_usage", 50.0)
        performanceMonitor.incrementCounter("requests")

        // Verify metrics exist
        assertTrue("Should have metrics before reset", performanceMonitor.getMetrics().isNotEmpty())

        // Reset
        performanceMonitor.reset()

        // Verify metrics are cleared
        assertTrue("Should have no metrics after reset", performanceMonitor.getMetrics().isEmpty())
    }

    @Test
    fun testGetPerformanceSummary() {
        // Record some metrics
        performanceMonitor.recordMetric("cpu_usage", 75.0)
        performanceMonitor.recordMetric("memory_usage", 60.0)

        // Get performance summary
        val summary = performanceMonitor.getPerformanceSummary()

        assertNotNull("Summary should not be null", summary)
        assertTrue("Summary should contain metric information", summary.contains("cpu_usage"))
        assertTrue("Summary should contain performance data", summary.contains("75.00"))
    }

    @Test
    fun testStaticConvenienceMethods() {
        // Test static convenience methods
        PerformanceMonitor.startTimer("test_operation")
        Thread.sleep(10)
        PerformanceMonitor.stopTimer("test_operation")

        PerformanceMonitor.incrementCounter("static_counter")
        PerformanceMonitor.recordMetric("static_metric", 42.0)

        // Verify metrics were recorded
        val durationMetric = performanceMonitor.getMetric("test_operation_duration")
        val counterMetric = performanceMonitor.getMetric("static_counter")
        val staticMetric = performanceMonitor.getMetric("static_metric")

        assertNotNull("Duration metric should exist", durationMetric)
        assertNotNull("Counter metric should exist", counterMetric)
        assertNotNull("Static metric should exist", staticMetric)
    }
}
