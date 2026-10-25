package com.payslipmax.pdfparser.ui.theme

import com.payslipmax.pdfparser.part2.domain.CancelRules
import com.payslipmax.pdfparser.part2.domain.CancelScope

/**
 * Copy for cancelling a published Part II order (docs/Plan/12_Part2Orders_PhasePlan.md, Phase 6c; ODP-2026 para 11 and pp.226-227). Kept
 * apart from [Part2Strings] and [Part2VaultStrings] so none of them nears the 300-line limit. Nothing here is built from an order's number or
 * date except through the functions that take them.
 */
object Part2CancelStrings {
    // The order screen.
    const val cancelOrder = "Cancel this order"
    const val cancelAndRedraft = "Cancel and re-draft"
    const val cancelNeedsNumber = "To cancel this order, type its number and date above first. The cancel slip quotes both."

    // The pair of slips: a cancel slip and the corrected draft made with it.
    const val openCorrectedDraft = "Open the corrected draft"
    const val openCancelSlip = "Open the cancel slip"

    // Why a cancel could not start.
    const val notReady = "Part II is not ready yet. Try again in a moment."
    const val notQuotable = "Type the order's number and date first. The cancel slip quotes both."
    const val nothingToCopy = "The draft this order came from is no longer on this device, so there is nothing to copy. Cancel the order and draft the corrected one by hand."
    const val cancelSlipSource = "This order came from a cancel slip, so there is no order to copy. Cancel it and draft the corrected order by hand."
    const val saveFailed = "The cancel slip could not be saved on this device."

    /** What the vault says of an order a slip has been made to cancel, by how much it covers and how far it has got. */
    fun printedOn(
        scope: CancelScope,
        date: String,
    ): String =
        when (scope) {
            CancelScope.ALL -> "Cancel slip printed on $date."
            CancelScope.ONE -> "A slip cancelling one occurrence was printed on $date."
        }

    fun cancelledBy(
        scope: CancelScope,
        no: String,
        date: String,
    ): String =
        when (scope) {
            CancelScope.ALL -> "Cancelled by order $no dated $date."
            CancelScope.ONE -> "One occurrence cancelled by order $no dated $date."
        }

    /** What a cancellation check says, by the rule's id; null for an id that is not one of the cancel family's (the caller then tries the others). */
    fun finding(ruleId: String): String? =
        when (ruleId) {
            CancelRules.CANCEL_QUOTED, CancelRules.CANCELS_QUOTED -> "Quote the number and date of the order being cancelled. Until you do, they print blank."
            CancelRules.CANCEL_TIME_BARRED, CancelRules.CANCELS_TIME_BARRED ->
                "The order being cancelled is more than 2 years old. Enclose the time-bar sanction with the cancellation. The manual counts the 2 years from the occurrence, which may be older still."
            CancelRules.CANCELS_CODE_UNKNOWN -> "The code you typed is not one the manual lists. Check it against the published order; it prints as you typed it."
            else -> null
        }
}
