package com.buccancs.gsrcapture.ml

import android.content.Context
import android.util.Log
import com.buccancs.gsrcapture.analytics.DataPoint
import com.buccancs.gsrcapture.analytics.DataType
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

/**
 * Data class representing a detected pattern
 */
data class DetectedPattern(
    val id: String,
    val type: PatternType,
    val dataType: DataType,
    val startTime: Long,
    val endTime: Long,
    val confidence: Double,
    val characteristics: Map<String, Double>,
    val description: String
)

/**
 * Enum representing different types of patterns
 */
enum class PatternType(val displayName: String) {
    TREND_INCREASING("Increasing Trend"),
    TREND_DECREASING("Decreasing Trend"),
    PERIODIC("Periodic Pattern"),
    SPIKE("Spike"),
    DIP("Dip"),
    PLATEAU("Plateau"),
    OSCILLATION("Oscillation"),
    ANOMALY("Anomaly"),
    STRESS_RESPONSE("Stress Response"),
    RELAXATION_RESPONSE("Relaxation Response")
}

/**
 * Configuration for pattern recognition
 */
data class PatternRecognitionConfig(
    val windowSize: Int = 50,
    val minPatternLength: Int = 5,
    val confidenceThreshold: Double = 0.7,
    val enableTrendDetection: Boolean = true,
    val enablePeriodicDetection: Boolean = true,
    val enableAnomalyDetection: Boolean = true,
    val enableStressDetection: Boolean = true,
    val trendSensitivity: Double = 0.1,
    val periodicSensitivity: Double = 0.8,
    val anomalyThreshold: Double = 2.5,
    val stressThreshold: Double = 1.5
)

/**
 * Machine Learning-based Pattern Recognition Engine
 * Uses statistical analysis and signal processing techniques for pattern detection
 */
class PatternRecognitionEngine(
    private val context: Context,
    private val config: PatternRecognitionConfig = PatternRecognitionConfig()
) {
    companion object {
        private const val TAG = "PatternRecognitionEngine"
        private const val MAX_HISTORY_SIZE = 1000
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val dataHistory = mutableMapOf<DataType, ConcurrentLinkedQueue<DataPoint>>()
    private val detectedPatterns = mutableListOf<DetectedPattern>()
    
    // Callbacks
    private var patternDetectedCallback: ((DetectedPattern) -> Unit)? = null
    private var anomalyDetectedCallback: ((DataPoint, Double) -> Unit)? = null
    private var stressDetectedCallback: ((Double) -> Unit)? = null

    /**
     * Add data point for pattern analysis
     */
    fun addDataPoint(dataPoint: DataPoint) {
        val queue = dataHistory.getOrPut(dataPoint.type) { ConcurrentLinkedQueue() }
        queue.offer(dataPoint)
        
        // Limit history size
        while (queue.size > MAX_HISTORY_SIZE) {
            queue.poll()
        }
        
        // Trigger pattern analysis asynchronously
        scope.launch {
            analyzePatterns(dataPoint.type)
        }
    }

    /**
     * Analyze patterns for a specific data type
     */
    private suspend fun analyzePatterns(dataType: DataType) {
        val queue = dataHistory[dataType] ?: return
        val dataPoints = queue.toList().takeLast(config.windowSize)
        
        if (dataPoints.size < config.minPatternLength) return
        
        try {
            // Detect different types of patterns
            if (config.enableTrendDetection) {
                detectTrends(dataType, dataPoints)
            }
            
            if (config.enablePeriodicDetection) {
                detectPeriodicPatterns(dataType, dataPoints)
            }
            
            if (config.enableAnomalyDetection) {
                detectAnomalies(dataType, dataPoints)
            }
            
            if (config.enableStressDetection && dataType == DataType.GSR) {
                detectStressPatterns(dataPoints)
            }
            
            // Detect spikes and dips
            detectSpikesAndDips(dataType, dataPoints)
            
            // Detect plateaus
            detectPlateaus(dataType, dataPoints)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing patterns for $dataType", e)
        }
    }

    /**
     * Detect trend patterns (increasing/decreasing)
     */
    private suspend fun detectTrends(dataType: DataType, dataPoints: List<DataPoint>) {
        if (dataPoints.size < 10) return
        
        val values = dataPoints.map { it.value }
        val timestamps = dataPoints.map { it.timestamp.toDouble() }
        
        // Calculate linear regression
        val n = values.size
        val sumX = timestamps.sum()
        val sumY = values.sum()
        val sumXY = timestamps.zip(values) { x, y -> x * y }.sum()
        val sumX2 = timestamps.sumOf { it.pow(2) }
        
        val slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX.pow(2))
        val intercept = (sumY - slope * sumX) / n
        
        // Calculate correlation coefficient
        val meanX = sumX / n
        val meanY = sumY / n
        val numerator = timestamps.zip(values) { x, y -> (x - meanX) * (y - meanY) }.sum()
        val denomX = sqrt(timestamps.sumOf { (it - meanX).pow(2) })
        val denomY = sqrt(values.sumOf { (it - meanY).pow(2) })
        val correlation = if (denomX > 0 && denomY > 0) numerator / (denomX * denomY) else 0.0
        
        val confidence = abs(correlation)
        
        if (confidence > config.confidenceThreshold && abs(slope) > config.trendSensitivity) {
            val patternType = if (slope > 0) PatternType.TREND_INCREASING else PatternType.TREND_DECREASING
            val pattern = DetectedPattern(
                id = generatePatternId(),
                type = patternType,
                dataType = dataType,
                startTime = dataPoints.first().timestamp,
                endTime = dataPoints.last().timestamp,
                confidence = confidence,
                characteristics = mapOf(
                    "slope" to slope,
                    "correlation" to correlation,
                    "intercept" to intercept
                ),
                description = "${patternType.displayName} detected with ${String.format("%.1f", confidence * 100)}% confidence"
            )
            
            addDetectedPattern(pattern)
        }
    }

    /**
     * Detect periodic patterns using autocorrelation
     */
    private suspend fun detectPeriodicPatterns(dataType: DataType, dataPoints: List<DataPoint>) {
        if (dataPoints.size < 20) return
        
        val values = dataPoints.map { it.value }
        val autocorrelations = calculateAutocorrelation(values)
        
        // Find peaks in autocorrelation
        val peaks = findPeaks(autocorrelations)
        val significantPeaks = peaks.filter { autocorrelations[it] > config.periodicSensitivity }
        
        if (significantPeaks.isNotEmpty()) {
            val period = significantPeaks.first()
            val confidence = autocorrelations[period]
            
            val pattern = DetectedPattern(
                id = generatePatternId(),
                type = PatternType.PERIODIC,
                dataType = dataType,
                startTime = dataPoints.first().timestamp,
                endTime = dataPoints.last().timestamp,
                confidence = confidence,
                characteristics = mapOf(
                    "period" to period.toDouble(),
                    "max_autocorr" to confidence
                ),
                description = "Periodic pattern detected with period ${period} samples"
            )
            
            addDetectedPattern(pattern)
        }
    }

    /**
     * Detect anomalies using statistical methods
     */
    private suspend fun detectAnomalies(dataType: DataType, dataPoints: List<DataPoint>) {
        if (dataPoints.size < 10) return
        
        val values = dataPoints.map { it.value }
        val mean = values.average()
        val stdDev = sqrt(values.map { (it - mean).pow(2) }.average())
        
        val threshold = config.anomalyThreshold * stdDev
        
        for (dataPoint in dataPoints.takeLast(5)) { // Check recent points
            val deviation = abs(dataPoint.value - mean)
            if (deviation > threshold) {
                val anomalyScore = deviation / stdDev
                
                // Notify anomaly callback
                anomalyDetectedCallback?.let { callback ->
                    withContext(Dispatchers.Main) {
                        callback(dataPoint, anomalyScore)
                    }
                }
                
                val pattern = DetectedPattern(
                    id = generatePatternId(),
                    type = PatternType.ANOMALY,
                    dataType = dataType,
                    startTime = dataPoint.timestamp,
                    endTime = dataPoint.timestamp,
                    confidence = min(anomalyScore / config.anomalyThreshold, 1.0),
                    characteristics = mapOf(
                        "deviation" to deviation,
                        "anomaly_score" to anomalyScore,
                        "threshold" to threshold
                    ),
                    description = "Anomaly detected: ${String.format("%.2f", anomalyScore)} standard deviations from mean"
                )
                
                addDetectedPattern(pattern)
            }
        }
    }

    /**
     * Detect stress patterns in GSR data
     */
    private suspend fun detectStressPatterns(dataPoints: List<DataPoint>) {
        if (dataPoints.size < 15) return
        
        val values = dataPoints.map { it.value }
        val recentValues = values.takeLast(10)
        val baselineValues = values.take(values.size - 10)
        
        if (baselineValues.isEmpty()) return
        
        val recentMean = recentValues.average()
        val baselineMean = baselineValues.average()
        val baselineStdDev = sqrt(baselineValues.map { (it - baselineMean).pow(2) }.average())
        
        val stressScore = (recentMean - baselineMean) / baselineStdDev
        
        if (stressScore > config.stressThreshold) {
            // Stress response detected
            stressDetectedCallback?.let { callback ->
                withContext(Dispatchers.Main) {
                    callback(stressScore)
                }
            }
            
            val pattern = DetectedPattern(
                id = generatePatternId(),
                type = PatternType.STRESS_RESPONSE,
                dataType = DataType.GSR,
                startTime = dataPoints[dataPoints.size - 10].timestamp,
                endTime = dataPoints.last().timestamp,
                confidence = min(stressScore / config.stressThreshold, 1.0),
                characteristics = mapOf(
                    "stress_score" to stressScore,
                    "recent_mean" to recentMean,
                    "baseline_mean" to baselineMean
                ),
                description = "Stress response detected with score ${String.format("%.2f", stressScore)}"
            )
            
            addDetectedPattern(pattern)
        } else if (stressScore < -config.stressThreshold) {
            // Relaxation response detected
            val pattern = DetectedPattern(
                id = generatePatternId(),
                type = PatternType.RELAXATION_RESPONSE,
                dataType = DataType.GSR,
                startTime = dataPoints[dataPoints.size - 10].timestamp,
                endTime = dataPoints.last().timestamp,
                confidence = min(abs(stressScore) / config.stressThreshold, 1.0),
                characteristics = mapOf(
                    "relaxation_score" to abs(stressScore),
                    "recent_mean" to recentMean,
                    "baseline_mean" to baselineMean
                ),
                description = "Relaxation response detected with score ${String.format("%.2f", abs(stressScore))}"
            )
            
            addDetectedPattern(pattern)
        }
    }

    /**
     * Detect spikes and dips
     */
    private suspend fun detectSpikesAndDips(dataType: DataType, dataPoints: List<DataPoint>) {
        if (dataPoints.size < 7) return
        
        val values = dataPoints.map { it.value }
        val smoothed = applyMovingAverage(values, 3)
        
        for (i in 3 until smoothed.size - 3) {
            val current = smoothed[i]
            val before = smoothed.subList(i - 3, i).average()
            val after = smoothed.subList(i + 1, i + 4).average()
            
            val spikeThreshold = (before + after) / 2 + 2 * calculateLocalStdDev(values, i, 3)
            val dipThreshold = (before + after) / 2 - 2 * calculateLocalStdDev(values, i, 3)
            
            if (current > spikeThreshold) {
                val pattern = DetectedPattern(
                    id = generatePatternId(),
                    type = PatternType.SPIKE,
                    dataType = dataType,
                    startTime = dataPoints[i].timestamp,
                    endTime = dataPoints[i].timestamp,
                    confidence = min((current - spikeThreshold) / spikeThreshold, 1.0),
                    characteristics = mapOf(
                        "peak_value" to current,
                        "baseline" to (before + after) / 2,
                        "magnitude" to (current - (before + after) / 2)
                    ),
                    description = "Spike detected with magnitude ${String.format("%.2f", current - (before + after) / 2)}"
                )
                
                addDetectedPattern(pattern)
            } else if (current < dipThreshold) {
                val pattern = DetectedPattern(
                    id = generatePatternId(),
                    type = PatternType.DIP,
                    dataType = dataType,
                    startTime = dataPoints[i].timestamp,
                    endTime = dataPoints[i].timestamp,
                    confidence = min((dipThreshold - current) / abs(dipThreshold), 1.0),
                    characteristics = mapOf(
                        "dip_value" to current,
                        "baseline" to (before + after) / 2,
                        "magnitude" to ((before + after) / 2 - current)
                    ),
                    description = "Dip detected with magnitude ${String.format("%.2f", (before + after) / 2 - current)}"
                )
                
                addDetectedPattern(pattern)
            }
        }
    }

    /**
     * Detect plateau patterns
     */
    private suspend fun detectPlateaus(dataType: DataType, dataPoints: List<DataPoint>) {
        if (dataPoints.size < 10) return
        
        val values = dataPoints.map { it.value }
        val windowSize = 5
        
        for (i in 0..values.size - windowSize) {
            val window = values.subList(i, i + windowSize)
            val variance = calculateVariance(window)
            val mean = window.average()
            
            // Low variance indicates a plateau
            if (variance < 0.01 * mean.pow(2)) { // 1% of mean squared
                val pattern = DetectedPattern(
                    id = generatePatternId(),
                    type = PatternType.PLATEAU,
                    dataType = dataType,
                    startTime = dataPoints[i].timestamp,
                    endTime = dataPoints[i + windowSize - 1].timestamp,
                    confidence = 1.0 - (variance / (0.01 * mean.pow(2))),
                    characteristics = mapOf(
                        "plateau_value" to mean,
                        "variance" to variance,
                        "duration" to (dataPoints[i + windowSize - 1].timestamp - dataPoints[i].timestamp).toDouble()
                    ),
                    description = "Plateau detected at value ${String.format("%.2f", mean)}"
                )
                
                addDetectedPattern(pattern)
            }
        }
    }

    // Helper functions
    private fun calculateAutocorrelation(values: List<Double>): List<Double> {
        val n = values.size
        val mean = values.average()
        val variance = values.map { (it - mean).pow(2) }.sum()
        
        return (0 until n / 2).map { lag ->
            val covariance = (0 until n - lag).map { i ->
                (values[i] - mean) * (values[i + lag] - mean)
            }.sum()
            covariance / variance
        }
    }

    private fun findPeaks(values: List<Double>): List<Int> {
        val peaks = mutableListOf<Int>()
        for (i in 1 until values.size - 1) {
            if (values[i] > values[i - 1] && values[i] > values[i + 1]) {
                peaks.add(i)
            }
        }
        return peaks
    }

    private fun applyMovingAverage(values: List<Double>, windowSize: Int): List<Double> {
        return values.windowed(windowSize) { it.average() }
    }

    private fun calculateLocalStdDev(values: List<Double>, center: Int, radius: Int): Double {
        val start = maxOf(0, center - radius)
        val end = minOf(values.size, center + radius + 1)
        val window = values.subList(start, end)
        val mean = window.average()
        return sqrt(window.map { (it - mean).pow(2) }.average())
    }

    private fun calculateVariance(values: List<Double>): Double {
        val mean = values.average()
        return values.map { (it - mean).pow(2) }.average()
    }

    private fun generatePatternId(): String {
        return "pattern_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}"
    }

    private suspend fun addDetectedPattern(pattern: DetectedPattern) {
        synchronized(detectedPatterns) {
            detectedPatterns.add(pattern)
            
            // Limit pattern history
            while (detectedPatterns.size > 100) {
                detectedPatterns.removeAt(0)
            }
        }
        
        // Notify callback on main thread
        patternDetectedCallback?.let { callback ->
            withContext(Dispatchers.Main) {
                callback(pattern)
            }
        }
    }

    /**
     * Get all detected patterns
     */
    fun getDetectedPatterns(): List<DetectedPattern> {
        return synchronized(detectedPatterns) {
            detectedPatterns.toList()
        }
    }

    /**
     * Get patterns for a specific data type
     */
    fun getPatternsForDataType(dataType: DataType): List<DetectedPattern> {
        return synchronized(detectedPatterns) {
            detectedPatterns.filter { it.dataType == dataType }
        }
    }

    /**
     * Get patterns within a time range
     */
    fun getPatternsInTimeRange(startTime: Long, endTime: Long): List<DetectedPattern> {
        return synchronized(detectedPatterns) {
            detectedPatterns.filter { 
                it.startTime >= startTime && it.endTime <= endTime 
            }
        }
    }

    /**
     * Clear pattern history
     */
    fun clearPatterns() {
        synchronized(detectedPatterns) {
            detectedPatterns.clear()
        }
    }

    // Callback setters
    fun setPatternDetectedCallback(callback: (DetectedPattern) -> Unit) {
        patternDetectedCallback = callback
    }

    fun setAnomalyDetectedCallback(callback: (DataPoint, Double) -> Unit) {
        anomalyDetectedCallback = callback
    }

    fun setStressDetectedCallback(callback: (Double) -> Unit) {
        stressDetectedCallback = callback
    }

    /**
     * Shutdown the pattern recognition engine
     */
    fun shutdown() {
        scope.cancel()
        dataHistory.clear()
        detectedPatterns.clear()
    }
}