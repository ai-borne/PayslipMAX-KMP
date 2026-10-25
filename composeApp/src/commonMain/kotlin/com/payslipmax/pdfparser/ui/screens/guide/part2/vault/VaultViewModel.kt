package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.domain.VaultOrderEditor
import com.payslipmax.pdfparser.part2.pages.CaptureResult
import com.payslipmax.pdfparser.part2.pages.PageCapture
import com.payslipmax.pdfparser.part2.pages.PageEncoder
import com.payslipmax.pdfparser.part2.pages.PageScanner
import com.payslipmax.pdfparser.rating.currentTimeMillis
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import com.payslipmax.pdfparser.utils.localUtcOffsetMillis
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The officer's vault of published orders: the list the Part II home shows, and the one place a new order is scanned in. A scan creates
 * the order first (the pages need somewhere to go) and keeps it only if at least one page was saved, so a cancelled or failed scan leaves
 * nothing behind; an order a killed process left empty is removed when the vault next opens. Everything goes through [Part2Repository]
 * (encrypted); nothing about an order is logged or reported. Who may scan is decided by the screens, not here (a lapsed officer can still
 * view and delete, Round 13).
 */
class VaultViewModel
    @OptIn(ExperimentalUuidApi::class)
    constructor(
        private val repository: Part2Repository,
        private val encoder: PageEncoder,
        private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
        private val now: () -> Long = ::currentTimeMillis,
        private val newId: () -> String = { Uuid.random().toString() },
        private val localOffset: (atMillis: Long) -> Long = ::localUtcOffsetMillis,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + dispatcher)
        private val scanning = MutableStateFlow(false)

        // Page changes do not touch the order's row, so the count is recomputed when the order screen says they happened.
        private val pagesChanged = MutableStateFlow(0)
        private val _state = MutableStateFlow(VaultUiState.Empty)
        private val _notice = MutableStateFlow<String?>(null)

        val state: StateFlow<VaultUiState> = _state

        /** What the last scan said, until the officer dismisses it or starts another; null when there is nothing to say. */
        val notice: StateFlow<String?> = _notice

        init {
            scope.launch {
                combine(repository.observeOrders(), repository.observeDrafts(), pagesChanged) { stored, drafts, _ -> stored to drafts.items }.collectLatest { (stored, drafts) ->
                    val rows = stored.items.map { row(it, repository.pages(it.id).size, CancelNotes.of(it, drafts, stored.items, localOffset)) }
                    _state.update { it.copy(rows = rows, unreadable = stored.unreadable > 0, hasAny = stored.items.isNotEmpty()) }
                }
            }
        }

        /** Shows [text] as the vault's notice (a cancel that could not start says why here); null clears it. */
        fun say(text: String?) {
            _notice.value = text
        }

        fun dismissNotice() {
            _notice.value = null
        }

        /**
         * Scans a new order in through [scanner], linked to [draftId] when it was added from a draft's slip. [onSaved] is called with the
         * order's id once at least one page is stored (on this model's dispatcher: a screen hops to its own before navigating). A second call
         * while a scan is open is ignored.
         */
        fun addOrder(
            draftId: String?,
            scanner: PageScanner,
            onSaved: (orderId: String) -> Unit,
        ) {
            if (!scanning.compareAndSet(false, true)) return
            _notice.value = null
            _state.update { it.copy(busy = true) }
            scope.launch {
                try {
                    scanNew(draftId, scanner, onSaved)
                } finally {
                    scanning.value = false
                    _state.update { it.copy(busy = false) }
                }
            }
        }

        private suspend fun scanNew(
            draftId: String?,
            scanner: PageScanner,
            onSaved: (String) -> Unit,
        ) {
            val order = VaultOrderEditor.blank(newId(), draftId, now())
            if (repository.saveOrder(order) != Part2WriteResult.SAVED) {
                _notice.value = Part2VaultStrings.saveFailed
                return
            }
            val result = PageCapture(scanner, encoder, repository, dispatcher).capture(order.id)
            if (((result as? CaptureResult.Saved)?.saved ?: 0) == 0) repository.deleteOrder(order.id) else onSaved(order.id)
            _notice.value = VaultNotices.of(result)
            pagesChanged.update { it + 1 }
        }

        fun delete(id: String) {
            scope.launch { repository.deleteOrder(id) }
        }

        /** The model for one order's screen. The caller closes it when it leaves. */
        fun open(orderId: String): VaultOrderViewModel =
            VaultOrderViewModel(orderId, repository, encoder, { _notice.value = it }, { pagesChanged.update { n -> n + 1 } }, now, dispatcher, localOffset)

        /**
         * The vault opened: clears what a killed scan left (a page in the scanner's cache, an order with no page and nothing typed). Skipped
         * while a scan of this model is open, which owns both.
         */
        fun opened(scanner: PageScanner) {
            if (scanning.value) return
            scanner.discardLeftovers()
            scope.launch { removeGhosts() }
        }

        private suspend fun removeGhosts() {
            for (order in repository.observeOrders().first().items) {
                if (scanning.value) return
                if (order.orderNo == null && order.orderDate == null && repository.pages(order.id).isEmpty()) repository.deleteOrder(order.id)
            }
        }

        private fun row(
            order: VaultOrder,
            pages: Int,
            note: String?,
        ): VaultRowUi {
            val added = CivilCalendar.format(PreClaimDates.localDay(order.createdAt, localOffset))
            return VaultRowUi(order.id, order.draftId, title(order), Part2VaultStrings.orderRowLine(pages, added), note)
        }

        private fun title(order: VaultOrder): String {
            val no = order.orderNo ?: return Part2VaultStrings.numberNotTyped
            val date = order.orderDate ?: return Part2VaultStrings.orderNumberOnly(no)
            return Part2VaultStrings.orderTitle(no, CivilCalendar.format(date))
        }
    }
