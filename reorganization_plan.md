# Project Reorganization Plan

## Current Issues Identified

1. **Multiple Windows directories**: `windows/`, `windows_controller/`, `<FILENAME>windows_controller`
2. **Scattered shared code**: `src/` and `shared/` both contain shared functionality
3. **Loose files at root level**: Test files, documentation, virtual environments
4. **Nested redundancy**: `windows_controller/windows_controller/`
5. **Multiple virtual environments**: Root `venv/` and `windows_controller/venv/`

## Proposed New Structure

```
fyp-gsr-unified-buccancs/
├── README.md
├── LICENSE
├── .gitignore
├── .github/
├── docs/
│   ├── CHANGELOG.md
│   ├── DEVELOPMENT_SETUP.md
│   ├── DOCUMENTATION_ORGANIZATION_PLAN.md
│   ├── DOCUMENTATION_ORGANIZATION_SUMMARY.md
│   └── missing_features.txt
├── platforms/
│   ├── android/
│   │   └── [existing android structure]
│   └── windows/
│       ├── src/
│       ├── tests/
│       ├── requirements.txt
│       ├── requirements-test.txt
│       ├── setup.py
│       └── README.md
├── shared/
│   ├── network/
│   │   ├── common/
│   │   ├── android/
│   │   └── windows/
│   └── utils/
├── tests/
│   ├── integration/
│   └── validation/
├── tools/
│   ├── setup_python_env.py
│   └── calibration/
└── environments/
    └── [virtual environments if needed]
```

## Reorganization Steps

1. **Consolidate Windows components**:
   - Merge `windows_controller/` as the main Windows platform
   - Remove redundant `windows/` and corrupted `<FILENAME>windows_controller`
   - Move to `platforms/windows/`

2. **Organize shared code**:
   - Merge `src/` and `shared/` into a unified `shared/` directory
   - Organize by functionality (network, utils)

3. **Clean up root directory**:
   - Move documentation to `docs/`
   - Move test files to `tests/`
   - Move tools to `tools/`
   - Remove redundant virtual environments

4. **Update references**:
   - Update import statements
   - Update build configurations
   - Update documentation paths

## Benefits

- Clear separation between platforms
- Consolidated shared code
- Clean root directory
- Logical grouping of related files
- Easier navigation and maintenance