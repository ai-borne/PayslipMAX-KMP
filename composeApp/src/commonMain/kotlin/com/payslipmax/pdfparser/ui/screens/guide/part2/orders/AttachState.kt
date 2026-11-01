package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankEntry

/** What is wrong with the attach form; shown only after a Save that was refused, so a fresh form is not covered in red. */
enum class AttachProblem { NO_PAGES, NO_CASUALTY, ORDER_NO_LONG, LAST_NO_LONG, BAD_COUNT }

/** Why the last scan produced nothing to show; the words are UI copy. A cancel is not a notice. */
enum class AttachNotice { SCAN_UNAVAILABLE, SCAN_FAILED, ORDER_FULL }

/** How Save ended. [Saved.notSaved] counts pages the order did not keep (an unreadable page, a full vault). */
sealed interface AttachOutcome {
    class Saved(val orderId: String, val saved: Int, val notSaved: Int) : AttachOutcome {
        override fun toString(): String = "Saved($saved, notSaved=$notSaved)"
    }

    data object Invalid : AttachOutcome

    data object BankFull : AttachOutcome

    data object VaultFull : AttachOutcome

    data object Failed : AttachOutcome
}

/**
 * The attach form. What is typed lives here, in memory only; the scanned pages are not here at all (the view model holds them and shows
 * only [pages], their count). [choices] are the casualties still waiting for an order; [ticked] are the ones this order will cover.
 */
data class AttachState(
    val pages: Int = 0,
    val orderNo: String = "",
    val date: CalendarDate? = null,
    val expected: String = "",
    val lastOrderNo: String = "",
    val choices: List<BankEntry> = emptyList(),
    val ticked: Set<String> = emptySet(),
    val problems: Set<AttachProblem> = emptySet(),
    val notice: AttachNotice? = null,
    val busy: Boolean = false,
) {
    /** Counts only: what was typed is the officer's private data. */
    override fun toString(): String = "AttachState(pages=$pages, choices=${choices.size}, ticked=${ticked.size}, problems=${problems.size})"
}
