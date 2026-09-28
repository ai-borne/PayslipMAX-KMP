package com.payslipmax.pdfparser.ui.pcdao

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.payslipmax.pcdao.engine.PayFixationOptimizer
import com.payslipmax.pcdao.engine.PayFixationRequest
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciler
import com.payslipmax.pcdao.reconciliation.SituationalAutoInferer
import com.payslipmax.pcdao.reconciliation.SituationalCategory
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
import kotlinx.coroutines.CoroutineScope
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
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val scope: CoroutineScope = coroutineScope ?: viewModelScope
    private val _uiState = MutableStateFlow(PcdaoAuditUiState())
    val uiState: StateFlow<PcdaoAuditUiState> = _uiState.asStateFlow()

    init {
        observePayslips()
    }

    private fun observePayslips() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                payslipRepository.getAllPayslips().collect { list ->
                    val newestFirst = VaultMonthGroupMapper.sortNewestFirst(list)
                    val nextSelected = _uiState.value.selectedPayslip ?: newestFirst.firstOrNull()
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
        val initialContext = autoInferer.inferActiveContext(payslip)
        val autoDetectedTiles = initialContext.activeTileIds
        val recon = reconciler.reconcile(payslip, initialContext)
        val rollup = rollupEngine.calculateRollup(allPayslips, initialContext)
        val milestones = milestoneAuditor.auditMilestones(allPayslips)
        val grouped = VaultMonthGroupMapper.groupByFinancialYear(allPayslips)

        _uiState.update {
            it.copy(
                availablePayslips = allPayslips,
                selectedPayslip = payslip,
                activeContext = initialContext,
                autoInferredTileIds = autoDetectedTiles,
                reconciliationResult = recon,
                cumulativeRollup = rollup,
                careerMilestones = milestones,
                groupedMonths = grouped,
                isLoading = false,
                errorMessage = null,
            )
        }
        recalculatePayFixationIfActive(payslip, initialContext)
    }

    fun selectPayslip(payslip: ParsedPayslip) {
        applyPayslipSelection(_uiState.value.availablePayslips, payslip)
    }

    fun selectCategory(category: SituationalCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun setFilter(filter: FindingFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun setAddFactorSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(isAddFactorSheetVisible = visible) }
    }

    fun toggleCumulativeView() {
        _uiState.update { it.copy(isCumulativeViewActive = !it.isCumulativeViewActive) }
    }

    fun toggleTile(tileId: String) {
        val currentContext = _uiState.value.activeContext
        val newTileIds = currentContext.activeTileIds.toMutableSet()
        if (newTileIds.contains(tileId)) {
            newTileIds.remove(tileId)
        } else {
            newTileIds.add(tileId)
        }

        val updatedContext = syncContextWithTile(currentContext, tileId, newTileIds)
        val payslip = _uiState.value.selectedPayslip
        val newRecon = payslip?.let { reconciler.reconcile(it, updatedContext) }
        val newRollup = rollupEngine.calculateRollup(_uiState.value.availablePayslips, updatedContext)

        _uiState.update {
            it.copy(
                activeContext = updatedContext,
                reconciliationResult = newRecon ?: it.reconciliationResult,
                cumulativeRollup = newRollup,
            )
        }

        if (payslip != null) recalculatePayFixationIfActive(payslip, updatedContext)
    }

    private fun syncContextWithTile(
        context: ActiveSituationalContext,
        toggledTile: String,
        newTiles: Set<String>,
    ): ActiveSituationalContext {
        var numChildren = context.numberOfChildrenCea
        var hasHostel = context.hasHostelChild

        when (toggledTile) {
            SituationalTileKeys.CEA_NONE -> numChildren = 0
            SituationalTileKeys.CEA_ONE_CHILD -> numChildren = if (newTiles.contains(toggledTile)) 1 else 0
            SituationalTileKeys.CEA_TWO_CHILDREN -> numChildren = if (newTiles.contains(toggledTile)) 2 else 0
            SituationalTileKeys.CEA_HOSTEL -> hasHostel = newTiles.contains(toggledTile)
        }

        return context.copy(
            activeTileIds = newTiles,
            numberOfChildrenCea = numChildren,
            hasHostelChild = hasHostel,
        )
    }

    fun toggleSpecializedFactor(factor: SpecializedMilitaryFactor) {
        val currentFactors = _uiState.value.activeContext.activeSpecializedFactors.toMutableSet()
        if (currentFactors.contains(factor)) {
            currentFactors.remove(factor)
        } else {
            currentFactors.add(factor)
        }

        val updatedContext = _uiState.value.activeContext.copy(activeSpecializedFactors = currentFactors)
        val payslip = _uiState.value.selectedPayslip
        val newRecon = payslip?.let { reconciler.reconcile(it, updatedContext) }
        val newRollup = rollupEngine.calculateRollup(_uiState.value.availablePayslips, updatedContext)

        _uiState.update {
            it.copy(
                activeContext = updatedContext,
                reconciliationResult = newRecon ?: it.reconciliationResult,
                cumulativeRollup = newRollup,
            )
        }
    }

    private fun recalculatePayFixationIfActive(
        payslip: ParsedPayslip,
        context: ActiveSituationalContext,
    ) {
        if (!context.activeTileIds.contains(SituationalTileKeys.PROMOTION_ACTIVE)) {
            _uiState.update { it.copy(payFixationResult = null) }
            return
        }

        scope.launch {
            try {
                val payMatrix = rulesRepository.getPayMatrix()
                val fromLevel = context.inferredFlags.inferredRankLevel ?: "10"
                val toLevel = if (fromLevel == "10") "11" else "12A"
                val optimizer = PayFixationOptimizer(payMatrix)
                val result =
                    optimizer.optimizePromotion(
                        PayFixationRequest(
                            fromLevel = fromLevel,
                            fromStage = 8,
                            toLevel = toLevel,
                            promotionDate = "2026-03-15",
                            dniMonth = 7,
                            mspMonthly = payslip.earnings.militaryServicePay.toInt().takeIf { it > 0 } ?: 15500,
                        ),
                    )
                _uiState.update { it.copy(payFixationResult = result) }
            } catch (e: Exception) {
                // Keep UI functional even if matrix load fails
            }
        }
    }

    fun generateRedressalLetter(): RedressalLetter? {
        val payslip = _uiState.value.selectedPayslip ?: return null
        val state = _uiState.value
        val request =
            if (state.isCumulativeViewActive && state.hasCumulativeArrears && state.cumulativeRollup != null) {
                RedressalLetterGenerator.createRequestFromCumulativeRollup(payslip, state.cumulativeRollup)
            } else {
                val recon = state.reconciliationResult ?: return null
                RedressalLetterGenerator.createRequestFromReconciliation(payslip, recon)
            }
        val letter = RedressalLetterGenerator.generateLetter(request)
        _uiState.update { it.copy(generatedLetter = letter) }
        return letter
    }

    fun clearGeneratedLetter() {
        _uiState.update { it.copy(generatedLetter = null) }
    }
}
