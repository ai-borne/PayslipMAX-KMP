package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/**
 * Consolidated back-navigation header: an [IconButton] with the standard
 * [Icons.AutoMirrored.Filled.ArrowBack] arrow followed by a title/subtitle block. This is the single
 * source of truth for the "back arrow + screen title" affordance across all detail screens,
 * replacing the three divergent styles (filled Button, TextButton, ad-hoc IconButton) that existed
 * before. Callers wanting a trailing action (e.g. the replica screen's edit toggle) place this in a
 * parent [Row] with `Modifier.weight(1f)` and add their action after it.
 */
@Composable
fun ScreenBackHeader(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = AppStrings.btnBack,
            )
        }
        Spacer(modifier = Modifier.width(AppDimensions.SpacingSmall))
        HeaderTitleBlock(title = title, subtitle = subtitle, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun HeaderTitleBlock(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
