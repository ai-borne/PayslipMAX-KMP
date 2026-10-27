package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.RealPart2.section
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What the leave form asks, read from the real shipped templates and rule families. The form is derived, not written per code, so these
 * tests are the proof that the officer is asked everything an order needs, nothing else, and that answering every question completes a slip.
 */
class Part2FormRealBundleTest {
    private val rules = Part2Rules.shipped()
    private val planner = LeaveFormPlanner(RealPart2.engine, rules)
    private val index = Part2LookupIndex(section)
    private val options = index.draftOptions(sameRibbonAs = "GTDAL").map { it.code }

    private fun request(
        code: String,
        vararg answers: String,
    ) = OccurrenceRequest(code, emptyMap(), answers.toSet())

    private fun ids(request: OccurrenceRequest) = planner.fields(request).map { it.slot.id }

    @Test
    fun theLeaveCasesOfferTheEightLeaveAndRejoiningCodesThenTheTwentyTwoOfTheRestOfChapterFive() {
        assertEquals(listOf("GTDAL", "GTDALCF", "EXTNAL", "GTDCL", "GTDSPCL", "REJOINAL", "VOLREJ", "RECALL"), options.take(8))
        assertEquals(22, options.size - 8)
        assertEquals(listOf("FURLEV", "EXTNFUR", "STDLEV", "STDLVAL", "STDLVFUR", "EOL", "TERLEV", "MTRNLV", "SPMTRNLV", "GTDCCL", "CHADPLVE", "PTRNLV", "PATLCHAD", "OSL", "REJOSL", "AWL", "REJAWL", "ACCLVE", "LVELTCAD", "LVELTC", "CLVLTCAD", "ENCASH"), options.drop(8))
    }

    @Test
    fun theCancellationCaseOffersTheTwoCancelCodesAndNeverTheLeaveOnes() {
        assertEquals(listOf("CANCEL", "CANCELS"), index.draftOptions(sameRibbonAs = "CANCEL").map { it.code })
        assertEquals(listOf("CANCEL", "CANCELS"), index.draftOptions(sameRibbonAs = "CANCELS").map { it.code })
        assertEquals(false, "CANCEL" in options || "CANCELS" in options, "a leave slip never offers a cancellation")
    }

    @Test
    fun annualLeaveAlonePromptsForItsDatesTheYearAndTheLeaveAlreadyTaken() {
        assertEquals(listOf("prefix_first", "leave_from", "leave_to", "suffix_last", "year", "earlier_al", "retire_on"), ids(request("GTDAL")))
        val required = planner.fields(request("GTDAL")).associate { it.slot.id to it.required }
        assertEquals(mapOf("prefix_first" to false, "leave_from" to true, "leave_to" to true, "suffix_last" to false, "year" to true, "earlier_al" to false, "retire_on" to false), required)
    }

    @Test
    fun rationStrengthAddsTheLeaveKindTheRationOrdersPrintAndNothingElse() {
        assertEquals(ids(request("GTDAL")) + "leave_kind", ids(request("GTDAL", RationRules.ANSWER_ON_RATION)))
    }

    @Test
    fun anRhStationAddsTheStopsStationThenTheGrantsAreaDetailsInOrder() {
        val rh = request("GTDAL", AreaRules.ANSWER_RH_PREFIX + "GTDRH12")
        assertEquals(ids(request("GTDAL")) + listOf("station", "corps", "notif", "serial"), ids(rh))
        assertEquals(false, planner.fields(rh).first { it.slot.id == "notif" }.required, "an order reference is never required")
    }

    @Test
    fun rationAndRhTogetherAskEachSharedQuestionOnce() {
        val both = request("GTDAL", RationRules.ANSWER_ON_RATION, AreaRules.ANSWER_RH_PREFIX + "GTDRH12")
        val all = ids(both)
        assertEquals(all.distinct(), all)
        assertEquals(setOf("leave_kind", "station", "corps", "notif", "serial"), (all - ids(request("GTDAL")).toSet()).toSet())
    }

    @Test
    fun annualLeaveAsksTheRationAndRhQuestionsAndTheOthersAskWhatTheirLinksNeed() {
        val areas = listOf("haa_code", "sccia_code", "field_code", "tran_code")
        assertEquals(listOf("ration", "rh_code") + areas + listOf("ncc_reemployed", "reemployed_same_year"), planner.questions("GTDAL").map { it.id })
        assertEquals(listOf("ration", "rh_code") + areas, planner.questions("GTDALCF").map { it.id })
        assertEquals(listOf("ration") + areas, planner.questions("GTDCL").map { it.id })
        assertEquals(listOf("ration") + areas, planner.questions("GTDSPCL").map { it.id })
        assertEquals(listOf("ncc_reemployed", "reemployed_same_year", "earlier_allowances"), planner.questions("EXTNAL").map { it.id })
        for (code in listOf("VOLREJ", "RECALL")) assertEquals(listOf("earlier_allowances"), planner.questions(code).map { it.id }, code)
        assertEquals(emptyList(), planner.questions("REJOINAL"))
    }

    @Test
    fun everyConditionARealLinkNamesIsAnsweredByExactlyOneQuestion() {
        // why: a link whose condition no question can set would never apply, so its orders could never be drafted from the form.
        val linked = section.templates.flatMap { t -> t.links.mapNotNull { it.condition } }.toSet()
        assertTrue(linked.isNotEmpty())
        for (condition in linked) assertEquals(1, rules.questions.count { condition in it.rules }, condition)
        for (question in rules.questions) for (id in question.rules) assertTrue(rules.condition(id) != null || rules.check(id) != null, "${question.id} names $id, which is neither a condition nor a check the app runs")
    }

    @Test
    fun everyAnswerAQuestionCanGiveAddsTheOrdersItPromises() {
        val ration = rules.questions.filterIsInstance<AnswerQuestion.YesNo>().single { it.id == "ration" }
        assertTrue("SORS" in planner.templates(request("GTDAL", ration.answer)).map { it.code })
        val areas = rules.questions.filterIsInstance<AnswerQuestion.OneOf>().associateBy { it.id }
        assertEquals(listOf("rh_code", "haa_code", "sccia_code", "field_code", "tran_code"), areas.keys.toList())
        assertEquals(AreaRules.rhCodes, areas.getValue("rh_code").options)
        for (option in AreaRules.rhCodes) {
            val codes = planner.templates(request("GTDAL", AreaRules.ANSWER_RH_PREFIX + option)).map { it.code }
            assertTrue(option in codes && "STPRH" in codes, "$option adds its own grant and the stop")
        }
        for (id in listOf("haa_code", "sccia_code", "field_code")) {
            val area = areas.getValue(id)
            for (option in area.options) assertTrue(option in planner.templates(request("GTDAL", area.prefix + option)).map { it.code }, "$option adds its own grant")
        }
        // the Transport Allowance is drafted only once the dates cover a whole month, so the form shows its fields from then on
        val tran = areas.getValue("tran_code")
        val longLeave = mapOf("leave_from" to SlotValue.DateValue(CalendarDate(2026, 6, 25)), "leave_to" to SlotValue.DateValue(CalendarDate(2026, 8, 5)))
        for (option in tran.options) {
            val codes = planner.templates(request("GTDAL", tran.prefix + option).copy(values = longLeave)).map { it.code }
            assertTrue(option in codes && "STPTRAN" in codes, "$option adds its own grant and the stop")
            assertEquals(false, option in planner.templates(request("GTDAL", tran.prefix + option)).map { it.code }, "$option waits for dates that cover a month")
        }
    }

    @Test
    fun answeringEveryQuestionAndEveryFieldAlwaysDraftsASlip() {
        // why: the form asks only for what the engine reads. If it asked for less, the officer could never finish; the engine would refuse.
        for (code in options) {
            val withAnswers =
                rules.questions.fold(OccurrenceRequest(code)) { r, q ->
                    if (q.id !in planner.questions(code).map { it.id }) {
                        r
                    } else {
                        when (q) {
                            is AnswerQuestion.YesNo -> r.copy(answers = r.answers + q.answer)
                            is AnswerQuestion.OneOf -> r.copy(answers = r.answers + (q.prefix + q.options.first()))
                        }
                    }
                }
            var day = 1
            val values =
                planner.fields(withAnswers).associate { field ->
                    field.slot.id to
                        when (field.slot.kind) {
                            "date" -> SlotValue.DateValue(CalendarDate(2026, 8, day++))
                            "number" -> SlotValue.NumberValue(1)
                            "ref" -> SlotValue.RefValue(OrderRef("N-1/26", CalendarDate(2026, 3, 2)))
                            "years" -> SlotValue.TextValue("2011 30; 2013 20")
                            else -> SlotValue.TextValue("plain")
                        }
                }
            val result = RealPart2.engine.draft(LeaveEventInput(listOf(withAnswers.copy(values = values))))
            assertIs<DraftResult.Drafted>(result, "$code: ${(result as? DraftResult.Refused)?.issues}")
        }
    }
}
