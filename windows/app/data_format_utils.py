"""
Utility module for defining standardized data formats across platforms.
This ensures that all data is stored and formatted consistently,
making it easier to parse and analyze.
"""
import json

# Video format constants
VIDEO_CONTAINER_FORMAT = "mp4"
VIDEO_CODEC = "h264"
DEFAULT_VIDEO_WIDTH = 1920
DEFAULT_VIDEO_HEIGHT = 1080
DEFAULT_VIDEO_FPS = 30
DEFAULT_THERMAL_WIDTH = 384
DEFAULT_THERMAL_HEIGHT = 288
DEFAULT_THERMAL_FPS = 25

# Audio format constants
AUDIO_FORMAT = "wav"
DEFAULT_AUDIO_SAMPLE_RATE = 44100
DEFAULT_AUDIO_CHANNELS = 2

# GSR data format constants
GSR_DATA_FORMAT = "csv"
DEFAULT_GSR_SAMPLE_RATE = 128

# Timestamp format constants
TIMESTAMP_FORMAT = "unix_epoch_ms"

# Manifest format constants
MANIFEST_FORMAT = "json"

def get_file_extension(data_type):
    """
    Get the file extension for a specific data type.
    
    Args:
        data_type: The type of data (e.g., "video", "audio", "gsr")
    
    Returns:
        The file extension for the data type
    """
    if data_type in ["rgb_video", "thermal_video"]:
        return VIDEO_CONTAINER_FORMAT
    elif data_type == "audio":
        return AUDIO_FORMAT
    elif data_type == "gsr":
        return GSR_DATA_FORMAT
    elif data_type == "manifest":
        return MANIFEST_FORMAT
    else:
        return "dat"

def get_gsr_csv_header():
    """
    Get the CSV header for GSR data.
    
    Returns:
        The CSV header for GSR data
    """
    return "timestamp_ms,gsr_microsiemens,ppg_raw,heart_rate_bpm"

def format_gsr_csv_line(timestamp, gsr_value, ppg_value, heart_rate):
    """
    Format a GSR data point as a CSV line.
    
    Args:
        timestamp: The timestamp in milliseconds
        gsr_value: The GSR value in microsiemens
        ppg_value: The PPG raw value
        heart_rate: The heart rate in BPM (can be -1 if not available)
    
    Returns:
        A CSV line with the GSR data
    """
    return f"{timestamp},{gsr_value:.2f},{ppg_value},{-1.0 if heart_rate < 0 else heart_rate:.1f}"

def get_manifest_template():
    """
    Get the JSON schema for the session manifest.
    
    Returns:
        A template for the session manifest as a dictionary
    """
    return {
        "session_id": "",
        "start_time": 0,
        "end_time": 0,
        "devices": [
            {
                "device_id": "",
                "device_type": "",
                "time_offset_ms": 0,
                "files": [
                    {
                        "file_path": "",
                        "file_type": "",
                        "format": "",
                        "duration_ms": 0
                    }
                ]
            }
        ],
        "sync_events": [
            {
                "timestamp": 0,
                "type": "start"
            }
        ],
        "metadata": {
            "experiment_name": "",
            "participant_id": "",
            "notes": ""
        }
    }

def create_empty_manifest(session_id):
    """
    Create an empty manifest for a new session.
    
    Args:
        session_id: The session ID
    
    Returns:
        A new manifest dictionary with the session ID set
    """
    manifest = get_manifest_template()
    manifest["session_id"] = session_id
    return manifest

def save_manifest(manifest, file_path):
    """
    Save a manifest to a JSON file.
    
    Args:
        manifest: The manifest dictionary
        file_path: The path to save the file
    """
    with open(file_path, 'w') as f:
        json.dump(manifest, f, indent=2)

def load_manifest(file_path):
    """
    Load a manifest from a JSON file.
    
    Args:
        file_path: The path to the manifest file
    
    Returns:
        The manifest dictionary
    """
    with open(file_path, 'r') as f:
        return json.load(f)