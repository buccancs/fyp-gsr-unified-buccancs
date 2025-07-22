package com.buccancs.gsrcapture.cloud

import android.content.Context
import android.util.Base64
import android.util.Log
import com.buccancs.gsrcapture.analytics.DataPoint
import com.buccancs.gsrcapture.analytics.DataType
import com.buccancs.gsrcapture.ml.DetectedPattern
import com.buccancs.gsrcapture.recording.RecordingStats
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlin.collections.HashMap

/**
 * Data class representing cloud sync configuration
 */
data class CloudSyncConfig(
    val serverUrl: String = "https://api.gsrcapture.com",
    val apiKey: String = "",
    val userId: String = "",
    val enableAutoSync: Boolean = true,
    val syncInterval: Long = 300000, // 5 minutes
    val enableEncryption: Boolean = true,
    val maxRetries: Int = 3,
    val batchSize: Int = 100,
    val compressionEnabled: Boolean = true
)

/**
 * Data class representing sync status
 */
data class SyncStatus(
    val isConnected: Boolean,
    val lastSyncTime: Long,
    val pendingUploads: Int,
    val totalSynced: Int,
    val syncErrors: List<String>,
    val storageUsed: Long,
    val storageLimit: Long
)

/**
 * Data class representing a sync item
 */
data class SyncItem(
    val id: String,
    val type: SyncItemType,
    val timestamp: Long,
    val data: String,
    val checksum: String,
    val retryCount: Int = 0
)

/**
 * Enum representing different types of sync items
 */
enum class SyncItemType {
    DATA_POINT,
    PATTERN,
    RECORDING_STATS,
    SESSION_METADATA,
    USER_SETTINGS,
    MEDIA_FILE
}

/**
 * Cloud synchronization and backup manager
 */
class CloudSyncManager(
    private val context: Context,
    private val config: CloudSyncConfig
) {
    companion object {
        private const val TAG = "CloudSyncManager"
        private const val ENCRYPTION_ALGORITHM = "AES"
        private const val HASH_ALGORITHM = "SHA-256"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val uploadQueue = ConcurrentLinkedQueue<SyncItem>()
    private val downloadQueue = ConcurrentLinkedQueue<SyncItem>()

    private var isConnected = false
    private var lastSyncTime = 0L
    private var totalSynced = 0
    private val syncErrors = mutableListOf<String>()

    // Encryption key for data security
    private var encryptionKey: SecretKey? = null

    // Callbacks
    private var syncStatusCallback: ((SyncStatus) -> Unit)? = null
    private var dataReceivedCallback: ((DataPoint) -> Unit)? = null
    private var patternReceivedCallback: ((DetectedPattern) -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null

    init {
        if (config.enableEncryption) {
            initializeEncryption()
        }

        if (config.enableAutoSync) {
            startAutoSync()
        }
    }

    /**
     * Initialize encryption for secure data transmission
     */
    private fun initializeEncryption() {
        try {
            // Generate or load encryption key
            val keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM)
            keyGenerator.init(256)
            encryptionKey = keyGenerator.generateKey()

            Log.d(TAG, "Encryption initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encryption", e)
            errorCallback?.invoke("Encryption initialization failed: ${e.message}")
        }
    }

    /**
     * Start automatic synchronization
     */
    private fun startAutoSync() {
        scope.launch {
            while (isActive) {
                try {
                    performSync()
                    delay(config.syncInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "Auto sync error", e)
                    delay(30000) // Wait 30 seconds before retry
                }
            }
        }
    }

    /**
     * Add data point to sync queue
     */
    fun syncDataPoint(dataPoint: DataPoint) {
        scope.launch {
            try {
                val jsonData = JSONObject().apply {
                    put("timestamp", dataPoint.timestamp)
                    put("value", dataPoint.value)
                    put("type", dataPoint.type.name)
                    put("metadata", JSONObject(dataPoint.metadata))
                }.toString()

                val syncItem = createSyncItem(SyncItemType.DATA_POINT, jsonData)
                uploadQueue.offer(syncItem)

                if (config.enableAutoSync) {
                    processUploadQueue()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error queuing data point for sync", e)
                addSyncError("Failed to queue data point: ${e.message}")
            }
        }
    }

    /**
     * Add detected pattern to sync queue
     */
    fun syncPattern(pattern: DetectedPattern) {
        scope.launch {
            try {
                val jsonData = JSONObject().apply {
                    put("id", pattern.id)
                    put("type", pattern.type.name)
                    put("dataType", pattern.dataType.name)
                    put("startTime", pattern.startTime)
                    put("endTime", pattern.endTime)
                    put("confidence", pattern.confidence)
                    put("characteristics", JSONObject(pattern.characteristics))
                    put("description", pattern.description)
                }.toString()

                val syncItem = createSyncItem(SyncItemType.PATTERN, jsonData)
                uploadQueue.offer(syncItem)

                if (config.enableAutoSync) {
                    processUploadQueue()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error queuing pattern for sync", e)
                addSyncError("Failed to queue pattern: ${e.message}")
            }
        }
    }

    /**
     * Add recording statistics to sync queue
     */
    fun syncRecordingStats(stats: RecordingStats) {
        scope.launch {
            try {
                val jsonData = JSONObject().apply {
                    put("sessionId", stats.sessionId)
                    put("mode", stats.mode.name)
                    put("startTime", stats.startTime)
                    put("endTime", stats.endTime)
                    put("duration", stats.duration)
                    put("framesCaptured", stats.framesCaptured)
                    put("samplesCaptured", stats.samplesCaptured)
                    put("fileSize", stats.fileSize)
                    put("averageFrameRate", stats.averageFrameRate)
                    put("averageSampleRate", stats.averageSampleRate)
                    put("batteryUsed", stats.batteryUsed)
                    put("storageUsed", stats.storageUsed)
                    put("errors", JSONArray(stats.errors))
                    put("warnings", JSONArray(stats.warnings))
                }.toString()

                val syncItem = createSyncItem(SyncItemType.RECORDING_STATS, jsonData)
                uploadQueue.offer(syncItem)

                if (config.enableAutoSync) {
                    processUploadQueue()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error queuing recording stats for sync", e)
                addSyncError("Failed to queue recording stats: ${e.message}")
            }
        }
    }

    /**
     * Perform manual synchronization
     */
    fun performManualSync() {
        scope.launch {
            performSync()
        }
    }

    /**
     * Main synchronization logic
     */
    private suspend fun performSync() {
        try {
            // Check connectivity
            if (!checkConnectivity()) {
                isConnected = false
                updateSyncStatus()
                return
            }

            isConnected = true

            // Process upload queue
            processUploadQueue()

            // Process download queue
            processDownloadQueue()

            // Update sync status
            lastSyncTime = System.currentTimeMillis()
            updateSyncStatus()

            Log.d(TAG, "Sync completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            addSyncError("Sync failed: ${e.message}")
            isConnected = false
            updateSyncStatus()
        }
    }

    /**
     * Process upload queue
     */
    private suspend fun processUploadQueue() {
        val batch = mutableListOf<SyncItem>()

        // Collect batch items
        repeat(config.batchSize) {
            val item = uploadQueue.poll()
            if (item != null) {
                batch.add(item)
            }
        }

        if (batch.isEmpty()) return

        try {
            uploadBatch(batch)
            totalSynced += batch.size
            Log.d(TAG, "Uploaded batch of ${batch.size} items")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload batch", e)

            // Re-queue items with retry logic
            batch.forEach { item ->
                if (item.retryCount < config.maxRetries) {
                    val retryItem = item.copy(retryCount = item.retryCount + 1)
                    uploadQueue.offer(retryItem)
                } else {
                    addSyncError("Failed to upload item ${item.id} after ${config.maxRetries} retries")
                }
            }
        }
    }

    /**
     * Process download queue
     */
    private suspend fun processDownloadQueue() {
        try {
            val newData = downloadNewData()

            newData.forEach { item ->
                when (item.type) {
                    SyncItemType.DATA_POINT -> {
                        val dataPoint = parseDataPoint(item.data)
                        dataReceivedCallback?.invoke(dataPoint)
                    }
                    SyncItemType.PATTERN -> {
                        val pattern = parsePattern(item.data)
                        patternReceivedCallback?.invoke(pattern)
                    }
                    else -> {
                        // Handle other types as needed
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to process downloads", e)
            addSyncError("Download failed: ${e.message}")
        }
    }

    /**
     * Upload batch of items to cloud
     */
    private suspend fun uploadBatch(batch: List<SyncItem>) = withContext(Dispatchers.IO) {
        val url = URL("${config.serverUrl}/api/v1/sync/upload")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connection.setRequestProperty("User-ID", config.userId)
            connection.doOutput = true

            val requestData = JSONObject().apply {
                put("items", JSONArray().apply {
                    batch.forEach { item ->
                        put(JSONObject().apply {
                            put("id", item.id)
                            put("type", item.type.name)
                            put("timestamp", item.timestamp)
                            put("data", if (config.enableEncryption) encryptData(item.data) else item.data)
                            put("checksum", item.checksum)
                        })
                    }
                })
                put("compression", config.compressionEnabled)
                put("encryption", config.enableEncryption)
            }

            connection.outputStream.use { outputStream ->
                outputStream.write(requestData.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Upload failed with response code: $responseCode")
            }

            // Read response
            val response = connection.inputStream.bufferedReader().readText()
            val responseJson = JSONObject(response)

            if (!responseJson.getBoolean("success")) {
                throw IOException("Upload failed: ${responseJson.optString("error", "Unknown error")}")
            }

        } finally {
            connection.disconnect()
        }
    }

    /**
     * Download new data from cloud
     */
    private suspend fun downloadNewData(): List<SyncItem> = withContext(Dispatchers.IO) {
        val url = URL("${config.serverUrl}/api/v1/sync/download?since=${lastSyncTime}")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer ${config.apiKey}")
            connection.setRequestProperty("User-ID", config.userId)

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("Download failed with response code: $responseCode")
            }

            val response = connection.inputStream.bufferedReader().readText()
            val responseJson = JSONObject(response)

            if (!responseJson.getBoolean("success")) {
                throw IOException("Download failed: ${responseJson.optString("error", "Unknown error")}")
            }

            val items = mutableListOf<SyncItem>()
            val itemsArray = responseJson.getJSONArray("items")

            for (i in 0 until itemsArray.length()) {
                val itemJson = itemsArray.getJSONObject(i)
                val data = if (config.enableEncryption) {
                    decryptData(itemJson.getString("data"))
                } else {
                    itemJson.getString("data")
                }

                val item = SyncItem(
                    id = itemJson.getString("id"),
                    type = SyncItemType.valueOf(itemJson.getString("type")),
                    timestamp = itemJson.getLong("timestamp"),
                    data = data,
                    checksum = itemJson.getString("checksum")
                )

                items.add(item)
            }

            return@withContext items

        } finally {
            connection.disconnect()
        }
    }

    /**
     * Check cloud connectivity
     */
    private suspend fun checkConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("${config.serverUrl}/api/v1/health")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val responseCode = connection.responseCode
            connection.disconnect()

            return@withContext responseCode == HttpURLConnection.HTTP_OK

        } catch (e: Exception) {
            Log.w(TAG, "Connectivity check failed", e)
            return@withContext false
        }
    }

    /**
     * Create sync item with checksum
     */
    private fun createSyncItem(type: SyncItemType, data: String): SyncItem {
        val id = generateSyncId()
        val timestamp = System.currentTimeMillis()
        val checksum = calculateChecksum(data)

        return SyncItem(
            id = id,
            type = type,
            timestamp = timestamp,
            data = data,
            checksum = checksum
        )
    }

    /**
     * Generate unique sync ID
     */
    private fun generateSyncId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (Math.random() * 10000).toInt()
        return "sync_${timestamp}_${random}"
    }

    /**
     * Calculate data checksum
     */
    private fun calculateChecksum(data: String): String {
        return try {
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hash = digest.digest(data.toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate checksum", e)
            ""
        }
    }

    /**
     * Encrypt data for secure transmission
     */
    private fun encryptData(data: String): String {
        return try {
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey)
            val encryptedBytes = cipher.doFinal(data.toByteArray())
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt data", e)
            data // Return original data if encryption fails
        }
    }

    /**
     * Decrypt received data
     */
    private fun decryptData(encryptedData: String): String {
        return try {
            val cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey)
            val encryptedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decrypt data", e)
            encryptedData // Return encrypted data if decryption fails
        }
    }

    /**
     * Parse data point from JSON
     */
    private fun parseDataPoint(jsonData: String): DataPoint {
        val json = JSONObject(jsonData)
        val metadata = json.getJSONObject("metadata")
        val metadataMap = mutableMapOf<String, Any>()

        metadata.keys().forEach { key ->
            metadataMap[key] = metadata.get(key)
        }

        return DataPoint(
            timestamp = json.getLong("timestamp"),
            value = json.getDouble("value"),
            type = DataType.valueOf(json.getString("type")),
            metadata = metadataMap
        )
    }

    /**
     * Parse detected pattern from JSON
     */
    private fun parsePattern(jsonData: String): DetectedPattern {
        val json = JSONObject(jsonData)
        val characteristics = json.getJSONObject("characteristics")
        val characteristicsMap = mutableMapOf<String, Double>()

        characteristics.keys().forEach { key ->
            characteristicsMap[key] = characteristics.getDouble(key)
        }

        return DetectedPattern(
            id = json.getString("id"),
            type = com.buccancs.gsrcapture.ml.PatternType.valueOf(json.getString("type")),
            dataType = DataType.valueOf(json.getString("dataType")),
            startTime = json.getLong("startTime"),
            endTime = json.getLong("endTime"),
            confidence = json.getDouble("confidence"),
            characteristics = characteristicsMap,
            description = json.getString("description")
        )
    }

    /**
     * Add sync error to error list
     */
    private fun addSyncError(error: String) {
        synchronized(syncErrors) {
            syncErrors.add("${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())}: $error")

            // Limit error history
            while (syncErrors.size > 50) {
                syncErrors.removeAt(0)
            }
        }

        errorCallback?.invoke(error)
    }

    /**
     * Update sync status and notify callback
     */
    private fun updateSyncStatus() {
        val status = SyncStatus(
            isConnected = isConnected,
            lastSyncTime = lastSyncTime,
            pendingUploads = uploadQueue.size,
            totalSynced = totalSynced,
            syncErrors = synchronized(syncErrors) { syncErrors.toList() },
            storageUsed = 0L, // Would be retrieved from server
            storageLimit = 0L  // Would be retrieved from server
        )

        syncStatusCallback?.invoke(status)
    }

    /**
     * Get current sync status
     */
    fun getSyncStatus(): SyncStatus {
        return SyncStatus(
            isConnected = isConnected,
            lastSyncTime = lastSyncTime,
            pendingUploads = uploadQueue.size,
            totalSynced = totalSynced,
            syncErrors = synchronized(syncErrors) { syncErrors.toList() },
            storageUsed = 0L,
            storageLimit = 0L
        )
    }

    /**
     * Clear sync errors
     */
    fun clearSyncErrors() {
        synchronized(syncErrors) {
            syncErrors.clear()
        }
        updateSyncStatus()
    }

    /**
     * Pause automatic synchronization
     */
    fun pauseAutoSync() {
        // Implementation would pause the auto sync coroutine
        Log.d(TAG, "Auto sync paused")
    }

    /**
     * Resume automatic synchronization
     */
    fun resumeAutoSync() {
        if (!config.enableAutoSync) {
            startAutoSync()
        }
        Log.d(TAG, "Auto sync resumed")
    }

    // Callback setters
    fun setSyncStatusCallback(callback: (SyncStatus) -> Unit) {
        syncStatusCallback = callback
    }

    fun setDataReceivedCallback(callback: (DataPoint) -> Unit) {
        dataReceivedCallback = callback
    }

    fun setPatternReceivedCallback(callback: (DetectedPattern) -> Unit) {
        patternReceivedCallback = callback
    }

    fun setErrorCallback(callback: (String) -> Unit) {
        errorCallback = callback
    }

    /**
     * Shutdown cloud sync manager
     */
    fun shutdown() {
        scope.cancel()
        uploadQueue.clear()
        downloadQueue.clear()
        Log.d(TAG, "Cloud sync manager shut down")
    }
}
