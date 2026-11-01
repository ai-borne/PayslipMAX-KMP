package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The one place that holds a page in plaintext after the scanner: encoder, then repository, one page at a time. On success, on any failure
 * and when the officer walks away, the scanner's copy of every page is deleted (each as soon as it is stored, the rest on the way out), the
 * plaintext and stored bytes are overwritten, and nothing about a page is logged, reported or put in an error. The encoder runs on
 * [dispatcher], off the main thread.
 */
class PageStore(
    private val encoder: PageEncoder,
    private val repository: Part2Repository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    /** Stores every page of [scan] in [orderId] and discards them all, whatever happens. */
    suspend fun store(
        orderId: String,
        scan: PageScan.Pages,
    ): CaptureResult.Saved {
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
