// platforms/pc/build.gradle.kts
// Python+C++ project configuration

plugins {
    base
}

// Define project properties
val pythonExecutable = findProperty("python.executable")?.toString() ?: "python3"
val cmakeBuildType = findProperty("cmake.build.type")?.toString() ?: "Release"
val buildDir = layout.buildDirectory.get().asFile

// Task to set up Python virtual environment
tasks.register<Exec>("setupPythonEnv") {
    group = "python"
    description = "Set up Python virtual environment and install dependencies"

    commandLine(pythonExecutable, "-m", "venv", "venv")
    workingDir = projectDir

    doLast {
        println("Python virtual environment created at: ${projectDir}/venv")
    }
}

// Task to install Python dependencies
tasks.register<Exec>("installPythonDeps") {
    group = "python"
    description = "Install Python dependencies from requirements.txt"
    dependsOn("setupPythonEnv")

    val pipExecutable = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "${projectDir}/venv/Scripts/pip"
    } else {
        "${projectDir}/venv/bin/pip"
    }

    commandLine(pipExecutable, "install", "-r", "requirements.txt")
    workingDir = projectDir

    inputs.file("requirements.txt")
    outputs.dir("venv/lib")

    doLast {
        println("Python dependencies installed")
    }
}

// Task to configure CMake build
tasks.register<Exec>("configureCMake") {
    group = "cpp"
    description = "Configure CMake build system"

    val cmakeBuildDir = File(buildDir, "cmake")
    cmakeBuildDir.mkdirs()

    commandLine("cmake", "-S", ".", "-B", cmakeBuildDir.absolutePath, "-DCMAKE_BUILD_TYPE=${cmakeBuildType}")
    workingDir = projectDir

    inputs.file("CMakeLists.txt")
    inputs.dir("src/cpp")
    outputs.dir(cmakeBuildDir)

    doLast {
        println("CMake configured in: ${cmakeBuildDir}")
    }
}

// Task to build C++ extension module
tasks.register<Exec>("buildCppExtension") {
    group = "cpp"
    description = "Build C++ Python extension module using CMake"
    dependsOn("configureCMake")

    val cmakeBuildDir = File(buildDir, "cmake")

    commandLine("cmake", "--build", cmakeBuildDir.absolutePath, "--config", cmakeBuildType)
    workingDir = projectDir

    inputs.dir("src/cpp")
    inputs.file("CMakeLists.txt")
    outputs.file("src/_hardware_backend.so")
    outputs.file("src/_hardware_backend.pyd")

    doLast {
        println("C++ extension module built successfully")
    }
}

// Task to install the C++ extension module
tasks.register<Exec>("installCppExtension") {
    group = "cpp"
    description = "Install C++ Python extension module"
    dependsOn("buildCppExtension")

    val cmakeBuildDir = File(buildDir, "cmake")

    commandLine("cmake", "--install", cmakeBuildDir.absolutePath)
    workingDir = projectDir

    doLast {
        println("C++ extension module installed to src/")
    }
}

// Task to run Python tests
tasks.register<Exec>("testPython") {
    group = "verification"
    description = "Run Python tests"
    dependsOn("installPythonDeps", "installCppExtension")

    val pythonExecutableInVenv = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "${projectDir}/venv/Scripts/python"
    } else {
        "${projectDir}/venv/bin/python"
    }

    commandLine(pythonExecutableInVenv, "-m", "pytest", "tests/", "-v")
    workingDir = projectDir

    inputs.dir("tests")
    inputs.dir("src")

    doLast {
        println("Python tests completed")
    }
}

// Task to clean build artifacts
tasks.register<Delete>("cleanAll") {
    group = "build"
    description = "Clean all build artifacts including Python cache and CMake build"

    delete(buildDir)
    delete("venv")
    delete(fileTree("src") { include("**/*.so", "**/*.pyd") })
    delete(fileTree(".") { include("**/__pycache__", "**/*.pyc") })

    doLast {
        println("All build artifacts cleaned")
    }
}

// Configure the main build task
tasks.named("build") {
    dependsOn("installPythonDeps", "installCppExtension")

    doLast {
        println("Python+C++ project build completed successfully")
    }
}

// Test task
tasks.register("test") {
    group = "verification"
    description = "Run all tests"
    dependsOn("testPython")
}

// Clean task
tasks.named("clean") {
    dependsOn("cleanAll")
}
