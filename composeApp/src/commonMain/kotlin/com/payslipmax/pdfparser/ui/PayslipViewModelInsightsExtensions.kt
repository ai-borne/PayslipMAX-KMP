package com.payslipmax.pdfparser.ui

import kotlinx.coroutines.flow.update

fun PayslipViewModel.saveInsightsScrollPosition(
    index: Int,
    offset: Int,
) {
    _uiState.update { it.copy(insightsScrollIndex = index, insightsScrollOffset = offset) }
}
