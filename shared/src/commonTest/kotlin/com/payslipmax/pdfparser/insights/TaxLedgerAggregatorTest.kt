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
import kotlin.test.assertTrue

class TaxLedgerAggregatorTest {
    private fun createPayslip(
        year: Int,
        monthNum: Int,
        grossPay: Double = 100000.0,
        basicPay: Double = 60000.0,
        da: Double = 30000.0,
        incomeTax: Double = 5000.0,
        educationCess: Double = 0.0,
        dsop: Double = 10000.0,
        agif: Double = 5000.0,
        fieldAllowance: Double = 4200.0,
        riskHardshipAllowance: Double = 0.0,
        rawEarnings: Map<String, Double> = emptyMap(),
        hra: Double = 8000.0,
        arrearsDa: Double = 0.0,
        arrearsTptaDa: Double = 0.0,
        adjPayAndAllce: Double = 0.0,
        adjTicketRecovery: Double = 0.0,
        taxDeductedYtd: Double = 0.0,
        cessDeductedYtd: Double = 0.0,
    ): ParsedPayslip {
        val monthStr = if (monthNum < 10) "0$monthNum" else "$monthNum"
        return ParsedPayslip(
            file = "payslip_$year$monthStr.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "Month$monthNum",
            dateStr = "$monthStr/$year",
            officer = Officer("John Doe", "12345", "ABCDE1234F"),
            earnings =
                Earnings(
                    basicPay = basicPay,
                    dearnessAllowance = da,
                    fieldAllowance = fieldAllowance,
                    riskHardshipAllowance = riskHardshipAllowance,
                    houseRentAllowance = hra,
                    arrearsDa = arrearsDa,
                    arrearsTptaDa = arrearsTptaDa,
                    adjPayAndAllce = adjPayAndAllce,
                    adjTicketRecovery = adjTicketRecovery,
                ),
            deductions =
                Deductions(
                    incomeTax = incomeTax,
                    educationCess = educationCess,
                    dsopSubscription = dsop,
                    agif = agif,
                ),
            ledgerBalances = LedgerBalances(),
            summary =
                PayslipSummary(
                    grossPay = grossPay,
                    totalDeductions = incomeTax + dsop + agif,
                    netRemittance = grossPay - (incomeTax + dsop + agif),
                ),
            taxAndSavings =
                TaxAndSavings(
                    grossSalaryYtd = grossPay,
                    taxDeductedYtd = taxDeductedYtd,
                    cessDeductedYtd = cessDeductedYtd,
                ),
            rawEarnings = rawEarnings,
        )
    }

    @Test
    fun testFinancialYearDetermination() {
        assertEquals("2024-25", TaxLedgerAggregator.computeFinancialYear(2024, 4))
        assertEquals("2024-25", TaxLedgerAggregator.computeFinancialYear(2024, 12))
        assertEquals("2024-25", TaxLedgerAggregator.computeFinancialYear(2025, 1))
        assertEquals("2025-26", TaxLedgerAggregator.computeFinancialYear(2025, 4))
    }

    @Test
    fun testFormatIndianCurrency() {
        assertEquals("8,02,444", TaxLedgerAggregator.formatIndianCurrency(802444.0))
        assertEquals("68,365", TaxLedgerAggregator.formatIndianCurrency(68365.0))
        assertEquals("70,200", TaxLedgerAggregator.formatIndianCurrency(70200.0))
        assertEquals("3,75,001", TaxLedgerAggregator.formatIndianCurrency(375001.0))
        assertEquals("500", TaxLedgerAggregator.formatIndianCurrency(500.0))
    }

    @Test
    fun testExtractRiskHardshipAllowanceKeepsSeparateFromField() {
        // D8: RH/field buckets must stay separate so each can be capped against its own Rule 2BB
        // category (Section10CapPolicy) -- a merged total "cannot be category-correct".
        val p1 = createPayslip(2024, 4, fieldAllowance = 0.0, riskHardshipAllowance = 6000.0)
        assertEquals(6000.0, TaxLedgerAggregator.extractRiskHardshipAllowance(p1))
        assertEquals(0.0, TaxLedgerAggregator.extractFieldAreaAllowance(p1))

        val p2 = createPayslip(2024, 4, fieldAllowance = 0.0, riskHardshipAllowance = 0.0, rawEarnings = mapOf("RHA" to 4200.0))
        assertEquals(4200.0, TaxLedgerAggregator.extractRiskHardshipAllowance(p2))
        assertEquals(0.0, TaxLedgerAggregator.extractFieldAreaAllowance(p2))
    }

    @Test
    fun testExtractFieldAreaAllowanceFallsBackToRawFieldKeyword() {
        val p = createPayslip(2024, 4, fieldAllowance = 0.0, riskHardshipAllowance = 0.0, rawEarnings = mapOf("FIELD ALLC" to 2700.0))
        assertEquals(2700.0, TaxLedgerAggregator.extractFieldAreaAllowance(p))
        assertEquals(0.0, TaxLedgerAggregator.extractRiskHardshipAllowance(p))
    }

    @Test
    fun testMultiMonthAggregationForFy() {
        val payslips =
            listOf(
                createPayslip(2024, 4, grossPay = 100000.0),
                createPayslip(2024, 5, grossPay = 100000.0),
                createPayslip(2024, 6, grossPay = 100000.0),
            )

        val summary = TaxLedgerAggregator.aggregateFy(payslips, targetFy = "2024-25")

        assertEquals("2024-25", summary.financialYear)
        assertEquals("2025-26", summary.assessmentYear)
        assertEquals(3, summary.parsedMonthCount)
        assertEquals(300000.0, summary.ytdGross)
        assertEquals(15000.0, summary.ytdTaxDeducted)
        assertEquals(30000.0, summary.ytdDsop)
        assertEquals(15000.0, summary.ytdAgif)
        assertEquals(12600.0, summary.ytdFieldAllowance)
        assertEquals(24000.0, summary.ytdHra)
    }

    @Test
    fun testTwelveMonthRunRateProjection() {
        val payslips =
            (4..9).map { month ->
                createPayslip(2024, month, grossPay = 100000.0, dsop = 10000.0, agif = 5000.0)
            }

        val summary = TaxLedgerAggregator.aggregateFy(payslips, targetFy = "2024-25")

        assertEquals(6, summary.parsedMonthCount)
        assertEquals(600000.0, summary.ytdGross)
        assertEquals(1200000.0, summary.projectedAnnualGross)
        assertEquals(120000.0, summary.projectedAnnualDsop)
        assertEquals(60000.0, summary.projectedAnnualAgif)
        assertEquals(50400.0, summary.projectedAnnualFieldAllowance)
    }

    @Test
    fun testMissingMonthsDetection() {
        val payslips =
            listOf(
                createPayslip(2024, 4),
                createPayslip(2024, 5),
                createPayslip(2024, 7),
            )

        val summary = TaxLedgerAggregator.aggregateFy(payslips, targetFy = "2024-25")

        assertEquals(3, summary.parsedMonthCount)
        assertTrue(summary.missingMonthNums.contains(6))
        assertEquals(9, summary.missingMonthNums.size)
    }

    // --- Phase 3: D4 arrears detection ---

    @Test
    fun testExtractNonRecurringArrearsReadsStructuredFields() {
        // Apr 2026 evidence-base payslip: ARR-DA 9,870 + ARR-TPTADA 216, both resolved into structured
        // `arrears*` fields at parse time -- the old keyword-list implementation summed these to zero
        // because they never reach `rawEarnings` (D4).
        val p = createPayslip(2026, 4, arrearsDa = 9870.0, arrearsTptaDa = 216.0)
        assertEquals(10086.0, TaxLedgerAggregator.extractNonRecurringArrears(p), 0.01)
    }

    @Test
    fun testExtractNonRecurringArrearsFallsBackToUnmappedArrPrefix() {
        val p = createPayslip(2024, 4, rawEarnings = mapOf("ARR-NEWCODE" to 500.0))
        assertEquals(500.0, TaxLedgerAggregator.extractNonRecurringArrears(p), 0.01)
    }

    @Test
    fun testExtractNonRecurringArrearsIgnoresUnrelatedRawEarnings() {
        val p = createPayslip(2024, 4, rawEarnings = mapOf("MISC ALLC" to 500.0))
        assertEquals(0.0, TaxLedgerAggregator.extractNonRecurringArrears(p), 0.01)
    }

    // --- Phase 3: D5 reimbursement exclusion ---

    @Test
    fun testExtractReimbursementsSumsAdjTicketRecoveryAndAdjPayAndAllce() {
        val p = createPayslip(2024, 4, adjTicketRecovery = 3056.0, adjPayAndAllce = 1657.0)
        assertEquals(4713.0, TaxLedgerAggregator.extractReimbursements(p), 0.01)
    }

    @Test
    fun testExtractReimbursementsFallsBackToUnmappedEtktCode() {
        val p = createPayslip(2024, 4, rawEarnings = mapOf("ETKT-NEWCODE" to 800.0))
        assertEquals(800.0, TaxLedgerAggregator.extractReimbursements(p), 0.01)
    }

    @Test
    fun testEtktReimbursementAppearingOnBothSidesNetsToZeroInProjection() {
        // A ticket reimbursement is credited (adjTicketRecovery, "ETKT-ref") then recovered in full via
        // the matching deduction ("ETKT" -> ticketRecovery) -- same amount, both sides. The credit side
        // must be excluded from the taxable projection entirely (D5), so a payslip whose gross happens to
        // include this pair projects identically to one that never had it.
        val reimbursedAmount = 3056.0
        val withReimbursement =
            createPayslip(2024, 4, grossPay = 103056.0, adjTicketRecovery = reimbursedAmount)
                .copy(deductions = Deductions(ticketRecovery = reimbursedAmount))
        val withoutReimbursement = createPayslip(2024, 4, grossPay = 100000.0)

        val summaryWith = TaxLedgerAggregator.aggregateFy(listOf(withReimbursement), targetFy = "2024-25")
        val summaryWithout = TaxLedgerAggregator.aggregateFy(listOf(withoutReimbursement), targetFy = "2024-25")

        assertEquals(reimbursedAmount, TaxLedgerAggregator.extractReimbursements(withReimbursement), 0.01)
        assertEquals(summaryWithout.projectedAnnualGross, summaryWith.projectedAnnualGross, 0.01)
    }

    // --- Phase 3: D11 months-elapsed-in-FY multiplier basis ---

    @Test
    fun testMonthsElapsedInFyUsesCalendarPositionNotUploadCount() {
        // Months 4 and 5 uploaded, then a gap, then 7 -- 3 payslips uploaded but July is 4 months into
        // the FY (Apr=1...Jul=4). The multiplier must annualise off 4, not 3, or the projection
        // understates whenever a month is missing before the latest upload (D11).
        val payslips =
            listOf(
                createPayslip(2024, 4, grossPay = 100000.0),
                createPayslip(2024, 5, grossPay = 100000.0),
                createPayslip(2024, 7, grossPay = 100000.0),
            )

        val summary = TaxLedgerAggregator.aggregateFy(payslips, targetFy = "2024-25")

        assertEquals(3, summary.parsedMonthCount)
        assertEquals(4, summary.monthsElapsedInFy)
        assertEquals(900000.0, summary.projectedAnnualGross, 0.01) // 300000 * (12/4)
    }

    // --- Phase 3: D11 YTD tax uses PCDA's own cumulative figure, cess included ---

    @Test
    fun testYtdTaxPrefersPcdaOwnCumulativeFigureIncludingCess() {
        val p = createPayslip(2026, 4, incomeTax = 50425.0, educationCess = 2017.0, taxDeductedYtd = 99567.0, cessDeductedYtd = 3983.0)
        val summary = TaxLedgerAggregator.aggregateFy(listOf(p), targetFy = "2026-27")
        assertEquals(103550.0, summary.ytdTaxDeducted, 0.01)
    }

    @Test
    fun testYtdTaxFallsBackToLedgerSumIncludingCessWhenPcdaFigureUnavailable() {
        val payslips =
            listOf(
                createPayslip(2024, 4, incomeTax = 5000.0, educationCess = 200.0),
                createPayslip(2024, 5, incomeTax = 5000.0, educationCess = 200.0),
            )
        val summary = TaxLedgerAggregator.aggregateFy(payslips, targetFy = "2024-25")
        assertEquals(10400.0, summary.ytdTaxDeducted, 0.01)
    }

    // --- Phase 3: end-to-end Apr 2026 evidence-base projection ---

    @Test
    fun testApr2026ProjectedGrossExcludesArrearsAnnualisationAndReimbursements() {
        // Apr 2026 evidence-base payslip (docs/Plan/04_TaxPlannerGoldStandard.md Phase 3 acceptance):
        // recurring components 2,90,085/mo (basic+DA+MSP+transport+transport DA+risk-hardship), ARR-DA
        // + ARR-TPTADA arrears of 10,086 added back verbatim, ETKT/"A/o Pay & Allce" reimbursements of
        // 1,657 dropped entirely. Only 1 month parsed (April = FY position 1) -> multiplier 12.
        val p =
            createPayslip(
                2026,
                4,
                grossPay = 301828.0,
                basicPay = 149000.0,
                da = 98700.0,
                fieldAllowance = 0.0,
                riskHardshipAllowance = 21125.0,
                hra = 0.0,
                arrearsDa = 9870.0,
                arrearsTptaDa = 216.0,
                adjPayAndAllce = 1657.0,
            )
        val summary = TaxLedgerAggregator.aggregateFy(listOf(p), targetFy = "2026-27")

        assertEquals(1, summary.monthsElapsedInFy)
        assertEquals(10086.0, TaxLedgerAggregator.extractNonRecurringArrears(p), 0.01)
        assertEquals(1657.0, TaxLedgerAggregator.extractReimbursements(p), 0.01)
        assertEquals(3491106.0, summary.projectedAnnualGross, 0.01)
    }
}
