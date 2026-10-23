package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.part2.domain.Part2CodeRow
import com.payslipmax.pdfparser.ui.components.AccentStripeCard
import com.payslipmax.pdfparser.ui.components.AccentStripeCardShape
import com.payslipmax.pdfparser.ui.screens.guide.Decoration
import com.payslipmax.pdfparser.ui.screens.guide.GuideLabelPill
import com.payslipmax.pdfparser.ui.screens.guide.GuideTileFrame
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/** A ribbon or a case on a Part II screen, drawn like an area row on Guide Home: a title, one line beneath, an arrow. Titles wrap. */
@Composable
internal fun Part2TileView(
    title: String,
    line: String,
    onClick: () -> Unit,
) {
    AccentStripeCard(
        accent = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clip(AccentStripeCardShape).clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Decoration(AppStrings.rowArrow, AppDimensions.TextSizeLarge, MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One code in a list: its HRMS code(s), the manual's name for it, and its group (or that the manual prints none). */
@Composable
internal fun Part2CodeRowView(
    row: Part2CodeRow,
    onClick: () -> Unit,
) {
    GuideTileFrame(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(Part2Strings.codesLine(row.codes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(row.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
                GuideLabelPill(row.group?.let(Part2Strings::groupLabel) ?: Part2Strings.groupNotPrinted)
                if (row.lookupOnly) GuideLabelPill(Part2Strings.lookupOnly)
            }
        }
    }
}
