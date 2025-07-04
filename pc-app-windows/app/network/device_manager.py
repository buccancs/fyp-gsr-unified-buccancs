import json
import logging
import socket
import threading
import time
from datetime import datetime
from typing import Any, Callable, Dict, List, Optional

logger = logging.getLogger(__name__)


class DeviceManager:
    """
    Manages communication with Android devices using TCP/IP with JSON protocol.
    Implements the communication protocol specified in the README:
    - TCP over WiFi on port 8080
    - JSON message format with UTF-8 encoding
    - Heartbeat every 30 seconds with 60-second timeout
    - Time synchronization with PC as reference clock
    """

    def __init__(self, session_manager=None):
        self.host = "0.0.0.0"
        self.port = 8080
        self.server_socket = None
        self.clients = (
            {}
        )  # device_id -> {"socket": socket, "info": device_info, "last_heartbeat": timestamp}
        self.client_threads = {}
        self.is_running = False
        self.session_manager = session_manager
        self.heartbeat_interval = 30  # seconds
        self.heartbeat_timeout = 60  # seconds
        self.message_callbacks = []

        # Start heartbeat monitoring thread
        self.heartbeat_thread = None

    def add_message_callback(self, callback: Callable):
        """Add a callback function for incoming messages."""
        self.message_callbacks.append(callback)

    def remove_message_callback(self, callback: Callable):
        """Remove a message callback function."""
        if callback in self.message_callbacks:
            self.message_callbacks.remove(callback)

    def start_server(self):
        """Start the TCP server for device communication."""
        if self.is_running:
            logger.info("Server is already running.")
            return

        self.is_running = True
        self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.server_socket.settimeout(1.0)  # Allow periodic checks

        try:
            self.server_socket.bind((self.host, self.port))
            self.server_socket.listen(5)
            logger.info(f"Server listening on {self.host}:{self.port}")

            self.accept_thread = threading.Thread(
                target=self._accept_connections, daemon=True
            )
            self.accept_thread.start()

            # Start heartbeat monitoring
            self.heartbeat_thread = threading.Thread(
                target=self._heartbeat_monitor, daemon=True
            )
            self.heartbeat_thread.start()

        except Exception as e:
            logger.error(f"Failed to start server: {e}")
            self.is_running = False

    def _accept_connections(self):
        """Accept incoming connections from Android devices."""
        while self.is_running:
            try:
                client_socket, addr = self.server_socket.accept()
                logger.info(f"Accepted connection from {addr}")
                client_thread = threading.Thread(
                    target=self._handle_client, args=(client_socket, addr), daemon=True
                )
                client_thread.start()
            except socket.timeout:
                continue
            except Exception as e:
                if self.is_running:
                    logger.error(f"Error accepting connection: {e}")
                break

    def _handle_client(self, client_socket, addr):
        """Handle communication with a connected Android device."""
        device_id = None
        try:
            client_socket.settimeout(5.0)  # Timeout for initial handshake

            # First message from client should be device registration
            device_info_raw = client_socket.recv(4096).decode("utf-8")
            device_registration = json.loads(device_info_raw)

            # Validate registration message
            if device_registration.get("command") != "register_device":
                logger.error(f"Invalid registration from {addr}")
                return

            device_id = device_registration.get(
                "device_id", f"unknown_{addr[0]}_{addr[1]}"
            )
            device_info = device_registration.get("data", {})

            # Store client information
            self.clients[device_id] = {
                "socket": client_socket,
                "address": addr,
                "info": device_info,
                "last_heartbeat": time.time(),
                "connected_time": time.time(),
            }
            self.client_threads[device_id] = threading.current_thread()

            # Register device with session manager if available
            if self.session_manager and self.session_manager.is_session_active():
                try:
                    device_dir = self.session_manager.register_device(
                        device_id, device_info
                    )
                    logger.info(
                        f"Device {device_id} registered to session directory: {device_dir}"
                    )
                except Exception as e:
                    logger.error(f"Failed to register device with session manager: {e}")

            # Send registration confirmation with time sync
            self._send_message(
                device_id,
                {
                    "command": "registration_confirmed",
                    "timestamp": time.time(),
                    "data": {
                        "server_time": time.time(),
                        "device_registered": True,
                        "session_active": (
                            self.session_manager.is_session_active()
                            if self.session_manager
                            else False
                        ),
                    },
                },
            )

            logger.info(f"Device {device_id} registered from {addr}")

            # Set longer timeout for regular communication
            client_socket.settimeout(self.heartbeat_timeout)

            # Handle ongoing communication
            while self.is_running:
                try:
                    data = client_socket.recv(4096)
                    if not data:
                        break

                    message_str = data.decode("utf-8")
                    message = json.loads(message_str)

                    # Update last heartbeat time
                    self.clients[device_id]["last_heartbeat"] = time.time()

                    # Process the message
                    self._process_message(device_id, message)

                except socket.timeout:
                    # Check if device has timed out
                    if (
                        time.time() - self.clients[device_id]["last_heartbeat"]
                        > self.heartbeat_timeout
                    ):
                        logger.warning(f"Device {device_id} timed out")
                        break
                except json.JSONDecodeError as e:
                    logger.error(f"Invalid JSON from {device_id}: {e}")
                except Exception as e:
                    logger.error(f"Error processing message from {device_id}: {e}")

        except Exception as e:
            logger.error(f"Error handling client {addr}: {e}")
        finally:
            # Clean up client connection
            if device_id and device_id in self.clients:
                del self.clients[device_id]
                if device_id in self.client_threads:
                    del self.client_threads[device_id]
            client_socket.close()
            logger.info(f"Client {addr} ({device_id}) disconnected.")

    def _process_message(self, device_id: str, message: Dict[str, Any]):
        """Process incoming message from device."""
        command = message.get("command")
        timestamp = message.get("timestamp")
        data = message.get("data", {})

        logger.debug(f"Processing message from {device_id}: {command}")

        # Handle different message types
        if command == "heartbeat":
            self._handle_heartbeat(device_id, message)
        elif command == "status_update":
            self._handle_status_update(device_id, message)
        elif command == "data_stream":
            self._handle_data_stream(device_id, message)
        elif command == "recording_complete":
            self._handle_recording_complete(device_id, message)
        elif command == "error_report":
            self._handle_error_report(device_id, message)
        else:
            logger.warning(f"Unknown command from {device_id}: {command}")

        # Notify callbacks
        for callback in self.message_callbacks:
            try:
                callback(device_id, message)
            except Exception as e:
                logger.error(f"Error in message callback: {e}")

    def _handle_heartbeat(self, device_id: str, message: Dict[str, Any]):
        """Handle heartbeat message from device."""
        # Send heartbeat response with time sync
        self._send_message(
            device_id,
            {
                "command": "heartbeat_response",
                "timestamp": time.time(),
                "data": {"server_time": time.time(), "device_status": "connected"},
            },
        )

    def _handle_status_update(self, device_id: str, message: Dict[str, Any]):
        """Handle status update from device."""
        status = message.get("data", {})
        logger.info(f"Status update from {device_id}: {status}")

    def _handle_data_stream(self, device_id: str, message: Dict[str, Any]):
        """Handle real-time data stream from device."""
        data = message.get("data", {})
        # This could be GSR data, preview frames, etc.
        logger.debug(
            f"Data stream from {device_id}: {data.get('data_type', 'unknown')}"
        )

    def _handle_recording_complete(self, device_id: str, message: Dict[str, Any]):
        """Handle recording completion notification."""
        data = message.get("data", {})
        logger.info(f"Recording complete on {device_id}: {data}")

    def _handle_error_report(self, device_id: str, message: Dict[str, Any]):
        """Handle error report from device."""
        error_data = message.get("data", {})
        logger.error(f"Error reported by {device_id}: {error_data}")

    def _heartbeat_monitor(self):
        """Monitor device heartbeats and send periodic heartbeats."""
        while self.is_running:
            try:
                current_time = time.time()

                # Check for timed out devices
                timed_out_devices = []
                for device_id, client_info in self.clients.items():
                    if (
                        current_time - client_info["last_heartbeat"]
                        > self.heartbeat_timeout
                    ):
                        timed_out_devices.append(device_id)

                # Remove timed out devices
                for device_id in timed_out_devices:
                    logger.warning(f"Device {device_id} timed out, removing")
                    try:
                        self.clients[device_id]["socket"].close()
                    except:
                        pass
                    if device_id in self.clients:
                        del self.clients[device_id]
                    if device_id in self.client_threads:
                        del self.client_threads[device_id]

                # Send heartbeat to all connected devices
                for device_id in list(self.clients.keys()):
                    self._send_message(
                        device_id,
                        {
                            "command": "heartbeat",
                            "timestamp": current_time,
                            "data": {"server_time": current_time},
                        },
                    )

                time.sleep(self.heartbeat_interval)

            except Exception as e:
                logger.error(f"Error in heartbeat monitor: {e}")
                time.sleep(5)

    def _send_message(self, device_id: str, message: Dict[str, Any]) -> bool:
        """Send a JSON message to a specific device."""
        if device_id not in self.clients:
            logger.warning(f"Device {device_id} not connected.")
            return False

        try:
            message_str = json.dumps(message, ensure_ascii=False)
            message_bytes = message_str.encode("utf-8")

            self.clients[device_id]["socket"].sendall(message_bytes)
            logger.debug(
                f"Sent message to {device_id}: {message.get('command', 'unknown')}"
            )
            return True

        except Exception as e:
            logger.error(f"Error sending message to {device_id}: {e}")
            return False

    def send_command(
        self, device_id: str, command: str, data: Optional[Dict[str, Any]] = None
    ) -> bool:
        """Send a command to a specific device."""
        message = {"command": command, "timestamp": time.time(), "data": data or {}}
        return self._send_message(device_id, message)

    def send_command_to_all(
        self, command: str, data: Optional[Dict[str, Any]] = None
    ) -> int:
        """Send a command to all connected devices. Returns number of devices reached."""
        count = 0
        for device_id in list(self.clients.keys()):
            if self.send_command(device_id, command, data):
                count += 1
        return count

    def send_command_to_device(
        self, device_id: str, command: str, data: Optional[Dict[str, Any]] = None
    ) -> bool:
        """Send a command to a specific device (alias for send_command)."""
        return self.send_command(device_id, command, data)

    def get_connected_devices(self) -> List[str]:
        """Get list of currently connected device IDs."""
        return list(self.clients.keys())

    def get_device_info(self, device_id: str) -> Optional[Dict[str, Any]]:
        """Get information about a specific device."""
        if device_id in self.clients:
            return self.clients[device_id]["info"]
        return None

    def get_all_device_info(self) -> Dict[str, Dict[str, Any]]:
        """Get information about all connected devices."""
        return {
            device_id: {
                "info": client_info["info"],
                "address": client_info["address"],
                "connected_time": client_info["connected_time"],
                "last_heartbeat": client_info["last_heartbeat"],
            }
            for device_id, client_info in self.clients.items()
        }

    def stop_server(self):
        """Stop the TCP server and disconnect all devices."""
        logger.info("Stopping device manager server...")
        self.is_running = False

        # Close all client connections
        for device_id, client_info in self.clients.items():
            try:
                client_info["socket"].close()
            except:
                pass

        # Close server socket
        if self.server_socket:
            self.server_socket.close()

        # Wait for threads to finish
        if self.accept_thread and self.accept_thread.is_alive():
            self.accept_thread.join(timeout=2.0)
        if self.heartbeat_thread and self.heartbeat_thread.is_alive():
            self.heartbeat_thread.join(timeout=2.0)

        self.clients.clear()
        self.client_threads.clear()
        logger.info("Device manager server stopped.")
