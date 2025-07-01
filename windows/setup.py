from setuptools import setup, find_packages

setup(
    name="gsr-windows-controller",
    version="0.1.0",
    packages=find_packages(),
    install_requires=[
        "opencv-python>=4.5.0",
        "numpy>=1.20.0",
        "pyqt5>=5.15.0",
        "pyserial>=3.5",
        "pylsl>=1.16.0",  # Optional: Lab Streaming Layer for sync
    ],
    author="GSR Project Team",
    author_email="example@example.com",
    description="Windows controller app for GSR and dual-video recording system",
    keywords="gsr, thermal, video, recording",
    python_requires=">=3.8",
)