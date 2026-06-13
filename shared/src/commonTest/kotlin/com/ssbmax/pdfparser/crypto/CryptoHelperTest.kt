package com.ssbmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CryptoHelperTest {
    @Test
    fun testSha256Hashing() {
        val input = "1234"
        val hash = CryptoHelper.sha256(input)

        // Assert hash is 64 characters (hex representation of 32 bytes)
        assertEquals(64, hash.length)

        // Assert hash is consistent for same input
        assertEquals(hash, CryptoHelper.sha256(input))

        // Assert different inputs produce different hashes
        assertNotEquals(hash, CryptoHelper.sha256("1235"))
    }

    @Test
    fun testPbkdf2KeyStretching() {
        val password = "StrongPassword123!"
        val salt = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)

        val key1 = CryptoHelper.pbkdf2(password, salt)
        val key2 = CryptoHelper.pbkdf2(password, salt)

        assertEquals(32, key1.size, "PBKDF2 key size should be 32 bytes (256 bits)")
        assertTrue(key1.contentEquals(key2), "PBKDF2 key should be consistent for same inputs")

        val keyDiffPass = CryptoHelper.pbkdf2("DifferentPass!", salt)
        assertTrue(!key1.contentEquals(keyDiffPass), "Different passwords should yield different keys")

        val keyDiffSalt = CryptoHelper.pbkdf2(password, byteArrayOf(8, 7, 6, 5, 4, 3, 2, 1))
        assertTrue(!key1.contentEquals(keyDiffSalt), "Different salts should yield different keys")
    }

    @Test
    fun testGetDatabaseSecretKey() {
        val key1 = CryptoHelper.getDatabaseSecretKey()
        val key2 = CryptoHelper.getDatabaseSecretKey()

        assertNotNull(key1)
        assertTrue(key1.isNotEmpty(), "Database key should not be empty")
        assertEquals(key1, key2, "Database key should be persistent across calls")
    }

    @Test
    fun testEncryptDecryptCycle() {
        val originalText = "Hello offline world of military finance!"
        val originalBytes = originalText.encodeToByteArray()
        val password = "SuperSecretPassword123"

        val encryptResult = CryptoHelper.encrypt(originalBytes, password)
        assertTrue(encryptResult.isSuccess, "Encryption should be successful")
        val cipherBytes = encryptResult.getOrThrow()

        val decryptResult = CryptoHelper.decrypt(cipherBytes, password)
        assertTrue(decryptResult.isSuccess, "Decryption should be successful")
        val decryptedBytes = decryptResult.getOrThrow()

        assertEquals(originalText, decryptedBytes.decodeToString(), "Decrypted content should match original")
    }

    @Test
    fun testDecryptWithWrongPasswordFails() {
        val originalBytes = "Top Secret Document".encodeToByteArray()
        val password = "CorrectPassword"

        val cipherBytes = CryptoHelper.encrypt(originalBytes, password).getOrThrow()

        val wrongDecryptResult = CryptoHelper.decrypt(cipherBytes, "WrongPassword")
        assertTrue(wrongDecryptResult.isFailure, "Decryption with wrong password must fail")
    }

    @Test
    fun testEncryptionIsRandomized() {
        val originalBytes = "Repeatable text".encodeToByteArray()
        val password = "SamePassword"

        val cipher1 = CryptoHelper.encrypt(originalBytes, password).getOrThrow()
        val cipher2 = CryptoHelper.encrypt(originalBytes, password).getOrThrow()

        assertFalse(cipher1.contentEquals(cipher2), "Encrypted outputs should differ due to randomized salt and IV")
    }
}
