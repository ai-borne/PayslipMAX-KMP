package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeterministicIntelligenceEngineTest {
    private fun createBaseRecord(
        dateStr: String = "04/2026",
        basicPay: Double = 105300.0,
        da: Double = 52500.0,
        msp: Double = 15500.0,
        hra: Double = 27000.0,
        tpta: Double = 3600.0,
        dsop: Double = 12000.0,
        tax: Double = 7200.0,
    ): LedgerRecordEntity {
        val gross = basicPay + da + msp + hra + tpta
        val net = gross - dsop - tax
        val parts = dateStr.split("/")
        val month = parts.getOrNull(0)?.toIntOrNull() ?: 4
        val yr = parts.getOrNull(1)?.toIntOrNull() ?: 2026
        return LedgerRecordEntity(
            dateStr = dateStr,
            year = yr,
            monthNum = month,
            basicPay = basicPay,
            dearnessAllowance = da,
            militaryServicePay = msp,
            transportAllowance = tpta,
            transportAllowanceDa = 1800.0,
            houseRentAllowance = hra,
            grossPay = gross,
            dsopSubscription = dsop,
            incomeTax = tax,
            netPay = net,
        )
    }

    private fun parsedPayslip(
        year: Int,
        month: Int,
        basicPay: Double,
    ) = ParsedPayslip(
        file = "t.pdf",
        year = year,
        monthNum = month,
        monthName = "",
        dateStr = "$month/$year",
        officer = Officer("N", "A", "P"),
        earnings = Earnings(basicPay = basicPay),
        deductions = Deductions(),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(0.0, 0.0, 0.0),
        taxAndSavings = null,
    )

    /** Pay Audit (docs/Plan/09_PayAudit_PhasePlan.md Phase 4): EngineResult must expose the timeline
     * it already builds internally, and the current month's change explanations derived from it. */
    @Test
    fun testEngineResultExposesTimelineAndChangeExplanations() {
        val previous = parsedPayslip(2018, 6, 82800.0)
        val current = parsedPayslip(2018, 7, 85300.0)

        val result = DeterministicIntelligenceEngine.analyze(current, previous, listOf(previous, current))

        assertTrue(result.timeline.months.isNotEmpty(), "EngineResult should expose the ServiceTimeline built for this run")
        val basicPayChange = result.changeExplanations.find { it.field == "basicPay" }
        assertNotNull(basicPayChange, "Should explain the basic-pay rise via the timeline-based increment rule")
        assertTrue(basicPayChange.reason!!.contains("increment", ignoreCase = true))
    }

    /** Pay Audit Phase 6: EngineResult must expose the next-increment prediction and DSOP room it derives
     * from the same timeline/history it already builds internally, with no user input. */
    @Test
    fun testEngineResultExposesIncrementPredictionAndDsopRoom() {
        val previous = parsedPayslip(2018, 6, 82800.0)
        val current = parsedPayslip(2018, 7, 85300.0)

        val result = DeterministicIntelligenceEngine.analyze(current, previous, listOf(previous, current))

        val prediction = assertNotNull(result.incrementPrediction, "should predict the next DNI from the increment just seen")
        assertEquals(2019, prediction.date.year)
        assertEquals(7, prediction.date.month)

        val dsopRoom = assertNotNull(result.dsopRoom, "DSOP room should always be computed, even at zero subscription")
        assertEquals("FY 2018-19", dsopRoom.financialYearLabel)
        assertEquals(500000.0, dsopRoom.roomLeft)
    }

    @Test
    fun testCleanLedgerNoAnomalies() {
        val current = createBaseRecord("05/2026")
        val previous = createBaseRecord("04/2026")

        val result = DeterministicIntelligenceEngine.analyze(current, previous)

        assertTrue(result.anomalies.isEmpty(), "A clean ledger should have no anomalies")
        assertEquals(100, result.healthScore, "A clean ledger with healthy savings should have 100 health score")
    }

    @Test
    fun testSalaryLossDetection() {
        val previous = createBaseRecord("04/2026", hra = 27000.0)
        val current = createBaseRecord("05/2026", hra = 0.0)

        val result = DeterministicIntelligenceEngine.analyze(current, previous)

        val salaryLossAnomaly = result.anomalies.find { it.type == "SALARY_LOSS" }
        val missingHraAnomaly = result.anomalies.find { it.type == "MISSING_ALLOWANCE" && it.field == "houseRentAllowance" }

        assertTrue(salaryLossAnomaly != null, "Should detect a salary loss anomaly")
        assertTrue(missingHraAnomaly != null, "Should detect a missing HRA allowance")
        assertTrue(result.healthScore < 100, "Health score should be lower than 100")
    }

    @Test
    fun testMissingTptaEntitlement() {
        val current = createBaseRecord("05/2026", basicPay = 56100.0, tpta = 0.0)

        val result = DeterministicIntelligenceEngine.analyze(current)

        val tptaAnomaly = result.anomalies.find { it.type == "TPTA_ENTITLEMENT" }
        assertTrue(tptaAnomaly != null, "Should flag missing TPTA entitlement for Level 10+")
    }

    @Test
    fun testDsopMandatoryMinimumViolation() {
        val current = createBaseRecord("05/2026", basicPay = 100000.0, dsop = 0.0)

        val result = DeterministicIntelligenceEngine.analyze(current)

        val dsopAnomaly = result.anomalies.find { it.type == "DSOP_COMPLIANCE" }
        assertTrue(dsopAnomaly != null, "Should flag zero DSOP contribution compliance error")
        assertTrue(result.healthScore <= 75, "Zero DSOP contribution should severely impact the score")
    }

    private fun parsedPayslipWithNetPay(
        year: Int,
        month: Int,
        netRemittance: Double,
        needsReview: Boolean = false,
    ) = parsedPayslip(year, month, 85300.0)
        .copy(summary = PayslipSummary(grossPay = netRemittance, totalDeductions = 0.0, netRemittance = netRemittance), needsReview = needsReview)

    /** Phase 8 P7-20: plain (non-[TimelineAuditor]) auditors never fire on a needsReview parse. */
    @Test
    fun testPlainAuditorsDoNotFireOnANeedsReviewMonth() {
        val previous = parsedPayslipWithNetPay(2026, 4, netRemittance = 100000.0)
        val current = parsedPayslipWithNetPay(2026, 5, netRemittance = 90000.0, needsReview = true)

        val result = DeterministicIntelligenceEngine.analyze(current, previous, emptyList())

        assertTrue(result.anomalies.none { it.type == "SALARY_LOSS" }, "SALARY_LOSS should not fire on a needsReview month")
    }

    @Test
    fun testTaxSpikeDeduction() {
        val previous = createBaseRecord("04/2026", tax = 5000.0)
        val current = createBaseRecord("05/2026", tax = 8000.0)

        val result = DeterministicIntelligenceEngine.analyze(current, previous)

        val taxAnomaly = result.anomalies.find { it.type == "DEDUCTION_SPIKE" && it.field == "incomeTax" }
        assertTrue(taxAnomaly != null, "Should detect a tax deduction spike")
    }
}
