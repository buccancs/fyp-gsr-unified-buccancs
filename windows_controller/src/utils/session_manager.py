#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Session Manager for the PC Controller App.
This module handles session management and manifest generation.
Cross-platform support: Windows, macOS, Linux.
"""

import os
import json
import datetime
import logging
import uuid
from pathlib import Path

from utils.logger import get_logger

class SessionManager:
    """
    Session Manager class for handling session management and manifest generation.
    """

    def __init__(self, base_dir=None):
        """
        Initialize the session manager.

        Args:
            base_dir: The base directory for sessions (default: None, which uses the default location)
        """
        # Set up logging
        self.logger = get_logger(__name__)
        self.logger.info("Initializing session manager")

        # Set base directory
        if base_dir is None:
            # Use default location: pc_controller/sessions
            self.base_dir = os.path.join(
                os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
                "sessions"
            )
        else:
            self.base_dir = base_dir

        # Create base directory if it doesn't exist
        os.makedirs(self.base_dir, exist_ok=True)

        # Initialize session state
        self.current_session_id = None
        self.current_session_dir = None
        self.session_start_time = None
        self.session_end_time = None
        self.session_metadata = {}

        self.logger.info(f"Session manager initialized with base directory: {self.base_dir}")

    def create_new_session(self, session_id=None):
        """
        Create a new session.

        Args:
            session_id: The ID of the session to create (default: None, which generates a new ID)

        Returns:
            True if the session was created successfully, False otherwise
        """
        self.logger.info("Creating new session")

        # Generate session ID if not provided
        if session_id is None:
            # Generate a session ID based on timestamp and a random UUID
            timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
            random_id = str(uuid.uuid4())[:8]
            session_id = f"session_{timestamp}_{random_id}"

        # Create session directory
        session_dir = os.path.join(self.base_dir, session_id)
        try:
            os.makedirs(session_dir, exist_ok=True)

            # Initialize session state
            self.current_session_id = session_id
            self.current_session_dir = session_dir
            self.session_start_time = datetime.datetime.now()
            self.session_end_time = None

            # Initialize session metadata
            self.session_metadata = {
                "session_id": session_id,
                "start_time": self.session_start_time.isoformat(),
                "end_time": None,
                "devices": {},
                "files": []
            }

            # Save initial metadata
            self._save_metadata()

            self.logger.info(f"Created new session: {session_id}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to create session: {str(e)}")
            return False

    def open_session(self, session_dir):
        """
        Open an existing session.

        Args:
            session_dir: The directory of the session to open

        Returns:
            True if the session was opened successfully, False otherwise
        """
        self.logger.info(f"Opening session from directory: {session_dir}")

        # Check if the directory exists
        if not os.path.isdir(session_dir):
            self.logger.error(f"Session directory does not exist: {session_dir}")
            return False

        # Check if the metadata file exists
        metadata_file = os.path.join(session_dir, "session_metadata.json")
        if not os.path.isfile(metadata_file):
            self.logger.error(f"Session metadata file does not exist: {metadata_file}")
            return False

        try:
            # Load metadata
            with open(metadata_file, "r") as f:
                self.session_metadata = json.load(f)

            # Set session state
            self.current_session_id = self.session_metadata.get("session_id")
            self.current_session_dir = session_dir

            # Parse timestamps
            start_time_str = self.session_metadata.get("start_time")
            if start_time_str:
                self.session_start_time = datetime.datetime.fromisoformat(start_time_str)
            else:
                self.session_start_time = None

            end_time_str = self.session_metadata.get("end_time")
            if end_time_str:
                self.session_end_time = datetime.datetime.fromisoformat(end_time_str)
            else:
                self.session_end_time = None

            self.logger.info(f"Opened session: {self.current_session_id}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to open session: {str(e)}")
            return False

    def close_session(self):
        """
        Close the current session.

        Returns:
            True if the session was closed successfully, False otherwise
        """
        self.logger.info("Closing session")

        # Check if a session is open
        if not self.has_current_session():
            self.logger.warning("No session is currently open")
            return True

        try:
            # Set end time
            self.session_end_time = datetime.datetime.now()
            self.session_metadata["end_time"] = self.session_end_time.isoformat()

            # Save metadata
            self._save_metadata()

            # Clear session state
            session_id = self.current_session_id
            self.current_session_id = None
            self.current_session_dir = None
            self.session_start_time = None
            self.session_end_time = None
            self.session_metadata = {}

            self.logger.info(f"Closed session: {session_id}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to close session: {str(e)}")
            return False

    def has_current_session(self):
        """
        Check if a session is currently open.

        Returns:
            True if a session is open, False otherwise
        """
        return self.current_session_id is not None

    def get_current_session_id(self):
        """
        Get the ID of the current session.

        Returns:
            The ID of the current session, or None if no session is open
        """
        return self.current_session_id

    def get_session_directory(self):
        """
        Get the directory of the current session.

        Returns:
            The directory of the current session, or None if no session is open
        """
        return self.current_session_dir

    def get_session_duration(self):
        """
        Get the duration of the current session.

        Returns:
            The duration of the current session as a timedelta, or None if no session is open
        """
        if not self.has_current_session() or self.session_start_time is None:
            return None

        if self.session_end_time is not None:
            return self.session_end_time - self.session_start_time
        else:
            return datetime.datetime.now() - self.session_start_time

    def add_device_to_session(self, device):
        """
        Add a device to the current session.

        Args:
            device: The device to add

        Returns:
            True if the device was added successfully, False otherwise
        """
        self.logger.info(f"Adding device to session: {device.id}")

        # Check if a session is open
        if not self.has_current_session():
            self.logger.error("Cannot add device: no session is currently open")
            return False

        try:
            # Add device to metadata
            self.session_metadata["devices"][device.id] = {
                "id": device.id,
                "name": device.name,
                "address": device.address,
                "port": device.port,
                "type": device.device_type,
                "capabilities": device.capabilities
            }

            # Save metadata
            self._save_metadata()

            self.logger.info(f"Added device to session: {device.id}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to add device to session: {str(e)}")
            return False

    def add_file_to_session(self, device_id, file_path, file_type):
        """
        Add a file to the current session.

        Args:
            device_id: The ID of the device that produced the file
            file_path: The path to the file
            file_type: The type of file (e.g. "rgb", "thermal", "gsr")

        Returns:
            True if the file was added successfully, False otherwise
        """
        self.logger.info(f"Adding file to session: {file_path}")

        # Check if a session is open
        if not self.has_current_session():
            self.logger.error("Cannot add file: no session is currently open")
            return False

        try:
            # Add file to metadata
            self.session_metadata["files"].append({
                "device_id": device_id,
                "path": file_path,
                "type": file_type,
                "size": os.path.getsize(file_path) if os.path.isfile(file_path) else 0,
                "timestamp": datetime.datetime.now().isoformat()
            })

            # Save metadata
            self._save_metadata()

            self.logger.info(f"Added file to session: {file_path}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to add file to session: {str(e)}")
            return False

    def generate_manifest(self):
        """
        Generate a manifest for the current session.

        Returns:
            True if the manifest was generated successfully, False otherwise
        """
        self.logger.info("Generating session manifest")

        # Check if a session is open
        if not self.has_current_session():
            self.logger.error("Cannot generate manifest: no session is currently open")
            return False

        try:
            # Set end time if not already set
            if self.session_end_time is None:
                self.session_end_time = datetime.datetime.now()
                self.session_metadata["end_time"] = self.session_end_time.isoformat()

            # Calculate duration
            duration = self.get_session_duration()
            if duration is not None:
                self.session_metadata["duration_seconds"] = duration.total_seconds()

            # Save metadata
            self._save_metadata()

            # Create a more detailed manifest file
            manifest_file = os.path.join(self.current_session_dir, "session_manifest.json")
            with open(manifest_file, "w") as f:
                json.dump(self.session_metadata, f, indent=2)

            self.logger.info(f"Generated session manifest: {manifest_file}")
            return True
        except Exception as e:
            self.logger.error(f"Failed to generate manifest: {str(e)}")
            return False

    def _save_metadata(self):
        """
        Save the session metadata to a file.
        """
        if not self.has_current_session():
            return

        metadata_file = os.path.join(self.current_session_dir, "session_metadata.json")
        with open(metadata_file, "w") as f:
            json.dump(self.session_metadata, f, indent=2)
