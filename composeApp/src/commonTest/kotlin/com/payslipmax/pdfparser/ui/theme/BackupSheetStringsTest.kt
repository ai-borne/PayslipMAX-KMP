package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupStage
import com.payslipmax.pdfparser.repository.RestorePreview
import com.payslipmax.pdfparser.repository.SkippedRows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P8, D11, D13, P10 "From P4" item 2 and "From P5" item 5): the sheet's words decide what an officer believes a restore
 * will do. The preview says what the backup HOLDS (a Merge may change less, so "adds" would be wrong), the skipped rows are named per kind
 * so the loss is concrete, and the progress line says only what is happening: a fixed phrase per stage, counts only for pages.
 */
class BackupSheetStringsTest {
    private fun preview(
        payslips: Int = 48,
        entries: Int = 0,
        orders: Int = 12,
        pages: Int = 120,
    ) = RestorePreview(6, null, payslips, payslips, 0, 0, 0, 0, entries, orders, pages, SkippedRows.NONE)

    @Test
    fun thePreviewSaysWhatTheBackupHoldsAndNeverThatItAdds() {
        assertEquals("This backup holds 48 payslips, 12 orders, 120 pages.", BackupRestoreStrings.previewHolds(preview()))
        assertEquals("This backup holds 1 payslip.", BackupRestoreStrings.previewHolds(preview(1, 0, 0, 0)))
        assertEquals("This backup holds 2 Orders Bank entries, 1 order.", BackupRestoreStrings.previewHolds(preview(0, 2, 1, 0)))
        assertFalse("add" in BackupRestoreStrings.previewHolds(preview()).lowercase())
    }

    @Test
    fun skippedRowsAreNamedPerKindAndZeroKindsAreLeftOut() {
        val skipped = SkippedRows(payslips = 2, corrections = 1, guideNotes = 1, part2Entries = 3, part2Orders = 1, part2Pages = 4)
        assertEquals(
            "2 payslips, 1 correction, 1 note, 3 Orders Bank entries, 1 order, 4 pages",
            BackupRestoreStrings.skippedList(skipped),
        )
        assertEquals("1 payslip", BackupRestoreStrings.skippedList(SkippedRows(payslips = 1)))
        assertEquals("", BackupRestoreStrings.skippedList(SkippedRows.NONE))
    }

    @Test
    fun theThreeSkippedSentencesAllNameTheList() {
        val skipped = SkippedRows(payslips = 2)
        assertTrue("2 payslips" in BackupRestoreStrings.previewSkipped(skipped))
        assertTrue("2 payslips" in BackupRestoreStrings.replaceSkipsMessage(skipped))
        assertTrue("2 payslips" in BackupRestoreStrings.restoredSkipped(skipped))
        assertTrue(BackupRestoreStrings.replaceSkipsMessage(skipped).contains("lost", ignoreCase = true), "D11: the second confirm names the loss")
    }

    @Test
    fun theProgressLineIsAFixedPhrasePerStageAndCountsOnlyPages() {
        val lines = mutableSetOf<String>()
        for (op in BackupOp.entries) {
            for (stage in BackupStage.entries) {
                val line = BackupRestoreStrings.progressLine(op, stage, 0, 0)
                assertTrue(line.isNotBlank(), "$op $stage")
                assertFalse(line.any { it.isDigit() }, "no number before a total is known: $line")
                lines += line
            }
        }
        assertTrue(lines.size >= BackupStage.entries.size, "stages read differently from each other")
        assertEquals("Restoring pages: 34 of 120", BackupRestoreStrings.progressLine(BackupOp.RESTORE, BackupStage.PAGES, 34, 120))
        assertEquals("Backing up pages: 3 of 9", BackupRestoreStrings.progressLine(BackupOp.BACKUP, BackupStage.PAGES, 3, 9))
        assertNotEquals(
            BackupRestoreStrings.progressLine(BackupOp.BACKUP, BackupStage.PAGES, 0, 0),
            BackupRestoreStrings.progressLine(BackupOp.RESTORE, BackupStage.PAGES, 0, 0),
        )
    }

    @Test
    fun theLastBackupLineSaysWhenOrThatThereWasNone() {
        assertEquals("Last backup: 03/11/2026", BackupRestoreStrings.lastBackup("03/11/2026"))
        assertTrue(BackupRestoreStrings.lastBackupNever.isNotBlank())
    }
}
