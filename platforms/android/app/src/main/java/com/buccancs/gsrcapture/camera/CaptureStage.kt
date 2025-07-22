package com.buccancs.gsrcapture.camera

/**
 * Represents the different camera pipeline stages for image capture.
 * Based on the Camera2 API pipeline stages.
 */
enum class CaptureStage(
    val displayName: String,
    val description: String,
    val maxFps: Int
) {
    /**
     * Stage 1 - RAW_SENSOR format
     * Earliest buffer the Camera2 API provides - still Bayer, before demosaic and tone-map
     */
    STAGE_1_RAW_SENSOR(
        displayName = "Stage 1 - RAW Sensor",
        description = "Raw Bayer data before demosaic and tone-map (30 fps max)",
        maxFps = 30
    ),

    /**
     * Stage 3 - YUV_420_888 format
     * After full ISP processing - what you see in stock camera apps
     */
    STAGE_3_YUV_PROCESSED(
        displayName = "Stage 3 - YUV Processed",
        description = "Fully processed YUV data after ISP (up to 240 fps in constrained mode)",
        maxFps = 240
    )
}