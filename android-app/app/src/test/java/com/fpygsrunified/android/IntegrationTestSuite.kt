package com.fpygsrunified.android

import com.fpygsrunified.android.camera.CameraHandlerTest
import com.fpygsrunified.android.network.NetworkHandlerTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Integration test suite for testing component interactions.
 * These tests focus on testing how multiple components work together
 * and their integration points.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Camera integration tests
    CameraHandlerTest::class,
    
    // Network integration tests
    NetworkHandlerTest::class
)
class IntegrationTestSuite