#pragma once

#include <string>
#include <vector>
#include <queue>
#include <mutex>
#include <condition_variable>
#include <thread>
#include <atomic>
#include <chrono>
#include <memory>

// Forward declaration for OpenCV Mat to avoid including opencv headers in header file
namespace cv {
    class Mat;
}

/**
 * High-precision C++ backend for webcam capture.
 * 
 * This class provides direct communication with webcam devices using OpenCV,
 * applying high-precision timestamps at the moment of frame capture to minimize
 * timing jitter compared to Python-based implementations.
 */
class NativeWebcam {
public:
    /**
     * Data structure to hold timestamped frame data.
     */
    struct TimestampedFrame {
        std::chrono::steady_clock::time_point timestamp;
        std::shared_ptr<cv::Mat> frame;
        
        TimestampedFrame(const std::chrono::steady_clock::time_point& ts, 
                        std::shared_ptr<cv::Mat> frame_data)
            : timestamp(ts), frame(frame_data) {}
    };

private:
    int camera_index_;
    std::atomic<bool> is_running_;
    std::atomic<bool> is_connected_;
    
    // Camera resolution settings
    int frame_width_;
    int frame_height_;
    
    // Thread-safe queue for timestamped frames
    std::queue<TimestampedFrame> frame_queue_;
    std::mutex queue_mutex_;
    std::condition_variable queue_condition_;
    
    // Worker thread for continuous frame capture
    std::unique_ptr<std::thread> capture_thread_;
    
    // OpenCV VideoCapture object (using void* to avoid header dependency)
    void* video_capture_;
    
    /**
     * Main capture loop that runs in a dedicated thread.
     * Continuously captures frames and timestamps them immediately.
     */
    void captureLoop();
    
    /**
     * Initialize camera connection.
     * @param camera_index Camera device index (0 for default camera)
     * @return true if successful, false otherwise
     */
    bool initializeCamera(int camera_index);
    
    /**
     * Close camera connection.
     */
    void closeCamera();
    
    /**
     * Configure camera settings (resolution, etc.).
     * @return true if successful, false otherwise
     */
    bool configureCameraSettings();

public:
    /**
     * Constructor.
     * @param camera_index Camera device index (0 for default camera)
     */
    explicit NativeWebcam(int camera_index = 0);
    
    /**
     * Destructor. Ensures proper cleanup of resources.
     */
    ~NativeWebcam();
    
    /**
     * Start the high-precision frame capture.
     * Creates a dedicated thread for continuous camera polling.
     * @return true if started successfully, false otherwise
     */
    bool start();
    
    /**
     * Stop the frame capture and cleanup resources.
     */
    void stop();
    
    /**
     * Non-blocking method to retrieve captured frames.
     * @return Vector of timestamped frames. Empty if no frames available.
     */
    std::vector<TimestampedFrame> getData();
    
    /**
     * Check if the camera is currently connected.
     * @return true if connected, false otherwise
     */
    bool isConnected() const;
    
    /**
     * Check if frame capture is currently running.
     * @return true if running, false otherwise
     */
    bool isRunning() const;
    
    /**
     * Get the camera index being used.
     * @return Camera index
     */
    int getCameraIndex() const;
    
    /**
     * Set the camera resolution.
     * Must be called before start() to take effect.
     * @param width Frame width in pixels
     * @param height Frame height in pixels
     * @return true if successful, false otherwise
     */
    bool setResolution(int width, int height);
    
    /**
     * Get current frame width.
     * @return Frame width in pixels
     */
    int getFrameWidth() const;
    
    /**
     * Get current frame height.
     * @return Frame height in pixels
     */
    int getFrameHeight() const;
    
    /**
     * Get camera information (resolution, FPS, etc.).
     * @return String containing camera information
     */
    std::string getCameraInfo() const;
};