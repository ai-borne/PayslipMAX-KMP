package com.ssbmax.pdfparser.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    color1: Color = Color(0xFF3B82F6), // Blue
    color2: Color = Color(0xFF10B981), // Emerald
    gridColor: Color = Color(0xFF1E293B),
    textColor: Color = Color(0xFF94A3B8),
    modifier: Modifier = Modifier.fillMaxWidth().height(200.dp)
) {
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(color = textColor, fontSize = 9.sp)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        val paddingLeft = 110f
        val paddingRight = 30f
        val paddingTop = 30f
        val paddingBottom = 60f
        
        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom
        
        if (labels.isEmpty() || lineData1.isEmpty()) {
            drawText(
                textMeasurer = textMeasurer,
                text = "No data available",
                topLeft = Offset(width / 2f - 60f, height / 2f - 10f),
                style = textStyle.copy(fontSize = 12.sp)
            )
            return@Canvas
        }
        
        val maxVal = maxOf(
            lineData1.maxOrNull() ?: 1.0,
            lineData2.maxOrNull() ?: 1.0
        ).coerceAtLeast(1.0)
        
        val gridCount = 4
        
        for (i in 0..gridCount) {
            val y = paddingTop + (chartHeight / gridCount) * i
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1f
            )
            
            val gridVal = maxVal * (gridCount - i) / gridCount
            val gridValStr = "₹${(gridVal / 1000).toInt()}k"
            drawText(
                textMeasurer = textMeasurer,
                text = gridValStr,
                topLeft = Offset(10f, y - 12f),
                style = textStyle
            )
        }
        
        val steps = labels.size
        val xPoints = FloatArray(steps) { i ->
            if (steps > 1) {
                paddingLeft + (chartWidth / (steps - 1)) * i
            } else {
                paddingLeft + chartWidth / 2f
            }
        }
        
        fun drawDataset(data: List<Double>, lineColor: Color) {
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
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
            )
            
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }
        
        if (lineData1.isNotEmpty()) drawDataset(lineData1, color1)
        if (lineData2.isNotEmpty()) drawDataset(lineData2, color2)
        
        labels.forEachIndexed { i, label ->
            val x = xPoints[i]
            val textLayoutResult = textMeasurer.measure(label, textStyle)
            drawText(
                textMeasurer = textMeasurer,
                text = label,
                topLeft = Offset(x - textLayoutResult.size.width / 2f, paddingTop + chartHeight + 10f),
                style = textStyle
            )
        }
    }
}

/**
 * Renders a doughnut/pie chart for earnings and deductions breakdown.
 */
@Composable
fun AllocationPieChart(
    values: List<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier.fillMaxWidth().height(180.dp)
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
                style = Stroke(width = diameter * 0.16f, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}
