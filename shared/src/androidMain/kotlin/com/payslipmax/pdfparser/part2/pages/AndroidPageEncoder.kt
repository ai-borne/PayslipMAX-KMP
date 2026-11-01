package com.payslipmax.pdfparser.part2.pages

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * Re-encodes a scanned page with the platform codec: decode at a fraction of the size when the photo is large, turn it upright by its Exif
 * orientation (the one piece of metadata that matters to the picture, applied before all of it is dropped), shrink to the long edge, then
 * the best JPEG quality that fits [maxBytes] (`JpegFit`). The codec writes no Exif but does write a colour-profile segment, and
 * `JpegMetadata` cuts every such segment before the size is judged, so the stored bytes carry no metadata. Nothing is logged: a refused
 * page is null.
 */
class AndroidPageEncoder(
    private val maxBytes: Int = Part2Limits.PAGE_MAX_BYTES,
    private val budgetBytes: Int = Part2Limits.PAGE_BUDGET_BYTES,
) : PageEncoder {
    override fun encode(raw: ByteArray): ByteArray? {
        if (raw.size > Part2Limits.PAGE_RAW_MAX_BYTES) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        // Not an image (or empty): the decoder reports no size. Too many pixels: refused from the header, before any pixel is decoded.
        if (PageGeometry.fit(bounds.outWidth, bounds.outHeight, Part2Limits.PAGE_LONG_EDGE_PX) == null) return null
        if (!PageGeometry.pixelsWithinLimit(bounds.outWidth, bounds.outHeight)) return null
        val options = BitmapFactory.Options().apply { inSampleSize = PageGeometry.sampleSize(bounds.outWidth, bounds.outHeight, Part2Limits.PAGE_LONG_EDGE_PX) }
        val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size, options) ?: return null
        val upright = upright(decoded, orientationOf(raw))
        val page = shrink(upright)
        return JpegFit.fitToBudget(budgetBytes, maxBytes, Part2Limits.PAGE_JPEG_QUALITY_START, Part2Limits.PAGE_JPEG_QUALITY_STEP, Part2Limits.PAGE_JPEG_QUALITY_FLOOR) { quality ->
            val out = ByteArrayOutputStream()
            page.compress(Bitmap.CompressFormat.JPEG, quality, out)
            // The cap is judged on what is stored, so the metadata cut comes first. A failed compress leaves nothing to cut, and a cut that
            // refuses gives null: both are "no fit".
            JpegMetadata.strip(out.toByteArray())
        }
    }

    private fun orientationOf(raw: ByteArray): Int = ExifInterface(ByteArrayInputStream(raw)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

    private fun upright(
        bitmap: Bitmap,
        orientation: Int,
    ): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply { postRotate(90f) }.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply { postRotate(-90f) }.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(-90f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun shrink(bitmap: Bitmap): Bitmap {
        val (width, height) = PageGeometry.fitPositive(bitmap.width, bitmap.height, Part2Limits.PAGE_LONG_EDGE_PX)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}

actual fun providePageEncoder(): PageEncoder = AndroidPageEncoder()
