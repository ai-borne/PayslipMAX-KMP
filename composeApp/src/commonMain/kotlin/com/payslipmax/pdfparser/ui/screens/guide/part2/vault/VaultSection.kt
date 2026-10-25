package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.guide.GuideTileFrame
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2Message
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DeleteButton
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings

/**
 * What the Part II home shows of the vault: the stored orders, whether the officer may scan ([canScan], the entitlement), the last scan's
 * [notice] and what a tap on a row, its delete button, the scan button or the notice's "OK" does.
 */
class Part2VaultList(
    val state: VaultUiState,
    val notice: String?,
    val canScan: Boolean,
    val onScan: () -> Unit,
    val onOpen: (id: String) -> Unit,
    val onDelete: (id: String) -> Unit,
    val onDismissNotice: () -> Unit,
)

/**
 * The "Your published orders" section of the Part II home: shown to an officer who may scan and to anyone who holds an order (so a lapsed
 * officer still reaches view and delete), and to no one else. The scan button is drawn only for an entitled officer.
 */
internal fun LazyListScope.part2VaultItems(vault: Part2VaultList) {
    val state = vault.state
    if (!vault.canScan && !state.hasAny && !state.unreadable) return
    item(key = "vault-heading") { Text(Part2VaultStrings.vaultHeading, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() }) }
    vault.notice?.let { notice -> item(key = "vault-notice") { VaultNoticeView(notice, vault.onDismissNotice) } }
    if (vault.canScan) item(key = "vault-scan") { Button(onClick = vault.onScan, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(Part2VaultStrings.scanOrder) } }
    if (state.unreadable) item(key = "vault-unreadable") { Part2Message(Part2VaultStrings.vaultUnreadable) }
    items(state.rows, key = { "order-${it.id}" }) { row ->
        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
            VaultRowTile(row) { vault.onOpen(row.id) }
            Part2DeleteButton(Part2VaultStrings.deleteOrder, Part2VaultStrings.deleteOrderTitle, Part2VaultStrings.deleteOrderBody) { vault.onDelete(row.id) }
        }
    }
}

/** One stored order: its number and date (or that the number is not typed yet), its page count and the day it was added. */
@Composable
internal fun VaultRowTile(
    row: VaultRowUi,
    onClick: () -> Unit,
) {
    GuideTileFrame(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
            Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(row.line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** What the last scan said, read out when it appears, with "OK" to dismiss it. */
@Composable
internal fun VaultNoticeView(
    text: String,
    onDismiss: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite })
        TextButton(onClick = onDismiss) { Text(Part2VaultStrings.noticeDismiss) }
    }
}

/** What the slip preview shows of the vault: this draft's orders and the way to add the published one (only for an entitled officer). */
class SlipOrders(
    val rows: List<VaultRowUi>,
    val canAdd: Boolean,
    val busy: Boolean,
    val notice: String?,
    val onAdd: () -> Unit,
    val onOpen: (id: String) -> Unit,
    val onDismissNotice: () -> Unit,
)

/** The published orders linked to this slip, a scan's notice, and "Add the published order". */
@Composable
internal fun SlipOrdersSection(orders: SlipOrders) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        if (orders.rows.isNotEmpty()) {
            Text(Part2VaultStrings.linkedOrdersHeading, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
            orders.rows.forEach { row -> VaultRowTile(row) { orders.onOpen(row.id) } }
        }
        orders.notice?.let { VaultNoticeView(it, orders.onDismissNotice) }
        if (orders.canAdd) Button(onClick = orders.onAdd, enabled = !orders.busy, modifier = Modifier.fillMaxWidth()) { Text(Part2VaultStrings.addPublishedOrder) }
    }
}
