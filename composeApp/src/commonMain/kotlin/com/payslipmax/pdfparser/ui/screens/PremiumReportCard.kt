package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/**
 * Compact premium entry point: the AI/Gemini generate-&-view audit flow ([aiSectionContent]) plus a
 * collapsed/expandable full premium-tools list (reuses [PremiumToolsSection] so all four paid entry
 * points — Tax Planner, DSOP Simulator, Claim Generator, Retirement Calculators — stay reachable).
 * Wealth-optimization figures live in [MonthlySnapshot]'s Pay Health chip, so this card no longer
 * duplicates them. Only ever reached from the PRO-dissolve path (Insights PRO consolidation, Phase 2)
 * — the free-tier teaser now lives in [LockedPremiumHubCard].
 */
@Composable
fun PremiumReportCard(
    toolsExpanded: Boolean,
    onToolsExpandClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
    aiSectionContent: @Composable () -> Unit,
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
                text = "${AppStrings.proTeaserCrownIcon} ${InsightsStrings.premiumReportTitle}${InsightsStrings.premiumActivatedSuffix}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            aiSectionContent()
            PremiumToolsExpandHeader(expanded = toolsExpanded, onClick = onToolsExpandClick)
            if (toolsExpanded) {
                PremiumToolsSection(onNavigateTo = onNavigateTo)
            }
        }
    }
}

@Composable
private fun PremiumToolsExpandHeader(
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (expanded) InsightsStrings.premiumReportHideToolsLabel else InsightsStrings.premiumReportViewAllToolsLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription =
                if (expanded) InsightsStrings.premiumReportToolsCollapseDesc else InsightsStrings.premiumReportToolsExpandDesc,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
