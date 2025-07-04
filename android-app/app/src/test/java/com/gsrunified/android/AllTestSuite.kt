package com.gsrunified.android

import com.gsrunified.android.analysis.HandAnalysisHandlerTest
import com.gsrunified.android.camera.CameraTestSuite
import com.gsrunified.android.core.CoreTestSuite
import com.gsrunified.android.network.NetworkHandlerTest
import com.gsrunified.android.testing.SystemTestingUtilityTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite that includes all tests in the project.
 * This suite groups all test classes for complete project testing.
 *
 * Test Organization:
 * - Core functionality (logging, performance monitoring)
 * - Camera functionality (video recording, frame capture)
 * - Network functionality (communication, data streaming)
 * - Analysis functionality (hand detection, pose analysis)
 * - Testing utilities (system testing, validation)
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Core functionality tests
    CoreTestSuite::class,
    // Camera functionality tests
    CameraTestSuite::class,
    // Network functionality tests
    NetworkHandlerTest::class,
    // Analysis functionality tests
    HandAnalysisHandlerTest::class,
    // Testing utilities tests
    SystemTestingUtilityTest::class,
    // LSL functionality tests
    LslStreamManagerTest::class,
    LslCommandInletTest::class,
)
class AllTestSuite
