package com.payslipmax.pdfparser.ui.screens.guide.part2.reminders

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.payslipmax.pdfparser.part2.reminders.NotificationPermission
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import kotlinx.coroutines.launch

/** Our own sentence before the system's permission dialog. Tapping outside is "Not now". */
@Composable
fun ReminderPromptDialog(
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onNotNow,
        title = { Text(Part2ReminderStrings.promptTitle) },
        text = { Text(Part2ReminderStrings.promptBody) },
        confirmButton = { TextButton(onClick = onAllow) { Text(Part2ReminderStrings.promptAllow) } },
        dismissButton = { TextButton(onClick = onNotNow) { Text(Part2ReminderStrings.promptNotNow) } },
    )
}

/**
 * Shows the one-time explainer while Part II is on screen, the first time the officer has a reminder. [permission] is built from the
 * screen (Android's system dialog needs an Activity). All the rules (once only, a decline is final) are [ReminderPromptModel]'s.
 */
@Composable
fun ReminderPromptHost(
    model: ReminderPromptModel,
    permission: NotificationPermission,
) {
    val has by model.hasReminders.collectAsState()
    val visible by model.visible.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(has) { model.onRemindersExist(permission) } // the model itself ignores a call with no reminder
    if (visible) ReminderPromptDialog(onAllow = { scope.launch { model.allow(permission) } }, onNotNow = model::notNow)
}
