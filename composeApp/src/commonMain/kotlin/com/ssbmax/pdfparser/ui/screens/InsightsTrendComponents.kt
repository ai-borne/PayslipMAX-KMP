package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun Sparkline(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: 0f
        val max = values.maxOrNull() ?: 1f
        val range = if (max - min == 0f) 1f else max - min
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min) / range * size.height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = AppDimensions.BorderMedium.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun TrendsSparklinesSection(
    history: List<LedgerRecordEntity>,
    modifier: Modifier = Modifier,
) {
    val historySorted = remember(history) {
        history.sortedWith(compareBy<LedgerRecordEntity> { it.year }.thenBy { it.monthNum }).takeLast(6)
    }
    if (historySorted.size >= 2) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier.padding(AppDimensions.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
            ) {
                Text(
                    text = AppStrings.historicalTrendsLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
                ) {
                    SparklineCard(
                        title = "Gross Pay",
                        currentValue = historySorted.last().grossPay,
                        values = historySorted.map { it.grossPay.toFloat() },
                        modifier = Modifier.weight(1f),
                    )
                    SparklineCard(
                        title = "Net Pay",
                        currentValue = historySorted.last().netPay,
                        values = historySorted.map { it.netPay.toFloat() },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SparklineCard(
    title: String,
    currentValue: Double,
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatCurrency(currentValue),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            Sparkline(
                values = values,
                modifier = Modifier.fillMaxWidth().height(AppDimensions.IconSizeLarge),
                lineColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun AuditsArchiveGrid(
    payslips: List<ParsedPayslip>,
    selected: ParsedPayslip,
    onSelect: (ParsedPayslip) -> Unit,
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
                text = AppStrings.historicalAuditsArchiveLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            ) {
                items(items = payslips.sortedWith(compareBy<ParsedPayslip> { it.year }.thenBy { it.monthNum }).reversed()) { payslip ->
                    ArchiveFolderCard(
                        monthName = payslip.monthName,
                        year = payslip.year.toString(),
                        isSelected = payslip.dateStr == selected.dateStr,
                        onClick = { onSelect(payslip) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveFolderCard(
    monthName: String,
    year: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    }
    Surface(
        modifier = Modifier.width(AppDimensions.IconSizeHuge + AppDimensions.SpacingLarge).clickable(onClick = onClick),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = containerColor,
        border = BorderStroke(AppDimensions.BorderThin, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "📁",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = monthName.take(3),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = year,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
