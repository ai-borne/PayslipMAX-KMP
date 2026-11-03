package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10 "From P3" item 7, closed in P6): the device database and backups up to format 5 are sealed by the legacy
 * [CryptoHelper.encrypt], whose salt and IV must be secret random bytes. It used to ignore `SecRandomCopyBytes`'s status, so a failed
 * generator would have sealed rows under an all-zero salt and IV. It now takes them from the checked generator and fails closed. The
 * layout (salt | IV | ciphertext | tag) is unchanged, so every row the device already holds still opens and new rows open on older code.
 */
class IosLegacyEncryptRandomTest {
    private val data = "synthetic row".encodeToByteArray()

    @Test
    fun aGeneratorThatFailsFailsTheSealInsteadOfUsingZeros() {
        val result = CryptoHelper.encryptWith(data, "pw-1") { error("No secure random bytes.") }

        assertTrue(result.isFailure, "a failed generator must not produce a sealed row")
    }

    @Test
    fun theSaltAndIvAreTheGeneratorsBytesInTheSameLayoutAsBefore() {
        val asked = mutableListOf<Int>()
        val salt = ByteArray(16) { (0x10 + it).toByte() }
        val iv = ByteArray(12) { (0x40 + it).toByte() }
        val sealed =
            CryptoHelper.encryptWith(data, "pw-1") { size ->
                asked += size
                if (asked.size == 1) salt.copyOf(size) else iv.copyOf(size)
            }.getOrThrow()

        assertEquals(listOf(16, 12), asked, "a 16-byte salt, then a 12-byte IV")
        assertContentEquals(salt, sealed.copyOfRange(0, 16), "the salt leads")
        assertContentEquals(iv, sealed.copyOfRange(16, 28), "the IV follows the salt")
        assertEquals(16 + 12 + 4 + data.size + 16, sealed.size, "salt, IV, the PCDA mark and the data, then the tag")
        assertContentEquals(data, CryptoHelper.decrypt(sealed, "pw-1").getOrThrow(), "the device read opens it as before")
    }

    @Test
    fun theRealGeneratorStillSealsRowsTheDeviceReads() {
        val first = CryptoHelper.encrypt(data, "pw-1").getOrThrow()
        val second = CryptoHelper.encrypt(data, "pw-1").getOrThrow()

        assertTrue(!first.copyOfRange(0, 28).contentEquals(second.copyOfRange(0, 28)), "a fresh salt and IV each time")
        assertContentEquals(data, CryptoHelper.decrypt(first, "pw-1").getOrThrow())
    }
}
