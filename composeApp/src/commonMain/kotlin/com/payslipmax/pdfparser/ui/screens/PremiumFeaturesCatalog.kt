package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

/**
 * Whether a catalogued Premium feature is shippable today ([AVAILABLE]) or a roadmap placeholder
 * ([COMING_SOON]). Coming-soon rows are visible but never navigate and never open the upgrade sheet.
 * (No gate is coming-soon at present — `CLAIM_GENERATOR` went [AVAILABLE] in Phase 4d.)
 */
enum class PremiumFeatureAvailability { AVAILABLE, COMING_SOON }

/**
 * Display metadata for one [FeatureGate] row in the Premium Features catalog.
 *
 * [target] is the detail [Screen] an unlocked, available feature opens, or `null` when the feature
 * lives inside an existing tab (Insights/Settings) rather than a standalone screen — such rows show
 * an "Included" status when unlocked instead of an Open button.
 */
data class PremiumFeatureMeta(
    val gate: FeatureGate,
    val icon: String,
    val title: String,
    val description: String,
    val target: Screen?,
    val availability: PremiumFeatureAvailability,
)

/**
 * Pure, exhaustive [FeatureGate] → [PremiumFeatureMeta] mapping — the SSOT the catalog screen renders.
 * The `when` is exhaustive, so a newly added [FeatureGate] fails to compile until it is catalogued
 * here (stronger than the runtime `values()` loop the test also asserts). No prices appear anywhere
 * (D8); all copy is sourced from [AppStringsPremium]/[InsightsStrings].
 */
fun featureMeta(gate: FeatureGate): PremiumFeatureMeta =
    when (gate) {
        FeatureGate.PREMIUM_INTELLIGENCE ->
            PremiumFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.premiumCatalogPremiumIntelligenceIcon,
                title = AppStringsPremium.premiumCatalogPremiumIntelligenceTitle,
                description = AppStringsPremium.premiumCatalogPremiumIntelligenceDesc,
                target = null,
                availability = PremiumFeatureAvailability.AVAILABLE,
            )
        FeatureGate.WEALTH_OPTIMIZATION ->
            PremiumFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.premiumCatalogWealthIcon,
                title = AppStringsPremium.premiumCatalogWealthTitle,
                description = AppStringsPremium.premiumCatalogWealthDesc,
                target = null,
                availability = PremiumFeatureAvailability.AVAILABLE,
            )
        FeatureGate.TAX_PLANNER ->
            PremiumFeatureMeta(
                gate = gate,
                icon = InsightsStrings.premiumToolsTaxPlannerIcon,
                title = AppStringsPremium.premiumToolsTaxPlanner,
                description = InsightsStrings.premiumToolsTaxPlannerValueProp,
                target = Screen.TaxPlanning,
                availability = PremiumFeatureAvailability.AVAILABLE,
            )
        FeatureGate.DSOP_SIMULATOR ->
            PremiumFeatureMeta(
                gate = gate,
                icon = InsightsStrings.premiumToolsDsopIcon,
                title = AppStringsPremium.premiumToolsDsopSimulator,
                description = InsightsStrings.premiumToolsDsopValueProp,
                target = Screen.RetirementPlanning,
                availability = PremiumFeatureAvailability.AVAILABLE,
            )
        FeatureGate.ANOMALY_DETECTION ->
            PremiumFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.premiumCatalogAnomalyIcon,
                title = AppStringsPremium.premiumCatalogAnomalyTitle,
                description = AppStringsPremium.premiumCatalogAnomalyDesc,
                target = null,
                availability = PremiumFeatureAvailability.AVAILABLE,
            )
        FeatureGate.CLAIM_GENERATOR ->
            PremiumFeatureMeta(
                gate = gate,
                icon = InsightsStrings.premiumToolsDraftClaimsIcon,
                title = AppStringsPremium.premiumCatalogClaimTitle,
                description = AppStringsPremium.premiumCatalogClaimDesc,
                target = Screen.Representation,
                availability = PremiumFeatureAvailability.AVAILABLE,
            )
        FeatureGate.RETIREMENT_CALCULATORS ->
            PremiumFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.premiumCatalogRetCalcIcon,
                title = AppStringsPremium.premiumCatalogRetCalcTitle,
                description = AppStringsPremium.premiumCatalogRetCalcDesc,
                target = Screen.RetirementCalculators,
                availability = PremiumFeatureAvailability.AVAILABLE,
            )
        FeatureGate.BACKUP_RESTORE ->
            PremiumFeatureMeta(
                gate = gate,
                icon = AppStringsPremium.premiumCatalogBackupIcon,
                title = AppStringsPremium.premiumCatalogBackupTitle,
                description = AppStringsPremium.premiumCatalogBackupDesc,
                target = null,
                availability = PremiumFeatureAvailability.AVAILABLE,
            )
    }

/** The full catalog in [FeatureGate] declaration order — one row per gate, none forgotten. */
fun premiumFeatureCatalog(): List<PremiumFeatureMeta> = FeatureGate.values().map(::featureMeta)

/**
 * Reverse lookup of [premiumFeatureCatalog]: the [FeatureGate] protecting a dedicated screen, or `null` if
 * the screen isn't a catalog navigation target (free, or reached only from an in-tab card). The single
 * source every Screen-emitting UI model (Smart Insights, Recommended Actions) resolves its `gate`
 * against, so a producer can never hardcode a gate that drifts from what the catalog actually protects.
 */
fun gateForScreen(screen: Screen): FeatureGate? = premiumFeatureCatalog().firstOrNull { it.target == screen }?.gate

/**
 * The catalog subset with a standalone screen — the Insights tab's quick-access cards. Derived
 * (not hand-maintained) so a gate that ships a [PremiumFeatureMeta.target] can never be forgotten there.
 */
fun quickAccessTools(): List<PremiumFeatureMeta> =
    premiumFeatureCatalog().filter { it.target != null && it.availability == PremiumFeatureAvailability.AVAILABLE }

/**
 * The bundle bullets shown on the locked Premium hub card — every shippable feature, coming-soon rows
 * excluded so the hub never promises something not yet live. Derived (not hand-maintained) so a new
 * [FeatureGate] automatically appears here once catalogued, with no separate list to fall out of sync.
 */
fun premiumBundleHighlights(): List<PremiumFeatureMeta> =
    premiumFeatureCatalog().filter { it.availability == PremiumFeatureAvailability.AVAILABLE }
