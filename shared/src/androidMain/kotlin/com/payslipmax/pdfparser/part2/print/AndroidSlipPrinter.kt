package com.payslipmax.pdfparser.part2.print

import android.app.Activity
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Prints the slip through Android's print dialog, built from the Activity the officer is looking at. With no Activity (the screen is
 * gone) it does nothing. Nothing is shared, sent or written to a file: the pages go to the system as it asks for them, and the print
 * counts only when the system's job completes ([PrintJobWatch]). The dialog is opened on [ui], the main thread.
 */
class AndroidSlipPrinter(
    private val activity: Activity?,
    private val ui: CoroutineDispatcher,
) : SlipPrinter {
    override suspend fun print(
        jobName: String,
        document: SlipDocument,
    ): PrintOutcome {
        val manager = activity?.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return PrintOutcome.NOT_PRINTED
        val adapter = SlipPrintAdapter(jobName, SlipLayout.paginate(document, PaintMeasurer()))
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
