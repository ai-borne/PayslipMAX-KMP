package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** WHY: data that authenticates but was not sealed by this app must be refused, on both platforms alike, so the check and its failure live once. */
class PcdaMarkTest {
    private fun marked(vararg rest: Int) = "PCDA".encodeToByteArray() + ByteArray(rest.size) { rest[it].toByte() }

    private fun assertRefused(plain: ByteArray) {
        val error = PcdaMark.open(plain).exceptionOrNull()
        assertTrue(error is DecryptionFailedException, "was $error")
        assertNull(error.cause)
    }

    @Test
    fun theMarkIsTakenOffAndTheRestIsGivenBack() = assertContentEquals(byteArrayOf(1, 2, 3), PcdaMark.open(marked(1, 2, 3)).getOrThrow())

    @Test
    fun aMarkWithNothingAfterItIsAnEmptyPayload() = assertContentEquals(ByteArray(0), PcdaMark.open(marked()).getOrThrow())

    @Test
    fun dataWithAnotherStartIsTheTypedFailure() {
        assertRefused("PCDB1".encodeToByteArray())
        assertRefused("pcda1".encodeToByteArray())
        assertRefused("XPCDA".encodeToByteArray())
    }

    @Test
    fun dataShorterThanTheMarkIsTheTypedFailure() {
        assertRefused(ByteArray(0))
        assertRefused("PCD".encodeToByteArray())
    }
}
