package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun RepresentationEditor(
    draft: RepresentationDraftEntity,
    editedBody: String,
    onBodyChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        Text(
            text = "${AppStringsPremium.representationEditDraftTitle} ${draft.disputeType}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = editedBody,
            onValueChange = onBodyChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            textStyle = MaterialTheme.typography.bodySmall,
            shape = RoundedCornerShape(AppDimensions.CornerRadius),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
            ) {
                Text(AppStringsPremium.representationSaveBtn)
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(AppStrings.btnCancel)
            }
        }
    }
}
