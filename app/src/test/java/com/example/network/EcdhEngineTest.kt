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

/**
 * JVM Unit Tests for [EcdhEngine].
 *
 * Verifies the mathematical and cryptographic properties of standalone ECDH + HKDF:
 * 1. Key pair generation (NIST P-256).
 * 2. Symmetric key agreement (Alice and Bob derive the exact same 256-bit AES key).
 * 3. Interoperability with [LocalCryptoEngine] (Alice encrypts, Bob decrypts with negotiated key).
 * 4. Forward secrecy (Fresh ephemeral keys generate unique session keys).
 * 5. Public key serialization (X.509 encoding & decoding).
 * 6. Error handling for malformed or corrupted public keys.
 * 7. HKDF determinism and domain separation.
 * 8. Cryptographic separation (Eve cannot decrypt Alice-Bob traffic).
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EcdhEngineTest {

    /**
     * 1. Verifies that ephemeral EC key pairs are generated properly on NIST P-256 curve.
     */
    @Test
    fun generateEphemeralKeyPairReturnsValidECKeys() {
        val keyPair = EcdhEngine.generateEphemeralKeyPair()

        assertNotNull("KeyPair must not be null", keyPair)
        assertNotNull("Public key must not be null", keyPair.public)
        assertNotNull("Private key must not be null", keyPair.private)
        assertEquals("Algorithm must be EC", "EC", keyPair.public.algorithm)
        assertEquals("Format must be X.509", "X.509", keyPair.public.format)
    }

    /**
     * 2. Alice and Bob perform symmetric ECDH agreement and derive the EXACT same AES-256 session key.
     */
    @Test
    fun aliceAndBobAgreeOnIdenticalSessionKey() {
        // Alice generates ephemeral pair
        val aliceKeyPair = EcdhEngine.generateEphemeralKeyPair()
        // Bob generates ephemeral pair
        val bobKeyPair = EcdhEngine.generateEphemeralKeyPair()

        // Alice computes session key using her private key and Bob's public key
        val aliceSessionKey = EcdhEngine.agreeAndDeriveKey(aliceKeyPair.private, bobKeyPair.public)

        // Bob computes session key using his private key and Alice's public key
        val bobSessionKey = EcdhEngine.agreeAndDeriveKey(bobKeyPair.private, aliceKeyPair.public)

        assertNotNull("Alice session key must not be null", aliceSessionKey)
        assertNotNull("Bob session key must not be null", bobSessionKey)
        assertEquals("Session key algorithm must be AES", "AES", aliceSessionKey.algorithm)
        assertEquals("Session key length must be 32 bytes (256 bits)", 32, aliceSessionKey.encoded.size)

        // The two derived keys must match bit-for-bit
        assertArrayEquals(
            "Alice and Bob must compute identical AES-256 session keys from ECDH agreement",
            aliceSessionKey.encoded,
            bobSessionKey.encoded
        )
    }

    /**
     * 3. Tests end-to-end integration: Alice encrypts payload with the negotiated session key,
     * and Bob decrypts it using his independently derived session key via [LocalCryptoEngine].
     */
    @Test
    fun sessionKeyCanEncryptAndDecryptWithLocalCryptoEngine() {
        val aliceKeyPair = EcdhEngine.generateEphemeralKeyPair()
        val bobKeyPair = EcdhEngine.generateEphemeralKeyPair()

        val aliceKey = EcdhEngine.agreeAndDeriveKey(aliceKeyPair.private, bobKeyPair.public)
        val bobKey = EcdhEngine.agreeAndDeriveKey(bobKeyPair.private, aliceKeyPair.public)

        val confidentialMessage = "Classified P2P message protected by ECDH session key!"
        val plainBytes = confidentialMessage.toByteArray(Charsets.UTF_8)

        // Alice encrypts with her session key
        val cipherPayload = LocalCryptoEngine.encrypt(plainBytes, key = aliceKey)
        assertNotNull(cipherPayload)

        // Bob decrypts with his session key
        val decryptedBytes = LocalCryptoEngine.decrypt(cipherPayload, key = bobKey)
        assertNotNull("Bob must successfully decrypt Alice's payload with negotiated key", decryptedBytes)

        val decryptedString = String(decryptedBytes!!, Charsets.UTF_8)
        assertEquals("Decrypted message must match original text exactly", confidentialMessage, decryptedString)
    }

    /**
     * 4. Forward Secrecy: Each handshake generates fresh ephemeral keys, yielding completely distinct
     * session keys even between the same two devices.
     */
    @Test
    fun freshEphemeralKeysProduceDistinctSessionKeys() {
        // Session 1
        val a1 = EcdhEngine.generateEphemeralKeyPair()
        val b1 = EcdhEngine.generateEphemeralKeyPair()
        val sessionKey1 = EcdhEngine.agreeAndDeriveKey(a1.private, b1.public)

        // Session 2
        val a2 = EcdhEngine.generateEphemeralKeyPair()
        val b2 = EcdhEngine.generateEphemeralKeyPair()
        val sessionKey2 = EcdhEngine.agreeAndDeriveKey(a2.private, b2.public)

        assertFalse(
            "Consecutive sessions with fresh ephemeral keys must produce different session keys (Forward Secrecy)",
            sessionKey1.encoded.contentEquals(sessionKey2.encoded)
        )
    }

    /**
     * 5. Serialization and Deserialization:
     * Alice encodes public key into X.509 byte array, transmits it, and Bob deserializes it.
     */
    @Test
    fun publicKeySerializationAndDeserializationRoundTrip() {
        val aliceKeyPair = EcdhEngine.generateEphemeralKeyPair()
        val bobKeyPair = EcdhEngine.generateEphemeralKeyPair()

        // Alice serializes her public key
        val aliceSerializedBytes = EcdhEngine.encodePublicKey(aliceKeyPair.public)
        assertTrue("Serialized public key bytes must not be empty", aliceSerializedBytes.isNotEmpty())

        // Bob receives and deserializes Alice's public key
        val bobReceivedAlicePublic = EcdhEngine.decodePublicKey(aliceSerializedBytes)
        assertNotNull("Deserialized public key must not be null", bobReceivedAlicePublic)
        assertArrayEquals(
            "Deserialized public key bytes must match original bytes",
            aliceSerializedBytes,
            bobReceivedAlicePublic!!.encoded
        )

        // Bob uses the deserialized public key to compute the session key
        val bobSessionKey = EcdhEngine.agreeAndDeriveKey(bobKeyPair.private, bobReceivedAlicePublic)
        val aliceSessionKey = EcdhEngine.agreeAndDeriveKey(aliceKeyPair.private, bobKeyPair.public)

        assertArrayEquals("Session key computed from deserialized public key must match", aliceSessionKey.encoded, bobSessionKey.encoded)
    }

    /**
     * 6. Error Handling: Passing malformed or corrupted public key bytes returns null safely.
     */
    @Test
    fun malformedPublicKeyBytesReturnsNull() {
        assertNull("Empty bytes must return null", EcdhEngine.decodePublicKey(ByteArray(0)))
        assertNull("Random garbage bytes must return null", EcdhEngine.decodePublicKey(ByteArray(32) { 0xFF.toByte() }))
        assertNull("Truncated header bytes must return null", EcdhEngine.decodePublicKey(byteArrayOf(0x30, 0x59, 0x30)))
    }

    /**
     * 7. HKDF Determinism and Domain Separation:
     * Same input = same key; different info/salt = cryptographically distinct key.
     */
    @Test
    fun hkdfDeterminismAndDomainSeparation() {
        val a = EcdhEngine.generateEphemeralKeyPair()
        val b = EcdhEngine.generateEphemeralKeyPair()
        val sharedSecret = EcdhEngine.computeSharedSecret(a.private, b.public)

        val keyA = EcdhEngine.deriveSessionKey(sharedSecret, info = "domain-A".toByteArray())
        val keyA2 = EcdhEngine.deriveSessionKey(sharedSecret, info = "domain-A".toByteArray())
        val keyB = EcdhEngine.deriveSessionKey(sharedSecret, info = "domain-B".toByteArray())

        // Determinism
        assertArrayEquals("Same sharedSecret and info must produce identical key", keyA.encoded, keyA2.encoded)

        // Domain separation
        assertFalse(
            "Different domain info must produce different key from the same shared secret",
            keyA.encoded.contentEquals(keyB.encoded)
        )
    }

    /**
     * 8. Cryptographic Separation: An unauthorized peer (Eve) computing key with her own keys
     * cannot decrypt Alice & Bob's communication.
     */
    @Test
    fun wrongPeerPublicKeyFailsDecryption() {
        val alice = EcdhEngine.generateEphemeralKeyPair()
        val bob = EcdhEngine.generateEphemeralKeyPair()
        val eve = EcdhEngine.generateEphemeralKeyPair()

        val aliceBobKey = EcdhEngine.agreeAndDeriveKey(alice.private, bob.public)
        val eveAliceKey = EcdhEngine.agreeAndDeriveKey(eve.private, alice.public)

        val plaintext = "Super secret transaction payload".toByteArray(Charsets.UTF_8)
        val ciphertext = LocalCryptoEngine.encrypt(plaintext, key = aliceBobKey)

        // Eve attempts decryption with her key
        val eveDecrypted = LocalCryptoEngine.decrypt(ciphertext, key = eveAliceKey)
        assertNull("Eve must fail to decrypt Alice & Bob's ciphertext", eveDecrypted)
    }
}
