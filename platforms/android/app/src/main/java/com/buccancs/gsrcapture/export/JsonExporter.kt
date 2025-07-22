package com.buccancs.gsrcapture.export

import com.buccancs.gsrcapture.export.interfaces.DataExporter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

/**
 * JSON exporter implementation for data export
 */
class JsonExporter : DataExporter {
    
    companion object {
        private const val TAG = "JsonExporter"
    }

    override fun getFormatName(): String = "JSON"

    override fun getFileExtension(): String = ".json"

    override fun supportsDataType(dataType: String): Boolean {
        return when (dataType.lowercase()) {
            "timeseries", "sensor", "analytics", "metadata", "video", "images" -> true
            else -> false
        }
    }

    override fun getAvailableOptions(): Map<String, Map<String, Any>> {
        return mapOf(
            "prettyPrint" to mapOf(
                "description" to "Format JSON with indentation",
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
                    "timestamp",
                    "iso8601"
                )
            ),
            "encoding" to mapOf(
                "description" to "File encoding",
                "type" to "string",
                "default" to "UTF-8",
                "options" to listOf("UTF-8", "ASCII", "ISO-8859-1")
            ),
            "includeSchema" to mapOf(
                "description" to "Include JSON schema information",
                "type" to "boolean",
                "default" to false
            )
        )
    }

    override fun validateConfig(config: DataExporter.ExportConfig): List<String> {
        val errors = mutableListOf<String>()
        
        val dateFormat = config.customOptions["dateFormat"] as? String
        if (dateFormat != null && dateFormat != "timestamp" && dateFormat != "iso8601") {
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
            val prettyPrint = config.customOptions["prettyPrint"] as? Boolean ?: true
            val dateFormat = config.customOptions["dateFormat"] as? String ?: "yyyy-MM-dd HH:mm:ss.SSS"
            val encoding = config.customOptions["encoding"] as? String ?: "UTF-8"
            val includeSchema = config.customOptions["includeSchema"] as? Boolean ?: false
            
            val dateFormatter = when (dateFormat) {
                "timestamp" -> null
                "iso8601" -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                else -> SimpleDateFormat(dateFormat, Locale.US)
            }
            
            val jsonObject = JSONObject()
            
            // Add schema if requested
            if (includeSchema) {
                jsonObject.put("${'$'}schema", createSchema())
            }
            
            // Add session metadata
            if (config.includeMetadata) {
                jsonObject.put("session", createSessionObject(dataset, dateFormatter))
            }
            
            // Add time series data
            if (config.includeRawData || config.includeProcessedData) {
                jsonObject.put("timeSeries", createTimeSeriesArray(dataset.dataSeries, dateFormatter))
            }
            
            // Add video data
            if (dataset.videoData.isNotEmpty()) {
                jsonObject.put("videos", createVideoArray(dataset.videoData, dateFormatter))
            }
            
            // Write to file
            OutputStreamWriter(FileOutputStream(outputFile), Charset.forName(encoding)).use { writer ->
                if (prettyPrint) {
                    writer.write(jsonObject.toString(2))
                } else {
                    writer.write(jsonObject.toString())
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
                errorMessage = "JSON export failed: ${e.message}"
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
            val prettyPrint = config.customOptions["prettyPrint"] as? Boolean ?: true
            val dateFormat = config.customOptions["dateFormat"] as? String ?: "yyyy-MM-dd HH:mm:ss.SSS"
            val encoding = config.customOptions["encoding"] as? String ?: "UTF-8"
            
            val dateFormatter = when (dateFormat) {
                "timestamp" -> null
                "iso8601" -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                else -> SimpleDateFormat(dateFormat, Locale.US)
            }
            
            val jsonObject = JSONObject()
            jsonObject.put("timeSeries", createTimeSeriesArray(dataSeries, dateFormatter))
            jsonObject.put("exportedAt", formatTimestamp(System.currentTimeMillis(), dateFormatter))
            
            OutputStreamWriter(FileOutputStream(outputFile), Charset.forName(encoding)).use { writer ->
                if (prettyPrint) {
                    writer.write(jsonObject.toString(2))
                } else {
                    writer.write(jsonObject.toString())
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
                errorMessage = "Time series JSON export failed: ${e.message}"
            )
        }
    }

    override fun estimateOutputSize(
        dataset: DataExporter.ExportDataset,
        config: DataExporter.ExportConfig
    ): Long {
        // JSON is typically larger than CSV due to structure overhead
        val dataPointsCount = dataset.dataSeries.sumOf { it.values.size }
        val avgBytesPerDataPoint = 80 // Estimated average bytes per JSON data point
        val metadataSize = 2048 // Estimated metadata size
        val videoMetadataSize = dataset.videoData.size * 300 // Estimated per video metadata
        val structureOverhead = 1024 // JSON structure overhead
        
        return (dataPointsCount * avgBytesPerDataPoint + metadataSize + videoMetadataSize + structureOverhead).toLong()
    }

    override fun getRequiredDependencies(): List<String> {
        return emptyList() // Uses built-in JSON library
    }

    override fun verifyDependencies(): Boolean {
        return true // Always available
    }

    private fun createSchema(): JSONObject {
        return JSONObject().apply {
            put("type", "object")
            put("properties", JSONObject().apply {
                put("session", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("sessionId", JSONObject().put("type", "string"))
                        put("startTime", JSONObject().put("type", "string"))
                        put("endTime", JSONObject().put("type", "string"))
                        put("duration", JSONObject().put("type", "number"))
                    })
                })
                put("timeSeries", JSONObject().apply {
                    put("type", "array")
                    put("items", JSONObject().apply {
                        put("type", "object")
                        put("properties", JSONObject().apply {
                            put("name", JSONObject().put("type", "string"))
                            put("unit", JSONObject().put("type", "string"))
                            put("data", JSONObject().apply {
                                put("type", "array")
                                put("items", JSONObject().apply {
                                    put("type", "object")
                                    put("properties", JSONObject().apply {
                                        put("timestamp", JSONObject().put("type", "string"))
                                        put("value", JSONObject().put("type", "number"))
                                    })
                                })
                            })
                        })
                    })
                })
            })
        }
    }

    private fun createSessionObject(
        dataset: DataExporter.ExportDataset,
        dateFormatter: SimpleDateFormat?
    ): JSONObject {
        return JSONObject().apply {
            put("sessionId", dataset.sessionId)
            put("startTime", formatTimestamp(dataset.startTime, dateFormatter))
            put("endTime", formatTimestamp(dataset.endTime, dateFormatter))
            put("duration", dataset.endTime - dataset.startTime)
            
            // Add metadata
            val metadataObject = JSONObject()
            dataset.metadata.forEach { (key, value) ->
                metadataObject.put(key, value)
            }
            put("metadata", metadataObject)
        }
    }

    private fun createTimeSeriesArray(
        dataSeries: List<DataExporter.DataSeries>,
        dateFormatter: SimpleDateFormat?
    ): JSONArray {
        val timeSeriesArray = JSONArray()
        
        for (series in dataSeries) {
            val seriesObject = JSONObject().apply {
                put("name", series.name)
                put("unit", series.unit)
                
                // Add series metadata
                val metadataObject = JSONObject()
                series.metadata.forEach { (key, value) ->
                    metadataObject.put(key, value)
                }
                put("metadata", metadataObject)
                
                // Add data points
                val dataArray = JSONArray()
                for (i in series.timestamps.indices) {
                    if (i < series.values.size) {
                        val dataPoint = JSONObject().apply {
                            put("timestamp", formatTimestamp(series.timestamps[i], dateFormatter))
                            put("value", series.values[i])
                        }
                        dataArray.put(dataPoint)
                    }
                }
                put("data", dataArray)
                put("sampleCount", dataArray.length())
            }
            timeSeriesArray.put(seriesObject)
        }
        
        return timeSeriesArray
    }

    private fun createVideoArray(
        videoData: List<DataExporter.VideoData>,
        dateFormatter: SimpleDateFormat?
    ): JSONArray {
        val videoArray = JSONArray()
        
        for (video in videoData) {
            val videoObject = JSONObject().apply {
                put("name", video.name)
                put("filePath", video.filePath)
                put("frameRate", video.frameRate)
                put("duration", video.duration)
                put("resolution", JSONObject().apply {
                    put("width", video.resolution.first)
                    put("height", video.resolution.second)
                })
                
                // Add video metadata
                val metadataObject = JSONObject()
                video.metadata.forEach { (key, value) ->
                    metadataObject.put(key, value)
                }
                put("metadata", metadataObject)
            }
            videoArray.put(videoObject)
        }
        
        return videoArray
    }

    private fun formatTimestamp(timestamp: Long, dateFormatter: SimpleDateFormat?): String {
        return if (dateFormatter != null) {
            dateFormatter.format(Date(timestamp))
        } else {
            timestamp.toString()
        }
    }
}