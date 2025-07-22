// platforms/android/build.gradle.kts
// Android platform configuration

plugins {
    base
}

// Define Android environment properties
val androidHome = "${rootProject.projectDir}/environments/android-sdk"
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val setupScript = if (isWindows) "setup_android.bat" else "setup_android.sh"

// Task to set up Android environment
tasks.register<Exec>("setupAndroidEnv") {
    group = "android"
    description = "Set up local Android SDK, Build Tools, and NDK"

    val setupScriptPath = "${rootProject.projectDir}/environments/$setupScript"

    if (isWindows) {
        commandLine("cmd", "/c", setupScriptPath)
    } else {
        commandLine("bash", setupScriptPath)
    }

    workingDir = file("${rootProject.projectDir}/environments")

    inputs.files("${rootProject.projectDir}/environments/$setupScript")
    outputs.dir(androidHome)

    doFirst {
        println("Setting up Android environment...")
        println("Android SDK will be installed to: $androidHome")
    }

    doLast {
        println("Android environment setup completed")
        println("ANDROID_HOME: $androidHome")
    }
}

// Task to verify Android environment
tasks.register("verifyAndroidEnv") {
    group = "android"
    description = "Verify Android environment is properly set up"

    doLast {
        val androidSdk = file("$androidHome/platforms")
        val buildTools = file("$androidHome/build-tools")
        val platformTools = file("$androidHome/platform-tools")

        println("🔍 Verifying Android Environment...")
        println("=====================================")

        if (androidSdk.exists()) {
            println("✅ Android SDK platforms found")
        } else {
            println("❌ Android SDK platforms not found")
            throw GradleException("Android SDK not properly installed. Run 'setupAndroidEnv' task first.")
        }

        if (buildTools.exists()) {
            println("✅ Android Build Tools found")
        } else {
            println("❌ Android Build Tools not found")
            throw GradleException("Android Build Tools not properly installed. Run 'setupAndroidEnv' task first.")
        }

        if (platformTools.exists()) {
            println("✅ Android Platform Tools found")
        } else {
            println("❌ Android Platform Tools not found")
            throw GradleException("Android Platform Tools not properly installed. Run 'setupAndroidEnv' task first.")
        }

        println("🎯 Android environment verification completed successfully!")
    }
}

// Task to clean Android environment
tasks.register<Delete>("cleanAndroidEnv") {
    group = "android"
    description = "Clean local Android environment"

    delete(androidHome)

    doLast {
        println("Android environment cleaned: $androidHome")
    }
}

// Configure the main build task
tasks.named("build") {
    dependsOn("verifyAndroidEnv")

    doLast {
        println("Android platform build completed successfully")
    }
}

// Make sure Android environment is set up before verification
tasks.named("verifyAndroidEnv") {
    dependsOn("setupAndroidEnv")
}

// Configure subprojects (app)
subprojects {
    // Set Android environment variables for all Android subprojects
    tasks.configureEach {
        if (name.contains("android", ignoreCase = true) || 
            name.contains("build", ignoreCase = true) ||
            name.contains("assemble", ignoreCase = true)) {

            doFirst {
                // Set environment variables for Android build
                System.setProperty("ANDROID_HOME", androidHome)
                System.setProperty("ANDROID_SDK_ROOT", androidHome)

                println("🤖 Using local Android SDK: $androidHome")
            }
        }
    }

    // Configure environment variables for Exec tasks specifically
    tasks.withType<Exec> {
        environment("ANDROID_HOME", androidHome)
        environment("ANDROID_SDK_ROOT", androidHome)
    }
}
