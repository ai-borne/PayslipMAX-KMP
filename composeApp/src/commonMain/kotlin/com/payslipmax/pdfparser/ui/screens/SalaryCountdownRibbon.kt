package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.SalaryCountdownUiModel
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.SalaryRibbonStrings

@Composable
fun SalaryCountdownRibbon(
    countdown: SalaryCountdownUiModel,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val gradientColors = getRibbonGradientColors(secondaryColor, surfaceColor, isDark)
    val borderColor = secondaryColor.copy(alpha = if (isDark) 0.35f else 0.45f)

    Card(
        modifier = modifier.fillMaxWidth().testTag("salary_countdown_ribbon"),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        border = BorderStroke(AppDimensions.BorderThin, borderColor),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(gradientColors))
                    .padding(AppDimensions.PaddingMedium),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
                RibbonHeaderRow(countdown)
                RibbonProgressRow(countdown)
            }
        }
    }
}

@Composable
private fun RibbonHeaderRow(countdown: SalaryCountdownUiModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (countdown.isPaydayToday) "🎉" else "⚡",
                style = MaterialTheme.typography.titleMedium,
            )
            Column {
                Text(
                    text = SalaryRibbonStrings.salaryRibbonTitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${SalaryRibbonStrings.salaryRibbonPaydayPrefix} ${countdown.paydayDateFormatted}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        RibbonBadge(countdown)
    }
}

@Composable
private fun RibbonBadge(countdown: SalaryCountdownUiModel) {
    val badgeContainerColor =
        if (countdown.isPaydayToday) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
        }
    val badgeTextColor =
        if (countdown.isPaydayToday) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.secondary
        }

    val badgeText =
        when {
            countdown.isPaydayToday -> SalaryRibbonStrings.salaryRibbonPaydayToday
            countdown.daysRemaining == 1 -> "1 ${SalaryRibbonStrings.salaryRibbonDayLeftSingleSuffix}"
            else -> "${countdown.daysRemaining} ${SalaryRibbonStrings.salaryRibbonDaysLeftSuffix}"
        }

    Surface(
        shape = CircleShape,
        color = badgeContainerColor,
        modifier = Modifier.testTag("salary_countdown_badge"),
    ) {
        Text(
            text = badgeText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = badgeTextColor,
            modifier = Modifier.padding(horizontal = AppDimensions.SpacingMedium, vertical = AppDimensions.SpacingTiny),
        )
    }
}

@Composable
private fun RibbonProgressRow(countdown: SalaryCountdownUiModel) {
    LinearProgressIndicator(
        progress = { countdown.progressRatio },
        modifier =
            Modifier
                .fillMaxWidth()
                .height(AppDimensions.ProgressTrackHeight)
                .clip(RoundedCornerShape(AppDimensions.ProgressCornerRadius))
                .testTag("salary_countdown_progress"),
        color = MaterialTheme.colorScheme.secondary,
        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
    )
}

private fun getRibbonGradientColors(
    secondary: Color,
    surface: Color,
    isDark: Boolean,
): List<Color> {
    return if (isDark) {
        listOf(
            surface,
            secondary.copy(alpha = 0.16f),
            surface,
        )
    } else {
        listOf(
            surface,
            secondary.copy(alpha = 0.12f),
            surface,
        )
    }
}
