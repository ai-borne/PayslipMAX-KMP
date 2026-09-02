package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.insights.*
import com.payslipmax.pdfparser.testing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class FinancialIntelligenceRepositoryTest {
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var repository: FinancialIntelligenceRepository

    @BeforeTest
    fun setUp() {
        fakeDao = FakePayslipDao()
        repository =
            FinancialIntelligenceRepository(
                payslipDao = fakeDao,
                dispatcher = Dispatchers.Unconfined,
            )
    }

    @Test
    fun testProcessPayslipAndRunAnalysisInsertsLedgerRecord() =
        runTest {
            val mockPayslip = createMockPayslip("05/2026", basicPay = 60000.0, dsop = 5000.0)

            val result = repository.processPayslipAndRunAnalysis(mockPayslip)

            assertNotNull(result)
            val ledgerRecords = repository.getAllLedgerRecords().first()
            assertEquals(1, ledgerRecords.size)
            assertEquals("05/2026", ledgerRecords.first().dateStr)
            assertEquals(60000.0, ledgerRecords.first().basicPay)
        }

    @Test
    fun testProcessPayslipGeneratesRepresentationDraftAndInsightsForMissingTPTA() =
        runTest {
            // Basic pay >= 56100 but TPTA is 0.0 -> TPTA_ENTITLEMENT anomaly
            val mockPayslip = createMockPayslip("05/2026", basicPay = 60000.0, tpta = 0.0)

            val result = repository.processPayslipAndRunAnalysis(mockPayslip)

            // Verify anomaly was detected
            val hasTptaAnomaly = result.anomalies.any { it.type == "TPTA_ENTITLEMENT" }
            assertTrue(hasTptaAnomaly)

            // Verify insight was inserted
            val insights = repository.getAllFinancialInsights().first()
            assertTrue(insights.any { it.category == "ALLOWANCE" && it.title == "TPTA Entitlement Advisory" })

            // Verify representation draft was generated
            val drafts = repository.getAllRepresentationDrafts().first()
            assertEquals(1, drafts.size)
            assertEquals("TPTA_ENTITLEMENT", drafts.first().disputeType)
            assertTrue(drafts.first().subject.contains("Transport Allowance (TPTA)"))
        }

    @Test
    fun testInsertAndGetAndDeleteRepresentationDraft() =
        runTest {
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
            repository.insertRepresentationDraft(draft)

            // Get
            val retrieved = repository.getRepresentationDraftById("draft123")
            assertNotNull(retrieved)
            assertEquals("draft123", retrieved.id)
            assertEquals("05/2026", retrieved.disputeMonth)

            // Delete
            repository.deleteRepresentationDraft("draft123")
            val retrievedAfterDelete = repository.getRepresentationDraftById("draft123")
            assertNull(retrievedAfterDelete)
        }

    private fun createMockPayslip(
        dateStr: String,
        basicPay: Double = 56100.0,
        tpta: Double = 3600.0,
        dsop: Double = 5000.0,
    ): ParsedPayslip {
        val (monthNum, year) = dateStr.split("/").let { it[0].toInt() to it[1].toInt() }
        return ParsedPayslip(
            file = "payslip.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "May",
            dateStr = dateStr,
            officer = Officer(name = "Test Officer", accountNo = "12345", pan = "ABCDE1234F"),
            earnings = Earnings(basicPay = basicPay, transportAllowance = tpta),
            deductions = Deductions(dsopSubscription = dsop),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = basicPay + tpta, totalDeductions = dsop, netRemittance = basicPay + tpta - dsop),
            taxAndSavings = null,
        )
    }
}
