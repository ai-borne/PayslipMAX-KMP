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
import com.payslipmax.pdfparser.part2.domain.Part2CaseContent
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/** A case's codes, then (when the layout lists any) the codes homed elsewhere that belong with them, under their own heading. */
@Composable
internal fun Part2CaseScreen(
    case: Part2CaseContent,
    onBack: () -> Unit,
    onOpenCode: (String) -> Unit,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = case.title, subtitle = Part2Strings.tileLine(case.span, case.rows.size), onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = Part2ListPadding,
            verticalArrangement = Part2ListSpacing,
        ) {
            items(case.rows, key = { it.serial }) { row -> Part2CodeRowView(row) { onOpenCode(row.openCode) } }
            if (case.also.isNotEmpty()) {
                item(key = "also") {
                    Text(Part2Strings.alsoSeeHeading, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                }
                items(case.also, key = { "also-${it.serial}" }) { row -> Part2CodeRowView(row) { onOpenCode(row.openCode) } }
            }
        }
    }
}
