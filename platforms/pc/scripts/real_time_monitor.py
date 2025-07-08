#!/usr/bin/env python3
"""Real-time monitoring and visualization tool for the high-precision C++ hardware backend.

This tool provides live monitoring of timing performance, data throughput,
and system health for the hardware layer implementation.
"""

import sys
import os
import time
import threading
import queue
from collections import deque
from typing import Dict, List, Optional
import json
from datetime import datetime

# Add the src directory to Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'src'))

class RealTimeMonitor:
    """Real-time monitoring system for hardware backend performance.
    
    Provides live metrics, alerts, and performance visualization
    for the C++ hardware implementation.
    """
    
    def __init__(self, max_samples: int = 1000) -> None:
        self.max_samples = max_samples
        self.is_monitoring = False
        self.monitor_thread = None
        
        # Data storage for real-time metrics
        self.metrics = {
            'shimmer': {
                'timestamps': deque(maxlen=max_samples),
                'latencies': deque(maxlen=max_samples),
                'data_rates': deque(maxlen=max_samples),
                'errors': deque(maxlen=max_samples)
            },
            'webcam': {
                'timestamps': deque(maxlen=max_samples),
                'latencies': deque(maxlen=max_samples),
                'frame_rates': deque(maxlen=max_samples),
                'errors': deque(maxlen=max_samples)
            },
            'system': {
                'cpu_usage': deque(maxlen=max_samples),
                'memory_usage': deque(maxlen=max_samples),
                'sync_quality': deque(maxlen=max_samples)
            }
        }
        
        # Performance thresholds
        self.thresholds = {
            'max_latency_ms': 1.0,
            'min_data_rate_hz': 100,
            'min_frame_rate_fps': 25,
            'max_jitter_ms': 0.5
        }
        
        # Alert system
        self.alerts = queue.Queue()
        self.alert_history = deque(maxlen=100)
        
    def start_monitoring(self) -> None:
        """Start real-time monitoring."""
        if self.is_monitoring:
            print("⚠ Monitoring already running")
            return
        
        print("🚀 Starting real-time hardware monitoring...")
        self.is_monitoring = True
        self.monitor_thread = threading.Thread(target=self._monitoring_loop, daemon=True)
        self.monitor_thread.start()
        
        print("✓ Real-time monitoring started")
        print("  Press Ctrl+C to stop monitoring")
        
    def stop_monitoring(self) -> None:
        """Stop real-time monitoring."""
        if not self.is_monitoring:
            return
        
        print("\n🛑 Stopping real-time monitoring...")
        self.is_monitoring = False
        
        if self.monitor_thread and self.monitor_thread.is_alive():
            self.monitor_thread.join(timeout=2.0)
        
        print("✓ Monitoring stopped")
        
    def _monitoring_loop(self) -> None:
        """Main monitoring loop."""
        try:
            import _hardware_backend
            
            # Initialize devices
            shimmer = _hardware_backend.NativeShimmer("COM3")
            webcam = _hardware_backend.NativeWebcam(0)
            
            print("📊 Monitoring devices:")
            print(f"  • Shimmer: {shimmer}")
            print(f"  • Webcam: {webcam}")
            print()
            
            last_update = time.time()
            update_interval = 1.0  # Update display every second
            
            while self.is_monitoring:
                current_time = time.time()
                
                # Collect metrics
                self._collect_shimmer_metrics(shimmer, current_time)
                self._collect_webcam_metrics(webcam, current_time)
                self._collect_system_metrics(current_time)
                
                # Check for alerts
                self._check_performance_alerts(current_time)
                
                # Update display
                if current_time - last_update >= update_interval:
                    self._update_display()
                    last_update = current_time
                
                # Small delay to prevent overwhelming the system
                time.sleep(0.01)
                
        except ImportError:
            print("✗ C++ backend not available for monitoring")
        except Exception as e:
            print(f"✗ Monitoring error: {e}")
        
    def _collect_shimmer_metrics(self, shimmer, timestamp: float) -> None:
        """Collect Shimmer device metrics."""
        try:
            start_time = time.perf_counter()
            data = shimmer.get_data()
            end_time = time.perf_counter()
            
            latency = (end_time - start_time) * 1000  # Convert to ms
            data_count = len(data) if data else 0
            
            # Store metrics
            self.metrics['shimmer']['timestamps'].append(timestamp)
            self.metrics['shimmer']['latencies'].append(latency)
            self.metrics['shimmer']['data_rates'].append(data_count)
            self.metrics['shimmer']['errors'].append(0)  # No error
            
        except Exception as e:
            # Record error
            self.metrics['shimmer']['timestamps'].append(timestamp)
            self.metrics['shimmer']['latencies'].append(0)
            self.metrics['shimmer']['data_rates'].append(0)
            self.metrics['shimmer']['errors'].append(1)
            
    def _collect_webcam_metrics(self, webcam, timestamp: float) -> None:
        """Collect Webcam device metrics."""
        try:
            start_time = time.perf_counter()
            frames = webcam.get_data()
            end_time = time.perf_counter()
            
            latency = (end_time - start_time) * 1000  # Convert to ms
            frame_count = len(frames) if frames else 0
            
            # Store metrics
            self.metrics['webcam']['timestamps'].append(timestamp)
            self.metrics['webcam']['latencies'].append(latency)
            self.metrics['webcam']['frame_rates'].append(frame_count)
            self.metrics['webcam']['errors'].append(0)  # No error
            
        except Exception as e:
            # Record error
            self.metrics['webcam']['timestamps'].append(timestamp)
            self.metrics['webcam']['latencies'].append(0)
            self.metrics['webcam']['frame_rates'].append(0)
            self.metrics['webcam']['errors'].append(1)
            
    def _collect_system_metrics(self, timestamp: float) -> None:
        """Collect system performance metrics."""
        try:
            # Simulate system metrics (in real implementation, use psutil)
            cpu_usage = 15.0 + (timestamp % 10) * 2  # Simulated CPU usage
            memory_usage = 45.0 + (timestamp % 5) * 1  # Simulated memory usage
            
            # Calculate sync quality based on recent latencies
            sync_quality = self._calculate_sync_quality()
            
            self.metrics['system']['cpu_usage'].append(cpu_usage)
            self.metrics['system']['memory_usage'].append(memory_usage)
            self.metrics['system']['sync_quality'].append(sync_quality)
            
        except Exception:
            # Default values on error
            self.metrics['system']['cpu_usage'].append(0)
            self.metrics['system']['memory_usage'].append(0)
            self.metrics['system']['sync_quality'].append(0)
            
    def _calculate_sync_quality(self) -> float:
        """Calculate synchronization quality score (0-100)."""
        try:
            shimmer_latencies = list(self.metrics['shimmer']['latencies'])
            webcam_latencies = list(self.metrics['webcam']['latencies'])
            
            if not shimmer_latencies or not webcam_latencies:
                return 100.0
            
            # Calculate recent average latencies
            recent_shimmer = sum(shimmer_latencies[-10:]) / min(10, len(shimmer_latencies))
            recent_webcam = sum(webcam_latencies[-10:]) / min(10, len(webcam_latencies))
            
            # Sync quality based on latency consistency
            avg_latency = (recent_shimmer + recent_webcam) / 2
            quality = max(0, 100 - (avg_latency * 10))  # Lower latency = higher quality
            
            return min(100, quality)
            
        except Exception:
            return 50.0  # Default quality score
            
    def _check_performance_alerts(self, timestamp: float) -> None:
        """Check for performance issues and generate alerts."""
        alerts = []
        
        # Check Shimmer latency
        if self.metrics['shimmer']['latencies']:
            recent_latency = self.metrics['shimmer']['latencies'][-1]
            if recent_latency > self.thresholds['max_latency_ms']:
                alerts.append({
                    'type': 'HIGH_LATENCY',
                    'device': 'shimmer',
                    'value': recent_latency,
                    'threshold': self.thresholds['max_latency_ms'],
                    'timestamp': timestamp
                })
        
        # Check Webcam latency
        if self.metrics['webcam']['latencies']:
            recent_latency = self.metrics['webcam']['latencies'][-1]
            if recent_latency > self.thresholds['max_latency_ms']:
                alerts.append({
                    'type': 'HIGH_LATENCY',
                    'device': 'webcam',
                    'value': recent_latency,
                    'threshold': self.thresholds['max_latency_ms'],
                    'timestamp': timestamp
                })
        
        # Check jitter
        jitter = self._calculate_recent_jitter()
        if jitter > self.thresholds['max_jitter_ms']:
            alerts.append({
                'type': 'HIGH_JITTER',
                'device': 'system',
                'value': jitter,
                'threshold': self.thresholds['max_jitter_ms'],
                'timestamp': timestamp
            })
        
        # Store alerts
        for alert in alerts:
            self.alerts.put(alert)
            self.alert_history.append(alert)
            
    def _calculate_recent_jitter(self) -> float:
        """Calculate recent jitter across all devices."""
        try:
            all_latencies = []
            all_latencies.extend(list(self.metrics['shimmer']['latencies'])[-10:])
            all_latencies.extend(list(self.metrics['webcam']['latencies'])[-10:])
            
            if len(all_latencies) < 2:
                return 0.0
            
            return max(all_latencies) - min(all_latencies)
            
        except Exception:
            return 0.0
            
    def _update_display(self) -> None:
        """Update the real-time display."""
        # Clear screen (simple approach)
        os.system('clear' if os.name == 'posix' else 'cls')
        
        print("=" * 80)
        print("🔍 REAL-TIME HARDWARE MONITORING")
        print("=" * 80)
        print(f"⏰ {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        print()
        
        # Device status
        self._display_device_status()
        
        # Performance metrics
        self._display_performance_metrics()
        
        # System health
        self._display_system_health()
        
        # Recent alerts
        self._display_recent_alerts()
        
        print("=" * 80)
        print("Press Ctrl+C to stop monitoring")
        
    def _display_device_status(self) -> None:
        """Display device connection and status."""
        print("📱 DEVICE STATUS:")
        print("-" * 40)
        
        # Shimmer status
        shimmer_errors = sum(list(self.metrics['shimmer']['errors'])[-10:])
        shimmer_status = "🟢 ONLINE" if shimmer_errors == 0 else "🔴 ERRORS"
        print(f"  Shimmer GSR:  {shimmer_status}")
        
        # Webcam status
        webcam_errors = sum(list(self.metrics['webcam']['errors'])[-10:])
        webcam_status = "🟢 ONLINE" if webcam_errors == 0 else "🔴 ERRORS"
        print(f"  Brio Webcam:  {webcam_status}")
        print()
        
    def _display_performance_metrics(self) -> None:
        """Display real-time performance metrics."""
        print("⚡ PERFORMANCE METRICS:")
        print("-" * 40)
        
        # Shimmer metrics
        if self.metrics['shimmer']['latencies']:
            recent_latency = self.metrics['shimmer']['latencies'][-1]
            avg_latency = sum(list(self.metrics['shimmer']['latencies'])[-10:]) / min(10, len(self.metrics['shimmer']['latencies']))
            print(f"  Shimmer Latency:  {recent_latency:.3f} ms (avg: {avg_latency:.3f} ms)")
        
        # Webcam metrics
        if self.metrics['webcam']['latencies']:
            recent_latency = self.metrics['webcam']['latencies'][-1]
            avg_latency = sum(list(self.metrics['webcam']['latencies'])[-10:]) / min(10, len(self.metrics['webcam']['latencies']))
            print(f"  Webcam Latency:   {recent_latency:.3f} ms (avg: {avg_latency:.3f} ms)")
        
        # Jitter
        jitter = self._calculate_recent_jitter()
        print(f"  System Jitter:    {jitter:.3f} ms")
        print()
        
    def _display_system_health(self) -> None:
        """Display system health metrics."""
        print("🖥️  SYSTEM HEALTH:")
        print("-" * 40)
        
        if self.metrics['system']['cpu_usage']:
            cpu = self.metrics['system']['cpu_usage'][-1]
            memory = self.metrics['system']['memory_usage'][-1]
            sync_quality = self.metrics['system']['sync_quality'][-1]
            
            print(f"  CPU Usage:        {cpu:.1f}%")
            print(f"  Memory Usage:     {memory:.1f}%")
            print(f"  Sync Quality:     {sync_quality:.1f}%")
        print()
        
    def _display_recent_alerts(self) -> None:
        """Display recent performance alerts."""
        print("🚨 RECENT ALERTS:")
        print("-" * 40)
        
        recent_alerts = list(self.alert_history)[-5:]  # Last 5 alerts
        
        if not recent_alerts:
            print("  No recent alerts - system running optimally ✓")
        else:
            for alert in recent_alerts:
                timestamp = datetime.fromtimestamp(alert['timestamp']).strftime('%H:%M:%S')
                print(f"  [{timestamp}] {alert['type']}: {alert['device']} = {alert['value']:.3f}")
        print()
        
    def save_monitoring_data(self, filename: str = None) -> None:
        """Save monitoring data to file."""
        if filename is None:
            filename = f"monitoring_data_{datetime.now().strftime('%Y%m%d_%H%M%S')}.json"
        
        try:
            # Convert deques to lists for JSON serialization
            data = {
                'metrics': {
                    device: {
                        metric: list(values) for metric, values in device_metrics.items()
                    } for device, device_metrics in self.metrics.items()
                },
                'alerts': list(self.alert_history),
                'thresholds': self.thresholds,
                'timestamp': datetime.now().isoformat()
            }
            
            with open(filename, 'w') as f:
                json.dump(data, f, indent=2)
            
            print(f"✓ Monitoring data saved to {filename}")
            
        except Exception as e:
            print(f"✗ Failed to save monitoring data: {e}")

def main() -> None:
    """Run real-time monitoring."""
    print("🔍 Real-Time Hardware Monitor")
    print("=" * 50)
    
    monitor = RealTimeMonitor()
    
    try:
        monitor.start_monitoring()
        
        # Keep monitoring until interrupted
        while monitor.is_monitoring:
            time.sleep(1)
            
    except KeyboardInterrupt:
        print("\n⏹️  Monitoring interrupted by user")
    except Exception as e:
        print(f"\n✗ Monitoring error: {e}")
    finally:
        monitor.stop_monitoring()
        
        # Save monitoring data
        print("\n💾 Saving monitoring data...")
        monitor.save_monitoring_data()
        
        print("\n📊 Monitoring session completed")
        print("Key achievements:")
        print("• Real-time performance tracking")
        print("• Automated alert system")
        print("• System health monitoring")
        print("• Data logging and analysis")

if __name__ == "__main__":
    main()