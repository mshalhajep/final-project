package com.example.network

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * JVM Unit Tests for [PeerSessionKeyManager].
 *
 * Verifies:
 * 1. End-to-end simulated handshake between Alice and Bob.
 * 2. Key matching and bidirectional AES-GCM encryption/decryption using stored session keys.
 * 3. Handling invalid or unrecognized replies.
 * 4. Session cleanup and multi-peer session isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PeerSessionKeyManagerTest {

    @Before
    @After
    fun cleanUp() {
        PeerSessionKeyManager.clearAll()
    }

    /**
     * 1. Simulates complete two-party ECDH handshake between Alice and Bob.
     */
    @Test
    fun simulatedHandshakeEstablishesIdenticalKeys() {
        val alicePeerId = "peer-alice-001"
        val bobPeerId = "peer-bob-002"

        // 1. Alice starts handshake to Bob
        val aliceEphemeralBytes = PeerSessionKeyManager.startHandshake(bobPeerId)
        assertTrue(aliceEphemeralBytes.isNotEmpty())

        // 2. Bob responds to Alice's handshake init
        // (In real network, Bob is another device; here we simulate Bob's side using EcdhEngine directly)
        val bobEphemeralPair = EcdhEngine.generateEphemeralKeyPair()
        val alicePublicKey = EcdhEngine.decodePublicKey(aliceEphemeralBytes)
        assertNotNull(alicePublicKey)
        val bobDerivedKey = EcdhEngine.agreeAndDeriveKey(bobEphemeralPair.private, alicePublicKey!!)
        val bobEphemeralBytes = EcdhEngine.encodePublicKey(bobEphemeralPair.public)

        // 3. Alice completes handshake using Bob's reply
        val aliceDerivedKey = PeerSessionKeyManager.completeHandshake(bobPeerId, bobEphemeralBytes)
        assertNotNull("Alice must complete handshake successfully", aliceDerivedKey)

        // 4. Verify keys match exactly
        assertArrayEquals(
            "Alice's stored session key must match Bob's derived key",
            bobDerivedKey.encoded,
            aliceDerivedKey!!.encoded
        )
        assertTrue(PeerSessionKeyManager.hasSessionKey(bobPeerId))
        assertEquals(aliceDerivedKey, PeerSessionKeyManager.getSessionKey(bobPeerId))
    }

    /**
     * 2. Tests communication encryption: Alice encrypts with her session key from [PeerSessionKeyManager],
     * and Bob decrypts with his derived session key.
     */
    @Test
    fun communicationUsingPeerSessionKey() {
        val peerId = "peer-charlie-999"

        val myEphemeralBytes = PeerSessionKeyManager.startHandshake(peerId)
        val remotePair = EcdhEngine.generateEphemeralKeyPair()
        val remoteKey = EcdhEngine.agreeAndDeriveKey(remotePair.private, EcdhEngine.decodePublicKey(myEphemeralBytes)!!)
        val remoteEphemeralBytes = EcdhEngine.encodePublicKey(remotePair.public)

        val localKey = PeerSessionKeyManager.completeHandshake(peerId, remoteEphemeralBytes)
        assertNotNull(localKey)

        val message = "Direct private message sent using pairwise ECDH session key!"
        val plainBytes = message.toByteArray(Charsets.UTF_8)

        // Encrypt with local session key retrieved from manager
        val activeKey = PeerSessionKeyManager.getSessionKey(peerId)!!
        val encrypted = LocalCryptoEngine.encrypt(plainBytes, key = activeKey)

        // Remote peer decrypts using remoteKey
        val decrypted = LocalCryptoEngine.decrypt(encrypted, key = remoteKey)
        assertNotNull(decrypted)
        assertEquals(message, String(decrypted!!, Charsets.UTF_8))
    }

    /**
     * 3. Replying with corrupted or invalid public key fails handshake completion safely.
     */
    @Test
    fun corruptedReplyFailsHandshakeCompletion() {
        val peerId = "peer-attacker-007"
        PeerSessionKeyManager.startHandshake(peerId)

        val garbageBytes = ByteArray(32) { 0xAA.toByte() }
        val result = PeerSessionKeyManager.completeHandshake(peerId, garbageBytes)

        assertNull("Corrupted reply must return null", result)
        assertFalse("Session key must not be established on corrupted reply", PeerSessionKeyManager.hasSessionKey(peerId))
    }

    /**
     * 4. Session cleanup removes active keys.
     */
    @Test
    fun clearSessionRemovesPeerKey() {
        val peerId = "peer-temporary-555"
        val fakeKey = javax.crypto.spec.SecretKeySpec(ByteArray(32) { 1 }, "AES")
        PeerSessionKeyManager.setSessionKey(peerId, fakeKey)

        assertTrue(PeerSessionKeyManager.hasSessionKey(peerId))
        PeerSessionKeyManager.clearSession(peerId)
        assertFalse(PeerSessionKeyManager.hasSessionKey(peerId))
        assertNull(PeerSessionKeyManager.getSessionKey(peerId))
    }
}
