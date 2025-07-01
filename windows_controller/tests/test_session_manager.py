#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Unit tests for the SessionManager class.
"""

import unittest
import os
import shutil
import tempfile
import json
from datetime import datetime

# Add the parent directory to the path so we can import our modules
import sys
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from src.utils.session_manager import SessionManager

class TestSessionManager(unittest.TestCase):
    """
    Test case for the SessionManager class.
    """
    
    def setUp(self):
        """
        Set up the test case.
        """
        # Create a temporary directory for testing
        self.test_dir = tempfile.mkdtemp()
        
        # Create a session manager with the test directory as the base directory
        self.session_manager = SessionManager(base_dir=self.test_dir)
    
    def tearDown(self):
        """
        Clean up after the test case.
        """
        # Remove the temporary directory
        shutil.rmtree(self.test_dir)
    
    def test_create_new_session(self):
        """
        Test creating a new session.
        """
        # Create a new session
        result = self.session_manager.create_new_session(session_id="test_session")
        
        # Check that the session was created successfully
        self.assertTrue(result)
        self.assertEqual(self.session_manager.get_current_session_id(), "test_session")
        
        # Check that the session directory was created
        session_dir = os.path.join(self.test_dir, "test_session")
        self.assertTrue(os.path.isdir(session_dir))
        
        # Check that the metadata file was created
        metadata_file = os.path.join(session_dir, "session_metadata.json")
        self.assertTrue(os.path.isfile(metadata_file))
        
        # Check that the metadata file contains the correct information
        with open(metadata_file, "r") as f:
            metadata = json.load(f)
        
        self.assertEqual(metadata["session_id"], "test_session")
        self.assertIsNotNone(metadata["start_time"])
        self.assertIsNone(metadata["end_time"])
        self.assertEqual(metadata["devices"], {})
        self.assertEqual(metadata["files"], [])
    
    def test_close_session(self):
        """
        Test closing a session.
        """
        # Create a new session
        self.session_manager.create_new_session(session_id="test_session")
        
        # Close the session
        result = self.session_manager.close_session()
        
        # Check that the session was closed successfully
        self.assertTrue(result)
        self.assertIsNone(self.session_manager.get_current_session_id())
        
        # Check that the metadata file was updated
        metadata_file = os.path.join(self.test_dir, "test_session", "session_metadata.json")
        with open(metadata_file, "r") as f:
            metadata = json.load(f)
        
        self.assertIsNotNone(metadata["end_time"])
    
    def test_generate_manifest(self):
        """
        Test generating a session manifest.
        """
        # Create a new session
        self.session_manager.create_new_session(session_id="test_session")
        
        # Generate a manifest
        result = self.session_manager.generate_manifest()
        
        # Check that the manifest was generated successfully
        self.assertTrue(result)
        
        # Check that the manifest file was created
        manifest_file = os.path.join(self.test_dir, "test_session", "session_manifest.json")
        self.assertTrue(os.path.isfile(manifest_file))
        
        # Check that the manifest file contains the correct information
        with open(manifest_file, "r") as f:
            manifest = json.load(f)
        
        self.assertEqual(manifest["session_id"], "test_session")
        self.assertIsNotNone(manifest["start_time"])
        self.assertIsNotNone(manifest["end_time"])
        self.assertIn("duration_seconds", manifest)
        self.assertEqual(manifest["devices"], {})
        self.assertEqual(manifest["files"], [])

if __name__ == "__main__":
    unittest.main()