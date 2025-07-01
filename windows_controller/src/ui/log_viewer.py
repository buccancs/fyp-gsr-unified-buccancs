#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Log Viewer for the Windows PC Controller App.
This module implements a log viewer for displaying application logs.
"""

import logging
import os
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QLabel, 
                           QTextEdit, QPushButton, QComboBox, QFileDialog)
from PyQt5.QtCore import Qt, pyqtSlot, pyqtSignal, QTimer
from PyQt5.QtGui import QColor, QTextCharFormat, QFont, QTextCursor

from utils.logger import get_logger

class LogViewer(QWidget):
    """
    Log Viewer class for displaying application logs.
    """
    
    def __init__(self, parent=None):
        """
        Initialize the log viewer.
        
        Args:
            parent: The parent widget (default: None)
        """
        super().__init__(parent)
        
        # Set up logging
        self.logger = get_logger(__name__)
        
        # Initialize state
        self.log_level = logging.INFO
        self.log_file = None
        self.log_buffer = []
        self.max_buffer_size = 1000  # Maximum number of log entries to keep in buffer
        
        # Set up UI
        self.setup_ui()
        
        # Set up log handler
        self.log_handler = LogHandler(self)
        logging.getLogger().addHandler(self.log_handler)
        
        # Set up timer for updating log view
        self.update_timer = QTimer(self)
        self.update_timer.timeout.connect(self.update_log_view)
        self.update_timer.start(1000)  # Update every second
        
        self.logger.info("Log viewer initialized")
    
    def setup_ui(self):
        """
        Set up the UI for the log viewer.
        """
        # Create main layout
        self.main_layout = QVBoxLayout(self)
        
        # Create header
        self.header_layout = QHBoxLayout()
        
        # Title
        self.title_label = QLabel("Log Viewer")
        self.title_label.setStyleSheet("font-weight: bold; font-size: 16px;")
        self.header_layout.addWidget(self.title_label)
        
        # Log level selector
        self.level_label = QLabel("Log Level:")
        self.header_layout.addWidget(self.level_label)
        
        self.level_combo = QComboBox()
        self.level_combo.addItem("DEBUG", logging.DEBUG)
        self.level_combo.addItem("INFO", logging.INFO)
        self.level_combo.addItem("WARNING", logging.WARNING)
        self.level_combo.addItem("ERROR", logging.ERROR)
        self.level_combo.addItem("CRITICAL", logging.CRITICAL)
        self.level_combo.setCurrentIndex(1)  # INFO by default
        self.level_combo.currentIndexChanged.connect(self.on_level_changed)
        self.header_layout.addWidget(self.level_combo)
        
        self.header_layout.addStretch(1)
        
        # Clear button
        self.clear_button = QPushButton("Clear")
        self.clear_button.clicked.connect(self.on_clear_clicked)
        self.header_layout.addWidget(self.clear_button)
        
        # Save button
        self.save_button = QPushButton("Save")
        self.save_button.clicked.connect(self.on_save_clicked)
        self.header_layout.addWidget(self.save_button)
        
        self.main_layout.addLayout(self.header_layout)
        
        # Create log text edit
        self.log_text = QTextEdit()
        self.log_text.setReadOnly(True)
        self.log_text.setLineWrapMode(QTextEdit.NoWrap)
        self.log_text.setFont(QFont("Courier New", 10))
        self.main_layout.addWidget(self.log_text)
    
    def add_log(self, record):
        """
        Add a log record to the buffer.
        
        Args:
            record: The log record to add
        """
        # Add record to buffer
        self.log_buffer.append(record)
        
        # Trim buffer if it's too large
        if len(self.log_buffer) > self.max_buffer_size:
            self.log_buffer = self.log_buffer[-self.max_buffer_size:]
    
    def update_log_view(self):
        """
        Update the log view with the current buffer.
        """
        # Check if there are any new logs
        if not self.log_buffer:
            return
        
        # Get cursor
        cursor = self.log_text.textCursor()
        cursor.movePosition(QTextCursor.End)
        
        # Add each log entry
        for record in self.log_buffer:
            # Check if the log level is high enough
            if record.levelno < self.log_level:
                continue
            
            # Format the log entry
            log_format = QTextCharFormat()
            
            # Set color based on level
            if record.levelno >= logging.CRITICAL:
                log_format.setForeground(QColor("purple"))
            elif record.levelno >= logging.ERROR:
                log_format.setForeground(QColor("red"))
            elif record.levelno >= logging.WARNING:
                log_format.setForeground(QColor("orange"))
            elif record.levelno >= logging.INFO:
                log_format.setForeground(QColor("black"))
            else:  # DEBUG
                log_format.setForeground(QColor("gray"))
            
            # Format the log entry
            timestamp = record.asctime if hasattr(record, 'asctime') else record.created
            log_entry = f"{timestamp} - {record.name} - {record.levelname} - {record.getMessage()}\n"
            
            # Insert the log entry
            cursor.insertText(log_entry, log_format)
        
        # Clear the buffer
        self.log_buffer.clear()
        
        # Scroll to the bottom
        self.log_text.setTextCursor(cursor)
        self.log_text.ensureCursorVisible()
    
    @pyqtSlot(int)
    def on_level_changed(self, index):
        """
        Handle the log level being changed.
        
        Args:
            index: The index of the selected level
        """
        self.log_level = self.level_combo.itemData(index)
        self.logger.info(f"Log level changed to {logging.getLevelName(self.log_level)}")
        
        # Clear the log view and re-add all logs
        self.log_text.clear()
        
        # Get all logs from the log file
        if self.log_file:
            try:
                with open(self.log_file, "r") as f:
                    for line in f:
                        # Parse the log line and add it to the view
                        # This is a simplified version; in a real implementation,
                        # we would parse the log line properly
                        record = logging.LogRecord(
                            name="",
                            level=0,
                            pathname="",
                            lineno=0,
                            msg=line.strip(),
                            args=(),
                            exc_info=None
                        )
                        self.add_log(record)
                
                # Update the log view
                self.update_log_view()
            except Exception as e:
                self.logger.error(f"Failed to read log file: {str(e)}")
    
    @pyqtSlot()
    def on_clear_clicked(self):
        """
        Handle the clear button being clicked.
        """
        self.logger.info("Clearing log view")
        self.log_text.clear()
        self.log_buffer.clear()
    
    @pyqtSlot()
    def on_save_clicked(self):
        """
        Handle the save button being clicked.
        """
        self.logger.info("Saving log")
        
        # Show file dialog
        file_path, _ = QFileDialog.getSaveFileName(
            self, "Save Log", "", "Log Files (*.log);;All Files (*)"
        )
        
        if file_path:
            try:
                with open(file_path, "w") as f:
                    f.write(self.log_text.toPlainText())
                self.logger.info(f"Log saved to {file_path}")
            except Exception as e:
                self.logger.error(f"Failed to save log: {str(e)}")
    
    def set_log_file(self, log_file):
        """
        Set the log file to display.
        
        Args:
            log_file: The path to the log file
        """
        self.log_file = log_file
        self.logger.info(f"Log file set to {log_file}")
        
        # Clear the log view
        self.log_text.clear()
        
        # Read the log file
        try:
            with open(log_file, "r") as f:
                for line in f:
                    self.log_text.append(line.strip())
        except Exception as e:
            self.logger.error(f"Failed to read log file: {str(e)}")


class LogHandler(logging.Handler):
    """
    Log handler for sending log records to the log viewer.
    """
    
    def __init__(self, log_viewer):
        """
        Initialize the log handler.
        
        Args:
            log_viewer: The log viewer to send records to
        """
        super().__init__()
        self.log_viewer = log_viewer
    
    def emit(self, record):
        """
        Emit a log record.
        
        Args:
            record: The log record to emit
        """
        # Format the record
        self.format(record)
        
        # Send the record to the log viewer
        self.log_viewer.add_log(record)