package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.insights.Anomaly
import com.ssbmax.pdfparser.insights.EngineResult
import com.ssbmax.pdfparser.insights.OptimizationResult
import kotlin.test.Test
import kotlin.test.assertTrue

class FreeInsightsLogicTest {
    private fun makeLedgerRecord(
        dateStr: String = "02/2026",
        netPay: Double = 100000.0,
        grossPay: Double = 120000.0,
        basicPay: Double = 60000.0,
        dearnessAllowance: Double = 30000.0,
        transportAllowance: Double = 3600.0,
        houseRentAllowance: Double = 15000.0,
        dsop: Double = 10000.0,
        tax: Double = 15000.0,
    ) = LedgerRecordEntity(
        dateStr = dateStr,
        year = dateStr.split("/")[1].toInt(),
        monthNum = dateStr.split("/")[0].toInt(),
        basicPay = basicPay,
        dearnessAllowance = dearnessAllowance,
        militaryServicePay = 15500.0,
        transportAllowance = transportAllowance,
        transportAllowanceDa = 1000.0,
        houseRentAllowance = houseRentAllowance,
        grossPay = grossPay,
        dsopSubscription = dsop,
        incomeTax = tax,
        netPay = netPay,
    )

    private fun makeInsightsState(
        current: LedgerRecordEntity,
        previous: LedgerRecordEntity?,
        anomalies: List<Anomaly> = emptyList(),
    ) = InsightsState(
        currentRecord = current,
        previousRecord = previous,
        historySorted = listOfNotNull(previous, current),
        engineResult = EngineResult(85, anomalies, 10.0, 5.0),
        scoreDelta = 0,
        optimizationResult = OptimizationResult(12000.0, 0.20, "OLD", emptyList(), 1000.0, 50000.0),
        momChanges = emptyList(),
    )

    @Test
    fun testKeyFindingsStable() {
        val curr = makeLedgerRecord()
        val prev = makeLedgerRecord("01/2026")
        val state = makeInsightsState(curr, prev)

        val findings = calculateKeyFindings(state)
        assertTrue(findings.any { it.text == "Net Salary stable" })
        assertTrue(findings.any { it.text == "Income Tax stable" })
        assertTrue(findings.any { it.text == "DSOP contribution stable" })
    }

    @Test
    fun testKeyFindingsSalaryIncreaseAndTaxSpike() {
        val curr = makeLedgerRecord(netPay = 111673.0, tax = 20000.0)
        val prev = makeLedgerRecord("01/2026", netPay = 100000.0, tax = 10000.0) // 100% tax increase
        val state = makeInsightsState(curr, prev)

        val findings = calculateKeyFindings(state)
        assertTrue(findings.any { it.text.contains("Salary increased") })
        assertTrue(findings.any { it.text.contains("Income Tax increased by 100%") })
    }

    @Test
    fun testKeyFindingsMissingAllowance() {
        val curr = makeLedgerRecord()
        val prev = makeLedgerRecord("01/2026")
        val anomaly = Anomaly("MISSING_ALLOWANCE", "transportAllowance", 3600.0, "02/2026", "Transport allowance missing")
        val state = makeInsightsState(curr, prev, listOf(anomaly))

        val findings = calculateKeyFindings(state)
        assertTrue(findings.any { it.text.contains("TransportAllowance missing") })
    }

    @Test
    fun testAiHighlightsLargestDeductionAndSalaryChange() {
        val curr = makeLedgerRecord(basicPay = 75000.0) // increased basic by 15000
        val prev = makeLedgerRecord("01/2026", basicPay = 60000.0)
        val state = makeInsightsState(curr, prev)

        val highlights = calculateAiHighlights(state)
        assertTrue(highlights.any { it.text.contains("Largest deduction this month: Income Tax") })
        assertTrue(highlights.any { it.text.contains("Biggest component change: Basic Pay") })
    }
}
