package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

private val TileShape = RoundedCornerShape(AppDimensions.CornerRadius)
private val PillShape = RoundedCornerShape(percent = 50)

/** A tappable bordered tile in the app's flat card style; read by screen readers as one button. */
@Composable
private fun GuideTileFrame(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlatBorderedCardShape(modifier = modifier.clip(TileShape).clickable(role = Role.Button, onClick = onClick)) {
        content()
    }
}

/** An area on Guide Home: its title from the bundle and how many topics it holds. Titles wrap, never truncate. */
@Composable
internal fun GuideAreaTileView(
    tile: GuideAreaTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GuideTileFrame(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.heightIn(min = AppDimensions.GuideAreaTileMinHeight).padding(AppDimensions.SpacingMedium),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(tile.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                GuideStrings.topicCount(tile.caseCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A case in an area: title, the rule-number subtitle when the bundle has one, and a card-count pill. */
@Composable
internal fun GuideCaseTileView(
    tile: GuideCaseTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GuideTileFrame(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(AppDimensions.SpacingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tile.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (tile.subtitle.isNotBlank()) {
                    Text(
                        tile.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            CountPill(tile.cardCount)
        }
    }
}

@Composable
private fun CountPill(count: Int) {
    val description = GuideStrings.cardCount(count)
    Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.primary,
        modifier =
            Modifier
                .clearAndSetSemantics { contentDescription = description }
                .background(MaterialTheme.colorScheme.primaryContainer, PillShape)
                .padding(horizontal = AppDimensions.SpacingSmall, vertical = AppDimensions.SpacingTwo),
    )
}
