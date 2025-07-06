"""
Camera Calibration Dialog for FYP-GSR System

This module provides a user-friendly GUI dialog for camera calibration
functionality. It integrates with the camera_calibration module to provide
an intuitive interface for selecting videos, configuring calibration
parameters, and running calibration routines.

Author: FYP-GSR Team
"""

import os
import sys
import logging
from typing import Dict, List, Optional
from pathlib import Path

from PySide6.QtWidgets import (
    QDialog, QVBoxLayout, QHBoxLayout, QGridLayout, QGroupBox,
    QPushButton, QLabel, QLineEdit, QComboBox, QSpinBox, QDoubleSpinBox,
    QFileDialog, QProgressBar, QTextEdit, QTabWidget, QWidget,
    QCheckBox, QMessageBox, QFormLayout, QScrollArea
)
from PySide6.QtCore import Qt, QThread, Signal, QTimer
from PySide6.QtGui import QFont, QPixmap, QIcon

# Import our calibration module
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.camera_calibration import CameraCalibrator, CalibrationPattern
from utils.logger import get_logger


class CalibrationWorker(QThread):
    """Worker thread for running calibration in the background."""
    
    # Signals
    progress_updated = Signal(int)  # Progress percentage
    status_updated = Signal(str)   # Status message
    calibration_completed = Signal(dict)  # Calibration results
    calibration_failed = Signal(str)      # Error message
    
    def __init__(self, calibrator: CameraCalibrator, calibration_config: Dict):
        super().__init__()
        self.calibrator = calibrator
        self.config = calibration_config
        self.logger = get_logger(__name__)
        
    def run(self):
        """Run calibration in background thread."""
        try:
            self.status_updated.emit("Starting calibration process...")
            self.progress_updated.emit(10)
            
            total_cameras = len([k for k in self.config.keys() if k.endswith('_path') and self.config[k]])
            current_camera = 0
            
            # Process RGB camera
            if self.config.get('rgb_video_path'):
                self.status_updated.emit("Processing RGB camera...")
                rgb_images = self.calibrator.extract_frames_from_video(
                    self.config['rgb_video_path'], 
                    self.config.get('max_frames', 50)
                )
                if rgb_images:
                    self.calibrator.calibrate_camera_intrinsics('rgb_camera', rgb_images)
                current_camera += 1
                self.progress_updated.emit(int(30 + (current_camera / total_cameras) * 40))
            
            # Process RGB frames directory
            elif self.config.get('rgb_frames_path'):
                self.status_updated.emit("Processing RGB frames...")
                rgb_images = self.calibrator.load_image_sequence(self.config['rgb_frames_path'])
                if rgb_images:
                    self.calibrator.calibrate_camera_intrinsics('rgb_camera', rgb_images)
                current_camera += 1
                self.progress_updated.emit(int(30 + (current_camera / total_cameras) * 40))
            
            # Process thermal camera
            if self.config.get('thermal_frames_path'):
                self.status_updated.emit("Processing thermal camera...")
                thermal_images = self.calibrator.load_image_sequence(self.config['thermal_frames_path'])
                if thermal_images:
                    self.calibrator.calibrate_camera_intrinsics('thermal_camera', thermal_images)
                current_camera += 1
                self.progress_updated.emit(int(30 + (current_camera / total_cameras) * 40))
            
            # Process webcam
            if self.config.get('webcam_video_path'):
                self.status_updated.emit("Processing webcam...")
                webcam_images = self.calibrator.extract_frames_from_video(
                    self.config['webcam_video_path'],
                    self.config.get('max_frames', 50)
                )
                if webcam_images:
                    self.calibrator.calibrate_camera_intrinsics('webcam_camera', webcam_images)
                current_camera += 1
                self.progress_updated.emit(int(30 + (current_camera / total_cameras) * 40))
            
            self.progress_updated.emit(70)
            
            # Perform stereo calibration if enabled and multiple cameras available
            camera_names = list(self.calibrator.cameras.keys())
            if len(camera_names) >= 2 and self.config.get('enable_stereo', False):
                self.status_updated.emit("Performing stereo calibration...")
                
                # RGB to thermal stereo calibration
                if 'rgb_camera' in camera_names and 'thermal_camera' in camera_names:
                    if self.config.get('rgb_video_path') and self.config.get('thermal_frames_path'):
                        rgb_images = self.calibrator.extract_frames_from_video(
                            self.config['rgb_video_path'], self.config.get('max_frames', 50)
                        )
                        thermal_images = self.calibrator.load_image_sequence(
                            self.config['thermal_frames_path']
                        )
                        
                        min_images = min(len(rgb_images), len(thermal_images))
                        if min_images >= 5:
                            self.calibrator.calibrate_stereo_extrinsics(
                                'rgb_camera', 'thermal_camera',
                                rgb_images[:min_images], thermal_images[:min_images]
                            )
            
            self.progress_updated.emit(90)
            
            # Save results
            if self.config.get('output_path'):
                self.status_updated.emit("Saving calibration results...")
                self.calibrator.save_calibration_results(self.config['output_path'])
            
            self.progress_updated.emit(100)
            self.status_updated.emit("Calibration completed successfully!")
            
            # Emit results
            self.calibration_completed.emit(self.calibrator.cameras)
            
        except Exception as e:
            self.logger.error(f"Calibration failed: {str(e)}")
            self.calibration_failed.emit(str(e))


class CalibrationDialog(QDialog):
    """Main calibration dialog window."""
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.logger = get_logger(__name__)
        self.calibrator = None
        self.worker = None
        
        self.setWindowTitle("Camera Calibration Tool")
        self.setMinimumSize(800, 600)
        self.setModal(True)
        
        self.setup_ui()
        self.connect_signals()
        
    def setup_ui(self):
        """Set up the user interface."""
        layout = QVBoxLayout(self)
        
        # Create tab widget for different sections
        self.tabs = QTabWidget()
        layout.addWidget(self.tabs)
        
        # Input Sources Tab
        self.create_input_tab()
        
        # Pattern Configuration Tab
        self.create_pattern_tab()
        
        # Advanced Options Tab
        self.create_advanced_tab()
        
        # Results Tab
        self.create_results_tab()
        
        # Progress and control section
        self.create_control_section(layout)
        
    def create_input_tab(self):
        """Create the input sources configuration tab."""
        input_widget = QWidget()
        layout = QVBoxLayout(input_widget)
        
        # RGB Camera Section
        rgb_group = QGroupBox("RGB Camera")
        rgb_layout = QFormLayout(rgb_group)
        
        # RGB Video input
        rgb_video_layout = QHBoxLayout()
        self.rgb_video_edit = QLineEdit()
        self.rgb_video_edit.setPlaceholderText("Select RGB video file (MP4)...")
        rgb_video_button = QPushButton("Browse")
        rgb_video_button.clicked.connect(self.browse_rgb_video)
        rgb_video_layout.addWidget(self.rgb_video_edit)
        rgb_video_layout.addWidget(rgb_video_button)
        rgb_layout.addRow("RGB Video:", rgb_video_layout)
        
        # RGB Frames input (alternative)
        rgb_frames_layout = QHBoxLayout()
        self.rgb_frames_edit = QLineEdit()
        self.rgb_frames_edit.setPlaceholderText("Or select RGB frames directory...")
        rgb_frames_button = QPushButton("Browse")
        rgb_frames_button.clicked.connect(self.browse_rgb_frames)
        rgb_frames_layout.addWidget(self.rgb_frames_edit)
        rgb_frames_layout.addWidget(rgb_frames_button)
        rgb_layout.addRow("RGB Frames:", rgb_frames_layout)
        
        layout.addWidget(rgb_group)
        
        # Thermal Camera Section
        thermal_group = QGroupBox("Thermal Camera")
        thermal_layout = QFormLayout(thermal_group)
        
        thermal_frames_layout = QHBoxLayout()
        self.thermal_frames_edit = QLineEdit()
        self.thermal_frames_edit.setPlaceholderText("Select thermal frames directory...")
        thermal_frames_button = QPushButton("Browse")
        thermal_frames_button.clicked.connect(self.browse_thermal_frames)
        thermal_frames_layout.addWidget(self.thermal_frames_edit)
        thermal_frames_layout.addWidget(thermal_frames_button)
        thermal_layout.addRow("Thermal Frames:", thermal_frames_layout)
        
        layout.addWidget(thermal_group)
        
        # Webcam Section
        webcam_group = QGroupBox("Webcam (Optional)")
        webcam_layout = QFormLayout(webcam_group)
        
        webcam_video_layout = QHBoxLayout()
        self.webcam_video_edit = QLineEdit()
        self.webcam_video_edit.setPlaceholderText("Select webcam video file...")
        webcam_video_button = QPushButton("Browse")
        webcam_video_button.clicked.connect(self.browse_webcam_video)
        webcam_video_layout.addWidget(self.webcam_video_edit)
        webcam_video_layout.addWidget(webcam_video_button)
        webcam_layout.addRow("Webcam Video:", webcam_video_layout)
        
        layout.addWidget(webcam_group)
        
        # Auto-detect session button
        auto_detect_layout = QHBoxLayout()
        self.auto_detect_button = QPushButton("🔍 Auto-Detect from Session Folder")
        self.auto_detect_button.clicked.connect(self.auto_detect_session)
        auto_detect_layout.addWidget(self.auto_detect_button)
        auto_detect_layout.addStretch()
        layout.addLayout(auto_detect_layout)
        
        layout.addStretch()
        self.tabs.addTab(input_widget, "Input Sources")
        
    def create_pattern_tab(self):
        """Create the calibration pattern configuration tab."""
        pattern_widget = QWidget()
        layout = QVBoxLayout(pattern_widget)
        
        pattern_group = QGroupBox("Calibration Pattern")
        pattern_layout = QFormLayout(pattern_group)
        
        # Pattern type
        self.pattern_combo = QComboBox()
        self.pattern_combo.addItems(["chessboard", "charuco"])
        self.pattern_combo.setCurrentText("chessboard")
        pattern_layout.addRow("Pattern Type:", self.pattern_combo)
        
        # Grid size
        grid_layout = QHBoxLayout()
        self.grid_width_spin = QSpinBox()
        self.grid_width_spin.setRange(3, 20)
        self.grid_width_spin.setValue(9)
        self.grid_height_spin = QSpinBox()
        self.grid_height_spin.setRange(3, 20)
        self.grid_height_spin.setValue(6)
        grid_layout.addWidget(QLabel("Width:"))
        grid_layout.addWidget(self.grid_width_spin)
        grid_layout.addWidget(QLabel("Height:"))
        grid_layout.addWidget(self.grid_height_spin)
        grid_layout.addStretch()
        pattern_layout.addRow("Grid Size:", grid_layout)
        
        # Square size
        self.square_size_spin = QDoubleSpinBox()
        self.square_size_spin.setRange(0.001, 1.0)
        self.square_size_spin.setValue(0.025)
        self.square_size_spin.setSuffix(" m")
        self.square_size_spin.setDecimals(3)
        pattern_layout.addRow("Square Size:", self.square_size_spin)
        
        # Marker size (for ChArUco)
        self.marker_size_spin = QDoubleSpinBox()
        self.marker_size_spin.setRange(0.001, 1.0)
        self.marker_size_spin.setValue(0.020)
        self.marker_size_spin.setSuffix(" m")
        self.marker_size_spin.setDecimals(3)
        self.marker_size_spin.setEnabled(False)
        pattern_layout.addRow("Marker Size:", self.marker_size_spin)
        
        # Connect pattern type change
        self.pattern_combo.currentTextChanged.connect(self.on_pattern_type_changed)
        
        layout.addWidget(pattern_group)
        
        # Pattern preview/instructions
        instructions_group = QGroupBox("Instructions")
        instructions_layout = QVBoxLayout(instructions_group)
        
        self.instructions_text = QTextEdit()
        self.instructions_text.setMaximumHeight(150)
        self.instructions_text.setReadOnly(True)
        self.update_instructions()
        instructions_layout.addWidget(self.instructions_text)
        
        layout.addWidget(instructions_group)
        layout.addStretch()
        
        self.tabs.addTab(pattern_widget, "Pattern Config")
        
    def create_advanced_tab(self):
        """Create the advanced options tab."""
        advanced_widget = QWidget()
        layout = QVBoxLayout(advanced_widget)
        
        # Processing options
        processing_group = QGroupBox("Processing Options")
        processing_layout = QFormLayout(processing_group)
        
        self.max_frames_spin = QSpinBox()
        self.max_frames_spin.setRange(10, 200)
        self.max_frames_spin.setValue(50)
        processing_layout.addRow("Max Frames per Video:", self.max_frames_spin)
        
        self.enable_stereo_check = QCheckBox("Enable Stereo Calibration")
        self.enable_stereo_check.setChecked(True)
        processing_layout.addRow("", self.enable_stereo_check)
        
        layout.addWidget(processing_group)
        
        # Output options
        output_group = QGroupBox("Output Options")
        output_layout = QFormLayout(output_group)
        
        output_layout_h = QHBoxLayout()
        self.output_edit = QLineEdit()
        self.output_edit.setPlaceholderText("Select output file for calibration results...")
        output_button = QPushButton("Browse")
        output_button.clicked.connect(self.browse_output_file)
        output_layout_h.addWidget(self.output_edit)
        output_layout_h.addWidget(output_button)
        output_layout.addRow("Output File:", output_layout_h)
        
        layout.addWidget(output_group)
        layout.addStretch()
        
        self.tabs.addTab(advanced_widget, "Advanced")
        
    def create_results_tab(self):
        """Create the results display tab."""
        results_widget = QWidget()
        layout = QVBoxLayout(results_widget)
        
        # Results display
        self.results_text = QTextEdit()
        self.results_text.setReadOnly(True)
        self.results_text.setFont(QFont("Courier", 10))
        layout.addWidget(self.results_text)
        
        # Export buttons
        export_layout = QHBoxLayout()
        self.export_json_button = QPushButton("Export JSON")
        self.export_json_button.clicked.connect(self.export_json)
        self.export_json_button.setEnabled(False)
        
        self.view_results_button = QPushButton("View Detailed Results")
        self.view_results_button.clicked.connect(self.view_detailed_results)
        self.view_results_button.setEnabled(False)
        
        export_layout.addWidget(self.export_json_button)
        export_layout.addWidget(self.view_results_button)
        export_layout.addStretch()
        layout.addLayout(export_layout)
        
        self.tabs.addTab(results_widget, "Results")
        
    def create_control_section(self, parent_layout):
        """Create the control section with progress and buttons."""
        control_group = QGroupBox("Calibration Control")
        control_layout = QVBoxLayout(control_group)
        
        # Progress bar
        self.progress_bar = QProgressBar()
        self.progress_bar.setVisible(False)
        control_layout.addWidget(self.progress_bar)
        
        # Status label
        self.status_label = QLabel("Ready to start calibration")
        control_layout.addWidget(self.status_label)
        
        # Buttons
        button_layout = QHBoxLayout()
        
        self.start_button = QPushButton("🚀 Start Calibration")
        self.start_button.clicked.connect(self.start_calibration)
        self.start_button.setMinimumHeight(40)
        
        self.cancel_button = QPushButton("❌ Cancel")
        self.cancel_button.clicked.connect(self.cancel_calibration)
        self.cancel_button.setEnabled(False)
        
        self.close_button = QPushButton("Close")
        self.close_button.clicked.connect(self.close)
        
        button_layout.addWidget(self.start_button)
        button_layout.addWidget(self.cancel_button)
        button_layout.addStretch()
        button_layout.addWidget(self.close_button)
        
        control_layout.addLayout(button_layout)
        parent_layout.addWidget(control_group)
        
    def connect_signals(self):
        """Connect internal signals."""
        pass
        
    def on_pattern_type_changed(self, pattern_type):
        """Handle pattern type change."""
        is_charuco = pattern_type == "charuco"
        self.marker_size_spin.setEnabled(is_charuco)
        self.update_instructions()
        
    def update_instructions(self):
        """Update the calibration instructions based on pattern type."""
        pattern_type = self.pattern_combo.currentText()
        
        if pattern_type == "chessboard":
            instructions = """
<b>Chessboard Calibration Instructions:</b><br>
1. Print a chessboard pattern with the specified grid size<br>
2. Ensure the pattern is flat and well-lit during recording<br>
3. Move the pattern to different positions and orientations<br>
4. Capture at least 10-15 good views of the pattern<br>
5. Avoid motion blur and ensure pattern is fully visible<br>
<br>
<b>Tips:</b><br>
• Use a rigid backing (cardboard, clipboard)<br>
• Vary distance and angles for better calibration<br>
• Ensure pattern fills 20-80% of the image
            """
        else:  # charuco
            instructions = """
<b>ChArUco Calibration Instructions:</b><br>
1. Print a ChArUco board with the specified parameters<br>
2. ChArUco boards are more robust than chessboards<br>
3. Partial occlusion is acceptable with ChArUco<br>
4. Move the pattern to different positions and orientations<br>
5. Capture at least 10-15 good views of the pattern<br>
<br>
<b>Advantages:</b><br>
• Works better in thermal imaging<br>
• More robust to partial occlusion<br>
• Better corner detection accuracy
            """
            
        self.instructions_text.setHtml(instructions)
        
    def browse_rgb_video(self):
        """Browse for RGB video file."""
        file_path, _ = QFileDialog.getOpenFileName(
            self, "Select RGB Video File", "", 
            "Video Files (*.mp4 *.avi *.mov *.mkv);;All Files (*)"
        )
        if file_path:
            self.rgb_video_edit.setText(file_path)
            self.rgb_frames_edit.clear()  # Clear alternative input
            
    def browse_rgb_frames(self):
        """Browse for RGB frames directory."""
        dir_path = QFileDialog.getExistingDirectory(
            self, "Select RGB Frames Directory"
        )
        if dir_path:
            self.rgb_frames_edit.setText(dir_path)
            self.rgb_video_edit.clear()  # Clear alternative input
            
    def browse_thermal_frames(self):
        """Browse for thermal frames directory."""
        dir_path = QFileDialog.getExistingDirectory(
            self, "Select Thermal Frames Directory"
        )
        if dir_path:
            self.thermal_frames_edit.setText(dir_path)
            
    def browse_webcam_video(self):
        """Browse for webcam video file."""
        file_path, _ = QFileDialog.getOpenFileName(
            self, "Select Webcam Video File", "",
            "Video Files (*.mp4 *.avi *.mov *.mkv);;All Files (*)"
        )
        if file_path:
            self.webcam_video_edit.setText(file_path)
            
    def browse_output_file(self):
        """Browse for output calibration file."""
        file_path, _ = QFileDialog.getSaveFileName(
            self, "Save Calibration Results", "calibration_results.json",
            "JSON Files (*.json);;All Files (*)"
        )
        if file_path:
            self.output_edit.setText(file_path)
            
    def auto_detect_session(self):
        """Auto-detect calibration files from a session folder."""
        session_dir = QFileDialog.getExistingDirectory(
            self, "Select Session Directory"
        )
        if not session_dir:
            return
            
        session_path = Path(session_dir)
        
        # Look for common file patterns
        rgb_video = None
        rgb_frames = None
        thermal_frames = None
        webcam_video = None
        
        # Search for RGB video
        for pattern in ["*rgb*.mp4", "*video*.mp4", "*.mp4"]:
            matches = list(session_path.glob(pattern))
            if matches:
                rgb_video = str(matches[0])
                break
                
        # Search for RGB frames directory
        for pattern in ["rgb_frames", "*rgb*", "frames"]:
            matches = list(session_path.glob(pattern))
            if matches and matches[0].is_dir():
                rgb_frames = str(matches[0])
                break
                
        # Search for thermal frames directory
        for pattern in ["thermal_frames", "*thermal*", "thermal"]:
            matches = list(session_path.glob(pattern))
            if matches and matches[0].is_dir():
                thermal_frames = str(matches[0])
                break
                
        # Search for webcam video
        for pattern in ["*webcam*.mp4", "*brio*.mp4", "*usb*.mp4"]:
            matches = list(session_path.glob(pattern))
            if matches:
                webcam_video = str(matches[0])
                break
        
        # Update UI fields
        if rgb_video:
            self.rgb_video_edit.setText(rgb_video)
        elif rgb_frames:
            self.rgb_frames_edit.setText(rgb_frames)
            
        if thermal_frames:
            self.thermal_frames_edit.setText(thermal_frames)
            
        if webcam_video:
            self.webcam_video_edit.setText(webcam_video)
            
        # Set default output path
        if not self.output_edit.text():
            output_path = session_path / "calibration_results.json"
            self.output_edit.setText(str(output_path))
            
        # Show summary
        found_items = []
        if rgb_video or rgb_frames:
            found_items.append("RGB camera")
        if thermal_frames:
            found_items.append("Thermal camera")
        if webcam_video:
            found_items.append("Webcam")
            
        if found_items:
            QMessageBox.information(
                self, "Auto-Detection Results",
                f"Found calibration data for: {', '.join(found_items)}"
            )
        else:
            QMessageBox.warning(
                self, "Auto-Detection Results",
                "No calibration data found in the selected directory."
            )
            
    def validate_inputs(self) -> bool:
        """Validate user inputs before starting calibration."""
        # Check if at least one camera input is provided
        has_rgb = bool(self.rgb_video_edit.text() or self.rgb_frames_edit.text())
        has_thermal = bool(self.thermal_frames_edit.text())
        has_webcam = bool(self.webcam_video_edit.text())
        
        if not (has_rgb or has_thermal or has_webcam):
            QMessageBox.warning(
                self, "Input Error",
                "Please select at least one camera input (RGB, thermal, or webcam)."
            )
            return False
            
        # Check if output path is specified
        if not self.output_edit.text():
            QMessageBox.warning(
                self, "Output Error",
                "Please specify an output file for calibration results."
            )
            return False
            
        # Validate file paths exist
        paths_to_check = [
            (self.rgb_video_edit.text(), "RGB video"),
            (self.rgb_frames_edit.text(), "RGB frames directory"),
            (self.thermal_frames_edit.text(), "Thermal frames directory"),
            (self.webcam_video_edit.text(), "Webcam video")
        ]
        
        for path, name in paths_to_check:
            if path and not os.path.exists(path):
                QMessageBox.warning(
                    self, "File Error",
                    f"The specified {name} path does not exist:\n{path}"
                )
                return False
                
        return True
        
    def start_calibration(self):
        """Start the calibration process."""
        if not self.validate_inputs():
            return
            
        # Create calibration pattern
        pattern = CalibrationPattern(
            self.pattern_combo.currentText(),
            (self.grid_width_spin.value(), self.grid_height_spin.value()),
            self.square_size_spin.value(),
            self.marker_size_spin.value() if self.pattern_combo.currentText() == "charuco" else None
        )
        
        # Create calibrator
        self.calibrator = CameraCalibrator(pattern, self.logger)
        
        # Prepare configuration
        config = {
            'rgb_video_path': self.rgb_video_edit.text(),
            'rgb_frames_path': self.rgb_frames_edit.text(),
            'thermal_frames_path': self.thermal_frames_edit.text(),
            'webcam_video_path': self.webcam_video_edit.text(),
            'output_path': self.output_edit.text(),
            'max_frames': self.max_frames_spin.value(),
            'enable_stereo': self.enable_stereo_check.isChecked()
        }
        
        # Create and start worker thread
        self.worker = CalibrationWorker(self.calibrator, config)
        self.worker.progress_updated.connect(self.progress_bar.setValue)
        self.worker.status_updated.connect(self.status_label.setText)
        self.worker.calibration_completed.connect(self.on_calibration_completed)
        self.worker.calibration_failed.connect(self.on_calibration_failed)
        
        # Update UI state
        self.start_button.setEnabled(False)
        self.cancel_button.setEnabled(True)
        self.progress_bar.setVisible(True)
        self.progress_bar.setValue(0)
        
        # Switch to results tab
        self.tabs.setCurrentIndex(3)
        
        # Start calibration
        self.worker.start()
        
    def cancel_calibration(self):
        """Cancel the ongoing calibration."""
        if self.worker and self.worker.isRunning():
            self.worker.terminate()
            self.worker.wait()
            
        self.reset_ui_state()
        self.status_label.setText("Calibration cancelled")
        
    def on_calibration_completed(self, results: Dict):
        """Handle successful calibration completion."""
        self.reset_ui_state()
        
        # Display results summary
        summary = self.format_calibration_summary(results)
        self.results_text.setPlainText(summary)
        
        # Enable export buttons
        self.export_json_button.setEnabled(True)
        self.view_results_button.setEnabled(True)
        
        QMessageBox.information(
            self, "Calibration Complete",
            "Camera calibration completed successfully!\nResults are displayed in the Results tab."
        )
        
    def on_calibration_failed(self, error_message: str):
        """Handle calibration failure."""
        self.reset_ui_state()
        self.status_label.setText(f"Calibration failed: {error_message}")
        
        QMessageBox.critical(
            self, "Calibration Failed",
            f"Calibration failed with error:\n\n{error_message}"
        )
        
    def reset_ui_state(self):
        """Reset UI to initial state."""
        self.start_button.setEnabled(True)
        self.cancel_button.setEnabled(False)
        self.progress_bar.setVisible(False)
        
    def format_calibration_summary(self, results: Dict) -> str:
        """Format calibration results for display."""
        summary = "CAMERA CALIBRATION RESULTS\n"
        summary += "=" * 50 + "\n\n"
        
        # Intrinsic calibration results
        for camera_name, data in results.items():
            if camera_name == 'extrinsics':
                continue
                
            summary += f"{camera_name.upper()} CAMERA:\n"
            summary += f"  Image Size: {data['image_size']}\n"
            summary += f"  Reprojection Error: {data['reprojection_error']:.3f} pixels\n"
            summary += f"  Images Used: {data['num_images_used']}\n"
            summary += f"  Calibration Date: {data['calibration_date']}\n"
            
            # Camera matrix
            K = data['camera_matrix']
            summary += f"  Camera Matrix:\n"
            summary += f"    [{K[0][0]:.2f}, {K[0][1]:.2f}, {K[0][2]:.2f}]\n"
            summary += f"    [{K[1][0]:.2f}, {K[1][1]:.2f}, {K[1][2]:.2f}]\n"
            summary += f"    [{K[2][0]:.2f}, {K[2][1]:.2f}, {K[2][2]:.2f}]\n"
            
            # Distortion coefficients
            D = data['distortion_coefficients'][0]
            summary += f"  Distortion: [{D[0]:.6f}, {D[1]:.6f}, {D[2]:.6f}, {D[3]:.6f}, {D[4]:.6f}]\n\n"
        
        # Extrinsic calibration results
        if 'extrinsics' in results:
            summary += "STEREO CALIBRATION:\n"
            for pair_name, data in results['extrinsics'].items():
                summary += f"  {pair_name}:\n"
                summary += f"    Reprojection Error: {data['reprojection_error']:.3f} pixels\n"
                summary += f"    Image Pairs Used: {data['num_image_pairs_used']}\n"
                
                # Translation vector
                T = data['translation_vector']
                summary += f"    Translation: [{T[0][0]:.3f}, {T[1][0]:.3f}, {T[2][0]:.3f}] meters\n\n"
        
        return summary
        
    def export_json(self):
        """Export calibration results to JSON file."""
        if not self.calibrator:
            return
            
        file_path, _ = QFileDialog.getSaveFileName(
            self, "Export Calibration Results", "calibration_export.json",
            "JSON Files (*.json);;All Files (*)"
        )
        
        if file_path:
            try:
                self.calibrator.save_calibration_results(file_path)
                QMessageBox.information(
                    self, "Export Successful",
                    f"Calibration results exported to:\n{file_path}"
                )
            except Exception as e:
                QMessageBox.critical(
                    self, "Export Failed",
                    f"Failed to export calibration results:\n{str(e)}"
                )
                
    def view_detailed_results(self):
        """Open detailed results in a new window."""
        if not self.calibrator:
            return
            
        # Create detailed results dialog
        dialog = QDialog(self)
        dialog.setWindowTitle("Detailed Calibration Results")
        dialog.setMinimumSize(600, 400)
        
        layout = QVBoxLayout(dialog)
        
        text_edit = QTextEdit()
        text_edit.setReadOnly(True)
        text_edit.setFont(QFont("Courier", 9))
        
        # Format detailed results
        import json
        detailed_results = {
            'pattern_info': {
                'type': self.calibrator.pattern.pattern_type,
                'grid_size': self.calibrator.pattern.grid_size,
                'square_size': self.calibrator.pattern.square_size,
                'marker_size': self.calibrator.pattern.marker_size
            },
            'cameras': self.calibrator.cameras
        }
        
        text_edit.setPlainText(json.dumps(detailed_results, indent=2))
        layout.addWidget(text_edit)
        
        # Close button
        close_button = QPushButton("Close")
        close_button.clicked.connect(dialog.close)
        layout.addWidget(close_button)
        
        dialog.exec_()