#!/usr/bin/env python3
"""Test script to verify Python 3.10 features are working correctly."""

import sys
print(f'✅ Python {sys.version_info.major}.{sys.version_info.minor} detected')

# Test Python 3.10 features
print('Testing Python 3.10 features:')

# Test type hints
from typing import Optional, List, Dict, Union
def test_function(param: Optional[str] = None) -> bool:
    return param is not None
print('✅ Type hints working')

# Test match statement (Python 3.10 feature)
def test_match(value):
    match value:
        case 1:
            return 'one'
        case 2:
            return 'two'
        case _:
            return 'other'

result = test_match(1)
print(f'✅ Match statement working: {result}')

# Test union types (Python 3.10 feature)
def test_union(value: int | str) -> str:
    return str(value)
print('✅ Union types (|) working')

# Test dataclasses
from dataclasses import dataclass

@dataclass
class TestClass:
    name: str
    value: int = 0

test_obj = TestClass('test')
print(f'✅ Dataclasses working: {test_obj}')

print('✅ All Python 3.10 features verified successfully!')