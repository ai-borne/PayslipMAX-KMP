package com.payslipmax.pdfparser.part2.domain

/**
 * Whether a scanned order copy is attached to an entry. Derived, never stored, and decided only here: an entry is [ATTACHED] only while the
 * order it names still exists, so a deleted order sends its entries back to [NOT_ATTACHED] with no write to them.
 */
enum class BankStatus {
    NOT_ATTACHED,
    ATTACHED,
    ;

    companion object {
        fun of(
            entry: BankEntry,
            orderIds: Set<String>,
        ): BankStatus = if (entry.orderId != null && entry.orderId in orderIds) ATTACHED else NOT_ATTACHED
    }
}
