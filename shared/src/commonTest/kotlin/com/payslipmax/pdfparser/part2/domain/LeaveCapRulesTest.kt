package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RuleContexts.context
import com.payslipmax.pdfparser.part2.domain.RuleContexts.date
import com.payslipmax.pdfparser.part2.domain.RuleContexts.number
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The two annual-leave entitlements the 60-day check did not know (Phase 11 item 11, p.115 notes 3 and 13; Round 15): NCC and re-employed
 * officers and the year of retirement. Each test says what an audit would reject. Dates are synthetic.
 */
class LeaveCapRulesTest {
    private val rule = assertNotNull(LeaveRules.all.filterIsInstance<CheckRule>().firstOrNull { it.id == LeaveRules.AL_YEAR_CAP })

    private fun over(
        days: Int,
        answers: Set<String> = emptySet(),
        retire: SlotValue? = null,
        year: Int = 2026,
        earlier: Int = 0,
    ): Boolean {
        val values = mapOf("year" to number(year), "earlier_al" to number(earlier)) + listOfNotNull(retire?.let { "retire_on" to it })
        return rule.violated(context("X", values, answers, computed = mapOf("days" to number(days))))
    }

    @Test
    fun anNccOrReEmployedOfficerGetsThirtyDaysAYearNotSixty() {
        // why: note 3 - NCC officers and re-employed officers get only 30 days of annual and extended annual leave in a year.
        val ncc = setOf(LeaveRules.ANSWER_NCC_REEMPLOYED)
        assertFalse(over(30, ncc), "exactly 30")
        assertTrue(over(31, ncc))
        assertTrue(over(20, ncc, earlier = 11), "11 earlier + 20 now is 31")
        assertFalse(over(31), "an ordinary officer may take 31")
    }

    @Test
    fun aReEmployedOfficerWhoRetiredAndWasReEmployedInTheSameYearGetsSixty() {
        // why: note 3 - such an officer may have 60 days in all, the 30 included.
        val both = setOf(LeaveRules.ANSWER_NCC_REEMPLOYED, LeaveRules.ANSWER_REEMPLOYED_SAME_YEAR)
        assertFalse(over(60, both))
        assertTrue(over(61, both))
        assertFalse(over(60, setOf(LeaveRules.ANSWER_REEMPLOYED_SAME_YEAR)), "the same-year answer alone still means re-employed with the larger allowance")
    }

    @Test
    fun theYearOfRetirementIsCutByTheRetirementDateAsNote13Says() {
        // why: note 13 - 31 Jan and February before the last day: 30 days; the last day of February: 58 (or 59 in a leap year); March on: 60.
        assertFalse(over(30, retire = date(1, 31)), "31 January: 30")
        assertTrue(over(31, retire = date(1, 31)))
        assertFalse(over(30, retire = date(2, 10)), "February before the last day: 30")
        assertTrue(over(31, retire = date(2, 10)))
        assertFalse(over(58, retire = date(2, 28)), "28 February 2026, not a leap year: 58")
        assertTrue(over(59, retire = date(2, 28)))
        assertFalse(over(30, retire = date(2, 28, 2028), year = 2028), "28 February 2028 is not the last day: 30")
        assertTrue(over(31, retire = date(2, 28, 2028), year = 2028))
        assertFalse(over(59, retire = date(2, 29, 2028), year = 2028), "29 February 2028, a leap year: 59")
        assertTrue(over(60, retire = date(2, 29, 2028), year = 2028))
        assertFalse(over(60, retire = date(3, 1)), "March: the full 60")
        assertTrue(over(61, retire = date(3, 1)))
        assertFalse(over(60, retire = date(1, 30)), "before 31 January the note says nothing, so the ordinary 60 stands")
    }

    @Test
    fun theRetirementEntitlementOnlyAppliesToTheYearOfRetirementAndNeverToAnythingNotARealDate() {
        // why: the cut is for the calendar year the officer retires in; a leave debited to another year, or a date the calendar rejects, is judged as usual.
        assertFalse(over(45, retire = date(2, 10, 2025)), "retiring in 2025, leave of 2026")
        assertTrue(over(45, retire = date(2, 10)), "retiring in 2026, leave of 2026")
        assertFalse(over(45, retire = SlotValue.DateValue(com.payslipmax.pdfparser.calendar.CalendarDate(2026, 2, 30))), "30 February does not exist")
        assertFalse(over(45, retire = SlotValue.TextValue("soon")), "not a date")
    }

    @Test
    fun theTighterOfTheTwoLimitsWins() {
        // why: an NCC officer retiring in March keeps the NCC 30; an ordinary officer retiring in February gets the February 30.
        assertTrue(over(31, setOf(LeaveRules.ANSWER_NCC_REEMPLOYED), retire = date(3, 1)))
        assertTrue(over(31, retire = date(2, 1)))
        assertFalse(over(30, setOf(LeaveRules.ANSWER_NCC_REEMPLOYED), retire = date(3, 1)))
    }
}
