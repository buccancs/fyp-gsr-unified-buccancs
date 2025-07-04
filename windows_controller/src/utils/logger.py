#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Logger module for the PC Controller App.
Provides logging functionality for the application.
Cross-platform support: Windows, macOS, Linux.
"""

import os
import logging
import datetime
from logging.handlers import RotatingFileHandler

def setup_logger(log_level=logging.INFO):
    """
    Set up the logger for the application.

    Args:
        log_level: The logging level to use (default: logging.INFO)

    Returns:
        None
    """
    # Create logs directory if it doesn't exist
    logs_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), 'logs')
    os.makedirs(logs_dir, exist_ok=True)

    # Generate log filename with timestamp
    timestamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    log_file = os.path.join(logs_dir, f'gsr_controller_{timestamp}.log')

    # Configure root logger
    logger = logging.getLogger()
    logger.setLevel(log_level)

    # Create console handler
    console_handler = logging.StreamHandler()
    console_handler.setLevel(log_level)
    console_format = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    console_handler.setFormatter(console_format)

    # Create file handler
    file_handler = RotatingFileHandler(log_file, maxBytes=10*1024*1024, backupCount=5)
    file_handler.setLevel(log_level)
    file_format = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    file_handler.setFormatter(file_format)

    # Add handlers to logger
    logger.addHandler(console_handler)
    logger.addHandler(file_handler)

    logger.info(f"Logger initialized. Log file: {log_file}")
    return logger

def get_logger(name):
    """
    Get a logger with the specified name.

    Args:
        name: The name of the logger

    Returns:
        A logger instance
    """
    return logging.getLogger(name)
