package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.payslipmax.pdfparser.part2.domain.HubStatus
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2Entry
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimEntry
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

/**
 * Guide Home: a search icon, then the "What's new" row when [whatsNewDate] is set, then the user's pinned cards when there are any (newest first; no section at all when
 * [pinned] is empty or the user is locked), then a two-column grid of area tiles in bundle order, with the TA/DA worksheet tile after them (owner decision 2026-11-02, Phase 8: one overview instead of a long list of rows). Last come the
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
    preClaim: PreClaimEntry = PreClaimEntry.HIDDEN,
    onOpenPreClaim: () -> Unit = {},
    part2: Part2Entry = Part2Entry.HIDDEN,
    part2Status: HubStatus = HubStatus.Unknown,
    onOpenPart2Hub: () -> Unit = {},
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
        guideSectionLabel("guide", GuideStrings.homeOverview)
        guideTileGrid(guideAreaItems(areas, onOpenArea, preClaim, onOpenPreClaim))
        guidePart2Section(part2, part2Status, onOpenPart2Hub)
        if (notes.removedCount > 0) {
            item(key = "removed-notes") { GuideRemovedNotesRow(notes.removedCount, notes.onOpenRemoved) }
        }
        if (notes.unreadable > 0) {
            item(key = "unreadable-notes") { GuideUnreadableNotesLine(notes.unreadable) }
        }
    }
}

/** The handbook areas as tiles, then the TA/DA worksheet tile (absent while its flag is off, Premium-marked for a free user). */
private fun guideAreaItems(
    areas: List<GuideAreaTile>,
    onOpenArea: (String) -> Unit,
    preClaim: PreClaimEntry,
    onOpenPreClaim: () -> Unit,
): List<GuideGridItem> {
    val tiles = areas.map { GuideGridItem("area-${it.id}", GuideStrings.areaEmoji(it.id), GuideStrings.areaTileLabel(it.id) ?: it.title, GuideStrings.topicCount(it.caseCount), { onOpenArea(it.id) }) }
    if (preClaim == PreClaimEntry.HIDDEN) return tiles
    val worksheet =
        GuideGridItem(
            "preclaim",
            PreClaimStrings.entryEmoji,
            PreClaimStrings.tileTitle,
            PreClaimStrings.entrySubtitle,
            onOpenPreClaim,
            badge = PreClaimStrings.entryPremium.takeIf { preClaim == PreClaimEntry.LOCKED },
        )
    return tiles + worksheet
}

/**
 * The Part II group below the handbook areas, labelled as the user's own private records (it is not part of the PCDA handbook): one
 * full-width Part II Orders tile that opens the hub with Learn and the Orders Bank. Its line and stripe follow [status] (the plain line and the
 * brand stripe until the file is known to hold casualties). Absent while its flag is off.
 */
private fun LazyListScope.guidePart2Section(
    part2: Part2Entry,
    status: HubStatus,
    onOpenPart2Hub: () -> Unit,
) {
    if (part2 == Part2Entry.HIDDEN) return
    guideSectionLabel("part2", LearnStrings.sectionHeading, note = LearnStrings.sectionSubtext)
    item(key = "part2") {
        Part2HubRow(LearnStrings.hubEmoji, LearnStrings.hubTitle, OrdersBankStrings.homeTileSubtitle(status), onOpenPart2Hub, accent = part2StatusAccent(status))
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
