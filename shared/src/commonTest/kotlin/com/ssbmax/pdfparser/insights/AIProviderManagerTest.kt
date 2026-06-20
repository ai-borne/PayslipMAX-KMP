package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.domain.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AIProviderManagerTest {
    private class FakeProvider(val response: String) : AIInsightProvider {
        var called = false
        override suspend fun generateInsights(payload: PromptPayload): Result<String> {
            called = true
            return Result.success(response)
        }
    }

    private fun createEmptyPayload(): PromptPayload {
        val dummyPayslip = ParsedPayslip(
            file = "test.pdf",
            year = 2026,
            monthNum = 4,
            monthName = "April",
            dateStr = "04/2026",
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(0.0, 0.0, 0.0),
            taxAndSavings = null
        )
        return PromptPayload(
            currentMonthRawText = "",
            sanitizedJsonData = "",
            historicalSummaryText = "",
            anomaliesCount = 0,
            sanitizedPayslip = dummyPayslip,
            engineResult = EngineResult(100, emptyList(), 0.0, 0.0)
        )
    }

    @Test
    fun testCloudProviderSelectedByDefault() = runTest {
        val cloud = FakeProvider("cloud response")
        val local = FakeProvider("local response")
        val manager = AIProviderManager(cloud, local, useLocalAi = false)

        val result = manager.generateInsights(createEmptyPayload())

        assertTrue(cloud.called)
        assertTrue(!local.called)
        assertEquals("cloud response", result.getOrNull())
    }

    @Test
    fun testLocalProviderSelectedWhenEnabled() = runTest {
        val cloud = FakeProvider("cloud response")
        val local = FakeProvider("local response")
        val manager = AIProviderManager(cloud, local, useLocalAi = true)

        val result = manager.generateInsights(createEmptyPayload())

        assertTrue(!cloud.called)
        assertTrue(local.called)
        assertEquals("local response", result.getOrNull())
    }
}
