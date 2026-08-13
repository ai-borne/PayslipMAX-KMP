package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the pure Premium Features catalog SSOT. The `FeatureGate.values()` loop is the key intent: a new
 * gate that ships without catalog metadata (or without a deliberate coming-soon/availability decision)
 * must fail here rather than silently vanishing from the catalog screen.
 */
class PremiumFeaturesCatalogTest {
    @Test
    fun everyFeatureGateHasExactlyOneCatalogEntry() {
        val catalog = premiumFeatureCatalog()
        assertEquals(FeatureGate.values().size, catalog.size, "catalog must cover every FeatureGate")
        assertEquals(FeatureGate.values().toList(), catalog.map { it.gate }, "catalog order follows FeatureGate order")
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
