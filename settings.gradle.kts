pluginManagement {
    repositories {
        // Local repositories first
        maven { url = uri("file://$rootDir/environments/repositories/gradle-plugins") }
        maven { url = uri("file://$rootDir/environments/repositories/google") }
        maven { url = uri("file://$rootDir/environments/repositories/maven-central") }
        // Fallback to external repositories if local not available
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        // Local repositories first
        maven { url = uri("file://$rootDir/environments/repositories/google") }
        maven { url = uri("file://$rootDir/environments/repositories/maven-central") }
        maven { url = uri("file://$rootDir/environments/repositories/jitpack") }
        // Fallback to external repositories if local not available
        google()
        mavenCentral()
        // Add JitPack for any dependencies that require it
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "GSR-Unified"

// Include the PC platform project
include(":pc")
project(":pc").projectDir = file("$rootDir/platforms/pc")

// Tell Gradle about the 'android' platform project
include(":android")
project(":android").projectDir = file("$rootDir/platforms/android")

// **CRITICAL**: Tell Gradle that ':android' contains a sub-project called ':app'
include(":android:app")
project(":android:app").projectDir = file("$rootDir/platforms/android/app")
