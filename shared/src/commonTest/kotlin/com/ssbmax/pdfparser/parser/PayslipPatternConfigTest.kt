package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayslipPatternConfigTest {
    @Test
    fun blocklistContainsRemittanceVariations() {
        val blocklist = PayslipPatternConfig.blocklist
        assertTrue(blocklist.contains("remittance"), "blocklist should contain remittance")
        assertTrue(blocklist.contains("remitance"), "blocklist should contain single-T remitance")
        assertTrue(blocklist.contains("net remittance"), "blocklist should contain net remittance")
    }

    @Test
    fun creditKeysMappingContainsPunctuationAndSpellingVariations() {
        val mapping = PayslipPatternConfig.creditKeysMapping
        assertEquals("basicPay", mapping["B.PAY"])
        assertEquals("basicPay", mapping["B PAY"])
        assertEquals("basicPay", mapping["Gr. Pay"])
        assertEquals("dearnessAllowance", mapping["D.A"])
        assertEquals("dearnessAllowance", mapping["Dearness Allowance"])
        assertEquals("militaryServicePay", mapping["M.S.P"])
        assertEquals("militaryServicePay", mapping["Military Service Pay"])
    }

    @Test
    fun debitKeysMappingContainsPunctuationAndSpellingVariations() {
        val mapping = PayslipPatternConfig.debitKeysMapping
        assertEquals("agif", mapping["A.G.I.F."])
        assertEquals("agif", mapping["A.G.I.F"])
        assertEquals("agif", mapping["AGIF Subn"])
        assertEquals("dsopSubscription", mapping["D.S.O.P.F."])
        assertEquals("dsopSubscription", mapping["D.S.O.P.F"])
        assertEquals("dsopSubscription", mapping["DSOP Subn"])
        assertEquals("incomeTax", mapping["Income Tax"])
        assertEquals("incomeTax", mapping["Inc Tax"])
        assertEquals("incomeTax", mapping["I Tax"])
    }

    @Test
    fun mandatoryFieldsAreDefined() {
        assertTrue(PayslipPatternConfig.strictlyMandatoryCredits.contains("basicPay"))
        assertTrue(PayslipPatternConfig.strictlyMandatoryCredits.contains("dearnessAllowance"))
        assertTrue(PayslipPatternConfig.strictlyMandatoryCredits.contains("militaryServicePay"))
        assertTrue(PayslipPatternConfig.strictlyMandatoryDebits.contains("agif"))
        assertTrue(PayslipPatternConfig.strictlyMandatoryDebits.contains("dsopSubscription"))
    }
}
