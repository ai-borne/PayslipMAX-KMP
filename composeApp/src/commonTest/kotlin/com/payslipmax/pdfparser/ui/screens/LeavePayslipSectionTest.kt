package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.insights.LeaveBankCalculator
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.BELOW_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.OVER_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * The payslip view shows two things from two sources: the statement's own leave table (free, as printed on that payslip) and
 * today's leave-bank value (from the summary, priced at the latest pay). These pin that the two are never mixed up, that the
 * section exists only where a table was printed and the kill switch allows it, and that the teaser respects the Premium gate.
 */
class LeavePayslipSectionTest {
    private val dec2022 = payslip(2022, 12, table(BELOW_CAP_DAYS, asOfDate = "2021-12-31"))
    private val dec2025 = payslip(2025, 12, table(OVER_CAP_DAYS, asOfDate = "2024-12-31"))
    private val summary = assertNotNull(LeaveBankCalculator.summarize(listOf(dec2022, dec2025)))

    @Test
    fun theSectionShowsOnlyForAPayslipThatPrintedATableWhileTheKillSwitchIsOn() {
        assertSame(dec2025.leaveAccumulation, payslipLeaveTable(dec2025, enabled = true))
        assertNull(payslipLeaveTable(payslip(2025, 7), enabled = true))
        assertNull(payslipLeaveTable(dec2025, enabled = false))
    }

    @Test
    fun subtitleAndRowsComeFromThisPayslipNotTheLatestStatement() {
        // Viewing Dec 2022 while Dec 2025 is the latest: the subtitle must still say 222 days as of 31 Dec 2021.
        val ui = leaveSectionUi(assertNotNull(dec2022.leaveAccumulation), summary, isEditing = false)

        assertEquals("As of 31 Dec 2021 · 222 days", ui.subtitle)
        assertEquals(listOf("31 Dec 2020", "31 Dec 2021"), ui.rows.map { it.date })
    }

    @Test
    fun theTeaserIsTodaysValueFromTheSummaryWhicheverPayslipIsOpen() {
        val ui = leaveSectionUi(assertNotNull(dec2022.leaveAccumulation), summary, isEditing = false)

        assertEquals("Your leave accumulation today ≈ ${formatCurrency(summary.valueAtCurrentPay)}", ui.teaser)
    }

    @Test
    fun theLatestStatementReadsAsThePlanExample() {
        assertEquals("As of 31 Dec 2024 · 301 days", leaveSectionUi(assertNotNull(dec2025.leaveAccumulation), summary, isEditing = false).subtitle)
    }

    @Test
    fun noSummaryMeansNoTeaserRatherThanAGuessedValue() {
        assertNull(leaveSectionUi(assertNotNull(dec2025.leaveAccumulation), summary = null, isEditing = false).teaser)
    }

    @Test
    fun theTeaserIsHiddenWhileEditingSoItCannotDropTheDrafts() {
        // Leaving the payslip ends the correction session; a teaser tap mid-edit would silently discard the user's drafts.
        assertNull(leaveSectionUi(assertNotNull(dec2025.leaveAccumulation), summary, isEditing = true).teaser)
    }

    @Test
    fun aStatementWithoutAnAccrualRowHasNoSubtitleButKeepsItsRows() {
        val ui = leaveSectionUi(LeaveAccumulation(listOf(ltcEncashed("2025-06-30", 10))), summary, isEditing = false)

        assertNull(ui.subtitle)
        assertEquals(1, ui.rows.size)
    }

    @Test
    fun withAccessTheTeaserOpensRetirementCalculatorsUnderItsCatalogGate() {
        val asked = mutableListOf<FeatureGate>()
        var opened: Screen? = null
        var locked = false

        val grantAndRecord: (FeatureGate) -> Boolean = { gate ->
            asked += gate
            true
        }

        openLeaveBankFromTeaser(hasAccess = grantAndRecord, onNavigate = { opened = it }, onLocked = { locked = true })

        assertEquals(listOf(FeatureGate.RETIREMENT_CALCULATORS), asked)
        assertEquals(Screen.RetirementCalculators, opened)
        assertEquals(false, locked)
    }

    @Test
    fun withoutAccessTheTeaserOffersTheUpgradeAndNeverOpensTheScreen() {
        var opened: Screen? = null
        var locked = false

        openLeaveBankFromTeaser(hasAccess = { false }, onNavigate = { opened = it }, onLocked = { locked = true })

        assertNull(opened)
        assertEquals(true, locked)
    }
}
