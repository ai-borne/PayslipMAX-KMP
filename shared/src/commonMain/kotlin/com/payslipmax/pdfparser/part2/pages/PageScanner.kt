package com.payslipmax.pdfparser.part2.pages

/** Why a scan produced nothing: the device cannot scan (no scanner service, no screen, no camera) or the scan broke. */
enum class ScanFailure { UNAVAILABLE, FAILED }

/**
 * One page as the system scanner handed it over, before it is re-encoded. It is plaintext and lives somewhere the scanner chose (a cache
 * file on Android, memory on iOS), so whoever reads it must call [discard] afterwards, on success and on failure.
 */
interface ScannedPage {
    /** The page's image bytes, or null when it can no longer be read or is too large. The caller zeroes the bytes after use. */
    fun read(): ByteArray?

    /** Deletes whatever the scanner left for this page. Safe to call twice and after a failed read. */
    fun discard()
}

/** The outcome of one scan. [Pages.dropped] counts pages the scanner returned beyond the limit or that could not be accepted. */
sealed interface PageScan {
    class Pages(val pages: List<ScannedPage>, val dropped: Int = 0) : PageScan {
        override fun toString(): String = "Pages(${pages.size}, dropped=$dropped)"
    }

    /** The officer closed the scanner, or it returned no page. */
    data object Cancelled : PageScan

    class Failed(val reason: ScanFailure) : PageScan
}

/**
 * The system document scanner (ML Kit's on Android, VisionKit's on iOS): photo only, never written to the gallery or a cloud photo
 * sync. A platform's scanner is built from the screen it opens over, so a view model depends on this interface and never on a platform type.
 */
interface PageScanner {
    /** Opens the scanner for at most [maxPages] pages and returns when the officer is done. A scan in flight when the process dies is lost. */
    suspend fun scan(maxPages: Int): PageScan

    /** Deletes any page a scan left behind (the process was killed between the scanner's answer and the encoder). Call it when the vault opens. */
    fun discardLeftovers()
}
