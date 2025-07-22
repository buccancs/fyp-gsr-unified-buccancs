package com.buccancs.gsrcapture.monitoring

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Data class representing performance metrics
 */
data class PerformanceMetrics(
    val timestamp: Long,
    val batteryLevel: Int,
    val batteryTemperature: Float,
    val batteryVoltage: Int,
    val isCharging: Boolean,
    val chargingType: String,
    val cpuUsage: Float,
    val memoryUsage: MemoryUsage,
    val storageUsage: StorageUsage,
    val thermalState: Int,
    val networkStats: NetworkStats? = null
)

/**
 * Data class representing memory usage
 */
data class MemoryUsage(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long,
    val usagePercentage: Float,
    val lowMemoryThreshold: Long,
    val isLowMemory: Boolean
)

/**
 * Data class representing storage usage
 */
data class StorageUsage(
    val totalSpace: Long,
    val freeSpace: Long,
    val usedSpace: Long,
    val usagePercentage: Float,
    val isLowStorage: Boolean
)

/**
 * Data class representing network statistics
 */
data class NetworkStats(
    val wifiConnected: Boolean,
    val mobileConnected: Boolean,
    val networkType: String,
    val signalStrength: Int
)

/**
 * Performance monitoring configuration
 */
data class MonitoringConfig(
    val updateInterval: Long = 5000, // Update interval in milliseconds
    val enableBatteryMonitoring: Boolean = true,
    val enableMemoryMonitoring: Boolean = true,
    val enableStorageMonitoring: Boolean = true,
    val enableCpuMonitoring: Boolean = true,
    val enableNetworkMonitoring: Boolean = false,
    val lowBatteryThreshold: Int = 20, // Percentage
    val lowMemoryThreshold: Float = 85.0f, // Percentage
    val lowStorageThreshold: Float = 90.0f, // Percentage
    val highCpuThreshold: Float = 80.0f, // Percentage
    val maxHistorySize: Int = 1000 // Maximum number of metrics to keep in memory
)

/**
 * Performance monitor that tracks device performance metrics
 */
class PerformanceMonitor(
    private val context: Context,
    private val config: MonitoringConfig = MonitoringConfig()
) {
    companion object {
        private const val TAG = "PerformanceMonitor"
    }

    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val isMonitoring = AtomicBoolean(false)
    
    // Metrics history
    private val metricsHistory = mutableListOf<PerformanceMetrics>()
    
    // System services
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
    
    // Callbacks
    private var metricsUpdateCallback: ((PerformanceMetrics) -> Unit)? = null
    private var alertCallback: ((String, PerformanceMetrics) -> Unit)? = null
    private var lowBatteryCallback: ((Int) -> Unit)? = null
    private var lowMemoryCallback: ((MemoryUsage) -> Unit)? = null
    private var lowStorageCallback: ((StorageUsage) -> Unit)? = null

    /**
     * Start performance monitoring
     */
    fun startMonitoring() {
        if (isMonitoring.getAndSet(true)) {
            Log.w(TAG, "Performance monitoring already started")
            return
        }

        Log.d(TAG, "Starting performance monitoring")
        
        scheduler.scheduleAtFixedRate({
            try {
                val metrics = collectMetrics()
                
                // Add to history
                synchronized(metricsHistory) {
                    metricsHistory.add(metrics)
                    
                    // Remove old metrics if history is too large
                    while (metricsHistory.size > config.maxHistorySize) {
                        metricsHistory.removeAt(0)
                    }
                }
                
                // Check for alerts
                checkAlerts(metrics)
                
                // Notify callback on main thread
                mainHandler.post {
                    metricsUpdateCallback?.invoke(metrics)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting performance metrics", e)
            }
        }, 0, config.updateInterval, TimeUnit.MILLISECONDS)
    }

    /**
     * Stop performance monitoring
     */
    fun stopMonitoring() {
        if (!isMonitoring.getAndSet(false)) {
            Log.w(TAG, "Performance monitoring not started")
            return
        }

        Log.d(TAG, "Stopping performance monitoring")
        scheduler.shutdown()
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    /**
     * Collect current performance metrics
     */
    private fun collectMetrics(): PerformanceMetrics {
        val timestamp = System.currentTimeMillis()
        
        // Battery metrics
        val batteryMetrics = if (config.enableBatteryMonitoring) {
            collectBatteryMetrics()
        } else {
            BatteryMetrics(0, 0f, 0, false, "Unknown")
        }
        
        // Memory metrics
        val memoryUsage = if (config.enableMemoryMonitoring) {
            collectMemoryMetrics()
        } else {
            MemoryUsage(0, 0, 0, 0f, 0, false)
        }
        
        // Storage metrics
        val storageUsage = if (config.enableStorageMonitoring) {
            collectStorageMetrics()
        } else {
            StorageUsage(0, 0, 0, 0f, false)
        }
        
        // CPU metrics
        val cpuUsage = if (config.enableCpuMonitoring) {
            collectCpuUsage()
        } else {
            0f
        }
        
        // Thermal state
        val thermalState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
                powerManager.currentThermalStatus
            } catch (e: Exception) {
                0 // THERMAL_STATUS_NONE
            }
        } else {
            0
        }
        
        // Network stats (if enabled)
        val networkStats = if (config.enableNetworkMonitoring) {
            collectNetworkStats()
        } else {
            null
        }
        
        return PerformanceMetrics(
            timestamp = timestamp,
            batteryLevel = batteryMetrics.level,
            batteryTemperature = batteryMetrics.temperature,
            batteryVoltage = batteryMetrics.voltage,
            isCharging = batteryMetrics.isCharging,
            chargingType = batteryMetrics.chargingType,
            cpuUsage = cpuUsage,
            memoryUsage = memoryUsage,
            storageUsage = storageUsage,
            thermalState = thermalState,
            networkStats = networkStats
        )
    }

    private data class BatteryMetrics(
        val level: Int,
        val temperature: Float,
        val voltage: Int,
        val isCharging: Boolean,
        val chargingType: String
    )

    private fun collectBatteryMetrics(): BatteryMetrics {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryLevel = if (level >= 0 && scale > 0) {
            (level * 100 / scale)
        } else {
            0
        }
        
        val temperature = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                        status == BatteryManager.BATTERY_STATUS_FULL
        
        val chargePlug = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val chargingType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Not Charging"
        }
        
        return BatteryMetrics(batteryLevel, temperature, voltage, isCharging, chargingType)
    }

    private fun collectMemoryMetrics(): MemoryUsage {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMemory = memoryInfo.totalMem
        val availableMemory = memoryInfo.availMem
        val usedMemory = totalMemory - availableMemory
        val usagePercentage = (usedMemory.toFloat() / totalMemory * 100)
        val lowMemoryThreshold = memoryInfo.threshold
        val isLowMemory = memoryInfo.lowMemory
        
        return MemoryUsage(
            totalMemory = totalMemory,
            availableMemory = availableMemory,
            usedMemory = usedMemory,
            usagePercentage = usagePercentage,
            lowMemoryThreshold = lowMemoryThreshold,
            isLowMemory = isLowMemory
        )
    }

    private fun collectStorageMetrics(): StorageUsage {
        val internalStorage = context.filesDir
        val totalSpace = internalStorage.totalSpace
        val freeSpace = internalStorage.freeSpace
        val usedSpace = totalSpace - freeSpace
        val usagePercentage = (usedSpace.toFloat() / totalSpace * 100)
        val isLowStorage = usagePercentage > config.lowStorageThreshold
        
        return StorageUsage(
            totalSpace = totalSpace,
            freeSpace = freeSpace,
            usedSpace = usedSpace,
            usagePercentage = usagePercentage,
            isLowStorage = isLowStorage
        )
    }

    private fun collectCpuUsage(): Float {
        return try {
            val statFile = RandomAccessFile("/proc/stat", "r")
            val cpuLine = statFile.readLine()
            statFile.close()
            
            val cpuTimes = cpuLine.split("\\s+".toRegex()).drop(1).map { it.toLong() }
            val idleTime = cpuTimes[3]
            val totalTime = cpuTimes.sum()
            
            // This is a simplified CPU usage calculation
            // In a real implementation, you would need to calculate the difference between two readings
            val usage = ((totalTime - idleTime).toFloat() / totalTime * 100)
            usage.coerceIn(0f, 100f)
        } catch (e: Exception) {
            Log.w(TAG, "Could not read CPU usage", e)
            0f
        }
    }

    private fun collectNetworkStats(): NetworkStats {
        // This is a simplified network stats collection
        // In a real implementation, you would use ConnectivityManager and other network APIs
        return NetworkStats(
            wifiConnected = false,
            mobileConnected = false,
            networkType = "Unknown",
            signalStrength = 0
        )
    }

    private fun checkAlerts(metrics: PerformanceMetrics) {
        // Check battery alerts
        if (metrics.batteryLevel <= config.lowBatteryThreshold) {
            mainHandler.post {
                alertCallback?.invoke("Low battery: ${metrics.batteryLevel}%", metrics)
                lowBatteryCallback?.invoke(metrics.batteryLevel)
            }
        }
        
        // Check memory alerts
        if (metrics.memoryUsage.usagePercentage > config.lowMemoryThreshold) {
            mainHandler.post {
                alertCallback?.invoke("High memory usage: ${String.format("%.1f", metrics.memoryUsage.usagePercentage)}%", metrics)
                lowMemoryCallback?.invoke(metrics.memoryUsage)
            }
        }
        
        // Check storage alerts
        if (metrics.storageUsage.usagePercentage > config.lowStorageThreshold) {
            mainHandler.post {
                alertCallback?.invoke("Low storage: ${String.format("%.1f", metrics.storageUsage.usagePercentage)}% used", metrics)
                lowStorageCallback?.invoke(metrics.storageUsage)
            }
        }
        
        // Check CPU alerts
        if (metrics.cpuUsage > config.highCpuThreshold) {
            mainHandler.post {
                alertCallback?.invoke("High CPU usage: ${String.format("%.1f", metrics.cpuUsage)}%", metrics)
            }
        }
    }

    /**
     * Get current performance metrics
     */
    fun getCurrentMetrics(): PerformanceMetrics? {
        return synchronized(metricsHistory) {
            metricsHistory.lastOrNull()
        }
    }

    /**
     * Get metrics history
     */
    fun getMetricsHistory(): List<PerformanceMetrics> {
        return synchronized(metricsHistory) {
            metricsHistory.toList()
        }
    }

    /**
     * Get metrics for a specific time range
     */
    fun getMetricsInRange(startTime: Long, endTime: Long): List<PerformanceMetrics> {
        return synchronized(metricsHistory) {
            metricsHistory.filter { it.timestamp in startTime..endTime }
        }
    }

    /**
     * Clear metrics history
     */
    fun clearHistory() {
        synchronized(metricsHistory) {
            metricsHistory.clear()
        }
    }

    /**
     * Generate performance summary
     */
    fun generateSummary(): String {
        val metrics = synchronized(metricsHistory) { metricsHistory.toList() }
        
        if (metrics.isEmpty()) {
            return "No performance data available"
        }
        
        val avgBattery = metrics.map { it.batteryLevel }.average()
        val avgMemory = metrics.map { it.memoryUsage.usagePercentage }.average()
        val avgStorage = metrics.map { it.storageUsage.usagePercentage }.average()
        val avgCpu = metrics.map { it.cpuUsage }.average()
        
        val minBattery = metrics.minOfOrNull { it.batteryLevel } ?: 0
        val maxMemory = metrics.maxOfOrNull { it.memoryUsage.usagePercentage } ?: 0f
        val maxCpu = metrics.maxOfOrNull { it.cpuUsage } ?: 0f
        
        return buildString {
            appendLine("=== Performance Summary ===")
            appendLine("Monitoring Duration: ${(metrics.last().timestamp - metrics.first().timestamp) / 1000} seconds")
            appendLine("Data Points: ${metrics.size}")
            appendLine()
            appendLine("Battery:")
            appendLine("  Average Level: ${String.format("%.1f", avgBattery)}%")
            appendLine("  Minimum Level: $minBattery%")
            appendLine()
            appendLine("Memory:")
            appendLine("  Average Usage: ${String.format("%.1f", avgMemory)}%")
            appendLine("  Peak Usage: ${String.format("%.1f", maxMemory)}%")
            appendLine()
            appendLine("Storage:")
            appendLine("  Average Usage: ${String.format("%.1f", avgStorage)}%")
            appendLine()
            appendLine("CPU:")
            appendLine("  Average Usage: ${String.format("%.1f", avgCpu)}%")
            appendLine("  Peak Usage: ${String.format("%.1f", maxCpu)}%")
        }
    }

    // Callback setters
    fun setMetricsUpdateCallback(callback: (PerformanceMetrics) -> Unit) {
        metricsUpdateCallback = callback
    }

    fun setAlertCallback(callback: (String, PerformanceMetrics) -> Unit) {
        alertCallback = callback
    }

    fun setLowBatteryCallback(callback: (Int) -> Unit) {
        lowBatteryCallback = callback
    }

    fun setLowMemoryCallback(callback: (MemoryUsage) -> Unit) {
        lowMemoryCallback = callback
    }

    fun setLowStorageCallback(callback: (StorageUsage) -> Unit) {
        lowStorageCallback = callback
    }

    /**
     * Check if monitoring is active
     */
    fun isMonitoring(): Boolean = isMonitoring.get()
}