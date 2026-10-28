package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a wrong password and a wrong file used to surface each platform's own exception, with the library's own text, and the screen
 * printed that text. Screens can only decide safely by type, so every "these bytes do not open" case must be the same type on Android and
 * on iOS (this common test runs on both). Synthetic bytes only.
 */
class DecryptFailureTypeTest {
    private val password = "RightPassword#1"
    private val sealed = CryptoHelper.encrypt("synthetic payload".encodeToByteArray(), password).getOrThrow()

    private fun assertNotOpened(
        result: Result<ByteArray>,
        what: String,
    ) {
        val error = result.exceptionOrNull()
        assertTrue(error is DecryptionFailedException, "$what must be a DecryptionFailedException but was $error")
        assertNull(error.cause, "$what must not keep the library's exception")
        assertEquals("The data could not be decrypted with this password.", error.message)
    }

    @Test
    fun aWrongPasswordIsTheTypedFailure() = assertNotOpened(CryptoHelper.decrypt(sealed, "WrongPassword#2"), "a wrong password")

    @Test
    fun anAlteredTagIsTheTypedFailure() {
        val altered = sealed.copyOf().also { it[it.lastIndex] = (it[it.lastIndex].toInt() xor 1).toByte() }
        assertNotOpened(CryptoHelper.decrypt(altered, password), "an altered archive")
    }

    @Test
    fun aFileTooShortToHoldTheHeaderIsTheTypedFailure() {
        assertNotOpened(CryptoHelper.decrypt(ByteArray(10) { it.toByte() }, password), "10 bytes")
        assertNotOpened(CryptoHelper.decrypt(ByteArray(0), password), "an empty file")
    }

    @Test
    fun aFileOfAnotherKindIsTheTypedFailure() {
        val notABackup = ByteArray(4096) { (it * 31 + 7).toByte() }
        assertNotOpened(CryptoHelper.decrypt(notABackup, password), "4 KB of other data")
    }

    @Test
    fun aGoodArchiveStillOpensAndGivesBackItsBytes() {
        assertContentEquals("synthetic payload".encodeToByteArray(), CryptoHelper.decrypt(sealed, password).getOrThrow())
    }

    @Test
    fun theTypeKeepsNoCauseWhateverItIsBuiltFrom() {
        assertNull(DecryptionFailedException().cause)
    }
}
