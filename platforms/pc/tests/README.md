# PC Platform Test Suite

This directory contains comprehensive tests for the PC platform application, organized into logical test categories.

## Test Structure

### Unit Tests (`unit/`)
Tests for individual components and modules:
- `test_hardware/` - Hardware component tests (Shimmer, Webcam, PC sensors)
- `test_ui/` - User interface component tests
- `test_network/` - Network and device management tests
- `test_integrations/` - External integration tests (LSL, PsychoPy)
- `test_utils/` - Utility function tests

### Integration Tests (`integration/`)
Tests for component interactions and system integration:
- `test_cpp_backend/` - C++ backend integration tests
- `test_hardware_integration/` - Hardware device integration tests
- `test_ui_integration/` - UI workflow integration tests
- `test_system_integration/` - End-to-end system tests

### Performance Tests (`performance/`)
Tests for performance, timing, and benchmarking:
- `test_timing_precision.py` - Timing accuracy tests
- `test_performance_benchmarks.py` - Performance benchmarking
- `test_memory_usage.py` - Memory usage analysis

### Functional Tests (`functional/`)
Tests for specific application features:
- `test_calibration.py` - Camera calibration functionality
- `test_video_playback.py` - Video playback features
- `test_data_export.py` - Data export functionality
- `test_session_management.py` - Session management features

## Running Tests

### Run All Tests
```bash
python -m pytest platforms/pc/tests/
```

### Run Specific Test Categories
```bash
# Unit tests only
python -m pytest platforms/pc/tests/unit/

# Integration tests only
python -m pytest platforms/pc/tests/integration/

# Performance tests only
python -m pytest platforms/pc/tests/performance/

# Functional tests only
python -m pytest platforms/pc/tests/functional/
```

### Run Tests with Coverage
```bash
python -m pytest platforms/pc/tests/ --cov=src --cov-report=html
```

## Test Configuration

### Requirements
- pytest
- pytest-cov
- unittest.mock
- All application dependencies

### Environment Setup
Tests automatically set up the Python path to include the `src` directory.

## Test Guidelines

### Writing New Tests
1. Use pytest framework for new tests
2. Follow the naming convention: `test_*.py`
3. Use descriptive test method names
4. Include docstrings for test methods
5. Use appropriate fixtures for setup/teardown
6. Mock external dependencies

### Test Categories
- **Unit Tests**: Test individual functions/classes in isolation
- **Integration Tests**: Test component interactions
- **Performance Tests**: Test timing, memory, and performance
- **Functional Tests**: Test complete features end-to-end

### Best Practices
- Keep tests independent and isolated
- Use meaningful assertions with clear error messages
- Test both success and failure scenarios
- Include edge cases and boundary conditions
- Use fixtures for common setup code