package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimQuietText
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/** When the slip was printed and, when the paper in the clerk's hand no longer matches the draft, why. */
@Composable
internal fun PrintedView(printed: PrintedUi) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Text(printed.line, style = MaterialTheme.typography.titleSmall)
        printed.note?.let { PreClaimQuietText(it) }
    }
}

/**
 * Print (for an entitled officer only) and the optional HRMS check (a read, so it stays for everyone who can see the slip). Print is off
 * while an `ack` check is unticked, and says why; while the system dialog is open it is off and says so.
 */
@Composable
internal fun SlipPrintSection(
    slip: SlipUi,
    printing: Boolean,
    canPrint: Boolean,
    onPrint: () -> Unit,
    onHrms: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (canPrint) {
            Button(onClick = onPrint, enabled = slip.readyToPrint && !printing) { Text(if (printing) Part2Strings.printingNow else Part2Strings.printButton) }
            if (!slip.readyToPrint) PreClaimQuietText(Part2Strings.printBlocked)
        }
        OutlinedButton(onClick = onHrms) { Text(Part2Strings.hrmsCheckButton) }
    }
}
