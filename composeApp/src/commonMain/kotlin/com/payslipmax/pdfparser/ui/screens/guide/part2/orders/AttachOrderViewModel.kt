package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.OrderLink
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.PageScanner
import com.payslipmax.pdfparser.part2.pages.PageStore
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The attach flow: scan a paper order, type its number and date, say how many casualties it publishes and which order it quotes, tick the
 * casualties it covers, save. The scanned pages are held here as the scanner's own files or memory and never shown, never saved with the
 * Guide stack, never logged: a process death loses the scan and the typing, nothing else. [close] (and a lock) deletes every held page.
 * Nothing is written until [save], and a save that fails part-way removes the order, so no entry ever reads as attached to nothing.
 * Premium is decided by the screen, not here.
 */
class AttachOrderViewModel(
    private val repository: Part2Repository,
    private val pageStore: PageStore,
    private val clock: () -> Long,
    private val newId: () -> String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _state = MutableStateFlow(AttachState())
    val state: StateFlow<AttachState> = _state
    private var held = emptyList<com.payslipmax.pdfparser.part2.pages.ScannedPage>()

    // Pages the scanner reported dropping, and ones beyond the room that were deleted here: told to the officer at Save, never lost.
    private var dropped = 0
    private var watching: Job? = null

    /** Starts a blank flow. [entryId] (attaching from one casualty) is ticked at once; casualties added during the flow are ticked as they appear. */
    fun begin(entryId: String? = null) {
        close()
        _state.value = AttachState(ticked = setOfNotNull(entryId))
        watching = scope.launch { watchWaiting() }
    }

    /** Starts the flow unless one is running: coming back from the Learn levels opened by "Add a casualty" must not wipe the form. */
    fun ensure(entryId: String? = null) {
        if (watching == null) begin(entryId)
    }

    private suspend fun watchWaiting() {
        var known: Set<String>? = null
        combine(repository.observeEntries(), repository.observeOrders()) { entries, orders ->
            val orderIds = orders.items.map { it.id }.toSet()
            entries.items.filter { BankStatus.of(it, orderIds) == BankStatus.NOT_ATTACHED }
        }.collect { waiting ->
            val ids = waiting.map { it.id }.toSet()
            val fresh = known?.let { ids - it }.orEmpty()
            known = ids
            _state.update { it.copy(choices = waiting, ticked = (it.ticked.filter { id -> id in ids } + fresh).toSet()) }
        }
    }

    fun holdsScan(): Boolean = held.isNotEmpty()

    /** Opens [scanner] for the room the order has left. A cancel is not a notice; a failure is, and keeps whatever was already held. */
    suspend fun scan(scanner: PageScanner) {
        val room = Part2Limits.PAGES_PER_ORDER_MAX - held.size
        if (room <= 0) return _state.update { it.copy(notice = AttachNotice.ORDER_FULL) }
        when (val result = scanner.scan(room)) {
            PageScan.Cancelled -> _state.update { it.copy(notice = null) }
            is PageScan.Failed -> _state.update { it.copy(notice = if (result.reason == ScanFailure.UNAVAILABLE) AttachNotice.SCAN_UNAVAILABLE else AttachNotice.SCAN_FAILED) }
            is PageScan.Pages -> {
                val keep = result.pages.take(room)
                result.pages.drop(room).forEach { it.discard() }
                dropped += result.dropped + (result.pages.size - keep.size)
                held = held + keep
                _state.update { it.copy(pages = held.size, notice = null, problems = it.problems - AttachProblem.NO_PAGES) }
            }
        }
    }

    /** Deletes every page the scanner left; the typed text stays. */
    fun discardScan() {
        held.forEach { it.discard() }
        held = emptyList()
        dropped = 0
        _state.update { it.copy(pages = 0) }
    }

    fun setOrderNo(text: String) = edit { it.copy(orderNo = text.take(Part2Limits.ORDER_NO_MAX + 1)) }

    fun setDate(date: CalendarDate?) = edit { it.copy(date = date) }

    fun setExpected(text: String) = edit { it.copy(expected = text.take(EXPECTED_TEXT_MAX)) }

    fun setLastOrderNo(text: String) = edit { it.copy(lastOrderNo = text.take(Part2Limits.ORDER_NO_MAX + 1)) }

    fun toggle(entryId: String) = edit { it.copy(ticked = if (entryId in it.ticked) it.ticked - entryId else it.ticked + entryId) }

    private fun edit(change: (AttachState) -> AttachState) = _state.update { change(it).let { next -> if (it.problems.isEmpty()) next else next.copy(problems = problemsOf(next)) } }

    private fun problemsOf(s: AttachState): Set<AttachProblem> =
        buildSet {
            if (s.pages == 0) add(AttachProblem.NO_PAGES)
            if (!OrderLink.canSave(s.ticked)) add(AttachProblem.NO_CASUALTY)
            if (s.orderNo.trim().length > Part2Limits.ORDER_NO_MAX) add(AttachProblem.ORDER_NO_LONG)
            if (s.lastOrderNo.trim().length > Part2Limits.ORDER_NO_MAX) add(AttachProblem.LAST_NO_LONG)
            if (expectedOf(s.expected).let { it == BAD }) add(AttachProblem.BAD_COUNT)
        }

    /** The count typed, null when blank, [BAD] when it is not a whole number from 1 to the limit. */
    private fun expectedOf(text: String): Int? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val n = if (t.all { it in '0'..'9' }) t.toIntOrNull() else null
        return if (n != null && n in 1..Part2Limits.EXPECTED_COUNT_MAX) n else BAD
    }

    /** Checks the form and, if clean, writes the order, its pages and the links. [onDone] runs after the write has ended; the scan is gone by then. */
    fun save(onDone: (AttachOutcome) -> Unit) {
        val current = _state.value
        if (current.busy) return
        val problems = problemsOf(current)
        if (problems.isNotEmpty()) {
            _state.update { it.copy(problems = problems) }
            return onDone(AttachOutcome.Invalid)
        }
        _state.update { it.copy(busy = true, problems = emptySet()) }
        scope.launch {
            val outcome = persist(current)
            if (outcome is AttachOutcome.Saved) close() else _state.update { it.copy(busy = false) }
            onDone(outcome)
        }
    }

    private suspend fun persist(s: AttachState): AttachOutcome {
        val now = clock()
        val order = VaultOrder(newId(), s.orderNo.trim().ifEmpty { null }, s.date, now, now, expectedOf(s.expected).takeIf { it != BAD }, s.lastOrderNo.trim().ifEmpty { null })
        var written = false
        return try {
            when (repository.saveOrder(order)) {
                Part2WriteResult.SAVED -> written = true
                Part2WriteResult.TOO_MANY -> return AttachOutcome.BankFull.also { discardScan() }
                else -> return AttachOutcome.Failed.also { discardScan() }
            }
            val scan = PageScan.Pages(held, dropped)
            held = emptyList()
            dropped = 0
            val stored = pageStore.store(order.id, scan)
            if (stored.saved == 0) {
                repository.deleteOrder(order.id)
                return if (stored.refusal == Part2WriteResult.VAULT_FULL) AttachOutcome.VaultFull else AttachOutcome.Failed
            }
            val picked = s.ticked.mapNotNull { repository.entry(it) }
            val linked = OrderLink.attach(picked, picked.map(BankEntry::id).toSet(), order.id, now)
            if (linked.isEmpty() || linked.any { repository.saveEntry(it) != Part2WriteResult.SAVED }) {
                repository.deleteOrder(order.id)
                return AttachOutcome.Failed
            }
            AttachOutcome.Saved(order.id, stored.saved, stored.notSaved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (written) runCatching { repository.deleteOrder(order.id) }
            discardScan()
            AttachOutcome.Failed
        }
    }

    /** Ends the flow: every held page is deleted and what was typed is dropped (also what a lock does). */
    fun close() {
        watching?.cancel()
        watching = null
        discardScan()
        _state.value = AttachState()
    }

    private companion object {
        const val BAD = -1
        const val EXPECTED_TEXT_MAX = 4
    }
}
