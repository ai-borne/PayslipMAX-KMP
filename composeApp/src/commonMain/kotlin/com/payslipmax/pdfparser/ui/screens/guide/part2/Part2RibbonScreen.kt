package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.payslipmax.pdfparser.part2.domain.Part2RibbonContent
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.theme.LearnStrings

/** A ribbon as one flat list: each case's name is a heading over the casualties it homes, and a casualty opens its page. */
@Composable
internal fun Part2RibbonScreen(
    ribbon: Part2RibbonContent,
    onBack: () -> Unit,
    onOpenCode: (String) -> Unit,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = ribbon.title, subtitle = LearnStrings.casualtyCount(ribbon.sections.sumOf { it.rows.size }), onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = Part2ListPadding,
            verticalArrangement = Part2ListSpacing,
        ) {
            ribbon.sections.forEachIndexed { index, section ->
                item(key = "case-$index") { Text(section.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }) }
                items(section.rows, key = { it.serial }) { row -> Part2CodeRowView(row) { onOpenCode(row.openCode) } }
            }
        }
    }
}
