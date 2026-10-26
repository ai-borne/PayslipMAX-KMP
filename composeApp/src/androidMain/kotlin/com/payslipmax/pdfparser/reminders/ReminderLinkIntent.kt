package com.payslipmax.pdfparser.reminders

import android.content.Context
import android.content.Intent
import com.payslipmax.pdfparser.MainActivity
import com.payslipmax.pdfparser.nav.PendingPart2Link
import com.payslipmax.pdfparser.part2.domain.ReminderKind

/**
 * The tap of a reminder notification as an intent for our own launcher activity, and the reading of it. It carries a reminder kind and a
 * draft id and nothing else; the launcher activity is exported, so anything arriving with these extras is untrusted and is only ever
 * offered to [PendingPart2Link], which shape-checks it and then checks the database. Holds no state.
 */
object ReminderLinkIntent {
    const val EXTRA_KIND = "part2_reminder_kind"
    const val EXTRA_DRAFT = "part2_reminder_draft"

    /** An explicit intent for [MainActivity] (no action, no data) that reuses the open app instead of stacking another. */
    fun tapIntent(
        context: Context,
        kind: ReminderKind,
        draftId: String,
    ): Intent =
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_KIND, kind.name)
            .putExtra(EXTRA_DRAFT, draftId)

    /**
     * Hands the tap in [intent] to [pending] (null while Part II is off, which drops it) and removes the extras, so a re-delivery of the same
     * intent (a rotation, a return from recents) cannot open Due a second time.
     */
    fun offer(
        intent: Intent?,
        pending: PendingPart2Link?,
    ) {
        intent ?: return
        val kind = intent.getStringExtra(EXTRA_KIND)
        val draftId = intent.getStringExtra(EXTRA_DRAFT)
        intent.removeExtra(EXTRA_KIND)
        intent.removeExtra(EXTRA_DRAFT)
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        pending?.offer(kind, draftId)
    }
}
