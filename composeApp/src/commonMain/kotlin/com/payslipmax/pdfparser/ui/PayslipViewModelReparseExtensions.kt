package com.payslipmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import com.payslipmax.pdfparser.repository.ReparseSummary
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Re-parses every stored payslip through the current parser and overwrites its stale record
 * (see [com.payslipmax.pdfparser.repository.PayslipRepository.reparseAllPayslips]). The Room-backed
 * payslips flow observed by [PayslipViewModel.observePayslips] auto-refreshes once the overwrite
 * lands, so no manual state merge is needed here beyond loading/error bookkeeping.
 */
fun PayslipViewModel.reparseAllPayslips(
    password: String,
    onComplete: (Result<ReparseSummary>) -> Unit,
) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        try {
            val summary = repository.reparseAllPayslips(password)
            _uiState.update { it.copy(isLoading = false) }
            onComplete(Result.success(summary))
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "Failed to re-parse payslips: ${e.message}") }
            onComplete(Result.failure(e))
        }
    }
}
