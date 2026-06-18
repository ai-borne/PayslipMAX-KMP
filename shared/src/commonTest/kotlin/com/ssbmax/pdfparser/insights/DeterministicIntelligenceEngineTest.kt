package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import kotlin.test.Test
import kotlin.test.assertEquals
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
        return LedgerRecordEntity(
            dateStr = dateStr,
            year = 2026,
            monthNum = 4,
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

    @Test
    fun testTaxSpikeDeduction() {
        val previous = createBaseRecord("04/2026", tax = 5000.0)
        val current = createBaseRecord("05/2026", tax = 8000.0)

        val result = DeterministicIntelligenceEngine.analyze(current, previous)

        val taxAnomaly = result.anomalies.find { it.type == "DEDUCTION_SPIKE" && it.field == "incomeTax" }
        assertTrue(taxAnomaly != null, "Should detect a tax deduction spike")
    }
}
