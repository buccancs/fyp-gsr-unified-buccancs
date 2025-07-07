"""
Real-time Calibration Feedback System for FYP-GSR System

This module provides real-time feedback during calibration data recording,
helping researchers ensure they capture high-quality calibration patterns.
It integrates with the existing camera calibration system to provide:

- Live pattern detection and quality assessment
- Real-time feedback on pattern positioning and quality
- Integration with video streams for live monitoring
- Recommendations for improving calibration data capture

Author: FYP-GSR Team
"""

import logging
import threading
import time
from dataclasses import dataclass
from enum import Enum
from queue import Empty, Queue
from typing import Callable, Dict, List, Optional, Tuple

import cv2
import numpy as np

from utils.camera_calibration import CalibrationPattern, CameraCalibrator


class PatternQuality(Enum):
    """Enumeration for pattern detection quality levels."""
    EXCELLENT = "excellent"
    GOOD = "good"
    FAIR = "fair"
    POOR = "poor"
    NOT_DETECTED = "not_detected"


@dataclass
class PatternFeedback:
    """Data class for pattern detection feedback."""
    quality: PatternQuality
    detected: bool
    corners_count: int
    pattern_area_ratio: float  # Ratio of pattern area to image area
    sharpness_score: float     # Image sharpness metric
    lighting_score: float      # Lighting quality metric
    angle_score: float         # Pattern angle/perspective score
    recommendations: List[str]
    confidence: float          # Overall confidence score (0-1)


class RealtimeCalibrationFeedback:
    """
    Real-time calibration feedback system that analyzes video frames
    and provides immediate feedback on calibration pattern quality.
    """

    def __init__(self, pattern: CalibrationPattern,
                 logger: logging.Logger = None):
        """
        Initialize the real-time feedback system.

        Args:
            pattern: Calibration pattern to detect
            logger: Logger instance
        """
        self.pattern = pattern
        self.logger = logger or logging.getLogger(__name__)
        self.calibrator = CameraCalibrator(pattern, logger)

        # Feedback parameters
        self.min_pattern_area_ratio = 0.15  # Minimum 15% of image
        self.max_pattern_area_ratio = 0.80  # Maximum 80% of image
        self.min_sharpness_threshold = 100  # Laplacian variance threshold
        self.min_lighting_score = 50       # Minimum lighting quality
        self.max_angle_threshold = 45      # Maximum angle from perpendicular

        # Tracking variables
        self.frame_count = 0
        self.detection_history = []
        self.quality_history = []
        self.last_feedback_time = 0
        self.feedback_interval = 0.1  # 100ms between feedback updates

        # Threading for real-time processing
        self.processing_queue = Queue(maxsize=10)
        self.feedback_queue = Queue(maxsize=5)
        self.processing_thread = None
        self.is_running = False

        # Callback for feedback updates
        self.feedback_callback: Optional[Callable[[
            PatternFeedback], None]] = None

    def set_feedback_callback(
            self, callback: Callable[[PatternFeedback], None]):
        """Set callback function for receiving feedback updates."""
        self.feedback_callback = callback

    def start_processing(self):
        """Start the real-time processing thread."""
        if self.is_running:
            return

        self.is_running = True
        self.processing_thread = threading.Thread(
            target=self._processing_loop, daemon=True)
        self.processing_thread.start()
        self.logger.info("Real-time calibration feedback started")

    def stop_processing(self):
        """Stop the real-time processing thread."""
        self.is_running = False
        if self.processing_thread:
            self.processing_thread.join(timeout=1.0)
        self.logger.info("Real-time calibration feedback stopped")

    def process_frame(
            self,
            frame: np.ndarray,
            timestamp: float = None) -> Optional[PatternFeedback]:
        """
        Process a single frame and return feedback.

        Args:
            frame: Input video frame
            timestamp: Frame timestamp (optional)

        Returns:
            PatternFeedback object or None if processing failed
        """
        if timestamp is None:
            timestamp = time.time()

        # Add frame to processing queue (non-blocking)
        try:
            self.processing_queue.put_nowait((frame.copy(), timestamp))
        except BaseException:
            # Queue full, skip this frame
            pass

        # Try to get latest feedback
        try:
            return self.feedback_queue.get_nowait()
        except Empty:
            return None

    def _processing_loop(self):
        """Main processing loop running in background thread."""
        while self.is_running:
            try:
                # Get frame from queue (with timeout)
                frame, timestamp = self.processing_queue.get(timeout=0.1)

                # Process frame
                feedback = self._analyze_frame(frame, timestamp)

                # Send feedback via callback
                if self.feedback_callback:
                    self.feedback_callback(feedback)

                # Store in feedback queue
                try:
                    self.feedback_queue.put_nowait(feedback)
                except BaseException:
                    # Queue full, remove old feedback
                    try:
                        self.feedback_queue.get_nowait()
                        self.feedback_queue.put_nowait(feedback)
                    except BaseException:
                        pass

            except Empty:
                continue
            except Exception as e:
                self.logger.error(f"Error in processing loop: {e}")

    def _analyze_frame(
            self,
            frame: np.ndarray,
            timestamp: float) -> PatternFeedback:
        """
        Analyze a single frame and generate feedback.

        Args:
            frame: Input frame
            timestamp: Frame timestamp

        Returns:
            PatternFeedback object
        """
        self.frame_count += 1

        # Convert to grayscale for analysis
        if len(frame.shape) == 3:
            gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
        else:
            gray = frame

        # Detect calibration pattern
        detected, corners = self.calibrator.detect_pattern_in_image(frame)

        # Initialize feedback
        feedback = PatternFeedback(
            quality=PatternQuality.NOT_DETECTED,
            detected=detected,
            corners_count=0,
            pattern_area_ratio=0.0,
            sharpness_score=0.0,
            lighting_score=0.0,
            angle_score=0.0,
            recommendations=[],
            confidence=0.0
        )

        if not detected:
            feedback.recommendations = self._get_detection_recommendations(
                gray)
            return feedback

        # Calculate pattern metrics
        feedback.corners_count = len(corners) if corners is not None else 0
        feedback.pattern_area_ratio = self._calculate_pattern_area_ratio(
            corners, frame.shape)
        feedback.sharpness_score = self._calculate_sharpness(gray)
        feedback.lighting_score = self._calculate_lighting_quality(gray)
        feedback.angle_score = self._calculate_angle_score(
            corners, frame.shape)

        # Determine overall quality
        feedback.quality = self._determine_quality(feedback)
        feedback.confidence = self._calculate_confidence(feedback)
        feedback.recommendations = self._generate_recommendations(feedback)

        # Update history
        self.detection_history.append(detected)
        self.quality_history.append(feedback.quality)

        # Keep only recent history
        if len(self.detection_history) > 30:
            self.detection_history.pop(0)
            self.quality_history.pop(0)

        return feedback

    def _calculate_pattern_area_ratio(
            self, corners: np.ndarray, image_shape: Tuple[int, int, int]) -> float:
        """Calculate the ratio of pattern area to image area."""
        if corners is None or len(corners) < 4:
            return 0.0

        # Calculate bounding rectangle of corners
        x_coords = corners[:, 0, 0]
        y_coords = corners[:, 0, 1]

        min_x, max_x = np.min(x_coords), np.max(x_coords)
        min_y, max_y = np.min(y_coords), np.max(y_coords)

        pattern_area = (max_x - min_x) * (max_y - min_y)
        image_area = image_shape[1] * image_shape[0]  # width * height

        return pattern_area / image_area

    def _calculate_sharpness(self, gray_image: np.ndarray) -> float:
        """Calculate image sharpness using Laplacian variance."""
        laplacian = cv2.Laplacian(gray_image, cv2.CV_64F)
        return laplacian.var()

    def _calculate_lighting_quality(self, gray_image: np.ndarray) -> float:
        """Calculate lighting quality based on histogram distribution."""
        hist = cv2.calcHist([gray_image], [0], None, [256], [0, 256])

        # Calculate histogram spread (higher is better)
        hist_normalized = hist.flatten() / hist.sum()

        # Calculate entropy as a measure of histogram spread
        entropy = -np.sum(hist_normalized * np.log2(hist_normalized + 1e-10))

        # Normalize to 0-100 scale
        return min(100, entropy * 12.5)

    def _calculate_angle_score(
            self, corners: np.ndarray, image_shape: Tuple[int, int, int]) -> float:
        """Calculate how perpendicular the pattern is to the camera."""
        if corners is None or len(corners) < 4:
            return 0.0

        # For chessboard, estimate perspective distortion
        if self.pattern.pattern_type == 'chessboard':
            # Find corner quadrilateral
            x_coords = corners[:, 0, 0]
            y_coords = corners[:, 0, 1]

            # Calculate aspect ratio distortion
            width = np.max(x_coords) - np.min(x_coords)
            height = np.max(y_coords) - np.min(y_coords)

            expected_ratio = self.pattern.grid_size[0] / \
                self.pattern.grid_size[1]
            actual_ratio = width / height if height > 0 else 0

            ratio_error = abs(actual_ratio - expected_ratio) / expected_ratio
            angle_score = max(0, 100 - ratio_error * 100)

            return min(100, angle_score)

        return 50.0  # Default score for other patterns

    def _determine_quality(self, feedback: PatternFeedback) -> PatternQuality:
        """Determine overall pattern quality based on metrics."""
        if not feedback.detected:
            return PatternQuality.NOT_DETECTED

        # Score components (0-100 each)
        area_score = 100 if self.min_pattern_area_ratio <= feedback.pattern_area_ratio <= self.max_pattern_area_ratio else 0
        sharpness_score = min(
            100,
            feedback.sharpness_score /
            self.min_sharpness_threshold *
            100)
        lighting_score = feedback.lighting_score
        angle_score = feedback.angle_score

        # Weighted average
        overall_score = (
            area_score * 0.3 +
            sharpness_score * 0.3 +
            lighting_score * 0.2 +
            angle_score * 0.2
        )

        if overall_score >= 80:
            return PatternQuality.EXCELLENT
        elif overall_score >= 65:
            return PatternQuality.GOOD
        elif overall_score >= 45:
            return PatternQuality.FAIR
        else:
            return PatternQuality.POOR

    def _calculate_confidence(self, feedback: PatternFeedback) -> float:
        """Calculate confidence score for the feedback."""
        if not feedback.detected:
            return 0.0

        # Base confidence on detection consistency
        recent_detections = self.detection_history[-10:] if len(
            self.detection_history) >= 10 else self.detection_history
        detection_rate = sum(recent_detections) / \
            len(recent_detections) if recent_detections else 0

        # Adjust based on quality metrics
        quality_multiplier = {
            PatternQuality.EXCELLENT: 1.0,
            PatternQuality.GOOD: 0.8,
            PatternQuality.FAIR: 0.6,
            PatternQuality.POOR: 0.4,
            PatternQuality.NOT_DETECTED: 0.0
        }

        return detection_rate * quality_multiplier.get(feedback.quality, 0.0)

    def _generate_recommendations(
            self, feedback: PatternFeedback) -> List[str]:
        """Generate specific recommendations based on feedback metrics."""
        recommendations = []

        if not feedback.detected:
            return self._get_detection_recommendations(None)

        # Pattern size recommendations
        if feedback.pattern_area_ratio < self.min_pattern_area_ratio:
            recommendations.append(
                "Move the calibration pattern closer to the camera")
        elif feedback.pattern_area_ratio > self.max_pattern_area_ratio:
            recommendations.append(
                "Move the calibration pattern further from the camera")

        # Sharpness recommendations
        if feedback.sharpness_score < self.min_sharpness_threshold:
            recommendations.append(
                "Ensure the pattern is in focus and avoid motion blur")

        # Lighting recommendations
        if feedback.lighting_score < self.min_lighting_score:
            recommendations.append(
                "Improve lighting conditions for better contrast")

        # Angle recommendations
        if feedback.angle_score < 70:
            recommendations.append(
                "Try to position the pattern more perpendicular to the camera")

        # Quality-specific recommendations
        if feedback.quality == PatternQuality.POOR:
            recommendations.append(
                "Consider repositioning the pattern for better detection")
        elif feedback.quality == PatternQuality.FAIR:
            recommendations.append(
                "Good detection - try to improve positioning for better quality")
        elif feedback.quality in [PatternQuality.GOOD, PatternQuality.EXCELLENT]:
            recommendations.append(
                "Excellent pattern detection - capture this position!")

        return recommendations

    def _get_detection_recommendations(
            self, gray_image: Optional[np.ndarray]) -> List[str]:
        """Get recommendations when pattern is not detected."""
        recommendations = [
            "Ensure the calibration pattern is fully visible in the frame",
            "Check that the pattern is well-lit and has good contrast",
            "Make sure the pattern is not too close or too far from the camera",
            "Verify that the pattern matches the configured type and size"]

        if gray_image is not None:
            # Add specific recommendations based on image analysis
            mean_brightness = np.mean(gray_image)
            if mean_brightness < 50:
                recommendations.append(
                    "Image appears too dark - increase lighting")
            elif mean_brightness > 200:
                recommendations.append(
                    "Image appears too bright - reduce lighting or exposure")

        return recommendations

    def get_session_statistics(self) -> Dict:
        """Get statistics for the current feedback session."""
        if not self.detection_history:
            return {}

        total_frames = len(self.detection_history)
        detected_frames = sum(self.detection_history)
        detection_rate = detected_frames / total_frames

        # Quality distribution
        quality_counts = {}
        for quality in self.quality_history:
            quality_counts[quality.value] = quality_counts.get(
                quality.value, 0) + 1

        return {
            'total_frames_processed': total_frames,
            'detection_rate': detection_rate,
            'detected_frames': detected_frames,
            'quality_distribution': quality_counts,
            'average_confidence': np.mean([1.0 if d else 0.0 for d in self.detection_history])
        }

    def reset_session(self):
        """Reset session statistics and history."""
        self.frame_count = 0
        self.detection_history.clear()
        self.quality_history.clear()
        self.logger.info("Calibration feedback session reset")


class CalibrationFeedbackVisualizer:
    """
    Utility class for visualizing calibration feedback on video frames.
    """

    @staticmethod
    def draw_feedback_overlay(frame: np.ndarray, feedback: PatternFeedback,
                              corners: np.ndarray = None) -> np.ndarray:
        """
        Draw feedback overlay on video frame.

        Args:
            frame: Input video frame
            feedback: Pattern feedback data
            corners: Detected pattern corners (optional)

        Returns:
            Frame with feedback overlay
        """
        overlay_frame = frame.copy()

        # Color scheme based on quality
        color_map = {
            PatternQuality.EXCELLENT: (0, 255, 0),      # Green
            PatternQuality.GOOD: (0, 200, 255),         # Orange
            PatternQuality.FAIR: (0, 165, 255),         # Yellow
            PatternQuality.POOR: (0, 0, 255),           # Red
            PatternQuality.NOT_DETECTED: (128, 128, 128)  # Gray
        }

        color = color_map.get(feedback.quality, (128, 128, 128))

        # Draw detected corners if available
        if feedback.detected and corners is not None:
            cv2.drawChessboardCorners(overlay_frame, (9, 6), corners, True)

        # Draw status box
        CalibrationFeedbackVisualizer._draw_status_box(
            overlay_frame, feedback, color
        )

        # Draw recommendations
        CalibrationFeedbackVisualizer._draw_recommendations(
            overlay_frame, feedback.recommendations
        )

        return overlay_frame

    @staticmethod
    def _draw_status_box(
            frame: np.ndarray, feedback: PatternFeedback, color: Tuple[int, int, int]):
        """Draw status information box."""
        height, width = frame.shape[:2]

        # Status box background
        cv2.rectangle(frame, (10, 10), (400, 150), (0, 0, 0), -1)
        cv2.rectangle(frame, (10, 10), (400, 150), color, 2)

        # Status text
        font = cv2.FONT_HERSHEY_SIMPLEX
        font_scale = 0.6
        thickness = 1

        y_offset = 35
        line_height = 20

        # Quality status
        quality_text = f"Quality: {feedback.quality.value.upper()}"
        cv2.putText(frame, quality_text, (20, y_offset),
                    font, font_scale, color, thickness)
        y_offset += line_height

        # Detection status
        detection_text = f"Detected: {'YES' if feedback.detected else 'NO'}"
        cv2.putText(frame, detection_text, (20, y_offset), font,
                    font_scale, (255, 255, 255), thickness)
        y_offset += line_height

        if feedback.detected:
            # Metrics
            metrics_text = f"Area: {
                feedback.pattern_area_ratio:.2f} | Sharpness: {
                feedback.sharpness_score:.0f}"
            cv2.putText(frame, metrics_text, (20, y_offset),
                        font, 0.5, (255, 255, 255), thickness)
            y_offset += line_height

            confidence_text = f"Confidence: {feedback.confidence:.2f}"
            cv2.putText(frame, confidence_text, (20, y_offset),
                        font, font_scale, (255, 255, 255), thickness)

    @staticmethod
    def _draw_recommendations(frame: np.ndarray, recommendations: List[str]):
        """Draw recommendations text."""
        if not recommendations:
            return

        height, width = frame.shape[:2]

        # Recommendations box
        box_height = min(200, len(recommendations) * 25 + 40)
        y_start = height - box_height - 10

        cv2.rectangle(frame, (10, y_start),
                      (width - 10, height - 10), (0, 0, 0), -1)
        cv2.rectangle(frame, (10, y_start), (width - 10,
                      height - 10), (255, 255, 255), 1)

        # Recommendations text
        font = cv2.FONT_HERSHEY_SIMPLEX
        font_scale = 0.5
        thickness = 1

        cv2.putText(frame, "Recommendations:", (20, y_start + 25),
                    font, 0.6, (255, 255, 0), thickness)

        y_offset = y_start + 50
        for i, recommendation in enumerate(
                recommendations[:6]):  # Limit to 6 recommendations
            text = f"• {recommendation}"
            cv2.putText(frame, text, (20, y_offset), font,
                        font_scale, (255, 255, 255), thickness)
            y_offset += 25
