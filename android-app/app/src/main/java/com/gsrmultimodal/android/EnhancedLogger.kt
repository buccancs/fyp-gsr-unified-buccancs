package com.gsrmultimodal.android

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
 * Enhanced logging system with structured logging, file output, and remote logging capabilities.
 * Provides comprehensive debugging and monitoring for production environments.
 */
class EnhancedLogger private constructor(private val context: Context) {

    private val logQueue = ConcurrentLinkedQueue<LogEntry>()
    private val isLogging = AtomicBoolean(false)
    private var loggingJob: Job? = null
    private var logFile: File? = null
    private var logWriter: BufferedWriter? = null

    // Configuration
    private var minLogLevel = LogLevel.DEBUG
    private var enableFileLogging = true
    private var enableRemoteLogging = false
    private var maxLogFileSize = 10 * 1024 * 1024 // 10MB
    private var maxLogFiles = 5

    // Remote logging
    private var remoteLogCallback: RemoteLogCallback? = null

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
            return JSONObject().apply {
                put("timestamp", timestamp)
                put("level", level.tag)
                put("tag", tag)
                put("message", message)
                put("thread", Thread.currentThread().name)
                throwable?.let { 
                    put("exception", it.toString())
                    put("stack_trace", it.stackTraceToString())
                }
                if (metadata.isNotEmpty()) {
                    put("metadata", JSONObject(metadata))
                }
            }.toString()
        }

        fun toFormattedString(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            val timeStr = dateFormat.format(Date(timestamp))
            val threadName = Thread.currentThread().name
            
            return buildString {
                append("$timeStr ${level.tag}/$tag [$threadName]: $message")
                if (metadata.isNotEmpty()) {
                    append(" | Metadata: $metadata")
                }
                throwable?.let {
                    append("\n")
                    append(it.stackTraceToString())
                }
            }
        }
    }

    /**
     * Initialize the enhanced logger.
     */
    fun initialize() {
        if (isLogging.get()) {
            Log.w(TAG, "Logger already initialized")
            return
        }

        try {
            if (enableFileLogging) {
                initializeFileLogging()
            }

            isLogging.set(true)
            startLoggingWorker()
            
            Log.i(TAG, "Enhanced logger initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize enhanced logger", e)
        }
    }

    /**
     * Shutdown the enhanced logger.
     */
    fun shutdown() {
        isLogging.set(false)
        loggingJob?.cancel()
        
        try {
            logWriter?.close()
            logWriter = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing log writer", e)
        }
        
        Log.i(TAG, "Enhanced logger shutdown")
    }

    /**
     * Configure logger settings.
     */
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
        
        Log.i(TAG, "Logger configured: level=${minLogLevel.tag}, file=$enableFileLogging, remote=$enableRemoteLogging")
    }

    /**
     * Set remote logging callback.
     */
    fun setRemoteLogCallback(callback: RemoteLogCallback) {
        this.remoteLogCallback = callback
    }

    // Logging methods
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

    /**
     * Log performance metrics.
     */
    fun logPerformance(tag: String, operation: String, duration: Long, metadata: Map<String, String> = emptyMap()) {
        val perfMetadata = metadata.toMutableMap().apply {
            put("operation", operation)
            put("duration_ms", duration.toString())
            put("type", "performance")
        }
        log(LogLevel.INFO, tag, "Performance: $operation took ${duration}ms", null, perfMetadata)
    }

    /**
     * Log network events.
     */
    fun logNetwork(tag: String, event: String, url: String? = null, statusCode: Int? = null, metadata: Map<String, String> = emptyMap()) {
        val networkMetadata = metadata.toMutableMap().apply {
            put("event", event)
            put("type", "network")
            url?.let { put("url", it) }
            statusCode?.let { put("status_code", it.toString()) }
        }
        log(LogLevel.INFO, tag, "Network: $event", null, networkMetadata)
    }

    /**
     * Log user actions.
     */
    fun logUserAction(tag: String, action: String, screen: String? = null, metadata: Map<String, String> = emptyMap()) {
        val actionMetadata = metadata.toMutableMap().apply {
            put("action", action)
            put("type", "user_action")
            screen?.let { put("screen", it) }
        }
        log(LogLevel.INFO, tag, "User Action: $action", null, actionMetadata)
    }

    /**
     * Log system events.
     */
    fun logSystemEvent(tag: String, event: String, component: String, metadata: Map<String, String> = emptyMap()) {
        val systemMetadata = metadata.toMutableMap().apply {
            put("event", event)
            put("component", component)
            put("type", "system_event")
        }
        log(LogLevel.INFO, tag, "System: $event in $component", null, systemMetadata)
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?, metadata: Map<String, String>) {
        if (level.priority < minLogLevel.priority) {
            return
        }

        val logEntry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable,
            metadata = metadata
        )

        // Always log to Android Log
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message, throwable)
            LogLevel.DEBUG -> Log.d(tag, message, throwable)
            LogLevel.INFO -> Log.i(tag, message, throwable)
            LogLevel.WARN -> Log.w(tag, message, throwable)
            LogLevel.ERROR -> Log.e(tag, message, throwable)
            LogLevel.ASSERT -> Log.wtf(tag, message, throwable)
        }

        // Queue for file/remote logging
        if (enableFileLogging || enableRemoteLogging) {
            logQueue.offer(logEntry)
        }
    }

    private fun initializeFileLogging() {
        try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            // Clean up old log files
            cleanupOldLogFiles(logDir)

            // Create new log file
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            logFile = File(logDir, "app_log_$timestamp.log")
            logWriter = BufferedWriter(FileWriter(logFile, true))

            Log.i(TAG, "File logging initialized: ${logFile?.absolutePath}")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize file logging", e)
            enableFileLogging = false
        }
    }

    private fun cleanupOldLogFiles(logDir: File) {
        try {
            val logFiles = logDir.listFiles { _, name -> name.startsWith("app_log_") && name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() }

            if (logFiles != null && logFiles.size > maxLogFiles) {
                logFiles.drop(maxLogFiles).forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old log file: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old log files", e)
        }
    }

    private fun startLoggingWorker() {
        loggingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isLogging.get()) {
                try {
                    processLogQueue()
                    delay(100) // Process logs every 100ms
                } catch (e: Exception) {
                    Log.e(TAG, "Error in logging worker", e)
                }
            }
        }
    }

    private suspend fun processLogQueue() {
        val logsToProcess = mutableListOf<LogEntry>()
        
        // Drain the queue
        while (logQueue.isNotEmpty()) {
            logQueue.poll()?.let { logsToProcess.add(it) }
        }

        if (logsToProcess.isEmpty()) return

        // Write to file
        if (enableFileLogging && logWriter != null) {
            try {
                logsToProcess.forEach { logEntry ->
                    logWriter?.write(logEntry.toFormattedString())
                    logWriter?.newLine()
                }
                logWriter?.flush()

                // Check file size and rotate if necessary
                logFile?.let { file ->
                    if (file.length() > maxLogFileSize) {
                        rotateLogFile()
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error writing to log file", e)
            }
        }

        // Send to remote
        if (enableRemoteLogging && remoteLogCallback != null) {
            logsToProcess.forEach { logEntry ->
                try {
                    remoteLogCallback?.sendLogToRemote(logEntry)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending log to remote", e)
                }
            }
        }
    }

    private fun rotateLogFile() {
        try {
            logWriter?.close()
            
            val logDir = logFile?.parentFile
            cleanupOldLogFiles(logDir!!)
            
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            logFile = File(logDir, "app_log_$timestamp.log")
            logWriter = BufferedWriter(FileWriter(logFile, true))
            
            Log.i(TAG, "Log file rotated: ${logFile?.absolutePath}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error rotating log file", e)
        }
    }

    /**
     * Get log file paths for sharing or analysis.
     */
    fun getLogFiles(): List<File> {
        return try {
            val logDir = File(context.getExternalFilesDir(null), "logs")
            logDir.listFiles { _, name -> name.startsWith("app_log_") && name.endsWith(".log") }
                ?.sortedByDescending { it.lastModified() }
                ?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting log files", e)
            emptyList()
        }
    }

    /**
     * Export logs as JSON for analysis.
     */
    fun exportLogsAsJson(outputFile: File, maxEntries: Int = 1000): Boolean {
        return try {
            val logFiles = getLogFiles()
            val allLogs = mutableListOf<String>()
            
            logFiles.forEach { file ->
                file.readLines().take(maxEntries - allLogs.size).forEach { line ->
                    allLogs.add(line)
                }
                if (allLogs.size >= maxEntries) return@forEach
            }
            
            outputFile.writeText(allLogs.joinToString("\n"))
            Log.i(TAG, "Logs exported to: ${outputFile.absolutePath}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting logs", e)
            false
        }
    }

    companion object {
        private const val TAG = "EnhancedLogger"
        
        @Volatile
        private var INSTANCE: EnhancedLogger? = null
        
        fun getInstance(context: Context): EnhancedLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EnhancedLogger(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        // Convenience methods for global access
        fun init(context: Context) {
            getInstance(context).initialize()
        }
        
        fun v(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.v(tag, message, metadata)
        }
        
        fun d(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.d(tag, message, metadata)
        }
        
        fun i(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.i(tag, message, metadata)
        }
        
        fun w(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.w(tag, message, metadata)
        }
        
        fun e(tag: String, message: String, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.e(tag, message, metadata)
        }
        
        fun e(tag: String, message: String, throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
            INSTANCE?.e(tag, message, throwable, metadata)
        }
    }
}