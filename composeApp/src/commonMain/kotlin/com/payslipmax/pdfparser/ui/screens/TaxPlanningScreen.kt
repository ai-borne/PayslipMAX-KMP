package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.insights.ConversationalTaxNarrativeEngine
import com.payslipmax.pdfparser.insights.OptimizationResult
import com.payslipmax.pdfparser.insights.TaxRulesUnavailableInfo
import com.payslipmax.pdfparser.tax.TaxRuleKnowledgeBase
import com.payslipmax.pdfparser.tax.TaxRuleResolution
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner

@Composable
fun TaxPlanningScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState.collectAsState().value
    TaxPlanningContentScreen(
        optimizationResult = uiState.taxOptimizationResult,
        activePayslipLabel = uiState.selectedPayslip?.let { "${it.monthName} ${it.year}" },
        onNavigateBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun TaxPlanningContentScreen(
    optimizationResult: OptimizationResult?,
    onNavigateBack: () -> Unit,
    activePayslipLabel: String? = null,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .detailScreenSafeArea()
                .padding(AppDimensions.PaddingMedium)
                .then(if (optimizationResult != null) Modifier.verticalScroll(scrollState) else Modifier),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        ScreenBackHeader(title = AppStringsPremium.taxPlanningTitle, subtitle = AppStringsPremium.taxPlanningSubtitle, onBack = onNavigateBack)
        if (optimizationResult == null) {
            TaxPlanningEmptyState()
        } else {
            activePayslipLabel?.let { TaxDataAsOfBanner(activePayslipLabel = it) }
            TaxPlanningContent(optimizationResult = optimizationResult)
        }
    }
}

@Composable
private fun TaxPlanningContent(
    optimizationResult: OptimizationResult,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
        TaxPlanningVerdictSection(optimizationResult)
        TaxPlanningDetailSection(optimizationResult)
    }
}

/** Answers the screen's opening questions: coverage, verdict, PCDA's own figures, and the reconciliation. */
@Composable
private fun TaxPlanningVerdictSection(
    optimizationResult: OptimizationResult,
) {
    // ADR-2 (Phase 7): the active FY has no resolvable rule pack -- degrade visibly instead of
    // silently rendering regime/liability cards computed off the nearest known FY's numbers.
    optimizationResult.taxRulesUnavailable?.let { unavailable ->
        TaxRulesUnavailableBanner(info = unavailable)
        return
    }

    optimizationResult.fySummary?.let { fySummary ->
        TaxFyRunwayHeaderCard(fySummary = fySummary)
    }

    TaxPlanningBlufSummary(optimizationResult)

    // D15: the deleted Peer Benchmark card is replaced by a verdict hero -- liability, winning
    // regime, and next month's TDS/net pay, the three things this screen owes an opening answer to.
    optimizationResult.regimeComparison?.let { comp ->
        TaxLiabilityVerdictHeroCard(
            regimeComparison = comp,
            tdsRunway = optimizationResult.tdsRunway,
            projectedNextMonthNetPay = optimizationResult.projectedNextMonthNetPay,
            parsedMonthCount = optimizationResult.fySummary?.parsedMonthCount ?: 1,
        )
    }

    // ADR-3/ADR-4 (Phase 5 domain, wired into UI here): TDS-vs-liability reconciliation and its
    // corollaries -- null only when PCDA's own totalTaxPayable is unavailable (D3 guard).
    optimizationResult.taxTrackReconciliation?.let { reconciliation ->
        TaxReconciliationDeltaCard(
            reconciliation = reconciliation,
            belatedReturnTrapWarning = optimizationResult.belatedReturnTrapWarning,
            midYearRegimeChange = optimizationResult.midYearRegimeChange,
            arrearsTransparency = optimizationResult.arrearsTransparency,
        )
    }

    TaxPlanningFullCalculationSection(optimizationResult)
}

/** U3 (Phase 8): the one plain-language sentence + no-action/flag line a reader owes an answer to
 * before any card requires them to synthesize it out of several numbers themselves. */
@Composable
private fun TaxPlanningBlufSummary(
    optimizationResult: OptimizationResult,
) {
    optimizationResult.regimeComparison?.let { comp ->
        TaxBlufSummaryCard(
            bluf =
                ConversationalTaxNarrativeEngine.buildBluf(
                    regimeComparison = comp,
                    reconciliation = optimizationResult.taxTrackReconciliation,
                    dsopWasteInsight = optimizationResult.dsopWasteInsight,
                    midYearRegimeChange = optimizationResult.midYearRegimeChange,
                    parsedMonthCount = optimizationResult.fySummary?.parsedMonthCount ?: 1,
                ),
        )
    }
}

/**
 * U3 (Phase 8): one single disclosure for everything a reader only needs "if you want to verify the
 * math" -- PCDA's own page-4 figures, the month-by-month breakdown, and the full Old-vs-New
 * comparison. Deliberately one combined section rather than three separate chevrons (which cluttered
 * the screen and buried the BLUF summary under repeated disclosure affordances); each card's content
 * is unchanged, only where it lives moved from always-visible to one tap away.
 */
@Composable
private fun TaxPlanningFullCalculationSection(
    optimizationResult: OptimizationResult,
) {
    val hasDetail =
        optimizationResult.pcdaOfficialComputation != null ||
            optimizationResult.storyNarrative != null ||
            optimizationResult.regimeComparison != null
    if (!hasDetail) return

    ExpandableDetailSection(
        title = AppStringsTaxPlanner.expandFullCalculationLabel,
        subtitle = AppStringsTaxPlanner.expandFullCalculationSubtitle,
    ) {
        optimizationResult.pcdaOfficialComputation?.let { pcda ->
            TaxPcdaOfficialComputationCard(taxAndSavings = pcda)
        }
        optimizationResult.storyNarrative?.let { narrative ->
            TaxNarrativeLedgerCard(narrative = narrative)
        }
        optimizationResult.regimeComparison?.let { comp ->
            TaxRegimeBattleHeroCard(comparison = comp)
        }
    }
}

/** The supporting detail: month-by-month audit, regime breakdown, runway, deductions, and actions. */
@Composable
private fun TaxPlanningDetailSection(
    optimizationResult: OptimizationResult,
) {
    optimizationResult.tdsRunway?.let { tds ->
        TaxTdsRunwayProgressCard(tdsRunway = tds)
    }

    optimizationResult.exemptionBreakdown?.let { exemptions ->
        TaxExemptionBreakdownCard(exemptionBreakdown = exemptions)
    }

    optimizationResult.dsopWasteInsight?.let { dsopWaste ->
        TaxDsopWasteInsightCard(insight = dsopWaste)
    }

    TaxActionableChecklistCard(opportunities = optimizationResult.opportunities)

    if (optimizationResult.opportunities.isNotEmpty()) {
        TaxAdviceDisclaimer()
    }

    TaxEducativeTipsCard(activeRegime = optimizationResult.regimeAssumed)

    TaxRuleVersionFooter(financialYear = optimizationResult.fySummary?.financialYear ?: "2026-27")
}

/** ADR-2 (Phase 7): shown at the top of the screen instead of every regime/liability card when the
 * active FY has no resolvable rule pack -- never a silently-wrong number for the wrong year. */
@Composable
private fun TaxRulesUnavailableBanner(
    info: TaxRulesUnavailableInfo,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = AppStringsTaxPlanner.rulesUnavailableTitle + info.requestedFy,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text =
                    AppStringsTaxPlanner.rulesUnavailableBodyPrefix + info.nearestKnownFy +
                        AppStringsTaxPlanner.rulesUnavailableBodySuffix,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/** Phase 7: paid feature now recommends executable financial actions (regime switching) -- shown
 * only alongside an actual actionable opportunity, not on an empty checklist. */
@Composable
private fun TaxAdviceDisclaimer() {
    Text(
        text = AppStringsTaxPlanner.adviceDisclaimer,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.PaddingSmall),
    )
}

@Composable
private fun TaxRuleVersionFooter(
    financialYear: String,
) {
    val footerText =
        when (val resolution = TaxRuleKnowledgeBase.resolve(financialYear)) {
            is TaxRuleResolution.OutOfRange ->
                AppStringsTaxPlanner.rulesUnavailableTitle + resolution.requestedFy
            is TaxRuleResolution.Resolved -> {
                val rules = resolution.rules
                AppStringsTaxPlanner.ruleFooterPrefix + rules.financialYear +
                    AppStringsTaxPlanner.ruleFooterAySeparator + rules.assessmentYear +
                    AppStringsTaxPlanner.ruleFooterVersionSeparator + rules.version +
                    AppStringsTaxPlanner.ruleFooterVerifiedSeparator + rules.lastVerifiedDate +
                    AppStringsTaxPlanner.ruleFooterSuffix
            }
        }

    Text(
        text = footerText,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth().padding(top = AppDimensions.PaddingSmall),
    )
}

@Composable
private fun TaxPlanningEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = AppStringsPremium.taxPlanningNoProjections,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
