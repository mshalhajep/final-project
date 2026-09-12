package com.example.network

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.crypto.spec.SecretKeySpec

/**
 * Unit tests verifying the current cryptographic behavior of [LocalCryptoEngine].
 *
 * Scope & Constraints:
 * - Tests execute against the existing public API of [LocalCryptoEngine].
 * - No production code or visibility modifiers are altered.
 * - These tests verify specific algorithmic behavior (AES-256-GCM, chunk nonces, JSON helpers,
 *   tampering rejection, and key separation) and do NOT claim complete cryptographic security,
 *   forward secrecy, or dynamic key exchange.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LocalCryptoEngineTest {

    // Helper: Creates an alternate valid 256-bit AES key for testing wrong-key rejection
    private val alternateKey = SecretKeySpec(ByteArray(32) { 0x5A.toByte() }, "AES")

    /**
     * 1. EncryptThenDecryptRestoresPlaintext
     * Encrypts arbitrary byte payload with the default networkKey and asserts that
     * decrypting the output faithfully restores the exact original plaintext bytes.
     */
    @Test
    fun encryptThenDecryptRestoresPlaintext() {
        val originalText = "Hello LocalConnect P2P Network!"
        val plainBytes = originalText.toByteArray(Charsets.UTF_8)

        val encrypted = LocalCryptoEngine.encrypt(plainBytes)
        assertNotNull("Encrypted payload must not be null", encrypted)
        assertTrue("Encrypted payload must be longer than plain payload", encrypted.size > plainBytes.size)

        val decrypted = LocalCryptoEngine.decrypt(encrypted)
        assertNotNull("Decryption must succeed for valid unmodified payload", decrypted)
        assertArrayEquals("Decrypted bytes must match original plaintext bytes", plainBytes, decrypted)
        assertEquals("Decrypted string must match original text", originalText, String(decrypted!!, Charsets.UTF_8))
    }

    /**
     * 2. EncryptingSamePlaintextProducesDifferentCiphertext
     * Verifies that encrypting the identical plaintext twice produces distinct ciphertexts.
     * This occurs because [LocalCryptoEngine.encrypt] prepends a fresh 12-byte random nonce
     * via [java.security.SecureRandom] for each encryption invocation.
     */
    @Test
    fun encryptingSamePlaintextProducesDifferentCiphertext() {
        val plainBytes = "Static repetitive payload for IV randomness check".toByteArray(Charsets.UTF_8)

        val encrypted1 = LocalCryptoEngine.encrypt(plainBytes)
        val encrypted2 = LocalCryptoEngine.encrypt(plainBytes)

        assertFalse(
            "Two consecutive encryptions of the same plaintext must produce different ciphertexts (random nonce)",
            encrypted1.contentEquals(encrypted2)
        )

        // Both must still decrypt to the same original plaintext
        val decrypted1 = LocalCryptoEngine.decrypt(encrypted1)
        val decrypted2 = LocalCryptoEngine.decrypt(encrypted2)
        assertArrayEquals(plainBytes, decrypted1)
        assertArrayEquals(plainBytes, decrypted2)
    }

    /**
     * 3. TamperedCiphertextIsRejected
     * Modifies bytes in the ciphertext / auth tag portion and asserts that [LocalCryptoEngine.decrypt]
     * catches the AEAD authentication failure and returns null (discarding the tampered packet).
     */
    @Test
    fun tamperedCiphertextIsRejected() {
        val plainBytes = "Confidential financial or message transaction".toByteArray(Charsets.UTF_8)
        val packed = LocalCryptoEngine.encrypt(plainBytes)

        // Verify valid decryption first
        assertNotNull(LocalCryptoEngine.decrypt(packed))

        // Tamper with a byte in the ciphertext body (beyond the 12-byte nonce)
        val tamperedPayload = packed.copyOf()
        val tamperIndex = LocalCryptoEngine.NONCE_SIZE + 1
        tamperedPayload[tamperIndex] = (tamperedPayload[tamperIndex].toInt() xor 0x01).toByte()

        val resultTamperedPayload = LocalCryptoEngine.decrypt(tamperedPayload)
        assertNull("Tampered payload body must fail AEAD verification and return null", resultTamperedPayload)

        // Tamper with the last byte (part of the 16-byte GCM authentication tag)
        val tamperedTag = packed.copyOf()
        val lastIndex = tamperedTag.size - 1
        tamperedTag[lastIndex] = (tamperedTag[lastIndex].toInt() xor 0xFF).toByte()

        val resultTamperedTag = LocalCryptoEngine.decrypt(tamperedTag)
        assertNull("Tampered authentication tag must fail AEAD verification and return null", resultTamperedTag)
    }

    /**
     * 4. WrongKeyIsRejected
     * Encrypts a payload with one key and attempts to decrypt it with a different 256-bit AES key.
     * Asserts that decryption fails and returns null.
     */
    @Test
    fun wrongKeyIsRejected() {
        val plainBytes = "Secret message protected by session key".toByteArray(Charsets.UTF_8)

        // Encrypt with default network key
        val packed = LocalCryptoEngine.encrypt(plainBytes)

        // Attempt decryption with alternate key
        val decryptedWithWrongKey = LocalCryptoEngine.decrypt(packed, key = alternateKey)
        assertNull("Decryption with incorrect AES key must fail AEAD auth and return null", decryptedWithWrongKey)
    }

    /**
     * 5. ArabicUtf8RoundTrip
     * Tests full round-trip encryption and decryption with Arabic multi-byte UTF-8 characters.
     */
    @Test
    fun arabicUtf8RoundTrip() {
        val arabicText = "مرحباً بك في نظام التواصل المحلي المشفر! تجربة الرسائل النصية باللغة العربية مع علامات التشكيل: ﴿بِسْمِ اللَّهِ﴾."
        val plainBytes = arabicText.toByteArray(Charsets.UTF_8)

        val encrypted = LocalCryptoEngine.encrypt(plainBytes)
        val decrypted = LocalCryptoEngine.decrypt(encrypted)

        assertNotNull("Decrypted Arabic payload must not be null", decrypted)
        val recoveredString = String(decrypted!!, Charsets.UTF_8)
        assertEquals("Decrypted text must exactly match the original Arabic UTF-8 text", arabicText, recoveredString)
    }

    /**
     * 6. EmptyOrSmallPayloadBehavior
     * Verifies the boundary behaviors:
     * - Encrypting and decrypting an empty 0-byte payload.
     * - Decrypting payloads that are smaller than or equal to NONCE_SIZE (12 bytes) returns null immediately.
     */
    @Test
    fun emptyOrSmallPayloadBehavior() {
        // Empty payload round-trip
        val emptyBytes = ByteArray(0)
        val encryptedEmpty = LocalCryptoEngine.encrypt(emptyBytes)
        // Expected size: 12 (nonce) + 0 (plain) + 16 (tag) = 28 bytes
        assertEquals("Empty payload packed size must be NONCE (12) + TAG (16) = 28 bytes", 28, encryptedEmpty.size)

        val decryptedEmpty = LocalCryptoEngine.decrypt(encryptedEmpty)
        assertNotNull("Decrypting encrypted empty payload must succeed", decryptedEmpty)
        assertEquals("Decrypted empty payload must have length 0", 0, decryptedEmpty!!.size)

        // Input payload smaller than or equal to NONCE_SIZE (12 bytes)
        assertNull("Payload of size 0 must return null", LocalCryptoEngine.decrypt(ByteArray(0)))
        assertNull("Payload of size 5 must return null", LocalCryptoEngine.decrypt(ByteArray(5)))
        assertNull("Payload exactly of NONCE_SIZE (12 bytes) must return null", LocalCryptoEngine.decrypt(ByteArray(LocalCryptoEngine.NONCE_SIZE)))
    }

    /**
     * 7. NonceLengthIsTwelveBytes
     * Asserts that the public constant NONCE_SIZE is exactly 12 bytes (the standard recommended
     * GCM nonce length), and verifies that the output packet structure reserves the first 12 bytes
     * as the nonce header.
     */
    @Test
    fun nonceLengthIsTwelveBytes() {
        assertEquals("GCM standard nonce size must be 12 bytes", 12, LocalCryptoEngine.NONCE_SIZE)

        val plainBytes = "Sample payload for layout verification".toByteArray(Charsets.UTF_8)
        val packed = LocalCryptoEngine.encrypt(plainBytes)

        // GCM tag is 128 bits = 16 bytes
        val expectedSize = LocalCryptoEngine.NONCE_SIZE + plainBytes.size + 16
        assertEquals("Packed size must equal NONCE_SIZE (12) + plainBytes.size + GCM_TAG (16)", expectedSize, packed.size)

        val extractedNonce = packed.copyOfRange(0, LocalCryptoEngine.NONCE_SIZE)
        assertEquals("Extracted nonce must have length 12", 12, extractedNonce.size)
    }

    /**
     * 8. JsonRoundTrip
     * Verifies the public [LocalCryptoEngine.encryptJson] and [LocalCryptoEngine.decryptJson] helpers
     * with valid JSON payloads and malformed packet rejection.
     */
    @Test
    fun jsonRoundTrip() {
        val jsonPayload = """{"type":"CHAT_MSG","messageId":"msg-12345","sender":"Peer_A","content":"مرحبا بالجميع","timestamp":1726000000}"""

        val packed = LocalCryptoEngine.encryptJson(jsonPayload)
        assertNotNull("Encrypted JSON bytes must not be null", packed)

        val recoveredJson = LocalCryptoEngine.decryptJson(packed)
        assertNotNull("Decrypted JSON string must not be null", recoveredJson)
        assertEquals("Decrypted JSON must match original JSON payload exactly", jsonPayload, recoveredJson)

        // Tampered JSON packet returns null
        packed[packed.size - 2] = (packed[packed.size - 2].toInt() xor 0xAA).toByte()
        val failedJson = LocalCryptoEngine.decryptJson(packed)
        assertNull("Tampered JSON packet must return null", failedJson)
    }

    /**
     * 9. ChunkNonceDeterminism
     * Verifies that [LocalCryptoEngine.deriveChunkNonce] produces a deterministic 12-byte nonce
     * from (fileId, chunkIndex, sessionId), and that varying any input alters the output nonce.
     */
    @Test
    fun chunkNonceDeterminism() {
        val fileId = "file-abc-987"
        val sessionId = "session-xyz-123"
        val chunkIndex = 42L

        val nonce1 = LocalCryptoEngine.deriveChunkNonce(fileId, chunkIndex, sessionId)
        val nonce2 = LocalCryptoEngine.deriveChunkNonce(fileId, chunkIndex, sessionId)

        // Check size
        assertEquals("Derived chunk nonce must be 12 bytes", LocalCryptoEngine.NONCE_SIZE, nonce1.size)

        // Determinism: Same parameters must yield identical nonces
        assertArrayEquals("Same parameters must yield identical nonces", nonce1, nonce2)

        // Altering chunkIndex must produce a different nonce
        val nonceDifferentChunk = LocalCryptoEngine.deriveChunkNonce(fileId, chunkIndex + 1, sessionId)
        assertFalse("Different chunkIndex must yield different nonce", nonce1.contentEquals(nonceDifferentChunk))

        // Altering fileId must produce a different nonce
        val nonceDifferentFile = LocalCryptoEngine.deriveChunkNonce("other-file", chunkIndex, sessionId)
        assertFalse("Different fileId must yield different nonce", nonce1.contentEquals(nonceDifferentFile))

        // Altering sessionId must produce a different nonce
        val nonceDifferentSession = LocalCryptoEngine.deriveChunkNonce(fileId, chunkIndex, "other-session")
        assertFalse("Different sessionId must yield different nonce", nonce1.contentEquals(nonceDifferentSession))
    }

    /**
     * 10. ChunkEncryptAndDecryptRoundTrip
     * Tests [LocalCryptoEngine.encryptChunk] and [LocalCryptoEngine.decryptChunk] with deterministic nonces.
     */
    @Test
    fun chunkEncryptAndDecryptRoundTrip() {
        val fileId = "transfer-test-file"
        val sessionId = "session-456"
        val chunkIndex = 5L
        val chunkData = ByteArray(1024) { (it % 256).toByte() }

        val encryptedChunk = LocalCryptoEngine.encryptChunk(chunkData, fileId, chunkIndex, sessionId)
        // encryptChunk output is ciphertext + tag (16 bytes), without prepended nonce (nonce is derived)
        assertEquals("Chunk ciphertext size must be data.size + 16-byte GCM tag", chunkData.size + 16, encryptedChunk.size)

        val decryptedChunk = LocalCryptoEngine.decryptChunk(encryptedChunk, fileId, chunkIndex, sessionId)
        assertNotNull("Decrypted chunk must not be null", decryptedChunk)
        assertArrayEquals("Decrypted chunk must match original data", chunkData, decryptedChunk)

        // Decrypting with wrong chunkIndex must fail
        val wrongChunkIndexResult = LocalCryptoEngine.decryptChunk(encryptedChunk, fileId, chunkIndex + 1, sessionId)
        assertNull("Decrypting with mismatched chunkIndex must fail AEAD verification and return null", wrongChunkIndexResult)
    }
}
