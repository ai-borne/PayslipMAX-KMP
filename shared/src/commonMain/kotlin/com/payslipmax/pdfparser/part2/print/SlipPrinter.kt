package com.payslipmax.pdfparser.part2.print

/** How a print ended: the system reports the job done, or it did not (cancelled, failed, no screen to print from). */
enum class PrintOutcome { COMPLETED, NOT_PRINTED }

/**
 * Prints a slip through the system print dialog and nowhere else: no share sheet, no file the officer can reach, no clipboard. The page
 * is built in memory. A platform's printer is built from the screen it prints over (Android needs an Activity), so a view model
 * depends on this interface and never on a platform type.
 */
interface SlipPrinter {
    /** Opens the system print dialog for [document] under the job name [jobName] and returns when the job ended. */
    suspend fun print(
        jobName: String,
        document: SlipDocument,
    ): PrintOutcome
}
