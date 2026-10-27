package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.AnswerQuestion
import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.VaultOrders
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankSource
import com.payslipmax.pdfparser.part2.print.SlipPrinter
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimInput
import com.payslipmax.pdfparser.utils.localUtcOffsetMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One leave draft: the form that edits it and the slip preview that shows it. A new draft starts from [start]; an existing one passes
 * null and is read from the repository by [id], so a draft restored after process death looks exactly as it did. The officer's answers
 * are saved as they go, from the first change (one write after a pause, never one per key), only through [Part2Repository], so they
 * are encrypted at rest and a lock or process death loses at most the last pause. Closing flushes a pending change; deleting cancels it.
 *
 * The screens read [state] and nothing else: the model holds the answers and `LeaveDraftStateBuilder` derives every sentence and slip
 * row from them, so no message is built in a composable. The model takes no payslip and no platform type, has no telemetry
 * (nothing typed here is logged, reported or put in an exception), and its answers never leave the repository.
 */
class LeaveDraftViewModel(
    val id: String,
    section: Part2Lookup,
    private val repository: Part2Repository,
    start: LeaveEventInput?,
    rules: Part2Rules = Part2Rules.shipped(),
    private val now: () -> Long = { 0L },
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    leaveBank: LeaveBankSource? = null,
    private val localOffset: (atMillis: Long) -> Long = ::localUtcOffsetMillis,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val builder = LeaveDraftStateBuilder(section, rules, localOffset)
    private val saver = AutoSaver(scope, dispatcher, ::pendingDraft, repository::saveDraft, { repository.deleteDraft(id) }, ::publish)
    private val areaPrefill = AreaPrefill(scope, repository, id, { event }, builder::fieldIds, ::edit)

    // The Leave Bank fill (ACCLVE, ENCASH), present only where the Leave Bank is on: the rule families decide which codes it serves.
    private val filler = leaveBank?.let { LeaveBankFiller(scope, it, repository, rules, id, { event }, ::edit) { if (loaded) publish() } }

    // Atomic, because the officer's edits (main thread) and a prefill (background) both change it.
    private val answers = MutableStateFlow(start ?: LeaveEventInput(emptyList()))
    private val event: LeaveEventInput get() = answers.value
    private var loaded = start != null
    private var createdAt: Long? = null

    // The draft this one was made with (a cancel slip and its corrected draft): kept through every save, offered while the partner exists.
    private var pairedWith: String? = null
    private var partner: String? = null

    // The reminders the officer dismissed (kept through every save; a completed print clears them, as it starts the reminders afresh).
    private var dismissed: Set<String> = emptySet()
    private var lastOrder: OrderRef? = null
    private var grants: List<OrderRef> = emptyList()
    private var acked: Set<String> = emptySet()
    private var closed = false

    // What the clerk was last handed. Kept through every edit (an edit is flagged against it, never erases it) and saved with the draft.
    private var printed: PrintedSnapshot? = null
    private val prints = SlipPrintSession(scope, now, ::publish, ::record)

    private val _state = MutableStateFlow(if (loaded) derive() else LeaveDraftUiState.Loading)
    val state: StateFlow<LeaveDraftUiState> = _state

    init {
        filler?.start()
        if (!loaded) scope.launch { load() }
        scope.launch {
            repository.observeDrafts().collect { stored ->
                partner = pairedWith?.takeIf { id -> stored.items.any { it.id == id } }
                if (loaded) publish()
            }
        }
        scope.launch {
            repository.observeOrders().collect {
                grants = VaultOrders.quotable(it.items)
                lastOrder = VaultOrders.lastOrder(it.items)
                if (loaded) publish()
            }
        }
    }

    private suspend fun load() {
        val draft = repository.draft(id)
        if (draft == null) {
            _state.value = LeaveDraftUiState.Gone
            return
        }
        answers.value = draft.event
        createdAt = draft.createdAt
        printed = draft.printed
        pairedWith = draft.pairedWith
        dismissed = draft.dismissed
        partner = draft.pairedWith?.takeIf { repository.draft(it) != null }
        loaded = true
        publish()
    }

    // ---- the officer's answers: each goes through the editor, so `Part2Limits` holds ----

    fun setDate(
        index: Int,
        slot: String,
        date: CalendarDate?,
    ) = edit { LeaveEventEditor.setValue(it, index, slot, date?.let(SlotValue::DateValue)) }

    fun setNumber(
        index: Int,
        slot: String,
        raw: String,
    ) {
        val digits = PreClaimInput.digits(raw, Part2Limits.NUMBER_MAX.toLong())
        edit { LeaveEventEditor.setValue(it, index, slot, digits.toIntOrNull()?.let(SlotValue::NumberValue)) }
    }

    fun setText(
        index: Int,
        slot: String,
        raw: String,
    ) = edit { LeaveEventEditor.setValue(it, index, slot, raw.takeIf { text -> text.isNotEmpty() }?.let(SlotValue::TextValue)) }

    /** A year-by-year list (the encashment's break-up) keeps the longer limit storage and the reader use, where an ordinary text stops at `Part2Limits.TEXT_MAX`. */
    fun setYears(
        index: Int,
        slot: String,
        raw: String,
    ) = edit { LeaveEventEditor.setValue(it, index, slot, raw.takeIf { text -> text.isNotEmpty() }?.let(SlotValue::TextValue), Part2Limits.BREAKUP_TEXT_MAX) }

    /** An order number and its date are set together; with neither, the answer is removed. A date not chosen yet is named as such by the engine. */
    fun setReference(
        index: Int,
        slot: String,
        number: String,
        date: CalendarDate?,
    ) {
        val ref = if (number.isBlank() && date == null) null else SlotValue.RefValue(OrderRef(number, date ?: LeaveEventEditor.UNSET_DATE))
        edit { LeaveEventEditor.setValue(it, index, slot, ref) }
    }

    fun setYesNo(
        index: Int,
        question: String,
        yes: Boolean,
    ) {
        val asked = askedOf<AnswerQuestion.YesNo>(index, question) ?: return
        edit { LeaveEventEditor.setYesNo(it, index, asked.answer, yes) }
    }

    fun choose(
        index: Int,
        question: String,
        option: String?,
    ) {
        val asked = askedOf<AnswerQuestion.OneOf>(index, question) ?: return
        if (option != null && option !in asked.options) return
        val before = event.requests.getOrNull(index)?.let(builder::fieldIds).orEmpty()
        edit { LeaveEventEditor.chooseOne(it, index, asked.prefix, option) }
        if (option != null) areaPrefill.after(index, asked.prefix + option, before)
    }

    /** Fills the boxes of occurrence [index] from the Leave Bank and the officer's printed drafts: only the empty ones, unless [replace] (a refill the officer confirmed). */
    fun fillFromLeaveBank(
        index: Int,
        replace: Boolean,
    ) {
        filler?.fill(index, replace)
    }

    fun addOccurrence(code: String) {
        edit { if (code in builder.offered(it)) LeaveEventEditor.addRequest(it, code) else it }
    }

    fun removeOccurrence(index: Int) = edit { LeaveEventEditor.removeRequest(it, index) }

    /** Ticks a finding the officer has read. Not an edit: ticks are never saved, and any edit to the draft clears them. */
    fun acknowledge(
        key: String,
        ticked: Boolean,
    ) {
        if (closed || !loaded) return
        acked = if (ticked) acked + key else acked - key
        publish()
    }

    /** The print just completed: kept and saved at once, unless the officer has already left or deleted the draft. */
    private suspend fun record(snapshot: PrintedSnapshot) {
        if (closed) return
        printed = snapshot
        dismissed = emptySet()
        publish()
        saver.saveAtOnce()
    }

    /** Prints the slip through [printer]; the print is recorded only once the printer reports it completed (see [SlipPrintSession]). */
    fun print(printer: SlipPrinter) {
        val request = builder.printRequest(_state.value, event.copy(lastOrder = lastOrder))
        if (!closed && request != null) prints.start(printer, request)
    }

    // ---- life cycle ----

    /** Deletes the draft for good; a change still waiting to be saved is dropped, never written back. [onDone] runs once it is gone. */
    fun delete(onDone: () -> Unit) {
        closed = true
        saver.delete(onDone)
    }

    /** Leaves the form: a change still waiting is written at once, then the model stops. */
    fun close() {
        if (closed) return
        closed = true
        val last = saver.closing()
        scope.cancel()
        last?.let(saver::writeAfterClose)
    }

    // ---- internals ----

    private inline fun <reified Q : AnswerQuestion> askedOf(
        index: Int,
        question: String,
    ): Q? {
        val code = event.requests.getOrNull(index)?.code ?: return null
        return builder.questionsFor(code).firstOrNull { it.id == question } as? Q
    }

    private fun edit(change: (LeaveEventInput) -> LeaveEventInput) {
        if (closed || !loaded) return
        var changed = false
        val before = event.requests.size
        answers.update { current -> change(current).also { changed = it != current } }
        if (!changed) return
        acked = emptySet()
        filler?.forget()
        if (event.requests.size < before) filler?.cancel()
        publish()
        saver.changed()
    }

    private fun pendingDraft(): Part2Draft {
        val stamp = now()
        val created = createdAt ?: stamp.also { createdAt = it }
        val saved = event.copy(lastOrder = lastOrder)
        return Part2Draft(id, saved, builder.templateRevs(saved), created, stamp, printed, pairedWith, dismissed)
    }

    private fun derive() = builder.build(event.copy(lastOrder = lastOrder), acked, saver.failed, printed, prints.printing, grants, today(), partner, filler?.let { LeaveBankView(it.rows.orEmpty(), it.messages) })

    /** The day where the officer is, which the checks that compare with it (a time bar) read; the engine itself holds no clock. */
    private fun today(): CalendarDate = PreClaimDates.localDay(now(), localOffset)

    private fun publish() {
        _state.value = derive()
    }

    companion object {
        /** The pause after the last change before the draft is written (see [AutoSaver]). */
        const val AUTOSAVE_DELAY_MS = AutoSaver.PAUSE_MS
    }
}
