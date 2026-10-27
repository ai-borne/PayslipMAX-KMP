package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.part2.leavebank.LeaveBankFeed
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankSource
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import com.payslipmax.pdfparser.ui.screens.isLeaveBankEnabled
import org.koin.core.Koin

/** The source the year-end forms fill from, or null where the Leave Bank is switched off (`LaunchFlags.LEAVE_BANK_ENABLED`): then no form offers a fill. */
fun leaveBankFor(
    source: LeaveBankSource,
    enabled: Boolean = isLeaveBankEnabled(),
): LeaveBankSource? = source.takeIf { enabled }

/**
 * Hands the feed the one `leaveBankSummary` the [PayslipViewModel] derives (the Leave Bank's single derivation, never recomputed here), so the
 * year-end forms read the same rows the Leave Bank screens show. Called from the Guide tab, which already holds that view model; a second call
 * only replaces the summary with the same one. Does nothing, and Koin is not asked for the feed, while Part II is off.
 */
fun attachPart2LeaveBank(
    koin: Koin,
    viewModel: PayslipViewModel,
    enabled: Boolean = isPart2Enabled(),
) {
    if (!enabled) return
    koin.getOrNull<LeaveBankFeed>()?.attach(viewModel.leaveBankSummary)
}
