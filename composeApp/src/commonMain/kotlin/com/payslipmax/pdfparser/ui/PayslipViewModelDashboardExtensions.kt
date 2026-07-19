package com.payslipmax.pdfparser.ui

import kotlinx.coroutines.flow.update

fun PayslipViewModel.saveDashboardScrollPosition(value: Int) {
    _uiState.update { it.copy(dashboardScrollValue = value) }
}
