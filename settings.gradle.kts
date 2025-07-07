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
        // Add JitPack for any dependencies that require it
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "GSR-Unified"

// Include the PC platform project
include(":pc")
project(":pc").projectDir = file("platforms/pc")

// Tell Gradle about the 'android' platform project
include(":android")
project(":android").projectDir = file("platforms/android")

// **CRITICAL**: Tell Gradle that ':android' contains a sub-project called ':app'
include(":android:app")
project(":android:app").projectDir = file("platforms/android/app")
