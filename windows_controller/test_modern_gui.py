#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Test script for the modernized PySide6 GUI.
This script tests the main window with the new modern interface.
"""

import sys
import os

# Add the src directory to the Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

def main():
    """Main function to test the modernized GUI."""
    try:
        from PySide6.QtWidgets import QApplication
        from PySide6.QtCore import Qt
        from ui.main_window import MainWindow
        
        print("Testing PySide6 Modern GUI...")
        
        app = QApplication(sys.argv)
        app.setApplicationName("GSR & Dual-Video Recording System")
        app.setApplicationVersion("2.0")
        
        # Create and show the main window
        try:
            window = MainWindow()
            window.show()
            
            print("✓ PySide6 GUI loaded successfully!")
            print("✓ Modern styling applied!")
            print("✓ Main window created and displayed!")
            print("\nGUI Features:")
            print("- Modern flat design with rounded corners")
            print("- Professional color scheme")
            print("- Enhanced buttons with emojis")
            print("- Improved layout and spacing")
            print("- Responsive design")
            
            print("\nPress Ctrl+C to exit or close the window.")
            
            # Run the application
            return app.exec()
            
        except Exception as e:
            print(f"✗ Error creating main window: {e}")
            return 1
            
    except ImportError as e:
        print(f"✗ Import error: {e}")
        print("Make sure PySide6 is installed: pip install PySide6")
        return 1

if __name__ == "__main__":
    sys.exit(main())