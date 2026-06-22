package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun HistoricalLedgerCard(
    ledgerRecords: List<LedgerRecordEntity>,
    modifier: Modifier = Modifier,
) {
    if (ledgerRecords.isEmpty()) return
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = AppStrings.historyLedgerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Text(text = if (isExpanded) "▲" else "▼")
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
                LedgerTable(ledgerRecords = ledgerRecords)
            }
        }
    }
}

@Composable
private fun LedgerTable(
    ledgerRecords: List<LedgerRecordEntity>,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
    ) {
        LedgerTableHeader()
        ledgerRecords.forEach { record ->
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            LedgerTableRow(record = record)
        }
    }
}

@Composable
private fun LedgerTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimensions.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        val cellModifier = Modifier.width(AppDimensions.LedgerCellWidth)
        Text(text = AppStrings.historyLedgerHeaderMonth, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderBasic, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderGross, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderNet, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderDsop, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = AppStrings.historyLedgerHeaderTax, modifier = cellModifier, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LedgerTableRow(record: LedgerRecordEntity) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppDimensions.SpacingSmall),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        val cellModifier = Modifier.width(AppDimensions.LedgerCellWidth)
        Text(text = record.dateStr, modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.basicPay)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.grossPay)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.netPay)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.dsopSubscription)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
        Text(text = "₹${formatAmount(record.incomeTax)}", modifier = cellModifier, style = MaterialTheme.typography.bodySmall)
    }
}
