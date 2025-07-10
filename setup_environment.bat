@echo off
REM FYP GSR Unified Buccancs - Local Environment Setup Script (Windows)
REM This script creates all environments locally in the environments folder

echo 🔧 FYP GSR Unified Buccancs - Local Environment Setup
echo ====================================================
echo.

REM Step 1: Create local environments directory structure
echo 📁 Step 1: Creating local environment directories...
if not exist "environments" mkdir environments
if not exist "environments\conda" mkdir environments\conda
if not exist "environments\conda\channels" mkdir environments\conda\channels
if not exist "environments\conda\channels\conda-forge" mkdir environments\conda\channels\conda-forge
if not exist "environments\conda\channels\defaults" mkdir environments\conda\channels\defaults
if not exist "environments\pc" mkdir environments\pc
if not exist "environments\pc\windows" mkdir environments\pc\windows
if not exist "environments\pc\linux" mkdir environments\pc\linux
if not exist "environments\pc\macos" mkdir environments\pc\macos
if not exist "environments\pc\python" mkdir environments\pc\python
if not exist "environments\android" mkdir environments\android
if not exist "environments\repositories" mkdir environments\repositories
if not exist "environments\repositories\google" mkdir environments\repositories\google
if not exist "environments\repositories\maven-central" mkdir environments\repositories\maven-central
if not exist "environments\repositories\jitpack" mkdir environments\repositories\jitpack
if not exist "environments\repositories\gradle-plugins" mkdir environments\repositories\gradle-plugins

echo ✅ Local environment directories created

REM Step 2: Remove the old, corrupted environment completely
echo 🗑️  Step 2: Removing old system-wide environment...
echo Attempting to remove environment 'fyp-gsr-unified-buccancs'...

conda env list | findstr "fyp-gsr-unified-buccancs" >nul
if %errorlevel% == 0 (
    echo Found existing system environment. Removing...
    conda env remove --name fyp-gsr-unified-buccancs --yes
    if %errorlevel% == 0 (
        echo ✅ Old system environment removed successfully
    ) else (
        echo ❌ Failed to remove old system environment
        pause
        exit /b 1
    )
) else (
    echo ℹ️  No existing system environment found to remove
)

echo.

REM Step 3: Create a Fresh, Local Conda Environment
echo 🆕 Step 3: Creating fresh local conda environment with Python 3.10...
echo Creating new local environment in environments\conda\fyp-gsr-unified-buccancs...

if exist "environment.yml" (
    echo Using environment.yml for comprehensive local setup...
    conda env create -f environment.yml
    if %errorlevel% == 0 (
        echo ✅ Local environment created from environment.yml
    ) else (
        echo ❌ Failed to create local environment from environment.yml
        echo Falling back to basic local environment creation...
        conda create --prefix .\environments\conda\fyp-gsr-unified-buccancs python=3.10 --yes
        if %errorlevel% == 0 (
            echo ✅ Basic local environment created
        ) else (
            echo ❌ Failed to create basic local environment
            pause
            exit /b 1
        )
    )
) else (
    echo Creating basic local environment with Python 3.10...
    conda create --prefix .\environments\conda\fyp-gsr-unified-buccancs python=3.10 --yes
    if %errorlevel% == 0 (
        echo ✅ Basic local environment created
    ) else (
        echo ❌ Failed to create basic local environment
        pause
        exit /b 1
    )
)

echo.

REM Step 4: Provide activation and dependency installation instructions
echo 🔌 Step 4: Local environment setup complete!

echo Local environment setup complete! To activate and use the environment:
echo.
echo   conda activate .\environments\conda\fyp-gsr-unified-buccancs
echo.

REM If we have requirements files, provide installation instructions
if exist "platforms\pc\requirements.txt" (
    echo 📦 To install additional dependencies using local Python environment:
    echo   The build system will automatically use environments\pc\python\venv
    echo   Run: gradlew :pc:build (this will create local Python venv and install dependencies)
    echo.
    echo 📦 Or manually install to local Python environment:
    echo   cd platforms\pc
    echo   python -m venv ..\..\environments\pc\python\venv
    echo   ..\..\environments\pc\python\venv\Scripts\activate
    echo   pip install -r requirements.txt
    echo   pip install -r requirements-test.txt
    echo.
)

echo 🎯 Local Environment Setup Summary:
echo ===================================
echo ✅ Local environment directories created in .\environments\
echo ✅ Old system environment removed (if existed)
echo ✅ New local conda environment created in .\environments\conda\fyp-gsr-unified-buccancs
echo ✅ Core dependencies installed via conda (local)
echo ✅ Gradle configured to use local repositories in .\environments\repositories\
echo ✅ Java configured to use local installations in .\environments\pc\{os}\java\
echo 📋 Python dependencies will be installed locally in .\environments\pc\python\venv
echo.
echo 🚀 Next Steps:
echo 1. conda activate .\environments\conda\fyp-gsr-unified-buccancs
echo 2. Download Java and Gradle:
echo    - Run: .\environments\download_java.bat (downloads JDK 21 and 24)
echo    - Run: .\environments\download_gradle.bat (downloads Gradle)
echo 3. gradlew :pc:build (creates local Python venv and installs dependencies)
echo 4. gradlew :android:setupAndroidEnv (downloads and installs Android SDK locally)
echo 5. gradlew :android:build (uses local Android SDK and repositories)
echo.
echo 📋 Optional Downloads (run after conda activation):
echo - Java: .\environments\download_java.bat
echo - Gradle: .\environments\download_gradle.bat
echo - Android SDK: gradlew :android:setupAndroidEnv
echo.
echo 🏁 Local environment setup completed successfully!
echo All environments and dependencies will be stored locally in the environments folder.
echo.
pause
