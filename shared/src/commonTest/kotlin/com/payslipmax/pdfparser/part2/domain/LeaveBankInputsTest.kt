package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * What a fill reads of the officer's other drafts: the live ones only, the draft being edited left out, and "printed" kept apart from "drafted"
 * (the leave used in a year is what was handed to the clerk; a total carried forward may come from any finished draft). Synthetic data.
 */
class LeaveBankInputsTest {
    private val day = CalendarDate(2026, 6, 20)

    /** A synthetic figure no real statement shows, to prove nothing prints it. */
    private companion object {
        const val MARKER_FIGURE = 918273
    }

    private val rows = listOf(accrued("2024-12-31", 19, 291))

    private fun draft(
        id: String,
        code: String,
        printed: Boolean = true,
        values: Map<String, SlotValue> = emptyMap(),
    ) = Part2Draft(id, LeaveEventInput(listOf(OccurrenceRequest(code, values))), emptyMap(), 1, 1, if (printed) PrintedSnapshot(5, emptyMap(), emptyList()) else null)

    private fun codes(requests: List<OccurrenceRequest>) = requests.map { it.code }

    @Test
    fun printedAndDraftedAreKeptApartInTheOrderGivenAndTheRowsPassThrough() {
        val inputs = LeaveBankInputs.of(rows, listOf(draft("a", "GTDAL"), draft("b", "EXTNAL", printed = false), draft("c", "ACCLVE")), emptyList(), selfId = "me")
        assertEquals(listOf("GTDAL", "ACCLVE"), codes(inputs.printed))
        assertEquals(listOf("GTDAL", "EXTNAL", "ACCLVE"), codes(inputs.drafted))
        assertEquals(rows, inputs.rows)
    }

    @Test
    fun theDraftBeingEditedIsNeverItsOwnSource() {
        val inputs = LeaveBankInputs.of(rows, listOf(draft("me", "ACCLVE"), draft("other", "GTDAL")), emptyList(), selfId = "me")
        assertEquals(listOf("GTDAL"), codes(inputs.drafted))
        assertEquals(listOf("GTDAL"), codes(inputs.printed))
    }

    @Test
    fun aDraftWhoseOrderWasCancelledInFullIsInNeitherList() {
        // why: a void order set no leave used and gave no total; carrying its figures forward would be a figure from an order that no longer stands.
        val order = VaultOrder("o1", "leave", "0123/26", day, 1, 1)
        val cancel = Part2Draft("cancel", LeaveEventInput(listOf(OccurrenceRequest("CANCEL", mapOf("cancelled" to SlotValue.RefValue(OrderRef("0123/26", day)))))), emptyMap(), 1, 1, PrintedSnapshot(5, emptyMap(), emptyList()))
        val inputs = LeaveBankInputs.of(rows, listOf(draft("leave", "GTDAL"), cancel, draft("fine", "EXTNAL")), listOf(order), selfId = "me")
        assertEquals(listOf("CANCEL", "EXTNAL"), codes(inputs.drafted), "the cancel slip itself stays; the cancelled leave goes")
        assertEquals(listOf("CANCEL", "EXTNAL"), codes(inputs.printed))
    }

    @Test
    fun theInputsAndTheirOutcomePrintNothingOfTheOfficersFigures() {
        // why: Leave Bank figures are real financial data and must not reach a log through a string template.
        val inputs = LeaveBankInputs.of(rows, listOf(draft("a", "GTDAL", values = mapOf("year" to SlotValue.NumberValue(MARKER_FIGURE)))), emptyList(), "me")
        assertFalse("$MARKER_FIGURE" in inputs.toString())
        assertFalse("291" in inputs.toString())
        assertFalse("$MARKER_FIGURE" in FillOutcome.Values(mapOf("x" to SlotValue.NumberValue(MARKER_FIGURE))).toString())
    }
}
