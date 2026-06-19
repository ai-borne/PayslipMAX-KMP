package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PremiumToolsSection(
    isPremiumEnabled: Boolean,
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(
                text = AppStrings.premiumToolsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            PremiumButtonsRow(
                isPremiumEnabled = isPremiumEnabled,
                onNavigateTo = onNavigateTo,
                onUpgradeClick = onUpgradeClick,
            )
        }
    }
}

@Composable
private fun PremiumButtonsRow(
    isPremiumEnabled: Boolean,
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
    onUpgradeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        val onClick = { target: com.ssbmax.pdfparser.Screen ->
            if (isPremiumEnabled) onNavigateTo(target) else onUpgradeClick()
        }
        OutlinedButton(
            onClick = { onClick(com.ssbmax.pdfparser.Screen.Representation) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.premiumToolsDraftClaims, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        OutlinedButton(
            onClick = { onClick(com.ssbmax.pdfparser.Screen.TaxPlanning) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.premiumToolsTaxPlanner, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        OutlinedButton(
            onClick = { onClick(com.ssbmax.pdfparser.Screen.RetirementPlanning) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.premiumToolsDsopSimulator, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
