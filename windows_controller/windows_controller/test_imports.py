#!/usr/bin/env python3

import sys
import os
sys.path.append('src')

print("Testing SDK integrations...")

try:
    from integrations.lsl_integration import LSLStreamer
    print('[SUCCESS] LSL integration imported successfully')
except Exception as e:
    print(f'[ERROR] LSL integration import failed: {e}')

try:
    from integrations.psychopy_integration import ExperimentController
    print('[SUCCESS] PsychoPy integration imported successfully')
except Exception as e:
    print(f'[ERROR] PsychoPy integration import failed: {e}')

try:
    from integrations.shimmer_integration import ShimmerSensor
    print('[SUCCESS] Shimmer integration imported successfully')
except Exception as e:
    print(f'[ERROR] Shimmer integration import failed: {e}')

print('[INFO] SDK integration test completed')