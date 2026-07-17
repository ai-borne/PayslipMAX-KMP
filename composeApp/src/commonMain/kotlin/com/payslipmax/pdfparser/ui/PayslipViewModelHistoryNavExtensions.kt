package com.payslipmax.pdfparser.ui

import kotlinx.coroutines.flow.update

fun PayslipViewModel.selectHistoryDetailPayslip(dateStr: String) {
    _uiState.update { it.copy(historyDetailPayslipId = dateStr) }
}
