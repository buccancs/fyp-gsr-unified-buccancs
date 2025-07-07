#!/usr/bin/env python3
"""
Comprehensive Test Runner for PC Platform

This script provides a unified way to run all tests in the PC platform
with various options for filtering and reporting.
"""

import sys
import os
import argparse
import subprocess
import time
from pathlib import Path

# Add the src directory to Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))


def run_command(command, description):
    """Run a command and return the result."""
    print(f"\n{'='*60}")
    print(f"Running: {description}")
    print(f"Command: {' '.join(command)}")
    print(f"{'='*60}")
    
    start_time = time.time()
    result = subprocess.run(command, capture_output=True, text=True)
    end_time = time.time()
    
    print(f"Duration: {end_time - start_time:.2f} seconds")
    print(f"Return code: {result.returncode}")
    
    if result.stdout:
        print(f"\nSTDOUT:\n{result.stdout}")
    
    if result.stderr:
        print(f"\nSTDERR:\n{result.stderr}")
    
    return result.returncode == 0


def run_unit_tests(verbose=False, coverage=False):
    """Run unit tests."""
    command = ['python', '-m', 'pytest', 'unit/']
    
    if verbose:
        command.append('-v')
    
    if coverage:
        command.extend(['--cov=../src', '--cov-report=html', '--cov-report=term'])
    
    return run_command(command, "Unit Tests")


def run_integration_tests(verbose=False):
    """Run integration tests."""
    command = ['python', '-m', 'pytest', 'integration/']
    
    if verbose:
        command.append('-v')
    
    return run_command(command, "Integration Tests")


def run_functional_tests(verbose=False):
    """Run functional tests."""
    command = ['python', '-m', 'pytest', 'functional/']
    
    if verbose:
        command.append('-v')
    
    return run_command(command, "Functional Tests")


def run_performance_tests(verbose=False):
    """Run performance tests."""
    command = ['python', '-m', 'pytest', 'performance/']
    
    if verbose:
        command.append('-v')
    
    return run_command(command, "Performance Tests")


def run_all_tests(verbose=False, coverage=False):
    """Run all tests."""
    command = ['python', '-m', 'pytest', '.']
    
    if verbose:
        command.append('-v')
    
    if coverage:
        command.extend(['--cov=../src', '--cov-report=html', '--cov-report=term'])
    
    return run_command(command, "All Tests")


def run_specific_test(test_path, verbose=False):
    """Run a specific test file or test method."""
    command = ['python', '-m', 'pytest', test_path]
    
    if verbose:
        command.append('-v')
    
    return run_command(command, f"Specific Test: {test_path}")


def check_dependencies():
    """Check if required dependencies are available."""
    print("Checking test dependencies...")
    
    required_packages = ['pytest', 'pytest-cov']
    missing_packages = []
    
    for package in required_packages:
        try:
            __import__(package.replace('-', '_'))
            print(f"✓ {package} is available")
        except ImportError:
            print(f"✗ {package} is missing")
            missing_packages.append(package)
    
    if missing_packages:
        print(f"\nMissing packages: {', '.join(missing_packages)}")
        print("Install them with: pip install " + " ".join(missing_packages))
        return False
    
    return True


def generate_test_report():
    """Generate a comprehensive test report."""
    print("\n" + "="*60)
    print("GENERATING COMPREHENSIVE TEST REPORT")
    print("="*60)
    
    # Run all tests with coverage
    command = [
        'python', '-m', 'pytest', '.',
        '--cov=../src',
        '--cov-report=html',
        '--cov-report=term',
        '--cov-report=xml',
        '--junit-xml=test_results.xml',
        '-v'
    ]
    
    return run_command(command, "Comprehensive Test Report")


def clean_test_artifacts():
    """Clean up test artifacts."""
    print("Cleaning up test artifacts...")
    
    artifacts = [
        'htmlcov',
        'coverage.xml',
        'test_results.xml',
        '.coverage',
        '.pytest_cache',
        '__pycache__'
    ]
    
    for artifact in artifacts:
        artifact_path = Path(artifact)
        if artifact_path.exists():
            if artifact_path.is_dir():
                import shutil
                shutil.rmtree(artifact_path)
                print(f"Removed directory: {artifact}")
            else:
                artifact_path.unlink()
                print(f"Removed file: {artifact}")


def main():
    """Main function."""
    parser = argparse.ArgumentParser(
        description="Comprehensive Test Runner for PC Platform",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python run_tests.py --all                    # Run all tests
  python run_tests.py --unit --coverage        # Run unit tests with coverage
  python run_tests.py --integration --verbose  # Run integration tests with verbose output
  python run_tests.py --specific unit/test_network/test_device_manager.py
  python run_tests.py --report                 # Generate comprehensive report
  python run_tests.py --clean                  # Clean up test artifacts
        """
    )
    
    # Test categories
    parser.add_argument('--all', action='store_true', help='Run all tests')
    parser.add_argument('--unit', action='store_true', help='Run unit tests')
    parser.add_argument('--integration', action='store_true', help='Run integration tests')
    parser.add_argument('--functional', action='store_true', help='Run functional tests')
    parser.add_argument('--performance', action='store_true', help='Run performance tests')
    parser.add_argument('--specific', type=str, help='Run specific test file or method')
    
    # Options
    parser.add_argument('--verbose', '-v', action='store_true', help='Verbose output')
    parser.add_argument('--coverage', '-c', action='store_true', help='Generate coverage report')
    parser.add_argument('--report', action='store_true', help='Generate comprehensive test report')
    parser.add_argument('--clean', action='store_true', help='Clean up test artifacts')
    parser.add_argument('--check-deps', action='store_true', help='Check test dependencies')
    
    args = parser.parse_args()
    
    # Change to tests directory
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    
    # Handle special commands
    if args.clean:
        clean_test_artifacts()
        return
    
    if args.check_deps:
        if check_dependencies():
            print("All dependencies are available!")
        else:
            sys.exit(1)
        return
    
    if args.report:
        success = generate_test_report()
        sys.exit(0 if success else 1)
    
    # Check dependencies before running tests
    if not check_dependencies():
        print("Please install missing dependencies before running tests.")
        sys.exit(1)
    
    # Run tests based on arguments
    success = True
    
    if args.all:
        success = run_all_tests(args.verbose, args.coverage)
    elif args.unit:
        success = run_unit_tests(args.verbose, args.coverage)
    elif args.integration:
        success = run_integration_tests(args.verbose)
    elif args.functional:
        success = run_functional_tests(args.verbose)
    elif args.performance:
        success = run_performance_tests(args.verbose)
    elif args.specific:
        success = run_specific_test(args.specific, args.verbose)
    else:
        # Default: run all tests
        print("No specific test category specified. Running all tests...")
        success = run_all_tests(args.verbose, args.coverage)
    
    # Exit with appropriate code
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()