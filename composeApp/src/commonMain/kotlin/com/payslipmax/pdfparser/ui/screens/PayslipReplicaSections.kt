package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

/** The ordered sections of a replica payslip, laid out inside the screen's scrolling column. */
@Composable
internal fun ColumnScope.ReplicaSections(
    payslip: ParsedPayslip,
    onBackClick: () -> Unit,
    onViewPdfClick: (String) -> Unit,
    onCorrectField: (fieldKey: String, newValue: Double) -> Unit,
    isEditModeActive: Boolean,
    draftCorrections: Map<String, SingleCorrection>,
    onStartEditing: () -> Unit,
    onUpdateDraft: (SingleCorrection) -> Unit,
    onDeleteDraft: (fieldKey: String, codeHead: String, category: EntryCategory, originalAmount: Double?) -> Unit,
    onCancelSession: () -> Unit,
    profile: Triple<String, String, String>,
    leaveSection: (@Composable () -> Unit)?,
    onItemClick: (code: String, desc: String) -> Unit,
) {
    ReplicaHeader(onBackClick, isEditModeActive, onStartEditing, onCancelSession)
    Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))

    MetadataSection(payslip = payslip, profileName = profile.first, profileCda = profile.second, profilePan = profile.third)
    Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))

    PdfDocumentCard(payslip = payslip, onViewPdfClick = onViewPdfClick)
    Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))

    LedgerSection(
        payslip = payslip,
        onItemClick = onItemClick,
        onCorrectField = onCorrectField,
        isEditModeActive = isEditModeActive,
        draftCorrections = draftCorrections,
        onUpdateDraft = onUpdateDraft,
        onDeleteDraft = onDeleteDraft,
    )
    leaveSection?.let {
        Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
        it()
    }

    Spacer(modifier = Modifier.height(AppDimensions.SpacingHuge))
    FooterSection()
}

@Composable
private fun ReplicaHeader(
    onBackClick: () -> Unit,
    isEditModeActive: Boolean,
    onStartEditing: () -> Unit,
    onCancelSession: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ScreenBackHeader(
            title = AppStrings.explorerHeader,
            subtitle = AppStrings.explorerSubheader,
            onBack = onBackClick,
            modifier = Modifier.weight(1f),
        )
        Column(
            modifier =
                Modifier
                    .clickable(onClick = { if (isEditModeActive) onCancelSession() else onStartEditing() })
                    .padding(AppDimensions.SpacingSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val editTint = if (isEditModeActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            Icon(
                imageVector = if (isEditModeActive) Icons.Default.Close else Icons.Default.Edit,
                contentDescription = null,
                tint = editTint,
            )
            Text(
                text = if (isEditModeActive) AppStrings.replicaCancelLabel else AppStrings.replicaEditLabel,
                style = MaterialTheme.typography.labelSmall,
                color = editTint,
            )
        }
    }
}

@Composable
private fun MetadataSection(
    payslip: ParsedPayslip,
    profileName: String = "",
    profileCda: String = "",
    profilePan: String = "",
) {
    val (name, cda, pan) = resolveOfficerDisplayDetails(payslip.officer, profileName, profileCda, profilePan)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = "${AppStrings.replicaOfficerPrefix}$name",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingTiny),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "${AppStrings.replicaCdaPrefix}$cda", style = MaterialTheme.typography.bodyMedium)
                Text(text = "${AppStrings.replicaPanPrefix}$pan", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun FooterSection() {
    Text(
        text = AppStrings.replicaFooter,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
