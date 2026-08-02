package com.payslipmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Evaluates app integrity via [appIntegrityChecker] and updates [_uiState].
 */
fun PayslipViewModel.verifyAppIntegrity() {
    viewModelScope.launch {
        val status = appIntegrityChecker.checkIntegrity()
        _uiState.update { it.copy(appIntegrityStatus = status) }
    }
}
