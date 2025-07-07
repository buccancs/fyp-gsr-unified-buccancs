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

/**
 * High-precision C++ backend for Shimmer sensor communication.
 * 
 * This class provides direct communication with the Shimmer sensor via serial port,
 * applying high-precision timestamps at the moment of data capture to minimize
 * timing jitter compared to Python-based implementations.
 */
class NativeShimmer {
public:
    /**
     * Data structure to hold timestamped sensor data.
     */
    struct TimestampedData {
        std::chrono::steady_clock::time_point timestamp;
        std::vector<uint8_t> data_packet;
        
        TimestampedData(const std::chrono::steady_clock::time_point& ts, 
                       const std::vector<uint8_t>& data)
            : timestamp(ts), data_packet(data) {}
    };

private:
    std::string com_port_;
    std::atomic<bool> is_running_;
    std::atomic<bool> is_connected_;
    
    // Thread-safe queue for timestamped data
    std::queue<TimestampedData> data_queue_;
    std::mutex queue_mutex_;
    std::condition_variable queue_condition_;
    
    // Worker thread for continuous data polling
    std::unique_ptr<std::thread> polling_thread_;
    
    // Serial port handle (platform-specific implementation needed)
    void* serial_handle_;
    
    /**
     * Main polling loop that runs in a dedicated thread.
     * Continuously reads from serial port and timestamps data immediately.
     */
    void pollingLoop();
    
    /**
     * Initialize serial port connection.
     * @param port_name COM port name (e.g., "COM3" on Windows, "/dev/ttyUSB0" on Linux)
     * @return true if successful, false otherwise
     */
    bool initializeSerialPort(const std::string& port_name);
    
    /**
     * Close serial port connection.
     */
    void closeSerialPort();
    
    /**
     * Read data packet from serial port.
     * @param buffer Buffer to store read data
     * @param max_size Maximum bytes to read
     * @return Number of bytes actually read, -1 on error
     */
    int readSerialData(uint8_t* buffer, size_t max_size);

public:
    /**
     * Constructor.
     * @param com_port COM port name for the Shimmer device
     */
    explicit NativeShimmer(const std::string& com_port);
    
    /**
     * Destructor. Ensures proper cleanup of resources.
     */
    ~NativeShimmer();
    
    /**
     * Start the high-precision data collection.
     * Creates a dedicated thread for continuous serial port polling.
     * @return true if started successfully, false otherwise
     */
    bool start();
    
    /**
     * Stop the data collection and cleanup resources.
     */
    void stop();
    
    /**
     * Non-blocking method to retrieve collected data.
     * @return Vector of timestamped data packets. Empty if no data available.
     */
    std::vector<TimestampedData> getData();
    
    /**
     * Check if the sensor is currently connected.
     * @return true if connected, false otherwise
     */
    bool isConnected() const;
    
    /**
     * Check if data collection is currently running.
     * @return true if running, false otherwise
     */
    bool isRunning() const;
    
    /**
     * Get the COM port being used.
     * @return COM port string
     */
    const std::string& getComPort() const;
};