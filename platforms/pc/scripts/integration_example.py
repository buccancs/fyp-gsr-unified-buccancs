#!/usr/bin/env python3
"""Comprehensive integration example for the high-precision C++ hardware backend.

This script demonstrates how to use all components together:
- C++ hardware backend
- Performance monitoring
- Configuration management
- Benchmarking and validation

This serves as a complete example for production deployment.
"""

import sys
import os
import time
import threading
import signal
from pathlib import Path
from datetime import datetime
from typing import Optional

# Add the src directory to Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

# Import our custom modules
from config_manager import ConfigManager, ApplicationConfig

class IntegratedHardwareSystem:
    """Complete integrated hardware system combining all components.
    
    This class demonstrates production-ready usage of:
    - C++ hardware backend
    - Real-time monitoring
    - Configuration management
    - Performance validation
    """
    
    def __init__(self, config_dir: str = "./config") -> None:
        self.config_manager = ConfigManager(config_dir)
        self.config: Optional[ApplicationConfig] = None
        self.is_running = False
        self.monitoring_thread: Optional[threading.Thread] = None
        
        # Hardware devices
        self.shimmer_device = None
        self.webcam_device = None
        
        # Performance tracking
        self.performance_data = {
            'shimmer_latencies': [],
            'webcam_latencies': [],
            'sync_deltas': [],
            'alerts': []
        }
        
        # Setup signal handlers for graceful shutdown
        signal.signal(signal.SIGINT, self._signal_handler)
        signal.signal(signal.SIGTERM, self._signal_handler)
        
    def initialize(self) -> bool:
        """Initialize the complete hardware system."""
        print("🚀 Initializing Integrated Hardware System")
        print("=" * 50)
        
        try:
            # Load configuration
            print("📋 Loading configuration...")
            self.config = self.config_manager.load_config()
            print(f"  Environment: {self.config_manager.environment}")
            print(f"  Shimmer COM Port: {self.config.hardware.shimmer_com_port}")
            print(f"  Webcam Resolution: {self.config.hardware.webcam_width}x{self.config.hardware.webcam_height}")
            print(f"  Max Latency Threshold: {self.config.performance.max_latency_ms} ms")
            
            # Validate configuration
            issues = self.config_manager.validate_config(self.config)
            if issues:
                print("⚠️  Configuration issues found:")
                for issue in issues:
                    print(f"    • {issue}")
                return False
            print("✅ Configuration validation passed")
            
            # Initialize C++ backend
            print("\n🔧 Initializing C++ hardware backend...")
            if not self._initialize_hardware():
                return False
            
            # Setup data output directory
            data_dir = Path(self.config.system.data_output_dir)
            data_dir.mkdir(exist_ok=True)
            print(f"📁 Data output directory: {data_dir}")
            
            print("\n✅ System initialization completed successfully")
            return True
            
        except Exception as e:
            print(f"❌ Initialization failed: {e}")
            return False
    
    def _initialize_hardware(self) -> bool:
        """Initialize C++ hardware devices."""
        try:
            import _hardware_backend
            
            # Initialize Shimmer device
            print(f"  Initializing Shimmer on {self.config.hardware.shimmer_com_port}...")
            self.shimmer_device = _hardware_backend.NativeShimmer(self.config.hardware.shimmer_com_port)
            print(f"    ✓ Shimmer: {self.shimmer_device}")
            
            # Initialize Webcam device
            print(f"  Initializing Webcam at index {self.config.hardware.webcam_index}...")
            self.webcam_device = _hardware_backend.NativeWebcam(self.config.hardware.webcam_index)
            
            # Configure webcam resolution
            self.webcam_device.set_resolution(
                self.config.hardware.webcam_width,
                self.config.hardware.webcam_height
            )
            print(f"    ✓ Webcam: {self.webcam_device}")
            
            return True
            
        except ImportError:
            print("❌ C++ hardware backend not available")
            print("   Please ensure the backend is compiled and installed")
            return False
        except Exception as e:
            print(f"❌ Hardware initialization failed: {e}")
            return False
    
    def start_system(self) -> bool:
        """Start the complete hardware system."""
        if not self.shimmer_device or not self.webcam_device:
            print("❌ Hardware not initialized")
            return False
        
        print("\n🎬 Starting Hardware System")
        print("-" * 30)
        
        try:
            # Start hardware devices
            print("Starting Shimmer device...")
            if not self.shimmer_device.start():
                print("❌ Failed to start Shimmer device")
                return False
            print("✅ Shimmer started")
            
            print("Starting Webcam device...")
            if not self.webcam_device.start():
                print("❌ Failed to start Webcam device")
                return False
            print("✅ Webcam started")
            
            # Start monitoring
            self.is_running = True
            self.monitoring_thread = threading.Thread(target=self._monitoring_loop, daemon=True)
            self.monitoring_thread.start()
            print("✅ Monitoring started")
            
            print("\n🟢 System is now running")
            return True
            
        except Exception as e:
            print(f"❌ Failed to start system: {e}")
            return False
    
    def _monitoring_loop(self) -> None:
        """Main monitoring loop for performance tracking."""
        print("📊 Performance monitoring active...")
        
        last_display = time.time()
        display_interval = self.config.performance.display_update_interval_s
        
        while self.is_running:
            try:
                current_time = time.time()
                
                # Collect performance metrics
                self._collect_performance_metrics()
                
                # Check for alerts
                self._check_performance_alerts()
                
                # Update display periodically
                if current_time - last_display >= display_interval:
                    self._display_performance_status()
                    last_display = current_time
                
                # Sleep based on monitoring interval
                time.sleep(self.config.performance.monitoring_interval_ms / 1000.0)
                
            except Exception as e:
                print(f"⚠️  Monitoring error: {e}")
                time.sleep(1.0)
    
    def _collect_performance_metrics(self) -> None:
        """Collect performance metrics from hardware devices."""
        try:
            # Measure Shimmer performance
            start_time = time.perf_counter()
            shimmer_data = self.shimmer_device.get_data()
            end_time = time.perf_counter()
            shimmer_latency = (end_time - start_time) * 1000
            
            # Measure Webcam performance
            start_time = time.perf_counter()
            webcam_data = self.webcam_device.get_data()
            end_time = time.perf_counter()
            webcam_latency = (end_time - start_time) * 1000
            
            # Store metrics (keep last N samples)
            max_samples = self.config.performance.max_samples
            
            self.performance_data['shimmer_latencies'].append(shimmer_latency)
            if len(self.performance_data['shimmer_latencies']) > max_samples:
                self.performance_data['shimmer_latencies'].pop(0)
            
            self.performance_data['webcam_latencies'].append(webcam_latency)
            if len(self.performance_data['webcam_latencies']) > max_samples:
                self.performance_data['webcam_latencies'].pop(0)
            
            # Calculate synchronization delta
            sync_delta = abs(shimmer_latency - webcam_latency)
            self.performance_data['sync_deltas'].append(sync_delta)
            if len(self.performance_data['sync_deltas']) > max_samples:
                self.performance_data['sync_deltas'].pop(0)
                
        except Exception as e:
            print(f"⚠️  Metrics collection error: {e}")
    
    def _check_performance_alerts(self) -> None:
        """Check for performance issues and generate alerts."""
        try:
            alerts = []
            
            # Check recent latencies
            if self.performance_data['shimmer_latencies']:
                recent_shimmer = self.performance_data['shimmer_latencies'][-1]
                if recent_shimmer > self.config.performance.max_latency_ms:
                    alerts.append(f"HIGH_SHIMMER_LATENCY: {recent_shimmer:.3f} ms")
            
            if self.performance_data['webcam_latencies']:
                recent_webcam = self.performance_data['webcam_latencies'][-1]
                if recent_webcam > self.config.performance.max_latency_ms:
                    alerts.append(f"HIGH_WEBCAM_LATENCY: {recent_webcam:.3f} ms")
            
            # Check jitter
            if len(self.performance_data['shimmer_latencies']) > 10:
                recent_shimmer_latencies = self.performance_data['shimmer_latencies'][-10:]
                shimmer_jitter = max(recent_shimmer_latencies) - min(recent_shimmer_latencies)
                if shimmer_jitter > self.config.performance.max_jitter_ms:
                    alerts.append(f"HIGH_SHIMMER_JITTER: {shimmer_jitter:.3f} ms")
            
            # Store alerts
            for alert in alerts:
                alert_entry = {
                    'timestamp': datetime.now().isoformat(),
                    'message': alert
                }
                self.performance_data['alerts'].append(alert_entry)
                
                # Keep only recent alerts
                max_alerts = self.config.performance.alert_history_size
                if len(self.performance_data['alerts']) > max_alerts:
                    self.performance_data['alerts'].pop(0)
                
                print(f"🚨 ALERT: {alert}")
                
        except Exception as e:
            print(f"⚠️  Alert checking error: {e}")
    
    def _display_performance_status(self) -> None:
        """Display current performance status."""
        try:
            # Clear screen for live updates
            os.system('clear' if os.name == 'posix' else 'cls')
            
            print("🔍 INTEGRATED HARDWARE SYSTEM - LIVE STATUS")
            print("=" * 60)
            print(f"⏰ {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
            print(f"🌍 Environment: {self.config_manager.environment}")
            print()
            
            # Device status
            print("📱 DEVICE STATUS:")
            shimmer_status = "🟢 RUNNING" if self.shimmer_device.is_running() else "🔴 STOPPED"
            webcam_status = "🟢 RUNNING" if self.webcam_device.is_running() else "🔴 STOPPED"
            print(f"  Shimmer GSR:  {shimmer_status}")
            print(f"  Brio Webcam:  {webcam_status}")
            print()
            
            # Performance metrics
            print("⚡ PERFORMANCE METRICS:")
            if self.performance_data['shimmer_latencies']:
                shimmer_avg = sum(self.performance_data['shimmer_latencies'][-10:]) / min(10, len(self.performance_data['shimmer_latencies']))
                shimmer_recent = self.performance_data['shimmer_latencies'][-1]
                print(f"  Shimmer Latency:  {shimmer_recent:.3f} ms (avg: {shimmer_avg:.3f} ms)")
            
            if self.performance_data['webcam_latencies']:
                webcam_avg = sum(self.performance_data['webcam_latencies'][-10:]) / min(10, len(self.performance_data['webcam_latencies']))
                webcam_recent = self.performance_data['webcam_latencies'][-1]
                print(f"  Webcam Latency:   {webcam_recent:.3f} ms (avg: {webcam_avg:.3f} ms)")
            
            if self.performance_data['sync_deltas']:
                sync_avg = sum(self.performance_data['sync_deltas'][-10:]) / min(10, len(self.performance_data['sync_deltas']))
                print(f"  Sync Delta:       {sync_avg:.3f} ms")
            print()
            
            # Configuration info
            print("⚙️  CONFIGURATION:")
            print(f"  Max Latency:      {self.config.performance.max_latency_ms} ms")
            print(f"  Max Jitter:       {self.config.performance.max_jitter_ms} ms")
            print(f"  Monitoring Rate:  {1000/self.config.performance.monitoring_interval_ms:.1f} Hz")
            print()
            
            # Recent alerts
            print("🚨 RECENT ALERTS:")
            recent_alerts = self.performance_data['alerts'][-3:]
            if not recent_alerts:
                print("  No recent alerts - system running optimally ✅")
            else:
                for alert in recent_alerts:
                    timestamp = datetime.fromisoformat(alert['timestamp']).strftime('%H:%M:%S')
                    print(f"  [{timestamp}] {alert['message']}")
            print()
            
            print("-" * 60)
            print("Press Ctrl+C to stop the system")
            
        except Exception as e:
            print(f"⚠️  Display error: {e}")
    
    def stop_system(self) -> None:
        """Stop the hardware system gracefully."""
        print("\n🛑 Stopping Hardware System...")
        
        self.is_running = False
        
        # Stop monitoring thread
        if self.monitoring_thread and self.monitoring_thread.is_alive():
            self.monitoring_thread.join(timeout=2.0)
        
        # Stop hardware devices
        if self.shimmer_device:
            self.shimmer_device.stop()
            print("✅ Shimmer stopped")
        
        if self.webcam_device:
            self.webcam_device.stop()
            print("✅ Webcam stopped")
        
        # Save performance data
        self._save_performance_data()
        
        print("✅ System stopped gracefully")
    
    def _save_performance_data(self) -> None:
        """Save performance data to file."""
        try:
            timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
            filename = f"performance_data_{timestamp}.json"
            
            data = {
                'timestamp': datetime.now().isoformat(),
                'environment': self.config_manager.environment,
                'configuration': {
                    'max_latency_ms': self.config.performance.max_latency_ms,
                    'max_jitter_ms': self.config.performance.max_jitter_ms,
                    'monitoring_interval_ms': self.config.performance.monitoring_interval_ms
                },
                'performance_data': self.performance_data,
                'statistics': self._calculate_statistics()
            }
            
            import json
            with open(filename, 'w') as f:
                json.dump(data, f, indent=2)
            
            print(f"💾 Performance data saved to {filename}")
            
        except Exception as e:
            print(f"⚠️  Failed to save performance data: {e}")
    
    def _calculate_statistics(self) -> dict:
        """Calculate performance statistics."""
        stats = {}
        
        if self.performance_data['shimmer_latencies']:
            shimmer_latencies = self.performance_data['shimmer_latencies']
            stats['shimmer'] = {
                'mean': sum(shimmer_latencies) / len(shimmer_latencies),
                'min': min(shimmer_latencies),
                'max': max(shimmer_latencies),
                'jitter': max(shimmer_latencies) - min(shimmer_latencies),
                'samples': len(shimmer_latencies)
            }
        
        if self.performance_data['webcam_latencies']:
            webcam_latencies = self.performance_data['webcam_latencies']
            stats['webcam'] = {
                'mean': sum(webcam_latencies) / len(webcam_latencies),
                'min': min(webcam_latencies),
                'max': max(webcam_latencies),
                'jitter': max(webcam_latencies) - min(webcam_latencies),
                'samples': len(webcam_latencies)
            }
        
        stats['total_alerts'] = len(self.performance_data['alerts'])
        
        return stats
    
    def _signal_handler(self, signum, frame) -> None:
        """Handle system signals for graceful shutdown."""
        print(f"\n📡 Received signal {signum}")
        self.stop_system()
        sys.exit(0)

def main() -> None:
    """Run the integrated hardware system demonstration."""
    print("🎯 Integrated Hardware System Demo")
    print("=" * 50)
    print("This demo shows the complete integration of:")
    print("• C++ hardware backend")
    print("• Real-time performance monitoring")
    print("• Advanced configuration management")
    print("• Production-ready deployment")
    print()
    
    # Initialize system
    system = IntegratedHardwareSystem()
    
    if not system.initialize():
        print("❌ System initialization failed")
        return False
    
    # Start system
    if not system.start_system():
        print("❌ Failed to start system")
        return False
    
    try:
        # Run for demonstration (or until interrupted)
        print("\n🔄 System running... (Press Ctrl+C to stop)")
        
        # Keep main thread alive
        while system.is_running:
            time.sleep(1)
            
    except KeyboardInterrupt:
        print("\n⏹️  Demo interrupted by user")
    except Exception as e:
        print(f"\n❌ System error: {e}")
    finally:
        system.stop_system()
    
    print("\n🏁 Integration demo completed successfully!")
    print("\n📊 Key Achievements:")
    print("• Complete system integration")
    print("• Real-time performance monitoring")
    print("• Configuration-driven operation")
    print("• Graceful shutdown handling")
    print("• Performance data logging")
    
    return True

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)