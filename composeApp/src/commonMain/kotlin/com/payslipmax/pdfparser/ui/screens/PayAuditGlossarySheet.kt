package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditEntryStrings
import com.payslipmax.pdfparser.ui.theme.PayAuditVerdictStrings

/** Plain-words glossary (U5): DNI, Stage, TPTA, MSP, DA, "waiting" and "verified". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayAuditGlossarySheet(
    onDismiss: () -> Unit,
    onShowOrientation: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(AppDimensions.PaddingMedium).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(PayAuditVerdictStrings.glossaryTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            PayAuditVerdictStrings.glossary.forEach { (term, meaning) ->
                Text(term, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onShowOrientation) { Text(PayAuditEntryStrings.orientationTitle) }
            OutlinedButton(onClick = onDismiss) { Text(PayAuditVerdictStrings.glossaryClose) }
        }
    }
}
