package com.payslipmax.pdfparser.part2.domain

/** The rules that tie casualties to a scanned order. Pure: callers store the result. */
object OrderLink {
    /** An order covers at least one casualty, so saving with none ticked is refused. */
    fun canSave(selected: Set<String>): Boolean = selected.isNotEmpty()

    fun attach(
        entries: List<BankEntry>,
        ids: Set<String>,
        orderId: String,
        now: Long,
    ): List<BankEntry> = entries.map { if (it.id in ids) it.copy(orderId = orderId, updatedAt = now) else it }

    /** The entries that name [orderId]. A deleted order's entries are not rewritten: [BankStatus] reads them as not attached. */
    fun covered(
        orderId: String,
        entries: List<BankEntry>,
    ): List<BankEntry> = entries.filter { it.orderId == orderId }

    /** One casualty can leave an order only while another stays: to remove the last, delete the order. */
    fun canUnlink(
        orderId: String,
        entries: List<BankEntry>,
    ): Boolean = covered(orderId, entries).size > 1

    fun detach(
        entry: BankEntry,
        now: Long,
    ): BankEntry = entry.copy(orderId = null, updatedAt = now)
}
