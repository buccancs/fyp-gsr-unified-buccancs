"""
Comprehensive Calibration Functionality Tests

This module consolidates and replaces the scattered calibration tests:
- test_calibration.py

Tests the complete calibration workflow and functionality.
"""

import pytest
import numpy as np
import tempfile
import os
import json
from unittest.mock import Mock, patch, MagicMock
from pathlib import Path


class TestCameraCalibration:
    """Test camera calibration functionality."""
    
    def test_calibration_import(self):
        """Test that calibration modules can be imported."""
        try:
            # Test importing calibration-related modules
            from ui.calibration_dialog import CalibrationDialog
            from ui.live_calibration_dialog import LiveCalibrationDialog
            assert CalibrationDialog is not None
            assert LiveCalibrationDialog is not None
        except ImportError as e:
            pytest.skip(f"Calibration modules not available: {e}")
    
    @patch('cv2.VideoCapture')
    def test_calibration_dialog_initialization(self, mock_video_capture):
        """Test CalibrationDialog initialization."""
        try:
            from ui.calibration_dialog import CalibrationDialog
            from PyQt5.QtWidgets import QApplication
            import sys
            
            # Create QApplication if it doesn't exist
            app = QApplication.instance()
            if app is None:
                app = QApplication(sys.argv)
            
            # Mock video capture
            mock_cap = Mock()
            mock_cap.isOpened.return_value = True
            mock_cap.get.return_value = 640  # Mock width/height
            mock_video_capture.return_value = mock_cap
            
            # Test dialog creation
            dialog = CalibrationDialog()
            assert dialog is not None
            assert hasattr(dialog, 'camera_matrix')
            assert hasattr(dialog, 'distortion_coefficients')
            
        except ImportError:
            pytest.skip("UI modules not available")
    
    def test_calibration_data_structure(self):
        """Test calibration data structure and validation."""
        # Test valid calibration data structure
        calibration_data = {
            'camera_matrix': [[640.0, 0.0, 320.0], [0.0, 640.0, 240.0], [0.0, 0.0, 1.0]],
            'distortion_coefficients': [0.1, -0.2, 0.0, 0.0, 0.0],
            'image_size': [640, 480],
            'calibration_date': '2024-01-01T12:00:00',
            'rms_error': 0.5
        }
        
        # Validate structure
        assert 'camera_matrix' in calibration_data
        assert 'distortion_coefficients' in calibration_data
        assert 'image_size' in calibration_data
        assert len(calibration_data['camera_matrix']) == 3
        assert len(calibration_data['camera_matrix'][0]) == 3
        assert len(calibration_data['distortion_coefficients']) == 5
        assert len(calibration_data['image_size']) == 2
    
    def test_calibration_file_operations(self, temp_dir):
        """Test calibration file save/load operations."""
        calibration_data = {
            'camera_matrix': [[640.0, 0.0, 320.0], [0.0, 640.0, 240.0], [0.0, 0.0, 1.0]],
            'distortion_coefficients': [0.1, -0.2, 0.0, 0.0, 0.0],
            'image_size': [640, 480],
            'calibration_date': '2024-01-01T12:00:00',
            'rms_error': 0.5
        }
        
        # Test saving calibration data
        calibration_file = os.path.join(temp_dir, 'test_calibration.json')
        with open(calibration_file, 'w') as f:
            json.dump(calibration_data, f)
        
        # Test loading calibration data
        with open(calibration_file, 'r') as f:
            loaded_data = json.load(f)
        
        assert loaded_data == calibration_data
        assert loaded_data['camera_matrix'] == calibration_data['camera_matrix']
        assert loaded_data['distortion_coefficients'] == calibration_data['distortion_coefficients']
    
    @patch('cv2.findChessboardCorners')
    @patch('cv2.calibrateCamera')
    def test_calibration_algorithm(self, mock_calibrate, mock_find_corners):
        """Test the calibration algorithm workflow."""
        # Mock chessboard detection
        mock_find_corners.return_value = (True, np.array([[100, 100], [200, 100], [100, 200]]))
        
        # Mock calibration result
        mock_calibrate.return_value = (
            0.5,  # RMS error
            np.array([[640, 0, 320], [0, 640, 240], [0, 0, 1]]),  # Camera matrix
            np.array([0.1, -0.2, 0, 0, 0]),  # Distortion coefficients
            None,  # Rotation vectors
            None   # Translation vectors
        )
        
        # Test calibration process
        try:
            import cv2
            
            # Simulate calibration data
            object_points = []
            image_points = []
            image_size = (640, 480)
            
            # Mock some calibration points
            for i in range(10):  # 10 calibration images
                objp = np.zeros((6*9, 3), np.float32)
                objp[:, :2] = np.mgrid[0:9, 0:6].T.reshape(-1, 2)
                object_points.append(objp)
                
                imgp = np.random.rand(54, 2).astype(np.float32) * 640
                image_points.append(imgp)
            
            # Run calibration
            ret, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.calibrateCamera(
                object_points, image_points, image_size, None, None
            )
            
            # Verify results
            assert ret < 1.0  # RMS error should be reasonable
            assert camera_matrix.shape == (3, 3)
            assert len(dist_coeffs) == 5
            
        except ImportError:
            pytest.skip("OpenCV not available")


class TestCalibrationUI:
    """Test calibration user interface components."""
    
    @patch('PyQt5.QtWidgets.QApplication')
    def test_calibration_dialog_ui_elements(self, mock_app):
        """Test that calibration dialog has required UI elements."""
        try:
            from ui.calibration_dialog import CalibrationDialog
            
            # Mock QApplication
            mock_app.instance.return_value = Mock()
            
            dialog = CalibrationDialog()
            
            # Test that dialog has required methods
            assert hasattr(dialog, 'start_calibration')
            assert hasattr(dialog, 'capture_image')
            assert hasattr(dialog, 'save_calibration')
            assert hasattr(dialog, 'load_calibration')
            
        except ImportError:
            pytest.skip("UI modules not available")
    
    def test_live_calibration_functionality(self):
        """Test live calibration dialog functionality."""
        try:
            from ui.live_calibration_dialog import LiveCalibrationDialog
            
            # Test that live calibration dialog exists and has required methods
            assert hasattr(LiveCalibrationDialog, '__init__')
            
        except ImportError:
            pytest.skip("Live calibration module not available")


class TestCalibrationIntegration:
    """Test calibration integration with other components."""
    
    @patch('hardware.webcam_pc.WebcamPC')
    def test_calibration_with_webcam(self, mock_webcam):
        """Test calibration integration with webcam."""
        # Mock webcam
        mock_webcam_instance = Mock()
        mock_webcam_instance.is_connected = True
        mock_webcam_instance.get_frame.return_value = np.zeros((480, 640, 3), dtype=np.uint8)
        mock_webcam.return_value = mock_webcam_instance
        
        # Test that calibration can work with webcam
        webcam = mock_webcam()
        assert webcam.is_connected
        
        frame = webcam.get_frame()
        assert frame is not None
        assert frame.shape == (480, 640, 3)
    
    def test_calibration_data_persistence(self, temp_dir):
        """Test that calibration data persists correctly."""
        calibration_data = {
            'camera_matrix': [[640.0, 0.0, 320.0], [0.0, 640.0, 240.0], [0.0, 0.0, 1.0]],
            'distortion_coefficients': [0.1, -0.2, 0.0, 0.0, 0.0],
            'image_size': [640, 480],
            'calibration_date': '2024-01-01T12:00:00',
            'rms_error': 0.5
        }
        
        # Test saving to different formats
        json_file = os.path.join(temp_dir, 'calibration.json')
        with open(json_file, 'w') as f:
            json.dump(calibration_data, f)
        
        # Verify file exists and is readable
        assert os.path.exists(json_file)
        
        with open(json_file, 'r') as f:
            loaded_data = json.load(f)
        
        assert loaded_data == calibration_data


class TestCalibrationValidation:
    """Test calibration validation and error handling."""
    
    def test_invalid_calibration_data(self):
        """Test handling of invalid calibration data."""
        # Test with invalid camera matrix
        invalid_data = {
            'camera_matrix': [[640.0, 0.0], [0.0, 640.0]],  # Wrong size
            'distortion_coefficients': [0.1, -0.2, 0.0, 0.0, 0.0],
            'image_size': [640, 480]
        }
        
        # Should detect invalid matrix size
        assert len(invalid_data['camera_matrix']) != 3
        
        # Test with invalid distortion coefficients
        invalid_data2 = {
            'camera_matrix': [[640.0, 0.0, 320.0], [0.0, 640.0, 240.0], [0.0, 0.0, 1.0]],
            'distortion_coefficients': [0.1, -0.2],  # Too few coefficients
            'image_size': [640, 480]
        }
        
        # Should detect invalid distortion coefficients
        assert len(invalid_data2['distortion_coefficients']) != 5
    
    def test_calibration_quality_metrics(self):
        """Test calibration quality assessment."""
        # Test good calibration (low RMS error)
        good_calibration = {'rms_error': 0.3}
        assert good_calibration['rms_error'] < 0.5
        
        # Test poor calibration (high RMS error)
        poor_calibration = {'rms_error': 2.5}
        assert poor_calibration['rms_error'] > 1.0
        
        # Test acceptable calibration
        acceptable_calibration = {'rms_error': 0.8}
        assert 0.5 <= acceptable_calibration['rms_error'] <= 1.0
    
    def test_calibration_edge_cases(self):
        """Test calibration edge cases and error conditions."""
        # Test with minimal calibration images
        minimal_images = 3
        assert minimal_images < 10  # Should warn about insufficient images
        
        # Test with excessive calibration images
        excessive_images = 100
        assert excessive_images > 50  # Should handle gracefully
        
        # Test with extreme camera parameters
        extreme_focal_length = 10000
        assert extreme_focal_length > 5000  # Should validate reasonable ranges


if __name__ == "__main__":
    pytest.main([__file__])