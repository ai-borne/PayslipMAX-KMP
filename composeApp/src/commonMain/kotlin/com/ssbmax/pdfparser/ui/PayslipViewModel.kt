package com.ssbmax.pdfparser.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.repository.PayslipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PayslipUiState(
    val payslips: List<ParsedPayslip> = emptyList(),
    val selectedPayslip: ParsedPayslip? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val importSuccess: Boolean = false,
    val isPremiumEnabled: Boolean = false,
    val geminiApiKey: String = "",
    val aiInsights: String? = null,
    val isAiLoading: Boolean = false,
    val aiError: String? = null,
)

class PayslipViewModel(
    private val repository: PayslipRepository,
    private val backupManager: com.ssbmax.pdfparser.backup.BackupManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PayslipUiState())
    val uiState: StateFlow<PayslipUiState> = _uiState.asStateFlow()

    init {
        observePayslips()
    }

    private fun observePayslips() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllPayslips().collect { list ->
                    _uiState.update { state ->
                        state.copy(
                            payslips = list,
                            selectedPayslip = state.selectedPayslip ?: list.lastOrNull(),
                            isLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = "Failed to load payslips: ${e.message}",
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun selectPayslip(payslip: ParsedPayslip) {
        _uiState.update { it.copy(selectedPayslip = payslip) }
    }

    fun getAvailableYears(): List<Int> {
        return _uiState.value.payslips
            .map { it.year }
            .distinct()
            .sortedDescending()
    }

    fun getMonthsForYear(year: Int): List<ParsedPayslip> {
        return _uiState.value.payslips
            .filter { it.year == year }
            .sortedByDescending { it.monthNum }
    }

    fun selectByYearMonth(
        year: Int,
        monthNum: Int,
    ) {
        val match =
            _uiState.value.payslips.find {
                it.year == year && it.monthNum == monthNum
            }
        if (match != null) {
            _uiState.update { it.copy(selectedPayslip = match) }
        }
    }

    fun importPayslip(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, importSuccess = false) }
            val result = repository.importPayslip(pdfBytes, password, filename)
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        selectedPayslip = result.getOrNull(),
                        isLoading = false,
                        importSuccess = true,
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        error = result.exceptionOrNull()?.message ?: "Decryption or parsing failed",
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun deletePayslip(dateStr: String) {
        viewModelScope.launch {
            repository.deletePayslip(dateStr)
            _uiState.update { state ->
                val remaining = state.payslips.filter { it.dateStr != dateStr }
                state.copy(
                    selectedPayslip =
                        if (state.selectedPayslip?.dateStr == dateStr) {
                            remaining.lastOrNull()
                        } else {
                            state.selectedPayslip
                        },
                )
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetImportSuccess() {
        _uiState.update { it.copy(importSuccess = false) }
    }

    fun seedMockData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                repository.clearAll()
                repository.seedMockData()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to seed data: ${e.message}") }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, selectedPayslip = null) }
            try {
                repository.clearAll()
                _uiState.update { it.copy(isLoading = false, payslips = emptyList()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Failed to clear data: ${e.message}") }
            }
        }
    }

    fun backupDatabase(
        password: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = backupManager.backup(password)
            _uiState.update { it.copy(isLoading = false) }
            onComplete(result)
        }
    }

    fun restoreDatabase(
        password: String,
        onComplete: (Result<Unit>) -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = backupManager.restore(password)
            if (result.isSuccess) {
                observePayslips()
            }
            _uiState.update { it.copy(isLoading = false) }
            onComplete(result)
        }
    }

    fun getPayslipPdf(
        dateStr: String,
        onResult: (ByteArray?) -> Unit,
    ) {
        viewModelScope.launch {
            val pdfBytes = repository.getPayslipPdf(dateStr)
            onResult(pdfBytes)
        }
    }

    private val geminiService = com.ssbmax.pdfparser.insights.GeminiService()

    fun setPremiumEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isPremiumEnabled = enabled) }
    }

    fun setGeminiApiKey(key: String) {
        _uiState.update { it.copy(geminiApiKey = key) }
    }

    fun generateAiInsights(payslip: ParsedPayslip) {
        val apiKey = _uiState.value.geminiApiKey
        if (apiKey.isBlank()) {
            _uiState.update { it.copy(aiError = "API Key is missing. Configure it in Settings.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null, aiInsights = null) }
            val result = geminiService.getFinancialInsights(payslip, apiKey)
            if (result.isSuccess) {
                _uiState.update { it.copy(aiInsights = result.getOrThrow(), isAiLoading = false) }
            } else {
                _uiState.update { it.copy(aiError = result.exceptionOrNull()?.message ?: "Failed to generate AI insights", isAiLoading = false) }
            }
        }
    }

    fun clearAiInsights() {
        _uiState.update { it.copy(aiInsights = null, aiError = null) }
    }
}
