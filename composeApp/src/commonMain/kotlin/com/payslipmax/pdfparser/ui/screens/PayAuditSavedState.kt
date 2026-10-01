package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.payslipmax.pdfparser.insights.timeline.PayMonth

private const val NO_MONTH = -1
private const val MONTHS_PER_YEAR = 12

/**
 * The part of Pay Audit's state that must outlive the system recreating the app: the month and tab the user
 * was on. Re-entering the screen from an entry point starts fresh (a new composition has no saved value), so
 * the entry point's month still wins. A stale or corrupt saved list falls back to the defaults.
 */
data class PayAuditSavedState(val month: PayMonth? = null, val tab: PayAuditTab = PayAuditTab.THIS_MONTH) {
    fun toList(): List<Any> = listOf(month?.index ?: NO_MONTH, tab.ordinal)

    companion object {
        fun fromList(list: List<Any>): PayAuditSavedState {
            val index = list.getOrNull(0) as? Int ?: return PayAuditSavedState()
            val tab = (list.getOrNull(1) as? Int)?.let { PayAuditTab.entries.getOrNull(it) } ?: PayAuditTab.THIS_MONTH
            val month = if (index >= 0) PayMonth(index / MONTHS_PER_YEAR, index % MONTHS_PER_YEAR + 1) else null
            return PayAuditSavedState(month, tab)
        }

        val Saver: Saver<PayAuditSavedState, Any> = listSaver(save = { it.toList() }, restore = { fromList(it) })
    }
}
