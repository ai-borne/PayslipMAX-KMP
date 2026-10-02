package com.payslipmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Audits a freshly imported payslip with the officer's stored corrections applied (the list Pay Audit
 * shows), so the ledger never holds a raw value the officer has already fixed. The decision logic lives in
 * the shared repository; this only feeds it the corrected payslip.
 */
internal suspend fun PayslipViewModel.auditImported(parsed: ParsedPayslip) {
    financialIntelligenceRepository?.processPayslipAndRunAnalysis(repository.getPayslipByDate(parsed.dateStr) ?: parsed)
}

/**
 * One-time startup repair: after the first payslip emission, rebuilds the ledger and insights if any stored
 * payslip has no ledger row (e.g. a backup restored before restores rebuilt them). Runs off the UI path,
 * once per ViewModel, and the shared repository never lets a failure escape.
 */
internal fun PayslipViewModel.repairAuditHistoryOnce(payslips: List<ParsedPayslip>) {
    if (auditRepairChecked) return
    auditRepairChecked = true
    val intelligence = financialIntelligenceRepository ?: return
    viewModelScope.launch { intelligence.repairAuditHistoryIfIncomplete(payslips) }
}

/**
 * Re-derives the ledger and insights from the payslips as the officer now sees them, after something changed
 * what a stored payslip holds (a saved correction, a re-parse). The payslips and corrections are already
 * committed, so a failure here is logged without PII and never fails the action that triggered it; the
 * startup repair and the next rebuild finish the job.
 */
internal suspend fun PayslipViewModel.refreshAuditHistory() {
    val intelligence = financialIntelligenceRepository ?: return
    try {
        intelligence.rebuildAuditHistory(repository.getAllPayslips().first())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Logger.e("PayslipViewModel", "Audit history refresh failed", e)
    }
}
