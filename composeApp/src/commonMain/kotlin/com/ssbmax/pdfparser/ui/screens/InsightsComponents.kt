package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings
import kotlin.math.abs
import androidx.compose.ui.text.font.FontWeight

data class MoMChange(
    val name: String,
    val prevValue: Double,
    val currValue: Double,
    val isEarning: Boolean
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
fun SalaryHealthScoreDial(
    score: Int,
    delta: Int?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HealthScoreDescColumn(score = score, modifier = Modifier.weight(1f).padding(end = AppDimensions.SpacingLarge))
            HealthScoreProgressIndicator(score = score, delta = delta)
        }
    }
}

@Composable
private fun HealthScoreDescColumn(
    score: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = AppStrings.salaryHealthScoreLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text = when {
                score >= 80 -> "Salary Stability: High | Allowance Continuity: Ok"
                score >= 50 -> "Salary Stability: Moderate | Check for Missing Allowances"
                else -> "Critical Discrepancy Detected | Audit Recommended"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HealthScoreProgressIndicator(
    score: Int,
    delta: Int?,
    modifier: Modifier = Modifier,
) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(AppDimensions.IconSizeHuge)) {
        val color = when {
            score >= 80 -> MaterialTheme.colorScheme.primary
            score >= 50 -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.error
        }
        CircularProgressIndicator(
            progress = { score.toFloat() / 100f },
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = AppDimensions.SpacingSix,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (delta != null && delta != 0) {
                val deltaSign = if (delta > 0) "+$delta" else "$delta"
                val deltaColor = if (delta > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                Text(
                    text = deltaSign,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = deltaColor,
                )
            }
        }
    }
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
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = AppStrings.executiveSummaryLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            ExecutiveSummaryRow("Net Salary", current.netPay, previous?.netPay)
            ExecutiveSummaryRow("Gross Pay", current.grossPay, previous?.grossPay)
            ExecutiveSummaryRow("Income Tax", current.incomeTax, previous?.incomeTax)
            ExecutiveSummaryRow("DSOP Subscription", current.dsopSubscription, previous?.dsopSubscription)
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
