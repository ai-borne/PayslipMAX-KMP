package com.payslipmax.pdfparser.crypto

import platform.CoreCrypto.kCCDecodeError
import platform.CoreCrypto.kCCParamError
import platform.CoreCrypto.kCCSuccess
import platform.CoreCrypto.kCCUnspecifiedError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

/**
 * WHY: iOS does not fail like Android. A wrong password is a CommonCrypto status, not an exception with text, so the mapping from status to
 * the typed failure is the iOS half of the restore message. Only "the data did not decode" is the officer's password or file; a status that
 * says the call itself was wrong, or that the symbol is missing (-1), must stay a generic failure, never "wrong password".
 */
class IosDecryptFailureTest {
    @Test
    fun bothStatusesCommonCryptoUsesForBadDataAreTheTypedFailure() {
        // -4308 is what the one-shot GCM returns for a tag that does not match (seen on the simulator); -4304 is the documented decode error.
        assertEquals(-4308, kCCUnspecifiedError, "the status this test names is the one the simulator returned")
        for (status in listOf(kCCUnspecifiedError, kCCDecodeError)) {
            val failure = decryptStatusFailure(status)
            assertTrue(failure is DecryptionFailedException, "status $status was $failure")
            assertNull(failure.cause)
        }
    }

    @Test
    fun aStatusThatIsNotAboutTheDataIsNotRetyped() {
        assertNull(decryptStatusFailure(kCCParamError))
        assertNull(decryptStatusFailure(-1))
        assertNull(decryptStatusFailure(kCCSuccess))
    }

    @Test
    fun aRealTamperedArchiveOnThisDeviceComesBackTypedAndPromptly() {
        val sealed = CryptoHelper.encrypt("synthetic".encodeToByteArray(), "pw-1").getOrThrow()
        val tampered = sealed.copyOf().also { it[it.lastIndex] = (it[it.lastIndex].toInt() xor 1).toByte() }

        val timed = measureTimedValue { CryptoHelper.decrypt(tampered, "pw-1") }

        assertTrue(timed.value.exceptionOrNull() is DecryptionFailedException, "was ${timed.value.exceptionOrNull()}")
        assertTrue(timed.duration < 1.seconds, "a refusal takes ${timed.duration}")
    }
}
