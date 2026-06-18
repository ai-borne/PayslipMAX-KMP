package com.ssbmax.pdfparser.crypto

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

    /**
     * Retrieves or generates a secure, device-persistent database encryption key.
     */
    fun getDatabaseSecretKey(): String

    /**
     * Retrieves the current system time in milliseconds.
     */
    fun getCurrentTimeMillis(): Long
}
