package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.FillOutcome
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.testing.FakeLeaveBankSource
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.theme.Part2LeaveBankStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The fill button's behaviour on the leave form (plan, phase 8b; owner, Round 33): it fills only the boxes still empty, and a confirmed
 * refill replaces only the figures the fill gives. The sibling `LeaveDraftFillLateTest` covers a Leave Bank that is slow, a form that closes
 * and the notes that follow the rows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LeaveDraftFillTest : LeaveDraftFillBase() {
    @Test
    fun theFillIsOfferedOnlyWhereTheLeaveBankIsOnAndTheCodeHasOne() =
        runTest {
            assertNull(fillModel(source = null).state.value.sections[0].fill, "the Leave Bank is off")
            assertNull(fillModel(start = LeaveEventEditor.start(EXTENSION)).also { advanceUntilIdle() }.state.value.sections[0].fill, "the code has no fill")
            val offered = fillModel().state.value.sections[0].fill
            assertNotNull(offered)
            assertEquals(false, offered.refill, "nothing is filled yet")
        }

    @Test
    fun aFillPutsTheFiguresInTheEmptyBoxesAndNeverOverwritesWhatWasTyped() =
        runTest {
            val vm = fillModel()
            advanceUntilIdle()
            vm.setDate(0, "leave_from", CalendarDate(2026, 7, 5))
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            assertEquals(FieldValue.Digits("2024"), vm.value("year"))
            assertEquals(FieldValue.Date(CalendarDate(2026, 7, 5)), vm.value("leave_from"), "the typed day stays")
            assertEquals(Part2LeaveBankStrings.filled(1), vm.message())
            assertEquals(true, vm.state.value.sections[0].fill?.refill, "a figure is there now, so the button asks before it replaces")
        }

    @Test
    fun withNoStatementRowsTheMessageDoesNotClaimTheLeaveBankGaveTheFigures() =
        runTest {
            // Item 69(a), found on the emulator: an officer with no Leave Bank statement was told "from your Leave Bank".
            val vm = fillModel(source = FakeLeaveBankSource(emptyList()))
            advanceUntilIdle()
            vm.setDate(0, "leave_from", CalendarDate(2026, 7, 5))
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            assertEquals(Part2LeaveBankStrings.filled(1, fromLeaveBank = false), vm.message())
            assertFalse("Leave Bank" in vm.message().orEmpty(), vm.message())
        }

    @Test
    fun theButtonAsksBeforeReplacingAsSoonAsAnyOneFigureOfTheFillIsThere() =
        runTest {
            val vm = fillModel()
            advanceUntilIdle()
            vm.setNumber(0, "year", "1999")
            assertEquals(true, vm.state.value.sections[0].fill?.refill, "one typed figure of the two is enough")
            vm.setNumber(0, "year", "")
            vm.setDate(0, "leave_to", CalendarDate(2031, 8, 9))
            assertEquals(false, vm.state.value.sections[0].fill?.refill, "a box the fill does not give does not count")
        }

    @Test
    fun theMessageAndTheFilledFiguresBelongToTheOccurrenceTheButtonWasPressedOn() =
        runTest {
            val vm = fillModel(start = LeaveEventInput(listOf(OccurrenceRequest(GRANT), OccurrenceRequest(GRANT))))
            advanceUntilIdle()
            vm.fillFromLeaveBank(1, replace = false)
            advanceUntilIdle()
            val sections = vm.state.value.sections
            assertNull(sections[0].fill?.message)
            assertEquals(Part2LeaveBankStrings.filled(2), sections[1].fill?.message)
            assertEquals(FieldValue.Digits(""), sections[0].fields.first { it.slotId == "year" }.value)
            assertEquals(FieldValue.Digits("2024"), sections[1].fields.first { it.slotId == "year" }.value)
        }

    @Test
    fun aConfirmedRefillReplacesOnlyTheFiguresTheFillGivesAndLeavesEveryOtherBox() =
        runTest {
            val vm = fillModel()
            advanceUntilIdle()
            vm.setNumber(0, "year", "1999")
            vm.setDate(0, "leave_from", CalendarDate(2026, 7, 5))
            vm.setDate(0, "leave_to", CalendarDate(2031, 8, 9))
            vm.fillFromLeaveBank(0, replace = true)
            advanceUntilIdle()
            assertEquals(FieldValue.Digits("2031"), vm.value("year"))
            assertEquals(FieldValue.Date(CalendarDate(2024, 1, 2)), vm.value("leave_from"))
            assertEquals(FieldValue.Date(CalendarDate(2031, 8, 9)), vm.value("leave_to"), "not a figure of the fill: untouched")
            assertEquals(Part2LeaveBankStrings.filled(2), vm.message())
        }

    @Test
    fun whatTheFillCouldNotGiveIsSaidInWordsAndChangesNothing() =
        runTest {
            val vm = fillModel()
            advanceUntilIdle()
            fill.outcome = { FillOutcome.NeedsYear }
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            assertEquals(Part2LeaveBankStrings.needsYear, vm.message())
            fill.outcome = { FillOutcome.NothingKnown }
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            assertEquals(Part2LeaveBankStrings.nothingKnown, vm.message())
            assertEquals(FieldValue.Digits(""), vm.value("year"))
            fill.outcome = { FillOutcome.Values(mapOf("year" to SlotValue.NumberValue(2024))) }
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = true)
            advanceUntilIdle()
            assertEquals(Part2LeaveBankStrings.unchanged, vm.message(), "the same figure again changes nothing and says so")
        }

    @Test
    fun aDraftWhoseOrderWasCancelledInFullIsNotWhatTheFillReadsFrom() =
        runTest {
            // why: a void order set no leave used and gave no total; the stored orders are what tell it from a live one.
            val day = CalendarDate(2026, 6, 20)
            val printed = PrintedSnapshot(5, emptyMap(), emptyList())
            val ref = SlotValue.RefValue(OrderRef("0123/26", day))
            repo.saveDraft(Part2Draft("leave", LeaveEventEditor.start(EXTENSION), emptyMap(), 1L, 5L, printed))
            repo.saveDraft(Part2Draft("kept", LeaveEventEditor.start(EXTENSION), emptyMap(), 1L, 4L, printed))
            repo.saveDraft(Part2Draft("cancel", LeaveEventInput(listOf(OccurrenceRequest("CANCEL", mapOf("cancelled" to ref)))), emptyMap(), 1L, 3L, printed))
            repo.saveOrder(VaultOrder("o1", "leave", "0123/26", day, 1L, 1L))
            val vm = fillModel()
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            assertEquals(listOf(EXTENSION, "CANCEL"), fill.asked.single().drafted.map { it.code }, "the cancelled leave is out; its cancel slip and the other draft stay")
        }

    @Test
    fun aMessageGoesAwayWhenTheOfficerEditsAgain() =
        runTest {
            val vm = fillModel()
            advanceUntilIdle()
            vm.fillFromLeaveBank(0, replace = false)
            advanceUntilIdle()
            assertNotNull(vm.message())
            vm.setNumber(0, "year", "2025")
            assertNull(vm.message())
        }
}
