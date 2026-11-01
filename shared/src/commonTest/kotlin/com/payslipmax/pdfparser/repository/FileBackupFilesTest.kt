package com.payslipmax.pdfparser.repository

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** A disk in a map: records what is open, so a test can say "closed before deleted" without a real file. */
private class MapBackupFiles : FileBackupFiles() {
    val disk = mutableMapOf<String, ByteArray>()
    val open = mutableSetOf<String>()
    val presented = mutableListOf<String>()
    var presentFails = false
    var deleteWhileOpen = false

    override val exportPath = "export.pcda"

    override fun leftovers() = listOf(exportPath, "restore.pcda")

    override fun openSink(path: String): ClosableBackupSink {
        disk[path] = ByteArray(0)
        open += path
        return object : ClosableBackupSink {
            override fun write(bytes: ByteArray) {
                disk[path] = disk.getValue(path) + bytes
            }

            override fun close() {
                open -= path
            }
        }
    }

    override fun openSource(path: String): ClosableBackupSource {
        val bytes = disk[path] ?: throw IllegalStateException("secret/path/$path")
        open += path
        return object : ClosableBackupSource, BackupSource by MemoryBackupSource(bytes) {
            override fun close() {
                open -= path
            }
        }
    }

    override fun delete(path: String) {
        if (path in open) deleteWhileOpen = true
        disk.remove(path)
    }

    override fun present(path: String) {
        check(!presentFails) { "secret/path/$path" }
        presented += path
    }
}

/**
 * WHY: a backup file is a copy of everything the officer owns, so it must never outlive its use. The export file is deleted the moment a
 * write or a share fails, a picked file is deleted after the restore whatever its result, a file is closed before it is deleted, and a
 * failure never carries a path.
 */
class FileBackupFilesTest {
    @Test
    fun aSuccessfulWriteLeavesTheClosedFileForTheShareSheet() =
        runTest {
            val files = MapBackupFiles()

            val result =
                files.write {
                    it.write(byteArrayOf(1, 2, 3))
                    Result.success(Unit)
                }

            assertTrue(result.isSuccess)
            assertContentEquals(byteArrayOf(1, 2, 3), files.disk["export.pcda"])
            assertTrue(files.open.isEmpty(), "the file is closed so the share sheet can read it")
        }

    @Test
    fun aFailedWriteDeletesTheHalfWrittenFile() =
        runTest {
            val files = MapBackupFiles()

            val result =
                files.write {
                    it.write(byteArrayOf(1))
                    Result.failure(IllegalStateException("wrong password"))
                }

            assertTrue(result.isFailure)
            assertEquals("wrong password", result.exceptionOrNull()?.message, "a failure the service reports is passed on")
            assertNull(files.disk["export.pcda"])
            assertFalse(files.deleteWhileOpen, "the file is closed before it is deleted")
        }

    @Test
    fun aWriteThatThrowsDeletesTheFileAndReportsOnlyAFixedMessage() =
        runTest {
            val files = MapBackupFiles()

            val result = files.write { throw IllegalStateException("secret/path/export.pcda") }

            assertIs<BackupFileException>(result.exceptionOrNull())
            assertFalse(result.exceptionOrNull()?.message.orEmpty().contains("secret"), "no path in a message")
            assertNull(files.disk["export.pcda"])
        }

    @Test
    fun aWriteFirstSweepsWhatEarlierRunsLeft() =
        runTest {
            val files = MapBackupFiles()
            files.disk["restore.pcda"] = byteArrayOf(9)

            files.write { Result.success(Unit) }

            assertNull(files.disk["restore.pcda"])
        }

    @Test
    fun aShareKeepsTheFileForTheTargetAndAFailedShareDeletesIt() =
        runTest {
            val files = MapBackupFiles()
            files.write {
                it.write(byteArrayOf(1))
                Result.success(Unit)
            }

            assertTrue(files.share().isSuccess)
            assertEquals(listOf("export.pcda"), files.presented)
            assertTrue("export.pcda" in files.disk, "the target app reads it after the sheet opens")

            files.presentFails = true
            val failed = files.share()

            assertIs<BackupFileException>(failed.exceptionOrNull())
            assertFalse(failed.exceptionOrNull()?.message.orEmpty().contains("secret"))
            assertNull(files.disk["export.pcda"])
        }

    @Test
    fun aPickedFileIsReadByRangeAndDeletedAfterwardsWhateverTheResult() =
        runTest {
            val files = MapBackupFiles()
            files.disk["p1"] = byteArrayOf(5, 6, 7)

            val ok = files.read("p1") { Result.success(it.read(1, 2).toList()) }

            assertEquals(listOf<Byte>(6, 7), ok.getOrNull())
            assertNull(files.disk["p1"])
            assertFalse(files.deleteWhileOpen, "closed before deleted")

            files.disk["p2"] = byteArrayOf(1)
            val failed = files.read<Unit>("p2") { Result.failure(IllegalStateException("wrong password")) }
            assertEquals("wrong password", failed.exceptionOrNull()?.message)
            assertNull(files.disk["p2"])

            files.disk["p3"] = byteArrayOf(1)
            val thrown = files.read<Unit>("p3") { throw IllegalStateException("secret/path") }
            assertIs<BackupFileException>(thrown.exceptionOrNull())
            assertNull(files.disk["p3"])
        }

    @Test
    fun aPickedFileThatCannotBeOpenedIsStillDeletedAndReportedWithoutItsPath() =
        runTest {
            val files = MapBackupFiles()

            val result = files.read<Unit>("missing") { Result.success(Unit) }

            assertIs<BackupFileException>(result.exceptionOrNull())
            assertFalse(result.exceptionOrNull()?.message.orEmpty().contains("secret"))
        }

    @Test
    fun discardAndSweepRemoveFilesAndNeverThrow() {
        val files = MapBackupFiles()
        files.disk["p1"] = byteArrayOf(1)
        files.disk["export.pcda"] = byteArrayOf(2)
        files.disk["restore.pcda"] = byteArrayOf(3)

        files.discard("p1")
        files.sweep()

        assertTrue(files.disk.isEmpty())
    }
}
