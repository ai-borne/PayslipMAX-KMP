package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.engine.PayFixationResult
import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.MissionPresetId
import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciliationResult
import com.payslipmax.pcdao.reconciliation.SituationalCategory
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.redressal.RedressalLetter
import com.payslipmax.pcdao.timeline.CareerMilestone
import com.payslipmax.pcdao.timeline.CumulativeArrearsRollup
import com.payslipmax.pdfparser.domain.ParsedPayslip

/**
 * Filter tabs for the Intelligence Feed.
 */
enum class FindingFilter {
    ALL,
    ENTITLEMENTS,
    HAZARDS,
    ALARMS,
    TAX_SHIELD,
}

/**
 * Single source of truth UI state for the PCDA(O) Audit Cockpit Screen.
 */
data class PcdaoAuditUiState(
    val availablePayslips: List<ParsedPayslip> = emptyList(),
    val selectedPayslip: ParsedPayslip? = null,
    val activeContext: ActiveSituationalContext = ActiveSituationalContext(),
    val autoInferredTileIds: Set<String> = emptySet(),
    val activePresetId: MissionPresetId? = null,
    val selectedCategory: SituationalCategory = SituationalCategory.POSTING,
    val selectedFilter: FindingFilter = FindingFilter.ALL,
    val isAddFactorSheetVisible: Boolean = false,
    val reconciliationResult: ShadowLedgerReconciliationResult? = null,
    val payFixationResult: PayFixationResult? = null,
    val generatedLetter: RedressalLetter? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val cumulativeRollup: CumulativeArrearsRollup? = null,
    val careerMilestones: List<CareerMilestone> = emptyList(),
    val groupedMonths: Map<String, List<ParsedPayslip>> = emptyMap(),
    val isCumulativeViewActive: Boolean = false,
    val sandboxFromLevel: String? = null,
    val sandboxToLevel: String? = null,
    val dismissedDiscrepancyIds: Set<String> = emptySet(),
) {
    val filteredDiscrepancies: List<AuditDiscrepancy>
        get() {
            val list = reconciliationResult?.discrepancies ?: return emptyList()
            val activeList = if (dismissedDiscrepancyIds.isEmpty()) list else list.filterNot { it.id in dismissedDiscrepancyIds }
            return when (selectedFilter) {
                FindingFilter.ALL -> activeList
                FindingFilter.ENTITLEMENTS -> activeList.filter { it.type == DiscrepancyType.UNDERPAYMENT }
                FindingFilter.HAZARDS -> activeList.filter { it.type == DiscrepancyType.RECOVERY_HAZARD }
                FindingFilter.ALARMS ->
                    activeList.filter {
                        it.severity == DiscrepancySeverity.CRITICAL || it.type == DiscrepancyType.FORFEITURE_RISK
                    }
                FindingFilter.TAX_SHIELD ->
                    activeList.filter {
                        it.type == DiscrepancyType.TAX_EXPOSURE || it.authority.contains("10(11)", ignoreCase = true)
                    }
            }
        }

    val isPromotionActive: Boolean
        get() = activeContext.activeTileIds.contains(SituationalTileKeys.PROMOTION_ACTIVE)

    val hasCumulativeArrears: Boolean
        get() = (cumulativeRollup?.totalUnderpaidArrears ?: 0.0) > 0.0 && availablePayslips.size > 1

    val unclaimedTotal: Double
        get() =
            if (isCumulativeViewActive && hasCumulativeArrears) {
                cumulativeRollup?.totalUnderpaidArrears ?: (reconciliationResult?.totalUnclaimedAnnual ?: 0.0)
            } else {
                reconciliationResult?.totalUnclaimedAnnual ?: 0.0
            }

    val hazardTotal: Double
        get() =
            if (isCumulativeViewActive && hasCumulativeArrears) {
                cumulativeRollup?.totalRecoveryHazard ?: (reconciliationResult?.totalRecoveryHazard ?: 0.0)
            } else {
                reconciliationResult?.totalRecoveryHazard ?: 0.0
            }

    val alarmsCount: Int
        get() =
            reconciliationResult?.discrepancies
                ?.filterNot { it.id in dismissedDiscrepancyIds }
                ?.count { it.severity == DiscrepancySeverity.CRITICAL || it.type == DiscrepancyType.FORFEITURE_RISK }
                ?: 0
}
