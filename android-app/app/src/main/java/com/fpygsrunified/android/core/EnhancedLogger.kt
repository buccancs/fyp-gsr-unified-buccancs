package com.fpygsrunified.android.core

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Enhanced logging system with file output, remote logging, and structured metadata support.
 * Provides comprehensive logging capabilities for debugging, monitoring, and analytics.
 */
class EnhancedLogger private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: EnhancedLogger? = null
        private const val LOG_FILE_PREFIX = "app_log_"
        private const val LOG_FILE_EXTENSION = ".txt"
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
        private const val FILE_DATE_FORMAT = "yyyy-MM-dd"

        // Remote logging callback interface
        interface RemoteLogCallback {
            fun sendLogToRemote(logEntry: LogEntry)
        }

        enum class LogLevel(val priority: Int, val tag: String) {
            VERBOSE(2, "V"),
            DEBUG(3, "D"),
            INFO(4, "I"),
            WARN(5, "W"),
            ERROR(6, "E"),
            ASSERT(7, "A")
        }

        data class LogEntry(
            val timestamp: Long,
            val level: LogLevel,
            val tag: String,
            val message: String,
            val throwable: Throwable? = null,
            val metadata: Map<String, String> = emptyMap()
        ) {
            fun toJsonString(): String {
                return try {
                    JSONObject().apply {
                        put("timestamp", timestamp)
                        put("level", level.name)
                        put("tag", tag)
                        put("message", message)
                        throwable?.let { 
                            put("exception", it.stackTraceToString()) 
                        }
                        if (metadata.isNotEmpty()) {
                            put("metadata", JSONObject(metadata))
                        }
                    }.toString()
                } catch (e: Exception) {
                    "Failed to serialize log entry: ${e.message}"
                }
            }

            fun toFormattedString(): String {
                val dateFormat = SimpleDateFormat(DATE_FORMAT, Locale.US)
                val formattedTime = dateFormat.format(Date(timestamp))
                val metadataStr = if (metadata.isNotEmpty()) {
                    " [${metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }}]"
                } else ""
                val exceptionStr = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
                return "$formattedTime ${level.tag}/$tag: $message$metadataStr$exceptionStr"
            }
        }

        fun initialize() {
            // Initialize logging system
        }

        fun shutdown() {
            INSTANCE?.let { logger ->
                logger.isShutdown.set(true)
                logger.loggingJob?.cancel()
                try {
                    logger.logWriter?.close()
                } catch (e: Exception) {
                    Log.e("EnhancedLogger", "Error closing log writer", e)
                }
            }
        }

        fun configure(
            minLogLevel: LogLevel = LogLevel.DEBUG,
            enableFileLogging: Boolean = true,
            enableRemoteLogging: Boolean = false,
            maxLogFileSize: Int = 10 * 1024 * 1024, // 10MB
            maxLogFiles: Int = 5
        ) {
            INSTANCE?.let { logger ->
                logger.minLogLevel = minLogLevel
                logger.enableFileLogging = enableFileLogging
                logger.enableRemoteLogging = enableRemoteLogging
                logger.maxLogFileSize = maxLogFileSize
                logger.maxLogFiles = maxLogFiles

                if (enableFileLogging && logger.logWriter == null) {
                    logger.initializeFileLogging()
                }
            }
        }

        fun setRemoteLogCallback(callback: RemoteLogCallback) {
            INSTANCE?.remoteLogCallback = callback
        }

        // Convenience methods for logging
        fun v(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.log(LogLevel.VERBOSE, tag, message, null, metadata)
        }

        fun d(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.log(LogLevel.DEBUG, tag, message, null, metadata)
        }

        fun i(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.log(LogLevel.INFO, tag, message, null, metadata)
        }

        fun w(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.log(LogLevel.WARN, tag, message, null, metadata)
        }

        fun w(tag: String, message: String, throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.log(LogLevel.WARN, tag, message, throwable, metadata)
        }

        fun e(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.log(LogLevel.ERROR, tag, message, null, metadata)
        }

        fun e(tag: String, message: String, throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.log(LogLevel.ERROR, tag, message, throwable, metadata)
        }

        fun logPerformance(tag: String, operation: String, duration: Long, metadata: Map<String, String> = emptyMap()) {
            val performanceMetadata = metadata.toMutableMap().apply {
                put("operation", operation)
                put("duration_ms", duration.toString())
                put("type", "performance")
            }
            INSTANCE?.log(LogLevel.INFO, tag, "Performance: $operation took ${duration}ms", null, performanceMetadata)
        }

        fun logNetwork(tag: String, event: String, url: String? = null, statusCode: Int? = null, metadata: Map<String, String> = emptyMap()) {
            val networkMetadata = metadata.toMutableMap().apply {
                put("event", event)
                put("type", "network")
                url?.let { put("url", it) }
                statusCode?.let { put("status_code", it.toString()) }
            }
            val message = buildString {
                append("Network: $event")
                url?.let { append(" - $it") }
                statusCode?.let { append(" (Status: $it)") }
            }
            INSTANCE?.log(LogLevel.INFO, tag, message, null, networkMetadata)
        }

        fun logUserAction(tag: String, action: String, screen: String? = null, metadata: Map<String, String> = emptyMap()) {
            val userMetadata = metadata.toMutableMap().apply {
                put("action", action)
                put("type", "user_action")
                screen?.let { put("screen", it) }
            }
            INSTANCE?.log(LogLevel.INFO, tag, "User Action: $action${screen?.let { " on $it" } ?: ""}", null, userMetadata)
        }

        fun logSystemEvent(tag: String, event: String, component: String, metadata: Map<String, String> = emptyMap()) {
            val systemMetadata = metadata.toMutableMap().apply {
                put("event", event)
                put("component", component)
                put("type", "system_event")
            }
            INSTANCE?.log(LogLevel.INFO, tag, "System Event: $event in $component", null, systemMetadata)
        }

        fun getInstance(context: Context): EnhancedLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EnhancedLogger(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun init(context: Context) {
            getInstance(context).initialize()
        }

    }

    // Instance variables
    private var minLogLevel = LogLevel.DEBUG
    private var enableFileLogging = true
    private var enableRemoteLogging = false
    private var maxLogFileSize = 10 * 1024 * 1024 // 10MB
    private var maxLogFiles = 5

    private var logWriter: BufferedWriter? = null
    private var currentLogFile: File? = null
    private var currentLogFileSize = 0L

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val isShutdown = AtomicBoolean(false)
    private var loggingJob: Job? = null
    private var remoteLogCallback: RemoteLogCallback? = null

    fun initialize() {
        if (enableFileLogging) {
            initializeFileLogging()
        }
        startLoggingWorker()
    }

    fun shutdown() {
        isShutdown.set(true)
        loggingJob?.cancel()
        try {
            logWriter?.close()
        } catch (e: Exception) {
            Log.e("EnhancedLogger", "Error closing log writer", e)
        }
    }

    fun configure(
        minLogLevel: LogLevel = LogLevel.DEBUG,
        enableFileLogging: Boolean = true,
        enableRemoteLogging: Boolean = false,
        maxLogFileSize: Int = 10 * 1024 * 1024,
        maxLogFiles: Int = 5
    ) {
        this.minLogLevel = minLogLevel
        this.enableFileLogging = enableFileLogging
        this.enableRemoteLogging = enableRemoteLogging
        this.maxLogFileSize = maxLogFileSize
        this.maxLogFiles = maxLogFiles

        if (enableFileLogging && logWriter == null) {
            initializeFileLogging()
        }
    }

    fun setRemoteLogCallback(callback: RemoteLogCallback) {
        this.remoteLogCallback = callback
    }

    // Instance logging methods
    fun v(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.VERBOSE, tag, message, null, metadata)
    }

    fun d(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.DEBUG, tag, message, null, metadata)
    }

    fun i(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.INFO, tag, message, null, metadata)
    }

    fun w(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.WARN, tag, message, null, metadata)
    }

    fun w(tag: String, message: String, throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.WARN, tag, message, throwable, metadata)
    }

    fun e(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.ERROR, tag, message, null, metadata)
    }

    fun e(tag: String, message: String, throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.ERROR, tag, message, throwable, metadata)
    }

    fun logPerformance(tag: String, operation: String, duration: Long, metadata: Map<String, String> = emptyMap()) {
        val performanceMetadata = metadata.toMutableMap().apply {
            put("operation", operation)
            put("duration_ms", duration.toString())
            put("type", "performance")
        }
        log(LogLevel.INFO, tag, "Performance: $operation took ${duration}ms", null, performanceMetadata)
    }

    fun logNetwork(tag: String, event: String, url: String? = null, statusCode: Int? = null, metadata: Map<String, String> = emptyMap()) {
        val networkMetadata = metadata.toMutableMap().apply {
            put("event", event)
            put("type", "network")
            url?.let { put("url", it) }
            statusCode?.let { put("status_code", it.toString()) }
        }
        val message = buildString {
            append("Network: $event")
            url?.let { append(" - $it") }
            statusCode?.let { append(" (Status: $it)") }
        }
        log(LogLevel.INFO, tag, message, null, networkMetadata)
    }

    fun logUserAction(tag: String, action: String, screen: String? = null, metadata: Map<String, String> = emptyMap()) {
        val userMetadata = metadata.toMutableMap().apply {
            put("action", action)
            put("type", "user_action")
            screen?.let { put("screen", it) }
        }
        log(LogLevel.INFO, tag, "User Action: $action${screen?.let { " on $it" } ?: ""}", null, userMetadata)
    }

    fun logSystemEvent(tag: String, event: String, component: String, metadata: Map<String, String> = emptyMap()) {
        val systemMetadata = metadata.toMutableMap().apply {
            put("event", event)
            put("component", component)
            put("type", "system_event")
        }
        log(LogLevel.INFO, tag, "System Event: $event in $component", null, systemMetadata)
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?, metadata: Map<String, String>) {
        if (level.priority < minLogLevel.priority) {
            return
        }

        // Always log to Android Log
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
            LogLevel.ASSERT -> Log.wtf(tag, message, throwable)
        }

        // Queue for file and remote logging
        val logEntry = LogEntry(System.currentTimeMillis(), level, tag, message, throwable, metadata)
        logQueue.offer(logEntry)
    }

    private fun initializeFileLogging() {
        try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            cleanupOldLogFiles(logDir)

            val dateFormat = SimpleDateFormat(FILE_DATE_FORMAT, Locale.US)
            val currentDate = dateFormat.format(Date())
            currentLogFile = File(logDir, "$LOG_FILE_PREFIX$currentDate$LOG_FILE_EXTENSION")

            logWriter = BufferedWriter(FileWriter(currentLogFile, true))
            currentLogFileSize = currentLogFile?.length() ?: 0L

        } catch (e: Exception) {
            Log.e("EnhancedLogger", "Failed to initialize file logging", e)
        }
    }

    private fun cleanupOldLogFiles(logDir: File) {
        try {
            val logFiles = logDir.listFiles { _, name -> 
                name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION) 
            }?.sortedByDescending { it.lastModified() }

            logFiles?.drop(maxLogFiles - 1)?.forEach { file ->
                try {
                    file.delete()
                } catch (e: Exception) {
                    Log.w("EnhancedLogger", "Failed to delete old log file: ${file.name}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("EnhancedLogger", "Error during log file cleanup", e)
        }
    }

    private fun startLoggingWorker() {
        loggingJob = CoroutineScope(Dispatchers.IO).launch {
            while (!isShutdown.get()) {
                try {
                    processLogQueue()
                    delay(100) // Process logs every 100ms
                } catch (e: Exception) {
                    Log.e("EnhancedLogger", "Error in logging worker", e)
                }
            }
        }
    }

    private suspend fun processLogQueue() {
        val entriesToProcess = mutableListOf<LogEntry>()

        // Drain the queue
        while (logQueue.isNotEmpty()) {
            logQueue.poll()?.let { entriesToProcess.add(it) }
        }

        if (entriesToProcess.isEmpty()) return

        // File logging
        if (enableFileLogging && logWriter != null) {
            try {
                entriesToProcess.forEach { logEntry ->
                    val logLine = logEntry.toFormattedString() + "\n"
                    logWriter?.write(logLine)
                    currentLogFileSize += logLine.length
                }
                logWriter?.flush()

                // Check if we need to rotate the log file
                if (currentLogFileSize > maxLogFileSize) {
                    rotateLogFile()
                }
            } catch (e: Exception) {
                Log.e("EnhancedLogger", "Error writing to log file", e)
            }
        }

        // Remote logging
        if (enableRemoteLogging && remoteLogCallback != null) {
            entriesToProcess.forEach { logEntry ->
                try {
                    remoteLogCallback?.sendLogToRemote(logEntry)
                } catch (e: Exception) {
                    Log.e("EnhancedLogger", "Error sending log to remote", e)
                }
            }
        }
    }

    private fun rotateLogFile() {
        try {
            logWriter?.close()

            val logDir = File(context.getExternalFilesDir(null), "logs")
            val dateFormat = SimpleDateFormat(FILE_DATE_FORMAT, Locale.US)
            val currentDate = dateFormat.format(Date())
            currentLogFile = File(logDir, "$LOG_FILE_PREFIX$currentDate$LOG_FILE_EXTENSION")

            logWriter = BufferedWriter(FileWriter(currentLogFile, true))
            currentLogFileSize = 0L

        } catch (e: Exception) {
            Log.e("EnhancedLogger", "Error rotating log file", e)
        }
    }

    fun getLogFiles(): List<File> {
        return try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            logDir.listFiles { _, name -> 
                name.startsWith(LOG_FILE_PREFIX) && name.endsWith(LOG_FILE_EXTENSION) 
            }?.sortedByDescending { it.lastModified() }?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e("EnhancedLogger", "Error getting log files", e)
            emptyList()
        }
    }

    fun exportLogsAsJson(outputFile: File, maxEntries: Int = 1000): Boolean {
        return try {
            val logEntries = mutableListOf<String>()
            var entryCount = 0

            getLogFiles().forEach { file ->
                if (entryCount >= maxEntries) return@forEach

                file.readLines().forEach { line ->
                    if (entryCount >= maxEntries) return@forEach
                    // Convert formatted log line back to JSON (simplified)
                    logEntries.add("\"$line\"")
                    entryCount++
                }
            }

            outputFile.writeText("[\n${logEntries.joinToString(",\n")}\n]")
            true
        } catch (e: Exception) {
            Log.e("EnhancedLogger", "Error exporting logs as JSON", e)
            false
        }
    }
}
