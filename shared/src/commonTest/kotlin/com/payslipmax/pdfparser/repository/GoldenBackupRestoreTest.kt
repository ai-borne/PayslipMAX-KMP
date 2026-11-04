package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * WHY: a backup is an officer's only copy of their payslips and orders when a phone is lost, and it outlives the app version that wrote
 * it. Every committed golden is a file a shipped (or this branch's) app wrote on Android or iOS; each must restore on head with exactly
 * what it carried. A change that drifts from a format already in the wild fails here, even when a backup made by today's code still
 * round-trips. Runs on the JVM and on the iOS simulator, so each platform also reads the other's files.
 */
class GoldenBackupRestoreTest {
    private data class Restored(
        val payslips: Int,
        val pdfs: Int,
        val corrections: Int,
        val guideNotes: Int,
        val drafts: Int,
        val dismissedDrafts: Int,
        val theme: String,
        val part2Entries: Int,
        val part2Orders: Int,
        val part2Pages: Int,
    )

    private fun GoldenBackup.expected() =
        Restored(payslips, pdfs, corrections, guideNotes, drafts, dismissedDrafts, theme, part2Entries, part2Orders, part2Pages)

    private suspend fun FakePayslipDao.restored() =
        Restored(
            payslips = getAllPayslips().first().size,
            pdfs = getAllPdfs().size,
            corrections = getAllCorrections().first().size,
            guideNotes = getAllGuideNotes().first().size,
            drafts = getAllRepresentationDrafts().first().size,
            dismissedDrafts = getAllDismissedDrafts().size,
            theme = getSettings()?.appTheme ?: "none",
            part2Entries = part2.allEntries().size,
            part2Orders = part2.allOrders().size,
            part2Pages = part2.allPageRefs().size,
        )

    @Test
    fun everyGoldenRestoresWithExactlyWhatItCarriedAndSkipsNothing() =
        runTest {
            for (golden in GoldenBackupCatalog.goldens) {
                val dao = FakePayslipDao()
                val service = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2))
                val report =
                    service
                        .restore(GoldenBackupCatalog.bytes(golden.file), Secret.of(golden.password), RestoreMode.REPLACE)
                        .getOrElse { fail("${golden.file} (${golden.origin}) no longer restores: ${it::class.simpleName}") }
                assertEquals(golden.expected(), dao.restored(), golden.file)
                assertEquals(SkippedRows.NONE, report.skipped, "${golden.file} skipped rows")
                assertEquals(
                    RestoredRows(golden.payslips, golden.part2Entries, golden.part2Orders, golden.part2Pages),
                    report.restored,
                    "${golden.file}: the report says what the file carried (P10 'From P8' item 3)",
                )
            }
        }

    @Test
    fun everyCommittedFileIsInTheManifestAndEveryEntryHasItsFile() {
        val listed = GoldenBackupCatalog.goldens.map { it.file }.toSet()
        val onDisk = goldenBackupFileNames().toSet() - GoldenBackupCatalog.MANIFEST
        assertEquals(onDisk, listed, "golden files and manifest entries must match one to one")
    }

    @Test
    fun everyFormatUpToTheCurrentOneHasAGolden() {
        val formats = GoldenBackupCatalog.goldens.map { it.format }.toSet()
        assertEquals((1..PortableBackup.CURRENT_VERSION).toSet(), formats, "bumping the backup version needs a golden of the new format")
    }

    @Test
    fun theCurrentFormatHasAGoldenWrittenOnEachPlatform() {
        val current = GoldenBackupCatalog.goldens.filter { it.format == PortableBackup.CURRENT_VERSION }.map { it.file.substringBefore('-') }
        assertTrue(current.containsAll(listOf("jvm", "ios")), "the current format needs a golden written on the JVM (Android code) and on iOS")
    }
}
