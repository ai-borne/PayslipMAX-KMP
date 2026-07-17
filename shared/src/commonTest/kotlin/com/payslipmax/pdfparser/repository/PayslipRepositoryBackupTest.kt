package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.testing.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Portable (cross-device) backup/restore: the encrypted-archive export/import path and its modes. */
class PayslipRepositoryBackupTest {
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
            // Entitlement never travels inside a backup (D3): the device had none at import time
            // (settings were cleared in step 3), so it stays free even though the backup was premium.
            assertFalse(restoredSettings.isPremiumEnabled)
            // Non-entitlement settings still round-trip.
            assertEquals("light", restoredSettings.appTheme)
        }

    @Test
    fun testExportSkipsUndecryptableRowInsteadOfFailing() =
        runTest {
            // A valid payslip the user can see...
            fakeParser.result = Result.success(createMockPayslip("08/2024"))
            repository.importPayslip(byteArrayOf(1), "pw", "a.pdf")
            // ...plus a stale/corrupt row that decrypts with neither the device nor the legacy key
            // (e.g. written by a previous install). The read path already skips it; export must too.
            fakeDao.insertPayslip(
                EncryptedPayslipEntity("09/2014", 2014, 9, "September", ciphertext = "deadbeef"),
            )

            // Export must succeed rather than abort on the bad row.
            val backup = repository.exportUniversalBackup("bpw")
            assertTrue(backup.isSuccess)

            // And it round-trips exactly the decryptable payslip — the corrupt row is dropped.
            repository.clearAll()
            repository.importUniversalBackup(backup.getOrThrow(), "bpw", RestoreMode.REPLACE)
            assertEquals(listOf("08/2024"), repository.getAllPayslips().first().map { it.dateStr })
        }

    @Test
    fun testGetStoredPayslipCount() =
        runTest {
            assertEquals(0, repository.getStoredPayslipCount())

            fakeParser.result = Result.success(createMockPayslip("08/2024"))
            repository.importPayslip(byteArrayOf(1), "pw", "a.pdf")
            fakeParser.result = Result.success(createMockPayslip("09/2024"))
            repository.importPayslip(byteArrayOf(2), "pw", "b.pdf")

            assertEquals(2, repository.getStoredPayslipCount())
        }

    @Test
    fun testRestoreReplaceModeWipesPayslipsNotInBackup() =
        runTest {
            // Backup captures only 08/2024.
            fakeParser.result = Result.success(createMockPayslip("08/2024"))
            repository.importPayslip(byteArrayOf(1), "pw", "a.pdf")
            val backup = repository.exportUniversalBackup("bpw").getOrThrow()

            // Device now holds a *different* payslip that is absent from the backup.
            repository.clearAll()
            fakeParser.result = Result.success(createMockPayslip("01/2023"))
            repository.importPayslip(byteArrayOf(2), "pw", "b.pdf")

            repository.importUniversalBackup(backup, "bpw", RestoreMode.REPLACE)

            // REPLACE makes the device an exact copy of the backup: 01/2023 is gone.
            val payslips = repository.getAllPayslips().first()
            assertEquals(1, payslips.size)
            assertEquals("08/2024", payslips.first().dateStr)
        }

    @Test
    fun testRestoreMergeModeKeepsExistingAndDedupesSameDate() =
        runTest {
            // Backup captures 08/2024 (and, to prove same-date dedupe, also 01/2023).
            fakeParser.result = Result.success(createMockPayslip("08/2024"))
            repository.importPayslip(byteArrayOf(1), "pw", "a.pdf")
            fakeParser.result = Result.success(createMockPayslip("01/2023"))
            repository.importPayslip(byteArrayOf(2), "pw", "b.pdf")
            val backup = repository.exportUniversalBackup("bpw").getOrThrow()

            // Device is reset to hold a newer payslip (12/2024) plus its own copy of 01/2023.
            repository.clearAll()
            fakeParser.result = Result.success(createMockPayslip("12/2024"))
            repository.importPayslip(byteArrayOf(3), "pw", "c.pdf")
            fakeParser.result = Result.success(createMockPayslip("01/2023"))
            repository.importPayslip(byteArrayOf(4), "pw", "d.pdf")

            repository.importUniversalBackup(backup, "bpw", RestoreMode.MERGE)

            // MERGE keeps the device's 12/2024, adds the backup's 08/2024, and the shared 01/2023
            // collapses to a single row (overwritten by the backup) rather than duplicating.
            val dates = repository.getAllPayslips().first().map { it.dateStr }.toSet()
            assertEquals(setOf("12/2024", "08/2024", "01/2023"), dates)
        }
}
