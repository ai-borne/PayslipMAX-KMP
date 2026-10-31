package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.testing.SyntheticBank.MARKER_DATA
import com.payslipmax.pdfparser.testing.SyntheticBank.MARKER_NOTE
import com.payslipmax.pdfparser.testing.SyntheticBank.MARKER_ORDER_NO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: an entry is the officer's private record (which casualty, which dates, what he typed), so nothing readable may reach the database
 * file. A row holds one AES-GCM ciphertext; the id is the key and the only plaintext. An entry must come back exactly as typed, and a row
 * that was tampered with, moved or written by a newer app must fail with a fixed message that quotes none of it.
 */
class Part2CodecTest {
    private val key = "device-key-for-tests"
    private val entry = SyntheticBank.entry(orderId = "order-1")

    private fun entityFor(
        json: String,
        id: String = entry.id,
    ) = Part2EntryEntity(id, CryptoHelper.encrypt(json.encodeToByteArray(), key).getOrThrow())

    private val base = """"id":"entry-1","code":"SYNCODE1","from":{"y":2026,"m":3,"d":1},"createdAt":1,"updatedAt":2"""

    @Test
    fun anEntryComesBackExactlyAsTheOfficerLeftIt() {
        assertEquals(entry, entry.toEntity(key).toEntry(key))
        val bare = SyntheticBank.entry("e2").copy(to = null, orderId = null, data = listOf("", "", "", ""), note = "")
        assertEquals(bare, bare.toEntity(key).toEntry(key))
    }

    @Test
    fun textIsStoredVerbatimWithItsSpacesAndUnicode() {
        val odd = (" two  spaces, हिन्दी ".repeat(9)).take(Part2Limits.TEXT_MAX)
        val typed = entry.copy(data = listOf(odd, "", "", ""), note = odd)

        assertEquals(typed, typed.toEntity(key).toEntry(key))
    }

    @Test
    fun theRowHoldsNoReadableCodeDateNoteOrColumnValue() {
        val row = entry.toEntity(key)
        val onDisk = row.id + row.ciphertext.decodeToString()

        for (secret in listOf(MARKER_NOTE, MARKER_DATA, "SYNCODE", "order-1", "2026")) {
            assertFalse(onDisk.contains(secret), "$secret must not be readable in the row")
        }
    }

    @Test
    fun theSameEntryEncryptsDifferentlyEachTime() {
        assertNotEquals(entry.toEntity(key).ciphertext.toList(), entry.toEntity(key).ciphertext.toList())
    }

    @Test
    fun theWrongKeyIsUnreadableAndTheErrorSaysNothingAboutTheEntry() {
        assertNoLeak(assertFailsWith<Part2UnreadableException> { entry.toEntity(key).toEntry("another-device-key") })
    }

    @Test
    fun tamperedGarbageAndEmptyCiphertextAreUnreadableNotACrash() {
        val bytes = entry.toEntity(key).ciphertext.copyOf()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 1).toByte()

        assertFailsWith<Part2UnreadableException> { Part2EntryEntity(entry.id, bytes).toEntry(key) }
        assertFailsWith<Part2UnreadableException> { Part2EntryEntity(entry.id, byteArrayOf(1, 2, 3)).toEntry(key) }
        assertFailsWith<Part2UnreadableException> { Part2EntryEntity(entry.id, ByteArray(0)).toEntry(key) }
    }

    @Test
    fun aRowMovedToAnotherIdIsRefused() {
        // GCM protects the ciphertext, not the primary key next to it; the id inside the ciphertext is what catches a swap.
        assertFailsWith<Part2UnreadableException> { Part2EntryEntity("entry-2", entry.toEntity(key).ciphertext).toEntry(key) }
    }

    @Test
    fun decryptableJsonThatIsNotAnEntryIsUnreadableAndNeverEchoesItsContent() {
        // kotlinx.serialization quotes the input in its error text; it must not reach ours.
        assertNoLeak(assertFailsWith<Part2UnreadableException> { entityFor("""{"v":1,"id":"entry-1","note":"$MARKER_NOTE""").toEntry(key) })
    }

    @Test
    fun aPayloadFromANewerAppIsUnreadableButAFieldAddedWithinVersionOneIsIgnored() {
        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":2,$base}""").toEntry(key) }
        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":0,$base}""").toEntry(key) }
        assertEquals("entry-1", entityFor("""{"v":1,$base,"futureField":{"at":9}}""").toEntry(key).id)
    }

    @Test
    fun aStoredEntryOutsideTheBoundsIsUnreadableSoARestoredFileCannotExceedThem() {
        val long = "a".repeat(Part2Limits.TEXT_MAX + 1)

        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":1,$base,"note":"$long"}""").toEntry(key) }
        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":1,$base,"data":["$long"]}""").toEntry(key) }
        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":1,${base.replace("SYNCODE1", "SYN CODE")}}""").toEntry(key) }
        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":1,$base,"orderId":"a/b"}""").toEntry(key) }
    }

    @Test
    fun aDateOfAnotherKindIsUnreadable() {
        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":1,${base.replace("\"d\":1}", "\"d\":1,\"k\":\"n\"}")}}""").toEntry(key) }
    }

    @Test
    fun aPayloadOverTheCeilingIsRefusedBeforeItIsEncrypted() {
        val huge = """{"pad":"${"x".repeat(Part2Limits.PAYLOAD_MAX_BYTES)}"}"""

        assertNoLeak(assertFailsWith<Part2TooLargeException> { sealPayload(huge, key) })
    }

    @Test
    fun theBoundsAcceptTheLimitAndRefuseOneMore() {
        val max = "a".repeat(Part2Limits.TEXT_MAX)
        val over = max + "a"

        assertNull(Part2Bounds.check(entry))
        assertNull(Part2Bounds.check(entry.copy(note = max, data = List(Part2Limits.DATA_COLUMNS) { max })))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(entry.copy(note = over)))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(entry.copy(data = listOf(over, "", "", ""))))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(entry.copy(data = listOf("", "", ""))), "the four Data columns are always there")
    }

    @Test
    fun anIdOrACodeThatIsNotAPlainTokenIsRefused() {
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(entry.copy(id = "a/b")))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(entry.copy(id = "")))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(entry.copy(id = "a".repeat(65))))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(entry.copy(code = "<b>")))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(entry.copy(orderId = "../x")))
    }

    @Test
    fun anOrderRoundTripsAndKeepsItsNumberAndDateOutOfTheFile() {
        val order = SyntheticBank.order()
        val row = order.toEntity(key)

        assertEquals(order, row.toOrder(key))
        assertFalse((row.id + row.ciphertext.decodeToString()).contains(MARKER_ORDER_NO))
        assertNull(SyntheticBank.order(orderNo = null, date = null).let { it.toEntity(key).toOrder(key) }.orderNo)
        assertFailsWith<Part2UnreadableException> { Part2OrderEntity("order-2", row.ciphertext).toOrder(key) }
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(order.copy(orderNo = "a".repeat(41))))
        assertNull(Part2Bounds.check(order))
    }

    @Test
    fun anOrderRowFromTheFrozenBranchStillOpensItsDraftLinkIgnored() {
        // The frozen branch wrote an order's date as a slot payload and a draftId; a phone that ran it must keep its scanned orders.
        val old = """{"v":1,"id":"order-1","draftId":"d-9","orderNo":"N/1","date":{"k":"d","y":2026,"m":2,"d":3,"n":0,"t":"","no":""},"createdAt":1,"updatedAt":2}"""
        val order = Part2OrderEntity("order-1", CryptoHelper.encrypt(old.encodeToByteArray(), key).getOrThrow()).toOrder(key)

        assertEquals(Triple("N/1", CalendarDate(2026, 2, 3), 2L), Triple(order.orderNo, order.orderDate, order.updatedAt))
    }

    @Test
    fun theTextFormsOfAnEntryAnOrderAndTheirRowsHideEverything() {
        val shown = listOf(entry.toString(), SyntheticBank.order().toString(), entry.toEntity(key).toString(), StoredPart2(listOf(entry), 2).toString())

        for (text in shown) {
            for (secret in listOf(MARKER_NOTE, MARKER_DATA, MARKER_ORDER_NO, "SYNCODE", "2026", entry.id)) assertFalse(text.contains(secret), "$text leaks $secret")
        }
    }

    @Test
    fun theSealOverheadTheCapsAssumeIsWhatTheRealCryptoHelperAdds() {
        // Restore compares a row's text length with this before decoding it; if the helper's format changes, the bound must change with it.
        assertEquals(Part2Limits.SEAL_OVERHEAD_BYTES, CryptoHelper.encrypt(ByteArray(0), key).getOrThrow().size)
        assertEquals(Part2Limits.SEAL_OVERHEAD_BYTES + 1000, CryptoHelper.encrypt(ByteArray(1000), key).getOrThrow().size)
    }

    private fun assertNoLeak(error: Throwable) {
        val everything = generateSequence(error) { it.cause }.joinToString(" | ") { it.message.orEmpty() }
        for (secret in listOf(MARKER_NOTE, MARKER_ORDER_NO, "SYNCODE", "entry-1")) assertFalse(everything.contains(secret), "the error text must not carry $secret: $everything")
        assertTrue(error.cause == null, "a cause could carry the input")
    }
}
