package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PayslipReplicaScreen(
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selectedPayslip
    var activeGlossaryItem by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (selected == null) {
        NoSelectedPayslipView()
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(AppDimensions.PaddingMedium),
    ) {
        HeaderSection()
        Spacer(modifier = Modifier.height(16.dp))

        MetadataSection(payslip = selected)
        Spacer(modifier = Modifier.height(16.dp))

        LedgerSection(payslip = selected) { code, desc ->
            activeGlossaryItem = code to desc
        }

        Spacer(modifier = Modifier.height(24.dp))
        FooterSection(payslip = selected)

        activeGlossaryItem?.let { (code, desc) ->
            GlossaryDialog(code = code, desc = desc, onDismiss = { activeGlossaryItem = null })
        }
    }
}

@Composable
private fun HeaderSection() {
    Text(
        text = AppStrings.explorerHeader,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
        text = AppStrings.explorerSubheader,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun NoSelectedPayslipView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Please import or select a payslip first.")
    }
}

@Composable
private fun MetadataSection(payslip: ParsedPayslip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = "Officer: ${payslip.officer.name}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "CDA A/C: ${payslip.officer.accountNo}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "PAN: ${payslip.officer.pan}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun LedgerSection(
    payslip: ParsedPayslip,
    onItemClick: (code: String, desc: String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Column {
            LedgerTableHeader()

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Credits Column
                Column(
                    modifier = Modifier.weight(1f).border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))),
                ) {
                    getCreditsList(payslip).forEach { (code, amount, desc) ->
                        LedgerRowItem(code = code, amount = amount, desc = desc, onClick = onItemClick)
                    }
                }
                // Debits Column
                Column(
                    modifier = Modifier.weight(1f).border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))),
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
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(8.dp),
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
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
            text = "₹${formatVal(amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
}

@Composable
private fun LedgerTableFooter(payslip: ParsedPayslip) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)).padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Gross Pay (Credits)", style = MaterialTheme.typography.bodyMedium)
            Text(text = "₹${formatVal(payslip.summary.grossPay)}", fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Total Deductions (Debits)", style = MaterialTheme.typography.bodyMedium)
            Text(text = "₹${formatVal(payslip.summary.totalDeductions)}", fontWeight = FontWeight.Bold)
        }
        Divider(modifier = Modifier.padding(vertical = 6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Net Take-Home Remittance",
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

@Composable
private fun FooterSection(payslip: ParsedPayslip) {
    Text(
        text = AppStrings.replicaFooter,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
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
                Text("Understood")
            }
        },
    )
}
