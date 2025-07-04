import logging
import os
import threading
import time
from pathlib import Path

import cv2

logger = logging.getLogger(__name__)


class WebcamHandler:
    def __init__(self):
        self.cap = None
        self.is_capturing = False
        self.is_recording = False
        self.frame = None
        self.video_writer = None
        self.recording_path = None
        self.lock = threading.Lock()
        self.frame_count = 0
        logger.info("WebcamHandler initialized.")

    def start_capture(self, camera_index=0, resolution=(640, 480), fps=30):
        """Start webcam capture with configurable resolution and frame rate."""
        self.cap = cv2.VideoCapture(camera_index)
        if not self.cap.isOpened():
            logger.error(f"Could not open webcam at index {camera_index}")
            return False

        # Set resolution and frame rate as mentioned in the issue
        self.cap.set(cv2.CAP_PROP_FRAME_WIDTH, resolution[0])
        self.cap.set(cv2.CAP_PROP_FRAME_HEIGHT, resolution[1])
        self.cap.set(cv2.CAP_PROP_FPS, fps)

        # Store capture settings
        self.fps = fps
        self.resolution = resolution

        self.is_capturing = True
        self.capture_thread = threading.Thread(target=self._capture_loop)
        self.capture_thread.daemon = True
        self.capture_thread.start()
        logger.info(
            f"Started webcam capture from index {camera_index} at {resolution} @ {fps}fps"
        )
        return True

    def _capture_loop(self):
        """Main capture loop that handles both display and recording."""
        while self.is_capturing:
            ret, frame = self.cap.read()
            if not ret:
                logger.warning("Failed to grab frame from webcam.")
                break

            with self.lock:
                self.frame = frame
                self.frame_count += 1

                # Write frame to video file if recording
                if self.is_recording and self.video_writer is not None:
                    self.video_writer.write(frame)

            time.sleep(1.0 / self.fps if hasattr(self, "fps") else 0.03)

    def get_latest_frame(self):
        with self.lock:
            return self.frame.copy() if self.frame is not None else None

    def start_recording(self, output_path: str, codec="XVID"):
        """Start recording webcam video to file using cv2.VideoWriter as mentioned in the issue."""
        if not self.is_capturing:
            logger.error("Cannot start recording: webcam capture is not active")
            return False

        if self.is_recording:
            logger.warning("Recording is already active")
            return False

        try:
            # Ensure output directory exists
            output_dir = Path(output_path).parent
            output_dir.mkdir(parents=True, exist_ok=True)

            # Set up video writer with fourcc codec
            fourcc = cv2.VideoWriter_fourcc(*codec)
            fps = getattr(self, "fps", 30)
            resolution = getattr(self, "resolution", (640, 480))

            self.video_writer = cv2.VideoWriter(output_path, fourcc, fps, resolution)

            if not self.video_writer.isOpened():
                logger.error(f"Failed to open video writer for {output_path}")
                return False

            self.recording_path = output_path
            self.is_recording = True
            self.frame_count = 0

            logger.info(f"Started recording to {output_path} with codec {codec}")
            return True

        except Exception as e:
            logger.error(f"Failed to start recording: {e}")
            return False

    def stop_recording(self):
        """Stop video recording and release the VideoWriter."""
        if not self.is_recording:
            logger.warning("No active recording to stop")
            return

        self.is_recording = False

        if self.video_writer:
            self.video_writer.release()
            self.video_writer = None

        logger.info(
            f"Stopped recording. Saved {self.frame_count} frames to {self.recording_path}"
        )
        self.recording_path = None

    def stop_capture(self):
        """Stop webcam capture and clean up resources."""
        # Stop recording if active
        if self.is_recording:
            self.stop_recording()

        self.is_capturing = False
        if (
            hasattr(self, "capture_thread")
            and self.capture_thread
            and self.capture_thread.is_alive()
        ):
            self.capture_thread.join(timeout=1)  # Wait for thread to finish
        if self.cap:
            self.cap.release()
        logger.info("Stopped webcam capture.")

    def shutdown(self):
        """Shutdown webcam handler and clean up all resources."""
        self.stop_capture()
        logger.info("WebcamHandler shutdown complete.")

    def is_recording_active(self):
        """Check if video recording is currently active."""
        return self.is_recording

    def get_recording_info(self):
        """Get information about current recording."""
        if not self.is_recording:
            return None

        return {
            "recording_path": self.recording_path,
            "frame_count": self.frame_count,
            "is_active": self.is_recording,
        }
