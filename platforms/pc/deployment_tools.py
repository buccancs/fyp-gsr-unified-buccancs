#!/usr/bin/env python3
"""
Production deployment tools for the high-precision C++ hardware backend.

This module provides tools for deploying the system in production environments,
including service management, installation scripts, and system configuration.
"""

import sys
import os
import subprocess
import shutil
from pathlib import Path
from typing import List, Dict, Optional
import json
import platform

class DeploymentManager:
    """
    Production deployment manager for the hardware system.
    
    Handles installation, configuration, and service management
    for production deployment.
    """
    
    def __init__(self, install_dir: str = "/opt/hardware-backend"):
        self.install_dir = Path(install_dir)
        self.system_type = platform.system().lower()
        self.service_name = "hardware-backend"
        
        # Deployment configuration
        self.config = {
            'install_dir': str(self.install_dir),
            'service_name': self.service_name,
            'user': 'hardware',
            'group': 'hardware',
            'log_dir': '/var/log/hardware-backend',
            'data_dir': '/var/lib/hardware-backend',
            'config_dir': '/etc/hardware-backend'
        }
        
    def check_system_requirements(self) -> Dict[str, bool]:
        """Check system requirements for deployment."""
        print("🔍 Checking System Requirements")
        print("-" * 40)
        
        requirements = {
            'python3': self._check_python(),
            'cmake': self._check_cmake(),
            'opencv': self._check_opencv(),
            'pybind11': self._check_pybind11(),
            'compiler': self._check_compiler(),
            'permissions': self._check_permissions()
        }
        
        for req, status in requirements.items():
            status_icon = "✅" if status else "❌"
            print(f"  {req}: {status_icon}")
        
        all_met = all(requirements.values())
        print(f"\nOverall: {'✅ All requirements met' if all_met else '❌ Some requirements missing'}")
        
        return requirements
    
    def _check_python(self) -> bool:
        """Check Python 3.8+ availability."""
        try:
            result = subprocess.run([sys.executable, '--version'], 
                                  capture_output=True, text=True)
            version = result.stdout.strip().split()[1]
            major, minor = map(int, version.split('.')[:2])
            return major >= 3 and minor >= 8
        except:
            return False
    
    def _check_cmake(self) -> bool:
        """Check CMake availability."""
        try:
            subprocess.run(['cmake', '--version'], 
                          capture_output=True, check=True)
            return True
        except:
            return False
    
    def _check_opencv(self) -> bool:
        """Check OpenCV availability."""
        try:
            import cv2
            return True
        except ImportError:
            return False
    
    def _check_pybind11(self) -> bool:
        """Check pybind11 availability."""
        try:
            import pybind11
            return True
        except ImportError:
            return False
    
    def _check_compiler(self) -> bool:
        """Check C++ compiler availability."""
        compilers = ['g++', 'clang++', 'cl.exe']
        for compiler in compilers:
            try:
                subprocess.run([compiler, '--version'], 
                              capture_output=True, check=True)
                return True
            except:
                continue
        return False
    
    def _check_permissions(self) -> bool:
        """Check if running with sufficient permissions."""
        if self.system_type == 'windows':
            return True  # Skip permission check on Windows
        return os.geteuid() == 0
    
    def create_installation_package(self, source_dir: str = ".") -> str:
        """Create installation package."""
        print("📦 Creating Installation Package")
        print("-" * 40)
        
        source_path = Path(source_dir)
        package_name = f"hardware-backend-{platform.machine()}.tar.gz"
        
        # Files to include in package
        include_files = [
            "src/cpp/include/*.h",
            "src/cpp/main/*.cpp",
            "src/hardware/*.py",
            "CMakeLists.txt",
            "requirements.txt",
            "config_manager.py",
            "benchmark_timing_precision.py",
            "real_time_monitor.py",
            "integration_example.py"
        ]
        
        # Create temporary package directory
        package_dir = Path("./package_temp")
        package_dir.mkdir(exist_ok=True)
        
        try:
            # Copy files
            for pattern in include_files:
                for file_path in source_path.glob(pattern):
                    if file_path.is_file():
                        dest_path = package_dir / file_path.relative_to(source_path)
                        dest_path.parent.mkdir(parents=True, exist_ok=True)
                        shutil.copy2(file_path, dest_path)
                        print(f"  Added: {file_path.relative_to(source_path)}")
            
            # Create installation script
            self._create_install_script(package_dir)
            
            # Create service files
            self._create_service_files(package_dir)
            
            # Create package
            shutil.make_archive("hardware-backend-package", 'gztar', package_dir)
            
            print(f"✅ Package created: hardware-backend-package.tar.gz")
            return "hardware-backend-package.tar.gz"
            
        finally:
            # Cleanup
            shutil.rmtree(package_dir, ignore_errors=True)
    
    def _create_install_script(self, package_dir: Path):
        """Create installation script."""
        install_script = package_dir / "install.sh"
        
        script_content = f"""#!/bin/bash
# Hardware Backend Installation Script

set -e

echo "🚀 Installing Hardware Backend System"
echo "======================================"

# Check if running as root
if [[ $EUID -ne 0 ]]; then
   echo "❌ This script must be run as root (use sudo)"
   exit 1
fi

# Create system user
if ! id "{self.config['user']}" &>/dev/null; then
    echo "👤 Creating system user: {self.config['user']}"
    useradd -r -s /bin/false -d {self.config['install_dir']} {self.config['user']}
fi

# Create directories
echo "📁 Creating directories..."
mkdir -p {self.config['install_dir']}
mkdir -p {self.config['log_dir']}
mkdir -p {self.config['data_dir']}
mkdir -p {self.config['config_dir']}

# Copy files
echo "📋 Copying files..."
cp -r src/ {self.config['install_dir']}/
cp -r config/ {self.config['config_dir']}/ 2>/dev/null || true
cp *.py {self.config['install_dir']}/
cp CMakeLists.txt {self.config['install_dir']}/

# Set permissions
echo "🔐 Setting permissions..."
chown -R {self.config['user']}:{self.config['group']} {self.config['install_dir']}
chown -R {self.config['user']}:{self.config['group']} {self.config['log_dir']}
chown -R {self.config['user']}:{self.config['group']} {self.config['data_dir']}
chmod 755 {self.config['install_dir']}
chmod 755 {self.config['log_dir']}
chmod 755 {self.config['data_dir']}

# Build C++ backend
echo "🔨 Building C++ backend..."
cd {self.config['install_dir']}
mkdir -p build
cd build
cmake ..
make

# Install Python dependencies
echo "🐍 Installing Python dependencies..."
pip3 install -r {self.config['install_dir']}/requirements.txt

# Install service
echo "⚙️  Installing system service..."
cp /tmp/hardware-backend.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable {self.service_name}

echo "✅ Installation completed successfully!"
echo ""
echo "Next steps:"
echo "1. Configure the system: {self.config['config_dir']}"
echo "2. Start the service: systemctl start {self.service_name}"
echo "3. Check status: systemctl status {self.service_name}"
"""
        
        with open(install_script, 'w') as f:
            f.write(script_content)
        
        install_script.chmod(0o755)
        print(f"  Created: install.sh")
    
    def _create_service_files(self, package_dir: Path):
        """Create systemd service files."""
        if self.system_type != 'linux':
            return
        
        service_content = f"""[Unit]
Description=Hardware Backend Service
After=network.target
Wants=network.target

[Service]
Type=simple
User={self.config['user']}
Group={self.config['group']}
WorkingDirectory={self.config['install_dir']}
ExecStart=/usr/bin/python3 {self.config['install_dir']}/integration_example.py
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier={self.service_name}

# Environment
Environment=HARDWARE_ENV=production
Environment=PYTHONPATH={self.config['install_dir']}/src

# Security
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths={self.config['log_dir']} {self.config['data_dir']}

[Install]
WantedBy=multi-user.target
"""
        
        service_file = package_dir / f"{self.service_name}.service"
        with open(service_file, 'w') as f:
            f.write(service_content)
        
        print(f"  Created: {self.service_name}.service")
    
    def create_docker_deployment(self) -> str:
        """Create Docker deployment configuration."""
        print("🐳 Creating Docker Deployment")
        print("-" * 40)
        
        dockerfile_content = """FROM ubuntu:22.04

# Install system dependencies
RUN apt-get update && apt-get install -y \\
    python3 \\
    python3-pip \\
    cmake \\
    build-essential \\
    libopencv-dev \\
    pkg-config \\
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
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \\
    CMD python3 -c "import _hardware_backend; print('OK')" || exit 1

# Run application
CMD ["python3", "integration_example.py"]
"""
        
        with open("Dockerfile", 'w') as f:
            f.write(dockerfile_content)
        
        # Create docker-compose.yml
        compose_content = """version: '3.8'

services:
  hardware-backend:
    build: .
    container_name: hardware-backend
    restart: unless-stopped
    
    # Device access for hardware
    privileged: true
    devices:
      - /dev/ttyUSB0:/dev/ttyUSB0  # Shimmer device
      - /dev/video0:/dev/video0    # Webcam device
    
    # Volume mounts
    volumes:
      - ./config:/etc/hardware-backend:ro
      - ./data:/var/lib/hardware-backend
      - ./logs:/var/log/hardware-backend
    
    # Environment
    environment:
      - HARDWARE_ENV=production
    
    # Logging
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
    
    # Health check
    healthcheck:
      test: ["CMD", "python3", "-c", "import _hardware_backend; print('OK')"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # Optional: Monitoring with Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: hardware-prometheus
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus.yml:/etc/prometheus/prometheus.yml:ro
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'

  # Optional: Grafana for visualization
  grafana:
    image: grafana/grafana:latest
    container_name: hardware-grafana
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-storage:/var/lib/grafana

volumes:
  grafana-storage:
"""
        
        with open("docker-compose.yml", 'w') as f:
            f.write(compose_content)
        
        print("  Created: Dockerfile")
        print("  Created: docker-compose.yml")
        
        return "Docker deployment files created"
    
    def create_monitoring_config(self):
        """Create monitoring configuration files."""
        print("📊 Creating Monitoring Configuration")
        print("-" * 40)
        
        # Create monitoring directory
        monitoring_dir = Path("./monitoring")
        monitoring_dir.mkdir(exist_ok=True)
        
        # Prometheus configuration
        prometheus_config = {
            'global': {
                'scrape_interval': '15s',
                'evaluation_interval': '15s'
            },
            'scrape_configs': [
                {
                    'job_name': 'hardware-backend',
                    'static_configs': [
                        {
                            'targets': ['localhost:8000']
                        }
                    ],
                    'scrape_interval': '5s',
                    'metrics_path': '/metrics'
                }
            ]
        }
        
        import yaml
        with open(monitoring_dir / "prometheus.yml", 'w') as f:
            yaml.dump(prometheus_config, f, default_flow_style=False)
        
        # Grafana dashboard
        dashboard_config = {
            "dashboard": {
                "title": "Hardware Backend Monitoring",
                "panels": [
                    {
                        "title": "Latency",
                        "type": "graph",
                        "targets": [
                            {"expr": "hardware_latency_ms"}
                        ]
                    },
                    {
                        "title": "Jitter",
                        "type": "graph", 
                        "targets": [
                            {"expr": "hardware_jitter_ms"}
                        ]
                    }
                ]
            }
        }
        
        with open(monitoring_dir / "dashboard.json", 'w') as f:
            json.dump(dashboard_config, f, indent=2)
        
        print("  Created: monitoring/prometheus.yml")
        print("  Created: monitoring/dashboard.json")
    
    def generate_deployment_guide(self) -> str:
        """Generate comprehensive deployment guide."""
        guide_content = """# Hardware Backend Deployment Guide

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
"""
        
        with open("DEPLOYMENT_GUIDE.md", 'w') as f:
            f.write(guide_content)
        
        print("📖 Created: DEPLOYMENT_GUIDE.md")
        return "DEPLOYMENT_GUIDE.md"

def main():
    """Demonstrate deployment tools."""
    print("🚀 Production Deployment Tools Demo")
    print("=" * 50)
    
    deployment = DeploymentManager()
    
    # Check system requirements
    requirements = deployment.check_system_requirements()
    print()
    
    # Create installation package
    try:
        package_file = deployment.create_installation_package()
        print()
    except Exception as e:
        print(f"⚠️  Package creation failed: {e}")
        print()
    
    # Create Docker deployment
    try:
        deployment.create_docker_deployment()
        print()
    except Exception as e:
        print(f"⚠️  Docker deployment creation failed: {e}")
        print()
    
    # Create monitoring configuration
    try:
        deployment.create_monitoring_config()
        print()
    except Exception as e:
        print(f"⚠️  Monitoring config creation failed: {e}")
        print()
    
    # Generate deployment guide
    try:
        guide_file = deployment.generate_deployment_guide()
        print()
    except Exception as e:
        print(f"⚠️  Deployment guide creation failed: {e}")
        print()
    
    print("🎯 Deployment Tools Summary:")
    print("-" * 40)
    print("✅ System requirements check")
    print("✅ Installation package creation")
    print("✅ Docker deployment configuration")
    print("✅ Monitoring setup")
    print("✅ Comprehensive deployment guide")
    print()
    print("📋 Next Steps:")
    print("• Review system requirements")
    print("• Choose deployment method (package/Docker/manual)")
    print("• Configure environment-specific settings")
    print("• Set up monitoring and alerting")
    print("• Test deployment in staging environment")
    
    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)