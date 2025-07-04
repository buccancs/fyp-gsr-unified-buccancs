package com.gsrunified.android

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
// MediaPipe imports for hand landmark detection
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * HandAnalysisHandler provides post-recording hand analysis capabilities
 * using MediaPipe for hand detection and ML Kit for pose estimation.
 */
class HandAnalysisHandler(
    private val context: Context,
) {
    companion object {
        private const val TAG = "HandAnalysisHandler"
        private const val FRAME_EXTRACTION_INTERVAL_MS = 100L // Extract frame every 100ms (10 FPS)
    }

    // MediaPipe Hand Landmarker
    private var handLandmarker: HandLandmarker? = null

    // ML Kit Pose Detector
    private var poseDetector: PoseDetector? = null

    // Analysis state
    private var isAnalyzing = false
    private var analysisJob: Job? = null

    // Session information
    private var sessionId: String? = null
    private var deviceId: String = "unknown"

    // Frame context for correlation
    private var currentFrameNumber: Int = 0
    private var currentTimestamp: Long = 0

    // Callback interface for analysis events
    interface HandAnalysisCallback {
        fun onAnalysisStarted(
            videoPath: String,
            timestamp: Long,
        )

        fun onAnalysisProgress(
            progress: Float,
            frameNumber: Int,
            totalFrames: Int,
        )

        fun onHandDetected(
            frameNumber: Int,
            timestamp: Long,
            handLandmarks: List<HandLandmark>,
        )

        fun onPoseDetected(
            frameNumber: Int,
            timestamp: Long,
            pose: PoseData,
        )

        fun onAnalysisCompleted(
            videoPath: String,
            resultsPath: String,
            timestamp: Long,
        )

        fun onAnalysisError(
            error: String,
            timestamp: Long,
        )
    }

    private var analysisCallback: HandAnalysisCallback? = null

    // Data classes for analysis results
    data class HandLandmark(
        val x: Float,
        val y: Float,
        val z: Float,
        val visibility: Float,
        val landmarkType: String,
    )

    data class PoseData(
        val landmarks: List<HandLandmark>,
        val confidence: Float,
    )

    fun setAnalysisCallback(callback: HandAnalysisCallback) {
        this.analysisCallback = callback
    }

    fun setSessionInfo(
        sessionId: String,
        deviceId: String,
    ) {
        this.sessionId = sessionId
        this.deviceId = deviceId
    }

    /**
     * Initialize MediaPipe and ML Kit components
     */
    fun initialize() {
        try {
            // MediaPipe Hand Landmarker initialization
            val baseOptions =
                BaseOptions
                    .builder()
                    .setModelAssetPath("hand_landmarker.task")
                    .build()

            val handLandmarkerOptions =
                HandLandmarkerOptions
                    .builder()
                    .setBaseOptions(baseOptions)
                    .setRunningMode(RunningMode.IMAGE)
                    .setNumHands(2)
                    .setMinHandDetectionConfidence(0.5f)
                    .setMinHandPresenceConfidence(0.5f)
                    .setMinTrackingConfidence(0.5f)
                    .setResultListener { result, inputImage ->
                        processHandLandmarkerResult(result)
                    }.setErrorListener { error ->
                        Log.e(TAG, "MediaPipe HandLandmarker error: ${error.message}", error)
                        analysisCallback?.onAnalysisError(
                            "MediaPipe error: ${error.message}",
                            System.currentTimeMillis(),
                        )
                    }.build()

            handLandmarker = HandLandmarker.createFromOptions(context, handLandmarkerOptions)

            // Initialize ML Kit Pose Detector
            val poseDetectorOptions =
                AccuratePoseDetectorOptions
                    .Builder()
                    .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
                    .build()
            poseDetector = PoseDetection.getClient(poseDetectorOptions)

            Log.d(TAG, "Hand analysis components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing hand analysis components", e)
            analysisCallback?.onAnalysisError("Initialization error: ${e.message}", System.currentTimeMillis())
        }
    }

    /**
     * Start post-recording hand analysis on a video file
     */
    fun analyzeVideoFile(videoPath: String) {
        if (isAnalyzing) {
            Log.w(TAG, "Analysis already in progress")
            return
        }

        val videoFile = File(videoPath)
        if (!videoFile.exists()) {
            analysisCallback?.onAnalysisError("Video file not found: $videoPath", System.currentTimeMillis())
            return
        }

        isAnalyzing = true
        analysisCallback?.onAnalysisStarted(videoPath, System.currentTimeMillis())

        analysisJob =
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    processVideoFile(videoPath)
                } catch (e: Exception) {
                    Log.e(TAG, "Error during video analysis", e)
                    withContext(Dispatchers.Main) {
                        analysisCallback?.onAnalysisError("Analysis error: ${e.message}", System.currentTimeMillis())
                    }
                } finally {
                    isAnalyzing = false
                }
            }
    }

    /**
     * Process video file frame by frame
     */
    private suspend fun processVideoFile(videoPath: String) {
        val retriever = MediaMetadataRetriever()
        var resultsWriter: BufferedWriter? = null

        try {
            retriever.setDataSource(videoPath)

            // Get video metadata
            val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val duration = durationString?.toLongOrNull() ?: 0L
            val totalFrames = (duration / FRAME_EXTRACTION_INTERVAL_MS).toInt()

            Log.d(TAG, "Processing video: duration=${duration}ms, estimated frames=$totalFrames")

            // Create results file
            val resultsPath = createResultsFile(videoPath)
            resultsWriter = BufferedWriter(FileWriter(File(resultsPath)))
            writeResultsHeader(resultsWriter)

            var frameNumber = 0
            var currentTimeUs = 0L

            while (currentTimeUs < duration * 1000) { // Convert to microseconds
                try {
                    // Extract frame at current time
                    val bitmap = retriever.getFrameAtTime(currentTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)

                    if (bitmap != null) {
                        // Process frame for hand and pose detection
                        processFrame(bitmap, frameNumber, currentTimeUs / 1000, resultsWriter)

                        // Update progress
                        val progress = frameNumber.toFloat() / totalFrames.toFloat()
                        withContext(Dispatchers.Main) {
                            analysisCallback?.onAnalysisProgress(progress, frameNumber, totalFrames)
                        }
                    }

                    frameNumber++
                    currentTimeUs += FRAME_EXTRACTION_INTERVAL_MS * 1000 // Convert to microseconds
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing frame $frameNumber", e)
                    // Continue with next frame
                }
            }

            withContext(Dispatchers.Main) {
                analysisCallback?.onAnalysisCompleted(videoPath, resultsPath, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing video file", e)
            throw e
        } finally {
            try {
                retriever.release()
                resultsWriter?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up resources", e)
            }
        }
    }

    /**
     * Process individual frame for hand and pose detection
     */
    private suspend fun processFrame(
        bitmap: Bitmap,
        frameNumber: Int,
        timestampMs: Long,
        resultsWriter: BufferedWriter?,
    ) {
        try {
            // Set current frame context for correlation
            currentFrameNumber = frameNumber
            currentTimestamp = timestampMs

            // Process with MediaPipe HandLandmarker
            handLandmarker?.let { landmarker ->
                try {
                    val mpImage =
                        com.google.mediapipe.framework.image
                            .BitmapImageBuilder(bitmap)
                            .build()
                    val result = landmarker.detect(mpImage)
                    processHandLandmarkerResult(result)
                } catch (e: Exception) {
                    Log.e(TAG, "MediaPipe hand detection failed for frame $frameNumber", e)
                }
            }

            // Process with ML Kit Pose Detection
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            poseDetector
                ?.process(inputImage)
                ?.addOnSuccessListener { pose ->
                    processPoseResult(pose, frameNumber, timestampMs, resultsWriter)
                }?.addOnFailureListener { e ->
                    Log.e(TAG, "Pose detection failed for frame $frameNumber", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing frame $frameNumber", e)
        }
    }

    /**
     * Process MediaPipe HandLandmarker results
     */
    private fun processHandLandmarkerResult(result: HandLandmarkerResult) {
        try {
            if (result.landmarks().isNotEmpty()) {
                val handLandmarks = mutableListOf<HandLandmark>()

                for (handIndex in result.landmarks().indices) {
                    val landmarks = result.landmarks()[handIndex]
                    for (landmarkIndex in landmarks.indices) {
                        val landmark = landmarks[landmarkIndex]
                        handLandmarks.add(
                            HandLandmark(
                                x = landmark.x(),
                                y = landmark.y(),
                                z = landmark.z(),
                                visibility = if (result.landmarks().size > handIndex) 1.0f else 0.0f,
                                landmarkType = "hand_${handIndex}_landmark_$landmarkIndex",
                            ),
                        )
                    }
                }

                // Notify callback with detected hand landmarks
                analysisCallback?.onHandDetected(currentFrameNumber, currentTimestamp, handLandmarks)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing hand landmarker result", e)
        }
    }

    /**
     * Process ML Kit Pose results
     */
    private fun processPoseResult(
        pose: Pose,
        frameNumber: Int,
        timestampMs: Long,
        resultsWriter: BufferedWriter?,
    ) {
        try {
            val poseLandmarks = mutableListOf<HandLandmark>()

            for (poseLandmark in pose.allPoseLandmarks) {
                poseLandmarks.add(
                    HandLandmark(
                        x = poseLandmark.position.x,
                        y = poseLandmark.position.y,
                        z = poseLandmark.position3D.z,
                        visibility = poseLandmark.inFrameLikelihood,
                        landmarkType = poseLandmark.landmarkType.toString(),
                    ),
                )
            }

            val poseData =
                PoseData(
                    landmarks = poseLandmarks,
                    confidence = pose.allPoseLandmarks.maxOfOrNull { it.inFrameLikelihood } ?: 0f,
                )

            // Write to results file
            writeFrameResults(resultsWriter, frameNumber, timestampMs, emptyList(), poseData)

            analysisCallback?.onPoseDetected(frameNumber, timestampMs, poseData)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing pose result", e)
        }
    }

    /**
     * Create results file for analysis output
     */
    private fun createResultsFile(videoPath: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sessionPrefix = sessionId ?: "session_$timestamp"
        val filename = "${sessionPrefix}_${deviceId}_hand_analysis.json"
        val file = File(context.getExternalFilesDir(null), filename)
        return file.absolutePath
    }

    /**
     * Write results file header
     */
    private fun writeResultsHeader(writer: BufferedWriter) {
        val header =
            JSONObject().apply {
                put("analysis_type", "hand_pose_detection")
                put("session_id", sessionId)
                put("device_id", deviceId)
                put("timestamp", System.currentTimeMillis())
                put("frame_interval_ms", FRAME_EXTRACTION_INTERVAL_MS)
                put("frames", JSONArray())
            }
        writer.write(header.toString())
        writer.newLine()
    }

    /**
     * Write frame analysis results
     */
    private fun writeFrameResults(
        writer: BufferedWriter?,
        frameNumber: Int,
        timestampMs: Long,
        handLandmarks: List<HandLandmark>,
        poseData: PoseData?,
    ) {
        try {
            val frameResult =
                JSONObject().apply {
                    put("frame_number", frameNumber)
                    put("timestamp_ms", timestampMs)

                    // Hand landmarks
                    val handsArray = JSONArray()
                    handLandmarks.forEach { landmark ->
                        val landmarkObj =
                            JSONObject().apply {
                                put("x", landmark.x)
                                put("y", landmark.y)
                                put("z", landmark.z)
                                put("visibility", landmark.visibility)
                                put("type", landmark.landmarkType)
                            }
                        handsArray.put(landmarkObj)
                    }
                    put("hand_landmarks", handsArray)

                    // Pose data
                    poseData?.let { pose ->
                        val poseObj =
                            JSONObject().apply {
                                put("confidence", pose.confidence)
                                val poseArray = JSONArray()
                                pose.landmarks.forEach { landmark ->
                                    val landmarkObj =
                                        JSONObject().apply {
                                            put("x", landmark.x)
                                            put("y", landmark.y)
                                            put("z", landmark.z)
                                            put("visibility", landmark.visibility)
                                            put("type", landmark.landmarkType)
                                        }
                                    poseArray.put(landmarkObj)
                                }
                                put("landmarks", poseArray)
                            }
                        put("pose_data", poseObj)
                    }
                }

            writer?.write(frameResult.toString())
            writer?.newLine()
        } catch (e: Exception) {
            Log.e(TAG, "Error writing frame results", e)
        }
    }

    /**
     * Stop ongoing analysis
     */
    fun stopAnalysis() {
        if (isAnalyzing) {
            analysisJob?.cancel()
            isAnalyzing = false
            Log.d(TAG, "Hand analysis stopped")
        }
    }

    /**
     * Check if analysis is currently running
     */
    fun isAnalyzing(): Boolean = isAnalyzing

    /**
     * Release resources and cleanup
     */
    fun release() {
        try {
            stopAnalysis()
            handLandmarker?.close()
            poseDetector?.close()
            Log.d(TAG, "Hand analysis handler released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing hand analysis handler", e)
        }
    }
}
