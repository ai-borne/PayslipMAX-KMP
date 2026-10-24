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
 * Prints the slip through iOS's print sheet (`UIPrintInteractionController`) with the PDF as in-memory data: no file, no share sheet.
 * The print counts when the sheet reports it completed.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSlipPrinter : SlipPrinter {
    override suspend fun print(
        jobName: String,
        document: SlipDocument,
    ): PrintOutcome {
        if (!UIPrintInteractionController.isPrintingAvailable()) return PrintOutcome.NOT_PRINTED
        return withContext(Dispatchers.Main) { present(jobName, IosSlipPdf.render(document)) }
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
