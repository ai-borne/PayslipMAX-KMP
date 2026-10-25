package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.SlotValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: the list on the Part II home and the orders linked to a slip show the same cancel note as the order's own screen (DECISIONS
 * Round 22), so the officer sees at a glance which published orders have been cancelled. Only the order the slip quotes carries it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class VaultListCancelNoteTest : VaultListTestBase() {
    private val date = CalendarDate(2026, 3, 1)

    private val cancelSlip =
        Part2Draft(
            "slip",
            LeaveEventInput(listOf(OccurrenceRequest("CANCELS", mapOf("cancelled" to SlotValue.RefValue(OrderRef("N/a", date)))))),
            emptyMap(),
            1,
            1,
            PrintedSnapshot(1_700_000_000_000L, emptyMap(), emptyList()),
        )

    @Test
    fun onlyTheOrderACancelSlipQuotesCarriesTheNoteAndItAppearsWhenTheSlipIsPrinted() =
        check { d ->
            saveOrder("a")
            saveOrder("b", no = "N/b")
            val vault = model(d)
            assertNull(vault.state.value.rows.first { it.id == "a" }.note)

            repo.saveDraft(cancelSlip)
            advanceUntilIdle()

            val rows = vault.state.value.rows.associateBy { it.id }
            assertEquals("A slip cancelling one occurrence was printed on 14/11/2023.", rows.getValue("a").note)
            assertNull(rows.getValue("b").note)
        }

    @Test
    fun theNoteUsesTheDayWhereTheOfficerIs() =
        check { d ->
            saveOrder("a")
            repo.saveDraft(cancelSlip)
            val vault = model(d, offset = 19_800_000L)

            assertEquals("A slip cancelling one occurrence was printed on 15/11/2023.", vault.state.value.rows.single().note)
        }
}
