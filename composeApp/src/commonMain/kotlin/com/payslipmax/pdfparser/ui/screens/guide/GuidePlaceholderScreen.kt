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
 * Stand-in for the search screen, which phase E4 builds (E3 replaced the feed and card stand-ins). Nothing opens
 * Search before E4, so only a restored stack can show it, in debug builds (the Guide is dark in release). E4
 * deletes this file, [GuideStrings.comingNext] and [GuideStrings.searchTitle] (plan, EP item 8).
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
