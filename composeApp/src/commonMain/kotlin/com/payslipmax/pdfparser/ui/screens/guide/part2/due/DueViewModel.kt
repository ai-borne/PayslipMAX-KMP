package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import com.payslipmax.pdfparser.rating.currentTimeMillis
import com.payslipmax.pdfparser.ui.UI_STOP_TIMEOUT_MS
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.DraftRowUi
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings
import com.payslipmax.pdfparser.utils.localUtcOffsetMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One line of the Due list: what to do ([action]), which slip ([title]), when ([line]) and the draft that opens ([draftId]). */
data class DueRowUi(
    val key: String,
    val draftId: String,
    val action: String,
    val title: String,
    val line: String,
    val overdue: Boolean,
    val task: String,
)

/** The Due list: what is due now, what comes next (one line per reminder, not one per repeat) and whether something on the device could not be read. */
data class DueUiState(
    val due: List<DueRowUi>,
    val upcoming: List<DueRowUi>,
    val unreadable: Boolean,
) {
    val dueCount: Int get() = due.size
    val isEmpty: Boolean get() = due.isEmpty() && upcoming.isEmpty()

    override fun toString(): String = "DueUiState(due=${due.size}, upcoming=${upcoming.size})"

    companion object {
        val Empty = DueUiState(emptyList(), emptyList(), unreadable = false)
    }
}

/**
 * The Due list: [ReminderPlanner]'s plan for the drafts and orders on the device, worded for the screen. It does no date arithmetic (the planner
 * says what is due and what is overdue; this only writes a date it is handed) and checks no entitlement (reminders are Premium, but the data layer
 * is ungated, DECISIONS Round 13: the screens decide who sees it). It is rebuilt when a draft or an order changes and at every local hour ([hourlyTicks]), so a list left open follows the clock (Phase 11 item 35). Each draft's title is the drafts list's own wording. It reads, with one
 * exception: [dismiss] writes the draft's `dismissed` field and nothing else (not its edit time, answers or print record). No telemetry:
 * nothing about a draft is logged.
 */
class DueViewModel(
    private val repository: Part2Repository,
    drafts: Part2DraftsViewModel,
    private val planner: ReminderPlanner = ReminderPlanner(offsetAt = ::localUtcOffsetMillis),
    private val now: () -> Long = ::currentTimeMillis,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    clockTicks: Flow<Unit> = hourlyTicks(planner, now),
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    val state: StateFlow<DueUiState> =
        combine(repository.observeDrafts(), repository.observeOrders(), drafts.state, clockTicks.onStart { emit(Unit) }) { saved, vault, listed, _ ->
            val plan = planner.plan(saved.items, vault.items, now())
            val titles = listed.rows.associateBy { it.id }
            val dueTasks = plan.due.map { task(it.reminder) }.toSet()
            DueUiState(
                due = plan.due.map { row(it.reminder, titles, it.overdue) },
                // A repeating reminder is one line (its next date), not one per repeat, and not again once it is due: it is the same task.
                upcoming = plan.upcoming.filterNot { task(it) in dueTasks }.distinctBy(::task).map { row(it, titles, overdue = false) },
                unreadable = saved.unreadable > 0 || vault.unreadable > 0,
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(UI_STOP_TIMEOUT_MS), DueUiState.Empty)

    /** What a reminder asks for, whichever repeat it is: its kind, the draft and (for a year end) the leave year. */
    private fun task(reminder: Reminder) = Triple(reminder.kind, reminder.draftId, reminder.year)

    private fun row(
        reminder: Reminder,
        titles: Map<String, DraftRowUi>,
        overdue: Boolean,
    ) = DueRowUi(
        key = reminder.key,
        draftId = reminder.draftId,
        action = Part2DueStrings.action(reminder.kind, reminder.year),
        title = titles[reminder.draftId]?.title ?: Part2DueStrings.untitled,
        line = Part2DueStrings.dueLine(CivilCalendar.format(reminder.dueOn), overdue),
        overdue = overdue,
        task = reminder.task,
    )

    /** Dismisses reminder [task] of draft [draftId]: remembered with the draft (the one field this model writes). Nothing happens for a draft that is gone. */
    fun dismiss(
        draftId: String,
        task: String,
    ) {
        scope.launch {
            val draft = repository.draft(draftId) ?: return@launch
            if (task !in draft.dismissed) repository.saveDraft(draft.copy(dismissed = draft.dismissed + task))
        }
    }
}
