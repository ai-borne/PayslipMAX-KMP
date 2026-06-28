package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.domain.isFieldLowConfidence
import com.ssbmax.pdfparser.ui.theme.AppColors
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun LedgerSection(
    payslip: ParsedPayslip,
    modifier: Modifier = Modifier,
    onItemClick: (code: String, desc: String) -> Unit,
    onCorrectField: (fieldKey: String, newValue: Double) -> Unit = { _, _ -> },
) {
    var editingLine by remember(payslip.dateStr) { mutableStateOf<LedgerLine?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Column {
            LedgerTableHeader()

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Credits Column
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .border(
                                BorderStroke(AppDimensions.BorderHairline, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            ),
                ) {
                    getCreditsList(payslip).forEach { line ->
                        LedgerRowItem(
                            line = line,
                            isLowConfidence = payslip.isFieldLowConfidence(line.fieldKey),
                            onClick = onItemClick,
                            onReview = { editingLine = line },
                        )
                    }
                }
                // Debits Column
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .border(
                                BorderStroke(AppDimensions.BorderHairline, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            ),
                ) {
                    getDebitsList(payslip).forEach { line ->
                        LedgerRowItem(
                            line = line,
                            isLowConfidence = payslip.isFieldLowConfidence(line.fieldKey),
                            onClick = onItemClick,
                            onReview = { editingLine = line },
                        )
                    }
                }
            }

            LedgerTableFooter(payslip)
        }
    }

    editingLine?.let { line ->
        LedgerCorrectionDialog(
            line = line,
            onDismiss = { editingLine = null },
            onConfirm = { fieldKey, newValue ->
                onCorrectField(fieldKey, newValue)
                editingLine = null
            },
        )
    }
}

@Composable
private fun LedgerTableHeader() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(AppDimensions.PaddingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = AppStrings.replicaEarningTitle,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Text(
            text = AppStrings.replicaDeductionTitle,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LedgerRowItem(
    line: LedgerLine,
    isLowConfidence: Boolean,
    onClick: (String, String) -> Unit,
    onReview: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick(line.code, line.desc) }
                .padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingSix),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = line.code,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (isLowConfidence) {
                IconButton(
                    onClick = onReview,
                    modifier = Modifier.size(AppDimensions.IconSizeMedium),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = AppStrings.correctionIndicatorDesc,
                        tint = AppColors.Warning,
                        modifier = Modifier.size(AppDimensions.IconSizeSmall),
                    )
                }
            }
        }
        Text(
            text = formatVal(line.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isLowConfidence) AppColors.Warning else MaterialTheme.colorScheme.primary,
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
}

@Composable
private fun LedgerTableFooter(payslip: ParsedPayslip) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .padding(AppDimensions.SpacingMedium),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = AppStrings.ledgerGrossPay, style = MaterialTheme.typography.bodyMedium)
            Text(text = "₹${formatVal(payslip.summary.grossPay)}", fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.SpacingTiny),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = AppStrings.ledgerTotalDeductions, style = MaterialTheme.typography.bodyMedium)
            Text(text = "₹${formatVal(payslip.summary.totalDeductions)}", fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = AppDimensions.SpacingSix))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = AppStrings.replicaNetLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "₹${formatVal(payslip.summary.netRemittance)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
