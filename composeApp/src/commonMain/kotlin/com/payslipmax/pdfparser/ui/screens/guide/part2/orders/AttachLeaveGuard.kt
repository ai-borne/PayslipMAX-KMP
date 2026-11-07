package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.payslipmax.pdfparser.ui.theme.OrderStrings

/**
 * Asks before a way out of the attach form throws a held scan away: the Back arrow, the Orders Bank shortcut and the system back all
 * delete the scan, and a scan is real work. [content] gets `leave`, which runs a way out at once when nothing is [holdsScan], and
 * otherwise only after the officer confirms. System back is [onSystemBack], asked about the same way.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AttachLeaveGuard(
    holdsScan: Boolean,
    onSystemBack: () -> Unit,
    content: @Composable (leave: (() -> Unit) -> Unit) -> Unit,
) {
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    BackHandler(enabled = holdsScan) { pending = onSystemBack }
    content { wayOut -> if (holdsScan) pending = wayOut else wayOut() }
    pending?.let { wayOut ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(OrderStrings.leaveScanTitle) },
            text = { Text(OrderStrings.leaveScanBody) },
            confirmButton = {
                TextButton(onClick = {
                    pending = null
                    wayOut()
                }) { Text(OrderStrings.leaveScanConfirm) }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text(OrderStrings.leaveScanKeep) } },
        )
    }
}
