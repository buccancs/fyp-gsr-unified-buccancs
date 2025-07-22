package com.buccancs.gsr.common.network;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * SecurityLayer provides authentication and encryption capabilities for network communication
 * between the PC controller and Android devices.
 * 
 * Features:
 * - Device authentication using pre-shared keys
 * - AES-256 encryption for message payload
 * - Message integrity verification using HMAC
 * - Session key management
 * - Replay attack prevention using timestamps and nonces
 */
public class SecurityLayer {

    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String ENCRYPTION_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int KEY_LENGTH = 256;
    private static final int IV_LENGTH = 16;
    private static final int NONCE_LENGTH = 8;
    private static final long MAX_TIMESTAMP_SKEW = 30000; // 30 seconds

    // Pre-shared keys for device authentication (device_id -> key)
    private final Map<String, SecretKey> deviceKeys;

    // Session keys for active connections (device_id -> session_key)
    private final Map<String, SecretKey> sessionKeys;

    // Used nonces to prevent replay attacks (device_id -> set of used nonces)
    private final Map<String, Set<String>> usedNonces;

    private final SecureRandom secureRandom;

    /**
     * Initialize the security layer.
     */
    public SecurityLayer() {
        this.deviceKeys = new ConcurrentHashMap<>();
        this.sessionKeys = new ConcurrentHashMap<>();
        this.usedNonces = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();

        // Initialize with default device keys (in production, these should be loaded securely)
        initializeDefaultKeys();
    }

    /**
     * Initialize default device authentication keys.
     * In production, these should be loaded from a secure key store.
     */
    private void initializeDefaultKeys() {
        try {
            // Generate default keys for known devices
            String[] defaultDevices = {"android_device_1", "android_device_2", "android_device_3"};

            for (String deviceId : defaultDevices) {
                SecretKey key = generateSecretKey();
                deviceKeys.put(deviceId, key);
            }

            // Add a master key for new device registration
            SecretKey masterKey = generateSecretKey();
            deviceKeys.put("MASTER_KEY", masterKey);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize security layer", e);
        }
    }

    /**
     * Generate a new AES secret key.
     */
    private SecretKey generateSecretKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ENCRYPTION_ALGORITHM);
        keyGenerator.init(KEY_LENGTH);
        return keyGenerator.generateKey();
    }

    /**
     * Register a new device with a pre-shared key.
     */
    public void registerDevice(String deviceId, String preSharedKey) {
        try {
            byte[] keyBytes = preSharedKey.getBytes(StandardCharsets.UTF_8);
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedKey = digest.digest(keyBytes);

            // Use first 32 bytes for AES-256
            byte[] aesKey = Arrays.copyOf(hashedKey, 32);
            SecretKey secretKey = new SecretKeySpec(aesKey, ENCRYPTION_ALGORITHM);

            deviceKeys.put(deviceId, secretKey);
            usedNonces.put(deviceId, ConcurrentHashMap.newKeySet());

        } catch (Exception e) {
            throw new RuntimeException("Failed to register device: " + deviceId, e);
        }
    }

    /**
     * Authenticate a device and establish a session key.
     */
    public AuthenticationResult authenticateDevice(String deviceId, byte[] authChallenge) {
        try {
            SecretKey deviceKey = deviceKeys.get(deviceId);
            if (deviceKey == null) {
                return new AuthenticationResult(false, "Device not registered", null);
            }

            // Decrypt the authentication challenge
            byte[] decryptedChallenge = decrypt(authChallenge, deviceKey);

            // Parse the challenge (timestamp + nonce + device_id)
            ByteBuffer buffer = ByteBuffer.wrap(decryptedChallenge);
            long timestamp = buffer.getLong();
            byte[] nonceBytes = new byte[NONCE_LENGTH];
            buffer.get(nonceBytes);
            String nonce = Base64.getEncoder().encodeToString(nonceBytes);

            byte[] deviceIdBytes = new byte[buffer.remaining()];
            buffer.get(deviceIdBytes);
            String challengeDeviceId = new String(deviceIdBytes, StandardCharsets.UTF_8);

            // Verify timestamp (prevent replay attacks)
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - timestamp) > MAX_TIMESTAMP_SKEW) {
                return new AuthenticationResult(false, "Authentication timestamp expired", null);
            }

            // Verify device ID
            if (!deviceId.equals(challengeDeviceId)) {
                return new AuthenticationResult(false, "Device ID mismatch", null);
            }

            // Check for nonce reuse (prevent replay attacks)
            Set<String> deviceNonces = usedNonces.computeIfAbsent(deviceId, k -> ConcurrentHashMap.newKeySet());
            if (deviceNonces.contains(nonce)) {
                return new AuthenticationResult(false, "Nonce already used", null);
            }
            deviceNonces.add(nonce);

            // Generate session key
            SecretKey sessionKey = generateSecretKey();
            sessionKeys.put(deviceId, sessionKey);

            // Create authentication response
            byte[] sessionKeyBytes = sessionKey.getEncoded();
            byte[] encryptedSessionKey = encrypt(sessionKeyBytes, deviceKey);

            return new AuthenticationResult(true, "Authentication successful", encryptedSessionKey);

        } catch (Exception e) {
            return new AuthenticationResult(false, "Authentication failed: " + e.getMessage(), null);
        }
    }

    /**
     * Encrypt a message for a specific device.
     */
    public SecureMessage encryptMessage(String deviceId, byte[] message) {
        try {
            SecretKey sessionKey = sessionKeys.get(deviceId);
            if (sessionKey == null) {
                throw new SecurityException("No session key for device: " + deviceId);
            }

            // Generate IV
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Encrypt message
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, sessionKey, ivSpec);
            byte[] encryptedData = cipher.doFinal(message);

            // Generate message hash for integrity
            byte[] messageHash = generateMessageHash(message, sessionKey);

            return new SecureMessage(encryptedData, iv, messageHash, System.currentTimeMillis());

        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt message for device: " + deviceId, e);
        }
    }

    /**
     * Decrypt a message from a specific device.
     */
    public byte[] decryptMessage(String deviceId, SecureMessage secureMessage) {
        try {
            SecretKey sessionKey = sessionKeys.get(deviceId);
            if (sessionKey == null) {
                throw new SecurityException("No session key for device: " + deviceId);
            }

            // Verify timestamp
            long currentTime = System.currentTimeMillis();
            if (Math.abs(currentTime - secureMessage.getTimestamp()) > MAX_TIMESTAMP_SKEW) {
                throw new SecurityException("Message timestamp expired");
            }

            // Decrypt message
            Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
            IvParameterSpec ivSpec = new IvParameterSpec(secureMessage.getIv());
            cipher.init(Cipher.DECRYPT_MODE, sessionKey, ivSpec);
            byte[] decryptedData = cipher.doFinal(secureMessage.getEncryptedData());

            // Verify message integrity
            byte[] expectedHash = generateMessageHash(decryptedData, sessionKey);
            if (!Arrays.equals(expectedHash, secureMessage.getMessageHash())) {
                throw new SecurityException("Message integrity verification failed");
            }

            return decryptedData;

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt message from device: " + deviceId, e);
        }
    }

    /**
     * Generate a message hash for integrity verification.
     */
    private byte[] generateMessageHash(byte[] message, SecretKey key) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        digest.update(message);
        digest.update(key.getEncoded());
        return digest.digest();
    }

    /**
     * Encrypt data with a specific key.
     */
    private byte[] encrypt(byte[] data, SecretKey key) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encryptedData = cipher.doFinal(data);

        // Prepend IV to encrypted data
        ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH + encryptedData.length);
        buffer.put(iv);
        buffer.put(encryptedData);
        return buffer.array();
    }

    /**
     * Decrypt data with a specific key.
     */
    private byte[] decrypt(byte[] encryptedData, SecretKey key) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(encryptedData);

        // Extract IV
        byte[] iv = new byte[IV_LENGTH];
        buffer.get(iv);

        // Extract encrypted data
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        // Decrypt
        Cipher cipher = Cipher.getInstance(ENCRYPTION_TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        return cipher.doFinal(data);
    }

    /**
     * Remove session for a device (on disconnect).
     */
    public void removeSession(String deviceId) {
        sessionKeys.remove(deviceId);
        // Keep used nonces for a while to prevent replay attacks
    }

    /**
     * Check if a device is authenticated.
     */
    public boolean isAuthenticated(String deviceId) {
        return sessionKeys.containsKey(deviceId);
    }

    /**
     * Get the list of authenticated devices.
     */
    public Set<String> getAuthenticatedDevices() {
        return new HashSet<>(sessionKeys.keySet());
    }

    /**
     * Authentication result container.
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final String message;
        private final byte[] sessionKeyData;

        public AuthenticationResult(boolean success, String message, byte[] sessionKeyData) {
            this.success = success;
            this.message = message;
            this.sessionKeyData = sessionKeyData;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public byte[] getSessionKeyData() { return sessionKeyData; }
    }

    /**
     * Secure message container.
     */
    public static class SecureMessage {
        private final byte[] encryptedData;
        private final byte[] iv;
        private final byte[] messageHash;
        private final long timestamp;

        public SecureMessage(byte[] encryptedData, byte[] iv, byte[] messageHash, long timestamp) {
            this.encryptedData = encryptedData;
            this.iv = iv;
            this.messageHash = messageHash;
            this.timestamp = timestamp;
        }

        public byte[] getEncryptedData() { return encryptedData; }
        public byte[] getIv() { return iv; }
        public byte[] getMessageHash() { return messageHash; }
        public long getTimestamp() { return timestamp; }

        /**
         * Serialize the secure message for transmission.
         */
        public byte[] serialize() {
            ByteBuffer buffer = ByteBuffer.allocate(
                4 + encryptedData.length +  // encrypted data length + data
                4 + iv.length +              // IV length + IV
                4 + messageHash.length +     // hash length + hash
                8                            // timestamp
            );

            buffer.putInt(encryptedData.length);
            buffer.put(encryptedData);
            buffer.putInt(iv.length);
            buffer.put(iv);
            buffer.putInt(messageHash.length);
            buffer.put(messageHash);
            buffer.putLong(timestamp);

            return buffer.array();
        }

        /**
         * Deserialize a secure message from bytes.
         */
        public static SecureMessage deserialize(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);

            int encryptedDataLength = buffer.getInt();
            byte[] encryptedData = new byte[encryptedDataLength];
            buffer.get(encryptedData);

            int ivLength = buffer.getInt();
            byte[] iv = new byte[ivLength];
            buffer.get(iv);

            int hashLength = buffer.getInt();
            byte[] messageHash = new byte[hashLength];
            buffer.get(messageHash);

            long timestamp = buffer.getLong();

            return new SecureMessage(encryptedData, iv, messageHash, timestamp);
        }
    }
}
