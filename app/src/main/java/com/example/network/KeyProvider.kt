package com.example.network

import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Abstraction for network key management in LocalConnect.
 *
 * Isolates cryptographic key derivation and provisioning from payload encryption/decryption.
 * Prepares the architecture for future key management strategies:
 * - Android Keystore hardware-backed keys
 * - ECDH session key exchange
 * - Key rotation
 */
interface KeyProvider {
    /** Returns the active SecretKey for network payload encryption/decryption. */
    fun getNetworkKey(): SecretKey

    /** Updates or overrides the passphrase used to derive the network key. */
    fun setNetworkPassphrase(passphrase: String)

    /** Updates the raw 256-bit AES network key directly. */
    fun updateNetworkKey(newKey: ByteArray)
}

/**
 * Legacy static key provider preserving current offline PBKDF2 key derivation
 * and default pairing passphrase.
 *
 * Phase 1 backward-compatibility implementation:
 * - Retains DEFAULT_PASSPHRASE ("LocalConnect-Offline-Pairing-Key-v1")
 * - Derives AES-256 key via PBKDF2WithHmacSHA256 (12,000 iterations, static salt)
 * - Supports runtime passphrase override and raw key updates
 * - Thread-safe cached key derivation
 */
class LegacyStaticKeyProvider : KeyProvider {

    companion object {
        private const val KEY_ALGORITHM = "AES"
        private const val KEY_BITS = 256
        private const val PBKDF2_ITERATIONS = 12000
        private const val KEY_SALT = "LocalConnect::P2P::NetworkSalt::v1"

        /** Shared pairing passphrase used to derive the AES-256 network session key. */
        const val DEFAULT_PASSPHRASE = "LocalConnect-Offline-Pairing-Key-v1"
    }

    private val defaultKeyBytes = ByteArray(32) { 0 }

    @Volatile
    private var _networkKey: ByteArray = defaultKeyBytes

    @Volatile
    private var networkPassphrase: String = DEFAULT_PASSPHRASE

    @Volatile
    private var cachedKey: SecretKey? = null

    override fun updateNetworkKey(newKey: ByteArray) {
        require(newKey.size == 32) { "Key must be 256 bits (32 bytes)" }
        _networkKey = newKey.copyOf()
    }

    @Synchronized
    override fun setNetworkPassphrase(passphrase: String) {
        if (passphrase.isBlank() || passphrase == networkPassphrase) return
        networkPassphrase = passphrase
        cachedKey = null
    }

    override fun getNetworkKey(): SecretKey {
        if (!_networkKey.contentEquals(defaultKeyBytes)) {
            return SecretKeySpec(_networkKey, KEY_ALGORITHM)
        }
        cachedKey?.let { return it }
        return synchronized(this) {
            cachedKey ?: deriveKey(networkPassphrase).also { cachedKey = it }
        }
    }

    private fun deriveKey(passphrase: String): SecretKey {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(
            passphrase.toCharArray(),
            KEY_SALT.toByteArray(Charsets.UTF_8),
            PBKDF2_ITERATIONS,
            KEY_BITS
        )
        return SecretKeySpec(factory.generateSecret(spec).encoded, KEY_ALGORITHM)
    }
}
