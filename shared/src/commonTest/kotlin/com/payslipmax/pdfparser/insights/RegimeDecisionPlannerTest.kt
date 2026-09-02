package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.domain.TaxRegime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegimeDecisionPlannerTest {
    private fun comparison(
        winner: String,
        savings: Double,
        fy: String = "2026-27",
    ): RegimeComparisonResult {
        val detail =
            RegimeTaxDetail(
                regimeName = "X",
                grossIncome = 0.0,
                standardDeduction = 0.0,
                totalDeductionsAndExemptions = 0.0,
                netTaxableIncome = 0.0,
                baseTax = 0.0,
                surcharge = 0.0,
                cess = 0.0,
                totalTaxPayable = 0.0,
                effectiveTaxRatePct = 0.0,
            )
        return RegimeComparisonResult(fy, detail, detail, winner, savings, 0.0)
    }

    private fun payslip(
        year: Int,
        monthNum: Int,
        regime: TaxRegime,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "test_$year$monthNum.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "Month$monthNum",
            dateStr = "$monthNum/$year",
            officer = Officer("Test", "1", "X"),
            earnings = Earnings(),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 0.0, totalDeductions = 0.0, netRemittance = 0.0),
            taxAndSavings = TaxAndSavings(taxRegime = regime),
        )

    // --- ADR-4 decision-plan split ---

    @Test
    fun testPlanNullWhenActiveRegimeAlreadyWins() {
        val plan = RegimeDecisionPlanner.buildRegimeDecisionPlan(comparison("OLD", 5000.0), TaxRegime.OLD)
        assertNull(plan)
    }

    @Test
    fun testPlanNullWhenNoSavings() {
        val plan = RegimeDecisionPlanner.buildRegimeDecisionPlan(comparison("NEW", 0.0), TaxRegime.OLD)
        assertNull(plan)
    }

    @Test
    fun testPlanSplitsIntoTwoDecisionsWithDifferentReversibility() {
        // ADR-4: PCDA intimation cannot be reversed mid-year (CBDT Circular 4/2023); the ITR election
        // can be, independent of what PCDA was told.
        val plan = RegimeDecisionPlanner.buildRegimeDecisionPlan(comparison("NEW", 25000.0), TaxRegime.OLD)
        assertNotNull(plan)
        assertFalse(plan.pcdaIntimationDecision.reversibleMidYear)
        assertTrue(plan.itrElectionDecision.reversibleMidYear)
        assertNotNull(plan.pcdaIntimationDecision.trapWarning)
    }

    @Test
    fun testItrElectionCarriesBelatedReturnTrapOnlyWhenOldRegimeIsBest() {
        val oldWins = RegimeDecisionPlanner.buildRegimeDecisionPlan(comparison("OLD", 25000.0), TaxRegime.NEW)
        assertNotNull(oldWins?.itrElectionDecision?.trapWarning)

        val newWins = RegimeDecisionPlanner.buildRegimeDecisionPlan(comparison("NEW", 25000.0), TaxRegime.OLD)
        assertNull(newWins?.itrElectionDecision?.trapWarning)
    }

    // --- Mid-year regime change detection ---

    @Test
    fun testDetectMidYearRegimeChangeFalseWhenSingleRegimeAllYear() {
        val payslips =
            listOf(
                payslip(2026, 4, TaxRegime.NEW),
                payslip(2026, 5, TaxRegime.NEW),
                payslip(2026, 6, TaxRegime.NEW),
            )
        val result = RegimeDecisionPlanner.detectMidYearRegimeChange(payslips, "2026-27")
        assertFalse(result.detected)
        assertEquals(listOf(TaxRegime.NEW), result.regimesSeen)
        assertNull(result.message)
    }

    @Test
    fun testDetectMidYearRegimeChangeTrueAcrossFy() {
        val payslips =
            listOf(
                payslip(2026, 4, TaxRegime.OLD),
                payslip(2026, 5, TaxRegime.OLD),
                payslip(2026, 6, TaxRegime.NEW),
            )
        val result = RegimeDecisionPlanner.detectMidYearRegimeChange(payslips, "2026-27")
        assertTrue(result.detected)
        assertEquals(6, result.changeMonthNum)
        assertNotNull(result.message)
    }

    @Test
    fun testDetectMidYearRegimeChangeIgnoresPayslipsOutsideTheFy() {
        val payslips =
            listOf(
                // FY 2024-25, not in scope
                payslip(2025, 3, TaxRegime.OLD),
                payslip(2026, 4, TaxRegime.NEW),
                payslip(2026, 5, TaxRegime.NEW),
            )
        val result = RegimeDecisionPlanner.detectMidYearRegimeChange(payslips, "2026-27")
        assertFalse(result.detected)
    }
}
