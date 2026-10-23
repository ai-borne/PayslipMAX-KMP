package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/**
 * A code page, with the manual's own wording: the code(s), the name, the group and what it means (or that the manual prints none: no
 * group is inferred), the serial, the chapter and the printed page. A code the slip prints differently carries a note saying so.
 */
@Composable
internal fun Part2CodeScreen(
    page: Part2CodeContent,
    onBack: () -> Unit,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = Part2Strings.codesLine(page.codes), onBack = onBack)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = Part2ListPadding,
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
        ) {
            item(key = "name") { Text(page.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            item(key = "group") { Part2GroupBlock(page.group) }
            item(key = "where") { Part2Fact(Part2Strings.manualLabel, Part2Strings.chapterAndPage(page.chapter, page.page)) }
            item(key = "serial") { Part2Fact(Part2Strings.serialLabel, page.serial) }
            page.printedAs.forEach { printed ->
                item(key = "printed-${printed.code}") { Part2Message(Part2Strings.printedAsNote(printed.code, printed.printed)) }
            }
        }
    }
}

@Composable
private fun Part2GroupBlock(group: Part2GroupInfo?) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        GuideLabelPill(group?.let { Part2Strings.groupLabel(it.letter) } ?: Part2Strings.groupNotPrinted)
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
