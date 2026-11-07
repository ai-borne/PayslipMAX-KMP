package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, section 4): on iOS the key derivation takes the password as UTF-8 bytes. Kotlin/Native's binding for
 * CommonCrypto would turn the password into an immutable `String`, so [pbkdf2Sha256] calls the C function with the bytes themselves. The
 * bytes must be zeroed once the derivation returns (or fails), must be exactly what a `String` would have given for real text, and a
 * failed derivation must throw rather than return a key of zeros.
 */
class IosSecretKdfTest {
    @Test
    fun theUtf8BytesGivenToTheDerivationAreZeroAfterItReturnsAndAfterItFails() {
        var seen = ByteArray(0)
        val key =
            CryptoHelper.pbkdf2With(Secret.of("Zq7!secretWörd"), ByteArray(16), 10) { bytes, salt, iterations ->
                seen = bytes
                assertTrue(bytes.any { it != 0.toByte() }, "the derivation is given the password")
                pbkdf2Sha256(bytes, salt, iterations)
            }
        assertEquals(32, key.size)
        assertTrue(seen.isNotEmpty() && seen.all { it == 0.toByte() }, "the bytes are wiped as soon as the derivation returns")

        var seenOnFailure = ByteArray(0)
        assertFailsWith<IllegalStateException> {
            CryptoHelper.pbkdf2With(Secret.of("pw"), ByteArray(16), 10) { bytes, _, _ ->
                seenOnFailure = bytes
                error("derivation failed")
            }
        }
        assertTrue(seenOnFailure.all { it == 0.toByte() }, "and when it fails")
    }

    @Test
    fun aFailedDerivationThrowsInsteadOfReturningAKeyOfZeros() {
        // CommonCrypto refuses zero rounds with a parameter error.
        assertFailsWith<IllegalStateException> { pbkdf2Sha256("pw".encodeToByteArray(), ByteArray(16), 0) }
    }

    @Test
    fun theBytesAreWhatAStringGaveBeforeIncludingALoneSurrogate() {
        // The previous code passed password.encodeToByteArray(); a backup made with it must still open.
        for (text in listOf("", "plain", "pässwörd", "पासवर्ड", "p@sswörd🔑🇮🇳", "a\uD800b", "a\uDC00b")) {
            assertContentEquals(text.encodeToByteArray(), Secret.of(text).withUtf8 { it.copyOf() }, text)
        }
    }

    @Test
    fun anEmptyPasswordAndAnEmptySaltStillDeriveTheStandardKey() {
        val expected = "5ddf839afa2d5fb4be56e1a0f48917617559bef61ec122bfca1c7f75ac8f401d"

        assertEquals(expected, CryptoHelper.pbkdf2(Secret.of(""), "salt".encodeToByteArray(), 3).joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') })
    }
}
