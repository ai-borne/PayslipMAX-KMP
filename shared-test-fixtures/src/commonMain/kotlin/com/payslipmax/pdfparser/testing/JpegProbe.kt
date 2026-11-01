package com.payslipmax.pdfparser.testing

/**
 * Reads and builds JPEG bytes for tests that must prove what a page file contains: which marker segments it has, its size, and whether
 * a planted piece of metadata survived. No codec is involved, so it works the same on the JVM and on Kotlin/Native.
 */
object JpegProbe {
    /** The marker codes of the segments before the image data (0xE1 is APP1, the Exif segment), in file order. */
    fun markers(jpeg: ByteArray): List<Int> {
        val found = mutableListOf<Int>()
        var at = 2
        while (at + 3 < jpeg.size) {
            if (jpeg[at].toInt() and 0xFF != 0xFF) break
            val marker = jpeg[at + 1].toInt() and 0xFF
            found += marker
            if (marker == 0xDA) break
            at += 2 + ((jpeg[at + 2].toInt() and 0xFF) shl 8 or (jpeg[at + 3].toInt() and 0xFF))
        }
        return found
    }

    /** Width and height from the frame header (SOF0, SOF1 or SOF2), or null. */
    fun size(jpeg: ByteArray): Pair<Int, Int>? {
        var at = 2
        while (at + 3 < jpeg.size) {
            val marker = jpeg[at + 1].toInt() and 0xFF
            val length = (jpeg[at + 2].toInt() and 0xFF shl 8) or (jpeg[at + 3].toInt() and 0xFF)
            if (marker in 0xC0..0xC2 && at + 8 < jpeg.size) {
                val height = (jpeg[at + 5].toInt() and 0xFF shl 8) or (jpeg[at + 6].toInt() and 0xFF)
                val width = (jpeg[at + 7].toInt() and 0xFF shl 8) or (jpeg[at + 8].toInt() and 0xFF)
                return width to height
            }
            at += 2 + length
        }
        return null
    }

    fun contains(
        jpeg: ByteArray,
        text: String,
    ): Boolean {
        val needle = text.encodeToByteArray()
        return (0..jpeg.size - needle.size).any { start -> needle.indices.all { jpeg[start + it] == needle[it] } }
    }

    /**
     * [jpeg] with segments a camera adds, right after the start marker: an Exif APP1 carrying [orientation] (1 to 8) and the text
     * [secret] (stands in for a GPS position or a device name), a comment holding [secret] and an APP13 (IPTC) holding it too.
     */
    fun withMetadata(
        jpeg: ByteArray,
        orientation: Int = 1,
        secret: String = "MARKER_SECRET",
    ): ByteArray {
        val tiff =
            byteArrayOf(0x4D, 0x4D, 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08, 0x00, 0x01, 0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, orientation.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        val exif = "Exif".encodeToByteArray() + byteArrayOf(0, 0) + tiff + secret.encodeToByteArray()
        return jpeg.copyOfRange(0, 2) + segment(0xE1, exif) + segment(0xFE, secret.encodeToByteArray()) + segment(0xED, secret.encodeToByteArray()) + jpeg.copyOfRange(2, jpeg.size)
    }

    /** One marker segment: 0xFF, the marker, a two-byte length that counts itself, the payload. */
    fun segment(
        marker: Int,
        payload: ByteArray,
    ): ByteArray {
        val length = payload.size + 2
        return byteArrayOf(0xFF.toByte(), marker.toByte(), (length shr 8).toByte(), length.toByte()) + payload
    }

    /**
     * A tiny structurally valid JPEG (start, JFIF, frame header, scan header, [entropy] data, end): enough for the marker walk and the
     * metadata strip, not for a decoder.
     */
    fun minimal(
        width: Int = 8,
        height: Int = 8,
        entropy: ByteArray = byteArrayOf(0x12, 0x34, 0xFF.toByte(), 0x00, 0x56),
    ): ByteArray {
        val jfif = "JFIF".encodeToByteArray() + byteArrayOf(0, 1, 1, 0, 0, 1, 0, 1, 0, 0)
        val frame = byteArrayOf(8, (height shr 8).toByte(), height.toByte(), (width shr 8).toByte(), width.toByte(), 1, 1, 0x11, 0)
        val scan = byteArrayOf(1, 1, 0, 0, 63, 0)
        return byteArrayOf(0xFF.toByte(), 0xD8.toByte()) + segment(0xE0, jfif) + segment(0xC0, frame) + segment(0xDA, scan) + entropy + byteArrayOf(0xFF.toByte(), 0xD9.toByte())
    }
}
