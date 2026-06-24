package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.ssbmax.pdfparser.ui.components.formatLakhs
import com.ssbmax.pdfparser.ui.theme.AppDimensions

/** Net is the bold "what you kept" color; the rest are muted neutrals so the eye lands on Net first. */
data class DeductionsChartColors(
    val net: Color,
    val dsop: Color,
    val tax: Color,
    val other: Color,
    val highlight: Color,
)

@Composable
fun deductionsChartColors(): DeductionsChartColors {
    val neutral = MaterialTheme.colorScheme.onSurfaceVariant
    return DeductionsChartColors(
        net = MaterialTheme.colorScheme.secondary,
        dsop = neutral.copy(alpha = 0.55f),
        tax = neutral.copy(alpha = 0.35f),
        other = neutral.copy(alpha = 0.20f),
        highlight = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun DeductionsBarChart(
    bars: List<DeductionBar>,
    colors: DeductionsChartColors = deductionsChartColors(),
    modifier: Modifier = Modifier.fillMaxWidth().height(AppDimensions.ChartHeightLarge),
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = textColor, fontSize = AppDimensions.TextSizeTiny)
    Canvas(modifier = modifier) {
        drawDeductionBars(bars, colors, gridColor, textMeasurer, textStyle)
    }
}

private fun DrawScope.drawDeductionBars(
    bars: List<DeductionBar>,
    colors: DeductionsChartColors,
    gridColor: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
) {
    if (bars.isEmpty()) return

    val paddingLeft = 50f
    val paddingBottom = 30f
    val paddingTop = 10f
    val chartWidth = size.width - paddingLeft
    val chartHeight = size.height - paddingTop - paddingBottom
    val maxGross = bars.maxOf { it.net + it.dsop + it.tax + it.other }.coerceAtLeast(1f)

    drawDeductionsYAxis(maxGross, paddingLeft, paddingTop, chartHeight, size.width, gridColor, textMeasurer, textStyle)

    val slotWidth = chartWidth / bars.size
    val barWidth = slotWidth * 0.55f

    bars.forEachIndexed { index, bar ->
        val centerX = paddingLeft + slotWidth * index + slotWidth / 2f
        val left = centerX - barWidth / 2f
        drawStackedBar(bar, left, barWidth, paddingTop, chartHeight, maxGross, colors)
        if (bar.isSelected) {
            drawRect(
                color = colors.highlight,
                topLeft = Offset(left - 4f, paddingTop - 4f),
                size = Size(barWidth + 8f, chartHeight + 8f),
                style = Stroke(width = 3f),
            )
        }
        val labelLayout = textMeasurer.measure(bar.label, textStyle)
        drawText(
            textMeasurer = textMeasurer,
            text = bar.label,
            topLeft = Offset(centerX - labelLayout.size.width / 2f, paddingTop + chartHeight + 8f),
            style = textStyle,
        )
    }
}

private fun DrawScope.drawDeductionsYAxis(
    maxGross: Float,
    paddingLeft: Float,
    paddingTop: Float,
    chartHeight: Float,
    width: Float,
    gridColor: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
) {
    val gridCount = 3
    for (i in 0..gridCount) {
        val y = paddingTop + (chartHeight / gridCount) * i
        drawLine(
            color = gridColor,
            start = Offset(paddingLeft, y),
            end = Offset(width, y),
            strokeWidth = 1f,
        )
        val gridVal = maxGross * (gridCount - i) / gridCount
        drawText(
            textMeasurer = textMeasurer,
            text = formatLakhs(gridVal.toDouble()),
            topLeft = Offset(0f, y - 12f),
            style = textStyle,
        )
    }
}

private fun DrawScope.drawStackedBar(
    bar: DeductionBar,
    left: Float,
    barWidth: Float,
    paddingTop: Float,
    chartHeight: Float,
    maxGross: Float,
    colors: DeductionsChartColors,
) {
    val segments = listOf(bar.net to colors.net, bar.dsop to colors.dsop, bar.tax to colors.tax, bar.other to colors.other)
    var bottomY = paddingTop + chartHeight
    segments.forEach { (value, color) ->
        val segmentHeight = chartHeight * (value / maxGross)
        drawRect(
            color = color,
            topLeft = Offset(left, bottomY - segmentHeight),
            size = Size(barWidth, segmentHeight),
        )
        bottomY -= segmentHeight
    }
}
