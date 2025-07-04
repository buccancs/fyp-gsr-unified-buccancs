#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Video Playback Window for the Windows PC Controller App.
This module implements a video playback window that displays a series of videos
while recording is happening, with annotation capabilities when new videos are played.
"""

import os
import logging
from typing import List, Dict, Optional
from PySide6.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel, 
                           QPushButton, QGroupBox, QListWidget, QListWidgetItem,
                           QTextEdit, QSlider, QSpinBox, QComboBox, QCheckBox,
                           QFileDialog, QMessageBox, QSplitter, QProgressBar,
                           QTableWidget, QTableWidgetItem, QHeaderView)
from PySide6.QtCore import Qt, Slot, Signal, QTimer, QThread, QUrl
from PySide6.QtGui import QPixmap, QImage, QPainter, QColor, QFont
from PySide6.QtMultimedia import QMediaPlayer
from PySide6.QtMultimediaWidgets import QVideoWidget

from utils.logger import get_logger

class VideoPlaybackWindow(QWidget):
    """
    Video Playback Window class for displaying a series of videos with annotation capabilities.
    """

    # Signals
    video_changed = Signal(str, dict)  # video_path, annotation_data
    annotation_added = Signal(str, str, dict)  # video_path, timestamp, annotation
    playback_started = Signal()
    playback_stopped = Signal()

    def __init__(self, parent=None):
        """
        Initialize the video playback window.

        Args:
            parent: The parent widget (default: None)
        """
        super().__init__(parent)

        # Set up logging
        self.logger = get_logger(__name__)

        # Initialize state
        self.video_playlist = []
        self.current_video_index = 0
        self.annotations = {}  # video_path -> list of annotations
        self.is_recording = False
        self.recording_start_time = None

        # Media player
        self.media_player = QMediaPlayer()
        self.video_widget = QVideoWidget()
        self.media_player.setVideoOutput(self.video_widget)

        # Set up UI
        self.setup_ui()
        self.connect_signals()

        # Timer for updating playback position
        self.position_timer = QTimer()
        self.position_timer.timeout.connect(self.update_position)
        self.position_timer.start(100)  # Update every 100ms

        self.logger.info("Video playback window initialized")

    def setup_ui(self):
        """
        Set up the UI for the video playback window.
        """
        self.setWindowTitle("Video Playback & Annotation")
        self.setMinimumSize(1200, 800)

        # Create main layout
        main_layout = QHBoxLayout(self)

        # Create splitter for main content
        splitter = QSplitter(Qt.Horizontal)

        # Left panel - Video playback
        video_panel = self.create_video_panel()
        splitter.addWidget(video_panel)

        # Right panel - Playlist and annotations
        control_panel = self.create_control_panel()
        splitter.addWidget(control_panel)

        # Set splitter proportions
        splitter.setSizes([800, 400])

        main_layout.addWidget(splitter)

    def create_video_panel(self):
        """
        Create the video playback panel.

        Returns:
            QWidget: The video panel widget
        """
        panel = QWidget()
        layout = QVBoxLayout(panel)

        # Video display
        video_group = QGroupBox("Video Playback")
        video_layout = QVBoxLayout(video_group)

        # Video widget
        self.video_widget.setMinimumSize(640, 480)
        video_layout.addWidget(self.video_widget)

        # Video info
        info_layout = QHBoxLayout()
        self.video_info_label = QLabel("No video loaded")
        self.video_info_label.setStyleSheet("font-weight: bold;")
        info_layout.addWidget(self.video_info_label)

        info_layout.addStretch()

        self.duration_label = QLabel("00:00 / 00:00")
        info_layout.addWidget(self.duration_label)

        video_layout.addLayout(info_layout)

        # Progress slider
        self.position_slider = QSlider(Qt.Horizontal)
        self.position_slider.setRange(0, 0)
        self.position_slider.sliderMoved.connect(self.set_position)
        video_layout.addWidget(self.position_slider)

        # Control buttons
        controls_layout = QHBoxLayout()

        self.play_button = QPushButton("Play")
        self.play_button.clicked.connect(self.toggle_playback)
        controls_layout.addWidget(self.play_button)

        self.stop_button = QPushButton("Stop")
        self.stop_button.clicked.connect(self.stop_playback)
        controls_layout.addWidget(self.stop_button)

        self.prev_button = QPushButton("Previous")
        self.prev_button.clicked.connect(self.previous_video)
        controls_layout.addWidget(self.prev_button)

        self.next_button = QPushButton("Next")
        self.next_button.clicked.connect(self.next_video)
        controls_layout.addWidget(self.next_button)

        controls_layout.addStretch()

        # Volume control
        volume_label = QLabel("Volume:")
        controls_layout.addWidget(volume_label)

        self.volume_slider = QSlider(Qt.Horizontal)
        self.volume_slider.setRange(0, 100)
        self.volume_slider.setValue(50)
        self.volume_slider.setMaximumWidth(100)
        self.volume_slider.valueChanged.connect(self.media_player.setVolume)
        controls_layout.addWidget(self.volume_slider)

        video_layout.addLayout(controls_layout)

        layout.addWidget(video_group)

        # Quick annotation panel
        annotation_group = QGroupBox("Quick Annotation")
        annotation_layout = QVBoxLayout(annotation_group)

        # Annotation input
        input_layout = QHBoxLayout()

        self.annotation_text = QTextEdit()
        self.annotation_text.setMaximumHeight(60)
        self.annotation_text.setPlaceholderText("Enter annotation for current video...")
        input_layout.addWidget(self.annotation_text)

        self.add_annotation_button = QPushButton("Add Annotation")
        self.add_annotation_button.clicked.connect(self.add_annotation)
        input_layout.addWidget(self.add_annotation_button)

        annotation_layout.addLayout(input_layout)

        # Auto-annotation settings
        auto_layout = QHBoxLayout()

        self.auto_annotate_checkbox = QCheckBox("Auto-annotate on video change")
        self.auto_annotate_checkbox.setChecked(True)
        auto_layout.addWidget(self.auto_annotate_checkbox)

        auto_layout.addStretch()

        annotation_layout.addLayout(auto_layout)

        layout.addWidget(annotation_group)

        return panel

    def create_control_panel(self):
        """
        Create the control panel with playlist and annotations.

        Returns:
            QWidget: The control panel widget
        """
        panel = QWidget()
        layout = QVBoxLayout(panel)

        # Playlist management
        playlist_group = QGroupBox("Video Playlist")
        playlist_layout = QVBoxLayout(playlist_group)

        # Playlist buttons
        playlist_buttons = QHBoxLayout()

        self.add_videos_button = QPushButton("Add Videos")
        self.add_videos_button.clicked.connect(self.add_videos)
        playlist_buttons.addWidget(self.add_videos_button)

        self.remove_video_button = QPushButton("Remove Selected")
        self.remove_video_button.clicked.connect(self.remove_selected_video)
        playlist_buttons.addWidget(self.remove_video_button)

        self.clear_playlist_button = QPushButton("Clear All")
        self.clear_playlist_button.clicked.connect(self.clear_playlist)
        playlist_buttons.addWidget(self.clear_playlist_button)

        playlist_layout.addLayout(playlist_buttons)

        # Playlist widget
        self.playlist_widget = QListWidget()
        self.playlist_widget.itemDoubleClicked.connect(self.on_playlist_item_double_clicked)
        self.playlist_widget.currentRowChanged.connect(self.on_playlist_selection_changed)
        playlist_layout.addWidget(self.playlist_widget)

        layout.addWidget(playlist_group)

        # Annotations table
        annotations_group = QGroupBox("Annotations")
        annotations_layout = QVBoxLayout(annotations_group)

        # Annotations table
        self.annotations_table = QTableWidget()
        self.annotations_table.setColumnCount(4)
        self.annotations_table.setHorizontalHeaderLabels(["Video", "Timestamp", "Recording Time", "Annotation"])

        # Set column widths
        header = self.annotations_table.horizontalHeader()
        header.setSectionResizeMode(0, QHeaderView.ResizeToContents)
        header.setSectionResizeMode(1, QHeaderView.ResizeToContents)
        header.setSectionResizeMode(2, QHeaderView.ResizeToContents)
        header.setSectionResizeMode(3, QHeaderView.Stretch)

        annotations_layout.addWidget(self.annotations_table)

        # Annotation export buttons
        export_layout = QHBoxLayout()

        self.export_annotations_button = QPushButton("Export Annotations")
        self.export_annotations_button.clicked.connect(self.export_annotations)
        export_layout.addWidget(self.export_annotations_button)

        self.clear_annotations_button = QPushButton("Clear Annotations")
        self.clear_annotations_button.clicked.connect(self.clear_annotations)
        export_layout.addWidget(self.clear_annotations_button)

        annotations_layout.addLayout(export_layout)

        layout.addWidget(annotations_group)

        # Recording status
        status_group = QGroupBox("Recording Status")
        status_layout = QVBoxLayout(status_group)

        self.recording_status_label = QLabel("Not Recording")
        self.recording_status_label.setStyleSheet("font-weight: bold;")
        status_layout.addWidget(self.recording_status_label)

        self.recording_time_label = QLabel("Recording Time: 00:00:00")
        status_layout.addWidget(self.recording_time_label)

        layout.addWidget(status_group)

        return panel

    def connect_signals(self):
        """
        Connect media player signals.
        """
        self.media_player.stateChanged.connect(self.on_media_state_changed)
        self.media_player.positionChanged.connect(self.on_position_changed)
        self.media_player.durationChanged.connect(self.on_duration_changed)
        self.media_player.mediaStatusChanged.connect(self.on_media_status_changed)

    def add_videos(self):
        """
        Add videos to the playlist.
        """
        file_dialog = QFileDialog()
        file_paths, _ = file_dialog.getOpenFileNames(
            self,
            "Select Video Files",
            "",
            "Video Files (*.mp4 *.avi *.mov *.mkv *.wmv *.flv);;All Files (*)"
        )

        if file_paths:
            for file_path in file_paths:
                self.add_video_to_playlist(file_path)

            self.logger.info(f"Added {len(file_paths)} videos to playlist")

    def add_video_to_playlist(self, file_path):
        """
        Add a single video to the playlist.

        Args:
            file_path: Path to the video file
        """
        if file_path not in self.video_playlist:
            self.video_playlist.append(file_path)

            # Add to playlist widget
            item = QListWidgetItem(os.path.basename(file_path))
            item.setData(Qt.UserRole, file_path)
            item.setToolTip(file_path)
            self.playlist_widget.addItem(item)

            # Initialize annotations for this video
            if file_path not in self.annotations:
                self.annotations[file_path] = []

    def remove_selected_video(self):
        """
        Remove the selected video from the playlist.
        """
        current_row = self.playlist_widget.currentRow()
        if current_row >= 0:
            item = self.playlist_widget.takeItem(current_row)
            if item:
                file_path = item.data(Qt.UserRole)
                if file_path in self.video_playlist:
                    self.video_playlist.remove(file_path)

                # Adjust current index if necessary
                if current_row <= self.current_video_index:
                    self.current_video_index = max(0, self.current_video_index - 1)

    def clear_playlist(self):
        """
        Clear the entire playlist.
        """
        self.video_playlist.clear()
        self.playlist_widget.clear()
        self.current_video_index = 0
        self.media_player.stop()
        self.video_info_label.setText("No video loaded")

    def on_playlist_item_double_clicked(self, item):
        """
        Handle double-click on playlist item.

        Args:
            item: The clicked item
        """
        file_path = item.data(Qt.UserRole)
        if file_path in self.video_playlist:
            self.current_video_index = self.video_playlist.index(file_path)
            self.load_current_video()

    def on_playlist_selection_changed(self, current_row):
        """
        Handle playlist selection change.

        Args:
            current_row: The currently selected row
        """
        if 0 <= current_row < len(self.video_playlist):
            self.current_video_index = current_row

    def load_current_video(self):
        """
        Load the current video from the playlist.
        """
        if not self.video_playlist or self.current_video_index >= len(self.video_playlist):
            return

        video_path = self.video_playlist[self.current_video_index]

        if os.path.exists(video_path):
            # Load video
            self.media_player.setSource(QUrl.fromLocalFile(video_path))

            # Update UI
            self.video_info_label.setText(f"Video: {os.path.basename(video_path)}")
            self.playlist_widget.setCurrentRow(self.current_video_index)

            # Auto-annotate if enabled
            if self.auto_annotate_checkbox.isChecked():
                self.auto_annotate_video_change(video_path)

            # Emit signal
            annotation_data = {
                'video_index': self.current_video_index,
                'video_name': os.path.basename(video_path),
                'annotations_count': len(self.annotations.get(video_path, []))
            }
            self.video_changed.emit(video_path, annotation_data)

            self.logger.info(f"Loaded video: {video_path}")
        else:
            QMessageBox.warning(self, "Error", f"Video file not found: {video_path}")

    def auto_annotate_video_change(self, video_path):
        """
        Automatically add annotation when video changes.

        Args:
            video_path: Path to the new video
        """
        if self.is_recording and self.recording_start_time:
            import time
            current_time = time.time()
            recording_elapsed = current_time - self.recording_start_time

            annotation = {
                'text': f"Video changed to: {os.path.basename(video_path)}",
                'timestamp': self.media_player.position(),
                'recording_time': recording_elapsed,
                'auto_generated': True
            }

            self.add_annotation_to_video(video_path, annotation)

    def toggle_playback(self):
        """
        Toggle video playback.
        """
        if self.media_player.state() == QMediaPlayer.PlayingState:
            self.media_player.pause()
        else:
            if not self.video_playlist:
                QMessageBox.information(self, "Info", "Please add videos to the playlist first.")
                return

            if not self.media_player.source().isValid():
                self.load_current_video()

            self.media_player.play()

    def stop_playback(self):
        """
        Stop video playback.
        """
        self.media_player.stop()

    def previous_video(self):
        """
        Play the previous video in the playlist.
        """
        if self.video_playlist and self.current_video_index > 0:
            self.current_video_index -= 1
            self.load_current_video()

    def next_video(self):
        """
        Play the next video in the playlist.
        """
        if self.video_playlist and self.current_video_index < len(self.video_playlist) - 1:
            self.current_video_index += 1
            self.load_current_video()

    def set_position(self, position):
        """
        Set the playback position.

        Args:
            position: The new position
        """
        self.media_player.setPosition(position)

    def add_annotation(self):
        """
        Add annotation for the current video.
        """
        if not self.video_playlist:
            QMessageBox.information(self, "Info", "No video loaded.")
            return

        annotation_text = self.annotation_text.toPlainText().strip()
        if not annotation_text:
            QMessageBox.information(self, "Info", "Please enter annotation text.")
            return

        video_path = self.video_playlist[self.current_video_index]

        import time
        annotation = {
            'text': annotation_text,
            'timestamp': self.media_player.position(),
            'recording_time': time.time() - self.recording_start_time if self.is_recording and self.recording_start_time else 0,
            'auto_generated': False
        }

        self.add_annotation_to_video(video_path, annotation)
        self.annotation_text.clear()

    def add_annotation_to_video(self, video_path, annotation):
        """
        Add annotation to a specific video.

        Args:
            video_path: Path to the video
            annotation: Annotation data
        """
        if video_path not in self.annotations:
            self.annotations[video_path] = []

        self.annotations[video_path].append(annotation)
        self.update_annotations_table()

        # Emit signal
        timestamp_str = self.format_time(annotation['timestamp'])
        self.annotation_added.emit(video_path, timestamp_str, annotation)

        self.logger.info(f"Added annotation to {os.path.basename(video_path)}: {annotation['text']}")

    def update_annotations_table(self):
        """
        Update the annotations table.
        """
        # Count total annotations
        total_annotations = sum(len(annotations) for annotations in self.annotations.values())
        self.annotations_table.setRowCount(total_annotations)

        row = 0
        for video_path, annotations in self.annotations.items():
            video_name = os.path.basename(video_path)

            for annotation in annotations:
                # Video name
                self.annotations_table.setItem(row, 0, QTableWidgetItem(video_name))

                # Timestamp
                timestamp_str = self.format_time(annotation['timestamp'])
                self.annotations_table.setItem(row, 1, QTableWidgetItem(timestamp_str))

                # Recording time
                recording_time_str = self.format_time(annotation['recording_time'] * 1000)  # Convert to ms
                self.annotations_table.setItem(row, 2, QTableWidgetItem(recording_time_str))

                # Annotation text
                text_item = QTableWidgetItem(annotation['text'])
                if annotation.get('auto_generated', False):
                    text_item.setBackground(QColor(240, 240, 240))
                self.annotations_table.setItem(row, 3, text_item)

                row += 1

    def export_annotations(self):
        """
        Export annotations to a file.
        """
        if not any(self.annotations.values()):
            QMessageBox.information(self, "Info", "No annotations to export.")
            return

        file_dialog = QFileDialog()
        file_path, _ = file_dialog.getSaveFileName(
            self,
            "Export Annotations",
            "annotations.csv",
            "CSV Files (*.csv);;JSON Files (*.json);;All Files (*)"
        )

        if file_path:
            try:
                if file_path.endswith('.json'):
                    self.export_annotations_json(file_path)
                else:
                    self.export_annotations_csv(file_path)

                QMessageBox.information(self, "Success", f"Annotations exported to: {file_path}")
                self.logger.info(f"Annotations exported to: {file_path}")
            except Exception as e:
                QMessageBox.critical(self, "Error", f"Failed to export annotations: {str(e)}")
                self.logger.error(f"Failed to export annotations: {str(e)}")

    def export_annotations_csv(self, file_path):
        """
        Export annotations to CSV format.

        Args:
            file_path: Path to save the CSV file
        """
        import csv

        with open(file_path, 'w', newline='', encoding='utf-8') as csvfile:
            writer = csv.writer(csvfile)
            writer.writerow(['Video', 'Timestamp', 'Recording Time', 'Annotation', 'Auto Generated'])

            for video_path, annotations in self.annotations.items():
                video_name = os.path.basename(video_path)

                for annotation in annotations:
                    writer.writerow([
                        video_name,
                        self.format_time(annotation['timestamp']),
                        self.format_time(annotation['recording_time'] * 1000),
                        annotation['text'],
                        annotation.get('auto_generated', False)
                    ])

    def export_annotations_json(self, file_path):
        """
        Export annotations to JSON format.

        Args:
            file_path: Path to save the JSON file
        """
        import json

        export_data = {}
        for video_path, annotations in self.annotations.items():
            export_data[video_path] = annotations

        with open(file_path, 'w', encoding='utf-8') as jsonfile:
            json.dump(export_data, jsonfile, indent=2, ensure_ascii=False)

    def clear_annotations(self):
        """
        Clear all annotations.
        """
        reply = QMessageBox.question(
            self,
            "Confirm Clear",
            "Are you sure you want to clear all annotations?",
            QMessageBox.Yes | QMessageBox.No,
            QMessageBox.No
        )

        if reply == QMessageBox.Yes:
            self.annotations.clear()
            self.update_annotations_table()
            self.logger.info("All annotations cleared")

    def set_recording_status(self, is_recording, start_time=None):
        """
        Set the recording status.

        Args:
            is_recording: Whether recording is active
            start_time: Recording start time (timestamp)
        """
        self.is_recording = is_recording
        self.recording_start_time = start_time

        if is_recording:
            self.recording_status_label.setText("Recording Active")
            self.recording_status_label.setStyleSheet("color: red; font-weight: bold;")
            self.playback_started.emit()
        else:
            self.recording_status_label.setText("Not Recording")
            self.recording_status_label.setStyleSheet("font-weight: bold;")
            self.playback_stopped.emit()

    def update_position(self):
        """
        Update the position display and recording time.
        """
        if self.is_recording and self.recording_start_time:
            import time
            elapsed = time.time() - self.recording_start_time
            self.recording_time_label.setText(f"Recording Time: {self.format_time(elapsed * 1000)}")

    def on_media_state_changed(self, state):
        """
        Handle media player state changes.

        Args:
            state: The new media player state
        """
        if state == QMediaPlayer.PlayingState:
            self.play_button.setText("Pause")
        else:
            self.play_button.setText("Play")

    def on_position_changed(self, position):
        """
        Handle position changes.

        Args:
            position: The new position
        """
        self.position_slider.setValue(position)

        # Update duration display
        duration = self.media_player.duration()
        if duration > 0:
            self.duration_label.setText(f"{self.format_time(position)} / {self.format_time(duration)}")

    def on_duration_changed(self, duration):
        """
        Handle duration changes.

        Args:
            duration: The media duration
        """
        self.position_slider.setRange(0, duration)

    def on_media_status_changed(self, status):
        """
        Handle media status changes.

        Args:
            status: The new media status
        """
        if status == QMediaPlayer.EndOfMedia:
            # Auto-advance to next video if available
            if self.current_video_index < len(self.video_playlist) - 1:
                self.next_video()
                self.media_player.play()

    def format_time(self, milliseconds):
        """
        Format time in milliseconds to HH:MM:SS format.

        Args:
            milliseconds: Time in milliseconds

        Returns:
            str: Formatted time string
        """
        seconds = int(milliseconds / 1000)
        hours = seconds // 3600
        minutes = (seconds % 3600) // 60
        seconds = seconds % 60

        if hours > 0:
            return f"{hours:02d}:{minutes:02d}:{seconds:02d}"
        else:
            return f"{minutes:02d}:{seconds:02d}"

    def closeEvent(self, event):
        """
        Handle window close event.

        Args:
            event: The close event
        """
        self.media_player.stop()
        self.position_timer.stop()
        event.accept()
