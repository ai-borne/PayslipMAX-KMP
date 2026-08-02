package com.payslipmax.pdfparser.tax

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PensionRuleKnowledgeBaseTest {
    @Test
    fun testCommutationFactorsCoverage() {
        // Test young officer PMR age (36)
        assertEquals(9.136, PensionRuleKnowledgeBase.getCommutationFactor(36))

        // Test standard officer retirement ages (54, 56, 58, 60)
        assertEquals(8.678, PensionRuleKnowledgeBase.getCommutationFactor(54))
        assertEquals(8.572, PensionRuleKnowledgeBase.getCommutationFactor(56))
        assertEquals(8.446, PensionRuleKnowledgeBase.getCommutationFactor(58))
        assertEquals(8.287, PensionRuleKnowledgeBase.getCommutationFactor(60))

        // Test boundary ages
        assertNotNull(PensionRuleKnowledgeBase.getCommutationFactor(20))
        assertNotNull(PensionRuleKnowledgeBase.getCommutationFactor(67))

        // Test out of bounds ages return null
        assertNull(PensionRuleKnowledgeBase.getCommutationFactor(19))
        assertNull(PensionRuleKnowledgeBase.getCommutationFactor(68))
    }

    @Test
    fun testStatutoryConstants() {
        assertEquals(2_500_000.0, PensionRuleKnowledgeBase.GRATUITY_CEILING)
        assertEquals(0.50, PensionRuleKnowledgeBase.MAX_COMMUTE_FRACTION_DEFENCE)
        assertEquals(300, PensionRuleKnowledgeBase.LEAVE_MAX_DAYS)
        assertEquals(0.30, PensionRuleKnowledgeBase.DISABILITY_100_PERCENT_RATE)
    }
}
