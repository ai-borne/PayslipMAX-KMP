package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a backup with scanned pages used to be one JSON string holding every page as Base64, copied several times on the way to
 * encryption (about 13 times the vault in memory). The archive is now the metadata document followed by one binary record per page, so
 * its size is the vault's size and a reader needs one page at a time. These tests pin the shape (a reader can find the document, an
 * old single-blob archive is never mistaken for it) and that a file that lies about its own lengths is refused before anything is read.
 */
class PageArchiveTest {
    private val meta = CryptoHelper.encrypt("{\"version\":5}".encodeToByteArray(), BACKUP_PASSWORD).getOrThrow()
    private val page = pageRow("p1", "o1", 0, SyntheticPart2Drafts.jpeg(3000))

    private fun built(vararg pages: RawPage): ByteArray {
        val writer = PageArchiveWriter(meta.size + pages.sumOf { it.sealed.size + 200 } + 400)
        writer.begin(meta)
        pages.forEach { writer.add(it.id, it.orderId, it.position, it.sealed) }
        return writer.finish(pages.size, BACKUP_PASSWORD)
    }

    @Test
    fun theDocumentOfAPageArchiveIsFoundByItsMagicAndIsExactlyWhatWasWritten() {
        assertContentEquals(meta, PageArchive.metaOf(built(page)))
    }

    @Test
    fun aFileWithEveryLengthRightButNotTheMagicIsNotAPageArchive() {
        val good = built(page)
        val wrongMagic = good.copyOf().also { it[0] = 'X'.code.toByte() }

        assertNotNull(PageArchive.metaOf(good))
        assertNull(PageArchive.metaOf(wrongMagic), "the magic is what keeps an old single-blob archive from being read as this format")
    }

    @Test
    fun anOldSingleBlobArchiveIsNeverMistakenForAPageArchive() {
        assertNull(PageArchive.metaOf(meta), "a v1-v4 archive (or a v5 one without pages) is the encrypted document itself")
        assertNull(PageArchive.metaOf(ByteArray(0)))
        assertNull(PageArchive.metaOf(ByteArray(7)))
    }

    @Test
    fun aFileThatLiesAboutItsLengthsIsNotAPageArchive() {
        val good = built(page)
        val lyingMeta = good.copyOf().also { it[4] = 0x7F } // a document length far past the end of the file
        val lyingTrailer = good.copyOf().also { it[it.size - 4] = 0x7F }
        val tiny = byteArrayOf('P'.code.toByte(), 'C'.code.toByte(), 'D'.code.toByte(), 'B'.code.toByte(), 0, 0, 0, 0)

        val overflowing = good.copyOf().also { bytes -> listOf(0x7F, 0xFF, 0xFF, 0xFF).forEachIndexed { i, b -> bytes[4 + i] = b.toByte() } } // Int.MAX_VALUE

        assertNull(PageArchive.metaOf(lyingMeta))
        assertNull(PageArchive.metaOf(overflowing), "8 + Int.MAX_VALUE must not wrap around to a small offset")
        assertNull(PageArchive.metaOf(lyingTrailer))
        assertNull(PageArchive.metaOf(tiny))
    }

    @Test
    fun theArchiveIsAsLargeAsTheSealedPagesPlusAFewHundredBytesNotAThirdLarger() {
        val pages = (1..5).map { pageRow("p$it", "o1", it, SyntheticPart2Drafts.jpeg(100_000, it)) }

        val archive = built(*pages.toTypedArray())

        val sealed = pages.sumOf { it.sealed.size }
        assertTrue(archive.size < sealed + meta.size + 1500, "binary records, no Base64 and no JSON around the pages: ${archive.size} vs $sealed")
    }

    @Test
    fun aTrailerWithTheWrongPasswordIsNotReadableSoTheCountCannotBeForged() {
        val good = built(page)
        val trailerLength = (0..3).fold(0) { n, i -> (n shl 8) or (good[good.size - 4 + i].toInt() and 0xFF) }
        val trailer = good.copyOfRange(good.size - 4 - trailerLength, good.size - 4)

        assertTrue(CryptoHelper.decrypt(trailer, "another password").isFailure)
        assertEquals("""{"pages":1}""", CryptoHelper.decrypt(trailer, BACKUP_PASSWORD).getOrThrow().decodeToString())
    }

    @Test
    fun theWriterRefusesARecordBeforeTheDocumentAndAnythingPastItsBuffer() {
        assertFailsWith<IllegalStateException> { PageArchiveWriter(1000).add("p1", "o1", 0, ByteArray(4)) }

        val small = PageArchiveWriter(meta.size + 40)
        small.begin(meta)
        assertFailsWith<IllegalStateException> { small.add("p1", "o1", 0, ByteArray(500)) }
    }
}
