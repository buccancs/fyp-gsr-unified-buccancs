#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Standalone Camera Calibration Tool for FYP-GSR System

This is a standalone script that provides easy access to the camera calibration
functionality without needing to run the full GUI application.

Usage:
    python calibrate_cameras.py --rgb-video session1/rgb_video.mp4 --output calibration.json
    python calibrate_cameras.py --help

Author: FYP-GSR Team
"""

from utils.camera_calibration import main
import os
import sys

# Add the src directory to the path so we can import our modules
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

# Import and run the calibration main function

if __name__ == "__main__":
    exit(main())
