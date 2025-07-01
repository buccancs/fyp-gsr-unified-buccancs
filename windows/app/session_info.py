"""
SessionInfo represents all the information about a recording session.
This class is used to track session metadata, devices, and files.
"""
from typing import Dict, List, Optional, Any
import time
from windows.app.file_naming_utils import generate_session_id

class FileInfo:
    """
    FileInfo represents information about a file in the session.
    """
    
    def __init__(self, file_path: str, file_type: str, format_: str):
        """
        Create a new FileInfo.
        
        Args:
            file_path: The file path
            file_type: The file type (e.g., "rgb", "thermal", "gsr")
            format_: The file format (e.g., "mp4", "csv")
        """
        self.file_path = file_path
        self.file_type = file_type
        self.format = format_
        self.duration_ms = 0
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        return {
            "file_path": self.file_path,
            "file_type": self.file_type,
            "format": self.format,
            "duration_ms": self.duration_ms
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'FileInfo':
        """Create from dictionary."""
        file_info = cls(data["file_path"], data["file_type"], data["format"])
        file_info.duration_ms = data.get("duration_ms", 0)
        return file_info


class DeviceInfo:
    """
    DeviceInfo represents information about a device in the session.
    """
    
    def __init__(self, device_id: str, device_type: str):
        """
        Create a new DeviceInfo.
        
        Args:
            device_id: The device ID
            device_type: The device type (e.g., "android", "pc")
        """
        self.device_id = device_id
        self.device_type = device_type
        self.time_offset_ms = 0
        self.files: List[FileInfo] = []
    
    def add_file(self, file: FileInfo) -> None:
        """Add a file to this device."""
        self.files.append(file)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        return {
            "device_id": self.device_id,
            "device_type": self.device_type,
            "time_offset_ms": self.time_offset_ms,
            "files": [f.to_dict() for f in self.files]
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'DeviceInfo':
        """Create from dictionary."""
        device_info = cls(data["device_id"], data["device_type"])
        device_info.time_offset_ms = data.get("time_offset_ms", 0)
        device_info.files = [FileInfo.from_dict(f) for f in data.get("files", [])]
        return device_info


class SyncEvent:
    """
    SyncEvent represents a synchronization event in the session.
    """
    
    def __init__(self, timestamp: int, event_type: str):
        """
        Create a new SyncEvent.
        
        Args:
            timestamp: The timestamp in milliseconds (epoch time)
            event_type: The event type (e.g., "start", "stop", "marker")
        """
        self.timestamp = timestamp
        self.type = event_type
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        return {
            "timestamp": self.timestamp,
            "type": self.type
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'SyncEvent':
        """Create from dictionary."""
        return cls(data["timestamp"], data["type"])


class SessionInfo:
    """
    SessionInfo represents all the information about a recording session.
    """
    
    def __init__(self, session_id: Optional[str] = None):
        """
        Create a new SessionInfo.
        
        Args:
            session_id: The session ID, or None to generate one
        """
        self.session_id = session_id if session_id else generate_session_id()
        self.start_time = 0
        self.end_time = 0
        self.devices: List[DeviceInfo] = []
        self.sync_events: List[SyncEvent] = []
        self.metadata: Dict[str, str] = {}
    
    def set_start_time(self, start_time: Optional[int] = None) -> None:
        """
        Set the session start time.
        
        Args:
            start_time: The start time in milliseconds (epoch time), or None for current time
        """
        self.start_time = start_time if start_time is not None else int(time.time() * 1000)
    
    def set_end_time(self, end_time: Optional[int] = None) -> None:
        """
        Set the session end time.
        
        Args:
            end_time: The end time in milliseconds (epoch time), or None for current time
        """
        self.end_time = end_time if end_time is not None else int(time.time() * 1000)
    
    def add_device(self, device: DeviceInfo) -> None:
        """Add a device to the session."""
        self.devices.append(device)
    
    def get_device(self, device_id: str) -> Optional[DeviceInfo]:
        """Get a device by ID."""
        for device in self.devices:
            if device.device_id == device_id:
                return device
        return None
    
    def add_sync_event(self, event: SyncEvent) -> None:
        """Add a sync event to the session."""
        self.sync_events.append(event)
    
    def set_metadata(self, key: str, value: str) -> None:
        """Set a metadata value."""
        self.metadata[key] = value
    
    def get_metadata(self, key: str) -> Optional[str]:
        """Get a metadata value."""
        return self.metadata.get(key)
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert to dictionary for serialization."""
        return {
            "session_id": self.session_id,
            "start_time": self.start_time,
            "end_time": self.end_time,
            "devices": [d.to_dict() for d in self.devices],
            "sync_events": [e.to_dict() for e in self.sync_events],
            "metadata": self.metadata
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'SessionInfo':
        """Create from dictionary."""
        session = cls(data["session_id"])
        session.start_time = data.get("start_time", 0)
        session.end_time = data.get("end_time", 0)
        session.devices = [DeviceInfo.from_dict(d) for d in data.get("devices", [])]
        session.sync_events = [SyncEvent.from_dict(e) for e in data.get("sync_events", [])]
        session.metadata = data.get("metadata", {})
        return session