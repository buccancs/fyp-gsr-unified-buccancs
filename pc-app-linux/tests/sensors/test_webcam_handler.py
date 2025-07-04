import time
from pathlib import Path
from unittest.mock import MagicMock, patch

import numpy as np
import pytest

# Mock the cv2 library before importing the handler
with patch("cv2.VideoCapture"), patch("cv2.VideoWriter"), patch(
    "cv2.VideoWriter_fourcc"
):
    from app.sensors.webcam_handler import WebcamHandler


@pytest.fixture
def webcam_handler():
    """Fixture to provide a clean WebcamHandler instance for each test."""
    handler = WebcamHandler()
    yield handler
    # Ensure cleanup after each test
    handler.shutdown()


def test_initialization(webcam_handler):
    """Verify the handler initializes in a clean state."""
    assert not webcam_handler.is_capturing
    assert not webcam_handler.is_recording
    assert webcam_handler.frame is None
    assert webcam_handler.video_writer is None


@patch("cv2.VideoCapture")
def test_start_capture_success(mock_video_capture, webcam_handler):
    """Verify that start_capture correctly initializes and starts the capture thread."""
    # Mock the VideoCapture object to simulate a working camera
    mock_capture_instance = MagicMock()
    mock_capture_instance.isOpened.return_value = True
    mock_capture_instance.read.return_value = (
        True,
        np.zeros((480, 640, 3), dtype=np.uint8),
    )
    mock_video_capture.return_value = mock_capture_instance

    result = webcam_handler.start_capture(camera_index=0, resolution=(640, 480), fps=30)

    assert result is True
    assert webcam_handler.is_capturing
    mock_video_capture.assert_called_with(0)
    mock_capture_instance.set.assert_any_call(3, 640)  # CAP_PROP_FRAME_WIDTH
    mock_capture_instance.set.assert_any_call(4, 480)  # CAP_PROP_FRAME_HEIGHT
    mock_capture_instance.set.assert_any_call(5, 30)  # CAP_PROP_FPS
    assert webcam_handler.capture_thread.is_alive()


@patch("cv2.VideoCapture")
def test_start_capture_failure(mock_video_capture, webcam_handler):
    """Verify that start_capture returns False if the camera cannot be opened."""
    mock_capture_instance = MagicMock()
    mock_capture_instance.isOpened.return_value = False
    mock_video_capture.return_value = mock_capture_instance

    result = webcam_handler.start_capture()

    assert result is False
    assert not webcam_handler.is_capturing


@patch("cv2.VideoCapture")
@patch("cv2.VideoWriter")
@patch("cv2.VideoWriter_fourcc")
def test_start_and_stop_recording(
    mock_fourcc, mock_video_writer, mock_video_capture, webcam_handler, tmp_path
):
    """Verify the full lifecycle of recording: start and stop."""
    # --- Setup for starting capture ---
    mock_capture_instance = MagicMock()
    mock_capture_instance.isOpened.return_value = True
    mock_video_capture.return_value = mock_capture_instance
    webcam_handler.start_capture()

    # --- Setup for starting recording ---
    mock_writer_instance = MagicMock()
    mock_writer_instance.isOpened.return_value = True
    mock_video_writer.return_value = mock_writer_instance
    mock_fourcc.return_value = 1234  # A dummy fourcc code

    output_file = tmp_path / "test_video.mp4"
    result = webcam_handler.start_recording(str(output_file), codec="XVID")

    assert result is True
    assert webcam_handler.is_recording
    mock_fourcc.assert_called_with(*"XVID")
    mock_video_writer.assert_called_with(str(output_file), 1234, 30, (640, 480))
    assert webcam_handler.video_writer is not None

    # --- Test stopping recording ---
    webcam_handler.stop_recording()

    assert not webcam_handler.is_recording
    mock_writer_instance.release.assert_called_once()
    assert webcam_handler.video_writer is None


def test_shutdown_cleans_up_resources(webcam_handler):
    """Verify that shutdown stops capture and recording and releases resources."""
    # Use MagicMock to spy on the internal methods
    webcam_handler.stop_capture = MagicMock()
    webcam_handler.cap = MagicMock()

    webcam_handler.shutdown()

    webcam_handler.stop_capture.assert_called_once()
