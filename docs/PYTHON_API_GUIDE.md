# Python API Reference Guide

This guide provides comprehensive documentation for using the Python API to programmatically control Android devices through the PC controller. The API allows you to automate recording sessions, manage multiple devices, and integrate the system with other research tools.

## 📋 Table of Contents

1. [API Overview](#api-overview)
2. [Getting Started](#getting-started)
3. [Core Classes](#core-classes)
4. [Device Management](#device-management)
5. [Recording Control](#recording-control)
6. [Session Management](#session-management)
7. [Status Monitoring](#status-monitoring)
8. [Event Handling](#event-handling)
9. [Integration Examples](#integration-examples)
10. [Advanced Usage](#advanced-usage)
11. [Error Handling](#error-handling)
12. [API Reference](#api-reference)

## 🔍 API Overview

The Python API is built on PySide6 and provides both GUI and programmatic interfaces for controlling the GSR & Dual-Video Recording System. The main components include:

- **DeviceManager**: Discover, connect, and manage Android devices
- **Device**: Individual device control and communication
- **SessionManager**: Handle recording sessions and metadata
- **MainWindow**: GUI interface with programmatic access
- **Event System**: Real-time notifications and callbacks

## 🚀 Getting Started

### Basic Setup

```python
import sys
import os
from PySide6.QtWidgets import QApplication
from PySide6.QtCore import QObject, Signal, Slot

# Add the source directory to Python path
sys.path.append('path/to/windows_controller/src')

from network.device_manager import DeviceManager
from utils.session_manager import SessionManager
from utils.logger import setup_logger

# Initialize logging
setup_logger()

# Create Qt Application (required for PySide6)
app = QApplication(sys.argv)
```

### Simple Recording Example

```python
from network.device_manager import DeviceManager
import time

# Create device manager
device_manager = DeviceManager()

# Discover and connect to devices
device_manager.discover_devices()
time.sleep(2)  # Wait for discovery

# Connect all discovered devices
device_manager.connect_all_devices()
time.sleep(1)  # Wait for connections

# Start recording
session_id = "experiment_001"
device_manager.start_recording(session_id)

# Record for 30 seconds
time.sleep(30)

# Stop recording
device_manager.stop_recording()

# Cleanup
device_manager.cleanup()
```

## 🏗️ Core Classes

### DeviceManager

The main class for managing multiple Android devices.

```python
from network.device_manager import DeviceManager

class DeviceManager(QObject):
    """
    Manages discovery, connection, and control of multiple Android devices.
    """
    
    # Signals
    device_discovered = Signal(object)  # Emitted when device found
    device_connected = Signal(object)   # Emitted when device connects
    device_disconnected = Signal(object) # Emitted when device disconnects
    recording_started = Signal(str)     # Emitted when recording starts
    recording_stopped = Signal()       # Emitted when recording stops
```

### Device

Represents an individual Android device.

```python
from network.device import Device

class Device(QObject):
    """
    Represents a single Android capture device.
    """
    
    # Signals
    connected = Signal()
    disconnected = Signal()
    status_updated = Signal(dict)
    error_occurred = Signal(str)
```

### SessionManager

Handles recording sessions and metadata.

```python
from utils.session_manager import SessionManager

class SessionManager:
    """
    Manages recording sessions, metadata, and file organization.
    """
    
    def create_session(self, session_id=None):
        """Create a new recording session."""
        
    def get_session_info(self, session_id):
        """Get information about a session."""
        
    def generate_manifest(self, session_id):
        """Generate session manifest with metadata."""
```

## 📱 Device Management

### Discovering Devices

```python
# Automatic discovery using Zeroconf/mDNS
device_manager = DeviceManager()

# Set up discovery callback
@device_manager.device_discovered.connect
def on_device_discovered(device):
    print(f"Found device: {device.name} at {device.address}")

# Start discovery
device_manager.discover_devices()

# Manual device addition (if automatic discovery fails)
manual_device = Device(
    id="phone_001",
    name="Samsung Galaxy S21",
    address="192.168.1.150",
    port=8080,
    device_type="phone"
)
device_manager.add_device(manual_device)
```

### Connecting to Devices

```python
# Connect to specific device
device_id = "phone_001"
success = device_manager.connect_device(device_id)
if success:
    print(f"Connected to {device_id}")
else:
    print(f"Failed to connect to {device_id}")

# Connect to all discovered devices
device_manager.connect_all_devices()

# Check connection status
for device_id, device in device_manager.devices.items():
    if device.is_connected():
        print(f"{device_id}: Connected")
    else:
        print(f"{device_id}: Disconnected")
```

### Device Information

```python
# Get device status
device = device_manager.get_device("phone_001")
status = device.get_status()

print(f"Battery: {status.get('battery', 'Unknown')}")
print(f"Storage: {status.get('storage', 'Unknown')}")
print(f"GSR Sensor: {status.get('gsr_connected', False)}")
print(f"Thermal Camera: {status.get('thermal_connected', False)}")

# Get all device statuses
all_statuses = device_manager.get_all_device_statuses()
for device_id, status in all_statuses.items():
    print(f"{device_id}: {status}")
```

## 🎬 Recording Control

### Basic Recording

```python
# Start recording on all connected devices
session_id = "experiment_20241201_001"
success = device_manager.start_recording(session_id)

if success:
    print(f"Recording started with session ID: {session_id}")
    
    # Monitor recording status
    time.sleep(1)
    for device_id in device_manager.devices:
        device = device_manager.get_device(device_id)
        if device.is_recording():
            print(f"{device_id}: Recording active")
    
    # Record for specified duration
    recording_duration = 60  # seconds
    time.sleep(recording_duration)
    
    # Stop recording
    device_manager.stop_recording()
    print("Recording stopped")
else:
    print("Failed to start recording")
```

### Advanced Recording Control

```python
# Start recording with custom parameters
recording_params = {
    "session_id": "custom_session_001",
    "duration": 120,  # seconds
    "video_quality": "1080p",
    "gsr_sample_rate": 128,  # Hz
    "thermal_fps": 30
}

# Start recording with parameters
device_manager.start_recording(
    session_id=recording_params["session_id"],
    parameters=recording_params
)

# Monitor recording progress
start_time = time.time()
while device_manager.is_recording():
    elapsed = time.time() - start_time
    remaining = recording_params["duration"] - elapsed
    
    if remaining <= 0:
        device_manager.stop_recording()
        break
    
    print(f"Recording... {remaining:.1f}s remaining")
    time.sleep(1)
```

### Individual Device Control

```python
# Control specific device
device = device_manager.get_device("phone_001")

# Send custom command to device
command_data = {
    "action": "start_recording",
    "session_id": "individual_test",
    "modalities": ["rgb", "thermal", "gsr"]
}

response = device.send_command("CUSTOM_RECORD", command_data)
if response.get("status") == "success":
    print("Custom recording started")

# Stop recording on specific device
stop_response = device.send_command("STOP_RECORDING")
print(f"Stop response: {stop_response}")
```

## 📊 Session Management

### Creating Sessions

```python
from utils.session_manager import SessionManager

session_manager = SessionManager()

# Create new session
session_info = session_manager.create_session(
    session_id="experiment_001",
    participant_id="P001",
    experiment_type="emotion_recognition",
    notes="Baseline recording session"
)

print(f"Created session: {session_info.session_id}")
print(f"Session directory: {session_info.session_dir}")
```

### Session Metadata

```python
# Add metadata to session
session_manager.add_metadata(
    session_id="experiment_001",
    metadata={
        "participant_age": 25,
        "participant_gender": "F",
        "experimental_condition": "happy_videos",
        "room_temperature": 22.5,
        "humidity": 45
    }
)

# Get session information
session_info = session_manager.get_session_info("experiment_001")
print(f"Session metadata: {session_info.metadata}")
```

### Session Manifest Generation

```python
# Generate comprehensive session manifest
manifest = session_manager.generate_manifest("experiment_001")

print("Session Manifest:")
print(f"Session ID: {manifest['session_id']}")
print(f"Start Time: {manifest['start_time']}")
print(f"Duration: {manifest['duration']}")
print(f"Devices: {len(manifest['devices'])}")

for device_info in manifest['devices']:
    print(f"  Device: {device_info['id']}")
    print(f"    Files: {len(device_info['files'])}")
    for file_info in device_info['files']:
        print(f"      {file_info['type']}: {file_info['path']}")
```

## 📈 Status Monitoring

### Real-time Status Updates

```python
class StatusMonitor(QObject):
    def __init__(self, device_manager):
        super().__init__()
        self.device_manager = device_manager
        
        # Connect to device status signals
        for device in device_manager.devices.values():
            device.status_updated.connect(self.on_status_update)
    
    @Slot(dict)
    def on_status_update(self, status):
        device_id = status.get('device_id')
        print(f"Status update from {device_id}:")
        print(f"  Battery: {status.get('battery')}")
        print(f"  Storage: {status.get('storage')}")
        print(f"  GSR: {status.get('gsr_value')} μS")
        print(f"  Heart Rate: {status.get('heart_rate')} BPM")

# Create and use status monitor
monitor = StatusMonitor(device_manager)
```

### Periodic Status Polling

```python
import threading

class PeriodicStatusChecker:
    def __init__(self, device_manager, interval=5):
        self.device_manager = device_manager
        self.interval = interval
        self.running = False
        self.thread = None
    
    def start(self):
        self.running = True
        self.thread = threading.Thread(target=self._check_status)
        self.thread.start()
    
    def stop(self):
        self.running = False
        if self.thread:
            self.thread.join()
    
    def _check_status(self):
        while self.running:
            statuses = self.device_manager.get_all_device_statuses()
            
            for device_id, status in statuses.items():
                # Check for low battery
                battery = status.get('battery_level', 100)
                if battery < 20:
                    print(f"WARNING: {device_id} battery low: {battery}%")
                
                # Check for low storage
                storage = status.get('storage_remaining', '100GB')
                if 'MB' in storage or (storage.startswith('0.') and 'GB' in storage):
                    print(f"WARNING: {device_id} storage low: {storage}")
            
            time.sleep(self.interval)

# Use periodic status checker
status_checker = PeriodicStatusChecker(device_manager, interval=10)
status_checker.start()

# ... do recording work ...

status_checker.stop()
```

## 🔔 Event Handling

### Setting Up Event Callbacks

```python
class RecordingEventHandler(QObject):
    def __init__(self):
        super().__init__()
    
    @Slot(object)
    def on_device_connected(self, device):
        print(f"Device connected: {device.name}")
        
        # Automatically check device capabilities
        status = device.get_status()
        capabilities = []
        
        if status.get('gsr_connected'):
            capabilities.append("GSR")
        if status.get('thermal_connected'):
            capabilities.append("Thermal")
        if status.get('rgb_available'):
            capabilities.append("RGB")
        
        print(f"Device capabilities: {', '.join(capabilities)}")
    
    @Slot(object)
    def on_device_disconnected(self, device):
        print(f"Device disconnected: {device.name}")
    
    @Slot(str)
    def on_recording_started(self, session_id):
        print(f"Recording started: {session_id}")
        
        # Log recording start time
        with open(f"recording_log_{session_id}.txt", "w") as f:
            f.write(f"Recording started at {time.ctime()}\n")
    
    @Slot()
    def on_recording_stopped(self):
        print("Recording stopped")

# Connect event handler
event_handler = RecordingEventHandler()
device_manager.device_connected.connect(event_handler.on_device_connected)
device_manager.device_disconnected.connect(event_handler.on_device_disconnected)
device_manager.recording_started.connect(event_handler.on_recording_started)
device_manager.recording_stopped.connect(event_handler.on_recording_stopped)
```

## 🔧 Integration Examples

### PsychoPy Integration

```python
from psychopy import visual, core, event
import threading

class PsychoPyGSRExperiment:
    def __init__(self, device_manager):
        self.device_manager = device_manager
        self.win = visual.Window(size=(800, 600), fullscr=False)
        self.session_id = f"psychopy_exp_{int(time.time())}"
    
    def run_experiment(self):
        # Start recording
        self.device_manager.start_recording(self.session_id)
        
        # Show instructions
        instructions = visual.TextStim(
            self.win, 
            text="Press SPACE to start the experiment"
        )
        instructions.draw()
        self.win.flip()
        
        # Wait for space key
        event.waitKeys(keyList=['space'])
        
        # Run experimental trials
        for trial in range(5):
            # Show stimulus
            stimulus = visual.TextStim(
                self.win,
                text=f"Trial {trial + 1}\nLook at the center",
                height=0.1
            )
            stimulus.draw()
            self.win.flip()
            
            # Record for 10 seconds per trial
            core.wait(10)
            
            # Inter-trial interval
            self.win.flip()  # Clear screen
            core.wait(2)
        
        # Stop recording
        self.device_manager.stop_recording()
        
        # Show completion message
        completion = visual.TextStim(
            self.win,
            text="Experiment complete!\nThank you for participating."
        )
        completion.draw()
        self.win.flip()
        core.wait(3)
        
        self.win.close()

# Run PsychoPy experiment
experiment = PsychoPyGSRExperiment(device_manager)
experiment.run_experiment()
```

### Lab Streaming Layer (LSL) Integration

```python
try:
    import pylsl
    LSL_AVAILABLE = True
except ImportError:
    LSL_AVAILABLE = False
    print("LSL not available. Install with: pip install pylsl")

class LSLIntegration:
    def __init__(self, device_manager):
        self.device_manager = device_manager
        self.streams = {}
        
        if LSL_AVAILABLE:
            self.setup_lsl_streams()
    
    def setup_lsl_streams(self):
        # Create LSL stream for GSR data
        gsr_info = pylsl.StreamInfo(
            'GSR_Data', 'GSR', 1, 128, 'float32', 'gsr_stream_001'
        )
        self.streams['gsr'] = pylsl.StreamOutlet(gsr_info)
        
        # Create LSL stream for markers
        marker_info = pylsl.StreamInfo(
            'Markers', 'Markers', 1, 0, 'string', 'marker_stream_001'
        )
        self.streams['markers'] = pylsl.StreamOutlet(marker_info)
    
    def send_marker(self, marker):
        if 'markers' in self.streams:
            self.streams['markers'].push_sample([marker])
            print(f"LSL Marker sent: {marker}")
    
    def start_gsr_streaming(self):
        # This would integrate with the actual GSR data stream
        # For demonstration, we'll simulate data
        def stream_gsr_data():
            while self.device_manager.is_recording():
                # Get real GSR data from devices
                for device in self.device_manager.devices.values():
                    status = device.get_status()
                    gsr_value = status.get('gsr_value', 0.0)
                    
                    if 'gsr' in self.streams:
                        self.streams['gsr'].push_sample([gsr_value])
                
                time.sleep(1/128)  # 128 Hz sampling rate
        
        threading.Thread(target=stream_gsr_data, daemon=True).start()

# Use LSL integration
if LSL_AVAILABLE:
    lsl_integration = LSLIntegration(device_manager)
    
    # Start recording with LSL
    device_manager.start_recording("lsl_experiment_001")
    lsl_integration.start_gsr_streaming()
    
    # Send experiment markers
    lsl_integration.send_marker("experiment_start")
    time.sleep(10)
    lsl_integration.send_marker("stimulus_presentation")
    time.sleep(30)
    lsl_integration.send_marker("experiment_end")
    
    device_manager.stop_recording()
```

### Custom Data Analysis Integration

```python
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

class DataAnalyzer:
    def __init__(self, session_manager):
        self.session_manager = session_manager
    
    def analyze_session(self, session_id):
        """Analyze data from a completed session."""
        session_info = self.session_manager.get_session_info(session_id)
        
        results = {
            'session_id': session_id,
            'gsr_analysis': {},
            'video_analysis': {},
            'synchronization_quality': {}
        }
        
        # Analyze GSR data
        gsr_files = self._find_files_by_type(session_info, 'gsr')
        for device_id, gsr_file in gsr_files.items():
            gsr_data = pd.read_csv(gsr_file)
            
            results['gsr_analysis'][device_id] = {
                'mean_gsr': gsr_data['gsr_value'].mean(),
                'std_gsr': gsr_data['gsr_value'].std(),
                'max_gsr': gsr_data['gsr_value'].max(),
                'min_gsr': gsr_data['gsr_value'].min(),
                'samples': len(gsr_data)
            }
        
        # Analyze video files
        video_files = self._find_files_by_type(session_info, 'video')
        for device_id, video_file in video_files.items():
            # Basic video analysis (would require OpenCV)
            results['video_analysis'][device_id] = {
                'file_path': video_file,
                'file_size': os.path.getsize(video_file),
                'estimated_duration': self._estimate_video_duration(video_file)
            }
        
        return results
    
    def _find_files_by_type(self, session_info, file_type):
        """Find files of specific type in session."""
        files = {}
        for device_info in session_info.devices:
            for file_info in device_info.get('files', []):
                if file_info.get('type') == file_type:
                    files[device_info['id']] = file_info['path']
        return files
    
    def _estimate_video_duration(self, video_file):
        """Estimate video duration (placeholder implementation)."""
        # In real implementation, would use OpenCV or similar
        return "Unknown"
    
    def generate_report(self, session_id):
        """Generate analysis report."""
        results = self.analyze_session(session_id)
        
        report = f"""
# Analysis Report for Session {session_id}

## GSR Analysis
"""
        for device_id, gsr_stats in results['gsr_analysis'].items():
            report += f"""
### Device: {device_id}
- Mean GSR: {gsr_stats['mean_gsr']:.3f} μS
- Standard Deviation: {gsr_stats['std_gsr']:.3f} μS
- Range: {gsr_stats['min_gsr']:.3f} - {gsr_stats['max_gsr']:.3f} μS
- Total Samples: {gsr_stats['samples']}
"""
        
        report += "\n## Video Analysis\n"
        for device_id, video_stats in results['video_analysis'].items():
            report += f"""
### Device: {device_id}
- File Size: {video_stats['file_size'] / (1024*1024):.1f} MB
- Estimated Duration: {video_stats['estimated_duration']}
"""
        
        return report

# Use data analyzer
analyzer = DataAnalyzer(session_manager)
report = analyzer.generate_report("experiment_001")
print(report)
```

## 🔧 Advanced Usage

### Custom Device Commands

```python
# Define custom command protocol
class CustomCommands:
    @staticmethod
    def set_video_quality(device, quality):
        """Set video recording quality."""
        command_data = {
            "parameter": "video_quality",
            "value": quality  # "720p", "1080p", "4K"
        }
        return device.send_command("SET_PARAMETER", command_data)
    
    @staticmethod
    def set_gsr_sample_rate(device, sample_rate):
        """Set GSR sampling rate."""
        command_data = {
            "parameter": "gsr_sample_rate",
            "value": sample_rate  # Hz
        }
        return device.send_command("SET_PARAMETER", command_data)
    
    @staticmethod
    def calibrate_sensors(device):
        """Calibrate device sensors."""
        return device.send_command("CALIBRATE_SENSORS")

# Use custom commands
device = device_manager.get_device("phone_001")
CustomCommands.set_video_quality(device, "1080p")
CustomCommands.set_gsr_sample_rate(device, 256)
CustomCommands.calibrate_sensors(device)
```

### Batch Operations

```python
class BatchOperations:
    def __init__(self, device_manager):
        self.device_manager = device_manager
    
    def configure_all_devices(self, config):
        """Apply configuration to all devices."""
        results = {}
        
        for device_id, device in self.device_manager.devices.items():
            try:
                # Apply video settings
                if 'video_quality' in config:
                    CustomCommands.set_video_quality(
                        device, config['video_quality']
                    )
                
                # Apply GSR settings
                if 'gsr_sample_rate' in config:
                    CustomCommands.set_gsr_sample_rate(
                        device, config['gsr_sample_rate']
                    )
                
                results[device_id] = "Success"
                
            except Exception as e:
                results[device_id] = f"Error: {str(e)}"
        
        return results
    
    def run_multiple_sessions(self, session_configs):
        """Run multiple recording sessions in sequence."""
        results = []
        
        for i, config in enumerate(session_configs):
            session_id = config.get('session_id', f"batch_session_{i}")
            duration = config.get('duration', 60)
            
            print(f"Starting session {session_id}")
            
            # Start recording
            success = self.device_manager.start_recording(session_id)
            if not success:
                results.append({
                    'session_id': session_id,
                    'status': 'Failed to start'
                })
                continue
            
            # Record for specified duration
            time.sleep(duration)
            
            # Stop recording
            self.device_manager.stop_recording()
            
            results.append({
                'session_id': session_id,
                'status': 'Completed',
                'duration': duration
            })
            
            # Wait between sessions
            inter_session_delay = config.get('delay', 10)
            time.sleep(inter_session_delay)
        
        return results

# Use batch operations
batch_ops = BatchOperations(device_manager)

# Configure all devices
config = {
    'video_quality': '1080p',
    'gsr_sample_rate': 128
}
config_results = batch_ops.configure_all_devices(config)
print(f"Configuration results: {config_results}")

# Run multiple sessions
session_configs = [
    {'session_id': 'baseline', 'duration': 120, 'delay': 30},
    {'session_id': 'stimulus_1', 'duration': 180, 'delay': 30},
    {'session_id': 'stimulus_2', 'duration': 180, 'delay': 30},
    {'session_id': 'recovery', 'duration': 120, 'delay': 0}
]

session_results = batch_ops.run_multiple_sessions(session_configs)
for result in session_results:
    print(f"Session {result['session_id']}: {result['status']}")
```

## ⚠️ Error Handling

### Robust Error Handling

```python
class RobustRecordingManager:
    def __init__(self, device_manager):
        self.device_manager = device_manager
        self.error_log = []
    
    def safe_start_recording(self, session_id, max_retries=3):
        """Start recording with error handling and retries."""
        for attempt in range(max_retries):
            try:
                # Check device readiness
                if not self._check_device_readiness():
                    raise Exception("Devices not ready")
                
                # Start recording
                success = self.device_manager.start_recording(session_id)
                if success:
                    print(f"Recording started successfully: {session_id}")
                    return True
                else:
                    raise Exception("Failed to start recording")
                    
            except Exception as e:
                error_msg = f"Attempt {attempt + 1} failed: {str(e)}"
                self.error_log.append(error_msg)
                print(error_msg)
                
                if attempt < max_retries - 1:
                    print(f"Retrying in 5 seconds...")
                    time.sleep(5)
                    
                    # Try to recover
                    self._attempt_recovery()
        
        print(f"Failed to start recording after {max_retries} attempts")
        return False
    
    def _check_device_readiness(self):
        """Check if all devices are ready for recording."""
        for device_id, device in self.device_manager.devices.items():
            if not device.is_connected():
                print(f"Device {device_id} not connected")
                return False
            
            status = device.get_status()
            
            # Check battery level
            battery = status.get('battery_level', 0)
            if battery < 15:
                print(f"Device {device_id} battery too low: {battery}%")
                return False
            
            # Check storage
            storage = status.get('storage_remaining', '0MB')
            if 'MB' in storage and int(storage.split('MB')[0]) < 500:
                print(f"Device {device_id} storage too low: {storage}")
                return False
        
        return True
    
    def _attempt_recovery(self):
        """Attempt to recover from errors."""
        print("Attempting recovery...")
        
        # Disconnect and reconnect all devices
        self.device_manager.disconnect_all_devices()
        time.sleep(2)
        self.device_manager.connect_all_devices()
        time.sleep(3)
    
    def safe_stop_recording(self):
        """Stop recording with error handling."""
        try:
            self.device_manager.stop_recording()
            print("Recording stopped successfully")
            return True
        except Exception as e:
            error_msg = f"Error stopping recording: {str(e)}"
            self.error_log.append(error_msg)
            print(error_msg)
            return False
    
    def get_error_log(self):
        """Get list of errors that occurred."""
        return self.error_log.copy()

# Use robust recording manager
robust_manager = RobustRecordingManager(device_manager)

# Start recording with error handling
if robust_manager.safe_start_recording("robust_session_001"):
    time.sleep(60)  # Record for 1 minute
    robust_manager.safe_stop_recording()

# Check for any errors
errors = robust_manager.get_error_log()
if errors:
    print("Errors occurred:")
    for error in errors:
        print(f"  - {error}")
```

## 📚 API Reference

### DeviceManager Methods

```python
class DeviceManager:
    def discover_devices() -> None:
        """Start device discovery process."""
    
    def connect_device(device_id: str) -> bool:
        """Connect to specific device."""
    
    def disconnect_device(device_id: str) -> bool:
        """Disconnect from specific device."""
    
    def connect_all_devices() -> dict:
        """Connect to all discovered devices."""
    
    def disconnect_all_devices() -> dict:
        """Disconnect from all devices."""
    
    def start_recording(session_id: str, parameters: dict = None) -> bool:
        """Start recording on all connected devices."""
    
    def stop_recording() -> bool:
        """Stop recording on all devices."""
    
    def get_device(device_id: str) -> Device:
        """Get device object by ID."""
    
    def get_all_device_statuses() -> dict:
        """Get status of all devices."""
    
    def is_recording() -> bool:
        """Check if any device is recording."""
    
    def cleanup() -> None:
        """Clean up resources and disconnect all devices."""
```

### Device Methods

```python
class Device:
    def connect() -> bool:
        """Connect to the device."""
    
    def disconnect() -> bool:
        """Disconnect from the device."""
    
    def is_connected() -> bool:
        """Check if device is connected."""
    
    def send_command(command: str, data: dict = None, timeout: float = 5.0) -> dict:
        """Send command to device."""
    
    def start_recording(session_id: str) -> bool:
        """Start recording on this device."""
    
    def stop_recording() -> bool:
        """Stop recording on this device."""
    
    def is_recording() -> bool:
        """Check if device is recording."""
    
    def get_status() -> dict:
        """Get current device status."""
    
    def collect_files(destination_dir: str) -> bool:
        """Collect recorded files from device."""
```

### SessionManager Methods

```python
class SessionManager:
    def create_session(session_id: str = None, **kwargs) -> SessionInfo:
        """Create new recording session."""
    
    def get_session_info(session_id: str) -> SessionInfo:
        """Get information about existing session."""
    
    def add_metadata(session_id: str, metadata: dict) -> None:
        """Add metadata to session."""
    
    def generate_manifest(session_id: str) -> dict:
        """Generate session manifest."""
    
    def list_sessions() -> list:
        """List all available sessions."""
    
    def delete_session(session_id: str) -> bool:
        """Delete session and associated files."""
```

## 🎯 Best Practices

1. **Always use Qt Application context** when using the API programmatically
2. **Handle errors gracefully** with try-catch blocks and retries
3. **Check device status** before starting recording sessions
4. **Use proper cleanup** to disconnect devices and free resources
5. **Monitor system resources** during long recording sessions
6. **Implement logging** for debugging and audit trails
7. **Test with single device** before scaling to multiple devices
8. **Use session management** for organized data collection

## 📞 Support

For additional help with the Python API:

- Check the source code in `windows_controller/src/`
- Review test files in `windows_controller/tests/`
- Create issues on the GitHub repository
- Refer to PySide6 documentation for GUI-related questions

---

**Note**: This API is designed for research applications and requires proper hardware setup. Always test your code with the actual hardware before running experiments.