package com.example.data.cryptography

import android.util.Base64
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptographyManager {

    private const val RSA_ALGORITHM = "RSA"
    private const val RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    private const val AES_ALGORITHM = "AES"
    private const val AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"

    // Hardcoded Developer KeyPair for simulation of "Only the Developer has the Master Key to inspect"
    // This represents the developer's private key stored securely in their developer console/backend.
    // In a production cloud system, database escrow entries are unlocked by the developer using this key.
    val DEVELOPER_PUBLIC_KEY_B64: String
    val DEVELOPER_PRIVATE_KEY_B64: String

    init {
        // Generate a standard pair used as the default fallback developer escrow key for this deployment run.
        val kp = generateRSAKeyPair()
        DEVELOPER_PUBLIC_KEY_B64 = encodeKeyToBase64(kp.public)
        DEVELOPER_PRIVATE_KEY_B64 = encodeKeyToBase64(kp.private)
    }

    // --- Key Generation Helpers ---

    fun generateRSAKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM)
        kpg.initialize(2048)
        return kpg.generateKeyPair()
    }

    fun generateAESKey(): String {
        val kg = KeyGenerator.getInstance(AES_ALGORITHM)
        kg.init(256)
        val secretKey = kg.generateKey()
        return Base64.encodeToString(secretKey.encoded, Base64.NO_WRAP)
    }

    // --- Hash Helper for Calls ---
    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // --- Base64 Conversion Helpers ---

    fun encodeKeyToBase64(key: java.security.Key): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    fun getRSAPublicKeyFromBase64(base64Str: String): PublicKey {
        val keyBytes = Base64.decode(base64Str, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance(RSA_ALGORITHM)
        return kf.generatePublic(spec)
    }

    fun getRSAPrivateKeyFromBase64(base64Str: String): PrivateKey {
        val keyBytes = Base64.decode(base64Str, Base64.NO_WRAP)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance(RSA_ALGORITHM)
        return kf.generatePrivate(spec)
    }

    // --- AES Symmetric Encryption ---

    data class AESEncryptedData(val ciphertext: String, val iv: String)

    fun encryptAES(plaintext: String, aesKeyBase64: String): AESEncryptedData {
        val keyBytes = Base64.decode(aesKeyBase64, Base64.NO_WRAP)
        val secretKey = SecretKeySpec(keyBytes, AES_ALGORITHM)
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ivBytes = cipher.iv

        return AESEncryptedData(
            ciphertext = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            iv = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
        )
    }

    fun decryptAES(ciphertextBase64: String, ivBase64: String, aesKeyBase64: String): String {
        return try {
            val keyBytes = Base64.decode(aesKeyBase64, Base64.NO_WRAP)
            val ivBytes = Base64.decode(ivBase64, Base64.NO_WRAP)
            val cipherTextBytes = Base64.decode(ciphertextBase64, Base64.NO_WRAP)

            val secretKey = SecretKeySpec(keyBytes, AES_ALGORITHM)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(ivBytes))

            val decryptedBytes = cipher.doFinal(cipherTextBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "[Decryption Error: ${e.localizedMessage}]"
        }
    }

    // --- RSA Asymmetric Envelope Encryption (Escrowing the AES keys) ---

    fun encryptKeyWithRSA(aesKeyBase64: String, rsaPublicKeyBase64: String): String {
        val publicKey = getRSAPublicKeyFromBase64(rsaPublicKeyBase64)
        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        
        val encryptedBytes = cipher.doFinal(aesKeyBase64.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    fun decryptKeyWithRSA(encryptedAesKeyBase64: String, rsaPrivateKeyBase64: String): String {
        return try {
            val privateKey = getRSAPrivateKeyFromBase64(rsaPrivateKeyBase64)
            val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, privateKey)
            
            val encryptedBytes = Base64.decode(encryptedAesKeyBase64, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "Error decrypting secret session key: ${e.localizedMessage}"
        }
    }
}
