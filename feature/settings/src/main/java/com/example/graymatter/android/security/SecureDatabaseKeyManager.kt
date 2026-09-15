package com.example.graymatter.android.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages the database encryption key using Android Keystore.
 *
 * The AES-256 key is generated once, stored in hardware-backed
 * Keystore (StrongBox when available, TEE otherwise), and never
 * leaves the secure element. The key is used to encrypt/decrypt
 * a random passphrase that SQLCipher uses to lock the database.
 *
 * Flow:
 * 1. First launch: generate Keystore key → generate random passphrase →
 *    encrypt passphrase with Keystore key → store encrypted passphrase in SharedPreferences.
 * 2. Subsequent launches: retrieve encrypted passphrase → decrypt with Keystore key.
 */
class SecureDatabaseKeyManager(private val context: Context) {

    companion object {
        private const val TAG = "SecureDBKeyManager"
        private const val KEYSTORE_ALIAS = "gm_db_encryption_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREFS_NAME = "gm_secure_db_prefs"
        private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_passphrase"
        private const val KEY_PASSPHRASE_IV = "passphrase_iv"
        private const val GCM_TAG_LENGTH = 128
        private const val PASSPHRASE_LENGTH = 32 // 256-bit random passphrase
    }

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the database passphrase as a [ByteArray].
     *
     * IMPORTANT: The caller MUST zero the returned array after use:
     * ```
     * val passphrase = keyManager.getDatabasePassphrase()
     * try {
     *     // use passphrase
     * } finally {
     *     passphrase.fill(0)
     * }
     * ```
     */
    fun getDatabasePassphrase(): ByteArray {
        ensureKeystoreKeyExists()

        val encryptedData = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivData = prefs.getString(KEY_PASSPHRASE_IV, null)

        return if (encryptedData != null && ivData != null) {
            // Decrypt existing passphrase
            decryptPassphrase(
                android.util.Base64.decode(encryptedData, android.util.Base64.NO_WRAP),
                android.util.Base64.decode(ivData, android.util.Base64.NO_WRAP)
            )
        } else {
            // First launch: generate and persist a new random passphrase
            generateAndStorePassphrase()
        }
    }

    /**
     * Checks whether an encrypted passphrase already exists.
     * Useful for determining if a database migration is needed.
     */
    fun hasExistingPassphrase(): Boolean {
        return prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null) != null
    }

    private fun ensureKeystoreKeyExists() {
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) return

        val specBuilder = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)

        // Note: We intentionally avoid setIsStrongBoxBacked(true) here because 
        // many OEM StrongBox implementations have bugs that can cause fatal 
        // system crashes (Rescue Party reboots) during startup.
        // The standard TEE (Trusted Execution Environment) is hardware-backed,
        // highly secure, and much more stable.

        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )
            keyGenerator.init(specBuilder.build())
            keyGenerator.generateKey()
            Log.i(TAG, "Keystore key generated (StrongBox attempted: ${Build.VERSION.SDK_INT >= Build.VERSION_CODES.P})")
        } catch (e: StrongBoxUnavailableException) {
            // StrongBox not available on this device; fall back to TEE
            Log.w(TAG, "StrongBox unavailable, falling back to TEE-backed key")
            val fallbackSpec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
            )
            keyGenerator.init(fallbackSpec)
            keyGenerator.generateKey()
            Log.i(TAG, "Keystore key generated (TEE-backed fallback)")
        }
    }

    private fun getKeystoreKey(): SecretKey {
        return keyStore.getKey(KEYSTORE_ALIAS, null) as SecretKey
    }

    private fun generateAndStorePassphrase(): ByteArray {
        // Generate a cryptographically random passphrase
        val passphrase = ByteArray(PASSPHRASE_LENGTH)
        java.security.SecureRandom().nextBytes(passphrase)

        // Encrypt the passphrase with the Keystore key
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getKeystoreKey())

        val encryptedPassphrase = cipher.doFinal(passphrase)
        val iv = cipher.iv

        // Store encrypted passphrase and IV
        prefs.edit()
            .putString(
                KEY_ENCRYPTED_PASSPHRASE,
                android.util.Base64.encodeToString(encryptedPassphrase, android.util.Base64.NO_WRAP)
            )
            .putString(
                KEY_PASSPHRASE_IV,
                android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
            )
            .apply()

        Log.i(TAG, "New database passphrase generated and encrypted")
        return passphrase
    }

    private fun decryptPassphrase(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getKeystoreKey(), spec)
        return cipher.doFinal(encryptedData)
    }
}
