package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A REPLACE restore promises "this device becomes an exact copy of the backup". Anything the device
 * held before that is not in the backup (derived ledger/insights, letters, corrections, PDFs) must
 * therefore be gone afterwards, and a backup that cannot be fully read must change nothing at all —
 * payslips exist only on the device, so a half-applied restore is data loss.
 */
class PayslipBackupServiceReplaceTest {
    private lateinit var dao: FakePayslipDao
    private lateinit var service: PayslipBackupService

    @BeforeTest
    fun setUp() {
        dao = FakePayslipDao()
        service = PayslipBackupService(dao, Dispatchers.Unconfined)
    }

    @Test
    fun replaceRestoreLeavesNothingOfTheDevicesPreviousData() =
        runTest {
            dao.seedDeviceMonth("01/2023")

            service.restore(backupOf("08/2024"), BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(listOf("08/2024"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals(listOf("08/2024"), dao.getAllPdfs().map { it.dateStr })
            assertTrue(dao.getAllLedgerRecords().first().isEmpty(), "previous ledger rows must go")
            assertTrue(dao.getAllFinancialInsights().first().isEmpty(), "previous insights must go")
            assertTrue(dao.getAllRepresentationDrafts().first().isEmpty(), "previous letters must go")
            assertTrue(dao.getAllDismissedDrafts().isEmpty(), "previous deleted-letter records must go")
            assertTrue(dao.getAllCorrections().first().isEmpty(), "previous corrections must go")
        }

    @Test
    fun mergeRestoreKeepsTheDevicesOwnDerivedData() =
        runTest {
            dao.seedDeviceMonth("01/2023")

            service.restore(backupOf("08/2024"), BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            assertEquals(setOf("01/2023", "08/2024"), dao.getAllPayslips().first().map { it.dateStr }.toSet())
            assertEquals(1, dao.getAllLedgerRecords().first().size)
            assertEquals(1, dao.getAllFinancialInsights().first().size)
            assertEquals(1, dao.getAllRepresentationDrafts().first().size)
            assertEquals(1, dao.getAllDismissedDrafts().size)
            assertEquals(1, dao.getAllCorrections().first().size)
        }

    @Test
    fun aBackupWithAnUnreadablePayslipChangesNothingOnTheDevice() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            dao.insertSettings(AppSettingsEntity(appTheme = "dark", isPremiumEnabled = true))
            val corrupt = EncryptedPayslipEntity("09/2014", 2014, 9, "September", ciphertext = "deadbeef")
            val backup = backupBytes(PortableBackup(2, listOf(goodEntity("08/2024"), corrupt), emptyList(), null))

            val result = service.restore(backup, BACKUP_PASSWORD, RestoreMode.REPLACE)

            assertTrue(result.isFailure)
            assertEquals(listOf("01/2023"), dao.getAllPayslips().first().map { it.dateStr })
            assertEquals(listOf("01/2023"), dao.getAllPdfs().map { it.dateStr })
            assertEquals(1, dao.getAllLedgerRecords().first().size)
            assertEquals(1, dao.getAllFinancialInsights().first().size)
            assertEquals(1, dao.getAllRepresentationDrafts().first().size)
            assertEquals(1, dao.getAllDismissedDrafts().size)
            assertEquals(1, dao.getAllCorrections().first().size)
            val settings = dao.getSettings()!!
            assertEquals("dark", settings.appTheme)
            assertTrue(settings.isPremiumEnabled)
        }

    @Test
    fun aBackupWithAnUnreadablePayslipAddsNothingInMergeMode() =
        runTest {
            val corrupt = EncryptedPayslipEntity("09/2014", 2014, 9, "September", ciphertext = "deadbeef")
            val backup = backupBytes(PortableBackup(2, listOf(goodEntity("08/2024"), corrupt), emptyList(), null))

            val result = service.restore(backup, BACKUP_PASSWORD, RestoreMode.MERGE)

            assertTrue(result.isFailure)
            assertFalse(dao.getAllPayslips().first().any { it.dateStr == "08/2024" }, "good rows must not be half-applied")
        }
}
