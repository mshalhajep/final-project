package com.example.network

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.security.MessageDigest

/**
 * Utility functions for cryptographic context binding and fingerprint computation in LocalConnect.
 *
 * Enforces canonical binary serialization for ECDSA signatures during ECDH handshakes,
 * ensuring that ephemeral keys are cryptographically bound to the session context (Protocol Version,
 * Sender ID, Receiver ID, Session ID, and Timestamp), mitigating replay and relay attacks.
 */
object HandshakeCryptoUtils {

    const val PROTOCOL_VERSION = 2
    private const val DOMAIN_TAG = "LocalConnect::Handshake::v1"

    /**
     * Constructs a deterministic, canonical binary representation of the handshake parameters.
     *
     * Serialization layout:
     * - Domain Tag (UTF String)
     * - Protocol Version (Int32)
     * - Sender ID (UTF String)
     * - Receiver ID (UTF String)
     * - Session ID (UTF String)
     * - Timestamp (Int64)
     * - Ephemeral Key Length (Int32)
     * - Ephemeral Public Key Bytes (Raw Bytes)
     */
    fun buildCanonicalSignatureInput(
        protocolVersion: Int,
        senderId: String,
        receiverId: String,
        sessionId: String,
        ephemeralPublicKeyBytes: ByteArray,
        timestamp: Long
    ): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)
        dos.writeUTF(DOMAIN_TAG)
        dos.writeInt(protocolVersion)
        dos.writeUTF(senderId)
        dos.writeUTF(receiverId)
        dos.writeUTF(sessionId)
        dos.writeLong(timestamp)
        dos.writeInt(ephemeralPublicKeyBytes.size)
        dos.write(ephemeralPublicKeyBytes)
        dos.flush()
        return bos.toByteArray()
    }

    /**
     * Computes the hex-formatted SHA-256 fingerprint of a public key byte array.
     * Used for TOFU (Trust On First Use) identity verification.
     */
    fun computeFingerprint(publicKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(publicKeyBytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}
