import datetime
import json
import logging
import time
from pathlib import Path
from typing import Any, Dict, List, Optional

logger = logging.getLogger(__name__)


class SessionManager:
    """
    Manages recording sessions with proper directory structure and metadata.
    Creates session directories following the specification:
    sessions/session_YYYYMMDD_HHMMSS/
    ├── metadata.json
    ├── device_001/
    ├── device_002/
    └── analysis/
    """

    def __init__(self, base_dir="./sessions"):
        self.base_dir = Path(base_dir)
        self.current_session_dir = None
        self.session_id = None
        self.session_start_time = None
        self.connected_devices = {}
        self.device_counter = 0
        logger.info(f"SessionManager initialized with base directory: {self.base_dir}")

    def start_session(self) -> str:
        """
        Start a new recording session.

        Returns:
            Session ID string
        """
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        self.session_id = f"session_{timestamp}"
        self.session_start_time = time.time()
        self.current_session_dir = self.base_dir / self.session_id

        # Create session directory structure
        self.current_session_dir.mkdir(parents=True, exist_ok=True)

        # Create analysis subdirectory
        (self.current_session_dir / "analysis").mkdir(exist_ok=True)

        logger.info(f"Session started: {self.session_id}")
        logger.info(f"Session directory: {self.current_session_dir}")

        self._write_session_metadata()
        return self.session_id

    def stop_session(self):
        """Stop the current recording session."""
        if self.current_session_dir:
            # Update metadata with session end time
            self._write_session_metadata(session_end=True)
            logger.info(f"Session stopped: {self.session_id}")

        self.current_session_dir = None
        self.session_id = None
        self.session_start_time = None
        self.connected_devices.clear()
        self.device_counter = 0

    def register_device(self, device_id: str, device_info: Dict[str, Any]) -> str:
        """
        Register a device for the current session.

        Args:
            device_id: Unique device identifier
            device_info: Device information dictionary

        Returns:
            Device directory name (e.g., "device_001")
        """
        if not self.current_session_dir:
            raise RuntimeError("No active session")

        self.device_counter += 1
        device_dir_name = f"device_{self.device_counter:03d}"
        device_dir = self.current_session_dir / device_dir_name
        device_dir.mkdir(exist_ok=True)

        # Store device mapping
        self.connected_devices[device_id] = {
            "device_dir_name": device_dir_name,
            "device_dir_path": device_dir,
            "device_info": device_info,
            "registration_time": time.time(),
        }

        # Write device info file
        device_info_file = device_dir / "device_info.json"
        with open(device_info_file, "w") as f:
            json.dump(
                {
                    "device_id": device_id,
                    "device_dir_name": device_dir_name,
                    "registration_time": datetime.datetime.now().isoformat(),
                    **device_info,
                },
                f,
                indent=4,
            )

        logger.info(f"Device registered: {device_id} -> {device_dir_name}")
        return device_dir_name

    def get_device_directory(self, device_id: str) -> Optional[Path]:
        """Get the directory path for a specific device."""
        if device_id in self.connected_devices:
            return self.connected_devices[device_id]["device_dir_path"]
        return None

    def get_session_info(self) -> Dict[str, Any]:
        """Get current session information."""
        return {
            "session_id": self.session_id,
            "session_dir": (
                str(self.current_session_dir) if self.current_session_dir else None
            ),
            "start_time": self.session_start_time,
            "connected_devices": list(self.connected_devices.keys()),
            "device_count": len(self.connected_devices),
        }

    def _write_session_metadata(self, session_end: bool = False):
        """Write session metadata to file."""
        if not self.current_session_dir:
            return

        metadata = {
            "session_id": self.session_id,
            "session_start_time": datetime.datetime.fromtimestamp(
                self.session_start_time
            ).isoformat(),
            "session_directory": str(self.current_session_dir),
            "connected_devices": {
                device_id: {
                    "device_dir_name": info["device_dir_name"],
                    "registration_time": datetime.datetime.fromtimestamp(
                        info["registration_time"]
                    ).isoformat(),
                    "device_info": info["device_info"],
                }
                for device_id, info in self.connected_devices.items()
            },
            "device_count": len(self.connected_devices),
        }

        if session_end:
            metadata["session_end_time"] = datetime.datetime.now().isoformat()
            metadata["session_duration"] = time.time() - self.session_start_time

        metadata_file = self.current_session_dir / "metadata.json"
        with open(metadata_file, "w") as f:
            json.dump(metadata, f, indent=4)

        logger.debug("Session metadata updated")

    def save_data(
        self, filename: str, data: str, device_id: Optional[str] = None, mode: str = "w"
    ):
        """
        Save data to session directory.

        Args:
            filename: Name of the file to save
            data: Data to write
            device_id: Optional device ID to save to device-specific directory
            mode: File write mode
        """
        if not self.current_session_dir:
            logger.warning("No active session. Data not saved.")
            return

        if device_id and device_id in self.connected_devices:
            # Save to device-specific directory
            file_path = self.connected_devices[device_id]["device_dir_path"] / filename
        else:
            # Save to session root directory
            file_path = self.current_session_dir / filename

        try:
            with open(file_path, mode) as f:
                f.write(data)
            logger.debug(f"Data saved to {file_path}")
        except Exception as e:
            logger.error(f"Failed to save data to {file_path}: {e}")

    def save_analysis_data(self, filename: str, data: str, mode: str = "w"):
        """Save analysis data to the analysis subdirectory."""
        if not self.current_session_dir:
            logger.warning("No active session. Analysis data not saved.")
            return

        analysis_dir = self.current_session_dir / "analysis"
        file_path = analysis_dir / filename

        try:
            with open(file_path, mode) as f:
                f.write(data)
            logger.debug(f"Analysis data saved to {file_path}")
        except Exception as e:
            logger.error(f"Failed to save analysis data to {file_path}: {e}")

    def is_session_active(self) -> bool:
        """Check if a session is currently active."""
        return self.current_session_dir is not None

    def get_all_sessions(self) -> List[Dict[str, Any]]:
        """Get information about all recorded sessions."""
        sessions = []
        if not self.base_dir.exists():
            return sessions

        for session_dir in self.base_dir.iterdir():
            if session_dir.is_dir() and session_dir.name.startswith("session_"):
                metadata_file = session_dir / "metadata.json"
                if metadata_file.exists():
                    try:
                        with open(metadata_file, "r") as f:
                            metadata = json.load(f)
                        sessions.append(metadata)
                    except Exception as e:
                        logger.error(f"Failed to read metadata for {session_dir}: {e}")

        return sorted(
            sessions, key=lambda x: x.get("session_start_time", ""), reverse=True
        )
