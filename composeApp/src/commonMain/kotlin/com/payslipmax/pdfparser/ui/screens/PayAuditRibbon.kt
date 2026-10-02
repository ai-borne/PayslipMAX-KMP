package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings
import org.koin.compose.koinInject

/** Badge text for the Pay Audit ribbon, or `null` on a clean month so the ribbon stays quiet when nothing is wrong. */
fun payAuditBadgeLabel(findings: Int): String? =
    when {
        findings <= 0 -> null
        findings == 1 -> "1 ${PayAuditStrings.badgeSingular}"
        else -> "$findings ${PayAuditStrings.badgePlural}"
    }

/**
 * The selected month's issue count for the Pay Audit ribbon. Runs the same [PayAuditViewModel] pipeline as
 * the Pay Audit screen so the badge always matches what the screen shows (waiting and verified arrears are
 * not issues). Gating is irrelevant to a count — the ribbon only exists inside the Premium tools list — so
 * it analyses as unlocked.
 */
@Composable
fun rememberPayAuditFindingsCount(
    selected: ParsedPayslip,
    payslips: List<ParsedPayslip>,
    viewModel: PayAuditViewModel = koinInject(),
): Int {
    DisposableEffect(viewModel) { onDispose { viewModel.dispose() } }
    LaunchedEffect(selected, payslips) {
        viewModel.setInputs(payslips, hasAccess = true, requestedMonth = PayMonth(selected.year, selected.monthNum))
    }
    val state by viewModel.uiState.collectAsState()
    return when (val verdict = state.verdict) {
        is PayAuditVerdict.Issue -> verdict.count
        is PayAuditVerdict.LockedIssue -> verdict.count
        else -> 0
    }
}
