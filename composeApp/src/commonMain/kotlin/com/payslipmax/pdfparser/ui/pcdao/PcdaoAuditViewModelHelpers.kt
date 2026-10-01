package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.reconciliation.ActiveSituationalContext
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.redressal.RedressalLetterGenerator
import com.payslipmax.pcdao.redressal.RedressalRequest
import com.payslipmax.pdfparser.domain.ParsedPayslip

internal object PcdaoAuditViewModelHelpers {
    fun defaultPromotionalTarget(from: String): String =
        when (from) {
            "10" -> "11"
            "11" -> "12A"
            "12A" -> "13"
            "13" -> "13A"
            "13A" -> "14"
            else -> "11"
        }

    fun syncContextWithTile(
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
        return context.copy(activeTileIds = newTiles, numberOfChildrenCea = numChildren, hasHostelChild = hasHostel)
    }

    fun buildRedressalRequest(
        state: PcdaoAuditUiState,
        payslip: ParsedPayslip,
        maskPii: Boolean,
    ): RedressalRequest? {
        val inferredLevel = state.activeContext.inferredFlags.inferredRankLevel
        val inferredRank = RedressalLetterGenerator.inferRankFromPayLevel(inferredLevel, payslip.earnings.basicPay)
        return if (state.isCumulativeViewActive && state.hasCumulativeArrears && state.cumulativeRollup != null) {
            RedressalLetterGenerator.createRequestFromCumulativeRollup(
                payslip = payslip,
                rollup = state.cumulativeRollup,
                rank = inferredRank,
                maskPii = maskPii,
            )
        } else {
            val recon = state.reconciliationResult ?: return null
            RedressalLetterGenerator.createRequestFromReconciliation(
                payslip = payslip,
                reconciliationResult = recon,
                rank = inferredRank,
                maskPii = maskPii,
            )
        }
    }
}
