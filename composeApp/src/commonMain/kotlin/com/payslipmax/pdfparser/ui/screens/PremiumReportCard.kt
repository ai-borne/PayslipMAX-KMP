package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/**
 * Compact premium entry point: the AI/Gemini generate-&-view audit flow ([aiSectionContent]) plus a
 * collapsed/expandable full premium-tools list (reuses [PremiumToolsSection] so all four paid entry
 * points — Tax Planner, DSOP Simulator, Claim Generator, Retirement Calculators — stay reachable).
 * Wealth-optimization figures live in [MonthlySnapshot]'s Pay Health chip, so this card no longer
 * duplicates them. Only ever reached from the Premium-dissolve path (Insights Premium consolidation, Phase 2)
 * — the free-tier teaser now lives in [LockedPremiumHubCard].
 */
@Composable
fun PremiumReportCard(
    toolsExpanded: Boolean,
    onToolsExpandClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, tint = CardTint.Accent) {
        Text(
            text = "${AppStrings.premiumTeaserCrownIcon} ${InsightsStrings.premiumReportTitle}${InsightsStrings.premiumActivatedSuffix}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        PremiumToolsExpandHeader(expanded = toolsExpanded, onClick = onToolsExpandClick)
        if (toolsExpanded) {
            PremiumToolsSection(onNavigateTo = onNavigateTo)
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
