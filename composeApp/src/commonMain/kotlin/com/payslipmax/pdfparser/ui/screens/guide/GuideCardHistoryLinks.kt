package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideColors
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings

/**
 * Under a card's answer: on a replaced rule, a notice and "See current rule"; on a rule that replaced an older one, "Earlier
 * rule (before <date>)". Ids and dates only, so a locked card shows them too; the card they open decides what is locked.
 */
@Composable
internal fun GuideCardHistoryLinks(
    card: GuideCardContent,
    onOpenCard: (cardId: String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (card.trust.replacedUntil != null) {
            Text(GuideMaintenanceStrings.replacedNotice, style = MaterialTheme.typography.bodySmall, color = GuideColors.watchOut())
        }
        card.history.currentRuleId?.let { id ->
            OutlinedButton(
                onClick = { onOpenCard(id) },
                modifier = Modifier.semantics { contentDescription = GuideMaintenanceStrings.seeCurrentRuleDescription },
            ) { Text(GuideMaintenanceStrings.seeCurrentRule) }
        }
        card.history.earlierRuleId?.let { id ->
            OutlinedButton(onClick = { onOpenCard(id) }) { Text(GuideMaintenanceStrings.earlierRule(card.history.earlierBefore)) }
        }
    }
}
