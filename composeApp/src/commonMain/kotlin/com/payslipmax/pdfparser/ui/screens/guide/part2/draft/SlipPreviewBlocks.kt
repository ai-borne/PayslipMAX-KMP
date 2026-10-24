package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.guide.GuideLabelPill
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimQuietText
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimSectionFrame
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimTickRow
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/** One order of the slip: its title (with the group letters) and its rows, in date order as the engine arranged them. */
@Composable
internal fun SlipOrderView(order: SlipOrderUi) {
    PreClaimSectionFrame(title = order.title) { order.rows.forEach { SlipRowView(it) } }
}

/** One row as the clerk types it: the code, where it comes from when the officer did not ask for it, the narrative and every column that holds something. */
@Composable
private fun SlipRowView(row: SlipRowUi) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            Text(row.code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            if (row.source.isNotEmpty()) GuideLabelPill(row.source)
        }
        Text(row.narrative, style = MaterialTheme.typography.bodyMedium)
        row.detailLines().forEach { PreClaimQuietText(it) }
    }
}

/** The checks against the manual: an `ack` finding is a prominent card with a tick the officer must set; a `warn` is a plain note. Each cites its source. */
@Composable
internal fun FindingsView(
    slip: SlipUi,
    onTick: (String, Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(Part2Strings.checksHeading, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        if (slip.findings.any { it.ack }) Text(Part2Strings.ackHeading, style = MaterialTheme.typography.bodyMedium)
        slip.findings.forEach { FindingView(it, onTick) }
        if (slip.findings.any { it.ack } && slip.readyToPrint) Text(Part2Strings.readyToPrint, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FindingView(
    finding: FindingUi,
    onTick: (String, Boolean) -> Unit,
) {
    val colors = if (finding.ack) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer) else CardDefaults.cardColors()
    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppDimensions.SpacingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
            Text(finding.text, style = MaterialTheme.typography.bodyMedium)
            PreClaimQuietText(finding.citation)
            if (finding.ack) PreClaimTickRow(Part2Strings.ackTick, finding.ticked) { onTick(finding.key, it) }
        }
    }
}

/** Orders that go with this one and have no template yet: listed so none is silently dropped. */
@Composable
internal fun UndraftedView(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Text(Part2Strings.undraftedHeading, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        lines.forEach { PreClaimQuietText(it) }
    }
}
