package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.screens.FlatBorderedCardShape
import com.payslipmax.pdfparser.ui.theme.AppDimensions

/** The frame every worksheet section shares: the app's flat bordered card, a heading, an optional quiet help line, then [content]. */
@Composable
internal fun PreClaimSectionFrame(
    title: String,
    help: String? = null,
    content: @Composable () -> Unit,
) {
    FlatBorderedCardShape {
        Column(
            modifier = Modifier.fillMaxWidth().padding(AppDimensions.SpacingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { heading() })
            help?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

/** A quiet explanatory line under a box or group. */
@Composable
internal fun PreClaimQuietText(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
