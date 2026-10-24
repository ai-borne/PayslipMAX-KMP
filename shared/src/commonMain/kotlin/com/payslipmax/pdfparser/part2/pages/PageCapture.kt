package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** How a capture ended. [Saved] counts only: [notSaved] is every page that was dropped, unreadable, refused by the encoder or not accepted by the vault. */
sealed interface CaptureResult {
    /** [refusal] is the vault's reason when it refused a page (the order or the vault is full, the order is gone); the pages after it were not tried. */
    class Saved(val saved: Int, val notSaved: Int, val refusal: Part2WriteResult? = null) : CaptureResult {
        override fun toString(): String = "Saved($saved, notSaved=$notSaved)"
    }

    data object Cancelled : CaptureResult

    /** The order already holds `Part2Limits.PAGES_PER_ORDER_MAX` pages, so the scanner was not opened. */
    data object OrderFull : CaptureResult

    class Failed(val reason: ScanFailure) : CaptureResult
}

/**
 * Scans pages into an order: scanner, then the encoder, then the repository, one page at a time. It is the one place that holds a page in
 * plaintext, so it guarantees, on success, on any failure and when the officer walks away: the scanner's copy of every page is deleted
 * (each one as soon as it is stored, the rest on the way out), the plaintext and stored bytes are overwritten, and nothing about a page
 * is logged, reported or put in an error. The encoder runs on [dispatcher], off the main thread.
 */
class PageCapture(
    private val scanner: PageScanner,
    private val encoder: PageEncoder,
    private val repository: Part2Repository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    suspend fun capture(orderId: String): CaptureResult {
        val room = Part2Limits.PAGES_PER_ORDER_MAX - repository.pages(orderId).size
        if (room <= 0) return CaptureResult.OrderFull
        return when (val scan = scanner.scan(room)) {
            PageScan.Cancelled -> CaptureResult.Cancelled
            is PageScan.Failed -> CaptureResult.Failed(scan.reason)
            is PageScan.Pages -> store(orderId, scan)
        }
    }

    private suspend fun store(
        orderId: String,
        scan: PageScan.Pages,
    ): CaptureResult {
        var saved = 0
        var notSaved = scan.dropped
        var refusal: Part2WriteResult? = null
        var next = 0
        try {
            while (next < scan.pages.size) {
                val page = scan.pages[next++]
                try {
                    if (refusal != null) {
                        notSaved++
                        continue
                    }
                    val result = keep(orderId, page)
                    when {
                        result == null -> notSaved++
                        result == Part2WriteResult.SAVED -> saved++
                        else -> {
                            notSaved++
                            refusal = result
                        }
                    }
                } finally {
                    page.discard()
                }
            }
        } finally {
            for (i in next until scan.pages.size) scan.pages[i].discard()
        }
        return CaptureResult.Saved(saved, notSaved, refusal)
    }

    /** Reads, encodes and stores one page; null when the page could not be read or encoded, else the vault's answer. */
    private suspend fun keep(
        orderId: String,
        page: ScannedPage,
    ): Part2WriteResult? {
        val raw = withContext(dispatcher) { page.read() } ?: return null
        val encoded =
            try {
                withContext(dispatcher) { encoder.encode(raw) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            } finally {
                raw.fill(0)
            } ?: return null
        return try {
            repository.addPage(orderId, encoded).result
        } finally {
            encoded.fill(0)
        }
    }
}
