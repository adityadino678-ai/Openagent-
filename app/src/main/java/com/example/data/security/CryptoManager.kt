package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed AES-256-GCM encryption manager using Android KeyStore with JVM test fallback.
 * Ensures zero plain-text storage of API keys, custom automation scripts, and personal configurations.
 */
class CryptoManager(context: Context) {

    private var isFallbackMode = false
    private var fallbackKey: SecretKey? = null

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
            load(null)
        }
    } catch (e: Exception) {
        null
    }

    private val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        if (keyStore != null) {
            try {
                if (!keyStore.containsAlias(KEY_ALIAS)) {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        KEYSTORE_PROVIDER
                    )
                    val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()

                    keyGenerator.init(keyGenParameterSpec)
                    keyGenerator.generateKey()
                }
                return
            } catch (e: Exception) {
                // KeyStore provider not available (e.g. JVM host in tests)
            }
        }

        // JVM / Testing fallback: standard 256-bit AES key
        isFallbackMode = true
        if (fallbackKey == null) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            fallbackKey = keyGen.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        if (isFallbackMode || keyStore == null) {
            return fallbackKey ?: run {
                val keyGen = KeyGenerator.getInstance("AES")
                keyGen.init(256)
                keyGen.generateKey().also { fallbackKey = it }
            }
        }
        val entry = try {
            keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        } catch (e: Exception) {
            null
        }
        return entry?.secretKey ?: run {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            keyGen.generateKey().also { fallbackKey = it }
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM with randomized 12-byte IV.
     * Output format: Base64( IV[12 bytes] + Ciphertext + Tag[16 bytes] )
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts AES-256-GCM ciphertext.
     */
    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH + 1) return ""

            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)

            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val decryptedBytes = cipher.doFinal(cipherText)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            return ""
        }
    }

    fun saveEncryptedString(key: String, value: String) {
        val encrypted = encrypt(value)
        sharedPreferences.edit().putString(key, encrypted).apply()
    }

    fun getEncryptedString(key: String, defaultValue: String = ""): String {
        val encrypted = sharedPreferences.getString(key, null) ?: return defaultValue
        val decrypted = decrypt(encrypted)
        return if (decrypted.isNotEmpty()) decrypted else defaultValue
    }

    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "OpenAgentMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        private const val PREFS_NAME = "open_agent_secure_vault"
    }
}
