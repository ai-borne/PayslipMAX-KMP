package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDateBox
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.OrderStrings

/** What the attach form needs from outside. [message] is the answer to the last Save or scan that the officer should read. */
internal class AttachActions(
    val message: String?,
    val nameOf: (String) -> String?,
    val onScan: () -> Unit,
    val onDiscard: () -> Unit,
    val onAddCasualty: () -> Unit,
    val onSave: () -> Unit,
    val onBack: () -> Unit,
)

/** Scan, type the order's number and date, its count and the order it quotes, tick the casualties it covers, save. */
@Composable
internal fun AttachOrderScreen(
    vm: AttachOrderViewModel,
    state: AttachState,
    actions: AttachActions,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = OrderStrings.attachTitle, onBack = actions.onBack)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = Part2ListPadding, verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
            item(key = "scan") { ScanSection(state, actions) }
            orderFields(vm, state)
            choices(vm, state, actions)
            item(key = "message") { Messages(state, actions.message) }
            item(key = "save") {
                Button(onClick = actions.onSave, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(if (state.busy) OrderStrings.savingOrder else OrderStrings.saveOrder) }
            }
        }
    }
}

@Composable
private fun ScanSection(
    state: AttachState,
    actions: AttachActions,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (state.pages > 0) Text(OrderStrings.pagesReady(state.pages), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Button(onClick = actions.onScan, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(if (state.pages > 0) OrderStrings.scanMoreButton else OrderStrings.scanButton) }
        if (state.pages > 0) OutlinedButton(onClick = actions.onDiscard, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(OrderStrings.discardScan) }
    }
}

private fun LazyListScope.orderFields(
    vm: AttachOrderViewModel,
    state: AttachState,
) {
    item(key = "orderNo") { Field(OrderStrings.orderNoLabel, state.orderNo, AttachProblem.ORDER_NO_LONG in state.problems, vm::setOrderNo) }
    item(key = "date") { PreClaimDateBox(OrderStrings.orderDateLabel, state.date, vm::setDate) }
    item(key = "expected") { Field(OrderStrings.expectedLabel, state.expected, AttachProblem.BAD_COUNT in state.problems, vm::setExpected, KeyboardType.Number) }
    item(key = "last") { Field(OrderStrings.lastOrderLabel, state.lastOrderNo, AttachProblem.LAST_NO_LONG in state.problems, vm::setLastOrderNo) }
}

private fun LazyListScope.choices(
    vm: AttachOrderViewModel,
    state: AttachState,
    actions: AttachActions,
) {
    item(key = "heading") { Text(OrderStrings.waitingHeading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
    if (state.choices.isEmpty()) item(key = "none") { Text(OrderStrings.noWaiting, style = MaterialTheme.typography.bodyMedium) }
    items(state.choices.size, key = { state.choices[it].id }) { Choice(state.choices[it], state, vm, actions) }
    item(key = "add") { OutlinedButton(onClick = actions.onAddCasualty, modifier = Modifier.fillMaxWidth()) { Text(OrderStrings.addCasualty) } }
}

@Composable
private fun Choice(
    entry: BankEntry,
    state: AttachState,
    vm: AttachOrderViewModel,
    actions: AttachActions,
) {
    val ticked = entry.id in state.ticked
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { vm.toggle(entry.id) }) {
        Checkbox(checked = ticked, onCheckedChange = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            actions.nameOf(entry.code)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(PreClaimDates.format(entry.from), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Messages(
    state: AttachState,
    message: String?,
) {
    val lines = listOfNotNull(state.notice?.let(OrderNotices::of), message) + state.problems.map(OrderNotices::problem).distinct()
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        lines.forEach { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    isError: Boolean,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}
