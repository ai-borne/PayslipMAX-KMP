package com.ssbmax.pdfparser.parser

import java.io.File
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class PlatformPdfParserTest {

    private fun isAndroidRuntime(): Boolean {
        return try {
            Class.forName("android.app.ActivityThread")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    @Test
    fun verifyAll46RealPayslips() {
        if (!isAndroidRuntime()) {
            println("Skipping PlatformPdfParserTest because it requires Android runtime to run PDFBox-Android.")
            return
        }

        val baseDir = File("/Users/sunil/Desktop/Pay Slip Elements")
        if (!baseDir.exists()) {
            println("Pay Slip Elements directory not found, skipping integration test.")
            return
        }

        val years = listOf("2022", "2023", "2024", "2025")
        val password = "535d04"
        val parser = PlatformPdfParser()

        var totalFiles = 0
        var successfullyParsed = 0
        var poorlyParsed = 0
        val errors = mutableListOf<String>()

        for (year in years) {
            val yearDir = File(baseDir, year)
            if (!yearDir.exists()) continue

            val pdfFiles = yearDir.listFiles { _, name -> name.endsWith(".pdf") }?.sortedBy { it.name } ?: emptyList()
            for (file in pdfFiles) {
                totalFiles++
                val bytes = file.readBytes()
                val result = parser.decryptAndParse(bytes, password)

                if (result.isFailure) {
                    val ex = result.exceptionOrNull()
                    errors.add("❌ ${file.name} - Failed to parse: ${ex?.message}")
                    println("❌ ${file.name} - Failed to parse: ${ex?.message}")
                    ex?.printStackTrace()
                } else {
                    val payslip = result.getOrNull()!!

                    val basicPay = payslip.earnings.basicPay
                    val da = payslip.earnings.dearnessAllowance
                    val gross = payslip.summary.grossPay
                    val net = payslip.summary.netRemittance

                    var isPoor = false
                    val reasons = mutableListOf<String>()

                    if (basicPay == 0.0 && payslip.earnings.adjBasicPay == 0.0 && payslip.earnings.adjPayAndAllce == 0.0) {
                        isPoor = true
                        reasons.add("basicPay is 0.0")
                    }
                    if (da == 0.0 && payslip.earnings.adjDa == 0.0 && payslip.earnings.adjPayAndAllce == 0.0) {
                        isPoor = true
                        reasons.add("dearnessAllowance is 0.0")
                    }
                    if (gross == 0.0) {
                        isPoor = true
                        reasons.add("grossPay is 0.0")
                    }

                    val sumEarnings = payslip.earnings.basicPay +
                            payslip.earnings.dearnessAllowance +
                            payslip.earnings.militaryServicePay +
                            payslip.earnings.transportAllowance +
                            payslip.earnings.transportAllowanceDa +
                            payslip.earnings.dressAllowance +
                            payslip.earnings.rationMoney +
                            payslip.earnings.specialForcesPay +
                            payslip.earnings.fieldAllowance +
                            payslip.earnings.childrenEducationAllowance +
                            payslip.earnings.adjBasicPay +
                            payslip.earnings.adjDa +
                            payslip.earnings.adjMsp +
                            payslip.earnings.adjTpta +
                            payslip.earnings.arrearsCea +
                            payslip.earnings.arrearsDa +
                            payslip.earnings.arrearsRation +
                            payslip.earnings.arrearsSpecialForces +
                            payslip.earnings.arrearsTpta +
                            payslip.earnings.arrearsTptaDa +
                            payslip.earnings.arrearsHra +
                            payslip.earnings.adjPayAndAllce +
                            payslip.earnings.adjFieldAllowance +
                            payslip.earnings.medicalAllowance

                    val diff = kotlin.math.abs(sumEarnings - gross)
                    val closingDr = payslip.ledgerBalances.closingDebitBalance
                    if (diff > 5.0 && kotlin.math.abs(diff - closingDr) > 5.0) {
                        isPoor = true
                        reasons.add("Sum of earnings ($sumEarnings) != grossPay ($gross) [diff: $diff, closingDr: $closingDr]")
                    }

                    if (isPoor) {
                        poorlyParsed++
                        println("⚠️ ${file.name} - Poorly parsed: ${reasons.joinToString()}")
                        println("   Earnings: ${payslip.earnings}")
                        println("   Deductions: ${payslip.deductions}")
                    } else {
                        successfullyParsed++
                        println("✅ ${file.name} - Perfect! (Basic: $basicPay, DA: $da, Gross: $gross, Net: $net)")
                    }
                }
            }
        }

        println("\n=========================================")
        println("Integration Parsing Summary:")
        println("Total Files Checked: $totalFiles")
        println("Perfectly Parsed: $successfullyParsed")
        println("Poorly Parsed: $poorlyParsed")
        println("Failed Files: ${errors.size}")
        println("=========================================")

        assertTrue(errors.isEmpty(), "There were failed files:\n${errors.joinToString("\n")}")
        assertEquals(0, poorlyParsed, "There were poorly parsed files.")
        assertEquals(totalFiles, successfullyParsed, "Not all files were perfectly parsed.")
    }
}
