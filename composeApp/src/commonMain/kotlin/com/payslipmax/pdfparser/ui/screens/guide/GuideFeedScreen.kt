package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

private const val FACETS_ITEM_KEY = "facets"

/**
 * A case's cards to scroll, in the authored order: facet chips on top when the feed is long enough, otherwise its
 * card count. [listState] is restored from the Guide stack, so the place survives a card, a tab switch and restore.
 */
@Composable
internal fun GuideFeedScreen(
    feed: GuideFeedContent,
    crumbs: List<GuideCrumb>,
    onCrumb: (path: List<GuideDestination>) -> Unit,
    onBack: () -> Unit,
    onSelectFacet: (facet: String?) -> Unit,
    onOpenCard: (cardId: String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(modifier = Modifier.fillMaxSize()) {
        GuideLevelHeader(crumbs, onCrumb, title = feed.title, subtitle = feed.subtitle, onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            item(key = FACETS_ITEM_KEY) {
                if (feed.facets.isEmpty()) {
                    Text(
                        GuideStrings.cardCount(feed.totalCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    GuideFacetChips(feed, onSelectFacet)
                }
            }
            items(feed.rows, key = { it.cardId }) { row -> GuideCardRowView(row, onClick = { onOpenCard(row.cardId) }) }
        }
    }
}
