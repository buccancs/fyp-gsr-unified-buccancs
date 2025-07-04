import json
import time
from datetime import datetime
from pathlib import Path

from freezegun import freeze_time

from app.session.session_manager import SessionManager


# Pytest's tmp_path fixture provides a temporary directory unique to the test invocation
def test_start_session_creates_directory_structure(tmp_path):
    """Verify that start_session creates the correct directories and metadata file."""
    session_manager = SessionManager(base_dir=tmp_path)

    # Use freezegun to control the current time for a predictable session ID
    with freeze_time("2023-10-27 10:00:00"):
        session_id = session_manager.start_session()

        expected_session_id = "session_20231027_100000"
        assert session_id == expected_session_id

        session_dir = tmp_path / expected_session_id
        assert session_dir.is_dir()
        assert (session_dir / "analysis").is_dir()

        metadata_file = session_dir / "metadata.json"
        assert metadata_file.is_file()

        with open(metadata_file, "r") as f:
            metadata = json.load(f)
            assert metadata["session_id"] == expected_session_id
            assert "session_start_time" in metadata


def test_stop_session_resets_state_and_updates_metadata(tmp_path):
    """Verify that stop_session cleans up state and writes the end time to metadata."""
    session_manager = SessionManager(base_dir=tmp_path)

    with freeze_time("2023-10-27 10:00:00"):
        session_id = session_manager.start_session()

    time.sleep(1)  # Simulate session duration

    with freeze_time("2023-10-27 10:00:01"):
        session_manager.stop_session()

    assert not session_manager.is_session_active()
    assert session_manager.session_id is None

    metadata_file = tmp_path / session_id / "metadata.json"
    with open(metadata_file, "r") as f:
        metadata = json.load(f)
        assert "session_end_time" in metadata
        assert "session_duration" in metadata
        assert metadata["session_duration"] >= 1.0


def test_register_device_creates_device_specific_directory(tmp_path):
    """Verify that registering a device creates its own folder and info file."""
    session_manager = SessionManager(base_dir=tmp_path)
    session_manager.start_session()

    device_id = "android_test_device"
    device_info = {"model": "Pixel 7", "os": "13"}

    device_dir_name = session_manager.register_device(device_id, device_info)

    assert device_dir_name == "device_001"

    device_dir = session_manager.get_device_directory(device_id)
    assert device_dir is not None
    assert device_dir.is_dir()
    assert device_dir.name == "device_001"

    device_info_file = device_dir / "device_info.json"
    assert device_info_file.is_file()

    with open(device_info_file, "r") as f:
        info = json.load(f)
        assert info["device_id"] == device_id
        assert info["model"] == "Pixel 7"


def test_save_data_to_correct_directories(tmp_path):
    """Verify that data is saved to the session root or device-specific directory."""
    session_manager = SessionManager(base_dir=tmp_path)
    session_id = session_manager.start_session()

    # Register a device
    device_id = "test_phone"
    session_manager.register_device(device_id, {})

    # Save data to session root
    session_data = "This is session-wide data."
    session_manager.save_data("session_log.txt", session_data)

    # Save data to device directory
    device_data = "This is device-specific data."
    session_manager.save_data("device_log.txt", device_data, device_id=device_id)

    # Save analysis data
    analysis_data = "This is analysis data."
    session_manager.save_analysis_data("analysis_results.txt", analysis_data)

    # Verify file locations
    session_dir = tmp_path / session_id
    assert (session_dir / "session_log.txt").read_text() == session_data
    assert (session_dir / "device_001" / "device_log.txt").read_text() == device_data
    assert (
        session_dir / "analysis" / "analysis_results.txt"
    ).read_text() == analysis_data


def test_is_session_active_state_management(tmp_path):
    """Verify the is_session_active flag is managed correctly."""
    session_manager = SessionManager(base_dir=tmp_path)

    assert not session_manager.is_session_active()

    session_manager.start_session()
    assert session_manager.is_session_active()

    session_manager.stop_session()
    assert not session_manager.is_session_active()
