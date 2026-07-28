package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.insights.InsightSeverity
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import com.payslipmax.pdfparser.ui.theme.severityColor

/**
 * Canonical border tint for [FlatBorderedCard] — collapses the ad-hoc border-alpha values (0.08–0.3)
 * that had drifted across the hand-rolled Insights/Retirement/Tax cards into two deliberate looks.
 * [Neutral] matches a plain informational card; [Accent] matches a premium/highlighted card and pairs
 * with a primary-tinted header text (cards with a genuinely filled background, e.g.
 * [RetirementHeroSummaryCard], intentionally don't use this primitive — see their own file).
 */
enum class CardTint { Neutral, Accent }

/** The border tint [FlatBorderedCard] and [FlatBorderedCardShape] resolve for a given [CardTint]. */
@Composable
fun flatBorderedCardBorderColor(tint: CardTint): Color =
    when (tint) {
        CardTint.Neutral -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        CardTint.Accent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    }

/**
 * The bare `Card` shape/color/border trio behind [FlatBorderedCard], exposed for composables (e.g.
 * [HistoryCard]) whose content already manages its own padding/scroll/gesture handling and shouldn't
 * be wrapped in a second padded `Column`.
 */
@Composable
fun FlatBorderedCardShape(
    modifier: Modifier = Modifier,
    tint: CardTint = CardTint.Neutral,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, flatBorderedCardBorderColor(tint)),
        content = content,
    )
}

/**
 * The flat-bordered `Card` shape shared by most Insights/Retirement/Tax cards, canonizing the
 * convention that already dominated before this was extracted into one composable.
 */
@Composable
fun FlatBorderedCard(
    modifier: Modifier = Modifier,
    tint: CardTint = CardTint.Neutral,
    contentSpacing: Dp = AppDimensions.SpacingMedium,
    content: @Composable ColumnScope.() -> Unit,
) {
    FlatBorderedCardShape(modifier = modifier, tint = tint) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content,
        )
    }
}

/** Short badge text for [InsightSeverity], shown in the [InsightCard] chip. */
fun severityLabel(severity: InsightSeverity): String =
    when (severity) {
        InsightSeverity.INFO -> InsightsStrings.severityLabelInfo
        InsightSeverity.WARNING -> InsightsStrings.severityLabelWarning
        InsightSeverity.IMPORTANT -> InsightsStrings.severityLabelImportant
        InsightSeverity.OPPORTUNITY -> InsightsStrings.severityLabelOpportunity
    }

/**
 * Single Smart Insights card: a severity-colored stripe, a chip + title, explanation, optional amount
 * and action. [hasAccess] gates the action button itself — a card whose [InsightUiModel.gate] is set
 * must never call [onActionClick] for a user who lacks that entitlement (it opens [onUpgradeClick]
 * instead), mirroring the check [RecommendedActions] already does at its own click site.
 */
@Composable
fun InsightCard(
    insight: InsightUiModel,
    hasAccess: (FeatureGate) -> Boolean,
    onActionClick: (Screen) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = severityColor(insight.severity)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, color.copy(alpha = 0.3f)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.fillMaxHeight().width(AppDimensions.SpacingTiny).background(color))
            Column(
                modifier = Modifier.padding(AppDimensions.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            ) {
                InsightCardHeader(insight = insight, color = color)
                Text(
                    text = insight.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                InsightCardFooter(
                    insight = insight,
                    hasAccess = hasAccess,
                    onActionClick = onActionClick,
                    onUpgradeClick = onUpgradeClick,
                )
            }
        }
    }
}

@Composable
private fun InsightCardHeader(
    insight: InsightUiModel,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        SeverityChip(severity = insight.severity, color = color)
        Text(
            text = insight.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SeverityChip(
    severity: InsightSeverity,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = severityLabel(severity),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = AppDimensions.SpacingSmall, vertical = AppDimensions.SpacingTiny),
        )
    }
}

@Composable
private fun InsightCardFooter(
    insight: InsightUiModel,
    hasAccess: (FeatureGate) -> Boolean,
    onActionClick: (Screen) -> Unit,
    onUpgradeClick: () -> Unit,
) {
    if (insight.amountLabel == null && insight.actionLabel == null) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (insight.amountLabel != null) {
            Text(text = insight.amountLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        } else {
            Spacer(modifier = Modifier)
        }
        val target = insight.actionTarget
        val gate = insight.gate
        if (insight.actionLabel != null && target != null) {
            TextButton(onClick = { if (gate == null || hasAccess(gate)) onActionClick(target) else onUpgradeClick() }) {
                Text(text = insight.actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}
