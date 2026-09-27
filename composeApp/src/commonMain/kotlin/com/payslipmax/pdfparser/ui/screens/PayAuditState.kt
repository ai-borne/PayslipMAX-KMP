package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.DeterministicIntelligenceEngine
import com.payslipmax.pdfparser.insights.EngineResult

/**
 * Pay Audit's own engine call (docs/Plan/09_PayAudit_PhasePlan.md Phase 7 carry-over), sourced from the
 * full [ParsedPayslip] history already in memory ([com.payslipmax.pdfparser.ui.PayslipUiState.payslips])
 * rather than the thin `LedgerRecordEntity` ledger [rememberInsightsState] uses. `LedgerRecordEntity`
 * carries none of Risk & Hardship/Field allowance, licence fee, arrears or `needsReview`, so the
 * timeline/change-explanation/finding rules built on those fields never actually fire against it. Kept
 * separate from [InsightsState] so this fix is scoped to the Pay Audit screen only, without touching the
 * other Insights consumers (MonthlySnapshot, PayTrendChart, Smart Insights) that already work against
 * [LedgerRecordEntity][com.payslipmax.pdfparser.database.LedgerRecordEntity] fields.
 */
@Composable
fun rememberPayAuditEngineResult(
    selected: ParsedPayslip,
    payslips: List<ParsedPayslip>,
): EngineResult {
    val historySorted =
        remember(payslips) {
            payslips.sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum })
        }
    val previous = remember(historySorted, selected) { findPreviousPayslip(selected, historySorted) }
    return remember(selected, previous, historySorted) {
        DeterministicIntelligenceEngine.analyze(selected, previous, historySorted)
    }
}

private fun findPreviousPayslip(
    current: ParsedPayslip,
    historySorted: List<ParsedPayslip>,
): ParsedPayslip? =
    historySorted.find {
        val isPrevYear = it.year == current.year && it.monthNum == current.monthNum - 1
        val isDecToJan = it.year == current.year - 1 && it.monthNum == 12 && current.monthNum == 1
        isPrevYear || isDecToJan
    }
