// platforms/pc/build.gradle.kts

plugins {
    `cpp-library`
    // Note: A direct `python-ext` plugin might not exist in standard Gradle.
    // This configuration shows the *intent*. You would typically achieve this
    // with custom tasks to invoke CMake with pybind11, or use a
    // community plugin that handles Python C-extensions.
}

library {
    // Define where the C++ source files are located
    source.from("src/cpp/main")
    // Define where the public headers are located
    publicHeaders.from("src/cpp/include")

    // Define the target machines (e.g., Windows, Linux, macOS)
    targetMachines.add(machines.windows.x86_64)
    targetMachines.add(machines.linux.x86_64)
    targetMachines.add(machines.macOS.x86_64)
}

// Custom task to build Python extension with pybind11
// This is a conceptual representation that would need to be adapted
// based on the actual build system setup
tasks.register("buildPythonExtension") {
    group = "build"
    description = "Build Python extension module with pybind11"
    
    doLast {
        println("Building Python extension module '_hardware_backend'")
        // This would typically invoke CMake or a similar build system
        // to compile the C++ code with pybind11 bindings
    }
}

// Make the Python extension build depend on the C++ library compilation
tasks.named("build") {
    dependsOn("buildPythonExtension")
}