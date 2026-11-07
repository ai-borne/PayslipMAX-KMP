@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.BackupSource
import com.payslipmax.pdfparser.repository.ClosableBackupSink
import com.payslipmax.pdfparser.repository.FileBackupSink
import com.payslipmax.pdfparser.repository.FileBackupSource
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.darwin.MACH_TASK_BASIC_INFO
import platform.darwin.mach_msg_type_number_tVar
import platform.darwin.mach_task_basic_info
import platform.darwin.mach_task_self_
import platform.darwin.task_info
import kotlin.native.runtime.GC
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the 250 MB ceiling is safe on the iPhone only if a restore does not hold the vault, on Kotlin/Native's allocator and collector
 * (the JVM twin is `Part2VaultHugeJvmTest`). A full vault (500 pages, about 256 MB) is written to a file and restored: every page must
 * come back, and the resident size after a forced collection, sampled through the whole restore, must stay a small fraction of the archive.
 */
class Part2VaultHugeIosTest {
    private fun residentMb(): Long =
        memScoped {
            val info = alloc<mach_task_basic_info>()
            val count = alloc<mach_msg_type_number_tVar>()
            count.value = (sizeOf<mach_task_basic_info>() / 4).toUInt()
            task_info(mach_task_self_, MACH_TASK_BASIC_INFO.toUInt(), info.ptr.reinterpret(), count.ptr)
            info.resident_size.toLong() / (1024 * 1024)
        }

    private fun collectedResidentMb(): Long {
        GC.collect()
        return residentMb()
    }

    /** Every 16th read, collects and notes the resident size that is still in use. */
    private class Sampled(private val source: BackupSource, private val measure: () -> Long) : BackupSource {
        var peakMb = 0L
        val trail = mutableListOf<Long>()
        private var reads = 0
        override val size: Long get() = source.size

        override fun read(
            offset: Long,
            length: Int,
        ): ByteArray {
            if (++reads % 16 == 0) {
                measure().also {
                    trail += it
                    peakMb = maxOf(peakMb, it)
                }
            }
            return source.read(offset, length)
        }
    }

    /** Every 16th write, collects and notes the resident size: what the export retains while it writes. */
    private class SampledSink(private val sink: ClosableBackupSink, private val measure: () -> Long) : ClosableBackupSink {
        val trail = mutableListOf<Long>()
        private var writes = 0

        override fun write(bytes: ByteArray) {
            if (++writes % 16 == 0) trail += measure()
            sink.write(bytes)
        }

        override fun close() = sink.close()
    }

    @Test
    fun aFull250MbVaultRestoresFromAFileWithAFlatResidentSize() =
        runTest {
            val path = NSTemporaryDirectory().trimEnd('/') + "/vault-huge-" + NSUUID().UUIDString + ".pcda"
            try {
                val sink = SampledSink(FileBackupSink(path), ::collectedResidentMb)
                val beforeExport = collectedResidentMb()
                val pages =
                    try {
                        writeHugeArchive(sink)
                    } finally {
                        sink.close()
                    }
                val exportRetained = (sink.trail.maxOrNull() ?: 0L) - beforeExport
                val baseline = collectedResidentMb()
                val source = FileBackupSource(path)
                val sampled = Sampled(source, ::collectedResidentMb)
                val result =
                    try {
                        restoreHuge(sampled)
                    } finally {
                        source.close()
                    }
                val retained = sampled.peakMb - baseline

                println("PART2-VAULT-HUGE ios pages=$pages restoreMs=${result.restoreMs} baselineResidentMB=$baseline restoreRetainedResidentMB=$retained exportRetainedResidentMB=$exportRetained")
                assertEquals(0, result.skipped, "no page was left out: the vault fits under the ceiling")
                assertEquals(HUGE_PAGES, result.restoredPages)
                assertTrue(result.restoredBytes <= Part2Limits.VAULT_BYTES_MAX)
                assertTrue(exportRetained < 64, "the export kept $exportRetained MB resident while writing a vault of the same size")
                assertTrue(retained < 64, "the restore kept $retained MB resident for a ${Part2Limits.VAULT_BYTES_MAX / (1024 * 1024)} MB vault")
                assertTrue(result.restoreMs < 60_000, "restore took ${result.restoreMs} ms")
            } finally {
                NSFileManager.defaultManager.removeItemAtPath(path, null)
            }
        }
}
