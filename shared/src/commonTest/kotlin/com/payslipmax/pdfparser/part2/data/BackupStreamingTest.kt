package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.BackupDamagedException
import com.payslipmax.pdfparser.repository.BackupSink
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.MemoryBackupSink
import com.payslipmax.pdfparser.repository.MemoryBackupSource
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.repository.pageArchiveMetaOfV6
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the backup is ONE file, and it must stay one file however big the vault grows, so export writes it to a sink a piece at a time and
 * restore reads it from a source by range. A 250 MB vault cannot pass through an app heap as one array. These pin that nothing the size
 * of the archive is ever written or read in one piece, and that the file is the same archive either way.
 */
class BackupStreamingTest {
    private lateinit var source: FakePayslipDao
    private lateinit var target: FakePayslipDao
    private lateinit var sourceService: PayslipBackupService
    private lateinit var targetService: PayslipBackupService
    private val pageBytes = 30_000
    private val pages = 40

    @BeforeTest
    fun setUp() {
        source = FakePayslipDao()
        target = FakePayslipDao()
        sourceService = PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2))
        targetService = PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(target.part2))
    }

    private suspend fun fill() {
        // An order holds at most Part2Limits.PAGES_PER_ORDER_MAX pages, so 40 pages are four orders.
        repeat(pages / 10) { source.putOrder(SyntheticBank.order("o$it")) }
        source.putEntry(SyntheticBank.entry("e1", 10, orderId = "o0"))
        repeat(pages) { source.putPage("p$it", "o${it / 10}", it % 10, SyntheticBank.jpeg(pageBytes, it + 1)) }
    }

    private class WatchedSink : BackupSink {
        val inner = MemoryBackupSink()
        var widest = 0
        var writes = 0

        override fun write(bytes: ByteArray) {
            widest = maxOf(widest, bytes.size)
            writes++
            inner.write(bytes)
        }
    }

    private class WatchedSource(private val inner: BackupSource) : BackupSource {
        var widest = 0
        var reads = 0
        override val size: Long get() = inner.size

        override fun read(
            offset: Long,
            length: Int,
        ): ByteArray {
            widest = maxOf(widest, length)
            reads++
            return inner.read(offset, length)
        }
    }

    @Test
    fun exportToWritesAPageAtATimeAndNeverTheWholeArchiveInOneWrite() =
        runTest {
            fill()
            val sink = WatchedSink()

            sourceService.exportTo(sink, BACKUP_PASSWORD).getOrThrow()

            val total = sink.inner.toByteArray().size
            assertTrue(total > pages * pageBytes, "the archive carries every page: $total")
            assertTrue(sink.widest < total / 10, "largest single write ${sink.widest} of $total")
            assertTrue(sink.writes >= pages, "the pages arrive one at a time: ${sink.writes} writes")
        }

    @Test
    fun theStreamedArchiveRestoresEveryPageByteForByteFromASourceReadByRange() =
        runTest {
            fill()
            val sink = MemoryBackupSink()
            sourceService.exportTo(sink, BACKUP_PASSWORD).getOrThrow()
            val file = WatchedSource(MemoryBackupSource(sink.toByteArray()))

            val report = targetService.restore(file, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(SkippedRows.NONE, report.skipped)
            assertEquals(pages, target.part2.allPageRefs().size)
            repeat(pages) { assertContentEquals(SyntheticBank.jpeg(pageBytes, it + 1), target.part2.page("p$it")!!.openPage(deviceKey)) }
            assertEquals("e1", target.part2.allEntries().single().id)
            assertTrue(file.widest < file.size / 10, "largest single read ${file.widest} of ${file.size}")
            assertTrue(file.reads > pages, "the restore walks the pages one range at a time: ${file.reads} reads")
        }

    @Test
    fun theByteArrayFormsAreTheSameArchiveAsTheStreamedOne() =
        runTest {
            fill()
            val streamed = MemoryBackupSink().also { sourceService.exportTo(it, BACKUP_PASSWORD).getOrThrow() }.toByteArray()
            val whole = sourceService.export(BACKUP_PASSWORD).getOrThrow()

            assertEquals(streamed.size, whole.size, "same layout, same length (the salts differ, the sizes do not)")
            assertTrue(pageArchiveMetaOfV6(streamed) != null && pageArchiveMetaOfV6(whole) != null)
            targetService.restore(streamed, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()
            assertEquals(pages, target.part2.allPageRefs().size)
        }

    @Test
    fun aVaultWithNoPagesStillWritesAPageArchiveThatCountsNone() =
        runTest {
            source.putEntry(SyntheticBank.entry("e1", 10))
            val sink = MemoryBackupSink()

            sourceService.exportTo(sink, BACKUP_PASSWORD).getOrThrow()

            val bytes = sink.toByteArray()
            assertTrue(pageArchiveMetaOfV6(bytes) != null, "no pages: still a page archive, one shape for format 6")
            targetService.restore(MemoryBackupSource(bytes), BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()
            assertEquals("e1", target.part2.allEntries().single().id)
        }

    @Test
    fun aSourceThatIsCutShortFailsTheWholeRestoreAndTheDeviceKeepsEverything() =
        runTest {
            fill()
            val bytes = MemoryBackupSink().also { sourceService.exportTo(it, BACKUP_PASSWORD).getOrThrow() }.toByteArray()
            target.putEntry(SyntheticBank.entry("keep", 5))
            val cut = MemoryBackupSource(bytes.copyOf(bytes.size - 60_000))

            val error = targetService.restore(cut, BACKUP_PASSWORD, RestoreMode.REPLACE).exceptionOrNull()

            assertTrue(error is BackupDamagedException || error != null, "a cut file fails")
            assertEquals(listOf("keep"), target.part2.allEntries().map { it.id }, "nothing was changed")
        }

    @Test
    fun anExportThatFailsHalfWayReportsFailureAndRecordsNoBackup() =
        runTest {
            fill()
            var writes = 0
            val failing = BackupSink { if (++writes > 5) throw IllegalStateException("the disk is full") }

            val result = sourceService.exportTo(failing, BACKUP_PASSWORD)

            assertTrue(result.isFailure)
        }

    @Test
    fun aFileThatClaimsAHugeTrailerIsRefusedWithoutReadingItAndKeepsTheDevice() =
        runTest {
            fill()
            val good = MemoryBackupSink().also { sourceService.exportTo(it, BACKUP_PASSWORD).getOrThrow() }.toByteArray()
            val lying = good.copyOf()
            val claimed = 200_000
            for (i in 0 until 4) lying[lying.size - 4 + i] = (claimed ushr (24 - 8 * i)).toByte()
            val watched = WatchedSource(MemoryBackupSource(lying))
            target.putEntry(SyntheticBank.entry("keep", 5))

            val error = targetService.restore(watched, BACKUP_PASSWORD, RestoreMode.REPLACE).exceptionOrNull()

            assertTrue(error is BackupDamagedException, "a trailer of $claimed bytes is a lie: $error")
            assertTrue(watched.widest < claimed, "the claimed range was never read: widest ${watched.widest}")
            assertEquals(listOf("keep"), target.part2.allEntries().map { it.id })
        }
}
