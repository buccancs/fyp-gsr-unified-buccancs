package com.buccancs.gsrcapture.accessibility

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

/**
 * Data class representing a voice command
 */
data class VoiceCommand(
    val command: String,
    val aliases: List<String>,
    val description: String,
    val action: () -> Unit
)

/**
 * Data class representing voice control configuration
 */
data class VoiceControlConfig(
    val language: Locale = Locale.getDefault(),
    val enableContinuousListening: Boolean = false,
    val confidenceThreshold: Float = 0.7f,
    val enableVoiceFeedback: Boolean = true,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val enableWakeWord: Boolean = true,
    val wakeWord: String = "GSR Capture"
)

/**
 * Voice Control Manager for hands-free operation
 * Provides speech recognition and text-to-speech capabilities
 */
class VoiceControlManager(
    private val context: Context,
    private val config: VoiceControlConfig = VoiceControlConfig()
) : RecognitionListener, TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "VoiceControlManager"
        private const val TTS_UTTERANCE_ID = "tts_utterance"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false
    private var isTtsInitialized = false
    private var isWakeWordMode = false

    // Voice commands registry
    private val voiceCommands = mutableMapOf<String, VoiceCommand>()

    // Callbacks
    private var commandRecognizedCallback: ((String) -> Unit)? = null
    private var listeningStateCallback: ((Boolean) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    private var speechFeedbackCallback: ((String) -> Unit)? = null

    init {
        initializeSpeechRecognizer()
        initializeTextToSpeech()
        registerDefaultCommands()
    }

    /**
     * Initialize speech recognizer
     */
    private fun initializeSpeechRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(this)
                Log.d(TAG, "Speech recognizer initialized successfully")
            } else {
                Log.w(TAG, "Speech recognition not available on this device")
                errorCallback?.invoke("Speech recognition not available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize speech recognizer", e)
            errorCallback?.invoke("Failed to initialize speech recognition: ${e.message}")
        }
    }

    /**
     * Initialize text-to-speech
     */
    private fun initializeTextToSpeech() {
        try {
            textToSpeech = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize text-to-speech", e)
            errorCallback?.invoke("Failed to initialize text-to-speech: ${e.message}")
        }
    }

    /**
     * Register default voice commands
     */
    private fun registerDefaultCommands() {
        // Recording commands
        registerCommand(VoiceCommand(
            command = "start recording",
            aliases = listOf("begin recording", "start capture", "record"),
            description = "Start recording session"
        ) {
            commandRecognizedCallback?.invoke("START_RECORDING")
            speak("Starting recording")
        })

        registerCommand(VoiceCommand(
            command = "stop recording",
            aliases = listOf("end recording", "stop capture", "finish"),
            description = "Stop recording session"
        ) {
            commandRecognizedCallback?.invoke("STOP_RECORDING")
            speak("Stopping recording")
        })

        // Camera commands
        registerCommand(VoiceCommand(
            command = "switch camera",
            aliases = listOf("change camera", "toggle camera", "switch view"),
            description = "Switch between RGB and thermal camera"
        ) {
            commandRecognizedCallback?.invoke("SWITCH_CAMERA")
            speak("Switching camera")
        })

        registerCommand(VoiceCommand(
            command = "thermal camera",
            aliases = listOf("thermal view", "heat camera"),
            description = "Switch to thermal camera"
        ) {
            commandRecognizedCallback?.invoke("THERMAL_CAMERA")
            speak("Switching to thermal camera")
        })

        registerCommand(VoiceCommand(
            command = "rgb camera",
            aliases = listOf("normal camera", "regular camera", "color camera"),
            description = "Switch to RGB camera"
        ) {
            commandRecognizedCallback?.invoke("RGB_CAMERA")
            speak("Switching to RGB camera")
        })

        // Settings commands
        registerCommand(VoiceCommand(
            command = "open settings",
            aliases = listOf("settings", "configuration", "preferences"),
            description = "Open settings menu"
        ) {
            commandRecognizedCallback?.invoke("OPEN_SETTINGS")
            speak("Opening settings")
        })

        // Status commands
        registerCommand(VoiceCommand(
            command = "status report",
            aliases = listOf("current status", "system status", "report"),
            description = "Get current system status"
        ) {
            commandRecognizedCallback?.invoke("STATUS_REPORT")
            speak("Generating status report")
        })

        // Help commands
        registerCommand(VoiceCommand(
            command = "help",
            aliases = listOf("commands", "what can you do", "voice commands"),
            description = "List available voice commands"
        ) {
            commandRecognizedCallback?.invoke("HELP")
            speakAvailableCommands()
        })

        // Wake word
        registerCommand(VoiceCommand(
            command = config.wakeWord.lowercase(),
            aliases = listOf("hey ${config.wakeWord.lowercase()}", "ok ${config.wakeWord.lowercase()}"),
            description = "Wake word to activate voice control"
        ) {
            if (config.enableWakeWord) {
                isWakeWordMode = false
                speak("Yes, how can I help?")
                startListening()
            }
        })
    }

    /**
     * Register a custom voice command
     */
    fun registerCommand(command: VoiceCommand) {
        voiceCommands[command.command.lowercase()] = command
        command.aliases.forEach { alias ->
            voiceCommands[alias.lowercase()] = command
        }
        Log.d(TAG, "Registered voice command: ${command.command}")
    }

    /**
     * Start listening for voice commands
     */
    fun startListening() {
        if (speechRecognizer == null) {
            errorCallback?.invoke("Speech recognizer not available")
            return
        }

        if (isListening) {
            Log.w(TAG, "Already listening")
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language.toString())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            listeningStateCallback?.invoke(true)
            Log.d(TAG, "Started listening for voice commands")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start listening", e)
            errorCallback?.invoke("Failed to start voice recognition: ${e.message}")
        }
    }

    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        if (!isListening) return

        try {
            speechRecognizer?.stopListening()
            isListening = false
            listeningStateCallback?.invoke(false)
            Log.d(TAG, "Stopped listening for voice commands")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop listening", e)
        }
    }

    /**
     * Enable wake word mode
     */
    fun enableWakeWordMode() {
        if (config.enableWakeWord) {
            isWakeWordMode = true
            startListening()
            Log.d(TAG, "Wake word mode enabled")
        }
    }

    /**
     * Speak text using text-to-speech
     */
    fun speak(text: String) {
        if (!config.enableVoiceFeedback || !isTtsInitialized) return

        try {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
            speechFeedbackCallback?.invoke(text)
            Log.d(TAG, "Speaking: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to speak text", e)
        }
    }

    /**
     * Speak available commands
     */
    private fun speakAvailableCommands() {
        val commands = voiceCommands.values.distinctBy { it.command }.take(5)
        val commandList = commands.joinToString(", ") { it.command }
        speak("Available commands include: $commandList. Say help for more commands.")
    }

    /**
     * Process recognized speech
     */
    private fun processRecognizedSpeech(results: List<String>, confidences: FloatArray?) {
        for (i in results.indices) {
            val result = results[i].lowercase().trim()
            val confidence = confidences?.getOrNull(i) ?: 1.0f

            Log.d(TAG, "Recognized: '$result' (confidence: $confidence)")

            if (confidence >= config.confidenceThreshold) {
                // Check for exact matches first
                val command = voiceCommands[result]
                if (command != null) {
                    Log.d(TAG, "Executing command: ${command.command}")
                    command.action()
                    return
                }

                // Check for partial matches
                val partialMatch = voiceCommands.entries.find { (key, _) ->
                    result.contains(key) || key.contains(result)
                }

                if (partialMatch != null) {
                    Log.d(TAG, "Executing partial match: ${partialMatch.value.command}")
                    partialMatch.value.action()
                    return
                }
            }
        }

        // No command found
        if (!isWakeWordMode) {
            speak("Sorry, I didn't understand that command. Say help for available commands.")
        }
    }

    // RecognitionListener implementation
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "Beginning of speech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Audio level changed - could be used for visual feedback
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Audio buffer received
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "End of speech")
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }

        Log.w(TAG, "Speech recognition error: $errorMessage")
        isListening = false
        listeningStateCallback?.invoke(false)

        // Restart listening in wake word mode or continuous mode
        if ((isWakeWordMode || config.enableContinuousListening) && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            // Restart after a short delay
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                startListening()
            }, 1000)
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

        if (matches != null && matches.isNotEmpty()) {
            processRecognizedSpeech(matches, confidences)
        }

        isListening = false
        listeningStateCallback?.invoke(false)

        // Restart listening in continuous mode or wake word mode
        if (config.enableContinuousListening || isWakeWordMode) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                startListening()
            }, 500)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            Log.d(TAG, "Partial result: ${matches[0]}")
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "Speech recognition event: $eventType")
    }

    // TextToSpeech.OnInitListener implementation
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val result = tts.setLanguage(config.language)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "Language not supported for TTS")
                    errorCallback?.invoke("Language not supported for text-to-speech")
                } else {
                    tts.setSpeechRate(config.speechRate)
                    tts.setPitch(config.speechPitch)
                    
                    // Set utterance progress listener
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            Log.d(TAG, "TTS started")
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.d(TAG, "TTS completed")
                        }

                        override fun onError(utteranceId: String?) {
                            Log.e(TAG, "TTS error")
                        }
                    })
                    
                    isTtsInitialized = true
                    Log.d(TAG, "Text-to-speech initialized successfully")
                }
            }
        } else {
            Log.e(TAG, "Text-to-speech initialization failed")
            errorCallback?.invoke("Text-to-speech initialization failed")
        }
    }

    /**
     * Get list of available commands
     */
    fun getAvailableCommands(): List<VoiceCommand> {
        return voiceCommands.values.distinctBy { it.command }
    }

    /**
     * Check if voice control is available
     */
    fun isVoiceControlAvailable(): Boolean {
        return speechRecognizer != null && isTtsInitialized
    }

    /**
     * Check if currently listening
     */
    fun isCurrentlyListening(): Boolean {
        return isListening
    }

    // Callback setters
    fun setCommandRecognizedCallback(callback: (String) -> Unit) {
        commandRecognizedCallback = callback
    }

    fun setListeningStateCallback(callback: (Boolean) -> Unit) {
        listeningStateCallback = callback
    }

    fun setErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    fun setSpeechFeedbackCallback(callback: (String) -> Unit) {
        speechFeedbackCallback = callback
    }

    /**
     * Shutdown voice control manager
     */
    fun shutdown() {
        try {
            stopListening()
            speechRecognizer?.destroy()
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            
            speechRecognizer = null
            textToSpeech = null
            isTtsInitialized = false
            
            Log.d(TAG, "Voice control manager shut down")
        } catch (e: Exception) {
            Log.e(TAG, "Error during shutdown", e)
        }
    }
}