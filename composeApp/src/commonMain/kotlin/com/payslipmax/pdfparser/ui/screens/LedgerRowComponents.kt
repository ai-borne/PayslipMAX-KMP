package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.domain.ConfidenceThresholds
import com.payslipmax.pdfparser.domain.CorrectionType
import com.payslipmax.pdfparser.domain.EntryCategory
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.ui.theme.AppColors
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
internal fun ReplicaLedgerTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(AppDimensions.PaddingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = AppStrings.replicaEarningTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(text = AppStrings.replicaDeductionTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
    }
}

@Composable
internal fun LedgerRowItem(
    line: DisplayLedgerLine,
    isLowConfidence: Boolean,
    isGemmaSourced: Boolean,
    isDiagnosed: Boolean,
    isEditModeActive: Boolean,
    onClick: (String, String) -> Unit,
    onReview: () -> Unit,
) {
    val textStyle = getRowTextStyle(line)
    val amountColor = getRowAmountColor(line, isLowConfidence)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { if (isEditModeActive) onReview() else onClick(line.code, line.desc) }
                .padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingSix),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowItemStart(line, isGemmaSourced, isLowConfidence, isDiagnosed, isEditModeActive, onReview)
        Text(text = formatVal(line.amount), style = textStyle, color = amountColor)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
}

@Composable
private fun RowItemStart(
    line: DisplayLedgerLine,
    isGemmaSourced: Boolean,
    isLowConfidence: Boolean,
    isDiagnosed: Boolean,
    isEditModeActive: Boolean,
    onReview: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = line.code,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (line.isDeleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
        )
        if (isGemmaSourced) GemmaSourceBadge()
        if (!isEditModeActive && (isLowConfidence || isDiagnosed)) {
            IconButton(onClick = onReview, modifier = Modifier.size(AppDimensions.IconSizeMedium)) {
                Icon(imageVector = Icons.Filled.Info, contentDescription = AppStrings.correctionIndicatorDesc, tint = AppColors.Warning, modifier = Modifier.size(AppDimensions.IconSizeSmall))
            }
        }
        if (isEditModeActive && !line.isDeleted) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(start = AppDimensions.SpacingTiny).size(AppDimensions.IconSizeSmall))
        }
    }
}

@Composable
private fun getRowTextStyle(line: DisplayLedgerLine): androidx.compose.ui.text.TextStyle =
    if (line.isDeleted) {
        MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
    } else if (line.isModified || line.isAdded) {
        MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.bodyMedium
    }

@Composable
private fun getRowAmountColor(
    line: DisplayLedgerLine,
    isLowConfidence: Boolean,
): androidx.compose.ui.graphics.Color =
    if (line.isDeleted) {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    } else if (line.isAdded || line.isModified) {
        AppColors.AiInferred
    } else if (isLowConfidence) {
        AppColors.Warning
    } else {
        MaterialTheme.colorScheme.primary
    }

@Composable
internal fun LedgerTableFooter(
    payslip: ParsedPayslip,
    creditMismatch: Double,
    debitMismatch: Double,
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)).padding(AppDimensions.SpacingMedium),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = AppStrings.ledgerGrossPay, style = MaterialTheme.typography.bodyMedium)
            Text(text = "₹${formatVal(payslip.summary.grossPay)}", fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingTiny), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = AppStrings.ledgerTotalDeductions, style = MaterialTheme.typography.bodyMedium)
            Text(text = "₹${formatVal(payslip.summary.totalDeductions)}", fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = AppDimensions.SpacingSix))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = AppStrings.replicaNetLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = "₹${formatVal(payslip.summary.netRemittance)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
        }
        if (creditMismatch > ConfidenceThresholds.ITEM_SUM_TOLERANCE || debitMismatch > ConfidenceThresholds.ITEM_SUM_TOLERANCE) {
            LedgerMismatchBanner(creditMismatch, debitMismatch)
        }
    }
}

@Composable
private fun LedgerMismatchBanner(
    creditMismatch: Double,
    debitMismatch: Double,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingSix).background(AppColors.Warning.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(AppDimensions.CornerRadiusSmall)).padding(AppDimensions.PaddingSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTiny),
    ) {
        if (creditMismatch > ConfidenceThresholds.ITEM_SUM_TOLERANCE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Info, contentDescription = AppStrings.ledgerMismatchIconDesc, tint = AppColors.Warning, modifier = Modifier.size(AppDimensions.IconSizeSmall))
                Spacer(Modifier.size(AppDimensions.SpacingTiny))
                Text(text = "${AppStrings.ledgerCreditMismatchPrefix}${formatVal(creditMismatch)}", style = MaterialTheme.typography.labelSmall, color = AppColors.Warning)
            }
        }
        if (debitMismatch > ConfidenceThresholds.ITEM_SUM_TOLERANCE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Filled.Info, contentDescription = AppStrings.ledgerMismatchIconDesc, tint = AppColors.Warning, modifier = Modifier.size(AppDimensions.IconSizeSmall))
                Spacer(Modifier.size(AppDimensions.SpacingTiny))
                Text(text = "${AppStrings.ledgerDebitMismatchPrefix}${formatVal(debitMismatch)}", style = MaterialTheme.typography.labelSmall, color = AppColors.Warning)
            }
        }
    }
}

internal fun buildDisplayList(
    baseItems: List<com.payslipmax.pdfparser.ui.screens.LedgerLine>,
    category: EntryCategory,
    draftCorrections: Map<String, SingleCorrection>,
): List<DisplayLedgerLine> {
    val displayList = mutableListOf<DisplayLedgerLine>()
    baseItems.forEach { base ->
        val draft = draftCorrections[base.fieldKey]
        if (draft != null) {
            if (draft.type == CorrectionType.DELETED) {
                displayList.add(DisplayLedgerLine(base.code, base.amount, base.desc, base.fieldKey, isDeleted = true))
            } else {
                displayList.add(DisplayLedgerLine(base.code, draft.amount, base.desc, base.fieldKey, isModified = true))
            }
        } else {
            displayList.add(DisplayLedgerLine(base.code, base.amount, base.desc, base.fieldKey))
        }
    }
    draftCorrections.values.forEach { draft ->
        if (draft.type == CorrectionType.ADDED && draft.category == category) {
            if (displayList.none { it.fieldKey == draft.fieldKey }) {
                displayList.add(DisplayLedgerLine(draft.codeHead, draft.amount, "", draft.fieldKey, isAdded = true))
            }
        }
    }
    return displayList
}

internal fun computeDisplayMismatch(
    displayList: List<DisplayLedgerLine>,
    printedTotal: Double,
): Double {
    val activeSum = displayList.filter { !it.isDeleted }.sumOf { it.amount }
    return activeSum - printedTotal
}
