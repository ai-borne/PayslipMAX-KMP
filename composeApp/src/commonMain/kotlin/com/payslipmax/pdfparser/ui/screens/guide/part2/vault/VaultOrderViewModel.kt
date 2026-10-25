package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.domain.VaultOrderEditor
import com.payslipmax.pdfparser.part2.domain.VaultPageRef
import com.payslipmax.pdfparser.part2.pages.PageCapture
import com.payslipmax.pdfparser.part2.pages.PageEncoder
import com.payslipmax.pdfparser.part2.pages.PageScanner
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.AutoSaver
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One published order: the number and date the officer types (saved as they go, one write after a pause), its pages, and the pages
 * read back to be shown. A page is decrypted only inside [view], and the bytes are zeroed when that call returns. Scanning more pages
 * reports what happened through [notify] (the vault's notice) and tells the list through [pagesChanged]. Nothing about the order is
 * logged. [close] writes a change still waiting; [delete] drops it.
 */
class VaultOrderViewModel(
    val id: String,
    private val repository: Part2Repository,
    private val encoder: PageEncoder,
    private val notify: (String?) -> Unit,
    private val pagesChanged: () -> Unit,
    private val now: () -> Long,
    private val dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val order = MutableStateFlow<VaultOrder?>(null)
    private val pages = MutableStateFlow<List<VaultPageRef>>(emptyList())
    private val draftId = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private val gone = MutableStateFlow(false)
    private val saver = AutoSaver(scope, dispatcher, { checkNotNull(order.value) }, repository::saveOrder, { repository.deleteOrder(id) }, ::publish)
    private var closed = false

    private val _state = MutableStateFlow(VaultOrderUiState.Loading)
    val state: StateFlow<VaultOrderUiState> = _state

    init {
        scope.launch { load() }
    }

    private suspend fun load() {
        val loaded = repository.order(id)
        if (loaded == null) {
            gone.value = true
            publish()
            return
        }
        pages.value = repository.pages(id)
        draftId.value = loaded.draftId?.takeIf { repository.draft(it) != null }
        order.value = loaded
        publish()
    }

    fun setNumber(raw: String) = edit { VaultOrderEditor.setNumber(it, raw, now()) }

    fun setDate(date: CalendarDate?) = edit { VaultOrderEditor.setDate(it, date, now()) }

    /** Scans more pages into this order through [scanner]; a second call while a scan is open is ignored. */
    fun addPages(scanner: PageScanner) {
        if (closed || order.value == null || !busy.compareAndSet(false, true)) return
        publish()
        scope.launch {
            try {
                notify(VaultNotices.of(PageCapture(scanner, encoder, repository, dispatcher).capture(id)))
                pages.value = repository.pages(id)
                if (repository.order(id) == null) gone.value = true
                pagesChanged()
            } finally {
                busy.value = false
                publish()
            }
        }
    }

    /**
     * Decrypts page [pageId] of this order and hands it to [decode]; the repository zeroes the bytes when [decode] returns, so [decode]
     * must copy what it keeps. Null when the page is not one of this order's, is gone or cannot be read, or [decode] gives nothing.
     */
    suspend fun <T> view(
        pageId: String,
        decode: (ByteArray) -> T?,
    ): T? = if (pages.value.none { it.id == pageId }) null else repository.readPage(pageId, decode)

    /** Deletes the order and its pages; a change still waiting is dropped, never written back. [onDone] runs once it is gone. */
    fun delete(onDone: () -> Unit) {
        closed = true
        saver.delete {
            pagesChanged()
            onDone()
        }
    }

    /** Leaves the screen: a change still waiting is written at once, then the model stops. */
    fun close() {
        if (closed) return
        closed = true
        val last = saver.closing()
        scope.cancel()
        last?.let(saver::writeAfterClose)
    }

    private fun edit(change: (VaultOrder) -> VaultOrder) {
        if (closed) return
        var changed = false
        order.update { current -> current?.let(change)?.also { changed = it != current } ?: current }
        if (!changed) return
        publish()
        saver.changed()
    }

    private fun publish() {
        _state.value = derive()
    }

    private fun derive(): VaultOrderUiState {
        if (gone.value) return VaultOrderUiState.Gone
        val current = order.value ?: return VaultOrderUiState.Loading
        val held = pages.value
        return VaultOrderUiState(
            status = VaultOrderStatus.READY,
            orderNo = current.orderNo.orEmpty(),
            orderDate = current.orderDate,
            numberError = VaultOrderEditor.numberIssue(current)?.let { Part2Strings.fieldIssue(Part2Strings.orderNoLabel, it) },
            pages = held.mapIndexed { i, page -> PageUi(page.id, i + 1, Part2VaultStrings.pageLabel(i + 1)) },
            canAddPages = held.size < Part2Limits.PAGES_PER_ORDER_MAX,
            draftId = draftId.value,
            saveFailed = saver.failed,
            busy = busy.value,
        )
    }
}
