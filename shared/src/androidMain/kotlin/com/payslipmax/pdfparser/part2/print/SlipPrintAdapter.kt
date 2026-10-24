package com.payslipmax.pdfparser.part2.print

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.IOException

/** Hands the system print dialog the slip's pages: [pages] are already laid out, so the preview and the paper are the same pages. */
internal class SlipPrintAdapter(
    private val jobName: String,
    private val pages: List<SlipPage>,
    private val newDocument: () -> SlipPdfDocument = ::FrameworkPdfDocument,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) return callback.onLayoutCancelled()
        val info = PrintDocumentInfo.Builder(jobName).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pages.size).build()
        // The pages do not depend on the paper chosen (A4, fitted by the system), so they are always written again.
        callback.onLayoutFinished(info, true)
    }

    override fun onWrite(
        ranges: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        if (cancellationSignal?.isCanceled == true) return callback.onWriteCancelled()
        val wanted = ranges.flatMap { (it.start..minOf(it.end, pages.lastIndex)).toList() }.distinct().sorted()
        try {
            ParcelFileDescriptor.AutoCloseOutputStream(destination).use { SlipPdf.write(pages, wanted, it, newDocument()) }
        } catch (e: IOException) {
            // No message: whatever the stream says is not for the officer, and nothing about the slip may leave through it.
            return callback.onWriteFailed(null)
        }
        callback.onWriteFinished(ranges.toList().toTypedArray())
    }
}
