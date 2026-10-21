package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.LeaveBankStrings

/**
 * Retirement Calculators' Leave bank card: the latest statement's printed balance, LTC days used, what the encashable part is
 * worth at the latest pay, and that statement's rows. Every figure comes from [LeaveBankCardUi], built from the summary.
 */
@Composable
fun LeaveBankCard(
    ui: LeaveBankCardUi,
    modifier: Modifier = Modifier,
) {
    FlatBorderedCard(modifier = modifier, tint = CardTint.Accent) {
        Text(
            text = LeaveBankStrings.cardTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = ui.balance, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            ui.overCap?.let { OverCapChip(it) }
        }
        SecondaryLine(ui.asOf)
        Text(text = ui.ltc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(text = ui.worth, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        SecondaryLine(ui.worthBasis)
        ExpandableDetailSection(title = ui.ordersTitle) {
            LeaveAccumulationTable(rows = ui.rows)
        }
    }
}

@Composable
private fun OverCapChip(text: String) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(AppDimensions.CornerRadius)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = AppDimensions.SpacingSmall, vertical = AppDimensions.SpacingTiny),
        )
    }
}

@Composable
private fun SecondaryLine(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
