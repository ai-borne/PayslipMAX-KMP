package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CalendarDate

/** One saved order in the list: its words are already chosen, [draftId] is the draft it came from (null when it was added from the vault). */
data class VaultRowUi(
    val id: String,
    val draftId: String?,
    val title: String,
    val line: String,
    /** Where cancelling this order stands in words, or null when nobody has tried to cancel it. */
    val note: String? = null,
)

/**
 * The vault list. [hasAny] is true whenever the device holds a readable order (the entry decision needs it); [busy] while a scan is open.
 * [toString] hides every row: an order's number and date must not reach a log through a string template.
 */
data class VaultUiState(
    val rows: List<VaultRowUi>,
    val unreadable: Boolean,
    val hasAny: Boolean,
    val busy: Boolean = false,
) {
    override fun toString(): String = "VaultUiState(rows=${rows.size}, busy=$busy)"

    companion object {
        val Empty = VaultUiState(emptyList(), unreadable = false, hasAny = false)
    }
}

/** Where an order's screen is: asking the repository, showing it, or the repository no longer has it (the level is then cut). */
enum class VaultOrderStatus { LOADING, READY, GONE }

/** A stored page of the order, in order. Its picture is never in the state: it is read, shown and dropped on demand. */
data class PageUi(
    val id: String,
    val number: Int,
    val label: String,
)

/**
 * Everything one order's screen draws. [orderNo] is the text as typed ("" for none); [numberError] is the refusal in words. [draftId] is set
 * only while the draft the order came from still exists. [canAddPages] is false at `Part2Limits.PAGES_PER_ORDER_MAX` pages.
 */
data class VaultOrderUiState(
    val status: VaultOrderStatus,
    val orderNo: String,
    val orderDate: CalendarDate?,
    val numberError: String?,
    val pages: List<PageUi>,
    val canAddPages: Boolean,
    val draftId: String?,
    val saveFailed: Boolean,
    val busy: Boolean = false,
    val linked: Boolean = false,
    /** The order has a number and a real date, so a cancel slip can quote it. */
    val quotable: Boolean = false,
    /** The draft the order came from still exists and is an order to copy, not itself a cancel slip. */
    val redraftable: Boolean = false,
    /** Where cancelling this order stands in words, or null when nobody has tried to cancel it. */
    val cancelNote: String? = null,
) {
    override fun toString(): String = "VaultOrderUiState($status, pages=${pages.size})"

    companion object {
        val Loading = VaultOrderUiState(VaultOrderStatus.LOADING, "", null, null, emptyList(), false, null, false)
        val Gone = VaultOrderUiState(VaultOrderStatus.GONE, "", null, null, emptyList(), false, null, false)
    }
}
