package com.example.graymatter.android.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides AES-256-GCM encryption and decryption for backup files.
 *
 * File format:
 * [4 bytes: magic "GMBK"]
 * [4 bytes: version (1)]
 * [16 bytes: PBKDF2 salt]
 * [12 bytes: GCM IV/nonce]
 * [N bytes: AES-GCM encrypted payload + 16-byte auth tag]
 */
object BackupCrypto {

    private const val MAGIC = "GMBK"
    private const val VERSION = 1
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH = 256
    private const val PBKDF2_ITERATIONS = 100_000
    private const val GCM_TAG_LENGTH = 128 // bits
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"

    /**
     * Derives a 256-bit AES key from a password and salt using PBKDF2.
     */
    fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts an input stream to an output stream using AES-256-GCM.
     * Writes the magic header, salt, and IV before the encrypted payload.
     */
    fun encrypt(inputStream: InputStream, outputStream: OutputStream, password: String) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        // Write header
        outputStream.write(MAGIC.toByteArray(Charsets.US_ASCII))
        outputStream.write(intToBytes(VERSION))
        outputStream.write(salt)
        outputStream.write(iv)

        // Stream-encrypt the payload
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val encrypted = cipher.update(buffer, 0, bytesRead)
            if (encrypted != null) outputStream.write(encrypted)
        }
        // Write final block + GCM auth tag
        val finalBlock = cipher.doFinal()
        outputStream.write(finalBlock)
        outputStream.flush()
    }

    /**
     * Decrypts an input stream to an output stream using AES-256-GCM.
     * Reads the magic header, salt, and IV from the stream.
     *
     * @throws SecurityException if magic bytes don't match or decryption fails.
     */
    fun decrypt(inputStream: InputStream, outputStream: OutputStream, password: String) {
        // Read and validate magic
        val magicBytes = ByteArray(4)
        inputStream.readExactly(magicBytes)
        val magic = String(magicBytes, Charsets.US_ASCII)
        if (magic != MAGIC) {
            throw SecurityException("Invalid backup file: bad magic header '$magic'")
        }

        // Read version
        val versionBytes = ByteArray(4)
        inputStream.readExactly(versionBytes)
        val version = bytesToInt(versionBytes)
        if (version != VERSION) {
            throw SecurityException("Unsupported backup version: $version")
        }

        // Read salt and IV
        val salt = ByteArray(SALT_LENGTH)
        inputStream.readExactly(salt)
        val iv = ByteArray(IV_LENGTH)
        inputStream.readExactly(iv)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        // GCM requires all ciphertext before doFinal for auth tag verification,
        // so we read all remaining bytes, then decrypt.
        val ciphertext = inputStream.readBytes()
        val plaintext = cipher.doFinal(ciphertext)
        outputStream.write(plaintext)
        outputStream.flush()
    }

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        (value shr 24).toByte(),
        (value shr 16).toByte(),
        (value shr 8).toByte(),
        value.toByte()
    )

    private fun bytesToInt(bytes: ByteArray): Int =
        (bytes[0].toInt() and 0xFF shl 24) or
        (bytes[1].toInt() and 0xFF shl 16) or
        (bytes[2].toInt() and 0xFF shl 8) or
        (bytes[3].toInt() and 0xFF)

    private fun InputStream.readExactly(buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val bytesRead = read(buffer, offset, buffer.size - offset)
            if (bytesRead == -1) throw SecurityException("Unexpected end of backup file")
            offset += bytesRead
        }
    }
}
