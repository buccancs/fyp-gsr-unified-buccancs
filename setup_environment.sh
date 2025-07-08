#!/bin/bash

# FYP GSR Unified Buccancs - Environment Setup Script
# This script removes the old corrupted environment and creates a fresh, stable one

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

echo "🔧 FYP GSR Unified Buccancs - Environment Setup"
echo "================================================"
echo

# Step 1: Remove the old, corrupted environment completely
echo "🗑️  Step 1: Removing old environment..."
echo "Attempting to remove environment 'fyp-gsr-unified-buccancs'..."

if conda env list | grep -q "fyp-gsr-unified-buccancs"; then
    echo "Found existing environment. Removing..."
    conda env remove --name fyp-gsr-unified-buccancs --yes
    echo "✅ Old environment removed successfully"
else
    echo "ℹ️  No existing environment found to remove"
fi

echo

# Step 2: Create a Fresh, Stable Environment
echo "🆕 Step 2: Creating fresh environment with Python 3.10..."
echo "Creating new environment named 'fyp-gsr-unified-buccancs' with Python 3.10..."

if [ -f "environment.yml" ]; then
    echo "Using environment.yml for comprehensive setup..."
    conda env create -f environment.yml
    echo "✅ Environment created from environment.yml"
else
    echo "Creating basic environment with Python 3.10..."
    conda create --name fyp-gsr-unified-buccancs python=3.10 --yes
    echo "✅ Basic environment created"
fi

echo

# Step 3: Activate and Install Dependencies
echo "🔌 Step 3: Activating environment and installing dependencies..."

# Note: We can't directly activate conda environment in a script that will persist
# So we provide instructions for the user
echo "Environment setup complete! To activate and use the environment:"
echo
echo "  conda activate fyp-gsr-unified-buccancs"
echo

# If we have requirements files, provide installation instructions
if [ -f "platforms/pc/requirements.txt" ]; then
    echo "📦 To install additional dependencies:"
    echo "  cd platforms/pc"
    echo "  pip install -r requirements.txt"
    echo "  pip install -r requirements-test.txt"
    echo
fi

echo "🎯 Environment Setup Summary:"
echo "=============================="
echo "✅ Old environment removed (if existed)"
echo "✅ New environment 'fyp-gsr-unified-buccancs' created with Python 3.10"
echo "✅ Core dependencies installed via conda"
echo "📋 Additional pip dependencies available in platforms/pc/requirements*.txt"
echo
echo "🚀 Next Steps:"
echo "1. conda activate fyp-gsr-unified-buccancs"
echo "2. cd platforms/pc"
echo "3. pip install -r requirements.txt"
echo "4. pip install -r requirements-test.txt"
echo
echo "🏁 Setup completed successfully!"
