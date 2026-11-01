package com.payslipmax.pdfparser.repository

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY: the backup passes through the app's cache as a file, so on the real files the promises hold: the export is written to the one
 * fixed name and is what gets shared, nothing else is left in the cache after a failure, a picked file is copied by stream (never read
 * whole into an array), the restore deletes it, and the sweep clears both names.
 */
class AndroidBackupFilesTest {
    private lateinit var dir: File
    private val shared = mutableListOf<File>()
    private lateinit var files: AndroidBackupFiles

    @Before
    fun make() {
        dir =
            File.createTempFile("cache", "").apply {
                delete()
                mkdirs()
            }
        files = AndroidBackupFiles(cacheDir = { dir }, share = { shared += it })
    }

    @After
    fun remove() {
        dir.deleteRecursively()
    }

    private fun names() = dir.list().orEmpty().sorted()

    @Test
    fun theExportIsWrittenToTheFixedNameAndThatFileIsWhatIsShared() =
        runTest {
            files.write {
                it.write(byteArrayOf(1, 2, 3))
                Result.success(Unit)
            }
            assertTrue(files.share().isSuccess)

            assertEquals(listOf("PayslipMax-backup.pcda"), names())
            assertEquals(listOf("PayslipMax-backup.pcda"), shared.map { it.name })
            assertContentEquals(byteArrayOf(1, 2, 3), shared.single().readBytes())
        }

    @Test
    fun aFailedExportOrShareLeavesNothingInTheCache() =
        runTest {
            files.write {
                it.write(byteArrayOf(1))
                Result.failure(IllegalStateException("x"))
            }
            assertEquals(emptyList(), names())

            files.write {
                it.write(byteArrayOf(1))
                Result.success(Unit)
            }
            val failing = AndroidBackupFiles(cacheDir = { dir }, share = { throw IOException("no sheet") })
            assertTrue(failing.share().isFailure)
            assertEquals(emptyList(), names())
        }

    @Test
    fun aPickedBackupIsCopiedByStreamThenReadAndDeletedByTheRestore() =
        runTest {
            val big = ByteArray(300_000) { (it % 251).toByte() }
            val path = copyPickedBackup(dir, ByteArrayInputStream(big))
            assertEquals(listOf("PayslipMax-restore.pcda"), names())

            val got = files.read(path) { Result.success(it.read(299_990, 10)) }

            assertContentEquals(big.copyOfRange(299_990, 300_000), got.getOrThrow())
            assertEquals(emptyList(), names(), "the restore deletes the picked copy")
        }

    @Test
    fun theCopyNeverAsksTheInputForItsWholeContentAtOnce() {
        val input =
            object : InputStream() {
                override fun read() = throw AssertionError("byte at a time")

                override fun read(
                    b: ByteArray,
                    off: Int,
                    len: Int,
                ): Int {
                    assertTrue(len <= 1024 * 1024, "a bounded buffer, never the whole file")
                    return -1
                }

                override fun readAllBytes(): ByteArray = throw AssertionError("whole file read")
            }

        copyPickedBackup(dir, input)
    }

    @Test
    fun aCopyThatFailsLeavesNoFileAndNoPath() {
        val broken =
            object : InputStream() {
                override fun read(): Int = throw IOException("secret/path")
            }

        val thrown = runCatching { copyPickedBackup(dir, broken) }.exceptionOrNull()

        assertIs<BackupFileException>(thrown)
        assertFalse(thrown.message.orEmpty().contains("secret"))
        assertEquals(emptyList(), names())
    }

    @Test
    fun aNewPickStartsTheRestoreFileAfresh() {
        copyPickedBackup(dir, ByteArrayInputStream(ByteArray(100)))
        val path = copyPickedBackup(dir, ByteArrayInputStream(ByteArray(7)))

        assertEquals(7L, File(path).length())
    }

    @Test
    fun theSweepClearsBothNamesAndNothingElse() {
        File(dir, "PayslipMax-backup.pcda").writeBytes(ByteArray(1))
        File(dir, "PayslipMax-restore.pcda").writeBytes(ByteArray(1))
        File(dir, "other.pdf").writeBytes(ByteArray(1))

        files.sweep()

        assertEquals(listOf("other.pdf"), names())
    }
}
