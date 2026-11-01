package com.payslipmax.pdfparser.part2.pages

import android.graphics.BitmapFactory
import android.graphics.Color
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.testing.JpegProbe
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a page in the vault is a photo of a RESTRICTED order. Whatever the scanner produced, what is stored must be one upright JPEG, no
 * larger than 1600 px on its long edge and 500 KB, with nothing about where, when or with what it was taken (DECISIONS Round 5 and 10).
 * These run the real platform codec on real JPEG bytes (Robolectric's native graphics), so they prove the file, not a stand-in for it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidPageEncoderTest {
    private val encoder = AndroidPageEncoder()

    @After
    fun unmock() = unmockkStatic(BitmapFactory::class)

    private fun encode(raw: ByteArray): ByteArray = assertNotNull(encoder.encode(raw))

    private fun sizeOf(jpeg: ByteArray) = assertNotNull(JpegProbe.size(jpeg))

    @Test
    fun aLargeLandscapePageIsShrunkToTheLongEdgeAndFitsTheCap() {
        val out = encode(PageBitmaps.jpeg(PageBitmaps.noisy(3000, 2000, 40)))

        assertEquals(1600 to 1067, sizeOf(out))
        assertTrue(out.size <= Part2Limits.PAGE_MAX_BYTES, "${out.size} bytes")
    }

    @Test
    fun aPortraitPageKeepsItsShape() {
        assertEquals(1067 to 1600, sizeOf(encode(PageBitmaps.jpeg(PageBitmaps.twoColour(2000, 3000)))))
    }

    @Test
    fun aSmallPageIsNotEnlarged() {
        assertEquals(800 to 600, sizeOf(encode(PageBitmaps.jpeg(PageBitmaps.twoColour(800, 600)))))
    }

    @Test
    fun aPngFromAScannerBecomesAJpeg() {
        val out = encode(PageBitmaps.png(PageBitmaps.twoColour(1000, 700)))

        assertEquals(listOf(0xFF, 0xD8), out.take(2).map { it.toInt() and 0xFF })
        assertEquals(1000 to 700, sizeOf(out))
    }

    @Test
    fun exifGpsAndEveryOtherSegmentAreGoneFromTheStoredBytes() {
        val tagged = JpegProbe.withMetadata(PageBitmaps.jpeg(PageBitmaps.twoColour(1200, 800)), orientation = 1, secret = "MARKER_GPS_TRACE")

        val out = encode(tagged)

        assertFalse(JpegProbe.contains(out, "MARKER_GPS_TRACE"))
        assertFalse(JpegProbe.contains(out, "Exif"))
        val markers = JpegProbe.markers(out)
        assertEquals(emptyList(), markers.filter { it in 0xE1..0xED || it == 0xEF || it == 0xFE }, "APP1 to APP13, APP15 and comments: $markers")
    }

    @Test
    fun theOutputIsStrippedEvenIfTheCodecWouldHaveWrittenMetadata() {
        // The encoder's last word is the byte-level cut, whatever the platform codec wrote: the output equals the cut of itself.
        val out = encode(PageBitmaps.jpeg(PageBitmaps.twoColour(900, 600)))

        assertTrue(out.contentEquals(JpegMetadata.strip(out)), "nothing left for the cut to remove")
    }

    @Test
    fun orientationIsAppliedBeforeItIsDroppedSoThePageIsUpright() {
        // Stored sideways with Exif orientation 6 ("rotate 90 degrees clockwise to display"): red (left) must end up on top.
        val sideways = JpegProbe.withMetadata(PageBitmaps.jpeg(PageBitmaps.twoColour(3000, 2000)), orientation = 6)

        val out = encode(sideways)

        assertEquals(1067 to 1600, sizeOf(out))
        val upright = PageBitmaps.decode(out)
        assertTrue(isRed(upright.getPixel(500, 100)), "top is red")
        assertTrue(isBlue(upright.getPixel(500, 1500)), "bottom is blue")
    }

    @Test
    fun everyExifOrientationEndsUpright() {
        // original: red left, blue right. Where red goes after the orientation's display transform: left, right, top or bottom.
        val expectedRedAt = mapOf(1 to "left", 2 to "right", 3 to "right", 4 to "left", 5 to "top", 6 to "top", 7 to "bottom", 8 to "bottom")
        for ((orientation, redAt) in expectedRedAt) {
            val out = encode(JpegProbe.withMetadata(PageBitmaps.jpeg(PageBitmaps.twoColour(400, 300)), orientation = orientation))
            val bitmap = PageBitmaps.decode(out)
            val (w, h) = bitmap.width to bitmap.height
            val got =
                when {
                    isRed(bitmap.getPixel(w / 4, h / 2)) && !isRed(bitmap.getPixel(3 * w / 4, h / 2)) -> "left"
                    isRed(bitmap.getPixel(3 * w / 4, h / 2)) && !isRed(bitmap.getPixel(w / 4, h / 2)) -> "right"
                    isRed(bitmap.getPixel(w / 2, h / 4)) && !isRed(bitmap.getPixel(w / 2, 3 * h / 4)) -> "top"
                    else -> "bottom"
                }
            assertEquals(redAt, got, "orientation $orientation")
            val swaps = orientation in 5..8
            assertEquals(if (swaps) 300 to 400 else 400 to 300, w to h, "orientation $orientation")
        }
    }

    @Test
    fun qualityIsSteppedDownUntilThePageFitsAndNoFurther() {
        val source = PageBitmaps.jpeg(PageBitmaps.noisy(1600, 1000, 60), 95)
        val scaled = PageBitmaps.decode(source)
        val oneStepDown = Part2Limits.PAGE_JPEG_QUALITY_START - Part2Limits.PAGE_JPEG_QUALITY_STEP

        // The size the encoder judges is the size after the metadata cut (the codec writes an ICC profile segment, which is cut).
        fun storedSize(quality: Int) = assertNotNull(JpegMetadata.strip(PageBitmaps.jpeg(scaled, quality))).size
        val atStart = storedSize(Part2Limits.PAGE_JPEG_QUALITY_START)
        val atOneStep = storedSize(oneStepDown)
        val atTwoSteps = storedSize(oneStepDown - Part2Limits.PAGE_JPEG_QUALITY_STEP)
        assertTrue(atTwoSteps < atOneStep && atOneStep < atStart, "the test image must get smaller with each step ($atTwoSteps, $atOneStep, $atStart)")

        val out = assertNotNull(AndroidPageEncoder(maxBytes = atOneStep, budgetBytes = atOneStep).encode(source))

        assertEquals(atOneStep, out.size, "exactly one step down: the first quality that fits is kept, neither the start nor anything lower")
    }

    @Test
    fun aPageThatDoesNotFitEvenAtTheFloorIsRefused() {
        val source = PageBitmaps.jpeg(PageBitmaps.noisy(1600, 1000, 255), 95)
        val atFloor = assertNotNull(JpegMetadata.strip(PageBitmaps.jpeg(PageBitmaps.decode(source), Part2Limits.PAGE_JPEG_QUALITY_FLOOR))).size

        assertNull(AndroidPageEncoder(maxBytes = atFloor - 1).encode(source))
        assertNotNull(AndroidPageEncoder(maxBytes = atFloor).encode(source), "exactly the floor's size fits")
    }

    @Test
    fun bytesThatAreNotAnImageAreRefused() {
        assertNull(encoder.encode(ByteArray(0)))
        assertNull(encoder.encode(ByteArray(5000) { (it * 31).toByte() }))
        assertNull(encoder.encode("not an image at all".encodeToByteArray()))
    }

    @Test
    fun aFileLargerThanTheReadCapIsRefusedWithoutBeingDecoded() {
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } throws AssertionError("must not decode an oversized file")
        every { BitmapFactory.decodeByteArray(any(), any(), any()) } throws AssertionError("must not decode an oversized file")

        assertNull(encoder.encode(ByteArray(Part2Limits.PAGE_RAW_MAX_BYTES + 1)))
    }

    @Test
    fun anImageWhoseDimensionsAreAbsurdIsRefusedFromItsHeaderAloneIncludingOneThatOverflowsAnInt() {
        val real = PageBitmaps.jpeg(PageBitmaps.twoColour(64, 64))
        val frame = (2 until real.size - 8).first { (real[it].toInt() and 0xFF) == 0xFF && (real[it + 1].toInt() and 0xFF) == 0xC0 }
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } answers {
            if (!arg<BitmapFactory.Options>(3).inJustDecodeBounds) throw AssertionError("must not decode the pixels of an oversized image")
            callOriginal()
        }
        // 20000 x 20000 = 400 MP; 50000 x 50000 = 2.5 billion pixels, which is negative in 32-bit arithmetic.
        for (side in listOf(20000, 50000, 65535)) {
            val bomb =
                real.copyOf().also {
                    listOf(frame + 5, frame + 7).forEach { at ->
                        it[at] = (side shr 8).toByte()
                        it[at + 1] = side.toByte()
                    }
                }

            assertNull(encoder.encode(bomb), "$side x $side")
        }
    }

    @Test
    fun aBigPhotoIsDecodedAtAFractionOfItsSizeNotInFull() {
        val source = PageBitmaps.jpeg(PageBitmaps.twoColour(6000, 4000), 80)
        val samples = mutableListOf<Int>()
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeByteArray(any(), any(), any(), any()) } answers {
            val options = arg<BitmapFactory.Options>(3)
            if (!options.inJustDecodeBounds) samples += options.inSampleSize
            callOriginal()
        }

        val out = encode(source)

        assertEquals(listOf(PageGeometry.sampleSize(6000, 4000, Part2Limits.PAGE_LONG_EDGE_PX)), samples)
        assertEquals(2, samples.single())
        assertEquals(1600 to 1067, sizeOf(out))
    }

    private fun isRed(pixel: Int) = Color.red(pixel) > 180 && Color.blue(pixel) < 80

    private fun isBlue(pixel: Int) = Color.blue(pixel) > 180 && Color.red(pixel) < 80
}
