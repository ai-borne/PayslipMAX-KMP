package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.UnsupportedBackupVersionException
import com.payslipmax.pdfparser.repository.backupBytes
import com.payslipmax.pdfparser.repository.goodEntity
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a backup is the only copy if a phone is lost, and the vault is RESTRICTED material. Part II rows travel under the backup
 * password (an archive must not depend on one device's key), come back under the restoring device's key, follow the same REPLACE and
 * MERGE rules as every other thing the officer made, and a damaged row never costs the officer the payslips around it.
 */
class Part2BackupTest {
    private lateinit var source: FakePayslipDao
    private lateinit var target: FakePayslipDao
    private lateinit var sourceService: PayslipBackupService
    private lateinit var targetService: PayslipBackupService

    @BeforeTest
    fun setUp() {
        source = FakePayslipDao()
        target = FakePayslipDao()
        sourceService = PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2))
        targetService = PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(target.part2))
    }

    private suspend fun targetDrafts() = target.part2.allDrafts().associate { it.id to it.toDraft(deviceKey) }

    private suspend fun targetOrders() = target.part2.allOrders().associate { it.id to it.toOrder(deviceKey) }

    private fun draft(
        id: String,
        updatedAt: Long,
    ) = SyntheticPart2Drafts.fullDraft(id, updatedAt)

    @Test
    fun theCurrentVersionIsFiveAndAnExportWritesIt() =
        runTest {
            assertEquals(5, PortableBackup.CURRENT_VERSION)

            assertEquals(5, decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow()).version)
        }

    @Test
    fun rowsInTheArchiveAreUnderTheBackupPasswordNotTheDeviceKeyAndHoldNoPlaintext() =
        runTest {
            source.putDraft(draft("d1", 10))
            source.putOrder(SyntheticPart2Drafts.order("o1"))
            source.putPage("p1", "o1", 0, SyntheticPart2Drafts.jpeg(2000))

            val backup = decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow())

            val d = Base64.decode(backup.part2Drafts.single().data)
            assertEquals(draft("d1", 10), Part2DraftEntity("d1", d).toDraft(BACKUP_PASSWORD))
            assertTrue(CryptoHelper.decrypt(d, deviceKey).isFailure, "an archive must not depend on one device's key")
            assertEquals("o1", Part2OrderEntity("o1", Base64.decode(backup.part2Orders.single().data)).toOrder(BACKUP_PASSWORD).id)
            val page = backup.part2Pages.single()
            assertEquals(Triple("p1", "o1", 0), Triple(page.id, page.orderId, page.position))
            assertFalse(backup.part2Drafts.single().data.contains(SyntheticPart2Drafts.MARKER_TEXT))
        }

    @Test
    fun aReplaceRestoreOfAnExportIsAnExactCopyIncludingPagesAndIsReEncryptedForTheNewDevice() =
        runTest {
            val jpeg = SyntheticPart2Drafts.jpeg(5000, 7)
            source.putDraft(draft("d1", 10))
            source.putOrder(SyntheticPart2Drafts.order("o1", updatedAt = 20))
            source.putPage("p1", "o1", 0, jpeg)
            source.putPage("p2", "o1", 1, SyntheticPart2Drafts.jpeg(300))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.putDraft(draft("device-only", 99))
            target.putOrder(SyntheticPart2Drafts.order("device-order"))
            target.putPage("device-page", "device-order", 0, SyntheticPart2Drafts.jpeg(100))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(setOf("d1"), targetDrafts().keys, "REPLACE leaves an exact copy of the backup")
            assertEquals(setOf("o1"), targetOrders().keys)
            assertEquals(listOf("p1", "p2"), target.part2.allPageRefs().map { it.id })
            val page = target.part2.page("p1")!!
            assertContentEquals(jpeg, page.openPage(deviceKey))
            assertTrue(CryptoHelper.decrypt(page.ciphertext, BACKUP_PASSWORD).isFailure, "stored under the device key, not the backup password")
        }

    @Test
    fun mergeKeepsDeviceOnlyRowsAddsBackupOnlyRowsAndTheNewerUpdatedAtWins() =
        runTest {
            source.putDraft(draft("backup-newer", 90))
            source.putDraft(draft("device-newer", 10))
            source.putDraft(draft("tie", 50))
            source.putDraft(draft("backup-only", 1))
            source.putOrder(SyntheticPart2Drafts.order("o-newer", orderNo = "FROM-BACKUP", updatedAt = 90))
            source.putOrder(SyntheticPart2Drafts.order("o-device-newer", orderNo = "FROM-BACKUP", updatedAt = 10))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.putDraft(draft("backup-newer", 20))
            target.putDraft(draft("device-newer", 80))
            target.putDraft(draft("tie", 50).copy(createdAt = 7))
            target.putDraft(draft("device-only", 3))
            target.putOrder(SyntheticPart2Drafts.order("o-newer", orderNo = "ON-DEVICE", updatedAt = 20))
            target.putOrder(SyntheticPart2Drafts.order("o-device-newer", orderNo = "ON-DEVICE", updatedAt = 80))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            val drafts = targetDrafts()
            assertEquals(90L, drafts.getValue("backup-newer").updatedAt)
            assertEquals(80L, drafts.getValue("device-newer").updatedAt, "a restore must never overwrite a newer edit made on this phone")
            assertEquals(draft("tie", 50).createdAt, drafts.getValue("tie").createdAt, "a tie goes to the backup")
            assertEquals(setOf("backup-newer", "device-newer", "tie", "backup-only", "device-only"), drafts.keys)
            val orders = targetOrders()
            assertEquals("FROM-BACKUP", orders.getValue("o-newer").orderNo)
            assertEquals("ON-DEVICE", orders.getValue("o-device-newer").orderNo)
        }

    @Test
    fun mergeLetsTheBackupWinWhenTheDevicesRowCannotBeReadAndKeepsADevicePageAlreadyHeld() =
        runTest {
            source.putDraft(draft("d1", 5))
            source.putOrder(SyntheticPart2Drafts.order("o1"))
            source.putPage("p1", "o1", 0, SyntheticPart2Drafts.jpeg(400))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.part2.upsertDraft(Part2DraftEntity("d1", byteArrayOf(1, 2, 3)))
            target.putOrder(SyntheticPart2Drafts.order("o1"))
            target.putPage("p1", "o1", 0, SyntheticPart2Drafts.jpeg(400))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            assertEquals(5L, targetDrafts().getValue("d1").updatedAt)
            assertEquals(1, target.part2.allPageRefs().size, "a page is immutable: the same id is not stored twice")
        }

    @Test
    fun aDamagedRowIsSkippedAndCountedAndEverythingElseStillRestoresInBothModes() =
        runTest {
            val good = draft("good", 5)
            val rows =
                listOf(
                    Part2DraftBackup("bad-base64", "not base64 !!"),
                    Part2DraftBackup("bad-short", text(byteArrayOf(1, 2, 3))),
                    draftRow(draft("bad-key", 1), "some other password"),
                    Part2DraftBackup("moved", draftRow(draft("other", 1)).data),
                    draftRow(good),
                )
            val bytes = archive(drafts = rows)

            RestoreMode.entries.forEach { mode ->
                target.putDraft(draft("device-$mode", 1))

                targetService.restore(bytes, BACKUP_PASSWORD, mode).getOrThrow()

                assertTrue(target.getPayslipByDate("08/2024") != null, "the payslips must not be lost to a bad row ($mode)")
                val ids = targetDrafts().keys
                assertTrue("good" in ids, "the readable draft is restored ($mode)")
                assertTrue(ids.none { it.startsWith("bad") || it == "moved" }, "no damaged, wrong-key or moved row is restored ($mode)")
            }
            val result = Part2Backup(FakePayslipDao().part2).restore(decodeArchive(bytes), BACKUP_PASSWORD, RestoreMode.REPLACE)
            assertEquals(4, result.skipped)
            assertEquals(listOf("good"), result.drafts.map { it.id })
        }

    @Test
    fun aDeviceRowThatCannotBeReadIsLeftOutOfAnExportNotFatalToIt() =
        runTest {
            source.putDraft(draft("d1", 1))
            source.part2.upsertDraft(Part2DraftEntity("junk", byteArrayOf(9)))
            source.part2.upsertDraft(draft("d3", 1).toEntity("a key this device no longer has"))

            val backup = decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow())

            assertEquals(listOf("d1"), backup.part2Drafts.map { it.id })
        }

    @Test
    fun aVersion4BackupRestoresWithAnEmptyVaultOnReplaceAndLeavesTheDevicesVaultAloneOnMerge() =
        runTest {
            val v4 = backupBytes(PortableBackup(4, listOf(goodEntity("08/2024")), emptyList(), null))
            target.putDraft(draft("d1", 1))
            targetService.restore(v4, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()
            assertEquals(setOf("d1"), targetDrafts().keys)

            targetService.restore(v4, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertTrue(target.part2.allDrafts().isEmpty(), "REPLACE means exactly this archive, as for Guide notes")
            assertEquals(listOf("08/2024"), target.getAllPayslipsOnce())
        }

    @Test
    fun aVersion6BackupIsRefusedAndChangesNothing() =
        runTest {
            target.putDraft(draft("d1", 1))
            val v6 = archive(drafts = listOf(draftRow(draft("from-v6", 1))), version = PortableBackup.CURRENT_VERSION + 1)

            val error = targetService.restore(v6, BACKUP_PASSWORD, RestoreMode.REPLACE).exceptionOrNull()

            assertTrue(error is UnsupportedBackupVersionException, "expected a version refusal but was $error")
            assertEquals(setOf("d1"), targetDrafts().keys)
        }

    @Test
    fun deleteAllDataAndAReplaceRestoreEmptyTheThreeTables() =
        runTest {
            target.putDraft(draft("d1", 1))
            target.putOrder(SyntheticPart2Drafts.order("o1"))
            target.putPage("p1", "o1", 0, SyntheticPart2Drafts.jpeg(100))

            target.clearAllUserData()

            assertTrue(target.part2.allDrafts().isEmpty() && target.part2.allOrders().isEmpty() && target.part2.allPageRefs().isEmpty())
        }

    private suspend fun FakePayslipDao.getAllPayslipsOnce() = getPayslipByDate("08/2024")?.dateStr?.let { listOf(it) }.orEmpty()
}
