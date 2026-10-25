package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

/**
 * When the card on the Dashboard shows (DECISIONS Round 25): after a payslip import completes, while something is due now, until the officer
 * says "Not now". Held in memory only, so nothing about it is stored or backed up: a new import (or a new launch) brings the card back.
 */
class DueCardModel(
    private val due: DueViewModel,
) {
    private val imports = MutableStateFlow(0)
    private val dismissedAt = MutableStateFlow(0)

    /** A payslip import finished: the card may show again, even if it was dismissed before. */
    fun onImportCompleted() {
        imports.update { it + 1 }
    }

    /** "Not now": hides the card until the next import. */
    fun notNow() {
        dismissedAt.value = imports.value
    }

    /** How many reminders are due when the card should show, and 0 when it should not. */
    val count: Flow<Int> = combine(imports, dismissedAt, due.state) { done, dismissed, list -> if (done > dismissed) list.dueCount else 0 }
}
