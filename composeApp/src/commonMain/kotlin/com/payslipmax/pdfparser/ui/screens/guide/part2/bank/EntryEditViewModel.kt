package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.EntryForm
import com.payslipmax.pdfparser.part2.domain.EntryInput
import com.payslipmax.pdfparser.part2.domain.EntryProblem
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How a tap on Add or Save ended. */
enum class SaveOutcome { SAVED, INVALID, FULL, FAILED }

/** What the form holds now. [problems] is shown only after an attempt to save, so a fresh form is not covered in red. */
data class EntryEditState(
    val input: EntryInput,
    val problems: Map<Int, EntryProblem> = emptyMap(),
    val saving: Boolean = false,
)

/**
 * One entry form: a new entry for a casualty, or an existing one being edited. What the officer types lives here, in memory only: it is
 * never saved with the Guide stack, never reaches telemetry (no CrashReporter, no Logger) and is dropped by [close] when the app locks.
 * Nothing is written until [save], and only a form that [EntryForm.check] passes is written. Premium is decided by the screen, not here.
 */
class EntryEditViewModel(
    private val repository: Part2Repository,
    val form: EntryForm,
    private val existing: BankEntry?,
    val id: String,
    private val clock: () -> Long,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val blank = EntryInput(null, null, List(Part2Limits.DATA_COLUMNS) { "" }, "")
    private val start = existing?.let { EntryInput(it.from, it.to, it.data, it.note) } ?: blank

    private val _state = MutableStateFlow(EntryEditState(start))
    val state: StateFlow<EntryEditState> = _state

    val isNew: Boolean get() = existing == null

    fun setFrom(date: CalendarDate?) = change { it.copy(from = date) }

    fun setTo(date: CalendarDate?) = change { it.copy(to = date) }

    /** [index] is 0 for Data 1. Over-long text is cut one past the limit, so the form can still say "Too long" without holding a crowd. */
    fun setData(
        index: Int,
        text: String,
    ) = change { input -> input.copy(data = input.data.mapIndexed { i, old -> if (i == index) text.take(Part2Limits.TEXT_MAX + 1) else old }) }

    fun setNote(text: String) = change { it.copy(note = text.take(Part2Limits.TEXT_MAX + 1)) }

    private fun change(edit: (EntryInput) -> EntryInput) {
        _state.update { current ->
            val next = edit(current.input)
            current.copy(input = next, problems = if (current.problems.isEmpty()) current.problems else form.check(next))
        }
    }

    /** Checks the form and, if clean, writes it. [onDone] runs on the caller's thread of control after the write has ended. */
    fun save(onDone: (SaveOutcome) -> Unit) {
        val input = _state.value.input
        val problems = form.check(input)
        if (problems.isNotEmpty()) {
            _state.update { it.copy(problems = problems) }
            return onDone(SaveOutcome.INVALID)
        }
        _state.update { it.copy(saving = true, problems = emptyMap()) }
        scope.launch {
            val now = clock()
            val entry = form.toEntry(input, id, existing?.orderId, existing?.createdAt ?: now, now)
            val outcome =
                try {
                    when (entry?.let { repository.saveEntry(it) }) {
                        Part2WriteResult.SAVED -> SaveOutcome.SAVED
                        Part2WriteResult.TOO_MANY -> SaveOutcome.FULL
                        else -> SaveOutcome.FAILED
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    SaveOutcome.FAILED
                }
            _state.update { it.copy(saving = false) }
            onDone(outcome)
        }
    }

    fun delete(onDone: () -> Unit) {
        scope.launch {
            try {
                repository.deleteEntry(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // The entry stays; the list shows it still there, which is the honest answer.
            }
            onDone()
        }
    }

    /** Drops what was typed (a lock). An existing entry reopens from storage, a new one from blank. */
    fun close() {
        _state.value = EntryEditState(blank)
    }
}
