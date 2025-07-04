import sys
import time
from unittest.mock import MagicMock, patch

import pytest
from PySide6.QtCore import Qt
from PySide6.QtWidgets import QApplication

# Mock backend components before importing the MainWindow
# This ensures that the UI tests are isolated from the backend logic.
mock_device_manager = MagicMock()
mock_gsr_handler = MagicMock()
mock_webcam_handler = MagicMock()
mock_session_manager = MagicMock()
mock_file_transfer_manager = MagicMock()

# We need to patch the imports within the main_window module
with patch.dict(
    "sys.modules",
    {
        "app.network.device_manager": MagicMock(
            DeviceManager=lambda sm: mock_device_manager
        ),
        "app.sensors.gsr_handler": MagicMock(GSRHandler=lambda: mock_gsr_handler),
        "app.sensors.webcam_handler": MagicMock(
            WebcamHandler=lambda: mock_webcam_handler
        ),
        "app.session.session_manager": MagicMock(
            SessionManager=lambda: mock_session_manager
        ),
        "app.transfer.file_transfer_manager": MagicMock(
            FileTransferManager=lambda sm: mock_file_transfer_manager
        ),
    },
):
    from app.gui.main_window import MainWindow


@pytest.fixture
def app(qtbot):
    """Fixture to create the main application window for testing."""
    # Reset mocks before each test
    mock_device_manager.reset_mock()
    mock_gsr_handler.reset_mock()
    mock_webcam_handler.reset_mock()
    mock_session_manager.reset_mock()
    mock_file_transfer_manager.reset_mock()

    # Create the main window instance with mocked backends
    window = MainWindow(
        device_manager=mock_device_manager,
        gsr_handler=mock_gsr_handler,
        webcam_handler=mock_webcam_handler,
        session_manager=mock_session_manager,
        file_transfer_manager=mock_file_transfer_manager,
    )
    qtbot.addWidget(window)
    window.show()
    return window, qtbot


def test_main_window_initialization(app):
    """Verify that the main window and its widgets are created correctly."""
    window, qtbot = app
    assert window.windowTitle() == "GSR Multimodal PC Controller"

    # Check for key widgets
    assert window.start_all_button.text() == "Start All Recording"
    assert window.stop_all_button.text() == "Stop All Recording"
    assert window.start_webcam_button.text() == "Start PC Webcam"
    assert window.device_tabs.count() == 0  # No devices connected initially


def test_start_pc_webcam_button_click(app):
    """Test that clicking 'Start PC Webcam' calls the correct handler method."""
    window, qtbot = app

    # Simulate a successful webcam start
    mock_webcam_handler.start_capture.return_value = True

    qtbot.mouseClick(window.start_webcam_button, Qt.LeftButton)

    # Verify that the webcam handler's start_capture method was called
    mock_webcam_handler.start_capture.assert_called_once()
    assert "PC Webcam started" in window.status_text.toPlainText()


def test_stop_pc_webcam_button_click(app):
    """Test that clicking 'Stop PC Webcam' calls the correct handler method."""
    window, qtbot = app

    qtbot.mouseClick(window.stop_webcam_button, Qt.LeftButton)

    # Verify that the webcam handler's stop_capture method was called
    mock_webcam_handler.stop_capture.assert_called_once()
    assert "PC Webcam stopped" in window.status_text.toPlainText()


def test_start_all_recording_button_click(app):
    """Verify that 'Start All Recording' starts a session and sends commands."""
    window, qtbot = app

    # Simulate a successful session start and device connection
    mock_session_manager.start_session.return_value = "session_test_123"
    mock_device_manager.get_connected_devices.return_value = ["android_test_1"]
    mock_device_manager.send_command_to_all.return_value = 1

    qtbot.mouseClick(window.start_all_button, Qt.LeftButton)

    # Verify that the session manager was called
    mock_session_manager.start_session.assert_called_once()

    # Verify that the device manager was called to send the start command
    mock_device_manager.send_command_to_all.assert_called_with(
        "start_recording",
        {
            "session_id": "session_test_123",
            "timestamp": pytest.approx(time.time(), abs=1),
            "recording_params": {
                "video_quality": "HD",
                "gsr_sampling_rate": 128,
                "thermal_fps": 25,
            },
        },
    )
    assert "Started new session: session_test_123" in window.status_text.toPlainText()


def test_stop_all_recording_button_click(app):
    """Verify that 'Stop All Recording' stops the session and initiates file transfer."""
    window, qtbot = app

    # Simulate an active session and connected devices
    mock_session_manager.get_session_info.return_value = {
        "session_id": "session_test_123"
    }
    mock_device_manager.get_connected_devices.return_value = ["android_test_1"]
    mock_device_manager.send_command_to_all.return_value = 1

    qtbot.mouseClick(window.stop_all_button, Qt.LeftButton)

    # Verify that the stop command was sent
    mock_device_manager.send_command_to_all.assert_called_with(
        "stop_recording",
        {
            "session_id": "session_test_123",
            "stop_timestamp": pytest.approx(time.time(), abs=1),
        },
    )

    # Verify that the session was stopped
    mock_session_manager.stop_session.assert_called_once()

    # Verify that file transfer was initiated
    mock_file_transfer_manager.start_file_transfer.assert_called_once()
    assert "Session stopped and data saved" in window.status_text.toPlainText()


def test_add_device_panel_on_connection(app):
    """Test that a new device tab is created when a device connects."""
    window, qtbot = app

    assert window.device_tabs.count() == 0

    # Simulate a device connection message by calling the handler directly
    device_id = "android_test_1"
    device_info = {"device_name": "Pixel Test"}
    window._handle_device_message(
        device_id, {"type": "device_connected", "data": device_info}
    )

    # Verify that a new tab was added
    assert window.device_tabs.count() == 1
    assert window.device_tabs.tabText(0) == "Pixel Test"
    assert device_id in window.device_panels


def test_update_device_gsr_data(app):
    """Test that the GSR label in a device panel is updated with new data."""
    window, qtbot = app

    # First, add a device panel
    device_id = "android_test_1"
    window._handle_device_message(device_id, {"type": "device_connected", "data": {}})

    gsr_label = window.device_panels[device_id]["gsr_label"]
    assert gsr_label.text() == "GSR: -- μS"

    # Simulate receiving GSR data
    window._handle_device_message(
        device_id, {"type": "gsr_data", "data": {"value": 12.345}}
    )

    # Verify the label was updated
    assert gsr_label.text() == "GSR: 12.3 μS"
