package com.payslipmax.pdfparser.part2.reminders

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps the operating system's reminders equal to what the drafts say (DECISIONS Round 10: drafts are the source of truth, what is scheduled
 * is rebuilt from them). Whenever the drafts, the orders or the entitlement change it asks [ReminderPlanner] for the plan and hands
 * `upcoming` to the [ReminderScheduler]; an officer who is not entitled gets an empty list, so nothing stays scheduled after Premium
 * lapses. It does no date arithmetic (the planner owns all timing) and checks no rule of its own. A list identical to the last one given is not
 * given again, because autosave changes a draft on every step. Nothing about a draft is logged or reported, and a scheduler that throws
 * neither stops the sync nor is remembered as done, so the next change tries again.
 */
class ReminderSync(
    private val repository: Part2Repository,
    private val scheduler: ReminderScheduler,
    private val planner: ReminderPlanner,
    private val now: () -> Long,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var job: Job? = null
    private var given: List<Reminder>? = null
    private val _hasReminders = MutableStateFlow(false)

    /** Whether an entitled officer has anything due or coming up: the moment "a draft sets a reminder" for the permission prompt. */
    val hasReminders: StateFlow<Boolean> = _hasReminders

    /** Starts following the device; a second call changes nothing. [entitled] says whether reminders are allowed (Premium, Round 10). */
    fun start(entitled: Flow<Boolean>) {
        if (job != null) return
        job =
            scope.launch {
                combine(repository.observeDrafts(), repository.observeOrders(), entitled) { drafts, orders, allowed ->
                    if (allowed) planner.plan(drafts.items, orders.items, now()) else null
                }.collect { plan ->
                    _hasReminders.value = plan != null && (plan.due.isNotEmpty() || plan.upcoming.isNotEmpty())
                    give(plan?.upcoming.orEmpty())
                }
            }
    }

    /**
     * One rebuild from the drafts as they are now, with no watching: for a boot receiver, because the system drops queued jobs at a reboot and
     * the running app is not there to rebuild them. It does not know the entitlement (no screen or billing is loaded in a boot process), so it
     * schedules as for an entitled officer; the next time the app opens, [start] corrects it (Phase 11 item 38).
     */
    suspend fun rebuildOnce() {
        give(planner.plan(repository.observeDrafts().first().items, repository.observeOrders().first().items, now()).upcoming)
    }

    private fun give(reminders: List<Reminder>) {
        if (reminders == given) return
        try {
            scheduler.replaceAll(reminders)
            given = reminders
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            given = null
        }
    }
}
