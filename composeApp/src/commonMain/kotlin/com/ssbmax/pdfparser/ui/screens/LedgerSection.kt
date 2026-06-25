package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun LedgerSection(
    payslip: ParsedPayslip,
    modifier: Modifier = Modifier,
    onItemClick: (code: String, desc: String) -> Unit,
) {
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
                    getCreditsList(payslip).forEach { (code, amount, desc) ->
                        LedgerRowItem(code = code, amount = amount, desc = desc, onClick = onItemClick)
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
                    getDebitsList(payslip).forEach { (code, amount, desc) ->
                        LedgerRowItem(code = code, amount = amount, desc = desc, onClick = onItemClick)
                    }
                }
            }

            LedgerTableFooter(payslip)
        }
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
    code: String,
    amount: Double,
    desc: String,
    onClick: (String, String) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick(code, desc) }
                .padding(horizontal = AppDimensions.PaddingSmall, vertical = AppDimensions.SpacingSix),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = formatVal(amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
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
