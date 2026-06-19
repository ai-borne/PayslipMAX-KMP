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
    if (financialIntelligenceRepository == null) {
        _uiState.update {
            it.copy(aiError = "AI insights service is unavailable. Please restart the app.")
        }
        return
    }
    launchAiGeneration(payslip)
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

fun PayslipViewModel.setPremiumEnabled(enabled: Boolean) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
        repository.saveSettings(current.copy(isPremiumEnabled = enabled))
    }
}

fun PayslipViewModel.setAppTheme(theme: String) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
        repository.saveSettings(current.copy(appTheme = theme))
    }
}

fun PayslipViewModel.setLockEnabled(
    enabled: Boolean,
    pin: String = "",
) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
        val pinHash = if (pin.isNotEmpty()) com.ssbmax.pdfparser.crypto.CryptoHelper.sha256(pin) else current.appPinHash
        repository.saveSettings(current.copy(isLockEnabled = enabled, appPinHash = pinHash))
    }
}

fun PayslipViewModel.verifyPin(pin: String): Boolean {
    val hash = com.ssbmax.pdfparser.crypto.CryptoHelper.sha256(pin)
    val matches = hash == _uiState.value.appPinHash
    if (matches) {
        _uiState.update { it.copy(isAppLocked = false) }
    }
    return matches
}

fun PayslipViewModel.lockApp() {
    if (_uiState.value.isLockEnabled) {
        _uiState.update { it.copy(isAppLocked = true) }
    }
}

fun PayslipViewModel.unlockApp() {
    _uiState.update { it.copy(isAppLocked = false) }
}

fun PayslipViewModel.updateProfileOverrides(
    name: String,
    cda: String,
    pan: String,
) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
        repository.saveSettings(current.copy(profileName = name, profileCdaNumber = cda, profilePanNumber = pan))
    }
}

fun PayslipViewModel.updateRepresentationDraft(draft: com.ssbmax.pdfparser.database.RepresentationDraftEntity) {
    viewModelScope.launch {
        financialIntelligenceRepository?.insertRepresentationDraft(draft)
    }
}
