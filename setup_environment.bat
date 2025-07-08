@echo off
REM FYP GSR Unified Buccancs - Environment Setup Script (Windows)
REM This script removes the old corrupted environment and creates a fresh, stable one

echo 🔧 FYP GSR Unified Buccancs - Environment Setup
echo ================================================
echo.

REM Step 1: Remove the old, corrupted environment completely
echo 🗑️  Step 1: Removing old environment...
echo Attempting to remove environment 'fyp-gsr-unified-buccancs'...

conda env list | findstr "fyp-gsr-unified-buccancs" >nul
if %errorlevel% == 0 (
    echo Found existing environment. Removing...
    conda env remove --name fyp-gsr-unified-buccancs --yes
    if %errorlevel% == 0 (
        echo ✅ Old environment removed successfully
    ) else (
        echo ❌ Failed to remove old environment
        pause
        exit /b 1
    )
) else (
    echo ℹ️  No existing environment found to remove
)

echo.

REM Step 2: Create a Fresh, Stable Environment
echo 🆕 Step 2: Creating fresh environment with Python 3.10...
echo Creating new environment named 'fyp-gsr-unified-buccancs' with Python 3.10...

if exist "environment.yml" (
    echo Using environment.yml for comprehensive setup...
    conda env create -f environment.yml
    if %errorlevel% == 0 (
        echo ✅ Environment created from environment.yml
    ) else (
        echo ❌ Failed to create environment from environment.yml
        pause
        exit /b 1
    )
) else (
    echo Creating basic environment with Python 3.10...
    conda create --name fyp-gsr-unified-buccancs python=3.10 --yes
    if %errorlevel% == 0 (
        echo ✅ Basic environment created
    ) else (
        echo ❌ Failed to create basic environment
        pause
        exit /b 1
    )
)

echo.

REM Step 3: Provide activation and dependency installation instructions
echo 🔌 Step 3: Environment setup complete!

echo Environment setup complete! To activate and use the environment:
echo.
echo   conda activate fyp-gsr-unified-buccancs
echo.

REM If we have requirements files, provide installation instructions
if exist "platforms\pc\requirements.txt" (
    echo 📦 To install additional dependencies:
    echo   cd platforms\pc
    echo   pip install -r requirements.txt
    echo   pip install -r requirements-test.txt
    echo.
)

echo 🎯 Environment Setup Summary:
echo ==============================
echo ✅ Old environment removed (if existed)
echo ✅ New environment 'fyp-gsr-unified-buccancs' created with Python 3.10
echo ✅ Core dependencies installed via conda
echo 📋 Additional pip dependencies available in platforms\pc\requirements*.txt
echo.
echo 🚀 Next Steps:
echo 1. conda activate fyp-gsr-unified-buccancs
echo 2. cd platforms\pc
echo 3. pip install -r requirements.txt
echo 4. pip install -r requirements-test.txt
echo.
echo 🏁 Setup completed successfully!
echo.
pause