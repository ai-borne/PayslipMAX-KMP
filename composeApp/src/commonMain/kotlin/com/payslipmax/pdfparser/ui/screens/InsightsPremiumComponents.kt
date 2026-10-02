package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings

@Composable
fun PremiumToolsSection(
    onNavigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    payAuditFindings: Int = 0,
) {
    val tools = quickAccessTools()
    FlatBorderedCard(modifier = modifier, tint = CardTint.Accent) {
        Text(
            text = AppStringsPremium.premiumToolsTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        // Pay Audit is gated by ANOMALY_DETECTION but deliberately has no catalog target (the Dashboard
        // entry stays free), so its ribbon is built here from that row's gate and icon.
        PremiumToolCard(
            spec =
                featureMeta(FeatureGate.ANOMALY_DETECTION).copy(
                    title = PayAuditStrings.screenTitle,
                    description = PayAuditStrings.entryCardSubtitle,
                ),
            badge = payAuditBadgeLabel(payAuditFindings),
            onClick = { onNavigateTo(Screen.PayAudit) },
        )
        tools.forEach { tool ->
            PremiumToolCard(
                spec = tool,
                // Non-null by construction: quickAccessTools() filters out entries with a null target.
                onClick = { onNavigateTo(requireNotNull(tool.target)) },
            )
        }
    }
}

/**
 * Intentionally NOT migrated to [FlatBorderedCard]: this nested tool row has a genuinely filled
 * [MaterialTheme.colorScheme.primaryContainer] background (not just border-alpha drift), a distinct
 * visual identity from the plain flat-bordered cards — see [FlatBorderedCard]'s doc comment.
 */
@Composable
private fun PremiumToolCard(
    spec: PremiumFeatureMeta,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
            ),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(spec.icon, style = MaterialTheme.typography.titleLarge)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
            ) {
                PremiumToolTitle(title = spec.title, badge = badge)
                Text(
                    text = spec.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onClick) {
                Text(
                    text = InsightsStrings.premiumToolsOpenLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun PremiumToolTitle(
    title: String,
    badge: String?,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
