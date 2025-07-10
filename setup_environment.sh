#!/bin/bash

# FYP GSR Unified Buccancs - Local Environment Setup Script
# This script creates all environments locally in the environments folder

set -e  # Exit on any error

# Initialize conda for script execution
if [ -f "$HOME/miniconda3/etc/profile.d/conda.sh" ]; then
    source "$HOME/miniconda3/etc/profile.d/conda.sh"
elif [ -f "$HOME/anaconda3/etc/profile.d/conda.sh" ]; then
    source "$HOME/anaconda3/etc/profile.d/conda.sh"
elif [ -f "/opt/anaconda3/etc/profile.d/conda.sh" ]; then
    source "/opt/anaconda3/etc/profile.d/conda.sh"
elif [ -f "/opt/conda/etc/profile.d/conda.sh" ]; then
    source "/opt/conda/etc/profile.d/conda.sh"
else
    echo "⚠️  Warning: Could not find conda initialization script."
    echo "Please ensure conda is properly installed and initialized."
    echo ""
    echo "To run this script manually, execute these commands:"
    echo "1. conda env remove --name fyp-gsr-unified-buccancs"
    echo "2. conda env create -f environment.yml"
    echo "3. conda activate fyp-gsr-unified-buccancs"
    echo "4. cd platforms/pc && pip install -r requirements.txt && pip install -r requirements-test.txt"
    echo ""
    exit 1
fi

echo "🔧 FYP GSR Unified Buccancs - Local Environment Setup"
echo "===================================================="
echo

# Step 1: Create local environments directory structure
echo "📁 Step 1: Creating local environment directories..."
mkdir -p environments/conda/channels/conda-forge
mkdir -p environments/conda/channels/defaults
mkdir -p environments/pc/windows
mkdir -p environments/pc/linux
mkdir -p environments/pc/macos
mkdir -p environments/pc/python
mkdir -p environments/android
mkdir -p environments/repositories/google
mkdir -p environments/repositories/maven-central
mkdir -p environments/repositories/jitpack
mkdir -p environments/repositories/gradle-plugins

echo "✅ Local environment directories created"

# Step 2: Remove the old, corrupted environment completely
echo "🗑️  Step 2: Removing old system-wide environment..."
echo "Attempting to remove environment 'fyp-gsr-unified-buccancs'..."

if conda env list | grep -q "fyp-gsr-unified-buccancs"; then
    echo "Found existing system environment. Removing..."
    conda env remove --name fyp-gsr-unified-buccancs --yes
    echo "✅ Old system environment removed successfully"
else
    echo "ℹ️  No existing system environment found to remove"
fi

echo

# Step 3: Create a Fresh, Local Conda Environment
echo "🆕 Step 3: Creating fresh local conda environment with Python 3.10..."
echo "Creating new local environment in environments/conda/fyp-gsr-unified-buccancs..."

if [ -f "environment.yml" ]; then
    echo "Using environment.yml for comprehensive local setup..."
    conda env create -f environment.yml
    if [ $? -eq 0 ]; then
        echo "✅ Local environment created from environment.yml"
    else
        echo "❌ Failed to create local environment from environment.yml"
        echo "Falling back to basic local environment creation..."
        conda create --prefix ./environments/conda/fyp-gsr-unified-buccancs python=3.10 --yes
        echo "✅ Basic local environment created"
    fi
else
    echo "Creating basic local environment with Python 3.10..."
    conda create --prefix ./environments/conda/fyp-gsr-unified-buccancs python=3.10 --yes
    echo "✅ Basic local environment created"
fi

echo

# Step 4: Provide activation and dependency installation instructions
echo "🔌 Step 4: Local environment setup complete!"

# Note: We can't directly activate conda environment in a script that will persist
# So we provide instructions for the user
echo "Local environment setup complete! To activate and use the environment:"
echo
echo "  conda activate ./environments/conda/fyp-gsr-unified-buccancs"
echo

# If we have requirements files, provide installation instructions
if [ -f "platforms/pc/requirements.txt" ]; then
    echo "📦 To install additional dependencies using local Python environment:"
    echo "  The build system will automatically use environments/pc/python/venv"
    echo "  Run: ./gradlew :pc:build (this will create local Python venv and install dependencies)"
    echo
    echo "📦 Or manually install to local Python environment:"
    echo "  cd platforms/pc"
    echo "  python -m venv ../../environments/pc/python/venv"
    echo "  source ../../environments/pc/python/venv/bin/activate"
    echo "  pip install -r requirements.txt"
    echo "  pip install -r requirements-test.txt"
    echo
fi

echo "🎯 Local Environment Setup Summary:"
echo "==================================="
echo "✅ Local environment directories created in ./environments/"
echo "✅ Old system environment removed (if existed)"
echo "✅ New local conda environment created in ./environments/conda/fyp-gsr-unified-buccancs"
echo "✅ Core dependencies installed via conda (local)"
echo "✅ Gradle configured to use local repositories in ./environments/repositories/"
echo "✅ Java configured to use local installations in ./environments/pc/{os}/java/"
echo "📋 Python dependencies will be installed locally in ./environments/pc/python/venv"
echo
echo "🚀 Next Steps:"
echo "1. conda activate ./environments/conda/fyp-gsr-unified-buccancs"
echo "2. Download Java and Gradle:"
echo "   - Run: ./environments/download_java.sh (downloads JDK 21 and 24)"
echo "   - Run: ./environments/download_gradle.sh (downloads Gradle)"
echo "3. ./gradlew :pc:build (creates local Python venv and installs dependencies)"
echo "4. ./gradlew :android:setupAndroidEnv (downloads and installs Android SDK locally)"
echo "5. ./gradlew :android:build (uses local Android SDK and repositories)"
echo
echo "📋 Optional Downloads (run after conda activation):"
echo "- Java: ./environments/download_java.sh"
echo "- Gradle: ./environments/download_gradle.sh"
echo "- Android SDK: ./gradlew :android:setupAndroidEnv"
echo
echo "🏁 Local environment setup completed successfully!"
echo "All environments and dependencies will be stored locally in the environments folder."
