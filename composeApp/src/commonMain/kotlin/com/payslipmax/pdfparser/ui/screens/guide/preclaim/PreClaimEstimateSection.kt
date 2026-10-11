package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings

/**
 * Leave travel has no estimates, so it gets no section. For the other moves each element the calculators answered is one row, in the
 * calculators' order, and the rows are never added together: the portal sets the advance (plan 21, F1).
 */
internal fun LazyListScope.preClaimEstimateItems(ws: PreClaimWorksheet) {
    if (ws.form.move == PreClaimMoveType.LEAVE_TRAVEL) return
    item(key = "estimates") {
        PreClaimSectionFrame(PreClaimStrings.estimatesTitle, PreClaimStrings.estimatesNote) {
            if (ws.estimates.isEmpty()) PreClaimQuietText(PreClaimStrings.estimatesNone)
            ws.estimates.forEach { PreClaimEstimateRow(PreClaimWording.line(it)) }
        }
    }
}

/** The name and amount on one line (the amount is bold), then the working, then the "Estimate. Rule: ... As of ..." line. */
@Composable
private fun PreClaimEstimateRow(line: PreClaimEstimateLine) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            Text(line.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            line.amount?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold) }
        }
        Text(line.detail, style = MaterialTheme.typography.bodyMedium)
        line.source?.let { PreClaimQuietText(it) }
    }
}
