package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversationalTaxNarrativeEngineTest {
    private fun createPayslip(
        year: Int,
        monthNum: Int,
        monthName: String,
        grossPay: Double = 150000.0,
        incomeTax: Double = 12500.0,
        dsop: Double = 15000.0,
    ): ParsedPayslip {
        return ParsedPayslip(
            file = "payslip_${year}_$monthNum.pdf",
            year = year,
            monthNum = monthNum,
            monthName = monthName,
            dateStr = "$monthNum/$year",
            officer = Officer("John Doe", "12345", "ABCDE1234F"),
            earnings = Earnings(basicPay = 90000.0, dearnessAllowance = 45000.0),
            deductions = Deductions(incomeTax = incomeTax, dsopSubscription = dsop),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = grossPay, totalDeductions = incomeTax + dsop, netRemittance = grossPay - (incomeTax + dsop)),
            // taxDeductedYtd deliberately left at its 0.0 default: this fixture doesn't model PCDA's own
            // running YTD counter, so `TaxLedgerAggregator` should fall back to summing each month's
            // `incomeTax` (D11) -- setting it per-payslip would (wrongly) be read as PCDA's own cumulative
            // figure and short-circuit the sum this test exists to verify.
            taxAndSavings = TaxAndSavings(grossSalaryYtd = grossPay),
        )
    }

    @Test
    fun testNarrativeGenerationSingleMonth() {
        val p1 = createPayslip(2025, 4, "April 2025")
        val summary = TaxLedgerAggregator.aggregateFy(listOf(p1), "2025-26")

        val narrative =
            ConversationalTaxNarrativeEngine.generateNarrative(
                payslips = listOf(p1),
                fySummary = summary,
                projectedTax = 215400.0,
            )

        assertEquals("2025-26", narrative.financialYear)
        assertEquals("2026-27", narrative.assessmentYear)
        assertEquals(1, narrative.parsedMonthCount)
        assertEquals(1, narrative.monthlyLedgerList.size)
        assertEquals(12500.0, narrative.monthlyLedgerList[0].tdsDeducted)
        assertEquals(15000.0, narrative.monthlyLedgerList[0].dsopContribution)
        // D17: a single April payslip has zero *elapsed* months still missing -- the other 11 months of
        // the FY haven't been issued yet, so nudging "upload remaining 11 months" was never satisfiable.
        assertNull(narrative.missingMonthNudge)
        assertTrue(narrative.effectiveTaxRatePct > 0.0)
    }

    @Test
    fun testNarrativeGenerationWithGapProducesActionableNudge() {
        // April present, May skipped, June uploaded -- May is elapsed (position 2 <= monthsElapsedInFy 3)
        // and genuinely missing, so this is the one case where the nudge is both true and actionable.
        val payslips = listOf(createPayslip(2025, 4, "April 2025"), createPayslip(2025, 6, "June 2025"))
        val summary = TaxLedgerAggregator.aggregateFy(payslips, "2025-26")

        val narrative =
            ConversationalTaxNarrativeEngine.generateNarrative(
                payslips = payslips,
                fySummary = summary,
                projectedTax = 215400.0,
            )

        assertNotNull(narrative.missingMonthNudge)
        assertTrue(narrative.missingMonthNudge!!.contains("1"))
    }

    @Test
    fun testNarrativeGenerationMultiMonth() {
        val payslips =
            listOf(
                createPayslip(2025, 4, "April 2025"),
                createPayslip(2025, 5, "May 2025"),
                createPayslip(2025, 6, "June 2025"),
                createPayslip(2025, 7, "July 2025"),
            )
        val summary = TaxLedgerAggregator.aggregateFy(payslips, "2025-26")

        val narrative =
            ConversationalTaxNarrativeEngine.generateNarrative(
                payslips = payslips,
                fySummary = summary,
                projectedTax = 215400.0,
            )

        assertEquals(4, narrative.parsedMonthCount)
        assertEquals(4, narrative.monthlyLedgerList.size)
        assertEquals(50000.0, narrative.totalTdsYtd)
        assertEquals(60000.0, narrative.totalDsopYtd)
    }

    // --- buildBluf (Phase 8, U3) -------------------------------------------------------------

    private fun regimeDetail(totalTax: Double) =
        RegimeTaxDetail(
            regimeName = "NEW",
            grossIncome = 4_000_000.0,
            standardDeduction = 75_000.0,
            totalDeductionsAndExemptions = 75_000.0,
            netTaxableIncome = 3_500_000.0,
            baseTax = totalTax,
            surcharge = 0.0,
            cess = 0.0,
            totalTaxPayable = totalTax,
            effectiveTaxRatePct = 18.0,
        )

    private fun regimeComparison(winner: String = "NEW") =
        RegimeComparisonResult(
            financialYear = "2026-27",
            oldRegime = regimeDetail(793_552.0),
            newRegime = regimeDetail(629_025.0),
            winnerRegime = winner,
            annualSavings = 164_527.0,
            breakEvenDeduction = 0.0,
        )

    private fun matchedReconciliation() =
        TaxTrackReconciliation(
            financialYear = "2026-27",
            tdsTrackAnnual = 629_025.0,
            tdsTrackRegime = com.payslipmax.pdfparser.domain.TaxRegime.NEW,
            liabilityTrackAnnual = 629_025.0,
            liabilityTrackRegime = com.payslipmax.pdfparser.domain.TaxRegime.NEW,
            deltaAmount = 0.0,
            reconciliationType = ReconciliationType.MATCHED,
            message = "PCDA's withholding already matches your best achievable liability under the New Tax Regime.",
        )

    @Test
    fun buildBlufSaysNoActionNeededWhenNothingDiverges() {
        val bluf =
            ConversationalTaxNarrativeEngine.buildBluf(
                regimeComparison = regimeComparison(),
                reconciliation = matchedReconciliation(),
                dsopWasteInsight = null,
                midYearRegimeChange = null,
                parsedMonthCount = 6,
            )

        assertEquals(false, bluf.isActionRequired)
        assertTrue(bluf.headline.contains("about ₹6,29,000"))
        // TDS is jargon this screen must gloss the first time it appears, per Phase 8's plain-language scope.
        assertTrue(bluf.headline.contains("(TDS)"))
    }

    @Test
    fun buildBlufFlagsAndSurfacesReconciliationMessageWhenTracksDiverge() {
        val diverging = matchedReconciliation().copy(reconciliationType = ReconciliationType.TOP_UP_DUE, message = "You will owe extra under the New Tax Regime.")
        val bluf =
            ConversationalTaxNarrativeEngine.buildBluf(
                regimeComparison = regimeComparison(),
                reconciliation = diverging,
                dsopWasteInsight = null,
                midYearRegimeChange = null,
                parsedMonthCount = 6,
            )

        assertEquals(true, bluf.isActionRequired)
        assertEquals(diverging.message, bluf.actionLine)
    }

    @Test
    fun buildBlufFlagsAndSurfacesDsopWasteMessageWhenPresent() {
        val dsopWaste =
            DsopWasteInsight(
                annualDsop = 480_000.0,
                annualAgif = 150_000.0,
                cappedContribution = 150_000.0,
                taxBenefitForgoneAnnual = 45_000.0,
                message = "Your DSOP + AGIF earns ₹0 benefit under the New Tax Regime.",
            )
        val bluf =
            ConversationalTaxNarrativeEngine.buildBluf(
                regimeComparison = regimeComparison(),
                reconciliation = matchedReconciliation(),
                dsopWasteInsight = dsopWaste,
                midYearRegimeChange = null,
                parsedMonthCount = 6,
            )

        assertEquals(true, bluf.isActionRequired)
        assertEquals(dsopWaste.message, bluf.actionLine)
    }

    @Test
    fun buildBlufShowsRangeBelowLowCoverageThresholdAndPointAtOrAboveIt() {
        val lowCoverage =
            ConversationalTaxNarrativeEngine.buildBluf(
                regimeComparison = regimeComparison(),
                reconciliation = matchedReconciliation(),
                dsopWasteInsight = null,
                midYearRegimeChange = null,
                parsedMonthCount = 1,
            )
        assertTrue(lowCoverage.headline.contains("between ₹"))

        val fullCoverage =
            ConversationalTaxNarrativeEngine.buildBluf(
                regimeComparison = regimeComparison(),
                reconciliation = matchedReconciliation(),
                dsopWasteInsight = null,
                midYearRegimeChange = null,
                parsedMonthCount = TaxLedgerAggregator.LOW_COVERAGE_MONTHS,
            )
        assertTrue(fullCoverage.headline.contains("about ₹"))
    }
}
