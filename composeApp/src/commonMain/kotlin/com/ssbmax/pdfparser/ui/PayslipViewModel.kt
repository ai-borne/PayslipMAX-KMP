package com.ssbmax.pdfparser.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.pdfparser.crypto.CryptoHelper
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.repository.PayslipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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
    val appTheme: String = "system",
    val isLockEnabled: Boolean = false,
    val appPinHash: String = "",
    val profileName: String = "",
    val profileCdaNumber: String = "",
    val profilePanNumber: String = "",
    val isAppLocked: Boolean = false,
)

class PayslipViewModel(
    internal val repository: PayslipRepository,
    internal val backupManager: com.ssbmax.pdfparser.backup.BackupManager,
    internal val geminiService: com.ssbmax.pdfparser.insights.GeminiService,
    internal val financialIntelligenceRepository: com.ssbmax.pdfparser.repository.FinancialIntelligenceRepository? = null,
    internal val cloudSyncRepository: com.ssbmax.pdfparser.repository.CloudSyncRepository? = null,
) : ViewModel() {
    internal val _uiState = MutableStateFlow(PayslipUiState())
    val uiState: StateFlow<PayslipUiState> = _uiState.asStateFlow()

    val ledgerRecords: StateFlow<List<com.ssbmax.pdfparser.database.LedgerRecordEntity>> =
        financialIntelligenceRepository?.getAllLedgerRecords()
            ?.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
            ?: MutableStateFlow(emptyList())

    val financialInsights: StateFlow<List<com.ssbmax.pdfparser.database.FinancialInsightEntity>> =
        financialIntelligenceRepository?.getAllFinancialInsights()
            ?.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
            ?: MutableStateFlow(emptyList())

    val representationDrafts: StateFlow<List<com.ssbmax.pdfparser.database.RepresentationDraftEntity>> =
        financialIntelligenceRepository?.getAllRepresentationDrafts()
            ?.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
            ?: MutableStateFlow(emptyList())

    init {
        observePayslips()
        observeSettings()
    }

    internal fun observePayslips() {
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
                val parsed = result.getOrNull()
                if (parsed != null) {
                    financialIntelligenceRepository?.processPayslipAndRunAnalysis(parsed)
                }
                _uiState.update { state ->
                    state.copy(
                        selectedPayslip = parsed,
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

    fun getPayslipPdf(
        dateStr: String,
        onResult: (ByteArray?) -> Unit,
    ) {
        viewModelScope.launch {
            val pdfBytes = repository.getPayslipPdf(dateStr)
            onResult(pdfBytes)
        }
    }

    private fun observeSettings() {
        var isFirstSettingsLoad = true
        viewModelScope.launch {
            repository.getSettingsFlow().collect { settings ->
                _uiState.update { state ->
                    val isLocked =
                        if (isFirstSettingsLoad) {
                            isFirstSettingsLoad = false
                            settings?.isLockEnabled ?: false
                        } else {
                            state.isAppLocked && (settings?.isLockEnabled ?: false)
                        }
                    state.copy(
                        isPremiumEnabled = settings?.isPremiumEnabled ?: false,
                        geminiApiKey = settings?.geminiApiKey ?: "",
                        appTheme = settings?.appTheme ?: "system",
                        isLockEnabled = settings?.isLockEnabled ?: false,
                        appPinHash = settings?.appPinHash ?: "",
                        profileName = settings?.profileName ?: "",
                        profileCdaNumber = settings?.profileCdaNumber ?: "",
                        profilePanNumber = settings?.profilePanNumber ?: "",
                        isAppLocked = isLocked,
                    )
                }
            }
        }
    }

    fun setPremiumEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
            repository.saveSettings(current.copy(isPremiumEnabled = enabled))
        }
    }

    fun setGeminiApiKey(key: String) {
        viewModelScope.launch {
            val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
            repository.saveSettings(current.copy(geminiApiKey = key))
        }
    }

    fun setAppTheme(theme: String) {
        viewModelScope.launch {
            val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
            repository.saveSettings(current.copy(appTheme = theme))
        }
    }

    fun setLockEnabled(
        enabled: Boolean,
        pin: String = "",
    ) {
        viewModelScope.launch {
            val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
            val pinHash = if (pin.isNotEmpty()) CryptoHelper.sha256(pin) else current.appPinHash
            repository.saveSettings(current.copy(isLockEnabled = enabled, appPinHash = pinHash))
        }
    }

    fun verifyPin(pin: String): Boolean {
        val hash = CryptoHelper.sha256(pin)
        val matches = hash == _uiState.value.appPinHash
        if (matches) {
            _uiState.update { it.copy(isAppLocked = false) }
        }
        return matches
    }

    fun lockApp() {
        if (_uiState.value.isLockEnabled) {
            _uiState.update { it.copy(isAppLocked = true) }
        }
    }

    fun unlockApp() {
        _uiState.update { it.copy(isAppLocked = false) }
    }

    fun updateProfileOverrides(
        name: String,
        cda: String,
        pan: String,
    ) {
        viewModelScope.launch {
            val current = repository.getSettings() ?: com.ssbmax.pdfparser.database.AppSettingsEntity()
            repository.saveSettings(current.copy(profileName = name, profileCdaNumber = cda, profilePanNumber = pan))
        }
    }

    fun updateRepresentationDraft(draft: com.ssbmax.pdfparser.database.RepresentationDraftEntity) {
        viewModelScope.launch {
            financialIntelligenceRepository?.insertRepresentationDraft(draft)
        }
    }
}
