package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
