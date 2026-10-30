package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.part2.domain.Part2RibbonTile
import com.payslipmax.pdfparser.part2.domain.Part2SearchResult
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.theme.LearnStrings

/**
 * Learn's home: the search box, then (with nothing typed) one row per ribbon, or (with a query) the matching casualties. Search reaches
 * all 303. The on-screen back header is the only way up on iOS, where a tab has no edge-swipe; on Android system back does the same
 * through the Guide's `BackHandler`.
 */
@Composable
internal fun Part2HomeScreen(
    ribbons: List<Part2RibbonTile>,
    query: String,
    result: Part2SearchResult,
    onQueryChange: (String) -> Unit,
    onOpenRibbon: (String) -> Unit,
    onOpenCode: (String) -> Unit,
    onBack: () -> Unit,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = LearnStrings.entryTitle, subtitle = LearnStrings.homeSubtitle, onBack = onBack)
        Part2SearchField(query, onQueryChange)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = Part2ListPadding,
            verticalArrangement = Part2ListSpacing,
        ) {
            if (result == Part2SearchResult.Idle) {
                item(key = "hint") { Part2Message(LearnStrings.searchHint) }
                items(ribbons, key = { it.id }) { Part2TileView(it.title, LearnStrings.casualtyCount(it.serialCount)) { onOpenRibbon(it.id) } }
            } else {
                part2SearchItems(result, onOpenCode)
            }
        }
    }
}
