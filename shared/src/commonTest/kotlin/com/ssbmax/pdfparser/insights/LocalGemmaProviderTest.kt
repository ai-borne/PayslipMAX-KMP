package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalGemmaProviderTest {

    private fun createPayload(anomalies: List<Anomaly>): PromptPayload {
        val dummyPayslip = ParsedPayslip(
            file = "test.pdf",
            year = 2026,
            monthNum = 4,
            monthName = "April",
            dateStr = "04/2026",
            officer = Officer("Officer Officer", "CDA-12345", "PAN12345"),
            earnings = Earnings(basicPay = 60000.0),
            deductions = Deductions(dsopSubscription = 5000.0),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(65000.0, 5000.0, 60000.0),
            taxAndSavings = null
        )
        return PromptPayload(
            currentMonthRawText = "",
            sanitizedJsonData = "",
            historicalSummaryText = "",
            anomaliesCount = anomalies.size,
            sanitizedPayslip = dummyPayslip,
            engineResult = EngineResult(100, anomalies, 8.3, 0.0)
        )
    }

    @Test
    fun testEmptyAnomaliesDynamicReport() = runTest {
        val provider = LocalGemmaProvider()
        val payload = createPayload(emptyList())

        val result = provider.generateInsights(payload)
        assertTrue(result.isSuccess)

        val jsonStr = result.getOrThrow()
        val report = AiInsightReportParser.parse(jsonStr)
        assertNotNull(report)

        // Basic Pay should be present and marked unchanged by default
        assertEquals(1, report.salaryChanges.size)
        assertEquals("Basic Pay", report.salaryChanges[0].item)
        assertEquals("unchanged", report.salaryChanges[0].change)
        assertEquals(0.0, report.salaryChanges[0].amount)

        // Opportunities should be populated from the WealthOptimizationEngine
        assertTrue(report.opportunities.isNotEmpty())
        assertTrue(report.riskAlerts.any { it.observation.contains("Offline local audit completed") })
    }

    @Test
    fun testAnomaliesMappingToReport() = runTest {
        val provider = LocalGemmaProvider()
        val anomalies = listOf(
            Anomaly("SALARY_LOSS", "Basic Pay", 5000.0, "04/2026", "Basic Pay reduced by 5000"),
            Anomaly("MISSING_ALLOWANCE", "Dearness Allowance", 2000.0, "04/2026", "Dearness Allowance missing"),
            Anomaly("DSOP_COMPLIANCE", "DSOP", 0.0, "04/2026", "DSOP subscription below 6%")
        )
        val payload = createPayload(anomalies)

        val result = provider.generateInsights(payload)
        assertTrue(result.isSuccess)

        val jsonStr = result.getOrThrow()
        val report = AiInsightReportParser.parse(jsonStr)
        assertNotNull(report)

        // Salary changes should capture Basic Pay decrease
        val salaryChange = report.salaryChanges.find { it.item == "Basic Pay" }
        assertNotNull(salaryChange)
        assertEquals("decreased", salaryChange.change)
        assertEquals(5000.0, salaryChange.amount)

        // Missing allowances should capture Dearness Allowance
        assertTrue(report.missingAllowances.contains("Dearness Allowance"))

        // DSOP compliance warning should go to riskAlerts
        assertTrue(report.riskAlerts.any { it.observation.contains("DSOP subscription non-compliance") })

        // Check action required
        assertTrue(report.actionRequired.any { it.contains("salary reduction") })
        assertTrue(report.actionRequired.any { it.contains("missing Dearness Allowance") })
    }
}
