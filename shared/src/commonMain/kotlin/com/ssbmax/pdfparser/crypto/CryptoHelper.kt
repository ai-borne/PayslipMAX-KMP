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
}
