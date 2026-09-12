package com.example.network

import android.util.Log
import java.security.KeyPair
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey

/**
 * Thread-safe manager for negotiated peer-to-peer ECDH session keys, TOFU identity trust,
 * and security session state.
 *
 * Coordinates pairwise ECDH session keys established with peers:
 * - Maintains active pairwise [SecretKey] mapping per peer ID.
 * - Tracks pending handshake initiations and enforces timeouts.
 * - Tracks [PeerSecurityStatus] to prevent silent downgrade attacks to default keys.
 * - Integrates with [PeerTrustStore] for TOFU-based identity verification.
 */
object PeerSessionKeyManager {

    private const val TAG = "PeerSessionKeyManager"
    private const val HANDSHAKE_TIMEOUT_MS = 30_000L // 30 seconds

    enum class PeerSecurityStatus {
        LEGACY,
        HANDSHAKE_IN_PROGRESS,
        SECURE_SESSION_ACTIVE,
        HANDSHAKE_FAILED
    }

    private data class PendingHandshake(
        val ephemeralKeyPair: KeyPair,
        val sessionId: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    // Active negotiated AES-256 session keys: peerId -> SecretKey
    private val activeSessionKeys = ConcurrentHashMap<String, SecretKey>()

    // In-flight handshakes initiated by this node: peerId -> PendingHandshake
    private val pendingInitiations = ConcurrentHashMap<String, PendingHandshake>()

    // Security status per peer: peerId -> PeerSecurityStatus
    private val peerSecurityStatuses = ConcurrentHashMap<String, PeerSecurityStatus>()

    // Pluggable TOFU trust store, defaults to InMemory for unit tests
    @Volatile
    var trustStore: PeerTrustStore = InMemoryPeerTrustStore()

    /**
     * Initializes the trust store (e.g. PersistentPeerTrustStore in production).
     */
    fun initTrustStore(store: PeerTrustStore) {
        trustStore = store
    }

    /**
     * Checks if an established session key exists for the given peer.
     */
    fun hasSessionKey(peerId: String): Boolean = activeSessionKeys.containsKey(peerId)

    /**
     * Retrieves the pairwise AES-256 session key for the peer, or null if unestablished.
     */
    fun getSessionKey(peerId: String): SecretKey? = activeSessionKeys[peerId]

    /**
     * Explicitly sets a session key for a peer.
     */
    fun setSessionKey(peerId: String, key: SecretKey) {
        activeSessionKeys[peerId] = key
        peerSecurityStatuses[peerId] = PeerSecurityStatus.SECURE_SESSION_ACTIVE
        Log.i(TAG, "Active ECDH session key registered for peer: $peerId")
    }

    /**
     * Returns the current [PeerSecurityStatus] for [peerId].
     */
    fun getSecurityStatus(peerId: String): PeerSecurityStatus {
        return peerSecurityStatuses[peerId]
            ?: if (hasSessionKey(peerId)) PeerSecurityStatus.SECURE_SESSION_ACTIVE else PeerSecurityStatus.LEGACY
    }

    /**
     * Updates the [PeerSecurityStatus] for [peerId].
     */
    fun setSecurityStatus(peerId: String, status: PeerSecurityStatus) {
        peerSecurityStatuses[peerId] = status
        if (status == PeerSecurityStatus.HANDSHAKE_FAILED) {
            activeSessionKeys.remove(peerId)
            pendingInitiations.remove(peerId)
        }
    }

    /**
     * Records a handshake failure for [peerId], clearing keys and marking as HANDSHAKE_FAILED.
     */
    fun recordHandshakeFailure(peerId: String) {
        Log.w(TAG, "Recording handshake failure for peer: $peerId - Downgrade prevention activated")
        setSecurityStatus(peerId, PeerSecurityStatus.HANDSHAKE_FAILED)
    }

    /**
     * Retrieves the pending session ID for an in-flight handshake with [peerId].
     */
    fun getPendingSessionId(peerId: String): String? = pendingInitiations[peerId]?.sessionId

    /**
     * Initiates a handshake with a peer.
     * Generates a fresh ephemeral EC P-256 KeyPair, stores it in pending handshakes with [sessionId],
     * marks peer state as HANDSHAKE_IN_PROGRESS, and returns the serialized public key bytes.
     */
    fun startHandshake(peerId: String, sessionId: String = UUID.randomUUID().toString()): ByteArray {
        val ephemeralPair = EcdhEngine.generateEphemeralKeyPair()
        pendingInitiations[peerId] = PendingHandshake(ephemeralPair, sessionId)
        peerSecurityStatuses[peerId] = PeerSecurityStatus.HANDSHAKE_IN_PROGRESS
        return EcdhEngine.encodePublicKey(ephemeralPair.public)
    }

    /**
     * Responds to an incoming KEY_EXCHANGE_INIT from a remote peer:
     * 1. Decodes remote peer's ephemeral public key.
     * 2. Generates our own ephemeral EC P-256 KeyPair.
     * 3. Computes the shared secret and derives the 256-bit AES session key.
     * 4. Stores the established session key in [activeSessionKeys] and marks SECURE_SESSION_ACTIVE.
     * 5. Returns our serialized ephemeral public key bytes to be sent back in KEY_EXCHANGE_REPLY.
     */
    fun respondToHandshake(peerId: String, remoteEphemeralBytes: ByteArray): ByteArray? {
        val remotePublicKey = EcdhEngine.decodePublicKey(remoteEphemeralBytes) ?: run {
            Log.w(TAG, "Invalid remote ephemeral public key in KEY_EXCHANGE_INIT from $peerId")
            recordHandshakeFailure(peerId)
            return null
        }

        val myEphemeralPair = EcdhEngine.generateEphemeralKeyPair()
        val sessionKey = EcdhEngine.agreeAndDeriveKey(myEphemeralPair.private, remotePublicKey)

        activeSessionKeys[peerId] = sessionKey
        peerSecurityStatuses[peerId] = PeerSecurityStatus.SECURE_SESSION_ACTIVE
        Log.i(TAG, "Responded to handshake and activated session key for peer: $peerId")

        return EcdhEngine.encodePublicKey(myEphemeralPair.public)
    }

    /**
     * Completes an initiated handshake upon receiving KEY_EXCHANGE_REPLY:
     * 1. Looks up our pending ephemeral private key for this peer.
     * 2. Decodes remote peer's ephemeral public key.
     * 3. Computes the shared secret and derives the identical 256-bit AES session key.
     * 4. Stores the established session key and marks SECURE_SESSION_ACTIVE.
     */
    fun completeHandshake(peerId: String, remoteEphemeralBytes: ByteArray): SecretKey? {
        val pending = pendingInitiations.remove(peerId) ?: run {
            Log.w(TAG, "No pending handshake found for reply from $peerId")
            return null
        }

        if (System.currentTimeMillis() - pending.timestamp > HANDSHAKE_TIMEOUT_MS) {
            Log.w(TAG, "Pending handshake for $peerId timed out")
            recordHandshakeFailure(peerId)
            return null
        }

        val remotePublicKey = EcdhEngine.decodePublicKey(remoteEphemeralBytes) ?: run {
            Log.w(TAG, "Invalid remote ephemeral public key in KEY_EXCHANGE_REPLY from $peerId")
            recordHandshakeFailure(peerId)
            return null
        }

        val sessionKey = EcdhEngine.agreeAndDeriveKey(pending.ephemeralKeyPair.private, remotePublicKey)
        activeSessionKeys[peerId] = sessionKey
        peerSecurityStatuses[peerId] = PeerSecurityStatus.SECURE_SESSION_ACTIVE
        Log.i(TAG, "Completed handshake and activated session key for peer: $peerId")
        return sessionKey
    }

    /**
     * Clears the active session key and pending handshake for a specific peer.
     */
    fun clearSession(peerId: String) {
        activeSessionKeys.remove(peerId)
        pendingInitiations.remove(peerId)
        peerSecurityStatuses.remove(peerId)
    }

    /**
     * Clears all session keys, pending handshakes, and security statuses.
     */
    fun clearAll() {
        activeSessionKeys.clear()
        pendingInitiations.clear()
        peerSecurityStatuses.clear()
    }
}
