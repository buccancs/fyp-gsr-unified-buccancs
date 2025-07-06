"""
Live Calibration Dialog with Real-time Feedback for FYP-GSR System

This module provides an enhanced calibration dialog that integrates real-time
feedback during calibration data recording. It shows live video preview with
pattern detection feedback, helping researchers capture optimal calibration data.

Features:
- Live video preview with pattern detection overlay
- Real-time quality feedback and recommendations
- Session management with automatic quality tracking
- Integration with existing camera calibration system
- Support for multiple camera sources

Author: FYP-GSR Team
"""

import os
import sys
import logging
import cv2
import numpy as np
from typing import Dict, List, Optional, Tuple
from pathlib import Path
import time
import threading
from queue import Queue, Empty

from PySide6.QtWidgets import (
    QDialog, QVBoxLayout, QHBoxLayout, QGridLayout, QGroupBox,
    QPushButton, QLabel, QLineEdit, QComboBox, QSpinBox, QDoubleSpinBox,
    QFileDialog, QProgressBar, QTextEdit, QTabWidget, QWidget,
    QCheckBox, QMessageBox, QFormLayout, QScrollArea, QSplitter,
    QFrame, QSlider, QListWidget, QListWidgetItem
)
from PySide6.QtCore import Qt, QThread, Signal, QTimer, pyqtSignal
from PySide6.QtGui import QFont, QPixmap, QIcon, QImage, QPainter, QPen, QBrush

# Import our modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from utils.camera_calibration import CameraCalibrator, CalibrationPattern
from utils.realtime_calibration_feedback import (
    RealtimeCalibrationFeedback, PatternFeedback, PatternQuality,
    CalibrationFeedbackVisualizer
)
from utils.logger import get_logger


class VideoPreviewWidget(QLabel):
    """Widget for displaying live video preview with calibration feedback."""
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setMinimumSize(640, 480)
        self.setStyleSheet("border: 2px solid gray; background-color: black;")
        self.setAlignment(Qt.AlignCenter)
        self.setText("No Video Feed")
        
        # Video properties
        self.current_frame = None
        self.feedback_overlay = True
        self.scale_factor = 1.0
        
    def update_frame(self, frame: np.ndarray, feedback: PatternFeedback = None):
        """Update the displayed frame with optional feedback overlay."""
        if frame is None:
            return
            
        self.current_frame = frame.copy()
        
        # Apply feedback overlay if enabled
        if self.feedback_overlay and feedback:
            frame = CalibrationFeedbackVisualizer.draw_feedback_overlay(frame, feedback)
            
        # Convert to Qt format and display
        self._display_frame(frame)
        
    def _display_frame(self, frame: np.ndarray):
        """Convert frame to Qt format and display."""
        height, width, channel = frame.shape
        bytes_per_line = 3 * width
        
        # Convert BGR to RGB
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        
        # Create QImage
        q_image = QImage(rgb_frame.data, width, height, bytes_per_line, QImage.Format_RGB888)
        
        # Scale to fit widget
        widget_size = self.size()
        scaled_pixmap = QPixmap.fromImage(q_image).scaled(
            widget_size, Qt.KeepAspectRatio, Qt.SmoothTransformation
        )
        
        self.setPixmap(scaled_pixmap)
        
    def toggle_feedback_overlay(self, enabled: bool):
        """Toggle feedback overlay on/off."""
        self.feedback_overlay = enabled
        
    def save_current_frame(self, filepath: str) -> bool:
        """Save the current frame to file."""
        if self.current_frame is not None:
            return cv2.imwrite(filepath, self.current_frame)
        return False


class LiveCalibrationWorker(QThread):
    """Worker thread for live calibration feedback processing."""
    
    # Signals
    frame_processed = Signal(np.ndarray, object)  # frame, feedback
    statistics_updated = Signal(dict)
    error_occurred = Signal(str)
    
    def __init__(self, video_source: str, pattern: CalibrationPattern):
        super().__init__()
        self.video_source = video_source
        self.pattern = pattern
        self.logger = get_logger(__name__)
        
        # Processing components
        self.feedback_system = RealtimeCalibrationFeedback(pattern, self.logger)
        self.cap = None
        self.is_running = False
        self.frame_skip = 2  # Process every nth frame for performance
        self.frame_count = 0
        
    def run(self):
        """Main processing loop."""
        try:
            # Initialize video capture
            if self.video_source.isdigit():
                self.cap = cv2.VideoCapture(int(self.video_source))
            else:
                self.cap = cv2.VideoCapture(self.video_source)
                
            if not self.cap.isOpened():
                self.error_occurred.emit(f"Failed to open video source: {self.video_source}")
                return
                
            # Start feedback processing
            self.feedback_system.start_processing()
            self.is_running = True
            
            while self.is_running:
                ret, frame = self.cap.read()
                if not ret:
                    break
                    
                self.frame_count += 1
                
                # Process every nth frame for performance
                if self.frame_count % self.frame_skip == 0:
                    feedback = self.feedback_system.process_frame(frame)
                    self.frame_processed.emit(frame, feedback)
                    
                    # Emit statistics periodically
                    if self.frame_count % 30 == 0:  # Every ~1 second at 30fps
                        stats = self.feedback_system.get_session_statistics()
                        self.statistics_updated.emit(stats)
                        
                # Small delay to prevent overwhelming the system
                self.msleep(33)  # ~30 FPS
                
        except Exception as e:
            self.error_occurred.emit(f"Processing error: {str(e)}")
        finally:
            self.cleanup()
            
    def stop_processing(self):
        """Stop the processing loop."""
        self.is_running = False
        
    def cleanup(self):
        """Clean up resources."""
        if self.cap:
            self.cap.release()
        self.feedback_system.stop_processing()


class CalibrationQualityWidget(QWidget):
    """Widget for displaying calibration quality metrics and recommendations."""
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.setup_ui()
        self.current_feedback = None
        
    def setup_ui(self):
        """Set up the user interface."""
        layout = QVBoxLayout(self)
        
        # Quality status section
        quality_group = QGroupBox("Pattern Quality")
        quality_layout = QFormLayout(quality_group)
        
        self.quality_label = QLabel("Not Detected")
        self.quality_label.setStyleSheet("font-weight: bold; font-size: 14px;")
        quality_layout.addRow("Status:", self.quality_label)
        
        self.confidence_label = QLabel("0.00")
        quality_layout.addRow("Confidence:", self.confidence_label)
        
        self.detection_rate_label = QLabel("0%")
        quality_layout.addRow("Detection Rate:", self.detection_rate_label)
        
        layout.addWidget(quality_group)
        
        # Metrics section
        metrics_group = QGroupBox("Quality Metrics")
        metrics_layout = QFormLayout(metrics_group)
        
        self.area_ratio_label = QLabel("0.00")
        metrics_layout.addRow("Pattern Area Ratio:", self.area_ratio_label)
        
        self.sharpness_label = QLabel("0")
        metrics_layout.addRow("Sharpness Score:", self.sharpness_label)
        
        self.lighting_label = QLabel("0")
        metrics_layout.addRow("Lighting Score:", self.lighting_label)
        
        self.angle_label = QLabel("0")
        metrics_layout.addRow("Angle Score:", self.angle_label)
        
        layout.addWidget(metrics_group)
        
        # Recommendations section
        recommendations_group = QGroupBox("Recommendations")
        recommendations_layout = QVBoxLayout(recommendations_group)
        
        self.recommendations_list = QListWidget()
        self.recommendations_list.setMaximumHeight(150)
        recommendations_layout.addWidget(self.recommendations_list)
        
        layout.addWidget(recommendations_group)
        
    def update_feedback(self, feedback: PatternFeedback):
        """Update the display with new feedback data."""
        if feedback is None:
            return
            
        self.current_feedback = feedback
        
        # Update quality status
        quality_text = feedback.quality.value.upper()
        quality_color = self._get_quality_color(feedback.quality)
        self.quality_label.setText(quality_text)
        self.quality_label.setStyleSheet(f"font-weight: bold; font-size: 14px; color: {quality_color};")
        
        # Update metrics
        self.confidence_label.setText(f"{feedback.confidence:.2f}")
        self.area_ratio_label.setText(f"{feedback.pattern_area_ratio:.3f}")
        self.sharpness_label.setText(f"{feedback.sharpness_score:.0f}")
        self.lighting_label.setText(f"{feedback.lighting_score:.0f}")
        self.angle_label.setText(f"{feedback.angle_score:.0f}")
        
        # Update recommendations
        self.recommendations_list.clear()
        for recommendation in feedback.recommendations:
            item = QListWidgetItem(recommendation)
            self.recommendations_list.addItem(item)
            
    def update_statistics(self, stats: Dict):
        """Update display with session statistics."""
        if 'detection_rate' in stats:
            detection_rate = stats['detection_rate'] * 100
            self.detection_rate_label.setText(f"{detection_rate:.1f}%")
            
    def _get_quality_color(self, quality: PatternQuality) -> str:
        """Get color for quality status."""
        color_map = {
            PatternQuality.EXCELLENT: "green",
            PatternQuality.GOOD: "orange",
            PatternQuality.FAIR: "yellow",
            PatternQuality.POOR: "red",
            PatternQuality.NOT_DETECTED: "gray"
        }
        return color_map.get(quality, "gray")


class LiveCalibrationDialog(QDialog):
    """Enhanced calibration dialog with live feedback capabilities."""
    
    def __init__(self, parent=None):
        super().__init__(parent)
        self.logger = get_logger(__name__)
        self.worker = None
        self.recording_session = False
        self.captured_frames = []
        self.session_start_time = None
        
        self.setWindowTitle("Live Calibration with Real-time Feedback")
        self.setMinimumSize(1200, 800)
        self.setModal(True)
        
        self.setup_ui()
        self.connect_signals()
        
    def setup_ui(self):
        """Set up the user interface."""
        layout = QHBoxLayout(self)
        
        # Left panel - Video preview
        left_panel = self._create_video_panel()
        layout.addWidget(left_panel, 2)
        
        # Right panel - Controls and feedback
        right_panel = self._create_control_panel()
        layout.addWidget(right_panel, 1)
        
    def _create_video_panel(self) -> QWidget:
        """Create the video preview panel."""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        
        # Video preview
        self.video_preview = VideoPreviewWidget()
        layout.addWidget(self.video_preview)
        
        # Video controls
        controls_layout = QHBoxLayout()
        
        self.overlay_checkbox = QCheckBox("Show Feedback Overlay")
        self.overlay_checkbox.setChecked(True)
        self.overlay_checkbox.toggled.connect(self.video_preview.toggle_feedback_overlay)
        controls_layout.addWidget(self.overlay_checkbox)
        
        self.capture_frame_button = QPushButton("📸 Capture Frame")
        self.capture_frame_button.clicked.connect(self.capture_current_frame)
        self.capture_frame_button.setEnabled(False)
        controls_layout.addWidget(self.capture_frame_button)
        
        controls_layout.addStretch()
        layout.addLayout(controls_layout)
        
        return panel
        
    def _create_control_panel(self) -> QWidget:
        """Create the control and feedback panel."""
        panel = QWidget()
        layout = QVBoxLayout(panel)
        
        # Source selection
        source_group = QGroupBox("Video Source")
        source_layout = QFormLayout(source_group)
        
        self.source_combo = QComboBox()
        self.source_combo.addItems(["0", "1", "2"])  # Camera indices
        self.source_combo.setEditable(True)
        source_layout.addRow("Camera/File:", self.source_combo)
        
        self.browse_source_button = QPushButton("Browse File")
        self.browse_source_button.clicked.connect(self.browse_video_source)
        source_layout.addRow("", self.browse_source_button)
        
        layout.addWidget(source_group)
        
        # Pattern configuration
        pattern_group = QGroupBox("Calibration Pattern")
        pattern_layout = QFormLayout(pattern_group)
        
        self.pattern_type_combo = QComboBox()
        self.pattern_type_combo.addItems(["chessboard", "charuco"])
        pattern_layout.addRow("Type:", self.pattern_type_combo)
        
        grid_layout = QHBoxLayout()
        self.grid_width_spin = QSpinBox()
        self.grid_width_spin.setRange(3, 20)
        self.grid_width_spin.setValue(9)
        self.grid_height_spin = QSpinBox()
        self.grid_height_spin.setRange(3, 20)
        self.grid_height_spin.setValue(6)
        grid_layout.addWidget(QLabel("W:"))
        grid_layout.addWidget(self.grid_width_spin)
        grid_layout.addWidget(QLabel("H:"))
        grid_layout.addWidget(self.grid_height_spin)
        pattern_layout.addRow("Grid Size:", grid_layout)
        
        self.square_size_spin = QDoubleSpinBox()
        self.square_size_spin.setRange(0.001, 1.0)
        self.square_size_spin.setValue(0.025)
        self.square_size_spin.setSuffix(" m")
        self.square_size_spin.setDecimals(3)
        pattern_layout.addRow("Square Size:", self.square_size_spin)
        
        layout.addWidget(pattern_group)
        
        # Quality feedback
        self.quality_widget = CalibrationQualityWidget()
        layout.addWidget(self.quality_widget)
        
        # Session controls
        session_group = QGroupBox("Session Control")
        session_layout = QVBoxLayout(session_group)
        
        self.start_button = QPushButton("🎥 Start Live Preview")
        self.start_button.clicked.connect(self.start_live_preview)
        session_layout.addWidget(self.start_button)
        
        self.record_button = QPushButton("🔴 Start Recording Session")
        self.record_button.clicked.connect(self.toggle_recording_session)
        self.record_button.setEnabled(False)
        session_layout.addWidget(self.record_button)
        
        self.stop_button = QPushButton("⏹️ Stop Preview")
        self.stop_button.clicked.connect(self.stop_live_preview)
        self.stop_button.setEnabled(False)
        session_layout.addWidget(self.stop_button)
        
        # Session info
        self.session_info_label = QLabel("Session: Not started")
        session_layout.addWidget(self.session_info_label)
        
        self.frames_captured_label = QLabel("Frames captured: 0")
        session_layout.addWidget(self.frames_captured_label)
        
        layout.addWidget(session_group)
        
        # Export controls
        export_group = QGroupBox("Export")
        export_layout = QVBoxLayout(export_group)
        
        self.export_frames_button = QPushButton("💾 Export Captured Frames")
        self.export_frames_button.clicked.connect(self.export_captured_frames)
        self.export_frames_button.setEnabled(False)
        export_layout.addWidget(self.export_frames_button)
        
        self.run_calibration_button = QPushButton("🔧 Run Calibration on Frames")
        self.run_calibration_button.clicked.connect(self.run_calibration_on_frames)
        self.run_calibration_button.setEnabled(False)
        export_layout.addWidget(self.run_calibration_button)
        
        layout.addWidget(export_group)
        
        layout.addStretch()
        
        # Close button
        self.close_button = QPushButton("Close")
        self.close_button.clicked.connect(self.close)
        layout.addWidget(self.close_button)
        
        return panel
        
    def connect_signals(self):
        """Connect internal signals."""
        pass
        
    def browse_video_source(self):
        """Browse for video file source."""
        file_path, _ = QFileDialog.getOpenFileName(
            self, "Select Video File", "",
            "Video Files (*.mp4 *.avi *.mov *.mkv);;All Files (*)"
        )
        if file_path:
            self.source_combo.setCurrentText(file_path)
            
    def start_live_preview(self):
        """Start live video preview with feedback."""
        try:
            # Create calibration pattern
            pattern = CalibrationPattern(
                self.pattern_type_combo.currentText(),
                (self.grid_width_spin.value(), self.grid_height_spin.value()),
                self.square_size_spin.value()
            )
            
            # Create and start worker
            video_source = self.source_combo.currentText()
            self.worker = LiveCalibrationWorker(video_source, pattern)
            self.worker.frame_processed.connect(self.on_frame_processed)
            self.worker.statistics_updated.connect(self.on_statistics_updated)
            self.worker.error_occurred.connect(self.on_error_occurred)
            self.worker.start()
            
            # Update UI state
            self.start_button.setEnabled(False)
            self.record_button.setEnabled(True)
            self.stop_button.setEnabled(True)
            self.capture_frame_button.setEnabled(True)
            
            self.logger.info("Live calibration preview started")
            
        except Exception as e:
            QMessageBox.critical(self, "Error", f"Failed to start live preview:\n{str(e)}")
            
    def stop_live_preview(self):
        """Stop live video preview."""
        if self.worker:
            self.worker.stop_processing()
            self.worker.wait(3000)  # Wait up to 3 seconds
            self.worker = None
            
        # Update UI state
        self.start_button.setEnabled(True)
        self.record_button.setEnabled(False)
        self.stop_button.setEnabled(False)
        self.capture_frame_button.setEnabled(False)
        
        # Stop recording session if active
        if self.recording_session:
            self.toggle_recording_session()
            
        self.logger.info("Live calibration preview stopped")
        
    def toggle_recording_session(self):
        """Toggle recording session on/off."""
        if not self.recording_session:
            # Start recording session
            self.recording_session = True
            self.session_start_time = time.time()
            self.captured_frames.clear()
            
            self.record_button.setText("⏹️ Stop Recording Session")
            self.session_info_label.setText("Session: Recording...")
            self.export_frames_button.setEnabled(False)
            self.run_calibration_button.setEnabled(False)
            
            self.logger.info("Calibration recording session started")
        else:
            # Stop recording session
            self.recording_session = False
            session_duration = time.time() - self.session_start_time if self.session_start_time else 0
            
            self.record_button.setText("🔴 Start Recording Session")
            self.session_info_label.setText(f"Session: Completed ({session_duration:.1f}s)")
            self.export_frames_button.setEnabled(len(self.captured_frames) > 0)
            self.run_calibration_button.setEnabled(len(self.captured_frames) > 0)
            
            self.logger.info(f"Calibration recording session completed. Captured {len(self.captured_frames)} frames")
            
    def capture_current_frame(self):
        """Manually capture the current frame."""
        if self.video_preview.current_frame is not None:
            frame_data = {
                'frame': self.video_preview.current_frame.copy(),
                'timestamp': time.time(),
                'feedback': self.quality_widget.current_feedback
            }
            self.captured_frames.append(frame_data)
            self.frames_captured_label.setText(f"Frames captured: {len(self.captured_frames)}")
            
            # Enable export buttons if we have frames
            if len(self.captured_frames) > 0:
                self.export_frames_button.setEnabled(True)
                self.run_calibration_button.setEnabled(True)
                
            self.logger.info(f"Frame captured manually. Total frames: {len(self.captured_frames)}")
            
    def on_frame_processed(self, frame: np.ndarray, feedback: PatternFeedback):
        """Handle processed frame from worker."""
        # Update video preview
        self.video_preview.update_frame(frame, feedback)
        
        # Update quality feedback
        if feedback:
            self.quality_widget.update_feedback(feedback)
            
            # Auto-capture high-quality frames during recording session
            if (self.recording_session and feedback.detected and 
                feedback.quality in [PatternQuality.EXCELLENT, PatternQuality.GOOD]):
                
                # Avoid capturing too many frames too quickly
                if (not self.captured_frames or 
                    time.time() - self.captured_frames[-1]['timestamp'] > 2.0):
                    
                    frame_data = {
                        'frame': frame.copy(),
                        'timestamp': time.time(),
                        'feedback': feedback
                    }
                    self.captured_frames.append(frame_data)
                    self.frames_captured_label.setText(f"Frames captured: {len(self.captured_frames)}")
                    
    def on_statistics_updated(self, stats: Dict):
        """Handle statistics update from worker."""
        self.quality_widget.update_statistics(stats)
        
    def on_error_occurred(self, error_message: str):
        """Handle error from worker."""
        QMessageBox.critical(self, "Processing Error", error_message)
        self.stop_live_preview()
        
    def export_captured_frames(self):
        """Export captured frames to directory."""
        if not self.captured_frames:
            QMessageBox.warning(self, "No Frames", "No frames have been captured yet.")
            return
            
        # Select output directory
        output_dir = QFileDialog.getExistingDirectory(
            self, "Select Output Directory for Captured Frames"
        )
        if not output_dir:
            return
            
        try:
            output_path = Path(output_dir)
            frames_dir = output_path / "calibration_frames"
            frames_dir.mkdir(exist_ok=True)
            
            # Export frames
            for i, frame_data in enumerate(self.captured_frames):
                frame_filename = f"calibration_frame_{i:04d}_{frame_data['timestamp']:.3f}.jpg"
                frame_path = frames_dir / frame_filename
                cv2.imwrite(str(frame_path), frame_data['frame'])
                
            # Export metadata
            metadata = {
                'total_frames': len(self.captured_frames),
                'pattern_type': self.pattern_type_combo.currentText(),
                'grid_size': [self.grid_width_spin.value(), self.grid_height_spin.value()],
                'square_size': self.square_size_spin.value(),
                'export_timestamp': time.time()
            }
            
            import json
            metadata_path = frames_dir / "calibration_metadata.json"
            with open(metadata_path, 'w') as f:
                json.dump(metadata, f, indent=2)
                
            QMessageBox.information(
                self, "Export Complete",
                f"Exported {len(self.captured_frames)} frames to:\n{frames_dir}"
            )
            
        except Exception as e:
            QMessageBox.critical(self, "Export Error", f"Failed to export frames:\n{str(e)}")
            
    def run_calibration_on_frames(self):
        """Run calibration on captured frames."""
        if not self.captured_frames:
            QMessageBox.warning(self, "No Frames", "No frames have been captured yet.")
            return
            
        try:
            # Create calibration pattern
            pattern = CalibrationPattern(
                self.pattern_type_combo.currentText(),
                (self.grid_width_spin.value(), self.grid_height_spin.value()),
                self.square_size_spin.value()
            )
            
            # Create calibrator
            calibrator = CameraCalibrator(pattern, self.logger)
            
            # Extract frames for calibration
            frames = [frame_data['frame'] for frame_data in self.captured_frames]
            
            # Run calibration
            results = calibrator.calibrate_camera_intrinsics('live_camera', frames)
            
            # Show results
            self._show_calibration_results(results)
            
        except Exception as e:
            QMessageBox.critical(self, "Calibration Error", f"Failed to run calibration:\n{str(e)}")
            
    def _show_calibration_results(self, results: Dict):
        """Show calibration results in a dialog."""
        dialog = QDialog(self)
        dialog.setWindowTitle("Calibration Results")
        dialog.setMinimumSize(500, 400)
        
        layout = QVBoxLayout(dialog)
        
        # Results text
        text_edit = QTextEdit()
        text_edit.setReadOnly(True)
        text_edit.setFont(QFont("Courier", 10))
        
        results_text = f"""
CALIBRATION RESULTS
==================

Image Size: {results['image_size']}
Reprojection Error: {results['reprojection_error']:.3f} pixels
Images Used: {results['num_images_used']}
Calibration Date: {results['calibration_date']}

Camera Matrix:
{np.array(results['camera_matrix'])}

Distortion Coefficients:
{np.array(results['distortion_coefficients'])}
        """
        
        text_edit.setPlainText(results_text)
        layout.addWidget(text_edit)
        
        # Buttons
        button_layout = QHBoxLayout()
        
        save_button = QPushButton("Save Results")
        save_button.clicked.connect(lambda: self._save_calibration_results(results))
        button_layout.addWidget(save_button)
        
        close_button = QPushButton("Close")
        close_button.clicked.connect(dialog.close)
        button_layout.addWidget(close_button)
        
        layout.addLayout(button_layout)
        
        dialog.exec_()
        
    def _save_calibration_results(self, results: Dict):
        """Save calibration results to JSON file."""
        file_path, _ = QFileDialog.getSaveFileName(
            self, "Save Calibration Results", "live_calibration_results.json",
            "JSON Files (*.json);;All Files (*)"
        )
        
        if file_path:
            try:
                # Create calibration pattern for metadata
                pattern = CalibrationPattern(
                    self.pattern_type_combo.currentText(),
                    (self.grid_width_spin.value(), self.grid_height_spin.value()),
                    self.square_size_spin.value()
                )
                
                # Create calibrator and save results
                calibrator = CameraCalibrator(pattern, self.logger)
                calibrator.cameras['live_camera'] = results
                calibrator.save_calibration_results(file_path)
                
                QMessageBox.information(
                    self, "Save Complete",
                    f"Calibration results saved to:\n{file_path}"
                )
                
            except Exception as e:
                QMessageBox.critical(
                    self, "Save Error",
                    f"Failed to save calibration results:\n{str(e)}"
                )
                
    def closeEvent(self, event):
        """Handle dialog close event."""
        if self.worker:
            self.stop_live_preview()
        event.accept()