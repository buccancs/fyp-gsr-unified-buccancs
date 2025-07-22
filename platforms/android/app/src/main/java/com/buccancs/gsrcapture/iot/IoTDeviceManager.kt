package com.buccancs.gsrcapture.iot

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Data class representing an IoT device
 */
data class IoTDevice(
    val id: String,
    val name: String,
    val type: DeviceType,
    val connectionType: ConnectionType,
    val address: String,
    val capabilities: List<DeviceCapability>,
    val status: DeviceStatus,
    val lastSeen: Long,
    val batteryLevel: Int? = null,
    val firmwareVersion: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Enum representing device types
 */
enum class DeviceType(val displayName: String) {
    ENVIRONMENTAL_SENSOR("Environmental Sensor"),
    WEARABLE_DEVICE("Wearable Device"),
    SMART_CAMERA("Smart Camera"),
    BIOMETRIC_SENSOR("Biometric Sensor"),
    TEMPERATURE_SENSOR("Temperature Sensor"),
    HUMIDITY_SENSOR("Humidity Sensor"),
    LIGHT_SENSOR("Light Sensor"),
    MOTION_DETECTOR("Motion Detector"),
    SMART_WATCH("Smart Watch"),
    FITNESS_TRACKER("Fitness Tracker"),
    HEART_RATE_MONITOR("Heart Rate Monitor"),
    BLOOD_PRESSURE_MONITOR("Blood Pressure Monitor"),
    SMART_SCALE("Smart Scale"),
    AIR_QUALITY_MONITOR("Air Quality Monitor"),
    NOISE_LEVEL_METER("Noise Level Meter"),
    UNKNOWN("Unknown Device")
}

/**
 * Enum representing connection types
 */
enum class ConnectionType {
    BLUETOOTH_LE,
    BLUETOOTH_CLASSIC,
    WIFI,
    ZIGBEE,
    MQTT,
    HTTP_REST,
    WEBSOCKET,
    USB,
    SERIAL
}

/**
 * Enum representing device capabilities
 */
enum class DeviceCapability {
    TEMPERATURE_READING,
    HUMIDITY_READING,
    PRESSURE_READING,
    HEART_RATE_MONITORING,
    STEP_COUNTING,
    SLEEP_TRACKING,
    GPS_TRACKING,
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    LIGHT_SENSING,
    PROXIMITY_SENSING,
    SOUND_RECORDING,
    IMAGE_CAPTURE,
    VIDEO_RECORDING,
    VIBRATION_FEEDBACK,
    LED_INDICATION,
    DISPLAY_OUTPUT,
    BUTTON_INPUT,
    TOUCH_INPUT,
    VOICE_RECOGNITION,
    GESTURE_RECOGNITION,
    AIR_QUALITY_MONITORING,
    NOISE_LEVEL_MONITORING,
    MOTION_DETECTION,
    FALL_DETECTION,
    MEDICATION_REMINDER,
    EMERGENCY_ALERT
}

/**
 * Enum representing device status
 */
enum class DeviceStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    DISCOVERING,
    ERROR,
    LOW_BATTERY,
    UPDATING_FIRMWARE
}

/**
 * Data class representing device data
 */
data class DeviceData(
    val deviceId: String,
    val timestamp: Long,
    val dataType: String,
    val value: Any,
    val unit: String? = null,
    val quality: DataQuality = DataQuality.GOOD,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Enum representing data quality
 */
enum class DataQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    INVALID
}

/**
 * Configuration for IoT device management
 */
data class IoTConfig(
    val enableAutoDiscovery: Boolean = true,
    val discoveryInterval: Long = 30000, // 30 seconds
    val connectionTimeout: Long = 10000, // 10 seconds
    val dataRetentionPeriod: Long = 86400000, // 24 hours
    val maxDevices: Int = 50,
    val enableDataValidation: Boolean = true,
    val enableEncryption: Boolean = true,
    val autoReconnect: Boolean = true,
    val batteryThreshold: Int = 20 // Low battery threshold
)

/**
 * IoT Device Manager for comprehensive device integration
 */
class IoTDeviceManager(
    private val context: Context,
    private val config: IoTConfig = IoTConfig()
) {
    companion object {
        private const val TAG = "IoTDeviceManager"
        private const val DISCOVERY_SERVICE_UUID = "0000180F-0000-1000-8000-00805F9B34FB"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    // Device management
    private val discoveredDevices = ConcurrentHashMap<String, IoTDevice>()
    private val connectedDevices = ConcurrentHashMap<String, IoTDevice>()
    private val deviceConnections = ConcurrentHashMap<String, Any>() // Stores actual connections
    private val deviceData = ConcurrentHashMap<String, MutableList<DeviceData>>()

    // Discovery state
    private val isDiscovering = AtomicBoolean(false)
    private val isScanning = AtomicBoolean(false)

    // Callbacks
    private var deviceDiscoveredCallback: ((IoTDevice) -> Unit)? = null
    private var deviceConnectedCallback: ((IoTDevice) -> Unit)? = null
    private var deviceDisconnectedCallback: ((IoTDevice) -> Unit)? = null
    private var dataReceivedCallback: ((DeviceData) -> Unit)? = null
    private var errorCallback: ((String, Exception?) -> Unit)? = null

    init {
        if (config.enableAutoDiscovery) {
            startAutoDiscovery()
        }
        startDataCleanup()
    }

    /**
     * Start automatic device discovery
     */
    private fun startAutoDiscovery() {
        scope.launch {
            while (isActive) {
                try {
                    if (!isDiscovering.get()) {
                        discoverDevices()
                    }
                    delay(config.discoveryInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in auto discovery", e)
                    errorCallback?.invoke("Auto discovery error", e)
                    delay(60000) // Wait 1 minute before retry
                }
            }
        }
    }

    /**
     * Start data cleanup routine
     */
    private fun startDataCleanup() {
        scope.launch {
            while (isActive) {
                try {
                    cleanupOldData()
                    delay(3600000) // Run every hour
                } catch (e: Exception) {
                    Log.e(TAG, "Error in data cleanup", e)
                }
            }
        }
    }

    /**
     * Discover available IoT devices
     */
    fun discoverDevices() {
        if (isDiscovering.getAndSet(true)) {
            Log.w(TAG, "Discovery already in progress")
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Starting device discovery")

                // Discover Bluetooth devices
                discoverBluetoothDevices()

                // Discover WiFi devices
                discoverWiFiDevices()

                // Discover MQTT devices
                discoverMqttDevices()

                Log.d(TAG, "Device discovery completed. Found ${discoveredDevices.size} devices")

            } catch (e: Exception) {
                Log.e(TAG, "Error during device discovery", e)
                errorCallback?.invoke("Device discovery failed", e)
            } finally {
                isDiscovering.set(false)
            }
        }
    }

    /**
     * Discover Bluetooth devices
     */
    private suspend fun discoverBluetoothDevices() = withContext(Dispatchers.Main) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.w(TAG, "Bluetooth not available or disabled")
            return@withContext
        }

        try {
            // Scan for BLE devices
            val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            if (bluetoothLeScanner != null && !isScanning.getAndSet(true)) {

                val scanCallback = object : android.bluetooth.le.ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: android.bluetooth.le.ScanResult) {
                        val device = result.device
                        val rssi = result.rssi

                        val iotDevice = createIoTDeviceFromBluetooth(device, rssi)
                        addDiscoveredDevice(iotDevice)
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e(TAG, "BLE scan failed with error code: $errorCode")
                        isScanning.set(false)
                    }
                }

                bluetoothLeScanner.startScan(scanCallback)

                // Stop scanning after 10 seconds
                delay(10000)
                bluetoothLeScanner.stopScan(scanCallback)
                isScanning.set(false)
            }

            // Also check bonded devices
            bluetoothAdapter.bondedDevices?.forEach { device ->
                val iotDevice = createIoTDeviceFromBluetooth(device, -50) // Assume good signal for bonded devices
                addDiscoveredDevice(iotDevice)
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
            errorCallback?.invoke("Bluetooth permission required", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering Bluetooth devices", e)
            errorCallback?.invoke("Bluetooth discovery failed", e)
        }
    }

    /**
     * Discover WiFi devices using network scanning
     */
    private suspend fun discoverWiFiDevices() {
        try {
            // Scan local network for IoT devices
            val localNetwork = getLocalNetworkRange()
            if (localNetwork != null) {
                scanNetworkRange(localNetwork)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error discovering WiFi devices", e)
            errorCallback?.invoke("WiFi discovery failed", e)
        }
    }

    /**
     * Discover MQTT devices
     */
    private suspend fun discoverMqttDevices() {
        try {
            // This would connect to MQTT brokers and discover devices
            // Implementation would depend on specific MQTT setup
            Log.d(TAG, "MQTT device discovery not implemented yet")

        } catch (e: Exception) {
            Log.e(TAG, "Error discovering MQTT devices", e)
        }
    }

    /**
     * Connect to a specific IoT device
     */
    fun connectToDevice(deviceId: String) {
        val device = discoveredDevices[deviceId]
        if (device == null) {
            errorCallback?.invoke("Device not found: $deviceId", null)
            return
        }

        scope.launch {
            try {
                Log.d(TAG, "Connecting to device: ${device.name}")

                val success = when (device.connectionType) {
                    ConnectionType.BLUETOOTH_LE -> connectBluetoothLE(device)
                    ConnectionType.BLUETOOTH_CLASSIC -> connectBluetoothClassic(device)
                    ConnectionType.WIFI -> connectWiFi(device)
                    ConnectionType.HTTP_REST -> connectHttpRest(device)
                    ConnectionType.WEBSOCKET -> connectWebSocket(device)
                    ConnectionType.MQTT -> connectMqtt(device)
                    else -> {
                        Log.w(TAG, "Unsupported connection type: ${device.connectionType}")
                        false
                    }
                }

                if (success) {
                    val connectedDevice = device.copy(status = DeviceStatus.CONNECTED)
                    connectedDevices[deviceId] = connectedDevice
                    deviceConnectedCallback?.invoke(connectedDevice)
                    Log.d(TAG, "Successfully connected to ${device.name}")
                } else {
                    errorCallback?.invoke("Failed to connect to ${device.name}", null)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to device", e)
                errorCallback?.invoke("Connection failed: ${e.message}", e)
            }
        }
    }

    /**
     * Disconnect from a device
     */
    fun disconnectFromDevice(deviceId: String) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            Log.w(TAG, "Device not connected: $deviceId")
            return
        }

        scope.launch {
            try {
                val connection = deviceConnections[deviceId]
                when (device.connectionType) {
                    ConnectionType.BLUETOOTH_LE -> {
                        (connection as? BluetoothGatt)?.disconnect()
                    }
                    ConnectionType.WIFI, ConnectionType.HTTP_REST -> {
                        (connection as? Socket)?.close()
                    }
                    ConnectionType.WEBSOCKET -> {
                        // Close WebSocket connection
                    }
                    else -> {
                        Log.w(TAG, "Disconnect not implemented for ${device.connectionType}")
                    }
                }

                deviceConnections.remove(deviceId)
                connectedDevices.remove(deviceId)

                val disconnectedDevice = device.copy(status = DeviceStatus.DISCONNECTED)
                deviceDisconnectedCallback?.invoke(disconnectedDevice)

                Log.d(TAG, "Disconnected from ${device.name}")

            } catch (e: Exception) {
                Log.e(TAG, "Error disconnecting from device", e)
                errorCallback?.invoke("Disconnect failed: ${e.message}", e)
            }
        }
    }

    /**
     * Send command to a connected device
     */
    fun sendCommand(deviceId: String, command: String, parameters: Map<String, Any> = emptyMap()) {
        val device = connectedDevices[deviceId]
        if (device == null) {
            errorCallback?.invoke("Device not connected: $deviceId", null)
            return
        }

        scope.launch {
            try {
                val commandData = JSONObject().apply {
                    put("command", command)
                    put("parameters", JSONObject(parameters))
                    put("timestamp", System.currentTimeMillis())
                }

                when (device.connectionType) {
                    ConnectionType.BLUETOOTH_LE -> sendBluetoothCommand(deviceId, commandData.toString())
                    ConnectionType.HTTP_REST -> sendHttpCommand(deviceId, commandData.toString())
                    ConnectionType.WEBSOCKET -> sendWebSocketCommand(deviceId, commandData.toString())
                    ConnectionType.MQTT -> sendMqttCommand(deviceId, commandData.toString())
                    else -> {
                        Log.w(TAG, "Command sending not implemented for ${device.connectionType}")
                    }
                }

                Log.d(TAG, "Sent command '$command' to ${device.name}")

            } catch (e: Exception) {
                Log.e(TAG, "Error sending command", e)
                errorCallback?.invoke("Command failed: ${e.message}", e)
            }
        }
    }

    /**
     * Get data from a specific device
     */
    fun getDeviceData(deviceId: String, dataType: String? = null, timeRange: Pair<Long, Long>? = null): List<DeviceData> {
        val data = deviceData[deviceId] ?: return emptyList()

        return data.filter { dataPoint ->
            val typeMatch = dataType == null || dataPoint.dataType == dataType
            val timeMatch = timeRange == null || dataPoint.timestamp in timeRange.first..timeRange.second
            typeMatch && timeMatch
        }
    }

    /**
     * Get all discovered devices
     */
    fun getDiscoveredDevices(): List<IoTDevice> {
        return discoveredDevices.values.toList()
    }

    /**
     * Get all connected devices
     */
    fun getConnectedDevices(): List<IoTDevice> {
        return connectedDevices.values.toList()
    }

    /**
     * Get device by ID
     */
    fun getDevice(deviceId: String): IoTDevice? {
        return connectedDevices[deviceId] ?: discoveredDevices[deviceId]
    }

    // Private helper methods

    private fun createIoTDeviceFromBluetooth(bluetoothDevice: BluetoothDevice, rssi: Int): IoTDevice {
        val deviceType = inferDeviceType(bluetoothDevice.name ?: "Unknown")
        val capabilities = inferCapabilities(deviceType, bluetoothDevice)

        return IoTDevice(
            id = bluetoothDevice.address,
            name = bluetoothDevice.name ?: "Unknown Bluetooth Device",
            type = deviceType,
            connectionType = ConnectionType.BLUETOOTH_LE,
            address = bluetoothDevice.address,
            capabilities = capabilities,
            status = DeviceStatus.DISCOVERING,
            lastSeen = System.currentTimeMillis(),
            metadata = mapOf(
                "rssi" to rssi,
                "bluetooth_class" to (bluetoothDevice.bluetoothClass?.deviceClass ?: -1)
            )
        )
    }

    private fun inferDeviceType(deviceName: String): DeviceType {
        val name = deviceName.lowercase()
        return when {
            name.contains("heart") || name.contains("hr") -> DeviceType.HEART_RATE_MONITOR
            name.contains("watch") -> DeviceType.SMART_WATCH
            name.contains("fitness") || name.contains("tracker") -> DeviceType.FITNESS_TRACKER
            name.contains("temp") || name.contains("thermometer") -> DeviceType.TEMPERATURE_SENSOR
            name.contains("humidity") -> DeviceType.HUMIDITY_SENSOR
            name.contains("pressure") || name.contains("bp") -> DeviceType.BLOOD_PRESSURE_MONITOR
            name.contains("scale") -> DeviceType.SMART_SCALE
            name.contains("camera") -> DeviceType.SMART_CAMERA
            name.contains("motion") -> DeviceType.MOTION_DETECTOR
            name.contains("light") -> DeviceType.LIGHT_SENSOR
            name.contains("air") || name.contains("quality") -> DeviceType.AIR_QUALITY_MONITOR
            name.contains("noise") || name.contains("sound") -> DeviceType.NOISE_LEVEL_METER
            else -> DeviceType.UNKNOWN
        }
    }

    private fun inferCapabilities(deviceType: DeviceType, bluetoothDevice: BluetoothDevice): List<DeviceCapability> {
        return when (deviceType) {
            DeviceType.HEART_RATE_MONITOR -> listOf(DeviceCapability.HEART_RATE_MONITORING)
            DeviceType.SMART_WATCH -> listOf(
                DeviceCapability.HEART_RATE_MONITORING,
                DeviceCapability.STEP_COUNTING,
                DeviceCapability.SLEEP_TRACKING,
                DeviceCapability.GPS_TRACKING,
                DeviceCapability.DISPLAY_OUTPUT,
                DeviceCapability.VIBRATION_FEEDBACK
            )
            DeviceType.FITNESS_TRACKER -> listOf(
                DeviceCapability.STEP_COUNTING,
                DeviceCapability.SLEEP_TRACKING,
                DeviceCapability.HEART_RATE_MONITORING,
                DeviceCapability.ACCELEROMETER
            )
            DeviceType.TEMPERATURE_SENSOR -> listOf(DeviceCapability.TEMPERATURE_READING)
            DeviceType.HUMIDITY_SENSOR -> listOf(DeviceCapability.HUMIDITY_READING)
            DeviceType.BLOOD_PRESSURE_MONITOR -> listOf(DeviceCapability.PRESSURE_READING)
            DeviceType.SMART_CAMERA -> listOf(
                DeviceCapability.IMAGE_CAPTURE,
                DeviceCapability.VIDEO_RECORDING,
                DeviceCapability.MOTION_DETECTION
            )
            DeviceType.MOTION_DETECTOR -> listOf(
                DeviceCapability.MOTION_DETECTION,
                DeviceCapability.ACCELEROMETER
            )
            DeviceType.AIR_QUALITY_MONITOR -> listOf(DeviceCapability.AIR_QUALITY_MONITORING)
            DeviceType.NOISE_LEVEL_METER -> listOf(DeviceCapability.NOISE_LEVEL_MONITORING)
            else -> emptyList()
        }
    }

    private fun addDiscoveredDevice(device: IoTDevice) {
        discoveredDevices[device.id] = device
        deviceDiscoveredCallback?.invoke(device)
        Log.d(TAG, "Discovered device: ${device.name} (${device.type.displayName})")
    }

    private fun addDeviceData(deviceId: String, data: DeviceData) {
        val dataList = deviceData.getOrPut(deviceId) { mutableListOf() }
        dataList.add(data)

        // Limit data size per device
        while (dataList.size > 1000) {
            dataList.removeAt(0)
        }

        dataReceivedCallback?.invoke(data)
    }

    private suspend fun connectBluetoothLE(device: IoTDevice): Boolean {
        return try {
            // Validate device address before attempting connection
            if (device.address.isNullOrBlank()) {
                Log.e(TAG, "Device address is null or empty. Cannot connect to GSR sensor.")
                errorCallback?.invoke("Device address is null or empty. Cannot connect to GSR sensor.", null)
                return false
            }

            // Validate Bluetooth address format
            if (!android.bluetooth.BluetoothAdapter.checkBluetoothAddress(device.address)) {
                Log.e(TAG, "Invalid Bluetooth address format: ${device.address}")
                errorCallback?.invoke("Invalid Bluetooth address format: ${device.address}", null)
                return false
            }

            val bluetoothDevice = bluetoothAdapter.getRemoteDevice(device.address)
            val gattCallback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            Log.d(TAG, "Connected to GATT server")
                            gatt.discoverServices()
                        }
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Log.d(TAG, "Disconnected from GATT server")
                            scope.launch {
                                disconnectFromDevice(device.id)
                            }
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "Services discovered for ${device.name}")
                        // Enable notifications for relevant characteristics
                        enableNotifications(gatt)
                    }
                }

                override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                    // Handle incoming data
                    val data = characteristic.value
                    processBluetoothData(device.id, characteristic.uuid.toString(), data)
                }
            }

            val gatt = bluetoothDevice.connectGatt(context, false, gattCallback)
            deviceConnections[device.id] = gatt

            // Wait for connection (simplified)
            delay(config.connectionTimeout)
            true

        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to Bluetooth LE device", e)
            false
        }
    }

    private suspend fun connectBluetoothClassic(device: IoTDevice): Boolean {
        // Implementation for classic Bluetooth connection
        return false // Placeholder
    }

    private suspend fun connectWiFi(device: IoTDevice): Boolean {
        // Implementation for WiFi device connection
        return false // Placeholder
    }

    private suspend fun connectHttpRest(device: IoTDevice): Boolean {
        // Implementation for HTTP REST connection
        return false // Placeholder
    }

    private suspend fun connectWebSocket(device: IoTDevice): Boolean {
        // Implementation for WebSocket connection
        return false // Placeholder
    }

    private suspend fun connectMqtt(device: IoTDevice): Boolean {
        // Implementation for MQTT connection
        return false // Placeholder
    }

    private fun enableNotifications(gatt: BluetoothGatt) {
        // Enable notifications for relevant characteristics
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    gatt.setCharacteristicNotification(characteristic, true)
                }
            }
        }
    }

    private fun processBluetoothData(deviceId: String, characteristicUuid: String, data: ByteArray) {
        try {
            // Parse data based on characteristic UUID and device type
            val device = connectedDevices[deviceId] ?: return

            when {
                characteristicUuid.contains("2A37") -> { // Heart Rate Measurement
                    val heartRate = parseHeartRateData(data)
                    val deviceData = DeviceData(
                        deviceId = deviceId,
                        timestamp = System.currentTimeMillis(),
                        dataType = "heart_rate",
                        value = heartRate,
                        unit = "bpm"
                    )
                    addDeviceData(deviceId, deviceData)
                }
                characteristicUuid.contains("2A6E") -> { // Temperature
                    val temperature = parseTemperatureData(data)
                    val deviceData = DeviceData(
                        deviceId = deviceId,
                        timestamp = System.currentTimeMillis(),
                        dataType = "temperature",
                        value = temperature,
                        unit = "°C"
                    )
                    addDeviceData(deviceId, deviceData)
                }
                // Add more characteristic parsers as needed
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing Bluetooth data", e)
        }
    }

    private fun parseHeartRateData(data: ByteArray): Int {
        // Parse heart rate data according to Bluetooth specification
        return if (data.isNotEmpty()) {
            if (data[0].toInt() and 0x01 == 0) {
                // 8-bit heart rate value
                data[1].toInt() and 0xFF
            } else {
                // 16-bit heart rate value
                ((data[2].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            }
        } else 0
    }

    private fun parseTemperatureData(data: ByteArray): Float {
        // Parse temperature data (simplified)
        return if (data.size >= 4) {
            java.nio.ByteBuffer.wrap(data).float
        } else 0f
    }

    private suspend fun sendBluetoothCommand(deviceId: String, command: String) {
        // Implementation for sending Bluetooth commands
    }

    private suspend fun sendHttpCommand(deviceId: String, command: String) {
        // Implementation for sending HTTP commands
    }

    private suspend fun sendWebSocketCommand(deviceId: String, command: String) {
        // Implementation for sending WebSocket commands
    }

    private suspend fun sendMqttCommand(deviceId: String, command: String) {
        // Implementation for sending MQTT commands
    }

    private fun getLocalNetworkRange(): String? {
        // Get local network range for WiFi device discovery
        return try {
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress
            val subnet = String.format(
                "%d.%d.%d",
                ipAddress and 0xFF,
                (ipAddress shr 8) and 0xFF,
                (ipAddress shr 16) and 0xFF
            )
            "$subnet.0/24"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting network range", e)
            null
        }
    }

    private suspend fun scanNetworkRange(networkRange: String) {
        // Implementation for scanning network range
        // This would ping devices in the network range and identify IoT devices
    }

    private suspend fun cleanupOldData() {
        val cutoffTime = System.currentTimeMillis() - config.dataRetentionPeriod

        deviceData.values.forEach { dataList ->
            dataList.removeAll { it.timestamp < cutoffTime }
        }

        Log.d(TAG, "Cleaned up old device data")
    }

    // Callback setters
    fun setDeviceDiscoveredCallback(callback: (IoTDevice) -> Unit) {
        deviceDiscoveredCallback = callback
    }

    fun setDeviceConnectedCallback(callback: (IoTDevice) -> Unit) {
        deviceConnectedCallback = callback
    }

    fun setDeviceDisconnectedCallback(callback: (IoTDevice) -> Unit) {
        deviceDisconnectedCallback = callback
    }

    fun setDataReceivedCallback(callback: (DeviceData) -> Unit) {
        dataReceivedCallback = callback
    }

    fun setErrorCallback(callback: (String, Exception?) -> Unit) {
        errorCallback = callback
    }

    /**
     * Shutdown IoT device manager
     */
    fun shutdown() {
        scope.cancel()

        // Disconnect all devices
        connectedDevices.keys.forEach { deviceId ->
            disconnectFromDevice(deviceId)
        }

        discoveredDevices.clear()
        connectedDevices.clear()
        deviceConnections.clear()
        deviceData.clear()

        Log.d(TAG, "IoT device manager shut down")
    }
}
