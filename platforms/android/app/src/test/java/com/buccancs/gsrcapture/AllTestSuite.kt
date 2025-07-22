package com.buccancs.gsrcapture

import com.buccancs.gsrcapture.analytics.DataAnalyticsTest
import com.buccancs.gsrcapture.camera.ThermalCameraManagerTest
import com.buccancs.gsrcapture.controller.RecordingControllerShutdownTest
import com.buccancs.gsrcapture.controller.RecordingControllerTest
import com.buccancs.gsrcapture.export.DataExporterTest
import com.buccancs.gsrcapture.MainActivityTest
import com.buccancs.gsrcapture.network.NetworkClientTest
import com.buccancs.gsrcapture.sensor.GsrSensorManagerTest
import com.buccancs.gsrcapture.ui.UIWorkflowTest
import com.buccancs.gsrcapture.utils.TimeManagerTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Comprehensive test suite that runs all unit tests in the project.
 * This provides a single entry point to run all tests and ensures
 * proper test organization and coverage.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Utility Tests
    TimeManagerTest::class,

    // Analytics Tests
    DataAnalyticsTest::class,

    // Controller Tests
    RecordingControllerTest::class,
    RecordingControllerShutdownTest::class,

    // UI Tests
    MainActivityTest::class,
    UIWorkflowTest::class,

    // Sensor Tests
    GsrSensorManagerTest::class,

    // Camera Tests
    ThermalCameraManagerTest::class,

    // Network Tests
    NetworkClientTest::class,

    // Export Tests
    DataExporterTest::class
)
class AllTestSuite {
    companion object {
        /**
         * Test categories for organized test execution
         */
        object Categories {
            const val UNIT = "unit"
            const val INTEGRATION = "integration"
            const val ANALYTICS = "analytics"
            const val CONTROLLER = "controller"
            const val UI = "ui"
            const val SENSOR = "sensor"
            const val NETWORK = "network"
            const val EXPORT = "export"
            const val UTILITY = "utility"
        }

        /**
         * Test priorities for execution order
         */
        object Priority {
            const val HIGH = 1
            const val MEDIUM = 2
            const val LOW = 3
        }
    }
}
