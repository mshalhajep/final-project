package com.example.network

import android.util.Log
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * Standalone Elliptic Curve Diffie-Hellman (ECDH) Engine for LocalConnect.
 *
 * Security Characteristics:
 * - Curve: NIST P-256 / secp256r1 (prime256v1), standard 256-bit elliptic curve.
 * - Key Agreement: ECDH via standard Java Cryptography Architecture (`KeyAgreement.getInstance("ECDH")`).
 * - Key Derivation Function: HKDF-SHA256 (RFC 5869) Extract-and-Expand.
 * - Derived Output: 256-bit AES symmetric session key (`SecretKey`).
 * - Forward Secrecy: Each handshake generates fresh ephemeral key pairs; compromising a long-term
 *   identity key does not decrypt past recorded traffic.
 * - Interoperability: Public keys are serialized using standard X.509 SubjectPublicKeyInfo (DER) format.
 * - JVM & Android Native: Runs seamlessly across both standard JVM (unit tests) and Android devices (API 24+).
 */
object EcdhEngine {

    private const val TAG = "EcdhEngine"
    private const val EC_ALGORITHM = "EC"
    private const val ECDH_ALGORITHM = "ECDH"
    private const val HMAC_ALGORITHM = "HmacSHA256"
    private const val AES_ALGORITHM = "AES"
    private const val CURVE_NAME = "secp256r1"
    private const val DERIVED_KEY_BYTES = 32 // 256 bits

    private val secureRandom = SecureRandom()

    /**
     * Generates a fresh ephemeral EC P-256 key pair.
     * Ephemeral keys MUST be generated once per handshake session and discarded after key agreement.
     */
    fun generateEphemeralKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(EC_ALGORITHM)
        kpg.initialize(ECGenParameterSpec(CURVE_NAME), secureRandom)
        return kpg.generateKeyPair()
    }

    /**
     * Serializes an EC public key to a standard X.509 DER byte array.
     */
    fun encodePublicKey(publicKey: PublicKey): ByteArray {
        return publicKey.encoded
    }

    /**
     * Deserializes an X.509 DER-encoded byte array into an EC [PublicKey].
     *
     * @param encodedBytes Raw X.509 SubjectPublicKeyInfo bytes.
     * @return Deserialized [PublicKey], or null if parsing fails.
     */
    fun decodePublicKey(encodedBytes: ByteArray): PublicKey? {
        return try {
            val keyFactory = KeyFactory.getInstance(EC_ALGORITHM)
            val keySpec = X509EncodedKeySpec(encodedBytes)
            keyFactory.generatePublic(keySpec)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode EC public key from bytes: ${e.message}")
            null
        }
    }

    /**
     * Computes the raw ECDH shared secret between our private key and the peer's public key.
     *
     * @param myPrivateKey Our ephemeral private key.
     * @param peerPublicKey The peer's ephemeral public key.
     * @return Raw shared secret byte array.
     */
    fun computeSharedSecret(myPrivateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance(ECDH_ALGORITHM)
        agreement.init(myPrivateKey)
        agreement.doPhase(peerPublicKey, true)
        return agreement.generateSecret()
    }

    /**
     * Derives a 256-bit AES symmetric session key from an ECDH shared secret using
     * RFC 5869 HKDF-SHA256 (Extract-and-Expand).
     *
     * @param sharedSecret Raw ECDH shared secret (Input Keying Material / IKM).
     * @param salt Optional cryptographic salt (defaults to 32 zero bytes per RFC 5869).
     * @param info Contextual domain-separation info (e.g. "LocalConnect::P2P::SessionKey::v1").
     * @return A 256-bit AES [SecretKey] ready for AES-256-GCM encryption.
     */
    fun deriveSessionKey(
        sharedSecret: ByteArray,
        salt: ByteArray? = null,
        info: ByteArray? = "LocalConnect::P2P::SessionKey::v1".toByteArray(Charsets.UTF_8)
    ): SecretKey {
        // 1. HKDF-Extract: PRK = HMAC-SHA256(salt, IKM)
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        val actualSalt = salt ?: ByteArray(32) { 0 }
        mac.init(SecretKeySpec(actualSalt, HMAC_ALGORITHM))
        val prk = mac.doFinal(sharedSecret)

        // 2. HKDF-Expand: OKM = HMAC-SHA256(PRK, info || 0x01)
        mac.init(SecretKeySpec(prk, HMAC_ALGORITHM))
        if (info != null) {
            mac.update(info)
        }
        mac.update(0x01.toByte())
        val okm = mac.doFinal()

        // Extract first 32 bytes (256 bits) for AES-256
        val keyBytes = okm.copyOf(DERIVED_KEY_BYTES)
        return SecretKeySpec(keyBytes, AES_ALGORITHM)
    }

    /**
     * Convenience method to complete the full key agreement in a single invocation:
     * Computes ECDH shared secret and immediately derives the 256-bit AES session key via HKDF.
     *
     * @param myPrivateKey Our ephemeral private key.
     * @param peerPublicKey The peer's ephemeral public key.
     * @param salt Optional salt.
     * @param info Optional context info.
     * @return Negotiated AES-256 [SecretKey].
     */
    fun agreeAndDeriveKey(
        myPrivateKey: PrivateKey,
        peerPublicKey: PublicKey,
        salt: ByteArray? = null,
        info: ByteArray? = "LocalConnect::P2P::SessionKey::v1".toByteArray(Charsets.UTF_8)
    ): SecretKey {
        val sharedSecret = computeSharedSecret(myPrivateKey, peerPublicKey)
        return deriveSessionKey(sharedSecret, salt, info)
    }
}
