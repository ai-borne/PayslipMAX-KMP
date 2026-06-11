@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

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
        val basePath = "/Users/test/Desktop/Pay Slip Elements"
        
        if (!fileManager.fileExistsAtPath(basePath)) {
            println("Pay Slip Elements directory not found at $basePath, skipping iOS integration test.")
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
            val yearPath = "$basePath/$year"
            if (!fileManager.fileExistsAtPath(yearPath)) continue

            val contents = fileManager.contentsOfDirectoryAtPath(yearPath, null) as? List<*> ?: continue
            val pdfFiles = contents.mapNotNull { it as? String }
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
                    val basicPay = payslip.earnings.basicPay
                    val da = payslip.earnings.dearnessAllowance
                    val gross = payslip.summary.grossPay
                    val net = payslip.summary.netRemittance

                    var isPoor = false
                    val reasons = mutableListOf<String>()

                    val isZeroPayMonth = fileName.contains("02 February 2022.pdf")

                    if (!isZeroPayMonth && basicPay == 0.0 && payslip.earnings.adjBasicPay == 0.0 && payslip.earnings.adjPayAndAllce == 0.0) {
                        isPoor = true
                        reasons.add("basicPay is 0.0")
                    }
                    if (!isZeroPayMonth && da == 0.0 && payslip.earnings.adjDa == 0.0 && payslip.earnings.adjPayAndAllce == 0.0) {
                        isPoor = true
                        reasons.add("dearnessAllowance is 0.0")
                    }
                    if (gross == 0.0) {
                        isPoor = true
                        reasons.add("grossPay is 0.0")
                    }

                    val sumEarnings =
                        payslip.earnings.basicPay +
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
                            payslip.earnings.medicalAllowance +
                            payslip.earnings.adjTicketRecovery

                    val diff = kotlin.math.abs(sumEarnings - gross)
                    val closingDr = payslip.ledgerBalances.closingDebitBalance
                    if (diff > 5.0 && kotlin.math.abs(diff - closingDr) > 5.0) {
                        isPoor = true
                        reasons.add("Sum of earnings ($sumEarnings) != grossPay ($gross) [diff: $diff, closingDr: $closingDr]")
                    }

                    if (isPoor) {
                        poorlyParsed++
                        println("⚠️ $fileName - Poorly parsed: ${reasons.joinToString()}")
                    } else {
                        successfullyParsed++
                        println("✅ $fileName - Perfect! (Basic: $basicPay, DA: $da, Gross: $gross, Net: $net)")
                    }
                }
            }
        }

        println("\n=========================================")
        println("iOS Integration Parsing Summary:")
        println("Total Files Checked: $totalFiles")
        println("Perfectly Parsed: $successfullyParsed")
        println("Poorly Parsed: $poorlyParsed")
        println("Failed Files: ${errors.size}")
        println("=========================================")

        assertTrue(errors.isEmpty(), "There were failed files:\n${errors.joinToString("\n")}")
        assertEquals(0, poorlyParsed, "There were poorly parsed files on iOS.")
        assertEquals(totalFiles, successfullyParsed, "Not all files were perfectly parsed on iOS.")
    }
}
