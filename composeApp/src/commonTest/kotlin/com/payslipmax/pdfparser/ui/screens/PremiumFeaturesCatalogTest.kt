package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.subscription.LaunchFlags
import com.payslipmax.pdfparser.ui.screens.guide.part2.isPart2Enabled
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the pure Premium Features catalog SSOT. The `FeatureGate.values()` loop is the key intent: a new
 * gate that ships without catalog metadata (or without a deliberate coming-soon/availability decision)
 * must fail here rather than silently vanishing from the catalog screen.
 */
class PremiumFeaturesCatalogTest {
    @Test
    fun everyAdvertisedFeatureGateHasExactlyOneCatalogEntry() {
        val catalog = premiumFeatureCatalog()
        val advertised = FeatureGate.values().filter { isAdvertised(it) }
        assertEquals(advertised, catalog.map { it.gate }, "catalog order follows FeatureGate order, one row per advertised gate")
        assertEquals(advertised.size, catalog.size)
        val dark = if (isPart2Enabled()) 0 else 1
        assertEquals(FeatureGate.values().size - dark, catalog.size, "the paywall is on (2026-10-18), so every gate but a dark Part II is advertised")
    }

    @Test
    fun partTwoOrdersIsCataloguedButAdvertisedOnlyWhilePartTwoIsEnabled() {
        // Dark launch: a gate that is catalogued but not for sale must stay off the paywall, or the hub promises what is not built.
        assertFalse(isAdvertised(FeatureGate.PART2_ORDERS, part2Enabled = false))
        assertTrue(isAdvertised(FeatureGate.PART2_ORDERS, part2Enabled = true))
        assertEquals(FeatureGate.PART2_ORDERS, featureMeta(FeatureGate.PART2_ORDERS).gate)
        assertEquals(isPart2Enabled(), FeatureGate.PART2_ORDERS in premiumFeatureCatalog().map { it.gate })
        assertEquals(LaunchFlags.GUIDE_ENABLED && LaunchFlags.PART2_ENABLED, isAdvertised(FeatureGate.PART2_ORDERS))
    }

    @Test
    fun withPartTwoOffThePaywallCatalogIsExactlyWhatItWasBeforePartTwo() {
        // why: a flag-off build must behave exactly as before; this list is the catalog as shipped in 1.3.0, in order.
        val before =
            listOf(
                FeatureGate.PREMIUM_INTELLIGENCE, FeatureGate.WEALTH_OPTIMIZATION, FeatureGate.TAX_PLANNER, FeatureGate.DSOP_SIMULATOR,
                FeatureGate.ANOMALY_DETECTION, FeatureGate.CLAIM_GENERATOR, FeatureGate.RETIREMENT_CALCULATORS, FeatureGate.BACKUP_RESTORE,
                FeatureGate.CLAIM_GUIDE,
            )
        val catalog = FeatureGate.values().filter { isAdvertised(it, part2Enabled = false) }
        assertEquals(before, catalog)
    }

    @Test
    fun theClaimGuideIsListedAsPremiumOnlyOnceItExistsAndItsPaywallIsOn() {
        // Dark or free, the Guide is not for sale, so the Premium hub must not promise it (the paywall is owner-gated).
        assertFalse(isAdvertised(FeatureGate.CLAIM_GUIDE, guideEnabled = false, guidePaywallEnabled = true))
        assertFalse(isAdvertised(FeatureGate.CLAIM_GUIDE, guideEnabled = true, guidePaywallEnabled = false))
        assertTrue(isAdvertised(FeatureGate.CLAIM_GUIDE, guideEnabled = true, guidePaywallEnabled = true))
        for (gate in FeatureGate.values().filter { it != FeatureGate.CLAIM_GUIDE && it != FeatureGate.PART2_ORDERS }) {
            assertTrue(isAdvertised(gate, guideEnabled = false, guidePaywallEnabled = false), "$gate is unaffected by the Guide flags")
        }
    }

    @Test
    fun everyEntryHasNonBlankCopyAndMatchingGate() {
        for (gate in FeatureGate.values()) {
            val meta = featureMeta(gate)
            assertEquals(gate, meta.gate)
            assertTrue(meta.icon.isNotBlank(), "$gate icon must be set")
            assertTrue(meta.title.isNotBlank(), "$gate title must be set")
            assertTrue(meta.description.isNotBlank(), "$gate description must be set")
        }
    }

    @Test
    fun noCatalogCopyLeaksAPrice() {
        // D8: benefit-only copy until billing lands — no ₹ / "rs" pricing in any catalog string.
        for (meta in premiumFeatureCatalog()) {
            val copy = "${meta.title} ${meta.description}"
            assertTrue('₹' !in copy, "${meta.gate} copy must not contain a price")
        }
    }

    @Test
    fun claimGeneratorIsAvailableAndOpensRepresentation() {
        // Phase 4d: the PDF claim generator shipped, so its catalog row is live and navigable.
        val meta = featureMeta(FeatureGate.CLAIM_GENERATOR)
        assertEquals(PremiumFeatureAvailability.AVAILABLE, meta.availability)
        assertEquals(Screen.Representation, meta.target)
    }

    @Test
    fun backupRestoreIsAvailableAndCataloguedAsInTabFeature() {
        // FeatureGate.BACKUP_RESTORE is catalogued as an available feature operating in-settings (target == null)
        val meta = featureMeta(FeatureGate.BACKUP_RESTORE)
        assertEquals(PremiumFeatureAvailability.AVAILABLE, meta.availability)
        assertNull(meta.target)
        assertTrue(meta.icon.isNotBlank())
        assertTrue(meta.title.isNotBlank())
        assertTrue(meta.description.isNotBlank())
        assertTrue(premiumBundleHighlights().any { it.gate == FeatureGate.BACKUP_RESTORE })
    }

    @Test
    fun availableGatesWithDedicatedScreensPointAtThem() {
        assertEquals(Screen.TaxPlanning, featureMeta(FeatureGate.TAX_PLANNER).target)
        assertEquals(Screen.RetirementPlanning, featureMeta(FeatureGate.DSOP_SIMULATOR).target)
        assertEquals(Screen.Representation, featureMeta(FeatureGate.CLAIM_GENERATOR).target)
        assertEquals(Screen.RetirementCalculators, featureMeta(FeatureGate.RETIREMENT_CALCULATORS).target)
    }

    @Test
    fun comingSoonEntriesNeverCarryANavigationTarget() {
        for (meta in premiumFeatureCatalog()) {
            if (meta.availability == PremiumFeatureAvailability.COMING_SOON) {
                assertNull(meta.target, "${meta.gate} is coming-soon and must not be navigable")
            }
        }
    }

    @Test
    fun quickAccessToolsIncludesEveryAvailableGateWithADedicatedScreen() {
        // Regression guard: Insights' quick-access cards must derive from the catalog, not a hand-maintained
        // list — RETIREMENT_CALCULATORS shipped a dedicated screen but was forgotten from a hardcoded list.
        val quick = quickAccessTools().map { it.gate }.toSet()
        assertEquals(
            premiumFeatureCatalog()
                .filter { it.target != null && it.availability == PremiumFeatureAvailability.AVAILABLE }
                .map { it.gate }.toSet(),
            quick,
        )
        assertTrue(FeatureGate.TAX_PLANNER in quick)
        assertTrue(FeatureGate.DSOP_SIMULATOR in quick)
        assertTrue(FeatureGate.CLAIM_GENERATOR in quick)
        assertTrue(FeatureGate.RETIREMENT_CALCULATORS in quick)
    }

    @Test
    fun premiumBundleHighlightsIncludesEveryAvailableGateAndStaysInSyncWithTheCatalog() {
        // Regression guard: the locked Premium hub's bundle bullets must derive from the catalog, not a
        // hand-maintained list — a newly catalogued gate must appear here automatically.
        val bundle = premiumBundleHighlights()
        assertEquals(
            premiumFeatureCatalog().filter { it.availability == PremiumFeatureAvailability.AVAILABLE }.map { it.gate },
            bundle.map { it.gate },
        )
        // Unlike quickAccessTools, gates with no dedicated screen (e.g. the ANOMALY_DETECTION and
        // WEALTH_OPTIMIZATION in-tab features) still belong in the bundle pitch.
        assertTrue(bundle.any { it.target == null })
    }

    @Test
    fun premiumBundleHighlightsExcludesComingSoonEntries() {
        for (meta in premiumBundleHighlights()) {
            assertEquals(PremiumFeatureAvailability.AVAILABLE, meta.availability)
        }
    }
}
