#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Main entry point for the Windows PC Controller App.
This script initializes the application and launches the main window.
"""

import sys
import os
import logging
from PyQt5.QtWidgets import QApplication
from PyQt5.QtCore import Qt

# Add the parent directory to the path so we can import our modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Import our modules
from ui.main_window import MainWindow
from utils.logger import setup_logger

def main():
    """
    Main function to initialize and run the application.
    """
    # Set up logging
    setup_logger()
    logger = logging.getLogger(__name__)
    logger.info("Starting Windows PC Controller App")

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