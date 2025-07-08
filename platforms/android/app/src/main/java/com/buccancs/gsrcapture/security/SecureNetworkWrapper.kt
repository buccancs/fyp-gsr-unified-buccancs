package com.buccancs.gsrcapture.security

import android.util.Log
import com.buccancs.gsrcapture.security.interfaces.AuthenticationProvider
import com.buccancs.gsrcapture.security.interfaces.EncryptionProvider
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Secure network wrapper that adds authentication and encryption
 * to network communications between devices.
 */
class SecureNetworkWrapper(
    private val authProvider: AuthenticationProvider,
    private val encryptionProvider: EncryptionProvider,
) {
    companion object {
        private const val TAG = "SecureNetworkWrapper"

        // Protocol message types
        private const val MSG_TYPE_AUTH_REQUEST = "AUTH_REQUEST"
        private const val MSG_TYPE_AUTH_RESPONSE = "AUTH_RESPONSE"
        private const val MSG_TYPE_ENCRYPTED_DATA = "ENCRYPTED_DATA"
        private const val MSG_TYPE_KEY_EXCHANGE = "KEY_EXCHANGE"
        private const val MSG_TYPE_HEARTBEAT = "HEARTBEAT"
        private const val MSG_TYPE_ERROR = "ERROR"
    }

    /**
     * Data class representing a secure session
     */
    data class SecureSession(
        val deviceId: String,
        val sessionToken: String,
        val symmetricKey: ByteArray,
        val isAuthenticated: Boolean,
        val createdAt: Long = System.currentTimeMillis(),
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SecureSession

            if (deviceId != other.deviceId) return false
            if (sessionToken != other.sessionToken) return false
            if (!symmetricKey.contentEquals(other.symmetricKey)) return false
            if (isAuthenticated != other.isAuthenticated) return false
            if (createdAt != other.createdAt) return false

            return true
        }

        override fun hashCode(): Int {
            var result = deviceId.hashCode()
            result = 31 * result + sessionToken.hashCode()
            result = 31 * result + symmetricKey.contentHashCode()
            result = 31 * result + isAuthenticated.hashCode()
            result = 31 * result + createdAt.hashCode()
            return result
        }
    }

    /**
     * Interface for secure communication callbacks
     */
    interface SecureNetworkCallback {
        fun onSecureMessageReceived(
            deviceId: String,
            message: String,
        )

        fun onAuthenticationSuccess(deviceId: String)

        fun onAuthenticationFailed(
            deviceId: String,
            reason: String,
        )

        fun onConnectionSecured(deviceId: String)

        fun onSecurityError(
            deviceId: String,
            error: String,
        )
    }

    private val activeSessions = mutableMapOf<String, SecureSession>()
    private var callback: SecureNetworkCallback? = null
    private val keyPair = encryptionProvider.generateKeyPair()

    /**
     * Set the callback for secure network events
     */
    fun setCallback(callback: SecureNetworkCallback) {
        this.callback = callback
    }

    /**
     * Initiate secure handshake with a remote device
     * @param socket Connected socket
     * @param deviceId Local device ID
     * @param deviceToken Authentication token
     * @return true if handshake was successful
     */
    fun initiateSecureHandshake(
        socket: Socket,
        deviceId: String,
        deviceToken: String,
    ): Boolean {
        return try {
            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Step 1: Send authentication request
            val authRequest = createAuthRequest(deviceId, deviceToken)
            writer.println(authRequest)

            // Step 2: Wait for authentication response
            val authResponse = reader.readLine()
            if (!handleAuthResponse(authResponse, deviceId)) {
                return false
            }

            // Step 3: Perform key exchange
            val keyExchange = createKeyExchange(deviceId)
            writer.println(keyExchange)

            // Step 4: Wait for key exchange response
            val keyResponse = reader.readLine()
            handleKeyExchangeResponse(keyResponse, deviceId)
        } catch (e: Exception) {
            Log.e(TAG, "Error during secure handshake", e)
            callback?.onSecurityError(deviceId, "Handshake failed: ${e.message}")
            false
        }
    }

    /**
     * Handle incoming secure handshake from a remote device
     * @param socket Connected socket
     * @return Device ID if handshake was successful, null otherwise
     */
    fun handleIncomingSecureHandshake(socket: Socket): String? =
        try {
            val writer = PrintWriter(socket.getOutputStream(), true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            // Step 1: Wait for authentication request
            val authRequest = reader.readLine()
            val deviceId = handleAuthRequest(authRequest)

            if (deviceId != null) {
                // Step 2: Send authentication response
                val authResponse = createAuthResponse(deviceId, true)
                writer.println(authResponse)

                // Step 3: Wait for key exchange
                val keyExchange = reader.readLine()
                handleKeyExchange(keyExchange, deviceId)

                // Step 4: Send key exchange response
                val keyResponse = createKeyExchangeResponse(deviceId)
                writer.println(keyResponse)

                deviceId
            } else {
                // Send authentication failure response
                val authResponse = createAuthResponse("", false)
                writer.println(authResponse)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling incoming secure handshake", e)
            null
        }

    /**
     * Encrypt and send a message securely
     * @param socket Connected socket
     * @param deviceId Target device ID
     * @param message Message to send
     * @return true if message was sent successfully
     */
    fun sendSecureMessage(
        socket: Socket,
        deviceId: String,
        message: String,
    ): Boolean {
        val session = activeSessions[deviceId]
        if (session == null || !session.isAuthenticated) {
            Log.w(TAG, "No secure session for device $deviceId")
            return false
        }

        return try {
            val writer = PrintWriter(socket.getOutputStream(), true)

            // Encrypt the message
            val messageBytes = message.toByteArray(StandardCharsets.UTF_8)
            val encryptedData = encryptionProvider.encryptSymmetric(messageBytes, session.symmetricKey)

            // Create secure message
            val secureMessage =
                JSONObject().apply {
                    put("type", MSG_TYPE_ENCRYPTED_DATA)
                    put("deviceId", deviceId)
                    put("sessionToken", session.sessionToken)
                    put("data", Base64.getEncoder().encodeToString(encryptedData.data))
                    put("iv", encryptedData.iv?.let { Base64.getEncoder().encodeToString(it) })
                    put("timestamp", System.currentTimeMillis())
                }

            writer.println(secureMessage.toString())
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending secure message", e)
            callback?.onSecurityError(deviceId, "Failed to send secure message: ${e.message}")
            false
        }
    }

    /**
     * Receive and decrypt a secure message
     * @param rawMessage Raw message received from socket
     * @return Decrypted message or null if decryption failed
     */
    fun receiveSecureMessage(rawMessage: String): String? {
        return try {
            val messageJson = JSONObject(rawMessage)
            val messageType = messageJson.getString("type")

            when (messageType) {
                MSG_TYPE_ENCRYPTED_DATA -> {
                    val deviceId = messageJson.getString("deviceId")
                    val sessionToken = messageJson.getString("sessionToken")

                    val session = activeSessions[deviceId]
                    if (session == null || session.sessionToken != sessionToken) {
                        Log.w(TAG, "Invalid session for device $deviceId")
                        return null
                    }

                    // Decrypt the message
                    val encryptedData = Base64.getDecoder().decode(messageJson.getString("data"))
                    val iv = messageJson.optString("iv")?.let { Base64.getDecoder().decode(it) }

                    val encryptedDataObj = EncryptionProvider.EncryptedData(encryptedData, iv)
                    val decryptedBytes = encryptionProvider.decryptSymmetric(encryptedDataObj, session.symmetricKey)

                    val decryptedMessage = String(decryptedBytes, StandardCharsets.UTF_8)
                    callback?.onSecureMessageReceived(deviceId, decryptedMessage)
                    decryptedMessage
                }
                MSG_TYPE_HEARTBEAT -> {
                    // Handle heartbeat messages
                    val deviceId = messageJson.getString("deviceId")
                    Log.d(TAG, "Received heartbeat from $deviceId")
                    "HEARTBEAT_ACK"
                }
                else -> {
                    Log.w(TAG, "Unknown message type: $messageType")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error receiving secure message", e)
            null
        }
    }

    /**
     * Check if a device has an active secure session
     */
    fun hasSecureSession(deviceId: String): Boolean {
        val session = activeSessions[deviceId]
        return session != null && session.isAuthenticated
    }

    /**
     * Terminate a secure session
     */
    fun terminateSession(deviceId: String) {
        activeSessions.remove(deviceId)
        Log.d(TAG, "Terminated secure session for device $deviceId")
    }

    // Private helper methods

    private fun createAuthRequest(
        deviceId: String,
        token: String,
    ): String =
        JSONObject()
            .apply {
                put("type", MSG_TYPE_AUTH_REQUEST)
                put("deviceId", deviceId)
                put("token", token)
                put("timestamp", System.currentTimeMillis())
            }.toString()

    private fun handleAuthRequest(authRequest: String): String? =
        try {
            val requestJson = JSONObject(authRequest)
            val deviceId = requestJson.getString("deviceId")
            val token = requestJson.getString("token")

            val credentials = AuthenticationProvider.Credentials(deviceId, token)
            val authResult = authProvider.authenticate(credentials)

            if (authResult.isAuthenticated) {
                Log.d(TAG, "Authentication successful for device $deviceId")
                callback?.onAuthenticationSuccess(deviceId)
                deviceId
            } else {
                Log.w(TAG, "Authentication failed for device $deviceId: ${authResult.errorMessage}")
                callback?.onAuthenticationFailed(deviceId, authResult.errorMessage ?: "Unknown error")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling auth request", e)
            null
        }

    private fun createAuthResponse(
        deviceId: String,
        success: Boolean,
    ): String =
        JSONObject()
            .apply {
                put("type", MSG_TYPE_AUTH_RESPONSE)
                put("success", success)
                if (success) {
                    put("sessionToken", authProvider.generateSessionToken(deviceId))
                }
                put("timestamp", System.currentTimeMillis())
            }.toString()

    private fun handleAuthResponse(
        authResponse: String,
        deviceId: String,
    ): Boolean =
        try {
            val responseJson = JSONObject(authResponse)
            val success = responseJson.getBoolean("success")

            if (success) {
                val sessionToken = responseJson.getString("sessionToken")
                Log.d(TAG, "Authentication response successful for device $deviceId")
                // Store session token temporarily until key exchange is complete
                true
            } else {
                Log.w(TAG, "Authentication response failed for device $deviceId")
                callback?.onAuthenticationFailed(deviceId, "Server rejected authentication")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling auth response", e)
            false
        }

    private fun createKeyExchange(deviceId: String): String =
        JSONObject()
            .apply {
                put("type", MSG_TYPE_KEY_EXCHANGE)
                put("deviceId", deviceId)
                put("publicKey", Base64.getEncoder().encodeToString(keyPair.publicKey.encoded))
                put("timestamp", System.currentTimeMillis())
            }.toString()

    private fun handleKeyExchange(
        keyExchange: String,
        deviceId: String,
    ) {
        try {
            val exchangeJson = JSONObject(keyExchange)
            val publicKeyBytes = Base64.getDecoder().decode(exchangeJson.getString("publicKey"))

            // Generate symmetric key and encrypt it with the received public key
            val symmetricKey = encryptionProvider.generateSymmetricKey()
            val sessionToken = authProvider.generateSessionToken(deviceId)

            // Store the secure session
            activeSessions[deviceId] =
                SecureSession(
                    deviceId = deviceId,
                    sessionToken = sessionToken,
                    symmetricKey = symmetricKey,
                    isAuthenticated = true,
                )

            callback?.onConnectionSecured(deviceId)
            Log.d(TAG, "Key exchange completed for device $deviceId")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling key exchange", e)
            callback?.onSecurityError(deviceId, "Key exchange failed: ${e.message}")
        }
    }

    private fun createKeyExchangeResponse(deviceId: String): String =
        JSONObject()
            .apply {
                put("type", "KEY_EXCHANGE_RESPONSE")
                put("deviceId", deviceId)
                put("status", "success")
                put("timestamp", System.currentTimeMillis())
            }.toString()

    private fun handleKeyExchangeResponse(
        keyResponse: String,
        deviceId: String,
    ): Boolean =
        try {
            val responseJson = JSONObject(keyResponse)
            val status = responseJson.getString("status")

            if (status == "success") {
                Log.d(TAG, "Key exchange response successful for device $deviceId")
                callback?.onConnectionSecured(deviceId)
                true
            } else {
                Log.w(TAG, "Key exchange response failed for device $deviceId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling key exchange response", e)
            false
        }
}
