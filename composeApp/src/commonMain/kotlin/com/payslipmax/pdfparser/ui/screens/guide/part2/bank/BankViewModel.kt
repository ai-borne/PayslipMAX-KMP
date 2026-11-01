package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.data.LastBackupStorage
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.part2.domain.BankIndex
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.BankView
import com.payslipmax.pdfparser.part2.domain.EntryForm
import com.payslipmax.pdfparser.part2.domain.OrderShelf
import com.payslipmax.pdfparser.part2.domain.ShelfYear
import com.payslipmax.pdfparser.part2.pages.PageStore
import com.payslipmax.pdfparser.rating.currentTimeMillis
import com.payslipmax.pdfparser.ui.UI_STOP_TIMEOUT_MS
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.AttachOrderViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.OrderViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.vaultBytesOrZero
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.utils.localUtcOffsetMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** What the Bank list shows. [loaded] is false until the first read, so an empty Bank is not announced before it is known to be empty. */
data class BankUiState(
    val loaded: Boolean = false,
    val view: BankView = BankView(emptyList(), 0, 0, 0),
    val filter: BankFilter = BankFilter.ALL,
    val unreadable: Int = 0,
    val lastBackup: CalendarDate? = null,
    val shelf: List<ShelfYear> = emptyList(),
    val vaultBytes: Long = 0,
    val attachedTo: Map<String, String> = emptyMap(),
)

/**
 * App-scoped (a Koin single, like the Learn model). The list is read from the encrypted store only while a screen collects it. Status and
 * age are computed, never stored: [BankIndex] decides both from the orders that exist now and the day [today] gives. It tells telemetry
 * nothing (no CrashReporter, no Logger), and an entry's code, dates and note stay in memory and in the store only.
 */
class BankViewModel
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val repository: Part2Repository,
        private val formFor: (String) -> EntryForm?,
        private val nameOf: (String) -> String?,
        private val lastBackup: LastBackupStorage,
        private val pageStore: PageStore,
        private val today: () -> CalendarDate = { systemToday() },
        private val clock: () -> Long = { currentTimeMillis() },
        private val newId: () -> String = { Uuid.random().toString() },
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + dispatcher)
        private val filter = MutableStateFlow(BankFilter.ALL)
        private val dispatcherInUse = dispatcher

        // The casualty a not-yet-saved entry is for, by the id it will be saved under. Codes only, never typed text.
        private val pending = mutableMapOf<String, String>()
        private var editor: EntryEditViewModel? = null
        private var order: OrderViewModel? = null

        /** The attach flow: one at a time, closed (its scan deleted) by [discardForms]. */
        val attach = AttachOrderViewModel(repository, pageStore, clock, newId, dispatcher)

        /** The model for scanned order [id], kept while it stays the open one. */
        fun orderModel(id: String): OrderViewModel = order?.takeIf { it.orderId == id } ?: OrderViewModel(repository, pageStore, id, dispatcherInUse, clock).also { order = it }

        val state: StateFlow<BankUiState> =
            combine(
                repository.observeEntries().catch { emit(com.payslipmax.pdfparser.part2.data.StoredPart2(emptyList())) },
                repository.observeOrders().catch { emit(com.payslipmax.pdfparser.part2.data.StoredPart2(emptyList())) },
                filter,
            ) { entries, orders, chosen ->
                BankUiState(
                    loaded = true,
                    view = BankIndex.view(entries.items, orders.items.map { it.id }.toSet(), chosen, today()),
                    filter = chosen,
                    unreadable = entries.unreadable + orders.unreadable,
                    lastBackup = lastBackup.load()?.let(::localDate),
                    shelf = OrderShelf.view(orders.items, entries.items),
                    vaultBytes = repository.vaultBytesOrZero(),
                    attachedTo = attachments(entries.items, orders.items.map { it.id }.toSet()),
                )
            }.stateIn(scope, SharingStarted.WhileSubscribed(UI_STOP_TIMEOUT_MS), BankUiState(filter = BankFilter.ALL))

        fun setFilter(next: BankFilter) {
            filter.value = next
        }

        /** The manual's name for a casualty, or null when the loaded bundle does not know it. */
        fun nameOf(code: String): String? = nameOf.invoke(code)

        /** Starts a new entry for [code] and returns the id it will be saved under; an unknown code starts nothing (null). */
        fun beginNew(code: String): String? {
            if (formFor(code) == null) return null
            return newId().also { pending[it] = code }
        }

        /** True for an id that [beginNew] made and nobody has saved: the saved Guide stack leaves such a level out. */
        fun isPending(id: String): Boolean = id in pending

        /** The form for [id]: a new entry's blank one, or the stored entry's. Null when the entry is no longer here (the level is then closed). */
        suspend fun openEditor(id: String): EntryEditViewModel? {
            editor?.takeIf { it.id == id }?.let { return it }
            val code = pending[id]
            val existing = if (code == null) repository.entry(id) else null
            val form = formFor(code ?: existing?.code ?: return null) ?: return null
            return EntryEditViewModel(repository, form, existing, id, clock, dispatcherInUse).also {
                editor?.close()
                editor = it
            }
        }

        /** A saved entry is no longer pending. */
        fun markSaved(id: String) {
            pending.remove(id)
        }

        /** Drops the open form (the lock): what was typed is gone; a new entry keeps only its casualty, so unlocking shows a blank form. A held scan is deleted. */
        fun discardForms() {
            editor?.close()
            editor = null
            attach.close()
        }

        /** Whether the store still holds [id]; the Guide stack is cut at an entry it does not (a restore, a delete elsewhere). */
        suspend fun exists(id: String): Boolean = id in pending || repository.entry(id) != null

        fun delete(
            id: String,
            onDone: () -> Unit = {},
        ) {
            scope.launch {
                try {
                    repository.deleteEntry(id)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Left in place; the list still shows it.
                }
                pending.remove(id)
                onDone()
            }
        }

        /** Entry id to order id, for the casualties whose order still exists: the one place that answers "is it attached" for a screen that has no chip. */
        private fun attachments(
            entries: List<BankEntry>,
            orderIds: Set<String>,
        ): Map<String, String> = entries.filter { BankStatus.of(it, orderIds) == BankStatus.ATTACHED }.associate { it.id to it.orderId!! }

        private fun localDate(epochMillis: Long): CalendarDate = PreClaimDates.fromUtcMillis(epochMillis + localUtcOffsetMillis(epochMillis))
    }

internal fun systemToday(): CalendarDate {
    val now = currentTimeMillis()
    return PreClaimDates.fromUtcMillis(now + localUtcOffsetMillis(now))
}
