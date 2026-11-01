package com.payslipmax.pdfparser.part2.print

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSData
import platform.UIKit.UIPrintInfo
import platform.UIKit.UIPrintInteractionController
import kotlin.coroutines.resume

/**
 * Prints the index through iOS's print sheet (`UIPrintInteractionController`) with the PDF as in-memory data: no file, no share sheet.
 * The call returns when the sheet is dismissed; nothing is recorded either way.
 */
@OptIn(ExperimentalForeignApi::class)
class IosIndexPrinter : IndexPrinter {
    override suspend fun print(
        jobName: String,
        document: IndexDocument,
    ): PrintOutcome {
        if (!UIPrintInteractionController.isPrintingAvailable()) return PrintOutcome.NOT_PRINTED
        val pdf = IosIndexPdf.render(document) ?: return PrintOutcome.NOT_PRINTED
        return withContext(Dispatchers.Main) { present(jobName, pdf) }
    }

    private suspend fun present(
        jobName: String,
        pdf: NSData,
    ): PrintOutcome =
        suspendCancellableCoroutine { continuation ->
            val controller = UIPrintInteractionController.sharedPrintController
            controller.printInfo = UIPrintInfo.printInfo().apply { this.jobName = jobName }
            controller.printingItem = pdf
            val shown = controller.presentAnimated(true) { _, completed, _ -> if (continuation.isActive) continuation.resume(outcomeOf(completed)) }
            if (!shown && continuation.isActive) continuation.resume(PrintOutcome.NOT_PRINTED)
        }

    internal companion object {
        /** The sheet's `completed` flag, which is false when the officer cancels or the job fails. */
        fun outcomeOf(completed: Boolean): PrintOutcome = if (completed) PrintOutcome.COMPLETED else PrintOutcome.NOT_PRINTED
    }
}
