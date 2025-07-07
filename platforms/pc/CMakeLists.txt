cmake_minimum_required(VERSION 3.12)
project(pc_hardware_backend)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(OpenCV REQUIRED)

# Add pybind11 subdirectory
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../third_party/pybind11 pybind11)

# Include directories
include_directories(src/cpp/include)
include_directories(${OpenCV_INCLUDE_DIRS})

# Define source files
set(SHIMMER_SOURCES
    src/cpp/main/NativeShimmer.cpp
)

set(WEBCAM_SOURCES
    src/cpp/main/NativeWebcam.cpp
)

set(BINDING_SOURCES
    src/cpp/main/bindings.cpp
)

# Create the Python extension module
pybind11_add_module(_hardware_backend 
    ${SHIMMER_SOURCES}
    ${WEBCAM_SOURCES}
    ${BINDING_SOURCES}
)

# Link libraries
target_link_libraries(_hardware_backend PRIVATE ${OpenCV_LIBS})

# Platform-specific libraries for serial communication
if(WIN32)
    # Windows: no additional libraries needed (using Win32 API)
elseif(UNIX)
    # Linux/macOS: no additional libraries needed (using POSIX)
endif()

# Compiler-specific options
target_compile_definitions(_hardware_backend PRIVATE VERSION_INFO=${EXAMPLE_VERSION_INFO})

# Set properties for the Python module
set_target_properties(_hardware_backend PROPERTIES
    CXX_VISIBILITY_PRESET "hidden"
    VISIBILITY_INLINES_HIDDEN ON
)

# Install the module to the Python source directory
install(TARGETS _hardware_backend
    DESTINATION ${CMAKE_CURRENT_SOURCE_DIR}/src
)

# Custom target to build and install in one step
add_custom_target(install_module
    COMMAND ${CMAKE_COMMAND} --build . --target _hardware_backend
    COMMAND ${CMAKE_COMMAND} --install . --component _hardware_backend
    DEPENDS _hardware_backend
    COMMENT "Building and installing Python hardware backend module"
)