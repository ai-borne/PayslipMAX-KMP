package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.database.*
import com.ssbmax.pdfparser.domain.*
import com.ssbmax.pdfparser.insights.*
import com.ssbmax.pdfparser.testing.FakePayslipDao
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.*

class AiReportCachingTest {
    private lateinit var fakeDao: FakePayslipDao

    @BeforeTest
    fun setUp() {
        fakeDao = FakePayslipDao()
    }

    private fun createRepository(respondJson: String): FinancialIntelligenceRepository {
        val mockEngine = MockEngine { _ ->
            respond(
                content = respondJson,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return FinancialIntelligenceRepository(
            payslipDao = fakeDao,
            geminiProxyService = GeminiProxyService(client),
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )
    }

    @Test
    fun testGenerateAndRetrieveReport() = runTest {
        val repo = createRepository("""{"success":true,"narrative":"Mock Narrative Report 2026"}""")
        val mockPayslip = createMockPayslip("02/2026")
        val engineResult = EngineResult(90, emptyList(), 15.0, 5.0)

        // Generate report
        val genResult = repo.generateNarrativeInsights(mockPayslip, engineResult)
        assertTrue(genResult.isSuccess)

        // Verify it was stored in the new table
        val savedReport = fakeDao.getAiInsightReportByMonth("02/2026")
        assertNotNull(savedReport)
        assertEquals("Mock Narrative Report 2026", savedReport.reportJSON)
        assertEquals("1.0.0", savedReport.reportVersion)

        // Verify getCachedAiInsights loads from new table
        val cached = repo.getCachedAiInsights("02/2026")
        assertEquals("Mock Narrative Report 2026", cached)

        // Verify getAllAiInsightReports exposes it
        val allReports = repo.getAllAiInsightReports().first()
        assertEquals(1, allReports.size)
        assertEquals("02/2026", allReports.first().payslipMonth)
    }

    @Test
    fun testFallbackToLegacyFinancialInsight() = runTest {
        val repo = createRepository("""{}""")
        
        // Directly seed legacy narrative insight in database
        val legacyInsight = FinancialInsightEntity(
            id = "legacy_id",
            monthStr = "01/2026",
            category = "NARRATIVE",
            title = "Legacy Monthly Audit",
            contentMarkdown = "Legacy Narrative Markdown",
            severity = "INFO",
            createdAt = 123456789L
        )
        fakeDao.insertFinancialInsight(legacyInsight)

        // Verify new table has nothing
        val savedReport = fakeDao.getAiInsightReportByMonth("01/2026")
        assertNull(savedReport)

        // Verify getCachedAiInsights falls back to financial_insights table
        val cached = repo.getCachedAiInsights("01/2026")
        assertEquals("Legacy Narrative Markdown", cached)
    }

    private fun createMockPayslip(dateStr: String): ParsedPayslip {
        val split = dateStr.split("/")
        val month = split[0].toInt()
        val year = split[1].toInt()
        return ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = year,
            monthNum = month,
            monthName = "Month_$month",
            dateStr = dateStr,
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(50000.0, 10000.0, 15500.0, 3600.0, 1000.0, 12000.0),
            deductions = Deductions(3000.0, 5000.0, 4000.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(102100.0, 14000.0, 88100.0),
            taxAndSavings = null,
        )
    }
}
