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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TwoTrackReconciliationEngineTest {
    private fun regimeDetail(
        regimeName: String,
        totalTax: Double,
        netTaxable: Double = 1_000_000.0,
    ): RegimeTaxDetail =
        RegimeTaxDetail(
            regimeName = regimeName,
            grossIncome = netTaxable + 75000.0,
            standardDeduction = 75000.0,
            totalDeductionsAndExemptions = 75000.0,
            netTaxableIncome = netTaxable,
            baseTax = totalTax,
            surcharge = 0.0,
            cess = 0.0,
            totalTaxPayable = totalTax,
            effectiveTaxRatePct = 0.0,
        )

    private fun comparison(
        winner: String,
        oldTax: Double,
        newTax: Double,
        fy: String = "2026-27",
    ): RegimeComparisonResult =
        RegimeComparisonResult(
            financialYear = fy,
            oldRegime = regimeDetail("OLD", oldTax),
            newRegime = regimeDetail("NEW", newTax),
            winnerRegime = winner,
            annualSavings = kotlin.math.abs(oldTax - newTax),
            breakEvenDeduction = 0.0,
        )

    private fun fySummary(
        arrears: Double = 0.0,
        dsop: Double = 0.0,
        agif: Double = 0.0,
    ): FyTaxLedgerSummary =
        FyTaxLedgerSummary(
            financialYear = "2026-27",
            assessmentYear = "2027-28",
            parsedMonthCount = 1,
            monthsElapsedInFy = 1,
            ytdGross = 0.0,
            ytdTaxDeducted = 0.0,
            ytdArrears = arrears,
            ytdReimbursements = 0.0,
            ytdDsop = dsop,
            ytdAgif = agif,
            ytdFieldAllowance = 0.0,
            ytdRiskHardshipAllowance = 0.0,
            ytdHra = 0.0,
            ytdBasicPay = 0.0,
            ytdDa = 0.0,
            projectedAnnualGross = 0.0,
            projectedAnnualTaxDeducted = 0.0,
            projectedAnnualDsop = dsop,
            projectedAnnualAgif = agif,
            projectedAnnualFieldAllowance = 0.0,
            projectedAnnualRiskHardshipAllowance = 0.0,
            projectedAnnualHra = 0.0,
            projectedAnnualBasicPay = 0.0,
            projectedAnnualDa = 0.0,
            missingMonthNums = emptyList(),
            latestBasicPay = 0.0,
            latestDa = 0.0,
        )

    private fun payslip(
        totalTaxPayable: Double?,
        taxRegime: TaxRegime,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "test.pdf",
            year = 2026,
            monthNum = 4,
            monthName = "April",
            dateStr = "04/2026",
            officer = Officer("Test", "1", "X"),
            earnings = Earnings(),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 0.0, totalDeductions = 0.0, netRemittance = 0.0),
            taxAndSavings = TaxAndSavings(totalTaxPayable = totalTaxPayable, taxRegime = taxRegime),
        )

    @Test
    fun testReconcileClassifiesRefundWhenPcdaWithholdsMoreThanBestLiability() {
        // PCDA withholds 700000 under NEW; our engine's genuinely best regime (OLD) only owes 600000.
        val comparison = comparison(winner = "OLD", oldTax = 600_000.0, newTax = 700_000.0)
        val result = TwoTrackReconciliationEngine.reconcile(fySummary(), comparison, payslip(700_000.0, TaxRegime.NEW))

        assertNotNull(result)
        assertEquals(ReconciliationType.REFUND_EXPECTED, result.reconciliationType)
        assertEquals(100_000.0, result.deltaAmount, 0.01)
        assertEquals(TaxRegime.OLD, result.liabilityTrackRegime)
        assertTrue(result.message.contains("recovers"))
    }

    @Test
    fun testReconcileClassifiesTopUpWhenPcdaWithholdsLessThanBestLiability() {
        val comparison = comparison(winner = "NEW", oldTax = 700_000.0, newTax = 600_000.0)
        val result = TwoTrackReconciliationEngine.reconcile(fySummary(), comparison, payslip(500_000.0, TaxRegime.NEW))

        assertNotNull(result)
        assertEquals(ReconciliationType.TOP_UP_DUE, result.reconciliationType)
        assertEquals(-100_000.0, result.deltaAmount, 0.01)
        assertTrue(result.message.contains("owe an additional"))
    }

    @Test
    fun testReconcileClassifiesMatchedWithinThreshold() {
        val comparison = comparison(winner = "NEW", oldTax = 700_000.0, newTax = 600_010.0)
        val result = TwoTrackReconciliationEngine.reconcile(fySummary(), comparison, payslip(600_000.0, TaxRegime.NEW))

        assertNotNull(result)
        assertEquals(ReconciliationType.MATCHED, result.reconciliationType)
    }

    @Test
    fun testReconcileNullWhenPcdaTotalTaxPayableUnavailable() {
        // D3: the guard leaves totalTaxPayable null on genuine implausibility -- reconciling against a
        // fabricated figure would reintroduce the exact bug D3 fixed.
        val comparison = comparison(winner = "OLD", oldTax = 600_000.0, newTax = 700_000.0)
        val result = TwoTrackReconciliationEngine.reconcile(fySummary(), comparison, payslip(null, TaxRegime.NEW))
        assertNull(result)
    }

    @Test
    fun testBelatedReturnTrapWarningShownOnlyWhenOldRegimeWins() {
        val oldWins = comparison(winner = "OLD", oldTax = 500_000.0, newTax = 600_000.0)
        assertNotNull(TwoTrackReconciliationEngine.belatedReturnTrapWarning(oldWins))

        val newWins = comparison(winner = "NEW", oldTax = 600_000.0, newTax = 500_000.0)
        assertNull(TwoTrackReconciliationEngine.belatedReturnTrapWarning(newWins))
    }

    @Test
    fun testDsopWasteInsightOnlyUnderNewWithNonZeroDsop() {
        val comparison = comparison(winner = "NEW", oldTax = 600_000.0, newTax = 500_000.0)
        val summary = fySummary(dsop = 480_000.0, agif = 150_000.0)

        val underNew = TwoTrackReconciliationEngine.dsopWasteInsight(summary, TaxRegime.NEW, comparison)
        assertNotNull(underNew)
        assertEquals(150_000.0, underNew.cappedContribution, 0.01) // capped at LIMIT_80C
        assertTrue(underNew.taxBenefitForgoneAnnual > 0.0)

        val underOld = TwoTrackReconciliationEngine.dsopWasteInsight(summary, TaxRegime.OLD, comparison)
        assertNull(underOld)

        val zeroDsop = TwoTrackReconciliationEngine.dsopWasteInsight(fySummary(dsop = 0.0), TaxRegime.NEW, comparison)
        assertNull(zeroDsop)
    }

    @Test
    fun testArrearsTransparencyOnlyWhenArrearsPresent() {
        val withArrears = TwoTrackReconciliationEngine.arrearsTransparency(fySummary(arrears = 10_086.0))
        assertNotNull(withArrears)
        assertEquals(10_086.0, withArrears.arrearsAmount, 0.01)

        val withoutArrears = TwoTrackReconciliationEngine.arrearsTransparency(fySummary(arrears = 0.0))
        assertNull(withoutArrears)
    }
}
