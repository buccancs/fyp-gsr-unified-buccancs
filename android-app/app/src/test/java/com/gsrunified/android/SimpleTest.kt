package com.gsrunified.android

import com.gsrunified.android.core.PerformanceMonitor

/**
 * Simple test class to verify basic compilation without external dependencies
 */
class SimpleTest {
    fun testBasicFunctionality() {
        val monitor = PerformanceMonitor()
        // Basic test without JUnit assertions
        val result = monitor != null
        println("Test result: $result")
    }

    fun testPerformanceMonitorCreation() {
        PerformanceMonitor()
        println("PerformanceMonitor created successfully")
    }
}
