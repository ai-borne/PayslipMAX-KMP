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
 * One rule card: breadcrumb and title, the answer, then the body sections, and a closing note that the card is
 * guidance, not a sanction. Copy cite, share and pin arrive in E8.
 */
@Composable
internal fun GuideCardScreen(
    card: GuideCardContent,
    crumbs: List<GuideCrumb>,
    onCrumb: (path: List<GuideDestination>) -> Unit,
    onBack: () -> Unit,
    listState: LazyListState = rememberLazyListState(),
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
            guideCardSections(card)
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
