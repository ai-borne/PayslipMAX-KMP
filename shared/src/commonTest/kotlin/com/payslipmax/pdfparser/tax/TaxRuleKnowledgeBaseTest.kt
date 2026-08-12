package com.payslipmax.pdfparser.tax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TaxRuleKnowledgeBaseTest {
    @Test
    fun testDefaultRuleResolutionForFy202526() {
        val rules = TaxRuleKnowledgeBase.getRulesForFy("2025-26")

        assertEquals("2025-26", rules.financialYear)
        assertEquals("2026-27", rules.assessmentYear)
        assertEquals(75000.0, rules.standardDeductionNew)
        assertEquals(50000.0, rules.standardDeductionOld)
        assertEquals(150000.0, rules.sec80CLimit)
        assertEquals(50000.0, rules.sec80CCD1BLimit)
        assertEquals(1200000.0, rules.sec87ARebateMaxIncomeNew)
        assertEquals(60000.0, rules.sec87ARebateCapNew)
        assertEquals(4.0, rules.cessRatePct)
        assertTrue(rules.newRegimeSlabs.isNotEmpty())
        assertTrue(rules.oldRegimeSlabs.isNotEmpty())
        assertTrue(rules.defenceSection10Rules.isNotEmpty())
    }

    @Test
    fun testSection10RuleLookup() {
        val rules = TaxRuleKnowledgeBase.getRulesForFy("2025-26")
        val highlyActiveRule = rules.defenceSection10Rules.find { it.allowanceKey == "HIGHLY_ACTIVE_FIELD" }

        assertNotNull(highlyActiveRule)
        assertEquals(4200.0, highlyActiveRule.monthlyExemptionLimit)
        assertEquals("Section 10(14)", highlyActiveRule.sectionRef)
    }

    @Test
    fun testFallbackForUnknownFy() {
        val rules = TaxRuleKnowledgeBase.getRulesForFy("2099-100")
        assertNotNull(rules)
        assertEquals(75000.0, rules.standardDeductionNew)
        assertTrue(rules.financialYear.isNotEmpty(), "Fallback rules should contain a non-empty financial year")
    }
}
