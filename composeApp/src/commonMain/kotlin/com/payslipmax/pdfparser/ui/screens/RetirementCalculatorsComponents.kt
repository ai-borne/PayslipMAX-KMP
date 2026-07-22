package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.payslipmax.pdfparser.insights.RetirementCalculatorEngine
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun RetCalcInputsSection(
    qualifyingYears: String,
    onQualifyingYearsChange: (String) -> Unit,
    ageNextBirthday: String,
    onAgeNextBirthdayChange: (String) -> Unit,
    leaveDays: String,
    onLeaveDaysChange: (String) -> Unit,
    dsopBalance: String = "",
    onDsopBalanceChange: (String) -> Unit = {},
) {
    Text(
        text = AppStringsPremium.retCalcInputsTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    LabeledNumberField(
        label = AppStringsPremium.retCalcQualifyingYearsLabel,
        value = qualifyingYears,
        onValueChange = onQualifyingYearsChange,
        supportingText = AppStringsPremium.retCalcQualifyingYearsHint,
    )
    LabeledNumberField(
        label = AppStringsPremium.retCalcAgeNextBirthdayLabel,
        value = ageNextBirthday,
        onValueChange = onAgeNextBirthdayChange,
        supportingText = AppStringsPremium.retCalcAgeHint,
    )
    LabeledNumberField(
        label = AppStringsPremium.retCalcLeaveDaysLabel,
        value = leaveDays,
        onValueChange = onLeaveDaysChange,
        supportingText = AppStringsPremium.retCalcLeaveHint,
    )
    if (dsopBalance.isNotEmpty()) {
        LabeledNumberField(
            label = "DSOP Closing Balance (₹)",
            value = dsopBalance,
            onValueChange = onDsopBalanceChange,
            supportingText = AppStringsPremium.retCalcDsopHint,
        )
    }
}

@Composable
private fun LabeledNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun RetCalcResultsSection(
    basicPay: Double,
    militaryServicePay: Double,
    dearnessAllowance: Double,
    qualifyingYears: Double,
    ageNextBirthday: Int?,
    leaveDays: Int,
) {
    val pension = RetirementCalculatorEngine.retiringPension(basicPay, militaryServicePay)
    val dynamicPensionNote = "50% of [Basic (₹${formatCurrency(basicPay)}) + MSP (₹${formatCurrency(militaryServicePay)})]"
    ResultCard(AppStringsPremium.retCalcPensionTitle, formatCurrency(pension), dynamicPensionNote)

    val gratuity = RetirementCalculatorEngine.retirementGratuity(basicPay, dearnessAllowance, qualifyingYears, militaryServicePay)
    ResultCard(AppStringsPremium.retCalcGratuityTitle, formatCurrency(gratuity), AppStringsPremium.retCalcGratuityNote)

    CommutationResultCard(pension = pension, ageNextBirthday = ageNextBirthday, basicPay = basicPay, dearnessAllowance = dearnessAllowance)

    val leave = RetirementCalculatorEngine.leaveEncashment(basicPay, dearnessAllowance, leaveDays)
    ResultCard(AppStringsPremium.retCalcLeaveTitle, formatCurrency(leave), AppStringsPremium.retCalcLeaveNote)
}

@Composable
private fun CommutationResultCard(
    pension: Double,
    ageNextBirthday: Int?,
    basicPay: Double = 0.0,
    dearnessAllowance: Double = 0.0,
) {
    val factor = ageNextBirthday?.let { RetirementCalculatorEngine.commutationFactor(it) }
    if (factor == null) {
        ResultCard(AppStringsPremium.retCalcCommutationTitle, "—", AppStringsPremium.retCalcCommutationNoFactor)
        return
    }
    val lumpSum = RetirementCalculatorEngine.commutedLumpSum(pension, RetirementCalculatorEngine.MAX_COMMUTE_FRACTION, factor)
    val residual = RetirementCalculatorEngine.residualPension(pension, RetirementCalculatorEngine.MAX_COMMUTE_FRACTION)
    val daPct = if (basicPay > 0.0) (dearnessAllowance / basicPay) * 100.0 else 50.0
    val netMonthlyWithDr = RetirementCalculatorEngine.calculateNetMonthlyPension(pension, 0.50, daPct)
    ResultCard(
        title = AppStringsPremium.retCalcCommutationTitle,
        value = formatCurrency(lumpSum),
        note = AppStringsPremium.retCalcCommutationResidualPrefix + formatCurrency(residual) + " · Net Monthly Payout (with DR): " + formatCurrency(netMonthlyWithDr),
    )
}

@Composable
private fun ResultCard(
    title: String,
    value: String,
    note: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
