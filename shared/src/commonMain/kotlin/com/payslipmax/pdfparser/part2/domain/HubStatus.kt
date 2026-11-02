package com.payslipmax.pdfparser.part2.domain

/**
 * Where the officer's Part II file stands, for the hub's top card. Pure and derived from [BankView], so the hub and the Bank list share one
 * set of counts. It holds numbers only (no code, date or note), so it is safe to show where a screenshot could be taken.
 */
sealed interface HubStatus {
    /** The first read has not finished: nothing is said, so an empty file is never announced before it is known to be empty. */
    data object Unknown : HubStatus

    data object Empty : HubStatus

    data class Filing(val total: Int, val attached: Int, val notAttached: Int) : HubStatus

    companion object {
        fun of(
            loaded: Boolean,
            view: BankView,
        ): HubStatus =
            when {
                !loaded -> Unknown
                view.all == 0 -> Empty
                else -> Filing(view.all, view.attached, view.notAttached)
            }
    }
}
