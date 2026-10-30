package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.part2.domain.Part2CodeContent
import com.payslipmax.pdfparser.part2.domain.Part2GroupInfo
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.GuideLabelPill
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.LearnStrings

/**
 * A casualty page, with the manual's own wording: the code(s), the name, the group and what it means (or that the manual prints none:
 * no group is inferred), the serial, the chapter and the printed page. Then what the order needs in each column, the plain-English page
 * where one was written, and what is usually published with it. Each of the last two has no heading at all where there is no data.
 */
@Composable
internal fun Part2CodeScreen(
    page: Part2CodeContent,
    onBack: () -> Unit,
    onOpenCode: (String) -> Unit,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = LearnStrings.codesLine(page.codes), onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = Part2ListPadding,
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
        ) {
            item(key = "name") { Text(page.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item(key = "group") { Part2GroupBlock(page.group) }
            item(key = "where") { Part2Fact(LearnStrings.manualLabel, LearnStrings.chapterAndPage(page.chapter, page.page)) }
            item(key = "serial") { Part2Fact(LearnStrings.serialLabel, page.serial) }
            part2ColumnItems(page)
            part2AboutItems(page.about)
            part2RelatedItems(page.related, onOpenCode)
            // Off until the Orders Bank exists (Phase 3): shown so the officer sees where the page leads, never a dead tap.
            item(key = "add") { Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text(LearnStrings.addToBank) } }
        }
    }
}

@Composable
private fun Part2GroupBlock(group: Part2GroupInfo?) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        GuideLabelPill(group?.let { LearnStrings.groupLabel(it.letter) } ?: LearnStrings.groupNotPrinted)
        group?.let { Text(it.meaning, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun Part2Fact(
    label: String,
    value: String,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
