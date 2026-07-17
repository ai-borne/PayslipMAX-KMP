package com.payslipmax.pdfparser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.RedactionSanitizer
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.TransparencyStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransparencyDialog(
    payslip: ParsedPayslip,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isVerified by remember { mutableStateOf(false) }
    val redacted = remember(payslip) { RedactionSanitizer.redact(payslip) }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.fillMaxWidth(0.95f),
    ) {
        Surface(
            shape = CardDefaults.shape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = AppDimensions.DialogElevation,
        ) {
            TransparencyDialogContent(
                raw = payslip,
                sanitized = redacted,
                isVerified = isVerified,
                onVerificationChange = { isVerified = it },
                onConfirm = onConfirm,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
private fun TransparencyDialogContent(
    raw: ParsedPayslip,
    sanitized: ParsedPayslip,
    isVerified: Boolean,
    onVerificationChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(AppDimensions.PaddingMedium)
                .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        Text(
            text = TransparencyStrings.transparencyTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = TransparencyStrings.transparencyMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TransparencyComparisonPanels(
            raw = raw,
            sanitized = sanitized,
            modifier =
                Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = AppDimensions.DialogMaxHeight),
        )

        TransparencyActionFooter(
            isVerified = isVerified,
            onVerificationChange = onVerificationChange,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun TransparencyComparisonPanels(
    raw: ParsedPayslip,
    sanitized: ParsedPayslip,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    AppDimensions.BorderThin,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    CardDefaults.shape,
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CardDefaults.shape)
                .padding(AppDimensions.PaddingSmall)
                .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            TransparencyDataColumn(
                title = TransparencyStrings.transparencyLabelRaw,
                titleColor = MaterialTheme.colorScheme.error,
                payslip = raw,
                isSanitized = false,
                modifier = Modifier.weight(1f),
            )
            TransparencyDataColumn(
                title = TransparencyStrings.transparencyLabelSanitized,
                titleColor = MaterialTheme.colorScheme.primary,
                payslip = sanitized,
                isSanitized = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TransparencyDataColumn(
    title: String,
    titleColor: Color,
    payslip: ParsedPayslip,
    isSanitized: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = titleColor,
        )
        Text(
            text = formatPayslipText(payslip, isSanitized),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

internal fun formatPayslipText(
    payslip: ParsedPayslip,
    isSanitized: Boolean,
): String {
    val sb = StringBuilder()

    // PII / Meta Section
    val nameVal = if (isSanitized) TransparencyStrings.labelAnonymous else payslip.officer.name
    sb.append("${TransparencyStrings.labelPerson}$nameVal\n")
    sb.append("${TransparencyStrings.labelPanNumber}${payslip.officer.pan}\n")
    sb.append("${TransparencyStrings.labelCdaAccount}${payslip.officer.accountNo}\n")
    sb.append("${TransparencyStrings.labelFilename}${payslip.file}\n\n")

    // Earnings Section
    val e = payslip.earnings
    if (e.basicPay > 0) sb.append("${TransparencyStrings.labelBasicPay}₹${e.basicPay.toInt()}\n")
    if (e.dearnessAllowance > 0) sb.append("${TransparencyStrings.labelDA}₹${e.dearnessAllowance.toInt()}\n")
    if (e.militaryServicePay > 0) sb.append("${TransparencyStrings.labelMSP}₹${e.militaryServicePay.toInt()}\n")
    if (e.transportAllowance > 0) sb.append("${TransparencyStrings.labelTPTA}₹${e.transportAllowance.toInt()}\n")
    if (e.houseRentAllowance > 0) sb.append("${TransparencyStrings.labelHRA}₹${e.houseRentAllowance.toInt()}\n")

    // Other non-zero earnings
    if (e.rationMoney > 0) sb.append("${TransparencyStrings.labelRationMoney}₹${e.rationMoney.toInt()}\n")
    if (e.specialForcesPay > 0) sb.append("${TransparencyStrings.labelSpecialForces}₹${e.specialForcesPay.toInt()}\n")
    if (e.fieldAllowance > 0) sb.append("${TransparencyStrings.labelFieldAllce}₹${e.fieldAllowance.toInt()}\n")
    if (e.riskHardshipAllowance > 0) sb.append("${TransparencyStrings.labelRiskHardship}₹${e.riskHardshipAllowance.toInt()}\n")
    if (e.childrenEducationAllowance > 0) sb.append("${TransparencyStrings.labelCEA}₹${e.childrenEducationAllowance.toInt()}\n")
    if (e.nonPracticingAllowance > 0) sb.append("${TransparencyStrings.labelNPA}₹${e.nonPracticingAllowance.toInt()}\n")
    if (e.medicalAllowance > 0) sb.append("${TransparencyStrings.labelMedicalAllce}₹${e.medicalAllowance.toInt()}\n")
    if (e.miscEarnings > 0) sb.append("${TransparencyStrings.labelMiscEarn}₹${e.miscEarnings.toInt()}\n")

    sb.append("---\n")
    sb.append("${TransparencyStrings.labelCredits}₹${payslip.summary.grossPay.toInt()}\n\n")

    // Deductions Section
    val d = payslip.deductions
    if (d.dsopSubscription > 0) sb.append("${TransparencyStrings.labelDSOP}₹${d.dsopSubscription.toInt()}\n")
    if (d.incomeTax > 0) sb.append("${TransparencyStrings.labelITax}₹${d.incomeTax.toInt()}\n")
    if (d.agif > 0) sb.append("${TransparencyStrings.labelAGIF}₹${d.agif.toInt()}\n")
    if (d.educationCess > 0) sb.append("${TransparencyStrings.labelEduCess}₹${d.educationCess.toInt()}\n")
    if (d.licenseFee > 0) sb.append("${TransparencyStrings.labelLicenceFee}₹${d.licenseFee.toInt()}\n")
    if (d.furnitureRent > 0) sb.append("${TransparencyStrings.labelFurnitureRent}₹${d.furnitureRent.toInt()}\n")
    if (d.waterCharges > 0) sb.append("${TransparencyStrings.labelWaterCharges}₹${d.waterCharges.toInt()}\n")
    if (d.electricityCharges > 0) sb.append("${TransparencyStrings.labelElectricity}₹${d.electricityCharges.toInt()}\n")
    if (d.barrackDamage > 0) sb.append("${TransparencyStrings.labelBarrackDamage}₹${d.barrackDamage.toInt()}\n")
    if (d.aobf > 0) sb.append("${TransparencyStrings.labelAOBF}₹${d.aobf.toInt()}\n")
    if (d.agifLoanRecovery > 0) sb.append("${TransparencyStrings.labelAgifLoanRec}₹${d.agifLoanRecovery.toInt()}\n")
    if (d.miscDeductions > 0) sb.append("${TransparencyStrings.labelMiscDeduct}₹${d.miscDeductions.toInt()}\n")

    sb.append("---\n")
    sb.append("${TransparencyStrings.labelDebits}₹${payslip.summary.totalDeductions.toInt()}\n\n")

    // Net and Balances Section
    sb.append("${TransparencyStrings.labelNetRemittance}₹${payslip.summary.netRemittance.toInt()}\n")
    val dsopBal = payslip.taxAndSavings?.dsopFund?.closingBalance ?: 0.0
    if (dsopBal > 0) {
        sb.append("${TransparencyStrings.labelDsopBalance}₹${dsopBal.toInt()}\n")
    } else {
        val ledgerClosing = payslip.ledgerBalances.closingCreditBalance
        if (ledgerClosing > 0) {
            sb.append("${TransparencyStrings.labelDsopBalance}₹${ledgerClosing.toInt()}\n")
        }
    }

    return sb.toString()
}

@Composable
private fun TransparencyActionFooter(
    isVerified: Boolean,
    onVerificationChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = isVerified,
                onCheckedChange = onVerificationChange,
            )
            Spacer(modifier = Modifier.width(AppDimensions.SpacingSmall))
            Text(
                text = TransparencyStrings.transparencyDisclaimer,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.btnCancel)
            }
            Spacer(modifier = Modifier.width(AppDimensions.SpacingMedium))
            Button(
                onClick = onConfirm,
                enabled = isVerified,
            ) {
                Text(TransparencyStrings.transparencyActionApprove)
            }
        }
    }
}
