# RGBTPhys_CPP Submodule Issue - Solution Guide

## Issue Description
When attempting to add RGBTPhys_CPP as a git submodule:
```bash
sudo git submodule add https://github.com/PhysiologicAILab/RGBTPhys_CPP
```

The following error occurs:
```
Cloning into '/Users/duyantran/workspace/fyp-gsr-unified-buccancs/libs/RGBTPhys_CPP'...
remote: Write access to repository not granted.
fatal: unable to access 'https://github.com/PhysiologicAILab/RGBTPhys_CPP/': The requested URL returned error: 403
fatal: clone of 'https://github.com/PhysiologicAILab/RGBTPhys_CPP' into submodule path '/Users/duyantran/workspace/fyp-gsr-unified-buccancs/libs/RGBTPhys_CPP' failed
```

## Root Cause Analysis
The repository `https://github.com/PhysiologicAILab/RGBTPhys_CPP` does not exist (returns 404 error when accessed). The 403 error in the git command is misleading - the actual issue is that the repository is not available at the specified URL.

## Available RGBT Functionality
**Good News**: RGBT (RGB-Thermal) functionality is already extensively available through the existing `FactorizePhys` submodule in this project.

### Current RGBT Capabilities Include:

#### 1. Pre-trained Models
- `libs/FactorizePhys/final_model_release/iBVP_RGBT_FactorizePhys_Base.pth`
- `libs/FactorizePhys/final_model_release/iBVP_RGBT_FactorizePhys_FSAM_Res.pth`

#### 2. Configuration Files
- Training configs: Multiple YAML files with `DATA_MODE: RGBT` support
- Inference configs: Ready-to-use configurations for RGBT processing
- Examples: `iBVP_iBVP_RGBT_FactorizePhys_Base.yaml`, `iBVP_iBVP_RGBT_FactorizePhys_FSAM_Res.yaml`

#### 3. Data Processing
- RGBT data loader support in `libs/FactorizePhys/dataset/data_loader/iBVPLoader.py`
- Handles combined RGB and thermal frame processing
- Supports multiple data modes: "RGBT", "RGB", "T"

#### 4. Model Architectures
- FactorizePhys with RGBT support
- EfficientPhys with RGBT capabilities
- PhysFormer with RGBT processing
- PhysNet with RGBT functionality

## Recommended Solutions

### Option 1: Use Existing FactorizePhys RGBT Functionality (Recommended)
This is the preferred solution as it provides comprehensive RGBT capabilities:

1. **For Training**: Use the RGBT training configurations in `libs/FactorizePhys/configs/train_configs/`
2. **For Inference**: Use the RGBT inference configurations in `libs/FactorizePhys/configs/infer_configs/`
3. **For Custom Implementation**: Reference the data loader implementation for RGBT processing

### Option 2: Alternative Repository Sources
If you specifically need the RGBTPhys_CPP implementation:

1. **Check for Forks**: Look for forks under the `buccancs` organization (following the pattern of other submodules)
2. **Contact Maintainers**: Reach out to the PhysiologicAILab team for repository availability
3. **Alternative Implementations**: Search for similar RGBT processing libraries

### Option 3: Manual Implementation
If neither option works, you can implement RGBT functionality using the existing FactorizePhys as a reference.

## Usage Examples

### Using RGBT with FactorizePhys
```yaml
# Example configuration snippet
DATA_MODE: RGBT  # Options: "RGBT", "RGB", "T"
MODEL_PATH: "./final_model_release/iBVP_RGBT_FactorizePhys_Base.pth"
```

### Data Processing
The existing data loader handles RGBT data automatically when `DATA_MODE: RGBT` is specified in the configuration.

## Verification Steps
To verify RGBT functionality is working:

1. Check that FactorizePhys submodule is properly initialized:
   ```bash
   git submodule status libs/FactorizePhys
   ```

2. Verify RGBT model files exist:
   ```bash
   ls libs/FactorizePhys/final_model_release/*RGBT*
   ```

3. Test RGBT configuration:
   ```bash
   ls libs/FactorizePhys/configs/*/iBVP_*RGBT*
   ```

## Conclusion
The RGBTPhys_CPP dependency is not available, but comprehensive RGBT functionality is already provided through the existing FactorizePhys submodule. This solution offers:

- ✅ Pre-trained RGBT models
- ✅ Complete configuration files
- ✅ Data processing capabilities
- ✅ Multiple model architectures
- ✅ No additional dependencies required

**Recommendation**: Proceed with the existing FactorizePhys RGBT functionality rather than waiting for the unavailable RGBTPhys_CPP repository.