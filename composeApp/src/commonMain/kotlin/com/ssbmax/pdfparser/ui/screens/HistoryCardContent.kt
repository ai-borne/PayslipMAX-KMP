package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

internal data class TrendInfo(
    val percentageChange: Double,
    val isIncrease: Boolean,
    val isZero: Boolean,
)

internal fun calculateTrend(
    current: ParsedPayslip,
    allPayslips: List<ParsedPayslip>,
): TrendInfo? {
    val sorted = allPayslips.sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum })
    val currentIndex = sorted.indexOfFirst { it.dateStr == current.dateStr }
    if (currentIndex <= 0) return null

    val previous = sorted[currentIndex - 1]
    val prevNet = previous.summary.netRemittance
    val currNet = current.summary.netRemittance
    if (prevNet <= 0.0) return null

    val diff = currNet - prevNet
    val pct = (diff / prevNet) * 100.0
    return TrendInfo(
        percentageChange = pct,
        isIncrease = diff > 0.0,
        isZero = diff == 0.0,
    )
}

internal fun formatPercentage(value: Double): String {
    val absVal = kotlin.math.abs(value)
    if (absVal % 1.0 == 0.0) {
        return "${absVal.toInt()}%"
    }
    val str = absVal.toString()
    val dotIndex = str.indexOf('.')
    return if (dotIndex == -1) {
        "$str%"
    } else {
        val decimals = str.length - dotIndex - 1
        if (decimals > 1) {
            "${str.substring(0, dotIndex + 2)}%"
        } else {
            "$str%"
        }
    }
}

internal fun getDisplayTaxAmount(payslip: ParsedPayslip): Double {
    return payslip.deductions.incomeTax
}

@Composable
fun HistoryCardContent(
    payslip: ParsedPayslip,
    allPayslips: List<ParsedPayslip>,
    modifier: Modifier = Modifier,
) {
    val trend = remember(payslip, allPayslips) { calculateTrend(payslip, allPayslips) }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        HistoryCardHeaderRow(payslip = payslip, trend = trend)
        CompositionSparkbar(payslip = payslip)
        HistoryCardFooterRow(payslip = payslip)
    }
}

@Composable
private fun HistoryCardHeaderRow(
    payslip: ParsedPayslip,
    trend: TrendInfo?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        ) {
            Text(
                text = payslip.monthName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (trend != null && !trend.isZero) {
                val trendColor = if (trend.isIncrease) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                Text(
                    text = " (${if (trend.isIncrease) "↑" else "↓"} ${formatPercentage(trend.percentageChange)})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = trendColor,
                )
            }
        }
        Text(
            text = "${AppStrings.historyNetTakeHomeShort} ${formatAmount(payslip.summary.netRemittance)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CompositionSparkbar(payslip: ParsedPayslip) {
    val gross = payslip.summary.grossPay
    if (gross <= 0.0) return

    val netPct = (payslip.summary.netRemittance / gross).toFloat().coerceIn(0f, 1f)
    val dsopPct = (payslip.deductions.dsopSubscription / gross).toFloat().coerceIn(0f, 1f)
    val taxPct = (getDisplayTaxAmount(payslip) / gross).toFloat().coerceIn(0f, 1f)
    val otherPct = (1.0f - netPct - dsopPct - taxPct).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AppDimensions.SpacingSix)
            .clip(RoundedCornerShape(AppDimensions.SpacingTwo))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
    ) {
        if (netPct > 0f) Box(modifier = Modifier.fillMaxHeight().weight(netPct).background(MaterialTheme.colorScheme.secondary))
        if (dsopPct > 0f) Box(modifier = Modifier.fillMaxHeight().weight(dsopPct).background(MaterialTheme.colorScheme.tertiary))
        if (taxPct > 0f) Box(modifier = Modifier.fillMaxHeight().weight(taxPct).background(MaterialTheme.colorScheme.error))
        if (otherPct > 0f) Box(modifier = Modifier.fillMaxHeight().weight(otherPct).background(MaterialTheme.colorScheme.outlineVariant))
    }
}

@Composable
private fun HistoryCardFooterRow(payslip: ParsedPayslip) {
    val tax = getDisplayTaxAmount(payslip)
    val greenColor = MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${AppStrings.historyGrossPayLabel}: ₹${formatAmount(payslip.summary.grossPay)}",
            style = MaterialTheme.typography.labelSmall,
            color = greenColor,
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = greenColor,
        )
        Text(
            text = "${AppStrings.historyDsopLabel}: ₹${formatAmount(payslip.deductions.dsopSubscription)}",
            style = MaterialTheme.typography.labelSmall,
            color = greenColor,
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = greenColor,
        )
        Text(
            text = "${AppStrings.historyTaxLabel}: ₹${formatAmount(tax)}",
            style = MaterialTheme.typography.labelSmall,
            color = greenColor,
        )
    }
}
