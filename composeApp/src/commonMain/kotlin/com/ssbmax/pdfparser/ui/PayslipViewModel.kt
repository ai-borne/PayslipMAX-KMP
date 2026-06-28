package com.ssbmax.pdfparser.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.insights.NetworkErrorMapper
import com.ssbmax.pdfparser.logging.Logger
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
    val isLoading: Boolean = true,
    val error: String? = null,
    val importError: String? = null,
    val importSuccess: Boolean = false,
    val isPremiumEnabled: Boolean = false,
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
    val useLocalAi: Boolean = false,
    val isGemmaSupported: Boolean = true,
    val gemmaSupportReason: String? = null,
)

class PayslipViewModel(
    internal val repository: PayslipRepository,
    internal val backupManager: com.ssbmax.pdfparser.backup.BackupManager,
    internal val financialIntelligenceRepository: com.ssbmax.pdfparser.repository.FinancialIntelligenceRepository? = null,
    internal val cloudSyncRepository: com.ssbmax.pdfparser.repository.CloudSyncRepository? = null,
) : ViewModel() {
    internal val _uiState = MutableStateFlow(PayslipUiState())
    val uiState: StateFlow<PayslipUiState> = _uiState.asStateFlow()

    val subscriptionManager =
        com.ssbmax.pdfparser.subscription.SubscriptionManager(
            isPremiumEnabledProvider = { _uiState.value.isPremiumEnabled },
        ).apply {
            if (com.ssbmax.pdfparser.subscription.isDebugBuild()) {
                enableDeveloperPro()
            }
        }

    val ledgerRecords: StateFlow<List<com.ssbmax.pdfparser.database.LedgerRecordEntity>> =
        financialIntelligenceRepository?.getAllLedgerRecords()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) ?: MutableStateFlow(emptyList())

    val financialInsights: StateFlow<List<com.ssbmax.pdfparser.database.FinancialInsightEntity>> =
        financialIntelligenceRepository?.getAllFinancialInsights()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) ?: MutableStateFlow(emptyList())

    val representationDrafts: StateFlow<List<com.ssbmax.pdfparser.database.RepresentationDraftEntity>> =
        financialIntelligenceRepository?.getAllRepresentationDrafts()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) ?: MutableStateFlow(emptyList())

    val aiInsightReports: StateFlow<List<com.ssbmax.pdfparser.database.AiInsightReportEntity>> =
        financialIntelligenceRepository?.getAllAiInsightReports()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) ?: MutableStateFlow(emptyList())

    init {
        checkGemmaSupport()
        observePayslips()
        observeSettings()
    }

    private fun checkGemmaSupport() {
        try {
            val capManager = com.ssbmax.pdfparser.insights.gemma.DeviceCapabilityManager()
            val status = capManager.checkGemmaSupport()
            val (supported, reason) =
                when (status) {
                    is com.ssbmax.pdfparser.insights.gemma.GemmaSupportStatus.Supported -> true to null
                    is com.ssbmax.pdfparser.insights.gemma.GemmaSupportStatus.InsufficientRam -> false to "Requires device with 4GB RAM"
                    is com.ssbmax.pdfparser.insights.gemma.GemmaSupportStatus.InsufficientStorage -> false to "Requires 1.5GB free storage"
                    is com.ssbmax.pdfparser.insights.gemma.GemmaSupportStatus.UnsupportedArchitecture -> false to status.reason
                }
            _uiState.update { it.copy(isGemmaSupported = supported, gemmaSupportReason = reason) }
        } catch (e: Throwable) {
            _uiState.update { it.copy(isGemmaSupported = true, gemmaSupportReason = null) }
        }
    }

    internal fun observePayslips() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllPayslips().collect { list ->
                    val nextSelected = _uiState.value.selectedPayslip ?: list.lastOrNull()
                    _uiState.update { state ->
                        state.copy(
                            payslips = list,
                            selectedPayslip = nextSelected,
                            isLoading = false,
                        )
                    }
                    if (nextSelected != null) {
                        loadCachedAiInsights(nextSelected.dateStr)
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

    private fun onPayslipSelected(payslip: ParsedPayslip?) {
        if (payslip == null) {
            _uiState.update { it.copy(selectedPayslip = null, aiInsights = null, aiError = null) }
            return
        }
        _uiState.update { it.copy(selectedPayslip = payslip, aiInsights = null, aiError = null) }
        loadCachedAiInsights(payslip.dateStr)
    }

    internal fun loadCachedAiInsights(dateStr: String) {
        viewModelScope.launch {
            val repo = financialIntelligenceRepository ?: return@launch
            val cached = repo.getCachedAiInsights(dateStr)
            var autoRunPayslip: ParsedPayslip? = null
            _uiState.update { state ->
                if (state.selectedPayslip?.dateStr == dateStr) {
                    if (state.isPremiumEnabled && cached == null && !state.isAiLoading) {
                        autoRunPayslip = state.selectedPayslip
                    }
                    state.copy(aiInsights = cached, aiError = null)
                } else {
                    state
                }
            }
            autoRunPayslip?.let { launchAiGeneration(it) }
        }
    }

    internal fun launchAiGeneration(payslip: ParsedPayslip) {
        val repo = financialIntelligenceRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true, aiError = null, aiInsights = null) }
            try {
                val engineResult = repo.processPayslipAndRunAnalysis(payslip)
                val result = repo.generateNarrativeInsights(payslip, engineResult)
                if (result.isSuccess) {
                    _uiState.update { it.copy(aiInsights = result.getOrThrow(), isAiLoading = false) }
                } else {
                    _uiState.update {
                        val ex = result.exceptionOrNull()
                        val mappedError =
                            if (ex != null) {
                                Logger.e("PayslipViewModel", "generateNarrativeInsights failed", ex)
                                NetworkErrorMapper.getUserFriendlyMessage(ex)
                            } else {
                                "Failed to generate narrative insights"
                            }
                        it.copy(
                            aiError = mappedError,
                            isAiLoading = false,
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.e("PayslipViewModel", "launchAiGeneration failed", e)
                _uiState.update { it.copy(aiError = NetworkErrorMapper.getUserFriendlyMessage(e), isAiLoading = false) }
            }
        }
    }

    fun selectPayslip(payslip: ParsedPayslip) = onPayslipSelected(payslip)

    fun getAvailableYears(): List<Int> = _uiState.value.payslips.map { it.year }.distinct().sortedDescending()

    fun getMonthsForYear(year: Int): List<ParsedPayslip> = _uiState.value.payslips.filter { it.year == year }.sortedByDescending { it.monthNum }

    fun selectByYearMonth(
        year: Int,
        monthNum: Int,
    ) {
        _uiState.value.payslips.find { it.year == year && it.monthNum == monthNum }?.let { onPayslipSelected(it) }
    }

    fun importPayslip(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, importError = null, importSuccess = false) }
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
                if (parsed != null) {
                    loadCachedAiInsights(parsed.dateStr)
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        importError = result.exceptionOrNull()?.message ?: "Decryption or parsing failed",
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
                val nextSelected =
                    if (state.selectedPayslip?.dateStr == dateStr) {
                        remaining.lastOrNull()
                    } else {
                        state.selectedPayslip
                    }
                state.copy(selectedPayslip = nextSelected)
            }
            val next = _uiState.value.selectedPayslip
            if (next != null) {
                loadCachedAiInsights(next.dateStr)
            } else {
                _uiState.update { it.copy(aiInsights = null, aiError = null) }
            }
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

    private fun observeSettings() {
        var isFirstSettingsLoad = true
        var previousPremiumEnabled = false
        viewModelScope.launch {
            repository.getSettingsFlow().collect { settings ->
                val isPremium = settings?.isPremiumEnabled ?: false
                _uiState.update { state ->
                    val isLocked =
                        if (isFirstSettingsLoad) {
                            isFirstSettingsLoad = false
                            settings?.isLockEnabled ?: false
                        } else {
                            state.isAppLocked && (settings?.isLockEnabled ?: false)
                        }
                    state.copy(
                        isPremiumEnabled = isPremium,
                        appTheme = settings?.appTheme ?: "system",
                        isLockEnabled = settings?.isLockEnabled ?: false,
                        appPinHash = settings?.appPinHash ?: "",
                        profileName = settings?.profileName ?: "",
                        profileCdaNumber = settings?.profileCdaNumber ?: "",
                        profilePanNumber = settings?.profilePanNumber ?: "",
                        isAppLocked = isLocked,
                        useLocalAi = settings?.useLocalAi ?: false,
                    )
                }
                if (isPremium && !previousPremiumEnabled) {
                    _uiState.value.selectedPayslip?.let { payslip ->
                        loadCachedAiInsights(payslip.dateStr)
                    }
                }
                previousPremiumEnabled = isPremium
            }
        }
    }
}
