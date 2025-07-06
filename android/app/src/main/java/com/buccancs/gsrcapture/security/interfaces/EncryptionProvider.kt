package com.buccancs.gsrcapture.security.interfaces

import java.security.PublicKey
import java.security.PrivateKey

/**
 * Interface for encryption providers
 * Supports different encryption methods for secure communication
 */
interface EncryptionProvider {
    
    /**
     * Data class representing encryption keys
     */
    data class KeyPair(
        val publicKey: PublicKey,
        val privateKey: PrivateKey
    )
    
    /**
     * Data class representing encrypted data
     */
    data class EncryptedData(
        val data: ByteArray,
        val iv: ByteArray? = null, // Initialization vector for symmetric encryption
        val metadata: Map<String, String> = emptyMap()
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as EncryptedData
            
            if (!data.contentEquals(other.data)) return false
            if (iv != null) {
                if (other.iv == null) return false
                if (!iv.contentEquals(other.iv)) return false
            } else if (other.iv != null) return false
            if (metadata != other.metadata) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + (iv?.contentHashCode() ?: 0)
            result = 31 * result + metadata.hashCode()
            return result
        }
    }
    
    /**
     * Generate a new key pair for asymmetric encryption
     * @return New key pair
     */
    fun generateKeyPair(): KeyPair
    
    /**
     * Encrypt data using symmetric encryption
     * @param data Data to encrypt
     * @param key Symmetric key
     * @return Encrypted data with IV
     */
    fun encryptSymmetric(data: ByteArray, key: ByteArray): EncryptedData
    
    /**
     * Decrypt data using symmetric encryption
     * @param encryptedData Encrypted data with IV
     * @param key Symmetric key
     * @return Decrypted data
     */
    fun decryptSymmetric(encryptedData: EncryptedData, key: ByteArray): ByteArray
    
    /**
     * Encrypt data using asymmetric encryption (public key)
     * @param data Data to encrypt
     * @param publicKey Public key for encryption
     * @return Encrypted data
     */
    fun encryptAsymmetric(data: ByteArray, publicKey: PublicKey): EncryptedData
    
    /**
     * Decrypt data using asymmetric encryption (private key)
     * @param encryptedData Encrypted data
     * @param privateKey Private key for decryption
     * @return Decrypted data
     */
    fun decryptAsymmetric(encryptedData: EncryptedData, privateKey: PrivateKey): ByteArray
    
    /**
     * Generate a random symmetric key
     * @param keySize Key size in bits (e.g., 128, 256)
     * @return Random symmetric key
     */
    fun generateSymmetricKey(keySize: Int = 256): ByteArray
    
    /**
     * Derive a key from a password using PBKDF2
     * @param password Password string
     * @param salt Salt bytes
     * @param iterations Number of iterations
     * @param keyLength Desired key length in bits
     * @return Derived key
     */
    fun deriveKeyFromPassword(
        password: String,
        salt: ByteArray,
        iterations: Int = 10000,
        keyLength: Int = 256
    ): ByteArray
    
    /**
     * Generate a random salt for key derivation
     * @param length Salt length in bytes
     * @return Random salt
     */
    fun generateSalt(length: Int = 16): ByteArray
    
    /**
     * Create a hash of data for integrity verification
     * @param data Data to hash
     * @return Hash bytes
     */
    fun createHash(data: ByteArray): ByteArray
    
    /**
     * Verify data integrity using hash
     * @param data Original data
     * @param hash Expected hash
     * @return true if hash matches
     */
    fun verifyHash(data: ByteArray, hash: ByteArray): Boolean
    
    /**
     * Get the encryption algorithm name
     * @return String identifying the encryption algorithm
     */
    fun getAlgorithmName(): String
    
    /**
     * Get the maximum data size that can be encrypted with asymmetric encryption
     * @return Maximum data size in bytes
     */
    fun getMaxAsymmetricDataSize(): Int
}