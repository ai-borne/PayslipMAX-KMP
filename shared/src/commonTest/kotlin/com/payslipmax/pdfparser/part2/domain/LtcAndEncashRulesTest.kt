package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RuleContexts.context
import com.payslipmax.pdfparser.part2.domain.RuleContexts.number
import com.payslipmax.pdfparser.part2.domain.RuleContexts.ref
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The limits of annual leave encashed on LTC (ODP-2026 pp.135, 138 and 140) and of the final encashment (pp.133-134, 140-141). Each test
 * says what an audit rejects: ten days at a time and sixty in a career, thirty days left at credit, a year above its thirty, a total above 300.
 */
class LtcAndEncashRulesTest {
    private fun ltc(id: String) = assertNotNull(LtcRules.all.filterIsInstance<CheckRule>().firstOrNull { it.id == id }, id)

    private fun encash(id: String) = assertNotNull(EncashRules.all.filterIsInstance<CheckRule>().firstOrNull { it.id == id }, id)

    private fun derive(id: String) = assertNotNull(EncashRules.all.filterIsInstance<DeriveRule>().firstOrNull { it.id == id }, id)

    private fun text(value: String) = SlotValue.TextValue(value)

    private fun worked(
        id: String,
        breakup: SlotValue?,
    ): SlotValue? {
        val values = listOfNotNull(breakup?.let { "breakup" to it }).toMap()
        return derive(id).derive(DeriveContext(OccurrenceRequest("X", values), values))
    }

    @Test
    fun theFamiliesOfferExactlyTheRulesTheTemplatesCite() {
        assertEquals(setOf("ltc_days_over_10", "ltc_total_over_60", "ltc_credit_under_30", "advance_quoted"), LtcRules.all.map { it.id }.toSet())
        assertEquals(setOf("encash_total", "encash_listing", "encash_year_over_30", "encash_over_cap"), EncashRules.all.map { it.id }.toSet())
        assertEquals(setOf("advance", "last_ltc"), LtcRules.VAULT_ORDER_SLOTS)
    }

    @Test
    fun tenDaysAreEncashedAtATimeAndSixtyInACareer() {
        // why: p.135 note 1 - encashment of 10 days at the time of availing LTC, to the extent of sixty days during the entire career.
        assertFalse(ltc("ltc_days_over_10").violated(context("X", mapOf("ltc_days" to number(10)))))
        assertTrue(ltc("ltc_days_over_10").violated(context("X", mapOf("ltc_days" to number(11)))))
        assertFalse(ltc("ltc_days_over_10").violated(context("X")))
        assertFalse(ltc("ltc_total_over_60").violated(context("X", mapOf("total_ltc" to number(60)))))
        assertTrue(ltc("ltc_total_over_60").violated(context("X", mapOf("total_ltc" to number(61)))))
        assertFalse(ltc("ltc_total_over_60").violated(context("X")))
    }

    @Test
    fun atLeastThirtyDaysMustStayAtCreditAfterTheEncashment() {
        // why: the certificates on pp.137-138 (1.4 and 2.4) - "minimum 30 days should be available at the credit of the officer".
        fun left(
            accumulation: Int,
            days: Int,
        ) = ltc("ltc_credit_under_30").violated(context("X", mapOf("accumulation" to number(accumulation), "ltc_days" to number(days))))
        assertFalse(left(40, 10), "exactly 30 stay")
        assertTrue(left(39, 10))
        assertTrue(left(10, 10))
        assertFalse(ltc("ltc_credit_under_30").violated(context("X", mapOf("accumulation" to number(5)))), "no days typed judges nothing")
        assertFalse(ltc("ltc_credit_under_30").violated(context("X", mapOf("ltc_days" to number(10)))), "no accumulation typed judges nothing")
    }

    @Test
    fun aCancellationWithNoAdvanceOrderYetIsWarnedAboutAndOneQuotingItIsNot() {
        // why: p.140 note 3 - the number and date of the order already published for the encashment are quoted.
        assertTrue(ltc("advance_quoted").violated(context("X")))
        assertTrue(ltc("advance_quoted").violated(context("X", mapOf("advance" to number(1)))))
        assertFalse(ltc("advance_quoted").violated(context("X", mapOf("advance" to ref("0123/26")))))
    }

    @Test
    fun theBreakupIsSummedCappedAt300AndListedLikeTheManualWithItsTotal() {
        val list = text("2011 30\n2013 30\n2015 30\n2017 30\n2018 30\n2019 30\n2020 20\n2021 20")
        assertEquals(SlotValue.NumberValue(220), worked("encash_total", list))
        assertEquals(SlotValue.TextValue("2011 30, 2013 30, 2015 30, 2017 30, 2018 30, 2019 30, 2020 20, 2021 20, Total 220"), worked("encash_listing", list), "the manual's own example, p.140")
        assertEquals(SlotValue.NumberValue(300), worked("encash_total", text("2000 30\n2001 30\n2002 30\n2003 30\n2004 30\n2005 30\n2006 30\n2007 30\n2008 30\n2009 30\n2010 1")), "301 is cut to 300")
        assertEquals(SlotValue.TextValue("2010 1, Total 1"), worked("encash_listing", text("2010 1")))
    }

    @Test
    fun aBreakupNothingCanReadWorksNothingOut() {
        for (bad in listOf(null, text(""), text("abc"), text("2013 5\n2011 5"), number(220))) {
            assertNull(worked("encash_total", bad), bad.toString())
            assertNull(worked("encash_listing", bad), bad.toString())
        }
    }

    @Test
    fun aYearAboveItsThirtyDaysAndATotalAbove300AreWarnedAbout() {
        // why: p.134 note 6 - leave accumulated in a year should not exceed 30 days; p.133 (a) - the total is restricted to 300.
        fun breakup(value: String) = context("X", mapOf("breakup" to text(value)))
        assertFalse(encash("encash_year_over_30").violated(breakup("2011 30")))
        assertTrue(encash("encash_year_over_30").violated(breakup("2011 31")))
        assertTrue(encash("encash_year_over_30").violated(breakup("2011 5\n2012 130")), "the 130 a real statement once showed")
        assertFalse(encash("encash_year_over_30").violated(context("X")))
        val ten = (0 until 10).joinToString("\n") { "${2000 + it} 30" }
        assertFalse(encash("encash_over_cap").violated(breakup(ten)), "exactly 300")
        assertTrue(encash("encash_over_cap").violated(breakup("$ten\n2010 1")))
        assertFalse(encash("encash_over_cap").violated(breakup("not a list")))
    }
}
