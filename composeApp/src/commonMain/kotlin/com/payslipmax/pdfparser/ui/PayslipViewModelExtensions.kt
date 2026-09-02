package com.payslipmax.pdfparser.ui

import androidx.lifecycle.viewModelScope
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
    mode: com.payslipmax.pdfparser.repository.RestoreMode,
    onComplete: (Result<Unit>) -> Unit,
) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        val result = repository.importUniversalBackup(backupBytes, password, mode)
        _uiState.update { it.copy(isLoading = false) }
        onComplete(result)
    }
}

fun PayslipViewModel.setPremiumEnabled(enabled: Boolean) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.payslipmax.pdfparser.database.AppSettingsEntity()
        if (current.isPremiumEnabled != enabled) {
            repository.saveSettings(current.copy(isPremiumEnabled = enabled))
        }
    }
}

fun PayslipViewModel.observeSubscriptionLifecycle() {
    viewModelScope.launch {
        billingManager.subscriptionState.collect { state ->
            when (state) {
                is com.payslipmax.pdfparser.billing.SubscriptionState.Active -> setPremiumEnabled(true)
                is com.payslipmax.pdfparser.billing.SubscriptionState.Inactive -> setPremiumEnabled(false)
                is com.payslipmax.pdfparser.billing.SubscriptionState.Unknown -> {
                    // Offline or cold startup: preserve existing Room DB cache
                }
            }
        }
    }
}

fun PayslipViewModel.launchPurchaseFlow(onResult: ((com.payslipmax.pdfparser.billing.PurchaseResult) -> Unit)? = null) {
    viewModelScope.launch {
        val result = billingManager.launchBillingFlow()
        if (result is com.payslipmax.pdfparser.billing.PurchaseResult.Success) {
            setPremiumEnabled(true)
        }
        onResult?.invoke(result)
    }
}

fun PayslipViewModel.restorePurchases(onResult: ((com.payslipmax.pdfparser.billing.PurchaseResult) -> Unit)? = null) {
    viewModelScope.launch {
        val result = billingManager.restorePurchases()
        if (result is com.payslipmax.pdfparser.billing.PurchaseResult.Success) {
            setPremiumEnabled(true)
        }
        onResult?.invoke(result)
    }
}

fun PayslipViewModel.setAppTheme(theme: String) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.payslipmax.pdfparser.database.AppSettingsEntity()
        repository.saveSettings(current.copy(appTheme = theme))
    }
}

fun PayslipViewModel.setLockEnabled(
    enabled: Boolean,
    pin: String = "",
) {
    viewModelScope.launch {
        val current = repository.getSettings() ?: com.payslipmax.pdfparser.database.AppSettingsEntity()
        val pinHash = if (pin.isNotEmpty()) com.payslipmax.pdfparser.crypto.CryptoHelper.sha256(pin) else current.appPinHash
        repository.saveSettings(current.copy(isLockEnabled = enabled, appPinHash = pinHash))
    }
}

fun PayslipViewModel.verifyPin(pin: String): Boolean {
    val hash = com.payslipmax.pdfparser.crypto.CryptoHelper.sha256(pin)
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
        val current = repository.getSettings() ?: com.payslipmax.pdfparser.database.AppSettingsEntity()
        repository.saveSettings(current.copy(profileName = name, profileCdaNumber = cda, profilePanNumber = pan))
    }
}

fun PayslipViewModel.updateRepresentationDraft(draft: com.payslipmax.pdfparser.database.RepresentationDraftEntity) {
    viewModelScope.launch {
        financialIntelligenceRepository?.insertRepresentationDraft(draft)
    }
}

/**
 * Phase 5 — persists a single per-field correction for a low-confidence field and immediately
 * reflects the merged value in [PayslipUiState] (the observed flow also re-emits, but updating the
 * selected payslip here keeps the open replica screen in sync without waiting for re-collection).
 */
fun PayslipViewModel.applyCorrection(
    dateStr: String,
    fieldKey: String,
    newValue: Double,
) {
    viewModelScope.launch {
        repository.saveCorrection(dateStr, fieldKey, newValue)
        val merged = repository.getPayslipByDate(dateStr) ?: return@launch
        _uiState.update { state ->
            state.copy(
                selectedPayslip =
                    if (state.selectedPayslip?.dateStr == dateStr) merged else state.selectedPayslip,
                payslips = state.payslips.map { if (it.dateStr == dateStr) merged else it },
            )
        }
    }
}

fun PayslipViewModel.clearError() {
    _uiState.update { it.copy(error = null, importError = null) }
}

fun PayslipViewModel.resetImportSuccess() {
    _uiState.update { it.copy(importSuccess = false) }
}

fun PayslipViewModel.resetPinWithPdf(
    pdfBytes: ByteArray,
    password: String,
    filename: String,
    onResult: (Result<Unit>) -> Unit,
) {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        val result = repository.importPayslip(pdfBytes, password, filename)
        if (result.isSuccess) {
            val parsed = result.getOrNull()
            if (parsed != null) {
                val expectedPan = _uiState.value.profilePanNumber
                if (expectedPan.isNotEmpty() && parsed.officer.pan.isNotEmpty() &&
                    !parsed.officer.pan.equals(expectedPan, ignoreCase = true)
                ) {
                    _uiState.update { it.copy(isLoading = false) }
                    onResult(Result.failure(Exception("PDF does not match the active user profile")))
                    return@launch
                }
                val current = repository.getSettings() ?: com.payslipmax.pdfparser.database.AppSettingsEntity()
                repository.saveSettings(current.copy(isLockEnabled = false, appPinHash = ""))
                _uiState.update { it.copy(isAppLocked = false, isLockEnabled = false, appPinHash = "", isLoading = false) }
                onResult(Result.success(Unit))
            } else {
                _uiState.update { it.copy(isLoading = false) }
                onResult(Result.failure(Exception("Failed to parse payslip content")))
            }
        } else {
            _uiState.update { it.copy(isLoading = false) }
            onResult(Result.failure(result.exceptionOrNull() ?: Exception("Decryption failed. Invalid PDF password.")))
        }
    }
}
