package com.ssbmax.pdfparser.database

import com.ssbmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PayslipDaoTest {
    private val dao = FakePayslipDao()

    @Test
    fun testLedgerRecordOperations() =
        runTest {
            val record =
                LedgerRecordEntity(
                    dateStr = "05/2026",
                    year = 2026,
                    monthNum = 5,
                    basicPay = 105300.0,
                    dearnessAllowance = 52500.0,
                    militaryServicePay = 15500.0,
                    transportAllowance = 3600.0,
                    transportAllowanceDa = 1800.0,
                    houseRentAllowance = 27000.0,
                    grossPay = 186000.0,
                    dsopSubscription = 12000.0,
                    incomeTax = 7200.0,
                    netPay = 145000.0,
                )

            dao.insertLedgerRecord(record)

            val fetched = dao.getLedgerRecordByDate("05/2026")
            assertNotNull(fetched)
            assertEquals(105300.0, fetched.basicPay)

            val allRecords = dao.getAllLedgerRecords().first()
            assertEquals(1, allRecords.size)

            dao.deleteLedgerRecord("05/2026")
            val deleted = dao.getLedgerRecordByDate("05/2026")
            assertNull(deleted)
        }

    @Test
    fun testFinancialInsightOperations() =
        runTest {
            val insight =
                FinancialInsightEntity(
                    id = "insight_05_2026",
                    monthStr = "05/2026",
                    category = "ALLOWANCE",
                    title = "Missing HRA",
                    contentMarkdown = "HRA is missing",
                    severity = "WARNING",
                    createdAt = 1718625600000L,
                )

            dao.insertFinancialInsight(insight)

            val fetched = dao.getFinancialInsightsByMonth("05/2026")
            assertEquals(1, fetched.size)
            assertEquals("Missing HRA", fetched[0].title)

            dao.deleteFinancialInsight("insight_05_2026")
            val afterDelete = dao.getFinancialInsightsByMonth("05/2026")
            assertTrue(afterDelete.isEmpty())
        }

    @Test
    fun testRepresentationDraftOperations() =
        runTest {
            val draft =
                RepresentationDraftEntity(
                    id = "draft_05_2026_MISSING_HRA",
                    disputeMonth = "05/2026",
                    disputeType = "MISSING_HRA",
                    recipient = "PCDA_O_PUNE",
                    subject = "Discrepancy in HRA",
                    bodyText = "Dear Sir, HRA is missing",
                    createdAt = 1718625600000L,
                )

            dao.insertRepresentationDraft(draft)

            val fetched = dao.getRepresentationDraftById("draft_05_2026_MISSING_HRA")
            assertNotNull(fetched)
            assertEquals("PCDA_O_PUNE", fetched.recipient)

            dao.deleteRepresentationDraft("draft_05_2026_MISSING_HRA")
            val afterDelete = dao.getRepresentationDraftById("draft_05_2026_MISSING_HRA")
            assertNull(afterDelete)
        }
}
