package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditEntryStrings

/**
 * Dashboard discovery banner for Pay Audit (ported from pay_audit_1.0, docs/Plan Phase 3). It makes no claim
 * about findings or amounts: only the audit screen, backed by the payslips, may say a month is right or wrong.
 */
@Composable
fun DashboardAuditBannerCard(
    payslipsCount: Int,
    onAuditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(AppDimensions.CornerRadius)
    Card(
        modifier = modifier.fillMaxWidth().clip(shape).clickable(onClick = onAuditClick).testTag("dashboard_audit_banner_card"),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = PayAuditEntryStrings.dashboardBannerTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = PayAuditEntryStrings.dashboardBannerSubtitle(payslipsCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onAuditClick, modifier = Modifier.fillMaxWidth().testTag("dashboard_audit_cta")) {
                Text(PayAuditEntryStrings.dashboardBannerCta)
            }
        }
    }
}
