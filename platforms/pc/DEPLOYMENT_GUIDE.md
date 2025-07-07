# Hardware Backend Deployment Guide

## Overview
This guide covers the deployment of the high-precision C++ hardware backend system in production environments.

## System Requirements

### Hardware Requirements
- CPU: Multi-core processor (Intel/AMD x64)
- RAM: Minimum 4GB, recommended 8GB+
- Storage: 10GB available space
- USB ports for Shimmer device and webcam

### Software Requirements
- Operating System: Ubuntu 20.04+ / CentOS 8+ / Windows 10+
- Python 3.8+
- CMake 3.12+
- OpenCV 4.0+
- C++ compiler (GCC 9+ / Clang 10+ / MSVC 2019+)

## Installation Methods

### Method 1: Package Installation (Recommended)
```bash
# Extract package
tar -xzf hardware-backend-package.tar.gz
cd hardware-backend-package

# Run installation script
sudo ./install.sh

# Configure system
sudo nano /etc/hardware-backend/production.json

# Start service
sudo systemctl start hardware-backend
sudo systemctl status hardware-backend
```

### Method 2: Docker Deployment
```bash
# Build and run with Docker Compose
docker-compose up -d

# Check status
docker-compose ps
docker-compose logs hardware-backend
```

### Method 3: Manual Installation
```bash
# Install dependencies
sudo apt-get update
sudo apt-get install python3 python3-pip cmake build-essential libopencv-dev

# Clone repository
git clone <repository-url>
cd hardware-backend

# Build C++ backend
mkdir build && cd build
cmake .. && make

# Install Python dependencies
pip3 install -r requirements.txt

# Run system
python3 integration_example.py
```

## Configuration

### Environment Configuration
Set the environment variable to select configuration:
```bash
export HARDWARE_ENV=production  # or development, testing
```

### Hardware Configuration
Edit `/etc/hardware-backend/production.json`:
```json
{
  "hardware": {
    "shimmer_com_port": "/dev/ttyUSB0",
    "webcam_index": 0,
    "webcam_width": 3840,
    "webcam_height": 2160
  },
  "performance": {
    "max_latency_ms": 0.5,
    "max_jitter_ms": 0.3
  }
}
```

## Service Management

### SystemD Service (Linux)
```bash
# Start/stop service
sudo systemctl start hardware-backend
sudo systemctl stop hardware-backend

# Enable/disable auto-start
sudo systemctl enable hardware-backend
sudo systemctl disable hardware-backend

# View logs
sudo journalctl -u hardware-backend -f
```

### Docker Service
```bash
# Start/stop containers
docker-compose up -d
docker-compose down

# View logs
docker-compose logs -f hardware-backend

# Update deployment
docker-compose pull
docker-compose up -d
```

## Monitoring and Maintenance

### Performance Monitoring
- Access Grafana dashboard: http://localhost:3000
- Default credentials: admin/admin
- Monitor latency, jitter, and system health

### Log Management
- System logs: `/var/log/hardware-backend/`
- Service logs: `journalctl -u hardware-backend`
- Docker logs: `docker-compose logs`

### Backup and Recovery
```bash
# Backup configuration
sudo cp -r /etc/hardware-backend /backup/config-$(date +%Y%m%d)

# Backup data
sudo cp -r /var/lib/hardware-backend /backup/data-$(date +%Y%m%d)
```

## Troubleshooting

### Common Issues

1. **Device Connection Failed**
   - Check device permissions: `ls -l /dev/ttyUSB*`
   - Add user to dialout group: `sudo usermod -a -G dialout hardware`

2. **High Latency Alerts**
   - Check system load: `top`, `htop`
   - Verify hardware connections
   - Review configuration thresholds

3. **Service Won't Start**
   - Check logs: `journalctl -u hardware-backend`
   - Verify permissions and file ownership
   - Test manual startup: `python3 integration_example.py`

### Performance Tuning

1. **CPU Affinity**
   ```bash
   # Pin service to specific CPU cores
   sudo systemctl edit hardware-backend
   # Add: ExecStart=taskset -c 0,1 /usr/bin/python3 ...
   ```

2. **Real-time Priority**
   ```bash
   # Set real-time scheduling (use with caution)
   sudo chrt -f 50 python3 integration_example.py
   ```

## Security Considerations

### Access Control
- Run service with dedicated user account
- Restrict file permissions (755 for directories, 644 for files)
- Use firewall to limit network access

### Device Security
- Secure physical access to hardware devices
- Monitor for unauthorized device connections
- Implement device authentication where possible

## Support and Maintenance

### Regular Maintenance
- Update system packages monthly
- Review and rotate logs weekly
- Monitor performance metrics daily
- Backup configuration and data weekly

### Updates and Upgrades
1. Stop the service
2. Backup current installation
3. Deploy new version
4. Test functionality
5. Start service

For technical support, contact: support@hardware-backend.com
