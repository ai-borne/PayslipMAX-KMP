package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.AnswerQuestion
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2LookupIndex
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.FieldValue
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveBankView
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftStateBuilder
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The leave form and its words on the real shipped bundle, run on each platform (`GuideBundleAndroidContractTest`, and timed on
 * Kotlin/Native by `GuideLoaderIosPerfTest`): every check and question the bundle can raise has its words in `Part2Strings`, and the
 * form built from the shipped templates can always be completed into a slip, so no code is offered that cannot be drafted.
 */
object Part2DraftContract {
    private fun builder(bundle: GuideBundle) = LeaveDraftStateBuilder(assertNotNull(bundle.part2), Part2Rules.shipped())

    fun assertEveryCheckAndQuestionHasItsWords(bundle: GuideBundle) {
        val section = assertNotNull(bundle.part2)
        val checks = section.rules.filter { it.kind == "check" }.map { it.id }
        assertTrue(checks.isNotEmpty(), "a loop over nothing would pass whatever the content")
        for (id in checks) assertNotNull(Part2Strings.finding(id), "check $id has no wording in Part2Strings, so the officer would see the fallback")
        val questions = Part2Rules.shipped().questions
        assertTrue(questions.isNotEmpty())
        for (question in questions) assertTrue(Part2Strings.hasQuestion(question.id), "question ${question.id} has no wording in Part2Strings")
    }

    /** Answers every question and fills every field the form shows, in one pass over each offered code; the slip must then draft with nothing missing or refused. */
    fun assertTheFormCompletesEveryOfferedCode(bundle: GuideBundle) {
        val builder = builder(bundle)
        val offered = Part2LookupIndex(assertNotNull(bundle.part2)).draftOptions()
        assertEquals(41, offered.size, "the Leave release offers the eight leave and rejoining codes, the two cancellation codes, the 22 serials 5.6-5.22 and 5.26-5.30 and the nine area re-grants of the absence case (Phase 12)")
        for (option in offered) {
            var event = LeaveEventEditor.start(option.code)
            for (question in builder.questionsFor(option.code)) {
                event =
                    when (question) {
                        is AnswerQuestion.YesNo -> LeaveEventEditor.setYesNo(event, 0, question.answer, true)
                        is AnswerQuestion.OneOf -> LeaveEventEditor.chooseOne(event, 0, question.prefix, question.options.first())
                    }
            }
            var day = 1
            for (field in builder.build(event, emptySet(), false).sections[0].fields) {
                event = LeaveEventEditor.setValue(event, 0, field.slotId, valueFor(field.value, day++, field.slotId))
            }
            val state = builder.build(event, emptySet(), false)
            assertEquals(emptyList(), state.missing, option.code)
            assertTrue(state.sections[0].fields.all { it.error == null }, "${option.code}: ${state.sections[0].fields.mapNotNull { it.error }}")
            assertNotNull(state.slip, "${option.code} drafts a slip once every field is answered")
        }
    }

    /**
     * The Leave Bank fill is offered on exactly the two year-end forms the rule families register it for, and on no other offered code; and
     * the encashment's break-up is a year list in a box of several lines (plan, phase 8b). Run on each platform.
     */
    fun assertTheLeaveBankFillIsOfferedOnTheYearEndFormsOnly(bundle: GuideBundle) {
        val builder = builder(bundle)
        val offered = Part2LookupIndex(assertNotNull(bundle.part2)).draftOptions()
        val view = LeaveBankView(emptyList(), emptyMap())
        val filled = offered.map { it.code }.filter { code -> builder.build(LeaveEventEditor.start(code), emptySet(), false, leaveBank = view).sections[0].fill != null }
        assertEquals(listOf("ACCLVE", "ENCASH"), filled.sorted(), "the fill is registered per code by the rule families, not decided by a screen")
        for (code in offered.map { it.code }) assertNull(builder.build(LeaveEventEditor.start(code), emptySet(), false).sections[0].fill, "$code: no fill without the Leave Bank")
        val breakup = builder.build(LeaveEventEditor.start("ENCASH"), emptySet(), false).sections[0].fields.single { it.slotId == "breakup" }
        assertEquals(FieldValue.Years(""), breakup.value)
    }

    /** The builder over the shipped section and rules; build it once and rebuild many times when timing. */
    internal fun newBuilder(bundle: GuideBundle) = builder(bundle)

    /** The state of the 30-day annual leave with ration and R&H, rebuilt as the form does on every change. Returns the rows on the slip (seven). */
    internal fun rowsOf(builder: LeaveDraftStateBuilder): Int {
        val event = LeaveEventInput(listOf(OccurrenceRequest("GTDAL", Part2BatchOneContract.values, Part2BatchOneContract.answers)))
        return assertNotNull(builder.build(event, emptySet(), false).slip).orders.sumOf { it.rows.size }
    }

    private fun valueFor(
        value: FieldValue,
        day: Int,
        slotId: String,
    ): SlotValue? =
        when (value) {
            // The sanction of a field-area grant must end after the rejoining day, so it is the last day of the year.
            is FieldValue.Date -> SlotValue.DateValue(if (slotId == "sanction_to") CalendarDate(2026, 12, 31) else CalendarDate(2026, 8, day))
            is FieldValue.Digits -> SlotValue.NumberValue(1)
            is FieldValue.Words -> SlotValue.TextValue("plain")
            is FieldValue.Years -> SlotValue.TextValue("2011 30; 2013 20")
            is FieldValue.Reference -> SlotValue.RefValue(OrderRef("N-1/26", CalendarDate(2026, 3, 2)))
        }
}
