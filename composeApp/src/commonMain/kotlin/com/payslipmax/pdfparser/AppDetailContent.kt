package com.payslipmax.pdfparser

import androidx.compose.runtime.Composable
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.screens.guide.GuideCardRoute
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState

/** Shows the Due list from the Dashboard's card: the Guide stack is set to Part II's Due and the Guide tab is brought up. A no-op without the Guide. */
internal fun openDue(
    guideNavState: GuideNavState?,
    onNavigate: (Screen) -> Unit,
) {
    guideNavState ?: return
    guideNavState.openPart2Due()
    onNavigate(Screen.Guide)
}

/** The pushed detail screens, split out of `App.kt` (which sat 3 lines under the 300-line limit) with no change to what any of them does. */
@Composable
internal fun DetailContent(
    detail: Screen,
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
    onNavigateTo: (Screen) -> Unit,
) {
    when (detail) {
        Screen.PremiumFeatures ->
            com.payslipmax.pdfparser.ui.screens.PremiumFeaturesScreen(viewModel = viewModel, onNavigateTo = onNavigateTo, onBack = onBack)
        Screen.Representation ->
            com.payslipmax.pdfparser.ui.screens.RepresentationScreen(viewModel = viewModel, onBack = onBack)
        Screen.TaxPlanning ->
            com.payslipmax.pdfparser.ui.screens.TaxPlanningScreen(viewModel = viewModel, onBack = onBack)
        Screen.RetirementPlanning ->
            com.payslipmax.pdfparser.ui.screens.RetirementPlanningScreen(viewModel = viewModel, onBack = onBack)
        Screen.RetirementCalculators ->
            com.payslipmax.pdfparser.ui.screens.RetirementCalculatorsScreen(viewModel = viewModel, onBack = onBack)
        Screen.FAQ ->
            com.payslipmax.pdfparser.ui.screens.HelpLegalScreen(screen = Screen.FAQ, onBack = onBack)
        Screen.PrivacyPolicy ->
            com.payslipmax.pdfparser.ui.screens.HelpLegalScreen(screen = Screen.PrivacyPolicy, onBack = onBack)
        Screen.PayslipReplica ->
            com.payslipmax.pdfparser.ui.screens.PayslipReplicaDetailScreen(
                viewModel = viewModel,
                onBack = onBack,
                onOpenPdf = onOpenPdf,
                onNavigateTo = onNavigateTo,
            )
        Screen.HelpLegal ->
            com.payslipmax.pdfparser.ui.screens.HelpLegalScreen(screen = Screen.HelpLegal, onBack = onBack)
        Screen.PayAudit ->
            com.payslipmax.pdfparser.ui.screens.PayAuditScreen(viewModel = viewModel, onBack = onBack, onNavigateTo = onNavigateTo)
        Screen.GuideCard -> GuideCardRoute(viewModel = viewModel, onBack = onBack)
        // Tab roots are structurally unreachable here: onNavigate() routes them via switchTab(),
        // never push(), and AppNavStateSaver.restore() filters activeDetail to !isTabRoot. Handled
        // only so this `when` stays exhaustive against future Screen cases.
        Screen.Dashboard, Screen.History, Screen.Insights, Screen.Settings, Screen.Guide ->
            com.payslipmax.pdfparser.ui.screens.HelpLegalScreen(screen = Screen.HelpLegal, onBack = onBack)
    }
}
