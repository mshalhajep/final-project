package com.example.network

import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-end payload protection engine for every byte leaving or entering the device.
 *
 * Cipher: AES-256-GCM (Galois/Counter Mode) with a 128-bit authentication tag and a
 * fresh 12-byte random nonce per packet. Any payload failing AEAD authentication is
 * discarded by the caller — tampered or foreign packets never reach the decoders.
 *
 * The network key is derived with PBKDF2-HmacSHA256 from a shared pairing passphrase.
 * All devices running LocalConnect on the same offline network share the default
 * passphrase; a future pairing flow can override it via [setNetworkPassphrase].
 */
object LocalCryptoEngine {

    private const val TAG = "LocalCryptoEngine"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    const val NONCE_SIZE = 12

    private const val PBKDF2_ITERATIONS = 12000
    private const val KEY_SALT = "LocalConnect::P2P::NetworkSalt::v1"

    /** Shared pairing passphrase used to derive the AES-256 network session key. */
    private const val DEFAULT_PASSPHRASE = "LocalConnect-Offline-Pairing-Key-v1"

    // WARNING: This is a default key for development. In production, generate a unique key per network.
    private val defaultKeyBytes = ByteArray(32) { 0 }
    private var _networkKey: ByteArray = defaultKeyBytes

    fun updateNetworkKey(newKey: ByteArray) {
        require(newKey.size == 32) { "Key must be 256 bits (32 bytes)" }
        _networkKey = newKey.copyOf()
    }

    @Volatile
    private var networkPassphrase: String = DEFAULT_PASSPHRASE

    @Volatile
    private var cachedKey: SecretKey? = null

    private val secureRandom = SecureRandom()

    /** Overrides the pairing passphrase and invalidates the cached derived key. */
    @Synchronized
    fun setNetworkPassphrase(passphrase: String) {
        if (passphrase.isBlank() || passphrase == networkPassphrase) return
        networkPassphrase = passphrase
        cachedKey = null
        Log.i(TAG, "Network pairing passphrase updated — session key re-derived")
    }

    /** Lazily derived AES-256 network key (PBKDF2-HmacSHA256, salted, 12k iterations). */
    val networkKey: SecretKey
        get() {
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

    /**
     * Encrypts arbitrary bytes with AES-256-GCM.
     * Output layout: [12-byte random nonce][ciphertext + 16-byte auth tag].
     */
    fun encrypt(plainBytes: ByteArray, key: SecretKey = networkKey): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(nonce)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
        val cipherText = cipher.doFinal(plainBytes)
        val packed = ByteArray(NONCE_SIZE + cipherText.size)
        System.arraycopy(nonce, 0, packed, 0, NONCE_SIZE)
        System.arraycopy(cipherText, 0, packed, NONCE_SIZE, cipherText.size)
        return packed
    }

    /**
     * Decrypts an AES-256-GCM packed payload.
     * Returns null when the nonce/tag authentication fails (foreign or tampered packet).
     */
    fun decrypt(packed: ByteArray, key: SecretKey = networkKey): ByteArray? {
        return try {
            if (packed.size <= NONCE_SIZE) return null
            val nonce = packed.copyOfRange(0, NONCE_SIZE)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
            cipher.doFinal(packed, NONCE_SIZE, packed.size - NONCE_SIZE)
        } catch (e: Exception) {
            Log.w(TAG, "AEAD authentication failed — encrypted packet discarded")
            null
        }
    }

    /** Encrypts a UTF-8 JSON payload for the P2P signaling channel. */
    fun encryptJson(json: String, key: SecretKey = networkKey): ByteArray =
        encrypt(json.toByteArray(Charsets.UTF_8), key)

    /** Decrypts a signaling payload back to UTF-8 JSON, or null on auth failure. */
    fun decryptJson(packed: ByteArray, key: SecretKey = networkKey): String? =
        decrypt(packed, key)?.let { String(it, Charsets.UTF_8) }

    /**
     * Deterministic per-chunk nonce for resumable encrypted file streams:
     * SHA-256(fileId + chunkIndex) truncated to 12 bytes, so byte-offset resume works
     * without transmitting nonces and without breaking alignment.
     */
    fun deriveChunkNonce(fileId: String, chunkIndex: Long, sessionId: String = ""): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$fileId:$sessionId:$chunkIndex".toByteArray(Charsets.UTF_8))
        return digest.copyOf(NONCE_SIZE)
    }

    /** Encrypts one file-transfer chunk with its deterministic chunk nonce. */
    fun encryptChunk(
        plainBytes: ByteArray,
        fileId: String,
        chunkIndex: Long,
        sessionId: String = "",
        key: SecretKey = networkKey
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, deriveChunkNonce(fileId, chunkIndex, sessionId)))
        return cipher.doFinal(plainBytes)
    }

    /** Decrypts one file-transfer chunk, or null when AEAD authentication fails. */
    fun decryptChunk(
        packed: ByteArray,
        fileId: String,
        chunkIndex: Long,
        sessionId: String = "",
        key: SecretKey = networkKey
    ): ByteArray? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, deriveChunkNonce(fileId, chunkIndex, sessionId)))
            cipher.doFinal(packed)
        } catch (e: Exception) {
            Log.w(TAG, "Chunk AEAD authentication failed — chunk discarded")
            null
        }
    }
}
