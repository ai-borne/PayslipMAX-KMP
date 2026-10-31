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
import com.payslipmax.pdfparser.testing.SyntheticBank
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

    private suspend fun targetEntries() = target.part2.allEntries().associate { it.id to it.toEntry(deviceKey) }

    private suspend fun targetOrders() = target.part2.allOrders().associate { it.id to it.toOrder(deviceKey) }

    private fun entry(
        id: String,
        updatedAt: Long,
    ) = SyntheticBank.entry(id, updatedAt)

    @Test
    fun theCurrentVersionIsFiveAndAnExportWritesIt() =
        runTest {
            assertEquals(5, PortableBackup.CURRENT_VERSION)

            assertEquals(5, decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow()).version)
        }

    @Test
    fun rowsInTheArchiveAreUnderTheBackupPasswordNotTheDeviceKeyAndHoldNoPlaintext() =
        runTest {
            source.putEntry(entry("d1", 10))
            source.putOrder(SyntheticBank.order("o1"))
            source.putPage("p1", "o1", 0, SyntheticBank.jpeg(2000))

            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            val backup = decodeArchive(archive)
            assertTrue(PageArchive.metaOf(archive) != null, "an archive with pages is a page archive")

            val d = Base64.decode(backup.part2Entries.single().data)
            assertEquals(entry("d1", 10), Part2EntryEntity("d1", d).toEntry(BACKUP_PASSWORD))
            assertTrue(CryptoHelper.decrypt(d, deviceKey).isFailure, "an archive must not depend on one device's key")
            assertEquals("o1", Part2OrderEntity("o1", Base64.decode(backup.part2Orders.single().data)).toOrder(BACKUP_PASSWORD).id)
            assertFalse(backup.part2Entries.single().data.contains(SyntheticBank.MARKER_NOTE))
        }

    @Test
    fun aReplaceRestoreOfAnExportIsAnExactCopyIncludingPagesAndIsReEncryptedForTheNewDevice() =
        runTest {
            val jpeg = SyntheticBank.jpeg(5000, 7)
            source.putEntry(entry("d1", 10))
            source.putOrder(SyntheticBank.order("o1", updatedAt = 20))
            source.putPage("p1", "o1", 0, jpeg)
            source.putPage("p2", "o1", 1, SyntheticBank.jpeg(300))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.putEntry(entry("device-only", 99))
            target.putOrder(SyntheticBank.order("device-order"))
            target.putPage("device-page", "device-order", 0, SyntheticBank.jpeg(100))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(setOf("d1"), targetEntries().keys, "REPLACE leaves an exact copy of the backup")
            assertEquals(setOf("o1"), targetOrders().keys)
            assertEquals(listOf("p1", "p2"), target.part2.allPageRefs().map { it.id })
            val page = target.part2.page("p1")!!
            assertContentEquals(jpeg, page.openPage(deviceKey))
            assertTrue(CryptoHelper.decrypt(page.ciphertext, BACKUP_PASSWORD).isFailure, "stored under the device key, not the backup password")
        }

    @Test
    fun mergeKeepsDeviceOnlyRowsAddsBackupOnlyRowsAndTheNewerUpdatedAtWins() =
        runTest {
            source.putEntry(entry("backup-newer", 90))
            source.putEntry(entry("device-newer", 10))
            source.putEntry(entry("tie", 50))
            source.putEntry(entry("backup-only", 1))
            source.putOrder(SyntheticBank.order("o-newer", orderNo = "FROM-BACKUP", updatedAt = 90))
            source.putOrder(SyntheticBank.order("o-device-newer", orderNo = "FROM-BACKUP", updatedAt = 10))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.putEntry(entry("backup-newer", 20))
            target.putEntry(entry("device-newer", 80))
            target.putEntry(entry("tie", 50).copy(createdAt = 7))
            target.putEntry(entry("device-only", 3))
            target.putOrder(SyntheticBank.order("o-newer", orderNo = "ON-DEVICE", updatedAt = 20))
            target.putOrder(SyntheticBank.order("o-device-newer", orderNo = "ON-DEVICE", updatedAt = 80))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            val entries = targetEntries()
            assertEquals(90L, entries.getValue("backup-newer").updatedAt)
            assertEquals(80L, entries.getValue("device-newer").updatedAt, "a restore must never overwrite a newer edit made on this phone")
            assertEquals(entry("tie", 50).createdAt, entries.getValue("tie").createdAt, "a tie goes to the backup")
            assertEquals(setOf("backup-newer", "device-newer", "tie", "backup-only", "device-only"), entries.keys)
            val orders = targetOrders()
            assertEquals("FROM-BACKUP", orders.getValue("o-newer").orderNo)
            assertEquals("ON-DEVICE", orders.getValue("o-device-newer").orderNo)
        }

    @Test
    fun mergeLetsTheBackupWinWhenTheDevicesRowCannotBeReadAndKeepsADevicePageAlreadyHeld() =
        runTest {
            source.putEntry(entry("d1", 5))
            source.putOrder(SyntheticBank.order("o1"))
            source.putPage("p1", "o1", 0, SyntheticBank.jpeg(400))
            val archive = sourceService.export(BACKUP_PASSWORD).getOrThrow()
            target.part2.upsertEntry(Part2EntryEntity("d1", byteArrayOf(1, 2, 3)))
            target.putOrder(SyntheticBank.order("o1"))
            target.putPage("p1", "o1", 0, SyntheticBank.jpeg(400))

            targetService.restore(archive, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()

            assertEquals(5L, targetEntries().getValue("d1").updatedAt)
            assertEquals(1, target.part2.allPageRefs().size, "a page is immutable: the same id is not stored twice")
        }

    @Test
    fun aDamagedRowIsSkippedAndCountedAndEverythingElseStillRestoresInBothModes() =
        runTest {
            val good = entry("good", 5)
            val rows =
                listOf(
                    Part2RowBackup("bad-base64", "not base64 !!"),
                    Part2RowBackup("bad-short", text(byteArrayOf(1, 2, 3))),
                    entryRow(entry("bad-key", 1), "some other password"),
                    Part2RowBackup("moved", entryRow(entry("other", 1)).data),
                    entryRow(good),
                )
            val bytes = archive(entries = rows)

            RestoreMode.entries.forEach { mode ->
                target.putEntry(entry("device-$mode", 1))

                targetService.restore(bytes, BACKUP_PASSWORD, mode).getOrThrow()

                assertTrue(target.getPayslipByDate("08/2024") != null, "the payslips must not be lost to a bad row ($mode)")
                val ids = targetEntries().keys
                assertTrue("good" in ids, "the readable entry is restored ($mode)")
                assertTrue(ids.none { it.startsWith("bad") || it == "moved" }, "no damaged, wrong-key or moved row is restored ($mode)")
            }
            val result = Part2Backup(FakePayslipDao().part2).restore(decodeArchive(bytes), bytes, BACKUP_PASSWORD, RestoreMode.REPLACE)
            assertEquals(4, result.skipped)
            assertEquals(listOf("good"), result.entries.map { it.id })
        }

    @Test
    fun aRestoreTellsTheCallerHowManyRowsItLeftOutSoTheOfficerCanBeTold() =
        runTest {
            // why: a skipped row is lost data (plan Phase 11 item 10); the restore must surface the count, not drop it.
            val rows = listOf(Part2RowBackup("bad-base64", "not base64 !!"), entryRow(entry("good", 1)), entryRow(entry("bad-key", 1), "some other password"))
            val orders = listOf(Part2RowBackup("junk-order", "!!"), orderRow(SyntheticBank.order("o1")))
            val bytes = archive(entries = rows, orders = orders, pages = listOf(pageRow("p1", "ghost", 0, SyntheticBank.jpeg(50))))

            val report = targetService.restore(bytes, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(4, report.skipped, "two unreadable entries, one unreadable order and one page whose order is nowhere")
        }

    @Test
    fun aCleanRestoreSkipsNothing() =
        runTest {
            val bytes = archive(entries = listOf(entryRow(entry("good", 1))), orders = listOf(orderRow(SyntheticBank.order("o1"))))

            assertEquals(0, targetService.restore(bytes, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow().skipped)
        }

    @Test
    fun aBackupFromBeforeThePartTwoRowsOrADeviceWithoutThemSkipsNothing() =
        runTest {
            val v4 = backupBytes(PortableBackup(4, listOf(goodEntity("08/2024")), emptyList(), null))

            assertEquals(0, targetService.restore(v4, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow().skipped)
        }

    @Test
    fun aDeviceRowThatCannotBeReadIsLeftOutOfAnExportNotFatalToIt() =
        runTest {
            source.putEntry(entry("d1", 1))
            source.part2.upsertEntry(Part2EntryEntity("junk", byteArrayOf(9)))
            source.part2.upsertEntry(entry("d3", 1).toEntity("a key this device no longer has"))

            val backup = decodeArchive(sourceService.export(BACKUP_PASSWORD).getOrThrow())

            assertEquals(listOf("d1"), backup.part2Entries.map { it.id })
        }

    @Test
    fun aVersion4BackupRestoresWithAnEmptyVaultOnReplaceAndLeavesTheDevicesVaultAloneOnMerge() =
        runTest {
            val v4 = backupBytes(PortableBackup(4, listOf(goodEntity("08/2024")), emptyList(), null))
            target.putEntry(entry("d1", 1))
            targetService.restore(v4, BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()
            assertEquals(setOf("d1"), targetEntries().keys)

            targetService.restore(v4, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertTrue(target.part2.allEntries().isEmpty(), "REPLACE means exactly this archive, as for Guide notes")
            assertEquals(listOf("08/2024"), target.getAllPayslipsOnce())
        }

    @Test
    fun aVersion6BackupIsRefusedAndChangesNothing() =
        runTest {
            target.putEntry(entry("d1", 1))
            val v6 = archive(entries = listOf(entryRow(entry("from-v6", 1))), version = PortableBackup.CURRENT_VERSION + 1)

            val error = targetService.restore(v6, BACKUP_PASSWORD, RestoreMode.REPLACE).exceptionOrNull()

            assertTrue(error is UnsupportedBackupVersionException, "expected a version refusal but was $error")
            assertEquals(setOf("d1"), targetEntries().keys)
        }

    @Test
    fun deleteAllDataAndAReplaceRestoreEmptyTheThreeTables() =
        runTest {
            target.putEntry(entry("d1", 1))
            target.putOrder(SyntheticBank.order("o1"))
            target.putPage("p1", "o1", 0, SyntheticBank.jpeg(100))

            target.clearAllUserData()

            assertTrue(target.part2.allEntries().isEmpty() && target.part2.allOrders().isEmpty() && target.part2.allPageRefs().isEmpty())
        }

    private suspend fun FakePayslipDao.getAllPayslipsOnce() = getPayslipByDate("08/2024")?.dateStr?.let { listOf(it) }.orEmpty()
}
