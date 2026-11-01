package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.crypto.toByteArray
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import kotlin.coroutines.resume

/** What the Swift presenter answers when the document camera ends. Exactly one of these should be called; a second one is ignored. */
interface IosScanListener {
    /** The scanned pages as in-memory image data (JPEG), in order. An empty list means the officer closed the camera. */
    fun onPages(pages: List<NSData>)

    fun onCancelled()

    /** The device cannot scan documents (no camera, or the simulator). */
    fun onUnavailable()

    fun onFailed()
}

/**
 * The Swift side of the scanner (`DocumentScanPresenter` in iosApp, which wraps VisionKit's document camera): Kotlin/Native shows the
 * camera only through this interface, so the camera view controller and its permission prompt stay in Swift. Both calls arrive on the
 * main thread; [dismiss] closes the camera if it is still up.
 */
interface IosDocumentScannerDelegate {
    fun present(
        maxPages: Int,
        listener: IosScanListener,
    )

    fun dismiss()
}

/**
 * The iOS [PageScanner]. VisionKit returns pages in memory, so nothing is written to disk and there is nothing to sweep. If the officer's
 * screen goes away while the camera is up, the camera is closed. Pages beyond the room left are dropped (VisionKit has no page limit).
 */
class IosPageScanner(
    private val ui: CoroutineDispatcher = Dispatchers.Main,
    private val delegateProvider: () -> IosDocumentScannerDelegate? = { delegate },
) : PageScanner {
    override suspend fun scan(maxPages: Int): PageScan {
        val presenter = delegateProvider() ?: return PageScan.Failed(ScanFailure.UNAVAILABLE)
        return withContext(ui) {
            suspendCancellableCoroutine { continuation ->
                fun answer(scan: PageScan) {
                    if (continuation.isActive) continuation.resume(scan)
                }
                continuation.invokeOnCancellation { presenter.dismiss() }
                presenter.present(
                    maxPages,
                    object : IosScanListener {
                        override fun onPages(pages: List<NSData>) = answer(pagesOf(pages, maxPages))

                        override fun onCancelled() = answer(PageScan.Cancelled)

                        override fun onUnavailable() = answer(PageScan.Failed(ScanFailure.UNAVAILABLE))

                        override fun onFailed() = answer(PageScan.Failed(ScanFailure.FAILED))
                    },
                )
            }
        }
    }

    /** Nothing is written to disk on iOS. */
    override fun discardLeftovers() = Unit

    private fun pagesOf(
        pages: List<NSData>,
        maxPages: Int,
    ): PageScan {
        if (pages.isEmpty()) return PageScan.Cancelled
        return PageScan.Pages(pages.take(maxPages).map { IosScannedPage(it) }, dropped = maxOf(0, pages.size - maxPages))
    }

    companion object {
        /** Registered once at startup by the Swift app, like the crash reporter's delegate. */
        var delegate: IosDocumentScannerDelegate? = null
    }
}

/**
 * A page held in memory. [discard] lets go of the data (ARC frees it); a copy made by [read] is the caller's to zero. Foundation's own
 * buffer cannot be overwritten, so it is released, not zeroed: it was never written to disk.
 */
internal class IosScannedPage(private var data: NSData?) : ScannedPage {
    override fun read(): ByteArray? {
        val held = data ?: return null
        return if (held.length <= Part2Limits.PAGE_RAW_MAX_BYTES.toULong()) held.toByteArray() else null
    }

    override fun discard() {
        data = null
    }
}
