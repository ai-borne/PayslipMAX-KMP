package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.pcdao.AppStringsPcdao
import com.payslipmax.pdfparser.ui.theme.AppDimensions

/**
 * Contextual action banner embedded in the Payslip Digital Replica screen.
 * Allows officers to audit the exact statement currently in view with 1 tap.
 */
@Composable
fun ReplicaAuditActionCard(
    onAuditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusMedium))
                .clickable(onClick = onAuditClick)
                .testTag("replica_audit_action_card"),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReplicaIconBadge()
            Spacer(modifier = Modifier.width(AppDimensions.SpacingMedium))
            ReplicaCardTextColumn(modifier = Modifier.weight(1f))
            Text(
                text = "→",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = AppDimensions.SpacingSmall),
            )
        }
    }
}

@Composable
private fun ReplicaIconBadge() {
    Box(
        modifier =
            Modifier
                .size(AppDimensions.IconSizeExtraLarge)
                .clip(RoundedCornerShape(AppDimensions.CornerRadiusSmall))
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = AppStringsPcdao.screenIcon,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ReplicaCardTextColumn(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = AppStringsPcdao.replicaAuditCta,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text = AppStringsPcdao.screenDescription,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
