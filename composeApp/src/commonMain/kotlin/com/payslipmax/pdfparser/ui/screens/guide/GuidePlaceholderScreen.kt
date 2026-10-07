package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * Stand-in for the feed, card and search screens, which phases E3 and E4 build. Debug builds only (the Guide is
 * dark in release). E3 deletes this file and [GuideStrings.comingNext] (plan, E2 tech-debt checkpoint).
 */
@Composable
internal fun GuidePlaceholderScreen(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenBackHeader(title = title, subtitle = subtitle?.takeIf(String::isNotBlank), onBack = onBack)
        Text(
            GuideStrings.comingNext,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
        )
    }
}
