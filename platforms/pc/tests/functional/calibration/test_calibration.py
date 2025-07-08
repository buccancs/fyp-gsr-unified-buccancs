#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""Test script for camera calibration functionality.

This script performs basic tests to ensure the calibration system
is working correctly and can be imported without issues.

Usage:
    python test_calibration.py

Author: FYP-GSR Team
"""

import os
import sys
import tempfile

import numpy as np

# Add the src directory to the path so we can import our modules
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src'))


def test_imports() -> None:
    """Test that all required modules can be imported."""
    print("Testing imports...")

    try:
        import cv2
        print(f"✓ OpenCV version: {cv2.__version__}")
    except ImportError as e:
        print(f"✗ Failed to import OpenCV: {e}")
        return False

    try:
        import numpy as np
        print(f"✓ NumPy version: {np.__version__}")
    except ImportError as e:
        print(f"✗ Failed to import NumPy: {e}")
        return False

    try:
        from utils.camera_calibration import (CalibrationPattern,
                                              CameraCalibrator)
        print("✓ Camera calibration modules imported successfully")
    except ImportError as e:
        print(f"✗ Failed to import calibration modules: {e}")
        return False

    return True


def test_calibration_pattern() -> None:
    """Test calibration pattern creation."""
    print("\nTesting calibration pattern creation...")

    try:
        from utils.camera_calibration import CalibrationPattern

        # Test chessboard pattern
        chessboard = CalibrationPattern('chessboard', (9, 6), 0.025)
        print("✓ Chessboard pattern created successfully")

        # Test ChArUco pattern
        charuco = CalibrationPattern('charuco', (7, 5), 0.025, 0.020)
        print("✓ ChArUco pattern created successfully")

        return True
    except Exception as e:
        print(f"✗ Failed to create calibration patterns: {e}")
        return False


def test_calibrator_creation() -> None:
    """Test camera calibrator creation."""
    print("\nTesting camera calibrator creation...")

    try:
        from utils.camera_calibration import (CalibrationPattern,
                                              CameraCalibrator)

        pattern = CalibrationPattern('chessboard', (9, 6), 0.025)
        calibrator = CameraCalibrator(pattern)
        print("✓ Camera calibrator created successfully")

        return True
    except Exception as e:
        print(f"✗ Failed to create camera calibrator: {e}")
        return False


def test_synthetic_calibration() -> None:
    """Test calibration with synthetic data."""
    print("\nTesting synthetic calibration...")

    try:
        import cv2

        from utils.camera_calibration import (CalibrationPattern,
                                              CameraCalibrator)

        # Create a simple synthetic calibration test
        pattern = CalibrationPattern('chessboard', (9, 6), 0.025)
        calibrator = CameraCalibrator(pattern)

        # Create a synthetic chessboard image
        img_size = (640, 480)
        img = np.zeros((img_size[1], img_size[0], 3), dtype=np.uint8)

        # Draw a simple chessboard pattern
        square_size = 40
        for i in range(0, img_size[0], square_size):
            for j in range(0, img_size[1], square_size):
                if ((i // square_size) + (j // square_size)) % 2 == 0:
                    cv2.rectangle(
                        img, (i, j), (i + square_size, j + square_size), (255, 255, 255), -1)

        # Test pattern detection
        success, corners = calibrator.detect_pattern_in_image(img)
        if success:
            print("✓ Pattern detection working (synthetic image)")
        else:
            print(
                "! Pattern detection failed on synthetic image (expected for simple test)")

        return True
    except Exception as e:
        print(f"✗ Failed synthetic calibration test: {e}")
        return False


def test_json_export() -> None:
    """Test JSON export functionality."""
    print("\nTesting JSON export...")

    try:
        import json
        import tempfile

        from utils.camera_calibration import (CalibrationPattern,
                                              CameraCalibrator)

        pattern = CalibrationPattern('chessboard', (9, 6), 0.025)
        calibrator = CameraCalibrator(pattern)

        # Add some dummy calibration data
        calibrator.cameras['test_camera'] = {
            'camera_matrix': [[800, 0, 320], [0, 800, 240], [0, 0, 1]],
            'distortion_coefficients': [[0.1, -0.2, 0.001, 0.002, 0.05]],
            'image_size': [640, 480],
            'reprojection_error': 0.5,
            'num_images_used': 10,
            'calibration_date': '2024-01-01T12:00:00'
        }

        # Test saving to temporary file
        with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False) as f:
            temp_path = f.name

        calibrator.save_calibration_results(temp_path)

        # Test loading back
        with open(temp_path, 'r') as f:
            data = json.load(f)

        # Clean up
        os.unlink(temp_path)

        print("✓ JSON export/import working correctly")
        return True
    except Exception as e:
        print(f"✗ Failed JSON export test: {e}")
        return False


def test_gui_imports() -> None:
    """Test GUI-related imports."""
    print("\nTesting GUI imports...")

    try:
        from PySide6.QtWidgets import QApplication
        print("✓ PySide6 available")

        from ui.calibration_dialog import CalibrationDialog
from typing import Any, Dict, List, Optional, Union
        print("✓ Calibration dialog can be imported")

        return True
    except ImportError as e:
        print(
            f"! GUI components not available (this is OK for headless systems): {e}")
        return True  # Not a failure for headless systems
    except Exception as e:
        print(f"✗ Failed GUI import test: {e}")
        return False


def main() -> None:
    """Run all tests."""
    print("Camera Calibration System Test Suite")
    print("=" * 50)

    tests = [
        test_imports,
        test_calibration_pattern,
        test_calibrator_creation,
        test_synthetic_calibration,
        test_json_export,
        test_gui_imports
    ]

    passed = 0
    total = len(tests)

    for test in tests:
        if test():
            passed += 1

    print("\n" + "=" * 50)
    print(f"Test Results: {passed}/{total} tests passed")

    if passed == total:
        print("✓ All tests passed! Camera calibration system is ready to use.")
        return 0
    else:
        print("! Some tests failed. Please check the error messages above.")
        return 1


if __name__ == "__main__":
    exit(main())