package com.ssbmax.pdfparser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

/**
 * Renders a simple, elegant line chart representing values over time.
 * Supports multiple lines (e.g. Gross vs Net) with gradients.
 */
@Composable
fun TrendLineChart(
    labels: List<String>,
    lineData1: List<Double>,
    lineData2: List<Double>,
    label1: String,
    label2: String,
    modifier: Modifier = Modifier.fillMaxWidth().height(AppDimensions.ChartHeightLarge),
    color1: Color = MaterialTheme.colorScheme.primary,
    color2: Color = MaterialTheme.colorScheme.secondary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    highlightIndex: Int? = null,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = textColor, fontSize = AppDimensions.TextSizeTiny)

    Canvas(modifier = modifier) {
        drawTrendSeries(
            labels, lineData1, lineData2, color1, color2, gridColor, highlightIndex, highlightColor, textMeasurer, textStyle, textColor,
        )
    }
}

private fun DrawScope.drawTrendSeries(
    labels: List<String>,
    lineData1: List<Double>,
    lineData2: List<Double>,
    color1: Color,
    color2: Color,
    gridColor: Color,
    highlightIndex: Int?,
    highlightColor: Color,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
    textColor: Color,
) {
    val paddingLeft = 110f
    val paddingRight = 30f
    val paddingTop = 30f
    val paddingBottom = 60f
    val chartWidth = size.width - paddingLeft - paddingRight
    val chartHeight = size.height - paddingTop - paddingBottom

    if (labels.isEmpty() || lineData1.isEmpty()) {
        drawNoDataPlaceholder(textMeasurer, textColor)
        return
    }

    val maxVal = maxOf(lineData1.maxOrNull() ?: 1.0, lineData2.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    drawChartGrid(
        textMeasurer = textMeasurer,
        textStyle = textStyle,
        maxVal = maxVal,
        gridCount = 4,
        paddingLeft = paddingLeft,
        paddingRight = paddingRight,
        paddingTop = paddingTop,
        chartHeight = chartHeight,
        width = size.width,
        gridColor = gridColor,
    )

    val xPoints = computeXPoints(labels.size, paddingLeft, chartWidth)
    if (highlightIndex != null && highlightIndex in xPoints.indices) {
        drawHighlightLine(xPoints[highlightIndex], paddingTop, chartHeight, highlightColor)
    }
    if (lineData1.isNotEmpty()) drawDatasetPath(lineData1, color1, xPoints, maxVal, paddingTop, chartHeight)
    if (lineData2.isNotEmpty()) drawDatasetPath(lineData2, color2, xPoints, maxVal, paddingTop, chartHeight)
    drawXAxisLabels(labels, xPoints, paddingTop, chartHeight, textMeasurer, textStyle)
}

private fun computeXPoints(
    steps: Int,
    paddingLeft: Float,
    chartWidth: Float,
): FloatArray =
    FloatArray(steps) { i ->
        if (steps > 1) paddingLeft + (chartWidth / (steps - 1)) * i else paddingLeft + chartWidth / 2f
    }

private fun DrawScope.drawNoDataPlaceholder(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textColor: Color,
) {
    drawText(
        textMeasurer = textMeasurer,
        text = AppStrings.chartNoDataAvailable,
        topLeft = Offset(size.width / 2f - 60f, size.height / 2f - 10f),
        style = TextStyle(color = textColor, fontSize = AppDimensions.TextSizeMedium),
    )
}

private fun DrawScope.drawHighlightLine(
    x: Float,
    paddingTop: Float,
    chartHeight: Float,
    highlightColor: Color,
) {
    drawLine(
        color = highlightColor.copy(alpha = 0.3f),
        start = Offset(x, paddingTop),
        end = Offset(x, paddingTop + chartHeight),
        strokeWidth = 6f,
    )
}

private fun DrawScope.drawChartGrid(
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
    maxVal: Double,
    gridCount: Int,
    paddingLeft: Float,
    paddingRight: Float,
    paddingTop: Float,
    chartHeight: Float,
    width: Float,
    gridColor: Color,
) {
    for (i in 0..gridCount) {
        val y = paddingTop + (chartHeight / gridCount) * i
        drawLine(
            color = gridColor,
            start = Offset(paddingLeft, y),
            end = Offset(width - paddingRight, y),
            strokeWidth = 1f,
        )

        val gridVal = maxVal * (gridCount - i) / gridCount
        drawText(
            textMeasurer = textMeasurer,
            text = formatLakhs(gridVal),
            topLeft = Offset(10f, y - 12f),
            style = textStyle,
        )
    }
}

/** Formats a rupee value in lakhs for compact chart axis labels, e.g. 250000.0 -> "₹2.5L". */
fun formatLakhs(value: Double): String {
    val lakhs = (value / 1000.0).toInt() / 100.0
    val lakhsStr = lakhs.toString()
    val finalStr =
        when {
            lakhs == 0.0 -> "0"
            lakhsStr.endsWith(".0") -> lakhsStr.dropLast(2)
            else -> lakhsStr
        }
    return if (finalStr == "0") "₹0" else "₹${finalStr}L"
}

private fun DrawScope.drawDatasetPath(
    data: List<Double>,
    lineColor: Color,
    xPoints: FloatArray,
    maxVal: Double,
    paddingTop: Float,
    chartHeight: Float,
) {
    val path = Path()
    val fillPath = Path()

    var started = false

    for (i in data.indices) {
        val valY = data[i]
        val x = xPoints[i]
        val y = paddingTop + chartHeight - (chartHeight * (valY / maxVal)).toFloat()

        if (!started) {
            path.moveTo(x, y)
            fillPath.moveTo(x, paddingTop + chartHeight)
            fillPath.lineTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
            fillPath.lineTo(x, y)
        }

        drawCircle(color = lineColor, radius = 4f, center = Offset(x, y))

        if (i == data.lastIndex) {
            fillPath.lineTo(x, paddingTop + chartHeight)
            fillPath.close()
        }
    }

    drawPath(
        path = fillPath,
        brush =
            Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                startY = paddingTop,
                endY = paddingTop + chartHeight,
            ),
    )

    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = 4f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawXAxisLabels(
    labels: List<String>,
    xPoints: FloatArray,
    paddingTop: Float,
    chartHeight: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    textStyle: TextStyle,
) {
    labels.forEachIndexed { i, label ->
        val x = xPoints[i]
        val textLayoutResult = textMeasurer.measure(label, textStyle)
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(x - textLayoutResult.size.width / 2f, paddingTop + chartHeight + 10f),
            style = textStyle,
        )
    }
}

/**
 * Renders a doughnut/pie chart for earnings and deductions breakdown.
 */
@Composable
fun AllocationPieChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier.fillMaxWidth().height(AppDimensions.ChartHeightMedium),
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val sizeMin = minOf(width, height)
        val diameter = sizeMin * 0.8f
        val x = (width - diameter) / 2f
        val y = (height - diameter) / 2f

        val sum = values.sum().coerceAtLeast(1f)
        var startAngle = -90f

        values.forEachIndexed { index, value ->
            val sweepAngle = (value / sum) * 360f
            val color = colors.getOrElse(index) { Color.Gray }

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(x, y),
                size = Size(diameter, diameter),
                style = Stroke(width = diameter * 0.16f, cap = StrokeCap.Butt),
            )
            startAngle += sweepAngle
        }
    }
}
