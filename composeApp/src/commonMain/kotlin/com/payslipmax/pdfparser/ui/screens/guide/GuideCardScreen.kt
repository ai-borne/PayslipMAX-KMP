package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * One rule card: breadcrumb and title, the answer with its trust chips, then the body sections (or the unlock panel in
 * the free preview), and a closing note that the card is guidance, not a sanction. [actions] (pin, share, copy cite) is null for a locked card.
 */
@Composable
internal fun GuideCardScreen(
    card: GuideCardContent,
    crumbs: List<GuideCrumb>,
    onCrumb: (path: List<GuideDestination>) -> Unit,
    onBack: () -> Unit,
    onUnlock: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
    actions: GuideCardActions? = null,
    onOpenCard: (cardId: String) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GuideLevelHeader(crumbs, onCrumb, title = card.title, subtitle = null, onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            item(key = "header") { GuideCardHeader(card) }
            if (card.history != GuideCardHistory.None || card.trust.replacedUntil != null) {
                item(key = "history") { GuideCardHistoryLinks(card, onOpenCard) }
            }
            actions?.let { item(key = "actions") { GuideCardActionRow(it) } }
            guideCardSections(card, onUnlock, actions?.onCopyCite)
            item(key = "disclaimer") {
                Text(
                    GuideStrings.cardDisclaimer,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
