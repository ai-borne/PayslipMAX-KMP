package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.part2.reminders.ReminderSync
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.hasAccessFlow
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import org.koin.core.Koin

/**
 * Starts keeping the system's reminders equal to the drafts, for as long as the app lives, with the entitlement of the one
 * [PayslipViewModel] the platform entry point owns (reminders are Premium, DECISIONS Round 10). Called once by `MainActivity` and by the iOS
 * nav host; a second call changes nothing. It does nothing, and Koin is not asked for the sync, while Part II is off. It is started from the
 * UI entry points and not from `Application.onCreate` on purpose: a worker the system starts in a cold process must not rebuild the schedule
 * (and so cancel the very job that is running) while it posts.
 */
fun startPart2Reminders(
    koin: Koin,
    viewModel: PayslipViewModel,
    enabled: Boolean = isPart2Enabled(),
) {
    if (!enabled) return
    koin.getOrNull<ReminderSync>()?.start(viewModel.hasAccessFlow(FeatureGate.PART2_ORDERS))
}
