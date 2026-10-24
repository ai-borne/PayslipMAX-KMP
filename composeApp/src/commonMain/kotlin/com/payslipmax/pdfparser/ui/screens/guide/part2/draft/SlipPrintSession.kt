package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.print.PrintOutcome
import com.payslipmax.pdfparser.part2.print.SlipDocument
import com.payslipmax.pdfparser.part2.print.SlipPrinter
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** What one print hands over: the paper's [document] and the [handed] snapshot of the same rows (its time is set when the print completes). */
internal class PrintRequest(val document: SlipDocument, val handed: PrintedSnapshot)

/** The request for printing [event] as it stands in [state], or null while the slip is incomplete or an `ack` check is still unticked. */
internal fun LeaveDraftStateBuilder.printRequest(
    state: LeaveDraftUiState,
    event: LeaveEventInput,
): PrintRequest? {
    val slip = state.slip?.takeIf { it.readyToPrint } ?: return null
    return snapshotOf(event, 0L)?.let { PrintRequest(slip.toDocument(), it) }
}

/**
 * One print at a time for a draft. It asks [SlipPrinter] and records the print only when the printer reports it completed: a cancelled
 * dialog, a failed job or a model that closed meanwhile (its [scope] is cancelled) records nothing. [onChange] says the model's state
 * moved and [record] stores the snapshot (the model decides whether it may still write). The job name is fixed (`Part2Strings`), because
 * the system keeps it; nothing about the draft goes into it, and nothing here logs.
 */
internal class SlipPrintSession(
    private val scope: CoroutineScope,
    private val now: () -> Long,
    private val onChange: () -> Unit,
    private val record: suspend (PrintedSnapshot) -> Unit,
) {
    var printing = false
        private set

    fun start(
        printer: SlipPrinter,
        request: PrintRequest,
    ) {
        if (printing) return
        printing = true
        onChange()
        scope.launch {
            val outcome = printer.print(Part2Strings.printJobName, request.document)
            printing = false
            if (outcome == PrintOutcome.COMPLETED) record(request.handed.copy(at = now())) else onChange()
        }
    }
}
