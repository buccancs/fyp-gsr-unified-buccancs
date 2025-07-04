import time
from unittest.mock import ANY, MagicMock, patch

import pytest

# Mock the entire pylsl library before importing the class under test
# This prevents the need for the actual library to be installed in the test environment.
mock_pylsl = MagicMock()
module_patches = {"pylsl": mock_pylsl}

with patch.dict("sys.modules", module_patches):
    from app.lsl.lsl_manager import LslCommandSender, LslInletManager

    # Mock the generated protobuf file as well
    mock_protobuf = MagicMock()
    patch.dict("sys.modules", {"gsrunified": mock_protobuf}).start()


@pytest.fixture
def inlet_manager():
    """Fixture for a clean LslInletManager instance for each test."""
    manager = LslInletManager()
    yield manager
    manager.stop()


@pytest.fixture
def command_sender():
    """Fixture for a clean LslCommandSender instance for each test."""
    sender = LslCommandSender(device_id="PC_Test_Controller")
    yield sender
    sender.cleanup()


def test_lsl_command_sender_initialization(command_sender):
    """Verify that the LslCommandSender creates a StreamOutlet on initialization."""
    mock_pylsl.StreamInfo.assert_called_with(
        "Command_PC_Test_Controller",
        "Command",
        1,
        mock_pylsl.IRREGULAR_RATE,
        mock_pylsl.cf_string,
        "PC_Test_Controller",
    )
    mock_pylsl.StreamOutlet.assert_called_once()
    assert command_sender.command_outlet is not None


def test_lsl_command_sender_sends_command(command_sender):
    """Verify that send_command pushes a sample to the outlet."""
    mock_outlet = mock_pylsl.StreamOutlet.return_value
    command_data = "test_command_string"

    result = command_sender.send_command(command_data)

    assert result is True
    mock_outlet.push_sample.assert_called_with([command_data], ANY)


def test_lsl_inlet_manager_discovery_loop(inlet_manager):
    """Verify that the discovery loop finds and processes streams."""
    # Create a mock stream info object
    mock_stream_info = MagicMock()
    mock_stream_info.name.return_value = "GSR_android_test_1"
    mock_stream_info.type.return_value = "GSR"
    mock_pylsl.resolve_streams.return_value = [mock_stream_info]

    # Start the manager and let the discovery loop run once
    inlet_manager.start()
    time.sleep(0.1)  # Give thread time to run

    # Assertions
    mock_pylsl.resolve_streams.assert_called()
    assert "GSR_android_test_1" in inlet_manager.streams
    # The manager should have auto-connected to the GSR stream
    mock_pylsl.StreamInlet.assert_called_with(
        mock_stream_info, max_buflen=360, max_chunklen=0
    )
    assert "GSR_android_test_1" in inlet_manager.inlets


def test_lsl_inlet_manager_data_reception_and_callback(inlet_manager):
    """Verify that data is received, processed, and triggers callbacks."""
    # Mock the inlet to return specific data
    mock_inlet = MagicMock()
    # Simulate a chunk of 2 samples for a GSR stream
    mock_inlet.pull_chunk.return_value = (
        [[1.23, 0.81, 100.0], [1.24, 0.80, 99.0]],  # samples
        [time.time(), time.time() + 0.008],  # timestamps
    )
    mock_pylsl.StreamInlet.return_value = mock_inlet

    # Register a mock callback
    mock_callback = MagicMock()
    inlet_manager.register_data_callback("GSR", mock_callback)

    # Manually trigger the data reception loop for a test stream
    mock_stream_info = MagicMock()
    mock_stream_info.name.return_value = "GSR_test_device"
    mock_stream_info.type.return_value = "GSR"
    inlet_manager.streams["GSR_test_device"] = mock_stream_info
    inlet_manager.inlets["GSR_test_device"] = mock_inlet

    # Run the reception loop once
    inlet_manager._data_reception_loop("GSR_test_device", mock_inlet, mock_stream_info)

    # Assertions
    assert mock_callback.call_count == 2
    # Check the content of the first call
    mock_callback.assert_any_call(
        {
            "stream_name": "GSR_test_device",
            "stream_type": "GSR",
            "timestamp": ANY,
            "sample": [1.23, 0.81, 100.0],
            "conductance": 1.23,
            "resistance": 0.81,
            "quality": 100,
        }
    )
    # Check that data was added to the queue
    assert inlet_manager.data_queue.qsize() == 2
