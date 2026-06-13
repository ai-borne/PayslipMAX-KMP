@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.*
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlatformPdfParserIosTest {
    private fun NSData.toByteArray(): ByteArray {
        val size = this.length.toInt()
        val bytes = ByteArray(size)
        if (size > 0) {
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), this.bytes, this.length)
            }
        }
        return bytes
    }

    @Test
    fun verifyAll46RealPayslipsOnIos() {
        val fileManager = NSFileManager.defaultManager
        val basePath = "/Users/sunil/Desktop/Pay Slip Elements"

        if (!fileManager.fileExistsAtPath(basePath)) {
            println("Pay Slip Elements directory not found at $basePath, skipping iOS integration test.")
            return
        }

        val jsonPath = "/Users/sunil/Downloads/PDFParser/payslips_data_standardized.json"
        val jsonData = NSData.create(contentsOfFile = jsonPath)
        assertNotNull(jsonData, "Standardized JSON file not found at $jsonPath!")

        val jsonArray =
            platform.Foundation.NSJSONSerialization.JSONObjectWithData(
                data = jsonData,
                options = 0UL,
                error = null,
            ) as? platform.Foundation.NSArray
        assertNotNull(jsonArray, "Failed to parse JSON on iOS!")

        val expectedMap = mutableMapOf<String, platform.Foundation.NSDictionary>()
        for (i in 0 until jsonArray.count.toInt()) {
            val obj = jsonArray.objectAtIndex(i.toULong()) as? platform.Foundation.NSDictionary ?: continue
            val fileName = obj.objectForKey("file") as? String ?: continue
            expectedMap[fileName] = obj
        }

        val years = listOf("2022", "2023", "2024", "2025")
        val password = "535d04"
        val parser = PlatformPdfParser()

        var totalFiles = 0
        var successfullyParsed = 0
        val errors = mutableListOf<String>()

        for (year in years) {
            val yearPath = "$basePath/$year"
            if (!fileManager.fileExistsAtPath(yearPath)) continue

            val contents = fileManager.contentsOfDirectoryAtPath(yearPath, null) as? List<*> ?: continue
            val pdfFiles =
                contents.mapNotNull { it as? String }
                    .filter { it.endsWith(".pdf", ignoreCase = true) }
                    .sorted()

            for (fileName in pdfFiles) {
                totalFiles++
                val filePath = "$yearPath/$fileName"
                val data = NSData.create(contentsOfFile = filePath)
                if (data == null) {
                    errors.add("❌ $fileName - Could not read data from $filePath")
                    continue
                }

                val bytes = data.toByteArray()
                val result = parser.decryptAndParse(bytes, password, fileName)

                if (result.isFailure) {
                    val ex = result.exceptionOrNull()
                    errors.add("❌ $fileName - Failed to parse: ${ex?.message}")
                    println("❌ $fileName - Failed to parse: ${ex?.message}")
                } else {
                    val payslip = result.getOrNull()!!
                    val expected = expectedMap[fileName]
                    if (expected == null) {
                        errors.add("❌ $fileName - No expected JSON record found!")
                        continue
                    }

                    try {
                        comparePayslips(fileName, payslip, expected)
                        successfullyParsed++
                        println("✅ $fileName - Perfect match!")
                    } catch (e: AssertionError) {
                        errors.add("❌ $fileName - Mismatch: ${e?.message}")
                        println("❌ $fileName - Mismatch: ${e?.message}")
                    }
                }
            }
        }

        println("\n=========================================")
        println("iOS Integration Parsing Summary:")
        println("Total Files Checked: $totalFiles")
        println("Perfectly Matched: $successfullyParsed")
        println("Failed/Mismatched Files: ${errors.size}")
        println("=========================================")

        assertTrue(errors.isEmpty(), "There were failed/mismatched files on iOS:\n${errors.joinToString("\n")}")
    }

    private fun comparePayslips(
        filename: String,
        actual: ParsedPayslip,
        expected: platform.Foundation.NSDictionary,
    ) {
        // 1. Officer
        val expOfficer = expected.objectForKey("officer") as? platform.Foundation.NSDictionary
        if (expOfficer != null) {
            assertEquals(expOfficer.objectForKey("name") as? String, actual.officer.name, "$filename: Officer Name mismatch")
            assertEquals(expOfficer.objectForKey("account_no") as? String, actual.officer.accountNo, "$filename: Officer Account mismatch")
            assertEquals(expOfficer.objectForKey("pan") as? String, actual.officer.pan, "$filename: Officer PAN mismatch")
        }

        // 2. Summary
        val expSummary = expected.objectForKey("summary") as? platform.Foundation.NSDictionary
        if (expSummary != null) {
            assertEquals((expSummary.objectForKey("gross_pay") as? Number)?.toDouble() ?: 0.0, actual.summary.grossPay, 5.0, "$filename: Gross Pay mismatch")
            assertEquals((expSummary.objectForKey("total_deductions") as? Number)?.toDouble() ?: 0.0, actual.summary.totalDeductions, 5.0, "$filename: Total Deductions mismatch")
            assertEquals((expSummary.objectForKey("net_remittance") as? Number)?.toDouble() ?: 0.0, actual.summary.netRemittance, 5.0, "$filename: Net Remittance mismatch")
        }

        // 3. Earnings
        val expEarnings = expected.objectForKey("earnings") as? platform.Foundation.NSDictionary
        if (expEarnings != null) {
            assertEquals((expEarnings.objectForKey("basic_pay") as? Number)?.toDouble() ?: 0.0, actual.earnings.basicPay, 5.0, "$filename: basicPay mismatch")
            assertEquals((expEarnings.objectForKey("dearness_allowance") as? Number)?.toDouble() ?: 0.0, actual.earnings.dearnessAllowance, 5.0, "$filename: dearnessAllowance mismatch")
            assertEquals((expEarnings.objectForKey("military_service_pay") as? Number)?.toDouble() ?: 0.0, actual.earnings.militaryServicePay, 5.0, "$filename: militaryServicePay mismatch")
            assertEquals((expEarnings.objectForKey("transport_allowance") as? Number)?.toDouble() ?: 0.0, actual.earnings.transportAllowance, 5.0, "$filename: transportAllowance mismatch")
            assertEquals((expEarnings.objectForKey("transport_allowance_da") as? Number)?.toDouble() ?: 0.0, actual.earnings.transportAllowanceDa, 5.0, "$filename: transportAllowanceDa mismatch")
            assertEquals((expEarnings.objectForKey("dress_allowance") as? Number)?.toDouble() ?: 0.0, actual.earnings.dressAllowance, 5.0, "$filename: dressAllowance mismatch")
            assertEquals((expEarnings.objectForKey("ration_money") as? Number)?.toDouble() ?: 0.0, actual.earnings.rationMoney, 5.0, "$filename: rationMoney mismatch")
            assertEquals((expEarnings.objectForKey("special_forces_pay") as? Number)?.toDouble() ?: 0.0, actual.earnings.specialForcesPay, 5.0, "$filename: specialForcesPay mismatch")
            assertEquals((expEarnings.objectForKey("field_allowance") as? Number)?.toDouble() ?: 0.0, actual.earnings.fieldAllowance, 5.0, "$filename: fieldAllowance mismatch")
            assertEquals((expEarnings.objectForKey("children_education_allowance") as? Number)?.toDouble() ?: 0.0, actual.earnings.childrenEducationAllowance, 5.0, "$filename: childrenEducationAllowance mismatch")
            assertEquals((expEarnings.objectForKey("adj_basic_pay") as? Number)?.toDouble() ?: 0.0, actual.earnings.adjBasicPay, 5.0, "$filename: adjBasicPay mismatch")
            assertEquals((expEarnings.objectForKey("adj_da") as? Number)?.toDouble() ?: 0.0, actual.earnings.adjDa, 5.0, "$filename: adjDa mismatch")
            assertEquals((expEarnings.objectForKey("adj_msp") as? Number)?.toDouble() ?: 0.0, actual.earnings.adjMsp, 5.0, "$filename: adjMsp mismatch")
            assertEquals((expEarnings.objectForKey("adj_tpta") as? Number)?.toDouble() ?: 0.0, actual.earnings.adjTpta, 5.0, "$filename: adjTpta mismatch")
            assertEquals((expEarnings.objectForKey("arrears_cea") as? Number)?.toDouble() ?: 0.0, actual.earnings.arrearsCea, 5.0, "$filename: arrearsCea mismatch")
            assertEquals((expEarnings.objectForKey("arrears_da") as? Number)?.toDouble() ?: 0.0, actual.earnings.arrearsDa, 5.0, "$filename: arrearsDa mismatch")
            assertEquals((expEarnings.objectForKey("arrears_ration") as? Number)?.toDouble() ?: 0.0, actual.earnings.arrearsRation, 5.0, "$filename: arrearsRation mismatch")
            assertEquals((expEarnings.objectForKey("arrears_special_forces") as? Number)?.toDouble() ?: 0.0, actual.earnings.arrearsSpecialForces, 5.0, "$filename: arrearsSpecialForces mismatch")
            assertEquals((expEarnings.objectForKey("arrears_tpta") as? Number)?.toDouble() ?: 0.0, actual.earnings.arrearsTpta, 5.0, "$filename: arrearsTpta mismatch")
            assertEquals((expEarnings.objectForKey("arrears_tpta_da") as? Number)?.toDouble() ?: 0.0, actual.earnings.arrearsTptaDa, 5.0, "$filename: arrearsTptaDa mismatch")
            assertEquals((expEarnings.objectForKey("arrears_hra") as? Number)?.toDouble() ?: 0.0, actual.earnings.arrearsHra, 5.0, "$filename: arrearsHra mismatch")
            assertEquals((expEarnings.objectForKey("adj_pay_and_allce") as? Number)?.toDouble() ?: 0.0, actual.earnings.adjPayAndAllce, 5.0, "$filename: adjPayAndAllce mismatch")
            assertEquals((expEarnings.objectForKey("adj_field_allowance") as? Number)?.toDouble() ?: 0.0, actual.earnings.adjFieldAllowance, 5.0, "$filename: adjFieldAllowance mismatch")
            assertEquals((expEarnings.objectForKey("medical_allowance") as? Number)?.toDouble() ?: 0.0, actual.earnings.medicalAllowance, 5.0, "$filename: medicalAllowance mismatch")
        }

        // 4. Deductions
        val expDeductions = expected.objectForKey("deductions") as? platform.Foundation.NSDictionary
        if (expDeductions != null) {
            assertEquals((expDeductions.objectForKey("dsop_subscription") as? Number)?.toDouble() ?: 0.0, actual.deductions.dsopSubscription, 5.0, "$filename: dsopSubscription mismatch")
            assertEquals((expDeductions.objectForKey("agif") as? Number)?.toDouble() ?: 0.0, actual.deductions.agif, 5.0, "$filename: agif mismatch")
            assertEquals((expDeductions.objectForKey("income_tax") as? Number)?.toDouble() ?: 0.0, actual.deductions.incomeTax, 5.0, "$filename: incomeTax mismatch")
            assertEquals((expDeductions.objectForKey("education_cess") as? Number)?.toDouble() ?: 0.0, actual.deductions.educationCess, 5.0, "$filename: educationCess mismatch")
            assertEquals((expDeductions.objectForKey("license_fee") as? Number)?.toDouble() ?: 0.0, actual.deductions.licenseFee, 5.0, "$filename: licenseFee mismatch")
            assertEquals((expDeductions.objectForKey("furniture_rent") as? Number)?.toDouble() ?: 0.0, actual.deductions.furnitureRent, 5.0, "$filename: furnitureRent mismatch")
            assertEquals((expDeductions.objectForKey("water_charges") as? Number)?.toDouble() ?: 0.0, actual.deductions.waterCharges, 5.0, "$filename: waterCharges mismatch")
            assertEquals((expDeductions.objectForKey("electricity_charges") as? Number)?.toDouble() ?: 0.0, actual.deductions.electricityCharges, 5.0, "$filename: electricityCharges mismatch")
            assertEquals((expDeductions.objectForKey("barrack_damage") as? Number)?.toDouble() ?: 0.0, actual.deductions.barrackDamage, 5.0, "$filename: barrackDamage mismatch")
            assertEquals((expDeductions.objectForKey("ticket_recovery") as? Number)?.toDouble() ?: 0.0, actual.deductions.ticketRecovery, 5.0, "$filename: ticketRecovery mismatch")
            assertEquals((expDeductions.objectForKey("rec_field_allowance") as? Number)?.toDouble() ?: 0.0, actual.deductions.recFieldAllowance, 5.0, "$filename: recFieldAllowance mismatch")
            assertEquals((expDeductions.objectForKey("rec_special_forces") as? Number)?.toDouble() ?: 0.0, actual.deductions.recSpecialForces, 5.0, "$filename: recSpecialForces mismatch")
            assertEquals((expDeductions.objectForKey("recovery_of_debits") as? Number)?.toDouble() ?: 0.0, actual.deductions.recoveryOfDebits, 5.0, "$filename: recoveryOfDebits mismatch")
        }

        // 5. Tax & Savings
        val expTax = expected.objectForKey("tax_and_savings") as? platform.Foundation.NSDictionary
        if (expTax != null && actual.taxAndSavings != null) {
            val actTax = actual.taxAndSavings!!
            assertEquals((expTax.objectForKey("gross_salary_ytd") as? Number)?.toDouble() ?: 0.0, actTax.grossSalaryYtd, 5.0, "$filename: grossSalaryYtd mismatch")
            assertEquals((expTax.objectForKey("total_taxable_income") as? Number)?.toDouble() ?: 0.0, actTax.totalTaxableIncome, 5.0, "$filename: totalTaxableIncome mismatch")
            assertEquals((expTax.objectForKey("standard_deduction") as? Number)?.toDouble() ?: 0.0, actTax.standardDeduction, 5.0, "$filename: standardDeduction mismatch")
            assertEquals((expTax.objectForKey("net_taxable_income") as? Number)?.toDouble() ?: 0.0, actTax.netTaxableIncome, 5.0, "$filename: netTaxableIncome mismatch")
            assertEquals((expTax.objectForKey("total_tax_payable") as? Number)?.toDouble() ?: 0.0, actTax.totalTaxPayable, 5.0, "$filename: totalTaxPayable mismatch")
            assertEquals((expTax.objectForKey("tax_deducted_ytd") as? Number)?.toDouble() ?: 0.0, actTax.taxDeductedYtd, 5.0, "$filename: taxDeductedYtd mismatch")
            assertEquals((expTax.objectForKey("cess_deducted_ytd") as? Number)?.toDouble() ?: 0.0, actTax.cessDeductedYtd, 5.0, "$filename: cessDeductedYtd mismatch")

            val expDsop = expTax.objectForKey("dsop_fund") as? platform.Foundation.NSDictionary
            if (expDsop != null && actTax.dsopFund != null) {
                val actDsop = actTax.dsopFund!!
                assertEquals((expDsop.objectForKey("opening_balance") as? Number)?.toDouble() ?: 0.0, actDsop.openingBalance, 5.0, "$filename: dsop opening_balance mismatch")
                assertEquals((expDsop.objectForKey("subscription_ytd") as? Number)?.toDouble() ?: 0.0, actDsop.subscriptionYtd, 5.0, "$filename: dsop subscription_ytd mismatch")
                assertEquals((expDsop.objectForKey("refund_ytd") as? Number)?.toDouble() ?: 0.0, actDsop.refundYtd, 5.0, "$filename: dsop refund_ytd mismatch")
                assertEquals((expDsop.objectForKey("misc_adj_ytd") as? Number)?.toDouble() ?: 0.0, actDsop.miscAdjYtd, 5.0, "$filename: dsop misc_adj_ytd mismatch")
                assertEquals((expDsop.objectForKey("withdrawal_ytd") as? Number)?.toDouble() ?: 0.0, actDsop.withdrawalYtd, 5.0, "$filename: dsop withdrawal_ytd mismatch")
                assertEquals((expDsop.objectForKey("closing_balance") as? Number)?.toDouble() ?: 0.0, actDsop.closingBalance, 5.0, "$filename: dsop closing_balance mismatch")
            }
        }
    }
}
