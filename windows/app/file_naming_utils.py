"""
Utility module for generating consistent file names across platforms.
This ensures that all output files follow the same naming convention,
making it easier to correlate data from multiple devices.
"""
import os
import datetime

# File type constants
TYPE_RGB_VIDEO = "rgb"
TYPE_THERMAL_VIDEO = "thermal"
TYPE_GSR_DATA = "gsr"
TYPE_AUDIO = "audio"
TYPE_MANIFEST = "manifest"

def generate_session_id():
    """
    Generate a unique session ID based on the current timestamp.
    
    Returns:
        A unique session ID in the format "Session_YYYYMMDD_HHMMSS"
    """
    timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    return f"Session_{timestamp}"

def generate_file_name(session_id, device_id, file_type, extension):
    """
    Generate a file name for a data file.
    
    Args:
        session_id: The session ID
        device_id: The device ID
        file_type: The type of file (use module constants)
        extension: The file extension (e.g., "mp4", "csv")
    
    Returns:
        A file name in the format "SessionID_DeviceID_FileType.Extension"
    """
    return f"{session_id}_{device_id}_{file_type}.{extension}"

def generate_session_directory(base_dir, session_id):
    """
    Generate a directory path for a session.
    
    Args:
        base_dir: The base directory
        session_id: The session ID
    
    Returns:
        A directory path in the format "BaseDir/SessionID"
    """
    return os.path.join(base_dir, session_id)

def generate_manifest_file_name(session_id):
    """
    Generate a manifest file name for a session.
    
    Args:
        session_id: The session ID
    
    Returns:
        A file name in the format "SessionID_info_manifest.json"
    """
    return generate_file_name(session_id, "info", TYPE_MANIFEST, "json")

def ensure_session_directory(base_dir, session_id):
    """
    Ensure that the session directory exists, creating it if necessary.
    
    Args:
        base_dir: The base directory
        session_id: The session ID
    
    Returns:
        The full path to the session directory
    """
    session_dir = generate_session_directory(base_dir, session_id)
    os.makedirs(session_dir, exist_ok=True)
    return session_dir