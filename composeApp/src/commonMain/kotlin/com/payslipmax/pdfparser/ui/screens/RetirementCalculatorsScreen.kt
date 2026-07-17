package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium

@Composable
fun RetirementCalculatorsScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selectedPayslip
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .detailScreenSafeArea()
                .padding(AppDimensions.PaddingMedium)
                .then(if (selected != null) Modifier.verticalScroll(scrollState) else Modifier),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        ScreenBackHeader(title = AppStringsPremium.retCalcTitle, subtitle = AppStringsPremium.retCalcSubtitle, onBack = onBack)
        if (selected == null) {
            RetCalcEmptyState()
        } else {
            RetirementCalculatorsBody(payslip = selected)
        }
    }
}

@Composable
private fun RetirementCalculatorsBody(payslip: ParsedPayslip) {
    val basic = payslip.earnings.basicPay
    val msp = payslip.earnings.militaryServicePay
    val da = payslip.earnings.dearnessAllowance

    var qualifyingYears by remember { mutableStateOf("") }
    var ageNextBirthday by remember { mutableStateOf("") }
    var leaveDays by remember { mutableStateOf("") }

    Text(
        text = AppStringsPremium.retCalcBasisLabel,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    RetCalcInputsSection(
        qualifyingYears = qualifyingYears,
        onQualifyingYearsChange = { qualifyingYears = it },
        ageNextBirthday = ageNextBirthday,
        onAgeNextBirthdayChange = { ageNextBirthday = it },
        leaveDays = leaveDays,
        onLeaveDaysChange = { leaveDays = it },
    )
    RetCalcResultsSection(
        basicPay = basic,
        militaryServicePay = msp,
        dearnessAllowance = da,
        qualifyingYears = qualifyingYears.toDoubleOrNull() ?: 0.0,
        ageNextBirthday = ageNextBirthday.toIntOrNull(),
        leaveDays = leaveDays.toIntOrNull() ?: 0,
    )
    Text(
        text = AppStringsPremium.retCalcDisclaimer,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RetCalcEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = AppStringsPremium.retCalcNoPayslip,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
