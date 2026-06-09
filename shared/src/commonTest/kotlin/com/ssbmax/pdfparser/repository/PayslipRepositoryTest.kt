package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.domain.*
import com.ssbmax.pdfparser.testing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class PayslipRepositoryTest {
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var repository: PayslipRepository

    @BeforeTest
    fun setUp() {
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        repository = PayslipRepository(fakeDao, fakeParser, kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @Test
    fun testGetAllPayslipsInitiallyEmpty() =
        runTest {
            val payslips = repository.getAllPayslips().first()
            assertTrue(payslips.isEmpty())
        }

    @Test
    fun testImportPayslipSuccess() =
        runTest {
            val mockPayslip = createMockPayslip("08/2024")
            fakeParser.result = Result.success(mockPayslip)

            val result =
                repository.importPayslip(
                    pdfBytes = byteArrayOf(1, 2, 3),
                    password = "test-password",
                    filename = "08-2024.pdf",
                )

            assertTrue(result.isSuccess)
            assertEquals(mockPayslip, result.getOrNull())

            // Verify it was inserted in the DAO
            val allDbPayslips = repository.getAllPayslips().first()
            assertEquals(1, allDbPayslips.size)
            assertEquals("08/2024", allDbPayslips.first().dateStr)
        }

    @Test
    fun testImportPayslipFailure() =
        runTest {
            val exception = Exception("Decryption failed")
            fakeParser.result = Result.failure(exception)

            val result =
                repository.importPayslip(
                    pdfBytes = byteArrayOf(1, 2, 3),
                    password = "wrong-password",
                    filename = "08-2024.pdf",
                )

            assertTrue(result.isFailure)
            assertEquals(exception, result.exceptionOrNull())

            // Verify nothing was inserted in the DAO
            val allDbPayslips = repository.getAllPayslips().first()
            assertTrue(allDbPayslips.isEmpty())
        }

    @Test
    fun testGetPayslipByDate() =
        runTest {
            val mockPayslip = createMockPayslip("05/2024")
            fakeParser.result = Result.success(mockPayslip)
            repository.importPayslip(byteArrayOf(), "pass", "file.pdf")

            val found = repository.getPayslipByDate("05/2024")
            assertNotNull(found)
            assertEquals("05/2024", found.dateStr)

            val notFound = repository.getPayslipByDate("06/2024")
            assertNull(notFound)
        }

    @Test
    fun testDeletePayslip() =
        runTest {
            val mockPayslip = createMockPayslip("05/2024")
            fakeParser.result = Result.success(mockPayslip)
            repository.importPayslip(byteArrayOf(), "pass", "file.pdf")

            // Assert inserted
            assertNotNull(repository.getPayslipByDate("05/2024"))

            // Delete
            repository.deletePayslip("05/2024")

            // Assert deleted
            assertNull(repository.getPayslipByDate("05/2024"))
        }

    @Test
    fun testClearAll() =
        runTest {
            fakeParser.result = Result.success(createMockPayslip("01/2024"))
            repository.importPayslip(byteArrayOf(), "pass", "file.pdf")
            fakeParser.result = Result.success(createMockPayslip("02/2024"))
            repository.importPayslip(byteArrayOf(), "pass", "file.pdf")

            assertEquals(2, repository.getAllPayslips().first().size)

            repository.clearAll()

            assertTrue(repository.getAllPayslips().first().isEmpty())
        }

    @Test
    fun testSeedMockData() =
        runTest {
            repository.seedMockData()
            val payslips = repository.getAllPayslips().first()
            // Seeding seeds months of years 2022, 2023, 2024, 2025 (12 months * 4 years = 48 entries)
            assertEquals(48, payslips.size)
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
            earnings = Earnings(100.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
            deductions = Deductions(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(100.0, 80.0, 20.0),
            taxAndSavings =
                TaxAndSavings(
                    grossSalaryYtd = 1000.0,
                    totalTaxableIncome = 900.0,
                    standardDeduction = 50.0,
                    netTaxableIncome = 850.0,
                    totalTaxPayable = 100.0,
                    taxDeductedYtd = 80.0,
                    cessDeductedYtd = 20.0,
                    dsopFund = DsopFund(100.0, 10.0, 0.0, 0.0, 0.0, 110.0),
                ),
        )
    }
}
