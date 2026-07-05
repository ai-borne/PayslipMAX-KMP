package com.payslipmax.pdfparser.ui

import kotlinx.coroutines.flow.update

fun PayslipViewModel.toggleHistoryYearExpanded(year: Int) {
    _uiState.update { state ->
        val expanded = state.expandedHistoryYears
        state.copy(
            expandedHistoryYears = if (expanded.contains(year)) expanded - year else expanded + year,
        )
    }
}

fun PayslipViewModel.saveHistoryScrollPosition(
    index: Int,
    offset: Int,
) {
    _uiState.update { it.copy(historyScrollIndex = index, historyScrollOffset = offset) }
}
