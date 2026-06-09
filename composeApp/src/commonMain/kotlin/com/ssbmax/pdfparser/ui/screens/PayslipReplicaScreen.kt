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
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun PayslipReplicaScreen(
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selectedPayslip
    var activeGlossaryItem by remember { mutableStateOf<Pair<String, String>?>(null) }

    if (selected == null) {
        NoSelectedPayslipView()
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(AppDimensions.PaddingMedium)
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
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = AppStrings.explorerSubheader,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = "Officer: ${payslip.officer.name}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
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
    onItemClick: (code: String, desc: String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column {
            LedgerTableHeader()
            
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                // Credits Column
                Column(modifier = Modifier.weight(1f).border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))) {
                    GetCreditsList(payslip).forEach { (code, amount, desc) ->
                        LedgerRowItem(code = code, amount = amount, desc = desc, onClick = onItemClick)
                    }
                }
                // Debits Column
                Column(modifier = Modifier.weight(1f).border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)))) {
                    GetDebitsList(payslip).forEach { (code, amount, desc) ->
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
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = AppStrings.replicaEarningTitle,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            text = AppStrings.replicaDeductionTitle,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LedgerRowItem(
    code: String,
    amount: Double,
    desc: String,
    onClick: (String, String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(code, desc) }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "₹${formatVal(amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
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
            Text(text = "Net Take-Home Remittance", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(text = "₹${formatVal(payslip.summary.netRemittance)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
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
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun GlossaryDialog(
    code: String,
    desc: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = code, fontWeight = FontWeight.Bold) },
        text = { Text(text = desc, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Understood")
            }
        }
    )
}

private fun formatVal(value: Double): String {
    val longVal = value.toLong()
    val str = longVal.toString()
    if (str.length <= 3) return str
    val lastThree = str.substring(str.length - 3)
    val remaining = str.substring(0, str.length - 3)
    val builder = StringBuilder()
    var i = remaining.length
    while (i > 0) {
        if (i >= 2) {
            builder.insert(0, remaining.substring(i - 2, i))
            if (i - 2 > 0) builder.insert(0, ",")
            i -= 2
        } else {
            builder.insert(0, remaining.substring(0, 1))
            i -= 1
        }
    }
    return "${builder.toString()},$lastThree"
}

private fun GetCreditsList(payslip: ParsedPayslip): List<Triple<String, Double, String>> {
    val earnings = payslip.earnings
    return listOf(
        Triple("BPAY", earnings.basicPay, "Core salary based on rank and service years under 7th Pay Commission rules."),
        Triple("MSP", earnings.militaryServicePay, "Military Service Pay. Compensates for hazourdous and volatile lifestyle of military personnel."),
        Triple("DA", earnings.dearnessAllowance, "Dearness Allowance. Cost of living adjustment, revised twice a year."),
        Triple("TPTA", earnings.transportAllowance, "Transport Allowance. Commuting allowance based on duty station."),
        Triple("TPTADA", earnings.transportAllowanceDa, "Dearness Allowance computed on Transport Allowance amount."),
        Triple("RSHNA", earnings.rationMoney, "Ration Money Allowance. Dietary compensation when messy is not occupied."),
        Triple("DRESALW", earnings.dressAllowance, "Annual uniform allowance credited usually in July month."),
        Triple("SPCDO", earnings.specialForcesPay, "Special Forces hazard pay for commando or airborne units."),
        Triple("FD", earnings.fieldAllowance, "Field Area Allowance for deployment in active operational zones.")
    ).filter { it.second > 0.0 }
}

private fun GetDebitsList(payslip: ParsedPayslip): List<Triple<String, Double, String>> {
    val deductions = payslip.deductions
    return listOf(
        Triple("DSOP", deductions.dsopSubscription, "Defence Services Officers Provident Fund. Tax-free retirement fund compound savings."),
        Triple("AGIF", deductions.agif, "Army Group Insurance Fund. Mandatory life cover and survival benefits contribution."),
        Triple("ITAX", deductions.incomeTax, "Income Tax deducted at source based on annual projections."),
        Triple("EHCESS", deductions.educationCess, "Health & Education Cess (4% of primary Income Tax amount)."),
        Triple("LF", deductions.licenseFee, "License Fee charged for occupying government married/single quarters."),
        Triple("FUR", deductions.furnitureRent, "Furniture Rent for government-provided appliances and items in quarters."),
        Triple("WATER", deductions.waterCharges, "Water supply charges for occupied quarters."),
        Triple("Elec", deductions.electricityCharges, "Electricity charges consumed in quarters."),
        Triple("Barrack Damage", deductions.barrackDamage, "Recoveries for damages or missing furniture items in quarters.")
    ).filter { it.second > 0.0 }
}
