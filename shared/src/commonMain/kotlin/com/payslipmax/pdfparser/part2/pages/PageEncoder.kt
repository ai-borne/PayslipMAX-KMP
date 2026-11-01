package com.payslipmax.pdfparser.part2.pages

/**
 * Turns a scanned page into what the vault stores: a JPEG whose long edge is at most `Part2Limits.PAGE_LONG_EDGE_PX` and that is at most
 * `Part2Limits.PAGE_MAX_BYTES`, upright, with every piece of metadata (Exif, GPS, comments, thumbnails) gone. It runs on the caller's
 * thread, so callers use a background dispatcher.
 */
interface PageEncoder {
    /** Null when [raw] is not a readable image, is too large to decode safely, or cannot be made small enough without becoming unreadable. */
    fun encode(raw: ByteArray): ByteArray?
}

/** The platform's encoder. */
expect fun providePageEncoder(): PageEncoder
