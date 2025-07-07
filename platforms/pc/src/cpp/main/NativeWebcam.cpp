#include "NativeWebcam.h"
#include <iostream>
#include <opencv2/opencv.hpp>

NativeWebcam::NativeWebcam(int camera_index)
    : camera_index_(camera_index)
    , is_running_(false)
    , is_connected_(false)
    , frame_width_(640)
    , frame_height_(480)
    , video_capture_(nullptr)
{
}

NativeWebcam::~NativeWebcam() {
    stop();
}

bool NativeWebcam::start() {
    if (is_running_.load()) {
        return true; // Already running
    }
    
    if (!initializeCamera(camera_index_)) {
        std::cerr << "Failed to initialize camera: " << camera_index_ << std::endl;
        return false;
    }
    
    is_connected_.store(true);
    is_running_.store(true);
    
    // Start the capture thread
    capture_thread_ = std::make_unique<std::thread>(&NativeWebcam::captureLoop, this);
    
    std::cout << "NativeWebcam started on camera index: " << camera_index_ << std::endl;
    return true;
}

void NativeWebcam::stop() {
    if (!is_running_.load()) {
        return; // Already stopped
    }
    
    is_running_.store(false);
    
    // Wake up the capture thread if it's waiting
    queue_condition_.notify_all();
    
    // Wait for the capture thread to finish
    if (capture_thread_ && capture_thread_->joinable()) {
        capture_thread_->join();
    }
    
    closeCamera();
    is_connected_.store(false);
    
    std::cout << "NativeWebcam stopped" << std::endl;
}

std::vector<NativeWebcam::TimestampedFrame> NativeWebcam::getData() {
    std::lock_guard<std::mutex> lock(queue_mutex_);
    std::vector<TimestampedFrame> result;
    
    // Move all available frames from queue to result vector
    while (!frame_queue_.empty()) {
        result.push_back(std::move(frame_queue_.front()));
        frame_queue_.pop();
    }
    
    return result;
}

bool NativeWebcam::isConnected() const {
    return is_connected_.load();
}

bool NativeWebcam::isRunning() const {
    return is_running_.load();
}

int NativeWebcam::getCameraIndex() const {
    return camera_index_;
}

bool NativeWebcam::setResolution(int width, int height) {
    if (is_running_.load()) {
        std::cerr << "Cannot change resolution while camera is running" << std::endl;
        return false;
    }
    
    frame_width_ = width;
    frame_height_ = height;
    return true;
}

int NativeWebcam::getFrameWidth() const {
    return frame_width_;
}

int NativeWebcam::getFrameHeight() const {
    return frame_height_;
}

std::string NativeWebcam::getCameraInfo() const {
    if (!video_capture_) {
        return "Camera not initialized";
    }
    
    cv::VideoCapture* cap = static_cast<cv::VideoCapture*>(video_capture_);
    
    std::string info = "Camera " + std::to_string(camera_index_) + ": ";
    info += std::to_string(static_cast<int>(cap->get(cv::CAP_PROP_FRAME_WIDTH))) + "x";
    info += std::to_string(static_cast<int>(cap->get(cv::CAP_PROP_FRAME_HEIGHT)));
    info += " @ " + std::to_string(cap->get(cv::CAP_PROP_FPS)) + " FPS";
    
    return info;
}

void NativeWebcam::captureLoop() {
    cv::VideoCapture* cap = static_cast<cv::VideoCapture*>(video_capture_);
    
    while (is_running_.load()) {
        auto frame = std::make_shared<cv::Mat>();
        
        // Capture frame from camera
        bool success = cap->read(*frame);
        
        if (success && !frame->empty()) {
            // Immediately timestamp the frame upon successful capture
            auto timestamp = std::chrono::steady_clock::now();
            
            // Add to thread-safe queue
            {
                std::lock_guard<std::mutex> lock(queue_mutex_);
                frame_queue_.emplace(timestamp, frame);
                
                // Limit queue size to prevent memory issues
                const size_t MAX_QUEUE_SIZE = 30; // ~1 second at 30 FPS
                while (frame_queue_.size() > MAX_QUEUE_SIZE) {
                    frame_queue_.pop();
                }
            }
            
            // Notify waiting threads
            queue_condition_.notify_one();
        } else {
            // No frame available or error, small delay to prevent busy waiting
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }
    }
}

bool NativeWebcam::initializeCamera(int camera_index) {
    try {
        cv::VideoCapture* cap = new cv::VideoCapture(camera_index);
        
        if (!cap->isOpened()) {
            delete cap;
            return false;
        }
        
        video_capture_ = cap;
        
        // Configure camera settings
        if (!configureCameraSettings()) {
            closeCamera();
            return false;
        }
        
        return true;
    } catch (const std::exception& e) {
        std::cerr << "Exception initializing camera: " << e.what() << std::endl;
        return false;
    }
}

void NativeWebcam::closeCamera() {
    if (video_capture_) {
        cv::VideoCapture* cap = static_cast<cv::VideoCapture*>(video_capture_);
        cap->release();
        delete cap;
        video_capture_ = nullptr;
    }
}

bool NativeWebcam::configureCameraSettings() {
    if (!video_capture_) {
        return false;
    }
    
    cv::VideoCapture* cap = static_cast<cv::VideoCapture*>(video_capture_);
    
    try {
        // Set resolution
        cap->set(cv::CAP_PROP_FRAME_WIDTH, frame_width_);
        cap->set(cv::CAP_PROP_FRAME_HEIGHT, frame_height_);
        
        // Set FPS (try for 30 FPS, but camera may not support it)
        cap->set(cv::CAP_PROP_FPS, 30.0);
        
        // Set buffer size to minimize latency
        cap->set(cv::CAP_PROP_BUFFERSIZE, 1);
        
        // Verify settings were applied
        int actual_width = static_cast<int>(cap->get(cv::CAP_PROP_FRAME_WIDTH));
        int actual_height = static_cast<int>(cap->get(cv::CAP_PROP_FRAME_HEIGHT));
        double actual_fps = cap->get(cv::CAP_PROP_FPS);
        
        std::cout << "Camera configured: " << actual_width << "x" << actual_height 
                  << " @ " << actual_fps << " FPS" << std::endl;
        
        // Update internal resolution values with actual values
        frame_width_ = actual_width;
        frame_height_ = actual_height;
        
        return true;
    } catch (const std::exception& e) {
        std::cerr << "Exception configuring camera: " << e.what() << std::endl;
        return false;
    }
}