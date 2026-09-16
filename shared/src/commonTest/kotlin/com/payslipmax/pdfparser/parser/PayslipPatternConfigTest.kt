package com.payslipmax.pdfparser.parser

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

    @Test
    fun bpayVaryingLevelTagsClassifyToBasicPay() {
        val pairs =
            listOf(
                LabelAmount("BPAY (14)", 140000.0, 44f, 140f, 200f),
                LabelAmount("BPAY (12A)", 120000.0, 44f, 140f, 180f),
                LabelAmount("BPAY (15)", 150000.0, 44f, 140f, 160f),
                LabelAmount("BPAY (Level 10)", 100000.0, 44f, 140f, 140f),
            )
        val classified = TokenTableClassifier.classifyPairs(pairs)
        val bpayEntries = classified.entries.filter { it.standardKey == "basicPay" }
        assertEquals(4, bpayEntries.size, "All varying BPAY (X) level tags must cleanly classify to basicPay")
    }

    @Test
    fun allowancePatternMappingsPartitionsAreValidAndComposed() {
        val allCredits = AllowancePatternMappings.all
        assertEquals(allCredits, PayslipPatternConfig.creditKeysMapping)

        // Verify Core
        assertEquals("basicPay", AllowancePatternMappings.corePayMappings["BPAY"])
        assertEquals("dearnessAllowance", AllowancePatternMappings.corePayMappings["DA"])
        assertEquals("militaryServicePay", AllowancePatternMappings.corePayMappings["MSP"])

        // Verify Housing
        assertEquals("houseRentAllowance", AllowancePatternMappings.housingMappings["HRA"])
        assertEquals("houseRentAllowance", AllowancePatternMappings.housingMappings["HH11"])
        assertEquals("arrearsHra", AllowancePatternMappings.housingMappings["ARR-HRA"])

        // Verify Field & Risk
        assertEquals("fieldAllowance", AllowancePatternMappings.fieldAndRiskMappings["FD"])
        assertEquals("riskHardshipAllowance", AllowancePatternMappings.fieldAndRiskMappings["RH11"])
        assertEquals("arrearsRiskHardship", AllowancePatternMappings.fieldAndRiskMappings["ARR-RH11"])

        // Verify General Allowances
        assertEquals("transportAllowance", AllowancePatternMappings.generalAllowanceMappings["TPTA"])
        assertEquals("dressAllowance", AllowancePatternMappings.generalAllowanceMappings["DRESALW"])
        assertEquals("rationMoney", AllowancePatternMappings.generalAllowanceMappings["RSHNA"])

        // Verify Adjustments & Ledger
        assertEquals("adjBasicPay", AllowancePatternMappings.adjustmentAndLedgerMappings["A/o BPAY-"])
        assertEquals("openingCreditBalance", AllowancePatternMappings.adjustmentAndLedgerMappings["Op Cr Bal"])
    }

    @Test
    fun deductionPatternMappingsPartitionsAreValidAndComposed() {
        val allDebits = DeductionPatternMappings.all
        assertEquals(allDebits, PayslipPatternConfig.debitKeysMapping)

        // Verify Statutory
        assertEquals("dsopSubscription", DeductionPatternMappings.statutoryMappings["DSOPF"])
        assertEquals("agif", DeductionPatternMappings.statutoryMappings["AGIF"])
        assertEquals("incomeTax", DeductionPatternMappings.statutoryMappings["Income Tax"])
        assertEquals("educationCess", DeductionPatternMappings.statutoryMappings["Educ Cess"])

        // Verify Accommodation & Utilities
        assertEquals("licenseFee", DeductionPatternMappings.accommodationMappings["L Fee"])
        assertEquals("furnitureRent", DeductionPatternMappings.accommodationMappings["Fur"])
        assertEquals("waterCharges", DeductionPatternMappings.accommodationMappings["Water"])
        assertEquals("electricityCharges", DeductionPatternMappings.accommodationMappings["Elec"])
        assertEquals("barrackDamage", DeductionPatternMappings.accommodationMappings["Barrack Damage"])

        // Verify Recoveries & Loans
        assertEquals("ticketRecovery", DeductionPatternMappings.recoveryAndLoanMappings["ETKT"])
        assertEquals("recFieldAllowance", DeductionPatternMappings.recoveryAndLoanMappings["Rec CIA-FD"])
        assertEquals("recSpecialForces", DeductionPatternMappings.recoveryAndLoanMappings["Rec PARA-SC"])
        assertEquals("agifLoanRecovery", DeductionPatternMappings.recoveryAndLoanMappings["AGIF-CAR"])

        // Verify Ledger
        assertEquals("openingDebitBalance", DeductionPatternMappings.ledgerMappings["Op Dr Bal"])
        assertEquals("closingCreditBalance", DeductionPatternMappings.ledgerMappings["Cl. Cr. Bal."])
    }
}
