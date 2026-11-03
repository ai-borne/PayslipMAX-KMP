package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.toCorrectionEntity
import com.payslipmax.pdfparser.database.toEntity
import com.payslipmax.pdfparser.guide.domain.GuideNote
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.Part2LegacyDraft
import com.payslipmax.pdfparser.part2.data.Part2RowBackup
import com.payslipmax.pdfparser.repository.backup.CorrectionBackupRow
import com.payslipmax.pdfparser.repository.backup.GuideNoteBackupRow
import com.payslipmax.pdfparser.repository.backup.PayslipBackupRow
import com.payslipmax.pdfparser.repository.backup.PdfBackupRow
import com.payslipmax.pdfparser.repository.backup.toBackupRow
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P5, F6): payslips exist only on the phone, so one row that will not open must not cost the officer every other row
 * in the file. Each payslip, correction and note is skipped and counted on its own (the Part II rule: skip, count, never fatal), and the
 * count is per kind so the officer can be told what is missing.
 */
class BackupSkipCountTest {
    private val dao = FakePayslipDao()
    private val service = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2))

    private fun badPayslip(dateStr: String) = PayslipBackupRow(dateStr, 2014, 9, "September", ciphertext = "deadbeef")

    private fun correction(dateStr: String) = mapOf("basicPay" to 1.0).toCorrectionEntity(dateStr, BACKUP_PASSWORD).toBackupRow()

    private fun note(i: Int) = GuideNote.of("RB-NOTE-$i", "note $i", "rev00001", 1L)!!.toEntity(BACKUP_PASSWORD).toBackupRow()

    private fun file(
        payslips: List<PayslipBackupRow> = listOf(goodRow("08/2024")),
        corrections: List<CorrectionBackupRow> = emptyList(),
        notes: List<GuideNoteBackupRow> = emptyList(),
        entries: List<Part2RowBackup> = emptyList(),
        drafts: List<Part2LegacyDraft> = emptyList(),
        pdfs: List<PdfBackupRow> = emptyList(),
    ) = backupBytes(
        PortableBackup(5, payslips, pdfs, null, corrections = corrections, guideNotes = notes, part2Entries = entries, part2Drafts = drafts),
    )

    private suspend fun restore(bytes: ByteArray): RestoreReport {
        val result = service.restore(bytes, BACKUP_PASSWORD, RestoreMode.REPLACE)
        assertTrue(result.isSuccess, "one unreadable row must not fail the restore: ${result.exceptionOrNull()?.let { it::class.simpleName }}")
        return result.getOrThrow()
    }

    @Test
    fun oneUnreadablePayslipAmongThreeIsSkippedAndCountedAndTheOtherTwoRestore() =
        runTest {
            val report = restore(file(payslips = listOf(goodRow("08/2024"), badPayslip("09/2014"), goodRow("10/2024"))))

            assertEquals(SkippedRows(payslips = 1), report.skipped)
            assertEquals(listOf("08/2024", "10/2024"), dao.getAllPayslips().first().map { it.dateStr }.sorted())
        }

    @Test
    fun oneUnreadableCorrectionAmongThreeIsSkippedAndCountedAndTheOtherTwoRestore() =
        runTest {
            val corrections = listOf(correction("08/2024"), CorrectionBackupRow("09/2024", "deadbeef"), correction("10/2024"))

            val report = restore(file(corrections = corrections))

            assertEquals(SkippedRows(corrections = 1), report.skipped)
            assertEquals(listOf("08/2024", "10/2024"), dao.getAllCorrections().first().map { it.dateStr }.sorted())
        }

    @Test
    fun oneUnreadableNoteAmongThreeIsSkippedAndNowCountedToo() =
        runTest {
            val report = restore(file(notes = listOf(note(1), GuideNoteBackupRow("RB-NOTE-2", "deadbeef"), note(3))))

            assertEquals(SkippedRows(guideNotes = 1), report.skipped)
            assertEquals(listOf("RB-NOTE-1", "RB-NOTE-3"), dao.getAllGuideNotes().first().map { it.cardId }.sorted())
        }

    @Test
    fun aSkippedPayslipsPdfAndCorrectionsStillRestoreBecauseEachRowStandsAlone() =
        runTest {
            // The officer still holds the file unchanged, and a correction applies on read by month, so it rejoins its payslip if a later
            // app restores that payslip or the month is imported again; dropping them would lose readable rows for nothing.
            val report =
                restore(
                    file(
                        payslips = listOf(goodRow("08/2024"), badPayslip("09/2024")),
                        corrections = listOf(correction("09/2024")),
                        pdfs = listOf(PdfBackupRow("09/2024", byteArrayOf(7))),
                    ),
                )

            assertEquals(SkippedRows(payslips = 1), report.skipped)
            assertEquals(listOf("09/2024"), dao.getAllPdfs().map { it.dateStr })
            assertEquals(listOf("09/2024"), dao.getAllCorrections().first().map { it.dateStr })
        }

    /**
     * WHY: "all rows unreadable" means every row that is opened one at a time (payslips, corrections, notes, Bank entries, orders and page
     * records) was skipped, and there was at least one. Such a file is damaged, and restoring "nothing" would wipe the phone on REPLACE, so
     * it fails as [BackupDamagedException] with the database untouched. PDFs, letters, deleted-letter records and settings are left out of
     * the count: they cannot fail one at a time once the document opens. Frozen-branch drafts are left out too: they are never restored.
     */
    @Test
    fun aFileWhoseEveryRowIsUnreadableIsDamagedAndChangesNothing() =
        runTest {
            dao.seedDeviceMonth("01/2023")
            val everyKindBad =
                file(
                    payslips = listOf(badPayslip("09/2014")),
                    corrections = listOf(CorrectionBackupRow("09/2014", "deadbeef")),
                    notes = listOf(GuideNoteBackupRow("RB-NOTE-1", "deadbeef")),
                    entries = listOf(Part2RowBackup("junk-entry", "!!")),
                    pdfs = listOf(PdfBackupRow("09/2014", byteArrayOf(7))),
                )
            val onlyPart2Bad = file(payslips = emptyList(), entries = listOf(Part2RowBackup("junk-1", "!!"), Part2RowBackup("junk-2", "!!")))

            for ((name, bytes) in listOf("every kind" to everyKindBad, "only Part II rows" to onlyPart2Bad)) {
                for (mode in RestoreMode.entries) {
                    val result = service.restore(bytes, BACKUP_PASSWORD, mode)

                    assertIs<BackupDamagedException>(result.exceptionOrNull(), "$name, $mode")
                    assertEquals(listOf("01/2023"), dao.getAllPayslips().first().map { it.dateStr }, "$name, $mode")
                    assertEquals(listOf("01/2023"), dao.getAllPdfs().map { it.dateStr }, "$name, $mode")
                    assertEquals(1, dao.getAllCorrections().first().size, "$name, $mode")
                    assertEquals(1, dao.getAllGuideNotes().first().size, "$name, $mode")
                }
            }
        }

    @Test
    fun aFileWithNoRowThatOpensOneAtATimeIsNotDamaged() =
        runTest {
            // Nothing to be unreadable: an empty device's backup, or one holding only PDFs or frozen-branch drafts, still restores.
            assertEquals(SkippedRows.NONE, restore(file(payslips = emptyList())).skipped)
            assertEquals(SkippedRows.NONE, restore(file(payslips = emptyList(), pdfs = listOf(PdfBackupRow("08/2024", byteArrayOf(1))))).skipped)
            assertEquals(SkippedRows(part2Entries = 2), restore(file(payslips = emptyList(), drafts = listOf(Part2LegacyDraft("d1"), Part2LegacyDraft("d2")))).skipped)
        }
}
