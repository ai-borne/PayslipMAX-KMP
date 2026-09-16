package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CryptoHelperVectorTest {
    @Test
    fun testPbkdf2HmacSha256StandardVector() {
        val password = "password"
        val salt = "salt".encodeToByteArray()
        val derivedKey = CryptoHelper.pbkdf2(password = password, salt = salt, iterations = 1)
        val hexOutput = derivedKey.toHexString()
        val expectedRfc6070 = "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b"
        assertEquals(expectedRfc6070, hexOutput, "PBKDF2 HMAC-SHA256 must match RFC 6070 vector")

        val derivedKey2 = CryptoHelper.pbkdf2(password = password, salt = salt, iterations = 2)
        val expectedHmacSha256Iter2 = "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43"
        assertEquals(expectedHmacSha256Iter2, derivedKey2.toHexString(), "PBKDF2 iterations=2 must match standard vector")
    }

    @Test
    fun testMultiByteUtf8PasswordParity() {
        // Multi-byte Unicode string: 11 characters, but 26 UTF-8 bytes
        val password = "p@sswörd🔑🇮🇳"
        val payload = "Military Payslip Confidential Data".encodeToByteArray()

        val encryptedResult = CryptoHelper.encrypt(payload, password)
        assertTrue(encryptedResult.isSuccess, "Encryption with multi-byte password must succeed")

        val decryptedResult = CryptoHelper.decrypt(encryptedResult.getOrThrow(), password)
        assertTrue(decryptedResult.isSuccess, "Decryption with multi-byte password must succeed")
        assertEquals(
            "Military Payslip Confidential Data",
            decryptedResult.getOrThrow().decodeToString(),
            "Decrypted data must match original payload for multi-byte Unicode password",
        )
    }

    @Test
    fun testEncryptDecryptRoundTrip() {
        val payload = "Sample Army Officer Payslip: Basic Pay 125000, DA 62500".encodeToByteArray()
        val password = "StrongUniqueBackupPassword!2026"

        val encrypted = CryptoHelper.encrypt(payload, password).getOrThrow()
        assertTrue(encrypted.size > payload.size, "Encrypted payload must contain salt, IV, and tag overhead")

        val decrypted = CryptoHelper.decrypt(encrypted, password).getOrThrow()
        assertEquals(payload.decodeToString(), decrypted.decodeToString(), "Round-trip decrypted text must match original")
    }

    @Test
    fun testDecryptionFailsOnWrongPassword() {
        val payload = "Confidential DSOP Fund Statement".encodeToByteArray()
        val encrypted = CryptoHelper.encrypt(payload, "RightPassword#123").getOrThrow()

        val wrongResult = CryptoHelper.decrypt(encrypted, "WrongPassword#123")
        assertTrue(wrongResult.isFailure, "Decryption must fail when incorrect password is provided")
    }

    @Test
    fun testDecryptionFailsOnTamperedData() {
        val payload = "Tax Calculation Summary 2025-2026".encodeToByteArray()
        val encrypted = CryptoHelper.encrypt(payload, "ValidPassword@999").getOrThrow()

        // Flip a bit in ciphertext / tag (last byte)
        val tampered = encrypted.copyOf()
        tampered[tampered.lastIndex] = (tampered[tampered.lastIndex].toInt() xor 0x01).toByte()

        val result = CryptoHelper.decrypt(tampered, "ValidPassword@999")
        assertTrue(result.isFailure, "AEAD integrity check must reject tampered data")
    }

    @Test
    fun testDecryptionFailsOnTruncatedData() {
        val truncatedPayload = ByteArray(10) { it.toByte() }
        val result = CryptoHelper.decrypt(truncatedPayload, "AnyPassword")
        assertTrue(result.isFailure, "Payload smaller than required salt+iv+tag header must fail cleanly")
    }

    @Test
    fun testZeroBytePayload() {
        val emptyPayload = ByteArray(0)
        val password = "ZeroByteSafePassword"

        val encrypted = CryptoHelper.encrypt(emptyPayload, password).getOrThrow()
        val decrypted = CryptoHelper.decrypt(encrypted, password).getOrThrow()
        assertEquals(0, decrypted.size, "Decrypted payload for zero-byte input must be empty ByteArray")
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
