package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.GuideStrings

/**
 * Stands in for the paid half of a card in the free preview. It names what Premium adds and offers the upgrade; it
 * holds no card text, because the state it is drawn from has none ([GuideCardContent.full] is null).
 */
@Composable
internal fun GuideLockedPanel(onUnlock: () -> Unit) {
    FlatBorderedCardShape {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                GuideStrings.lockedTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            Text(GuideStrings.lockedBody, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onUnlock) { Text(GuideStrings.unlock) }
        }
    }
}
