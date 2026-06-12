package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlatformPdfParserTest {
    @Test
    fun verifyAll46RealPayslips() {
        val baseDir = File("/Users/test/Desktop/Pay Slip Elements")
        if (!baseDir.exists()) {
            println("Pay Slip Elements directory not found, skipping integration test.")
            return
        }

        val jsonFile = File("/Users/test/Downloads/PDFParser/payslips_data_standardized.json")
        assertTrue(jsonFile.exists(), "Standardized JSON file not found!")

        val jsonText = jsonFile.readText()
        val jsonArray = org.json.JSONArray(jsonText)
        val expectedMap = mutableMapOf<String, org.json.JSONObject>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            expectedMap[obj.getString("file")] = obj
        }
        println("[TEST DEBUG] expectedMap keys size: ${expectedMap.size}, keys: ${expectedMap.keys.take(5)}")

        val years = listOf("2022", "2023", "2024", "2025")
        val password = "535d04"
        val parser = PlatformPdfParser()

        var totalFiles = 0
        var successfullyParsed = 0
        val errors = mutableListOf<String>()

        for (year in years) {
            val yearDir = File(baseDir, year)
            if (!yearDir.exists()) continue

            val pdfFiles = yearDir.listFiles { _, name -> name.endsWith(".pdf") }?.sortedBy { it.name } ?: emptyList()
            for (file in pdfFiles) {
                totalFiles++
                val bytes = file.readBytes()
                val result = parser.decryptAndParse(bytes, password, file.name)

                if (file.name == "09 Sep 2024.pdf" || file.name == "10 Oct 2024.pdf") {
                    println("[TEST DEBUG] File: ${file.name}")
                    if (result.isFailure) {
                        println("  Failed to parse: ${result.exceptionOrNull()?.message}")
                    } else {
                        val p = result.getOrNull()!!
                        println("  grossPay: ${p.summary.grossPay}, totalDeductions: ${p.summary.totalDeductions}, netRemittance: ${p.summary.netRemittance}")
                        println("  earnings: ${p.earnings}")
                        println("  deductions: ${p.deductions}")
                        println("  ledgerBalances: ${p.ledgerBalances}")
                    }
                }

                if (result.isFailure) {
                    val ex = result.exceptionOrNull()
                    errors.add("❌ ${file.name} - Failed to parse: ${ex?.message}")
                    println("❌ ${file.name} - Failed to parse: ${ex?.message}")
                    ex?.printStackTrace()
                } else {
                    val payslip = result.getOrNull()!!
                    val expected = expectedMap[file.name]
                    if (expected == null) {
                        errors.add("❌ ${file.name} - No expected JSON record found!")
                        continue
                    }

                    try {
                        comparePayslips(file.name, payslip, expected)
                        successfullyParsed++
                        println("✅ ${file.name} - Perfect match!")
                    } catch (e: AssertionError) {
                        errors.add("❌ ${file.name} - Mismatch: ${e.message}")
                        println("❌ ${file.name} - Mismatch: ${e.message}")
                    }
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
        // 1. Officer
        val expOfficer = expected.getJSONObject("officer")
        assertEquals(expOfficer.getString("name"), actual.officer.name, "$filename: Officer Name mismatch")
        assertEquals(expOfficer.getString("account_no"), actual.officer.accountNo, "$filename: Officer Account mismatch")
        assertEquals(expOfficer.getString("pan"), actual.officer.pan, "$filename: Officer PAN mismatch")

        // 2. Summary
        val expSummary = expected.getJSONObject("summary")
        assertEquals(expSummary.getDouble("gross_pay"), actual.summary.grossPay, 5.0, "$filename: Gross Pay mismatch")
        assertEquals(expSummary.getDouble("total_deductions"), actual.summary.totalDeductions, 5.0, "$filename: Total Deductions mismatch")
        assertEquals(expSummary.getDouble("net_remittance"), actual.summary.netRemittance, 5.0, "$filename: Net Remittance mismatch")

        // 3. Earnings
        val expEarnings = expected.getJSONObject("earnings")
        assertEquals(expEarnings.optDouble("basic_pay", 0.0), actual.earnings.basicPay, 5.0, "$filename: basicPay mismatch")
        assertEquals(expEarnings.optDouble("dearness_allowance", 0.0), actual.earnings.dearnessAllowance, 5.0, "$filename: dearnessAllowance mismatch")
        assertEquals(expEarnings.optDouble("military_service_pay", 0.0), actual.earnings.militaryServicePay, 5.0, "$filename: militaryServicePay mismatch")
        assertEquals(expEarnings.optDouble("transport_allowance", 0.0), actual.earnings.transportAllowance, 5.0, "$filename: transportAllowance mismatch")
        assertEquals(expEarnings.optDouble("transport_allowance_da", 0.0), actual.earnings.transportAllowanceDa, 5.0, "$filename: transportAllowanceDa mismatch")
        assertEquals(expEarnings.optDouble("dress_allowance", 0.0), actual.earnings.dressAllowance, 5.0, "$filename: dressAllowance mismatch")
        assertEquals(expEarnings.optDouble("ration_money", 0.0), actual.earnings.rationMoney, 5.0, "$filename: rationMoney mismatch")
        assertEquals(expEarnings.optDouble("special_forces_pay", 0.0), actual.earnings.specialForcesPay, 5.0, "$filename: specialForcesPay mismatch")
        assertEquals(expEarnings.optDouble("field_allowance", 0.0), actual.earnings.fieldAllowance, 5.0, "$filename: fieldAllowance mismatch")
        assertEquals(expEarnings.optDouble("children_education_allowance", 0.0), actual.earnings.childrenEducationAllowance, 5.0, "$filename: childrenEducationAllowance mismatch")
        assertEquals(expEarnings.optDouble("adj_basic_pay", 0.0), actual.earnings.adjBasicPay, 5.0, "$filename: adjBasicPay mismatch")
        assertEquals(expEarnings.optDouble("adj_da", 0.0), actual.earnings.adjDa, 5.0, "$filename: adjDa mismatch")
        assertEquals(expEarnings.optDouble("adj_msp", 0.0), actual.earnings.adjMsp, 5.0, "$filename: adjMsp mismatch")
        assertEquals(expEarnings.optDouble("adj_tpta", 0.0), actual.earnings.adjTpta, 5.0, "$filename: adjTpta mismatch")
        assertEquals(expEarnings.optDouble("arrears_cea", 0.0), actual.earnings.arrearsCea, 5.0, "$filename: arrearsCea mismatch")
        assertEquals(expEarnings.optDouble("arrears_da", 0.0), actual.earnings.arrearsDa, 5.0, "$filename: arrearsDa mismatch")
        assertEquals(expEarnings.optDouble("arrears_ration", 0.0), actual.earnings.arrearsRation, 5.0, "$filename: arrearsRation mismatch")
        assertEquals(expEarnings.optDouble("arrears_special_forces", 0.0), actual.earnings.arrearsSpecialForces, 5.0, "$filename: arrearsSpecialForces mismatch")
        assertEquals(expEarnings.optDouble("arrears_tpta", 0.0), actual.earnings.arrearsTpta, 5.0, "$filename: arrearsTpta mismatch")
        assertEquals(expEarnings.optDouble("arrears_tpta_da", 0.0), actual.earnings.arrearsTptaDa, 5.0, "$filename: arrearsTptaDa mismatch")
        assertEquals(expEarnings.optDouble("arrears_hra", 0.0), actual.earnings.arrearsHra, 5.0, "$filename: arrearsHra mismatch")
        assertEquals(expEarnings.optDouble("adj_pay_and_allce", 0.0), actual.earnings.adjPayAndAllce, 5.0, "$filename: adjPayAndAllce mismatch")
        assertEquals(expEarnings.optDouble("adj_field_allowance", 0.0), actual.earnings.adjFieldAllowance, 5.0, "$filename: adjFieldAllowance mismatch")
        assertEquals(expEarnings.optDouble("medical_allowance", 0.0), actual.earnings.medicalAllowance, 5.0, "$filename: medicalAllowance mismatch")

        // 4. Deductions
        val expDeductions = expected.getJSONObject("deductions")
        assertEquals(expDeductions.optDouble("dsop_subscription", 0.0), actual.deductions.dsopSubscription, 5.0, "$filename: dsopSubscription mismatch")
        assertEquals(expDeductions.optDouble("agif", 0.0), actual.deductions.agif, 5.0, "$filename: agif mismatch")
        assertEquals(expDeductions.optDouble("income_tax", 0.0), actual.deductions.incomeTax, 5.0, "$filename: incomeTax mismatch")
        assertEquals(expDeductions.optDouble("education_cess", 0.0), actual.deductions.educationCess, 5.0, "$filename: educationCess mismatch")
        assertEquals(expDeductions.optDouble("license_fee", 0.0), actual.deductions.licenseFee, 5.0, "$filename: licenseFee mismatch")
        assertEquals(expDeductions.optDouble("furniture_rent", 0.0), actual.deductions.furnitureRent, 5.0, "$filename: furnitureRent mismatch")
        assertEquals(expDeductions.optDouble("water_charges", 0.0), actual.deductions.waterCharges, 5.0, "$filename: waterCharges mismatch")
        assertEquals(expDeductions.optDouble("electricity_charges", 0.0), actual.deductions.electricityCharges, 5.0, "$filename: electricityCharges mismatch")
        assertEquals(expDeductions.optDouble("barrack_damage", 0.0), actual.deductions.barrackDamage, 5.0, "$filename: barrackDamage mismatch")
        assertEquals(expDeductions.optDouble("ticket_recovery", 0.0), actual.deductions.ticketRecovery, 5.0, "$filename: ticketRecovery mismatch")
        assertEquals(expDeductions.optDouble("rec_field_allowance", 0.0), actual.deductions.recFieldAllowance, 5.0, "$filename: recFieldAllowance mismatch")
        assertEquals(expDeductions.optDouble("rec_special_forces", 0.0), actual.deductions.recSpecialForces, 5.0, "$filename: recSpecialForces mismatch")
        assertEquals(expDeductions.optDouble("recovery_of_debits", 0.0), actual.deductions.recoveryOfDebits, 5.0, "$filename: recoveryOfDebits mismatch")

        // 5. Tax & Savings
        if (expected.has("tax_and_savings") && !expected.isNull("tax_and_savings") && actual.taxAndSavings != null) {
            val expTax = expected.getJSONObject("tax_and_savings")
            val actTax = actual.taxAndSavings
            assertEquals(expTax.optDouble("gross_salary_ytd", 0.0), actTax.grossSalaryYtd, 5.0, "$filename: grossSalaryYtd mismatch")
            assertEquals(expTax.optDouble("total_taxable_income", 0.0), actTax.totalTaxableIncome, 5.0, "$filename: totalTaxableIncome mismatch")
            assertEquals(expTax.optDouble("standard_deduction", 0.0), actTax.standardDeduction, 5.0, "$filename: standardDeduction mismatch")
            assertEquals(expTax.optDouble("net_taxable_income", 0.0), actTax.netTaxableIncome, 5.0, "$filename: netTaxableIncome mismatch")
            assertEquals(expTax.optDouble("total_tax_payable", 0.0), actTax.totalTaxPayable, 5.0, "$filename: totalTaxPayable mismatch")
            assertEquals(expTax.optDouble("tax_deducted_ytd", 0.0), actTax.taxDeductedYtd, 5.0, "$filename: taxDeductedYtd mismatch")
            assertEquals(expTax.optDouble("cess_deducted_ytd", 0.0), actTax.cessDeductedYtd, 5.0, "$filename: cessDeductedYtd mismatch")

            if (expTax.has("dsop_fund") && !expTax.isNull("dsop_fund") && actTax.dsopFund != null) {
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
    }
}
