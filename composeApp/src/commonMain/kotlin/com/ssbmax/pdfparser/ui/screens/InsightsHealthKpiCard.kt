package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.ui.theme.AppColors
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.InsightsStrings

enum class HealthGrade(val letter: String, val label: String) {
    EXCELLENT("A", InsightsStrings.healthGradeExcellent),
    GOOD("B", InsightsStrings.healthGradeGood),
    FAIR("C", InsightsStrings.healthGradeFair),
    NEEDS_ATTENTION("D", InsightsStrings.healthGradeNeedsAttention),
}

fun gradeFor(score: Int): HealthGrade =
    when {
        score >= 90 -> HealthGrade.EXCELLENT
        score >= 75 -> HealthGrade.GOOD
        score >= 60 -> HealthGrade.FAIR
        else -> HealthGrade.NEEDS_ATTENTION
    }

@Composable
fun gradeColor(grade: HealthGrade): Color =
    when (grade) {
        HealthGrade.EXCELLENT -> MaterialTheme.colorScheme.primary
        HealthGrade.GOOD -> MaterialTheme.colorScheme.secondary
        HealthGrade.FAIR -> AppColors.Warning
        HealthGrade.NEEDS_ATTENTION -> MaterialTheme.colorScheme.error
    }

@Composable
fun HealthKpiCard(
    score: Int,
    delta: Int?,
    expanded: Boolean,
    onExpandClick: () -> Unit,
    drivers: List<WellnessDriver>,
    opportunityAmount: Double,
    onSeeHowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val grade = gradeFor(score)
    val color = gradeColor(grade)
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onExpandClick),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, color.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            HealthKpiCardHeader(score, delta, expanded, grade, color)
            if (expanded) {
                HealthKpiCardBreakdown(drivers, opportunityAmount, onSeeHowClick)
            }
        }
    }
}

@Composable
private fun HealthKpiCardHeader(
    score: Int,
    delta: Int?,
    expanded: Boolean,
    grade: HealthGrade,
    color: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(
                text = grade.letter,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Column {
                Text(
                    text = InsightsStrings.wellnessChipLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "$score/100${getDeltaText(delta)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Text(
                    text = grade.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription =
                if (expanded) InsightsStrings.wellnessChipCollapseDesc else InsightsStrings.wellnessChipExpandDesc,
            tint = color,
        )
    }
}

@Composable
private fun HealthKpiCardBreakdown(
    drivers: List<WellnessDriver>,
    opportunityAmount: Double,
    onSeeHowClick: () -> Unit,
) {
    val positiveDrivers = drivers.filter { it.pointImpact >= 0 }
    val riskDrivers = drivers.filter { it.pointImpact < 0 }

    if (positiveDrivers.isNotEmpty()) {
        HealthKpiDriverGroup(InsightsStrings.positiveFactorsTitle, positiveDrivers)
    }
    if (riskDrivers.isNotEmpty()) {
        HealthKpiDriverGroup(InsightsStrings.riskFactorsTitle, riskDrivers)
    }
    if (opportunityAmount > 0.0) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            Text(
                text = InsightsStrings.opportunityTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatCurrency(opportunityAmount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                OutlinedButton(onClick = onSeeHowClick) {
                    Text(InsightsStrings.heroWealthCtaLabel)
                }
            }
        }
    }
}

@Composable
private fun HealthKpiDriverGroup(
    title: String,
    drivers: List<WellnessDriver>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        drivers.forEach { driver -> WellnessDriverRow(driver = driver) }
    }
}
