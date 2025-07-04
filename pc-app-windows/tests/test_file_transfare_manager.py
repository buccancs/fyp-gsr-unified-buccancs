import json
import socket
import threading
import time
from pathlib import Path
from unittest.mock import MagicMock, call, patch

import pytest

from app.session.session_manager import SessionManager
from app.transfer.file_transfer_manager import FileTransferManager


# Helper class to simulate a connected Android device socket
class MockClientSocket:
    def __init__(self, files_to_send):
        self.files_to_send = files_to_send
        self.sent_data = b""
        self.is_closed = False

    def recv(self, size):
        if not self.files_to_send:
            # Send completion message when out of files
            completion_header = (
                json.dumps({"type": "complete"}).ljust(1024, "\0").encode("utf-8")
            )
            if not self.sent_data:  # Send only once
                self.sent_data += completion_header
                return completion_header
            return b""

        # Simulate sending one file at a time
        filename, content = self.files_to_send.pop(0)
        header = {"type": "file", "filename": filename, "size": len(content)}
        header_bytes = json.dumps(header).ljust(1024, "\0").encode("utf-8")

        # Combine header and content for the recv buffer
        full_data = header_bytes + content

        if not self.sent_data:
            self.sent_data = full_data

        if size >= len(self.sent_data):
            data_to_return = self.sent_data
            self.sent_data = b""
            return data_to_return
        else:
            data_to_return = self.sent_data[:size]
            self.sent_data = self.sent_data[size:]
            return data_to_return

    def close(self):
        self.is_closed = True


@pytest.fixture
def session_manager(tmp_path):
    """Fixture for a SessionManager instance."""
    return SessionManager(base_dir=tmp_path)


@pytest.fixture
def file_transfer_manager(session_manager):
    """Fixture for a FileTransferManager instance."""
    return FileTransferManager(session_manager)


def test_start_file_transfer_sends_correct_command(
    file_transfer_manager, session_manager
):
    """Verify that starting a transfer sends the correct command to the device."""
    device_id = "android_test_1"

    # Mock the device manager
    mock_device_manager = MagicMock()
    mock_device_manager.send_command_to_device.return_value = True

    # Mock the file list request to return some files
    with patch.object(
        file_transfer_manager,
        "request_device_files",
        return_value={"files": ["test.mp4"], "total_size": 100},
    ):
        # Start a session and register the device
        session_manager.start_session()
        session_manager.register_device(device_id, {})
        device_dir = session_manager.get_device_directory(device_id)

        # Start the transfer
        file_transfer_manager.start_file_transfer(
            mock_device_manager, device_id, str(device_dir.parent)
        )

        # Verify the 'start_file_transfer' command was sent
        mock_device_manager.send_command_to_device.assert_called_with(
            device_id, "start_file_transfer", ANY
        )

        # Check that the command data contains the correct port
        args, kwargs = mock_device_manager.send_command_to_device.call_args
        command_data = args[2]
        assert command_data["transfer_port"] == file_transfer_manager.transfer_port


def test_file_transfer_thread_receives_files_correctly(
    file_transfer_manager, session_manager, tmp_path
):
    """Test the complete file transfer process using mock sockets."""
    device_id = "android_test_2"

    # Prepare mock files to be "sent" by the device
    files_to_send = [
        ("rgb_video.mp4", b"video_content_123"),
        ("gsr_data.csv", b"timestamp,value\n1,10\n2,11"),
    ]

    # Create a mock client socket that will simulate the Android device connecting
    mock_client_socket = MockClientSocket(files_to_send)

    # Mock the server socket to accept our mock client
    mock_server_socket = MagicMock()
    mock_server_socket.accept.return_value = (mock_client_socket, ("127.0.0.1", 54321))

    # Use patch to replace the real socket creation with our mock
    with patch("socket.socket") as mock_socket_class:
        mock_socket_class.return_value = mock_server_socket

        # Start a session and get the destination directory
        session_id = session_manager.start_session()
        session_dir = tmp_path / session_id

        # Manually call the transfer thread method to test its logic
        file_transfer_manager.active_transfers[device_id] = {"status": "starting"}
        file_transfer_manager._transfer_files_thread(
            MagicMock(), device_id, str(session_dir)
        )

        # Wait a moment for the thread to (conceptually) finish
        time.sleep(0.1)

    # Assertions
    # Check that the files were created in the correct location
    dest_path = session_dir
    assert (dest_path / "rgb_video.mp4").exists()
    assert (dest_path / "gsr_data.csv").exists()

    # Check file content
    assert (dest_path / "rgb_video.mp4").read_bytes() == b"video_content_123"
    assert (dest_path / "gsr_data.csv").read_bytes() == b"timestamp,value\n1,10\n2,11"

    # Check that the transfer status is marked as completed
    status = file_transfer_manager.get_transfer_status(device_id)
    assert status["status"] == "completed"
    assert status["transferred_files"] == 2


def test_transfer_fails_on_socket_timeout(file_transfer_manager, session_manager):
    """Verify that the transfer status is set to 'failed' on a socket timeout."""
    device_id = "android_timeout"

    # Mock the server socket to raise a timeout
    mock_server_socket = MagicMock()
    mock_server_socket.accept.side_effect = socket.timeout("Simulated timeout")

    with patch("socket.socket") as mock_socket_class:
        mock_socket_class.return_value = mock_server_socket

        session_manager.start_session()

        file_transfer_manager.active_transfers[device_id] = {"status": "starting"}
        file_transfer_manager._transfer_files_thread(MagicMock(), device_id, "/tmp")

    # Verify the status
    status = file_transfer_manager.get_transfer_status(device_id)
    assert status["status"] == "failed"
    assert "timeout" in status["error"].lower()
