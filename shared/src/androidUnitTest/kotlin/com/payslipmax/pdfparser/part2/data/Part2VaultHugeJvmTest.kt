package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.FileBackupSink
import com.payslipmax.pdfparser.repository.FileBackupSource
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the vault ceiling is 250 MB on a phone whose app heap is 256 MB, which is safe only because a restore never holds the vault. This
 * writes a full vault (500 pages, about 256 MB) to a file and restores it, in the 256 MB heap every unit test gets: every page must come
 * back (the ceiling really is 250 MB, not 100), and collected, the heap in use must stay a small fraction of the archive all the way
 * through (forced-collection sampling, as in `Part2VaultStreamJvmTest`). Running out of memory IS the failure.
 */
class Part2VaultHugeJvmTest {
    private val runtime = Runtime.getRuntime()

    private fun usedMb(): Long {
        repeat(2) { System.gc() }
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    /** Every 16th read, collects garbage and notes what is still alive: what the restore retains. */
    private inner class Sampled(private val source: BackupSource) : BackupSource {
        var peakLiveMb = 0L
        private var reads = 0
        override val size: Long get() = source.size

        override fun read(
            offset: Long,
            length: Int,
        ): ByteArray {
            if (++reads % 16 == 0) peakLiveMb = maxOf(peakLiveMb, usedMb())
            return source.read(offset, length)
        }
    }

    @Test
    fun `a full 250 MB vault restores from a file with a flat heap`() {
        val file = File.createTempFile("vault-huge", ".pcda")
        try {
            val pages = FileBackupSink(file).use { runBlocking { writeHugeArchive(it) } }
            val archiveMb = file.length() / (1024 * 1024)
            val baselineMb = usedMb()
            lateinit var sampled: Sampled
            val result =
                FileBackupSource(file).use { input ->
                    sampled = Sampled(input)
                    runBlocking { restoreHuge(sampled) }
                }
            val retainedMb = sampled.peakLiveMb - baselineMb

            println("PART2-VAULT-HUGE jvm pages=$pages archiveMB=$archiveMb restoreMs=${result.restoreMs} restoreRetainedHeapMB=$retainedMb")
            assertTrue(archiveMb in 200..250, "the archive is a real 250 MB-class file, not a small one: $archiveMb MB")
            assertEquals(0, result.skipped, "no page was left out: the vault fits under the ceiling")
            assertEquals(HUGE_PAGES, result.restoredPages)
            assertTrue(result.restoredBytes <= Part2Limits.VAULT_BYTES_MAX, "and the stored vault is under the ceiling")
            assertTrue(retainedMb < archiveMb / 8, "the restore kept $retainedMb MB alive for a $archiveMb MB archive")
            assertTrue(result.restoreMs < 300_000, "restore took ${result.restoreMs} ms")
        } finally {
            file.delete()
        }
    }
}
