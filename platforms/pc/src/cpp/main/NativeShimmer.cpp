#include "NativeShimmer.h"
#include <iostream>
#include <cstring>

// Platform-specific includes for serial communication
#ifdef _WIN32
    #include <windows.h>
#else
    #include <fcntl.h>
    #include <termios.h>
    #include <unistd.h>
#endif

NativeShimmer::NativeShimmer(const std::string& com_port)
    : com_port_(com_port)
    , is_running_(false)
    , is_connected_(false)
    , serial_handle_(nullptr)
{
}

NativeShimmer::~NativeShimmer() {
    stop();
}

bool NativeShimmer::start() {
    if (is_running_.load()) {
        return true; // Already running
    }
    
    if (!initializeSerialPort(com_port_)) {
        std::cerr << "Failed to initialize serial port: " << com_port_ << std::endl;
        return false;
    }
    
    is_connected_.store(true);
    is_running_.store(true);
    
    // Start the polling thread
    polling_thread_ = std::make_unique<std::thread>(&NativeShimmer::pollingLoop, this);
    
    std::cout << "NativeShimmer started on port: " << com_port_ << std::endl;
    return true;
}

void NativeShimmer::stop() {
    if (!is_running_.load()) {
        return; // Already stopped
    }
    
    is_running_.store(false);
    
    // Wake up the polling thread if it's waiting
    queue_condition_.notify_all();
    
    // Wait for the polling thread to finish
    if (polling_thread_ && polling_thread_->joinable()) {
        polling_thread_->join();
    }
    
    closeSerialPort();
    is_connected_.store(false);
    
    std::cout << "NativeShimmer stopped" << std::endl;
}

std::vector<NativeShimmer::TimestampedData> NativeShimmer::getData() {
    std::lock_guard<std::mutex> lock(queue_mutex_);
    std::vector<TimestampedData> result;
    
    // Move all available data from queue to result vector
    while (!data_queue_.empty()) {
        result.push_back(std::move(data_queue_.front()));
        data_queue_.pop();
    }
    
    return result;
}

bool NativeShimmer::isConnected() const {
    return is_connected_.load();
}

bool NativeShimmer::isRunning() const {
    return is_running_.load();
}

const std::string& NativeShimmer::getComPort() const {
    return com_port_;
}

void NativeShimmer::pollingLoop() {
    const size_t BUFFER_SIZE = 1024;
    uint8_t buffer[BUFFER_SIZE];
    
    while (is_running_.load()) {
        int bytes_read = readSerialData(buffer, BUFFER_SIZE);
        
        if (bytes_read > 0) {
            // Immediately timestamp the data upon successful read
            auto timestamp = std::chrono::steady_clock::now();
            
            // Create data packet
            std::vector<uint8_t> data_packet(buffer, buffer + bytes_read);
            
            // Add to thread-safe queue
            {
                std::lock_guard<std::mutex> lock(queue_mutex_);
                data_queue_.emplace(timestamp, data_packet);
            }
            
            // Notify waiting threads
            queue_condition_.notify_one();
        } else if (bytes_read == 0) {
            // No data available, small delay to prevent busy waiting
            std::this_thread::sleep_for(std::chrono::milliseconds(1));
        } else {
            // Error occurred
            std::cerr << "Error reading from serial port" << std::endl;
            break;
        }
    }
}

bool NativeShimmer::initializeSerialPort(const std::string& port_name) {
#ifdef _WIN32
    // Windows implementation
    HANDLE hSerial = CreateFileA(port_name.c_str(),
                                GENERIC_READ | GENERIC_WRITE,
                                0,
                                NULL,
                                OPEN_EXISTING,
                                FILE_ATTRIBUTE_NORMAL,
                                NULL);
    
    if (hSerial == INVALID_HANDLE_VALUE) {
        return false;
    }
    
    DCB dcbSerialParams = {0};
    dcbSerialParams.DCBlength = sizeof(dcbSerialParams);
    
    if (!GetCommState(hSerial, &dcbSerialParams)) {
        CloseHandle(hSerial);
        return false;
    }
    
    // Configure serial port settings (adjust as needed for Shimmer)
    dcbSerialParams.BaudRate = CBR_115200;
    dcbSerialParams.ByteSize = 8;
    dcbSerialParams.StopBits = ONESTOPBIT;
    dcbSerialParams.Parity = NOPARITY;
    
    if (!SetCommState(hSerial, &dcbSerialParams)) {
        CloseHandle(hSerial);
        return false;
    }
    
    // Set timeouts
    COMMTIMEOUTS timeouts = {0};
    timeouts.ReadIntervalTimeout = 50;
    timeouts.ReadTotalTimeoutConstant = 50;
    timeouts.ReadTotalTimeoutMultiplier = 10;
    timeouts.WriteTotalTimeoutConstant = 50;
    timeouts.WriteTotalTimeoutMultiplier = 10;
    
    if (!SetCommTimeouts(hSerial, &timeouts)) {
        CloseHandle(hSerial);
        return false;
    }
    
    serial_handle_ = hSerial;
    return true;
    
#else
    // Linux/macOS implementation
    int fd = open(port_name.c_str(), O_RDWR | O_NOCTTY | O_NONBLOCK);
    if (fd == -1) {
        return false;
    }
    
    struct termios tty;
    if (tcgetattr(fd, &tty) != 0) {
        close(fd);
        return false;
    }
    
    // Configure serial port settings
    cfsetospeed(&tty, B115200);
    cfsetispeed(&tty, B115200);
    
    tty.c_cflag &= ~PARENB; // No parity
    tty.c_cflag &= ~CSTOPB; // One stop bit
    tty.c_cflag &= ~CSIZE;
    tty.c_cflag |= CS8;     // 8 data bits
    tty.c_cflag &= ~CRTSCTS; // No hardware flow control
    tty.c_cflag |= CREAD | CLOCAL; // Enable reading
    
    tty.c_lflag &= ~ICANON; // Non-canonical mode
    tty.c_lflag &= ~ECHO;   // No echo
    tty.c_lflag &= ~ECHOE;
    tty.c_lflag &= ~ECHONL;
    tty.c_lflag &= ~ISIG;
    
    tty.c_iflag &= ~(IXON | IXOFF | IXANY); // No software flow control
    tty.c_iflag &= ~(IGNBRK | BRKINT | PARMRK | ISTRIP | INLCR | IGNCR | ICRNL);
    
    tty.c_oflag &= ~OPOST; // No output processing
    tty.c_oflag &= ~ONLCR;
    
    tty.c_cc[VTIME] = 1;    // Wait for up to 0.1s
    tty.c_cc[VMIN] = 0;     // No minimum number of characters
    
    if (tcsetattr(fd, TCSANOW, &tty) != 0) {
        close(fd);
        return false;
    }
    
    serial_handle_ = reinterpret_cast<void*>(static_cast<intptr_t>(fd));
    return true;
#endif
}

void NativeShimmer::closeSerialPort() {
    if (serial_handle_ == nullptr) {
        return;
    }
    
#ifdef _WIN32
    CloseHandle(static_cast<HANDLE>(serial_handle_));
#else
    close(static_cast<int>(reinterpret_cast<intptr_t>(serial_handle_)));
#endif
    
    serial_handle_ = nullptr;
}

int NativeShimmer::readSerialData(uint8_t* buffer, size_t max_size) {
    if (serial_handle_ == nullptr) {
        return -1;
    }
    
#ifdef _WIN32
    DWORD bytes_read = 0;
    if (ReadFile(static_cast<HANDLE>(serial_handle_), buffer, static_cast<DWORD>(max_size), &bytes_read, NULL)) {
        return static_cast<int>(bytes_read);
    }
    return -1;
#else
    int fd = static_cast<int>(reinterpret_cast<intptr_t>(serial_handle_));
    ssize_t bytes_read = read(fd, buffer, max_size);
    return static_cast<int>(bytes_read);
#endif
}