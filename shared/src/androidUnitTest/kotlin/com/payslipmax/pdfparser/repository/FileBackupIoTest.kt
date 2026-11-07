package com.payslipmax.pdfparser.repository

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * WHY: a backup lives in a file the app made and must delete, never in one array: the sink appends in order and the source reads any
 * range, and both release the file when closed so it can be deleted. A range outside the file is refused, never read short.
 */
class FileBackupIoTest {
    private lateinit var file: File

    @Before
    fun make() {
        file = File.createTempFile("backup-io", ".pcda")
    }

    @After
    fun remove() {
        file.delete()
    }

    @Test
    fun whatIsWrittenInOrderIsReadBackByRange() {
        FileBackupSink(file).use { sink ->
            sink.write(byteArrayOf(1, 2, 3))
            sink.write(ByteArray(100_000) { 7 })
            sink.write(byteArrayOf(9))
        }

        FileBackupSource(file).use { source ->
            assertEquals(100_004L, source.size)
            assertContentEquals(byteArrayOf(1, 2, 3, 7), source.read(0, 4))
            assertContentEquals(byteArrayOf(7, 9), source.read(100_002, 2))
            assertContentEquals(ByteArray(0), source.read(100_004, 0))
        }
    }

    @Test
    fun aRangeOutsideTheFileIsRefusedNotReadShort() {
        file.writeBytes(ByteArray(10))

        FileBackupSource(file).use { source ->
            assertFailsWith<IllegalArgumentException> { source.read(8, 5) }
            assertFailsWith<IllegalArgumentException> { source.read(-1, 2) }
            assertFailsWith<IllegalArgumentException> { source.read(0, -1) }
        }
    }

    @Test
    fun aSinkStartsTheFileAfreshAndTheFileCanBeDeletedOnceBothAreClosed() {
        file.writeBytes(ByteArray(50) { 5 })
        FileBackupSink(file).use { it.write(byteArrayOf(1)) }
        assertEquals(1L, file.length(), "an old file is not appended to")

        FileBackupSource(file).close()
        assertTrue(file.delete(), "nothing holds the file open")
    }
}
