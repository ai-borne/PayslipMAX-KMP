package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions

/** One tile of the Guide Home grid: an emoji, a title and one line under it. [badge] is the small "Premium" mark, null when there is none. */
internal class GuideGridItem(
    val key: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
    val badge: String? = null,
)

/**
 * A group's label on Guide Home: its name as a heading, and an optional [note] beside it (the user's own group says it is private).
 * No divider: the label alone separates the groups, which keeps Home within one phone screen.
 */
internal fun LazyListScope.guideSectionLabel(
    key: String,
    label: String,
    note: String? = null,
) {
    item(key = "$key-label") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingSmall),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false).semantics { heading() },
            )
            note?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

/** [items] as rows of two equal-height tiles (an odd last row leaves its right half empty), or one full-width tile per row when [fullRow]. */
internal fun LazyListScope.guideTileGrid(
    items: List<GuideGridItem>,
    fullRow: Boolean = false,
) {
    items.chunked(if (fullRow) 1 else GRID_COLUMNS).forEach { pair ->
        item(key = "grid-${pair.first().key}") {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
            ) {
                pair.forEach { GuideGridTileView(it, Modifier.weight(1f).fillMaxHeight()) }
                if (!fullRow) repeat(GRID_COLUMNS - pair.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private const val GRID_COLUMNS = 2

/** A compact grid tile read by screen readers as one button: the emoji (decoration) beside the title, the one line under the title. Titles and lines wrap, never truncate. */
@Composable
private fun GuideGridTileView(
    item: GuideGridItem,
    modifier: Modifier = Modifier,
) {
    GuideTileFrame(onClick = item.onClick, modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = AppDimensions.PaddingMedium, vertical = AppDimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Decoration(item.emoji, AppDimensions.TextSizeHuge)
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                item.badge?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary) }
                Text(item.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
