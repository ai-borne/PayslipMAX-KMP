package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.insights.DsopRoom
import com.payslipmax.pdfparser.insights.timeline.ChangeExplanation
import com.payslipmax.pdfparser.insights.timeline.NextIncrementPrediction
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline

enum class PayAuditTab { THIS_MONTH, HISTORY, PLAN_AHEAD }

/**
 * The one-glance answer for the selected month (docs/Plan Phase 2 step 0). [Waiting] and verified arrears
 * are never counted as issues: a waiting finding is unproven until a later payslip, a verified one matched.
 */
sealed interface PayAuditVerdict {
    data object NoPayslip : PayAuditVerdict

    data class Clean(val linesChecked: Int, val verifiedCount: Int) : PayAuditVerdict

    /** [amount] is set only for a single issue, where it is that issue's shortfall. */
    data class Issue(
        val count: Int,
        val labels: List<String>,
        val amount: Double?,
        /** True when at least one issue is proven and letter-eligible, so the card can offer a draft. */
        val canDraftLetter: Boolean,
    ) : PayAuditVerdict

    /** Free tier: the count and pay-line names stay visible, the amount and evidence do not. */
    data class LockedIssue(val count: Int, val labels: List<String>) : PayAuditVerdict

    /** [forMissingPayLine]: nothing waited on is an arrears credit (e.g. a held TPTA gap), so the copy must not talk of arrears. */
    data class Waiting(val count: Int, val forMissingPayLine: Boolean = false) : PayAuditVerdict
}

/** "12 months audited · 0 issues": waiting findings are deliberately left out of [issues]. */
data class PayAuditHistorySummary(val monthsAudited: Int, val issues: Int)

/** Findings for the selected month, split by what the user should do about each. Empty when locked. */
data class PayAuditMonthFindings(
    val issues: List<Anomaly> = emptyList(),
    val waiting: List<Anomaly> = emptyList(),
    val verified: List<Anomaly> = emptyList(),
)

data class PayAuditUiState(
    val selectedMonth: PayMonth? = null,
    val availableMonths: List<PayMonth> = emptyList(),
    val tab: PayAuditTab = PayAuditTab.THIS_MONTH,
    val verdict: PayAuditVerdict = PayAuditVerdict.NoPayslip,
    val history: PayAuditHistorySummary = PayAuditHistorySummary(0, 0),
    val isLocked: Boolean = false,
    val findings: PayAuditMonthFindings = PayAuditMonthFindings(),
    /** Findings kept out of [findings] because the free tier only sees counts. */
    val hiddenFindingCount: Int = 0,
    val changes: List<ChangeExplanation> = emptyList(),
    val allChanges: List<ChangeExplanation> = emptyList(),
    val timeline: ServiceTimeline = ServiceTimeline(emptyList(), emptyList(), emptyList()),
    val incrementPrediction: NextIncrementPrediction? = null,
    val dsopRoom: DsopRoom? = null,
)
