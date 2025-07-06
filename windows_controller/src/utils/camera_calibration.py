"""
Camera Calibration Module for Multi-Camera System

This module provides comprehensive camera calibration functionality for the
FYP-GSR Unified Recording and Calibration System. It supports:
- Intrinsic calibration for individual cameras
- Extrinsic calibration between camera pairs
- Multiple calibration patterns (chessboard, ChArUco)
- Video-based calibration using recorded sessions
- Cross-platform compatibility (Windows, macOS, Linux)

Author: FYP-GSR Team
"""

import cv2
import numpy as np
import json
import os
import glob
import logging
from typing import List, Tuple, Dict, Optional, Union
from pathlib import Path
import argparse
from datetime import datetime


class CalibrationPattern:
    """Represents a calibration pattern (chessboard or ChArUco board)."""
    
    def __init__(self, pattern_type: str, grid_size: Tuple[int, int], 
                 square_size: float, marker_size: float = None):
        """
        Initialize calibration pattern.
        
        Args:
            pattern_type: 'chessboard' or 'charuco'
            grid_size: (width, height) in number of corners/markers
            square_size: Size of squares in real-world units (e.g., meters)
            marker_size: Size of ArUco markers (for ChArUco only)
        """
        self.pattern_type = pattern_type.lower()
        self.grid_size = grid_size
        self.square_size = square_size
        self.marker_size = marker_size or (square_size * 0.8)
        
        if self.pattern_type == 'charuco':
            # Create ChArUco board
            self.aruco_dict = cv2.aruco.Dictionary_get(cv2.aruco.DICT_6X6_250)
            self.board = cv2.aruco.CharucoBoard_create(
                grid_size[0], grid_size[1], square_size, self.marker_size, self.aruco_dict
            )
        elif self.pattern_type == 'chessboard':
            # Create object points for chessboard
            self.object_points = np.zeros((grid_size[0] * grid_size[1], 3), np.float32)
            self.object_points[:, :2] = np.mgrid[0:grid_size[0], 0:grid_size[1]].T.reshape(-1, 2)
            self.object_points *= square_size
        else:
            raise ValueError(f"Unsupported pattern type: {pattern_type}")


class CameraCalibrator:
    """Main camera calibration class."""
    
    def __init__(self, pattern: CalibrationPattern, logger: logging.Logger = None):
        """
        Initialize camera calibrator.
        
        Args:
            pattern: Calibration pattern to use
            logger: Logger instance
        """
        self.pattern = pattern
        self.logger = logger or logging.getLogger(__name__)
        
        # Storage for calibration data
        self.cameras = {}  # camera_name -> calibration data
        
    def detect_pattern_in_image(self, image: np.ndarray) -> Tuple[bool, np.ndarray]:
        """
        Detect calibration pattern in an image.
        
        Args:
            image: Input image (BGR or grayscale)
            
        Returns:
            (success, corners): Detection success and corner coordinates
        """
        if len(image.shape) == 3:
            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
        else:
            gray = image
            
        if self.pattern.pattern_type == 'chessboard':
            # Detect chessboard corners
            ret, corners = cv2.findChessboardCorners(
                gray, self.pattern.grid_size,
                cv2.CALIB_CB_ADAPTIVE_THRESH + cv2.CALIB_CB_NORMALIZE_IMAGE
            )
            
            if ret:
                # Refine corner positions
                criteria = (cv2.TERM_CRITERIA_EPS + cv2.TERM_CRITERIA_MAX_ITER, 30, 0.001)
                corners = cv2.cornerSubPix(gray, corners, (11, 11), (-1, -1), criteria)
                
            return ret, corners
            
        elif self.pattern.pattern_type == 'charuco':
            # Detect ChArUco markers
            corners, ids, _ = cv2.aruco.detectMarkers(gray, self.pattern.aruco_dict)
            
            if len(corners) > 0:
                # Interpolate ChArUco corners
                ret, charuco_corners, charuco_ids = cv2.aruco.interpolateCornersCharuco(
                    corners, ids, gray, self.pattern.board
                )
                return ret > 0, charuco_corners
            else:
                return False, None
                
        return False, None
    
    def extract_frames_from_video(self, video_path: str, max_frames: int = 50) -> List[np.ndarray]:
        """
        Extract frames from video file.
        
        Args:
            video_path: Path to video file
            max_frames: Maximum number of frames to extract
            
        Returns:
            List of extracted frames
        """
        frames = []
        cap = cv2.VideoCapture(video_path)
        
        if not cap.isOpened():
            self.logger.error(f"Could not open video: {video_path}")
            return frames
            
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        frame_interval = max(1, total_frames // max_frames)
        
        frame_idx = 0
        while len(frames) < max_frames:
            cap.set(cv2.CAP_PROP_POS_FRAMES, frame_idx)
            ret, frame = cap.read()
            
            if not ret:
                break
                
            frames.append(frame)
            frame_idx += frame_interval
            
        cap.release()
        self.logger.info(f"Extracted {len(frames)} frames from {video_path}")
        return frames
    
    def load_image_sequence(self, image_dir: str) -> List[np.ndarray]:
        """
        Load images from directory.
        
        Args:
            image_dir: Directory containing images
            
        Returns:
            List of loaded images
        """
        images = []
        image_extensions = ['*.jpg', '*.jpeg', '*.png', '*.bmp', '*.tiff']
        
        for ext in image_extensions:
            pattern_path = os.path.join(image_dir, ext)
            for img_path in glob.glob(pattern_path):
                img = cv2.imread(img_path)
                if img is not None:
                    images.append(img)
                    
        self.logger.info(f"Loaded {len(images)} images from {image_dir}")
        return images
    
    def calibrate_camera_intrinsics(self, camera_name: str, 
                                  images: List[np.ndarray]) -> Dict:
        """
        Calibrate intrinsic parameters for a single camera.
        
        Args:
            camera_name: Name identifier for the camera
            images: List of calibration images
            
        Returns:
            Dictionary containing calibration results
        """
        object_points = []
        image_points = []
        image_size = None
        
        self.logger.info(f"Starting intrinsic calibration for {camera_name}")
        
        for i, image in enumerate(images):
            success, corners = self.detect_pattern_in_image(image)
            
            if success:
                if self.pattern.pattern_type == 'chessboard':
                    object_points.append(self.pattern.object_points)
                    image_points.append(corners)
                elif self.pattern.pattern_type == 'charuco':
                    # For ChArUco, we need to get the object points for detected corners
                    obj_pts = self.pattern.board.chessboardCorners[corners]
                    object_points.append(obj_pts)
                    image_points.append(corners)
                    
                if image_size is None:
                    image_size = (image.shape[1], image.shape[0])
                    
                self.logger.debug(f"Pattern detected in image {i+1}/{len(images)}")
            else:
                self.logger.debug(f"Pattern not detected in image {i+1}/{len(images)}")
        
        if len(object_points) < 10:
            raise ValueError(f"Insufficient calibration images for {camera_name}. "
                           f"Found {len(object_points)}, need at least 10.")
        
        self.logger.info(f"Using {len(object_points)} images for calibration")
        
        # Perform camera calibration
        ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.calibrateCamera(
            object_points, image_points, image_size, None, None
        )
        
        # Calculate reprojection error
        total_error = 0
        for i in range(len(object_points)):
            projected_points, _ = cv2.projectPoints(
                object_points[i], rvecs[i], tvecs[i], camera_matrix, dist_coeffs
            )
            error = cv2.norm(image_points[i], projected_points, cv2.NORM_L2) / len(projected_points)
            total_error += error
            
        mean_error = total_error / len(object_points)
        
        # Store calibration results
        calibration_data = {
            'camera_matrix': camera_matrix.tolist(),
            'distortion_coefficients': dist_coeffs.tolist(),
            'image_size': image_size,
            'reprojection_error': mean_error,
            'num_images_used': len(object_points),
            'calibration_date': datetime.now().isoformat()
        }
        
        self.cameras[camera_name] = calibration_data
        
        self.logger.info(f"Intrinsic calibration completed for {camera_name}")
        self.logger.info(f"Reprojection error: {mean_error:.3f} pixels")
        
        return calibration_data
    
    def calibrate_stereo_extrinsics(self, camera1_name: str, camera2_name: str,
                                   images1: List[np.ndarray], images2: List[np.ndarray]) -> Dict:
        """
        Calibrate extrinsic parameters between two cameras.
        
        Args:
            camera1_name: Name of first camera
            camera2_name: Name of second camera
            images1: Calibration images from first camera
            images2: Calibration images from second camera (synchronized)
            
        Returns:
            Dictionary containing stereo calibration results
        """
        if len(images1) != len(images2):
            raise ValueError("Number of images must be equal for both cameras")
            
        if camera1_name not in self.cameras or camera2_name not in self.cameras:
            raise ValueError("Both cameras must be intrinsically calibrated first")
        
        object_points = []
        image_points1 = []
        image_points2 = []
        
        self.logger.info(f"Starting stereo calibration between {camera1_name} and {camera2_name}")
        
        for i, (img1, img2) in enumerate(zip(images1, images2)):
            success1, corners1 = self.detect_pattern_in_image(img1)
            success2, corners2 = self.detect_pattern_in_image(img2)
            
            if success1 and success2:
                if self.pattern.pattern_type == 'chessboard':
                    object_points.append(self.pattern.object_points)
                elif self.pattern.pattern_type == 'charuco':
                    obj_pts = self.pattern.board.chessboardCorners[corners1]
                    object_points.append(obj_pts)
                    
                image_points1.append(corners1)
                image_points2.append(corners2)
                
                self.logger.debug(f"Pattern detected in both images {i+1}/{len(images1)}")
        
        if len(object_points) < 5:
            raise ValueError(f"Insufficient synchronized calibration images. "
                           f"Found {len(object_points)}, need at least 5.")
        
        # Get intrinsic parameters
        K1 = np.array(self.cameras[camera1_name]['camera_matrix'])
        D1 = np.array(self.cameras[camera1_name]['distortion_coefficients'])
        K2 = np.array(self.cameras[camera2_name]['camera_matrix'])
        D2 = np.array(self.cameras[camera2_name]['distortion_coefficients'])
        
        image_size = tuple(self.cameras[camera1_name]['image_size'])
        
        # Perform stereo calibration
        ret, K1_new, D1_new, K2_new, D2_new, R, T, E, F = cv2.stereoCalibrate(
            object_points, image_points1, image_points2,
            K1, D1, K2, D2, image_size,
            flags=cv2.CALIB_FIX_INTRINSIC
        )
        
        # Store extrinsic calibration results
        extrinsic_key = f"{camera1_name}_to_{camera2_name}"
        extrinsic_data = {
            'rotation_matrix': R.tolist(),
            'translation_vector': T.tolist(),
            'essential_matrix': E.tolist(),
            'fundamental_matrix': F.tolist(),
            'reprojection_error': ret,
            'num_image_pairs_used': len(object_points),
            'calibration_date': datetime.now().isoformat()
        }
        
        if 'extrinsics' not in self.cameras:
            self.cameras['extrinsics'] = {}
        self.cameras['extrinsics'][extrinsic_key] = extrinsic_data
        
        self.logger.info(f"Stereo calibration completed between {camera1_name} and {camera2_name}")
        self.logger.info(f"Stereo reprojection error: {ret:.3f} pixels")
        
        return extrinsic_data
    
    def save_calibration_results(self, output_path: str) -> None:
        """
        Save calibration results to JSON file.
        
        Args:
            output_path: Path to output JSON file
        """
        calibration_results = {
            'pattern_info': {
                'type': self.pattern.pattern_type,
                'grid_size': self.pattern.grid_size,
                'square_size': self.pattern.square_size,
                'marker_size': self.pattern.marker_size
            },
            'cameras': self.cameras,
            'export_date': datetime.now().isoformat(),
            'opencv_version': cv2.__version__
        }
        
        os.makedirs(os.path.dirname(output_path), exist_ok=True)
        
        with open(output_path, 'w') as f:
            json.dump(calibration_results, f, indent=2)
            
        self.logger.info(f"Calibration results saved to {output_path}")
    
    def load_calibration_results(self, input_path: str) -> None:
        """
        Load calibration results from JSON file.
        
        Args:
            input_path: Path to input JSON file
        """
        with open(input_path, 'r') as f:
            data = json.load(f)
            
        self.cameras = data.get('cameras', {})
        
        # Restore pattern info if available
        pattern_info = data.get('pattern_info', {})
        if pattern_info:
            self.pattern = CalibrationPattern(
                pattern_info['type'],
                tuple(pattern_info['grid_size']),
                pattern_info['square_size'],
                pattern_info.get('marker_size')
            )
            
        self.logger.info(f"Calibration results loaded from {input_path}")


def create_cli_parser() -> argparse.ArgumentParser:
    """Create command-line argument parser for calibration tool."""
    parser = argparse.ArgumentParser(
        description="Camera Calibration Tool for FYP-GSR System",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Calibrate RGB camera from video
  python camera_calibration.py --rgb-video session1/rgb_video.mp4 --output calibration.json
  
  # Calibrate RGB and thermal cameras
  python camera_calibration.py --rgb-video session1/rgb_video.mp4 \\
                               --thermal-frames session1/thermal_frames/ \\
                               --output calibration.json
  
  # Use ChArUco pattern
  python camera_calibration.py --rgb-video session1/rgb_video.mp4 \\
                               --pattern charuco --grid-size 7x5 \\
                               --output calibration.json
        """
    )
    
    # Input sources
    parser.add_argument('--rgb-video', type=str,
                       help='Path to RGB video file (MP4)')
    parser.add_argument('--thermal-frames', type=str,
                       help='Path to directory containing thermal frame images')
    parser.add_argument('--webcam-video', type=str,
                       help='Path to webcam video file')
    parser.add_argument('--rgb-frames', type=str,
                       help='Path to directory containing RGB frame images')
    
    # Pattern configuration
    parser.add_argument('--pattern', type=str, choices=['chessboard', 'charuco'],
                       default='chessboard', help='Calibration pattern type')
    parser.add_argument('--grid-size', type=str, default='9x6',
                       help='Pattern grid size as WIDTHxHEIGHT (e.g., 9x6)')
    parser.add_argument('--square-size', type=float, default=0.025,
                       help='Square size in meters (default: 0.025m = 25mm)')
    parser.add_argument('--marker-size', type=float,
                       help='ArUco marker size in meters (for ChArUco pattern)')
    
    # Output configuration
    parser.add_argument('--output', type=str, required=True,
                       help='Output path for calibration JSON file')
    parser.add_argument('--max-frames', type=int, default=50,
                       help='Maximum number of frames to extract from videos')
    
    # Processing options
    parser.add_argument('--verbose', '-v', action='store_true',
                       help='Enable verbose logging')
    
    return parser


def main():
    """Main function for CLI usage."""
    parser = create_cli_parser()
    args = parser.parse_args()
    
    # Set up logging
    log_level = logging.DEBUG if args.verbose else logging.INFO
    logging.basicConfig(
        level=log_level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    logger = logging.getLogger(__name__)
    
    # Parse grid size
    try:
        grid_width, grid_height = map(int, args.grid_size.split('x'))
        grid_size = (grid_width, grid_height)
    except ValueError:
        logger.error(f"Invalid grid size format: {args.grid_size}. Use WIDTHxHEIGHT (e.g., 9x6)")
        return 1
    
    # Create calibration pattern
    pattern = CalibrationPattern(
        args.pattern, grid_size, args.square_size, args.marker_size
    )
    
    # Create calibrator
    calibrator = CameraCalibrator(pattern, logger)
    
    try:
        # Process RGB video/frames
        if args.rgb_video:
            logger.info(f"Processing RGB video: {args.rgb_video}")
            rgb_images = calibrator.extract_frames_from_video(args.rgb_video, args.max_frames)
            if rgb_images:
                calibrator.calibrate_camera_intrinsics('rgb_camera', rgb_images)
        elif args.rgb_frames:
            logger.info(f"Processing RGB frames: {args.rgb_frames}")
            rgb_images = calibrator.load_image_sequence(args.rgb_frames)
            if rgb_images:
                calibrator.calibrate_camera_intrinsics('rgb_camera', rgb_images)
        
        # Process thermal frames
        if args.thermal_frames:
            logger.info(f"Processing thermal frames: {args.thermal_frames}")
            thermal_images = calibrator.load_image_sequence(args.thermal_frames)
            if thermal_images:
                calibrator.calibrate_camera_intrinsics('thermal_camera', thermal_images)
        
        # Process webcam video
        if args.webcam_video:
            logger.info(f"Processing webcam video: {args.webcam_video}")
            webcam_images = calibrator.extract_frames_from_video(args.webcam_video, args.max_frames)
            if webcam_images:
                calibrator.calibrate_camera_intrinsics('webcam_camera', webcam_images)
        
        # Perform stereo calibration if multiple cameras are available
        camera_names = list(calibrator.cameras.keys())
        if len(camera_names) >= 2:
            logger.info("Performing stereo calibration between camera pairs")
            
            # RGB to thermal
            if 'rgb_camera' in camera_names and 'thermal_camera' in camera_names:
                if args.rgb_video and args.thermal_frames:
                    # For stereo calibration, we need synchronized images
                    # This is a simplified approach - in practice, you'd need
                    # to ensure the images are truly synchronized
                    logger.warning("Stereo calibration requires synchronized images. "
                                 "Ensure RGB and thermal images were captured simultaneously.")
                    
                    rgb_images = calibrator.extract_frames_from_video(args.rgb_video, args.max_frames)
                    thermal_images = calibrator.load_image_sequence(args.thermal_frames)
                    
                    # Take minimum number of images
                    min_images = min(len(rgb_images), len(thermal_images))
                    if min_images >= 5:
                        calibrator.calibrate_stereo_extrinsics(
                            'rgb_camera', 'thermal_camera',
                            rgb_images[:min_images], thermal_images[:min_images]
                        )
        
        # Save results
        calibrator.save_calibration_results(args.output)
        logger.info("Calibration completed successfully!")
        
        return 0
        
    except Exception as e:
        logger.error(f"Calibration failed: {str(e)}")
        return 1


if __name__ == "__main__":
    exit(main())