package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.ui.theme.Part2LeaveBankStrings

/**
 * What the screens draw of the Leave Bank: the statement's [rows] and what the last press of each section's button did ([messages]). It
 * asks the rule family (through [Part2Rules.fill]) for the notes and the check, and puts them in `Part2LeaveBankStrings`' words, so no code and
 * no sentence is named by the model or a composable. The rows are the officer's financial data: this class is a plain class, so it prints none of them.
 */
internal class LeaveBankView(
    private val rows: List<LeaveEntry>,
    private val messages: Map<Int, FillMessage>,
) {
    /** The fill of occurrence [index] ([request]), or null when its code has none. */
    fun fillOf(
        rules: Part2Rules,
        index: Int,
        request: OccurrenceRequest,
    ): FillUi? {
        val fill = rules.fill(request.code) ?: return null
        return FillUi(
            refill = fill.slots.any { it in request.values },
            message = messages[index]?.let(::words),
            notes = fill.notes(request, rows).map(Part2LeaveBankStrings::note),
        )
    }

    /** The lines that compare each drafted total with the statement, one per request that has a check. */
    fun crossChecks(
        rules: Part2Rules,
        requests: List<OccurrenceRequest>,
    ): List<String> = requests.mapNotNull { request -> rules.fill(request.code)?.crossCheck(request, rows) }.map(Part2LeaveBankStrings::crossCheck)

    private fun words(message: FillMessage): String =
        when (message) {
            FillMessage.Waiting -> Part2LeaveBankStrings.waiting
            is FillMessage.Filled -> Part2LeaveBankStrings.filled(message.count)
            FillMessage.Unchanged -> Part2LeaveBankStrings.unchanged
            FillMessage.NeedsYear -> Part2LeaveBankStrings.needsYear
            FillMessage.NothingKnown -> Part2LeaveBankStrings.nothingKnown
        }
}
