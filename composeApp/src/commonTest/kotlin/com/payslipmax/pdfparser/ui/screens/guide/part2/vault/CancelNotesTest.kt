package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: the officer must be able to see, on the order and on the slip it came from, that a cancel slip has been handed over and when the
 * published cancel order proved it, in words and in the day where they are, never from stored text. Synthetic orders only.
 */
class CancelNotesTest {
    private val target = VaultOrder("o-target", null, "0123/26", CalendarDate(2026, 6, 20), 1, 1)

    // 1_700_000_000_000 ms is 14 Nov 2023 22:13 UTC, so +5:30 is already 15 Nov.
    private val printedAt = 1_700_000_000_000L

    private fun slip(
        code: String,
        printed: Long? = printedAt,
        id: String = "d",
    ) = Part2Draft(
        id,
        LeaveEventInput(listOf(OccurrenceRequest(code, mapOf("cancelled" to SlotValue.RefValue(OrderRef("0123/26", CalendarDate(2026, 6, 20))))))),
        emptyMap(),
        1,
        1,
        printed?.let { PrintedSnapshot(it, emptyMap(), emptyList()) },
    )

    private fun note(
        drafts: List<Part2Draft>,
        orders: List<VaultOrder> = emptyList(),
        offset: Long = 0L,
    ) = CancelNotes.of(target, drafts, orders + target) { offset }

    @Test
    fun anOrderNobodyHasTriedToCancelHasNoNote() {
        assertNull(note(emptyList()))
        assertNull(note(listOf(slip("CANCEL", printed = null))), "a slip not yet handed over has cancelled nothing")
    }

    @Test
    fun aPrintedCancelSlipSaysItWasPrintedOnTheDayWhereTheOfficerIs() {
        assertEquals("Cancel slip printed on 14/11/2023.", note(listOf(slip("CANCEL"))))
        assertEquals("Cancel slip printed on 15/11/2023.", note(listOf(slip("CANCEL")), offset = 19_800_000L))
    }

    @Test
    fun aPublishedCancelOrderLinkedToTheSlipNamesItsNumberAndDate() {
        val proof = VaultOrder("o-cancel", "d", "9/26", CalendarDate(2026, 7, 1), 2, 2)

        assertEquals("Cancelled by order 9/26 dated 01/07/2026.", note(listOf(slip("CANCEL")), listOf(proof)))
    }

    @Test
    fun aSlipThatCancelsOneOccurrenceSaysSoInsteadOfCancellingTheOrder() {
        val proof = VaultOrder("o-cancel", "d", "9/26", CalendarDate(2026, 7, 1), 2, 2)

        assertEquals("A slip cancelling one occurrence was printed on 14/11/2023.", note(listOf(slip("CANCELS"))))
        assertEquals("One occurrence cancelled by order 9/26 dated 01/07/2026.", note(listOf(slip("CANCELS")), listOf(proof)))
    }
}
