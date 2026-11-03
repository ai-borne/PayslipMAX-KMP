package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.part2.data.InMemoryLastBackupStorage
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.putOrder
import com.payslipmax.pdfparser.part2.data.putPage
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.space.FreeSpaceGate
import com.payslipmax.pdfparser.testing.FakeBackupFiles
import com.payslipmax.pdfparser.testing.FakeFreeSpaceReader
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P6): a disk that fills half-way through used to fail with whatever the platform said, after minutes of work. Free
 * space is checked first, against what each operation will write, plus the margin the app keeps free before any write
 * ([Part2Limits.FREE_SPACE_REFUSE_BYTES], 100 MB, the line below which no scan opens either), so the phone is never left full:
 * - a backup writes one file about the size of the vault (its pages are copied as they are; payslips, PDFs and rows are a few MB, inside
 *   the margin), so it needs the vault's bytes plus the margin;
 * - a restore writes the backup's rows twice before space is given back (into the database's write-ahead log, then into the database
 *   file), with the picked file already on the disk, so it needs twice the file's size plus the margin.
 * A device that cannot say how much is free is never blocked: the writes' own failures still guard it.
 */
class BackupStorageFullTest {
    private val source = FakePayslipDao()
    private val files = FakeBackupFiles()
    private val lastBackup = InMemoryLastBackupStorage()
    private val space = FakeFreeSpaceReader()
    private val margin = Part2Limits.FREE_SPACE_REFUSE_BYTES

    private fun service(dao: FakePayslipDao) =
        PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2), lastBackup = lastBackup, space = FreeSpaceGate(space))

    private suspend fun seed() {
        source.seedDeviceMonth("08/2024")
        source.putOrder(SyntheticBank.order("o1"))
        repeat(3) { source.putPage("p$it", "o1", it, SyntheticBank.jpeg(5_000, it)) }
    }

    @Test
    fun aBackupWithoutRoomForTheVaultAndTheMarginWritesNoFileAndRecordsNothing() =
        runTest {
            seed()
            val vault = source.part2.vaultBytes()
            assertTrue(vault > 0)
            space.free = vault + margin - 1

            val result = service(source).backUp(files, BACKUP_PASSWORD)

            assertIs<BackupStorageFullException>(result.exceptionOrNull())
            assertEquals(0, files.verifyCount, "nothing was written, so nothing was read back")
            assertNull(files.exportedBytes)
            assertNull(lastBackup.load())
        }

    @Test
    fun aBackupWithExactlyEnoughRoomGoesAheadAndItsReadBackIsNotHeldToTheRestoreRule() =
        runTest {
            seed()
            // Enough for the backup, far from enough for a restore of the same file: the read-back writes nothing, so it is not checked.
            space.free = source.part2.vaultBytes() + margin

            service(source).backUp(files, BACKUP_PASSWORD).getOrThrow()

            assertEquals(1, files.verifyCount)
        }

    @Test
    fun aRestoreWithoutRoomForTwiceTheFileAndTheMarginChangesNothing() =
        runTest {
            seed()
            val file = service(source).export(BACKUP_PASSWORD).getOrThrow()
            val device = FakePayslipDao().apply { seedDeviceMonth("01/2023") }
            val before = device.getAllPayslips().first().map { it.dateStr } to device.part2.vaultBytes()
            space.free = 2L * file.size + margin - 1

            for (mode in RestoreMode.entries) {
                val result = service(device).restore(file, BACKUP_PASSWORD, mode)
                assertIs<BackupStorageFullException>(result.exceptionOrNull(), "$mode")
                assertEquals(before, device.getAllPayslips().first().map { it.dateStr } to device.part2.vaultBytes(), "$mode: untouched")
            }
            assertIs<BackupStorageFullException>(service(device).prepare(MemoryBackupSource(file), BACKUP_PASSWORD).exceptionOrNull())

            space.free = 2L * file.size + margin
            service(device).restore(file, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()
            assertEquals(listOf("08/2024"), device.getAllPayslips().first().map { it.dateStr })
        }

    @Test
    fun aDeviceThatCannotSayHowMuchIsFreeIsNeverBlocked() =
        runTest {
            seed()
            for (reader in listOf(FakeFreeSpaceReader(free = null), FakeFreeSpaceReader(fails = true))) {
                val unknown = PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2), space = FreeSpaceGate(reader))
                unknown.backUp(files, BACKUP_PASSWORD).getOrThrow()
                unknown.restore(unknown.export(BACKUP_PASSWORD).getOrThrow(), BACKUP_PASSWORD, RestoreMode.MERGE).getOrThrow()
            }
        }
}
