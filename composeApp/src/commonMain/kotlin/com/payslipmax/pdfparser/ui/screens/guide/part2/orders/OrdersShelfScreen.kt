package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.part2.domain.ChainState
import com.payslipmax.pdfparser.part2.domain.ShelfRow
import com.payslipmax.pdfparser.part2.domain.ShelfYear
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.screens.guide.GuideTileFrame
import com.payslipmax.pdfparser.ui.screens.guide.part2.Part2ListPadding
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.chainTint
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.OrderStrings

/** The orders shelf: text rows by order date under year headings (undated orders last). Thumbnails are not built. */
@Composable
internal fun OrdersShelfScreen(
    shelf: List<ShelfYear>,
    onOpen: (String) -> Unit,
    onLens: (orders: Boolean) -> Unit,
    onBack: () -> Unit,
    listState: LazyListState,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = OrderStrings.shelfTitle, onBack = onBack)
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = Part2ListPadding, verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
            item(key = "lens") { OrdersLens(ordersSelected = true, onSelect = onLens) }
            if (shelf.isEmpty()) item(key = "empty") { Text(OrderStrings.shelfEmpty, style = MaterialTheme.typography.bodyMedium) }
            shelf.forEach { year ->
                item(key = year.year ?: 0) { Text(year.year?.toString() ?: OrderStrings.undatedHeading, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(year.rows.size, key = { year.rows[it].order.id }) { ShelfRowView(year.rows[it], onOpen) }
            }
        }
    }
}

@Composable
private fun ShelfRowView(
    row: ShelfRow,
    onOpen: (String) -> Unit,
) {
    GuideTileFrame(onClick = { onOpen(row.order.id) }, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny)) {
            Text(row.order.orderNo ?: OrderStrings.noOrderNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(row.order.orderDate?.let(PreClaimDates::format) ?: OrderStrings.noOrderDate, style = MaterialTheme.typography.bodyMedium)
            Text(OrderStrings.counter(row.completeness), style = MaterialTheme.typography.bodyMedium)
            if (row.chain == ChainState.MISSING) Text(OrderStrings.checkChain, style = MaterialTheme.typography.labelLarge, color = chainTint(), fontWeight = FontWeight.SemiBold)
        }
    }
}
