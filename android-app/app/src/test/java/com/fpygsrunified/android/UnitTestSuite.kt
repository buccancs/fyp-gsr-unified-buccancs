package com.fpygsrunified.android

import com.fpygsrunified.android.core.EnhancedLoggerTest
import com.fpygsrunified.android.core.PerformanceMonitorTest
import com.fpygsrunified.android.analysis.HandAnalysisHandlerTest
import com.fpygsrunified.android.testing.SystemTestingUtilityTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Unit test suite for testing individual components in isolation.
 * These tests focus on testing single classes and their methods
 * without external dependencies.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Core component unit tests
    EnhancedLoggerTest::class,
    PerformanceMonitorTest::class,
    
    // Analysis component unit tests
    HandAnalysisHandlerTest::class,
    
    // Testing utility unit tests
    SystemTestingUtilityTest::class
)
class UnitTestSuite