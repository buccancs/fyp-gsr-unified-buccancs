"""Comprehensive C++ Backend Integration Tests

This module consolidates and replaces the scattered C++ backend tests:
- test_cpp_backend.py
- test_shimmer_integration.py  
- test_webcam_integration.py

Tests the complete integration of the C++ hardware backend with Python components.
"""

import pytest
import sys
import os
from unittest.mock import Mock, patch, MagicMock


class TestCppBackendAvailability:
    """Test C++ backend module availability and basic functionality."""
    
    def test_cpp_module_import(self) -> None:
        """Test that the C++ hardware backend module can be imported."""
        try:
            import _hardware_backend
            assert hasattr(_hardware_backend, '__version__')
            assert hasattr(_hardware_backend, '__author__')
            assert hasattr(_hardware_backend, 'get_version')
            assert hasattr(_hardware_backend, 'test_connection')
        except ImportError:
            pytest.skip("C++ hardware backend not available")
    
    def test_cpp_module_functions(self) -> None:
        """Test basic C++ module functions."""
        try:
            import _hardware_backend
            
            # Test version function
            version = _hardware_backend.get_version()
            assert isinstance(version, str)
            assert len(version) > 0
            
            # Test connection function
            test_msg = _hardware_backend.test_connection()
            assert isinstance(test_msg, str)
            
        except ImportError:
            pytest.skip("C++ hardware backend not available")
    
    def test_native_shimmer_class(self) -> None:
        """Test NativeShimmer class instantiation and basic methods."""
        try:
            import _hardware_backend
            
            # Test instantiation
            shimmer = _hardware_backend.NativeShimmer("COM3")
            assert shimmer is not None
            
            # Test basic methods
            assert hasattr(shimmer, 'get_com_port')
            assert hasattr(shimmer, 'is_connected')
            assert hasattr(shimmer, 'is_running')
            
            # Test method calls
            com_port = shimmer.get_com_port()
            assert com_port == "COM3"
            
            connected = shimmer.is_connected()
            assert isinstance(connected, bool)
            
            running = shimmer.is_running()
            assert isinstance(running, bool)
            
        except ImportError:
            pytest.skip("C++ hardware backend not available")
    
    def test_native_webcam_class(self) -> None:
        """Test NativeWebcam class instantiation and basic methods."""
        try:
            import _hardware_backend
            
            # Test instantiation
            webcam = _hardware_backend.NativeWebcam(0)
            assert webcam is not None
            
            # Test basic methods
            assert hasattr(webcam, 'get_camera_index')
            assert hasattr(webcam, 'is_connected')
            assert hasattr(webcam, 'is_running')
            assert hasattr(webcam, 'get_frame_width')
            assert hasattr(webcam, 'get_frame_height')
            
            # Test method calls
            camera_index = webcam.get_camera_index()
            assert camera_index == 0
            
            connected = webcam.is_connected()
            assert isinstance(connected, bool)
            
            running = webcam.is_running()
            assert isinstance(running, bool)
            
            width = webcam.get_frame_width()
            assert isinstance(width, int)
            assert width >= 0
            
            height = webcam.get_frame_height()
            assert isinstance(height, int)
            assert height >= 0
            
        except ImportError:
            pytest.skip("C++ hardware backend not available")


class TestShimmerIntegration:
    """Test ShimmerPC integration with C++ backend."""
    
    def test_shimmer_pc_import(self) -> None:
        """Test ShimmerPC class can be imported."""
        from hardware.shimmer_pc import ShimmerPC
        assert ShimmerPC is not None
    
    def test_shimmer_pc_instantiation(self) -> None:
        """Test ShimmerPC instantiation with various parameters."""
        from hardware.shimmer_pc import ShimmerPC
        
        # Test with COM port
        shimmer = ShimmerPC(com_port="COM3")
        assert shimmer.com_port == "COM3"
        assert hasattr(shimmer, 'use_cpp_backend')
        assert hasattr(shimmer, 'is_connected')
        assert hasattr(shimmer, 'is_streaming')
    
    @patch('hardware.shimmer_pc._hardware_backend', create=True)
    def test_shimmer_pc_cpp_backend_integration(self, mock_backend) -> None:
        """Test ShimmerPC integration with mocked C++ backend."""
        from hardware.shimmer_pc import ShimmerPC
        
        # Mock the NativeShimmer class
        mock_native_shimmer = Mock()
        mock_native_shimmer.get_com_port.return_value = "COM3"
        mock_native_shimmer.is_connected.return_value = False
        mock_native_shimmer.is_running.return_value = False
        mock_backend.NativeShimmer.return_value = mock_native_shimmer
        
        # Test instantiation and connection
        shimmer = ShimmerPC(com_port="COM3")
        shimmer.connect()
        
        # Verify backend was called
        mock_backend.NativeShimmer.assert_called_with("COM3")
    
    def test_shimmer_pc_api_compatibility(self) -> None:
        """Test that ShimmerPC maintains API compatibility."""
        from hardware.shimmer_pc import ShimmerPC
        
        shimmer = ShimmerPC(com_port="COM3")
        
        # Test required methods exist
        assert hasattr(shimmer, 'connect')
        assert hasattr(shimmer, 'disconnect')
        assert hasattr(shimmer, 'start_streaming')
        assert hasattr(shimmer, 'stop_streaming')
        assert hasattr(shimmer, 'get_data')
        
        # Test properties exist
        assert hasattr(shimmer, 'is_connected')
        assert hasattr(shimmer, 'is_streaming')
        assert hasattr(shimmer, 'com_port')


class TestWebcamIntegration:
    """Test WebcamPC integration with C++ backend."""
    
    def test_webcam_pc_import(self) -> None:
        """Test WebcamPC class can be imported."""
        from hardware.webcam_pc import WebcamPC
        assert WebcamPC is not None
    
    def test_webcam_pc_instantiation(self) -> None:
        """Test WebcamPC instantiation with various parameters."""
        from hardware.webcam_pc import WebcamPC
        
        # Test with camera index
        webcam = WebcamPC(camera_index=0)
        assert webcam.camera_index == 0
        assert hasattr(webcam, 'use_cpp_backend')
        assert hasattr(webcam, 'is_connected')
        assert hasattr(webcam, 'is_streaming')
    
    @patch('hardware.webcam_pc._hardware_backend', create=True)
    def test_webcam_pc_cpp_backend_integration(self, mock_backend) -> None:
        """Test WebcamPC integration with mocked C++ backend."""
        from hardware.webcam_pc import WebcamPC
        
        # Mock the NativeWebcam class
        mock_native_webcam = Mock()
        mock_native_webcam.get_camera_index.return_value = 0
        mock_native_webcam.is_connected.return_value = False
        mock_native_webcam.is_running.return_value = False
        mock_native_webcam.get_frame_width.return_value = 640
        mock_native_webcam.get_frame_height.return_value = 480
        mock_backend.NativeWebcam.return_value = mock_native_webcam
        
        # Test instantiation and connection
        webcam = WebcamPC(camera_index=0)
        webcam.connect()
        
        # Verify backend was called
        mock_backend.NativeWebcam.assert_called_with(0)
    
    def test_webcam_pc_api_compatibility(self) -> None:
        """Test that WebcamPC maintains API compatibility."""
        from hardware.webcam_pc import WebcamPC
        
        webcam = WebcamPC(camera_index=0)
        
        # Test required methods exist
        assert hasattr(webcam, 'connect')
        assert hasattr(webcam, 'disconnect')
        assert hasattr(webcam, 'start_streaming')
        assert hasattr(webcam, 'stop_streaming')
        assert hasattr(webcam, 'get_frame')
        
        # Test properties exist
        assert hasattr(webcam, 'is_connected')
        assert hasattr(webcam, 'is_streaming')
        assert hasattr(webcam, 'camera_index')


class TestCompleteHardwareIntegration:
    """Test complete hardware integration with both Shimmer and Webcam."""
    
    @patch('hardware.shimmer_pc._hardware_backend', create=True)
    @patch('hardware.webcam_pc._hardware_backend', create=True)
    def test_simultaneous_hardware_integration(self, mock_webcam_backend, mock_shimmer_backend) -> None:
        """Test that both Shimmer and Webcam can be used simultaneously."""
        from hardware.shimmer_pc import ShimmerPC
        from hardware.webcam_pc import WebcamPC
        
        # Mock backends
        mock_shimmer = Mock()
        mock_webcam = Mock()
        mock_shimmer_backend.NativeShimmer.return_value = mock_shimmer
        mock_webcam_backend.NativeWebcam.return_value = mock_webcam
        
        # Test instantiation of both
        shimmer = ShimmerPC(com_port="COM3")
        webcam = WebcamPC(camera_index=0)
        
        # Test connections
        shimmer.connect()
        webcam.connect()
        
        # Verify both backends were called
        mock_shimmer_backend.NativeShimmer.assert_called_with("COM3")
        mock_webcam_backend.NativeWebcam.assert_called_with(0)
    
    def test_hardware_classes_independence(self) -> None:
        """Test that hardware classes can be instantiated independently."""
        from hardware.shimmer_pc import ShimmerPC
        from hardware.webcam_pc import WebcamPC
        
        # Test independent instantiation
        shimmer = ShimmerPC(com_port="COM3")
        webcam = WebcamPC(camera_index=0)
        
        # Test that they don't interfere with each other
        assert shimmer.com_port == "COM3"
        assert webcam.camera_index == 0
        assert shimmer is not webcam


class TestBackendFallback:
    """Test fallback behavior when C++ backend is not available."""
    
    @patch('hardware.shimmer_pc._hardware_backend', None)
    def test_shimmer_fallback_behavior(self) -> None:
        """Test ShimmerPC behavior when C++ backend is not available."""
        from hardware.shimmer_pc import ShimmerPC
        
        # Should still instantiate but use fallback
        shimmer = ShimmerPC(com_port="COM3")
        assert shimmer.com_port == "COM3"
        # Backend availability should be handled gracefully
    
    @patch('hardware.webcam_pc._hardware_backend', None)
    def test_webcam_fallback_behavior(self) -> None:
        """Test WebcamPC behavior when C++ backend is not available."""
        from hardware.webcam_pc import WebcamPC
from typing import Any, Dict, List, Optional, Union
        
        # Should still instantiate but use fallback
        webcam = WebcamPC(camera_index=0)
        assert webcam.camera_index == 0
        # Backend availability should be handled gracefully


if __name__ == "__main__":
    pytest.main([__file__])