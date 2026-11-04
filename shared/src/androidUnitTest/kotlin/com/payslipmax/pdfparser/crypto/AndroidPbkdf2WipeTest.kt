package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, section 4): Android's `PBEKeySpec` keeps its own copy of the password's characters. The key derivation must wipe
 * that copy before it returns, whatever the outcome, and must leave the caller's [Secret] alone (its owner closes it). The spec is not
 * reachable from outside, so [CryptoHelper.pbkdf2Observed] shows it.
 */
class AndroidPbkdf2WipeTest {
    @Test
    fun theKeySpecsOwnCopyOfThePasswordIsWipedWhenTheDerivationReturns() {
        val chars = "Zq7!secretWord".toCharArray()
        val secret = Secret.adopt(chars)
        var spec: javax.crypto.spec.PBEKeySpec? = null

        val key = CryptoHelper.pbkdf2Observed(secret, ByteArray(16), 10) { spec = it }

        assertEquals(32, key.size)
        assertEquals("Zq7!secretWord", String(chars), "the caller's secret is borrowed, not used up")
        assertFailsWith<IllegalStateException>("the spec's copy was cleared") { spec!!.password }
    }

    @Test
    fun theKeySpecsCopyIsWipedWhenTheDerivationFails() {
        var spec: javax.crypto.spec.PBEKeySpec? = null

        // Zero iterations is refused by the provider, after the spec has been made.
        assertFailsWith<Exception> { CryptoHelper.pbkdf2Observed(Secret.of("pw"), ByteArray(16), 0) { spec = it } }

        assertTrue(spec == null || runCatching { spec!!.password }.isFailure, "a failed derivation still wipes the spec's copy")
    }

    @Test
    fun theDerivedKeyIsTheStandardOneForTheSamePassword() {
        val key = CryptoHelper.pbkdf2Observed(Secret.of("password"), "salt".encodeToByteArray(), 1) {}

        assertContentEquals(
            "120fb6cffcf8b32c43e7225256c4f837a86548c92ccc35480805987cb70be17b".chunked(2).map { it.toInt(16).toByte() }.toByteArray(),
            key,
        )
    }
}
