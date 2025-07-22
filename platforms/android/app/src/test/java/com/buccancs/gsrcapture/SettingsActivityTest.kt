package com.buccancs.gsrcapture

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.buccancs.gsrcapture.camera.CaptureStage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SettingsActivityTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `SettingsActivity should be creatable`() {
        val activityController = Robolectric.buildActivity(SettingsActivity::class.java)
        val activity = activityController.create().get()

        assertNotNull(activity)
        assertTrue(activity is SettingsActivity)
    }

    @Test
    fun `SettingsActivity should handle intent creation`() {
        val intent = Intent(context, SettingsActivity::class.java)

        assertNotNull(intent)
        assertEquals(SettingsActivity::class.java.name, intent.component?.className)
    }

    @Test
    fun `CaptureStage enum should have correct values`() {
        val stage1 = CaptureStage.STAGE_1_RAW_SENSOR
        val stage3 = CaptureStage.STAGE_3_YUV_PROCESSED

        assertEquals("Stage 1 - RAW Sensor", stage1.displayName)
        assertEquals(30, stage1.maxFps)

        assertEquals("Stage 3 - YUV Processed", stage3.displayName)
        assertEquals(240, stage3.maxFps)
    }

    @Test
    fun `SettingsActivity should initialize without crashing`() {
        val activityController = Robolectric.buildActivity(SettingsActivity::class.java)

        // This should not throw any exceptions
        val activity = activityController.create().start().resume().get()

        assertNotNull(activity)
    }
}
