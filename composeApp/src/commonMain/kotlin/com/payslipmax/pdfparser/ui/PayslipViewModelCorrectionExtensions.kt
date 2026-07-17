package com.payslipmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import com.payslipmax.pdfparser.database.toCorrectionList
import com.payslipmax.pdfparser.domain.CorrectionType
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.SingleCorrection
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun PayslipViewModel.startEditingSession(dateStr: String) {
    viewModelScope.launch {
        val existingEntity = repository.getPayslipCorrections(dateStr)
        val correctionsList = existingEntity?.toCorrectionList() ?: emptyList()
        _uiState.update {
            it.copy(
                isEditModeActive = true,
                draftCorrections = correctionsList.associateBy { c -> c.fieldKey },
            )
        }
    }
}

fun PayslipViewModel.updateDraftCorrection(correction: SingleCorrection) {
    _uiState.update {
        it.copy(draftCorrections = it.draftCorrections + (correction.fieldKey to correction))
    }
}

fun PayslipViewModel.deleteDraftCorrection(
    fieldKey: String,
    codeHead: String,
    category: EntryCategory,
    originalAmount: Double?,
) {
    val updatedCorrection =
        SingleCorrection(
            fieldKey = fieldKey,
            codeHead = codeHead,
            amount = 0.0,
            category = category,
            type = CorrectionType.DELETED,
            originalAmount = originalAmount,
            originalCodeHead = codeHead,
            timestamp = 0L,
        )
    _uiState.update {
        it.copy(draftCorrections = it.draftCorrections + (fieldKey to updatedCorrection))
    }
}

fun PayslipViewModel.saveEditingSession(dateStr: String) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val corrections = _uiState.value.draftCorrections.values.toList()
        repository.saveAllCorrections(dateStr, corrections)

        val merged = repository.getPayslipByDate(dateStr)
        _uiState.update { state ->
            state.copy(
                selectedPayslip = if (state.selectedPayslip?.dateStr == dateStr) merged else state.selectedPayslip,
                isEditModeActive = false,
                draftCorrections = emptyMap(),
                isLoading = false,
            )
        }
        observePayslips()
    }
}

fun PayslipViewModel.cancelEditingSession() {
    _uiState.update {
        it.copy(
            isEditModeActive = false,
            draftCorrections = emptyMap(),
        )
    }
}
