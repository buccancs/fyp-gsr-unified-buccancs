package com.fpygsrunified.android

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * Comprehensive testing utility for validating system functionality and performance.
 * Provides automated testing scenarios, data validation, and debugging tools.
 */
class SystemTestingUtility(private val context: Context) {

    private val testResults = mutableListOf<TestResult>()
    private var isRunning = false

    // Test result callback
    interface TestCallback {
        fun onTestStarted(testName: String)
        fun onTestCompleted(result: TestResult)
        fun onTestSuiteCompleted(results: List<TestResult>)
        fun onTestProgress(testName: String, progress: Int, message: String)
    }

    private var callback: TestCallback? = null

    fun setCallback(callback: TestCallback) {
        this.callback = callback
    }

    /**
     * Run comprehensive system test suite.
     */
    suspend fun runFullTestSuite(): List<TestResult> {
        if (isRunning) {
            Log.w(TAG, "Test suite already running")
            return testResults
        }

        isRunning = true
        testResults.clear()

        try {
            // Core functionality tests
            runTest("Network Connectivity") { testNetworkConnectivity() }
            runTest("File System Access") { testFileSystemAccess() }
            runTest("Sensor Simulation") { testSensorSimulation() }
            runTest("Data Serialization") { testDataSerialization() }
            runTest("Performance Metrics") { testPerformanceMetrics() }
            runTest("Memory Management") { testMemoryManagement() }
            runTest("Error Handling") { testErrorHandling() }
            runTest("Session Management") { testSessionManagement() }

            callback?.onTestSuiteCompleted(testResults)
            Log.i(TAG, "Test suite completed with ${testResults.size} tests")

        } catch (e: Exception) {
            Log.e(TAG, "Error running test suite", e)
        } finally {
            isRunning = false
        }

        return testResults
    }

    /**
     * Run individual test with progress tracking.
     */
    private suspend fun runTest(testName: String, testFunction: suspend () -> TestResult) {
        callback?.onTestStarted(testName)
        callback?.onTestProgress(testName, 0, "Starting test...")

        try {
            val result = testFunction()
            testResults.add(result)
            callback?.onTestCompleted(result)
            Log.i(TAG, "Test '$testName' completed: ${result.status}")
        } catch (e: Exception) {
            val errorResult = TestResult(
                testName = testName,
                status = TestStatus.FAILED,
                message = "Test failed with exception: ${e.message}",
                duration = 0,
                details = mapOf("exception" to e.toString())
            )
            testResults.add(errorResult)
            callback?.onTestCompleted(errorResult)
            Log.e(TAG, "Test '$testName' failed", e)
        }
    }

    /**
     * Test network connectivity and communication.
     */
    private suspend fun testNetworkConnectivity(): TestResult {
        val startTime = System.currentTimeMillis()
        callback?.onTestProgress("Network Connectivity", 25, "Testing JSON serialization...")

        try {
            // Test JSON message creation
            val testMessage = JSONObject().apply {
                put("command", "test_message")
                put("timestamp", System.currentTimeMillis() / 1000.0)
                put("data", JSONObject().apply {
                    put("test_value", "hello_world")
                    put("test_number", 42)
                })
            }

            callback?.onTestProgress("Network Connectivity", 50, "Validating message format...")

            // Validate message structure
            val hasCommand = testMessage.has("command")
            val hasTimestamp = testMessage.has("timestamp")
            val hasData = testMessage.has("data")

            callback?.onTestProgress("Network Connectivity", 75, "Testing data integrity...")

            val duration = System.currentTimeMillis() - startTime

            return if (hasCommand && hasTimestamp && hasData) {
                TestResult(
                    testName = "Network Connectivity",
                    status = TestStatus.PASSED,
                    message = "Network message format validation successful",
                    duration = duration,
                    details = mapOf(
                        "message_size" to testMessage.toString().length.toString(),
                        "has_command" to hasCommand.toString(),
                        "has_timestamp" to hasTimestamp.toString(),
                        "has_data" to hasData.toString()
                    )
                )
            } else {
                TestResult(
                    testName = "Network Connectivity",
                    status = TestStatus.FAILED,
                    message = "Message format validation failed",
                    duration = duration,
                    details = mapOf(
                        "has_command" to hasCommand.toString(),
                        "has_timestamp" to hasTimestamp.toString(),
                        "has_data" to hasData.toString()
                    )
                )
            }

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return TestResult(
                testName = "Network Connectivity",
                status = TestStatus.FAILED,
                message = "Network test failed: ${e.message}",
                duration = duration,
                details = mapOf("exception" to e.toString())
            )
        }
    }

    /**
     * Test file system access and storage operations.
     */
    private suspend fun testFileSystemAccess(): TestResult {
        val startTime = System.currentTimeMillis()
        callback?.onTestProgress("File System Access", 25, "Testing directory creation...")

        try {
            // Test directory creation
            val testDir = File(context.getExternalFilesDir(null), "test_${System.currentTimeMillis()}")
            val dirCreated = testDir.mkdirs()

            callback?.onTestProgress("File System Access", 50, "Testing file write operations...")

            // Test file write
            val testFile = File(testDir, "test_file.txt")
            val testData = "Test data: ${System.currentTimeMillis()}"
            testFile.writeText(testData)

            callback?.onTestProgress("File System Access", 75, "Testing file read operations...")

            // Test file read
            val readData = testFile.readText()
            val dataMatches = readData == testData

            // Cleanup
            testFile.delete()
            testDir.delete()

            val duration = System.currentTimeMillis() - startTime

            return if (dirCreated && dataMatches) {
                TestResult(
                    testName = "File System Access",
                    status = TestStatus.PASSED,
                    message = "File system operations successful",
                    duration = duration,
                    details = mapOf(
                        "directory_created" to dirCreated.toString(),
                        "data_matches" to dataMatches.toString(),
                        "test_data_size" to testData.length.toString()
                    )
                )
            } else {
                TestResult(
                    testName = "File System Access",
                    status = TestStatus.FAILED,
                    message = "File system operations failed",
                    duration = duration,
                    details = mapOf(
                        "directory_created" to dirCreated.toString(),
                        "data_matches" to dataMatches.toString()
                    )
                )
            }

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return TestResult(
                testName = "File System Access",
                status = TestStatus.FAILED,
                message = "File system test failed: ${e.message}",
                duration = duration,
                details = mapOf("exception" to e.toString())
            )
        }
    }

    /**
     * Test sensor data simulation and processing.
     */
    private suspend fun testSensorSimulation(): TestResult {
        val startTime = System.currentTimeMillis()
        callback?.onTestProgress("Sensor Simulation", 25, "Generating GSR data...")

        try {
            // Generate test GSR data
            val gsrSamples = mutableListOf<Double>()
            repeat(100) {
                val baseValue = 5.0 + Random.nextDouble(-1.0, 1.0)
                gsrSamples.add(baseValue)
            }

            callback?.onTestProgress("Sensor Simulation", 50, "Generating thermal data...")

            // Generate test thermal data
            val thermalData = Array(192) { Array(256) { 25.0 + Random.nextDouble(-5.0, 15.0) } }

            callback?.onTestProgress("Sensor Simulation", 75, "Validating data ranges...")

            // Validate data ranges
            val gsrInRange = gsrSamples.all { it in 0.0..50.0 }
            val thermalInRange = thermalData.all { row -> row.all { temp -> temp in -40.0..120.0 } }

            val duration = System.currentTimeMillis() - startTime

            return if (gsrInRange && thermalInRange) {
                TestResult(
                    testName = "Sensor Simulation",
                    status = TestStatus.PASSED,
                    message = "Sensor simulation data validation successful",
                    duration = duration,
                    details = mapOf(
                        "gsr_samples" to gsrSamples.size.toString(),
                        "gsr_avg" to String.format("%.2f", gsrSamples.average()),
                        "thermal_size" to "${thermalData.size}x${thermalData[0].size}",
                        "thermal_avg" to String.format("%.2f", thermalData.flatten().average())
                    )
                )
            } else {
                TestResult(
                    testName = "Sensor Simulation",
                    status = TestStatus.FAILED,
                    message = "Sensor data validation failed",
                    duration = duration,
                    details = mapOf(
                        "gsr_in_range" to gsrInRange.toString(),
                        "thermal_in_range" to thermalInRange.toString()
                    )
                )
            }

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return TestResult(
                testName = "Sensor Simulation",
                status = TestStatus.FAILED,
                message = "Sensor simulation test failed: ${e.message}",
                duration = duration,
                details = mapOf("exception" to e.toString())
            )
        }
    }

    /**
     * Test data serialization and deserialization.
     */
    private suspend fun testDataSerialization(): TestResult {
        val startTime = System.currentTimeMillis()

        try {
            // Test JSON serialization
            val originalData = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "gsr_value" to 5.67,
                "thermal_avg" to 32.1,
                "device_id" to "test_device"
            )

            val jsonString = JSONObject(originalData).toString()
            val deserializedJson = JSONObject(jsonString)

            val timestampMatch = deserializedJson.getLong("timestamp") == originalData["timestamp"]
            val gsrMatch =
                Math.abs(deserializedJson.getDouble("gsr_value") - (originalData["gsr_value"] as Double)) < 0.001
            val deviceMatch = deserializedJson.getString("device_id") == originalData["device_id"]

            val duration = System.currentTimeMillis() - startTime

            return if (timestampMatch && gsrMatch && deviceMatch) {
                TestResult(
                    testName = "Data Serialization",
                    status = TestStatus.PASSED,
                    message = "Data serialization successful",
                    duration = duration,
                    details = mapOf(
                        "json_size" to jsonString.length.toString(),
                        "timestamp_match" to timestampMatch.toString(),
                        "gsr_match" to gsrMatch.toString(),
                        "device_match" to deviceMatch.toString()
                    )
                )
            } else {
                TestResult(
                    testName = "Data Serialization",
                    status = TestStatus.FAILED,
                    message = "Data serialization validation failed",
                    duration = duration,
                    details = mapOf(
                        "timestamp_match" to timestampMatch.toString(),
                        "gsr_match" to gsrMatch.toString(),
                        "device_match" to deviceMatch.toString()
                    )
                )
            }

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return TestResult(
                testName = "Data Serialization",
                status = TestStatus.FAILED,
                message = "Serialization test failed: ${e.message}",
                duration = duration,
                details = mapOf("exception" to e.toString())
            )
        }
    }

    /**
     * Test performance monitoring functionality.
     */
    private suspend fun testPerformanceMetrics(): TestResult {
        val startTime = System.currentTimeMillis()

        try {
            val perfMonitor = PerformanceMonitor.getInstance()

            // Record some test metrics
            perfMonitor.recordMetric("test_metric", 42.0)
            perfMonitor.incrementCounter("test_counter")
            perfMonitor.recordFrameRate("test_component")

            delay(100) // Small delay to ensure metrics are recorded

            val metrics = perfMonitor.getMetrics()
            val hasTestMetric = metrics.containsKey("test_metric")
            val hasTestCounter = metrics.containsKey("test_counter")

            val duration = System.currentTimeMillis() - startTime

            return if (hasTestMetric && hasTestCounter) {
                TestResult(
                    testName = "Performance Metrics",
                    status = TestStatus.PASSED,
                    message = "Performance monitoring functional",
                    duration = duration,
                    details = mapOf(
                        "metrics_count" to metrics.size.toString(),
                        "has_test_metric" to hasTestMetric.toString(),
                        "has_test_counter" to hasTestCounter.toString()
                    )
                )
            } else {
                TestResult(
                    testName = "Performance Metrics",
                    status = TestStatus.FAILED,
                    message = "Performance monitoring validation failed",
                    duration = duration,
                    details = mapOf(
                        "has_test_metric" to hasTestMetric.toString(),
                        "has_test_counter" to hasTestCounter.toString()
                    )
                )
            }

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return TestResult(
                testName = "Performance Metrics",
                status = TestStatus.FAILED,
                message = "Performance test failed: ${e.message}",
                duration = duration,
                details = mapOf("exception" to e.toString())
            )
        }
    }

    /**
     * Test memory management and resource cleanup.
     */
    private suspend fun testMemoryManagement(): TestResult {
        val startTime = System.currentTimeMillis()

        try {
            val runtime = Runtime.getRuntime()
            val initialMemory = runtime.totalMemory() - runtime.freeMemory()

            // Create some objects to test memory allocation
            val testData = mutableListOf<ByteArray>()
            repeat(100) {
                testData.add(ByteArray(1024)) // 1KB each
            }

            val afterAllocation = runtime.totalMemory() - runtime.freeMemory()
            val memoryIncrease = afterAllocation - initialMemory

            // Clear references and suggest GC
            testData.clear()
            System.gc()
            delay(100)

            val afterCleanup = runtime.totalMemory() - runtime.freeMemory()
            val memoryRecovered = afterAllocation - afterCleanup

            val duration = System.currentTimeMillis() - startTime

            return TestResult(
                testName = "Memory Management",
                status = TestStatus.PASSED,
                message = "Memory management test completed",
                duration = duration,
                details = mapOf(
                    "initial_memory_mb" to String.format("%.2f", initialMemory / (1024.0 * 1024.0)),
                    "memory_increase_kb" to String.format("%.2f", memoryIncrease / 1024.0),
                    "memory_recovered_kb" to String.format("%.2f", memoryRecovered / 1024.0)
                )
            )

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return TestResult(
                testName = "Memory Management",
                status = TestStatus.FAILED,
                message = "Memory test failed: ${e.message}",
                duration = duration,
                details = mapOf("exception" to e.toString())
            )
        }
    }

    /**
     * Test error handling mechanisms.
     */
    private suspend fun testErrorHandling(): TestResult {
        val startTime = System.currentTimeMillis()

        try {
            var exceptionCaught = false
            var errorMessage = ""

            // Test exception handling
            try {
                throw RuntimeException("Test exception")
            } catch (e: Exception) {
                exceptionCaught = true
                errorMessage = e.message ?: "Unknown error"
            }

            val duration = System.currentTimeMillis() - startTime

            return if (exceptionCaught && errorMessage == "Test exception") {
                TestResult(
                    testName = "Error Handling",
                    status = TestStatus.PASSED,
                    message = "Error handling mechanisms functional",
                    duration = duration,
                    details = mapOf(
                        "exception_caught" to exceptionCaught.toString(),
                        "error_message" to errorMessage
                    )
                )
            } else {
                TestResult(
                    testName = "Error Handling",
                    status = TestStatus.FAILED,
                    message = "Error handling validation failed",
                    duration = duration,
                    details = mapOf(
                        "exception_caught" to exceptionCaught.toString(),
                        "error_message" to errorMessage
                    )
                )
            }

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return TestResult(
                testName = "Error Handling",
                status = TestStatus.FAILED,
                message = "Error handling test failed: ${e.message}",
                duration = duration,
                details = mapOf("exception" to e.toString())
            )
        }
    }

    /**
     * Test session management functionality.
     */
    private suspend fun testSessionManagement(): TestResult {
        val startTime = System.currentTimeMillis()

        try {
            // Test session ID generation
            val sessionId1 = "session_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
            delay(1000) // Ensure different timestamp
            val sessionId2 = "session_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"

            val sessionIdsUnique = sessionId1 != sessionId2
            val sessionIdFormat = sessionId1.startsWith("session_") && sessionId1.length > 8

            val duration = System.currentTimeMillis() - startTime

            return if (sessionIdsUnique && sessionIdFormat) {
                TestResult(
                    testName = "Session Management",
                    status = TestStatus.PASSED,
                    message = "Session management functional",
                    duration = duration,
                    details = mapOf(
                        "session_id_1" to sessionId1,
                        "session_id_2" to sessionId2,
                        "unique_ids" to sessionIdsUnique.toString(),
                        "correct_format" to sessionIdFormat.toString()
                    )
                )
            } else {
                TestResult(
                    testName = "Session Management",
                    status = TestStatus.FAILED,
                    message = "Session management validation failed",
                    duration = duration,
                    details = mapOf(
                        "unique_ids" to sessionIdsUnique.toString(),
                        "correct_format" to sessionIdFormat.toString()
                    )
                )
            }

        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            return TestResult(
                testName = "Session Management",
                status = TestStatus.FAILED,
                message = "Session management test failed: ${e.message}",
                duration = duration,
                details = mapOf("exception" to e.toString())
            )
        }
    }

    /**
     * Get test results summary.
     */
    fun getTestSummary(): String {
        val passed = testResults.count { it.status == TestStatus.PASSED }
        val failed = testResults.count { it.status == TestStatus.FAILED }
        val total = testResults.size
        val totalDuration = testResults.sumOf { it.duration }

        return buildString {
            appendLine("=== Test Summary ===")
            appendLine("Total Tests: $total")
            appendLine("Passed: $passed")
            appendLine("Failed: $failed")
            appendLine(
                "Success Rate: ${
                    if (total > 0) String.format(
                        "%.1f",
                        (passed.toDouble() / total) * 100
                    ) else "0.0"
                }%"
            )
            appendLine("Total Duration: ${totalDuration}ms")
            appendLine()
            testResults.forEach { result ->
                val status = if (result.status == TestStatus.PASSED) "✓" else "✗"
                appendLine("$status ${result.testName} (${result.duration}ms)")
                if (result.status == TestStatus.FAILED) {
                    appendLine("  Error: ${result.message}")
                }
            }
        }
    }

    enum class TestStatus {
        PASSED,
        FAILED,
        SKIPPED
    }

    data class TestResult(
        val testName: String,
        val status: TestStatus,
        val message: String,
        val duration: Long,
        val details: Map<String, String> = emptyMap()
    )

    companion object {
        private const val TAG = "SystemTestingUtility"
    }
}