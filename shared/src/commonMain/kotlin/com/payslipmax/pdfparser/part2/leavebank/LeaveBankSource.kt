package com.payslipmax.pdfparser.part2.leavebank

import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.insights.LeaveBankSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * Where the leave form gets the Leave Bank's rows (Part II plan, phase 8b): `LeaveEntry` only, never a payslip. [rows] emits the rows of the
 * app's latest Leave Bank summary each time it changes, nothing until the summary is known, and an empty list when the app holds no statement
 * that prints the table. The form collects it while a draft is open and waits for its first answer, so a source may answer late.
 */
interface LeaveBankSource {
    val rows: Flow<List<LeaveEntry>>
}

/**
 * The one [LeaveBankSource] the app binds: the rows of the single `PayslipViewModel.leaveBankSummary`, handed in by [attach] (the summary is
 * derived once, there; nothing here recomputes it). Nothing is collected from the summary while no draft is open.
 */
class LeaveBankFeed : LeaveBankSource {
    private val summary = MutableStateFlow<Flow<LeaveBankSummary?>?>(null)

    fun attach(summary: Flow<LeaveBankSummary?>) {
        this.summary.value = summary
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val rows: Flow<List<LeaveEntry>> = summary.filterNotNull().flatMapLatest { it }.map(LeaveBankFigures::entriesOf)
}
