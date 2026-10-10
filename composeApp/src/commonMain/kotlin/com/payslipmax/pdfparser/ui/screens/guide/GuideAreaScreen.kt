package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * An area's case tiles. The on-screen back header is the only way up on iOS, where a tab has no edge-swipe; on
 * Android system back does the same through the Guide's `BackHandler`.
 */
@Composable
internal fun GuideAreaScreen(
    area: GuideAreaContent,
    onBack: () -> Unit,
    onOpenCase: (caseId: String) -> Unit,
    listState: LazyListState = rememberLazyListState(),
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = area.title, subtitle = GuideStrings.topicCount(area.cases.size), onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            items(area.cases, key = { it.id }) { tile -> GuideCaseTileView(tile, onClick = { onOpenCase(tile.id) }) }
        }
    }
}
