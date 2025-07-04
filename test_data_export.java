import com.buccancs.gsr.shared.utils.DataExporter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test class for the DataExporter utility.
 * Creates a mock session directory and tests all export formats.
 */
public class test_data_export {

    public static void main(String[] args) {
        System.out.println("Testing DataExporter utility...");

        try {
            // Create a mock session directory for testing
            String mockSessionDir = createMockSession();
            System.out.println("Created mock session directory: " + mockSessionDir);

            // Test all export formats
            testExportFormat(mockSessionDir, DataExporter.ExportFormat.CSV_BUNDLE);
            testExportFormat(mockSessionDir, DataExporter.ExportFormat.MATLAB_MAT);
            testExportFormat(mockSessionDir, DataExporter.ExportFormat.HDF5);

            System.out.println("\n=== All DataExporter tests completed successfully! ===");

        } catch (Exception e) {
            System.err.println("Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates a mock session directory with sample data for testing.
     */
    private static String createMockSession() throws IOException {
        // Create temporary session directory
        Path tempDir = Files.createTempDirectory("test_session_");
        String sessionId = "session_20241201_123456_test";

        // Create session metadata
        String metadata = "{\n" +
            "    \"sessionId\": \"" + sessionId + "\",\n" +
            "    \"timestamp\": \"2024-12-01T12:34:56.789Z\",\n" +
            "    \"timestampEpochMillis\": 1733058896789,\n" +
            "    \"sessionStartTimeNanos\": 1733058896789000000,\n" +
            "    \"device\": {\n" +
            "        \"manufacturer\": \"TestManufacturer\",\n" +
            "        \"model\": \"TestDevice\",\n" +
            "        \"androidVersion\": \"14\"\n" +
            "    },\n" +
            "    \"components\": {\n" +
            "        \"rgbCamera\": true,\n" +
            "        \"thermalCamera\": true,\n" +
            "        \"gsrSensor\": true,\n" +
            "        \"audio\": true\n" +
            "    },\n" +
            "    \"settings\": {\n" +
            "        \"rgbVideoQuality\": \"1080p\",\n" +
            "        \"audioSampleRate\": 44100,\n" +
            "        \"gsrSampleRate\": 128\n" +
            "    }\n" +
            "}";

        try (FileWriter writer = new FileWriter(tempDir.resolve("session_metadata.json").toFile())) {
            writer.write(metadata);
        }

        // Create mock GSR data
        String gsrData = "timestamp_nanos,session_offset_nanos,gsr_microsiemens\n" +
            "1733058896789000000,0,2.345\n" +
            "1733058896797000000,8000000,2.367\n" +
            "1733058896805000000,16000000,2.389\n" +
            "1733058896813000000,24000000,2.412\n" +
            "1733058896821000000,32000000,2.434\n";

        try (FileWriter writer = new FileWriter(tempDir.resolve("GSR_" + sessionId + "_20241201_123456.csv").toFile())) {
            writer.write(gsrData);
        }

        // Create mock video file (empty file for testing)
        Files.createFile(tempDir.resolve("RGB_" + sessionId + "_20241201_123456.mp4"));

        // Create mock audio file (empty file for testing)
        Files.createFile(tempDir.resolve("Audio_" + sessionId + "_20241201_123456.wav"));

        // Create mock raw RGB images directory
        Path rawRgbDir = tempDir.resolve("raw_rgb_" + sessionId);
        Files.createDirectories(rawRgbDir);
        Files.createFile(rawRgbDir.resolve("frame_001.jpg"));
        Files.createFile(rawRgbDir.resolve("frame_002.jpg"));

        // Create mock thermal frames directory
        Path thermalDir = tempDir.resolve("thermal_" + sessionId);
        Files.createDirectories(thermalDir);
        Files.createFile(thermalDir.resolve("frame_1733058896789000000.jpg"));
        Files.createFile(thermalDir.resolve("frame_1733058896797000000.jpg"));

        return tempDir.toString();
    }

    /**
     * Tests a specific export format.
     */
    private static void testExportFormat(String sessionDir, DataExporter.ExportFormat format) {
        try {
            System.out.println("\n--- Testing " + format + " export ---");

            DataExporter.ExportConfig config = new DataExporter.ExportConfig(format);
            config.outputDirectory = System.getProperty("java.io.tmpdir") + "/GSR_Test_Exports";

            String result = DataExporter.exportSession(sessionDir, config);
            System.out.println("✓ " + format + " export successful: " + result);

            // Verify the output exists
            if (Files.exists(Paths.get(result))) {
                System.out.println("✓ Output file/directory verified: " + result);
            } else {
                System.err.println("✗ Output file/directory not found: " + result);
            }

        } catch (Exception e) {
            System.err.println("✗ " + format + " export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
