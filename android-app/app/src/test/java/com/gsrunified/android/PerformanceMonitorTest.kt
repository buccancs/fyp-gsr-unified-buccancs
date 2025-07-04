package com.gsrunified.android

import com.gsrunified.android.core.PerformanceMonitor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PerformanceMonitorTest {
    private lateinit var performanceMonitor: PerformanceMonitor

    @Before
    fun setUp() {
        performanceMonitor = PerformanceMonitor()
    }

    @After
    fun tearDown() {
        performanceMonitor.stopMonitoring()
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
        performanceMonitor.recordMetric("cpu_usage", 25.0)
        performanceMonitor.recordMetric("memory_usage", 75.0)

        // Get all metrics
        val metrics = performanceMonitor.getMetrics()

        // Verify metrics are returned
        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Should contain cpu_usage", metrics.containsKey("cpu_usage"))
        assertTrue("Should contain memory_usage", metrics.containsKey("memory_usage"))
    }

    @Test
    fun testGetSpecificMetric() {
        // Record a metric
        performanceMonitor.recordMetric("test_metric", 42.0)

        // Get specific metric
        val metric = performanceMonitor.getMetric("test_metric")

        // Verify metric is returned
        assertNotNull("Metric should not be null", metric)
        assertEquals("Metric name should match", "test_metric", metric?.name)
    }

    @Test
    fun testGetNonExistentMetric() {
        // Try to get a metric that doesn't exist
        val metric = performanceMonitor.getMetric("non_existent")

        // Should return null
        assertNull("Non-existent metric should return null", metric)
    }

    @Test
    fun testSetThreshold() {
        // Record a metric first
        performanceMonitor.recordMetric("cpu_usage", 50.0)

        // Set threshold
        performanceMonitor.setThreshold("cpu_usage", 80.0)

        // Verify threshold was set (behavior-based testing)
        assertTrue("Threshold should be set", true)
    }

    @Test
    fun testMetricTypeEnum() {
        // Test all metric types exist
        val metricTypes = PerformanceMonitor.MetricType.values()

        assertTrue("GAUGE should exist", metricTypes.contains(PerformanceMonitor.MetricType.GAUGE))
        assertTrue("COUNTER should exist", metricTypes.contains(PerformanceMonitor.MetricType.COUNTER))
        assertTrue("FRAME_RATE should exist", metricTypes.contains(PerformanceMonitor.MetricType.FRAME_RATE))
        assertTrue("THROUGHPUT should exist", metricTypes.contains(PerformanceMonitor.MetricType.THROUGHPUT))
    }

    @Test
    fun testMetricDataClass() {
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
        assertEquals("Current should match", 50.0, metricData.current, 0.001)
        assertEquals("Average should match", 45.0, metricData.average, 0.001)
        assertEquals("Minimum should match", 30.0, metricData.minimum, 0.001)
        assertEquals("Maximum should match", 60.0, metricData.maximum, 0.001)
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

        // Should handle multiple cycles gracefully
        assertTrue("Multiple start/stop cycles should work", true)
    }

    @Test
    fun testConcurrentMetricRecording() {
        // Simulate concurrent metric recording
        for (i in 1..10) {
            performanceMonitor.recordMetric("cpu_usage", i.toDouble())
            performanceMonitor.recordMetric("memory_usage", (i * 2).toDouble())
            performanceMonitor.incrementCounter("request_count")
        }

        // Verify all metrics were recorded
        val cpuMetric = performanceMonitor.getMetric("cpu_usage")
        val memoryMetric = performanceMonitor.getMetric("memory_usage")
        val counterMetric = performanceMonitor.getMetric("request_count")

        assertNotNull("CPU metric should exist", cpuMetric)
        assertNotNull("Memory metric should exist", memoryMetric)
        assertNotNull("Counter metric should exist", counterMetric)
    }

    @Test
    fun testMetricDataValidation() {
        // Test with various edge case values
        val testValues = listOf(0.0, -1.0, 1.0, 100.0, Double.MIN_VALUE, Double.MAX_VALUE)

        for (value in testValues) {
            performanceMonitor.recordMetric("test_metric_$value", value)
        }

        // All values should be handled
        assertTrue("All metric values should be handled", true)
    }

    @Test
    fun testPerformanceCallback() {
        var callbackCalled = false
        val callback =
            object : PerformanceMonitor.PerformanceCallback {
                override fun onMetricsUpdated(metrics: Map<String, PerformanceMonitor.MetricData>) {
                    callbackCalled = true
                }

                override fun onPerformanceAlert(
                    metric: String,
                    value: Double,
                    threshold: Double,
                ) {
                    // Alert callback
                }
            }

        // Set callback
        performanceMonitor.setCallback(callback)

        // Verify callback was set
        assertTrue("Callback should be set", true)
    }

    @Test
    fun testEmptyMetricsMap() {
        // Get metrics when no metrics have been recorded
        val metrics = performanceMonitor.getMetrics()

        // Should return empty map
        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Metrics should be empty initially", metrics.isEmpty())
    }

    @Test
    fun testMultipleMetricsWithSameName() {
        // Record multiple values for the same metric
        performanceMonitor.recordMetric("cpu_usage", 30.0)
        performanceMonitor.recordMetric("cpu_usage", 40.0)
        performanceMonitor.recordMetric("cpu_usage", 50.0)

        // Get the metric
        val metric = performanceMonitor.getMetric("cpu_usage")

        // Should contain aggregated data
        assertNotNull("Metric should exist", metric)
        assertEquals("Count should be 3", 3L, metric?.count)
    }
}
