package com.buccancs.gsrcapture.export

import com.buccancs.gsrcapture.export.interfaces.DataExporter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/**
 * CSV exporter implementation for data export
 */
class CsvExporter : DataExporter {
    
    companion object {
        private const val TAG = "CsvExporter"
        private const val CSV_SEPARATOR = ","
        private const val CSV_QUOTE = "\""
        private const val CSV_NEWLINE = "\n"
    }

    override fun getFormatName(): String = "CSV"

    override fun getFileExtension(): String = ".csv"

    override fun supportsDataType(dataType: String): Boolean {
        return when (dataType.lowercase()) {
            "timeseries", "sensor", "analytics", "metadata" -> true
            else -> false
        }
    }

    override fun getAvailableOptions(): Map<String, Map<String, Any>> {
        return mapOf(
            "separator" to mapOf(
                "description" to "CSV field separator",
                "type" to "string",
                "default" to ",",
                "options" to listOf(",", ";", "\t", "|")
            ),
            "includeHeaders" to mapOf(
                "description" to "Include column headers",
                "type" to "boolean",
                "default" to true
            ),
            "dateFormat" to mapOf(
                "description" to "Date format for timestamps",
                "type" to "string",
                "default" to "yyyy-MM-dd HH:mm:ss.SSS",
                "options" to listOf(
                    "yyyy-MM-dd HH:mm:ss.SSS",
                    "yyyy-MM-dd HH:mm:ss",
                    "MM/dd/yyyy HH:mm:ss",
                    "timestamp"
                )
            ),
            "encoding" to mapOf(
                "description" to "File encoding",
                "type" to "string",
                "default" to "UTF-8",
                "options" to listOf("UTF-8", "ASCII", "ISO-8859-1")
            )
        )
    }

    override fun validateConfig(config: DataExporter.ExportConfig): List<String> {
        val errors = mutableListOf<String>()
        
        // Validate custom options
        val separator = config.customOptions["separator"] as? String
        if (separator != null && separator.length != 1) {
            errors.add("CSV separator must be a single character")
        }
        
        val dateFormat = config.customOptions["dateFormat"] as? String
        if (dateFormat != null && dateFormat != "timestamp") {
            try {
                SimpleDateFormat(dateFormat, Locale.US)
            } catch (e: IllegalArgumentException) {
                errors.add("Invalid date format: $dateFormat")
            }
        }
        
        return errors
    }

    override fun exportData(
        dataset: DataExporter.ExportDataset,
        outputFile: File,
        config: DataExporter.ExportConfig
    ): DataExporter.ExportResult {
        val startTime = System.currentTimeMillis()
        
        try {
            val separator = config.customOptions["separator"] as? String ?: CSV_SEPARATOR
            val includeHeaders = config.customOptions["includeHeaders"] as? Boolean ?: true
            val dateFormat = config.customOptions["dateFormat"] as? String ?: "yyyy-MM-dd HH:mm:ss.SSS"
            val encoding = config.customOptions["encoding"] as? String ?: "UTF-8"
            
            val dateFormatter = if (dateFormat == "timestamp") null else SimpleDateFormat(dateFormat, Locale.US)
            
            OutputStreamWriter(FileOutputStream(outputFile), Charset.forName(encoding)).use { writer ->
                // Write headers if requested
                if (includeHeaders) {
                    writeHeaders(writer, dataset, separator)
                }
                
                // Write session metadata
                if (config.includeMetadata) {
                    writeMetadata(writer, dataset, separator, dateFormatter)
                }
                
                // Write time series data
                if (config.includeRawData || config.includeProcessedData) {
                    writeTimeSeriesData(writer, dataset, separator, dateFormatter, config)
                }
                
                // Write video metadata
                writeVideoMetadata(writer, dataset, separator, dateFormatter)
            }
            
            val endTime = System.currentTimeMillis()
            val fileSize = outputFile.length()
            
            return DataExporter.ExportResult(
                success = true,
                outputFile = outputFile,
                fileSize = fileSize,
                exportDuration = endTime - startTime
            )
            
        } catch (e: Exception) {
            return DataExporter.ExportResult(
                success = false,
                outputFile = null,
                errorMessage = "CSV export failed: ${e.message}"
            )
        }
    }

    override fun exportTimeSeries(
        dataSeries: List<DataExporter.DataSeries>,
        outputFile: File,
        config: DataExporter.ExportConfig
    ): DataExporter.ExportResult {
        val startTime = System.currentTimeMillis()
        
        try {
            val separator = config.customOptions["separator"] as? String ?: CSV_SEPARATOR
            val includeHeaders = config.customOptions["includeHeaders"] as? Boolean ?: true
            val dateFormat = config.customOptions["dateFormat"] as? String ?: "yyyy-MM-dd HH:mm:ss.SSS"
            val encoding = config.customOptions["encoding"] as? String ?: "UTF-8"
            
            val dateFormatter = if (dateFormat == "timestamp") null else SimpleDateFormat(dateFormat, Locale.US)
            
            OutputStreamWriter(FileOutputStream(outputFile), Charset.forName(encoding)).use { writer ->
                // Write headers
                if (includeHeaders) {
                    writeTimeSeriesHeaders(writer, dataSeries, separator)
                }
                
                // Combine all timestamps and sort
                val allTimestamps = dataSeries.flatMap { it.timestamps }.distinct().sorted()
                
                // Write data rows
                for (timestamp in allTimestamps) {
                    val row = mutableListOf<String>()
                    
                    // Add timestamp
                    row.add(formatTimestamp(timestamp, dateFormatter))
                    
                    // Add values for each series
                    for (series in dataSeries) {
                        val index = series.timestamps.indexOf(timestamp)
                        val value = if (index >= 0 && index < series.values.size) {
                            series.values[index].toString()
                        } else {
                            "" // Empty for missing values
                        }
                        row.add(escapeCsvValue(value))
                    }
                    
                    writer.write(row.joinToString(separator) + CSV_NEWLINE)
                }
            }
            
            val endTime = System.currentTimeMillis()
            val fileSize = outputFile.length()
            
            return DataExporter.ExportResult(
                success = true,
                outputFile = outputFile,
                fileSize = fileSize,
                exportDuration = endTime - startTime
            )
            
        } catch (e: Exception) {
            return DataExporter.ExportResult(
                success = false,
                outputFile = null,
                errorMessage = "Time series CSV export failed: ${e.message}"
            )
        }
    }

    override fun estimateOutputSize(
        dataset: DataExporter.ExportDataset,
        config: DataExporter.ExportConfig
    ): Long {
        // Rough estimation based on data points and metadata
        val dataPointsCount = dataset.dataSeries.sumOf { it.values.size }
        val avgBytesPerDataPoint = 50 // Estimated average bytes per CSV row
        val metadataSize = 1024 // Estimated metadata size
        val videoMetadataSize = dataset.videoData.size * 200 // Estimated per video metadata
        
        return (dataPointsCount * avgBytesPerDataPoint + metadataSize + videoMetadataSize).toLong()
    }

    override fun getRequiredDependencies(): List<String> {
        return emptyList() // No external dependencies required
    }

    override fun verifyDependencies(): Boolean {
        return true // Always available
    }

    private fun writeHeaders(
        writer: OutputStreamWriter,
        dataset: DataExporter.ExportDataset,
        separator: String
    ) {
        val headers = mutableListOf("Timestamp")
        
        // Add headers for each data series
        dataset.dataSeries.forEach { series ->
            headers.add("${series.name} (${series.unit})")
        }
        
        // Add video metadata headers if present
        if (dataset.videoData.isNotEmpty()) {
            headers.addAll(listOf("Video_Name", "Video_Path", "Frame_Rate", "Duration", "Resolution"))
        }
        
        writer.write(headers.joinToString(separator) + CSV_NEWLINE)
    }

    private fun writeTimeSeriesHeaders(
        writer: OutputStreamWriter,
        dataSeries: List<DataExporter.DataSeries>,
        separator: String
    ) {
        val headers = mutableListOf("Timestamp")
        dataSeries.forEach { series ->
            headers.add("${series.name} (${series.unit})")
        }
        writer.write(headers.joinToString(separator) + CSV_NEWLINE)
    }

    private fun writeMetadata(
        writer: OutputStreamWriter,
        dataset: DataExporter.ExportDataset,
        separator: String,
        dateFormatter: SimpleDateFormat?
    ) {
        // Write session metadata as comments
        writer.write("# Session Metadata$CSV_NEWLINE")
        writer.write("# Session ID${separator}${dataset.sessionId}$CSV_NEWLINE")
        writer.write("# Start Time${separator}${formatTimestamp(dataset.startTime, dateFormatter)}$CSV_NEWLINE")
        writer.write("# End Time${separator}${formatTimestamp(dataset.endTime, dateFormatter)}$CSV_NEWLINE")
        writer.write("# Duration${separator}${dataset.endTime - dataset.startTime} ms$CSV_NEWLINE")
        
        dataset.metadata.forEach { (key, value) ->
            writer.write("# $key${separator}$value$CSV_NEWLINE")
        }
        
        writer.write("$CSV_NEWLINE") // Empty line after metadata
    }

    private fun writeTimeSeriesData(
        writer: OutputStreamWriter,
        dataset: DataExporter.ExportDataset,
        separator: String,
        dateFormatter: SimpleDateFormat?,
        config: DataExporter.ExportConfig
    ) {
        // Combine all timestamps and sort
        val allTimestamps = dataset.dataSeries.flatMap { it.timestamps }.distinct().sorted()
        
        for (timestamp in allTimestamps) {
            val row = mutableListOf<String>()
            
            // Add timestamp
            row.add(formatTimestamp(timestamp, dateFormatter))
            
            // Add values for each series
            for (series in dataset.dataSeries) {
                val index = series.timestamps.indexOf(timestamp)
                val value = if (index >= 0 && index < series.values.size) {
                    series.values[index].toString()
                } else {
                    "" // Empty for missing values
                }
                row.add(escapeCsvValue(value))
            }
            
            writer.write(row.joinToString(separator) + CSV_NEWLINE)
        }
    }

    private fun writeVideoMetadata(
        writer: OutputStreamWriter,
        dataset: DataExporter.ExportDataset,
        separator: String,
        dateFormatter: SimpleDateFormat?
    ) {
        if (dataset.videoData.isNotEmpty()) {
            writer.write("$CSV_NEWLINE# Video Metadata$CSV_NEWLINE")
            
            for (video in dataset.videoData) {
                val row = listOf(
                    "",  // Empty timestamp for video metadata
                    escapeCsvValue(video.name),
                    escapeCsvValue(video.filePath),
                    video.frameRate.toString(),
                    video.duration.toString(),
                    "${video.resolution.first}x${video.resolution.second}"
                )
                writer.write(row.joinToString(separator) + CSV_NEWLINE)
            }
        }
    }

    private fun formatTimestamp(timestamp: Long, dateFormatter: SimpleDateFormat?): String {
        return if (dateFormatter != null) {
            dateFormatter.format(Date(timestamp))
        } else {
            timestamp.toString()
        }
    }

    private fun escapeCsvValue(value: String): String {
        return if (value.contains(CSV_SEPARATOR) || value.contains(CSV_QUOTE) || value.contains(CSV_NEWLINE)) {
            CSV_QUOTE + value.replace(CSV_QUOTE, CSV_QUOTE + CSV_QUOTE) + CSV_QUOTE
        } else {
            value
        }
    }
}