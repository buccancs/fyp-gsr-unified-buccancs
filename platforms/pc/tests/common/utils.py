"""Common test utility functions.

This module provides utility functions that can be shared across different test modules.
"""

import os
import sys
import tempfile
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Union


def setup_test_environment() -> Dict[str, str]:
    """Set up a clean test environment with temporary directories."""
    temp_dir = tempfile.mkdtemp(prefix='pc_tests_')
    
    test_env = {
        'temp_dir': temp_dir,
        'data_dir': os.path.join(temp_dir, 'data'),
        'config_dir': os.path.join(temp_dir, 'config'),
        'logs_dir': os.path.join(temp_dir, 'logs'),
        'output_dir': os.path.join(temp_dir, 'output')
    }
    
    # Create directories
    for dir_path in test_env.values():
        if dir_path != temp_dir:  # temp_dir already exists
            os.makedirs(dir_path, exist_ok=True)
    
    return test_env


def cleanup_test_environment(test_env: Dict[str, str]) -> None:
    """Clean up test environment by removing temporary directories."""
    import shutil
    temp_dir = test_env.get('temp_dir')
    if temp_dir and os.path.exists(temp_dir):
        shutil.rmtree(temp_dir)


def add_src_to_path() -> None:
    """Add the src directory to Python path for imports."""
    # Get the path to the src directory relative to the tests directory
    tests_dir = Path(__file__).parent.parent
    src_dir = tests_dir.parent / 'src'
    
    if src_dir.exists():
        sys.path.insert(0, str(src_dir))


def create_test_config_file(config_path: str, config_data: Dict[str, Any]) -> None:
    """Create a test configuration file."""
    import configparser
    
    config = configparser.ConfigParser()
    
    for section_name, section_data in config_data.items():
        config.add_section(section_name)
        for key, value in section_data.items():
            config.set(section_name, key, str(value))
    
    with open(config_path, 'w') as config_file:
        config.write(config_file)


def wait_for_condition(condition_func, timeout: float = 5.0, interval: float = 0.1) -> bool:
    """Wait for a condition to become true within a timeout period."""
    start_time = time.time()
    
    while time.time() - start_time < timeout:
        if condition_func():
            return True
        time.sleep(interval)
    
    return False


def assert_files_exist(file_paths: List[str]) -> None:
    """Assert that all specified files exist."""
    missing_files = []
    for file_path in file_paths:
        if not os.path.exists(file_path):
            missing_files.append(file_path)
    
    if missing_files:
        raise AssertionError(f"Missing files: {missing_files}")


def assert_directories_exist(dir_paths: List[str]) -> None:
    """Assert that all specified directories exist."""
    missing_dirs = []
    for dir_path in dir_paths:
        if not os.path.isdir(dir_path):
            missing_dirs.append(dir_path)
    
    if missing_dirs:
        raise AssertionError(f"Missing directories: {missing_dirs}")


def get_test_data_path(filename: str) -> str:
    """Get the full path to a test data file."""
    tests_dir = Path(__file__).parent.parent
    test_data_dir = tests_dir / 'test_data'
    return str(test_data_dir / filename)


def create_mock_csv_data(filename: str, num_rows: int = 100) -> str:
    """Create a mock CSV file with test data."""
    import csv
    
    with open(filename, 'w', newline='') as csvfile:
        writer = csv.writer(csvfile)
        
        # Write header
        writer.writerow(['timestamp', 'accel_x', 'accel_y', 'accel_z', 'gyro_x', 'gyro_y', 'gyro_z', 'gsr'])
        
        # Write data rows
        base_time = time.time()
        for i in range(num_rows):
            row = [
                base_time + i * 0.01,  # timestamp
                0.1 * i,               # accel_x
                0.2 * i,               # accel_y
                9.8 + 0.1 * i,         # accel_z
                0.05 * i,              # gyro_x
                0.03 * i,              # gyro_y
                0.02 * i,              # gyro_z
                1000 + 10 * i          # gsr
            ]
            writer.writerow(row)
    
    return filename


def compare_csv_files(file1: str, file2: str, tolerance: float = 1e-6) -> bool:
    """Compare two CSV files with numerical tolerance."""
    import csv
    
    with open(file1, 'r') as f1, open(file2, 'r') as f2:
        reader1 = csv.reader(f1)
        reader2 = csv.reader(f2)
        
        for row1, row2 in zip(reader1, reader2):
            if len(row1) != len(row2):
                return False
            
            for val1, val2 in zip(row1, row2):
                # Try to compare as numbers first
                try:
                    num1 = float(val1)
                    num2 = float(val2)
                    if abs(num1 - num2) > tolerance:
                        return False
                except ValueError:
                    # Compare as strings if not numbers
                    if val1 != val2:
                        return False
    
    return True


def capture_stdout():
    """Context manager to capture stdout for testing print statements."""
    from io import StringIO
    from contextlib import redirect_stdout
    
    class StdoutCapture:
        def __init__(self):
            self.output = StringIO()
        
        def __enter__(self):
            self.redirect = redirect_stdout(self.output)
            self.redirect.__enter__()
            return self
        
        def __exit__(self, *args):
            self.redirect.__exit__(*args)
        
        def get_output(self) -> str:
            return self.output.getvalue()
    
    return StdoutCapture()


def run_with_timeout(func, timeout: float = 10.0, *args, **kwargs):
    """Run a function with a timeout."""
    import threading
    import queue
    
    result_queue = queue.Queue()
    exception_queue = queue.Queue()
    
    def target():
        try:
            result = func(*args, **kwargs)
            result_queue.put(result)
        except Exception as e:
            exception_queue.put(e)
    
    thread = threading.Thread(target=target)
    thread.daemon = True
    thread.start()
    thread.join(timeout)
    
    if thread.is_alive():
        raise TimeoutError(f"Function {func.__name__} timed out after {timeout} seconds")
    
    if not exception_queue.empty():
        raise exception_queue.get()
    
    if not result_queue.empty():
        return result_queue.get()
    
    return None


def skip_if_no_hardware(test_func):
    """Decorator to skip tests if hardware is not available."""
    import unittest
    
    def wrapper(*args, **kwargs):
        # Check if hardware is available (this is a simple check)
        # In a real implementation, you might check for specific hardware
        hardware_available = os.environ.get('HARDWARE_TESTS_ENABLED', 'false').lower() == 'true'
        
        if not hardware_available:
            raise unittest.SkipTest("Hardware not available for testing")
        
        return test_func(*args, **kwargs)
    
    return wrapper


def skip_if_no_gui(test_func):
    """Decorator to skip tests if GUI is not available."""
    import unittest
    
    def wrapper(*args, **kwargs):
        # Check if GUI is available
        gui_available = os.environ.get('DISPLAY') is not None or os.name == 'nt'
        
        if not gui_available:
            raise unittest.SkipTest("GUI not available for testing")
        
        return test_func(*args, **kwargs)
    
    return wrapper