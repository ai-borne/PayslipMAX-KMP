package com.payslipmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import com.payslipmax.pdfparser.domain.ParsedPayslip
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
