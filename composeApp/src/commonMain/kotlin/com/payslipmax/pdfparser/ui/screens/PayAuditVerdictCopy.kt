package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.ui.theme.PayAuditVerdictStrings as S

enum class VerdictTone { OK, ISSUE, WAITING, LOCKED, NEUTRAL }

data class VerdictCopy(
    val label: String,
    val headline: String,
    val subtitle: String,
    val tone: VerdictTone,
    val cta: String? = null,
)

/** Wording for the one-glance verdict card. Pure so the acceptance copy is unit-tested without Compose. */
fun payAuditVerdictCopy(
    verdict: PayAuditVerdict,
    month: PayMonth,
): VerdictCopy {
    val monthText = formatPayMonth(month)
    return when (verdict) {
        PayAuditVerdict.NoPayslip -> VerdictCopy(S.verdictLabelNoPayslip, S.noPayslipHeadline, S.noPayslipSubtitle, VerdictTone.NEUTRAL)
        is PayAuditVerdict.Clean ->
            VerdictCopy(
                S.verdictLabelCorrect,
                "$monthText${S.cleanHeadlineMiddle}${verdict.linesChecked}${S.cleanHeadlineSuffix}",
                if (verdict.verifiedCount > 0) "${S.cleanVerifiedPrefix}${verdict.verifiedCount}" else S.cleanSubtitle,
                VerdictTone.OK,
            )
        is PayAuditVerdict.Issue ->
            VerdictCopy(
                S.verdictLabelIssue,
                issueHeadline(verdict),
                "$monthText. ${if (verdict.count == 1) S.issueCountSingular else "${verdict.count}${S.issueCountPluralSuffix}"}",
                VerdictTone.ISSUE,
                if (verdict.canDraftLetter) S.seeWhyAndDraftCta else S.seeEvidenceCta,
            )
        is PayAuditVerdict.LockedIssue ->
            VerdictCopy(
                S.verdictLabelIssue,
                "${S.lockedHeadlinePrefix}${verdict.labels.joinToString(", ")}",
                "$monthText. ${S.lockedSubtitle}",
                VerdictTone.LOCKED,
                S.lockedCta,
            )
        is PayAuditVerdict.Waiting ->
            VerdictCopy(S.verdictLabelWaiting, "${verdict.count}${S.waitingHeadlineSuffix}", "$monthText. ${S.waitingSubtitle}", VerdictTone.WAITING)
    }
}

private fun issueHeadline(verdict: PayAuditVerdict.Issue): String {
    val amount = verdict.amount
    return if (verdict.count == 1 && amount != null) {
        "${verdict.labels.single()} ${formatCurrency(amount)}${S.issueShortSuffix}"
    } else {
        "${verdict.count} issues: ${verdict.labels.joinToString(", ")}"
    }
}

/** "20 months audited · 1 issue": waiting findings are deliberately not in the issue count. */
fun payAuditHistoryLine(summary: PayAuditHistorySummary): String {
    val issues =
        when (summary.issues) {
            0 -> S.historyZeroIssues
            1 -> S.historyIssuesSingular
            else -> "${summary.issues}${S.historyIssuesPluralSuffix}"
        }
    return "${summary.monthsAudited}${S.historyMonthsSuffix}$issues"
}
