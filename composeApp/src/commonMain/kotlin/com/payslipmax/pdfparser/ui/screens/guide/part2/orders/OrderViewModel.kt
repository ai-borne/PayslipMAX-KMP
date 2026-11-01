package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.ChainCheck
import com.payslipmax.pdfparser.part2.domain.ChainState
import com.payslipmax.pdfparser.part2.domain.OrderCompleteness
import com.payslipmax.pdfparser.part2.domain.OrderLink
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.domain.VaultPageRef
import com.payslipmax.pdfparser.part2.pages.CaptureResult
import com.payslipmax.pdfparser.part2.pages.PageCapture
import com.payslipmax.pdfparser.part2.pages.PageScanner
import com.payslipmax.pdfparser.part2.pages.PageStore
import com.payslipmax.pdfparser.rating.currentTimeMillis
import com.payslipmax.pdfparser.ui.UI_STOP_TIMEOUT_MS
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the order screen shows. [capture] is how the last "Add pages" ended, for the notice; counts only. */
data class OrderState(
    val loaded: Boolean = false,
    val order: VaultOrder? = null,
    val pages: List<VaultPageRef> = emptyList(),
    val casualties: List<BankEntry> = emptyList(),
    val completeness: OrderCompleteness = OrderCompleteness(0, null),
    val chain: ChainState = ChainState.NO_QUOTE,
    val canUnlink: Boolean = false,
    val capture: CaptureResult? = null,
) {
    override fun toString(): String = "OrderState(pages=${pages.size}, casualties=${casualties.size})"
}

/**
 * One scanned order: its pages, the casualties on it, the counter and the chain check. Pages are read only through [page], which zeroes
 * the bytes when the caller is done, and nothing about an order, number or page reaches a log. Deleting the order deletes its pages and
 * never rewrites an entry: [com.payslipmax.pdfparser.part2.domain.BankStatus] reads a casualty of a missing order as not attached.
 */
class OrderViewModel(
    private val repository: Part2Repository,
    private val pageStore: PageStore,
    val orderId: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = { currentTimeMillis() },
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val pageVersion = MutableStateFlow(0)
    private val capture = MutableStateFlow<CaptureResult?>(null)

    val state: StateFlow<OrderState> =
        combine(
            repository.observeOrders().catch { emit(com.payslipmax.pdfparser.part2.data.StoredPart2(emptyList())) },
            repository.observeEntries().catch { emit(com.payslipmax.pdfparser.part2.data.StoredPart2(emptyList())) },
            pageVersion,
            capture,
        ) { orders, entries, _, lastCapture ->
            val order = orders.items.firstOrNull { it.id == orderId }
            val covered = OrderLink.covered(orderId, entries.items)
            OrderState(
                loaded = true,
                order = order,
                pages = if (order == null) emptyList() else repository.pages(orderId),
                casualties = covered,
                completeness = order?.let { OrderCompleteness.of(it, entries.items) } ?: OrderCompleteness(covered.size, null),
                chain = order?.let { ChainCheck.of(it, orders.items) } ?: ChainState.NO_QUOTE,
                canUnlink = OrderLink.canUnlink(orderId, entries.items),
                capture = lastCapture,
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(UI_STOP_TIMEOUT_MS), OrderState())

    /** Shows how the attach flow ended (a dropped page), on the order it created. */
    fun note(result: CaptureResult?) {
        capture.value = result
    }

    /** Scans for the room the order has left and stores what comes back. A cancel leaves nothing and says nothing. */
    suspend fun addPages(scanner: PageScanner) {
        capture.value = null
        val result = PageCapture(scanner, pageStore, repository).capture(orderId)
        capture.value = result.takeUnless { it == CaptureResult.Cancelled }
        pageVersion.update { it + 1 }
    }

    /** Hands the decrypted page to [use] and overwrites it afterwards; null when the page is gone or cannot be read. */
    suspend fun <T> page(
        pageId: String,
        use: (ByteArray) -> T,
    ): T? = repository.readPage(pageId, use)

    fun deletePage(pageId: String) {
        scope.launch {
            quietly { repository.deletePage(pageId) }
            pageVersion.update { it + 1 }
        }
    }

    fun deleteOrder(onDone: () -> Unit) {
        scope.launch {
            quietly { repository.deleteOrder(orderId) }
            onDone()
        }
    }

    /** Takes one casualty off the order; refused while it is the last one. */
    fun unlink(entryId: String) {
        scope.launch {
            val entries = repository.observeEntries().first().items
            val entry = entries.firstOrNull { it.id == entryId && it.orderId == orderId }
            if (entry != null && OrderLink.canUnlink(orderId, entries)) quietly { repository.saveEntry(OrderLink.detach(entry, clock())) }
        }
    }

    private suspend fun quietly(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The store keeps what it had; the screen shows that honestly.
        }
    }
}
