package com.ssbmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
import com.ssbmax.pdfparser.domain.ParsedPayslip
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun PayslipViewModel.seedMockData() {
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

fun PayslipViewModel.clearAllData() {
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

fun PayslipViewModel.backupDatabase(
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

fun PayslipViewModel.restoreDatabase(
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

fun PayslipViewModel.generateAiInsights(payslip: ParsedPayslip) {
    val repo = financialIntelligenceRepository
    if (repo == null) {
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
        return
    }

    viewModelScope.launch {
        _uiState.update { it.copy(isAiLoading = true, aiError = null, aiInsights = null) }
        try {
            val engineResult = repo.processPayslipAndRunAnalysis(payslip)
            val result = repo.generateNarrativeInsights(payslip, engineResult)
            if (result.isSuccess) {
                _uiState.update { it.copy(aiInsights = result.getOrThrow(), isAiLoading = false) }
            } else {
                _uiState.update {
                    it.copy(
                        aiError = result.exceptionOrNull()?.message ?: "Failed to generate narrative insights",
                        isAiLoading = false,
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(aiError = e.message ?: "Failed to generate insights", isAiLoading = false) }
        }
    }
}

fun PayslipViewModel.clearAiInsights() {
    _uiState.update { it.copy(aiInsights = null, aiError = null) }
}

fun PayslipViewModel.exportBackup(
    password: String,
    onComplete: (Result<ByteArray>) -> Unit,
) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val result = repository.exportUniversalBackup(password)
        _uiState.update { it.copy(isLoading = false) }
        onComplete(result)
    }
}

fun PayslipViewModel.importBackup(
    backupBytes: ByteArray,
    password: String,
    onComplete: (Result<Unit>) -> Unit,
) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val result = repository.importUniversalBackup(backupBytes, password)
        _uiState.update { it.copy(isLoading = false) }
        onComplete(result)
    }
}

fun PayslipViewModel.backupToCloud(
    userId: String,
    authToken: String,
    password: String,
    onComplete: (Result<Unit>) -> Unit,
) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        val repo = cloudSyncRepository
        val result =
            if (repo != null) {
                repo.uploadBackup(userId, authToken, password)
            } else {
                Result.failure(Exception("CloudSyncRepository not initialized"))
            }
        _uiState.update { it.copy(isLoading = false) }
        onComplete(result)
    }
}

fun PayslipViewModel.restoreFromCloud(
    userId: String,
    authToken: String,
    password: String,
    onComplete: (Result<Unit>) -> Unit,
) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        val repo = cloudSyncRepository
        val result =
            if (repo != null) {
                repo.downloadAndRestoreBackup(userId, authToken, password)
            } else {
                Result.failure(Exception("CloudSyncRepository not initialized"))
            }
        if (result.isSuccess) {
            observePayslips()
        }
        _uiState.update { it.copy(isLoading = false) }
        onComplete(result)
    }
}
