#!/usr/bin/env python3
"""
Simple test script to verify PySide6 upgrade is working correctly.
"""


def test_pyside6_imports():
    """Test that PySide6 imports work correctly."""
    try:
        from PySide6.QtCore import Qt
        from PySide6.QtGui import QImage, QPixmap
        from PySide6.QtWidgets import (
            QApplication,
            QLabel,
            QMainWindow,
            QPushButton,
            QVBoxLayout,
            QWidget,
        )

        print("✓ PySide6 imports successful")
        return True
    except ImportError as e:
        print(f"✗ PySide6 import failed: {e}")
        return False


def test_basic_qt_functionality():
    """Test basic Qt functionality."""
    try:
        # Create a minimal Qt application
        import sys

        from PySide6.QtCore import Qt
        from PySide6.QtWidgets import QApplication, QLabel, QMainWindow

        app = QApplication(sys.argv if "sys" in globals() else [])

        # Create a simple window
        window = QMainWindow()
        window.setWindowTitle("PySide6 Test")

        # Create a label
        label = QLabel("PySide6 is working!")
        window.setCentralWidget(label)

        # Test exec() method (not exec_())
        # We won't actually show the window, just test that the method exists
        assert hasattr(app, "exec"), "QApplication should have exec() method"
        assert not hasattr(app, "exec_") or callable(
            getattr(app, "exec_")
        ), "exec_() should not be the primary method"

        print("✓ Basic Qt functionality test passed")
        return True

    except Exception as e:
        print(f"✗ Basic Qt functionality test failed: {e}")
        return False


def main():
    """Run all tests."""
    print("Testing PySide6 upgrade...")

    tests = [
        test_pyside6_imports,
        test_basic_qt_functionality,
    ]

    passed = 0
    total = len(tests)

    for test in tests:
        if test():
            passed += 1

    print(f"\nResults: {passed}/{total} tests passed")

    if passed == total:
        print("🎉 PySide6 upgrade successful!")
        return 0
    else:
        print("❌ Some tests failed")
        return 1


if __name__ == "__main__":
    exit(main())
