package com.example.network

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.InvalidKeyException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.UnrecoverableEntryException
import java.security.spec.ECGenParameterSpec

/**
 * Manages long-term device identity key pair stored inside Android Keystore.
 *
 * Security Architecture (Phase 2):
 * - Key Algorithm: EC (Elliptic Curve) secp256r1 (NIST P-256).
 * - Purpose: Digital signatures (PURPOSE_SIGN / PURPOSE_VERIFY) for future P2P handshake authentication.
 * - Storage: Android Keystore ("AndroidKeyStore").
 * - Non-exportable: The private key material never leaves the Keystore and cannot be exported.
 * - No Logging: Private keys and sensitive plaintext payloads are NEVER logged to Logcat.
 * - Hardware Backing: Opportunistic (TEE / StrongBox when supported by hardware; software fallback otherwise).
 * - Scope Isolation: This manager handles DEVICE IDENTITY only. It does NOT manage AES-256 network
 *   session encryption, does NOT perform ECDH key agreement, and does NOT alter packet protocols.
 */
object KeystoreIdentityManager {

    private const val TAG = "KeystoreIdentityManager"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    const val IDENTITY_KEY_ALIAS = "LocalConnect_Identity_EC_P256"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val EC_CURVE_NAME = "secp256r1"

    /**
     * Retrieves the existing device identity KeyPair from Android Keystore,
     * or generates a fresh EC P-256 pair if it does not yet exist.
     *
     * The private key remains protected inside the Android Keystore provider
     * and cannot be exported or stored elsewhere.
     */
    @Synchronized
    fun getOrCreateIdentityKeyPair(): KeyPair {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        try {
            if (ks.containsAlias(IDENTITY_KEY_ALIAS)) {
                val entry = ks.getEntry(IDENTITY_KEY_ALIAS, null)
                if (entry is KeyStore.PrivateKeyEntry) {
                    val cert = entry.certificate
                    if (cert != null) {
                        return KeyPair(cert.publicKey, entry.privateKey)
                    }
                }
            }
        } catch (e: KeyStoreException) {
            Log.w(TAG, "KeyStoreException accessing alias '$IDENTITY_KEY_ALIAS': ${e.message}")
        } catch (e: NoSuchAlgorithmException) {
            Log.w(TAG, "NoSuchAlgorithmException accessing key entry: ${e.message}")
        } catch (e: UnrecoverableEntryException) {
            Log.w(TAG, "UnrecoverableEntryException for alias '$IDENTITY_KEY_ALIAS': ${e.message}")
        }

        return generateIdentityKeyPair()
    }

    private fun generateIdentityKeyPair(): KeyPair {
        try {
            val kpg = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEYSTORE
            )
            val parameterSpec = KeyGenParameterSpec.Builder(
                IDENTITY_KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE_NAME))
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build()

            kpg.initialize(parameterSpec)
            val keyPair = kpg.generateKeyPair()
            Log.i(TAG, "Generated fresh EC P-256 identity key pair with alias '$IDENTITY_KEY_ALIAS'")
            return keyPair
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "NoSuchAlgorithmException while generating EC key pair: ${e.message}")
            throw e
        } catch (e: NoSuchProviderException) {
            Log.e(TAG, "NoSuchProviderException for AndroidKeyStore: ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate identity key pair in AndroidKeyStore: ${e.message}")
            throw e
        }
    }

    /**
     * Returns the public key of the device identity key pair.
     * This public key can be safely shared over the network with peers for handshake verification.
     */
    fun getIdentityPublicKey(): PublicKey {
        return getOrCreateIdentityKeyPair().public
    }

    /**
     * Signs arbitrary payload bytes using the device's Keystore-protected private key.
     * Uses SHA256withECDSA.
     *
     * @param data Raw bytes to be signed.
     * @return DER-encoded ECDSA signature bytes.
     * @throws SecurityException If signing fails due to invalid key or signature algorithm issues.
     */
    fun sign(data: ByteArray): ByteArray {
        val privateKey = getOrCreateIdentityKeyPair().private
        return try {
            val signer = Signature.getInstance(SIGNATURE_ALGORITHM)
            signer.initSign(privateKey)
            signer.update(data)
            signer.sign()
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Signature algorithm not supported: $SIGNATURE_ALGORITHM", e)
            throw SecurityException("Signature algorithm not supported: $SIGNATURE_ALGORITHM", e)
        } catch (e: InvalidKeyException) {
            Log.e(TAG, "Invalid private key for signing", e)
            throw SecurityException("Invalid private key for signing", e)
        } catch (e: SignatureException) {
            Log.e(TAG, "Failed to sign payload", e)
            throw SecurityException("Failed to sign payload", e)
        }
    }

    /**
     * Signs arbitrary payload bytes, returning null if signing fails instead of throwing an exception.
     */
    fun signOrNull(data: ByteArray): ByteArray? {
        return try {
            sign(data)
        } catch (e: Exception) {
            Log.w(TAG, "signOrNull failed: ${e.message}")
            null
        }
    }

    /**
     * Verifies an ECDSA signature against the provided public key and original data.
     *
     * @param publicKey Public key of the claiming signer.
     * @param data Original data bytes that were signed.
     * @param signature DER-encoded ECDSA signature to verify.
     * @return true if the signature is authentic and matches the data and public key; false otherwise.
     */
    fun verify(publicKey: PublicKey, data: ByteArray, signature: ByteArray): Boolean {
        return try {
            val verifier = Signature.getInstance(SIGNATURE_ALGORITHM)
            verifier.initVerify(publicKey)
            verifier.update(data)
            verifier.verify(signature)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Signature algorithm not supported: $SIGNATURE_ALGORITHM", e)
            false
        } catch (e: InvalidKeyException) {
            Log.w(TAG, "Invalid public key for verification", e)
            false
        } catch (e: SignatureException) {
            Log.w(TAG, "Signature verification rejected (mismatched, tampered, or malformed signature)", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during signature verification: ${e.message}")
            false
        }
    }

    /**
     * Inspects whether the identity private key resides inside secure hardware (TEE or StrongBox).
     *
     * Note: Hardware protection is opportunistic and depends on device hardware capabilities.
     * Returns null if inspection is unavailable or fails.
     */
    fun isInsideSecureHardware(): Boolean? {
        return try {
            val privateKey = getOrCreateIdentityKeyPair().private
            val factory = KeyFactory.getInstance(privateKey.algorithm, ANDROID_KEYSTORE)
            val keyInfo = factory.getKeySpec(privateKey, KeyInfo::class.java)
            keyInfo.isInsideSecureHardware
        } catch (e: Exception) {
            Log.d(TAG, "Could not determine hardware backing via KeyInfo: ${e.message}")
            null
        }
    }

    /**
     * Deletes the identity key from Android Keystore if present.
     * Primarily useful for testing key recreation and rotation scenarios.
     */
    @Synchronized
    fun deleteIdentityKey(): Boolean {
        return try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(IDENTITY_KEY_ALIAS)) {
                ks.deleteEntry(IDENTITY_KEY_ALIAS)
                Log.i(TAG, "Identity key '$IDENTITY_KEY_ALIAS' successfully deleted from AndroidKeyStore")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete identity key alias '$IDENTITY_KEY_ALIAS': ${e.message}")
            false
        }
    }
}
