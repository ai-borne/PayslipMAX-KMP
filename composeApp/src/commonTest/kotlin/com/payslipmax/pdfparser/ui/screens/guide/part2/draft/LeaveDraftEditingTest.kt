package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.ANSWER_RATION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.RATION
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** What each answer does to the form: limits, digits only, questions that add fields, order references, ticks and occurrences. The templates are invented. */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveDraftEditingTest : LeaveDraftTestBase() {
    @Test
    fun typedTextAndOrderNumbersAreCutAtTheirLimits() =
        runTest {
            val vm = LeaveDraftViewModel("d", SyntheticPart2Templates.lookup, repo, LeaveEventEditor.start(SyntheticPart2Templates.NOTE), SyntheticPart2Templates.registry(), { clock }, StandardTestDispatcher(testScheduler))
            vm.setText(0, "remark", "x".repeat(Part2Limits.TEXT_MAX + 30))
            assertEquals(Part2Limits.TEXT_MAX, (vm.field("remark").value as FieldValue.Words).text.length)
            vm.setText(0, "remark", "")
            assertEquals(FieldValue.Words(""), vm.field("remark").value)
        }

    @Test
    fun numbersAreDigitsOnlyAndEmptyingTheBoxRemovesTheAnswer() =
        runTest {
            val vm = model()
            vm.setNumber(0, "year", "20a2 6x9")
            assertEquals(FieldValue.Digits("2026"), vm.field("year").value, "digits only, no longer than the largest number")
            vm.setNumber(0, "year", "")
            assertEquals(FieldValue.Digits(""), vm.field("year").value)
        }

    @Test
    fun answeringAQuestionAddsItsFieldsAndAnAnswerForAQuestionNotAskedIsIgnored() =
        runTest {
            val vm = model()
            vm.setYesNo(0, "ration", true)
            assertTrue(vm.state.value.sections[0].fields.any { it.slotId == "away_from" })
            vm.setYesNo(0, "no_such_question", true)
            val extension = model(LeaveEventEditor.start(EXTENSION))
            extension.setYesNo(0, "ration", true)
            advanceUntilIdle()
            assertEquals(setOf(ANSWER_RATION), saved()!!.event.requests.single().answers, "only the request that asked the question holds the answer")
        }

    @Test
    fun anOrderReferenceIsHeldWhenOneHalfIsGivenAndRemovedWhenBothAreCleared() =
        runTest {
            val vm = model(LeaveEventEditor.start(EXTENSION))
            vm.setReference(0, "grant", "N/5", null)
            assertEquals(FieldValue.Reference("N/5", null), vm.field("grant").value)
            assertNotNull(vm.field("grant").error)
            vm.setReference(0, "grant", "N/5", CalendarDate(2026, 6, 1))
            assertNull(vm.field("grant").error)
            vm.setReference(0, "grant", "", null)
            assertEquals(FieldValue.Reference("", null), vm.field("grant").value)
            assertNull(vm.field("grant").error)
        }

    @Test
    fun anAckIsTickedAndAnyLaterEditClearsTheTick() =
        runTest {
            val start = LeaveEventInput(listOf(OccurrenceRequest(EXTENSION, mapOf("ext_from" to SlotValue.DateValue(from), "new_to" to SlotValue.DateValue(CalendarDate(2026, 7, 9))))))
            val vm = model(start)
            val key = vm.state.value.slip!!.findings.single().key
            assertFalse(vm.state.value.slip!!.readyToPrint)
            vm.acknowledge(key, true)
            assertTrue(vm.state.value.slip!!.readyToPrint)
            vm.acknowledge(key, false)
            assertFalse(vm.state.value.slip!!.readyToPrint)
            vm.acknowledge(key, true)
            vm.setDate(0, "new_to", CalendarDate(2026, 7, 10))
            assertFalse(vm.state.value.slip!!.findings.single().ticked, "the draft changed, so the officer must read the check again")
        }

    @Test
    fun anotherOccurrenceIsAddedFromTheOfferedCodesOnlyAndOneAlwaysStays() =
        runTest {
            val vm = model()
            vm.addOccurrence("NOT_OFFERED")
            assertEquals(1, vm.state.value.sections.size)
            vm.addOccurrence(EXTENSION)
            assertEquals(listOf(GRANT, EXTENSION), vm.state.value.sections.map { it.code })
            vm.removeOccurrence(0)
            assertEquals(listOf(EXTENSION), vm.state.value.sections.map { it.code })
            vm.removeOccurrence(0)
            assertEquals(1, vm.state.value.sections.size)
            advanceUntilIdle()
            assertEquals(listOf(EXTENSION), saved()!!.event.requests.map { it.code })
        }

    @Test
    fun aRationRowIsOnTheSlipInDateOrderBeforeTheLeaveItStartsEarlierThan() =
        runTest {
            val vm = model(complete(ANSWER_RATION, extra = mapOf("away_from" to SlotValue.DateValue(CalendarDate(2026, 7, 1)))))
            assertEquals(listOf(RATION, GRANT), vm.state.value.slip!!.orders.flatMap { it.rows }.map { it.code }, "para 6.4: date order, and the ration starts on 01/07")
        }

    @Test
    fun aChoiceOutsideTheQuestionsOptionsIsIgnoredAndNoneClearsIt() =
        runTest {
            val base = SyntheticPart2Templates.registry()
            val oneOf = com.payslipmax.pdfparser.part2.domain.AnswerQuestion.OneOf("hill_code", "hill_code:", listOf("H1", "H2"), setOf(SyntheticPart2Templates.IN_HILL_STATION))
            val rules = com.payslipmax.pdfparser.part2.domain.Part2Rules(base.ids.map { base.check(it) ?: checkNotNull(base.condition(it)) }, base.questions + oneOf)
            val vm = LeaveDraftViewModel("d", SyntheticPart2Templates.lookup, repo, LeaveEventEditor.start(GRANT), rules, { clock }, StandardTestDispatcher(testScheduler))
            vm.choose(0, "hill_code", "H9")
            advanceUntilIdle()
            assertNull(saved("d"), "an option the question does not offer changes nothing, so nothing is saved")
            vm.choose(0, "hill_code", "H2")
            assertEquals("H2", vm.state.value.sections[0].questions.first { it.id == "hill_code" }.chosen)
            vm.choose(0, "hill_code", null)
            assertNull(vm.state.value.sections[0].questions.first { it.id == "hill_code" }.chosen)
            vm.choose(0, "ration", "H1")
            advanceUntilIdle()
            assertEquals(emptySet(), saved("d")!!.event.requests.single().answers, "a yes/no question takes no choice")
        }

    @Test
    fun aGoneDraftAndAClosedModelTakeNoTicks() =
        runTest {
            val gone = LeaveDraftViewModel("gone", SyntheticPart2Templates.lookup, repo, null, SyntheticPart2Templates.registry(), { clock }, StandardTestDispatcher(testScheduler))
            advanceUntilIdle()
            gone.acknowledge("any|key", true)
            assertEquals(DraftStatus.GONE, gone.state.value.status, "a tick must not bring a gone draft back to life")
            val open = model(LeaveEventInput(listOf(OccurrenceRequest(EXTENSION, mapOf("ext_from" to SlotValue.DateValue(from), "new_to" to SlotValue.DateValue(CalendarDate(2026, 7, 9)))))))
            val key = open.state.value.slip!!.findings.single().key
            open.close()
            open.acknowledge(key, true)
            assertFalse(open.state.value.slip!!.findings.single().ticked, "a closed model takes no ticks")
        }
}
