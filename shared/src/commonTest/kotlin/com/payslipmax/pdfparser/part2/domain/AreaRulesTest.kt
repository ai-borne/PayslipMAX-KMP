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
 * The re-grants of the area allowances after leave (Part II plan, phase 9): HAA (p.49 note ii), SCCIA (p.53 note 2), the field-area
 * allowances (p.55 note 2) and Transport Allowance (p.71 notes iii and v). The family holds conditions that decide which linked order
 * applies and two derive rules that date the Transport Allowance rows; the officer's own code is an answer, as with R&H. Synthetic dates only.
 */
class AreaRulesTest {
    private val haa = AreaRules.ANSWER_HAA_PREFIX + "GTDHH11"
    private val field = AreaRules.ANSWER_FIELD_PREFIX + "GTDCFAA"
    private val sccia = AreaRules.ANSWER_SCCIA_PREFIX + "GTDCIAFD"

    private fun condition(id: String) = assertNotNull(AreaRules.all.filterIsInstance<ConditionRule>().firstOrNull { it.id == id }, "condition $id")

    private fun derive(id: String) = assertNotNull(AreaRules.all.filterIsInstance<DeriveRule>().firstOrNull { it.id == id }, "derive rule $id")

    private fun casual(
        id: String,
        answer: String,
        days: Int,
        code: String = "GTDCL",
    ) = condition(id).holds(context(code, answers = setOf(answer), computed = mapOf("days" to number(days))))

    private fun absence(
        from: Pair<Int, Int>,
        to: Pair<Int, Int>,
        prefix: Pair<Int, Int>? = null,
        suffix: Pair<Int, Int>? = null,
        year: Int = 2026,
    ): Map<String, SlotValue> =
        buildMap {
            put("leave_from", date(from.first, from.second, year))
            put("leave_to", date(to.first, to.second, year))
            prefix?.let { put("prefix_first", date(it.first, it.second, year)) }
            suffix?.let { put("suffix_last", date(it.first, it.second, year)) }
        }

    private fun tranHolds(
        values: Map<String, SlotValue>,
        answer: String = AreaRules.ANSWER_TRAN_PREFIX + "GTDTRAN1",
        id: String = "tran_gtdtran1",
    ) = condition(id).holds(context("GTDAL", values = values, answers = setOf(answer)))

    private fun day(
        month: Int,
        day: Int,
        year: Int = 2026,
    ) = SlotValue.DateValue(CalendarDate(year, month, day))

    @Test
    fun theFamilyHoldsOneConditionPerCodeAndTheTransportStopAndTwoDeriveRulesForTheTransportDates() {
        assertEquals(listOf("GTDHH11", "GTDHH31", "GTDHH32"), AreaRules.haaCodes)
        assertEquals(listOf("GTDCIAFD", "GTDCIAMF", "GTDCIAPC"), AreaRules.scciaCodes)
        assertEquals(listOf("GTDHAFA", "GTDCFAA", "GTDCMFA"), AreaRules.fieldCodes)
        assertEquals(listOf("GTDTRAN1", "GTDTRAN2", "GTDTRAN3", "GTDTRAN4", "GTDTRAN5"), AreaRules.tranCodes)
        val ids = AreaRules.all.map { it.id }.toSet()
        val prefixes = mapOf("haa_" to AreaRules.haaCodes, "sccia_" to AreaRules.scciaCodes, "field_" to AreaRules.fieldCodes, "tran_" to AreaRules.tranCodes)
        for ((prefix, group) in prefixes) for (code in group) assertTrue(prefix + code.lowercase() in ids, code)
        assertTrue("tran_stop" in ids && "tran_stop_on" in ids && "tran_grant_on" in ids)
        assertEquals(12 + 14 + 1 + 2, AreaRules.all.size, "R&H's station and eleven codes stay; the new family adds one rule per code (14), the stop, and two derive rules")
        assertTrue(AreaRules.all.filter { it.id.startsWith("tran_") && it.id.endsWith("_on") }.all { it is DeriveRule })
        assertTrue(AreaRules.all.filter { it.id.startsWith("haa_") || it.id.startsWith("sccia_") || it.id.startsWith("field_") }.all { it is ConditionRule })
    }

    @Test
    fun haaIsRegrantedAfterAnyLeaveExceptACasualLeaveOfLessThan14Days() {
        // why: p.49 note ii. The day count lives in AreaRules and nowhere else; 13 is the last day with no re-grant, 14 the first with one.
        assertEquals(14, AreaRules.HAA_CASUAL_MIN_DAYS)
        assertFalse(casual("haa_gtdhh11", haa, 13), "casual leave of 13 days: the allowance never stopped")
        assertTrue(casual("haa_gtdhh11", haa, 14), "casual leave of 14 days: re-granted")
        assertTrue(casual("haa_gtdhh11", haa, 40, code = "GTDAL"), "annual leave is always followed by a re-grant, however short")
        assertTrue(casual("haa_gtdhh11", haa, 3, code = "GTDAL"))
    }

    @Test
    fun theCasualLeaveExceptionIsForCasualLeaveOnlyNotSpecialCasualLeave() {
        // why: the manual's ration notes list special casual leave (1.8) apart from casual leave (1.5); the exception says "Casual Leave".
        assertTrue(casual("haa_gtdhh11", haa, 5, code = "GTDSPCL"))
        assertTrue(casual("field_gtdcfaa", field, 5, code = "GTDSPCL"))
    }

    @Test
    fun theFieldAreaAllowancesAreRegrantedAfterAnyLeaveExceptACasualLeaveOfLessThan15Days() {
        // why: p.55 note 2: "(except when he rejoins from Casual Leave or hospitalization of less than 15 days)".
        assertEquals(15, AreaRules.FIELD_CASUAL_MIN_DAYS)
        assertFalse(casual("field_gtdcfaa", field, 14))
        assertTrue(casual("field_gtdcfaa", field, 15))
        assertTrue(casual("field_gtdcfaa", field, 2, code = "FURLEV"))
    }

    @Test
    fun sccAlwaysRegrantsOnRejoiningWithNoCasualLeaveThreshold() {
        // why: p.53 note 2 names leave, hospitalization and courses and no exception.
        assertTrue(casual("sccia_gtdciafd", sccia, 1))
        assertTrue(casual("sccia_gtdciafd", sccia, 1, code = "GTDAL"))
    }

    @Test
    fun anOfficerRegrantsOnlyUnderTheirOwnCodeAndNeverWithoutAnAnswer() {
        // why: the order carries the rate and the Corps notification of the officer's own area; another code's order would be wrong, an unasked one an invention.
        for (code in AreaRules.haaCodes) assertEquals(code == "GTDHH11", condition("haa_" + code.lowercase()).holds(context("GTDAL", answers = setOf(haa))), code)
        for (code in AreaRules.fieldCodes) assertEquals(code == "GTDCFAA", condition("field_" + code.lowercase()).holds(context("GTDAL", answers = setOf(field))), code)
        for (code in AreaRules.scciaCodes) assertEquals(code == "GTDCIAFD", condition("sccia_" + code.lowercase()).holds(context("GTDAL", answers = setOf(sccia))), code)
        assertFalse(condition("haa_gtdhh11").holds(context("GTDAL")), "no area named: nothing drafted")
        assertFalse(condition("haa_gtdhh11").holds(context("GTDAL", answers = setOf(AreaRules.ANSWER_HAA_PREFIX + "GTDHH99", AreaRules.ANSWER_HAA_PREFIX))), "an unknown code is no answer")
        assertFalse(condition("haa_gtdhh11").holds(context("GTDAL", answers = setOf(AreaRules.ANSWER_FIELD_PREFIX + "GTDHH11"))), "another area's prefix is no answer")
    }

    @Test
    fun theTransportRowsAreDraftedOnlyWhenTheAbsenceCoversAFullCalendarMonth() {
        // why: p.71 notes iii and v. 05 Jul to 03 Aug covers no whole month; 25 Jun to 05 Aug covers all of July.
        assertFalse(tranHolds(absence(7 to 5, 8 to 3)))
        assertTrue(tranHolds(absence(6 to 25, 8 to 5)))
        assertFalse(condition("tran_stop").holds(context("GTDAL", values = absence(7 to 5, 8 to 3), answers = setOf(AreaRules.ANSWER_TRAN_PREFIX + "GTDTRAN1"))))
        assertTrue(condition("tran_stop").holds(context("GTDAL", values = absence(6 to 25, 8 to 5), answers = setOf(AreaRules.ANSWER_TRAN_PREFIX + "GTDTRAN1"))))
    }

    @Test
    fun aMonthIsCoveredExactlyFromItsFirstDayToItsLastDayNotOneDayShort() {
        assertTrue(tranHolds(absence(7 to 1, 7 to 31)), "all of July")
        assertFalse(tranHolds(absence(7 to 2, 7 to 31)), "one day short at the start")
        assertFalse(tranHolds(absence(7 to 1, 7 to 30)), "one day short at the end")
        assertTrue(tranHolds(absence(2 to 1, 2 to 29, year = 2028)), "February of a leap year has 29 days")
        assertFalse(tranHolds(absence(2 to 1, 2 to 28, year = 2028)), "28 days is not a leap February")
        assertTrue(tranHolds(absence(2 to 1, 2 to 28)), "February of an ordinary year has 28 days")
    }

    @Test
    fun aPrefixAndASuffixCountAsAbsenceAndADateInsideTheLeaveAddsNothing() {
        assertTrue(tranHolds(absence(7 to 2, 7 to 29, prefix = 6 to 30, suffix = 7 to 31)), "the prefix and suffix make July whole")
        assertFalse(tranHolds(absence(7 to 2, 7 to 29, prefix = 7 to 10, suffix = 7 to 20)), "dates inside the leave add nothing")
        assertFalse(tranHolds(absence(7 to 2, 7 to 29)), "without them July is not whole")
    }

    @Test
    fun theAbsenceMayRunAcrossTheYearEndAndSeveralMonths() {
        assertTrue(tranHolds(mapOf("leave_from" to day(11, 15), "leave_to" to day(1, 5, 2027))), "December is whole")
        assertTrue(tranHolds(absence(7, 1, 8, 31)))
    }

    private fun absence(
        fromMonth: Int,
        fromDay: Int,
        toMonth: Int,
        toDay: Int,
    ) = absence(fromMonth to fromDay, toMonth to toDay)

    @Test
    fun aMissingOrImpossibleDateDraftsNothingAndNeverLoops() {
        // why: storage does not bound a date, and the calendar counts years one by one; a rule must check a stored date before it uses it.
        assertFalse(tranHolds(emptyMap()))
        assertFalse(tranHolds(mapOf("leave_from" to day(7, 1))), "no last day")
        assertFalse(tranHolds(mapOf("leave_from" to day(4, 31), "leave_to" to day(8, 5))), "31 April does not exist")
        assertFalse(tranHolds(mapOf("leave_from" to day(7, 1, 1), "leave_to" to day(8, 5, 9_999_999))), "years far outside the calendar")
        assertFalse(tranHolds(mapOf("leave_from" to day(7, 1), "leave_to" to day(8, 5, 2101))))
        assertNull(derive("tran_stop_on").derive(DeriveContext(OccurrenceRequest("GTDTRAN1", emptyMap(), emptySet()), mapOf("leave_from" to day(1, 1, 9_999_999), "leave_to" to day(8, 5)))))
    }

    @Test
    fun aCasualLeaveWhoseDaysAreNotKnownYetDraftsNoHaaOrFieldRegrantSoTheFormShowsItsBoxesOnlyOnceTheDatesSayTheyApply() {
        // why: until the dates are typed there is no day count, and a re-grant that might not apply must not be asked for.
        assertFalse(condition("haa_gtdhh11").holds(context("GTDCL", answers = setOf(haa))))
        assertFalse(condition("field_gtdcfaa").holds(context("GTDCL", answers = setOf(field))))
        assertTrue(condition("sccia_gtdciafd").holds(context("GTDCL", answers = setOf(sccia))), "SCCIA has no exception, so it never waits")
    }

    @Test
    fun anAbsenceEndingInTheLastMonthTheCalendarHoldsCannotBeDatedAndDraftsNothing() {
        // why: the grant would fall on 1 January 2101, a date the calendar refuses; a rule must answer "none", not invent a date outside it.
        val values = mapOf("leave_from" to day(10, 15, 2100), "leave_to" to day(12, 31, 2100))
        assertFalse(tranHolds(values))
        assertNull(derive("tran_grant_on").derive(DeriveContext(OccurrenceRequest("GTDTRAN1", values, emptySet()), values)))
    }

    @Test
    fun theTransportRowsAreDatedByTheMonthsThatWereLostNotByTheDaysAway() {
        // why: the Pay & Allowances handbook (2023, pp.124-125): the allowance is lost only for a calendar month wholly covered by leave, and a part month is paid in full.
        fun dates(values: Map<String, SlotValue>): Pair<CalendarDate?, CalendarDate?> {
            val context = DeriveContext(OccurrenceRequest("GTDTRAN1", values, emptySet()), values)
            return (derive("tran_stop_on").derive(context) as? SlotValue.DateValue)?.date to (derive("tran_grant_on").derive(context) as? SlotValue.DateValue)?.date
        }
        assertEquals(CalendarDate(2026, 7, 1) to CalendarDate(2026, 8, 1), dates(absence(6 to 25, 8 to 5)), "stopped from 1 July, paid again from 1 August")
        assertEquals(CalendarDate(2026, 7, 1) to CalendarDate(2026, 8, 1), dates(absence(6 to 25, 7 to 31)), "ends on the last day of July: still 1 August")
        assertEquals(CalendarDate(2026, 7, 1) to CalendarDate(2026, 9, 1), dates(absence(7 to 1, 8 to 31)))
        assertEquals(CalendarDate(2026, 12, 1) to CalendarDate(2027, 1, 1), dates(mapOf("leave_from" to day(11, 15), "leave_to" to day(1, 5, 2027))), "across the year end")
        assertEquals(CalendarDate(2028, 2, 1) to CalendarDate(2028, 3, 1), dates(absence(1 to 15, 3 to 5, year = 2028)), "a leap February")
        assertEquals(null to null, dates(absence(7 to 5, 8 to 3)), "no month lost: nothing to date")
    }

    @Test
    fun theTransportDatesStartFromThePrefixAndEndAtTheSuffix() {
        val values = absence(7 to 2, 7 to 29, prefix = 6 to 30, suffix = 7 to 31)
        val context = DeriveContext(OccurrenceRequest("GTDTRAN1", values, emptySet()), values)
        assertEquals(CalendarDate(2026, 7, 1), (derive("tran_stop_on").derive(context) as SlotValue.DateValue).date)
        assertEquals(CalendarDate(2026, 8, 1), (derive("tran_grant_on").derive(context) as SlotValue.DateValue).date)
    }

    @Test
    fun theFormAsksOneQuestionPerAreaAndEachConditionIsReadByExactlyOne() {
        val byId = AreaRules.questions.associateBy { it.id }
        assertEquals(listOf("rh_code", "haa_code", "sccia_code", "field_code", "tran_code"), AreaRules.questions.map { it.id })
        assertEquals(AreaRules.haaCodes, (byId.getValue("haa_code") as AnswerQuestion.OneOf).options)
        assertEquals(AreaRules.tranCodes, (byId.getValue("tran_code") as AnswerQuestion.OneOf).options)
        assertEquals("haa_code:", (byId.getValue("haa_code") as AnswerQuestion.OneOf).prefix)
        val conditions = AreaRules.all.filter { it is ConditionRule }.map { it.id }
        for (id in conditions) assertEquals(1, AreaRules.questions.count { id in it.rules }, id)
    }
}
