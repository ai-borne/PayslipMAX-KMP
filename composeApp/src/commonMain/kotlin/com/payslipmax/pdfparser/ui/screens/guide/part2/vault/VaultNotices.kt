package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.pages.CaptureResult
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.CancelRefusal
import com.payslipmax.pdfparser.ui.theme.Part2CancelStrings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings

/**
 * The words for how a scan ended (plan Phase 11 item 23): what was saved, then why the rest was not. Null when there is nothing to say:
 * the officer closed the scanner themselves, or nothing was asked of it. The words never name an order, a number or a date.
 */
internal object VaultNotices {
    fun of(result: CaptureResult): String? =
        when (result) {
            CaptureResult.Cancelled -> null
            CaptureResult.OrderFull -> Part2VaultStrings.orderFull()
            is CaptureResult.Failed -> if (result.reason == ScanFailure.UNAVAILABLE) Part2VaultStrings.scanNeedsConnection else Part2VaultStrings.scanFailed
            is CaptureResult.Saved -> saved(result)
        }

    /** The words for why a cancel could not start. */
    fun cancelRefusal(why: CancelRefusal): String =
        when (why) {
            CancelRefusal.NOT_READY -> Part2CancelStrings.notReady
            CancelRefusal.GONE -> Part2VaultStrings.orderGone
            CancelRefusal.NOT_QUOTABLE -> Part2CancelStrings.notQuotable
            CancelRefusal.NOTHING_TO_COPY -> Part2CancelStrings.nothingToCopy
            CancelRefusal.CANCEL_SLIP -> Part2CancelStrings.cancelSlipSource
            CancelRefusal.SAVE_FAILED -> Part2CancelStrings.saveFailed
        }

    private fun saved(result: CaptureResult.Saved): String? =
        listOfNotNull(
            Part2VaultStrings.pagesSaved(result.saved).takeIf { result.saved > 0 },
            refusal(result.refusal) ?: Part2VaultStrings.pagesNotSaved(result.notSaved).takeIf { result.notSaved > 0 },
        ).joinToString(" ").takeIf { it.isNotEmpty() }

    /** The vault's reason for refusing a page, when it has a sentence of its own; the rest are counted as pages not saved. */
    private fun refusal(reason: Part2WriteResult?): String? =
        when (reason) {
            Part2WriteResult.VAULT_FULL -> Part2VaultStrings.vaultFull
            Part2WriteResult.TOO_MANY -> Part2VaultStrings.orderHoldsMax()
            Part2WriteResult.NO_SUCH_ORDER -> Part2VaultStrings.orderGone
            else -> null
        }
}
