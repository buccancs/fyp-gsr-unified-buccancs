FROM ubuntu:22.04

# Install system dependencies
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    cmake \
    build-essential \
    libopencv-dev \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

# Create application user
RUN useradd -r -s /bin/false -d /app hardware

# Set working directory
WORKDIR /app

# Copy application files
COPY src/ ./src/
COPY *.py ./
COPY CMakeLists.txt ./
COPY requirements.txt ./

# Install Python dependencies
RUN pip3 install -r requirements.txt

# Build C++ backend
RUN mkdir build && cd build && cmake .. && make
RUN cp build/_hardware_backend*.so src/

# Set permissions
RUN chown -R hardware:hardware /app

# Create data directories
RUN mkdir -p /var/log/hardware-backend /var/lib/hardware-backend
RUN chown hardware:hardware /var/log/hardware-backend /var/lib/hardware-backend

# Switch to application user
USER hardware

# Set environment
ENV HARDWARE_ENV=production
ENV PYTHONPATH=/app/src

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python3 -c "import _hardware_backend; print('OK')" || exit 1

# Run application
CMD ["python3", "integration_example.py"]
