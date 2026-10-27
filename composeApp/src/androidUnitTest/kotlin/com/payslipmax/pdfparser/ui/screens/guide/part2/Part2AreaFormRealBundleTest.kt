package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.part2.domain.AreaRules
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.FieldValue
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftStateBuilder
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The area questions of the leave form on the shipped templates (Part II plan, phase 9): the five questions and their option words, the
 * Transport Allowance fields that wait for dates covering a whole month, and the prefill of an earlier draft's Corps notification details
 * for HAA, SCCIA and the field areas, which brings back text and references and never a date (the sanction period of a field-area grant is a date).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class Part2AreaFormRealBundleTest {
    private val section = checkNotNull(kotlinx.coroutines.runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }.part2)
    private val builder = LeaveDraftStateBuilder(section, Part2Rules.shipped())

    private fun day(
        month: Int,
        day: Int,
    ) = SlotValue.DateValue(CalendarDate(2026, month, day))

    private fun annual(
        vararg answers: String,
        values: Map<String, SlotValue> = emptyMap(),
    ) = LeaveEventInput(listOf(OccurrenceRequest("GTDAL", values, answers.toSet())))

    @Test
    fun annualLeaveAsksTheFiveAreaQuestionsAndTheTransportOptionsReadAsFiveCategories() {
        val questions = builder.build(annual(), emptySet(), false).sections[0].questions
        assertEquals(listOf("ration", "rh_code", "haa_code", "sccia_code", "field_code", "tran_code", "ncc_reemployed", "reemployed_same_year"), questions.map { it.id })
        val tran = questions.first { it.id == "tran_code" }
        assertEquals(AreaRules.tranCodes, tran.options.map { it.code })
        assertEquals(5, tran.options.map { it.name }.toSet().size, "the five categories share one printed name, so each is worded apart")
        assertEquals("Category 3: with disability, lower rate", tran.options[2].name)
        assertEquals("Granted HAA for Cat-III @ R1H1 rate", questions.first { it.id == "haa_code" }.options.first().name, "the other areas use the manual's own serial name")
    }

    @Test
    fun theTransportFieldsAppearOnlyOnceTheDatesCoverAWholeCalendarMonth() {
        fun ids(values: Map<String, SlotValue>) = builder.build(annual(AreaRules.ANSWER_TRAN_PREFIX + "GTDTRAN1", values = values), emptySet(), false).sections[0].fields.map { it.slotId }
        assertTrue("tran_station" !in ids(emptyMap()), "no dates yet")
        assertTrue("tran_station" !in ids(mapOf("leave_from" to day(7, 5), "leave_to" to day(8, 3))), "05 Jul to 03 Aug covers no whole month")
        assertTrue("tran_station" in ids(mapOf("leave_from" to day(6, 25), "leave_to" to day(8, 5))), "25 Jun to 05 Aug covers July")
    }

    private fun draftOf(
        code: String,
        answer: String,
        values: Map<String, SlotValue>,
    ) = Part2Draft("old", LeaveEventInput(listOf(OccurrenceRequest(code, values, setOf(answer)))), emptyMap(), 1L, 5L)

    private fun notif(no: String) = SlotValue.RefValue(OrderRef(no, CalendarDate(2026, 3, 2)))

    @Test
    fun theCorpsNotificationDetailsOfAnEarlierDraftWithTheSameAreaCodeAreBroughtBackButNeverADate() =
        runTest {
            val cases =
                listOf(
                    Triple("haa_code", "GTDHH31", mapOf("station" to SlotValue.TextValue("ZZ01"), "haa_corps" to SlotValue.TextValue("Alpha"), "haa_notif" to notif("N-1/26"), "haa_serial" to SlotValue.TextValue("12"))),
                    Triple("sccia_code", "GTDCIAMF", mapOf("operation" to SlotValue.TextValue("OP TESTONE"), "sccia_corps" to SlotValue.TextValue("Bravo"), "sccia_notif" to notif("N-2/26"), "sccia_serial" to SlotValue.TextValue("7"))),
                    Triple(
                        "field_code",
                        "GTDHAFA",
                        mapOf("location" to SlotValue.TextValue("Testpur"), "field_corps" to SlotValue.TextValue("Charlie"), "field_notif" to notif("N-3/26"), "field_serial" to SlotValue.TextValue("41"), "sanction_to" to day(12, 31)),
                    ),
                )
            for ((question, code, values) in cases) {
                val prefix = (AreaRules.questions.first { it.id == question } as com.payslipmax.pdfparser.part2.domain.AnswerQuestion.OneOf).prefix
                val repo = FakePart2Repository()
                repo.saveDraft(draftOf("GTDAL", prefix + code, values + ("leave_from" to day(7, 6))))
                val vm = LeaveDraftViewModel("new", section, repo, LeaveEventEditor.start("GTDAL"), Part2Rules.shipped(), { 1L }, StandardTestDispatcher(testScheduler))
                vm.choose(0, question, code)
                advanceUntilIdle()
                val fields = vm.state.value.sections[0].fields.associate { it.slotId to it.value }
                for ((slot, value) in values) {
                    when (value) {
                        is SlotValue.TextValue -> assertEquals(FieldValue.Words(value.text), fields[slot], "$code $slot")
                        is SlotValue.RefValue -> assertEquals(FieldValue.Reference(value.ref.orderNo, value.ref.date), fields[slot], "$code $slot")
                        else -> assertEquals(FieldValue.Date(null), fields[slot], "$code $slot: a date is the officer's to enter again")
                    }
                }
                assertEquals(FieldValue.Date(null), fields["leave_from"], "$code: the dates of the last leave are never brought back")
            }
        }
}
