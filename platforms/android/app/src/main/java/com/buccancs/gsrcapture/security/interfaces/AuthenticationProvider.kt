package com.buccancs.gsrcapture.security.interfaces

/**
 * Interface for authentication providers
 * Supports different authentication methods for device connections
 */
interface AuthenticationProvider {
    /**
     * Data class representing authentication credentials
     */
    data class Credentials(
        val deviceId: String,
        val token: String,
        val metadata: Map<String, String> = emptyMap(),
    )

    /**
     * Data class representing authentication result
     */
    data class AuthenticationResult(
        val isAuthenticated: Boolean,
        val sessionToken: String? = null,
        val expirationTime: Long? = null,
        val errorMessage: String? = null,
    )

    /**
     * Authenticate a device using provided credentials
     * @param credentials Device credentials
     * @return Authentication result
     */
    fun authenticate(credentials: Credentials): AuthenticationResult

    /**
     * Validate an existing session token
     * @param sessionToken Token to validate
     * @param deviceId Device ID associated with the token
     * @return true if token is valid and not expired
     */
    fun validateSession(
        sessionToken: String,
        deviceId: String,
    ): Boolean

    /**
     * Generate a new session token for authenticated device
     * @param deviceId Device ID
     * @return New session token
     */
    fun generateSessionToken(deviceId: String): String

    /**
     * Revoke a session token
     * @param sessionToken Token to revoke
     * @param deviceId Device ID
     */
    fun revokeSession(
        sessionToken: String,
        deviceId: String,
    )

    /**
     * Check if a device is authorized to connect
     * @param deviceId Device ID to check
     * @return true if device is authorized
     */
    fun isDeviceAuthorized(deviceId: String): Boolean

    /**
     * Add a device to the authorized list
     * @param deviceId Device ID to authorize
     * @param deviceName Human-readable device name
     * @return true if device was successfully authorized
     */
    fun authorizeDevice(
        deviceId: String,
        deviceName: String,
    ): Boolean

    /**
     * Remove a device from the authorized list
     * @param deviceId Device ID to deauthorize
     * @return true if device was successfully deauthorized
     */
    fun deauthorizeDevice(deviceId: String): Boolean

    /**
     * Get list of authorized devices
     * @return Map of device ID to device name
     */
    fun getAuthorizedDevices(): Map<String, String>

    /**
     * Get authentication method name
     * @return String identifying the authentication method
     */
    fun getAuthenticationMethod(): String
}
