package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.JpegProbe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * WHY: the same promise as on Android, kept with iOS's own codec (ImageIO): what is stored is one upright JPEG, at most 1600 px on the
 * long edge and 500 KB, with nothing about where, when or with what it was taken. Re-encoding runs on every scan on Kotlin/Native, so
 * the whole path also has a time bound.
 */
class IosPageEncoderTest {
    private val encoder = IosPageEncoder()

    private fun encode(raw: ByteArray): ByteArray = assertNotNull(encoder.encode(raw))

    private fun sizeOf(jpeg: ByteArray) = assertNotNull(JpegProbe.size(jpeg))

    @Test
    fun aLargeLandscapePageIsShrunkToTheLongEdgeAndFitsTheCap() {
        val out = encode(IosTestImages.jpeg(3000, 2000, noise = 3000))

        assertEquals(1600 to 1067, sizeOf(out))
        assertTrue(out.size <= Part2Limits.PAGE_MAX_BYTES, "${out.size} bytes")
    }

    @Test
    fun aPortraitPageKeepsItsShapeAndASmallOneIsNotEnlarged() {
        assertEquals(1067 to 1600, sizeOf(encode(IosTestImages.jpeg(2000, 3000))))
        assertEquals(800 to 600, sizeOf(encode(IosTestImages.jpeg(800, 600))))
    }

    @Test
    fun exifGpsAndEveryOtherSegmentAreGoneFromTheStoredBytes() {
        val tagged = JpegProbe.withMetadata(IosTestImages.jpeg(1200, 800), orientation = 1, secret = "MARKER_GPS_TRACE")

        val out = encode(tagged)

        assertFalse(JpegProbe.contains(out, "MARKER_GPS_TRACE"))
        assertFalse(JpegProbe.contains(out, "Exif"))
        assertEquals(emptyList(), JpegProbe.markers(out).filter { it in 0xE1..0xED || it == 0xEF || it == 0xFE }, "${JpegProbe.markers(out)}")
        assertContentEquals(out, JpegMetadata.strip(out), "nothing is left for the byte-level cut to remove")
    }

    @Test
    fun everyExifOrientationEndsUpright() {
        // original: red left, blue right. Where red goes after the orientation's display transform.
        val redAt = mapOf(1 to "left", 2 to "right", 3 to "right", 4 to "left", 5 to "top", 6 to "top", 7 to "bottom", 8 to "bottom")
        for ((orientation, expected) in redAt) {
            val out = encode(JpegProbe.withMetadata(IosTestImages.jpeg(400, 300), orientation = orientation))
            val (w, h) = sizeOf(out)
            val red = { x: Int, y: Int -> IosTestImages.isRed(IosTestImages.rgbAt(out, x, y)) }
            val got =
                when {
                    red(w / 4, h / 2) && !red(3 * w / 4, h / 2) -> "left"
                    red(3 * w / 4, h / 2) && !red(w / 4, h / 2) -> "right"
                    red(w / 2, h / 4) && !red(w / 2, 3 * h / 4) -> "top"
                    else -> "bottom"
                }
            assertEquals(expected, got, "orientation $orientation")
            assertEquals(if (orientation in 5..8) 300 to 400 else 400 to 300, w to h, "orientation $orientation")
        }
    }

    @Test
    fun qualityIsSteppedDownUntilThePageFitsTheCap() {
        val source = IosTestImages.jpeg(1600, 1000, noise = 6000, quality = 0.95)
        val atStart = assertNotNull(IosPageEncoder(maxBytes = Int.MAX_VALUE).encode(source)).size

        val out = assertNotNull(IosPageEncoder(maxBytes = atStart - 1).encode(source), "one byte under the start quality needs a lower quality, not a refusal")

        assertTrue(out.size <= atStart - 1, "${out.size} vs ${atStart - 1}")
        assertTrue(out.size > atStart * 7 / 10, "one step down, not a collapse to a low quality: the first quality that fits is kept (${out.size} vs $atStart)")
    }

    @Test
    fun aPageThatDoesNotFitEvenAtTheFloorIsRefused() {
        assertNull(IosPageEncoder(maxBytes = 200).encode(IosTestImages.jpeg(1600, 1000, noise = 6000)))
    }

    @Test
    fun bytesThatAreNotAnImageAreRefused() {
        assertNull(encoder.encode(ByteArray(0)))
        assertNull(encoder.encode(ByteArray(5000) { (it * 31).toByte() }))
        assertNull(encoder.encode("not an image at all".encodeToByteArray()))
    }

    @Test
    fun aRealImageLargerThanTheReadCapIsRefusedWithoutBeingDecoded() {
        val padded = IosTestImages.jpeg(400, 300) + ByteArray(Part2Limits.PAGE_RAW_MAX_BYTES) // trailing bytes after the end marker: still a decodable JPEG

        assertNotNull(encoder.encode(IosTestImages.jpeg(400, 300)), "the unpadded image is fine")
        assertNull(encoder.encode(padded), "only its size is wrong")
    }

    @Test
    fun anImageOverThePixelLimitIsRefusedEvenThoughItWouldDecode() {
        assertNull(encoder.encode(IosTestImages.jpeg(11_000, 10_000)), "110 megapixels is over the 100 MP limit")
    }

    @Test
    fun aWidePanoramaWithinThePixelLimitIsAccepted() {
        val out = encode(IosTestImages.jpeg(12_000, 2_000))

        assertEquals(1600 to 267, sizeOf(out), "judged by area (24 MP), not by width squared")
    }

    @Test
    fun anImageWhoseDimensionsAreAbsurdIsRefusedFromItsHeaderAlone() {
        val real = IosTestImages.jpeg(64, 64)
        val frame = (2 until real.size - 8).first { (real[it].toInt() and 0xFF) == 0xFF && (real[it + 1].toInt() and 0xFF) == 0xC0 }
        val bomb =
            real.copyOf().also { // 20000 x 20000 = 400 MP, over Part2Limits.PAGE_PIXELS_MAX
                it[frame + 5] = 0x4E
                it[frame + 6] = 0x20
                it[frame + 7] = 0x4E
                it[frame + 8] = 0x20
            }

        assertNull(encoder.encode(bomb))
    }

    @Test
    fun aScannedTaggedPhotoComesOutOfABackupAsASmallCleanPage() =
        runTest {
            val raw = listOf(JpegProbe.withMetadata(IosTestImages.jpeg(3000, 2000, noise = 2000), 1, "MARKER_GPS_TRACE"), JpegProbe.withMetadata(IosTestImages.jpeg(2000, 3000), 6, "MARKER_GPS_TRACE"))

            val trip = roundTrip(IosPageEncoder(), raw)

            val saved = trip.captured as CaptureResult.Saved
            assertEquals(2 to 0, saved.saved to saved.notSaved)
            for ((stored, restored) in trip.onDevice.zip(trip.afterRestore)) {
                assertContentEquals(stored, restored)
                assertFalse(JpegProbe.contains(restored, "MARKER_GPS_TRACE"))
                assertTrue(restored.size <= Part2Limits.PAGE_MAX_BYTES)
            }
            assertEquals(listOf(1600 to 1067, 1600 to 1067), trip.afterRestore.map { JpegProbe.size(it) }, "the second photo was stored sideways (orientation 6), so it is turned to landscape")
        }

    @Test
    fun encodingATwelveMegapixelPhotoIsWithinTheTimeBound() {
        val source = IosTestImages.jpeg(4000, 3000, noise = 4000)
        val start = TimeSource.Monotonic.markNow()

        encode(source)

        val elapsed = start.elapsedNow().inWholeMilliseconds
        assertTrue(elapsed < 1500, "re-encoding took $elapsed ms (budget 1500 ms in the simulator; 160 ms measured)")
    }
}
