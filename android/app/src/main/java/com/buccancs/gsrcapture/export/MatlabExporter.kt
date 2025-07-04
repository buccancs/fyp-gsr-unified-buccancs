package com.buccancs.gsrcapture.export

import android.util.Log
import com.buccancs.gsrcapture.export.interfaces.DataExporter
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

/**
 * MATLAB .mat file exporter implementation
 * Creates MATLAB-compatible files for data analysis
 */
class MatlabExporter : DataExporter {
    
    companion object {
        private const val TAG = "MatlabExporter"
        
        // MATLAB file format constants
        private const val MAT_FILE_HEADER_SIZE = 128
        private const val MAT_FILE_VERSION = 0x0100
        private const val MATLAB_MAGIC = "MATLAB 5.0 MAT-file"
        
        // Data types
        private const val miINT8 = 1
        private const val miUINT8 = 2
        private const val miINT16 = 3
        private const val miUINT16 = 4
        private const val miINT32 = 5
        private const val miUINT32 = 6
        private const val miSINGLE = 7
        private const val miDOUBLE = 9
        private const val miINT64 = 12
        private const val miUINT64 = 13
        private const val miMATRIX = 14
        private const val miCOMPRESSED = 15
        private const val miUTF8 = 16
        
        // Array types
        private const val mxCELL_CLASS = 1
        private const val mxSTRUCT_CLASS = 2
        private const val mxOBJECT_CLASS = 3
        private const val mxCHAR_CLASS = 4
        private const val mxSPARSE_CLASS = 5
        private const val mxDOUBLE_CLASS = 6
        private const val mxSINGLE_CLASS = 7
        private const val mxINT8_CLASS = 8
        private const val mxUINT8_CLASS = 9
        private const val mxINT16_CLASS = 10
        private const val mxUINT16_CLASS = 11
        private const val mxINT32_CLASS = 12
        private const val mxUINT32_CLASS = 13
        private const val mxINT64_CLASS = 14
        private const val mxUINT64_CLASS = 15
    }
    
    override fun getFormatName(): String = "MATLAB"
    
    override fun getFileExtension(): String = ".mat"
    
    override fun supportsDataType(dataType: String): Boolean {
        return when (dataType.lowercase()) {
            "timeseries", "numeric", "matrix", "struct" -> true
            "video", "images" -> false // MATLAB files don't directly support video
            else -> false
        }
    }
    
    override fun getAvailableOptions(): Map<String, Map<String, Any>> {
        return mapOf(
            "compressionLevel" to mapOf(
                "description" to "Compression level (0-9)",
                "type" to "integer",
                "min" to 0,
                "max" to 9,
                "default" to 6
            ),
            "includeMetadata" to mapOf(
                "description" to "Include metadata in the output",
                "type" to "boolean",
                "default" to true
            ),
            "precision" to mapOf(
                "description" to "Numeric precision",
                "type" to "string",
                "options" to listOf("single", "double"),
                "default" to "double"
            )
        )
    }
    
    override fun validateConfig(config: DataExporter.ExportConfig): List<String> {
        val errors = mutableListOf<String>()
        
        if (config.compressionLevel < 0 || config.compressionLevel > 9) {
            errors.add("Compression level must be between 0 and 9")
        }
        
        val precision = config.customOptions["precision"] as? String
        if (precision != null && precision !in listOf("single", "double")) {
            errors.add("Precision must be 'single' or 'double'")
        }
        
        return errors
    }
    
    override fun exportData(
        dataset: DataExporter.ExportDataset,
        outputFile: File,
        config: DataExporter.ExportConfig
    ): DataExporter.ExportResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            // Validate configuration
            val validationErrors = validateConfig(config)
            if (validationErrors.isNotEmpty()) {
                return DataExporter.ExportResult(
                    success = false,
                    outputFile = null,
                    errorMessage = "Configuration validation failed: ${validationErrors.joinToString(", ")}"
                )
            }
            
            // Create output directory if it doesn't exist
            outputFile.parentFile?.mkdirs()
            
            // Write MATLAB file
            FileOutputStream(outputFile).use { fos ->
                writeMatFile(fos, dataset, config)
            }
            
            val endTime = System.currentTimeMillis()
            val fileSize = outputFile.length()
            
            Log.d(TAG, "Successfully exported dataset to MATLAB format: ${outputFile.absolutePath}")
            
            DataExporter.ExportResult(
                success = true,
                outputFile = outputFile,
                fileSize = fileSize,
                exportDuration = endTime - startTime,
                warnings = if (dataset.videoData.isNotEmpty()) {
                    listOf("Video data was skipped - MATLAB format doesn't support embedded video")
                } else emptyList()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting to MATLAB format", e)
            DataExporter.ExportResult(
                success = false,
                outputFile = null,
                errorMessage = "Export failed: ${e.message}"
            )
        }
    }
    
    override fun exportTimeSeries(
        dataSeries: List<DataExporter.DataSeries>,
        outputFile: File,
        config: DataExporter.ExportConfig
    ): DataExporter.ExportResult {
        val dataset = DataExporter.ExportDataset(
            sessionId = "timeseries_export",
            startTime = dataSeries.minOfOrNull { it.timestamps.minOrNull() ?: 0L } ?: 0L,
            endTime = dataSeries.maxOfOrNull { it.timestamps.maxOrNull() ?: 0L } ?: 0L,
            dataSeries = dataSeries,
            videoData = emptyList()
        )
        
        return exportData(dataset, outputFile, config)
    }
    
    override fun estimateOutputSize(
        dataset: DataExporter.ExportDataset,
        config: DataExporter.ExportConfig
    ): Long {
        var estimatedSize = MAT_FILE_HEADER_SIZE.toLong()
        
        // Estimate size for each data series
        for (series in dataset.dataSeries) {
            val dataPoints = series.values.size
            val bytesPerValue = if (config.customOptions["precision"] == "single") 4 else 8
            val seriesSize = dataPoints * bytesPerValue * 2 // timestamps + values
            val overhead = 200 // Estimated overhead for MATLAB structure
            
            estimatedSize += seriesSize + overhead
        }
        
        // Apply compression factor
        val compressionFactor = when (config.compressionLevel) {
            0 -> 1.0
            in 1..3 -> 0.8
            in 4..6 -> 0.6
            in 7..9 -> 0.4
            else -> 0.6
        }
        
        return (estimatedSize * compressionFactor).toLong()
    }
    
    override fun getRequiredDependencies(): List<String> {
        return emptyList() // No external dependencies required
    }
    
    override fun verifyDependencies(): Boolean {
        return true // No dependencies to verify
    }
    
    // Private helper methods for MATLAB file format
    
    private fun writeMatFile(
        outputStream: OutputStream,
        dataset: DataExporter.ExportDataset,
        config: DataExporter.ExportConfig
    ) {
        val buffer = ByteArrayOutputStream()
        val dataStream = DataOutputStream(buffer)
        
        // Write file header
        writeFileHeader(dataStream)
        
        // Create main structure
        val structData = createDataStructure(dataset, config)
        writeMatrixElement(dataStream, "gsrData", structData, config)
        
        // Write to output stream (with optional compression)
        if (config.compressionLevel > 0) {
            writeCompressed(outputStream, buffer.toByteArray(), config.compressionLevel)
        } else {
            outputStream.write(buffer.toByteArray())
        }
    }
    
    private fun writeFileHeader(dataStream: DataOutputStream) {
        // Write header text
        val headerText = MATLAB_MAGIC.padEnd(116, ' ')
        dataStream.write(headerText.toByteArray())
        
        // Write subsystem data offset (8 bytes)
        dataStream.writeLong(0)
        
        // Write version
        dataStream.writeShort(MAT_FILE_VERSION)
        
        // Write endian indicator
        dataStream.write("IM".toByteArray())
    }
    
    private fun createDataStructure(
        dataset: DataExporter.ExportDataset,
        config: DataExporter.ExportConfig
    ): Map<String, Any> {
        val structData = mutableMapOf<String, Any>()
        
        // Add session metadata
        if (config.includeMetadata) {
            structData["sessionId"] = dataset.sessionId
            structData["startTime"] = dataset.startTime
            structData["endTime"] = dataset.endTime
            structData["metadata"] = dataset.metadata
        }
        
        // Add data series
        for (series in dataset.dataSeries) {
            val seriesStruct = mapOf(
                "timestamps" to series.timestamps.map { it.toDouble() },
                "values" to series.values,
                "unit" to series.unit,
                "metadata" to series.metadata
            )
            structData[series.name] = seriesStruct
        }
        
        // Add video metadata (since we can't embed video in .mat files)
        if (dataset.videoData.isNotEmpty() && config.includeMetadata) {
            val videoMetadata = dataset.videoData.map { video ->
                mapOf(
                    "name" to video.name,
                    "filePath" to video.filePath,
                    "frameRate" to video.frameRate,
                    "duration" to video.duration,
                    "resolution" to listOf(video.resolution.first, video.resolution.second),
                    "metadata" to video.metadata
                )
            }
            structData["videoMetadata"] = videoMetadata
        }
        
        return structData
    }
    
    private fun writeMatrixElement(
        dataStream: DataOutputStream,
        name: String,
        data: Any,
        config: DataExporter.ExportConfig
    ) {
        val elementBuffer = ByteArrayOutputStream()
        val elementStream = DataOutputStream(elementBuffer)
        
        // Write array flags
        writeArrayFlags(elementStream, mxSTRUCT_CLASS)
        
        // Write dimensions
        writeDimensions(elementStream, intArrayOf(1, 1))
        
        // Write array name
        writeArrayName(elementStream, name)
        
        // Write field names and data
        writeStructData(elementStream, data as Map<String, Any>, config)
        
        // Write element header
        val elementData = elementBuffer.toByteArray()
        dataStream.writeInt(miMATRIX)
        dataStream.writeInt(elementData.size)
        dataStream.write(elementData)
    }
    
    private fun writeArrayFlags(dataStream: DataOutputStream, arrayClass: Int) {
        dataStream.writeInt(miUINT32)
        dataStream.writeInt(8)
        dataStream.writeInt(arrayClass)
        dataStream.writeInt(0) // flags
    }
    
    private fun writeDimensions(dataStream: DataOutputStream, dimensions: IntArray) {
        dataStream.writeInt(miINT32)
        dataStream.writeInt(dimensions.size * 4)
        for (dim in dimensions) {
            dataStream.writeInt(dim)
        }
    }
    
    private fun writeArrayName(dataStream: DataOutputStream, name: String) {
        val nameBytes = name.toByteArray()
        dataStream.writeInt(miINT8)
        dataStream.writeInt(nameBytes.size)
        dataStream.write(nameBytes)
        
        // Pad to 8-byte boundary
        val padding = (8 - (nameBytes.size % 8)) % 8
        repeat(padding) { dataStream.writeByte(0) }
    }
    
    private fun writeStructData(
        dataStream: DataOutputStream,
        data: Map<String, Any>,
        config: DataExporter.ExportConfig
    ) {
        // This is a simplified implementation
        // In a full implementation, you would need to handle all MATLAB data types
        // and properly structure the field names and data
        
        // Write field names
        val fieldNames = data.keys.toList()
        writeFieldNames(dataStream, fieldNames)
        
        // Write field data
        for (fieldName in fieldNames) {
            val fieldData = data[fieldName]
            writeFieldData(dataStream, fieldData, config)
        }
    }
    
    private fun writeFieldNames(dataStream: DataOutputStream, fieldNames: List<String>) {
        val maxNameLength = fieldNames.maxOfOrNull { it.length } ?: 0
        val nameLength = ((maxNameLength + 7) / 8) * 8 // Round up to 8-byte boundary
        
        dataStream.writeInt(miINT32)
        dataStream.writeInt(4)
        dataStream.writeInt(nameLength)
        dataStream.writeInt(0) // padding
        
        for (name in fieldNames) {
            val nameBytes = name.toByteArray()
            dataStream.write(nameBytes)
            // Pad to nameLength
            repeat(nameLength - nameBytes.size) { dataStream.writeByte(0) }
        }
    }
    
    private fun writeFieldData(dataStream: DataOutputStream, data: Any?, config: DataExporter.ExportConfig) {
        // Simplified field data writing
        // In a full implementation, this would handle all MATLAB data types properly
        when (data) {
            is String -> writeStringData(dataStream, data)
            is Number -> writeNumericData(dataStream, doubleArrayOf(data.toDouble()))
            is List<*> -> {
                if (data.isNotEmpty() && data[0] is Number) {
                    val doubleArray = data.map { (it as Number).toDouble() }.toDoubleArray()
                    writeNumericData(dataStream, doubleArray)
                }
            }
            is Map<*, *> -> {
                // Nested structure - would need recursive handling
                writeStringData(dataStream, data.toString())
            }
            else -> writeStringData(dataStream, data.toString())
        }
    }
    
    private fun writeStringData(dataStream: DataOutputStream, text: String) {
        val textBytes = text.toByteArray()
        
        // Write array flags for char array
        writeArrayFlags(dataStream, mxCHAR_CLASS)
        
        // Write dimensions
        writeDimensions(dataStream, intArrayOf(1, textBytes.size))
        
        // Write array name (empty for field data)
        writeArrayName(dataStream, "")
        
        // Write string data
        dataStream.writeInt(miUTF8)
        dataStream.writeInt(textBytes.size)
        dataStream.write(textBytes)
        
        // Pad to 8-byte boundary
        val padding = (8 - (textBytes.size % 8)) % 8
        repeat(padding) { dataStream.writeByte(0) }
    }
    
    private fun writeNumericData(dataStream: DataOutputStream, values: DoubleArray) {
        // Write array flags for double array
        writeArrayFlags(dataStream, mxDOUBLE_CLASS)
        
        // Write dimensions
        writeDimensions(dataStream, intArrayOf(values.size, 1))
        
        // Write array name (empty for field data)
        writeArrayName(dataStream, "")
        
        // Write numeric data
        dataStream.writeInt(miDOUBLE)
        dataStream.writeInt(values.size * 8)
        for (value in values) {
            dataStream.writeDouble(value)
        }
    }
    
    private fun writeCompressed(
        outputStream: OutputStream,
        data: ByteArray,
        compressionLevel: Int
    ) {
        val deflater = Deflater(compressionLevel)
        val compressedStream = DeflaterOutputStream(outputStream, deflater)
        compressedStream.write(data)
        compressedStream.close()
    }
}