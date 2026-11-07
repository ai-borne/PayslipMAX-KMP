package com.payslipmax.pdfparser.part2.print

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Prints the index through Android's print dialog, built from the Activity the officer is looking at. With no Activity (the screen is
 * gone) it does nothing. Nothing is shared, sent or written to a file: the pages go to the system as it asks for them, and the call returns
 * when the system's job ends ([PrintJobWatch]); nothing is recorded either way. The dialog is opened on [ui], the main thread.
 */
class AndroidIndexPrinter(
    private val activity: Activity?,
    private val ui: CoroutineDispatcher,
) : IndexPrinter {
    override suspend fun print(
        jobName: String,
        document: IndexDocument,
    ): PrintOutcome {
        val manager = activity?.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return PrintOutcome.NOT_PRINTED
        val pages = IndexLayout.paginate(document, PaintMeasurer())
        if (pages.isEmpty()) return PrintOutcome.NOT_PRINTED
        val adapter = IndexPrintAdapter(jobName, pages)
        val attributes = PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build()
        val job =
            try {
                withContext(ui) { manager.print(jobName, adapter, attributes) }
            } catch (e: IllegalStateException) {
                null
            } ?: return PrintOutcome.NOT_PRINTED
        return PrintJobWatch.outcome({ job.info.state })
    }
}
