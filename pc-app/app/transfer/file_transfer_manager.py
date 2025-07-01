"""
File Transfer Manager for GSR Multimodal System
Handles automatic file transfer from Android devices to PC after recording.
"""

import logging
import os
import socket
import threading
import time
from pathlib import Path
from typing import Dict, List, Optional, Callable, Any
import json
import hashlib

logger = logging.getLogger(__name__)

class FileTransferManager:
    """Manages automatic file transfer from Android devices to PC."""

    def __init__(self, session_manager=None):
        """Initialize the file transfer manager."""
        self.session_manager = session_manager
        self.transfer_callbacks: List[Callable] = []
        self.active_transfers: Dict[str, Dict] = {}
        self.transfer_port = 8081  # Different port from main communication
        
    def add_transfer_callback(self, callback: Callable):
        """Add a callback function for transfer progress updates."""
        self.transfer_callbacks.append(callback)
        
    def remove_transfer_callback(self, callback: Callable):
        """Remove a transfer callback function."""
        if callback in self.transfer_callbacks:
            self.transfer_callbacks.remove(callback)

    def request_device_files(self, device_manager, device_id: str) -> Optional[Dict[str, Any]]:
        """Request list of recorded files from a device."""
        try:
            logger.info(f"Requesting file list from device {device_id}")
            
            # Send file list request command
            success = device_manager.send_command_to_device(device_id, "get_recorded_files")
            if not success:
                logger.error(f"Failed to send file list request to {device_id}")
                return None
                
            # Wait for response (this would need to be handled in device_manager)
            # For now, return a mock response structure
            return {
                "device_id": device_id,
                "files": [],
                "total_size": 0,
                "session_id": self.session_manager.get_session_info()["session_id"] if self.session_manager else None
            }
            
        except Exception as e:
            logger.error(f"Error requesting files from device {device_id}: {e}")
            return None

    def start_file_transfer(self, device_manager, device_id: str, destination_dir: str) -> bool:
        """Start automatic file transfer from a device."""
        try:
            logger.info(f"Starting file transfer from device {device_id}")
            
            # Get file list from device
            file_info = self.request_device_files(device_manager, device_id)
            if not file_info:
                logger.error(f"Could not get file list from device {device_id}")
                return False
                
            # Create device-specific directory
            device_dir = Path(destination_dir) / f"device_{device_id}"
            device_dir.mkdir(parents=True, exist_ok=True)
            
            # Initialize transfer tracking
            self.active_transfers[device_id] = {
                "status": "starting",
                "files": file_info.get("files", []),
                "total_files": len(file_info.get("files", [])),
                "transferred_files": 0,
                "total_size": file_info.get("total_size", 0),
                "transferred_size": 0,
                "start_time": time.time(),
                "destination_dir": str(device_dir)
            }
            
            # Start transfer in background thread
            transfer_thread = threading.Thread(
                target=self._transfer_files_thread,
                args=(device_manager, device_id, str(device_dir)),
                daemon=True
            )
            transfer_thread.start()
            
            self._notify_callbacks(device_id, "transfer_started", self.active_transfers[device_id])
            return True
            
        except Exception as e:
            logger.error(f"Error starting file transfer from device {device_id}: {e}")
            return False

    def _transfer_files_thread(self, device_manager, device_id: str, destination_dir: str):
        """Background thread for file transfer operations."""
        try:
            transfer_info = self.active_transfers[device_id]
            transfer_info["status"] = "transferring"
            
            # Send transfer start command to device
            transfer_command = {
                "transfer_port": self.transfer_port,
                "destination": "pc",
                "session_id": transfer_info.get("session_id")
            }
            
            success = device_manager.send_command_to_device(device_id, "start_file_transfer", transfer_command)
            if not success:
                transfer_info["status"] = "failed"
                transfer_info["error"] = "Failed to start transfer on device"
                self._notify_callbacks(device_id, "transfer_failed", transfer_info)
                return
                
            # Set up file transfer server
            self._setup_transfer_server(device_id, destination_dir)
            
        except Exception as e:
            logger.error(f"Error in file transfer thread for device {device_id}: {e}")
            if device_id in self.active_transfers:
                self.active_transfers[device_id]["status"] = "failed"
                self.active_transfers[device_id]["error"] = str(e)
                self._notify_callbacks(device_id, "transfer_failed", self.active_transfers[device_id])

    def _setup_transfer_server(self, device_id: str, destination_dir: str):
        """Set up a temporary server to receive files from device."""
        try:
            server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            server_socket.bind(('', self.transfer_port))
            server_socket.listen(1)
            server_socket.settimeout(30)  # 30 second timeout
            
            logger.info(f"File transfer server listening on port {self.transfer_port} for device {device_id}")
            
            # Wait for device connection
            client_socket, client_address = server_socket.accept()
            logger.info(f"Device {device_id} connected for file transfer from {client_address}")
            
            # Handle file transfer
            self._handle_file_transfer(device_id, client_socket, destination_dir)
            
            client_socket.close()
            server_socket.close()
            
        except socket.timeout:
            logger.error(f"File transfer timeout for device {device_id}")
            if device_id in self.active_transfers:
                self.active_transfers[device_id]["status"] = "failed"
                self.active_transfers[device_id]["error"] = "Transfer timeout"
                self._notify_callbacks(device_id, "transfer_failed", self.active_transfers[device_id])
        except Exception as e:
            logger.error(f"Error in file transfer server for device {device_id}: {e}")
            if device_id in self.active_transfers:
                self.active_transfers[device_id]["status"] = "failed"
                self.active_transfers[device_id]["error"] = str(e)
                self._notify_callbacks(device_id, "transfer_failed", self.active_transfers[device_id])

    def _handle_file_transfer(self, device_id: str, client_socket: socket.socket, destination_dir: str):
        """Handle the actual file transfer from device."""
        try:
            transfer_info = self.active_transfers[device_id]
            buffer_size = 8192  # 8KB buffer
            
            while True:
                # Receive file header
                header_data = self._receive_exact(client_socket, 1024)
                if not header_data:
                    break
                    
                try:
                    header = json.loads(header_data.decode('utf-8').strip('\x00'))
                except json.JSONDecodeError:
                    logger.error(f"Invalid file header from device {device_id}")
                    break
                
                if header.get("type") == "file":
                    # Receive file
                    filename = header["filename"]
                    file_size = header["size"]
                    file_type = header.get("file_type", "unknown")
                    
                    logger.info(f"Receiving file {filename} ({file_size} bytes) from device {device_id}")
                    
                    # Create file path
                    file_path = Path(destination_dir) / filename
                    file_path.parent.mkdir(parents=True, exist_ok=True)
                    
                    # Receive file data
                    received_size = 0
                    file_hash = hashlib.md5()
                    
                    with open(file_path, 'wb') as f:
                        while received_size < file_size:
                            chunk_size = min(buffer_size, file_size - received_size)
                            chunk = client_socket.recv(chunk_size)
                            if not chunk:
                                break
                            f.write(chunk)
                            file_hash.update(chunk)
                            received_size += len(chunk)
                            
                            # Update progress
                            transfer_info["transferred_size"] += len(chunk)
                            self._notify_callbacks(device_id, "transfer_progress", transfer_info)
                    
                    if received_size == file_size:
                        transfer_info["transferred_files"] += 1
                        logger.info(f"Successfully received file {filename} from device {device_id}")
                        
                        # Add file to session manager
                        if self.session_manager and self.session_manager.is_session_active():
                            file_info = {
                                "filename": filename,
                                "file_path": str(file_path),
                                "file_size": file_size,
                                "file_type": file_type,
                                "checksum": file_hash.hexdigest(),
                                "transferred_at": time.time()
                            }
                            self.session_manager.add_device_file(device_id, file_type, file_info)
                    else:
                        logger.error(f"Incomplete file transfer for {filename} from device {device_id}")
                        
                elif header.get("type") == "complete":
                    # Transfer complete
                    logger.info(f"File transfer completed for device {device_id}")
                    transfer_info["status"] = "completed"
                    transfer_info["end_time"] = time.time()
                    transfer_info["duration"] = transfer_info["end_time"] - transfer_info["start_time"]
                    self._notify_callbacks(device_id, "transfer_completed", transfer_info)
                    break
                    
        except Exception as e:
            logger.error(f"Error handling file transfer for device {device_id}: {e}")
            transfer_info["status"] = "failed"
            transfer_info["error"] = str(e)
            self._notify_callbacks(device_id, "transfer_failed", transfer_info)

    def _receive_exact(self, sock: socket.socket, size: int) -> bytes:
        """Receive exactly the specified number of bytes."""
        data = b''
        while len(data) < size:
            chunk = sock.recv(size - len(data))
            if not chunk:
                break
            data += chunk
        return data

    def _notify_callbacks(self, device_id: str, event: str, data: Dict[str, Any]):
        """Notify all registered callbacks about transfer events."""
        for callback in self.transfer_callbacks:
            try:
                callback(device_id, event, data)
            except Exception as e:
                logger.error(f"Error in transfer callback: {e}")

    def get_transfer_status(self, device_id: str) -> Optional[Dict[str, Any]]:
        """Get current transfer status for a device."""
        return self.active_transfers.get(device_id)

    def get_all_transfer_status(self) -> Dict[str, Dict[str, Any]]:
        """Get transfer status for all devices."""
        return self.active_transfers.copy()

    def cancel_transfer(self, device_id: str) -> bool:
        """Cancel an ongoing file transfer."""
        try:
            if device_id in self.active_transfers:
                self.active_transfers[device_id]["status"] = "cancelled"
                self._notify_callbacks(device_id, "transfer_cancelled", self.active_transfers[device_id])
                logger.info(f"File transfer cancelled for device {device_id}")
                return True
            return False
        except Exception as e:
            logger.error(f"Error cancelling transfer for device {device_id}: {e}")
            return False

    def cleanup_completed_transfers(self):
        """Clean up completed or failed transfers."""
        completed_devices = []
        for device_id, transfer_info in self.active_transfers.items():
            if transfer_info["status"] in ["completed", "failed", "cancelled"]:
                completed_devices.append(device_id)
        
        for device_id in completed_devices:
            del self.active_transfers[device_id]
            logger.info(f"Cleaned up transfer info for device {device_id}")

    def start_batch_transfer(self, device_manager, device_ids: List[str], destination_dir: str) -> Dict[str, bool]:
        """Start file transfer for multiple devices."""
        results = {}
        for device_id in device_ids:
            results[device_id] = self.start_file_transfer(device_manager, device_id, destination_dir)
        return results