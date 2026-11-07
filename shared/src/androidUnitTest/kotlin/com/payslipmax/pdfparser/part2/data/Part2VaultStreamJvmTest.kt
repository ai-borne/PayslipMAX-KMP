package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.BackupSink
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.FileBackupSink
import com.payslipmax.pdfparser.repository.FileBackupSource
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the backup is one file and the vault ceiling is 250 MB, so export and restore must not need memory that grows with the vault. This
 * is the 120 page, 61 MB vault of `Part2VaultScaleJvmTest`, but through a file: the largest single write and read are one sealed page, and
 * the heap above the starting point (garbage included, sampled) stays far below the archive's size, in the 256 MB heap every unit test gets,
 * with both databases held at once. The old path (one array) is what `Part2VaultScaleJvmTest` still measures through the byte-array door.
 */
class Part2VaultStreamJvmTest {
    private val runtime = Runtime.getRuntime()

    private fun usedMb(): Long {
        repeat(2) { System.gc() }
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
    }

    /** Counts the widest write and, every eighth write, collects garbage and notes the heap that is still alive (what the export retains). */
    private inner class Watched(private val sink: BackupSink) : BackupSink {
        var widest = 0
        var peakLiveMb = 0L
        private var writes = 0

        override fun write(bytes: ByteArray) {
            widest = maxOf(widest, bytes.size)
            if (++writes % 8 == 0) peakLiveMb = maxOf(peakLiveMb, usedMb())
            sink.write(bytes)
        }
    }

    private class WatchedSource(private val source: BackupSource) : BackupSource {
        var widest = 0
        var total = 0L
        override val size: Long get() = source.size

        override fun read(
            offset: Long,
            length: Int,
        ): ByteArray {
            widest = maxOf(widest, length)
            total += length
            return source.read(offset, length)
        }
    }

    @Test
    fun `a full 120 page vault exports to a file and restores from it within a small heap`() {
        val file = File.createTempFile("vault-stream", ".pcda")
        try {
            val source = FakePayslipDao()
            val target = FakePayslipDao()
            val bytes = Part2Limits.PAGE_MAX_BYTES
            var seed = 0
            runBlocking {
                repeat(SCALE_ORDERS) { o ->
                    source.putOrder(SyntheticBank.order("order-$o"))
                    repeat(Part2Limits.PAGES_PER_ORDER_MAX) { p -> source.putPage("page-$o-$p", "order-$o", p, SyntheticBank.jpeg(bytes, seed++)) }
                }
            }
            val sourceService = PayslipBackupService(source, Dispatchers.Unconfined, part2Backup = Part2Backup(source.part2))
            val targetService = PayslipBackupService(target, Dispatchers.Unconfined, part2Backup = Part2Backup(target.part2))
            val baselineMb = usedMb()
            val watchedSink: Watched
            FileBackupSink(file).use { out ->
                watchedSink = Watched(out)
                runBlocking { sourceService.exportTo(watchedSink, Secret.of(BACKUP_PASSWORD)).getOrThrow() }
            }
            val archiveMb = file.length() / (1024 * 1024)
            val watchedSource: WatchedSource
            FileBackupSource(file).use { input ->
                watchedSource = WatchedSource(input)
                runBlocking { targetService.restore(watchedSource, Secret.of(BACKUP_PASSWORD), RestoreMode.REPLACE).getOrThrow() }
            }
            val retainedMb = watchedSink.peakLiveMb - baselineMb

            println("PART2-VAULT-STREAM jvm pages=120 archiveMB=$archiveMb exportRetainedHeapMB=$retainedMb widestWriteKB=${watchedSink.widest / 1024} widestReadKB=${watchedSource.widest / 1024} readTotalMB=${watchedSource.total / (1024 * 1024)}")
            assertEquals(120, runBlocking { target.part2.allPageRefs().size })
            assertContentEquals(SyntheticBank.jpeg(bytes, seed - 1), runBlocking { target.part2.page("page-${SCALE_ORDERS - 1}-9")!!.openPage(deviceKey) })
            val sealedMax = Part2Limits.PAGE_MAX_BYTES + 3 + 64 + 64 + Part2Limits.SEAL_OVERHEAD_BYTES
            assertTrue(watchedSink.widest <= sealedMax && watchedSource.widest <= sealedMax, "widest write ${watchedSink.widest}, read ${watchedSource.widest}: one sealed page at most")
            assertTrue(retainedMb < archiveMb / 4, "the export kept $retainedMb MB alive for a $archiveMb MB archive")
            assertTrue(watchedSource.total < 3 * file.length(), "the restore read ${watchedSource.total} bytes of a ${file.length()} byte file: each page a bounded number of times")
        } finally {
            file.delete()
        }
    }
}
