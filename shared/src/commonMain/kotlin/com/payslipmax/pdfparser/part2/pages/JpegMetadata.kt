package com.payslipmax.pdfparser.part2.pages

/**
 * Cuts every piece of metadata from a JPEG's bytes: Exif (GPS position, camera, time, thumbnail), the other application segments
 * (APP1 to APP13 and APP15: XMP, IPTC, vendor data) and comments. The picture itself, its tables and the colour-transform segment
 * (APP14) are not touched. It works on the bytes, so it holds whatever a platform's encoder wrote.
 */
object JpegMetadata {
    private const val FF = 0xFF
    private const val SOI = 0xD8
    private const val EOI = 0xD9
    private const val SOS = 0xDA

    /** [jpeg] without metadata, or null when it is not a well-formed JPEG with image data (a malformed one is refused, never guessed at). */
    fun strip(jpeg: ByteArray): ByteArray? {
        if (jpeg.size < 2 || jpeg[0].u() != FF || jpeg[1].u() != SOI) return null
        val kept = mutableListOf<IntRange>()
        var runStart = 0
        var at = 2
        var sawScan = false
        while (at < jpeg.size) {
            if (jpeg[at].u() != FF || at + 1 >= jpeg.size) return null
            val marker = jpeg[at + 1].u()
            when {
                marker == FF -> at++
                marker == EOI -> {
                    at += 2
                    break
                }
                marker == 0x01 || marker in 0xD0..0xD8 -> at += 2
                else -> {
                    if (at + 3 >= jpeg.size) return null
                    val length = (jpeg[at + 2].u() shl 8) or jpeg[at + 3].u()
                    val end = at.toLong() + 2 + length
                    if (length < 2 || end > jpeg.size) return null
                    if (isMetadata(marker)) {
                        kept += runStart until at
                        runStart = end.toInt()
                    }
                    at = end.toInt()
                    if (marker == SOS) {
                        sawScan = true
                        at = endOfScanData(jpeg, at)
                    }
                }
            }
        }
        if (!sawScan) return null
        kept += runStart until at
        val out = ByteArray(kept.sumOf { it.last - it.first + 1 })
        var to = 0
        for (run in kept) {
            jpeg.copyInto(out, to, run.first, run.last + 1)
            to += run.last - run.first + 1
        }
        return out
    }

    private fun isMetadata(marker: Int): Boolean = marker in 0xE1..0xED || marker == 0xEF || marker == 0xFE

    /** Where the entropy-coded data after a scan header ends: at the next real marker (a stuffed FF 00, a restart marker and fill bytes are data). */
    private fun endOfScanData(
        jpeg: ByteArray,
        from: Int,
    ): Int {
        var at = from
        while (at < jpeg.size) {
            if (jpeg[at].u() != FF) {
                at++
                continue
            }
            if (at + 1 >= jpeg.size) return jpeg.size
            val next = jpeg[at + 1].u()
            when {
                next == 0x00 || next in 0xD0..0xD7 -> at += 2
                else -> return at
            }
        }
        return at
    }

    private fun Byte.u(): Int = toInt() and 0xFF
}
