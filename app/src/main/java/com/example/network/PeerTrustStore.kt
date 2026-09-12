package com.example.network

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface defining identity trust operations using TOFU (Trust On First Use).
 */
interface PeerTrustStore {

    enum class TrustResult {
        TRUSTED_FIRST_USE,
        TRUSTED_EXISTING,
        IDENTITY_CHANGED
    }

    /**
     * Verifies the peer's identity key fingerprint against previously saved fingerprints,
     * or records it if this is the first interaction with the peer.
     */
    fun verifyOrStoreTrust(peerId: String, identityPublicKeyBytes: ByteArray): TrustResult

    /**
     * Returns the recorded SHA-256 fingerprint for [peerId], or null if not yet recorded.
     */
    fun getTrustedFingerprint(peerId: String): String?

    /**
     * Clears recorded trust for a single peer.
     */
    fun resetTrust(peerId: String)

    /**
     * Clears all recorded trust mappings.
     */
    fun clearAll()
}

/**
 * In-memory implementation of [PeerTrustStore] suitable for unit testing and ephemeral sessions.
 */
class InMemoryPeerTrustStore : PeerTrustStore {
    private val store = ConcurrentHashMap<String, String>()

    @Synchronized
    override fun verifyOrStoreTrust(peerId: String, identityPublicKeyBytes: ByteArray): PeerTrustStore.TrustResult {
        val newFingerprint = HandshakeCryptoUtils.computeFingerprint(identityPublicKeyBytes)
        val existingFingerprint = store[peerId]

        return if (existingFingerprint == null) {
            store[peerId] = newFingerprint
            PeerTrustStore.TrustResult.TRUSTED_FIRST_USE
        } else if (existingFingerprint == newFingerprint) {
            PeerTrustStore.TrustResult.TRUSTED_EXISTING
        } else {
            PeerTrustStore.TrustResult.IDENTITY_CHANGED
        }
    }

    override fun getTrustedFingerprint(peerId: String): String? = store[peerId]

    override fun resetTrust(peerId: String) {
        store.remove(peerId)
    }

    override fun clearAll() {
        store.clear()
    }
}

/**
 * SharedPreferences-backed implementation of [PeerTrustStore] for persistent storage across app launches.
 */
class PersistentPeerTrustStore(context: Context) : PeerTrustStore {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun verifyOrStoreTrust(peerId: String, identityPublicKeyBytes: ByteArray): PeerTrustStore.TrustResult {
        val newFingerprint = HandshakeCryptoUtils.computeFingerprint(identityPublicKeyBytes)
        val existingFingerprint = prefs.getString(peerId, null)

        return if (existingFingerprint == null) {
            prefs.edit().putString(peerId, newFingerprint).apply()
            PeerTrustStore.TrustResult.TRUSTED_FIRST_USE
        } else if (existingFingerprint == newFingerprint) {
            PeerTrustStore.TrustResult.TRUSTED_EXISTING
        } else {
            PeerTrustStore.TrustResult.IDENTITY_CHANGED
        }
    }

    override fun getTrustedFingerprint(peerId: String): String? = prefs.getString(peerId, null)

    override fun resetTrust(peerId: String) {
        prefs.edit().remove(peerId).apply()
    }

    override fun clearAll() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "com.example.network.PEER_TRUST_STORE"
    }
}
