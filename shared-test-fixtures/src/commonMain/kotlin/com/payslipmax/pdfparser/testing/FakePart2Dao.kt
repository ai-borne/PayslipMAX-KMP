package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.data.Part2Dao
import com.payslipmax.pdfparser.part2.data.Part2EntryEntity
import com.payslipmax.pdfparser.part2.data.Part2OrderEntity
import com.payslipmax.pdfparser.part2.data.Part2PageEntity
import com.payslipmax.pdfparser.part2.domain.VaultPageRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [Part2Dao]. The `@Transaction` cap checks are the interface's own default bodies, so they run here as in Room.
 * [FakePayslipDao] shares one of these, so the bulk restore writes and the per-row reads see the same rows, as the real tables do.
 */
class FakePart2Dao(
    /** False makes bulk page writes count their bytes and drop them, so a test can restore a vault larger than its heap. */
    private val keepPageBytes: Boolean = true,
) : Part2Dao {
    /** What the bulk writes saw when [keepPageBytes] is false: every page, and the bytes it carried. */
    var droppedPages = 0
        private set
    var droppedBytes = 0L
        private set

    private val entries = MutableStateFlow<Map<String, Part2EntryEntity>>(emptyMap())
    private val orders = MutableStateFlow<Map<String, Part2OrderEntity>>(emptyMap())
    private val pages = MutableStateFlow<Map<String, Part2PageEntity>>(emptyMap())

    override suspend fun upsertEntry(row: Part2EntryEntity) {
        entries.value = entries.value + (row.id to row)
    }

    override suspend fun entry(id: String): Part2EntryEntity? = entries.value[id]

    override fun observeEntries(): Flow<List<Part2EntryEntity>> = entries.map { it.values.toList() }

    override suspend fun allEntries(): List<Part2EntryEntity> = entries.value.values.toList()

    override suspend fun entryCount(): Int = entries.value.size

    override suspend fun deleteEntry(id: String) {
        entries.value = entries.value - id
    }

    override suspend fun upsertOrder(row: Part2OrderEntity) {
        orders.value = orders.value + (row.id to row)
    }

    override suspend fun order(id: String): Part2OrderEntity? = orders.value[id]

    override fun observeOrders(): Flow<List<Part2OrderEntity>> = orders.map { it.values.toList() }

    override suspend fun allOrders(): List<Part2OrderEntity> = orders.value.values.toList()

    override suspend fun orderCount(): Int = orders.value.size

    override suspend fun deleteOrder(id: String) {
        orders.value = orders.value - id
    }

    override suspend fun upsertPage(row: Part2PageEntity) {
        pages.value = pages.value + (row.id to row)
    }

    override suspend fun page(id: String): Part2PageEntity? = pages.value[id]

    override suspend fun pageRefs(orderId: String): List<VaultPageRef> = allPageRefs().filter { it.orderId == orderId }

    override suspend fun allPageRefs(): List<VaultPageRef> =
        pages.value.values
            .map { VaultPageRef(it.id, it.orderId, it.position) }
            .sortedWith(compareBy({ it.orderId }, { it.position }))

    override suspend fun nextPosition(orderId: String): Int = (pages.value.values.filter { it.orderId == orderId }.maxOfOrNull { it.position } ?: -1) + 1

    override suspend fun vaultBytes(): Long = pages.value.values.sumOf { it.ciphertext.size.toLong() }

    override fun observeVaultBytes(): Flow<Long> = pages.map { rows -> rows.values.sumOf { it.ciphertext.size.toLong() } }

    override suspend fun deletePage(id: String) {
        pages.value = pages.value - id
    }

    override suspend fun deletePagesOf(orderId: String) {
        pages.value = pages.value.filterValues { it.orderId != orderId }
    }

    // The bulk writes and clears PayslipDao owns in Room (one transaction with the rest of a restore); FakePayslipDao delegates here.
    fun putAll(
        newEntries: List<Part2EntryEntity>,
        newOrders: List<Part2OrderEntity>,
        newPages: List<Part2PageEntity>,
    ) {
        entries.value = entries.value + newEntries.associateBy { it.id }
        orders.value = orders.value + newOrders.associateBy { it.id }
        if (keepPageBytes) {
            pages.value = pages.value + newPages.associateBy { it.id }
        } else {
            droppedPages += newPages.size
            droppedBytes += newPages.sumOf { it.ciphertext.size.toLong() }
        }
    }

    fun clearEntries() {
        entries.value = emptyMap()
    }

    fun clearOrders() {
        orders.value = emptyMap()
    }

    fun clearPages() {
        pages.value = emptyMap()
    }
}
