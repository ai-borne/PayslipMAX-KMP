package com.payslipmax.pdfparser.reminders

import com.payslipmax.pdfparser.ensureKoin
import com.payslipmax.pdfparser.nav.PendingPart2Link
import org.koin.core.Koin
import org.koin.mp.KoinPlatformTools

/**
 * Swift calls this when the officer taps a reminder notification (`ReminderTapRouter`). The kind and draft id are untrusted text: they only
 * go into the Koin-scoped [PendingPart2Link], which shape-checks them and later checks the database. With Part II off nothing is bound and
 * the tap is dropped. Koin is started first, because a tap can launch the app cold.
 */
fun acceptReminderTap(
    kind: String?,
    draftId: String?,
) {
    ensureKoin()
    offerReminderTap(KoinPlatformTools.defaultContext().getOrNull(), kind, draftId)
}

/** The Koin-facing half of [acceptReminderTap], so a test can hand it a Koin of its own. */
internal fun offerReminderTap(
    koin: Koin?,
    kind: String?,
    draftId: String?,
) {
    koin?.getOrNull<PendingPart2Link>()?.offer(kind, draftId)
}
