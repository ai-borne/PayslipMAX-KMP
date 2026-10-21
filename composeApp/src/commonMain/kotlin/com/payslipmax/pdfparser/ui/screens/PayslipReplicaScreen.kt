package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun PayslipReplicaScreen(
    payslip: ParsedPayslip,
    onBackClick: () -> Unit,
    onViewPdfClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCorrectField: (fieldKey: String, newValue: Double) -> Unit = { _, _ -> },
    isEditModeActive: Boolean = false,
    draftCorrections: Map<String, SingleCorrection> = emptyMap(),
    onStartEditing: () -> Unit = {},
    onUpdateDraft: (SingleCorrection) -> Unit = {},
    onDeleteDraft: (fieldKey: String, codeHead: String, category: EntryCategory, originalAmount: Double?) -> Unit = { _, _, _, _ -> },
    onSaveSession: () -> Unit = {},
    onCancelSession: () -> Unit = {},
    profileName: String = "",
    profileCda: String = "",
    profilePan: String = "",
    leaveSection: (@Composable () -> Unit)? = null,
) {
    var activeGlossaryItem by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .detailScreenSafeArea()
                    .verticalScroll(rememberScrollState())
                    .padding(AppDimensions.PaddingMedium)
                    .then(if (isEditModeActive) Modifier.padding(bottom = AppDimensions.BannerClearance) else Modifier),
        ) {
            ReplicaSections(
                payslip, onBackClick, onViewPdfClick, onCorrectField, isEditModeActive, draftCorrections, onStartEditing, onUpdateDraft, onDeleteDraft,
                onCancelSession, Triple(profileName, profileCda, profilePan), leaveSection,
                onItemClick = { code, desc -> activeGlossaryItem = code to desc },
            )
        }

        if (isEditModeActive) {
            BottomConfirmationBanner(draftCorrections.size, onCancelSession, onSaveSession)
        }

        activeGlossaryItem?.let { (code, desc) ->
            GlossaryDialog(code = code, desc = desc, onDismiss = { activeGlossaryItem = null })
        }
    }
}

@Composable
private fun GlossaryDialog(
    code: String,
    desc: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = code, fontWeight = FontWeight.Bold) },
        text = { Text(text = desc, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = AppStrings.glossaryUnderstood)
            }
        },
    )
}

@Composable
private fun BoxScope.BottomConfirmationBanner(
    draftCount: Int,
    onCancelSession: () -> Unit,
    onSaveSession: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(AppDimensions.PaddingMedium),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.CardElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${AppStrings.correctionSessionActivePrefix} ($draftCount)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onCancelSession) {
                    Text(
                        text = AppStrings.correctionSessionDiscard,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                Spacer(modifier = Modifier.width(AppDimensions.SpacingSmall))
                Button(onClick = onSaveSession) {
                    Text(
                        text = AppStrings.correctionSessionApply,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
