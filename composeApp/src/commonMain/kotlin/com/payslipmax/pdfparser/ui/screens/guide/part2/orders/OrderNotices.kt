package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.pages.CaptureResult
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.ui.theme.OrderStrings

/** The words for how a scan or a save ended. None names an order, a number, a date or a page. */
internal object OrderNotices {
    fun of(notice: AttachNotice): String =
        when (notice) {
            AttachNotice.SCAN_UNAVAILABLE -> OrderStrings.scanUnavailable
            AttachNotice.SCAN_FAILED -> OrderStrings.scanFailed
            AttachNotice.ORDER_FULL -> OrderStrings.orderFull()
        }

    fun problem(problem: AttachProblem): String =
        when (problem) {
            AttachProblem.NO_PAGES -> OrderStrings.needPages
            AttachProblem.NO_CASUALTY -> OrderStrings.needCasualty
            AttachProblem.ORDER_NO_LONG, AttachProblem.LAST_NO_LONG -> OrderStrings.orderNoTooLong
            AttachProblem.BAD_COUNT -> OrderStrings.badCount
        }

    /** What to tell the officer when Save did not finish, or when it kept fewer pages than were scanned; null when all is well. */
    fun outcome(outcome: AttachOutcome): String? =
        when (outcome) {
            AttachOutcome.Invalid -> null
            AttachOutcome.BankFull -> OrderStrings.bankFull
            AttachOutcome.VaultFull -> OrderStrings.vaultFull
            AttachOutcome.Failed -> OrderStrings.saveFailed
            is AttachOutcome.Saved -> if (outcome.notSaved > 0) OrderStrings.pagesSaved(outcome.saved, outcome.saved + outcome.notSaved) else null
        }

    /** How "Add pages" ended on an existing order; null when the officer closed the scanner or all pages were kept. */
    fun of(result: CaptureResult?): String? =
        when (result) {
            null, CaptureResult.Cancelled -> null
            CaptureResult.OrderFull -> OrderStrings.orderFull()
            is CaptureResult.Failed -> if (result.reason == ScanFailure.UNAVAILABLE) OrderStrings.scanUnavailable else OrderStrings.scanFailed
            is CaptureResult.Saved -> saved(result)
        }

    private fun saved(result: CaptureResult.Saved): String? =
        when {
            result.refusal == Part2WriteResult.VAULT_FULL -> OrderStrings.vaultFull
            result.refusal == Part2WriteResult.NO_SUCH_ORDER -> OrderStrings.orderGone
            result.notSaved > 0 -> OrderStrings.pagesSaved(result.saved, result.saved + result.notSaved)
            else -> null
        }
}
