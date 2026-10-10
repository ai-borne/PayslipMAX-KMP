package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.payslipmax.pdfparser.ui.components.AccentStripeCard
import com.payslipmax.pdfparser.ui.components.AccentStripeCardShape
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings

private val TileShape = RoundedCornerShape(AppDimensions.CornerRadius)
private val PillShape = RoundedCornerShape(percent = 50)

/** A tappable bordered tile in the app's flat card style; read by screen readers as one button. */
@Composable
internal fun GuideTileFrame(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlatBorderedCardShape(modifier = modifier.clip(TileShape).clickable(role = Role.Button, onClick = onClick)) {
        content()
    }
}

/**
 * An area on Guide Home: the app's accent-stripe card with the area's emoji, its title from the bundle, its topic
 * count and an arrow, laid out like a Settings row. Titles wrap, never truncate; the emoji and arrow are not read aloud.
 */
@Composable
internal fun GuideAreaTileView(
    tile: GuideAreaTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccentStripeCard(
        accent = MaterialTheme.colorScheme.primary,
        modifier = modifier.clip(AccentStripeCardShape).clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Decoration(GuideStrings.areaEmoji(tile.id), AppDimensions.TextSizeHuge)
            Column(modifier = Modifier.weight(1f)) {
                Text(tile.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    GuideStrings.topicCount(tile.caseCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Decoration(AppStrings.rowArrow, AppDimensions.TextSizeLarge, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** An emoji or arrow beside a row's text: seen, never read aloud. */
@Composable
internal fun Decoration(
    text: String,
    fontSize: TextUnit,
    color: Color = Color.Unspecified,
) {
    Text(text, fontSize = fontSize, color = color, modifier = Modifier.clearAndSetSemantics {})
}

/**
 * A case in an area, drawn like an area row on Guide Home: its title, then one line with the rule subtitle (when the
 * bundle has one) and the card count, so every row is the same height.
 */
@Composable
internal fun GuideCaseTileView(
    tile: GuideCaseTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AccentStripeCard(
        accent = MaterialTheme.colorScheme.primary,
        modifier = modifier.clip(AccentStripeCardShape).clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tile.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    GuideStrings.caseLine(tile.subtitle, tile.cardCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Decoration(AppStrings.rowArrow, AppDimensions.TextSizeLarge, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A card in a feed: title, one-line answer and facet; an "also relevant here" card also names its main case. [noteMark] is
 * the user's own "Note" marker, passed in only for an unlocked user.
 */
@Composable
internal fun GuideCardRowView(
    row: GuideFeedRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    noteMark: GuideNoteMark = GuideNoteMark.NONE,
) {
    GuideTileFrame(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        // Smart Insights card type and padding: a bold title, then a body-size answer.
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(row.answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GuideLabelPill(row.facetLabel)
            GuideTrustChips(row.trust, noteMark = noteMark)
            row.alsoHomeTitle?.let { home ->
                Text(
                    GuideStrings.alsoRelevant(home),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** A small neutral pill, such as a card's facet ("How much"); its text is bundle content. */
@Composable
internal fun GuideLabelPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, PillShape)
                .padding(horizontal = AppDimensions.SpacingSmall, vertical = AppDimensions.SpacingTwo),
    )
}
