package com.buccancs.gsrcapture.export.interfaces

import java.io.File

/**
 * Interface for data export utilities
 * Supports exporting collected data to various analysis formats
 */
interface DataExporter {
    /**
     * Data class representing a data series (e.g., GSR, PPG, temperature)
     */
    data class DataSeries(
        val name: String,
        val timestamps: List<Long>,
        val values: List<Double>,
        val unit: String,
        val metadata: Map<String, Any> = emptyMap(),
    )

    /**
     * Data class representing video/image data
     */
    data class VideoData(
        val name: String,
        val filePath: String,
        val frameRate: Double,
        val duration: Long, // in milliseconds
        val resolution: Pair<Int, Int>, // width x height
        val metadata: Map<String, Any> = emptyMap(),
    )

    /**
     * Data class representing a complete dataset for export
     */
    data class ExportDataset(
        val sessionId: String,
        val startTime: Long,
        val endTime: Long,
        val dataSeries: List<DataSeries>,
        val videoData: List<VideoData>,
        val metadata: Map<String, Any> = emptyMap(),
    )

    /**
     * Export configuration options
     */
    data class ExportConfig(
        val includeRawData: Boolean = true,
        val includeProcessedData: Boolean = true,
        val includeMetadata: Boolean = true,
        val compressionLevel: Int = 6, // 0-9, where 9 is maximum compression
        val customOptions: Map<String, Any> = emptyMap(),
    )

    /**
     * Export result information
     */
    data class ExportResult(
        val success: Boolean,
        val outputFile: File?,
        val fileSize: Long = 0,
        val exportDuration: Long = 0, // in milliseconds
        val errorMessage: String? = null,
        val warnings: List<String> = emptyList(),
    )

    /**
     * Get the supported export format name
     * @return Format name (e.g., "MATLAB", "HDF5", "CSV")
     */
    fun getFormatName(): String

    /**
     * Get the file extension for this format
     * @return File extension (e.g., ".mat", ".h5", ".csv")
     */
    fun getFileExtension(): String

    /**
     * Check if the exporter supports a specific data type
     * @param dataType Type of data (e.g., "timeseries", "video", "images")
     * @return true if supported
     */
    fun supportsDataType(dataType: String): Boolean

    /**
     * Get available export configuration options
     * @return Map of option names to their descriptions and possible values
     */
    fun getAvailableOptions(): Map<String, Map<String, Any>>

    /**
     * Validate export configuration
     * @param config Export configuration to validate
     * @return List of validation errors (empty if valid)
     */
    fun validateConfig(config: ExportConfig): List<String>

    /**
     * Export dataset to the specified format
     * @param dataset Dataset to export
     * @param outputFile Output file path
     * @param config Export configuration
     * @return Export result
     */
    fun exportData(
        dataset: ExportDataset,
        outputFile: File,
        config: ExportConfig = ExportConfig(),
    ): ExportResult

    /**
     * Export only time series data (without video)
     * @param dataSeries List of data series to export
     * @param outputFile Output file path
     * @param config Export configuration
     * @return Export result
     */
    fun exportTimeSeries(
        dataSeries: List<DataSeries>,
        outputFile: File,
        config: ExportConfig = ExportConfig(),
    ): ExportResult

    /**
     * Get estimated output file size
     * @param dataset Dataset to estimate
     * @param config Export configuration
     * @return Estimated file size in bytes
     */
    fun estimateOutputSize(
        dataset: ExportDataset,
        config: ExportConfig,
    ): Long

    /**
     * Check if the exporter requires external dependencies
     * @return List of required dependencies or empty list if none
     */
    fun getRequiredDependencies(): List<String>

    /**
     * Verify that all required dependencies are available
     * @return true if all dependencies are available
     */
    fun verifyDependencies(): Boolean
}
