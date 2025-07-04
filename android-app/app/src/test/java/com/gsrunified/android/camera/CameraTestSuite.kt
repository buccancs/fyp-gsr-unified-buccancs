package com.gsrunified.android.camera

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for camera functionality including video recording and frame capture.
 * This suite groups all camera-related tests for easier execution and organization.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    CameraHandlerTest::class,
)
class CameraTestSuite
