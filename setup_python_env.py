#!/usr/bin/env python3
"""
Setup script for creating a Python virtual environment with all required dependencies.
This script makes the repository self-sufficient by setting up the Python environment.
"""

import os
import sys
import subprocess
import platform

def run_command(command, cwd=None):
    """Run a command and return the result."""
    try:
        result = subprocess.run(command, shell=True, cwd=cwd, capture_output=True, text=True)
        if result.returncode != 0:
            print(f"Error running command: {command}")
            print(f"Error output: {result.stderr}")
            return False
        return True
    except Exception as e:
        print(f"Exception running command {command}: {e}")
        return False

def setup_python_environment():
    """Set up Python virtual environment and install dependencies."""
    print("Setting up Python virtual environment...")

    # Create virtual environment
    venv_path = "venv"
    if not os.path.exists(venv_path):
        print("Creating virtual environment...")
        if not run_command(f"{sys.executable} -m venv {venv_path}"):
            return False
    else:
        print("Virtual environment already exists.")

    # Determine the correct pip path based on platform
    if platform.system() == "Windows":
        pip_path = os.path.join(venv_path, "Scripts", "pip")
        python_path = os.path.join(venv_path, "Scripts", "python")
    else:
        pip_path = os.path.join(venv_path, "bin", "pip")
        python_path = os.path.join(venv_path, "bin", "python")

    # Upgrade pip
    print("Upgrading pip...")
    if not run_command(f"{python_path} -m pip install --upgrade pip"):
        return False

    # Install Windows controller dependencies
    print("Installing Windows controller dependencies...")
    if not run_command(f"{pip_path} install -r windows_controller/requirements.txt"):
        print("Warning: Some dependencies failed to install. Trying alternative approach...")
        # Try installing core dependencies individually
        core_deps = [
            "numpy>=1.24.3",
            "opencv-python>=4.8.0",
            "pandas>=2.0.3",
            "matplotlib>=3.7.2",
            "pyserial>=3.5",
            "websockets>=11.0.3",
            "pyyaml>=6.0.1"
        ]

        failed_deps = []
        for dep in core_deps:
            print(f"Installing {dep}...")
            if not run_command(f"{pip_path} install {dep}"):
                failed_deps.append(dep)

        if failed_deps:
            print(f"Warning: The following dependencies failed to install: {failed_deps}")
            print("You may need to install them manually or install additional system dependencies.")

        # Try to install PyQt6 separately
        print("Attempting to install PyQt6...")
        if not run_command(f"{pip_path} install PyQt6"):
            print("Warning: PyQt6 installation failed. You may need to install Qt development tools.")
            print("Alternative: Try 'pip install PySide6' for a different Qt binding.")

    # Install Windows app dependencies (if setup.py exists)
    if os.path.exists("windows/setup.py"):
        print("Installing Windows app dependencies...")
        if not run_command(f"{pip_path} install -e windows/"):
            return False

    print("Python environment setup completed successfully!")
    print(f"To activate the environment:")
    if platform.system() == "Windows":
        print(f"  {venv_path}\\Scripts\\activate")
    else:
        print(f"  source {venv_path}/bin/activate")

    return True

if __name__ == "__main__":
    if setup_python_environment():
        print("Setup completed successfully!")
        sys.exit(0)
    else:
        print("Setup failed!")
        sys.exit(1)
