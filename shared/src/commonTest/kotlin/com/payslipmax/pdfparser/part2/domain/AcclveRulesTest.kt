package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.RuleContexts.context
import com.payslipmax.pdfparser.part2.domain.RuleContexts.date
import com.payslipmax.pdfparser.part2.domain.RuleContexts.number
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The year-end accumulation as the family works it out and checks it (Part II plan, phase 8; ODP-2026 pp.133-134). The derived texts are
 * the manual's own sentence fragments with the officer's figures in them, so a wrong figure would print in an order the clerk types into HRMS.
 */
class AcclveRulesTest {
    private fun derive(id: String) = assertNotNull(AcclveRules.all.filterIsInstance<DeriveRule>().firstOrNull { it.id == id }, "derive rule $id")

    private fun check(id: String) = assertNotNull(AcclveRules.all.filterIsInstance<CheckRule>().firstOrNull { it.id == id }, "check $id")

    private fun figures(
        previous: Int,
        ltc: Int = 0,
        accrued: Int = 0,
        year: Int = 2025,
    ): Map<String, SlotValue> = mapOf("year" to number(year), "prev_total" to number(previous), "ltc_days" to number(ltc), "accrued" to number(accrued))

    private fun value(
        id: String,
        values: Map<String, SlotValue>,
    ) = derive(id).derive(DeriveContext(OccurrenceRequest("X", values), values))

    @Test
    fun theFamilyOffersFourDeriveRulesAndFourChecksAndOneQuestion() {
        assertEquals(setOf("acclve_prev_phrase", "acclve_prev_data", "acclve_sum_text", "acclve_total"), AcclveRules.all.filterIsInstance<DeriveRule>().map { it.id }.toSet())
        assertEquals(setOf("acclve_accrual_30", "acclve_year_60", "acclve_over_cap", "acclve_too_early"), AcclveRules.all.filterIsInstance<CheckRule>().map { it.id }.toSet())
        assertEquals(listOf("acclve_retiring"), AcclveRules.questions.map { it.id })
        assertEquals(setOf("acclve_too_early"), AcclveRules.questions.single().rules)
    }

    @Test
    fun theManualsWorkedExampleReadsAsTheManualPrintsIt() {
        // why: p.133 - (a) "380 (Restricted to 300) as on ...", Data 3 "300-2013", and (d) "(300-10+00) days" equal to 290.
        val example = figures(previous = 380, ltc = 10, accrued = 0, year = 2014)
        assertEquals(SlotValue.TextValue("380 (Restricted to 300) as on 31/12/2013"), value("acclve_prev_phrase", example))
        assertEquals(SlotValue.TextValue("300-2013"), value("acclve_prev_data", example))
        assertEquals(SlotValue.TextValue("(300-10+0)"), value("acclve_sum_text", example))
        assertEquals(SlotValue.NumberValue(290), value("acclve_total", example))
    }

    @Test
    fun aPreviousTotalWithinTheCapPrintsNoRestrictionAndANewYearRollsTheDateBack() {
        val real = figures(previous = 282, ltc = 0, accrued = 19, year = 2024)
        assertEquals(SlotValue.TextValue("282 as on 31/12/2023"), value("acclve_prev_phrase", real))
        assertEquals(SlotValue.NumberValue(301), value("acclve_total", real), "the figure a real statement shows")
        assertEquals(SlotValue.TextValue("(282-0+19)"), value("acclve_sum_text", real))
        assertEquals(SlotValue.TextValue("300 as on 31/12/2024"), value("acclve_prev_phrase", figures(300, year = 2025)), "300 itself is not restricted")
    }

    @Test
    fun aFigureNotAnsweredYetWorksNothingOutAndANonNumberIsNotMistakenForOne() {
        for (id in listOf("acclve_prev_phrase", "acclve_prev_data", "acclve_sum_text", "acclve_total")) {
            assertNull(value(id, emptyMap()), id)
            assertNull(value(id, figures(10).filterKeys { it != "year" && it != "ltc_days" }), "$id: with the year and the LTC days missing")
        }
        assertNull(value("acclve_total", figures(10) + ("accrued" to SlotValue.TextValue("five"))))
        assertEquals(SlotValue.TextValue("10 as on 31/12/1998"), value("acclve_prev_phrase", figures(10, year = 1999)), "any plausible year drafts: the date is printed, not counted")
        assertEquals(SlotValue.TextValue("10 as on 31/12/0000"), value("acclve_prev_phrase", figures(10, year = 1)), "the form tries any number: a hidden worked-out slot must never refuse the slip")
        assertEquals(SlotValue.TextValue("10-0"), value("acclve_prev_data", figures(10, year = 0)), "the year before year 0 stays 0")
    }

    @Test
    fun theYearsAccumulationIsAtMostThirtyDaysAndWithTheLeaveUsedAtMostSixty() {
        // why: notes 6 and 5 (p.134): both are limits the manual states, so an order breaking one is queried.
        val thirty = check("acclve_accrual_30")
        assertFalse(thirty.violated(context("X", figures(0, accrued = 30))))
        assertTrue(thirty.violated(context("X", figures(0, accrued = 31))))
        val sixty = check("acclve_year_60")

        fun year(
            accrued: Int,
            used: Int?,
        ) = context("X", figures(0, accrued = accrued) + listOfNotNull(used?.let { "al_used" to number(it) }))
        assertFalse(sixty.violated(year(30, 30)), "30 + 30 is exactly 60")
        assertTrue(sixty.violated(year(30, 31)))
        assertTrue(sixty.violated(year(20, 41)))
        assertFalse(sixty.violated(year(30, null)), "no leave used typed counts as none")
        assertFalse(sixty.violated(context("X")), "nothing accumulated yet is no breach")
    }

    @Test
    fun aTotalAbove300IsWarnedAboutBecauseOnly300CanBeEncashed() {
        val rule = check("acclve_over_cap")
        assertFalse(rule.violated(context("X", computed = mapOf("total" to number(300)))))
        assertTrue(rule.violated(context("X", computed = mapOf("total" to number(301)))))
        assertFalse(rule.violated(context("X")), "no total worked out yet")
    }

    @Test
    fun theOrderIsAcknowledgedWhenTheDeviceDayIsBeforeTheFirstOfMarchUnlessTheOfficerIsRetiring() {
        // why: note 7 (p.134) - on or after 1 March of the following year, or on the date of retirement.
        val rule = check("acclve_too_early")

        fun on(
            today: CalendarDate?,
            vararg answers: String,
        ) = rule.violated(context("X", mapOf("year" to number(2025)), answers.toSet(), today = today))
        assertTrue(on(CalendarDate(2026, 2, 28)))
        assertFalse(on(CalendarDate(2026, 3, 1)))
        assertFalse(on(CalendarDate(2026, 2, 28), AcclveRules.ANSWER_RETIRING))
        assertFalse(on(null))
        assertFalse(rule.violated(context("X", today = CalendarDate(2026, 2, 28))), "no year typed judges nothing")
    }

    @Test
    fun theTotalOfADraftedRequestIsTheSameFigureTheSlipPrintsAndNullWhileAFigureIsMissing() {
        assertEquals(290, AcclveRules.totalOf(OccurrenceRequest("X", figures(380, 10, 0, 2014))))
        assertNull(AcclveRules.totalOf(OccurrenceRequest("X", figures(380).filterKeys { it != "accrued" })))
    }

    @Test
    fun theLeaveUsedInAYearIsTheDaysOfItsAnnualGrantsExtensionsAndStudyExtensionsAndNoOther() {
        // why: note 5 - leave availed, extended and extended for study together with the accumulation may not pass 60.
        fun grant(
            code: String,
            from: Int,
            to: Int,
            year: Int,
        ) = OccurrenceRequest(code, mapOf("leave_from" to date(7, from, year), "leave_to" to date(7, to, year), "year" to number(year)))
        val requests = listOf(grant("GTDAL", 1, 10, 2025), grant("EXTNAL", 11, 15, 2025), grant("STDLVAL", 16, 18, 2025), grant("GTDAL", 1, 20, 2024), grant("GTDCL", 1, 9, 2025))
        assertEquals(10 + 5 + 3, AcclveRules.alUsed(2025, requests))
        assertEquals(20, AcclveRules.alUsed(2024, requests))
        assertEquals(0, AcclveRules.alUsed(2023, requests))
        assertEquals(0, AcclveRules.alUsed(2025, listOf(OccurrenceRequest("GTDAL", mapOf("year" to number(2025))))), "a grant with no dates yet counts nothing")
    }
}
