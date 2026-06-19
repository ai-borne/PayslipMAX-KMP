package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.database.*
import com.ssbmax.pdfparser.domain.*
import com.ssbmax.pdfparser.insights.*
import com.ssbmax.pdfparser.testing.*
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

class FinancialIntelligenceRepositoryTest {
    private lateinit var fakeDao: FakePayslipDao

    @BeforeTest
    fun setUp() {
        fakeDao = FakePayslipDao()
    }

    private fun createRepositoryWithMockEngine(
        respondJson: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): FinancialIntelligenceRepository {
        val mockEngine =
            MockEngine { _ ->
                respond(
                    content = respondJson,
                    status = status,
                    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                )
            }

        val client =
            HttpClient(mockEngine) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                            isLenient = true
                        },
                    )
                }
            }

        val geminiProxyService = GeminiProxyService(client)
        return FinancialIntelligenceRepository(
            payslipDao = fakeDao,
            geminiProxyService = geminiProxyService,
            dispatcher = kotlinx.coroutines.Dispatchers.Unconfined,
        )
    }

    @Test
    fun testProcessPayslipAndRunAnalysisInsertsLedgerRecord() =
        runTest {
            val repo = createRepositoryWithMockEngine("""{"success":true,"narrative":"Mock Narrative"}""")
            val mockPayslip = createMockPayslip("05/2026", basicPay = 60000.0, dsop = 5000.0)

            val result = repo.processPayslipAndRunAnalysis(mockPayslip)

            assertNotNull(result)
            val ledgerRecords = repo.getAllLedgerRecords().first()
            assertEquals(1, ledgerRecords.size)
            assertEquals("05/2026", ledgerRecords.first().dateStr)
            assertEquals(60000.0, ledgerRecords.first().basicPay)
        }

    @Test
    fun testProcessPayslipGeneratesRepresentationDraftAndInsightsForMissingTPTA() =
        runTest {
            val repo = createRepositoryWithMockEngine("""{"success":true,"narrative":"Mock Narrative"}""")
            // Basic pay >= 56100 but TPTA is 0.0 -> TPTA_ENTITLEMENT anomaly
            val mockPayslip = createMockPayslip("05/2026", basicPay = 60000.0, tpta = 0.0)

            val result = repo.processPayslipAndRunAnalysis(mockPayslip)

            // Verify anomaly was detected
            val hasTptaAnomaly = result.anomalies.any { it.type == "TPTA_ENTITLEMENT" }
            assertTrue(hasTptaAnomaly)

            // Verify insight was inserted
            val insights = repo.getAllFinancialInsights().first()
            assertTrue(insights.any { it.category == "ALLOWANCE" && it.title == "TPTA Entitlement Advisory" })

            // Verify representation draft was generated
            val drafts = repo.getAllRepresentationDrafts().first()
            assertEquals(1, drafts.size)
            assertEquals("TPTA_ENTITLEMENT", drafts.first().disputeType)
            assertTrue(drafts.first().subject.contains("Transport Allowance (TPTA)"))
        }

    @Test
    fun testGenerateNarrativeInsightsSuccess() =
        runTest {
            val responsePayload =
                """
                {
                    "success": true,
                    "narrative": "Detailed financial recommendations based on your payslip data."
                }
                """.trimIndent()
            val repo = createRepositoryWithMockEngine(responsePayload)
            val mockPayslip = createMockPayslip("05/2026")
            val engineResult = EngineResult(healthScore = 90, anomalies = emptyList(), monthlySavingRate = 10.0, taxRatio = 5.0)

            val result = repo.generateNarrativeInsights(mockPayslip, engineResult)

            assertTrue(result.isSuccess)
            assertEquals("Detailed financial recommendations based on your payslip data.", result.getOrNull())

            // Verify narrative insight is stored in database
            val insights = repo.getAllFinancialInsights().first()
            val narrativeInsight = insights.firstOrNull { it.category == "NARRATIVE" }
            assertNotNull(narrativeInsight)
            assertEquals("Detailed financial recommendations based on your payslip data.", narrativeInsight.contentMarkdown)
        }

    @Test
    fun testGenerateNarrativeInsightsFailureResponse() =
        runTest {
            val responsePayload =
                """
                {
                    "success": false,
                    "error": "Rate limit exceeded"
                }
                """.trimIndent()
            val repo = createRepositoryWithMockEngine(responsePayload)
            val mockPayslip = createMockPayslip("05/2026")
            val engineResult = EngineResult(healthScore = 95, anomalies = emptyList(), monthlySavingRate = 12.0, taxRatio = 4.0)

            val result = repo.generateNarrativeInsights(mockPayslip, engineResult)

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Rate limit exceeded") == true)
        }

    private fun createMockPayslip(
        dateStr: String,
        basicPay: Double = 50000.0,
        dsop: Double = 3000.0,
        tpta: Double = 3600.0,
    ): ParsedPayslip {
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
            earnings =
                Earnings(
                    basicPay = basicPay,
                    dearnessAllowance = 10000.0,
                    militaryServicePay = 15500.0,
                    transportAllowance = tpta,
                    transportAllowanceDa = 1000.0,
                    houseRentAllowance = 12000.0,
                ),
            deductions =
                Deductions(
                    dsopSubscription = dsop,
                    agif = 5000.0,
                    incomeTax = 4000.0,
                ),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(102100.0, 14000.0, 88100.0),
            taxAndSavings = null,
        )
    }

    @Test
    fun testInsertAndGetAndDeleteRepresentationDraft() =
        runTest {
            val repo = createRepositoryWithMockEngine("""{"success":true,"narrative":"Mock Narrative"}""")
            val draft =
                RepresentationDraftEntity(
                    id = "draft123",
                    disputeMonth = "05/2026",
                    disputeType = "MISSING_ALLOWANCE",
                    recipient = "PCDA_O_PUNE",
                    subject = "Subject",
                    bodyText = "Body text",
                    createdAt = 123456789L,
                )

            // Insert
            repo.insertRepresentationDraft(draft)

            // Get
            val retrieved = repo.getRepresentationDraftById("draft123")
            assertNotNull(retrieved)
            assertEquals("draft123", retrieved.id)
            assertEquals("05/2026", retrieved.disputeMonth)

            // Delete
            repo.deleteRepresentationDraft("draft123")
            val retrievedAfterDelete = repo.getRepresentationDraftById("draft123")
            assertNull(retrievedAfterDelete)
        }
}
