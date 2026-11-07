package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.part2.domain.Part2AboutContent
import com.payslipmax.pdfparser.part2.domain.Part2CodeContent
import com.payslipmax.pdfparser.part2.domain.Part2ColumnLine
import com.payslipmax.pdfparser.part2.domain.Part2Need
import com.payslipmax.pdfparser.part2.domain.Part2RelatedLine
import com.payslipmax.pdfparser.ui.screens.guide.GuideLabelPill
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.LearnStrings

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
}

/** What the order needs in each column it uses, in the manual's words; a required column the manual gives no caption for points to its page. */
internal fun LazyListScope.part2ColumnItems(page: Part2CodeContent) {
    item(key = "columns-heading") { SectionHeading(LearnStrings.columnsHeading) }
    if (page.columns.isEmpty()) item(key = "columns-none") { Part2Message(LearnStrings.columnsNone) }
    page.columns.forEach { line -> item(key = "column-${line.position}") { Part2ColumnLineView(line, page.page) } }
}

@Composable
private fun Part2ColumnLineView(
    line: Part2ColumnLine,
    manualPage: Int,
) {
    val required = line.need == Part2Need.REQUIRED
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
            Text(LearnStrings.columnLabel(line.position), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            GuideLabelPill(if (required) LearnStrings.needRequired else LearnStrings.needOptional)
        }
        val caption = line.caption.ifEmpty { if (required) LearnStrings.requiredNoCaption(manualPage) else "" }
        if (caption.isNotEmpty()) Text(caption, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** The plain-English page: four short parts, each under its own small label. Absent where none was written. */
internal fun LazyListScope.part2AboutItems(about: Part2AboutContent?) {
    if (about == null) return
    item(key = "about-heading") { SectionHeading(LearnStrings.aboutHeading) }
    val parts = listOf(LearnStrings.aboutWhat to about.what, LearnStrings.aboutWhen to about.whenSeen, LearnStrings.aboutReady to about.ready, LearnStrings.aboutWatch to about.watch)
    parts.forEach { (label, text) ->
        item(key = "about-$label") {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** "Usually published with": each companion by name, with whether the manual (and its page) or practice says so. Absent where there is no data. */
internal fun LazyListScope.part2RelatedItems(
    related: List<Part2RelatedLine>,
    onOpenCode: (String) -> Unit,
) {
    if (related.isEmpty()) return
    item(key = "related-heading") { SectionHeading(LearnStrings.relatedHeading) }
    related.forEach { line ->
        item(key = "related-${line.code}") { Part2TileView(line.name, "${line.code} · ${LearnStrings.relatedBasis(line.page)}") { onOpenCode(line.code) } }
    }
}
