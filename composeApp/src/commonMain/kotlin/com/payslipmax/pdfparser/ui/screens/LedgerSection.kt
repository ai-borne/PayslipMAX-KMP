package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.CorrectionType
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.domain.diagnosticSuggestionFor
import com.payslipmax.pdfparser.domain.isFieldDiagnosed
import com.payslipmax.pdfparser.domain.isFieldGemmaSourced
import com.payslipmax.pdfparser.domain.isFieldLowConfidence
import com.payslipmax.pdfparser.ui.theme.AppDimensions

internal data class DisplayLedgerLine(
    val code: String,
    val amount: Double,
    val desc: String,
    val fieldKey: String,
    val isModified: Boolean = false,
    val isDeleted: Boolean = false,
    val isAdded: Boolean = false,
)

@Composable
fun LedgerSection(
    payslip: ParsedPayslip,
    modifier: Modifier = Modifier,
    onItemClick: (code: String, desc: String) -> Unit,
    onCorrectField: (fieldKey: String, newValue: Double) -> Unit = { _, _ -> },
    isEditModeActive: Boolean = false,
    draftCorrections: Map<String, SingleCorrection> = emptyMap(),
    onUpdateDraft: (SingleCorrection) -> Unit = {},
    onDeleteDraft: (fieldKey: String, codeHead: String, category: EntryCategory, originalAmount: Double?) -> Unit = { _, _, _, _ -> },
) {
    var editingLine by remember(payslip.dateStr) { mutableStateOf<DisplayLedgerLine?>(null) }
    var addingEarning by remember { mutableStateOf(false) }
    var addingDeduction by remember { mutableStateOf(false) }

    val displayCredits =
        if (isEditModeActive) {
            buildDisplayList(getCreditsList(payslip), EntryCategory.EARNING, draftCorrections)
        } else {
            getCreditsList(payslip).map { DisplayLedgerLine(it.code, it.amount, it.desc, it.fieldKey) }
        }
    val displayDebits =
        if (isEditModeActive) {
            buildDisplayList(getDebitsList(payslip), EntryCategory.DEDUCTION, draftCorrections)
        } else {
            getDebitsList(payslip).map { DisplayLedgerLine(it.code, it.amount, it.desc, it.fieldKey) }
        }

    val creditMismatch = if (isEditModeActive) computeDisplayMismatch(displayCredits, payslip.summary.grossPay) else creditsMismatch(payslip)
    val debitMismatch = if (isEditModeActive) computeDisplayMismatch(displayDebits, payslip.summary.totalDeductions) else debitsMismatch(payslip)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Column {
            ReplicaLedgerTableHeader()
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                CreditsColumn(payslip, displayCredits, isEditModeActive, onItemClick, onReview = { editingLine = it }, onAddClick = { addingEarning = true })
                DebitsColumn(payslip, displayDebits, isEditModeActive, onItemClick, onReview = { editingLine = it }, onAddClick = { addingDeduction = true })
            }
            LedgerTableFooter(payslip, creditMismatch, debitMismatch)
        }
    }

    DialogHandles(
        editingLine = editingLine, addingEarning = addingEarning, addingDeduction = addingDeduction,
        payslip = payslip, isEditModeActive = isEditModeActive, draftCorrections = draftCorrections,
        onUpdateDraft = onUpdateDraft, onDeleteDraft = onDeleteDraft, onCorrectField = onCorrectField,
        onDismissEditing = { editingLine = null }, onDismissAddingEarning = { addingEarning = false },
        onDismissAddingDeduction = { addingDeduction = false },
    )
}

@Composable
private fun RowScope.CreditsColumn(
    payslip: ParsedPayslip,
    displayCredits: List<DisplayLedgerLine>,
    isEditModeActive: Boolean,
    onItemClick: (code: String, desc: String) -> Unit,
    onReview: (DisplayLedgerLine) -> Unit,
    onAddClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .weight(1f)
                .border(BorderStroke(AppDimensions.BorderHairline, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))),
    ) {
        displayCredits.forEach { line ->
            LedgerRowItem(
                line = line,
                isLowConfidence = payslip.isFieldLowConfidence(line.fieldKey),
                isGemmaSourced = payslip.isFieldGemmaSourced(line.fieldKey),
                isDiagnosed = payslip.isFieldDiagnosed(line.fieldKey),
                isEditModeActive = isEditModeActive,
                onClick = onItemClick,
                onReview = { onReview(line) },
            )
        }
        if (isEditModeActive) {
            AddEntryButton(isEarning = true, onClick = onAddClick)
        }
    }
}

@Composable
private fun RowScope.DebitsColumn(
    payslip: ParsedPayslip,
    displayDebits: List<DisplayLedgerLine>,
    isEditModeActive: Boolean,
    onItemClick: (code: String, desc: String) -> Unit,
    onReview: (DisplayLedgerLine) -> Unit,
    onAddClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .weight(1f)
                .border(BorderStroke(AppDimensions.BorderHairline, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))),
    ) {
        displayDebits.forEach { line ->
            LedgerRowItem(
                line = line,
                isLowConfidence = payslip.isFieldLowConfidence(line.fieldKey),
                isGemmaSourced = payslip.isFieldGemmaSourced(line.fieldKey),
                isDiagnosed = payslip.isFieldDiagnosed(line.fieldKey),
                isEditModeActive = isEditModeActive,
                onClick = onItemClick,
                onReview = { onReview(line) },
            )
        }
        if (isEditModeActive) {
            AddEntryButton(isEarning = false, onClick = onAddClick)
        }
    }
}

@Composable
private fun AddEntryButton(
    isEarning: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = AppDimensions.SpacingTiny),
        contentPadding = PaddingValues(AppDimensions.SpacingSix),
    ) {
        Text(
            text = if (isEarning) "+ Add Earning" else "+ Add Deduction",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun DialogHandles(
    editingLine: DisplayLedgerLine?,
    addingEarning: Boolean,
    addingDeduction: Boolean,
    payslip: ParsedPayslip,
    isEditModeActive: Boolean,
    draftCorrections: Map<String, SingleCorrection>,
    onUpdateDraft: (SingleCorrection) -> Unit,
    onDeleteDraft: (fieldKey: String, codeHead: String, category: EntryCategory, originalAmount: Double?) -> Unit,
    onCorrectField: (fieldKey: String, newValue: Double) -> Unit,
    onDismissEditing: () -> Unit,
    onDismissAddingEarning: () -> Unit,
    onDismissAddingDeduction: () -> Unit,
) {
    editingLine?.let { line ->
        EditDialog(
            line = line,
            payslip = payslip,
            isEditModeActive = isEditModeActive,
            draftCorrections = draftCorrections,
            onUpdateDraft = onUpdateDraft,
            onDeleteDraft = onDeleteDraft,
            onCorrectField = onCorrectField,
            onDismiss = onDismissEditing,
        )
    }

    if (addingEarning) {
        AddDialog(isEarning = true, onDismiss = onDismissAddingEarning, onUpdateDraft = onUpdateDraft)
    }
    if (addingDeduction) {
        AddDialog(isEarning = false, onDismiss = onDismissAddingDeduction, onUpdateDraft = onUpdateDraft)
    }
}

@Composable
private fun EditDialog(
    line: DisplayLedgerLine,
    payslip: ParsedPayslip,
    isEditModeActive: Boolean,
    draftCorrections: Map<String, SingleCorrection>,
    onUpdateDraft: (SingleCorrection) -> Unit,
    onDeleteDraft: (fieldKey: String, codeHead: String, category: EntryCategory, originalAmount: Double?) -> Unit,
    onCorrectField: (fieldKey: String, newValue: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val isEarning = getCreditsList(payslip).any { it.fieldKey == line.fieldKey } || line.isAdded
    val isDeletedInitial = draftCorrections[line.fieldKey]?.type == CorrectionType.DELETED
    val original = if (line.isAdded) null else line.amount
    val category = if (isEarning) EntryCategory.EARNING else EntryCategory.DEDUCTION
    LedgerCorrectionDialog(
        line = LedgerLine(line.code, line.amount, line.desc, line.fieldKey),
        isEarning = isEarning,
        isDeletedInitial = isDeletedInitial,
        reasons = if (payslip.needsReview) payslip.reviewReasons else emptyList(),
        diagnosticHint = payslip.diagnosticSuggestionFor(line.fieldKey),
        onDismiss = onDismiss,
        onConfirm = { fieldKey, codeHead, amount, isDelete ->
            if (isEditModeActive) {
                if (isDelete) {
                    onDeleteDraft(fieldKey, codeHead, category, original)
                } else {
                    onUpdateDraft(
                        SingleCorrection(
                            fieldKey = fieldKey,
                            codeHead = codeHead,
                            amount = amount,
                            category = category,
                            type = if (line.isAdded) CorrectionType.ADDED else CorrectionType.EDITED,
                            originalAmount = original,
                            originalCodeHead = codeHead,
                            timestamp = 0L,
                        ),
                    )
                }
            } else {
                onCorrectField(fieldKey, amount)
            }
            onDismiss()
        },
    )
}

@Composable
private fun AddDialog(
    isEarning: Boolean,
    onDismiss: () -> Unit,
    onUpdateDraft: (SingleCorrection) -> Unit,
) {
    LedgerCorrectionDialog(
        line = null,
        isEarning = isEarning,
        onDismiss = onDismiss,
        onConfirm = { fieldKey, codeHead, amount, _ ->
            onUpdateDraft(
                SingleCorrection(
                    fieldKey = fieldKey,
                    codeHead = codeHead,
                    amount = amount,
                    category = if (isEarning) EntryCategory.EARNING else EntryCategory.DEDUCTION,
                    type = CorrectionType.ADDED,
                    originalAmount = null,
                    originalCodeHead = codeHead,
                    timestamp = 0L,
                ),
            )
            onDismiss()
        },
    )
}
