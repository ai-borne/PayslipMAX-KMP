package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

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
 * Scans pages into an order: scanner, then [PageStore]. The attach flow scans before any order exists, so it holds the scan and calls the
 * store itself once the order is saved; adding pages to an order that exists goes through [capture]. Cleanup guarantees are the store's.
 */
class PageCapture(
    private val scanner: PageScanner,
    private val pageStore: PageStore,
    private val repository: Part2Repository,
) {
    constructor(
        scanner: PageScanner,
        encoder: PageEncoder,
        repository: Part2Repository,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) : this(scanner, PageStore(encoder, repository, dispatcher), repository)

    suspend fun capture(orderId: String): CaptureResult {
        val room = Part2Limits.PAGES_PER_ORDER_MAX - repository.pages(orderId).size
        if (room <= 0) return CaptureResult.OrderFull
        return when (val scan = scanner.scan(room)) {
            PageScan.Cancelled -> CaptureResult.Cancelled
            is PageScan.Failed -> CaptureResult.Failed(scan.reason)
            is PageScan.Pages -> pageStore.store(orderId, scan)
        }
    }
}
