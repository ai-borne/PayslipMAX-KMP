package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.testing.JpegProbe
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a photo of a RESTRICTED order must not carry where, when or with what it was taken. Whatever a camera, a scanner or a platform
 * encoder put in front of the picture (Exif with GPS, comments, IPTC, a thumbnail) is cut from the stored bytes, and the picture itself
 * is not touched. The cut is made on the bytes, so it holds on both platforms whatever their encoder writes.
 */
class JpegMetadataTest {
    private val plain = JpegProbe.minimal()

    private fun marker(code: Int) = JpegProbe.segment(code, byteArrayOf(1, 2, 3, 4))

    private fun inserted(vararg segments: ByteArray): ByteArray = plain.copyOfRange(0, 2) + segments.fold(ByteArray(0)) { all, s -> all + s } + plain.copyOfRange(2, plain.size)

    @Test
    fun exifGpsCommentsAndIptcAreGoneAndTheSecretTextWithThem() {
        val stripped = assertNotNull(JpegMetadata.strip(JpegProbe.withMetadata(plain, orientation = 6, secret = "MARKER_SECRET")))

        assertFalse(JpegProbe.contains(stripped, "MARKER_SECRET"))
        assertFalse(JpegProbe.contains(stripped, "Exif"))
        assertEquals(listOf(0xE0, 0xC0, 0xDA), JpegProbe.markers(stripped))
    }

    @Test
    fun everyApplicationSegmentFromApp1ToApp13AndApp15AndEveryCommentIsDropped() {
        for (code in (0xE1..0xED) + 0xEF + 0xFE) {
            val stripped = assertNotNull(JpegMetadata.strip(inserted(marker(code))), "marker ${code.toString(16)}")
            assertEquals(listOf(0xE0, 0xC0, 0xDA), JpegProbe.markers(stripped), "marker ${code.toString(16)} must be dropped")
        }
    }

    @Test
    fun theSegmentsThatMakeThePictureReadableStay() {
        val withTables = inserted(marker(0xDB), marker(0xC4), marker(0xEE), marker(0xDD))

        assertEquals(listOf(0xDB, 0xC4, 0xEE, 0xDD, 0xE0, 0xC0, 0xDA), JpegProbe.markers(assertNotNull(JpegMetadata.strip(withTables))), "quantisation, Huffman, Adobe (APP14) and restart interval stay")
    }

    @Test
    fun theImageDataAndTheFrameAreByteForByteTheSame() {
        val entropy = byteArrayOf(0x01, 0xFF.toByte(), 0x00, 0x7F, 0xFF.toByte(), 0xD0.toByte(), 0x33, 0xFF.toByte(), 0x00, 0xFF.toByte())
        val image = JpegProbe.minimal(entropy = entropy)

        val stripped = assertNotNull(JpegMetadata.strip(JpegProbe.withMetadata(image)))

        assertContentEquals(image, stripped, "stuffed bytes (FF 00), fill bytes (FF FF) and restart markers (FF D0) inside the image data are data, not segments")
    }

    @Test
    fun aPictureWithNothingToCutComesBackIdentical() {
        assertContentEquals(plain, JpegMetadata.strip(plain))
    }

    @Test
    fun aMultiScanPictureLosesMetadataBetweenItsScansToo() {
        val firstScan = JpegProbe.segment(0xDA, byteArrayOf(1, 1, 0, 0, 63, 0)) + byteArrayOf(0x11, 0x22)
        val progressive =
            byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + JpegProbe.segment(0xC2, byteArrayOf(8, 0, 8, 0, 8, 1, 1, 0x11, 0)) + firstScan +
                JpegProbe.segment(0xE1, "MARKER_SECRET".encodeToByteArray()) + firstScan + byteArrayOf(0xFF.toByte(), 0xD9.toByte())

        val stripped = assertNotNull(JpegMetadata.strip(progressive))

        assertFalse(JpegProbe.contains(stripped, "MARKER_SECRET"))
        assertEquals(progressive.size - 4 - "MARKER_SECRET".length, stripped.size)
        assertTrue(stripped.last() == 0xD9.toByte())
    }

    @Test
    fun whatIsNotAJpegIsRefused() {
        assertNull(JpegMetadata.strip(ByteArray(0)))
        assertNull(JpegMetadata.strip(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0, 0, 0, 0)))
        assertNull(JpegMetadata.strip(byteArrayOf(0xFF.toByte(), 0xD8.toByte())), "a start marker alone has no picture")
    }

    @Test
    fun aSegmentThatRunsPastTheEndOfTheFileIsRefusedNotRead() {
        val truncated = plain.copyOfRange(0, 2) + byteArrayOf(0xFF.toByte(), 0xE1.toByte(), 0xFF.toByte(), 0xFF.toByte(), 1, 2, 3)

        assertNull(JpegMetadata.strip(truncated))
    }

    @Test
    fun aSegmentWhoseLengthIsLessThanItsOwnLengthFieldIsRefused() {
        assertNull(JpegMetadata.strip(plain.copyOfRange(0, 2) + byteArrayOf(0xFF.toByte(), 0xE1.toByte(), 0, 1) + plain.copyOfRange(2, plain.size)))
    }

    @Test
    fun garbageBetweenSegmentsIsRefused() {
        assertNull(JpegMetadata.strip(plain.copyOfRange(0, 2) + byteArrayOf(0x00, 0x00, 0x00, 0x00) + plain.copyOfRange(2, plain.size)))
    }

    @Test
    fun aFileThatEndsInsideTheImageDataStillComesBackWithoutMetadata() {
        val cut = JpegProbe.withMetadata(plain).let { it.copyOfRange(0, it.size - 2) }

        val stripped = assertNotNull(JpegMetadata.strip(cut))

        assertFalse(JpegProbe.contains(stripped, "MARKER_SECRET"))
    }

    @Test
    fun whatFollowsTheEndMarkerIsCutBecauseVendorsHideMetadataThere() {
        val withTrailer = plain + "MARKER_VENDOR_TRAILER".encodeToByteArray()

        val stripped = assertNotNull(JpegMetadata.strip(withTrailer))

        assertContentEquals(plain, stripped, "bytes after the end of the picture (Samsung and Google trailers carry a second Exif) are not part of it")
    }

    @Test
    fun aSegmentHeaderCutInHalfIsRefusedNotReadPastTheEnd() {
        val start = byteArrayOf(0xFF.toByte(), 0xD8.toByte())

        assertNull(JpegMetadata.strip(start + byteArrayOf(0xFF.toByte(), 0xE1.toByte(), 0x00)), "marker and half a length")
        assertNull(JpegMetadata.strip(start + byteArrayOf(0xFF.toByte(), 0xE1.toByte())), "marker and no length")
        assertNull(JpegMetadata.strip(start + byteArrayOf(0xFF.toByte())), "half a marker")
    }

    @Test
    fun aFileCutRightAfterAStuffingByteInTheImageDataStillComesBackWithoutMetadata() {
        val cut = JpegProbe.withMetadata(JpegProbe.minimal(entropy = byteArrayOf(0x12, 0xFF.toByte()))).let { it.copyOfRange(0, it.size - 2) }

        val stripped = assertNotNull(JpegMetadata.strip(cut))

        assertFalse(JpegProbe.contains(stripped, "MARKER_SECRET"))
        assertEquals(0xFF.toByte(), stripped.last(), "the lone FF at the cut is image data and is kept")
    }

    @Test
    fun aFileThatStartsWithFFButNotWithTheStartMarkerIsRefused() {
        assertNull(JpegMetadata.strip(byteArrayOf(0xFF.toByte(), 0xE0.toByte()) + plain.copyOfRange(2, plain.size)))
    }

    @Test
    fun aFileWhoseFirstByteIsNotFFIsRefusedWhateverFollows() {
        assertNull(JpegMetadata.strip(byteArrayOf(0x00, 0xD8.toByte()) + plain.copyOfRange(2, plain.size)))
        assertNull(JpegMetadata.strip(byteArrayOf(0xFF.toByte())), "one byte")
    }

    @Test
    fun fillBytesBeforeAMarkerAreAllowedAndKept() {
        val filled = plain.copyOfRange(0, 2) + byteArrayOf(0xFF.toByte(), 0xFF.toByte()) + plain.copyOfRange(2, plain.size)

        assertContentEquals(filled, JpegMetadata.strip(filled))
    }

    @Test
    fun standaloneMarkersBetweenSegmentsAreAllowedAndKept() {
        for (code in listOf(0x01) + (0xD0..0xD7).toList()) {
            val withMarker = plain.copyOfRange(0, 2) + byteArrayOf(0xFF.toByte(), code.toByte()) + plain.copyOfRange(2, plain.size)

            assertContentEquals(withMarker, JpegMetadata.strip(withMarker), "marker ${code.toString(16)}")
        }
    }

    @Test
    fun everyRestartMarkerInsideTheImageDataIsData() {
        val entropy = ByteArray(0) + (0xD0..0xD7).flatMap { listOf(0x10, 0xFF.toByte(), it.toByte(), 0x20) }.toByteArray()
        val image = JpegProbe.minimal(entropy = entropy)

        assertContentEquals(image, JpegMetadata.strip(JpegProbe.withMetadata(image)))
    }

    @Test
    fun aScanHeaderWithAnImpossibleLengthIsRefused() {
        val head = plain.copyOfRange(0, 2) + JpegProbe.segment(0xE0, "JFIF".encodeToByteArray() + byteArrayOf(0))
        for (length in listOf(0, 1)) {
            val badScan = head + byteArrayOf(0xFF.toByte(), 0xDA.toByte(), 0x00, length.toByte(), 0x11, 0x22, 0xFF.toByte(), 0xD9.toByte())

            assertNull(JpegMetadata.strip(badScan), "scan length $length")
        }
    }

    @Test
    fun aScanHeaderThatClaimsToRunPastTheEndIsRefused() {
        val head = plain.copyOfRange(0, 2) + JpegProbe.segment(0xE0, "JFIF".encodeToByteArray() + byteArrayOf(0))

        assertNull(JpegMetadata.strip(head + byteArrayOf(0xFF.toByte(), 0xDA.toByte(), 0x00, 0x40, 0x11, 0x22)))
    }

    @Test
    fun aFileWithHeadersButNoImageDataIsRefused() {
        val head = plain.copyOfRange(0, 2) + JpegProbe.segment(0xE0, "JFIF".encodeToByteArray() + byteArrayOf(0))

        assertNull(JpegMetadata.strip(head), "headers only")
        assertNull(JpegMetadata.strip(head + byteArrayOf(0xFF.toByte(), 0xD9.toByte())), "headers and an end marker")
    }

    @Test
    fun aStrayByteInPlaceOfAMarkersFFIsRefusedEvenIfWhatFollowsLooksLikeASegment() {
        val first = (2 until plain.size).first { plain[it] == 0xFF.toByte() }
        val broken = plain.copyOf().also { it[first] = 0x00 }

        assertNull(JpegMetadata.strip(broken))
    }

    @Test
    fun aFileThatEndsOnALoneFFBetweenSegmentsIsRefusedNotReadPastTheEnd() {
        val head = plain.copyOfRange(0, 2) + JpegProbe.segment(0xE0, "JFIF".encodeToByteArray() + byteArrayOf(0))

        assertNull(JpegMetadata.strip(head + byteArrayOf(0xFF.toByte())))
    }
}
