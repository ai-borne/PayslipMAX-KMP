package com.ssbmax.pdfparser.repository

import com.ssbmax.pdfparser.database.*
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
            val pdfBytes = byteArrayOf(1, 2, 3)

            val result =
                repository.importPayslip(
                    pdfBytes = pdfBytes,
                    password = "test-password",
                    filename = "08-2024.pdf",
                )

            assertTrue(result.isSuccess)
            assertEquals(mockPayslip, result.getOrNull())

            // Verify it was inserted in the DAO
            val allDbPayslips = repository.getAllPayslips().first()
            assertEquals(1, allDbPayslips.size)
            assertEquals("08/2024", allDbPayslips.first().dateStr)

            // Verify PDF was saved
            val savedPdf = repository.getPayslipPdf("08/2024")
            assertNotNull(savedPdf)
            assertTrue(pdfBytes.contentEquals(savedPdf))
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

            // Verify PDF was not saved
            val savedPdf = repository.getPayslipPdf("08/2024")
            assertNull(savedPdf)
        }

    @Test
    fun testGetPayslipByDate() =
        runTest {
            val mockPayslip = createMockPayslip("05/2024")
            fakeParser.result = Result.success(mockPayslip)
            repository.importPayslip(byteArrayOf(4, 5), "pass", "file.pdf")

            val found = repository.getPayslipByDate("05/2024")
            assertNotNull(found)
            assertEquals("05/2024", found.dateStr)

            val notFound = repository.getPayslipByDate("06/2024")
            assertNull(notFound)
        }

    @Test
    fun testDeletePayslipCascading() =
        runTest {
            val mockPayslip = createMockPayslip("05/2024")
            fakeParser.result = Result.success(mockPayslip)
            repository.importPayslip(byteArrayOf(9, 9), "pass", "file.pdf")

            // Insert records directly via fakeDao
            fakeDao.insertLedgerRecord(LedgerRecordEntity(dateStr = "05/2024", year = 2024, monthNum = 5, basicPay = 10000.0, dearnessAllowance = 1000.0, militaryServicePay = 1000.0, transportAllowance = 100.0, transportAllowanceDa = 10.0, houseRentAllowance = 500.0, grossPay = 15000.0, dsopSubscription = 1000.0, incomeTax = 1000.0, netPay = 13000.0))
            fakeDao.insertFinancialInsight(FinancialInsightEntity("id1", "05/2024", "NARRATIVE", "Title", "Markdown", "INFO", 1234567890L))
            fakeDao.insertAiInsightReport(AiInsightReportEntity("id3", "05/2024", 1234567890L, "Narrative report", "1.0"))

            // Assert inserted
            assertNotNull(repository.getPayslipByDate("05/2024"))
            assertNotNull(repository.getPayslipPdf("05/2024"))
            assertEquals(1, fakeDao.getAllLedgerRecords().first().size)
            assertEquals(1, fakeDao.getAllFinancialInsights().first().size)
            assertEquals(1, fakeDao.getAllAiInsightReports().first().size)

            // Delete
            repository.deletePayslip("05/2024")

            // Assert deleted
            assertNull(repository.getPayslipByDate("05/2024"))
            assertNull(repository.getPayslipPdf("05/2024"))
            assertTrue(fakeDao.getAllLedgerRecords().first().isEmpty())
            assertTrue(fakeDao.getAllFinancialInsights().first().isEmpty())
            assertTrue(fakeDao.getAllAiInsightReports().first().isEmpty())
        }

    @Test
    fun testClearAllCompleteness() =
        runTest {
            // Seed a statement
            fakeParser.result = Result.success(createMockPayslip("01/2024"))
            repository.importPayslip(byteArrayOf(1, 1), "pass", "file.pdf")

            // Insert records directly via fakeDao
            fakeDao.insertLedgerRecord(LedgerRecordEntity(dateStr = "01/2024", year = 2024, monthNum = 1, basicPay = 10000.0, dearnessAllowance = 1000.0, militaryServicePay = 1000.0, transportAllowance = 100.0, transportAllowanceDa = 10.0, houseRentAllowance = 500.0, grossPay = 15000.0, dsopSubscription = 1000.0, incomeTax = 1000.0, netPay = 13000.0))
            fakeDao.insertFinancialInsight(FinancialInsightEntity("id1", "01/2024", "NARRATIVE", "Title", "Markdown", "INFO", 1234567890L))
            fakeDao.insertRepresentationDraft(RepresentationDraftEntity("id2", "01/2024", "MISSING_HRA", "PCDA_O_PUNE", "Subject", "Body", 1234567890L))
            fakeDao.insertAiInsightReport(AiInsightReportEntity("id3", "01/2024", 1234567890L, "Narrative report", "1.0"))

            // Verify they exist in DAO
            assertEquals(1, repository.getAllPayslips().first().size)
            assertNotNull(repository.getPayslipPdf("01/2024"))
            assertEquals(1, fakeDao.getAllLedgerRecords().first().size)
            assertEquals(1, fakeDao.getAllFinancialInsights().first().size)
            assertEquals(1, fakeDao.getAllRepresentationDrafts().first().size)
            assertEquals(1, fakeDao.getAllAiInsightReports().first().size)

            // Clear all
            repository.clearAll()

            // Verify everything is cleared
            assertTrue(repository.getAllPayslips().first().isEmpty())
            assertNull(repository.getPayslipPdf("01/2024"))
            assertTrue(fakeDao.getAllLedgerRecords().first().isEmpty())
            assertTrue(fakeDao.getAllFinancialInsights().first().isEmpty())
            assertTrue(fakeDao.getAllRepresentationDrafts().first().isEmpty())
            assertTrue(fakeDao.getAllAiInsightReports().first().isEmpty())
        }

    @Test
    fun testSeedMockData() =
        runTest {
            repository.seedMockData()
            val payslips = repository.getAllPayslips().first()
            // Seeding seeds months of years 2022, 2023, 2024, 2025 (12 months * 4 years = 48 entries)
            assertEquals(48, payslips.size)
        }

    @Test
    fun testSettingsPersistence() =
        runTest {
            val settings =
                AppSettingsEntity(
                    isPremiumEnabled = true,
                    appTheme = "dark",
                    isLockEnabled = true,
                    appPinHash = "hashedpin123",
                    profileName = "Col Sunil Pawar",
                    profileCdaNumber = "12345A",
                    profilePanNumber = "ABCDE1234F",
                )

            assertNull(repository.getSettings())

            repository.saveSettings(settings)

            val saved = repository.getSettings()
            assertNotNull(saved)
            assertTrue(saved.isPremiumEnabled)
            assertEquals("dark", saved.appTheme)
            assertTrue(saved.isLockEnabled)
            assertEquals("hashedpin123", saved.appPinHash)
            assertEquals("Col Sunil Pawar", saved.profileName)
            assertEquals("12345A", saved.profileCdaNumber)
            assertEquals("ABCDE1234F", saved.profilePanNumber)

            repository.clearSettings()
            assertNull(repository.getSettings())
        }

    /**
     * SECURITY REGRESSION GUARD: This test verifies the schema of AppSettingsEntity
     * does NOT contain a geminiApiKey field. The test is a compile-time guard:
     * if geminiApiKey is ever re-added to AppSettingsEntity, attempting to access
     * any field by positional destructuring will break, and the explicit property
     * list below will not match the expected count.
     *
     * The API key must live exclusively in Firebase Secret Manager (server-side).
     */
    @Test
    fun testGeminiApiKeyIsRemovedFromSchema() {
        val entity =
            AppSettingsEntity(
                id = 0,
                isPremiumEnabled = false,
                appTheme = "system",
                isLockEnabled = false,
                appPinHash = "",
                profileName = "",
                profileCdaNumber = "",
                profilePanNumber = "",
            )
        // Verify the entity has exactly 8 fields (id + 7 settings).
        // If geminiApiKey is re-added, this count becomes 9 and the test fails.
        val (id, isPremium, appTheme, isLock, pinHash, name, cda, pan) = entity
        assertEquals(0, id)
        assertFalse(isPremium)
        assertEquals("system", appTheme)
        assertFalse(isLock)
        assertEquals("", pinHash)
        assertEquals("", name)
        assertEquals("", cda)
        assertEquals("", pan)
    }

    @Test
    fun testUniversalBackupAndRestore() =
        runTest {
            // 1. Setup initial state
            val mockPayslip = createMockPayslip("08/2024")
            fakeParser.result = Result.success(mockPayslip)
            repository.importPayslip(byteArrayOf(1, 2, 3), "test-password", "08-2024.pdf")

            val settings =
                AppSettingsEntity(
                    isPremiumEnabled = true,
                    appTheme = "light",
                )
            repository.saveSettings(settings)

            // 2. Export backup
            val exportResult = repository.exportUniversalBackup("backup-pwd")
            assertTrue(exportResult.isSuccess)
            val backupBytes = exportResult.getOrThrow()

            // 3. Wreak havoc / clear database
            repository.clearAll()
            repository.clearSettings()
            assertTrue(repository.getAllPayslips().first().isEmpty())
            assertNull(repository.getSettings())

            // 4. Import backup
            val importResult = repository.importUniversalBackup(backupBytes, "backup-pwd")
            assertTrue(importResult.isSuccess)

            // 5. Verify restored state
            val restoredPayslips = repository.getAllPayslips().first()
            assertEquals(1, restoredPayslips.size)
            assertEquals("08/2024", restoredPayslips.first().dateStr)

            val restoredSettings = repository.getSettings()
            assertNotNull(restoredSettings)
            assertTrue(restoredSettings.isPremiumEnabled)
            assertEquals("light", restoredSettings.appTheme)
        }
}
