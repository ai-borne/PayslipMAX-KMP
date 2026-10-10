package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

/**
 * Guide Home: a search icon, then the "What's new" row when [whatsNewDate] is set, then the user's pinned cards when there are any (newest first; no section at all when
 * [pinned] is empty or the user is locked), then one accent-striped row per area, in bundle order (owner decision 2026-10-10: one column, like Smart Insights). Last come the
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        item { GuideHomeHeader(onOpenSearch) }
        // Only a bundle that carries a change entry has this row; today's real bundle has none, so Home is unchanged.
        whatsNewDate?.let { date ->
            item(key = "whats-new") { GuideWhatsNewRow(date, onOpenWhatsNew) }
        }
        if (pinned.isNotEmpty()) {
            item(key = "pinned-heading") {
                Text(
                    GuideStrings.pinnedSection,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() },
                )
            }
            items(pinned, key = { "pinned-${it.cardId}" }) { row ->
                GuideCardRowView(row, onClick = { onOpenPinned(row.cardId) }, noteMark = if (row.cardId in notedCards) GuideNoteMark.HAS_NOTE else GuideNoteMark.NONE)
            }
        }
        items(areas, key = { it.id }) { tile -> GuideAreaTileView(tile, onClick = { onOpenArea(tile.id) }) }
        if (notes.removedCount > 0) {
            item(key = "removed-notes") { GuideRemovedNotesRow(notes.removedCount, notes.onOpenRemoved) }
        }
        if (notes.unreadable > 0) {
            item(key = "unreadable-notes") { GuideUnreadableNotesLine(notes.unreadable) }
        }
    }
}

@Composable
private fun GuideHomeHeader(onOpenSearch: () -> Unit) {
    // The Settings / History / Insights header: a bold title with its action beside it, then a body-size subtitle.
    Column(modifier = Modifier.padding(bottom = AppDimensions.SpacingSmall)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                GuideStrings.homeTitle,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            IconButton(onClick = onOpenSearch) { Icon(Icons.Default.Search, contentDescription = GuideStrings.searchOpen) }
        }
        Text(
            GuideStrings.homeSection,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
