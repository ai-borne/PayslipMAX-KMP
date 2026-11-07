package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.writeToFile
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * WHY: the same promises as the Android file tests, on Kotlin/Native's file calls: a range outside the file is refused, a sink starts
 * the file afresh, a closed file can be deleted, the export is shared from the one fixed name and deleted when the sheet is done or
 * fails, a picked copy is deleted after the restore, and a large backup passes through in bounded memory in a sane time.
 */
@OptIn(ExperimentalForeignApi::class)
class IosBackupFilesTest {
    private val dir = NSTemporaryDirectory().trimEnd('/') + "/backup-" + NSUUID().UUIDString
    private val manager = NSFileManager.defaultManager

    private fun fresh(): IosBackupFiles {
        manager.createDirectoryAtPath(dir, true, null, null)
        return IosBackupFiles(dir, share = { _, _ -> })
    }

    private fun names() = (manager.contentsOfDirectoryAtPath(dir, null) as List<*>?).orEmpty().map { it as String }.sorted()

    private fun cleanup() {
        manager.removeItemAtPath(dir, null)
    }

    @Test
    fun whatIsWrittenInOrderIsReadBackByRangeAndARangeOutsideIsRefused() {
        val files = fresh()
        val path = "$dir/a.pcda"
        FileBackupSink(path).also {
            it.write(byteArrayOf(1, 2, 3))
            it.write(ByteArray(100_000) { 7 })
            it.write(byteArrayOf(9))
            it.close()
        }

        val source = FileBackupSource(path)
        assertEquals(100_004L, source.size)
        assertContentEquals(byteArrayOf(1, 2, 3, 7), source.read(0, 4))
        assertContentEquals(byteArrayOf(7, 9), source.read(100_002, 2))
        assertContentEquals(ByteArray(0), source.read(100_004, 0))
        assertFailsWith<IllegalArgumentException> { source.read(8, 100_000) }
        assertFailsWith<IllegalArgumentException> { source.read(-1, 2) }
        source.close()
        files.discard(path)
        cleanup()
    }

    @Test
    fun aSinkStartsTheFileAfreshAndAClosedFileCanBeDeleted() {
        fresh()
        val path = "$dir/b.pcda"
        byteArrayOf(5, 5, 5, 5, 5).toNSData().writeToFile(path, true)

        FileBackupSink(path).also {
            it.write(byteArrayOf(1))
            it.close()
        }
        val source = FileBackupSource(path)
        assertEquals(1L, source.size, "an old file is not appended to")
        source.close()

        assertTrue(manager.removeItemAtPath(path, null))
        cleanup()
    }

    @Test
    fun aMissingFileIsReportedWithoutItsPath() {
        fresh()
        val thrown = runCatching { FileBackupSource("$dir/none.pcda") }.exceptionOrNull()

        assertIs<BackupFileException>(thrown)
        assertFalse(thrown.message.orEmpty().contains(dir))
        cleanup()
    }

    @Test
    fun theExportIsSharedFromTheFixedNameAndDeletedWhenTheSheetIsDone() =
        runTest {
            manager.createDirectoryAtPath(dir, true, null, null)
            var sharedPath: String? = null
            var done: (() -> Unit)? = null
            val files =
                IosBackupFiles(dir, share = { path, onDone ->
                    sharedPath = path
                    done = onDone
                })

            var readBack: List<Byte>? = null
            files.write(verify = { source -> Result.success(Unit).also { readBack = source.read(0, source.size.toInt()).toList() } }) {
                it.write(byteArrayOf(1, 2))
                Result.success(Unit)
            }
            assertEquals(listOf<Byte>(1, 2), readBack, "the closed file on disk is what is read back before it can be shared")
            assertTrue(files.share().isSuccess)

            assertEquals("$dir/PayslipMax-backup.pcda", sharedPath)
            assertEquals(listOf("PayslipMax-backup.pcda"), names())
            done!!.invoke()
            assertEquals(emptyList(), names(), "deleted once the sheet closes")
            cleanup()
        }

    @Test
    fun aFailedExportOrShareLeavesNothing() =
        runTest {
            manager.createDirectoryAtPath(dir, true, null, null)
            val files = IosBackupFiles(dir, share = { _, _ -> error("no sheet") })

            files.write(readsBack) {
                it.write(byteArrayOf(1))
                Result.failure(IllegalStateException("x"))
            }
            assertEquals(emptyList(), names())

            files.write(readsBack) {
                it.write(byteArrayOf(1))
                Result.success(Unit)
            }
            assertTrue(files.share().isFailure)
            assertEquals(emptyList(), names())
            cleanup()
        }

    @Test
    fun aPickedCopyIsReadByRangeAndDeletedByTheRestore() =
        runTest {
            val files = fresh()
            val picked = "$dir/picked.pcda"
            byteArrayOf(4, 5, 6).toNSData().writeToFile(picked, true)

            val got = files.read(picked) { Result.success(it.read(1, 2).toList()) }

            assertEquals(listOf<Byte>(5, 6), got.getOrNull())
            assertEquals(emptyList(), names())
            cleanup()
        }

    @Test
    fun aLargeBackupStreamsThroughInBoundedMemoryAndInSaneTime() {
        fresh()
        val path = "$dir/big.pcda"
        val chunk = ByteArray(256 * 1024) { (it % 251).toByte() }
        val chunks = 160 // 40 MB through a 256 KB window; the whole file is never one array
        val best =
            (1..3).minOf {
                val start = TimeSource.Monotonic.markNow()
                val sink = FileBackupSink(path)
                repeat(chunks) { sink.write(chunk) }
                sink.close()
                val source = FileBackupSource(path)
                var offset = 0L
                while (offset < source.size) {
                    val n = minOf(chunk.size.toLong(), source.size - offset).toInt()
                    assertEquals(chunk[n - 1], source.read(offset, n)[n - 1])
                    offset += n
                }
                source.close()
                start.elapsedNow().inWholeMilliseconds
            }
        println("IosBackupFilesTest: 40 MB written and read in $best ms (best of three)")

        assertTrue(best < 5_000, "40 MB took $best ms")
        cleanup()
    }
}
