package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/**
 * Whether a catalogued PRO feature is shippable today ([AVAILABLE]) or a roadmap placeholder
 * ([COMING_SOON]). Coming-soon rows are visible but never navigate and never open the upgrade sheet.
 * (No gate is coming-soon at present — `CLAIM_GENERATOR` went [AVAILABLE] in Phase 4d.)
 */
enum class ProFeatureAvailability { AVAILABLE, COMING_SOON }

/**
 * Display metadata for one [FeatureGate] row in the PRO Features catalog.
 *
 * [target] is the detail [Screen] an unlocked, available feature opens, or `null` when the feature
 * lives inside an existing tab (Insights/Settings) rather than a standalone screen — such rows show
 * an "Included" status when unlocked instead of an Open button.
 */
data class ProFeatureMeta(
    val gate: FeatureGate,
    val icon: String,
    val title: String,
    val description: String,
    val target: Screen?,
    val availability: ProFeatureAvailability,
)

/**
 * Pure, exhaustive [FeatureGate] → [ProFeatureMeta] mapping — the SSOT the catalog screen renders.
 * The `when` is exhaustive, so a newly added [FeatureGate] fails to compile until it is catalogued
 * here (stronger than the runtime `values()` loop the test also asserts). No prices appear anywhere
 * (D8); all copy is sourced from [AppStringsPremium]/[InsightsStrings].
 */
fun featureMeta(gate: FeatureGate): ProFeatureMeta =
    when (gate) {
        FeatureGate.PREMIUM_INTELLIGENCE ->
            ProFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.proCatalogPremiumIntelligenceIcon,
                title = AppStringsPremium.proCatalogPremiumIntelligenceTitle,
                description = AppStringsPremium.proCatalogPremiumIntelligenceDesc,
                target = null,
                availability = ProFeatureAvailability.AVAILABLE,
            )
        FeatureGate.WEALTH_OPTIMIZATION ->
            ProFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.proCatalogWealthIcon,
                title = AppStringsPremium.proCatalogWealthTitle,
                description = AppStringsPremium.proCatalogWealthDesc,
                target = null,
                availability = ProFeatureAvailability.AVAILABLE,
            )
        FeatureGate.AI_AUDIT ->
            ProFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.proCatalogAiAuditIcon,
                title = AppStringsPremium.proCatalogAiAuditTitle,
                description = AppStringsPremium.proCatalogAiAuditDesc,
                target = null,
                availability = ProFeatureAvailability.AVAILABLE,
            )
        FeatureGate.TAX_PLANNER ->
            ProFeatureMeta(
                gate = gate,
                icon = InsightsStrings.premiumToolsTaxPlannerIcon,
                title = AppStringsPremium.premiumToolsTaxPlanner,
                description = InsightsStrings.premiumToolsTaxPlannerValueProp,
                target = Screen.TaxPlanning,
                availability = ProFeatureAvailability.AVAILABLE,
            )
        FeatureGate.DSOP_SIMULATOR ->
            ProFeatureMeta(
                gate = gate,
                icon = InsightsStrings.premiumToolsDsopIcon,
                title = AppStringsPremium.premiumToolsDsopSimulator,
                description = InsightsStrings.premiumToolsDsopValueProp,
                target = Screen.RetirementPlanning,
                availability = ProFeatureAvailability.AVAILABLE,
            )
        FeatureGate.ANOMALY_DETECTION ->
            ProFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.proCatalogAnomalyIcon,
                title = AppStringsPremium.proCatalogAnomalyTitle,
                description = AppStringsPremium.proCatalogAnomalyDesc,
                target = null,
                availability = ProFeatureAvailability.AVAILABLE,
            )
        FeatureGate.CLAIM_GENERATOR ->
            ProFeatureMeta(
                gate = gate,
                icon = InsightsStrings.premiumToolsDraftClaimsIcon,
                title = AppStringsPremium.proCatalogClaimTitle,
                description = AppStringsPremium.proCatalogClaimDesc,
                target = Screen.Representation,
                availability = ProFeatureAvailability.AVAILABLE,
            )
        FeatureGate.RETIREMENT_CALCULATORS ->
            ProFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.proCatalogRetCalcIcon,
                title = AppStringsPremium.proCatalogRetCalcTitle,
                description = AppStringsPremium.proCatalogRetCalcDesc,
                target = Screen.RetirementCalculators,
                availability = ProFeatureAvailability.AVAILABLE,
            )
        FeatureGate.BACKUP_RESTORE ->
            ProFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.proCatalogBackupIcon,
                title = AppStringsPremium.proCatalogBackupTitle,
                description = AppStringsPremium.proCatalogBackupDesc,
                target = null,
                availability = ProFeatureAvailability.AVAILABLE,
            )
    }

/** The full catalog in [FeatureGate] declaration order — one row per gate, none forgotten. */
fun proFeatureCatalog(): List<ProFeatureMeta> = FeatureGate.values().map(::featureMeta)

/**
 * Reverse lookup of [proFeatureCatalog]: the [FeatureGate] protecting a dedicated screen, or `null` if
 * the screen isn't a catalog navigation target (free, or reached only from an in-tab card). The single
 * source every Screen-emitting UI model (Smart Insights, Recommended Actions) resolves its `gate`
 * against, so a producer can never hardcode a gate that drifts from what the catalog actually protects.
 */
fun gateForScreen(screen: Screen): FeatureGate? = proFeatureCatalog().firstOrNull { it.target == screen }?.gate

/**
 * The catalog subset with a standalone screen — the Insights tab's quick-access cards. Derived
 * (not hand-maintained) so a gate that ships a [ProFeatureMeta.target] can never be forgotten there.
 */
fun quickAccessTools(): List<ProFeatureMeta> =
    proFeatureCatalog().filter { it.target != null && it.availability == ProFeatureAvailability.AVAILABLE }
