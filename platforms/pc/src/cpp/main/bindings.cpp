#include <pybind11/pybind11.h>
#include <pybind11/stl.h>
#include <pybind11/chrono.h>
#include <pybind11/numpy.h>
#include <opencv2/opencv.hpp>
#include "NativeShimmer.h"
#include "NativeWebcam.h"

namespace py = pybind11;

// Helper function to convert cv::Mat to numpy array
py::array_t<uint8_t> mat_to_numpy(const cv::Mat& mat) {
    return py::array_t<uint8_t>(
        {mat.rows, mat.cols, mat.channels()},
        {sizeof(uint8_t) * mat.cols * mat.channels(), sizeof(uint8_t) * mat.channels(), sizeof(uint8_t)},
        mat.data
    );
}

PYBIND11_MODULE(_hardware_backend, m) {
    m.doc() = "High-performance C++ hardware backend for Shimmer sensor and webcam";

    // Bind NativeShimmer::TimestampedData
    py::class_<NativeShimmer::TimestampedData>(m, "ShimmerTimestampedData")
        .def_readonly("timestamp", &NativeShimmer::TimestampedData::timestamp)
        .def_readonly("data_packet", &NativeShimmer::TimestampedData::data_packet)
        .def("__repr__", [](const NativeShimmer::TimestampedData& data) {
            return "<ShimmerTimestampedData: " + std::to_string(data.data_packet.size()) + " bytes>";
        });

    // Bind NativeShimmer class
    py::class_<NativeShimmer>(m, "NativeShimmer")
        .def(py::init<const std::string&>(), 
             "Constructor that takes COM port name",
             py::arg("com_port"))
        .def("start", &NativeShimmer::start,
             "Start high-precision data collection")
        .def("stop", &NativeShimmer::stop,
             "Stop data collection and cleanup resources")
        .def("get_data", &NativeShimmer::getData,
             "Non-blocking method to retrieve collected data")
        .def("is_connected", &NativeShimmer::isConnected,
             "Check if sensor is currently connected")
        .def("is_running", &NativeShimmer::isRunning,
             "Check if data collection is currently running")
        .def("get_com_port", &NativeShimmer::getComPort,
             "Get the COM port being used")
        .def("__repr__", [](const NativeShimmer& shimmer) {
            return "<NativeShimmer: port=" + shimmer.getComPort() + 
                   ", connected=" + (shimmer.isConnected() ? "true" : "false") +
                   ", running=" + (shimmer.isRunning() ? "true" : "false") + ">";
        });

    // Bind NativeWebcam::TimestampedFrame
    py::class_<NativeWebcam::TimestampedFrame>(m, "WebcamTimestampedFrame")
        .def_readonly("timestamp", &NativeWebcam::TimestampedFrame::timestamp)
        .def_property_readonly("frame", [](const NativeWebcam::TimestampedFrame& frame) {
            if (frame.frame && !frame.frame->empty()) {
                return mat_to_numpy(*frame.frame);
            }
            return py::array_t<uint8_t>();
        })
        .def("__repr__", [](const NativeWebcam::TimestampedFrame& frame) {
            if (frame.frame && !frame.frame->empty()) {
                return "<WebcamTimestampedFrame: " + 
                       std::to_string(frame.frame->rows) + "x" + 
                       std::to_string(frame.frame->cols) + ">";
            }
            return std::string("<WebcamTimestampedFrame: empty>");
        });

    // Bind NativeWebcam class
    py::class_<NativeWebcam>(m, "NativeWebcam")
        .def(py::init<int>(), 
             "Constructor that takes camera index",
             py::arg("camera_index") = 0)
        .def("start", &NativeWebcam::start,
             "Start high-precision frame capture")
        .def("stop", &NativeWebcam::stop,
             "Stop frame capture and cleanup resources")
        .def("get_data", &NativeWebcam::getData,
             "Non-blocking method to retrieve captured frames")
        .def("is_connected", &NativeWebcam::isConnected,
             "Check if camera is currently connected")
        .def("is_running", &NativeWebcam::isRunning,
             "Check if frame capture is currently running")
        .def("get_camera_index", &NativeWebcam::getCameraIndex,
             "Get the camera index being used")
        .def("set_resolution", &NativeWebcam::setResolution,
             "Set camera resolution (must be called before start)",
             py::arg("width"), py::arg("height"))
        .def("get_frame_width", &NativeWebcam::getFrameWidth,
             "Get current frame width")
        .def("get_frame_height", &NativeWebcam::getFrameHeight,
             "Get current frame height")
        .def("get_camera_info", &NativeWebcam::getCameraInfo,
             "Get camera information string")
        .def("__repr__", [](const NativeWebcam& webcam) {
            return "<NativeWebcam: index=" + std::to_string(webcam.getCameraIndex()) + 
                   ", connected=" + (webcam.isConnected() ? "true" : "false") +
                   ", running=" + (webcam.isRunning() ? "true" : "false") + 
                   ", resolution=" + std::to_string(webcam.getFrameWidth()) + "x" + 
                   std::to_string(webcam.getFrameHeight()) + ">";
        });

    // Module-level functions for utility
    m.def("get_version", []() {
        return "1.0.0";
    }, "Get the version of the hardware backend");

    m.def("test_connection", []() {
        return "Hardware backend loaded successfully";
    }, "Test function to verify module loading");

    // Add module attributes
    m.attr("__version__") = "1.0.0";
    m.attr("__author__") = "GSR-Unified Project";
}
