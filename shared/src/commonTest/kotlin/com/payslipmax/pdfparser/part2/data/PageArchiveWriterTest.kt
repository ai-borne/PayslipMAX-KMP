package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.crypto.PasswordKey
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.BackupSink
import com.payslipmax.pdfparser.repository.MemoryBackupSink
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * WHY: a backup of any size must reach the file a piece at a time, so nothing the size of the vault is ever one array, and the bytes must
 * be exactly the layout every earlier v5 backup has (the file format did not change): "PCDB", the length of the document, the document,
 * a record per page, the encrypted page count, its length.
 */
class PageArchiveWriterTest {
    private val meta = ByteArray(40) { (it + 1).toByte() }
    private val sealedOne = ByteArray(300) { 1 }
    private val sealedTwo = ByteArray(500) { 2 }

    private fun written(sink: BackupSink = MemoryBackupSink()): Pair<MemoryBackupSink, PageArchiveWriter> {
        val memory = sink as MemoryBackupSink
        return memory to PageArchiveWriter(memory)
    }

    @Test
    fun theBytesAreTheDocumentedLayoutAndNothingElse() {
        val (sink, stream) = written()
        stream.begin(meta)
        stream.add("p1", "o1", 0, sealedOne)
        stream.add("p2", "o1", 1, sealedTwo)
        stream.finish(2, PasswordKey(BACKUP_PASSWORD))
        val bytes = sink.toByteArray()

        assertEquals("PCDB", bytes.copyOf(4).decodeToString())
        assertEquals(meta.size, PageArchive.u32(bytes, 4))
        assertContentEquals(meta, bytes.copyOfRange(8, 8 + meta.size))
        var at = 8 + meta.size
        assertEquals(2, bytes[at].toInt())
        assertEquals("p1", bytes.decodeToString(at + 1, at + 3))
        assertEquals(2, bytes[at + 3].toInt())
        assertEquals("o1", bytes.decodeToString(at + 4, at + 6))
        assertEquals(0, PageArchive.u32(bytes, at + 6))
        assertEquals(sealedOne.size, PageArchive.u32(bytes, at + 10))
        assertContentEquals(sealedOne, bytes.copyOfRange(at + 14, at + 14 + sealedOne.size))
        at += 14 + sealedOne.size + 14 + sealedTwo.size
        val trailerLength = PageArchive.u32(bytes, bytes.size - 4)!!
        assertEquals(bytes.size - 4 - trailerLength, at, "the trailer follows the last record and nothing else is there")
        val layout = PageArchive.layout(bytes)!!
        assertEquals(at.toLong(), layout.trailerStart)
        assertTrue(CryptoHelper.decrypt(bytes.copyOfRange(layout.trailerStart.toInt(), layout.trailerStart.toInt() + trailerLength), BACKUP_PASSWORD).isSuccess)
    }

    @Test
    fun noWriteIsBiggerThanOnePageRecordSoNothingTheSizeOfTheVaultIsEverOneArray() {
        val widest = mutableListOf<Int>()
        val sink = BackupSink { widest += it.size }
        val stream = PageArchiveWriter(sink)
        stream.begin(meta)
        repeat(50) { stream.add("p$it", "o1", it, ByteArray(100_000)) }
        stream.finish(50, PasswordKey(BACKUP_PASSWORD))

        assertTrue(widest.max() <= 100_000 + 14 + 10, "largest single write ${widest.max()}")
        assertTrue(widest.size >= 50, "the pages arrive one at a time")
    }

    @Test
    fun theDocumentMustComeFirstAndOnlyOnce() {
        val (_, stream) = written()
        assertFailsWith<IllegalStateException> { stream.add("p1", "o1", 0, sealedOne) }
        assertFailsWith<IllegalStateException> { stream.finish(0, PasswordKey(BACKUP_PASSWORD)) }
        stream.begin(meta)
        assertFailsWith<IllegalStateException> { stream.begin(meta) }
    }

    @Test
    fun anIdThatIsNotAPlainTokenIsRefusedBeforeAnythingIsWritten() {
        val (sink, stream) = written()
        stream.begin(meta)
        val before = sink.toByteArray().size

        assertFailsWith<IllegalArgumentException> { stream.add("x".repeat(256), "o1", 0, sealedOne) }
        assertFailsWith<IllegalArgumentException> { stream.add("p1", "", 0, sealedOne) }
        assertEquals(before, sink.toByteArray().size, "a refused record writes nothing")
    }
}
