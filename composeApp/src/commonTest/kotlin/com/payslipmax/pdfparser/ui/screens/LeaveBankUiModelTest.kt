package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.LeaveBankCalculator
import com.payslipmax.pdfparser.insights.LeaveBankSummary
import com.payslipmax.pdfparser.insights.RetirementCalculatorEngine
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.AT_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.BELOW_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.OVER_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.table
import com.payslipmax.pdfparser.ui.theme.LeaveBankStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The Leave bank card is a rupee claim about the user's retirement, so these pin that it shows PCDA's printed balance (not the
 * capped one), names the statement and the pay month its figures come from, and shows the summary's own value: the card never
 * recomputes a figure the SSOT already holds.
 */
class LeaveBankUiModelTest {
    private fun summary(vararg payslips: ParsedPayslip): LeaveBankSummary =
        assertNotNull(LeaveBankCalculator.summarize(payslips.toList()))

    private val overCap = summary(payslip(2025, 12, table(OVER_CAP_DAYS, asOfDate = "2025-12-31", ltcDays = listOf(10, 10))))

    @Test
    fun overCapShowsThePrintedBalanceAndHowFarOverItIs() {
        val ui = leaveBankCardUi(overCap)

        assertEquals("301 days", ui.balance)
        assertEquals("1 over cap", ui.overCap)
    }

    @Test
    fun atOrBelowTheCapThereIsNoOverCapLabel() {
        assertNull(leaveBankCardUi(summary(payslip(2025, 12, table(AT_CAP_DAYS)))).overCap)
        assertNull(leaveBankCardUi(summary(payslip(2025, 12, table(BELOW_CAP_DAYS)))).overCap)
    }

    @Test
    fun asOfNamesTheAccrualDateAndTheStatementItWasPrintedOn() {
        assertEquals("As of 31 Dec 2025 · Dec 2025 statement", leaveBankCardUi(overCap).asOf)
    }

    @Test
    fun ltcReadsAsDaysUsedOfTheCapAndNothingMore() {
        assertEquals("LTC: 20 of 60 days used", leaveBankCardUi(overCap).ltc)
    }

    @Test
    fun worthIsTheSummaryValueCaptionedWithThePayMonthItWasPricedAt() {
        // Pay is from Mar 2026 though the leave table is Dec 2025: the caption must name the month the rupees came from.
        val s = summary(payslip(2025, 12, table(OVER_CAP_DAYS)), payslip(2026, 3, leave = null, basicPay = 144_700.0, dearnessAllowance = 92_916.0))

        val ui = leaveBankCardUi(s)

        assertEquals("Worth ≈ ${formatCurrency(s.valueAtCurrentPay)}", ui.worth)
        assertEquals("Worth ≈ ₹22,86,260", ui.worth)
        assertEquals("at Mar 2026 pay (300 days)", ui.worthBasis)
    }

    @Test
    fun rowsKeepPrintedOrderAndMapOnlyTheCodesTheAppUnderstands() {
        val leave =
            LeaveAccumulation(
                listOf(
                    accrued("2023-12-31", 30, 252, orderNo = "0446/2023", orderDate = "2024-01-15"),
                    ltcEncashed("2024-06-30", 10, orderNo = "0101/2024", orderDate = "2024-07-02"),
                    LeaveEntry("2024-08-01", "XYZLVE", 5, null, "0202/2024", "2024-08-03"),
                    accrued("2024-12-31", 30, 282, orderNo = "0446/2024", orderDate = "2025-01-15"),
                ),
            )

        val ui = leaveBankCardUi(summary(payslip(2025, 12, leave)))

        assertEquals("Leave orders (4)", ui.ordersTitle)
        assertEquals(listOf("Accrued", "LTC encashed", "XYZLVE", "Accrued"), ui.rows.map { it.type })
        assertEquals(LeaveRowUi("31 Dec 2023", "Accrued", "30", "252", "Pt II Order No 0446/2023 · 15 Jan 2024"), ui.rows.first())
        assertEquals("—", ui.rows[1].total)
        assertEquals("10", ui.rows[1].days)
    }

    @Test
    fun aDateThatIsNotIsoIsShownAsStoredRatherThanGuessed() {
        assertEquals("31 Dec 2025", leaveDisplayDate("2025-12-31"))
        assertEquals("2025-13-01", leaveDisplayDate("2025-13-01"))
        assertEquals("garbage", leaveDisplayDate("garbage"))
    }

    @Test
    fun leaveDaysStartAtTheEncashableDaysOrThePcdaCapWithoutAStatement() {
        assertEquals("300", initialLeaveDays(overCap))
        assertEquals("222", initialLeaveDays(summary(payslip(2025, 12, table(BELOW_CAP_DAYS)))))
        assertEquals(RetirementCalculatorEngine.LEAVE_MAX_DAYS.toString(), initialLeaveDays(null))
    }

    @Test
    fun theLeaveFieldNamesTheStatementAndThePrintedBalance() {
        assertEquals("From your Dec 2025 statement (301 days printed)", leaveDaysSourceLabel(overCap))
    }

    @Test
    fun belowTheCapTheNoteAddsThatLeaveKeepsAccruing() {
        val note = leaveDaysNote(summary(payslip(2025, 12, table(BELOW_CAP_DAYS))), enabled = true)

        assertEquals("From your Dec 2025 statement (222 days printed). Leave keeps accruing until retirement, up to 300 days.", note)
    }

    @Test
    fun atOrOverTheCapTheNoteIsOnlyTheSource() {
        assertEquals(leaveDaysSourceLabel(overCap), leaveDaysNote(overCap, enabled = true))
    }

    @Test
    fun withoutAStatementTheNoteSaysTheCapIsAssumedAndOnlyWhileTheFeatureIsOn() {
        assertEquals(LeaveBankStrings.noStatementAssumesCap, leaveDaysNote(null, enabled = true))
        assertNull(leaveDaysNote(null, enabled = false))
    }
}
