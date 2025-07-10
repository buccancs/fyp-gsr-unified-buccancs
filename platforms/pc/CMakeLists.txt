cmake_minimum_required(VERSION 3.12)
project(pc_hardware_backend)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_package(OpenCV QUIET)
if(NOT OpenCV_FOUND)
    message(WARNING "OpenCV not found. Building without OpenCV support.")
    set(OPENCV_AVAILABLE FALSE)
else()
    message(STATUS "OpenCV found: ${OpenCV_VERSION}")
    set(OPENCV_AVAILABLE TRUE)
endif()

# Find pybind11 (installed via pip)
find_package(pybind11 QUIET)
if(NOT pybind11_FOUND)
    message(WARNING "pybind11 not found via find_package. Trying to use pybind11 from Python installation.")
    # Try to find pybind11 using Python
    set(PYTHON_EXECUTABLE "${CMAKE_CURRENT_SOURCE_DIR}/../../environments/pc/windows/python/python.exe")
    execute_process(
        COMMAND "${PYTHON_EXECUTABLE}" -c "import pybind11; print(pybind11.get_cmake_dir())"
        OUTPUT_VARIABLE PYBIND11_CMAKE_DIR
        OUTPUT_STRIP_TRAILING_WHITESPACE
        RESULT_VARIABLE PYBIND11_RESULT
    )
    if(PYBIND11_RESULT EQUAL 0)
        set(pybind11_DIR ${PYBIND11_CMAKE_DIR})
        find_package(pybind11 REQUIRED)
        message(STATUS "Found pybind11 via Python: ${PYBIND11_CMAKE_DIR}")
    else()
        message(FATAL_ERROR "pybind11 not found. Please install pybind11.")
    endif()
else()
    message(STATUS "Found pybind11: ${pybind11_VERSION}")
endif()

# Include directories
include_directories(src/cpp/include)
if(OPENCV_AVAILABLE)
    include_directories(${OpenCV_INCLUDE_DIRS})
endif()

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
if(OPENCV_AVAILABLE)
    target_link_libraries(_hardware_backend PRIVATE ${OpenCV_LIBS})
endif()

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
