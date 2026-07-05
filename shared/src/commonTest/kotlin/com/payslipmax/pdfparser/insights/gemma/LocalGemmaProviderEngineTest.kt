package com.payslipmax.pdfparser.insights.gemma

import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.insights.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class LocalGemmaProviderEngineTest {
    private fun createPayload(): PromptPayload {
        val dummyPayslip =
            ParsedPayslip(
                file = "test.pdf",
                year = 2026,
                monthNum = 4,
                monthName = "April",
                dateStr = "04/2026",
                officer = Officer("Test Officer", "CDA-123", "PAN123"),
                earnings = Earnings(basicPay = 50000.0),
                deductions = Deductions(dsopSubscription = 4000.0),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(54000.0, 4000.0, 50000.0),
                taxAndSavings = null,
            )
        return PromptPayload(
            currentMonthRawText = "",
            sanitizedJsonData = "",
            historicalSummaryText = "",
            anomaliesCount = 0,
            sanitizedPayslip = dummyPayslip,
            engineResult = EngineResult(100, emptyList(), 0.0, 0.0),
        )
    }

    @Test
    fun testProviderDelegatesToMockEngine() =
        runTest {
            val config = GemmaEngineConfig(modelPath = "models/gemma3-1b.task")
            val mockEngine = MockGemmaEngine(config, mockResponse = "{\"status\":\"ok\"}")
            val provider = LocalGemmaProvider(mockEngine)
            val result = provider.generateInsights(createPayload())

            assertTrue(result.isSuccess)
            val json = result.getOrThrow()
            assertTrue(json.contains("salaryChanges"))
        }
}
