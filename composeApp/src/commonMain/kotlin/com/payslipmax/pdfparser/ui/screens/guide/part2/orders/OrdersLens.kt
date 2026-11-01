package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.OrderStrings

/** The two lenses on the same data: by casualty (the Bank list) or by order (the shelf). */
@Composable
internal fun OrdersLens(
    ordersSelected: Boolean,
    onSelect: (orders: Boolean) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        FilterChip(selected = !ordersSelected, onClick = { onSelect(false) }, label = { Text(OrderStrings.lensEntries) })
        FilterChip(selected = ordersSelected, onClick = { onSelect(true) }, label = { Text(OrderStrings.lensOrders) })
    }
}
