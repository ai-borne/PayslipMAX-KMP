package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.guide.summary.TdLevelGroup
import com.payslipmax.pdfparser.guide.summary.TdSummary
import com.payslipmax.pdfparser.ui.screens.CardTint
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.TdSummaryStrings as S

/**
 * The TD entitlements table (plan 24): one block per pay-level group with its hotel, taxi and food amounts and the food by time away.
 * Every amount is already worked out in the [TdSummary]; this only lays it out. Free for every user (owner
 * decision D3), so it is drawn outside the paid half. Nothing is drawn for a card with no table.
 */
internal fun LazyListScope.guideTdSummary(card: GuideCardContent) {
    val summary = card.summary ?: return
    item(key = "td-summary") { TdSummaryBlock(summary, card.ownLevel?.let(summary::groupFor)) }
}

@Composable
private fun TdSummaryBlock(
    summary: TdSummary,
    own: TdLevelGroup?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
        Text(
            TdSummaryWording.band(summary.daBandFrom, summary.daBandTo),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() },
        )
        summary.levelGroups.forEach { TdLevelGroupCard(it, summary, isOwn = it == own) }
    }
}

@Composable
private fun TdLevelGroupCard(
    group: TdLevelGroup,
    summary: TdSummary,
    isOwn: Boolean,
) {
    val description = TdSummaryWording.description(group, summary.foodTimings, isOwn)
    FlatBorderedCardShape(tint = if (isOwn) CardTint.Accent else CardTint.Neutral) {
        Column(
            modifier =
                Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium).semantics(mergeDescendants = true) {
                    contentDescription = description
                    if (isOwn) stateDescription = S.yourLevel
                },
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall), verticalAlignment = Alignment.CenterVertically) {
                Text(TdSummaryWording.levels(group.levels), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (isOwn) Text(S.yourLevel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            group.hotelPaise?.let { TdLine(S.hotelLabel, TdSummaryWording.hotel(it)) }
            group.taxi?.let { TdLine(S.taxiLabel, TdSummaryWording.taxi(it)) }
            group.foodPaise?.let { TdLine(S.foodLabel, TdSummaryWording.food(it)) }
            TdSummaryWording.timingLines(summary.foodTimings, group.foodByTimingPaise).forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TdLine(
    label: String,
    value: String,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
