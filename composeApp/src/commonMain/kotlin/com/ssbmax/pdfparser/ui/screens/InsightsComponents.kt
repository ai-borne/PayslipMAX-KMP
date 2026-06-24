package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings
import com.ssbmax.pdfparser.ui.theme.AppStringsPremium
import kotlin.math.abs

data class MoMChange(
    val name: String,
    val prevValue: Double,
    val currValue: Double,
    val isEarning: Boolean,
)

fun formatCurrency(amount: Double): String {
    val longVal = amount.toLong()
    val str = longVal.toString()
    if (str.length <= 3) return "₹$str"
    val lastThree = str.substring(str.length - 3)
    val remaining = str.substring(0, str.length - 3)
    val builder = StringBuilder()
    var i = remaining.length
    while (i > 0) {
        if (i >= 2) {
            builder.insert(0, remaining.substring(i - 2, i))
            if (i - 2 > 0) builder.insert(0, ",")
            i -= 2
        } else {
            builder.insert(0, remaining.substring(0, 1))
            i -= 1
        }
    }
    return "₹$builder,$lastThree"
}

@Composable
fun ExecutiveSummaryCard(
    current: LedgerRecordEntity,
    previous: LedgerRecordEntity?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = AppDimensions.PaddingMedium, vertical = AppDimensions.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                text = AppStrings.executiveSummaryLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            ExecutiveSummaryRow(AppStringsPremium.execSummaryNetSalary, current.netPay, previous?.netPay)
            ExecutiveSummaryRow(AppStringsPremium.execSummaryGrossPay, current.grossPay, previous?.grossPay)
            ExecutiveSummaryRow(AppStringsPremium.execSummaryIncomeTax, current.incomeTax, previous?.incomeTax)
            ExecutiveSummaryRow(AppStringsPremium.execSummaryDsop, current.dsopSubscription, previous?.dsopSubscription)
        }
    }
}

@Composable
private fun ExecutiveSummaryRow(
    label: String,
    value: Double,
    prevValue: Double?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                text = formatCurrency(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (prevValue != null && prevValue != 0.0) {
                val diff = value - prevValue
                if (abs(diff) > 1.0) {
                    val pct = (diff / prevValue) * 100.0
                    val sign = if (diff > 0) "↑" else "↓"
                    val color = if (diff > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    Text(
                        text = "$sign${pct.toString().take(4)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
