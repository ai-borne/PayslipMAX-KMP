package com.payslipmax.pdfparser.ui.pcdao

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
import com.payslipmax.pdfparser.ui.theme.AppDimensions

/**
 * Executive AI Audit Banner Card rendered prominently on the primary Dashboard.
 * Informs officers of their automated IRLA & 7th CPC audit status and provides 1-tap entry
 * to PayslipMax AI.
 */
@Composable
fun DashboardAuditBannerCard(
    payslipsCount: Int,
    onAuditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AppDimensions.CornerRadius))
                .clickable(onClick = onAuditClick)
                .testTag("dashboard_audit_banner_card"),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            BannerHeaderRow(payslipsCount = payslipsCount)
            BannerCtaButton(onAuditClick = onAuditClick)
        }
    }
}

@Composable
private fun BannerHeaderRow(payslipsCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(AppDimensions.IconSizeExtraLarge)
                    .clip(RoundedCornerShape(AppDimensions.CornerRadiusMedium))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = AppStringsPcdao.screenIcon,
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(modifier = Modifier.width(AppDimensions.SpacingMedium))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = AppStringsPcdao.dashboardBannerTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
            Text(
                text = AppStringsPcdao.formatDashboardStatementsAnalyzed(payslipsCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BannerCtaButton(onAuditClick: () -> Unit) {
    Button(
        onClick = onAuditClick,
        modifier = Modifier.fillMaxWidth().testTag("dashboard_audit_cta"),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
    ) {
        Text(
            text = AppStringsPcdao.dashboardBannerCta,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
