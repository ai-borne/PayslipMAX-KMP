package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.payslipmax.pdfparser.part2.reminders.ShortDelayReminderScheduler
import com.payslipmax.pdfparser.subscription.isDebugBuild
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * Debug-only switch in Settings that brings every Part II reminder forward to a few seconds from now, so a device walk can see a notification
 * arrive. Nothing is composed (and Koin is not asked) in a build that is not debug, or while Part II is off. The scheduler re-sends its list
 * off the main thread because WorkManager's queries block.
 */
@Composable
fun ReminderDelaySwitch(
    enabled: Boolean = isPart2Enabled(),
    debug: Boolean = isDebugBuild(),
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    if (!enabled || !debug) return
    val scheduler = koinInject<ShortDelayReminderScheduler>()
    val scope = rememberCoroutineScope()
    var on by remember { mutableStateOf(scheduler.isShort) }
    Row(
        modifier =
            Modifier.fillMaxWidth().padding(AppDimensions.PaddingMedium).toggleable(
                value = on,
                role = Role.Switch,
                onValueChange = {
                    on = it
                    scope.launch(dispatcher) { scheduler.useShortDelay(it) }
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(Part2ReminderStrings.debugDelayTitle, style = MaterialTheme.typography.bodyLarge)
            Text(Part2ReminderStrings.debugDelayDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = on, onCheckedChange = null)
    }
}
