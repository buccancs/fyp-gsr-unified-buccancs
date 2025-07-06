#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Main entry point for the PC Controller App.
This script initializes the application and launches the main window.
Cross-platform support: Windows, macOS, Linux.
"""

from utils.logger import setup_logger
from utils.camera_calibration import main as calibration_main
from ui.main_window import MainWindow
import argparse
import logging
import os
import sys

from PySide6.QtCore import Qt
from PySide6.QtWidgets import QApplication

# Add the parent directory to the path so we can import our modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Import our modules


def main():
    """
    Main function to initialize and run the application.
    """
    # Parse command line arguments
    parser = argparse.ArgumentParser(
        description="GSR & Dual-Video Recording System",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  # Run GUI application
  python main.py

  # Run camera calibration from command line
  python main.py --calibrate --rgb-video session1/rgb_video.mp4 --output calibration.json

  # Run calibration with thermal camera
  python main.py --calibrate --rgb-video session1/rgb_video.mp4 \\
                             --thermal-frames session1/thermal_frames/ \\
                             --output calibration.json
        """
    )

    # Add calibration mode flag
    parser.add_argument('--calibrate', action='store_true',
                        help='Run in calibration mode (CLI)')

    # Calibration arguments (only used when --calibrate is specified)
    parser.add_argument('--rgb-video', type=str,
                        help='Path to RGB video file (MP4)')
    parser.add_argument(
        '--thermal-frames',
        type=str,
        help='Path to directory containing thermal frame images')
    parser.add_argument('--webcam-video', type=str,
                        help='Path to webcam video file')
    parser.add_argument('--rgb-frames', type=str,
                        help='Path to directory containing RGB frame images')
    parser.add_argument(
        '--pattern',
        type=str,
        choices=[
            'chessboard',
            'charuco'],
        default='chessboard',
        help='Calibration pattern type')
    parser.add_argument('--grid-size', type=str, default='9x6',
                        help='Pattern grid size as WIDTHxHEIGHT (e.g., 9x6)')
    parser.add_argument('--square-size', type=float, default=0.025,
                        help='Square size in meters (default: 0.025m = 25mm)')
    parser.add_argument(
        '--marker-size',
        type=float,
        help='ArUco marker size in meters (for ChArUco pattern)')
    parser.add_argument('--output', type=str,
                        help='Output path for calibration JSON file')
    parser.add_argument('--max-frames', type=int, default=50,
                        help='Maximum number of frames to extract from videos')
    parser.add_argument('--verbose', '-v', action='store_true',
                        help='Enable verbose logging')

    args = parser.parse_args()

    # Set up logging
    setup_logger()
    logger = logging.getLogger(__name__)

    # Check if running in calibration mode
    if args.calibrate:
        logger.info("Starting camera calibration (CLI mode)")

        # Prepare arguments for calibration module
        calibration_args = [
            '--pattern', args.pattern,
            '--grid-size', args.grid_size,
            '--square-size', str(args.square_size),
            '--max-frames', str(args.max_frames)
        ]

        if args.rgb_video:
            calibration_args.extend(['--rgb-video', args.rgb_video])
        if args.rgb_frames:
            calibration_args.extend(['--rgb-frames', args.rgb_frames])
        if args.thermal_frames:
            calibration_args.extend(['--thermal-frames', args.thermal_frames])
        if args.webcam_video:
            calibration_args.extend(['--webcam-video', args.webcam_video])
        if args.marker_size:
            calibration_args.extend(['--marker-size', str(args.marker_size)])
        if args.output:
            calibration_args.extend(['--output', args.output])
        else:
            calibration_args.extend(['--output', 'calibration_results.json'])
        if args.verbose:
            calibration_args.append('--verbose')

        # Override sys.argv for calibration module
        original_argv = sys.argv
        sys.argv = ['camera_calibration.py'] + calibration_args

        try:
            # Run calibration
            exit_code = calibration_main()
            sys.exit(exit_code)
        except Exception as e:
            logger.error(f"Calibration failed: {str(e)}")
            sys.exit(1)
        finally:
            # Restore original argv
            sys.argv = original_argv

    # Run GUI application
    logger.info("Starting PC Controller App (GUI mode)")

    # Create the Qt Application
    app = QApplication(sys.argv)
    app.setApplicationName("GSR & Dual-Video Recording System")
    app.setOrganizationName("BuccaNCS")

    # Enable High DPI scaling
    app.setAttribute(Qt.AA_EnableHighDpiScaling, True)
    app.setAttribute(Qt.AA_UseHighDpiPixmaps, True)

    # Create and show the main window
    main_window = MainWindow()
    main_window.show()

    # Run the application event loop
    sys.exit(app.exec_())


if __name__ == "__main__":
    main()
