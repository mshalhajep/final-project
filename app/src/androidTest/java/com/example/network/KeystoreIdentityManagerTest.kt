package com.example.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [KeystoreIdentityManager] executing on an Android device or emulator.
 *
 * Verifies real interaction with the "AndroidKeyStore" security provider:
 * 1. Creation of EC P-256 identity key pair when alias does not exist.
 * 2. Key persistence and reuse upon repeated calls.
 * 3. ECDSA SHA-256 signing and public key verification round-trip.
 * 4. Rejection of signature when message data has been tampered with.
 * 5. Rejection of invalid or corrupted signatures.
 */
@RunWith(AndroidJUnit4::class)
class KeystoreIdentityManagerTest {

    @Before
    fun setUp() {
        // Ensure clean state before each test run
        KeystoreIdentityManager.deleteIdentityKey()
    }

    /**
     * 1. Creates identity key when alias does not exist.
     */
    @Test
    fun testCreatesIdentityKeyWhenAliasDoesNotExist() {
        val keyPair = KeystoreIdentityManager.getOrCreateIdentityKeyPair()
        assertNotNull("KeyPair must not be null", keyPair)
        assertNotNull("Public key must not be null", keyPair.public)
        assertNotNull("Private key must not be null", keyPair.private)
        assertEquals("Key algorithm must be EC", "EC", keyPair.public.algorithm)
    }

    /**
     * 2. Reuses existing key when getOrCreateIdentityKeyPair is called again.
     */
    @Test
    fun testReusesExistingKeyUponRepeatedCalls() {
        val firstKeyPair = KeystoreIdentityManager.getOrCreateIdentityKeyPair()
        val firstPublicKeyEncoded = firstKeyPair.public.encoded

        val secondKeyPair = KeystoreIdentityManager.getOrCreateIdentityKeyPair()
        val secondPublicKeyEncoded = secondKeyPair.public.encoded

        assertArrayEquals(
            "Repeated calls to getOrCreateIdentityKeyPair must return the same existing public key",
            firstPublicKeyEncoded,
            secondPublicKeyEncoded
        )
    }

    /**
     * 3. Signs data and verifies signature using the public key.
     */
    @Test
    fun testSignAndVerifyValidSignature() {
        val payload = "LocalConnect Handshake Identity Message: Peer-12345".toByteArray(Charsets.UTF_8)
        val signature = KeystoreIdentityManager.sign(payload)

        assertNotNull("Signature must not be null", signature)
        assertTrue("Signature byte array must not be empty", signature.isNotEmpty())

        val publicKey = KeystoreIdentityManager.getIdentityPublicKey()
        val isValid = KeystoreIdentityManager.verify(publicKey, payload, signature)

        assertTrue("Signature verification must succeed for authentic payload and key", isValid)
    }

    /**
     * 4. Verification fails when data has been modified / tampered with.
     */
    @Test
    fun testVerificationFailsWhenDataIsTampered() {
        val originalPayload = "Authenticate: Device-A to Device-B".toByteArray(Charsets.UTF_8)
        val signature = KeystoreIdentityManager.sign(originalPayload)
        val publicKey = KeystoreIdentityManager.getIdentityPublicKey()

        val tamperedPayload = "Authenticate: Device-Attacker to Device-B".toByteArray(Charsets.UTF_8)
        val isValid = KeystoreIdentityManager.verify(publicKey, tamperedPayload, signature)

        assertFalse("Verification must fail when payload is tampered with", isValid)
    }

    /**
     * 5. Verification fails when signature is corrupted or invalid.
     */
    @Test
    fun testVerificationFailsWhenSignatureIsCorrupted() {
        val payload = "Important Transaction Payload".toByteArray(Charsets.UTF_8)
        val signature = KeystoreIdentityManager.sign(payload)
        val publicKey = KeystoreIdentityManager.getIdentityPublicKey()

        // Corrupt a byte in the DER-encoded signature
        val corruptedSignature = signature.copyOf()
        corruptedSignature[corruptedSignature.size / 2] =
            (corruptedSignature[corruptedSignature.size / 2].toInt() xor 0x55).toByte()

        val isValid = KeystoreIdentityManager.verify(publicKey, payload, corruptedSignature)

        assertFalse("Verification must fail when signature is corrupted", isValid)
    }
}
