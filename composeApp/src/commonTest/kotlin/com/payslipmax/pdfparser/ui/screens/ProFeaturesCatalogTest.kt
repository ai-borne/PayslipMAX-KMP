package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.subscription.FeatureGate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Guards the pure PRO Features catalog SSOT. The `FeatureGate.values()` loop is the key intent: a new
 * gate that ships without catalog metadata (or without a deliberate coming-soon/availability decision)
 * must fail here rather than silently vanishing from the catalog screen.
 */
class ProFeaturesCatalogTest {
    @Test
    fun everyFeatureGateHasExactlyOneCatalogEntry() {
        val catalog = proFeatureCatalog()
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
        for (meta in proFeatureCatalog()) {
            val copy = "${meta.title} ${meta.description}"
            assertTrue('₹' !in copy, "${meta.gate} copy must not contain a price")
        }
    }

    @Test
    fun claimGeneratorIsAvailableAndOpensRepresentation() {
        // Phase 4d: the PDF claim generator shipped, so its catalog row is live and navigable.
        val meta = featureMeta(FeatureGate.CLAIM_GENERATOR)
        assertEquals(ProFeatureAvailability.AVAILABLE, meta.availability)
        assertEquals(Screen.Representation, meta.target)
    }

    @Test
    fun availableGatesWithDedicatedScreensPointAtThem() {
        assertEquals(Screen.TaxPlanning, featureMeta(FeatureGate.TAX_PLANNER).target)
        assertEquals(Screen.RetirementPlanning, featureMeta(FeatureGate.DSOP_SIMULATOR).target)
        assertEquals(Screen.Representation, featureMeta(FeatureGate.CLAIM_GENERATOR).target)
    }

    @Test
    fun comingSoonEntriesNeverCarryANavigationTarget() {
        for (meta in proFeatureCatalog()) {
            if (meta.availability == ProFeatureAvailability.COMING_SOON) {
                assertNull(meta.target, "${meta.gate} is coming-soon and must not be navigable")
            }
        }
    }
}
