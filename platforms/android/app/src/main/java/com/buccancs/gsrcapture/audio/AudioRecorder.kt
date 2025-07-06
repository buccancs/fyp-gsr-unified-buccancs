package com.buccancs.gsrcapture.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.buccancs.gsrcapture.utils.TimeManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages audio recording operations.
 * Records synchronized audio via the phone's microphone.
 */
class AudioRecorder(
    private val context: Context,
    private val audioExecutor: ExecutorService
) {
    private val TAG = "AudioRecorder"
    
    // Audio recording parameters
    companion object {
        private const val SAMPLE_RATE = 44100 // 44.1 kHz
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_FACTOR = 2 // Buffer size multiplier
    }
    
    // Audio recording state
    private val isRecording = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var outputFile: File? = null
    private var bufferSize: Int = 0
    
    /**
     * Initializes the audio recorder.
     * @return True if initialization was successful, false otherwise
     */
    fun initialize(): Boolean {
        try {
            // Get the minimum buffer size for the audio recorder
            bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            
            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size")
                return false
            }
            
            // Use a larger buffer to avoid underruns
            bufferSize *= BUFFER_SIZE_FACTOR
            
            Log.d(TAG, "Audio recorder initialized with buffer size: $bufferSize")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing audio recorder", e)
            return false
        }
    }
    
    /**
     * Starts recording audio.
     * @param outputDir Directory where the audio file will be saved
     * @param sessionId Unique identifier for the recording session
     * @return True if recording started successfully, false otherwise
     */
    fun startRecording(outputDir: File, sessionId: String): Boolean {
        if (isRecording.get()) {
            Log.d(TAG, "Already recording")
            return true
        }
        
        try {
            // Create output file
            val timestamp = TimeManager.getCurrentTimestamp()
            outputFile = File(outputDir, "Audio_${sessionId}_${timestamp}.wav")
            
            // Create and start the audio recorder
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
            
            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Audio recorder not initialized")
                releaseResources()
                return false
            }
            
            // Start recording
            audioRecord?.startRecording()
            isRecording.set(true)
            
            // Start writing audio data to file in a background thread
            audioExecutor.execute { writeAudioDataToFile() }
            
            Log.d(TAG, "Started recording audio to ${outputFile?.absolutePath}")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio recording", e)
            releaseResources()
            return false
        }
    }
    
    /**
     * Stops recording audio.
     */
    fun stopRecording() {
        if (!isRecording.getAndSet(false)) {
            return
        }
        
        try {
            audioRecord?.stop()
            Log.d(TAG, "Stopped recording audio")
            
            // Wait for the background thread to finish writing the WAV header
            // This is handled in the writeAudioDataToFile method
            
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio recording", e)
        }
    }
    
    /**
     * Writes audio data to a WAV file.
     */
    private fun writeAudioDataToFile() {
        val data = ByteArray(bufferSize)
        var totalBytes = 0
        
        try {
            // Create a temporary raw audio file
            val rawFile = File("${outputFile?.absolutePath}.raw")
            val rawOutput = FileOutputStream(rawFile)
            
            // Record audio data
            while (isRecording.get()) {
                val read = audioRecord?.read(data, 0, bufferSize) ?: -1
                if (read > 0) {
                    rawOutput.write(data, 0, read)
                    totalBytes += read
                }
            }
            
            // Close the raw output stream
            rawOutput.close()
            
            // Convert the raw file to WAV format
            convertRawToWav(rawFile, outputFile!!, totalBytes)
            
            // Delete the temporary raw file
            rawFile.delete()
            
        } catch (e: IOException) {
            Log.e(TAG, "Error writing audio data to file", e)
        } finally {
            releaseResources()
        }
    }
    
    /**
     * Converts a raw audio file to WAV format.
     * @param rawFile Raw audio file
     * @param wavFile WAV output file
     * @param totalAudioLen Total length of audio data in bytes
     */
    private fun convertRawToWav(rawFile: File, wavFile: File, totalAudioLen: Int) {
        try {
            val rawData = rawFile.readBytes()
            val wavOutput = FileOutputStream(wavFile)
            
            // WAV header constants
            val channelCount = 2 // Stereo
            val byteRate = SAMPLE_RATE * channelCount * 2 // 2 bytes per sample
            
            // Write the WAV header
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            
            // RIFF header
            header.put("RIFF".toByteArray()) // ChunkID
            header.putInt(36 + totalAudioLen) // ChunkSize
            header.put("WAVE".toByteArray()) // Format
            
            // fmt subchunk
            header.put("fmt ".toByteArray()) // Subchunk1ID
            header.putInt(16) // Subchunk1Size (16 for PCM)
            header.putShort(1) // AudioFormat (1 for PCM)
            header.putShort(channelCount.toShort()) // NumChannels
            header.putInt(SAMPLE_RATE) // SampleRate
            header.putInt(byteRate) // ByteRate
            header.putShort((channelCount * 2).toShort()) // BlockAlign
            header.putShort(16) // BitsPerSample
            
            // data subchunk
            header.put("data".toByteArray()) // Subchunk2ID
            header.putInt(totalAudioLen) // Subchunk2Size
            
            // Write the header and audio data
            wavOutput.write(header.array())
            wavOutput.write(rawData)
            
            wavOutput.close()
            Log.d(TAG, "Converted raw audio to WAV format: ${wavFile.absolutePath}")
            
        } catch (e: IOException) {
            Log.e(TAG, "Error converting raw audio to WAV", e)
        }
    }
    
    /**
     * Releases all resources.
     */
    private fun releaseResources() {
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing audio recorder", e)
        } finally {
            audioRecord = null
        }
    }
    
    /**
     * Shuts down the audio recorder.
     */
    fun shutdown() {
        stopRecording()
        releaseResources()
    }
}