package com.ssbmax.pdfparser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ssbmax.pdfparser.ui.theme.AppDimensions

@Composable
fun TrendChartLegend(
    label1: String,
    label2: String,
    color1: Color = MaterialTheme.colorScheme.primary,
    color2: Color = MaterialTheme.colorScheme.secondary,
) {
    ChartLegend(listOf(label1 to color1, label2 to color2))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartLegend(entries: List<Pair<String, Color>>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        entries.forEach { (label, color) -> ChartLegendEntry(label, color) }
    }
}

@Composable
private fun ChartLegendEntry(
    label: String,
    color: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(AppDimensions.SpacingMedium, AppDimensions.SpacingTiny)
                    .background(color, RoundedCornerShape(AppDimensions.CornerRadiusSmall)),
        )
        Spacer(modifier = Modifier.width(AppDimensions.SpacingSix))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
