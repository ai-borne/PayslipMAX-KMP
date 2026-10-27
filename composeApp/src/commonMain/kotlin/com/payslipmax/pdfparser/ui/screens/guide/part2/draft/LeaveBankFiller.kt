package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.FillOutcome
import com.payslipmax.pdfparser.part2.domain.LeaveBankInputs
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The Leave Bank fill of one open draft (plan, phase 8b; owner, Round 33). It listens to the [source] while the form is open, so the notes and
 * the check against the statement follow it, and on a press fills occurrence [index] from what its rule family offers for the code. The family
 * decides which codes and which boxes; this class names neither.
 *
 * The fill is applied inside one atomic [edit] at the moment the Leave Bank has answered, to the answers as they are THEN, so a source that
 * answers late fills only what is still empty and from the year the officer has typed meanwhile, never the one they pressed with. Only empty
 * boxes are filled unless the press is a confirmed refill, and then only the boxes the fill gives. A second press while one waits replaces
 * it. Nothing is written once the form has closed (the [scope] is cancelled with it), and nothing is logged.
 */
internal class LeaveBankFiller(
    private val scope: CoroutineScope,
    private val source: LeaveBankSource,
    private val repository: Part2Repository,
    private val rules: Part2Rules,
    private val draftId: String,
    private val event: () -> LeaveEventInput,
    private val edit: ((LeaveEventInput) -> LeaveEventInput) -> Unit,
    private val onChange: () -> Unit,
) {
    private val latest = MutableStateFlow<List<LeaveEntry>?>(null)
    private val said = MutableStateFlow<Map<Int, FillMessage>>(emptyMap())
    private var pending: Job? = null

    /** The statement's rows as the source last gave them, or null while it has not answered. */
    val rows: List<LeaveEntry>? get() = latest.value

    /** What the last press of each section's button did, by occurrence index, until the officer's next edit. */
    val messages: Map<Int, FillMessage> get() = said.value

    /** Starts listening to the source; called once the model is ready to publish. */
    fun start() {
        scope.launch {
            source.rows.collect {
                latest.value = it
                onChange()
            }
        }
    }

    /** The officer edited: what the last press said no longer describes the boxes. */
    fun forget() {
        said.value = emptyMap()
    }

    /** An occurrence was removed: a fill still waiting was pressed on one of them and the places have moved, so it is dropped with what it said. */
    fun cancel() {
        pending?.cancel()
    }

    fun fill(
        index: Int,
        replace: Boolean,
    ) {
        val capability = event().requests.getOrNull(index)?.let { rules.fill(it.code) } ?: return
        pending?.cancel()
        pending =
            scope.launch {
                val statement =
                    latest.value ?: run {
                        say(index, FillMessage.Waiting)
                        latest.filterNotNull().first()
                    }
                val inputs = LeaveBankInputs.of(statement, repository.observeDrafts().first().items, repository.observeOrders().first().items, draftId)
                var message: FillMessage? = null
                edit { current ->
                    val target = current.requests.getOrNull(index)
                    if (target == null || target.code != capability.code) {
                        current
                    } else {
                        when (val outcome = capability.fill(target, inputs)) {
                            FillOutcome.NeedsYear -> current.also { message = FillMessage.NeedsYear }
                            FillOutcome.NothingKnown -> current.also { message = FillMessage.NothingKnown }
                            is FillOutcome.Values -> write(current, index, outcome.values, replace).also { (_, changed) -> message = if (changed == 0) FillMessage.Unchanged else FillMessage.Filled(changed) }.first
                        }
                    }
                }
                message?.let { say(index, it) }
            }
    }

    /** [values] written into request [index]: the empty boxes only, or every one of them on a refill; returns the event and how many boxes changed. */
    private fun write(
        current: LeaveEventInput,
        index: Int,
        values: Map<String, SlotValue>,
        replace: Boolean,
    ): Pair<LeaveEventInput, Int> {
        var next = current
        var changed = 0
        for ((slot, value) in values) {
            if (!replace && slot in next.requests[index].values) continue
            val updated = LeaveEventEditor.setValue(next, index, slot, value, Part2Limits.BREAKUP_TEXT_MAX)
            if (updated != next) {
                next = updated
                changed++
            }
        }
        return next to changed
    }

    private fun say(
        index: Int,
        message: FillMessage,
    ) {
        said.update { it + (index to message) }
        onChange()
    }
}
