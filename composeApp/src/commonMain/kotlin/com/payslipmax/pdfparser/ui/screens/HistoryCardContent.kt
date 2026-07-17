package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

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
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        HistoryCardTopRow(payslip = payslip, trend = trend)
        HistoryCardAmountRow(payslip = payslip)
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        CompositionSparkbar(payslip = payslip)
        HistoryCardFooterRow(payslip = payslip)
    }
}

@Composable
private fun HistoryCardTopRow(
    payslip: ParsedPayslip,
    trend: TrendInfo?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = payslip.monthName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (trend != null && !trend.isZero) {
            TrendBadge(trend)
        }
    }
}

@Composable
private fun TrendBadge(trend: TrendInfo) {
    val color = if (trend.isIncrease) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
    val arrow = if (trend.isIncrease) "↑" else "↓"
    Surface(
        shape = RoundedCornerShape(AppDimensions.IconSizeHuge),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(AppDimensions.BorderThin, color.copy(alpha = 0.4f)),
    ) {
        Text(
            text = "$arrow ${formatPercentage(trend.percentageChange)}",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = AppDimensions.SpacingSmall, vertical = AppDimensions.SpacingTiny),
        )
    }
}

@Composable
private fun HistoryCardAmountRow(payslip: ParsedPayslip) {
    Text(
        text = "₹${formatAmount(payslip.summary.netRemittance)}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End,
    )
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
        modifier =
            Modifier
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorDot(MaterialTheme.colorScheme.secondary)
        Text(
            text = "${AppStrings.historyGrossPayLabel}: ₹${formatAmount(payslip.summary.grossPay)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ColorDot(MaterialTheme.colorScheme.tertiary)
        Text(
            text = "${AppStrings.historyDsopLabel}: ₹${formatAmount(payslip.deductions.dsopSubscription)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ColorDot(MaterialTheme.colorScheme.error)
        Text(
            text = "${AppStrings.historyTaxLabel}: ₹${formatAmount(tax)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier =
            Modifier
                .size(AppDimensions.SpacingSix)
                .clip(CircleShape)
                .background(color),
    )
}
