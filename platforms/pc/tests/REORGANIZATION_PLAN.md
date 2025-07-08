# Test Reorganization Plan

## Current Issues
1. **Incomplete previous reorganization**: Only 6 files were moved, many remain at root level
2. **Mixed test types**: Files contain both unit and integration tests mixed together
3. **Inconsistent patterns**: Some use unittest framework, others are standalone scripts
4. **Poor categorization**: Many files at root level should be properly categorized
5. **Duplicate functionality**: Multiple similar test files (e.g., cpp_backend tests)
6. **Redirect files**: Old files that just point to new locations should be cleaned up

## Proposed Directory Structure

```
platforms/pc/tests/
├── conftest.py                     # Pytest configuration and fixtures
├── run_tests.py                    # Main test runner
├── README.md                       # Test documentation
├── common/                         # Shared test utilities
│   ├── __init__.py
│   ├── fixtures.py                 # Common test fixtures
│   ├── mocks.py                    # Common mock objects
│   └── utils.py                    # Test utility functions
├── unit/                           # Unit tests (single component)
│   ├── __init__.py
│   ├── hardware/                   # Hardware driver unit tests
│   │   ├── __init__.py
│   │   ├── test_shimmer_pc.py      # From test_hardware_drivers.py
│   │   ├── test_webcam_pc.py       # From test_hardware_drivers.py
│   │   └── test_pc_sensor.py       # New/existing
│   ├── network/                    # Network and device management
│   │   ├── __init__.py
│   │   ├── test_device.py          # Move from root
│   │   ├── test_device_manager.py  # Move from root
│   │   └── test_local_device.py    # Move from root
│   ├── ui/                         # UI component unit tests
│   │   ├── __init__.py
│   │   └── test_ui_components.py   # Move from root
│   ├── session/                    # Session management
│   │   ├── __init__.py
│   │   └── test_session_manager.py # Move from root
│   └── discovery/                  # Device discovery (existing)
│       ├── __init__.py
│       ├── test_usb_discovery.py   # Already moved
│       └── test_usb_discovery_verification.py # Move from root
├── integration/                    # Integration tests (multiple components)
│   ├── __init__.py
│   ├── cpp_backend/                # C++ backend integration (existing)
│   │   ├── __init__.py
│   │   ├── test_cpp_backend.py     # Already moved
│   │   ├── test_cpp_integration.py # Move from root
│   │   └── test_complete_cpp_backend.py # Move from root
│   ├── hardware/                   # Hardware integration tests
│   │   ├── __init__.py
│   │   ├── test_shimmer_integration.py # Move from root
│   │   └── test_webcam_integration.py  # Move from root
│   ├── sdk/                        # SDK integration tests
│   │   ├── __init__.py
│   │   └── test_sdk_integrations.py # Move from root
│   └── system/                     # Full system integration
│       ├── __init__.py
│       └── test_system_integration.py # New comprehensive test
├── functional/                     # Functional/end-to-end tests
│   ├── __init__.py
│   ├── calibration/                # Camera calibration (existing)
│   │   ├── __init__.py
│   │   └── test_calibration.py     # Already moved
│   ├── gui/                        # GUI functional tests (existing)
│   │   ├── __init__.py
│   │   └── test_modern_gui.py      # Already moved
│   ├── video/                      # Video processing (existing)
│   │   ├── __init__.py
│   │   └── test_video_playback.py  # Already moved
│   └── workflows/                  # Complete workflow tests
│       ├── __init__.py
│       └── test_recording_workflow.py # New comprehensive workflow test
└── performance/                    # Performance and benchmarking tests
    ├── __init__.py
    ├── timing/                     # Timing tests (existing)
    │   ├── __init__.py
    │   ├── benchmark_timing_precision.py # Already moved
    │   └── test_timing_and_performance.py # Move from root
    └── benchmarks/                 # Performance benchmarks
        ├── __init__.py
        └── test_performance_benchmarks.py # New comprehensive benchmarks
```

## Files to Move/Reorganize

### Unit Tests (move to unit/)
- `test_hardware_drivers.py` → Split into `unit/hardware/test_shimmer_pc.py` and `unit/hardware/test_webcam_pc.py`
- `test_device.py` → `unit/network/test_device.py`
- `test_device_manager.py` → `unit/network/test_device_manager.py`
- `test_local_device.py` → `unit/network/test_local_device.py`
- `test_ui_components.py` → `unit/ui/test_ui_components.py`
- `test_session_manager.py` → `unit/session/test_session_manager.py`
- `test_usb_discovery_verification.py` → `unit/discovery/test_usb_discovery_verification.py`

### Integration Tests (move to integration/)
- `test_cpp_integration.py` → `integration/cpp_backend/test_cpp_integration.py`
- `test_complete_cpp_backend.py` → `integration/cpp_backend/test_complete_cpp_backend.py`
- `test_shimmer_integration.py` → `integration/hardware/test_shimmer_integration.py`
- `test_webcam_integration.py` → `integration/hardware/test_webcam_integration.py`
- `test_sdk_integrations.py` → `integration/sdk/test_sdk_integrations.py`

### Performance Tests (move to performance/)
- `test_timing_and_performance.py` → `performance/timing/test_timing_and_performance.py`

### Files to Remove/Clean Up
- `test_cpp_backend.py` (redirect file - remove)
- `test_cpp_backend_only.py` (standalone script - consolidate into integration tests)
- `test_python310_features.py` (move to unit/system/ or remove if obsolete)
- `test_structure_verification.py` (move to unit/system/ or integrate into other tests)

### Common Utilities to Create
- `common/fixtures.py` - Common test fixtures (mock devices, test data, etc.)
- `common/mocks.py` - Reusable mock objects
- `common/utils.py` - Test utility functions

## Implementation Steps

1. **Create new directory structure**
2. **Create common utilities** to reduce duplication
3. **Move and refactor unit tests** with proper imports
4. **Move and refactor integration tests** with proper imports
5. **Move and refactor performance tests** with proper imports
6. **Update test runner** to work with new structure
7. **Clean up old files** and redirect files
8. **Update documentation** and README files
9. **Verify all tests work** with new structure
10. **Update CI/CD configuration** if needed

## Benefits

1. **Clear separation** of test types (unit/integration/functional/performance)
2. **Logical grouping** by feature/component
3. **Reduced duplication** through common utilities
4. **Better maintainability** with organized structure
5. **Easier test discovery** and execution
6. **Scalable structure** for future test additions
7. **Consistent patterns** across all test files