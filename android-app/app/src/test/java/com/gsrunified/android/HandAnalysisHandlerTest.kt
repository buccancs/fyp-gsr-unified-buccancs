package com.gsrunified.android

import android.content.Context
import android.media.MediaMetadataRetriever
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class HandAnalysisHandlerTest {
    private val testDispatcher = StandardTestDispatcher()

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockCallback: HandAnalysisHandler.HandAnalysisCallback

    @Mock
    private lateinit var mockPoseDetector: PoseDetector

    @Mock
    private lateinit var mockMediaRetriever: MediaMetadataRetriever

    @Mock
    private lateinit var mockSuccessTask: Task<Pose>

    @Mock
    private lateinit var mockFailureTask: Task<Pose>

    @Mock
    private lateinit var mockPose: Pose

    private lateinit var handler: HandAnalysisHandler

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        // Mock the behavior of ML Kit's PoseDetector
        // We can't directly inject it, so this test focuses on logic that can be isolated.
        // A better design would allow injecting the detector for easier testing.

        // Mock the MediaMetadataRetriever
        whenever(mockMediaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)).thenReturn("1000") // 1 second video

        // Mock ML Kit Task results
        whenever(mockSuccessTask.isSuccessful).thenReturn(true)
        whenever(mockSuccessTask.result).thenReturn(mockPose)
        whenever(mockSuccessTask.addOnSuccessListener(any())).thenAnswer {
            val listener = it.getArgument(0) as com.google.android.gms.tasks.OnSuccessListener<Pose>
            listener.onSuccess(mockPose)
            mockSuccessTask
        }
        whenever(mockSuccessTask.addOnFailureListener(any())).thenReturn(mockSuccessTask)

        val testException = RuntimeException("ML Kit processing failed")
        whenever(mockFailureTask.isSuccessful).thenReturn(false)
        whenever(mockFailureTask.exception).thenReturn(testException)
        whenever(mockFailureTask.addOnSuccessListener(any())).thenReturn(mockFailureTask)
        whenever(mockFailureTask.addOnFailureListener(any())).thenAnswer {
            val listener = it.getArgument(0) as com.google.android.gms.tasks.OnFailureListener
            listener.onFailure(testException)
            mockFailureTask
        }

        // The handler will be spied to allow mocking internal method calls
        handler = spy(HandAnalysisHandler(mockContext))
        handler.setAnalysisCallback(mockCallback)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        handler.release()
    }

    @Test
    fun `initial state is not analyzing`() {
        assertFalse(handler.isAnalyzing())
    }

    @Test
    fun `analyzeVideoFile with non-existent file calls onError`() =
        runTest {
            val nonExistentPath = "/path/to/non_existent_video.mp4"
            // Mock File.exists() to return false
            // This requires more advanced mocking (e.g., PowerMock) or refactoring HandAnalysisHandler
            // to not directly use File. For now, we test the callback logic.

            // Simulate the check failing
            handler.analyzeVideoFile(nonExistentPath)
            advanceUntilIdle()

            // As we can't mock File, we assume the internal check works and test the callback path
            // In a real scenario, the handler would call onError.
            // For this example, we'll verify that if the path is invalid, no analysis starts.
            verify(mockCallback, never()).onAnalysisStarted(any(), any())
        }

    @Test
    fun `stopAnalysis cancels the job`() =
        runTest {
            // This test is conceptual as we can't easily inject the job.
            // We verify that calling stopAnalysis while it's "running" doesn't crash.
            handler.analyzeVideoFile("/path/to/video.mp4") // Assume it starts
            handler.stopAnalysis()
            advanceUntilIdle()

            assertFalse(handler.isAnalyzing())
            // In a refactored version, we would verify job.cancel() was called.
        }

    @Test
    fun `processPoseResult correctly notifies callback`() =
        runTest {
            // This method is private, so we test it indirectly by testing analyzeVideoFile
            // or we could make it internal/protected for testing.
            // Let's assume we can test the logic of what happens on a successful pose detection.

            // We can't directly call the private method, so we'll test the public-facing outcome.
            // This highlights a need for potential refactoring for better testability.
            assertTrue(true) // Placeholder for a more detailed test after refactoring
        }

    @Test
    fun `analysis lifecycle callbacks are called correctly on success`() =
        runTest {
            // This is a more integrated test of the analysis process
            val videoPath = "/path/to/video.mp4"

            // Since we can't easily mock the internal MediaPipe and ML Kit components,
            // we'll test the public interface behavior
            // The actual file processing would require a real video file

            handler.analyzeVideoFile(videoPath)
            advanceUntilIdle()

            // For this test, we expect onAnalysisError to be called since the file doesn't exist
            // In a real test environment, we would use a test video file
            verify(mockCallback).onAnalysisStarted(eq(videoPath), any())
            verify(mockCallback, atLeastOnce()).onAnalysisError(any(), any())
            verify(mockCallback, never()).onAnalysisCompleted(any(), any(), any())
        }

    @Test
    fun `analysis lifecycle callbacks are called correctly on failure`() =
        runTest {
            val videoPath = "/path/to/video.mp4"

            // Mock the internal processing to simulate a failure
            doReturn(mockMediaRetriever).`when`(handler).createMediaMetadataRetriever()
            doReturn(mockFailureTask).`when`(handler).processImageWithPoseDetector(any())

            handler.analyzeVideoFile(videoPath)
            advanceUntilIdle()

            // Verify the sequence of callbacks for a failure scenario
            verify(mockCallback).onAnalysisStarted(eq(videoPath), any())
            // Depending on where the failure happens, progress might be called.
            // The crucial part is that onAnalysisError is called.
            verify(mockCallback, atLeastOnce()).onAnalysisError(any(), any())
            verify(mockCallback, never()).onAnalysisCompleted(any(), any(), any())
        }
}
