# Gradle Files Fix Summary

This document summarizes all the fixes applied to the gradle build files in the FYP GSR Unified project.

## Issues Fixed

### 1. Missing Version Catalog
**Problem**: Both `build.gradle.kts` files referenced `alias(libs.plugins.sonarqube)` but there was no version catalog file.

**Solution**: Created `android-app/gradle/libs.versions.toml` with comprehensive version management for:
- Plugin versions (Android Gradle Plugin, Kotlin, Protobuf, SonarQube)
- Dependency versions (AndroidX, CameraX, Coroutines, Networking, ML libraries)
- Shimmer SDK versions
- Testing library versions

### 2. Java Version Inconsistencies
**Problem**: Different Java versions were specified across gradle files:
- Root `build.gradle.kts`: Java source "11"
- App `build.gradle.kts`: Java source "17" 
- App `build.gradle.kts`: Java compatibility VERSION_1_8

**Solution**: Standardized all Java version references to Java 8 to match the project's compatibility settings:
- Updated `sonar.java.source` to "8" in both files
- Kept Java compatibility at VERSION_1_8 in compileOptions

### 3. Security Vulnerability - Hardcoded Credentials
**Problem**: GitHub credentials were hardcoded in `settings.gradle.kts`:
```kotlin
val githubUsername = "buccancs"
val githubToken = "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"
val githubPassword = "Fesztike96()123456789@"
```

**Solution**: Replaced with secure environment variable and gradle properties approach:
```kotlin
val githubUsername = System.getenv("GITHUB_USERNAME") ?: settings.extra.properties["github.username"] as String? ?: ""
val githubToken = System.getenv("GITHUB_TOKEN") ?: settings.extra.properties["github.token"] as String? ?: ""
```

### 4. Missing Gradle Wrapper (Unix)
**Problem**: Only `gradlew.bat` (Windows) existed, no `gradlew` (Unix) file for Linux/Mac developers.

**Solution**: Created `android-app/gradlew` with proper POSIX shell script for cross-platform compatibility.

### 5. Dependency Management Improvement
**Problem**: Dependencies were hardcoded with version numbers throughout the build file.

**Solution**: Updated `app/build.gradle.kts` to use version catalog references:
- `implementation("androidx.core:core-ktx:1.12.0")` → `implementation(libs.androidx.core.ktx)`
- Applied to all 40+ dependencies for better maintainability

## New Files Created

### 1. `android-app/gradle/libs.versions.toml`
Comprehensive version catalog with:
- 25+ version definitions
- 5 plugin definitions
- 30+ library definitions
- Organized by category (AndroidX, CameraX, Networking, ML, Testing, etc.)

### 2. `android-app/gradlew`
Unix-compatible gradle wrapper script for cross-platform development.

### 3. `android-app/gradle.properties.template`
Template file documenting required properties:
- Android project settings
- Gradle performance settings
- GitHub credentials setup instructions
- SonarQube configuration
- Signing configuration template

## Setup Instructions for Developers

### 1. Configure GitHub Credentials
Copy the template and set up credentials:
```bash
cp gradle.properties.template gradle.properties
```

Edit `gradle.properties` and set:
```properties
github.username=YOUR_GITHUB_USERNAME
github.token=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

### 2. Get GitHub Personal Access Token
1. Go to https://github.com/settings/tokens
2. Create a new token with `read:packages` scope
3. Use this token in your `gradle.properties`

### 3. Alternative: Environment Variables
Instead of gradle.properties, you can set environment variables:
```bash
export GITHUB_USERNAME=your_username
export GITHUB_TOKEN=your_token
```

### 4. Fix Gradle Wrapper (if needed)
If you encounter "Could not find GradleWrapperMain" error:
1. Open the project in Android Studio
2. Go to File → Project Structure → Project
3. Set Gradle version to 8.4
4. Android Studio will regenerate the wrapper files

## Build Verification

After applying these fixes, the gradle configuration should:
- ✅ Resolve all plugin aliases correctly
- ✅ Use consistent Java versions across all configurations
- ✅ Securely handle GitHub credentials
- ✅ Support cross-platform development
- ✅ Maintain dependencies through version catalog
- ✅ Build successfully (assuming proper credentials are set)

## Security Notes

⚠️ **IMPORTANT**: Never commit `gradle.properties` with real credentials to version control.

The `.gitignore` should include:
```
gradle.properties
local.properties
```

## Dependencies Overview

The project now uses a centralized version catalog for:
- **AndroidX**: Core, AppCompat, Material Design, ConstraintLayout, Lifecycle, Activity
- **CameraX**: Core, Camera2, Lifecycle, Video, View, Extensions
- **Networking**: OkHttp, Okio
- **Serialization**: Kotlinx Serialization JSON, Protocol Buffers
- **ML/AI**: MediaPipe, ML Kit Pose Detection, OpenCV
- **Sensors**: Shimmer SDK (GSR), Lab Streaming Layer
- **Testing**: JUnit, AndroidX Test, Espresso
- **Development**: Kotlin Coroutines, SonarQube

All versions are now managed centrally in `libs.versions.toml` for easier maintenance and updates.