package com.payslipmax.pdfparser.ui.pcdao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payslipmax.pcdao.engine.PayFixationOptimizer
import com.payslipmax.pcdao.engine.PayFixationRequest
import com.payslipmax.pcdao.engine.PayFixationResult
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.MissionPresetId
import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciler
import com.payslipmax.pcdao.reconciliation.SituationalAutoInferer
import com.payslipmax.pcdao.reconciliation.SituationalCategory
import com.payslipmax.pcdao.reconciliation.SituationalMissionPresets
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.reconciliation.SpecializedMilitaryFactor
import com.payslipmax.pcdao.redressal.RedressalLetter
import com.payslipmax.pcdao.redressal.RedressalLetterGenerator
import com.payslipmax.pcdao.repository.PcdaoRulesRepository
import com.payslipmax.pcdao.timeline.CareerMilestoneAuditor
import com.payslipmax.pcdao.timeline.CumulativeLedgerRollupEngine
import com.payslipmax.pcdao.timeline.VaultMonthGroupMapper
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.pcdao.ComposePcdaoAssetProvider
import com.payslipmax.pdfparser.repository.PayslipRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PcdaoAuditViewModel(
    private val payslipRepository: PayslipRepository,
    private val rulesRepository: PcdaoRulesRepository = PcdaoRulesRepository(ComposePcdaoAssetProvider()),
    private val reconciler: ShadowLedgerReconciler = ShadowLedgerReconciler(),
    private val autoInferer: SituationalAutoInferer = SituationalAutoInferer(),
    private val rollupEngine: CumulativeLedgerRollupEngine = CumulativeLedgerRollupEngine(reconciler),
    private val milestoneAuditor: CareerMilestoneAuditor = CareerMilestoneAuditor(),
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    coroutineScope: CoroutineScope? = null,
    private val initialSelectedPayslip: ParsedPayslip? = null,
) : ViewModel() {
    private val scope: CoroutineScope = coroutineScope ?: viewModelScope
    private val _uiState = MutableStateFlow(PcdaoAuditUiState())
    val uiState: StateFlow<PcdaoAuditUiState> = _uiState.asStateFlow()

    private val monthContextOverrides: MutableMap<String, ActiveSituationalContext> = mutableMapOf()
    private val monthPresetOverrides: MutableMap<String, MissionPresetId?> = mutableMapOf()
    private val monthDismissedOverrides: MutableMap<String, MutableSet<String>> = mutableMapOf()
    private var calculationJob: Job? = null
    internal val activeCalculationJob: Job? get() = calculationJob

    init {
        observePayslips()
    }

    private fun observePayslips() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                payslipRepository.getAllPayslips().collect { list ->
                    val newestFirst = VaultMonthGroupMapper.sortNewestFirst(list)
                    val nextSelected =
                        _uiState.value.selectedPayslip
                            ?: initialSelectedPayslip?.let { init -> newestFirst.find { it.dateStr == init.dateStr } }
                            ?: newestFirst.firstOrNull()
                    if (nextSelected != null) {
                        applyPayslipSelection(newestFirst, nextSelected)
                    } else {
                        _uiState.update { it.copy(availablePayslips = newestFirst, isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun applyPayslipSelection(
        allPayslips: List<ParsedPayslip>,
        payslip: ParsedPayslip,
    ) {
        val key = monthKey(payslip)
        val context = monthContextOverrides[key] ?: autoInferer.inferActiveContext(payslip, allPayslips)
        val autoDetectedTiles = autoInferer.inferActiveContext(payslip, allPayslips).activeTileIds
        val presetId = monthPresetOverrides[key]
        val dismissed = monthDismissedOverrides[key]?.toSet() ?: emptySet()
        val recon = reconciler.reconcile(payslip, context, allPayslips)
        val milestones = milestoneAuditor.auditMilestones(allPayslips)
        val grouped = VaultMonthGroupMapper.groupByFinancialYear(allPayslips)

        _uiState.update {
            it.copy(
                availablePayslips = allPayslips,
                selectedPayslip = payslip,
                activeContext = context,
                autoInferredTileIds = autoDetectedTiles,
                activePresetId = presetId,
                dismissedDiscrepancyIds = dismissed,
                reconciliationResult = recon,
                careerMilestones = milestones,
                groupedMonths = grouped,
                isLoading = false,
                errorMessage = null,
            )
        }
        launchCalculation(context)
    }

    fun selectPayslip(payslip: ParsedPayslip) = applyPayslipSelection(_uiState.value.availablePayslips, payslip)

    fun selectCategory(category: SituationalCategory) = _uiState.update { it.copy(selectedCategory = category) }

    fun setFilter(filter: FindingFilter) = _uiState.update { it.copy(selectedFilter = filter) }

    fun setAddFactorSheetVisible(visible: Boolean) = _uiState.update { it.copy(isAddFactorSheetVisible = visible) }

    fun toggleCumulativeView() = _uiState.update { it.copy(isCumulativeViewActive = !it.isCumulativeViewActive) }

    fun applyMissionPreset(presetId: MissionPresetId) {
        val preset = SituationalMissionPresets.getById(presetId)
        val current = _uiState.value.activeContext
        val numChildren =
            when {
                preset.tileIds.contains(SituationalTileKeys.CEA_TWO_CHILDREN) -> 2
                preset.tileIds.contains(SituationalTileKeys.CEA_ONE_CHILD) -> 1
                else -> 0
            }
        val updatedContext =
            current.copy(
                activeTileIds = preset.tileIds,
                activeSpecializedFactors = preset.specializedFactors,
                sprCityTier = preset.sprCityTier,
                numberOfChildrenCea = numChildren,
                hasHostelChild = preset.tileIds.contains(SituationalTileKeys.CEA_HOSTEL),
            )
        recordContextOverride(updatedContext, presetId)
        _uiState.update { it.copy(activeContext = updatedContext, activePresetId = presetId) }
        launchCalculation(updatedContext)
    }

    fun toggleTile(tileId: String) {
        val currentContext = _uiState.value.activeContext
        val newTileIds = currentContext.activeTileIds.toMutableSet()
        if (newTileIds.contains(tileId)) {
            newTileIds.remove(tileId)
        } else {
            newTileIds.add(tileId)
            if (SituationalTileKeys.PEACE_STATION_KEYS.contains(tileId)) newTileIds.removeAll(SituationalTileKeys.PEACE_STATION_KEYS - tileId)
            if (SituationalTileKeys.HOUSING_KEYS.contains(tileId)) newTileIds.removeAll(SituationalTileKeys.HOUSING_KEYS - tileId)
            if (SituationalTileKeys.CEA_CHILD_KEYS.contains(tileId)) newTileIds.removeAll(SituationalTileKeys.CEA_CHILD_KEYS - tileId)
        }
        val updatedContext = PcdaoAuditViewModelHelpers.syncContextWithTile(currentContext, tileId, newTileIds)
        recordContextOverride(updatedContext, _uiState.value.activePresetId)
        _uiState.update { it.copy(activeContext = updatedContext) }
        launchCalculation(updatedContext)
    }

    fun toggleSpecializedFactor(factor: SpecializedMilitaryFactor) {
        val currentFactors = _uiState.value.activeContext.activeSpecializedFactors.toMutableSet()
        if (currentFactors.contains(factor)) currentFactors.remove(factor) else currentFactors.add(factor)
        val updatedContext = _uiState.value.activeContext.copy(activeSpecializedFactors = currentFactors)
        recordContextOverride(updatedContext, _uiState.value.activePresetId)
        _uiState.update { it.copy(activeContext = updatedContext) }
        launchCalculation(updatedContext)
    }

    fun dismissDiscrepancy(discrepancyId: String) {
        val key = _uiState.value.selectedPayslip?.let { monthKey(it) } ?: ""
        val currentDismissed = monthDismissedOverrides.getOrPut(key) { mutableSetOf() }
        currentDismissed.add(discrepancyId)
        _uiState.update { it.copy(dismissedDiscrepancyIds = currentDismissed.toSet()) }
    }

    fun resetDismissedDiscrepancies() {
        val key = _uiState.value.selectedPayslip?.let { monthKey(it) } ?: ""
        monthDismissedOverrides.remove(key)
        _uiState.update { it.copy(dismissedDiscrepancyIds = emptySet()) }
    }

    private fun recordContextOverride(
        context: ActiveSituationalContext,
        presetId: MissionPresetId?,
    ) {
        _uiState.value.selectedPayslip?.let { payslip ->
            val key = monthKey(payslip)
            monthContextOverrides[key] = context
            monthPresetOverrides[key] = presetId
        }
    }

    private fun monthKey(payslip: ParsedPayslip): String =
        "${payslip.year}-${payslip.monthNum.toString().padStart(2, '0')}"

    private fun launchCalculation(context: ActiveSituationalContext) {
        calculationJob?.cancel()
        calculationJob =
            scope.launch(defaultDispatcher) {
                val payslip = _uiState.value.selectedPayslip
                val allPayslips = _uiState.value.availablePayslips
                val recon = payslip?.let { reconciler.reconcile(it, context, allPayslips) }
                coroutineContext.ensureActive()
                val rollup = rollupEngine.calculateRollup(allPayslips, context)
                coroutineContext.ensureActive()
                val fixation = calculatePayFixation(payslip, context)
                coroutineContext.ensureActive()
                _uiState.update { it.copy(reconciliationResult = recon ?: it.reconciliationResult, cumulativeRollup = rollup, payFixationResult = fixation) }
            }
    }

    private suspend fun calculatePayFixation(
        payslip: ParsedPayslip?,
        context: ActiveSituationalContext,
    ): PayFixationResult? {
        if (payslip == null || !context.activeTileIds.contains(SituationalTileKeys.PROMOTION_ACTIVE)) return null
        return try {
            val payMatrix = rulesRepository.getPayMatrix()
            val fromLevel = _uiState.value.sandboxFromLevel ?: context.inferredFlags.inferredRankLevel ?: "10"
            val toLevel = _uiState.value.sandboxToLevel ?: PcdaoAuditViewModelHelpers.defaultPromotionalTarget(fromLevel)
            PayFixationOptimizer(payMatrix).optimizePromotion(
                PayFixationRequest(
                    fromLevel = fromLevel,
                    fromStage = 8,
                    toLevel = toLevel,
                    promotionDate = "2026-03-15",
                    dniMonth = 7,
                    mspMonthly = payslip.earnings.militaryServicePay.toInt().takeIf { it > 0 } ?: 15500,
                ),
            )
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            null
        }
    }

    fun setSandboxLevels(
        fromLevel: String,
        toLevel: String,
    ) {
        _uiState.update { it.copy(sandboxFromLevel = fromLevel, sandboxToLevel = toLevel) }
        launchCalculation(_uiState.value.activeContext)
    }

    fun generateRedressalLetter(maskPii: Boolean = false): RedressalLetter? {
        val payslip = _uiState.value.selectedPayslip ?: return null
        val request = PcdaoAuditViewModelHelpers.buildRedressalRequest(_uiState.value, payslip, maskPii) ?: return null
        val letter = RedressalLetterGenerator.generateLetter(request)
        _uiState.update { it.copy(generatedLetter = letter) }
        return letter
    }

    fun clearGeneratedLetter() = _uiState.update { it.copy(generatedLetter = null) }
}
