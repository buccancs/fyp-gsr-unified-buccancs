"""
Utility module for aligning data from multiple devices.
This ensures that data from different sources can be merged for analysis.
"""
from typing import Dict, List, Optional, Any
from windows.app.session_info import SessionInfo, DeviceInfo, SyncEvent

def calculate_time_offsets(session: SessionInfo, reference_device_id: str) -> None:
    """
    Calculate time offsets for all devices in a session based on sync events.
    This updates the time_offset_ms field in each DeviceInfo.
    
    Args:
        session: The session info
        reference_device_id: The ID of the reference device (e.g., the PC)
    """
    sync_events = session.sync_events
    if not sync_events:
        return
    
    # Group sync events by type
    events_by_type: Dict[str, List[SyncEvent]] = {}
    for event in sync_events:
        event_type = event.type
        if event_type not in events_by_type:
            events_by_type[event_type] = []
        events_by_type[event_type].append(event)
    
    # Find the reference device
    reference_device = None
    for device in session.devices:
        if device.device_id == reference_device_id:
            reference_device = device
            break
    
    if reference_device is None:
        # If reference device not found, use the first device
        if session.devices:
            reference_device = session.devices[0]
        else:
            return
    
    # Set the reference device's offset to 0
    reference_device.time_offset_ms = 0
    
    # Calculate offsets for other devices
    for device in session.devices:
        if device == reference_device:
            continue
        
        # Use the "start" event if available
        if "start" in events_by_type:
            reference_start_time = find_event_time_for_device(events_by_type["start"], reference_device_id)
            device_start_time = find_event_time_for_device(events_by_type["start"], device.device_id)
            
            if reference_start_time > 0 and device_start_time > 0:
                offset = reference_start_time - device_start_time
                device.time_offset_ms = offset
                continue
        
        # If no start event, use the average of all sync events
        total_offset = 0
        count = 0
        
        for event_type, events in events_by_type.items():
            reference_time = find_event_time_for_device(events, reference_device_id)
            device_time = find_event_time_for_device(events, device.device_id)
            
            if reference_time > 0 and device_time > 0:
                total_offset += (reference_time - device_time)
                count += 1
        
        if count > 0:
            device.time_offset_ms = total_offset // count  # Integer division for consistency

def find_event_time_for_device(events: List[SyncEvent], device_id: str) -> int:
    """
    Find the timestamp of an event for a specific device.
    
    Args:
        events: The list of events
        device_id: The device ID
    
    Returns:
        The timestamp, or -1 if not found
    """
    for event in events:
        if device_id in event.type:
            return event.timestamp
    return -1

def convert_to_reference_time(local_timestamp: int, device_info: DeviceInfo) -> int:
    """
    Convert a timestamp from a device's local time to the reference timeline.
    
    Args:
        local_timestamp: The local timestamp
        device_info: The device info with the time offset
    
    Returns:
        The timestamp in the reference timeline
    """
    return local_timestamp + device_info.time_offset_ms

def generate_aligned_timestamps_csv(session: SessionInfo) -> str:
    """
    Generate a CSV file with aligned timestamps for all sync events.
    
    Args:
        session: The session info
    
    Returns:
        A CSV string with aligned timestamps
    """
    csv_lines = []
    
    # Add header
    header = ["event_type"]
    for device in session.devices:
        header.append(device.device_id)
    csv_lines.append(",".join(header))
    
    # Group sync events by type
    events_by_type: Dict[str, List[SyncEvent]] = {}
    for event in session.sync_events:
        event_type = event.type
        if event_type not in events_by_type:
            events_by_type[event_type] = []
        events_by_type[event_type].append(event)
    
    # Add rows for each event type
    for event_type, events in events_by_type.items():
        row = [event_type]
        
        for device in session.devices:
            timestamp = find_event_time_for_device(events, device.device_id)
            if timestamp > 0:
                # Convert to reference time
                timestamp = convert_to_reference_time(timestamp, device)
                row.append(str(timestamp))
            else:
                row.append("")
        
        csv_lines.append(",".join(row))
    
    return "\n".join(csv_lines)

def save_aligned_timestamps_csv(session: SessionInfo, file_path: str) -> None:
    """
    Save aligned timestamps to a CSV file.
    
    Args:
        session: The session info
        file_path: The path to save the file
    """
    csv_content = generate_aligned_timestamps_csv(session)
    with open(file_path, 'w') as f:
        f.write(csv_content)

def analyze_sync_quality(session: SessionInfo) -> Dict[str, Any]:
    """
    Analyze the quality of synchronization by calculating statistics on sync events.
    
    Args:
        session: The session info
    
    Returns:
        A dictionary with sync quality metrics
    """
    if not session.devices or len(session.devices) < 2:
        return {"error": "Not enough devices for sync analysis"}
    
    # Use the first device as reference
    reference_device = session.devices[0]
    
    # Calculate max drift and average drift for each device
    results = {
        "reference_device": reference_device.device_id,
        "devices": []
    }
    
    for device in session.devices[1:]:  # Skip reference device
        device_stats = {
            "device_id": device.device_id,
            "time_offset_ms": device.time_offset_ms,
            "sync_events": []
        }
        
        # Collect all sync events for this device
        device_events = []
        for event in session.sync_events:
            if device.device_id in event.type:
                device_events.append(event)
        
        # Calculate drift for each event
        if device_events:
            drifts = []
            for event in device_events:
                # Find corresponding reference event
                for ref_event in session.sync_events:
                    if reference_device.device_id in ref_event.type and event.type.split('_')[0] == ref_event.type.split('_')[0]:
                        # Calculate drift after applying offset
                        adjusted_time = convert_to_reference_time(event.timestamp, device)
                        drift = ref_event.timestamp - adjusted_time
                        drifts.append(drift)
                        device_stats["sync_events"].append({
                            "type": event.type,
                            "drift_ms": drift
                        })
                        break
            
            if drifts:
                device_stats["max_drift_ms"] = max(abs(d) for d in drifts)
                device_stats["avg_drift_ms"] = sum(abs(d) for d in drifts) / len(drifts)
                device_stats["drift_std_dev"] = (sum((d - device_stats["avg_drift_ms"])**2 for d in drifts) / len(drifts))**0.5
        
        results["devices"].append(device_stats)
    
    return results