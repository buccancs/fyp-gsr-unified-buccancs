"""
DataEvent represents a common data format for events across platforms.
This class is used by the Windows application to ensure consistent data handling
with the Android application.
"""
import time
from typing import Dict, Any, Optional


class DataEvent:
    # Event type constants
    TYPE_GSR = "gsr"
    TYPE_RGB_FRAME = "rgb_frame"
    TYPE_THERMAL_FRAME = "thermal_frame"
    TYPE_AUDIO = "audio"
    TYPE_SYNC_MARKER = "sync_marker"
    
    def __init__(self, event_type: str, timestamp: Optional[int] = None, device_id: str = ""):
        """
        Initialize a new DataEvent.
        
        Args:
            event_type: The type of event (use class constants)
            timestamp: Timestamp in milliseconds (epoch time), defaults to current time if None
            device_id: Unique identifier for the source device
        """
        self.type = event_type
        self.timestamp = timestamp if timestamp is not None else int(time.time() * 1000)
        self.device_id = device_id
        self.data: Dict[str, Any] = {}
    
    def add_data(self, key: str, value: Any) -> None:
        """Add data to the event."""
        self.data[key] = value
    
    def get_value(self, key: str) -> Any:
        """Get a value from the event data."""
        return self.data.get(key)
    
    def __str__(self) -> str:
        """String representation of the event."""
        return (f"DataEvent(type='{self.type}', timestamp={self.timestamp}, "
                f"device_id='{self.device_id}', data={self.data})")
    
    def to_dict(self) -> Dict[str, Any]:
        """Convert the event to a dictionary for serialization."""
        return {
            "type": self.type,
            "timestamp": self.timestamp,
            "device_id": self.device_id,
            "data": self.data
        }
    
    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> 'DataEvent':
        """Create a DataEvent from a dictionary."""
        event = cls(data["type"], data["timestamp"], data["device_id"])
        event.data = data["data"]
        return event