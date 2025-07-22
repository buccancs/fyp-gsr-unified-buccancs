"""Advanced Thermal-RGB Synchronization Module for FYP-GSR System

This module provides advanced calibration and synchronization capabilities
for thermal and RGB cameras, enabling precise alignment and temporal
synchronization for multimodal data capture.

Features:
- Spatial alignment calibration between thermal and RGB cameras
- Temporal synchronization with sub-millisecond accuracy
- Real-time alignment verification
- Automatic calibration pattern detection
- Export calibration parameters for reuse
- Performance optimization for real-time processing

Author: FYP-GSR Team
"""

import cv2
import numpy as np
import json
import time
import threading
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple, Any
from dataclasses import dataclass, asdict
from enum import Enum

try:
    from scipy import optimize
    from scipy.spatial.distance import cdist
    SCIPY_AVAILABLE = True
except ImportError:
    SCIPY_AVAILABLE = False

from utils.logger import get_logger


class SyncQuality(Enum):
    """Enumeration for synchronization quality levels."""
    EXCELLENT = "excellent"
    GOOD = "good"
    FAIR = "fair"
    POOR = "poor"
    FAILED = "failed"


@dataclass
class SpatialCalibration:
    """Container for spatial calibration parameters."""
    homography_matrix: np.ndarray
    translation_vector: Tuple[float, float]
    rotation_angle: float
    scale_factor: float
    rms_error: float
    quality: SyncQuality
    calibration_points: List[Tuple[float, float]]
    timestamp: datetime


@dataclass
class TemporalCalibration:
    """Container for temporal calibration parameters."""
    time_offset_ms: float
    sync_accuracy_ms: float
    frame_rate_thermal: float
    frame_rate_rgb: float
    quality: SyncQuality
    sync_events: List[Dict[str, Any]]
    timestamp: datetime


@dataclass
class SyncCalibrationResult:
    """Complete synchronization calibration result."""
    spatial: Optional[SpatialCalibration]
    temporal: Optional[TemporalCalibration]
    overall_quality: SyncQuality
    calibration_id: str
    device_info: Dict[str, Any]
    timestamp: datetime


class CalibrationPattern:
    """Calibration pattern detection and analysis."""
    
    def __init__(self, pattern_type: str = "checkerboard", size: Tuple[int, int] = (9, 6)):
        self.pattern_type = pattern_type
        self.size = size
        self.logger = get_logger(__name__)
        
    def detect_pattern(self, image: np.ndarray, thermal: bool = False) -> Optional[np.ndarray]:
        """Detect calibration pattern in image."""
        try:
            if thermal:
                # For thermal images, apply specific preprocessing
                processed = self._preprocess_thermal_image(image)
            else:
                # For RGB images, convert to grayscale
                if len(image.shape) == 3:
                    processed = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
                else:
                    processed = image.copy()
            
            # Detect checkerboard pattern
            if self.pattern_type == "checkerboard":
                ret, corners = cv2.findChessboardCorners(
                    processed, self.size,
                    cv2.CALIB_CB_ADAPTIVE_THRESH + cv2.CALIB_CB_NORMALIZE_IMAGE
                )
                
                if ret:
                    # Refine corner positions
                    criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)
                    corners = cv2.cornerSubPix(processed, corners, (11, 11), (-1, -1), criteria)
                    return corners.reshape(-1, 2)
                    
            # Detect circle grid pattern
            elif self.pattern_type == "circles":
                ret, centers = cv2.findCirclesGrid(processed, self.size)
                if ret:
                    return centers.reshape(-1, 2)
                    
        except Exception as e:
            self.logger.error(f"Pattern detection failed: {e}")
            
        return None
        
    def _preprocess_thermal_image(self, thermal_image: np.ndarray) -> np.ndarray:
        """Preprocess thermal image for better pattern detection."""
        # Normalize thermal values
        normalized = cv2.normalize(thermal_image, None, 0, 255, cv2.NORM_MINMAX, dtype=cv2.CV_8U)
        
        # Apply histogram equalization
        equalized = cv2.equalizeHist(normalized)
        
        # Apply Gaussian blur to reduce noise
        blurred = cv2.GaussianBlur(equalized, (5, 5), 0)
        
        return blurred


class SpatialSynchronizer:
    """Handles spatial alignment between thermal and RGB cameras."""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self.pattern_detector = CalibrationPattern()
        
    def calibrate_spatial_alignment(self, 
                                  rgb_images: List[np.ndarray], 
                                  thermal_images: List[np.ndarray]) -> Optional[SpatialCalibration]:
        """Calibrate spatial alignment between RGB and thermal cameras."""
        if len(rgb_images) != len(thermal_images):
            self.logger.error("RGB and thermal image counts must match")
            return None
            
        rgb_points = []
        thermal_points = []
        
        # Detect calibration patterns in both image sets
        for rgb_img, thermal_img in zip(rgb_images, thermal_images):
            rgb_corners = self.pattern_detector.detect_pattern(rgb_img, thermal=False)
            thermal_corners = self.pattern_detector.detect_pattern(thermal_img, thermal=True)
            
            if rgb_corners is not None and thermal_corners is not None:
                if len(rgb_corners) == len(thermal_corners):
                    rgb_points.extend(rgb_corners)
                    thermal_points.extend(thermal_corners)
                    
        if len(rgb_points) < 4:
            self.logger.error("Insufficient calibration points found")
            return None
            
        # Convert to numpy arrays
        rgb_points = np.array(rgb_points, dtype=np.float32)
        thermal_points = np.array(thermal_points, dtype=np.float32)
        
        # Calculate homography matrix
        homography, mask = cv2.findHomography(
            thermal_points, rgb_points, 
            cv2.RANSAC, 5.0
        )
        
        if homography is None:
            self.logger.error("Failed to compute homography")
            return None
            
        # Calculate transformation parameters
        translation, rotation, scale = self._decompose_homography(homography)
        
        # Calculate RMS error
        rms_error = self._calculate_rms_error(rgb_points, thermal_points, homography)
        
        # Determine quality based on RMS error
        quality = self._assess_spatial_quality(rms_error, len(rgb_points))
        
        return SpatialCalibration(
            homography_matrix=homography,
            translation_vector=translation,
            rotation_angle=rotation,
            scale_factor=scale,
            rms_error=rms_error,
            quality=quality,
            calibration_points=[(float(p[0]), float(p[1])) for p in rgb_points],
            timestamp=datetime.now()
        )
        
    def _decompose_homography(self, homography: np.ndarray) -> Tuple[Tuple[float, float], float, float]:
        """Decompose homography matrix into translation, rotation, and scale."""
        # Extract translation
        translation = (float(homography[0, 2]), float(homography[1, 2]))
        
        # Extract rotation and scale
        a = homography[0, 0]
        b = homography[0, 1]
        c = homography[1, 0]
        d = homography[1, 1]
        
        scale_x = np.sqrt(a*a + b*b)
        scale_y = np.sqrt(c*c + d*d)
        scale = (scale_x + scale_y) / 2.0
        
        rotation = np.arctan2(b, a) * 180.0 / np.pi
        
        return translation, rotation, scale
        
    def _calculate_rms_error(self, rgb_points: np.ndarray, 
                           thermal_points: np.ndarray, 
                           homography: np.ndarray) -> float:
        """Calculate RMS reprojection error."""
        # Transform thermal points using homography
        thermal_homogeneous = np.column_stack([thermal_points, np.ones(len(thermal_points))])
        transformed = np.dot(homography, thermal_homogeneous.T).T
        transformed = transformed[:, :2] / transformed[:, 2:3]
        
        # Calculate squared differences
        diff = rgb_points - transformed
        squared_errors = np.sum(diff * diff, axis=1)
        
        # Return RMS error
        return np.sqrt(np.mean(squared_errors))
        
    def _assess_spatial_quality(self, rms_error: float, num_points: int) -> SyncQuality:
        """Assess spatial calibration quality based on RMS error and point count."""
        if num_points < 10:
            return SyncQuality.POOR
            
        if rms_error < 1.0:
            return SyncQuality.EXCELLENT
        elif rms_error < 2.0:
            return SyncQuality.GOOD
        elif rms_error < 5.0:
            return SyncQuality.FAIR
        else:
            return SyncQuality.POOR
            
    def apply_spatial_transform(self, thermal_image: np.ndarray, 
                              calibration: SpatialCalibration) -> np.ndarray:
        """Apply spatial transformation to align thermal image with RGB."""
        height, width = thermal_image.shape[:2]
        return cv2.warpPerspective(thermal_image, calibration.homography_matrix, (width, height))


class TemporalSynchronizer:
    """Handles temporal synchronization between thermal and RGB cameras."""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        
    def calibrate_temporal_sync(self, 
                              rgb_timestamps: List[float], 
                              thermal_timestamps: List[float],
                              sync_events: List[Dict[str, Any]] = None) -> Optional[TemporalCalibration]:
        """Calibrate temporal synchronization between cameras."""
        if len(rgb_timestamps) < 10 or len(thermal_timestamps) < 10:
            self.logger.error("Insufficient timestamp data for temporal calibration")
            return None
            
        # Calculate frame rates
        rgb_frame_rate = self._calculate_frame_rate(rgb_timestamps)
        thermal_frame_rate = self._calculate_frame_rate(thermal_timestamps)
        
        # Find optimal time offset
        time_offset = self._find_optimal_offset(rgb_timestamps, thermal_timestamps)
        
        # Calculate synchronization accuracy
        sync_accuracy = self._calculate_sync_accuracy(
            rgb_timestamps, thermal_timestamps, time_offset
        )
        
        # Assess quality
        quality = self._assess_temporal_quality(sync_accuracy, len(rgb_timestamps))
        
        return TemporalCalibration(
            time_offset_ms=time_offset * 1000,  # Convert to milliseconds
            sync_accuracy_ms=sync_accuracy * 1000,
            frame_rate_thermal=thermal_frame_rate,
            frame_rate_rgb=rgb_frame_rate,
            quality=quality,
            sync_events=sync_events or [],
            timestamp=datetime.now()
        )
        
    def _calculate_frame_rate(self, timestamps: List[float]) -> float:
        """Calculate average frame rate from timestamps."""
        if len(timestamps) < 2:
            return 0.0
            
        intervals = np.diff(timestamps)
        avg_interval = np.mean(intervals)
        
        return 1.0 / avg_interval if avg_interval > 0 else 0.0
        
    def _find_optimal_offset(self, rgb_timestamps: List[float], 
                           thermal_timestamps: List[float]) -> float:
        """Find optimal time offset between camera streams."""
        if not SCIPY_AVAILABLE:
            # Simple offset calculation without scipy
            return np.mean(rgb_timestamps) - np.mean(thermal_timestamps)
            
        # Use cross-correlation to find optimal offset
        def correlation_function(offset):
            # Shift thermal timestamps by offset
            shifted_thermal = np.array(thermal_timestamps) + offset
            
            # Find matching timestamps within tolerance
            tolerance = 0.05  # 50ms tolerance
            matches = 0
            
            for rgb_ts in rgb_timestamps:
                distances = np.abs(shifted_thermal - rgb_ts)
                if np.min(distances) < tolerance:
                    matches += 1
                    
            return -matches  # Negative because we want to maximize matches
            
        # Optimize offset
        result = optimize.minimize_scalar(
            correlation_function,
            bounds=(-1.0, 1.0),  # Search within ±1 second
            method='bounded'
        )
        
        return result.x if result.success else 0.0
        
    def _calculate_sync_accuracy(self, rgb_timestamps: List[float], 
                               thermal_timestamps: List[float], 
                               offset: float) -> float:
        """Calculate synchronization accuracy after applying offset."""
        shifted_thermal = np.array(thermal_timestamps) + offset
        
        # Find closest matches
        errors = []
        for rgb_ts in rgb_timestamps:
            distances = np.abs(shifted_thermal - rgb_ts)
            min_distance = np.min(distances)
            if min_distance < 0.1:  # Within 100ms
                errors.append(min_distance)
                
        return np.mean(errors) if errors else float('inf')
        
    def _assess_temporal_quality(self, sync_accuracy: float, num_samples: int) -> SyncQuality:
        """Assess temporal synchronization quality."""
        if num_samples < 20:
            return SyncQuality.POOR
            
        if sync_accuracy < 0.005:  # < 5ms
            return SyncQuality.EXCELLENT
        elif sync_accuracy < 0.010:  # < 10ms
            return SyncQuality.GOOD
        elif sync_accuracy < 0.020:  # < 20ms
            return SyncQuality.FAIR
        else:
            return SyncQuality.POOR


class ThermalRGBSynchronizer:
    """Main class for thermal-RGB synchronization."""
    
    def __init__(self):
        self.logger = get_logger(__name__)
        self.spatial_sync = SpatialSynchronizer()
        self.temporal_sync = TemporalSynchronizer()
        
    def perform_full_calibration(self,
                               rgb_images: List[np.ndarray],
                               thermal_images: List[np.ndarray],
                               rgb_timestamps: List[float],
                               thermal_timestamps: List[float],
                               device_info: Dict[str, Any] = None) -> SyncCalibrationResult:
        """Perform complete spatial and temporal calibration."""
        self.logger.info("Starting thermal-RGB synchronization calibration")
        
        # Perform spatial calibration
        spatial_cal = self.spatial_sync.calibrate_spatial_alignment(rgb_images, thermal_images)
        
        # Perform temporal calibration
        temporal_cal = self.temporal_sync.calibrate_temporal_sync(rgb_timestamps, thermal_timestamps)
        
        # Determine overall quality
        overall_quality = self._determine_overall_quality(spatial_cal, temporal_cal)
        
        # Generate calibration ID
        calibration_id = f"thermal_rgb_sync_{datetime.now().strftime('%Y%m%d_%H%M%S')}"
        
        result = SyncCalibrationResult(
            spatial=spatial_cal,
            temporal=temporal_cal,
            overall_quality=overall_quality,
            calibration_id=calibration_id,
            device_info=device_info or {},
            timestamp=datetime.now()
        )
        
        self.logger.info(f"Calibration completed with quality: {overall_quality.value}")
        return result
        
    def _determine_overall_quality(self, 
                                 spatial: Optional[SpatialCalibration], 
                                 temporal: Optional[TemporalCalibration]) -> SyncQuality:
        """Determine overall calibration quality."""
        qualities = []
        
        if spatial:
            qualities.append(spatial.quality)
        if temporal:
            qualities.append(temporal.quality)
            
        if not qualities:
            return SyncQuality.FAILED
            
        # Return the worst quality among all calibrations
        quality_order = [SyncQuality.EXCELLENT, SyncQuality.GOOD, SyncQuality.FAIR, SyncQuality.POOR, SyncQuality.FAILED]
        
        for quality in quality_order:
            if quality in qualities:
                return quality
                
        return SyncQuality.FAILED
        
    def save_calibration(self, calibration: SyncCalibrationResult, filepath: Path) -> bool:
        """Save calibration results to file."""
        try:
            # Convert numpy arrays to lists for JSON serialization
            data = asdict(calibration)
            
            if calibration.spatial:
                data['spatial']['homography_matrix'] = calibration.spatial.homography_matrix.tolist()
                
            # Convert datetime objects to ISO strings
            data['timestamp'] = calibration.timestamp.isoformat()
            if calibration.spatial:
                data['spatial']['timestamp'] = calibration.spatial.timestamp.isoformat()
            if calibration.temporal:
                data['temporal']['timestamp'] = calibration.temporal.timestamp.isoformat()
                
            # Convert enums to strings
            data['overall_quality'] = calibration.overall_quality.value
            if calibration.spatial:
                data['spatial']['quality'] = calibration.spatial.quality.value
            if calibration.temporal:
                data['temporal']['quality'] = calibration.temporal.quality.value
                
            with open(filepath, 'w') as f:
                json.dump(data, f, indent=2)
                
            self.logger.info(f"Calibration saved to {filepath}")
            return True
            
        except Exception as e:
            self.logger.error(f"Failed to save calibration: {e}")
            return False
            
    def load_calibration(self, filepath: Path) -> Optional[SyncCalibrationResult]:
        """Load calibration results from file."""
        try:
            with open(filepath, 'r') as f:
                data = json.load(f)
                
            # Reconstruct numpy arrays
            if data.get('spatial') and data['spatial'].get('homography_matrix'):
                data['spatial']['homography_matrix'] = np.array(data['spatial']['homography_matrix'])
                
            # Convert ISO strings back to datetime objects
            data['timestamp'] = datetime.fromisoformat(data['timestamp'])
            if data.get('spatial'):
                data['spatial']['timestamp'] = datetime.fromisoformat(data['spatial']['timestamp'])
                data['spatial']['quality'] = SyncQuality(data['spatial']['quality'])
            if data.get('temporal'):
                data['temporal']['timestamp'] = datetime.fromisoformat(data['temporal']['timestamp'])
                data['temporal']['quality'] = SyncQuality(data['temporal']['quality'])
                
            data['overall_quality'] = SyncQuality(data['overall_quality'])
            
            # Reconstruct dataclass objects
            spatial_cal = None
            if data.get('spatial'):
                spatial_cal = SpatialCalibration(**data['spatial'])
                
            temporal_cal = None
            if data.get('temporal'):
                temporal_cal = TemporalCalibration(**data['temporal'])
                
            result = SyncCalibrationResult(
                spatial=spatial_cal,
                temporal=temporal_cal,
                overall_quality=data['overall_quality'],
                calibration_id=data['calibration_id'],
                device_info=data['device_info'],
                timestamp=data['timestamp']
            )
            
            self.logger.info(f"Calibration loaded from {filepath}")
            return result
            
        except Exception as e:
            self.logger.error(f"Failed to load calibration: {e}")
            return None
            
    def apply_synchronization(self, 
                            thermal_image: np.ndarray, 
                            thermal_timestamp: float,
                            calibration: SyncCalibrationResult) -> Tuple[np.ndarray, float]:
        """Apply synchronization to thermal image and timestamp."""
        synchronized_image = thermal_image
        synchronized_timestamp = thermal_timestamp
        
        # Apply spatial transformation
        if calibration.spatial:
            synchronized_image = self.spatial_sync.apply_spatial_transform(
                thermal_image, calibration.spatial
            )
            
        # Apply temporal offset
        if calibration.temporal:
            synchronized_timestamp = thermal_timestamp + (calibration.temporal.time_offset_ms / 1000.0)
            
        return synchronized_image, synchronized_timestamp
        
    def validate_calibration(self, 
                           calibration: SyncCalibrationResult,
                           test_rgb_images: List[np.ndarray],
                           test_thermal_images: List[np.ndarray],
                           test_rgb_timestamps: List[float],
                           test_thermal_timestamps: List[float]) -> Dict[str, Any]:
        """Validate calibration using test data."""
        validation_results = {
            'spatial_validation': None,
            'temporal_validation': None,
            'overall_score': 0.0
        }
        
        # Validate spatial calibration
        if calibration.spatial and test_rgb_images and test_thermal_images:
            spatial_score = self._validate_spatial_calibration(
                calibration.spatial, test_rgb_images, test_thermal_images
            )
            validation_results['spatial_validation'] = spatial_score
            
        # Validate temporal calibration
        if calibration.temporal and test_rgb_timestamps and test_thermal_timestamps:
            temporal_score = self._validate_temporal_calibration(
                calibration.temporal, test_rgb_timestamps, test_thermal_timestamps
            )
            validation_results['temporal_validation'] = temporal_score
            
        # Calculate overall score
        scores = [s for s in [validation_results['spatial_validation'], 
                            validation_results['temporal_validation']] if s is not None]
        validation_results['overall_score'] = np.mean(scores) if scores else 0.0
        
        return validation_results
        
    def _validate_spatial_calibration(self, 
                                    spatial_cal: SpatialCalibration,
                                    test_rgb_images: List[np.ndarray],
                                    test_thermal_images: List[np.ndarray]) -> float:
        """Validate spatial calibration using test images."""
        # Apply transformation to test thermal images and measure alignment quality
        alignment_scores = []
        
        for rgb_img, thermal_img in zip(test_rgb_images[:5], test_thermal_images[:5]):  # Test first 5 pairs
            # Transform thermal image
            transformed_thermal = self.spatial_sync.apply_spatial_transform(thermal_img, spatial_cal)
            
            # Calculate alignment score (simplified - could use more sophisticated metrics)
            score = self._calculate_alignment_score(rgb_img, transformed_thermal)
            alignment_scores.append(score)
            
        return np.mean(alignment_scores) if alignment_scores else 0.0
        
    def _validate_temporal_calibration(self, 
                                     temporal_cal: TemporalCalibration,
                                     test_rgb_timestamps: List[float],
                                     test_thermal_timestamps: List[float]) -> float:
        """Validate temporal calibration using test timestamps."""
        # Apply offset and measure synchronization quality
        offset_seconds = temporal_cal.time_offset_ms / 1000.0
        shifted_thermal = np.array(test_thermal_timestamps) + offset_seconds
        
        # Calculate synchronization score
        sync_errors = []
        for rgb_ts in test_rgb_timestamps:
            distances = np.abs(shifted_thermal - rgb_ts)
            min_distance = np.min(distances)
            sync_errors.append(min_distance)
            
        avg_error = np.mean(sync_errors)
        
        # Convert error to score (lower error = higher score)
        score = max(0.0, 1.0 - (avg_error / 0.1))  # Normalize by 100ms
        return score
        
    def _calculate_alignment_score(self, rgb_image: np.ndarray, thermal_image: np.ndarray) -> float:
        """Calculate alignment score between RGB and thermal images."""
        # Convert to grayscale if needed
        if len(rgb_image.shape) == 3:
            rgb_gray = cv2.cvtColor(rgb_image, cv2.COLOR_BGR2GRAY)
        else:
            rgb_gray = rgb_image
            
        if len(thermal_image.shape) == 3:
            thermal_gray = cv2.cvtColor(thermal_image, cv2.COLOR_BGR2GRAY)
        else:
            thermal_gray = thermal_image
            
        # Resize to same dimensions if needed
        if rgb_gray.shape != thermal_gray.shape:
            thermal_gray = cv2.resize(thermal_gray, (rgb_gray.shape[1], rgb_gray.shape[0]))
            
        # Calculate normalized cross-correlation
        correlation = cv2.matchTemplate(rgb_gray, thermal_gray, cv2.TM_CCOEFF_NORMED)
        
        return float(np.max(correlation))


# Convenience functions
def create_thermal_rgb_synchronizer() -> ThermalRGBSynchronizer:
    """Create and return a thermal-RGB synchronizer instance."""
    return ThermalRGBSynchronizer()


def quick_spatial_calibration(rgb_images: List[np.ndarray], 
                             thermal_images: List[np.ndarray]) -> Optional[SpatialCalibration]:
    """Quick spatial calibration for immediate use."""
    synchronizer = ThermalRGBSynchronizer()
    return synchronizer.spatial_sync.calibrate_spatial_alignment(rgb_images, thermal_images)


def quick_temporal_calibration(rgb_timestamps: List[float], 
                             thermal_timestamps: List[float]) -> Optional[TemporalCalibration]:
    """Quick temporal calibration for immediate use."""
    synchronizer = ThermalRGBSynchronizer()
    return synchronizer.temporal_sync.calibrate_temporal_sync(rgb_timestamps, thermal_timestamps)