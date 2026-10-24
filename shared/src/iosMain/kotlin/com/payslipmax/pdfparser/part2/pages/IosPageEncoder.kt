@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.crypto.toCFData
import com.payslipmax.pdfparser.crypto.toCFString
import com.payslipmax.pdfparser.crypto.toNSData
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import platform.CoreFoundation.CFDataCreateMutable
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFStringRef
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGImageRelease
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.ImageIO.CGImageDestinationAddImage
import platform.ImageIO.CGImageDestinationCreateWithData
import platform.ImageIO.CGImageDestinationFinalize
import platform.ImageIO.CGImageSourceCopyPropertiesAtIndex
import platform.ImageIO.CGImageSourceCreateThumbnailAtIndex
import platform.ImageIO.CGImageSourceCreateWithData
import platform.ImageIO.CGImageSourceRef
import platform.ImageIO.kCGImageDestinationLossyCompressionQuality
import platform.ImageIO.kCGImagePropertyPixelHeight
import platform.ImageIO.kCGImagePropertyPixelWidth
import platform.ImageIO.kCGImageSourceCreateThumbnailFromImageAlways
import platform.ImageIO.kCGImageSourceCreateThumbnailWithTransform
import platform.ImageIO.kCGImageSourceShouldCacheImmediately
import platform.ImageIO.kCGImageSourceThumbnailMaxPixelSize

/**
 * Re-encodes a scanned page with ImageIO: the image is read at the target size straight from the file (a large photo is never decoded in
 * full), turned upright by its Exif orientation while being read, then written as the best JPEG quality that fits [maxBytes]
 * (`JpegFit`). A decoded image carries no metadata and the encoder is given none, and `JpegMetadata` cuts anything it wrote anyway. Runs
 * off the main thread (the caller's dispatcher). Nothing is logged: a refused page is null.
 */
class IosPageEncoder(
    private val maxBytes: Int = Part2Limits.PAGE_MAX_BYTES,
) : PageEncoder {
    override fun encode(raw: ByteArray): ByteArray? {
        if (raw.size > Part2Limits.PAGE_RAW_MAX_BYTES) return null
        val data = raw.toNSData().toCFData() ?: return null
        try {
            val source = CGImageSourceCreateWithData(data, null) ?: return null
            try {
                val (width, height) = pixelSize(source) ?: return null
                // Too many pixels: refused from the header, before any pixel is decoded (not an image: there is no size to read, above).
                if (!PageGeometry.pixelsWithinLimit(width, height)) return null
                // ImageIO never enlarges: a thumbnail "may be as big as the image itself".
                val image = thumbnail(source, Part2Limits.PAGE_LONG_EDGE_PX) ?: return null
                try {
                    return JpegFit.fit(maxBytes, Part2Limits.PAGE_JPEG_QUALITY_START, Part2Limits.PAGE_JPEG_QUALITY_STEP, Part2Limits.PAGE_JPEG_QUALITY_FLOOR) { quality ->
                        // The cap is judged on what is stored, so the metadata cut comes first (a cut that refuses gives null, which is no fit).
                        JpegMetadata.strip(jpeg(image, quality))
                    }
                } finally {
                    CGImageRelease(image)
                }
            } finally {
                CFRelease(source)
            }
        } finally {
            CFRelease(data)
        }
    }

    private fun pixelSize(source: CGImageSourceRef): Pair<Int, Int>? {
        val properties = CGImageSourceCopyPropertiesAtIndex(source, 0u, null) ?: return null

        @Suppress("UNCHECKED_CAST")
        val map = CFBridgingRelease(properties) as? Map<Any?, *> ?: return null
        val width = (map[key(kCGImagePropertyPixelWidth)] as? Number)?.toInt() ?: return null
        val height = (map[key(kCGImagePropertyPixelHeight)] as? Number)?.toInt() ?: return null
        return width to height
    }

    private fun thumbnail(
        source: CGImageSourceRef,
        longEdge: Int,
    ): CGImageRef? =
        withOptions(
            mapOf(
                key(kCGImageSourceCreateThumbnailFromImageAlways) to true,
                key(kCGImageSourceCreateThumbnailWithTransform) to true,
                key(kCGImageSourceShouldCacheImmediately) to true,
                key(kCGImageSourceThumbnailMaxPixelSize) to longEdge,
            ),
        ) { options -> CGImageSourceCreateThumbnailAtIndex(source, 0u, options) }

    private fun jpeg(
        image: CGImageRef,
        quality: Int,
    ): ByteArray {
        val out = CFDataCreateMutable(null, 0) ?: return ByteArray(0)
        val type = "public.jpeg".toCFString()
        try {
            val destination = CGImageDestinationCreateWithData(out, type, 1u, null) ?: return ByteArray(0)
            try {
                withOptions(mapOf(key(kCGImageDestinationLossyCompressionQuality) to quality / 100.0)) { options -> CGImageDestinationAddImage(destination, image, options) }
                if (!CGImageDestinationFinalize(destination)) return ByteArray(0)
            } finally {
                CFRelease(destination)
            }
            val length = CFDataGetLength(out).toInt()
            return CFDataGetBytePtr(out)?.reinterpret<kotlinx.cinterop.ByteVar>()?.readBytes(length) ?: ByteArray(0)
        } finally {
            CFRelease(type)
            CFRelease(out)
        }
    }

    /** A CoreFoundation constant as a Kotlin key (the constants are owned by the system, so one is retained before it is handed over). */
    private fun key(constant: CFStringRef?): Any? = CFBridgingRelease(CFRetain(constant))

    private fun <T> withOptions(
        options: Map<Any?, Any?>,
        use: (CFDictionaryRef?) -> T,
    ): T {
        val retained = CFBridgingRetain(options)
        try {
            @Suppress("UNCHECKED_CAST")
            return use(retained as CFDictionaryRef?)
        } finally {
            CFRelease(retained)
        }
    }
}

actual fun providePageEncoder(): PageEncoder = IosPageEncoder()
