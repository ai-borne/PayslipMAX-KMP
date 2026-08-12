package com.payslipmax.pdfparser.tax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** ADR-2: unknown FYs must resolve to an explicit [TaxRuleResolution.OutOfRange], never silent rules. */
class TaxRuleLookupTest {
    @Test
    fun knownFyResolves() {
        val resolution = TaxRuleKnowledgeBase.resolve("2025-26")
        val resolved = assertIs<TaxRuleResolution.Resolved>(resolution)
        assertEquals("2025-26", resolved.rules.financialYear)
    }

    @Test
    fun earliestKnownFyResolves() {
        val resolution = TaxRuleKnowledgeBase.resolve("2015-16")
        val resolved = assertIs<TaxRuleResolution.Resolved>(resolution)
        assertEquals("2015-16", resolved.rules.financialYear)
    }

    @Test
    fun unknownFyNeverSilentlyResolves() {
        val resolution = TaxRuleKnowledgeBase.resolve("1999-2000")
        val outOfRange = assertIs<TaxRuleResolution.OutOfRange>(resolution)
        assertEquals("1999-2000", outOfRange.requestedFy)
        assertEquals("2015-16", outOfRange.nearestKnownFy)
    }

    @Test
    fun farFutureFyNeverSilentlyResolves() {
        val resolution = TaxRuleKnowledgeBase.resolve("2099-100")
        val outOfRange = assertIs<TaxRuleResolution.OutOfRange>(resolution)
        assertEquals("2026-27", outOfRange.nearestKnownFy)
    }

    @Test
    fun legacyAccessorFallsBackToNearestKnownFyInsteadOfCrashing() {
        val rules = TaxRuleKnowledgeBase.getRulesForFy("1999-2000")
        assertEquals("2015-16", rules.financialYear)
    }
}
