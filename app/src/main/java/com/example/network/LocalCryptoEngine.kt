package com.example.network

import android.util.Log
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * End-to-end payload protection engine for every byte leaving or entering the device.
 *
 * Cipher: AES-256-GCM (Galois/Counter Mode) with a 128-bit authentication tag and a
 * fresh 12-byte random nonce per packet. Any payload failing AEAD authentication is
 * discarded by the caller — tampered or foreign packets never reach the decoders.
 *
 * Key management is delegated to a [KeyProvider] abstraction (defaults to [LegacyStaticKeyProvider]),
 * decoupling cipher operations from key derivation and paving the way for hardware-backed keys and ECDH.
 */
object LocalCryptoEngine {

    private const val TAG = "LocalCryptoEngine"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    const val NONCE_SIZE = 12

    /** Key provider abstraction delegating key management and derivation. */
    @Volatile
    var keyProvider: KeyProvider = LegacyStaticKeyProvider()

    /** Updates the raw network key via the active [KeyProvider]. */
    fun updateNetworkKey(newKey: ByteArray) {
        keyProvider.updateNetworkKey(newKey)
    }

    /** Overrides the pairing passphrase via the active [KeyProvider]. */
    fun setNetworkPassphrase(passphrase: String) {
        keyProvider.setNetworkPassphrase(passphrase)
        Log.i(TAG, "Network pairing passphrase updated — session key re-derived")
    }

    /** Active AES-256 network key retrieved from the current [KeyProvider]. */
    val networkKey: SecretKey
        get() = keyProvider.getNetworkKey()

    private val secureRandom = SecureRandom()

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
