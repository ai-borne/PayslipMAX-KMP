package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a page is a photo of a RESTRICTED published order. The file must hold only ciphertext, a page must stay with the order and id it
 * was sealed for (GCM does not cover the columns beside it), a page over the cap must never reach the encryptor, and a damaged page
 * must fail with a fixed message.
 */
class Part2PageCodecTest {
    private val key = "device-key-for-tests"
    private val jpeg = SyntheticBank.jpeg(4000)

    private fun framed(
        id: String,
        orderId: String,
        body: ByteArray = jpeg,
    ): ByteArray {
        val a = id.encodeToByteArray()
        val b = orderId.encodeToByteArray()
        return byteArrayOf(1, a.size.toByte()) + a + byteArrayOf(b.size.toByte()) + b + body
    }

    private fun entityOf(
        plain: ByteArray,
        id: String = "page-1",
        orderId: String = "order-1",
    ) = Part2PageEntity(id, orderId, 0, CryptoHelper.encrypt(plain, key).getOrThrow())

    @Test
    fun aPageRoundTripsByteForByteAndKeepsItsLinkColumns() {
        val row = sealPage("page-1", "order-1", 3, jpeg, key)

        assertContentEquals(jpeg, row.openPage(key))
        assertEquals(Triple("page-1", "order-1", 3), Triple(row.id, row.orderId, row.position))
    }

    @Test
    fun theStoredBytesHoldNoRecognisablePartOfThePhoto() {
        val row = sealPage("page-1", "order-1", 0, jpeg, key)
        val chunk = jpeg.copyOfRange(100, 140).toList()

        assertFalse(row.ciphertext.toList().windowed(chunk.size).any { it == chunk }, "a run of the plain page must not appear in the row")
        assertEquals(jpeg.size + Part2Limits.SEAL_OVERHEAD_BYTES + 3 + "page-1".length + "order-1".length, row.ciphertext.size)
    }

    @Test
    fun aPageSealedForOneOrderIsRefusedUnderAnother() {
        val row = sealPage("page-1", "order-1", 0, jpeg, key)

        assertFailsWith<Part2UnreadableException> { Part2PageEntity("page-1", "order-2", 0, row.ciphertext).openPage(key) }
        assertFailsWith<Part2UnreadableException> { Part2PageEntity("page-2", "order-1", 0, row.ciphertext).openPage(key) }
    }

    @Test
    fun theWrongKeyATamperedByteAndGarbageAreUnreadableWithAFixedMessage() {
        val row = sealPage("page-1", "order-1", 0, jpeg, key)
        val bytes = row.ciphertext.copyOf().also { it[it.size / 2] = (it[it.size / 2].toInt() xor 1).toByte() }

        val wrongKey = assertFailsWith<Part2UnreadableException> { row.openPage("another-device-key") }
        assertFailsWith<Part2UnreadableException> { Part2PageEntity("page-1", "order-1", 0, bytes).openPage(key) }
        assertFailsWith<Part2UnreadableException> { Part2PageEntity("page-1", "order-1", 0, ByteArray(0)).openPage(key) }
        assertTrue(wrongKey.cause == null && wrongKey.message == "A Part II item could not be read.")
    }

    @Test
    fun aDecryptableFrameThatIsTruncatedOrOfAnotherVersionIsUnreadable() {
        val good = framed("page-1", "order-1")

        assertContentEquals(jpeg, entityOf(good).openPage(key))
        assertFailsWith<Part2UnreadableException> { entityOf(byteArrayOf(1, 60) + "short".encodeToByteArray()).openPage(key) }
        assertFailsWith<Part2UnreadableException> { entityOf(byteArrayOf(2) + good.copyOfRange(1, good.size)).openPage(key) }
        assertFailsWith<Part2UnreadableException> { entityOf(byteArrayOf(1)).openPage(key) }
        assertFailsWith<Part2UnreadableException> { entityOf(framed("page-1", "order-1", ByteArray(0))).openPage(key) }
    }

    @Test
    fun aPageOverTheCapOrEmptyIsRefusedBeforeItIsEncrypted() {
        assertContentEquals(
            SyntheticBank.jpeg(Part2Limits.PAGE_MAX_BYTES),
            sealPage("page-1", "order-1", 0, SyntheticBank.jpeg(Part2Limits.PAGE_MAX_BYTES), key).openPage(key),
        )

        assertFailsWith<Part2TooLargeException> { sealPage("page-1", "order-1", 0, SyntheticBank.jpeg(Part2Limits.PAGE_MAX_BYTES + 1), key) }
        assertFailsWith<Part2TooLargeException> { sealPage("page-1", "order-1", 0, ByteArray(0), key) }
    }

    @Test
    fun theIdsOfAPageMustBePlainTokens() {
        assertFailsWith<IllegalArgumentException> { sealPage("page/1", "order-1", 0, jpeg, key) }
        assertFailsWith<IllegalArgumentException> { sealPage("page-1", "", 0, jpeg, key) }
        assertFailsWith<IllegalArgumentException> { sealPage("page-1", "order-1", -1, jpeg, key) }
    }
}
