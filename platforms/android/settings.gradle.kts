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
        // Add JitPack for usb-serial-for-android
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "GSRCapture"
include(":app")

