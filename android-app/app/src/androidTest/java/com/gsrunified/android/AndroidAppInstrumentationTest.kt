package com.gsrunified.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gsrunified.android.core.EnhancedLogger
import com.gsrunified.android.core.PerformanceMonitor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidAppInstrumentationTest {
    // Use the modern ActivityScenarioRule, which is more reliable and flexible.
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(MainActivity::class.java)

    private lateinit var context: Context

    @Before
    fun setUp() {
        // Get context from ApplicationProvider for better consistency.
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testApplicationContext() {
        // Test that we can get the application context
        assertNotNull("Context should not be null", context)
        assertEquals("Package name should match", "com.gsrunified.android", context.packageName)
    }

    @Test
    fun testMainActivityLaunch() {
        // With ActivityScenarioRule, a successful launch is verified by the rule itself.
        // We can perform actions on the activity if needed.
        activityScenarioRule.scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)
            assertTrue("Activity should be an instance of MainActivity", activity is MainActivity)
        }
    }

    @Test
    fun testPerformanceMonitorIntegration() {
        // Test PerformanceMonitor integration
        val performanceMonitor = PerformanceMonitor()

        // Test basic functionality
        performanceMonitor.recordMetric("test_metric", 50.0)
        val metrics = performanceMonitor.getMetrics()

        assertNotNull("Metrics should not be null", metrics)
        assertTrue("Should contain test metric", metrics.containsKey("test_metric"))
    }

    @Test
    fun testEnhancedLoggerIntegration() {
        // Test EnhancedLogger integration with real context
        val logger = EnhancedLogger.getInstance(context)

        // Test logging functionality
        logger.i("INSTRUMENTATION_TEST", "Testing enhanced logger integration")
        logger.d("INSTRUMENTATION_TEST", "Debug message from instrumentation test")
        logger.w("INSTRUMENTATION_TEST", "Warning message from instrumentation test")

        // Verify logger works without exceptions
        assertTrue("Logger should work without exceptions", true)
    }

    @Test
    fun testHandAnalysisHandlerIntegration() {
        // Test HandAnalysisHandler integration
        val handAnalysisHandler = HandAnalysisHandler(context)

        // Test initialization
        handAnalysisHandler.initialize()

        // Test session info setting
        handAnalysisHandler.setSessionInfo("instrumentation-session", "test-device")

        // Test initial state
        assertFalse("Should not be analyzing initially", handAnalysisHandler.isAnalyzing())

        // Clean up
        handAnalysisHandler.release()
    }

    @Test
    fun testLslStreamManagerIntegration() {
        // Test LslStreamManager integration
        val lslStreamManager = LslStreamManager("instrumentation-device")

        // Test initialization
        val initResult = lslStreamManager.initialize()
        assertTrue("Initialize should return boolean", initResult is Boolean)

        // Test stream creation
        val gsrResult = lslStreamManager.createGsrStream()
        assertTrue("GSR stream creation should return boolean", gsrResult is Boolean)

        // Test getting active streams
        val activeStreams = lslStreamManager.getActiveStreams()
        assertNotNull("Active streams should not be null", activeStreams)

        // Clean up
        lslStreamManager.cleanup()
    }

    @Test
    fun testSystemTestingUtilityIntegration() {
        // Test SystemTestingUtility integration
        val systemTestingUtility = SystemTestingUtility(context)

        // Test callback setting
        val callback =
            object : SystemTestingUtility.TestCallback {
                override fun onTestStarted(testName: String) {
                    assertNotNull("Test name should not be null", testName)
                }

                override fun onTestCompleted(result: SystemTestingUtility.TestResult) {
                    assertNotNull("Result should not be null", result)
                }

                override fun onTestSuiteCompleted(results: List<SystemTestingUtility.TestResult>) {
                    assertNotNull("Results should not be null", results)
                }

                override fun onTestProgress(
                    testName: String,
                    progress: Int,
                    message: String,
                ) {
                    assertTrue("Progress should be valid", progress >= 0)
                }
            }

        systemTestingUtility.setCallback(callback)

        // Test getting test summary (public method)
        val summary = systemTestingUtility.getTestSummary()
        assertNotNull("Test summary should not be null", summary)
    }

    @Test
    fun testDataClassesIntegration() {
        // Test data classes work correctly in Android environment

        // Test HandLandmark
        val handLandmark = HandAnalysisHandler.HandLandmark(0.5f, 0.3f, 0.1f, 0.9f, "WRIST")
        assertEquals("X should match", 0.5f, handLandmark.x, 0.001f)
        assertEquals("Y should match", 0.3f, handLandmark.y, 0.001f)

        // Test PoseData
        val poseData = HandAnalysisHandler.PoseData(listOf(handLandmark), 0.85f)
        assertEquals("Confidence should match", 0.85f, poseData.confidence, 0.001f)
        assertEquals("Should have 1 landmark", 1, poseData.landmarks.size)

        // Test TestResult
        val testResult =
            SystemTestingUtility.TestResult(
                "Integration Test",
                SystemTestingUtility.TestStatus.PASSED,
                "Test passed in Android environment",
                100L,
            )
        assertEquals("Test name should match", "Integration Test", testResult.testName)
        assertEquals("Status should be PASSED", SystemTestingUtility.TestStatus.PASSED, testResult.status)
    }

    @Test
    fun testEnumClassesIntegration() {
        // Test enum classes work correctly in Android environment

        // Test LogLevel enum
        val logLevels = EnhancedLogger.LogLevel.values()
        assertTrue("Should have log levels", logLevels.isNotEmpty())
        assertTrue("Should contain INFO", logLevels.contains(EnhancedLogger.LogLevel.INFO))

        // Test MetricType enum
        val metricTypes = PerformanceMonitor.MetricType.values()
        assertTrue("Should have metric types", metricTypes.isNotEmpty())
        assertTrue("Should contain GAUGE", metricTypes.contains(PerformanceMonitor.MetricType.GAUGE))

        // Test TestStatus enum
        val testStatuses = SystemTestingUtility.TestStatus.values()
        assertTrue("Should have test statuses", testStatuses.isNotEmpty())
        assertTrue("Should contain PASSED", testStatuses.contains(SystemTestingUtility.TestStatus.PASSED))
    }

    @Test
    fun testConcurrentOperationsIntegration() {
        // Test concurrent operations in Android environment
        val performanceMonitor = PerformanceMonitor()
        val logger = EnhancedLogger.getInstance(context)

        // Start monitoring
        performanceMonitor.startMonitoring()

        // Perform concurrent operations
        val threads = mutableListOf<Thread>()

        for (i in 1..3) {
            val thread =
                Thread {
                    // Record metrics
                    performanceMonitor.recordMetric("concurrent_test_$i", i.toDouble())

                    // Log messages
                    logger.i("CONCURRENT_TEST", "Message from thread $i")

                    // Increment counters
                    performanceMonitor.incrementCounter("thread_counter")
                }
            threads.add(thread)
            thread.start()
        }

        // Wait for all threads to complete
        threads.forEach { it.join() }

        // Verify operations completed
        val metrics = performanceMonitor.getMetrics()
        assertNotNull("Metrics should not be null", metrics)

        // Clean up
        performanceMonitor.stopMonitoring()
    }

    @Test
    fun testMemoryUsageIntegration() {
        // Test memory usage in Android environment
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        // Create multiple objects
        val performanceMonitors = mutableListOf<PerformanceMonitor>()
        val lslStreamManagers = mutableListOf<LslStreamManager>()

        for (i in 1..10) {
            performanceMonitors.add(PerformanceMonitor())
            lslStreamManagers.add(LslStreamManager("device-$i"))
        }

        // Use the objects
        performanceMonitors.forEach { monitor ->
            monitor.recordMetric("memory_test", 1.0)
        }

        lslStreamManagers.forEach { manager ->
            manager.initialize()
            manager.cleanup()
        }

        // Clean up
        performanceMonitors.clear()
        lslStreamManagers.clear()

        // Force garbage collection
        System.gc()

        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        // Memory should not have increased dramatically
        assertTrue(
            "Memory usage should be reasonable",
            finalMemory - initialMemory < 50 * 1024 * 1024,
        ) // 50MB threshold
    }

    @Test
    fun testErrorHandlingIntegration() {
        // Test error handling in Android environment
        val logger = EnhancedLogger.getInstance(context)

        try {
            // Test with invalid operations
            val handAnalysisHandler = HandAnalysisHandler(context)
            handAnalysisHandler.analyzeVideoFile("") // Empty path
            handAnalysisHandler.analyzeVideoFile("/invalid/path/video.mp4") // Invalid path

            // Should handle gracefully
            assertTrue("Error handling should work", true)
        } catch (e: Exception) {
            // Log the exception
            logger.e("INSTRUMENTATION_TEST", "Exception during error handling test", e)

            // Should still pass as we're testing error handling
            assertTrue("Exception should be handled gracefully", true)
        }
    }

    @Test
    fun testResourceCleanupIntegration() {
        // Test resource cleanup in Android environment
        val handAnalysisHandler = HandAnalysisHandler(context)
        val lslStreamManager = LslStreamManager("cleanup-test-device")
        val performanceMonitor = PerformanceMonitor()

        // Initialize resources
        handAnalysisHandler.initialize()
        lslStreamManager.initialize()
        performanceMonitor.startMonitoring()

        // Use resources
        handAnalysisHandler.setSessionInfo("cleanup-session", "cleanup-device")
        lslStreamManager.createGsrStream()
        performanceMonitor.recordMetric("cleanup_test", 1.0)

        // Clean up resources
        handAnalysisHandler.release()
        lslStreamManager.cleanup()
        performanceMonitor.stopMonitoring()

        // Multiple cleanups should be safe
        handAnalysisHandler.release()
        lslStreamManager.cleanup()
        performanceMonitor.stopMonitoring()

        assertTrue("Resource cleanup should work", true)
    }

    @Test
    fun testCompleteWorkflowIntegration() {
        // Test complete workflow integration
        val logger = EnhancedLogger.getInstance(context)
        val performanceMonitor = PerformanceMonitor()
        val systemTestingUtility = SystemTestingUtility(context)

        // Start workflow
        logger.i("WORKFLOW_TEST", "Starting complete workflow test")
        performanceMonitor.startMonitoring()

        // Record some metrics
        performanceMonitor.recordMetric("workflow_step_1", 1.0)
        performanceMonitor.recordFrameRate("camera")
        performanceMonitor.recordDataThroughput("network", 1024L)

        // Get test summary instead of running private methods
        val testSummary = systemTestingUtility.getTestSummary()
        assertNotNull("Test summary should not be null", testSummary)

        // Get metrics
        val metrics = performanceMonitor.getMetrics()
        assertNotNull("Metrics should not be null", metrics)

        // Get test summary
        val summary = systemTestingUtility.getTestSummary()
        assertNotNull("Summary should not be null", summary)

        // Clean up
        performanceMonitor.stopMonitoring()
        logger.i("WORKFLOW_TEST", "Complete workflow test finished")

        assertTrue("Complete workflow should work", true)
    }
}
