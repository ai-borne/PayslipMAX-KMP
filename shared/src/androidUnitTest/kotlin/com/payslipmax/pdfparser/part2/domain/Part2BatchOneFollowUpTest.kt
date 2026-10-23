package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RealPart2.annualLeave
import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.draft
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import com.payslipmax.pdfparser.part2.domain.RealPart2.rows
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The follow-up orders of the annual-leave set on the real templates (Part II plan, phase 2b): an extension, a carry-forward, a rejoining,
 * a voluntary rejoining and a recall each quote the order that granted the leave, or print a blank while it is unpublished (the practice
 * lag is months), and the last two cancel the days left. Synthetic dates and order numbers only.
 */
class Part2BatchOneFollowUpTest {
    private val grant = "grant" to ref("0123/26", 6, 20)
    private val leaveType = "leave_type" to text("annual leave")

    private fun one(
        code: String,
        vararg values: Pair<String, SlotValue>,
    ): Part2Occurrence = rows(draft(OccurrenceRequest(code, mapOf(*values)))).single()

    private fun findings(
        code: String,
        vararg values: Pair<String, SlotValue>,
    ) = draft(OccurrenceRequest(code, mapOf(*values))).findings

    private val extension = arrayOf("leave_from" to date(8, 5), "leave_to" to date(8, 14), "year" to number(2026))

    @Test
    fun anExtensionQuotesTheGrantOrderInItsNarrativeAndDataFourAndAllowsNoPrefix() {
        val row = one("EXTNAL", grant, *extension)
        assertEquals(
            "Granted 10 days extension of Annual Leave of year (2026) from 05/08/2026 to 14/08/2026 with permission to suffix Nil. " +
                "Annual leave initially granted vide Part II Order No 0123/26 and date 20/06/2026",
            row.narrative,
        )
        assertEquals(listOf("", "", "10", "0123/26 dated 20/06/2026"), row.data, "Data 1 is empty: a prefix is not allowed on an extension")
    }

    @Test
    fun aSuffixOnAnExtensionGoesInDataTwo() {
        assertEquals("17/08/2026", one("EXTNAL", grant, *extension, "suffix_last" to date(8, 17)).data[1])
    }

    @Test
    fun anExtensionWithoutTheGrantOrderPrintsBlanksAndIsWarnedNotRefused() {
        val row = one("EXTNAL", *extension)
        assertEquals("____ dated ____", row.data[3])
        val finding = findings("EXTNAL", *extension).single()
        assertEquals(listOf("grant_quoted_annual", RuleSeverity.WARN, 115, "7"), listOf(finding.ruleId, finding.severity, finding.page, finding.note))
        assertEquals(emptyList(), findings("EXTNAL", grant, *extension), "quoting it clears the warning")
    }

    @Test
    fun theExtensionCountsAgainstTheSixtyDayCapToo() {
        val finding = findings("EXTNAL", grant, *extension, "earlier_al" to number(55)).single()
        assertEquals("al_year_cap", finding.ruleId)
    }

    @Test
    fun aCarryForwardNamesTheAuthorisingOrderAndTheGroupIsA() {
        val row = one("GTDALCF", *extension, "carry_order" to ref("0050/25", 12, 1, 2025))
        assertEquals(
            "Granted 10 days Annual leave for the year 2026, from 05/08/2026 to 14/08/2026 carry forwarded due to UN Mission posting vide " +
                "Part II Order No 0050/25 dated 01/12/2025",
            row.narrative,
        )
        assertEquals(listOf("", "", "10", "2026"), row.data)
        assertEquals("A", row.group)
    }

    @Test
    fun aRejoiningIsPrintedAsRejoinAndQuotesTheGrantOrder() {
        // why: Round 9 - HRMS publishes REJOIN; the manual's REJOINAL (5.23) is the only code the slip renames.
        val row = one("REJOINAL", grant, leaveType, "rejoined_on" to date(8, 5))
        assertEquals("REJOIN", row.code)
        assertEquals("Rejoined from annual leave w.e.f. 05/08/2026. Annual Leave initially granted vide Pt II Order No 0123/26 dated 20/06/2026", row.narrative)
        assertEquals(date(8, 5).date, row.from)
        assertNull(row.to, "To is not required")
        assertEquals(listOf("annual leave", "", "0123/26 dated 20/06/2026", ""), row.data)
    }

    @Test
    fun aRejoiningWithoutTheGrantOrderPrintsBlanksAndWarnsOnPageOneThirtyOne() {
        val finding = findings("REJOINAL", leaveType, "rejoined_on" to date(8, 5)).single()
        assertEquals(listOf("grant_quoted_rejoin", RuleSeverity.WARN, 131), listOf(finding.ruleId, finding.severity, finding.page))
        assertEquals("REJOIN", finding.code, "findings name the code as printed")
    }

    @Test
    fun aVoluntaryRejoiningCancelsTheRemainingDaysAndCountsThemBothDaysIncluded() {
        val row = one("VOLREJ", grant, leaveType, "rejoined_on" to date(8, 1), "cancel_from" to date(8, 2), "cancel_to" to date(8, 4))
        assertEquals(
            "Voluntarily rejoined from annual leave on 01/08/2026. Balance of annual leave for 3 days from 02/08/2026 to 04/08/2026 is hereby cancelled. " +
                "Annual Leave initially granted vide Pt II Order No 0123/26 dated 20/06/2026",
            row.narrative,
        )
        assertEquals(listOf("annual leave", "0123/26 dated 20/06/2026", "3", ""), row.data)
        assertEquals(date(8, 1).date, row.from)
    }

    @Test
    fun aRecallCancelsTheRemainingDaysAndCitesPageOneThirtyTwoWhenTheGrantIsNotQuoted() {
        val values = arrayOf(leaveType, "recalled_on" to date(7, 20), "cancel_from" to date(7, 21), "cancel_to" to date(8, 4))
        val row = one("RECALL", grant, *values)
        assertEquals(
            "Recalled from annual leave from 20/07/2026. Balance of annual leave for 15 days from 21/07/2026 to 04/08/2026 is hereby cancelled. " +
                "Annual Leave initially granted vide Pt II Order No 0123/26 dated 20/06/2026",
            row.narrative,
        )
        assertEquals(listOf("annual leave", "0123/26 dated 20/06/2026", "15", ""), row.data)
        assertEquals(132, findings("RECALL", *values).single().page)
    }

    @Test
    fun theRationAndRhLinksAreOnlyOnTheGrantsNotOnTheFollowUps() {
        // why: a follow-up quotes the grant; the ration and R&H rows follow from the grant, and an extension keeps the same rows open.
        val extended = draft(OccurrenceRequest("EXTNAL", annualLeave + mapOf(grant), setOf(RationRules.ANSWER_ON_RATION)))
        assertEquals(listOf("EXTNAL"), rows(extended).map { it.code })
    }
}
