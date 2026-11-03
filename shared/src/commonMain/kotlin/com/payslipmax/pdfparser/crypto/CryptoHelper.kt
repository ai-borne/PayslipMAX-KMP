package com.payslipmax.pdfparser.crypto

expect object CryptoHelper {
    /**
     * Encrypts plain bytes using AES-256 symmetric encryption.
     * The encryption key is derived from the user-provided password.
     * @param data The raw bytes to encrypt.
     * @param password The user password/passphrase.
     * @return A Result containing the encrypted byte array.
     */
    fun encrypt(
        data: ByteArray,
        password: String,
    ): Result<ByteArray>

    /**
     * Decrypts AES-256 encrypted bytes.
     * @param encryptedData The encrypted byte array.
     * @param password The user password/passphrase.
     * @return A Result containing the decrypted byte array.
     */
    fun decrypt(
        encryptedData: ByteArray,
        password: String,
    ): Result<ByteArray>

    /**
     * Hashes string input using SHA-256 and returns the hex-encoded string.
     */
    fun sha256(input: String): String

    /**
     * Stretches a user-provided password using PBKDF2-HMAC-SHA256.
     */
    fun pbkdf2(
        password: String,
        salt: ByteArray,
        iterations: Int = 10000,
    ): ByteArray

    /** [size] bytes from the platform's secure random generator. */
    fun randomBytes(size: Int): ByteArray

    /**
     * AES-256-GCM under a raw 32-byte [key], with no key derivation: IV (12) | ciphertext | tag (16), the plaintext starting with the
     * "PCDA" mark, and [aad] authenticated but not stored. A fresh random IV on every call. One key seals a whole backup (`ArchiveKey`).
     */
    fun sealWithKey(
        data: ByteArray,
        key: ByteArray,
        aad: ByteArray,
    ): Result<ByteArray>

    /** What [sealWithKey] sealed, or [DecryptionFailedException] when the key, the [aad] or any byte differs. */
    fun openWithKey(
        sealed: ByteArray,
        key: ByteArray,
        aad: ByteArray,
    ): Result<ByteArray>

    /**
     * Retrieves or generates a secure, device-persistent database encryption key.
     */
    fun getDatabaseSecretKey(): String

    /**
     * Retrieves the current system time in milliseconds.
     */
    fun getCurrentTimeMillis(): Long
}

/**
 * Retrieves and de-obfuscates the legacy fallback decryption key.
 */
fun CryptoHelper.getLegacyFallbackKey(): String {
    val xored = byteArrayOf(10, 25, 30, 27, 10, 59, 35, 41, 54, 51, 42, 21, 60, 60, 54, 51, 52, 63, 9, 63, 57, 40, 63, 46, 104, 106, 104, 108, 123)
    val key = 90
    return ByteArray(xored.size) { i -> (xored[i].toInt() xor key).toByte() }.decodeToString()
}
