package com.payslipmax.pdfparser.part2.reminders

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import kotlinx.coroutines.flow.first

/**
 * Asked by the worker the system starts when a reminder's time comes: is this reminder still due? It reads the drafts and orders then and
 * asks [ReminderPlanner], so an answered, dismissed or deleted reminder stays silent even if the app has not been opened to rebuild the
 * schedule. Like the planner it is ungated (DECISIONS Round 13); a lapse is cancelled by [ReminderSync] the next time the app opens.
 */
class ReminderFireCheck(
    private val repository: Part2Repository,
    private val planner: ReminderPlanner,
    private val now: () -> Long,
) {
    /** True when a reminder of [kind] for draft [draftId] is due now. */
    suspend fun shouldPost(
        kind: ReminderKind,
        draftId: String,
    ): Boolean {
        val drafts = repository.observeDrafts().first().items
        val orders = repository.observeOrders().first().items
        return planner.plan(drafts, orders, now()).due.any { it.reminder.kind == kind && it.reminder.draftId == draftId }
    }

    /** True when the draft is still on the device. The debug walk switch uses this alone: it moves a reminder in time, so "due" cannot be asked. */
    suspend fun draftExists(draftId: String): Boolean = repository.draft(draftId) != null
}
