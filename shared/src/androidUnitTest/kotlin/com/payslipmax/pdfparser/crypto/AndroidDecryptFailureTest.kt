package com.payslipmax.pdfparser.crypto

import java.security.NoSuchAlgorithmException
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * WHY: on a phone the GCM tag check fails with the platform's own text ("error:1e000065:Cipher functions:OPENSSL_internal:BAD_DECRYPT"),
 * which the restore screen once printed. The JVM's own cipher words that differently, so the phone's exact exception is built here and must
 * become the typed failure with its text gone. Only a data failure is retyped: a missing cipher is not the officer's password.
 */
class AndroidDecryptFailureTest {
    private val phoneText = "error:1e000065:Cipher functions:OPENSSL_internal:BAD_DECRYPT"

    @Test
    fun theTagFailureAPhoneRaisesBecomesTheTypedFailureWithoutItsText() {
        val failure = decryptFailure(AEADBadTagException(phoneText))

        assertTrue(failure is DecryptionFailedException, "was $failure")
        assertNull(failure.cause)
        assertTrue("BAD_DECRYPT" !in failure.message.orEmpty() && "error:" !in failure.message.orEmpty())
    }

    @Test
    fun anyBadPaddingFailureIsRetypedToo() {
        assertTrue(decryptFailure(BadPaddingException("padding")) is DecryptionFailedException)
    }

    @Test
    fun aFailureThatIsNotAboutTheDataIsReturnedUnchanged() {
        val missing = NoSuchAlgorithmException("no GCM here")
        assertSame(missing, decryptFailure(missing))
    }

    /** Sealed in this app's own layout (salt, IV, ciphertext and tag) but by something that did not put the "PCDA" mark first. */
    private fun sealed(plain: ByteArray): ByteArray {
        val salt = ByteArray(16) { it.toByte() }
        val iv = ByteArray(12) { (it + 40).toByte() }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(CryptoHelper.pbkdf2("pw-1", salt), "AES"), GCMParameterSpec(128, iv))
        return salt + iv + cipher.doFinal(plain)
    }

    @Test
    fun dataThatAuthenticatesButWasNotSealedByThisAppIsTheTypedFailure() {
        assertTrue(CryptoHelper.decrypt(sealed("PCDA-and-more".encodeToByteArray()), "pw-1").isSuccess, "control: the recipe is this app's layout")

        val error = CryptoHelper.decrypt(sealed("no mark first".encodeToByteArray()), "pw-1").exceptionOrNull()

        assertTrue(error is DecryptionFailedException, "was $error")
    }
}
