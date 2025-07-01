import logging
import cv2
import threading
import time

logger = logging.getLogger(__name__)

class WebcamHandler:
    def __init__(self):
        self.cap = None
        self.is_capturing = False
        self.frame = None
        self.lock = threading.Lock()
        logger.info("WebcamHandler initialized.")

    def start_capture(self, camera_index=0):
        self.cap = cv2.VideoCapture(camera_index)
        if not self.cap.isOpened():
            logger.error(f"Could not open webcam at index {camera_index}")
            return False
        self.is_capturing = True
        self.capture_thread = threading.Thread(target=self._capture_loop)
        self.capture_thread.daemon = True
        self.capture_thread.start()
        logger.info(f"Started webcam capture from index {camera_index}")
        return True

    def _capture_loop(self):
        while self.is_capturing:
            ret, frame = self.cap.read()
            if not ret:
                logger.warning("Failed to grab frame from webcam.")
                break
            with self.lock:
                self.frame = frame
            time.sleep(0.03) # Simulate ~30 FPS

    def get_latest_frame(self):
        with self.lock:
            return self.frame.copy() if self.frame is not None else None

    def stop_capture(self):
        self.is_capturing = False
        if self.capture_thread and self.capture_thread.is_alive():
            self.capture_thread.join(timeout=1) # Wait for thread to finish
        if self.cap:
            self.cap.release()
        logger.info("Stopped webcam capture.")

