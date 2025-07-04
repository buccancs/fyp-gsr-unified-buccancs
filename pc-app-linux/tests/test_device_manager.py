import json
import socket
import threading
import time
from unittest.mock import MagicMock, patch

import pytest

from app.network.device_manager import DeviceManager
from app.session.session_manager import SessionManager


# A mock socket client to simulate an Android device connection
class MockSocketClient:
    def __init__(self, initial_message=None):
        self.sent_data = []
        self.is_closed = False
        self.recv_buffer = [initial_message] if initial_message else []
        self.timeout_error = socket.timeout("Simulated timeout")

    def recv(self, size):
        if not self.recv_buffer:
            raise self.timeout_error
        return self.recv_buffer.pop(0)

    def sendall(self, data):
        self.sent_data.append(data)

    def close(self):
        self.is_closed = True

    def settimeout(self, timeout):
        pass

    def add_to_buffer(self, message):
        self.recv_buffer.append(message)


@pytest.fixture
def session_manager(tmp_path):
    """Fixture for a SessionManager instance."""
    return SessionManager(base_dir=tmp_path)


@pytest.fixture
def device_manager(session_manager):
    """Fixture for a DeviceManager instance."""
    manager = DeviceManager(session_manager)
    # Use monkeypatch to prevent the server from actually starting
    with patch.object(manager, "_accept_connections"), patch.object(
        manager, "_heartbeat_monitor"
    ):
        yield manager
    manager.stop_server()


def test_start_and_stop_server(device_manager):
    """Verify that the server can be started and stopped."""
    # We use a real socket here to test bind/listen/close
    with patch("socket.socket") as mock_socket_class:
        mock_server_socket = MagicMock()
        mock_socket_class.return_value = mock_server_socket

        device_manager.start_server()
        assert device_manager.is_running
        mock_server_socket.bind.assert_called_with(("0.0.0.0", 8080))
        mock_server_socket.listen.assert_called_with(5)

        device_manager.stop_server()
        assert not device_manager.is_running
        mock_server_socket.close.assert_called()


def test_handle_client_registration_and_disconnect(device_manager):
    """Test the full lifecycle of a client: connect, register, disconnect."""
    device_id = "android_test_1"
    registration_msg = json.dumps(
        {
            "command": "register_device",
            "device_id": device_id,
            "data": {"device_name": "Pixel Test"},
        }
    ).encode("utf-8")

    mock_client_socket = MockSocketClient(initial_message=registration_msg)

    # To simulate a disconnect, the recv buffer will become empty, and then return b''
    mock_client_socket.add_to_buffer(b"")

    # Manually call _handle_client to test its logic
    device_manager._handle_client(mock_client_socket, ("127.0.0.1", 12345))

    # Assertions
    assert (
        device_id not in device_manager.clients
    )  # Should be cleaned up after disconnect
    assert mock_client_socket.is_closed

    # Check that a confirmation was sent
    assert len(mock_client_socket.sent_data) > 0
    response = json.loads(mock_client_socket.sent_data[0].decode("utf-8"))
    assert response["command"] == "registration_confirmed"
    assert response["data"]["device_registered"] is True


def test_heartbeat_monitor_removes_timed_out_clients(device_manager, monkeypatch):
    """Verify that the heartbeat monitor correctly identifies and removes stale clients."""
    device_id = "android_stale_1"

    # Add a mock client that is already "timed out"
    mock_socket = MockSocketClient()
    device_manager.clients[device_id] = {
        "socket": mock_socket,
        "address": ("127.0.0.1", 54321),
        "info": {},
        "last_heartbeat": time.time()
        - (device_manager.heartbeat_timeout + 5),  # 5s overdue
        "connected_time": time.time(),
    }

    # Run one cycle of the heartbeat monitor
    device_manager._heartbeat_monitor()

    # Assertions
    assert device_id not in device_manager.clients
    assert mock_socket.is_closed


def test_send_command_to_all(device_manager):
    """Verify that a command is broadcast to all connected clients."""
    # Add two mock clients
    client1_socket = MockSocketClient()
    client2_socket = MockSocketClient()
    device_manager.clients["device1"] = {
        "socket": client1_socket,
        "last_heartbeat": time.time(),
    }
    device_manager.clients["device2"] = {
        "socket": client2_socket,
        "last_heartbeat": time.time(),
    }

    command = "start_recording"
    data = {"session_id": "session_xyz"}

    devices_reached = device_manager.send_command_to_all(command, data)

    assert devices_reached == 2

    # Verify command sent to client 1
    sent_msg1 = json.loads(client1_socket.sent_data[0].decode("utf-8"))
    assert sent_msg1["command"] == command
    assert sent_msg1["data"] == data

    # Verify command sent to client 2
    sent_msg2 = json.loads(client2_socket.sent_data[0].decode("utf-8"))
    assert sent_msg2["command"] == command
    assert sent_msg2["data"] == data
