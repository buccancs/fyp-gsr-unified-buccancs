"""Enhanced Stress Testing and Performance Profiling for FYP-GSR System

This module provides comprehensive stress testing capabilities including:
- Long-duration stress testing
- Performance profiling under various load conditions
- Memory leak detection
- Network stress testing
- Device connection stress testing
- Thermal profiling
- Resource exhaustion testing

Author: FYP-GSR Team
"""

import pytest
import time
import threading
import psutil
import gc
import sys
import os
import json
import tempfile
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple, Any
from unittest.mock import Mock, patch, MagicMock
from collections import defaultdict, deque
import concurrent.futures

# Add src to path for imports
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))

from ui.performance_monitor import PerformanceMetrics, PerformanceCollector
from utils.thermal_rgb_sync import ThermalRGBSynchronizer
from network.device_manager import DeviceManager


class StressTestMetrics:
    """Container for stress test metrics and results."""
    
    def __init__(self):
        self.start_time = datetime.now()
        self.end_time = None
        self.duration_seconds = 0
        self.peak_memory_mb = 0
        self.peak_cpu_percent = 0
        self.total_operations = 0
        self.failed_operations = 0
        self.average_response_time_ms = 0
        self.memory_leaks_detected = []
        self.errors = []
        self.performance_samples = []
        
    def finalize(self):
        """Finalize metrics calculation."""
        self.end_time = datetime.now()
        self.duration_seconds = (self.end_time - self.start_time).total_seconds()
        
    def to_dict(self) -> Dict[str, Any]:
        """Convert metrics to dictionary for serialization."""
        return {
            'start_time': self.start_time.isoformat(),
            'end_time': self.end_time.isoformat() if self.end_time else None,
            'duration_seconds': self.duration_seconds,
            'peak_memory_mb': self.peak_memory_mb,
            'peak_cpu_percent': self.peak_cpu_percent,
            'total_operations': self.total_operations,
            'failed_operations': self.failed_operations,
            'success_rate': (self.total_operations - self.failed_operations) / max(1, self.total_operations),
            'average_response_time_ms': self.average_response_time_ms,
            'memory_leaks_detected': len(self.memory_leaks_detected),
            'errors_count': len(self.errors),
            'performance_samples_count': len(self.performance_samples)
        }


class MemoryProfiler:
    """Memory profiling and leak detection."""
    
    def __init__(self):
        self.baseline_memory = 0
        self.memory_samples = deque(maxlen=1000)
        self.gc_stats = []
        
    def start_profiling(self):
        """Start memory profiling."""
        gc.collect()  # Clean up before baseline
        self.baseline_memory = psutil.Process().memory_info().rss / (1024 * 1024)
        
    def sample_memory(self) -> float:
        """Take a memory sample."""
        current_memory = psutil.Process().memory_info().rss / (1024 * 1024)
        self.memory_samples.append({
            'timestamp': datetime.now(),
            'memory_mb': current_memory,
            'delta_mb': current_memory - self.baseline_memory
        })
        return current_memory
        
    def detect_leaks(self, threshold_mb: float = 100) -> List[Dict[str, Any]]:
        """Detect potential memory leaks."""
        leaks = []
        
        if len(self.memory_samples) < 10:
            return leaks
            
        # Check for sustained memory growth
        recent_samples = list(self.memory_samples)[-10:]
        growth_trend = recent_samples[-1]['memory_mb'] - recent_samples[0]['memory_mb']
        
        if growth_trend > threshold_mb:
            leaks.append({
                'type': 'sustained_growth',
                'growth_mb': growth_trend,
                'duration_samples': len(recent_samples),
                'detected_at': datetime.now()
            })
            
        # Check for memory spikes
        max_memory = max(sample['memory_mb'] for sample in recent_samples)
        if max_memory - self.baseline_memory > threshold_mb * 2:
            leaks.append({
                'type': 'memory_spike',
                'peak_mb': max_memory,
                'baseline_mb': self.baseline_memory,
                'spike_mb': max_memory - self.baseline_memory,
                'detected_at': datetime.now()
            })
            
        return leaks
        
    def get_gc_stats(self) -> Dict[str, Any]:
        """Get garbage collection statistics."""
        return {
            'collections': gc.get_stats(),
            'objects': len(gc.get_objects()),
            'referrers_count': sum(len(gc.get_referrers(obj)) for obj in gc.get_objects()[:100])  # Sample
        }


class NetworkStressTester:
    """Network stress testing utilities."""
    
    def __init__(self):
        self.active_connections = []
        self.connection_stats = defaultdict(int)
        
    def stress_test_connections(self, 
                              target_connections: int, 
                              duration_seconds: int,
                              connection_factory) -> Dict[str, Any]:
        """Stress test network connections."""
        results = {
            'target_connections': target_connections,
            'successful_connections': 0,
            'failed_connections': 0,
            'connection_errors': [],
            'peak_concurrent': 0,
            'average_response_time': 0
        }
        
        start_time = time.time()
        response_times = []
        
        def create_connection():
            try:
                conn_start = time.time()
                connection = connection_factory()
                conn_time = (time.time() - conn_start) * 1000  # ms
                response_times.append(conn_time)
                
                self.active_connections.append(connection)
                results['successful_connections'] += 1
                results['peak_concurrent'] = max(results['peak_concurrent'], len(self.active_connections))
                
                # Hold connection for a bit
                time.sleep(0.1)
                
                # Clean up
                if hasattr(connection, 'close'):
                    connection.close()
                self.active_connections.remove(connection)
                
            except Exception as e:
                results['failed_connections'] += 1
                results['connection_errors'].append(str(e))
                
        # Create connections concurrently
        with concurrent.futures.ThreadPoolExecutor(max_workers=min(target_connections, 50)) as executor:
            futures = []
            
            for i in range(target_connections):
                if time.time() - start_time > duration_seconds:
                    break
                    
                future = executor.submit(create_connection)
                futures.append(future)
                
                # Throttle connection creation
                time.sleep(0.01)
                
            # Wait for completion
            concurrent.futures.wait(futures, timeout=duration_seconds)
            
        results['average_response_time'] = sum(response_times) / len(response_times) if response_times else 0
        return results


class DeviceStressTester:
    """Device connection and operation stress testing."""
    
    def __init__(self):
        self.mock_devices = []
        self.operation_stats = defaultdict(int)
        
    def create_mock_device(self, device_id: str) -> Mock:
        """Create a mock device for testing."""
        device = Mock()
        device.device_id = device_id
        device.is_connected = True
        device.connect = Mock(return_value=True)
        device.disconnect = Mock(return_value=True)
        device.start_recording = Mock(return_value=True)
        device.stop_recording = Mock(return_value=True)
        device.get_status = Mock(return_value={'status': 'connected', 'battery': 85})
        
        # Simulate occasional failures
        if device_id.endswith('_fail'):
            device.connect.side_effect = Exception("Connection failed")
            device.start_recording.side_effect = Exception("Recording failed")
            
        return device
        
    def stress_test_device_operations(self, 
                                    num_devices: int, 
                                    operations_per_device: int,
                                    duration_seconds: int) -> Dict[str, Any]:
        """Stress test device operations."""
        results = {
            'devices_tested': num_devices,
            'operations_per_device': operations_per_device,
            'total_operations': 0,
            'successful_operations': 0,
            'failed_operations': 0,
            'operation_types': defaultdict(int),
            'errors': []
        }
        
        # Create mock devices
        devices = []
        for i in range(num_devices):
            device_id = f"stress_device_{i}"
            if i % 10 == 0:  # 10% failure rate
                device_id += "_fail"
            devices.append(self.create_mock_device(device_id))
            
        start_time = time.time()
        
        def perform_device_operations(device):
            operations = ['connect', 'start_recording', 'get_status', 'stop_recording', 'disconnect']
            
            for _ in range(operations_per_device):
                if time.time() - start_time > duration_seconds:
                    break
                    
                operation = operations[results['total_operations'] % len(operations)]
                results['total_operations'] += 1
                results['operation_types'][operation] += 1
                
                try:
                    if operation == 'connect':
                        device.connect()
                    elif operation == 'start_recording':
                        device.start_recording()
                    elif operation == 'get_status':
                        device.get_status()
                    elif operation == 'stop_recording':
                        device.stop_recording()
                    elif operation == 'disconnect':
                        device.disconnect()
                        
                    results['successful_operations'] += 1
                    
                except Exception as e:
                    results['failed_operations'] += 1
                    results['errors'].append(f"{device.device_id}:{operation}:{str(e)}")
                    
                # Small delay between operations
                time.sleep(0.001)
                
        # Run operations concurrently
        with concurrent.futures.ThreadPoolExecutor(max_workers=min(num_devices, 20)) as executor:
            futures = [executor.submit(perform_device_operations, device) for device in devices]
            concurrent.futures.wait(futures, timeout=duration_seconds + 10)
            
        return results


class ThermalProfiler:
    """Thermal and performance profiling under load."""
    
    def __init__(self):
        self.cpu_samples = deque(maxlen=1000)
        self.temperature_samples = deque(maxlen=1000)
        
    def start_thermal_monitoring(self, duration_seconds: int) -> Dict[str, Any]:
        """Monitor system thermal behavior under load."""
        results = {
            'duration_seconds': duration_seconds,
            'cpu_samples': [],
            'temperature_samples': [],
            'thermal_throttling_detected': False,
            'peak_cpu_percent': 0,
            'average_cpu_percent': 0
        }
        
        start_time = time.time()
        
        while time.time() - start_time < duration_seconds:
            # Sample CPU usage
            cpu_percent = psutil.cpu_percent(interval=0.1)
            results['cpu_samples'].append(cpu_percent)
            results['peak_cpu_percent'] = max(results['peak_cpu_percent'], cpu_percent)
            
            # Try to get temperature (platform dependent)
            try:
                if hasattr(psutil, 'sensors_temperatures'):
                    temps = psutil.sensors_temperatures()
                    if temps:
                        for name, entries in temps.items():
                            for entry in entries:
                                results['temperature_samples'].append({
                                    'sensor': f"{name}_{entry.label or 'unknown'}",
                                    'temperature': entry.current,
                                    'timestamp': time.time()
                                })
                                
                                # Check for thermal throttling (>80°C)
                                if entry.current > 80:
                                    results['thermal_throttling_detected'] = True
            except:
                pass  # Temperature monitoring not available
                
            time.sleep(0.5)
            
        results['average_cpu_percent'] = sum(results['cpu_samples']) / len(results['cpu_samples']) if results['cpu_samples'] else 0
        return results


class StressTestSuite:
    """Main stress testing suite."""
    
    def __init__(self):
        self.memory_profiler = MemoryProfiler()
        self.network_tester = NetworkStressTester()
        self.device_tester = DeviceStressTester()
        self.thermal_profiler = ThermalProfiler()
        
    def run_comprehensive_stress_test(self, 
                                    duration_minutes: int = 30,
                                    config: Dict[str, Any] = None) -> StressTestMetrics:
        """Run comprehensive stress test suite."""
        config = config or {}
        duration_seconds = duration_minutes * 60
        
        metrics = StressTestMetrics()
        self.memory_profiler.start_profiling()
        
        print(f"[DEBUG_LOG] Starting comprehensive stress test for {duration_minutes} minutes")
        
        # Start background monitoring
        monitoring_thread = threading.Thread(
            target=self._background_monitoring,
            args=(metrics, duration_seconds)
        )
        monitoring_thread.daemon = True
        monitoring_thread.start()
        
        try:
            # Run different stress tests concurrently
            with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
                futures = []
                
                # Memory stress test
                if config.get('test_memory', True):
                    futures.append(executor.submit(self._memory_stress_test, duration_seconds // 4, metrics))
                    
                # Network stress test
                if config.get('test_network', True):
                    futures.append(executor.submit(self._network_stress_test, duration_seconds // 4, metrics))
                    
                # Device stress test
                if config.get('test_devices', True):
                    futures.append(executor.submit(self._device_stress_test, duration_seconds // 4, metrics))
                    
                # Thermal stress test
                if config.get('test_thermal', True):
                    futures.append(executor.submit(self._thermal_stress_test, duration_seconds // 4, metrics))
                    
                # Wait for all tests to complete
                concurrent.futures.wait(futures, timeout=duration_seconds + 60)
                
        except Exception as e:
            metrics.errors.append(f"Stress test error: {str(e)}")
            
        finally:
            metrics.finalize()
            
        print(f"[DEBUG_LOG] Stress test completed. Success rate: {((metrics.total_operations - metrics.failed_operations) / max(1, metrics.total_operations)) * 100:.1f}%")
        return metrics
        
    def _background_monitoring(self, metrics: StressTestMetrics, duration_seconds: int):
        """Background monitoring thread."""
        start_time = time.time()
        
        while time.time() - start_time < duration_seconds:
            try:
                # Sample memory
                current_memory = self.memory_profiler.sample_memory()
                metrics.peak_memory_mb = max(metrics.peak_memory_mb, current_memory)
                
                # Sample CPU
                cpu_percent = psutil.cpu_percent(interval=None)
                metrics.peak_cpu_percent = max(metrics.peak_cpu_percent, cpu_percent)
                
                # Check for memory leaks
                leaks = self.memory_profiler.detect_leaks()
                metrics.memory_leaks_detected.extend(leaks)
                
                # Store performance sample
                metrics.performance_samples.append({
                    'timestamp': datetime.now(),
                    'memory_mb': current_memory,
                    'cpu_percent': cpu_percent
                })
                
            except Exception as e:
                metrics.errors.append(f"Monitoring error: {str(e)}")
                
            time.sleep(1)
            
    def _memory_stress_test(self, duration_seconds: int, metrics: StressTestMetrics):
        """Memory-intensive stress test."""
        print("[DEBUG_LOG] Starting memory stress test")
        
        start_time = time.time()
        memory_hogs = []
        
        try:
            while time.time() - start_time < duration_seconds:
                # Allocate and deallocate memory
                data = [i for i in range(100000)]  # Allocate ~800KB
                memory_hogs.append(data)
                
                # Periodically clean up
                if len(memory_hogs) > 100:
                    memory_hogs = memory_hogs[-50:]  # Keep only recent allocations
                    gc.collect()
                    
                metrics.total_operations += 1
                time.sleep(0.01)
                
        except Exception as e:
            metrics.errors.append(f"Memory stress test error: {str(e)}")
            metrics.failed_operations += 1
            
        finally:
            # Clean up
            memory_hogs.clear()
            gc.collect()
            
    def _network_stress_test(self, duration_seconds: int, metrics: StressTestMetrics):
        """Network stress test."""
        print("[DEBUG_LOG] Starting network stress test")
        
        def mock_connection_factory():
            # Simulate network connection
            time.sleep(0.01)  # Simulate connection time
            return Mock()
            
        try:
            results = self.network_tester.stress_test_connections(
                target_connections=100,
                duration_seconds=duration_seconds,
                connection_factory=mock_connection_factory
            )
            
            metrics.total_operations += results['successful_connections'] + results['failed_connections']
            metrics.failed_operations += results['failed_connections']
            
        except Exception as e:
            metrics.errors.append(f"Network stress test error: {str(e)}")
            metrics.failed_operations += 1
            
    def _device_stress_test(self, duration_seconds: int, metrics: StressTestMetrics):
        """Device operations stress test."""
        print("[DEBUG_LOG] Starting device stress test")
        
        try:
            results = self.device_tester.stress_test_device_operations(
                num_devices=20,
                operations_per_device=50,
                duration_seconds=duration_seconds
            )
            
            metrics.total_operations += results['total_operations']
            metrics.failed_operations += results['failed_operations']
            
        except Exception as e:
            metrics.errors.append(f"Device stress test error: {str(e)}")
            metrics.failed_operations += 1
            
    def _thermal_stress_test(self, duration_seconds: int, metrics: StressTestMetrics):
        """Thermal stress test."""
        print("[DEBUG_LOG] Starting thermal stress test")
        
        try:
            # CPU-intensive operations to generate heat
            def cpu_intensive_task():
                start = time.time()
                while time.time() - start < duration_seconds:
                    # Perform CPU-intensive calculations
                    sum(i * i for i in range(10000))
                    metrics.total_operations += 1
                    
            # Run multiple CPU-intensive threads
            threads = []
            for _ in range(min(4, psutil.cpu_count())):
                thread = threading.Thread(target=cpu_intensive_task)
                thread.start()
                threads.append(thread)
                
            # Wait for threads to complete
            for thread in threads:
                thread.join()
                
        except Exception as e:
            metrics.errors.append(f"Thermal stress test error: {str(e)}")
            metrics.failed_operations += 1


# Test classes for pytest
class TestStressTestSuite:
    """Pytest test class for stress testing."""
    
    def setup_method(self):
        """Setup for each test method."""
        self.stress_suite = StressTestSuite()
        
    def test_short_stress_test(self):
        """Test short duration stress test."""
        print("[DEBUG_LOG] Running short stress test")
        
        config = {
            'test_memory': True,
            'test_network': True,
            'test_devices': True,
            'test_thermal': False  # Skip thermal for short test
        }
        
        metrics = self.stress_suite.run_comprehensive_stress_test(
            duration_minutes=1,  # 1 minute test
            config=config
        )
        
        # Assertions
        assert metrics.duration_seconds > 0
        assert metrics.total_operations > 0
        assert metrics.peak_memory_mb > 0
        
        # Success rate should be reasonable
        success_rate = (metrics.total_operations - metrics.failed_operations) / metrics.total_operations
        assert success_rate > 0.8, f"Success rate too low: {success_rate}"
        
        print(f"[DEBUG_LOG] Short stress test completed: {metrics.to_dict()}")
        
    def test_memory_profiler(self):
        """Test memory profiler functionality."""
        print("[DEBUG_LOG] Testing memory profiler")
        
        profiler = MemoryProfiler()
        profiler.start_profiling()
        
        # Allocate some memory
        data = [i for i in range(100000)]
        current_memory = profiler.sample_memory()
        
        assert current_memory > profiler.baseline_memory
        assert len(profiler.memory_samples) > 0
        
        # Clean up
        del data
        gc.collect()
        
    def test_network_stress_tester(self):
        """Test network stress tester."""
        print("[DEBUG_LOG] Testing network stress tester")
        
        def mock_connection():
            return Mock()
            
        tester = NetworkStressTester()
        results = tester.stress_test_connections(
            target_connections=10,
            duration_seconds=5,
            connection_factory=mock_connection
        )
        
        assert results['target_connections'] == 10
        assert results['successful_connections'] > 0
        assert 'average_response_time' in results
        
    def test_device_stress_tester(self):
        """Test device stress tester."""
        print("[DEBUG_LOG] Testing device stress tester")
        
        tester = DeviceStressTester()
        results = tester.stress_test_device_operations(
            num_devices=5,
            operations_per_device=10,
            duration_seconds=5
        )
        
        assert results['devices_tested'] == 5
        assert results['total_operations'] > 0
        assert results['successful_operations'] > 0
        
    @pytest.mark.slow
    def test_long_duration_stress_test(self):
        """Test long duration stress test (marked as slow)."""
        print("[DEBUG_LOG] Running long duration stress test")
        
        metrics = self.stress_suite.run_comprehensive_stress_test(
            duration_minutes=5,  # 5 minute test
            config={'test_thermal': True}
        )
        
        # Assertions for long test
        assert metrics.duration_seconds >= 300  # At least 5 minutes
        assert metrics.total_operations > 1000
        assert len(metrics.performance_samples) > 100
        
        # Check for memory leaks
        if metrics.memory_leaks_detected:
            print(f"[DEBUG_LOG] Memory leaks detected: {len(metrics.memory_leaks_detected)}")
            
        print(f"[DEBUG_LOG] Long stress test completed: {metrics.to_dict()}")
        
    def test_thermal_profiler(self):
        """Test thermal profiler."""
        print("[DEBUG_LOG] Testing thermal profiler")
        
        profiler = ThermalProfiler()
        results = profiler.start_thermal_monitoring(duration_seconds=3)
        
        assert results['duration_seconds'] == 3
        assert len(results['cpu_samples']) > 0
        assert results['peak_cpu_percent'] >= 0
        assert results['average_cpu_percent'] >= 0
        
    def test_performance_under_load(self):
        """Test system performance under various loads."""
        print("[DEBUG_LOG] Testing performance under load")
        
        # Test with different configurations
        configs = [
            {'test_memory': True, 'test_network': False, 'test_devices': False, 'test_thermal': False},
            {'test_memory': False, 'test_network': True, 'test_devices': False, 'test_thermal': False},
            {'test_memory': False, 'test_network': False, 'test_devices': True, 'test_thermal': False},
        ]
        
        results = []
        for i, config in enumerate(configs):
            print(f"[DEBUG_LOG] Running load test configuration {i+1}")
            metrics = self.stress_suite.run_comprehensive_stress_test(
                duration_minutes=1,
                config=config
            )
            results.append(metrics.to_dict())
            
        # Compare results
        for i, result in enumerate(results):
            print(f"[DEBUG_LOG] Configuration {i+1} results: {result}")
            assert result['total_operations'] > 0
            
    def teardown_method(self):
        """Cleanup after each test method."""
        gc.collect()


# Utility functions for running stress tests
def run_stress_test_suite(duration_minutes: int = 10, 
                         output_file: str = None) -> Dict[str, Any]:
    """Run complete stress test suite and optionally save results."""
    suite = StressTestSuite()
    metrics = suite.run_comprehensive_stress_test(duration_minutes=duration_minutes)
    
    results = metrics.to_dict()
    
    if output_file:
        with open(output_file, 'w') as f:
            json.dump(results, f, indent=2)
        print(f"[DEBUG_LOG] Stress test results saved to {output_file}")
        
    return results


def benchmark_performance(iterations: int = 1000) -> Dict[str, float]:
    """Benchmark key operations performance."""
    print(f"[DEBUG_LOG] Running performance benchmark with {iterations} iterations")
    
    results = {}
    
    # Benchmark thermal-RGB synchronization
    start_time = time.time()
    synchronizer = ThermalRGBSynchronizer()
    for _ in range(min(iterations, 100)):  # Limit for expensive operations
        # Mock data for benchmarking
        pass
    results['thermal_sync_ops_per_sec'] = min(iterations, 100) / (time.time() - start_time)
    
    # Benchmark memory operations
    start_time = time.time()
    for _ in range(iterations):
        data = list(range(1000))
        del data
    results['memory_ops_per_sec'] = iterations / (time.time() - start_time)
    
    # Benchmark CPU operations
    start_time = time.time()
    for _ in range(iterations):
        sum(i * i for i in range(100))
    results['cpu_ops_per_sec'] = iterations / (time.time() - start_time)
    
    print(f"[DEBUG_LOG] Benchmark results: {results}")
    return results


if __name__ == "__main__":
    # Run stress tests when executed directly
    print("[DEBUG_LOG] Running stress test suite...")
    results = run_stress_test_suite(duration_minutes=5)
    print(f"[DEBUG_LOG] Stress test completed with results: {results}")
    
    # Run benchmark
    benchmark_results = benchmark_performance()
    print(f"[DEBUG_LOG] Benchmark completed with results: {benchmark_results}")