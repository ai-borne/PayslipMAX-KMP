package com.payslipmax.pdfparser.ui

import kotlinx.coroutines.flow.update

fun PayslipViewModel.saveSettingsScrollPosition(value: Int) {
    _uiState.update { it.copy(settingsScrollValue = value) }
}
