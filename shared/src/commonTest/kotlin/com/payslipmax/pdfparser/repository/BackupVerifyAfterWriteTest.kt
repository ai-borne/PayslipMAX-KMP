package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.part2.data.InMemoryLastBackupStorage
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.Part2RowBackup
import com.payslipmax.pdfparser.part2.data.archive
import com.payslipmax.pdfparser.part2.data.junkEntry
import com.payslipmax.pdfparser.part2.data.orderRow
import com.payslipmax.pdfparser.part2.data.pageRow
import com.payslipmax.pdfparser.part2.data.putOrder
import com.payslipmax.pdfparser.part2.data.putPage
import com.payslipmax.pdfparser.repository.backup.BackupEnvelope
import com.payslipmax.pdfparser.repository.backup.CorrectionBackupRow
import com.payslipmax.pdfparser.repository.backup.GuideNoteBackupRow
import com.payslipmax.pdfparser.repository.backup.PayslipBackupRow
import com.payslipmax.pdfparser.testing.FakeBackupFiles
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P4, F4): an officer finds out a backup is bad only when the phone is gone. So a backup is read back from the file
 * as written, every page included, before it can be shared or counted as "last backup"; one that does not read back is deleted.
 */
class BackupVerifyAfterWriteTest {
    private val dao = FakePayslipDao()
    private val files = FakeBackupFiles()
    private val lastBackup = InMemoryLastBackupStorage()
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2), lastBackup = lastBackup, now = { 7_000L })

    private suspend fun seed() {
        dao.seedDeviceMonth("08/2024")
        dao.putOrder(SyntheticBank.order("o1"))
        repeat(2) { dao.putPage("p$it", "o1", it, SyntheticBank.jpeg(3_000, it)) }
    }

    @Test
    fun aBackupThatReadsBackIsKeptForTheShareSheetAndRecorded() =
        runTest {
            seed()

            service.backUp(files, Secret.of(BACKUP_PASSWORD)).getOrThrow()

            assertEquals(1, files.verifyCount)
            assertNotNull(files.exportedBytes)
            assertEquals(7_000L, lastBackup.load())
        }

    @Test
    fun aWrittenByteTheDiskDidNotKeepDeletesTheFileAndRecordsNothing() =
        runTest {
            seed()
            val good = service.export(Secret.of(BACKUP_PASSWORD)).getOrThrow()
            lastBackup.save(1L)
            // One byte in the document, one in the last page record, one in the trailer: wherever the damage is, it is found.
            for (at in listOf(BackupEnvelope.HEADER_BYTES + 12, good.size - 200, good.size - 10)) {
                files.corruptWrittenByteAt = at

                val result = service.backUp(files, Secret.of(BACKUP_PASSWORD))

                assertIs<BackupVerifyException>(result.exceptionOrNull(), "byte $at")
                assertNull(files.exportedBytes, "byte $at: the damaged file is gone")
                assertEquals(1L, lastBackup.load(), "byte $at: a backup that did not read back is not the last backup")
            }
        }

    @Test
    fun aPageWhoseOrderDoesNotTravelIsLeftOutSoTheBackupStillReadsBackWhole() =
        runTest {
            seed()
            // An orphan: no order "gone" exists, so a restore could only skip this page; a backup carrying it would never verify.
            dao.putPage("orphan", "gone", 0, SyntheticBank.jpeg(3_000, 9))

            service.backUp(files, Secret.of(BACKUP_PASSWORD)).getOrThrow()

            val target = FakePayslipDao()
            val restored = PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(target.part2))
            restored.restore(files.exportedBytes!!, Secret.of(BACKUP_PASSWORD)).getOrThrow()
            assertEquals(listOf("p0", "p1"), target.part2.allPageRefs().map { it.id }.sorted())
        }

    /** WHY (14a P5): a restore now skips an unreadable row instead of failing, so a fresh backup that reads back with ANY row skipped is bad. */
    @Test
    fun aFileThatReadsBackWithOneRowSkippedOfAnyKindIsNotKept() =
        runTest {
            seed()
            val bad = PayslipBackupRow("09/2014", 2014, 9, "September", ciphertext = "deadbeef")
            val order = orderRow(SyntheticBank.order("o1"))
            val oneSkipEach =
                mapOf(
                    "payslip" to backupBytes(PortableBackup(5, listOf(goodRow("08/2024"), bad), emptyList(), null)),
                    "correction" to backupBytes(PortableBackup(5, listOf(goodRow("08/2024")), emptyList(), null, corrections = listOf(CorrectionBackupRow("08/2024", "beef")))),
                    "note" to backupBytes(PortableBackup(5, listOf(goodRow("08/2024")), emptyList(), null, guideNotes = listOf(GuideNoteBackupRow("RB-NOTE-1", "beef")))),
                    "entry" to archive(entries = listOf(junkEntry(1))),
                    "order" to archive(orders = listOf(Part2RowBackup("o2", "!!"))),
                    "page" to archive(orders = listOf(order), pages = listOf(pageRow("p1", "o1", 0, SyntheticBank.jpeg(500), password = "other"))),
                )
            for ((kind, file) in oneSkipEach) {
                files.readBackInstead = file

                assertIs<BackupVerifyException>(service.backUp(files, Secret.of(BACKUP_PASSWORD)).exceptionOrNull(), kind)
                assertNull(files.exportedBytes, kind)
            }
        }

    @Test
    fun theVerifyFailureSaysNothingAboutTheFileOrItsContent() =
        runTest {
            seed()
            files.corruptWrittenByteAt = BackupEnvelope.HEADER_BYTES + 12

            val message = service.backUp(files, Secret.of(BACKUP_PASSWORD)).exceptionOrNull()?.message.orEmpty()

            assertTrue(message.isNotEmpty() && !message.contains("08/2024") && !message.contains("pcda") && !message.contains("/"), message)
        }
}
