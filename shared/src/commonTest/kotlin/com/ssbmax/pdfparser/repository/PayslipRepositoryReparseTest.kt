package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.domain.PayslipSummary
import com.ssbmax.pdfparser.testing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Covers [PayslipRepository.reparseAllPayslips]: the maintenance action that re-runs every stored
 * payslip's saved PDF bytes through the *current* parser and overwrites the stale stored result,
 * so a parser fix reaches payslips that were already imported before the fix landed (importPayslip
 * only ever parses once, at import time).
 */
class PayslipRepositoryReparseTest {
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
    fun testReparseOverwritesStaleStoredResult() =
        runTest {
            // Import under the "old, buggy" parser behavior.
            val stale = createMockPayslip("03/2025").copy(summary = PayslipSummary(1.0, 1.0, 0.0))
            fakeParser.result = Result.success(stale)
            repository.importPayslip(byteArrayOf(1, 2, 3), "535d04", "03 Mar 2025.pdf")
            assertEquals(1.0, repository.getPayslipByDate("03/2025")?.summary?.grossPay)

            // Simulate a parser fix: the same stored PDF bytes now parse correctly.
            val fixed = createMockPayslip("03/2025").copy(summary = PayslipSummary(271739.0, 96432.0, 175307.0))
            fakeParser.resultsByFilename = mapOf("03/2025.pdf" to Result.success(fixed))

            val summary = repository.reparseAllPayslips("535d04")

            assertEquals(1, summary.total)
            assertEquals(1, summary.succeeded)
            assertTrue(summary.failedDates.isEmpty())
            assertEquals(271739.0, repository.getPayslipByDate("03/2025")?.summary?.grossPay)
        }

    @Test
    fun testReparseLeavesRecordUntouchedOnFailure() =
        runTest {
            val original = createMockPayslip("05/2024")
            fakeParser.result = Result.success(original)
            repository.importPayslip(byteArrayOf(9, 9), "535d04", "05 May 2024.pdf")

            // Wrong password (or any parse failure) on the bulk re-parse pass.
            fakeParser.resultsByFilename = mapOf("05/2024.pdf" to Result.failure(Exception("Incorrect password")))

            val summary = repository.reparseAllPayslips("wrong-password")

            assertEquals(1, summary.total)
            assertEquals(0, summary.succeeded)
            assertEquals(listOf("05/2024"), summary.failedDates)
            // The original stored record must survive untouched — no data loss on failure.
            assertEquals(original, repository.getPayslipByDate("05/2024"))
        }

    @Test
    fun testReparseHandlesMixedSuccessAndFailureAcrossMultiplePayslips() =
        runTest {
            fakeParser.result = Result.success(createMockPayslip("01/2023"))
            repository.importPayslip(byteArrayOf(1), "535d04", "01 Jan 2023.pdf")
            fakeParser.result = Result.success(createMockPayslip("02/2023"))
            repository.importPayslip(byteArrayOf(2), "535d04", "02 Feb 2023.pdf")

            val fixedJan = createMockPayslip("01/2023").copy(summary = PayslipSummary(500.0, 100.0, 400.0))
            fakeParser.resultsByFilename =
                mapOf(
                    "01/2023.pdf" to Result.success(fixedJan),
                    "02/2023.pdf" to Result.failure(Exception("still broken")),
                )

            val summary = repository.reparseAllPayslips("535d04")

            assertEquals(2, summary.total)
            assertEquals(1, summary.succeeded)
            assertEquals(listOf("02/2023"), summary.failedDates)
            assertEquals(500.0, repository.getPayslipByDate("01/2023")?.summary?.grossPay)
            assertEquals("02/2023", repository.getPayslipByDate("02/2023")?.dateStr)
        }

    @Test
    fun testReparseWithNoStoredPayslipsReturnsEmptySummary() =
        runTest {
            val summary = repository.reparseAllPayslips("535d04")
            assertEquals(0, summary.total)
            assertEquals(0, summary.succeeded)
            assertTrue(summary.failedDates.isEmpty())
            assertTrue(repository.getAllPayslips().first().isEmpty())
        }
}
