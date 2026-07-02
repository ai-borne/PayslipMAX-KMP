package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Opt-in, full-pipeline integration test against real (encrypted) PDFs on the developer machine.
 * It is skipped cleanly in CI: nothing runs unless both system properties are supplied, so there
 * are no hardcoded absolute paths and no dependency on out-of-repo data.
 *
 * Usage:
 * ```
 * ./gradlew :shared:testDebugUnitTest --tests "*PlatformPdfParserTest" \
 *   -Dpayslip.localCorpus="/Users/test/Desktop/Pay Slip Elements" \
 *   -Dpayslip.localCorpus.json="/Users/test/Downloads/PDFParser/payslips_data_standardized.json"
 * ```
 * Optional: `.password` (default 535d04), `.minYear` (default 2022).
 */
class PlatformPdfParserTest {
    private val password: String get() = System.getProperty("payslip.localCorpus.password") ?: "535d04"

    private fun localCorpusDir(): File? =
        System.getProperty("payslip.localCorpus")
            ?.takeIf { it.isNotBlank() }
            ?.let { File(it) }
            ?.takeIf { it.isDirectory }

    @Test
    fun verifyRealPayslipsAgainstGroundTruth() {
        val baseDir = localCorpusDir()
        if (baseDir == null) {
            println("[PlatformPdfParserTest] -Dpayslip.localCorpus not set; skipping integration test (expected in CI).")
            return
        }
        val jsonPath = System.getProperty("payslip.localCorpus.json")
        if (jsonPath.isNullOrBlank() || !File(jsonPath).exists()) {
            println("[PlatformPdfParserTest] -Dpayslip.localCorpus.json missing; skipping integration test.")
            return
        }

        val jsonArray = org.json.JSONArray(File(jsonPath).readText())
        val expectedMap = mutableMapOf<String, org.json.JSONObject>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            expectedMap[obj.getString("file")] = obj
        }

        val minYear = System.getProperty("payslip.localCorpus.minYear")?.toIntOrNull() ?: 2022
        val parser = PlatformPdfParser()

        var totalFiles = 0
        var successfullyParsed = 0
        val errors = mutableListOf<String>()

        baseDir.listFiles { f -> f.isDirectory && f.name.toIntOrNull()?.let { it >= minYear } == true }
            ?.sortedBy { it.name }
            ?.forEach { yearDir ->
                val pdfFiles = yearDir.listFiles { _, name -> name.endsWith(".pdf", ignoreCase = true) }?.sortedBy { it.name } ?: emptyList()
                for (file in pdfFiles) {
                    totalFiles++
                    val result = parser.decryptAndParse(file.readBytes(), password, file.name)
                    if (result.isFailure) {
                        errors.add("❌ ${file.name} - Failed to parse: ${result.exceptionOrNull()?.message}")
                        continue
                    }
                    val payslip = result.getOrNull()!!
                    val expected = expectedMap[file.name]
                    if (expected == null) {
                        println("⏭️ ${file.name} - No expected JSON entry; skipping (print variant or unlisted file)")
                        totalFiles--
                        continue
                    }
                    try {
                        comparePayslips(file.name, payslip, expected)
                        successfullyParsed++
                        println("✅ ${file.name} - Perfect match!")
                    } catch (e: AssertionError) {
                        errors.add("❌ ${file.name} - Mismatch: ${e.message}")
                    }
                }
            }

        println("\n=========================================")
        println("Integration Parsing Summary:")
        println("Total Files Checked: $totalFiles")
        println("Perfectly Matched: $successfullyParsed")
        println("Failed/Mismatched Files: ${errors.size}")
        println("=========================================")

        assertTrue(errors.isEmpty(), "There were failed/mismatched files:\n${errors.joinToString("\n")}")
    }

    private fun comparePayslips(
        filename: String,
        actual: ParsedPayslip,
        expected: org.json.JSONObject,
    ) {
        val expOfficer = expected.getJSONObject("officer")
        assertEquals(expOfficer.getString("name"), actual.officer.name, "$filename: Officer Name mismatch")
        assertEquals(expOfficer.getString("account_no"), actual.officer.accountNo, "$filename: Officer Account mismatch")
        assertEquals(expOfficer.getString("pan"), actual.officer.pan, "$filename: Officer PAN mismatch")

        val expSummary = expected.getJSONObject("summary")
        assertEquals(expSummary.getDouble("gross_pay"), actual.summary.grossPay, 5.0, "$filename: Gross Pay mismatch")
        assertEquals(expSummary.getDouble("total_deductions"), actual.summary.totalDeductions, 5.0, "$filename: Total Deductions mismatch")
        assertEquals(expSummary.getDouble("net_remittance"), actual.summary.netRemittance, 5.0, "$filename: Net Remittance mismatch")

        compareEarnings(filename, actual, expected.getJSONObject("earnings"))
        compareDeductions(filename, actual, expected.getJSONObject("deductions"))
        compareTax(filename, actual, expected)
    }

    private fun compareEarnings(
        filename: String,
        actual: ParsedPayslip,
        exp: org.json.JSONObject,
    ) {
        val e = actual.earnings
        assertEquals(exp.optDouble("basic_pay", 0.0), e.basicPay, 5.0, "$filename: basicPay mismatch")
        assertEquals(exp.optDouble("dearness_allowance", 0.0), e.dearnessAllowance, 5.0, "$filename: dearnessAllowance mismatch")
        assertEquals(exp.optDouble("military_service_pay", 0.0), e.militaryServicePay, 5.0, "$filename: militaryServicePay mismatch")
        assertEquals(exp.optDouble("transport_allowance", 0.0), e.transportAllowance, 5.0, "$filename: transportAllowance mismatch")
        assertEquals(exp.optDouble("transport_allowance_da", 0.0), e.transportAllowanceDa, 5.0, "$filename: transportAllowanceDa mismatch")
        assertEquals(exp.optDouble("dress_allowance", 0.0), e.dressAllowance, 5.0, "$filename: dressAllowance mismatch")
        assertEquals(exp.optDouble("ration_money", 0.0), e.rationMoney, 5.0, "$filename: rationMoney mismatch")
        assertEquals(exp.optDouble("special_forces_pay", 0.0), e.specialForcesPay, 5.0, "$filename: specialForcesPay mismatch")
        assertEquals(exp.optDouble("field_allowance", 0.0), e.fieldAllowance, 5.0, "$filename: fieldAllowance mismatch")
        assertEquals(exp.optDouble("children_education_allowance", 0.0), e.childrenEducationAllowance, 5.0, "$filename: childrenEducationAllowance mismatch")
        assertEquals(exp.optDouble("adj_basic_pay", 0.0), e.adjBasicPay, 5.0, "$filename: adjBasicPay mismatch")
        assertEquals(exp.optDouble("adj_da", 0.0), e.adjDa, 5.0, "$filename: adjDa mismatch")
        assertEquals(exp.optDouble("adj_msp", 0.0), e.adjMsp, 5.0, "$filename: adjMsp mismatch")
        assertEquals(exp.optDouble("adj_tpta", 0.0), e.adjTpta, 5.0, "$filename: adjTpta mismatch")
        assertEquals(exp.optDouble("arrears_cea", 0.0), e.arrearsCea, 5.0, "$filename: arrearsCea mismatch")
        assertEquals(exp.optDouble("arrears_da", 0.0), e.arrearsDa, 5.0, "$filename: arrearsDa mismatch")
        assertEquals(exp.optDouble("arrears_ration", 0.0), e.arrearsRation, 5.0, "$filename: arrearsRation mismatch")
        assertEquals(exp.optDouble("arrears_special_forces", 0.0), e.arrearsSpecialForces, 5.0, "$filename: arrearsSpecialForces mismatch")
        assertEquals(exp.optDouble("arrears_tpta", 0.0), e.arrearsTpta, 5.0, "$filename: arrearsTpta mismatch")
        assertEquals(exp.optDouble("arrears_tpta_da", 0.0), e.arrearsTptaDa, 5.0, "$filename: arrearsTptaDa mismatch")
        assertEquals(exp.optDouble("arrears_hra", 0.0), e.arrearsHra, 5.0, "$filename: arrearsHra mismatch")
        assertEquals(exp.optDouble("adj_pay_and_allce", 0.0), e.adjPayAndAllce, 5.0, "$filename: adjPayAndAllce mismatch")
        assertEquals(exp.optDouble("adj_field_allowance", 0.0), e.adjFieldAllowance, 5.0, "$filename: adjFieldAllowance mismatch")
        assertEquals(exp.optDouble("medical_allowance", 0.0), e.medicalAllowance, 5.0, "$filename: medicalAllowance mismatch")
    }

    private fun compareDeductions(
        filename: String,
        actual: ParsedPayslip,
        exp: org.json.JSONObject,
    ) {
        val d = actual.deductions
        assertEquals(exp.optDouble("dsop_subscription", 0.0), d.dsopSubscription, 5.0, "$filename: dsopSubscription mismatch")
        assertEquals(exp.optDouble("agif", 0.0), d.agif, 5.0, "$filename: agif mismatch")
        assertEquals(exp.optDouble("income_tax", 0.0), d.incomeTax, 5.0, "$filename: incomeTax mismatch")
        assertEquals(exp.optDouble("education_cess", 0.0), d.educationCess, 5.0, "$filename: educationCess mismatch")
        assertEquals(exp.optDouble("license_fee", 0.0), d.licenseFee, 5.0, "$filename: licenseFee mismatch")
        assertEquals(exp.optDouble("furniture_rent", 0.0), d.furnitureRent, 5.0, "$filename: furnitureRent mismatch")
        assertEquals(exp.optDouble("water_charges", 0.0), d.waterCharges, 5.0, "$filename: waterCharges mismatch")
        assertEquals(exp.optDouble("electricity_charges", 0.0), d.electricityCharges, 5.0, "$filename: electricityCharges mismatch")
        assertEquals(exp.optDouble("barrack_damage", 0.0), d.barrackDamage, 5.0, "$filename: barrackDamage mismatch")
        assertEquals(exp.optDouble("ticket_recovery", 0.0), d.ticketRecovery, 5.0, "$filename: ticketRecovery mismatch")
        assertEquals(exp.optDouble("rec_field_allowance", 0.0), d.recFieldAllowance, 5.0, "$filename: recFieldAllowance mismatch")
        assertEquals(exp.optDouble("rec_special_forces", 0.0), d.recSpecialForces, 5.0, "$filename: recSpecialForces mismatch")
        assertEquals(exp.optDouble("recovery_of_debits", 0.0), d.recoveryOfDebits, 5.0, "$filename: recoveryOfDebits mismatch")
    }

    private fun compareTax(
        filename: String,
        actual: ParsedPayslip,
        expected: org.json.JSONObject,
    ) {
        if (!expected.has("tax_and_savings") || expected.isNull("tax_and_savings") || actual.taxAndSavings == null) return
        val expTax = expected.getJSONObject("tax_and_savings")
        val actTax = actual.taxAndSavings
        assertEquals(expTax.optDouble("gross_salary_ytd", 0.0), actTax.grossSalaryYtd, 5.0, "$filename: grossSalaryYtd mismatch")
        assertEquals(expTax.optDouble("total_taxable_income", 0.0), actTax.totalTaxableIncome, 5.0, "$filename: totalTaxableIncome mismatch")
        assertEquals(expTax.optDouble("standard_deduction", 0.0), actTax.standardDeduction, 5.0, "$filename: standardDeduction mismatch")
        assertEquals(expTax.optDouble("net_taxable_income", 0.0), actTax.netTaxableIncome, 5.0, "$filename: netTaxableIncome mismatch")
        assertEquals(expTax.optDouble("total_tax_payable", 0.0), actTax.totalTaxPayable, 5.0, "$filename: totalTaxPayable mismatch")
        assertEquals(expTax.optDouble("tax_deducted_ytd", 0.0), actTax.taxDeductedYtd, 5.0, "$filename: taxDeductedYtd mismatch")
        assertEquals(expTax.optDouble("cess_deducted_ytd", 0.0), actTax.cessDeductedYtd, 5.0, "$filename: cessDeductedYtd mismatch")

        if (!expTax.has("dsop_fund") || expTax.isNull("dsop_fund") || actTax.dsopFund == null) return
        val expDsop = expTax.getJSONObject("dsop_fund")
        val actDsop = actTax.dsopFund
        assertEquals(expDsop.optDouble("opening_balance", 0.0), actDsop.openingBalance, 5.0, "$filename: dsop opening_balance mismatch")
        assertEquals(expDsop.optDouble("subscription_ytd", 0.0), actDsop.subscriptionYtd, 5.0, "$filename: dsop subscription_ytd mismatch")
        assertEquals(expDsop.optDouble("refund_ytd", 0.0), actDsop.refundYtd, 5.0, "$filename: dsop refund_ytd mismatch")
        assertEquals(expDsop.optDouble("misc_adj_ytd", 0.0), actDsop.miscAdjYtd, 5.0, "$filename: dsop misc_adj_ytd mismatch")
        assertEquals(expDsop.optDouble("withdrawal_ytd", 0.0), actDsop.withdrawalYtd, 5.0, "$filename: dsop withdrawal_ytd mismatch")
        assertEquals(expDsop.optDouble("closing_balance", 0.0), actDsop.closingBalance, 5.0, "$filename: dsop closing_balance mismatch")
    }
}
