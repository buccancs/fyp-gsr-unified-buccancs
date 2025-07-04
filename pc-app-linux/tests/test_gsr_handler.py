import queue
import time
from unittest.mock import MagicMock, patch

import pytest

from app.sensors.gsr_handler import GSRHandler

# Mock external libraries so tests can run without them
# These mocks will replace the actual libraries during the test run
mock_serial = MagicMock()
mock_pylsl = MagicMock()
mock_pyshimmer = MagicMock()

# We use a dictionary to patch the modules in sys.modules
module_patches = {
    "serial": mock_serial,
    "serial.tools": MagicMock(),
    "serial.tools.list_ports": MagicMock(),
    "pylsl": mock_pylsl,
    "pyshimmer": mock_pyshimmer,
}


@pytest.fixture
def gsr_handler():
    """Fixture to provide a clean GSRHandler instance for each test."""
    # The 'with patch.dict' temporarily replaces modules in the Python environment
    with patch.dict("sys.modules", module_patches):
        handler = GSRHandler()
        yield handler
        # Teardown: ensure capture is stopped after each test
        if handler.is_capturing_data():
            handler.stop_gsr_capture()


def test_initialization(gsr_handler):
    """Verify the handler initializes in a clean state."""
    assert not gsr_handler.is_capturing
    assert gsr_handler.capture_mode == "simulation"
    assert gsr_handler.data_queue.empty()
    assert gsr_handler.latest_data is None


def test_set_capture_mode(gsr_handler):
    """Test setting different valid capture modes."""
    valid_modes = ["simulation", "serial", "lsl", "pyshimmer"]
    for mode in valid_modes:
        gsr_handler.set_capture_mode(mode)
        assert gsr_handler.capture_mode == mode


def test_set_invalid_capture_mode_raises_error(gsr_handler):
    """Verify that setting an invalid mode raises a ValueError."""
    with pytest.raises(ValueError):
        gsr_handler.set_capture_mode("invalid_mode")


def test_start_and_stop_simulation_capture(gsr_handler):
    """Test the simulation capture mode lifecycle."""
    gsr_handler.set_capture_mode("simulation")
    assert gsr_handler.start_gsr_capture()
    assert gsr_handler.is_capturing
    assert gsr_handler.capture_thread.is_alive()

    # Let it run for a bit
    time.sleep(0.1)
    assert not gsr_handler.data_queue.empty()
    assert gsr_handler.latest_data is not None
    assert gsr_handler.latest_data["source"] == "simulation"

    gsr_handler.stop_gsr_capture()
    assert not gsr_handler.is_capturing
    # The thread might take a moment to die
    gsr_handler.capture_thread.join(timeout=1.0)
    assert not gsr_handler.capture_thread.is_alive()


def test_start_serial_capture_success(gsr_handler):
    """Test starting serial capture with a mocked serial port."""
    gsr_handler.set_capture_mode("serial")
    gsr_handler.configure_serial(port="/dev/ttyUSB0")

    # Mock the serial.Serial class to avoid real hardware interaction
    mock_serial.Serial.return_value = MagicMock()

    assert gsr_handler.start_gsr_capture()
    mock_serial.Serial.assert_called_with(
        port="/dev/ttyUSB0", baudrate=115200, timeout=1.0
    )
    assert gsr_handler.is_capturing


def test_start_lsl_capture_success(gsr_handler):
    """Test starting LSL capture with a mocked LSL stream."""
    gsr_handler.set_capture_mode("lsl")

    # Mock the pylsl functions to simulate finding a stream
    mock_stream_info = MagicMock()
    mock_stream_info.name.return_value = "MockGSRStream"
    mock_pylsl.resolve_stream.return_value = [mock_stream_info]
    mock_pylsl.StreamInlet.return_value = MagicMock()

    assert gsr_handler.start_gsr_capture()
    mock_pylsl.resolve_stream.assert_called_with("type", "GSR", timeout=5.0)
    mock_pylsl.StreamInlet.assert_called_with(mock_stream_info)
    assert gsr_handler.is_capturing


def test_start_pyshimmer_capture_success(gsr_handler):
    """Test starting PyShimmer capture with a mocked device."""
    gsr_handler.set_capture_mode("pyshimmer")
    gsr_handler.configure_pyshimmer(mac_address="00:11:22:33:44:55")

    # Mock the pyshimmer.ShimmerBluetooth class
    mock_device = MagicMock()
    mock_pyshimmer.ShimmerBluetooth.return_value = mock_device

    assert gsr_handler.start_gsr_capture()
    mock_pyshimmer.ShimmerBluetooth.assert_called_with("00:11:22:33:44:55")
    mock_device.connect.assert_called_once()
    mock_device.set_sensors.assert_called_once()
    assert gsr_handler.is_capturing


def test_data_callback_is_called(gsr_handler):
    """Verify that a registered data callback is invoked with new data."""
    mock_callback = MagicMock()
    gsr_handler.add_data_callback(mock_callback)

    test_data = {
        "timestamp": time.time(),
        "conductance": 10.5,
        "resistance": 1.0 / 10.5,
        "source": "test",
    }

    # Manually call the internal processing method to test the callback logic
    gsr_handler._process_gsr_data(test_data)

    mock_callback.assert_called_once_with(test_data)


def test_get_capture_statistics(gsr_handler):
    """Test that capture statistics are updated correctly."""
    gsr_handler.set_capture_mode("simulation")
    gsr_handler.start_gsr_capture()
    time.sleep(0.1)
    gsr_handler.stop_gsr_capture()

    stats = gsr_handler.get_capture_statistics()

    assert stats["is_capturing"] is False
    assert stats["capture_mode"] == "simulation"
    assert stats["total_samples"] > 0
    assert stats["duration_seconds"] > 0
    assert stats["average_sample_rate"] > 0
