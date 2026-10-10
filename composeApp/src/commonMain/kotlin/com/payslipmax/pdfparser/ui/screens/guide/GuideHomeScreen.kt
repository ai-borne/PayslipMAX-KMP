package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

private const val AREA_COLUMNS = 2

/**
 * Guide Home: a search icon, then the "What's new" row when [whatsNewDate] is set, then the user's pinned cards when there are any (newest first; no section at all when
 * [pinned] is empty or the user is locked), then one tile per area, in bundle order, two to a row (the approved preview). Last come the
 * user's notes lines ([notes]): the removed-cards row and the quiet "could not be read" line, each only when it applies.
 */
@Composable
internal fun GuideHomeScreen(
    areas: List<GuideAreaTile>,
    onOpenArea: (areaId: String) -> Unit,
    onOpenSearch: () -> Unit,
    pinned: List<GuideFeedRow> = emptyList(),
    onOpenPinned: (cardId: String) -> Unit = {},
    whatsNewDate: String? = null,
    onOpenWhatsNew: () -> Unit = {},
    notes: GuideHomeNotes = GuideHomeNotes.None,
    notedCards: Set<String> = emptySet(),
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(AREA_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppDimensions.PaddingMedium),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTen),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTen),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { GuideHomeHeader(onOpenSearch) }
        // Only a bundle that carries a change entry has this row; today's real bundle has none, so Home is unchanged.
        whatsNewDate?.let { date ->
            item(key = "whats-new", span = { GridItemSpan(maxLineSpan) }) { GuideWhatsNewRow(date, onOpenWhatsNew) }
        }
        if (pinned.isNotEmpty()) {
            item(key = "pinned-heading", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    GuideStrings.pinnedSection,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            items(pinned, key = { "pinned-${it.cardId}" }, span = { GridItemSpan(maxLineSpan) }) { row ->
                GuideCardRowView(row, onClick = { onOpenPinned(row.cardId) }, noteMark = if (row.cardId in notedCards) GuideNoteMark.HAS_NOTE else GuideNoteMark.NONE)
            }
        }
        items(areas, key = { it.id }) { tile -> GuideAreaTileView(tile, onClick = { onOpenArea(tile.id) }) }
        if (notes.removedCount > 0) {
            item(key = "removed-notes", span = { GridItemSpan(maxLineSpan) }) { GuideRemovedNotesRow(notes.removedCount, notes.onOpenRemoved) }
        }
        if (notes.unreadable > 0) {
            item(key = "unreadable-notes", span = { GridItemSpan(maxLineSpan) }) { GuideUnreadableNotesLine(notes.unreadable) }
        }
    }
}

@Composable
private fun GuideHomeHeader(onOpenSearch: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            Text(
                GuideStrings.homeTitle,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                GuideStrings.homeSection,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, contentDescription = GuideStrings.searchOpen) }
    }
}
