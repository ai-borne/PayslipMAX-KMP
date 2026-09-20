package com.payslipmax.pdfparser.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.WealthOptimizationEngine
import com.payslipmax.pdfparser.insights.gemma.GemmaBaseModelInstaller
import com.payslipmax.pdfparser.insights.gemma.GemmaModelStorageManager
import com.payslipmax.pdfparser.insights.gemma.provideGemmaBaseModelInstaller
import com.payslipmax.pdfparser.rating.RatingPromptManager
import com.payslipmax.pdfparser.rating.requestReview
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.telemetry.GemmaInstallTelemetry
import com.payslipmax.pdfparser.telemetry.provideGemmaInstallTelemetry
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.utils.shareTextViaEmail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PayslipViewModel(
    internal val repository: PayslipRepository,
    internal val financialIntelligenceRepository: com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository? = null,
    internal val gemmaBaseModelInstaller: GemmaBaseModelInstaller = provideGemmaBaseModelInstaller(),
    internal val gemmaModelStorage: GemmaModelStorageManager = GemmaModelStorageManager(),
    internal val gemmaInstallTelemetry: GemmaInstallTelemetry = provideGemmaInstallTelemetry(),
    internal val appIntegrityChecker: com.payslipmax.pdfparser.domain.AppIntegrityChecker = com.payslipmax.pdfparser.domain.provideAppIntegrityChecker(),
    internal val billingManager: com.payslipmax.pdfparser.billing.BillingManager = com.payslipmax.pdfparser.billing.provideBillingManager(),
    internal val isFreeLaunchModeProvider: () -> Boolean = { com.payslipmax.pdfparser.subscription.isFreeLaunchModePlatform() },
    internal val ratingPromptManager: RatingPromptManager = RatingPromptManager(),
    internal var reviewRequester: () -> Unit = ::requestReview,
    internal var issueReportSender: (to: String, subject: String, body: String) -> Unit = ::shareTextViaEmail,
) : ViewModel() {
    internal val _uiState = MutableStateFlow(PayslipUiState())
    val uiState: StateFlow<PayslipUiState> = _uiState.asStateFlow()

    // SSOT for PRO entitlement. Defaults to FORCE_PRO in debug (preserving prior dev behaviour) and
    // FOLLOW_FLAG in release, where the DevOverride mechanism is inert. Composables never read this
    // directly — they go through the hasAccess/devOverride/setDevOverride façade (SubscriptionAccess.kt).
    val subscriptionManager =
        com.payslipmax.pdfparser.subscription.SubscriptionManager(
            isPremiumEnabledProvider = { _uiState.value.isPremiumEnabled },
            billingManager = billingManager,
            isFreeLaunchModeProvider = isFreeLaunchModeProvider,
        )

    val ledgerRecords: StateFlow<List<com.payslipmax.pdfparser.database.LedgerRecordEntity>> =
        financialIntelligenceRepository?.getAllLedgerRecords()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) ?: MutableStateFlow(emptyList())

    val financialInsights: StateFlow<List<com.payslipmax.pdfparser.database.FinancialInsightEntity>> =
        financialIntelligenceRepository?.getAllFinancialInsights()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) ?: MutableStateFlow(emptyList())

    val representationDrafts: StateFlow<List<com.payslipmax.pdfparser.database.RepresentationDraftEntity>> =
        financialIntelligenceRepository?.getAllRepresentationDrafts()?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) ?: MutableStateFlow(emptyList())

    // SSOT for the Premium yearly price shown across the upgrade sheet, settings card, and Insights
    // hub. The store is the only source of truth for price: this stays null until RevenueCat
    // resolves a live one, and stays null if it never does. There is deliberately no static
    // fallback — a hardcoded price made a dead product render as a healthy paywall (Phase 7 debt,
    // docs/Launch/08_ios_monetization_phaseplan.md). Consumers degrade instead (see
    // UpgradePricingSection).
    internal val _premiumPriceState = MutableStateFlow<String?>(null)
    val premiumPriceState: StateFlow<String?> = _premiumPriceState.asStateFlow()

    // Temporary in-memory cache for the active import session; cleared immediately on dismiss or success.
    internal var pendingImportPdfBytes: ByteArray? = null
    internal var pendingImportFilename: String? = null

    // Set by handleSuccessfulImport: whether the most recent successful import added a genuinely
    // new payslip (vs. a duplicate re-import of an existing dateStr). Gates the rating prompt so a
    // duplicate import never counts as a fresh "positive moment".
    internal var lastImportWasNewPayslip: Boolean = false

    init {
        verifyAppIntegrity()
        checkGemmaSupport()
        observePayslips()
        observeSettings()
        observeSubscriptionLifecycle()
        installGemmaBaseModel()
        refreshPremiumPrice()
    }

    /**
     * Re-reads the store's formatted price. Called at startup and again whenever the paywall is
     * presented: the startup read can land before StoreKit has resolved the device's storefront
     * and return the wrong currency, and a value read once would then be quoted for the rest of
     * the session while Apple charges the real local price (see `PayslipViewModelBillingTest`).
     *
     * A fetch that returns nothing leaves the last known price in place rather than blanking it —
     * a transient failure while the sheet is open must not disable Unlock mid-decision.
     */
    fun refreshPremiumPrice() {
        viewModelScope.launch {
            billingManager.getFormattedPrice()?.let { _premiumPriceState.value = it }
        }
    }

    internal fun observePayslips() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllPayslips().collect { list ->
                    val nextSelected = _uiState.value.selectedPayslip ?: list.lastOrNull()
                    val latestYear = list.maxOfOrNull { it.year }
                    val optimizationResult = computeTaxOptimization(list, nextSelected)
                    _uiState.update { state ->
                        val expandedYears =
                            if (latestYear != null && latestYear != state.lastKnownHistoryYear) {
                                state.expandedHistoryYears + latestYear
                            } else {
                                state.expandedHistoryYears
                            }
                        state.copy(
                            payslips = list,
                            selectedPayslip = nextSelected,
                            taxOptimizationResult = optimizationResult,
                            isLoading = false,
                            expandedHistoryYears = expandedYears,
                            lastKnownHistoryYear = latestYear ?: state.lastKnownHistoryYear,
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

    private fun onPayslipSelected(payslip: ParsedPayslip?) {
        val optimizationResult = computeTaxOptimization(_uiState.value.payslips, payslip)
        _uiState.update {
            it.copy(
                selectedPayslip = payslip,
                taxOptimizationResult = optimizationResult,
            )
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
                    val updatedPayslips =
                        if (parsed != null && state.payslips.none { it.dateStr == parsed.dateStr }) {
                            state.payslips + parsed
                        } else {
                            state.payslips
                        }
                    state.copy(
                        payslips = updatedPayslips,
                        selectedPayslip = parsed,
                        taxOptimizationResult = computeTaxOptimization(updatedPayslips, parsed),
                        isLoading = false,
                        importSuccess = true,
                    )
                }
            } else {
                val error = result.exceptionOrNull()
                val rawMessage = error?.message ?: ""
                val safeFilename = com.payslipmax.pdfparser.telemetry.TelemetrySanitizer.sanitizeFilename(filename)
                com.payslipmax.pdfparser.logging.Logger.e(
                    "PayslipViewModel",
                    "importPayslip failed for $safeFilename: $rawMessage",
                    error,
                )
                if (error != null && !rawMessage.contains("PASSWORD_PROTECTED") && !rawMessage.contains("InvalidPasswordException")) {
                    val crashReporter = com.payslipmax.pdfparser.telemetry.provideCrashReporter()
                    crashReporter.recordException(
                        error,
                        mapOf(
                            "operation" to "pdf_import",
                            "parser_stage" to if (rawMessage.contains("UNRECOGNIZED_GRAMMAR")) "preflight" else "grammar",
                            "file_size_bytes" to pdfBytes.size.toString(),
                            "error_reason" to rawMessage.take(100),
                        ),
                    )
                }
                val friendlyError =
                    when {
                        rawMessage.contains("UNRECOGNIZED_GRAMMAR") || rawMessage.contains("PdfPreFlightValidationFailed") ->
                            AppStrings.errorUnrecognizedPdf
                        rawMessage.contains("PASSWORD_PROTECTED") ->
                            AppStrings.errorEncryptedPdfDesc
                        rawMessage.contains("NO_TEXT_TOKENS") ->
                            AppStrings.errorZeroTokensDesc
                        rawMessage.isNotBlank() && !rawMessage.contains("Exception") && !rawMessage.contains(":") ->
                            rawMessage
                        else ->
                            AppStrings.errorUnrecognizedPdf
                    }
                _uiState.update { state ->
                    state.copy(
                        importError = friendlyError,
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
                state.copy(
                    payslips = remaining,
                    selectedPayslip = nextSelected,
                    taxOptimizationResult = computeTaxOptimization(remaining, nextSelected),
                )
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

    private fun computeTaxOptimization(
        payslips: List<ParsedPayslip>,
        selectedPayslip: ParsedPayslip?,
    ): com.payslipmax.pdfparser.insights.OptimizationResult? {
        return if (payslips.isNotEmpty()) {
            WealthOptimizationEngine.analyzeLedger(payslips, selectedPayslip)
        } else {
            null
        }
    }
}
