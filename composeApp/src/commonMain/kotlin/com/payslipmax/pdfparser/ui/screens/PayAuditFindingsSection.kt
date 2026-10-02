package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.Anomaly
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditVerdictStrings as S

private enum class FindingKind { ISSUE, WAITING, VERIFIED }

/**
 * "This month" findings as evidence cards (docs/Plan Phase 2 U6): pay line, should-be / credited /
 * difference, a "Why?" expander with the authority, and "Draft letter" only for proven findings. Verified
 * arrears are shown (the user marked them genuine) but styled as good news, never as an issue. When the
 * free tier hides the evidence, [hiddenCount] drives one locked card instead.
 */
fun LazyListScope.payAuditFindingsItems(
    findings: PayAuditMonthFindings,
    hiddenCount: Int,
    onUnlockClick: () -> Unit,
    onDraftLetter: () -> Unit,
) {
    val rows = findings.issues.map { it to FindingKind.ISSUE } + findings.waiting.map { it to FindingKind.WAITING } + findings.verified.map { it to FindingKind.VERIFIED }
    itemsIndexed(
        rows,
        // (type, month) is not unique: the basic-DA and TPTA-DA arrears checks share both.
        key = { index, (a, _) -> "finding_${a.type}_${a.month}_$index" },
        contentType = { _, _ -> "finding_card" },
    ) { _, (anomaly, kind) ->
        PayAuditFindingCard(anomaly = anomaly, kind = kind, onDraftLetter = onDraftLetter)
    }
    if (hiddenCount > 0) {
        item(key = "pay_audit_findings_locked", contentType = "locked_card") { PayAuditLockedCard(onUnlockClick) }
    }
}

@Composable
private fun PayAuditFindingCard(
    anomaly: Anomaly,
    kind: FindingKind,
    onDraftLetter: () -> Unit,
) {
    var showWhy by rememberSaveable { mutableStateOf(false) }
    val tone =
        when (kind) {
            FindingKind.ISSUE -> MaterialTheme.colorScheme.error
            FindingKind.WAITING -> MaterialTheme.colorScheme.tertiary
            FindingKind.VERIFIED -> MaterialTheme.colorScheme.secondary
        }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(AppDimensions.BorderThin, tone.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            val kicker =
                when (kind) {
                    FindingKind.ISSUE -> S.kickerIssue
                    FindingKind.WAITING -> S.kickerWaiting
                    FindingKind.VERIFIED -> S.kickerVerified
                }
            Text(kicker, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = tone)
            Text(payLineLabel(anomaly.field), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            PayAuditEvidenceRow(anomaly, kind)
            Text(anomaly.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (kind == FindingKind.WAITING) Text(S.waitingNote, style = MaterialTheme.typography.bodySmall)
            PayAuditFindingActions(anomaly, showWhy, onToggleWhy = { showWhy = !showWhy }, onDraftLetter = onDraftLetter)
        }
    }
}

@Composable
private fun PayAuditEvidenceRow(
    anomaly: Anomaly,
    kind: FindingKind,
) {
    val expected = anomaly.expected
    val actual = anomaly.actual
    if (expected == null || actual == null) return
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge)) {
        PayAuditEvidenceCell(S.evidenceShouldBe, formatCurrency(expected))
        PayAuditEvidenceCell(S.evidenceCredited, formatCurrency(actual))
        if (kind != FindingKind.WAITING) PayAuditEvidenceCell(S.evidenceDifference, formatCurrency(expected - actual))
    }
}

@Composable
private fun PayAuditEvidenceCell(
    label: String,
    value: String,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PayAuditFindingActions(
    anomaly: Anomaly,
    showWhy: Boolean,
    onToggleWhy: () -> Unit,
    onDraftLetter: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        OutlinedButton(onClick = onToggleWhy) { Text(if (showWhy) S.hideWhyButton else S.whyButton) }
        if (anomaly.canDraftLetter()) Button(onClick = onDraftLetter) { Text(S.draftLetterButton) }
    }
    if (showWhy) {
        Text(
            text = anomaly.authority?.let { "${S.whyAuthorityPrefix}$it" } ?: S.whyNoAuthority,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PayAuditLockedCard(onUnlockClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            Text(S.kickerLocked, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text(S.lockedCardNote, style = MaterialTheme.typography.bodySmall)
            Button(onClick = onUnlockClick) { Text(S.lockedCardCta) }
        }
    }
}
