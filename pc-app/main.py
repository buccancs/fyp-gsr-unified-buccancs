"""
GSR Multimodal System - PC Controller Application
Entry point for the PC GUI application that controls and coordinates
multiple Android devices for synchronized data collection.
"""

import sys
from PySide6.QtWidgets import QApplication
import logging
from pathlib import Path
from app.gui.main_window import MainWindow
from app.network.device_manager import DeviceManager
from app.sensors.gsr_handler import GSRHandler
from app.sensors.webcam_handler import WebcamHandler
from app.session.session_manager import SessionManager
from app.transfer.file_transfer_manager import FileTransferManager

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('gsr_multimodal.log'),
        logging.StreamHandler(sys.stdout)
    ]
)

logger = logging.getLogger(__name__)

def main():
    """Main entry point for the PC application."""
    try:
        logger.info("Starting GSR Multimodal System - PC Controller")

        # Initialize core components in proper order
        session_manager = SessionManager()
        device_manager = DeviceManager(session_manager)  # Pass session manager to device manager
        gsr_handler = GSRHandler()
        webcam_handler = WebcamHandler()
        file_transfer_manager = FileTransferManager(session_manager)

        # Start the device manager server
        device_manager.start_server()
        logger.info("Device manager server started")

        # Create and run the GUI application
        app = QApplication(sys.argv)
        main_window = MainWindow(device_manager, gsr_handler, webcam_handler, session_manager, file_transfer_manager)
        main_window.show()

        # Ensure proper cleanup on exit
        def cleanup():
            logger.info("Shutting down application...")
            device_manager.stop_server()
            webcam_handler.shutdown()

        app.aboutToQuit.connect(cleanup)
        sys.exit(app.exec())

    except Exception as e:
        logger.error(f"Failed to start application: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
