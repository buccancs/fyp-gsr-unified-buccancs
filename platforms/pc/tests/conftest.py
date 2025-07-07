"""
Pytest configuration and fixtures for PC platform tests.
"""

import os
import sys
import pytest
import tempfile
import shutil
from pathlib import Path

# Add the src directory to Python path for all tests
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

@pytest.fixture(scope="session")
def test_data_dir():
    """Fixture providing path to test data directory."""
    return Path(__file__).parent / "test_data"

@pytest.fixture
def temp_dir():
    """Fixture providing a temporary directory for tests."""
    temp_dir = tempfile.mkdtemp()
    yield temp_dir
    shutil.rmtree(temp_dir)

@pytest.fixture
def mock_config():
    """Fixture providing mock configuration for tests."""
    return {
        'hardware': {
            'shimmer_com_port': 'COM3',
            'webcam_index': 0,
            'use_cpp_backend': True
        },
        'network': {
            'discovery_timeout': 5,
            'connection_timeout': 10
        },
        'ui': {
            'theme': 'light',
            'auto_save': True
        }
    }

@pytest.fixture
def sample_device_data():
    """Fixture providing sample device data for tests."""
    return {
        'device_id': 'test_device_001',
        'device_type': 'shimmer',
        'com_port': 'COM3',
        'is_connected': False,
        'is_streaming': False,
        'sample_rate': 512
    }