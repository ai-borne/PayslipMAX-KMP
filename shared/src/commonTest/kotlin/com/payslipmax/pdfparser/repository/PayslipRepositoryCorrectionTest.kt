package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.toDomain
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Phase 5 — repository-level per-field correction behavior: corrections are persisted separately,
 * merged on read, and never mutate the stored encrypted parse.
 */
class PayslipRepositoryCorrectionTest {
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var repository: PayslipRepository

    @BeforeTest
    fun setUp() {
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
    }

    @Test
    fun testSaveCorrectionMergesOnReadWithoutMutatingStoredParse() =
        runTest {
            fakeParser.result = Result.success(createMockPayslip("08/2024"))
            repository.importPayslip(byteArrayOf(1), "pwd", "08-2024.pdf")

            repository.saveCorrection("08/2024", "basicPay", 555.0)

            val merged = repository.getAllPayslips().first().single()
            assertEquals(555.0, merged.earnings.basicPay)
            assertEquals(555.0, repository.getPayslipByDate("08/2024")!!.earnings.basicPay)

            // Stored encrypted parse is untouched (raw entity still has the original value).
            assertEquals(100.0, fakeDao.getPayslipByDate("08/2024")!!.toDomain().earnings.basicPay)
        }

    @Test
    fun testSaveCorrectionAccumulatesMultipleFields() =
        runTest {
            fakeParser.result = Result.success(createMockPayslip("09/2024"))
            repository.importPayslip(byteArrayOf(1), "pwd", "09-2024.pdf")

            repository.saveCorrection("09/2024", "basicPay", 200.0)
            repository.saveCorrection("09/2024", "incomeTax", 33.0)

            val merged = repository.getAllPayslips().first().single()
            assertEquals(200.0, merged.earnings.basicPay)
            assertEquals(33.0, merged.deductions.incomeTax)
        }

    @Test
    fun testDeletePayslipRemovesItsCorrections() =
        runTest {
            fakeParser.result = Result.success(createMockPayslip("10/2024"))
            repository.importPayslip(byteArrayOf(1), "pwd", "10-2024.pdf")
            repository.saveCorrection("10/2024", "basicPay", 1.0)

            repository.deletePayslip("10/2024")

            assertNull(fakeDao.getCorrectionByDate("10/2024"))
        }
}
