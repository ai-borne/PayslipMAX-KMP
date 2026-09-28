package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.engine.PayFixationResult
import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciliationResult
import com.payslipmax.pcdao.reconciliation.SituationalCategory
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.redressal.RedressalLetter
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
    val selectedCategory: SituationalCategory = SituationalCategory.POSTING,
    val selectedFilter: FindingFilter = FindingFilter.ALL,
    val isAddFactorSheetVisible: Boolean = false,
    val reconciliationResult: ShadowLedgerReconciliationResult? = null,
    val payFixationResult: PayFixationResult? = null,
    val generatedLetter: RedressalLetter? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
) {
    val filteredDiscrepancies: List<AuditDiscrepancy>
        get() {
            val list = reconciliationResult?.discrepancies ?: return emptyList()
            return when (selectedFilter) {
                FindingFilter.ALL -> list
                FindingFilter.ENTITLEMENTS -> list.filter { it.type == DiscrepancyType.UNDERPAYMENT }
                FindingFilter.HAZARDS -> list.filter { it.type == DiscrepancyType.RECOVERY_HAZARD }
                FindingFilter.ALARMS ->
                    list.filter {
                        it.severity == DiscrepancySeverity.CRITICAL || it.type == DiscrepancyType.FORFEITURE_RISK
                    }
                FindingFilter.TAX_SHIELD ->
                    list.filter {
                        it.type == DiscrepancyType.TAX_EXPOSURE || it.authority.contains("10(11)", ignoreCase = true)
                    }
            }
        }

    val isPromotionActive: Boolean
        get() = activeContext.activeTileIds.contains(SituationalTileKeys.PROMOTION_ACTIVE)

    val unclaimedTotal: Double
        get() = reconciliationResult?.totalUnclaimedAnnual ?: 0.0

    val hazardTotal: Double
        get() = reconciliationResult?.totalRecoveryHazard ?: 0.0

    val alarmsCount: Int
        get() = reconciliationResult?.criticalAlarmCount ?: 0
}
