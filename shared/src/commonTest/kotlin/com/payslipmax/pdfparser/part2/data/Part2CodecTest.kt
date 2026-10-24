package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.AreaRules
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.RationRules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts.MARKER_ORDER_NO
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts.MARKER_TEXT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a draft is the officer's private plan (which leave, which dates, which order they quote), so nothing readable may reach the
 * database file. A row holds one AES-GCM ciphertext; the id is the key and the only plaintext. A draft must also come back exactly
 * as typed (the answers and slot values are what the engine re-reads), and a row that was tampered with, moved or written by a newer
 * app must fail with a fixed message that quotes none of it.
 */
class Part2CodecTest {
    private val key = "device-key-for-tests"
    private val draft = SyntheticPart2Drafts.fullDraft()

    private fun entityFor(
        json: String,
        id: String = draft.id,
    ) = Part2DraftEntity(id, CryptoHelper.encrypt(json.encodeToByteArray(), key).getOrThrow())

    @Test
    fun aDraftComesBackExactlyAsTheOfficerLeftIt() {
        assertEquals(draft, draft.toEntity(key).toDraft(key))
    }

    @Test
    fun everyKindOfSlotValueAndBothKindsOfAnswerSurvive() {
        val back = draft.toEntity(key).toDraft(key)
        val request = back.event.requests.first()

        assertTrue(request.values.getValue("leave_from") is SlotValue.DateValue)
        assertEquals(SlotValue.NumberValue(2026), request.values["year"])
        assertEquals(SlotValue.TextValue(MARKER_TEXT), request.values["remark"])
        assertEquals(setOf(RationRules.ANSWER_ON_RATION, AreaRules.ANSWER_RH_PREFIX + "SYNRH12"), request.answers)
        assertEquals(mapOf("SYNCODE1" to "a1b2c3d4", "SYNCODE2" to "e5f6a7b8"), back.templateRevs)
        assertEquals("SYN/0001/2026", back.event.lastOrder?.orderNo)
    }

    @Test
    fun textIsStoredVerbatimWithItsSpacesNewlinesAndUnicode() {
        val odd = " two  spaces\nand a line, हिन्दी ".repeat(2).take(200)
        val request = OccurrenceRequest("SYNCODE1", mapOf("remark" to SlotValue.TextValue(odd), "n" to SlotValue.NumberValue(-3)))

        val back = draft.copy(event = draft.event.copy(requests = listOf(request))).toEntity(key).toDraft(key)

        assertEquals(request, back.event.requests.single())
    }

    @Test
    fun anEmptyDraftAndAnUnsetLastOrderRoundTrip() {
        val empty = SyntheticPart2Drafts.emptyDraft()

        assertEquals(empty, empty.toEntity(key).toDraft(key))
        assertNull(empty.toEntity(key).toDraft(key).event.lastOrder)
    }

    @Test
    fun theRowHoldsNoReadableCodeDateAnswerOrOrderNumber() {
        val row = draft.toEntity(key)
        val onDisk = row.id + row.ciphertext.decodeToString()

        for (secret in listOf(MARKER_TEXT, MARKER_ORDER_NO, "SYNCODE", "leave_from", "ration", "a1b2c3d4", "2026")) {
            assertFalse(onDisk.contains(secret), "$secret must not be readable in the row")
        }
    }

    @Test
    fun theSameDraftEncryptsDifferentlyEachTime() {
        assertNotEquals(draft.toEntity(key).ciphertext.toList(), draft.toEntity(key).ciphertext.toList())
    }

    @Test
    fun theWrongKeyIsUnreadableAndTheErrorSaysNothingAboutTheDraft() {
        val error = assertFailsWith<Part2UnreadableException> { draft.toEntity(key).toDraft("another-device-key") }

        assertNoLeak(error)
    }

    @Test
    fun tamperedCiphertextIsUnreadable() {
        val bytes = draft.toEntity(key).ciphertext.copyOf()
        bytes[bytes.size - 1] = (bytes[bytes.size - 1].toInt() xor 1).toByte()

        assertFailsWith<Part2UnreadableException> { Part2DraftEntity(draft.id, bytes).toDraft(key) }
    }

    @Test
    fun garbageAndEmptyCiphertextAreUnreadableNotACrash() {
        assertFailsWith<Part2UnreadableException> { Part2DraftEntity(draft.id, byteArrayOf(1, 2, 3)).toDraft(key) }
        assertFailsWith<Part2UnreadableException> { Part2DraftEntity(draft.id, ByteArray(0)).toDraft(key) }
    }

    @Test
    fun aRowMovedToAnotherIdIsRefused() {
        // GCM protects the ciphertext, not the primary key next to it; the id inside the ciphertext is what catches a swap.
        val moved = Part2DraftEntity("draft-2", draft.toEntity(key).ciphertext)

        assertFailsWith<Part2UnreadableException> { moved.toDraft(key) }
    }

    @Test
    fun decryptableJsonThatIsNotADraftIsUnreadableAndNeverEchoesItsContent() {
        // kotlinx.serialization quotes the input in its error text; it must not reach ours.
        val error = assertFailsWith<Part2UnreadableException> { entityFor("""{"v":1,"id":"draft-1","requests":[{"code":"$MARKER_TEXT""").toDraft(key) }

        assertNoLeak(error)
    }

    @Test
    fun aPayloadFromANewerAppIsUnreadableButAFieldAddedWithinVersionOneIsIgnored() {
        val base = """"id":"draft-1","createdAt":1,"updatedAt":2,"requests":[],"revs":{}"""

        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":2,$base}""").toDraft(key) }
        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":0,$base}""").toDraft(key) }
        assertEquals("draft-1", entityFor("""{"v":1,$base,"futureField":{"at":9}}""").toDraft(key).id)
    }

    @Test
    fun aStoredDraftOutsideTheBoundsIsUnreadableSoARestoredFileCannotExceedThem() {
        val tooManyRequests = (1..11).joinToString(",") { """{"code":"C$it"}""" }
        val longText = "a".repeat(201)

        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"requests":[$tooManyRequests]}""").toDraft(key) }
        assertFailsWith<Part2UnreadableException> {
            entityFor("""{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"requests":[{"code":"C1","values":{"s":{"k":"t","t":"$longText"}}}]}""").toDraft(key)
        }
        assertFailsWith<Part2UnreadableException> { entityFor("""{"v":1,"id":"draft-1","createdAt":1,"updatedAt":2,"requests":[{"code":"C 1"}]}""").toDraft(key) }
    }

    @Test
    fun aPayloadOverTheCeilingIsRefusedBeforeItIsEncrypted() {
        val huge = """{"pad":"${"x".repeat(Part2Limits.PAYLOAD_MAX_BYTES)}"}"""

        val error = assertFailsWith<Part2TooLargeException> { sealPayload(huge, key) }

        assertNoLeak(error)
    }

    @Test
    fun theBoundsAcceptTheLimitAndRefuseOneMore() {
        val limits = Part2Limits

        fun request(vararg values: Pair<String, SlotValue>) = OccurrenceRequest("SYNCODE1", values.toMap())

        fun withRequests(requests: List<OccurrenceRequest>) = draft.copy(event = draft.event.copy(requests = requests))

        assertNull(Part2Bounds.check(draft))
        assertNull(Part2Bounds.check(withRequests(List(limits.REQUESTS_MAX) { request() })))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(withRequests(List(limits.REQUESTS_MAX + 1) { request() })))
        assertNull(Part2Bounds.check(withRequests(listOf(request("s" to SlotValue.TextValue("a".repeat(limits.TEXT_MAX)))))))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(withRequests(listOf(request("s" to SlotValue.TextValue("a".repeat(limits.TEXT_MAX + 1)))))))
        assertEquals(
            Part2WriteResult.REJECTED,
            Part2Bounds.check(withRequests(listOf(request(*Array(limits.VALUES_PER_REQUEST_MAX + 1) { "s$it" to SlotValue.NumberValue(1) })))),
        )
        assertEquals(
            Part2WriteResult.REJECTED,
            Part2Bounds.check(withRequests(listOf(OccurrenceRequest("SYNCODE1", answers = (0..limits.ANSWERS_PER_REQUEST_MAX).map { "a$it" }.toSet())))),
        )
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(withRequests(listOf(OccurrenceRequest("SYNCODE1", answers = setOf("a".repeat(limits.ANSWER_MAX + 1)))))))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(draft.copy(templateRevs = (0..limits.REVS_MAX).associate { "C$it" to "r" })))
    }

    @Test
    fun anIdOrACodeThatIsNotAPlainTokenIsRefused() {
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(draft.copy(id = "a/b")))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(draft.copy(id = "")))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(draft.copy(id = "a".repeat(65))))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(draft.copy(event = draft.event.copy(requests = listOf(OccurrenceRequest("<b>"))))))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(draft.copy(event = draft.event.copy(requests = listOf(OccurrenceRequest("C", mapOf("a b" to SlotValue.NumberValue(1))))))))
    }

    @Test
    fun anOrderRoundTripsAndKeepsItsNumberDateAndDraftLinkOutOfTheFile() {
        val order = SyntheticPart2Drafts.order()
        val row = order.toEntity(key)

        assertEquals(order, row.toOrder(key))
        assertFalse((row.id + row.ciphertext.decodeToString()).contains(MARKER_ORDER_NO))
        assertEquals(SyntheticPart2Drafts.order(draftId = null, orderNo = null, date = null).let { it.toEntity(key).toOrder(key) }.orderNo, null)
        assertFailsWith<Part2UnreadableException> { Part2OrderEntity("order-2", row.ciphertext).toOrder(key) }
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(order.copy(orderNo = "a".repeat(41))))
        assertEquals(Part2WriteResult.REJECTED, Part2Bounds.check(order.copy(draftId = "a/b")))
        assertNull(Part2Bounds.check(order))
    }

    @Test
    fun theTextFormsOfADraftAnOrderAndTheirRowsHideEverything() {
        val shown = listOf(draft.toString(), SyntheticPart2Drafts.order().toString(), draft.toEntity(key).toString(), StoredPart2(listOf(draft), 2).toString())

        for (text in shown) {
            for (secret in listOf(MARKER_TEXT, MARKER_ORDER_NO, "SYNCODE", "2026", draft.id)) assertFalse(text.contains(secret), "$text leaks $secret")
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
        for (secret in listOf(MARKER_TEXT, MARKER_ORDER_NO, "SYNCODE", "draft-1")) assertFalse(everything.contains(secret), "the error text must not carry $secret: $everything")
        assertTrue(error.cause == null, "a cause could carry the input")
    }
}
