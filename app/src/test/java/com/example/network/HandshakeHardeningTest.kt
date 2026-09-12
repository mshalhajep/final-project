package com.example.network

import org.junit.After
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
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * JVM Unit Tests for Handshake Hardening (Phase 3B Hardening):
 * - Canonical Context-Bound Signatures (mitigating relay, MITM, and ephemeral key replays)
 * - TOFU (Trust On First Use) Identity Verification
 * - Downgrade Attack Prevention
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HandshakeHardeningTest {

    private lateinit var trustStore: InMemoryPeerTrustStore

    @Before
    fun setUp() {
        trustStore = InMemoryPeerTrustStore()
        PeerSessionKeyManager.initTrustStore(trustStore)
        PeerSessionKeyManager.clearAll()
    }

    @After
    fun tearDown() {
        trustStore.clearAll()
        PeerSessionKeyManager.clearAll()
    }

    private fun generateEcKeyPair(): java.security.KeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec("secp256r1"))
        return kpg.generateKeyPair()
    }

    private fun signData(privateKey: java.security.PrivateKey, data: ByteArray): ByteArray {
        val sig = Signature.getInstance("SHA256withECDSA")
        sig.initSign(privateKey)
        sig.update(data)
        return sig.sign()
    }

    private fun verifyData(publicKey: java.security.PublicKey, data: ByteArray, signature: ByteArray): Boolean {
        return try {
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initVerify(publicKey)
            sig.update(data)
            sig.verify(signature)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 1. Changing receiverId alters canonical binary serialization and fails verification.
     */
    @Test
    fun signatureChangesWhenReceiverIdChanges() {
        val keyPair = generateEcKeyPair()
        val ephemeralBytes = ByteArray(65) { 0x04 }
        val timestamp = 1700000000000L
        val sessionId = "session-test-01"

        val canonicalForBob = HandshakeCryptoUtils.buildCanonicalSignatureInput(
            protocolVersion = 2,
            senderId = "alice",
            receiverId = "bob",
            sessionId = sessionId,
            ephemeralPublicKeyBytes = ephemeralBytes,
            timestamp = timestamp
        )

        val canonicalForEve = HandshakeCryptoUtils.buildCanonicalSignatureInput(
            protocolVersion = 2,
            senderId = "alice",
            receiverId = "eve",
            sessionId = sessionId,
            ephemeralPublicKeyBytes = ephemeralBytes,
            timestamp = timestamp
        )

        assertFalse("Canonical inputs for different receivers must differ", canonicalForBob.contentEquals(canonicalForEve))

        val signature = signData(keyPair.private, canonicalForBob)
        assertTrue("Valid signature for Bob", verifyData(keyPair.public, canonicalForBob, signature))
        assertFalse("Signature for Bob must fail when verified as Eve", verifyData(keyPair.public, canonicalForEve, signature))
    }

    /**
     * 2. Changing sessionId in canonical signature input fails ECDSA signature verification.
     */
    @Test
    fun signatureVerificationFailsWhenSessionIdChanges() {
        val keyPair = generateEcKeyPair()
        val ephemeralBytes = ByteArray(65) { 0x02 }
        val timestamp = 1700000000000L

        val originalInput = HandshakeCryptoUtils.buildCanonicalSignatureInput(
            protocolVersion = 2,
            senderId = "alice",
            receiverId = "bob",
            sessionId = "session-alpha",
            ephemeralPublicKeyBytes = ephemeralBytes,
            timestamp = timestamp
        )
        val signature = signData(keyPair.private, originalInput)

        val modifiedSessionInput = HandshakeCryptoUtils.buildCanonicalSignatureInput(
            protocolVersion = 2,
            senderId = "alice",
            receiverId = "bob",
            sessionId = "session-beta",
            ephemeralPublicKeyBytes = ephemeralBytes,
            timestamp = timestamp
        )

        assertFalse("Signature must fail on tampered session ID", verifyData(keyPair.public, modifiedSessionInput, signature))
    }

    /**
     * 3. Modifying ephemeral key in canonical signature input fails verification.
     */
    @Test
    fun signatureVerificationFailsWhenEphemeralKeyChanges() {
        val keyPair = generateEcKeyPair()
        val originalEphemeralBytes = ByteArray(65) { 0x01 }
        val tamperedEphemeralBytes = ByteArray(65) { 0x09 }
        val timestamp = 1700000000000L
        val sessionId = "session-gamma"

        val originalInput = HandshakeCryptoUtils.buildCanonicalSignatureInput(
            protocolVersion = 2,
            senderId = "alice",
            receiverId = "bob",
            sessionId = sessionId,
            ephemeralPublicKeyBytes = originalEphemeralBytes,
            timestamp = timestamp
        )
        val signature = signData(keyPair.private, originalInput)

        val tamperedInput = HandshakeCryptoUtils.buildCanonicalSignatureInput(
            protocolVersion = 2,
            senderId = "alice",
            receiverId = "bob",
            sessionId = sessionId,
            ephemeralPublicKeyBytes = tamperedEphemeralBytes,
            timestamp = timestamp
        )

        assertFalse("Signature must fail on tampered ephemeral key bytes", verifyData(keyPair.public, tamperedInput, signature))
    }

    /**
     * 4. First identity is saved and returns TRUSTED_FIRST_USE for TOFU.
     */
    @Test
    fun firstIdentityIsStoredForTofu() {
        val peerId = "peer-charlie"
        val identityKeyBytes = ByteArray(65) { 0x05 }

        val result = trustStore.verifyOrStoreTrust(peerId, identityKeyBytes)
        assertEquals(PeerTrustStore.TrustResult.TRUSTED_FIRST_USE, result)

        val savedFingerprint = trustStore.getTrustedFingerprint(peerId)
        assertNotNull(savedFingerprint)
        assertEquals(HandshakeCryptoUtils.computeFingerprint(identityKeyBytes), savedFingerprint)
    }

    /**
     * 5. Presenting identical identity key fingerprint returns TRUSTED_EXISTING.
     */
    @Test
    fun sameIdentityFingerprintIsAccepted() {
        val peerId = "peer-david"
        val identityKeyBytes = ByteArray(65) { 0x07 }

        val firstResult = trustStore.verifyOrStoreTrust(peerId, identityKeyBytes)
        assertEquals(PeerTrustStore.TrustResult.TRUSTED_FIRST_USE, firstResult)

        val secondResult = trustStore.verifyOrStoreTrust(peerId, identityKeyBytes)
        assertEquals(PeerTrustStore.TrustResult.TRUSTED_EXISTING, secondResult)
    }

    /**
     * 6. Changed identity key fingerprint for known peer is rejected with IDENTITY_CHANGED.
     */
    @Test
    fun changedIdentityFingerprintIsRejected() {
        val peerId = "peer-target"
        val legitimateKeyBytes = ByteArray(65) { 0x11 }
        val attackerKeyBytes = ByteArray(65) { 0x22 }

        val trust1 = trustStore.verifyOrStoreTrust(peerId, legitimateKeyBytes)
        assertEquals(PeerTrustStore.TrustResult.TRUSTED_FIRST_USE, trust1)

        // Attacker attempts MITM with new identity key for the same peerId
        val trust2 = trustStore.verifyOrStoreTrust(peerId, attackerKeyBytes)
        assertEquals(PeerTrustStore.TrustResult.IDENTITY_CHANGED, trust2)

        // Fingerprint in store remains the original legitimate one
        assertEquals(
            HandshakeCryptoUtils.computeFingerprint(legitimateKeyBytes),
            trustStore.getTrustedFingerprint(peerId)
        )
    }

    /**
     * 7. Handshake failure transitions peer to HANDSHAKE_FAILED and wipes session key (preventing silent downgrade).
     */
    @Test
    fun handshakeFailureDoesNotSilentlyEnableLegacyFallback() {
        val peerId = "peer-failing"
        PeerSessionKeyManager.startHandshake(peerId)
        assertEquals(PeerSessionKeyManager.PeerSecurityStatus.HANDSHAKE_IN_PROGRESS, PeerSessionKeyManager.getSecurityStatus(peerId))

        // Record handshake failure
        PeerSessionKeyManager.recordHandshakeFailure(peerId)

        assertEquals(PeerSessionKeyManager.PeerSecurityStatus.HANDSHAKE_FAILED, PeerSessionKeyManager.getSecurityStatus(peerId))
        assertFalse("Session key must not exist on failure", PeerSessionKeyManager.hasSessionKey(peerId))
        assertNull(PeerSessionKeyManager.getSessionKey(peerId))
        assertNull(PeerSessionKeyManager.getPendingSessionId(peerId))
    }

    /**
     * 8. Unsupported or corrupted canonical signature input safely fails verification.
     */
    @Test
    fun unsupportedOrInvalidCanonicalDataIsRejected() {
        val keyPair = generateEcKeyPair()
        val garbageSignature = ByteArray(64) { 0xAA.toByte() }
        val data = "some-random-data".toByteArray(Charsets.UTF_8)

        val result = verifyData(keyPair.public, data, garbageSignature)
        assertFalse("Garbage signature must return false safely without throwing unhandled exception", result)
    }
}
