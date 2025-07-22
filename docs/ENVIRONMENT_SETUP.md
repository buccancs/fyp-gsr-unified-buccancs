# Environment Setup Guide

This guide provides instructions for setting up the conda environment for the FYP GSR Unified Buccancs project.

## Quick Setup

### Automated Setup (Recommended)

We provide automated setup scripts for both Unix-like systems (Linux/macOS) and Windows:

#### For Linux/macOS:
```bash
./setup_environment.sh
```

#### For Windows:
```cmd
setup_environment.bat
```

### Manual Setup

If you prefer to set up the environment manually or the automated scripts don't work:

#### 1. Remove Old Environment (if exists)
```bash
conda env remove --name fyp-gsr-unified-buccancs
```

#### 2. Create New Environment
```bash
# Option A: Using environment.yml (recommended)
conda env create -f environment.yml

# Option B: Basic setup
conda create --name fyp-gsr-unified-buccancs python=3.10
```

#### 3. Activate Environment
```bash
conda activate fyp-gsr-unified-buccancs
```

#### 4. Install Dependencies
```bash
cd platforms/pc
pip install -r requirements.txt
pip install -r requirements-test.txt
```

## Environment Details

- **Environment Name**: `fyp-gsr-unified-buccancs`
- **Python Version**: 3.10 (stable and compatible)
- **Package Manager**: Conda + pip
- **Dependencies**: Defined in `environment.yml`, `platforms/pc/requirements.txt`, and `platforms/pc/requirements-test.txt`

## Key Dependencies

### Core Dependencies (via conda)
- Python 3.10
- NumPy, Pandas, Matplotlib
- OpenCV, Pillow
- PySerial, PyYAML
- Pytest and testing tools

### Specialized Dependencies (via pip)
- **GUI**: PySide6 (Qt for Python)
- **Networking**: python-socketio, websockets, zeroconf
- **Hardware Integration**: bleak, pylsl, pyshimmer
- **Scientific Computing**: PsychoPy
- **macOS Compatibility**: pyobjc (pinned to 7.3 for compatibility)

## Platform-Specific Notes

### macOS
- Uses `pyobjc==7.3` for compatibility with bleak and PsychoPy
- Includes specific versions for Bluetooth Low Energy support

### Windows
- All dependencies should work out of the box
- May require Visual C++ redistributables for some packages

### Linux
- May require additional system packages for GUI applications
- Consider installing `python3-dev` and build tools

## Troubleshooting

### Common Issues

1. **Environment already exists**
   - Run the removal command first: `conda env remove --name fyp-gsr-unified-buccancs`

2. **Permission errors**
   - Ensure you have write permissions to your conda installation
   - On Unix systems, make sure the script is executable: `chmod +x setup_environment.sh`

3. **Package conflicts**
   - The environment.yml is carefully crafted to avoid conflicts
   - If issues persist, try the manual setup approach

4. **Missing system dependencies**
   - Some packages may require system-level dependencies
   - Install build tools and development headers as needed

### Verification

After setup, verify the environment works:

```bash
conda activate fyp-gsr-unified-buccancs
python -c "import PySide6, numpy, pandas, cv2, serial, pytest; print('Environment setup successful!')"
```

## Development Workflow

1. Always activate the environment before working:
   ```bash
   conda activate fyp-gsr-unified-buccancs
   ```

2. For PC platform development:
   ```bash
   cd platforms/pc
   ```

3. Run tests to verify everything works:
   ```bash
   python -m pytest tests/
   ```

## Updating Dependencies

To update dependencies:

1. Modify `environment.yml` or `requirements.txt` files
2. Update the environment:
   ```bash
   conda env update -f environment.yml
   # or
   pip install -r requirements.txt --upgrade
   ```

## Support

If you encounter issues with the environment setup:

1. Check this documentation first
2. Verify you have conda installed and updated
3. Try the manual setup process
4. Check the project's issue tracker for known problems

---

**Note**: This environment setup is specifically designed for the FYP GSR Unified Buccancs project and includes all necessary dependencies for hardware integration, GUI applications, and scientific computing.
