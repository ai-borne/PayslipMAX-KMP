package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.FieldSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * End-to-end reconciliation tests for LTC leave encashment paycodes (`ARR-LVELTC`, `LVELTC`).
 * Validates that PCDA(O) leave encashment claims reconcile cleanly to [adjPayAndAllce]
 * with zero misc residual and high confidence.
 */
class ReconciliationSolverLtcTest {
    private fun credit(
        key: String,
        amount: Double,
        rawLabel: String = key,
    ) = ClassifiedEntry(rawLabel, key, amount, TableSide.CREDIT, 44f, 0f, key, TableSide.CREDIT)

    private fun debit(
        key: String,
        amount: Double,
        rawLabel: String = key,
    ) = ClassifiedEntry(rawLabel, key, amount, TableSide.DEBIT, 186f, 0f, key, TableSide.DEBIT)

    private fun tok(
        text: String,
        x: Float,
        y: Float,
        w: Float = text.length * 5f,
        h: Float = 6f,
    ) = PositionedToken(text = text, x = x, y = y, width = w, height = h)

    private fun solve(
        entries: List<ClassifiedEntry>,
        gross: Double,
        deductions: Double,
        net: Double,
    ) = ReconciliationSolver.solve(ClassifiedTable(entries), gross, deductions, net, fullText = "", filename = "test_ltc")

    @Test
    fun userPayslipWithArrLveltcReconcilesCleanlyWithoutMiscResidual() {
        val entries =
            listOf(
                credit("basicPay", 153500.0, "BPAY"),
                credit("dearnessAllowance", 98020.0, "DA"),
                credit("houseRentAllowance", 46050.0, "HRAX"),
                credit("militaryServicePay", 15500.0, "MSP"),
                credit("riskHardshipAllowance", 21125.0, "RH12"),
                credit("technicalAllowance", 3000.0, "TECI"),
                credit("transportAllowance", 3600.0, "TPTA"),
                credit("transportAllowanceDa", 2088.0, "TPTADA"),
                credit("adjPayAndAllce", 78473.0, "ARR-LVELTC"),
                debit("dsopSubscription", 30000.0, "DSOP"),
                debit("agif", 12500.0, "AGIF"),
                debit("incomeTax", 67951.0, "ITAX"),
                debit("educationCess", 2718.0, "EHCESS"),
            )

        val solved =
            solve(
                entries = entries,
                gross = 421356.0,
                deductions = 113169.0,
                net = 308187.0,
            )

        assertEquals(78473.0, solved.earningsMap["adjPayAndAllce"])
        assertEquals(0.0, solved.reconciled.miscEarnings, "balanced table leaves no misc residual")
        assertEquals(0.0, solved.reconciled.miscDeductions)
        assertTrue(solved.rawEarnings.isEmpty(), "raw earnings must be empty when all codes matched")
        assertTrue(solved.rawDeductions.isEmpty(), "raw deductions must be empty when all codes matched")
        assertFalse(solved.needsReview, "a fully reconciled slip needs no review")
        assertEquals(1.0f, solved.fieldConfidence["adjPayAndAllce"])
        assertEquals(
            FieldSource.GEOMETRY,
            solved.fieldSource["adjPayAndAllce"],
            "all structured fields must be tagged GEOMETRY",
        )

        val parsed =
            assembleParsedPayslip(
                filename = "test_ltc",
                year = 2026,
                monthNum = 1,
                monthName = "January",
                dateStr = "01/2026",
                officer = com.payslipmax.pdfparser.domain.Officer("Officer", "16/000/000000X", "AR0000000G"),
                earningsMap = solved.earningsMap,
                deductionsMap = solved.deductionsMap,
                reconciled = solved.reconciled,
                taxAndSavings = null,
                rawEarnings = solved.rawEarnings,
                rawDeductions = solved.rawDeductions,
            )
        assertEquals(78473.0, parsed.earnings.adjPayAndAllce)
        assertEquals(0.0, parsed.earnings.miscEarnings)
    }

    @Test
    fun tokenClassifierEndToEndMapsArrLveltcToAdjPayAndAllce() {
        val tokens =
            listOf(
                tok("BPAY", 44f, 261f),
                tok("153500", 143f, 261f),
                tok("DSOP", 186f, 261f),
                tok("30000", 275f, 261f),
                tok("ARR-LVELTC", 44f, 281f),
                tok("78473", 143f, 281f),
                tok("AGIF", 186f, 281f),
                tok("12500", 275f, 281f),
            )

        val table = TokenTableClassifier.classify(tokens)
        val credits = table.standardizedCredits()
        val debits = table.standardizedDebits()

        assertEquals(153500.0, credits["basicPay"])
        assertEquals(78473.0, credits["adjPayAndAllce"])
        assertEquals(30000.0, debits["dsopSubscription"])
        assertEquals(12500.0, debits["agif"])
        assertTrue(table.rawCredits().isEmpty(), "ARR-LVELTC should be recognized as standardized credit, not raw")
    }
}
