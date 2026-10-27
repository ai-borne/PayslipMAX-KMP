package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.AreaRules
import com.payslipmax.pdfparser.part2.domain.DraftResult
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.RationRules
import com.payslipmax.pdfparser.part2.domain.RealPart2
import com.payslipmax.pdfparser.part2.domain.SlotValue
import kotlinx.serialization.encodeToString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY: the 128 KB payload ceiling was sized when a request printed at most seven rows. The area re-grants put a certificate of about a
 * kilobyte on nearly every linked row, so the largest draft a form can produce must be measured against the ceiling and the print-snapshot
 * bounds on the real bundle, with every text box at its longest: a draft that could not be saved after the officer had typed it all would
 * be the worst way to find out.
 */
class Part2WorstDraftSizeTest {
    private val long = "x".repeat(Part2Limits.TEXT_MAX)

    private fun request(i: Int): OccurrenceRequest {
        val values =
            mapOf(
                "leave_from" to SlotValue.DateValue(CalendarDate(2000 + i, 6, 25)),
                "leave_to" to SlotValue.DateValue(CalendarDate(2000 + i, 8, 5)),
                "prefix_first" to SlotValue.DateValue(CalendarDate(2000 + i, 6, 23)),
                "suffix_last" to SlotValue.DateValue(CalendarDate(2000 + i, 8, 6)),
                "year" to SlotValue.NumberValue(2000 + i),
                "leave_kind" to SlotValue.TextValue(long),
                "sanction_to" to SlotValue.DateValue(CalendarDate(2000 + i, 12, 31)),
            ) +
                listOf("station", "corps", "serial", "haa_corps", "haa_serial", "operation", "sccia_corps", "sccia_serial", "location", "field_corps", "field_serial", "field_operation", "tran_station", "tran_disability")
                    .associateWith { SlotValue.TextValue(long) } + listOf("notif", "haa_notif", "sccia_notif", "field_notif").associateWith { SlotValue.RefValue(OrderRef("N".repeat(Part2Limits.ORDER_NO_MAX), CalendarDate(2026, 3, 2))) }
        val answers = setOf(RationRules.ANSWER_ON_RATION, AreaRules.ANSWER_RH_PREFIX + "GTDRH12", AreaRules.ANSWER_HAA_PREFIX + "GTDHH11", AreaRules.ANSWER_SCCIA_PREFIX + "GTDCIAFD", AreaRules.ANSWER_FIELD_PREFIX + "GTDHAFA", AreaRules.ANSWER_TRAN_PREFIX + "GTDTRAN3")
        return OccurrenceRequest("GTDAL", values, answers)
    }

    @Test
    fun theLargestDraftTheFormCanMakeFitsTheStoredPayloadAndEveryBound() {
        val event = LeaveEventInput((0 until Part2Limits.REQUESTS_MAX).map(::request))
        assertTrue(event.requests.all { it.values.size <= Part2Limits.VALUES_PER_REQUEST_MAX }, "28 boxes at most")
        val slip = assertIs<DraftResult.Drafted>(RealPart2.engine.draft(event)).slip
        val rows = slip.orders.flatMap { it.occurrences }
        assertEquals(Part2Limits.REQUESTS_MAX * 12, rows.size, "each request prints the ration four, R&H, the five area rows and itself, and none merge")
        assertTrue(rows.size <= Part2Limits.PRINTED_ROWS_MAX)
        val revs = RealPart2.section.templates.take(Part2Limits.REVS_MAX).associate { it.code to it.rev }
        val draft = Part2Draft("worst", event, revs, 1L, 2L, PrintedSnapshot.of(slip, revs, 3L))
        assertEquals(null, Part2Bounds.check(draft), "the stored draft passes the shape bounds")
        val plain = kotlinx.serialization.json.Json { encodeDefaults = true }.encodeToString(draft.toPayload()).encodeToByteArray().size
        assertTrue(plain < Part2Limits.PAYLOAD_MAX_BYTES, "the payload is $plain bytes against a ceiling of ${Part2Limits.PAYLOAD_MAX_BYTES}")
        assertTrue(draft.toEntity("a long backup password for the test").ciphertext.isNotEmpty())
    }
}
