package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.tax.PensionRuleKnowledgeBase
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.AT_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.BELOW_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.MILITARY_SERVICE_PAY
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.OVER_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.table
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The leave balance feeds a retirement-encashment rupee figure, so these pin which statement is trusted (the latest one that
 * prints the table, never a merge of years), that PCDA's printed total is shown as printed, and that the rupee value uses
 * today's pay through the same formula the Retirement Calculators use.
 */
class LeaveBankCalculatorTest {
    private fun summary(vararg payslips: ParsedPayslip) =
        assertNotNull(LeaveBankCalculator.summarize(payslips.toList()))

    @Test
    fun usesTheLatestLeaveStatementForTheBalanceAndTheLatestPayslipForThePay() {
        val dec2024 = payslip(2024, 12, table(282, asOfDate = "2023-12-31"), basicPay = 100_000.0)
        val dec2025 = payslip(2025, 12, table(OVER_CAP_DAYS), basicPay = 110_000.0)
        val jul2026 = payslip(2026, 7, leave = null, basicPay = 120_000.0, dearnessAllowance = 70_000.0)

        val result = summary(dec2024, dec2025, jul2026)

        assertEquals(OVER_CAP_DAYS, result.balanceDays)
        assertEquals(2025, result.sourceYear)
        assertEquals(12, result.sourceMonth)
        assertEquals(RetirementCalculatorEngine.leaveEncashment(120_000.0, MILITARY_SERVICE_PAY, 70_000.0, 300), result.valueAtCurrentPay)
    }

    @Test
    fun payMonthNamesThePayslipTheValueWasPricedAtNotTheLeaveStatement() {
        // The card captions its rupee figure with this month; naming the leave statement's month would misstate the basis.
        val dec2025 = payslip(2025, 12, table(OVER_CAP_DAYS))
        val mar2026 = payslip(2026, 3, leave = null)

        val result = summary(mar2026, dec2025)

        assertEquals(3, result.payMonth)
        assertEquals(2026, result.payYear)
        assertEquals(12, result.sourceMonth)
    }

    @Test
    fun entriesAreTheSourceStatementsOwnRowsAndNeverAnOlderYears() {
        // Statements list different rows each year, so the table shown must be the one the balance came from.
        val dec2024 = payslip(2024, 12, table(282, ltcDays = listOf(10, 10)))
        val dec2025Table = table(OVER_CAP_DAYS, asOfDate = "2025-12-31")

        val result = summary(dec2024, payslip(2025, 12, dec2025Table))

        assertEquals(dec2025Table.entries, result.entries)
    }

    @Test
    fun aLeaveTableOutsideDecemberIsStillTheLatestSource() {
        val dec2025 = payslip(2025, 12, table(OVER_CAP_DAYS))
        val mar2026 = payslip(2026, 3, table(BELOW_CAP_DAYS, asOfDate = "2025-12-31"))

        val result = summary(dec2025, mar2026)

        assertEquals(BELOW_CAP_DAYS, result.balanceDays)
        assertEquals(3, result.sourceMonth)
    }

    @Test
    fun aBalanceOverTheCapShowsThePrintedFigureButEncashesOnlyTheCap() {
        val result = summary(payslip(2025, 12, table(OVER_CAP_DAYS)))

        assertEquals(OVER_CAP_DAYS, result.balanceDays)
        assertEquals(1, result.daysOverCap)
        assertEquals(PensionRuleKnowledgeBase.LEAVE_MAX_DAYS, result.encashableDays)
    }

    @Test
    fun aBalanceAtOrBelowTheCapIsFullyEncashable() {
        val below = summary(payslip(2022, 12, table(BELOW_CAP_DAYS)))
        val atCap = summary(payslip(2022, 12, table(AT_CAP_DAYS)))

        assertEquals(0, below.daysOverCap)
        assertEquals(BELOW_CAP_DAYS, below.encashableDays)
        assertEquals(0, atCap.daysOverCap)
        assertEquals(AT_CAP_DAYS, atCap.encashableDays)
    }

    @Test
    fun asOfDateIsTheLatestAccrualDateNotTheStatementMonthOrALaterLtcRow() {
        val leave =
            LeaveAccumulation(
                listOf(
                    accrued("2024-12-31", 30, 282),
                    accrued("2023-12-31", 30, 252),
                    ltcEncashed("2025-06-30", 10),
                ),
            )

        val result = summary(payslip(2025, 12, leave))

        assertEquals("2024-12-31", result.asOfDate)
        assertEquals(282, result.balanceDays)
    }

    @Test
    fun theBalanceIsThePrintedTotalEvenWhenItDoesNotAddUp() {
        // PCDA's running totals can drop between years or not match the rows; the printed figure is the record.
        val leave = LeaveAccumulation(listOf(accrued("2023-12-31", 30, 290), accrued("2024-12-31", 19, 282)))

        assertEquals(282, summary(payslip(2025, 12, leave)).balanceDays)
    }

    @Test
    fun ltcDaysAreSummedFromLtcRowsOnly() {
        val leave =
            LeaveAccumulation(
                listOf(
                    accrued("2023-12-31", 30, 252),
                    ltcEncashed("2024-03-01", 10),
                    LeaveEntry("2024-04-01", "XYZLVE", 5, null, "303", "2024-04-01"),
                    ltcEncashed("2024-06-01", 10),
                    accrued("2024-12-31", 30, 282),
                ),
            )

        val result = summary(payslip(2025, 12, leave))

        assertEquals(20, result.ltcDaysUsed)
        assertEquals(PensionRuleKnowledgeBase.LTC_ENCASH_MAX_DAYS, result.ltcCapDays)
    }

    @Test
    fun valueIsTheRetirementEngineFormulaOnCurrentBasicDaAndNpa() {
        val pay = payslip(2025, 12, table(BELOW_CAP_DAYS), basicPay = 140_000.0, dearnessAllowance = 81_200.0, npa = 28_000.0)

        val expected = RetirementCalculatorEngine.leaveEncashment(140_000.0, MILITARY_SERVICE_PAY, 81_200.0, BELOW_CAP_DAYS, 28_000.0)

        assertEquals(expected, summary(pay).valueAtCurrentPay)
    }

    @Test
    fun valueLeavesOutTheDaOnMsp() {
        // Dec 2025 pay: DA 92,916 is 58% of Basic + MSP; only the DA on Basic counts, so 22,86,260 and not 23,76,160.
        val pay = payslip(2025, 12, table(OVER_CAP_DAYS), basicPay = 144_700.0, dearnessAllowance = 92_916.0)

        assertEquals(2_286_260.0, summary(pay).valueAtCurrentPay, 1e-6)
    }

    @Test
    fun noLeaveDataGivesNoSummary() {
        assertNull(LeaveBankCalculator.summarize(emptyList()))
        assertNull(LeaveBankCalculator.summarize(listOf(payslip(2025, 11), payslip(2025, 12))))
    }

    @Test
    fun aLatestTableWithoutAnAccrualGivesNoSummaryRatherThanAnOlderYear() {
        val dec2024 = payslip(2024, 12, table(282))
        val dec2025 = payslip(2025, 12, LeaveAccumulation(listOf(ltcEncashed("2025-06-30", 10))))

        assertNull(LeaveBankCalculator.summarize(listOf(dec2024, dec2025)))
    }

    @Test
    fun inputOrderDoesNotChangeTheResult() {
        val payslips =
            listOf(
                payslip(2026, 7, leave = null, basicPay = 120_000.0),
                payslip(2024, 12, table(282)),
                payslip(2025, 12, table(OVER_CAP_DAYS)),
                payslip(2025, 1, leave = null, basicPay = 90_000.0),
            )
        val expected = LeaveBankCalculator.summarize(payslips)

        assertEquals(expected, LeaveBankCalculator.summarize(payslips.reversed()))
        assertEquals(expected, LeaveBankCalculator.summarize(payslips.sortedBy { it.file }))
    }

    @Test
    fun twoStatementsForTheSameMonthResolveTheSameWayInAnyOrder() {
        val a = payslip(2025, 12, table(BELOW_CAP_DAYS), basicPay = 100_000.0, file = "a.pdf")
        val b = payslip(2025, 12, table(OVER_CAP_DAYS), basicPay = 110_000.0, file = "b.pdf")

        assertEquals(summary(a, b), summary(b, a))
    }

    @Test
    fun twoAccrualsOnTheSameDateResolveToTheLaterOrder() {
        val leave =
            LeaveAccumulation(
                listOf(
                    accrued("2024-12-31", 30, 290, orderNo = "402", orderDate = "2025-05-02"),
                    accrued("2024-12-31", 30, 282, orderNo = "401", orderDate = "2025-03-01"),
                ),
            )

        assertEquals(290, summary(payslip(2025, 12, leave)).balanceDays)
    }

    @Test
    fun latestAccrualIsTheRowTheSummaryBalanceComesFrom() {
        // The payslip view reads any one statement's balance through latestAccrual; it must be the same rule as the summary's,
        // or the payslip and Retirement Calculators would print different balances for the same statement.
        val leave =
            LeaveAccumulation(
                listOf(
                    accrued("2024-12-31", 30, 290, orderNo = "402", orderDate = "2025-05-02"),
                    ltcEncashed("2025-06-30", 10),
                    accrued("2023-12-31", 30, 260),
                    accrued("2024-12-31", 30, 282, orderNo = "401", orderDate = "2025-03-01"),
                ),
            )

        val latest = assertNotNull(LeaveBankCalculator.latestAccrual(leave.entries))

        assertEquals("402", latest.orderNo)
        assertEquals(latest, LeaveBankCalculator.latestAccrual(leave.entries.reversed()))
        assertEquals(latest.totalDays, summary(payslip(2025, 12, leave)).balanceDays)
    }

    @Test
    fun latestAccrualIsNullWhenTheStatementHasNoAccrualRow() {
        assertNull(LeaveBankCalculator.latestAccrual(listOf(ltcEncashed("2025-06-30", 10))))
        assertNull(LeaveBankCalculator.latestAccrual(emptyList()))
    }
}
