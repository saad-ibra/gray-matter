package com.example.graymatter.android.backup

import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Provides AES-256-GCM encryption and decryption for backup files.
 *
 * File format v1 (legacy):
 * [4 bytes: magic "GMBK"]
 * [4 bytes: version (1)]
 * [16 bytes: PBKDF2 salt]
 * [12 bytes: GCM IV/nonce]
 * [N bytes: AES-GCM encrypted payload + 16-byte auth tag]
 *
 * File format v2 (current):
 * [4 bytes: magic "GMBK"]
 * [4 bytes: version (2)]
 * [16 bytes: PBKDF2 salt]
 * [12 bytes: GCM IV/nonce]
 * [4 bytes: PBKDF2 iteration count]
 * [N bytes: AES-GCM encrypted payload + 16-byte auth tag]
 */
object BackupCrypto {

    private const val MAGIC = "GMBK"
    private const val VERSION = 2
    private const val LEGACY_VERSION = 1
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH = 256
    private const val PBKDF2_ITERATIONS = 600_000
    private const val LEGACY_PBKDF2_ITERATIONS = 100_000
    private const val GCM_TAG_LENGTH = 128 // bits
    private const val GCM_TAG_BYTES = GCM_TAG_LENGTH / 8 // 16 bytes
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val BUFFER_SIZE = 8192

    /**
     * Derives a 256-bit AES key from a password and salt using PBKDF2.
     *
     * @param password Password as a mutable CharArray. The caller MUST zero this after use.
     * @param salt Random salt bytes.
     * @param iterations PBKDF2 iteration count.
     */
    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int = PBKDF2_ITERATIONS): SecretKey {
        val spec = PBEKeySpec(password, salt, iterations, KEY_LENGTH)
        return try {
            val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
            val keyBytes = factory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword() // Zero the PBEKeySpec's internal password copy
        }
    }

    /**
     * Encrypts an input stream to an output stream using AES-256-GCM.
     * Writes the v2 header (magic, version, salt, IV, iterations) before the encrypted payload.
     *
     * @param password Password as CharArray. Caller MUST zero this after the call returns.
     */
    fun encrypt(inputStream: InputStream, outputStream: OutputStream, password: CharArray) {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).also { random.nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { random.nextBytes(it) }

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        // Write v2 header
        outputStream.write(MAGIC.toByteArray(Charsets.US_ASCII))
        outputStream.write(intToBytes(VERSION))
        outputStream.write(salt)
        outputStream.write(iv)
        outputStream.write(intToBytes(PBKDF2_ITERATIONS))

        // Stream-encrypt the payload
        val buffer = ByteArray(BUFFER_SIZE)
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
     * Supports both v1 (legacy 100k iterations) and v2 (embedded iteration count) formats.
     *
     * Uses streaming decryption to avoid loading the entire ciphertext into memory,
     * preventing OOM errors on large backup files.
     *
     * @param password Password as CharArray. Caller MUST zero this after the call returns.
     * @throws SecurityException if magic bytes don't match, version is unsupported, or decryption fails.
     */
    fun decrypt(inputStream: InputStream, outputStream: OutputStream, password: CharArray) {
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
        if (version != LEGACY_VERSION && version != VERSION) {
            throw SecurityException("Unsupported backup version: $version")
        }

        // Read salt and IV
        val salt = ByteArray(SALT_LENGTH)
        inputStream.readExactly(salt)
        val iv = ByteArray(IV_LENGTH)
        inputStream.readExactly(iv)

        // Determine iteration count based on version
        val iterations = when (version) {
            LEGACY_VERSION -> LEGACY_PBKDF2_ITERATIONS
            VERSION -> {
                val iterBytes = ByteArray(4)
                inputStream.readExactly(iterBytes)
                bytesToInt(iterBytes)
            }
            else -> throw SecurityException("Unsupported backup version: $version")
        }

        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        // Streamed GCM decryption with trailing auth tag buffering.
        //
        // AES-GCM appends a 16-byte authentication tag at the end of the ciphertext.
        // We must pass the final bytes (which include the tag) to cipher.doFinal()
        // rather than cipher.update(). To achieve this without loading everything
        // into memory, we maintain a rolling buffer of the last GCM_TAG_BYTES.
        streamDecrypt(inputStream, outputStream, cipher)
    }

    /**
     * Performs streamed AES-GCM decryption by buffering the trailing auth tag bytes.
     *
     * Strategy: Always keep GCM_TAG_BYTES worth of data in a "tail buffer".
     * Process all preceding bytes through cipher.update(), and pass the final
     * tail buffer to cipher.doFinal() which verifies the auth tag.
     */
    private fun streamDecrypt(
        inputStream: InputStream,
        outputStream: OutputStream,
        cipher: Cipher
    ) {
        val readBuffer = ByteArray(BUFFER_SIZE)
        // Accumulation buffer: we accumulate data and only process when we have
        // more than GCM_TAG_BYTES, keeping the tail reserved for doFinal.
        val accumulator = ByteArrayAccumulator()

        var bytesRead: Int
        while (inputStream.read(readBuffer).also { bytesRead = it } != -1) {
            accumulator.append(readBuffer, 0, bytesRead)

            // Process all bytes except the last GCM_TAG_BYTES
            val processable = accumulator.size - GCM_TAG_BYTES
            if (processable > 0) {
                val toProcess = accumulator.drain(processable)
                val decrypted = cipher.update(toProcess)
                if (decrypted != null) outputStream.write(decrypted)
            }
        }

        // The remaining bytes in the accumulator include the GCM auth tag.
        // Pass everything to doFinal() which will verify the tag.
        val remaining = accumulator.drainAll()
        if (remaining.isEmpty()) {
            throw SecurityException("Unexpected end of encrypted backup file")
        }

        try {
            val finalBlock = cipher.doFinal(remaining)
            if (finalBlock != null) outputStream.write(finalBlock)
        } catch (e: AEADBadTagException) {
            throw SecurityException("Decryption failed: wrong password or corrupted backup file", e)
        }

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

    /**
     * Simple growable byte buffer to accumulate stream reads and drain
     * processed bytes efficiently without excessive array allocation.
     */
    private class ByteArrayAccumulator {
        private var buffer = ByteArray(BUFFER_SIZE * 2)
        var size: Int = 0
            private set

        fun append(data: ByteArray, offset: Int, length: Int) {
            ensureCapacity(size + length)
            System.arraycopy(data, offset, buffer, size, length)
            size += length
        }

        /**
         * Removes and returns the first [count] bytes from the buffer.
         */
        fun drain(count: Int): ByteArray {
            require(count in 1..size)
            val result = buffer.copyOfRange(0, count)
            // Shift remaining bytes to the front
            val remaining = size - count
            if (remaining > 0) {
                System.arraycopy(buffer, count, buffer, 0, remaining)
            }
            size = remaining
            return result
        }

        /**
         * Removes and returns all bytes from the buffer.
         */
        fun drainAll(): ByteArray {
            val result = buffer.copyOfRange(0, size)
            size = 0
            return result
        }

        private fun ensureCapacity(requiredCapacity: Int) {
            if (requiredCapacity > buffer.size) {
                val newSize = maxOf(buffer.size * 2, requiredCapacity)
                buffer = buffer.copyOf(newSize)
            }
        }
    }
}
