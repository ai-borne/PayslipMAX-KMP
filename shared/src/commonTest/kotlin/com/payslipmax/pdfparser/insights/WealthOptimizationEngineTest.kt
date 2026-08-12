package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WealthOptimizationEngineTest {
    private fun createPayslip(
        dsopMonthly: Double = 8_000.0,
        agifMonthly: Double = 5_000.0,
        grossPay: Double = 180_000.0,
        netTaxableIncome: Double = 800_000.0,
        dsopClosingBalance: Double = 500_000.0,
        taxRegime: TaxRegime = TaxRegime.OLD,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "test.pdf",
            year = 2025,
            monthNum = 4,
            monthName = "April",
            dateStr = "04/2025",
            officer = Officer("Test Officer", "12345", "XXXXX0000X"),
            earnings = Earnings(basicPay = 105_300.0, houseRentAllowance = 8000.0),
            deductions = Deductions(dsopSubscription = dsopMonthly, agif = agifMonthly, incomeTax = 5000.0),
            ledgerBalances = LedgerBalances(),
            summary =
                PayslipSummary(
                    grossPay = grossPay,
                    totalDeductions = dsopMonthly + agifMonthly + 5000.0,
                    netRemittance = grossPay - dsopMonthly - agifMonthly - 5000.0,
                ),
            taxAndSavings =
                TaxAndSavings(
                    grossSalaryYtd = grossPay,
                    netTaxableIncome = netTaxableIncome,
                    dsopFund = DsopFund(closingBalance = dsopClosingBalance),
                    taxRegime = taxRegime,
                ),
        )

    @Test
    fun testMarginalRateZeroBelowExemptionLimit() {
        assertEquals(0.0, WealthOptimizationEngine.deriveMarginalRate(200_000.0, TaxRegime.OLD))
    }

    @Test
    fun testMarginalRateFivePercent() {
        assertEquals(0.05, WealthOptimizationEngine.deriveMarginalRate(400_000.0, TaxRegime.OLD))
    }

    @Test
    fun testMarginalRateTwentyPercent() {
        assertEquals(0.20, WealthOptimizationEngine.deriveMarginalRate(700_000.0, TaxRegime.OLD))
    }

    @Test
    fun testMarginalRateThirtyPercent() {
        assertEquals(0.30, WealthOptimizationEngine.deriveMarginalRate(1_200_000.0, TaxRegime.OLD))
    }

    @Test
    fun testNewRegimeMarginalRates() {
        // Phase 1 (D1/D2/D6 fix): FY2025-26+ slabs (4/8/12/16/20/24L), not the stale FY2023-24-vintage
        // thresholds (3/7/9/12/15L) this test used to pin -- default fy is "2026-27".
        assertEquals(0.0, WealthOptimizationEngine.deriveMarginalRate(200_000.0, TaxRegime.NEW))
        assertEquals(0.05, WealthOptimizationEngine.deriveMarginalRate(600_000.0, TaxRegime.NEW))
        assertEquals(0.10, WealthOptimizationEngine.deriveMarginalRate(1_000_000.0, TaxRegime.NEW))
        assertEquals(0.15, WealthOptimizationEngine.deriveMarginalRate(1_400_000.0, TaxRegime.NEW))
        assertEquals(0.20, WealthOptimizationEngine.deriveMarginalRate(1_800_000.0, TaxRegime.NEW))
        assertEquals(0.25, WealthOptimizationEngine.deriveMarginalRate(2_200_000.0, TaxRegime.NEW))
        assertEquals(0.30, WealthOptimizationEngine.deriveMarginalRate(3_000_000.0, TaxRegime.NEW))
    }

    @Test
    fun test80CHeadroomPositiveWhenUnused() {
        val payslip = createPayslip(dsopMonthly = 2_000.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        val opportunity = result.opportunities.find { it.id == "80c_dsop" }
        assertTrue(opportunity != null, "80C DSOP opportunity should exist when headroom is positive")
        assertEquals(66_000.0, opportunity.unusedAmount, 0.01)
    }

    @Test
    fun test80CHeadroomZeroWhenExceeded() {
        val payslip = createPayslip(dsopMonthly = 8_000.0, agifMonthly = 5_000.0)
        val result = WealthOptimizationEngine.analyze(payslip)
        val opportunity = result.opportunities.find { it.id == "80c_dsop" }
        assertNull(opportunity, "80C DSOP opportunity should not exist when 80C limit is exceeded")
    }

    @Test
    fun testNpsOpportunityAlwaysPresent() {
        val payslip = createPayslip()
        val result = WealthOptimizationEngine.analyze(payslip)
        val nps = result.opportunities.find { it.id == "80ccd_nps" }
        assertTrue(nps != null, "NPS 80CCD(1B) opportunity must be included")
        assertEquals(50_000.0, nps.unusedAmount, 0.01)
    }

    @Test
    fun testNewRegimeUserGetsNoOldRegimeSectionOpportunities() {
        // D10: 80C/80CCD(1B) advice does not apply under NEW (Section 115BAC) even though headroom
        // would otherwise be positive -- gating must be on the active regime, not headroom alone.
        val payslip = createPayslip(dsopMonthly = 2_000.0, agifMonthly = 2_000.0, taxRegime = TaxRegime.NEW)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertNull(result.opportunities.find { it.id == "80c_dsop" }, "80C opportunity must not be offered under NEW regime")
        assertNull(result.opportunities.find { it.id == "80ccd_nps" }, "80CCD(1B) opportunity must not be offered under NEW regime")
    }

    @Test
    fun testNewRegimeExemptionBreakdownIsGatedButRegimeComparisonStaysCapped() {
        // D9: the OLD-regime hypothetical inside regimeComparison must still use the full capped
        // deductions (not the NEW-regime-gated, zeroed display breakdown) -- otherwise a NEW-regime
        // user's "switch to OLD" savings figure would be computed off zero deductions.
        val payslip = createPayslip(dsopMonthly = 8_000.0, agifMonthly = 5_000.0, taxRegime = TaxRegime.NEW)
        val result = WealthOptimizationEngine.analyze(payslip)

        assertEquals(0.0, result.exemptionBreakdown?.totalOldRegimeDeductions)
        val oldRegimeDeductionsUsedInComparison =
            (result.regimeComparison?.oldRegime?.totalDeductionsAndExemptions ?: 0.0) -
                (result.regimeComparison?.oldRegime?.standardDeduction ?: 0.0)
        assertTrue(oldRegimeDeductionsUsedInComparison > 0.0, "OLD-regime comparison must still see the capped, un-gated deductions")
    }

    @Test
    fun testRegimeIsDefaultOld() {
        val result = WealthOptimizationEngine.analyze(createPayslip())
        assertEquals("OLD", result.regimeAssumed)
    }

    @Test
    fun testModernTaxEngineOutputsPopulated() {
        val payslip = createPayslip()
        val result = WealthOptimizationEngine.analyze(payslip)

        assertNotNull(result.fySummary)
        assertNotNull(result.regimeComparison)
        assertNotNull(result.exemptionBreakdown)
        assertNotNull(result.tdsRunway)

        assertEquals("2025-26", result.fySummary?.financialYear)
        assertTrue(result.regimeComparison?.winnerRegime == "OLD" || result.regimeComparison?.winnerRegime == "NEW")
    }

    @Test
    fun testHandlesNullTaxAndSavings() {
        val payslip = createPayslip().copy(taxAndSavings = null)
        val result = WealthOptimizationEngine.analyze(payslip)
        assertEquals("OLD", result.regimeAssumed)
    }

    @Test
    fun testApr2026EndToEndAcceptance() {
        // docs/Plan/04_TaxPlannerGoldStandard.md Phase 3 acceptance, driven through the full
        // WealthOptimizationEngine pipeline (not just IncomeProjectionPolicy in isolation): total tax
        // ~6,29,025 vs PCDA's own 6,27,975 (within 0.2%), and the false "+32%" spike warning (D12) must
        // disappear as a consequence of the corrected inputs, not by suppressing the check.
        val payslip =
            ParsedPayslip(
                file = "04_apr_2026.pdf",
                year = 2026,
                monthNum = 4,
                monthName = "April",
                dateStr = "04/2026",
                officer = Officer("Officer", "16/000/000000X", "AR*****90G"),
                earnings =
                    Earnings(
                        basicPay = 149000.0,
                        dearnessAllowance = 98700.0,
                        militaryServicePay = 15500.0,
                        transportAllowance = 3600.0,
                        transportAllowanceDa = 2160.0,
                        riskHardshipAllowance = 21125.0,
                        arrearsDa = 9870.0,
                        arrearsTptaDa = 216.0,
                        adjPayAndAllce = 1657.0,
                    ),
                deductions =
                    Deductions(
                        dsopSubscription = 40000.0,
                        agif = 12500.0,
                        incomeTax = 50425.0,
                        educationCess = 2017.0,
                        ticketRecovery = 3056.0,
                    ),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(grossPay = 301828.0, totalDeductions = 107998.0, netRemittance = 193830.0),
                taxAndSavings =
                    TaxAndSavings(
                        grossSalaryYtd = 586894.0,
                        totalTaxableIncome = 3487744.0,
                        standardDeduction = 75000.0,
                        netTaxableIncome = 3412740.0,
                        totalTaxPayable = 603822.0,
                        taxDeductedYtd = 99567.0,
                        cessDeductedYtd = 3983.0,
                        taxRegime = TaxRegime.NEW,
                    ),
            )

        val result = WealthOptimizationEngine.analyzeLedger(listOf(payslip), payslip, "2026-27")

        val totalTax = result.regimeComparison?.newRegime?.totalTaxPayable ?: 0.0
        assertEquals(629025.07, totalTax, 1.0)
        assertTrue(kotlin.math.abs(totalTax - 627975.0) / 627975.0 < 0.002, "Expected within 0.2% of PCDA's 6,27,975, got $totalTax")
        assertTrue(result.tdsRunway?.hasTdsSpikeWarning == false, "D12: corrected inputs must not produce a false spike warning")
    }

    @Test
    fun testTdsRunwayUsesFyCalendarPositionNotUploadCountForRemainingMonths() {
        // D11/D12: with a gap (April uploaded, May missing, June present), the June payslip is 3 months
        // into the FY. The runway must split the remaining tax over 12-3=9 months, not 12-2(uploaded)=10 --
        // wiring `parsedMonthCount` here instead of `monthsElapsedInFy` would manufacture a false spike.
        val payslips =
            listOf(
                createPayslip().copy(monthNum = 4, dateStr = "04/2025"),
                createPayslip().copy(monthNum = 6, dateStr = "06/2025"),
            )
        val result = WealthOptimizationEngine.analyzeLedger(payslips, targetFy = "2025-26")
        assertEquals(3, result.fySummary?.monthsElapsedInFy)
        assertEquals(9, result.tdsRunway?.remainingMonths)
    }
}
