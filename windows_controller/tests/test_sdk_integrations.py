#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Unit tests for SDK integrations (LSL, PsychoPy, Shimmer).
"""

import unittest
import os
import sys

# Add the parent directory to the path so we can import our modules
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

class TestSDKIntegrations(unittest.TestCase):
    """
    Test case for SDK integrations.
    """
    
    def test_lsl_integration_import(self):
        """
        Test that LSL integration can be imported.
        """
        try:
            from src.integrations.lsl_integration import LSLStreamer, LSLReceiver
            self.assertTrue(True, "LSL integration imported successfully")
        except ImportError as e:
            self.fail(f"Failed to import LSL integration: {e}")
    
    def test_psychopy_integration_import(self):
        """
        Test that PsychoPy integration can be imported.
        """
        try:
            from src.integrations.psychopy_integration import ExperimentController, StimulusLibrary
            self.assertTrue(True, "PsychoPy integration imported successfully")
        except ImportError as e:
            self.fail(f"Failed to import PsychoPy integration: {e}")
    
    def test_shimmer_integration_import(self):
        """
        Test that Shimmer integration can be imported.
        """
        try:
            from src.integrations.shimmer_integration import ShimmerSensor, ShimmerManager
            self.assertTrue(True, "Shimmer integration imported successfully")
        except ImportError as e:
            self.fail(f"Failed to import Shimmer integration: {e}")
    
    def test_lsl_streamer_initialization(self):
        """
        Test that LSL streamer can be initialized.
        """
        try:
            from src.integrations.lsl_integration import LSLStreamer
            streamer = LSLStreamer()
            self.assertIsNotNone(streamer)
            self.assertFalse(streamer.is_streaming)
            self.assertEqual(len(streamer.streams), 0)
        except Exception as e:
            self.fail(f"Failed to initialize LSL streamer: {e}")
    
    def test_psychopy_controller_initialization(self):
        """
        Test that PsychoPy controller can be initialized.
        """
        try:
            from src.integrations.psychopy_integration import ExperimentController
            controller = ExperimentController()
            self.assertIsNotNone(controller)
            self.assertFalse(controller.is_running)
            self.assertEqual(controller.current_trial, 0)
        except Exception as e:
            self.fail(f"Failed to initialize PsychoPy controller: {e}")
    
    def test_shimmer_sensor_initialization(self):
        """
        Test that Shimmer sensor can be initialized.
        """
        try:
            from src.integrations.shimmer_integration import ShimmerSensor
            sensor = ShimmerSensor("test_sensor", "bluetooth")
            self.assertIsNotNone(sensor)
            self.assertEqual(sensor.device_id, "test_sensor")
            self.assertEqual(sensor.connection_type, "bluetooth")
            self.assertFalse(sensor.is_connected)
            self.assertFalse(sensor.is_streaming)
        except Exception as e:
            self.fail(f"Failed to initialize Shimmer sensor: {e}")
    
    def test_shimmer_manager_initialization(self):
        """
        Test that Shimmer manager can be initialized.
        """
        try:
            from src.integrations.shimmer_integration import ShimmerManager
            manager = ShimmerManager()
            self.assertIsNotNone(manager)
            self.assertEqual(len(manager.sensors), 0)
        except Exception as e:
            self.fail(f"Failed to initialize Shimmer manager: {e}")
    
    def test_lsl_stream_creation(self):
        """
        Test that LSL streams can be created (if pylsl is available).
        """
        try:
            from src.integrations.lsl_integration import LSLStreamer
            streamer = LSLStreamer()
            
            # Try to create GSR stream (will fail gracefully if pylsl not available)
            result = streamer.create_gsr_stream("Test_GSR_Stream")
            # Don't assert True/False since pylsl might not be available
            # Just check that the method doesn't crash
            self.assertIsInstance(result, bool)
            
        except Exception as e:
            self.fail(f"LSL stream creation test failed: {e}")
    
    def test_shimmer_sensor_configuration(self):
        """
        Test that Shimmer sensor configuration works.
        """
        try:
            from src.integrations.shimmer_integration import ShimmerSensor
            sensor = ShimmerSensor("test_sensor", "serial")
            
            # Test configuration (should fail since not connected, but shouldn't crash)
            result = sensor.configure_sensors(gsr=True, ppg=True, sampling_rate=128)
            self.assertFalse(result)  # Should fail since not connected
            
            # Check that configuration was stored
            self.assertTrue(sensor.enabled_sensors['gsr'])
            self.assertTrue(sensor.enabled_sensors['ppg'])
            self.assertEqual(sensor.sampling_rate, 128)
            
        except Exception as e:
            self.fail(f"Shimmer sensor configuration test failed: {e}")
    
    def test_experiment_controller_stimulus_creation(self):
        """
        Test that PsychoPy stimulus creation works (if PsychoPy is available).
        """
        try:
            from src.integrations.psychopy_integration import ExperimentController
            controller = ExperimentController()
            
            # Try to initialize (will fail gracefully if PsychoPy not available)
            result = controller.initialize()
            # Don't assert True/False since PsychoPy might not be available
            # Just check that the method doesn't crash
            self.assertIsInstance(result, bool)
            
        except Exception as e:
            self.fail(f"PsychoPy controller test failed: {e}")


if __name__ == "__main__":
    unittest.main()