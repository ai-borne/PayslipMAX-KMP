package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.part2.domain.Part2RibbonContent
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/** A ribbon's case tiles, each with the chapters and printed pages its codes cover and how many codes it holds. */
@Composable
internal fun Part2RibbonScreen(
    ribbon: Part2RibbonContent,
    onBack: () -> Unit,
    onOpenCase: (String) -> Unit,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = ribbon.title, subtitle = GuideStrings.topicCount(ribbon.cases.size), onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = Part2ListPadding,
            verticalArrangement = Part2ListSpacing,
        ) {
            items(ribbon.cases, key = { it.id }) { case ->
                Part2TileView(case.title, Part2Strings.tileLine(case.span, case.codeCount)) { onOpenCase(case.id) }
            }
        }
    }
}
