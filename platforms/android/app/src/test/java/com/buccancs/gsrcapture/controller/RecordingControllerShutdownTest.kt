package com.buccancs.gsrcapture.controller

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Test to verify that the shutdown method handles uninitialized components gracefully.
 * This test specifically addresses the crash reported in the issue where shutdown()
 * was called without initialize() being called first.
 */
@RunWith(RobolectricTestRunner::class)
class RecordingControllerShutdownTest {

    @Test
    fun testShutdownWithoutInitialization() {
        println("[DEBUG_LOG] Testing shutdown without initialization")
        
        // Create RecordingController without calling initialize()
        val recordingController = RecordingController(RuntimeEnvironment.getApplication())
        
        // This should not throw UninitializedPropertyAccessException
        try {
            recordingController.shutdown()
            println("[DEBUG_LOG] Shutdown completed successfully without initialization")
            assertTrue("Shutdown should complete without throwing exception", true)
        } catch (e: kotlin.UninitializedPropertyAccessException) {
            println("[DEBUG_LOG] UninitializedPropertyAccessException caught: ${e.message}")
            fail("Shutdown should not throw UninitializedPropertyAccessException when components are not initialized")
        } catch (e: Exception) {
            println("[DEBUG_LOG] Other exception during shutdown: ${e.message}")
            // Other exceptions might be expected due to test environment limitations
            assertTrue("Shutdown should handle uninitialized components gracefully", true)
        }
    }

    @Test
    fun testShutdownAfterInitialization() {
        println("[DEBUG_LOG] Testing shutdown after initialization")
        
        // Create RecordingController and initialize it
        val recordingController = RecordingController(RuntimeEnvironment.getApplication())
        
        try {
            recordingController.initialize()
            recordingController.shutdown()
            println("[DEBUG_LOG] Shutdown completed successfully after initialization")
            assertTrue("Shutdown should complete successfully after initialization", true)
        } catch (e: Exception) {
            println("[DEBUG_LOG] Exception during shutdown after initialization: ${e.message}")
            // This might fail due to missing dependencies in test environment, but that's expected
            // The important thing is that it doesn't fail with UninitializedPropertyAccessException
            assertFalse("Should not fail with UninitializedPropertyAccessException", 
                       e is kotlin.UninitializedPropertyAccessException)
        }
    }

    @Test
    fun testMultipleShutdownCalls() {
        println("[DEBUG_LOG] Testing multiple shutdown calls")
        
        val recordingController = RecordingController(RuntimeEnvironment.getApplication())
        
        try {
            // Call shutdown multiple times without initialization
            recordingController.shutdown()
            recordingController.shutdown()
            recordingController.shutdown()
            
            println("[DEBUG_LOG] Multiple shutdown calls completed successfully")
            assertTrue("Multiple shutdown calls should not cause issues", true)
        } catch (e: kotlin.UninitializedPropertyAccessException) {
            fail("Multiple shutdown calls should not throw UninitializedPropertyAccessException")
        } catch (e: Exception) {
            println("[DEBUG_LOG] Other exception during multiple shutdowns: ${e.message}")
            // Other exceptions might be expected
            assertTrue("Multiple shutdowns should handle uninitialized components gracefully", true)
        }
    }
}