package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimQuietText
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2LeaveBankStrings

/**
 * The Leave Bank fill of one section (plan, phase 8b): a button, what the last press did, and the notes on the figures the box then holds.
 * A first press fills the empty boxes; once any figure is there the button reads "Refill" and asks before it replaces them, so a figure
 * the officer typed is never lost to a stray tap.
 */
@Composable
internal fun LeaveBankFillView(
    index: Int,
    fill: FillUi,
    actions: LeaveDraftActions,
) {
    var asking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        OutlinedButton(onClick = { if (fill.refill) asking = true else actions.onFill(index, false) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (fill.refill) Part2LeaveBankStrings.refillButton else Part2LeaveBankStrings.fillButton)
        }
        fill.message?.let { PreClaimQuietText(it) }
        fill.notes.forEach { PreClaimQuietText(it) }
    }
    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(Part2LeaveBankStrings.refillTitle) },
            text = { Text(Part2LeaveBankStrings.refillBody) },
            confirmButton = {
                TextButton(onClick = {
                    asking = false
                    actions.onFill(index, true)
                }) { Text(Part2LeaveBankStrings.refillConfirm) }
            },
            dismissButton = { TextButton(onClick = { asking = false }) { Text(Part2LeaveBankStrings.refillKeep) } },
        )
    }
}

/** The check of the drafted total against the Leave Bank's statement, under the slip's other checks; it informs and changes nothing. */
@Composable
internal fun CrossChecksView(lines: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Text(Part2LeaveBankStrings.crossCheckHeading, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        lines.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}
