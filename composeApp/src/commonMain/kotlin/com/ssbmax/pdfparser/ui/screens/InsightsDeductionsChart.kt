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
import com.ssbmax.pdfparser.ui.theme.AppColors
import com.ssbmax.pdfparser.ui.theme.AppDimensions

@Composable
fun DeductionsBarChart(
    bars: List<DeductionBar>,
    modifier: Modifier = Modifier.fillMaxWidth().height(AppDimensions.ChartHeightLarge),
    netColor: Color = MaterialTheme.colorScheme.secondary,
    dsopColor: Color = MaterialTheme.colorScheme.tertiary,
    taxColor: Color = MaterialTheme.colorScheme.error,
    otherColor: Color = AppColors.Warning,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = textColor, fontSize = AppDimensions.TextSizeTiny)
    Canvas(modifier = modifier) {
        drawDeductionBars(bars, netColor, dsopColor, taxColor, otherColor, highlightColor, textMeasurer, textStyle)
    }
}

private fun DrawScope.drawDeductionBars(
    bars: List<DeductionBar>,
    netColor: Color,
    dsopColor: Color,
    taxColor: Color,
    otherColor: Color,
    highlightColor: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
) {
    if (bars.isEmpty()) return

    val paddingBottom = 30f
    val paddingTop = 10f
    val chartHeight = size.height - paddingTop - paddingBottom
    val maxGross = bars.maxOf { it.net + it.dsop + it.tax + it.other }.coerceAtLeast(1f)

    val slotWidth = size.width / bars.size
    val barWidth = slotWidth * 0.55f

    bars.forEachIndexed { index, bar ->
        val centerX = slotWidth * index + slotWidth / 2f
        val left = centerX - barWidth / 2f
        drawStackedBar(bar, left, barWidth, paddingTop, chartHeight, maxGross, netColor, dsopColor, taxColor, otherColor)
        if (bar.isSelected) {
            drawRect(
                color = highlightColor,
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

private fun DrawScope.drawStackedBar(
    bar: DeductionBar,
    left: Float,
    barWidth: Float,
    paddingTop: Float,
    chartHeight: Float,
    maxGross: Float,
    netColor: Color,
    dsopColor: Color,
    taxColor: Color,
    otherColor: Color,
) {
    val segments = listOf(bar.net to netColor, bar.dsop to dsopColor, bar.tax to taxColor, bar.other to otherColor)
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
