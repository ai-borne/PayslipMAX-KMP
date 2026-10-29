package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RuleContexts.date
import com.payslipmax.pdfparser.part2.domain.RuleContexts.occurrence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * WHY (DECISIONS Round 44, plan item 49; ODP p.55 note 2): the field-area allowances are re-granted on rejoining from a hospital stay or a
 * course, except after a hospital stay of fewer than 15 days. An order for a stay that short must not be published, and the app cannot
 * tell a short stay in hospital from a short course, so it warns the officer. The warning belongs to the draft started FROM the field code;
 * the same code linked from a leave order is the leave's business (any leave but a short casual one re-grants it) and must stay silent.
 */
class AbsenceRulesTest {
    private val rule = assertNotNull(AbsenceRules.all.filterIsInstance<CheckRule>().firstOrNull { it.id == AbsenceRules.FIELD_SHORT_ABSENCE })

    /** The first day is 1 October and the last is the [days]th, so the days away, both counted, are [days] (at most 31). A null [days] types no dates. */
    private fun away(days: Int?) = days?.let { mapOf("leave_from" to date(10, 1), "leave_to" to date(10, it)) } ?: emptyMap()

    private fun primary(
        code: String,
        days: Int?,
    ) = RuleContext(OccurrenceRequest(code, away(days)), occurrence(code), emptyList())

    @Test
    fun theFamilyHoldsTheOneCheck() {
        assertEquals(setOf("field_short_absence"), AbsenceRules.all.map { it.id }.toSet())
    }

    @Test
    fun anAbsenceOfFewerThan15DaysIsWarnedForEveryFieldAreaCode() {
        for (code in AreaRules.fieldCodes) {
            assertTrue(rule.violated(primary(code, 1)), "$code, 1 day")
            assertTrue(rule.violated(primary(code, AreaRules.FIELD_CASUAL_MIN_DAYS - 1)), "$code, 14 days")
        }
    }

    @Test
    fun fifteenDaysIsTheFirstDayThatIsNotWarnedAndTheNumberHasOneHome() {
        assertEquals(15, AreaRules.FIELD_CASUAL_MIN_DAYS, "the manual's 15 (p.55 note 2) is read from the one constant")
        for (code in AreaRules.fieldCodes) {
            assertFalse(rule.violated(primary(code, AreaRules.FIELD_CASUAL_MIN_DAYS)), "$code, 15 days")
            assertFalse(rule.violated(primary(code, 31)), "$code, 31 days")
        }
    }

    @Test
    fun nothingIsSaidBeforeTheDaysAreKnown() {
        assertFalse(rule.violated(primary("GTDHAFA", null)), "the dates are not typed yet")
    }

    @Test
    fun aLastDayBeforeTheFirstDayIsNotCountedAsAShortStay() {
        val backwards = RuleContext(OccurrenceRequest("GTDHAFA", mapOf("leave_from" to date(10, 9), "leave_to" to date(10, 1))), occurrence("GTDHAFA"), emptyList())
        assertFalse(rule.violated(backwards), "the form names the bad dates; the check says nothing on top")
    }

    @Test
    fun aDateTheCalendarCannotHoldIsNotCountedEither() {
        // why: 31 November is not a day; read unchecked it would sit on 1 December and make a 5-day stay.
        val bad = RuleContext(OccurrenceRequest("GTDHAFA", mapOf("leave_from" to date(11, 31), "leave_to" to date(12, 5))), occurrence("GTDHAFA"), emptyList())
        assertFalse(rule.violated(bad))
    }

    @Test
    fun theSameCodeLinkedFromALeaveIsNeverWarned() {
        // why: annual leave of 10 days re-grants the field allowance; the exception is for a hospital stay or a casual leave, judged elsewhere.
        val linked = RuleContext(OccurrenceRequest("GTDAL", away(10)), occurrence("GTDHAFA"), emptyList())
        assertFalse(rule.violated(linked))
    }

    @Test
    fun onlyTheFieldAreaCodesAreJudgedNotHaaOrSccia() {
        // why: p.49 note ii and p.53 note 2 name no hospital exception, so a short absence re-grants HAA and SCCIA.
        for (code in AreaRules.haaCodes + AreaRules.scciaCodes) assertFalse(rule.violated(primary(code, 3)), code)
    }
}
