package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.data.AddedPage
import com.payslipmax.pdfparser.part2.data.Part2Bounds
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.data.StoredPart2
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.domain.VaultPageRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [Part2Repository] with the same caps as the Room one (they are `Part2Limits`'); nothing is encrypted. Run against the same
 * contract test as the Room repository, so the two cannot drift.
 */
class FakePart2Repository(
    private val vaultBytesMax: Long = Part2Limits.VAULT_BYTES_MAX,
) : Part2Repository {
    private var counter = 1
    private val drafts = MutableStateFlow<Map<String, Part2Draft>>(emptyMap())
    private val orders = MutableStateFlow<Map<String, VaultOrder>>(emptyMap())
    private val pages = MutableStateFlow<Map<String, Pair<VaultPageRef, ByteArray>>>(emptyMap())

    override fun observeDrafts(): Flow<StoredPart2<Part2Draft>> = drafts.map { StoredPart2(it.values.sortedByDescending(Part2Draft::updatedAt)) }

    override suspend fun draft(id: String): Part2Draft? = drafts.value[id]

    override suspend fun saveDraft(draft: Part2Draft): Part2WriteResult {
        Part2Bounds.check(draft)?.let { return it }
        if (draft.id !in drafts.value && drafts.value.size >= Part2Limits.DRAFTS_MAX) return Part2WriteResult.TOO_MANY
        drafts.value = drafts.value + (draft.id to draft)
        return Part2WriteResult.SAVED
    }

    override suspend fun deleteDraft(id: String) {
        drafts.value = drafts.value - id
    }

    override fun observeOrders(): Flow<StoredPart2<VaultOrder>> = orders.map { StoredPart2(it.values.sortedByDescending(VaultOrder::updatedAt)) }

    override suspend fun saveOrder(order: VaultOrder): Part2WriteResult {
        Part2Bounds.check(order)?.let { return it }
        if (order.id !in orders.value && orders.value.size >= Part2Limits.ORDERS_MAX) return Part2WriteResult.TOO_MANY
        orders.value = orders.value + (order.id to order)
        return Part2WriteResult.SAVED
    }

    override suspend fun deleteOrder(id: String) {
        orders.value = orders.value - id
        pages.value = pages.value.filterValues { it.first.orderId != id }
    }

    override suspend fun pages(orderId: String): List<VaultPageRef> = pages.value.values.map { it.first }.filter { it.orderId == orderId }.sortedBy { it.position }

    override suspend fun addPage(
        orderId: String,
        jpeg: ByteArray,
    ): AddedPage {
        val held = pages(orderId)
        val result =
            when {
                jpeg.isEmpty() || jpeg.size > Part2Limits.PAGE_MAX_BYTES -> Part2WriteResult.TOO_LARGE
                orderId !in orders.value -> Part2WriteResult.NO_SUCH_ORDER
                held.size >= Part2Limits.PAGES_PER_ORDER_MAX -> Part2WriteResult.TOO_MANY
                vaultBytes() + jpeg.size > vaultBytesMax -> Part2WriteResult.VAULT_FULL
                else -> Part2WriteResult.SAVED
            }
        if (result != Part2WriteResult.SAVED) return AddedPage(result)
        val ref = VaultPageRef("page-${counter++}", orderId, (held.maxOfOrNull { it.position } ?: -1) + 1)
        pages.value = pages.value + (ref.id to Pair(ref, jpeg.copyOf()))
        return AddedPage(Part2WriteResult.SAVED, ref.id)
    }

    override suspend fun <T> readPage(
        pageId: String,
        use: (ByteArray) -> T,
    ): T? {
        val copy = pages.value[pageId]?.second?.copyOf() ?: return null
        return try {
            use(copy)
        } finally {
            copy.fill(0)
        }
    }

    override suspend fun deletePage(pageId: String) {
        pages.value = pages.value - pageId
    }

    override suspend fun vaultBytes(): Long = pages.value.values.sumOf { it.second.size.toLong() }
}
