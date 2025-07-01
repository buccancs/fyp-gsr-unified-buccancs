#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Video Preview for the Windows PC Controller App.
This module implements a video preview widget for displaying live video feeds from devices.
"""

import logging
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel, 
                           QPushButton, QGroupBox, QSizePolicy)
from PyQt5.QtCore import Qt, pyqtSlot, pyqtSignal, QSize
from PyQt5.QtGui import QPixmap, QImage, QPainter, QColor, QFont

from utils.logger import get_logger

class VideoPreview(QWidget):
    """
    Video Preview class for displaying live video feeds from devices.
    """
    
    def __init__(self, title="Video", parent=None):
        """
        Initialize the video preview.
        
        Args:
            title: The title of the preview (default: "Video")
            parent: The parent widget (default: None)
        """
        super().__init__(parent)
        
        # Set up logging
        self.logger = get_logger(__name__)
        
        # Store title
        self.title = title
        
        # Initialize state
        self.status = "No Signal"
        self.frame = None
        
        # Set up UI
        self.setup_ui()
        
        self.logger.info(f"Video preview initialized: {title}")
    
    def setup_ui(self):
        """
        Set up the UI for the video preview.
        """
        # Create main layout
        self.main_layout = QVBoxLayout(self)
        
        # Create header
        self.header_layout = QHBoxLayout()
        
        # Title
        self.title_label = QLabel(self.title)
        self.title_label.setStyleSheet("font-weight: bold;")
        self.header_layout.addWidget(self.title_label)
        
        # Status
        self.status_label = QLabel(self.status)
        self.header_layout.addWidget(self.status_label)
        
        self.header_layout.addStretch(1)
        
        self.main_layout.addLayout(self.header_layout)
        
        # Create video frame
        self.frame_widget = VideoFrameWidget()
        self.frame_widget.setSizePolicy(QSizePolicy.Expanding, QSizePolicy.Expanding)
        self.frame_widget.setMinimumSize(320, 240)
        self.main_layout.addWidget(self.frame_widget)
    
    def update_frame(self, frame):
        """
        Update the video frame.
        
        Args:
            frame: The new frame to display (QImage)
        """
        self.frame = frame
        self.frame_widget.set_image(frame)
        self.update_status("Receiving")
    
    def update_status(self, status):
        """
        Update the status of the video preview.
        
        Args:
            status: The new status
        """
        self.status = status
        self.status_label.setText(status)
        
        # Update status color
        if status == "Receiving":
            self.status_label.setStyleSheet("color: green;")
        elif status == "No Signal":
            self.status_label.setStyleSheet("color: red;")
        else:
            self.status_label.setStyleSheet("")
        
        # Update frame widget
        self.frame_widget.set_status(status)
        self.frame_widget.update()
    
    def clear(self):
        """
        Clear the video preview.
        """
        self.frame = None
        self.frame_widget.set_image(None)
        self.update_status("No Signal")


class VideoFrameWidget(QWidget):
    """
    Widget for displaying a video frame.
    """
    
    def __init__(self, parent=None):
        """
        Initialize the video frame widget.
        
        Args:
            parent: The parent widget (default: None)
        """
        super().__init__(parent)
        
        # Initialize state
        self.image = None
        self.status = "No Signal"
        
        # Set background color
        self.setAutoFillBackground(True)
        palette = self.palette()
        palette.setColor(self.backgroundRole(), QColor(0, 0, 0))
        self.setPalette(palette)
    
    def set_image(self, image):
        """
        Set the image to display.
        
        Args:
            image: The image to display (QImage)
        """
        self.image = image
        self.update()
    
    def set_status(self, status):
        """
        Set the status of the video frame.
        
        Args:
            status: The new status
        """
        self.status = status
        self.update()
    
    def paintEvent(self, event):
        """
        Paint the widget.
        
        Args:
            event: The paint event
        """
        painter = QPainter(self)
        
        # Fill background
        painter.fillRect(self.rect(), QColor(0, 0, 0))
        
        if self.image:
            # Draw the image
            scaled_image = self.image.scaled(self.size(), Qt.KeepAspectRatio, Qt.SmoothTransformation)
            x = (self.width() - scaled_image.width()) // 2
            y = (self.height() - scaled_image.height()) // 2
            painter.drawImage(x, y, scaled_image)
        else:
            # Draw status text
            painter.setPen(QColor(255, 255, 255))
            font = QFont()
            font.setPointSize(12)
            painter.setFont(font)
            painter.drawText(self.rect(), Qt.AlignCenter, self.status)
    
    def sizeHint(self):
        """
        Get the suggested size of the widget.
        
        Returns:
            The suggested size
        """
        return QSize(320, 240)