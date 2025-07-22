package com.buccancs.gsrcapture.visualization

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.buccancs.gsrcapture.analytics.DataPoint
import com.buccancs.gsrcapture.analytics.DataType
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.*

/**
 * Custom view for real-time data visualization
 * Supports multiple data streams with different colors and scales
 */
class RealTimeDataVisualizer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "RealTimeDataVisualizer"
        private const val MAX_DATA_POINTS = 500
        private const val GRID_LINES = 10
        private const val MARGIN = 50f
        private const val LEGEND_HEIGHT = 100f
    }

    // Data storage for different streams
    private val dataStreams = mutableMapOf<DataType, ConcurrentLinkedQueue<DataPoint>>()
    private val streamColors = mutableMapOf<DataType, Int>()
    private val streamScales = mutableMapOf<DataType, Pair<Float, Float>>() // min, max

    // Visualization settings
    private var timeWindow: Long = 30000 // 30 seconds
    private var showGrid = true
    private var showLegend = true
    private var autoScale = true
    private var smoothLines = true

    // Paint objects
    private val linePaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val gridPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
        alpha = 100
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        isAntiAlias = true
    }

    private val backgroundPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Default colors for different data types
    private val defaultColors = mapOf(
        DataType.GSR to Color.BLUE,
        DataType.HEART_RATE to Color.RED,
        DataType.TEMPERATURE to Color.GREEN,
        DataType.MOTION to Color.MAGENTA,
        DataType.BATTERY_LEVEL to Color.YELLOW,
        DataType.FRAME_RATE to Color.CYAN,
        DataType.SAMPLE_RATE to Color.parseColor("#FF6600")
    )

    init {
        // Initialize default colors
        defaultColors.forEach { (dataType, color) ->
            streamColors[dataType] = color
        }
    }

    /**
     * Add a data point to the visualization
     */
    fun addDataPoint(dataPoint: DataPoint) {
        val queue = dataStreams.getOrPut(dataPoint.type) { ConcurrentLinkedQueue() }

        queue.offer(dataPoint)

        // Remove old data points
        val cutoffTime = System.currentTimeMillis() - timeWindow
        while (queue.isNotEmpty() && queue.peek().timestamp < cutoffTime) {
            queue.poll()
        }

        // Limit queue size
        while (queue.size > MAX_DATA_POINTS) {
            queue.poll()
        }

        // Update scale if auto-scaling is enabled
        if (autoScale) {
            updateScale(dataPoint.type)
        }

        // Trigger redraw
        post { invalidate() }
    }

    /**
     * Add multiple data points at once
     */
    fun addDataPoints(dataPoints: List<DataPoint>) {
        dataPoints.forEach { addDataPoint(it) }
    }

    /**
     * Clear all data for a specific stream
     */
    fun clearStream(dataType: DataType) {
        dataStreams[dataType]?.clear()
        invalidate()
    }

    /**
     * Clear all data streams
     */
    fun clearAllStreams() {
        dataStreams.values.forEach { it.clear() }
        invalidate()
    }

    /**
     * Set custom color for a data stream
     */
    fun setStreamColor(dataType: DataType, color: Int) {
        streamColors[dataType] = color
        invalidate()
    }

    /**
     * Set custom scale for a data stream
     */
    fun setStreamScale(dataType: DataType, minValue: Float, maxValue: Float) {
        streamScales[dataType] = minValue to maxValue
        invalidate()
    }

    /**
     * Configure visualization settings
     */
    fun configure(
        timeWindowMs: Long = this.timeWindow,
        showGrid: Boolean = this.showGrid,
        showLegend: Boolean = this.showLegend,
        autoScale: Boolean = this.autoScale,
        smoothLines: Boolean = this.smoothLines
    ) {
        this.timeWindow = timeWindowMs
        this.showGrid = showGrid
        this.showLegend = showLegend
        this.autoScale = autoScale
        this.smoothLines = smoothLines
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        val drawableWidth = width - 2 * MARGIN
        val drawableHeight = height - 2 * MARGIN - (if (showLegend) LEGEND_HEIGHT else 0f)

        if (drawableWidth <= 0 || drawableHeight <= 0) return

        // Draw grid
        if (showGrid) {
            drawGrid(canvas, drawableWidth, drawableHeight)
        }

        // Draw data streams
        dataStreams.forEach { (dataType, queue) ->
            if (queue.isNotEmpty()) {
                drawDataStream(canvas, dataType, queue.toList(), drawableWidth, drawableHeight)
            }
        }

        // Draw legend
        if (showLegend) {
            drawLegend(canvas, drawableWidth, drawableHeight)
        }

        // Draw axes labels
        drawAxesLabels(canvas, drawableWidth, drawableHeight)
    }

    private fun drawGrid(canvas: Canvas, drawableWidth: Float, drawableHeight: Float) {
        // Vertical grid lines (time)
        for (i in 0..GRID_LINES) {
            val x = MARGIN + (i * drawableWidth / GRID_LINES)
            canvas.drawLine(x, MARGIN, x, MARGIN + drawableHeight, gridPaint)
        }

        // Horizontal grid lines (values)
        for (i in 0..GRID_LINES) {
            val y = MARGIN + (i * drawableHeight / GRID_LINES)
            canvas.drawLine(MARGIN, y, MARGIN + drawableWidth, y, gridPaint)
        }
    }

    private fun drawDataStream(
        canvas: Canvas,
        dataType: DataType,
        dataPoints: List<DataPoint>,
        drawableWidth: Float,
        drawableHeight: Float
    ) {
        if (dataPoints.size < 2) return

        val color = streamColors[dataType] ?: Color.BLACK
        linePaint.color = color

        val scale = streamScales[dataType]
        val minValue = scale?.first ?: dataPoints.minOfOrNull { it.value.toFloat() } ?: 0f
        val maxValue = scale?.second ?: dataPoints.maxOfOrNull { it.value.toFloat() } ?: 1f
        val valueRange = maxValue - minValue

        if (valueRange == 0f) return

        val currentTime = System.currentTimeMillis()
        val timeRange = timeWindow.toFloat()

        val path = Path()
        var isFirstPoint = true
        var prevX = 0f
        var prevY = 0f

        for (dataPoint in dataPoints) {
            val x = MARGIN + drawableWidth * (1f - (currentTime - dataPoint.timestamp) / timeRange)
            val y = MARGIN + drawableHeight * (1f - (dataPoint.value.toFloat() - minValue) / valueRange)

            if (x >= MARGIN && x <= MARGIN + drawableWidth) {
                if (isFirstPoint) {
                    path.moveTo(x, y)
                    isFirstPoint = false
                } else {
                    if (smoothLines) {
                        // Use quadratic bezier curves for smooth lines
                        val controlX = (prevX + x) / 2
                        path.quadTo(controlX, prevY, x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                prevX = x
                prevY = y
            }
        }

        canvas.drawPath(path, linePaint)

        // Draw data points as small circles
        linePaint.style = Paint.Style.FILL
        for (dataPoint in dataPoints) {
            val x = MARGIN + drawableWidth * (1f - (currentTime - dataPoint.timestamp) / timeRange)
            val y = MARGIN + drawableHeight * (1f - (dataPoint.value.toFloat() - minValue) / valueRange)

            if (x >= MARGIN && x <= MARGIN + drawableWidth) {
                canvas.drawCircle(x, y, 4f, linePaint)
            }
        }
        linePaint.style = Paint.Style.STROKE
    }

    private fun drawLegend(canvas: Canvas, drawableWidth: Float, drawableHeight: Float) {
        val legendY = MARGIN + drawableHeight + 20f
        var legendX = MARGIN

        dataStreams.keys.forEach { dataType ->
            val color = streamColors[dataType] ?: Color.BLACK

            // Draw color indicator
            val colorPaint = Paint().apply {
                this.color = color
                style = Paint.Style.FILL
            }
            canvas.drawRect(legendX, legendY, legendX + 20f, legendY + 20f, colorPaint)

            // Draw label
            val label = "${dataType.displayName} (${dataType.unit})"
            canvas.drawText(label, legendX + 30f, legendY + 15f, textPaint)

            legendX += textPaint.measureText(label) + 60f

            // Wrap to next line if needed
            if (legendX > MARGIN + drawableWidth - 100f) {
                legendX = MARGIN
                // Note: This simple implementation doesn't handle multi-line legends
            }
        }
    }

    private fun drawAxesLabels(canvas: Canvas, drawableWidth: Float, drawableHeight: Float) {
        // Time axis labels
        val currentTime = System.currentTimeMillis()
        for (i in 0..4) {
            val x = MARGIN + (i * drawableWidth / 4)
            val timeOffset = (4 - i) * timeWindow / 4
            val timeLabel = "${timeOffset / 1000}s"
            canvas.drawText(timeLabel, x - textPaint.measureText(timeLabel) / 2, 
                          MARGIN + drawableHeight + 40f, textPaint)
        }

        // Value axis labels (show for primary stream if available)
        val primaryStream = dataStreams.keys.firstOrNull()
        if (primaryStream != null) {
            val scale = streamScales[primaryStream]
            val dataPoints = dataStreams[primaryStream]?.toList() ?: emptyList()

            if (dataPoints.isNotEmpty()) {
                val minValue = scale?.first ?: dataPoints.minOfOrNull { it.value.toFloat() } ?: 0f
                val maxValue = scale?.second ?: dataPoints.maxOfOrNull { it.value.toFloat() } ?: 1f

                for (i in 0..4) {
                    val y = MARGIN + (i * drawableHeight / 4)
                    val value = maxValue - (i * (maxValue - minValue) / 4)
                    val valueLabel = String.format("%.1f", value)
                    canvas.drawText(valueLabel, 10f, y + textPaint.textSize / 2, textPaint)
                }
            }
        }
    }

    private fun updateScale(dataType: DataType) {
        val queue = dataStreams[dataType] ?: return
        val dataPoints = queue.toList()

        if (dataPoints.isEmpty()) return

        val minValue = dataPoints.minOfOrNull { it.value.toFloat() } ?: 0f
        val maxValue = dataPoints.maxOfOrNull { it.value.toFloat() } ?: 1f

        // Add some padding to the scale
        val padding = (maxValue - minValue) * 0.1f
        streamScales[dataType] = (minValue - padding) to (maxValue + padding)
    }

    /**
     * Get statistics for a data stream
     */
    fun getStreamStatistics(dataType: DataType): StreamStatistics? {
        val queue = dataStreams[dataType] ?: return null
        val dataPoints = queue.toList()

        if (dataPoints.isEmpty()) return null

        val values = dataPoints.map { it.value }
        val mean = values.average()
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        val variance = values.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)

        return StreamStatistics(
            dataType = dataType,
            sampleCount = dataPoints.size,
            mean = mean,
            min = min,
            max = max,
            standardDeviation = stdDev,
            latest = dataPoints.lastOrNull()?.value ?: 0.0
        )
    }

    /**
     * Data class for stream statistics
     */
    data class StreamStatistics(
        val dataType: DataType,
        val sampleCount: Int,
        val mean: Double,
        val min: Double,
        val max: Double,
        val standardDeviation: Double,
        val latest: Double
    )
}
