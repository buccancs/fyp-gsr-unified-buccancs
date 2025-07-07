// Root-level settings.gradle.kts
// This file allows Gradle commands to be run from the project root
// and delegates to the Android platform build

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "GSR-Unified"

// Include the Android platform as a subproject
include(":android")
project(":android").projectDir = file("platforms/android")

// // Include the Android app subproject
include(":android:app")
project(":android:app").projectDir = file("platforms/android/app")
