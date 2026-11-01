package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.ChainState
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.chainTint
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.OrderStrings

/** What the order screen needs from outside. [canEdit] is false for a lapsed officer: pages and casualties are shown and the order can be deleted, nothing more. */
internal class OrderActions(
    val canEdit: Boolean,
    val nameOf: (String) -> String?,
    val onAddPages: () -> Unit,
    val onUnlock: (String) -> Unit,
    val onDeletePage: (String) -> Unit,
    val onDeleteOrder: () -> Unit,
    val onOpenEntry: (String) -> Unit,
    val onBack: () -> Unit,
    val readPage: PageReader,
)

/** One scanned order: the label, its number and date, the counter and chain check, its pages, the casualties on it and Delete. */
@Composable
internal fun OrderScreen(
    state: OrderState,
    actions: OrderActions,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = OrderStrings.orderTitle, onBack = actions.onBack)
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = Part2ListPadding, verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
            item(key = "label") { Text(OrderStrings.referenceCopy, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item(key = "summary") { Summary(state) }
            item(key = "pages") { Text(OrderStrings.pagesHeading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            pageRows(state, actions)
            pagesFooter(state, actions)
            item(key = "casualties") { Text(OrderStrings.casualtiesHeading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            casualtyRows(state, actions)
            item(key = "delete") { DeleteOrder(actions.onDeleteOrder) }
        }
    }
}

@Composable
private fun Summary(state: OrderState) {
    val order = state.order
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Text(order?.orderNo ?: OrderStrings.noOrderNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(order?.orderDate?.let(PreClaimDates::format) ?: OrderStrings.noOrderDate, style = MaterialTheme.typography.bodyMedium)
        Text(OrderStrings.counter(state.completeness), style = MaterialTheme.typography.bodyMedium)
        val quoted = order?.lastOrderNo
        if (state.chain == ChainState.MISSING && quoted != null) Text(OrderStrings.chainMissing(quoted.trim()), style = MaterialTheme.typography.bodyMedium, color = chainTint())
    }
}

private fun LazyListScope.casualtyRows(
    state: OrderState,
    actions: OrderActions,
) {
    items(state.casualties.size, key = { state.casualties[it].id }) { CasualtyRow(state.casualties[it], state.canUnlink && actions.canEdit, actions) }
}

@Composable
private fun CasualtyRow(
    entry: BankEntry,
    canUnlink: Boolean,
    actions: OrderActions,
) {
    Column(modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { actions.onOpenEntry(entry.id) }, verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
        Text(entry.code, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        actions.nameOf(entry.code)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text(PreClaimDates.format(entry.from), style = MaterialTheme.typography.bodySmall)
        if (canUnlink) TextButton(onClick = { actions.onUnlock(entry.id) }) { Text(OrderStrings.unlink) }
    }
}

@Composable
private fun DeleteOrder(onDelete: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { confirming = true }, modifier = Modifier.fillMaxWidth()) { Text(OrderStrings.deleteOrder) }
    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text(OrderStrings.deleteOrder) },
            text = { Text(OrderStrings.deleteOrderBody) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onDelete()
                }) { Text(OrderStrings.confirm) }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text(OrderStrings.cancel) } },
        )
    }
}
