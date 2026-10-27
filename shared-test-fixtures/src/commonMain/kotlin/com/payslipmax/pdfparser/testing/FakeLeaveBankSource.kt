package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull

/**
 * A [LeaveBankSource] a test controls: [initial] null means the source has not answered yet (the form must wait, as it would for a slow
 * summary), and [publish] answers, again and again if wanted. Counts its collectors so a test can see the form stops listening when it closes.
 */
class FakeLeaveBankSource(initial: List<LeaveEntry>? = emptyList()) : LeaveBankSource {
    private val latest = MutableStateFlow(initial)

    fun publish(rows: List<LeaveEntry>) {
        latest.value = rows
    }

    /** How many collectors are listening right now. */
    val listeners: Int get() = latest.subscriptionCount.value

    override val rows: Flow<List<LeaveEntry>> = latest.filterNotNull()
}
