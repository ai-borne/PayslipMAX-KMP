package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CryptoHelperVectorTest {
    @Test
    fun testPbkdf2HmacSha256StandardVector() {
        val password = "password"
        val salt = "salt".encodeToByteArray()
        val derivedKey = CryptoHelper.pbkdf2(password = Secret.of(password), salt = salt, iterations = 1)
        val hexOutput = derivedKey.toHexString()
        val expectedRfc6070 = "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b"
        assertEquals(expectedRfc6070, hexOutput, "PBKDF2 HMAC-SHA256 must match RFC 6070 vector")

        val derivedKey2 = CryptoHelper.pbkdf2(password = Secret.of(password), salt = salt, iterations = 2)
        val expectedHmacSha256Iter2 = "ae4d0c95af6b46d32d0adff928f06dd02a303f8ef3c251dfd6e2d85a95474c43"
        assertEquals(expectedHmacSha256Iter2, derivedKey2.toHexString(), "PBKDF2 iterations=2 must match standard vector")
    }

    @Test
    fun multiByteAndEmptyPasswordsGiveTheStandardKeyOfTheirUtf8Bytes() {
        // Computed with Python's hashlib.pbkdf2_hmac('sha256', password.encode('utf-8'), b'salt', 3): the key every platform must derive,
        // so a password typed on one phone opens a backup made on the other (docs/Plan/14a P10).
        val expected =
            mapOf(
                "p@sswörd🔑🇮🇳" to "6c3b8cfa1e1db0d7a171fe48fbda36ac4c6855610ca55c5200391e739ecbae5e",
                "" to "5ddf839afa2d5fb4be56e1a0f48917617559bef61ec122bfca1c7f75ac8f401d",
                "pässwörd" to "bcb7fbf967215fc8097eb5451077a9255d623fb7e0480a7ae2e1a0a7a8972897",
            )
        for ((password, hex) in expected) {
            assertEquals(hex, CryptoHelper.pbkdf2(Secret.of(password), "salt".encodeToByteArray(), 3).toHexString(), "'$password'")
        }
    }

    @Test
    fun testMultiByteUtf8PasswordParity() {
        // Multi-byte Unicode string: 11 characters, but 26 UTF-8 bytes
        val password = "p@sswörd🔑🇮🇳"
        val payload = "Military Payslip Confidential Data".encodeToByteArray()

        val encryptedResult = CryptoHelper.encrypt(payload, Secret.of(password))
        assertTrue(encryptedResult.isSuccess, "Encryption with multi-byte password must succeed")

        val decryptedResult = CryptoHelper.decrypt(encryptedResult.getOrThrow(), Secret.of(password))
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

        val encrypted = CryptoHelper.encrypt(payload, Secret.of(password)).getOrThrow()
        assertTrue(encrypted.size > payload.size, "Encrypted payload must contain salt, IV, and tag overhead")

        val decrypted = CryptoHelper.decrypt(encrypted, Secret.of(password)).getOrThrow()
        assertEquals(payload.decodeToString(), decrypted.decodeToString(), "Round-trip decrypted text must match original")
    }

    @Test
    fun testDecryptionFailsOnWrongPassword() {
        val payload = "Confidential DSOP Fund Statement".encodeToByteArray()
        val encrypted = CryptoHelper.encrypt(payload, Secret.of("RightPassword#123")).getOrThrow()

        val wrongResult = CryptoHelper.decrypt(encrypted, Secret.of("WrongPassword#123"))
        assertTrue(wrongResult.isFailure, "Decryption must fail when incorrect password is provided")
    }

    @Test
    fun testDecryptionFailsOnTamperedData() {
        val payload = "Tax Calculation Summary 2025-2026".encodeToByteArray()
        val encrypted = CryptoHelper.encrypt(payload, Secret.of("ValidPassword@999")).getOrThrow()

        // Flip a bit in ciphertext / tag (last byte)
        val tampered = encrypted.copyOf()
        tampered[tampered.lastIndex] = (tampered[tampered.lastIndex].toInt() xor 0x01).toByte()

        val result = CryptoHelper.decrypt(tampered, Secret.of("ValidPassword@999"))
        assertTrue(result.isFailure, "AEAD integrity check must reject tampered data")
    }

    @Test
    fun testDecryptionFailsOnTruncatedData() {
        val truncatedPayload = ByteArray(10) { it.toByte() }
        val result = CryptoHelper.decrypt(truncatedPayload, Secret.of("AnyPassword"))
        assertTrue(result.isFailure, "Payload smaller than required salt+iv+tag header must fail cleanly")
    }

    @Test
    fun testZeroBytePayload() {
        val emptyPayload = ByteArray(0)
        val password = "ZeroByteSafePassword"

        val encrypted = CryptoHelper.encrypt(emptyPayload, Secret.of(password)).getOrThrow()
        val decrypted = CryptoHelper.decrypt(encrypted, Secret.of(password)).getOrThrow()
        assertEquals(0, decrypted.size, "Decrypted payload for zero-byte input must be empty ByteArray")
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
