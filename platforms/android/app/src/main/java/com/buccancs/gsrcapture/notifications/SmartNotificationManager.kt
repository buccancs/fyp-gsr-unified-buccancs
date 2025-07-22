package com.buccancs.gsrcapture.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.buccancs.gsrcapture.MainActivity
import com.buccancs.gsrcapture.R
import com.buccancs.gsrcapture.analytics.DataPoint
import com.buccancs.gsrcapture.analytics.DataType
import com.buccancs.gsrcapture.ml.DetectedPattern
import com.buccancs.gsrcapture.ml.PatternType
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Data class representing a smart notification
 */
data class SmartNotification(
    val id: Int,
    val type: NotificationType,
    val title: String,
    val message: String,
    val priority: NotificationPriority,
    val category: String,
    val timestamp: Long,
    val actionable: Boolean = false,
    val actions: List<NotificationAction> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Enum representing notification types
 */
enum class NotificationType {
    SYSTEM_ALERT,
    DATA_ANOMALY,
    PATTERN_DETECTED,
    RECORDING_STATUS,
    SENSOR_STATUS,
    PERFORMANCE_WARNING,
    BATTERY_LOW,
    STORAGE_LOW,
    SYNC_STATUS,
    STRESS_ALERT,
    HEALTH_INSIGHT,
    MAINTENANCE_REMINDER
}

/**
 * Enum representing notification priorities
 */
enum class NotificationPriority(val value: Int) {
    LOW(NotificationCompat.PRIORITY_LOW),
    NORMAL(NotificationCompat.PRIORITY_DEFAULT),
    HIGH(NotificationCompat.PRIORITY_HIGH),
    URGENT(NotificationCompat.PRIORITY_MAX)
}

/**
 * Data class representing notification actions
 */
data class NotificationAction(
    val id: String,
    val title: String,
    val icon: Int,
    val action: () -> Unit
)

/**
 * Configuration for smart notifications
 */
data class NotificationConfig(
    val enableSmartNotifications: Boolean = true,
    val enablePatternNotifications: Boolean = true,
    val enableAnomalyNotifications: Boolean = true,
    val enablePerformanceNotifications: Boolean = true,
    val enableHealthInsights: Boolean = true,
    val quietHoursStart: Int = 22, // 10 PM
    val quietHoursEnd: Int = 7,    // 7 AM
    val maxNotificationsPerHour: Int = 10,
    val groupSimilarNotifications: Boolean = true,
    val adaptivePriority: Boolean = true
)

/**
 * Smart Notification Manager with AI-driven notification intelligence
 */
class SmartNotificationManager(
    private val context: Context,
    private val config: NotificationConfig = NotificationConfig()
) {
    companion object {
        private const val TAG = "SmartNotificationManager"
        private const val CHANNEL_ID_ALERTS = "gsr_alerts"
        private const val CHANNEL_ID_PATTERNS = "gsr_patterns"
        private const val CHANNEL_ID_SYSTEM = "gsr_system"
        private const val CHANNEL_ID_HEALTH = "gsr_health"
        private const val GROUP_KEY_GSR = "gsr_notifications"
    }

    private val notificationManager = NotificationManagerCompat.from(context)
    private val notificationIdGenerator = AtomicInteger(1000)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Notification history and analytics
    private val notificationHistory = mutableListOf<SmartNotification>()
    private val notificationCounts = mutableMapOf<NotificationType, Int>()
    private val lastNotificationTime = mutableMapOf<NotificationType, Long>()
    
    // Callbacks
    private var notificationClickCallback: ((SmartNotification) -> Unit)? = null
    private var notificationDismissCallback: ((SmartNotification) -> Unit)? = null

    init {
        createNotificationChannels()
        startNotificationAnalytics()
    }

    /**
     * Create notification channels for different types of notifications
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ID_ALERTS,
                    "System Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Critical system alerts and warnings"
                    enableLights(true)
                    lightColor = Color.RED
                    enableVibration(true)
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                },
                
                NotificationChannel(
                    CHANNEL_ID_PATTERNS,
                    "Pattern Detection",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications about detected patterns in data"
                    enableLights(true)
                    lightColor = Color.BLUE
                    enableVibration(false)
                },
                
                NotificationChannel(
                    CHANNEL_ID_SYSTEM,
                    "System Status",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "General system status updates"
                    enableLights(false)
                    enableVibration(false)
                },
                
                NotificationChannel(
                    CHANNEL_ID_HEALTH,
                    "Health Insights",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Health and wellness insights"
                    enableLights(true)
                    lightColor = Color.GREEN
                    enableVibration(true)
                }
            )

            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
            
            Log.d(TAG, "Created ${channels.size} notification channels")
        }
    }

    /**
     * Start notification analytics and optimization
     */
    private fun startNotificationAnalytics() {
        scope.launch {
            while (isActive) {
                try {
                    analyzeNotificationPatterns()
                    optimizeNotificationDelivery()
                    cleanupOldNotifications()
                    delay(300000) // Run every 5 minutes
                } catch (e: Exception) {
                    Log.e(TAG, "Error in notification analytics", e)
                    delay(60000) // Wait 1 minute before retry
                }
            }
        }
    }

    /**
     * Send a smart notification with intelligent routing
     */
    fun sendNotification(notification: SmartNotification) {
        scope.launch {
            try {
                if (!shouldSendNotification(notification)) {
                    Log.d(TAG, "Notification filtered: ${notification.title}")
                    return@launch
                }

                val optimizedNotification = optimizeNotification(notification)
                deliverNotification(optimizedNotification)
                
                // Track notification
                trackNotification(optimizedNotification)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notification", e)
            }
        }
    }

    /**
     * Send notification for detected pattern
     */
    fun notifyPatternDetected(pattern: DetectedPattern) {
        val notification = SmartNotification(
            id = notificationIdGenerator.incrementAndGet(),
            type = NotificationType.PATTERN_DETECTED,
            title = "Pattern Detected",
            message = "${pattern.type.displayName} detected in ${pattern.dataType.displayName} with ${String.format("%.0f", pattern.confidence * 100)}% confidence",
            priority = when (pattern.type) {
                PatternType.STRESS_RESPONSE -> NotificationPriority.HIGH
                PatternType.ANOMALY -> NotificationPriority.HIGH
                else -> NotificationPriority.NORMAL
            },
            category = "patterns",
            timestamp = System.currentTimeMillis(),
            actionable = true,
            actions = listOf(
                NotificationAction("view_details", "View Details", android.R.drawable.ic_menu_info_details) {
                    // Open pattern details
                },
                NotificationAction("dismiss", "Dismiss", android.R.drawable.ic_menu_close_clear_cancel) {
                    // Dismiss notification
                }
            ),
            metadata = mapOf(
                "pattern_id" to pattern.id,
                "pattern_type" to pattern.type.name,
                "confidence" to pattern.confidence
            )
        )
        
        sendNotification(notification)
    }

    /**
     * Send notification for data anomaly
     */
    fun notifyDataAnomaly(dataPoint: DataPoint, anomalyScore: Double) {
        val notification = SmartNotification(
            id = notificationIdGenerator.incrementAndGet(),
            type = NotificationType.DATA_ANOMALY,
            title = "Data Anomaly Detected",
            message = "Unusual ${dataPoint.type.displayName} reading: ${String.format("%.2f", dataPoint.value)} ${dataPoint.type.unit} (${String.format("%.1f", anomalyScore)}σ from normal)",
            priority = if (anomalyScore > 3.0) NotificationPriority.HIGH else NotificationPriority.NORMAL,
            category = "anomalies",
            timestamp = System.currentTimeMillis(),
            actionable = true,
            actions = listOf(
                NotificationAction("investigate", "Investigate", android.R.drawable.ic_menu_search) {
                    // Open investigation view
                },
                NotificationAction("mark_normal", "Mark as Normal", android.R.drawable.ic_menu_agenda) {
                    // Mark as false positive
                }
            ),
            metadata = mapOf(
                "data_type" to dataPoint.type.name,
                "value" to dataPoint.value,
                "anomaly_score" to anomalyScore
            )
        )
        
        sendNotification(notification)
    }

    /**
     * Send notification for stress detection
     */
    fun notifyStressDetected(stressScore: Double) {
        val stressLevel = when {
            stressScore > 3.0 -> "High"
            stressScore > 2.0 -> "Moderate"
            else -> "Mild"
        }
        
        val notification = SmartNotification(
            id = notificationIdGenerator.incrementAndGet(),
            type = NotificationType.STRESS_ALERT,
            title = "$stressLevel Stress Detected",
            message = "Your stress levels appear elevated. Consider taking a break or practicing relaxation techniques.",
            priority = if (stressScore > 2.5) NotificationPriority.HIGH else NotificationPriority.NORMAL,
            category = "health",
            timestamp = System.currentTimeMillis(),
            actionable = true,
            actions = listOf(
                NotificationAction("breathing_exercise", "Breathing Exercise", android.R.drawable.ic_media_play) {
                    // Start breathing exercise
                },
                NotificationAction("view_trends", "View Trends", android.R.drawable.ic_menu_sort_by_size) {
                    // Open stress trends
                }
            ),
            metadata = mapOf(
                "stress_score" to stressScore,
                "stress_level" to stressLevel
            )
        )
        
        sendNotification(notification)
    }

    /**
     * Send system status notification
     */
    fun notifySystemStatus(title: String, message: String, priority: NotificationPriority = NotificationPriority.NORMAL) {
        val notification = SmartNotification(
            id = notificationIdGenerator.incrementAndGet(),
            type = NotificationType.SYSTEM_ALERT,
            title = title,
            message = message,
            priority = priority,
            category = "system",
            timestamp = System.currentTimeMillis()
        )
        
        sendNotification(notification)
    }

    /**
     * Send performance warning notification
     */
    fun notifyPerformanceWarning(component: String, issue: String, severity: String) {
        val notification = SmartNotification(
            id = notificationIdGenerator.incrementAndGet(),
            type = NotificationType.PERFORMANCE_WARNING,
            title = "Performance Warning",
            message = "$component: $issue (Severity: $severity)",
            priority = when (severity.lowercase()) {
                "critical" -> NotificationPriority.URGENT
                "high" -> NotificationPriority.HIGH
                else -> NotificationPriority.NORMAL
            },
            category = "performance",
            timestamp = System.currentTimeMillis(),
            actionable = true,
            actions = listOf(
                NotificationAction("optimize", "Optimize", android.R.drawable.ic_menu_manage) {
                    // Start optimization
                },
                NotificationAction("details", "Details", android.R.drawable.ic_menu_info_details) {
                    // Show details
                }
            ),
            metadata = mapOf(
                "component" to component,
                "issue" to issue,
                "severity" to severity
            )
        )
        
        sendNotification(notification)
    }

    /**
     * Send health insight notification
     */
    fun notifyHealthInsight(insight: String, recommendation: String) {
        val notification = SmartNotification(
            id = notificationIdGenerator.incrementAndGet(),
            type = NotificationType.HEALTH_INSIGHT,
            title = "Health Insight",
            message = "$insight\n\nRecommendation: $recommendation",
            priority = NotificationPriority.NORMAL,
            category = "health",
            timestamp = System.currentTimeMillis(),
            actionable = true,
            actions = listOf(
                NotificationAction("learn_more", "Learn More", android.R.drawable.ic_menu_help) {
                    // Open educational content
                },
                NotificationAction("set_reminder", "Set Reminder", android.R.drawable.ic_menu_recent_history) {
                    // Set reminder
                }
            ),
            metadata = mapOf(
                "insight" to insight,
                "recommendation" to recommendation
            )
        )
        
        sendNotification(notification)
    }

    /**
     * Determine if notification should be sent based on intelligent filtering
     */
    private fun shouldSendNotification(notification: SmartNotification): Boolean {
        if (!config.enableSmartNotifications) return false
        
        // Check quiet hours
        if (isQuietHours()) {
            return notification.priority == NotificationPriority.URGENT
        }
        
        // Check rate limiting
        val currentHour = System.currentTimeMillis() / (1000 * 60 * 60)
        val hourlyCount = notificationHistory.count { 
            it.timestamp / (1000 * 60 * 60) == currentHour 
        }
        
        if (hourlyCount >= config.maxNotificationsPerHour) {
            return notification.priority >= NotificationPriority.HIGH
        }
        
        // Check for duplicate notifications
        val recentSimilar = notificationHistory.takeLast(10).any { recent ->
            recent.type == notification.type && 
            recent.title == notification.title &&
            (System.currentTimeMillis() - recent.timestamp) < 300000 // 5 minutes
        }
        
        if (recentSimilar && !config.groupSimilarNotifications) {
            return false
        }
        
        return true
    }

    /**
     * Optimize notification based on context and user behavior
     */
    private fun optimizeNotification(notification: SmartNotification): SmartNotification {
        var optimized = notification
        
        if (config.adaptivePriority) {
            // Adjust priority based on historical user interaction
            val typeInteractionRate = calculateInteractionRate(notification.type)
            
            if (typeInteractionRate < 0.2 && notification.priority > NotificationPriority.LOW) {
                optimized = optimized.copy(priority = NotificationPriority.LOW)
            } else if (typeInteractionRate > 0.8 && notification.priority < NotificationPriority.HIGH) {
                optimized = optimized.copy(priority = NotificationPriority.HIGH)
            }
        }
        
        return optimized
    }

    /**
     * Deliver the notification to the system
     */
    private fun deliverNotification(notification: SmartNotification) {
        val channelId = when (notification.type) {
            NotificationType.SYSTEM_ALERT, NotificationType.PERFORMANCE_WARNING, 
            NotificationType.BATTERY_LOW, NotificationType.STORAGE_LOW -> CHANNEL_ID_ALERTS
            NotificationType.PATTERN_DETECTED, NotificationType.DATA_ANOMALY -> CHANNEL_ID_PATTERNS
            NotificationType.STRESS_ALERT, NotificationType.HEALTH_INSIGHT -> CHANNEL_ID_HEALTH
            else -> CHANNEL_ID_SYSTEM
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_id", notification.id)
            putExtra("notification_type", notification.type.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            notification.id, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(notification.priority.value)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_KEY_GSR)
            .setWhen(notification.timestamp)
            .setShowWhen(true)

        // Add big text style for longer messages
        if (notification.message.length > 50) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
        }

        // Add actions if notification is actionable
        if (notification.actionable && notification.actions.isNotEmpty()) {
            notification.actions.take(3).forEach { action ->
                val actionIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    putExtra("action_id", action.id)
                    putExtra("notification_id", notification.id)
                }
                
                val actionPendingIntent = PendingIntent.getBroadcast(
                    context,
                    "${notification.id}_${action.id}".hashCode(),
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                builder.addAction(action.icon, action.title, actionPendingIntent)
            }
        }

        try {
            notificationManager.notify(notification.id, builder.build())
            Log.d(TAG, "Delivered notification: ${notification.title}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied for notification", e)
        }
    }

    /**
     * Track notification for analytics
     */
    private fun trackNotification(notification: SmartNotification) {
        synchronized(notificationHistory) {
            notificationHistory.add(notification)
            
            // Limit history size
            while (notificationHistory.size > 1000) {
                notificationHistory.removeAt(0)
            }
        }
        
        notificationCounts[notification.type] = (notificationCounts[notification.type] ?: 0) + 1
        lastNotificationTime[notification.type] = notification.timestamp
    }

    /**
     * Check if current time is within quiet hours
     */
    private fun isQuietHours(): Boolean {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        
        return if (config.quietHoursStart < config.quietHoursEnd) {
            currentHour >= config.quietHoursStart && currentHour < config.quietHoursEnd
        } else {
            currentHour >= config.quietHoursStart || currentHour < config.quietHoursEnd
        }
    }

    /**
     * Calculate interaction rate for notification type
     */
    private fun calculateInteractionRate(type: NotificationType): Double {
        val typeNotifications = notificationHistory.filter { it.type == type }
        if (typeNotifications.isEmpty()) return 0.5 // Default rate
        
        // This would be enhanced with actual click/dismiss tracking
        return 0.6 // Placeholder implementation
    }

    /**
     * Analyze notification patterns for optimization
     */
    private suspend fun analyzeNotificationPatterns() {
        // Analyze user interaction patterns
        val recentNotifications = notificationHistory.filter { 
            System.currentTimeMillis() - it.timestamp < 86400000 // Last 24 hours
        }
        
        // Group by type and analyze effectiveness
        val typeAnalysis = recentNotifications.groupBy { it.type }
        
        typeAnalysis.forEach { (type, notifications) ->
            val avgPriority = notifications.map { it.priority.value }.average()
            val count = notifications.size
            
            Log.d(TAG, "Type: $type, Count: $count, Avg Priority: $avgPriority")
        }
    }

    /**
     * Optimize notification delivery based on patterns
     */
    private suspend fun optimizeNotificationDelivery() {
        // Implement machine learning-based optimization
        // This could include:
        // - Optimal timing prediction
        // - Priority adjustment
        // - Content personalization
        
        Log.d(TAG, "Optimizing notification delivery")
    }

    /**
     * Clean up old notifications from history
     */
    private suspend fun cleanupOldNotifications() {
        val cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000) // 7 days
        
        synchronized(notificationHistory) {
            notificationHistory.removeAll { it.timestamp < cutoffTime }
        }
        
        Log.d(TAG, "Cleaned up old notifications")
    }

    /**
     * Get notification statistics
     */
    fun getNotificationStats(): Map<String, Any> {
        return mapOf(
            "total_sent" to notificationHistory.size,
            "by_type" to notificationCounts.toMap(),
            "last_24h" to notificationHistory.count { 
                System.currentTimeMillis() - it.timestamp < 86400000 
            },
            "avg_per_day" to if (notificationHistory.isNotEmpty()) {
                val daysSinceFirst = (System.currentTimeMillis() - notificationHistory.first().timestamp) / 86400000.0
                notificationHistory.size / maxOf(daysSinceFirst, 1.0)
            } else 0.0
        )
    }

    /**
     * Clear all notifications
     */
    fun clearAllNotifications() {
        notificationManager.cancelAll()
        Log.d(TAG, "Cleared all notifications")
    }

    /**
     * Cancel specific notification
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        Log.d(TAG, "Cancelled notification: $notificationId")
    }

    // Callback setters
    fun setNotificationClickCallback(callback: (SmartNotification) -> Unit) {
        notificationClickCallback = callback
    }

    fun setNotificationDismissCallback(callback: (SmartNotification) -> Unit) {
        notificationDismissCallback = callback
    }

    /**
     * Shutdown notification manager
     */
    fun shutdown() {
        scope.cancel()
        clearAllNotifications()
        Log.d(TAG, "Smart notification manager shut down")
    }
}

/**
 * Broadcast receiver for notification actions
 */
class NotificationActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val actionId = intent.getStringExtra("action_id")
        val notificationId = intent.getIntExtra("notification_id", -1)
        
        Log.d("NotificationActionReceiver", "Action: $actionId, Notification: $notificationId")
        
        // Handle notification actions
        when (actionId) {
            "dismiss" -> {
                NotificationManagerCompat.from(context).cancel(notificationId)
            }
            // Add more action handlers as needed
        }
    }
}