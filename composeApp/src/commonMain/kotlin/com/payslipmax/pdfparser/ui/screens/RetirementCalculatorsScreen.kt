package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
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
import com.payslipmax.pdfparser.insights.RetirementCalculatorEngine
import com.payslipmax.pdfparser.insights.RetirementPlannerResult
import com.payslipmax.pdfparser.insights.RetirementPlannerResultBuilder
import com.payslipmax.pdfparser.insights.RetirementYearResolver
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
    val initialDsop = payslip.taxAndSavings?.dsopFund?.closingBalance ?: 0.0

    var qualifyingYears by remember(payslip) {
        mutableStateOf(RetirementYearResolver.DEFAULT_QUALIFYING_YEARS_FULL_PENSION.toInt().toString())
    }
    var ageNextBirthday by remember(payslip) {
        mutableStateOf(RetirementYearResolver.estimateAgeNextBirthday(basic, payslip.officer.name).toString())
    }
    var leaveDays by remember(payslip) {
        mutableStateOf(RetirementCalculatorEngine.LEAVE_MAX_DAYS.toString())
    }
    var dsopBalance by remember(payslip) {
        mutableStateOf(if (initialDsop > 0.0) initialDsop.toLong().toString() else "")
    }

    val result =
        remember(payslip, qualifyingYears, ageNextBirthday, leaveDays, dsopBalance) {
            RetirementPlannerResultBuilder.build(
                payslip = payslip,
                overrideQualifyingYears = qualifyingYears.toDoubleOrNull(),
                overrideAgeNextBirthday = ageNextBirthday.toIntOrNull(),
                overrideLeaveDays = leaveDays.toIntOrNull(),
                overrideDsopBalance = dsopBalance.toDoubleOrNull(),
            )
        }

    RetinementCalculatorsContent(
        qualifyingYears = qualifyingYears,
        onQualifyingYearsChange = { qualifyingYears = it },
        ageNextBirthday = ageNextBirthday,
        onAgeNextBirthdayChange = { ageNextBirthday = it },
        leaveDays = leaveDays,
        onLeaveDaysChange = { leaveDays = it },
        dsopBalance = dsopBalance,
        onDsopBalanceChange = { dsopBalance = it },
        result = result,
    )
}

@Composable
private fun RetinementCalculatorsContent(
    qualifyingYears: String,
    onQualifyingYearsChange: (String) -> Unit,
    ageNextBirthday: String,
    onAgeNextBirthdayChange: (String) -> Unit,
    leaveDays: String,
    onLeaveDaysChange: (String) -> Unit,
    dsopBalance: String,
    onDsopBalanceChange: (String) -> Unit,
    result: RetirementPlannerResult,
) {
    Text(
        text = AppStringsPremium.retCalcBasisLabel,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    RetCalcInputsSection(
        qualifyingYears = qualifyingYears,
        onQualifyingYearsChange = onQualifyingYearsChange,
        ageNextBirthday = ageNextBirthday,
        onAgeNextBirthdayChange = onAgeNextBirthdayChange,
        leaveDays = leaveDays,
        onLeaveDaysChange = onLeaveDaysChange,
        dsopBalance = dsopBalance,
        onDsopBalanceChange = onDsopBalanceChange,
    )
    RetirementHeroSummaryCard(result = result)
    CaTaxAuditCard(result = result)
    RetirementDay1CorpusCard(result = result)
    CommutationAdvisorCard(result = result)
    CommutationSimulatorCard(scenarios = result.commutationScenarios)
    PensionTaxabilityCard()
    SparshAndResettlementCard()
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
