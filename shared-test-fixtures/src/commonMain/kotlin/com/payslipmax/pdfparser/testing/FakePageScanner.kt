package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.PageScanner
import com.payslipmax.pdfparser.part2.pages.ScannedPage
import kotlinx.coroutines.CompletableDeferred

/**
 * A [ScannedPage] over bytes in memory that records whether it was discarded and how often it was read. [readable] false makes [read] answer
 * null, as a cache file that vanished does.
 */
class FakeScannedPage(
    private val bytes: ByteArray,
    var readable: Boolean = true,
    private val log: MutableList<String>? = null,
    val name: String = "page",
) : ScannedPage {
    var reads = 0
        private set
    var discards = 0
        private set
    val discarded: Boolean get() = discards > 0

    override fun read(): ByteArray? {
        reads++
        log?.add("read $name")
        return if (readable) bytes.copyOf() else null
    }

    override fun discard() {
        discards++
        log?.add("discard $name")
    }
}

/** A [PageScanner] that answers [next] (once [gate] is open, when set), records the page limits it was asked for and how often leftovers were discarded: no screen, no platform. */
class FakePageScanner(
    var next: PageScan = PageScan.Cancelled,
) : PageScanner {
    val limits = mutableListOf<Int>()
    var leftoversDiscarded = 0
        private set

    /** When set, [scan] stays open until it is completed, so a test can look at the screen while a scan is in flight. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun scan(maxPages: Int): PageScan {
        limits += maxPages
        gate?.await()
        return next
    }

    override fun discardLeftovers() {
        leftoversDiscarded++
    }
}
