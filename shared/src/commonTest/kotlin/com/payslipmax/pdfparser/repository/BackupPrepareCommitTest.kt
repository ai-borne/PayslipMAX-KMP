package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.part2.data.openPage
import com.payslipmax.pdfparser.part2.data.putEntry
import com.payslipmax.pdfparser.part2.data.putOrder
import com.payslipmax.pdfparser.part2.data.putPage
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * WHY (docs/Plan/14a P4, F4): an officer must see what a backup holds before anything on the phone changes, and a restore must then write
 * exactly that. `prepare` decodes and checks the whole file (every page included) and writes nothing; `commit` is the one transaction.
 * The preview's counts are pinned to the goldens' manifest, so a preview that miscounts a shipped format fails here.
 */
class BackupPrepareCommitTest {
    private var clock = 1_788_000_000_000L

    private fun serviceOn(
        dao: FakePayslipDao,
        part2: Part2Backup = Part2Backup(dao.part2),
    ) = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = part2, now = { clock })

    private suspend fun FakePayslipDao.isEmpty() =
        getAllPayslips().first().isEmpty() && getAllPdfs().isEmpty() && getAllCorrections().first().isEmpty() &&
            getAllGuideNotes().first().isEmpty() && getAllRepresentationDrafts().first().isEmpty() && getAllDismissedDrafts().isEmpty() &&
            getSettings() == null && part2.allEntries().isEmpty() && part2.allOrders().isEmpty() && part2.allPageRefs().isEmpty()

    private suspend fun FakePayslipDao.counts() =
        listOf(
            getAllPayslips().first().size,
            getAllPdfs().size,
            getAllCorrections().first().size,
            getAllGuideNotes().first().size,
            getAllRepresentationDrafts().first().size,
            getAllDismissedDrafts().size,
            part2.allEntries().size,
            part2.allOrders().size,
            part2.allPageRefs().size,
        )

    private fun RestorePreview.counts() = listOf(payslips, pdfs, corrections, guideNotes, drafts, dismissedDrafts, part2Entries, part2Orders, part2Pages)

    private fun GoldenBackup.counts() = listOf(payslips, pdfs, corrections, guideNotes, drafts, dismissedDrafts, part2Entries, part2Orders, part2Pages)

    private suspend fun deviceWithPages(pages: Int): FakePayslipDao =
        FakePayslipDao().apply {
            seedDeviceMonth("08/2024")
            putEntry(SyntheticBank.entry("e1", 1))
            putOrder(SyntheticBank.order("o1"))
            repeat(pages) { putPage("p$it", "o1", it, SyntheticBank.jpeg(4_000, it)) }
        }

    @Test
    fun aPreviewOfEveryGoldenCountsWhatItsManifestSaysAndWritesNothing() =
        runTest {
            for (golden in GoldenBackupCatalog.goldens) {
                val dao = FakePayslipDao()
                val prepared =
                    serviceOn(dao)
                        .prepare(MemoryBackupSource(GoldenBackupCatalog.bytes(golden.file)), golden.password)
                        .getOrElse { fail("${golden.file} no longer prepares: ${it::class.simpleName}") }
                prepared.use {
                    assertEquals(golden.counts(), it.preview.counts(), golden.file)
                    assertEquals(golden.format, it.preview.formatVersion, golden.file)
                    assertEquals(SkippedRows.NONE, it.preview.skipped, golden.file)
                }
                assertTrue(dao.isEmpty(), "${golden.file}: a preview wrote to the database")
            }
        }

    @Test
    fun aCommitWritesExactlyWhatThePreviewReported() =
        runTest {
            val file = serviceOn(deviceWithPages(3)).export(BACKUP_PASSWORD).getOrThrow()
            val target = FakePayslipDao()
            val service = serviceOn(target)

            val prepared = service.prepare(MemoryBackupSource(file), BACKUP_PASSWORD).getOrThrow()
            val report = prepared.use { service.commit(it, RestoreMode.REPLACE).getOrThrow() }

            assertEquals(listOf(1, 1, 1, 1, 1, 1, 1, 1, 3), prepared.preview.counts(), "what the device held: one of each, three pages")
            assertEquals(prepared.preview.counts(), target.counts())
            assertEquals(prepared.preview.skipped, report.skipped)
        }

    @Test
    fun aPreviewChecksEveryPageButKeepsNoneTheCommitReEncryptsThemOneAtATime() =
        runTest {
            val file = serviceOn(deviceWithPages(4)).export(BACKUP_PASSWORD).getOrThrow()
            val target = FakePayslipDao()
            var opened = 0
            val service = serviceOn(target, Part2Backup(target.part2, openPage = { page, key -> opened++.let { page.openPage(key) } }))

            val prepared = service.prepare(MemoryBackupSource(file), BACKUP_PASSWORD).getOrThrow()
            assertEquals(4, opened, "each page is checked once in the preview, and nothing re-encrypted yet")
            assertTrue(target.part2.allPageRefs().isEmpty())

            prepared.use { service.commit(it, RestoreMode.REPLACE).getOrThrow() }
            assertEquals(8, opened, "the commit opens each page again as it inserts it: the vault is never held as a list")
        }

    @Test
    fun aNewBackupSaysWhenItWasMadeAndAnOlderOneSaysNothing() =
        runTest {
            val file = serviceOn(deviceWithPages(0)).export(BACKUP_PASSWORD).getOrThrow()
            val service = serviceOn(FakePayslipDao())

            assertEquals(clock, service.prepare(MemoryBackupSource(file), BACKUP_PASSWORD).getOrThrow().use { it.preview.exportedAt })
            val v5 = GoldenBackupCatalog.goldens.first { it.format == 5 }
            assertNull(service.prepare(MemoryBackupSource(GoldenBackupCatalog.bytes(v5.file)), v5.password).getOrThrow().use { it.preview.exportedAt })
        }

    @Test
    fun aMergeCommitKeepsTheDevicesOwnPayslipsAfterAPreview() =
        runTest {
            val file = serviceOn(deviceWithPages(1)).export(BACKUP_PASSWORD).getOrThrow()
            val target = FakePayslipDao().apply { seedDeviceMonth("01/2023") }
            val service = serviceOn(target)

            service.prepare(MemoryBackupSource(file), BACKUP_PASSWORD).getOrThrow().use { service.commit(it, RestoreMode.MERGE).getOrThrow() }

            assertEquals(listOf("01/2023", "08/2024"), target.getAllPayslips().first().map { it.dateStr }.sorted())
            assertEquals(1, target.part2.allPageRefs().size)
        }

    @Test
    fun progressGoesFromOpeningToReadingAndCountsEveryPageWritten() =
        runTest {
            val file = serviceOn(deviceWithPages(3)).export(BACKUP_PASSWORD).getOrThrow()
            val service = serviceOn(FakePayslipDao())
            val seen = mutableListOf<Triple<BackupStage, Int, Int>>()
            val progress = BackupProgress { stage, done, total -> seen += Triple(stage, done, total) }

            service.prepare(MemoryBackupSource(file), BACKUP_PASSWORD, progress).getOrThrow().use { service.commit(it, RestoreMode.REPLACE, progress).getOrThrow() }

            assertEquals(BackupStage.OPENING, seen.first().first)
            assertTrue(seen.any { it.first == BackupStage.READING })
            assertEquals(Triple(BackupStage.PAGES, 3, 3), seen.last(), "the last update is the last page written")
            assertEquals(seen.map { it.first.ordinal }, seen.map { it.first.ordinal }.sorted(), "stages only move forward")
        }
}
