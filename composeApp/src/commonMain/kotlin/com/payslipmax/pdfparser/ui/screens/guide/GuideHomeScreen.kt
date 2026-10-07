package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

private const val AREA_COLUMNS = 2

/** Guide Home: one tile per area, in bundle order, two to a row (the approved preview). */
@Composable
internal fun GuideHomeScreen(
    areas: List<GuideAreaTile>,
    onOpenArea: (areaId: String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(AREA_COLUMNS),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppDimensions.PaddingMedium),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTen),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTen),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) { GuideHomeHeader() }
        items(areas, key = { it.id }) { tile -> GuideAreaTileView(tile, onClick = { onOpenArea(tile.id) }) }
    }
}

@Composable
private fun GuideHomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
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
}
