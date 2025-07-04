# External SDK Libraries

This directory contains external SDK libraries required for the GSR Multimodal Android application.

## Required Libraries

### Topdon TC001 Thermal Camera SDK
- **File**: `TopdonSDK.aar` (to be provided by vendor)
- **Version**: Latest compatible version
- **Purpose**: Interface with Topdon TC001 thermal camera via USB-C
- **Documentation**: Refer to Topdon developer documentation

### Installation Instructions

1. **Obtain Topdon SDK**:
   - Contact Topdon support for the latest Android SDK
   - Download the `.aar` file from their developer portal
   - Place the file in this directory as `TopdonSDK.aar`

2. **Verify Gradle Configuration**:
   - The `build.gradle.kts` file is already configured to include all `.aar` files from this directory
   - No additional configuration should be needed

3. **SDK Integration**:
   - The MainActivity includes placeholder code for thermal camera initialization
   - Implement actual SDK calls according to Topdon documentation
   - Test with physical hardware

## Notes

- The Shimmer SDK is included via Maven dependencies and doesn't require manual installation
- Keep SDK files updated to the latest versions for optimal performance
- Ensure proper licensing compliance for all third-party SDKs