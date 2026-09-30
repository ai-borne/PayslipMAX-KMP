package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Informational dialog explaining PCDA(O) Recovery Hazards to prevent officer panic.
 * Clarifies TR-230(B) allowance collision auditing and 18% penal recovery risks.
 */
@Composable
fun HazardExplainerDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.testTag(TestTags.HAZARD_EXPLAINER_DIALOG),
        title = {
            Text(
                text = AppStringsPcdao.hazardDialogTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
            ) {
                Text(
                    text = AppStringsPcdao.hazardDialogBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("hazard_explainer_dismiss_button"),
            ) {
                Text(
                    text = AppStringsPcdao.hazardDialogDismiss,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
    )
}
