package com.buccancs.gsrcapture.export

import com.buccancs.gsrcapture.export.interfaces.DataExporter
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.IOException

/**
 * Test class for data export functionality
 */
class DataExporterTest {
    
    @Test
    fun testMatlabExporter() {
        val exporter = MatlabExporter()
        
        // Test basic properties
        assertEquals("MATLAB", exporter.getFormatName())
        assertEquals(".mat", exporter.getFileExtension())
        assertTrue(exporter.supportsDataType("timeseries"))
        assertFalse(exporter.supportsDataType("video"))
        assertTrue(exporter.verifyDependencies())
        
        // Test configuration validation
        val validConfig = DataExporter.ExportConfig(compressionLevel = 5)
        val validationErrors = exporter.validateConfig(validConfig)
        assertTrue("Valid config should have no errors", validationErrors.isEmpty())
        
        val invalidConfig = DataExporter.ExportConfig(compressionLevel = 15)
        val invalidErrors = exporter.validateConfig(invalidConfig)
        assertFalse("Invalid config should have errors", invalidErrors.isEmpty())
    }
    
    @Test
    fun testHdf5Exporter() {
        val exporter = Hdf5Exporter()
        
        // Test basic properties
        assertEquals("HDF5", exporter.getFormatName())
        assertEquals(".h5", exporter.getFileExtension())
        assertTrue(exporter.supportsDataType("timeseries"))
        assertTrue(exporter.supportsDataType("images"))
        assertFalse(exporter.supportsDataType("video"))
        assertTrue(exporter.verifyDependencies())
        
        // Test configuration validation
        val validConfig = DataExporter.ExportConfig(
            compressionLevel = 6,
            customOptions = mapOf("chunkSize" to 32768)
        )
        val validationErrors = exporter.validateConfig(validConfig)
        assertTrue("Valid config should have no errors", validationErrors.isEmpty())
    }
    
    @Test
    fun testDataExportWorkflow() {
        // Create sample data
        val timestamps = (0..99).map { it * 100L } // 100ms intervals
        val gsrValues = timestamps.map { Math.sin(it / 1000.0) * 10 + 50 } // Simulated GSR data
        val ppgValues = timestamps.map { Math.cos(it / 800.0) * 5 + 75 } // Simulated PPG data
        
        val gsrSeries = DataExporter.DataSeries(
            name = "GSR",
            timestamps = timestamps,
            values = gsrValues,
            unit = "microsiemens",
            metadata = mapOf("sensorType" to "Shimmer3", "samplingRate" to "10Hz")
        )
        
        val ppgSeries = DataExporter.DataSeries(
            name = "PPG",
            timestamps = timestamps,
            values = ppgValues,
            unit = "arbitrary_units",
            metadata = mapOf("sensorType" to "Shimmer3", "channel" to "A12")
        )
        
        val dataset = DataExporter.ExportDataset(
            sessionId = "test_session_001",
            startTime = timestamps.first(),
            endTime = timestamps.last(),
            dataSeries = listOf(gsrSeries, ppgSeries),
            videoData = emptyList(),
            metadata = mapOf(
                "participant" to "P001",
                "condition" to "baseline",
                "notes" to "Test export functionality"
            )
        )
        
        // Test MATLAB export
        testExporter(MatlabExporter(), dataset, "test_output.mat")
        
        // Test HDF5 export
        testExporter(Hdf5Exporter(), dataset, "test_output.h5")
    }
    
    private fun testExporter(
        exporter: DataExporter,
        dataset: DataExporter.ExportDataset,
        filename: String
    ) {
        try {
            // Create temporary file
            val tempFile = File.createTempFile("export_test", exporter.getFileExtension())
            tempFile.deleteOnExit()
            
            // Test size estimation
            val estimatedSize = exporter.estimateOutputSize(dataset, DataExporter.ExportConfig())
            assertTrue("Estimated size should be positive", estimatedSize > 0)
            
            // Test export
            val config = DataExporter.ExportConfig(
                includeRawData = true,
                includeMetadata = true,
                compressionLevel = 3
            )
            
            val result = exporter.exportData(dataset, tempFile, config)
            
            // Verify export result
            assertTrue("Export should succeed", result.success)
            assertNotNull("Output file should be set", result.outputFile)
            assertTrue("Output file should exist", tempFile.exists())
            assertTrue("File should have content", tempFile.length() > 0)
            assertTrue("Export duration should be recorded", result.exportDuration >= 0)
            
            println("[DEBUG_LOG] ${exporter.getFormatName()} export test passed:")
            println("[DEBUG_LOG] - File: ${tempFile.absolutePath}")
            println("[DEBUG_LOG] - Size: ${result.fileSize} bytes")
            println("[DEBUG_LOG] - Duration: ${result.exportDuration} ms")
            println("[DEBUG_LOG] - Estimated vs Actual: ${estimatedSize} vs ${result.fileSize}")
            
            if (result.warnings.isNotEmpty()) {
                println("[DEBUG_LOG] - Warnings: ${result.warnings}")
            }
            
        } catch (e: IOException) {
            fail("Export test failed with IOException: ${e.message}")
        } catch (e: Exception) {
            fail("Export test failed with exception: ${e.message}")
        }
    }
    
    @Test
    fun testTimeSeriesExport() {
        val timestamps = (0..49).map { it * 200L } // 200ms intervals
        val values = timestamps.map { Math.random() * 100 } // Random data
        
        val series = DataExporter.DataSeries(
            name = "TestSeries",
            timestamps = timestamps,
            values = values,
            unit = "units",
            metadata = mapOf("test" to "true")
        )
        
        val exporter = MatlabExporter()
        val tempFile = File.createTempFile("timeseries_test", ".mat")
        tempFile.deleteOnExit()
        
        val result = exporter.exportTimeSeries(listOf(series), tempFile)
        
        assertTrue("Time series export should succeed", result.success)
        assertTrue("Output file should exist", tempFile.exists())
        assertTrue("File should have content", tempFile.length() > 0)
        
        println("[DEBUG_LOG] Time series export test passed:")
        println("[DEBUG_LOG] - File: ${tempFile.absolutePath}")
        println("[DEBUG_LOG] - Size: ${result.fileSize} bytes")
    }
}