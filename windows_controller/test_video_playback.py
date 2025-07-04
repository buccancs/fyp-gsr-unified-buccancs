#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Test script for the Video Playback Window.
This script tests the video playback functionality independently.
"""

import sys
import os

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

from PySide6.QtWidgets import QApplication, QMainWindow, QVBoxLayout, QWidget, QPushButton
from PySide6.QtCore import Qt
from ui.video_playback_window import VideoPlaybackWindow

class TestMainWindow(QMainWindow):
    """
    Test main window for the video playback functionality.
    """

    def __init__(self):
        super().__init__()
        self.setWindowTitle("Video Playback Test")
        self.setGeometry(100, 100, 1400, 900)

        # Create central widget
        central_widget = QWidget()
        self.setCentralWidget(central_widget)

        # Create layout
        layout = QVBoxLayout(central_widget)

        # Create test buttons
        self.start_recording_button = QPushButton("Start Recording")
        self.start_recording_button.clicked.connect(self.start_recording)
        layout.addWidget(self.start_recording_button)

        self.stop_recording_button = QPushButton("Stop Recording")
        self.stop_recording_button.clicked.connect(self.stop_recording)
        layout.addWidget(self.stop_recording_button)

        # Create video playback window
        self.video_playback = VideoPlaybackWindow()
        layout.addWidget(self.video_playback)

        # Connect signals
        self.video_playback.video_changed.connect(self.on_video_changed)
        self.video_playback.annotation_added.connect(self.on_annotation_added)
        self.video_playback.playback_started.connect(self.on_playback_started)
        self.video_playback.playback_stopped.connect(self.on_playback_stopped)

        print("Video Playback Test Window initialized")

    def start_recording(self):
        """Start recording simulation."""
        import time
        self.video_playback.set_recording_status(True, time.time())
        print("Recording started")

    def stop_recording(self):
        """Stop recording simulation."""
        self.video_playback.set_recording_status(False)
        print("Recording stopped")

    def on_video_changed(self, video_path, annotation_data):
        """Handle video change events."""
        print(f"Video changed: {annotation_data}")

    def on_annotation_added(self, video_path, timestamp, annotation):
        """Handle annotation added events."""
        print(f"Annotation added: {annotation['text']} at {timestamp}")

    def on_playback_started(self):
        """Handle playback started events."""
        print("Playback started")

    def on_playback_stopped(self):
        """Handle playback stopped events."""
        print("Playback stopped")

def main():
    """Main function to run the test."""
    app = QApplication(sys.argv)

    # Create and show the test window
    window = TestMainWindow()
    window.show()

    print("Test application started. You can:")
    print("1. Click 'Add Videos' to add video files to the playlist")
    print("2. Click 'Start Recording' to simulate recording start")
    print("3. Play videos and add annotations")
    print("4. Click 'Stop Recording' to simulate recording stop")
    print("5. Export annotations to see the results")

    # Run the application
    sys.exit(app.exec_())

if __name__ == "__main__":
    main()
