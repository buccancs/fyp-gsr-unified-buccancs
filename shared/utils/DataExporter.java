package com.buccancs.gsr.shared.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.util.Date;
// Removed JSONObject dependency - using simple string parsing instead

/**
 * DataExporter provides utilities to export GSR & Dual-Video Recording System data
 * to common analysis formats like MATLAB (.mat) and HDF5 (.h5).
 * 
 * This utility can process complete recording sessions and convert them into
 * formats suitable for research analysis.
 */
public class DataExporter {

    private static final String TAG = "DataExporter";

    /**
     * Export formats supported by the exporter.
     */
    public enum ExportFormat {
        MATLAB_MAT,     // MATLAB .mat format
        HDF5,           // HDF5 .h5 format
        CSV_BUNDLE      // Organized CSV files with metadata
    }

    /**
     * Configuration for export operations.
     */
    public static class ExportConfig {
        public ExportFormat format;
        public boolean includeRawImages;
        public boolean includeThermalFrames;
        public boolean includeAudioData;
        public boolean compressOutput;
        public String outputDirectory;

        public ExportConfig(ExportFormat format) {
            this.format = format;
            this.includeRawImages = true;
            this.includeThermalFrames = true;
            this.includeAudioData = true;
            this.compressOutput = false;
            this.outputDirectory = System.getProperty("user.home") + "/GSR_Exports";
        }
    }

    /**
     * Represents a complete recording session with all its data.
     */
    public static class SessionData {
        public String sessionId;
        public String metadataJson;
        public String deviceManufacturer;
        public String deviceModel;
        public List<GsrDataPoint> gsrData;
        public List<String> rgbVideoFiles;
        public List<String> rawImageFiles;
        public List<String> thermalFrameFiles;
        public List<String> audioFiles;
        public long sessionStartTime;
        public long sessionEndTime;

        public SessionData() {
            this.gsrData = new ArrayList<>();
            this.rgbVideoFiles = new ArrayList<>();
            this.rawImageFiles = new ArrayList<>();
            this.thermalFrameFiles = new ArrayList<>();
            this.audioFiles = new ArrayList<>();
        }
    }

    /**
     * Represents a single GSR data point.
     */
    public static class GsrDataPoint {
        public long timestampNanos;
        public long sessionOffsetNanos;
        public float gsrMicrosiemens;

        public GsrDataPoint(long timestampNanos, long sessionOffsetNanos, float gsrMicrosiemens) {
            this.timestampNanos = timestampNanos;
            this.sessionOffsetNanos = sessionOffsetNanos;
            this.gsrMicrosiemens = gsrMicrosiemens;
        }
    }

    /**
     * Exports a recording session to the specified format.
     * 
     * @param sessionDirectory Path to the session directory
     * @param config Export configuration
     * @return Path to the exported file/directory
     * @throws IOException If export fails
     */
    public static String exportSession(String sessionDirectory, ExportConfig config) throws IOException {
        System.out.println("Starting export of session: " + sessionDirectory);

        // Load session data
        SessionData sessionData = loadSessionData(sessionDirectory);

        // Create output directory
        Path outputDir = Paths.get(config.outputDirectory);
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        // Generate output filename
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String outputFileName = sessionData.sessionId + "_export_" + timestamp;

        // Export based on format
        switch (config.format) {
            case MATLAB_MAT:
                return exportToMatlab(sessionData, config, outputFileName);
            case HDF5:
                return exportToHDF5(sessionData, config, outputFileName);
            case CSV_BUNDLE:
                return exportToCsvBundle(sessionData, config, outputFileName);
            default:
                throw new IllegalArgumentException("Unsupported export format: " + config.format);
        }
    }

    /**
     * Loads all data from a session directory.
     */
    private static SessionData loadSessionData(String sessionDirectory) throws IOException {
        SessionData sessionData = new SessionData();
        Path sessionPath = Paths.get(sessionDirectory);

        if (!Files.exists(sessionPath)) {
            throw new IOException("Session directory does not exist: " + sessionDirectory);
        }

        // Load metadata
        Path metadataFile = sessionPath.resolve("session_metadata.json");
        if (Files.exists(metadataFile)) {
            String metadataContent = new String(Files.readAllBytes(metadataFile));
            sessionData.metadataJson = metadataContent;

            // Simple JSON parsing for key fields
            sessionData.sessionId = extractJsonValue(metadataContent, "sessionId", "unknown");
            sessionData.sessionStartTime = Long.parseLong(extractJsonValue(metadataContent, "timestampEpochMillis", "0"));
            sessionData.deviceManufacturer = extractJsonValue(metadataContent, "manufacturer", "Unknown");
            sessionData.deviceModel = extractJsonValue(metadataContent, "model", "Unknown");
        }

        // Load GSR data
        loadGsrData(sessionPath, sessionData);

        // Load file lists
        loadMediaFiles(sessionPath, sessionData);

        System.out.println("Loaded session data: " + sessionData.sessionId);
        System.out.println("- GSR data points: " + sessionData.gsrData.size());
        System.out.println("- RGB videos: " + sessionData.rgbVideoFiles.size());
        System.out.println("- Raw images: " + sessionData.rawImageFiles.size());
        System.out.println("- Thermal frames: " + sessionData.thermalFrameFiles.size());
        System.out.println("- Audio files: " + sessionData.audioFiles.size());

        return sessionData;
    }

    /**
     * Simple JSON value extraction helper method.
     */
    private static String extractJsonValue(String json, String key, String defaultValue) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) return defaultValue;

            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) return defaultValue;

            int valueStart = colonIndex + 1;
            while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
                valueStart++;
            }

            if (valueStart >= json.length()) return defaultValue;

            char firstChar = json.charAt(valueStart);
            if (firstChar == '"') {
                // String value
                int valueEnd = json.indexOf('"', valueStart + 1);
                if (valueEnd == -1) return defaultValue;
                return json.substring(valueStart + 1, valueEnd);
            } else {
                // Number or boolean value
                int valueEnd = valueStart;
                while (valueEnd < json.length() && 
                       json.charAt(valueEnd) != ',' && 
                       json.charAt(valueEnd) != '}' && 
                       json.charAt(valueEnd) != '\n') {
                    valueEnd++;
                }
                return json.substring(valueStart, valueEnd).trim();
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Loads GSR data from CSV files.
     */
    private static void loadGsrData(Path sessionPath, SessionData sessionData) throws IOException {
        // Find GSR CSV files
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionPath, "GSR_*.csv")) {
            for (Path gsrFile : stream) {
                loadGsrCsvFile(gsrFile, sessionData);
            }
        }
    }

    /**
     * Loads GSR data from a single CSV file.
     */
    private static void loadGsrCsvFile(Path csvFile, SessionData sessionData) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    try {
                        long timestampNanos = Long.parseLong(parts[0]);
                        long sessionOffsetNanos = Long.parseLong(parts[1]);
                        float gsrValue = Float.parseFloat(parts[2]);

                        sessionData.gsrData.add(new GsrDataPoint(timestampNanos, sessionOffsetNanos, gsrValue));
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing GSR data line: " + line);
                    }
                }
            }
        }
    }

    /**
     * Loads lists of media files.
     */
    private static void loadMediaFiles(Path sessionPath, SessionData sessionData) throws IOException {
        // RGB video files
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionPath, "RGB_*.mp4")) {
            for (Path file : stream) {
                sessionData.rgbVideoFiles.add(file.toString());
            }
        }

        // Raw RGB images
        Path rawRgbDir = sessionPath.resolve("raw_rgb_" + sessionData.sessionId);
        if (Files.exists(rawRgbDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(rawRgbDir, "*.jpg")) {
                for (Path file : stream) {
                    sessionData.rawImageFiles.add(file.toString());
                }
            }
        }

        // Thermal frames
        Path thermalDir = sessionPath.resolve("thermal_" + sessionData.sessionId);
        if (Files.exists(thermalDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(thermalDir, "frame_*.jpg")) {
                for (Path file : stream) {
                    sessionData.thermalFrameFiles.add(file.toString());
                }
            }
        }

        // Audio files
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sessionPath, "Audio_*.wav")) {
            for (Path file : stream) {
                sessionData.audioFiles.add(file.toString());
            }
        }
    }

    /**
     * Exports session data to MATLAB .mat format.
     */
    private static String exportToMatlab(SessionData sessionData, ExportConfig config, String outputFileName) throws IOException {
        String outputPath = Paths.get(config.outputDirectory, outputFileName + ".mat").toString();

        // Create MATLAB script to generate .mat file
        String matlabScript = generateMatlabScript(sessionData, config, outputPath);
        String scriptPath = Paths.get(config.outputDirectory, outputFileName + "_generate.m").toString();

        try (FileWriter writer = new FileWriter(scriptPath)) {
            writer.write(matlabScript);
        }

        System.out.println("Generated MATLAB script: " + scriptPath);
        System.out.println("Run this script in MATLAB to create the .mat file: " + outputPath);

        return scriptPath;
    }

    /**
     * Generates MATLAB script to create .mat file.
     */
    private static String generateMatlabScript(SessionData sessionData, ExportConfig config, String outputPath) {
        StringBuilder script = new StringBuilder();

        script.append("%% GSR & Dual-Video Recording System - Data Export Script\n");
        script.append("%% Generated on: ").append(new Date()).append("\n");
        script.append("%% Session: ").append(sessionData.sessionId).append("\n\n");

        // Session metadata
        script.append("% Session metadata\n");
        script.append("session.id = '").append(sessionData.sessionId).append("';\n");
        script.append("session.start_time = ").append(sessionData.sessionStartTime).append(";\n");
        script.append("session.device_manufacturer = '").append(sessionData.deviceManufacturer).append("';\n");
        script.append("session.device_model = '").append(sessionData.deviceModel).append("';\n");

        // GSR data
        if (!sessionData.gsrData.isEmpty()) {
            script.append("\n% GSR data\n");
            script.append("gsr.timestamp_nanos = [");
            for (int i = 0; i < sessionData.gsrData.size(); i++) {
                if (i > 0) script.append("; ");
                script.append(sessionData.gsrData.get(i).timestampNanos);
            }
            script.append("];\n");

            script.append("gsr.session_offset_nanos = [");
            for (int i = 0; i < sessionData.gsrData.size(); i++) {
                if (i > 0) script.append("; ");
                script.append(sessionData.gsrData.get(i).sessionOffsetNanos);
            }
            script.append("];\n");

            script.append("gsr.microsiemens = [");
            for (int i = 0; i < sessionData.gsrData.size(); i++) {
                if (i > 0) script.append("; ");
                script.append(sessionData.gsrData.get(i).gsrMicrosiemens);
            }
            script.append("];\n");
        }

        // File paths
        script.append("\n% Media file paths\n");
        script.append("files.rgb_videos = {");
        for (int i = 0; i < sessionData.rgbVideoFiles.size(); i++) {
            if (i > 0) script.append("; ");
            script.append("'").append(sessionData.rgbVideoFiles.get(i).replace("\\", "\\\\")).append("'");
        }
        script.append("};\n");

        script.append("files.audio_files = {");
        for (int i = 0; i < sessionData.audioFiles.size(); i++) {
            if (i > 0) script.append("; ");
            script.append("'").append(sessionData.audioFiles.get(i).replace("\\", "\\\\")).append("'");
        }
        script.append("};\n");

        if (config.includeRawImages) {
            script.append("files.raw_images = {");
            for (int i = 0; i < Math.min(sessionData.rawImageFiles.size(), 100); i++) { // Limit to first 100 for performance
                if (i > 0) script.append("; ");
                script.append("'").append(sessionData.rawImageFiles.get(i).replace("\\", "\\\\")).append("'");
            }
            script.append("};\n");
        }

        if (config.includeThermalFrames) {
            script.append("files.thermal_frames = {");
            for (int i = 0; i < Math.min(sessionData.thermalFrameFiles.size(), 100); i++) { // Limit to first 100 for performance
                if (i > 0) script.append("; ");
                script.append("'").append(sessionData.thermalFrameFiles.get(i).replace("\\", "\\\\")).append("'");
            }
            script.append("};\n");
        }

        // Save command
        script.append("\n% Save to .mat file\n");
        script.append("save('").append(outputPath.replace("\\", "\\\\")).append("', 'session', 'gsr', 'files');\n");
        script.append("fprintf('Data exported to: ").append(outputPath.replace("\\", "\\\\")).append("\\n');\n");

        return script.toString();
    }

    /**
     * Exports session data to HDF5 format.
     */
    private static String exportToHDF5(SessionData sessionData, ExportConfig config, String outputFileName) throws IOException {
        String outputPath = Paths.get(config.outputDirectory, outputFileName + ".h5").toString();

        // Create Python script to generate HDF5 file
        String pythonScript = generatePythonScript(sessionData, config, outputPath);
        String scriptPath = Paths.get(config.outputDirectory, outputFileName + "_generate.py").toString();

        try (FileWriter writer = new FileWriter(scriptPath)) {
            writer.write(pythonScript);
        }

        System.out.println("Generated Python script: " + scriptPath);
        System.out.println("Run this script with h5py installed to create the HDF5 file: " + outputPath);

        return scriptPath;
    }

    /**
     * Generates Python script to create HDF5 file.
     */
    private static String generatePythonScript(SessionData sessionData, ExportConfig config, String outputPath) {
        StringBuilder script = new StringBuilder();

        script.append("#!/usr/bin/env python3\n");
        script.append("# GSR & Dual-Video Recording System - HDF5 Export Script\n");
        script.append("# Generated on: ").append(new Date()).append("\n");
        script.append("# Session: ").append(sessionData.sessionId).append("\n\n");

        script.append("import h5py\n");
        script.append("import numpy as np\n");
        script.append("import json\n\n");

        script.append("# Create HDF5 file\n");
        script.append("with h5py.File('").append(outputPath.replace("\\", "/")).append("', 'w') as f:\n");

        // Session metadata
        script.append("    # Session metadata\n");
        script.append("    session_group = f.create_group('session')\n");
        script.append("    session_group.attrs['id'] = '").append(sessionData.sessionId).append("'\n");
        script.append("    session_group.attrs['start_time'] = ").append(sessionData.sessionStartTime).append("\n");
        script.append("    session_group.attrs['device_manufacturer'] = '").append(sessionData.deviceManufacturer).append("'\n");
        script.append("    session_group.attrs['device_model'] = '").append(sessionData.deviceModel).append("'\n");

        // GSR data
        if (!sessionData.gsrData.isEmpty()) {
            script.append("\n    # GSR data\n");
            script.append("    gsr_group = f.create_group('gsr')\n");

            script.append("    timestamp_nanos = np.array([");
            for (int i = 0; i < sessionData.gsrData.size(); i++) {
                if (i > 0) script.append(", ");
                script.append(sessionData.gsrData.get(i).timestampNanos);
            }
            script.append("], dtype=np.int64)\n");
            script.append("    gsr_group.create_dataset('timestamp_nanos', data=timestamp_nanos)\n");

            script.append("    session_offset_nanos = np.array([");
            for (int i = 0; i < sessionData.gsrData.size(); i++) {
                if (i > 0) script.append(", ");
                script.append(sessionData.gsrData.get(i).sessionOffsetNanos);
            }
            script.append("], dtype=np.int64)\n");
            script.append("    gsr_group.create_dataset('session_offset_nanos', data=session_offset_nanos)\n");

            script.append("    microsiemens = np.array([");
            for (int i = 0; i < sessionData.gsrData.size(); i++) {
                if (i > 0) script.append(", ");
                script.append(sessionData.gsrData.get(i).gsrMicrosiemens);
            }
            script.append("], dtype=np.float32)\n");
            script.append("    gsr_group.create_dataset('microsiemens', data=microsiemens)\n");
        }

        // File paths
        script.append("\n    # Media file paths\n");
        script.append("    files_group = f.create_group('files')\n");

        if (!sessionData.rgbVideoFiles.isEmpty()) {
            script.append("    rgb_videos = [");
            for (int i = 0; i < sessionData.rgbVideoFiles.size(); i++) {
                if (i > 0) script.append(", ");
                script.append("'").append(sessionData.rgbVideoFiles.get(i).replace("\\", "/")).append("'");
            }
            script.append("]\n");
            script.append("    files_group.create_dataset('rgb_videos', data=rgb_videos)\n");
        }

        if (!sessionData.audioFiles.isEmpty()) {
            script.append("    audio_files = [");
            for (int i = 0; i < sessionData.audioFiles.size(); i++) {
                if (i > 0) script.append(", ");
                script.append("'").append(sessionData.audioFiles.get(i).replace("\\", "/")).append("'");
            }
            script.append("]\n");
            script.append("    files_group.create_dataset('audio_files', data=audio_files)\n");
        }

        script.append("\nprint(f'Data exported to: ").append(outputPath.replace("\\", "/")).append("')\n");

        return script.toString();
    }

    /**
     * Exports session data to organized CSV bundle.
     */
    private static String exportToCsvBundle(SessionData sessionData, ExportConfig config, String outputFileName) throws IOException {
        Path outputDir = Paths.get(config.outputDirectory, outputFileName + "_csv_bundle");
        Files.createDirectories(outputDir);

        // Export metadata
        if (sessionData.metadataJson != null) {
            Path metadataFile = outputDir.resolve("session_metadata.json");
            try (FileWriter writer = new FileWriter(metadataFile.toFile())) {
                writer.write(sessionData.metadataJson);
            }
        }

        // Export GSR data
        if (!sessionData.gsrData.isEmpty()) {
            Path gsrFile = outputDir.resolve("gsr_data.csv");
            try (FileWriter writer = new FileWriter(gsrFile.toFile())) {
                writer.write("timestamp_nanos,session_offset_nanos,gsr_microsiemens\n");
                for (GsrDataPoint point : sessionData.gsrData) {
                    writer.write(String.format("%d,%d,%.6f\n", 
                        point.timestampNanos, point.sessionOffsetNanos, point.gsrMicrosiemens));
                }
            }
        }

        // Export file lists
        exportFileList(outputDir.resolve("rgb_videos.txt"), sessionData.rgbVideoFiles);
        exportFileList(outputDir.resolve("audio_files.txt"), sessionData.audioFiles);

        if (config.includeRawImages) {
            exportFileList(outputDir.resolve("raw_images.txt"), sessionData.rawImageFiles);
        }

        if (config.includeThermalFrames) {
            exportFileList(outputDir.resolve("thermal_frames.txt"), sessionData.thermalFrameFiles);
        }

        System.out.println("CSV bundle exported to: " + outputDir.toString());
        return outputDir.toString();
    }


    /**
     * Helper method to export a list of file paths to a text file.
     */
    private static void exportFileList(Path outputFile, List<String> files) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile.toFile())) {
            for (String file : files) {
                writer.write(file + "\n");
            }
        }
    }

    /**
     * Main method for command-line usage.
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java DataExporter <session_directory> <format> [output_directory]");
            System.out.println("Formats: MATLAB_MAT, HDF5, CSV_BUNDLE");
            return;
        }

        String sessionDirectory = args[0];
        String formatStr = args[1];

        try {
            ExportFormat format = ExportFormat.valueOf(formatStr.toUpperCase());
            ExportConfig config = new ExportConfig(format);

            if (args.length > 2) {
                config.outputDirectory = args[2];
            }

            String result = exportSession(sessionDirectory, config);
            System.out.println("Export completed: " + result);

        } catch (IllegalArgumentException e) {
            System.err.println("Invalid format: " + formatStr);
            System.err.println("Valid formats: MATLAB_MAT, HDF5, CSV_BUNDLE");
        } catch (IOException e) {
            System.err.println("Export failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
