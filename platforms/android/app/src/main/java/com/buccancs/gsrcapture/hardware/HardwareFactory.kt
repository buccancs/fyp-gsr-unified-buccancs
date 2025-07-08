package com.buccancs.gsrcapture.hardware

import android.bluetooth.BluetoothManager
import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.buccancs.gsrcapture.hardware.interfaces.PhysiologicalSensor
import com.buccancs.gsrcapture.hardware.interfaces.ThermalCamera
import java.util.concurrent.ExecutorService

/**
 * Hardware abstraction factory that detects available hardware
 * and creates appropriate interface implementations.
 * This allows the system to support multiple hardware types dynamically.
 */
class HardwareFactory(
    private val context: Context,
) {
    companion object {
        private const val TAG = "HardwareFactory"

        // Known hardware identifiers
        private const val SHIMMER_DEVICE_NAME_PREFIX = "Shimmer"
        private const val TOPDON_VENDOR_ID = 0x2E42 // Example vendor ID for Topdon
        private const val TOPDON_PRODUCT_ID = 0x0001 // Example product ID for TC001
    }

    /**
     * Enum representing supported hardware types
     */
    enum class HardwareType {
        SHIMMER_GSR,
        TOPDON_THERMAL,
        GENERIC_THERMAL,
        UNKNOWN,
    }

    /**
     * Data class representing detected hardware
     */
    data class DetectedHardware(
        val type: HardwareType,
        val identifier: String,
        val name: String,
        val metadata: Map<String, Any> = emptyMap(),
    )

    /**
     * Detect available physiological sensors
     * @return List of detected sensor hardware
     */
    fun detectPhysiologicalSensors(): List<DetectedHardware> {
        val detectedSensors = mutableListOf<DetectedHardware>()

        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter?.isEnabled == true) {
                // Check for paired Shimmer devices
                val pairedDevices = bluetoothAdapter.bondedDevices
                for (device in pairedDevices) {
                    if (device.name?.startsWith(SHIMMER_DEVICE_NAME_PREFIX) == true) {
                        detectedSensors.add(
                            DetectedHardware(
                                type = HardwareType.SHIMMER_GSR,
                                identifier = device.address,
                                name = device.name ?: "Unknown Shimmer Device",
                                metadata =
                                    mapOf(
                                        "bluetoothAddress" to device.address,
                                        "deviceClass" to device.bluetoothClass.toString(),
                                    ),
                            ),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting physiological sensors", e)
        }

        return detectedSensors
    }

    /**
     * Detect available thermal cameras
     * @return List of detected thermal camera hardware
     */
    fun detectThermalCameras(): List<DetectedHardware> {
        val detectedCameras = mutableListOf<DetectedHardware>()

        try {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
            val deviceList = usbManager?.deviceList

            deviceList?.values?.forEach { usbDevice ->
                when {
                    // Check for Topdon TC001
                    usbDevice.vendorId == TOPDON_VENDOR_ID && usbDevice.productId == TOPDON_PRODUCT_ID -> {
                        detectedCameras.add(
                            DetectedHardware(
                                type = HardwareType.TOPDON_THERMAL,
                                identifier = usbDevice.deviceName,
                                name = "Topdon TC001 Thermal Camera",
                                metadata =
                                    mapOf(
                                        "vendorId" to usbDevice.vendorId,
                                        "productId" to usbDevice.productId,
                                        "deviceName" to usbDevice.deviceName,
                                    ),
                            ),
                        )
                    }
                    // Check for other thermal cameras (generic USB thermal devices)
                    isGenericThermalCamera(usbDevice.vendorId, usbDevice.productId) -> {
                        detectedCameras.add(
                            DetectedHardware(
                                type = HardwareType.GENERIC_THERMAL,
                                identifier = usbDevice.deviceName,
                                name = "Generic Thermal Camera",
                                metadata =
                                    mapOf(
                                        "vendorId" to usbDevice.vendorId,
                                        "productId" to usbDevice.productId,
                                        "deviceName" to usbDevice.deviceName,
                                    ),
                            ),
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting thermal cameras", e)
        }

        return detectedCameras
    }

    /**
     * Create a physiological sensor instance based on detected hardware
     * @param hardware Detected hardware information
     * @param executor ExecutorService for background operations
     * @return PhysiologicalSensor instance or null if creation failed
     */
    fun createPhysiologicalSensor(
        hardware: DetectedHardware,
        executor: ExecutorService,
    ): PhysiologicalSensor? =
        try {
            when (hardware.type) {
                HardwareType.SHIMMER_GSR -> {
                    // Create Shimmer-specific implementation
                    createShimmerSensor(hardware, executor)
                }
                else -> {
                    Log.w(TAG, "Unsupported physiological sensor type: ${hardware.type}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating physiological sensor", e)
            null
        }

    /**
     * Create a thermal camera instance based on detected hardware
     * @param hardware Detected hardware information
     * @param executor ExecutorService for background operations
     * @return ThermalCamera instance or null if creation failed
     */
    fun createThermalCamera(
        hardware: DetectedHardware,
        executor: ExecutorService,
    ): ThermalCamera? =
        try {
            when (hardware.type) {
                HardwareType.TOPDON_THERMAL -> {
                    // Create Topdon-specific implementation
                    createTopdonThermalCamera(hardware, executor)
                }
                HardwareType.GENERIC_THERMAL -> {
                    // Create generic thermal camera implementation
                    createGenericThermalCamera(hardware, executor)
                }
                else -> {
                    Log.w(TAG, "Unsupported thermal camera type: ${hardware.type}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating thermal camera", e)
            null
        }

    /**
     * Get all available hardware (sensors and cameras)
     * @return List of all detected hardware
     */
    fun detectAllHardware(): List<DetectedHardware> {
        val allHardware = mutableListOf<DetectedHardware>()
        allHardware.addAll(detectPhysiologicalSensors())
        allHardware.addAll(detectThermalCameras())
        return allHardware
    }

    // Private helper methods

    private fun isGenericThermalCamera(
        vendorId: Int,
        productId: Int,
    ): Boolean {
        // Add logic to identify other thermal camera vendors/products
        // This is a placeholder - in a real implementation, you would maintain
        // a list of known thermal camera vendor/product ID combinations
        return false
    }

    private fun createShimmerSensor(
        hardware: DetectedHardware,
        executor: ExecutorService,
    ): PhysiologicalSensor? =
        try {
            val shimmerSensor =
                com.buccancs.gsrcapture.hardware.impl.ShimmerPhysiologicalSensor(
                    context = context,
                    executor = executor,
                )

            if (shimmerSensor.initialize()) {
                Log.d(TAG, "Created Shimmer sensor for ${hardware.identifier}")
                shimmerSensor
            } else {
                Log.e(TAG, "Failed to initialize Shimmer sensor")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Shimmer sensor", e)
            null
        }

    private fun createTopdonThermalCamera(
        hardware: DetectedHardware,
        executor: ExecutorService,
    ): ThermalCamera? =
        try {
            val topdonCamera =
                com.buccancs.gsrcapture.hardware.impl.TopdonThermalCamera(
                    context = context,
                    executor = executor,
                )

            if (topdonCamera.initialize()) {
                Log.d(TAG, "Created Topdon thermal camera for ${hardware.identifier}")
                topdonCamera
            } else {
                Log.e(TAG, "Failed to initialize Topdon thermal camera")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Topdon thermal camera", e)
            null
        }

    private fun createGenericThermalCamera(
        hardware: DetectedHardware,
        executor: ExecutorService,
    ): ThermalCamera? =
        try {
            // Create device info map from hardware metadata
            val deviceInfo = mutableMapOf<String, Any>()
            deviceInfo.putAll(hardware.metadata)
            deviceInfo["model"] = hardware.name

            val genericCamera =
                com.buccancs.gsrcapture.hardware.impl.GenericThermalCamera(
                    context = context,
                    executor = executor,
                    deviceInfo = deviceInfo,
                )

            if (genericCamera.initialize()) {
                Log.d(TAG, "Created generic thermal camera for ${hardware.identifier}")
                genericCamera
            } else {
                Log.e(TAG, "Failed to initialize generic thermal camera")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating generic thermal camera", e)
            null
        }
}
