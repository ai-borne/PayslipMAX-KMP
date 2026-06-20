package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.Screen
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings
import com.ssbmax.pdfparser.ui.theme.InsightsStrings

private data class PremiumToolSpec(
    val icon: String,
    val title: String,
    val valueProp: String,
    val target: Screen,
)

@Composable
fun PremiumToolsSection(
    isPremiumEnabled: Boolean,
    onNavigateTo: (Screen) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tools =
        listOf(
            PremiumToolSpec("📋", AppStrings.premiumToolsDraftClaims, InsightsStrings.premiumToolsDraftClaimsValueProp, Screen.Representation),
            PremiumToolSpec("📊", AppStrings.premiumToolsTaxPlanner, InsightsStrings.premiumToolsTaxPlannerValueProp, Screen.TaxPlanning),
            PremiumToolSpec("📈", AppStrings.premiumToolsDsopSimulator, InsightsStrings.premiumToolsDsopValueProp, Screen.RetirementPlanning),
        )
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(
                text = AppStrings.premiumToolsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            tools.forEach { tool ->
                PremiumToolCard(
                    spec = tool,
                    isPremiumEnabled = isPremiumEnabled,
                    onClick = { if (isPremiumEnabled) onNavigateTo(tool.target) else onUpgradeClick() },
                )
            }
        }
    }
}

@Composable
fun ProFeaturesTeaser(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            ProTeaserHeader()
            ProFeatureRow(
                icon = AppStrings.proTeaserAiIcon,
                title = AppStrings.settingsAiInsightsLockedTitle,
                detail = InsightsStrings.proTeaserAiDetail,
            )
            ProFeatureRow(
                icon = AppStrings.proTeaserToolsIcon,
                title = AppStrings.premiumToolsTitle,
                detail = InsightsStrings.proTeaserToolsDetail,
            )
            Button(onClick = onUpgradeClick, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = AppStrings.settingsProUpgradeBtn,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ProTeaserHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Text(AppStrings.proTeaserCrownIcon, style = MaterialTheme.typography.titleMedium)
        Column {
            Text(
                text = AppStrings.settingsProPlanTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = AppStrings.settingsProPlanPrice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProFeatureRow(icon: String, title: String, detail: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, style = MaterialTheme.typography.bodyLarge)
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PremiumToolCard(
    spec: PremiumToolSpec,
    isPremiumEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isPremiumEnabled) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
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
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = spec.valueProp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onClick) {
                Text(
                    text = if (isPremiumEnabled) InsightsStrings.premiumToolsOpenLabel else InsightsStrings.premiumToolsLockedLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
