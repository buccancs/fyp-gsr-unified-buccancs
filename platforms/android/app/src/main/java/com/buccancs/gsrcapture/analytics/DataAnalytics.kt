package com.buccancs.gsrcapture.analytics

import android.content.Context
import android.util.Log
import com.buccancs.gsrcapture.recording.RecordingStats
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

/**
 * Data class representing a data point for analytics
 */
data class DataPoint(
    val timestamp: Long,
    val value: Double,
    val type: DataType,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Enum representing different types of data
 */
enum class DataType(val displayName: String, val unit: String) {
    GSR("GSR", "μS"),
    HEART_RATE("Heart Rate", "BPM"),
    TEMPERATURE("Temperature", "°C"),
    MOTION("Motion", "m/s²"),
    BATTERY_LEVEL("Battery", "%"),
    STORAGE_USAGE("Storage", "MB"),
    FRAME_RATE("Frame Rate", "FPS"),
    SAMPLE_RATE("Sample Rate", "Hz")
}

/**
 * Data class representing statistical analysis results
 */
data class AnalyticsResult(
    val dataType: DataType,
    val timeRange: Pair<Long, Long>,
    val sampleCount: Int,
    val mean: Double,
    val median: Double,
    val standardDeviation: Double,
    val minimum: Double,
    val maximum: Double,
    val range: Double,
    val variance: Double,
    val skewness: Double,
    val kurtosis: Double,
    val trend: TrendDirection,
    val anomalies: List<DataPoint>,
    val insights: List<String>
)

/**
 * Enum representing trend directions
 */
enum class TrendDirection {
    INCREASING, DECREASING, STABLE, VOLATILE
}

/**
 * Data class representing real-time analytics configuration
 */
data class AnalyticsConfig(
    val windowSize: Int = 100, // Number of data points to analyze
    val updateInterval: Long = 1000, // Update interval in milliseconds
    val anomalyThreshold: Double = 2.0, // Standard deviations for anomaly detection
    val enableTrendAnalysis: Boolean = true,
    val enableAnomalyDetection: Boolean = true,
    val enableInsightGeneration: Boolean = true
)

/**
 * Main analytics engine for processing and analyzing sensor data
 */
class DataAnalytics(
    private val context: Context,
    private val config: AnalyticsConfig = AnalyticsConfig()
) {
    companion object {
        private const val TAG = "DataAnalytics"
        private const val MAX_DATA_POINTS = 10000 // Maximum data points to keep in memory
    }

    // Data storage for different types
    private val dataBuffers = mutableMapOf<DataType, ConcurrentLinkedQueue<DataPoint>>()

    // Callbacks
    private var analyticsUpdateCallback: ((DataType, AnalyticsResult) -> Unit)? = null
    private var anomalyDetectedCallback: ((DataPoint) -> Unit)? = null
    private var insightGeneratedCallback: ((String) -> Unit)? = null

    init {
        // Initialize data buffers for each data type
        DataType.values().forEach { dataType ->
            dataBuffers[dataType] = ConcurrentLinkedQueue()
        }
    }

    /**
     * Add a new data point for analysis
     */
    fun addDataPoint(dataPoint: DataPoint) {
        val buffer = dataBuffers[dataPoint.type] ?: return

        // Add new data point
        buffer.offer(dataPoint)

        // Remove old data points if buffer is too large
        while (buffer.size > MAX_DATA_POINTS) {
            buffer.poll()
        }

        // Trigger analysis if we have enough data points
        if (buffer.size >= config.windowSize) {
            analyzeData(dataPoint.type)
        }
    }

    /**
     * Add multiple data points at once
     */
    fun addDataPoints(dataPoints: List<DataPoint>) {
        dataPoints.forEach { addDataPoint(it) }
    }

    /**
     * Perform statistical analysis on the data
     */
    private fun analyzeData(dataType: DataType) {
        val buffer = dataBuffers[dataType] ?: return
        val dataPoints = buffer.toList().takeLast(config.windowSize)

        if (dataPoints.isEmpty()) return

        try {
            val values = dataPoints.map { it.value }
            val timestamps = dataPoints.map { it.timestamp }

            // Calculate basic statistics
            val mean = values.average()
            val median = calculateMedian(values)
            val variance = calculateVariance(values, mean)
            val standardDeviation = sqrt(variance)
            val minimum = values.minOrNull() ?: 0.0
            val maximum = values.maxOrNull() ?: 0.0
            val range = maximum - minimum
            val skewness = calculateSkewness(values, mean, standardDeviation)
            val kurtosis = calculateKurtosis(values, mean, standardDeviation)

            // Analyze trend
            val trend = if (config.enableTrendAnalysis) {
                analyzeTrend(values, timestamps)
            } else {
                TrendDirection.STABLE
            }

            // Detect anomalies
            val anomalies = if (config.enableAnomalyDetection) {
                detectAnomalies(dataPoints, mean, standardDeviation)
            } else {
                emptyList()
            }

            // Generate insights
            val insights = if (config.enableInsightGeneration) {
                generateInsights(dataType, mean, standardDeviation, trend, anomalies)
            } else {
                emptyList()
            }

            val result = AnalyticsResult(
                dataType = dataType,
                timeRange = timestamps.minOrNull()!! to timestamps.maxOrNull()!!,
                sampleCount = dataPoints.size,
                mean = mean,
                median = median,
                standardDeviation = standardDeviation,
                minimum = minimum,
                maximum = maximum,
                range = range,
                variance = variance,
                skewness = skewness,
                kurtosis = kurtosis,
                trend = trend,
                anomalies = anomalies,
                insights = insights
            )

            // Notify callbacks
            analyticsUpdateCallback?.invoke(dataType, result)

            // Notify about anomalies
            anomalies.forEach { anomaly ->
                anomalyDetectedCallback?.invoke(anomaly)
            }

            // Notify about insights
            insights.forEach { insight ->
                insightGeneratedCallback?.invoke(insight)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing data for $dataType", e)
        }
    }

    private fun calculateMedian(values: List<Double>): Double {
        val sorted = values.sorted()
        val size = sorted.size
        return if (size % 2 == 0) {
            (sorted[size / 2 - 1] + sorted[size / 2]) / 2.0
        } else {
            sorted[size / 2]
        }
    }

    private fun calculateVariance(values: List<Double>, mean: Double): Double {
        return values.map { (it - mean).pow(2) }.average()
    }

    private fun calculateSkewness(values: List<Double>, mean: Double, standardDeviation: Double): Double {
        if (standardDeviation == 0.0) return 0.0
        val n = values.size
        val sum = values.sumOf { ((it - mean) / standardDeviation).pow(3) }
        return (n.toDouble() / ((n - 1) * (n - 2))) * sum
    }

    private fun calculateKurtosis(values: List<Double>, mean: Double, standardDeviation: Double): Double {
        if (standardDeviation == 0.0) return 0.0
        val n = values.size
        val sum = values.sumOf { ((it - mean) / standardDeviation).pow(4) }
        return ((n * (n + 1).toDouble()) / ((n - 1) * (n - 2) * (n - 3))) * sum - 
               (3 * (n - 1).toDouble().pow(2)) / ((n - 2) * (n - 3))
    }

    private fun analyzeTrend(values: List<Double>, timestamps: List<Long>): TrendDirection {
        if (values.size < 2) return TrendDirection.STABLE

        // Calculate linear regression slope
        val n = values.size
        val sumX = timestamps.sum().toDouble()
        val sumY = values.sum()
        val sumXY = timestamps.zip(values) { x, y -> x * y }.sum()
        val sumX2 = timestamps.sumOf { it.toDouble().pow(2) }

        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX.pow(2))

        // Calculate coefficient of variation to determine volatility
        val mean = values.average()
        val standardDeviation = sqrt(calculateVariance(values, mean))
        val coefficientOfVariation = if (mean != 0.0) standardDeviation / abs(mean) else 0.0

        return when {
            coefficientOfVariation > 0.3 -> TrendDirection.VOLATILE
            slope > 0.001 -> TrendDirection.INCREASING
            slope < -0.001 -> TrendDirection.DECREASING
            else -> TrendDirection.STABLE
        }
    }

    private fun detectAnomalies(dataPoints: List<DataPoint>, mean: Double, standardDeviation: Double): List<DataPoint> {
        val threshold = config.anomalyThreshold * standardDeviation
        return dataPoints.filter { abs(it.value - mean) > threshold }
    }

    private fun generateInsights(
        dataType: DataType,
        mean: Double,
        standardDeviation: Double,
        trend: TrendDirection,
        anomalies: List<DataPoint>
    ): List<String> {
        val insights = mutableListOf<String>()

        // Trend insights
        when (trend) {
            TrendDirection.INCREASING -> insights.add("${dataType.displayName} is showing an increasing trend")
            TrendDirection.DECREASING -> insights.add("${dataType.displayName} is showing a decreasing trend")
            TrendDirection.VOLATILE -> insights.add("${dataType.displayName} is highly volatile")
            TrendDirection.STABLE -> insights.add("${dataType.displayName} is stable")
        }

        // Anomaly insights
        if (anomalies.isNotEmpty()) {
            insights.add("Detected ${anomalies.size} anomalies in ${dataType.displayName}")
        }

        // Data type specific insights
        when (dataType) {
            DataType.GSR -> {
                when {
                    mean > 10.0 -> insights.add("High stress levels detected")
                    mean < 2.0 -> insights.add("Very low GSR readings - check sensor connection")
                    standardDeviation > 5.0 -> insights.add("High GSR variability indicates emotional responses")
                }
            }
            DataType.HEART_RATE -> {
                when {
                    mean > 100.0 -> insights.add("Elevated heart rate detected")
                    mean < 60.0 -> insights.add("Low heart rate detected")
                    standardDeviation > 20.0 -> insights.add("High heart rate variability")
                }
            }
            DataType.BATTERY_LEVEL -> {
                when {
                    mean < 20.0 -> insights.add("Low battery level - consider charging")
                    trend == TrendDirection.DECREASING -> insights.add("Battery draining rapidly")
                }
            }
            DataType.FRAME_RATE -> {
                when {
                    mean < 20.0 -> insights.add("Low frame rate detected - performance issues")
                    standardDeviation > 10.0 -> insights.add("Unstable frame rate")
                }
            }
            else -> {
                // Generic insights for other data types
                if (standardDeviation > mean * 0.5) {
                    insights.add("High variability in ${dataType.displayName}")
                }
            }
        }

        return insights
    }

    /**
     * Get current analytics for a specific data type
     */
    fun getCurrentAnalytics(dataType: DataType): AnalyticsResult? {
        val buffer = dataBuffers[dataType] ?: return null
        val dataPoints = buffer.toList().takeLast(config.windowSize)

        if (dataPoints.size < 2) return null

        val values = dataPoints.map { it.value }
        val timestamps = dataPoints.map { it.timestamp }
        val mean = values.average()
        val variance = calculateVariance(values, mean)
        val standardDeviation = sqrt(variance)

        return AnalyticsResult(
            dataType = dataType,
            timeRange = timestamps.minOrNull()!! to timestamps.maxOrNull()!!,
            sampleCount = dataPoints.size,
            mean = mean,
            median = calculateMedian(values),
            standardDeviation = standardDeviation,
            minimum = values.minOrNull() ?: 0.0,
            maximum = values.maxOrNull() ?: 0.0,
            range = (values.maxOrNull() ?: 0.0) - (values.minOrNull() ?: 0.0),
            variance = variance,
            skewness = calculateSkewness(values, mean, standardDeviation),
            kurtosis = calculateKurtosis(values, mean, standardDeviation),
            trend = analyzeTrend(values, timestamps),
            anomalies = detectAnomalies(dataPoints, mean, standardDeviation),
            insights = generateInsights(dataType, mean, standardDeviation, analyzeTrend(values, timestamps), detectAnomalies(dataPoints, mean, standardDeviation))
        )
    }

    /**
     * Get data points for a specific data type and time range
     */
    fun getDataPoints(dataType: DataType, startTime: Long, endTime: Long): List<DataPoint> {
        val buffer = dataBuffers[dataType] ?: return emptyList()
        return buffer.filter { it.timestamp in startTime..endTime }
    }

    /**
     * Clear all data for a specific data type
     */
    fun clearData(dataType: DataType) {
        dataBuffers[dataType]?.clear()
    }

    /**
     * Clear all data
     */
    fun clearAllData() {
        dataBuffers.values.forEach { it.clear() }
    }

    // Callback setters
    fun setAnalyticsUpdateCallback(callback: (DataType, AnalyticsResult) -> Unit) {
        analyticsUpdateCallback = callback
    }

    fun setAnomalyDetectedCallback(callback: (DataPoint) -> Unit) {
        anomalyDetectedCallback = callback
    }

    fun setInsightGeneratedCallback(callback: (String) -> Unit) {
        insightGeneratedCallback = callback
    }

    /**
     * Generate a summary report for recording statistics
     */
    fun generateRecordingReport(stats: RecordingStats): String {
        val duration = stats.duration / 1000.0 // Convert to seconds
        val durationMinutes = duration / 60.0

        return buildString {
            appendLine("=== Recording Session Report ===")
            appendLine("Session ID: ${stats.sessionId}")
            appendLine("Recording Mode: ${stats.mode.displayName}")
            appendLine("Duration: ${String.format("%.1f", durationMinutes)} minutes")
            appendLine("Frames Captured: ${stats.framesCaptured}")
            appendLine("Samples Captured: ${stats.samplesCaptured}")
            appendLine("Average Frame Rate: ${String.format("%.1f", stats.averageFrameRate)} FPS")
            appendLine("Average Sample Rate: ${String.format("%.1f", stats.averageSampleRate)} Hz")
            appendLine("File Size: ${stats.fileSize / (1024 * 1024)} MB")
            appendLine("Storage Used: ${stats.storageUsed / (1024 * 1024)} MB")
            appendLine("Battery Used: ${String.format("%.1f", stats.batteryUsed)}%")

            if (stats.errors.isNotEmpty()) {
                appendLine("\nErrors:")
                stats.errors.forEach { error ->
                    appendLine("- $error")
                }
            }

            if (stats.warnings.isNotEmpty()) {
                appendLine("\nWarnings:")
                stats.warnings.forEach { warning ->
                    appendLine("- $warning")
                }
            }
        }
    }
}
