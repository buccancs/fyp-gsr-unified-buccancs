package com.gsrunified.android.core

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for core functionality including logging and performance monitoring.
 * This suite groups all core-related tests for easier execution and organization.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    EnhancedLoggerTest::class,
    PerformanceMonitorTest::class,
)
class CoreTestSuite
