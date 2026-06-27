package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Phase 4 — pure-common unit tests for [ReconciliationSolver]. They drive the solver from hand-built
 * [ClassifiedEntry]s (no grid, no tokens, no device) to verify the three things the plan asks of it:
 * balanced reconciliation → high confidence, cross-column ambiguity routing (recoveries & reversals),
 * ledger carry-over handling, and — replacing the old hard-fail — a `needsReview` flag on a residual.
 */
class ReconciliationSolverTest {
    private fun credit(
        key: String,
        amount: Double,
    ) = ClassifiedEntry("L", key, amount, TableSide.CREDIT, 44f, 0f, key, TableSide.CREDIT)

    private fun debit(
        key: String,
        amount: Double,
    ) = ClassifiedEntry("L", key, amount, TableSide.DEBIT, 186f, 0f, key, TableSide.DEBIT)

    private fun solve(
        entries: List<ClassifiedEntry>,
        gross: Double,
        deductions: Double,
        net: Double,
    ) = ReconciliationSolver.solve(ClassifiedTable(entries), gross, deductions, net, fullText = "", filename = "test")

    @Test
    fun balancedTableReconcilesWithHighConfidence() {
        val solved =
            solve(
                listOf(credit("basicPay", 140500.0), credit("dearnessAllowance", 71760.0), credit("militaryServicePay", 15500.0), debit("dsopSubscription", 40000.0), debit("incomeTax", 40521.0)),
                gross = 227760.0,
                deductions = 80521.0,
                net = 147239.0,
            )

        assertEquals(140500.0, solved.earningsMap["basicPay"])
        assertEquals(0.0, solved.reconciled.miscEarnings, "balanced table leaves no residual")
        assertEquals(0.0, solved.reconciled.miscDeductions)
        assertFalse(solved.needsReview, "a fully reconciled slip needs no review")
        assertEquals(1.0f, solved.fieldConfidence["basicPay"])
    }

    @Test
    fun creditKeyInDebitColumnRoutesToRecovery() {
        // A field-allowance label physically in the debit column is a recovery of that allowance.
        val recovery = ClassifiedEntry("FD", standardKey = null, amount = 500.0, side = TableSide.DEBIT, labelCenterX = 186f, centerY = 0f, matchedKey = "fieldAllowance", matchedSide = TableSide.CREDIT)
        val solved = solve(listOf(credit("basicPay", 100000.0), debit("dsopSubscription", 9500.0), recovery), gross = 100000.0, deductions = 10000.0, net = 90000.0)

        assertEquals(500.0, solved.deductionsMap["recFieldAllowance"], "credit key in debit column → recovery")
        assertEquals(null, solved.deductionsMap["fieldAllowance"])
    }

    @Test
    fun debitReversalInCreditColumnBecomesAdjustment() {
        // A licence-fee label physically in the credit column is a refund → pay adjustment.
        val reversal = ClassifiedEntry("L Fee", standardKey = null, amount = 748.0, side = TableSide.CREDIT, labelCenterX = 44f, centerY = 0f, matchedKey = "licenseFee", matchedSide = TableSide.DEBIT)
        val solved = solve(listOf(credit("basicPay", 100000.0), reversal, debit("dsopSubscription", 10000.0)), gross = 100748.0, deductions = 10000.0, net = 90748.0)

        assertEquals(748.0, solved.earningsMap["adjPayAndAllce"], "debit reversal in credit column → adjPayAndAllce")
    }

    @Test
    fun openingCreditBalanceIsPulledToLedgerNotEarnings() {
        val solved =
            solve(
                listOf(credit("basicPay", 100000.0), credit("openingCreditBalance", 5000.0), debit("dsopSubscription", 20000.0)),
                gross = 105000.0,
                deductions = 20000.0,
                net = 85000.0,
            )

        assertEquals(5000.0, solved.reconciled.ledger.openingCredit)
        assertEquals(null, solved.earningsMap["openingCreditBalance"], "ledger entry must not pollute earnings")
        assertEquals(0.0, solved.reconciled.miscEarnings, "trueGross (gross − openingCr) equals the real earnings")
        assertFalse(solved.needsReview)
    }

    @Test
    fun unattributedResidualLowersConfidenceAndIsBookedToMisc() {
        // Parsed credits fall ₹2000 short of the printed gross — the residual the legacy fudge hid.
        val solved = solve(listOf(credit("basicPay", 98000.0), debit("dsopSubscription", 20000.0)), gross = 100000.0, deductions = 20000.0, net = 80000.0)

        assertEquals(2000.0, solved.reconciled.miscEarnings)
        assertTrue((solved.fieldConfidence["basicPay"] ?: 1f) < 1.0f, "an unbalanced side lowers its fields' confidence")
    }

    @Test
    fun netResidualFlagsReviewInsteadOfFailing() {
        // gross − deductions = 80000 but the printed net is 70000: a ₹10000 net residual.
        val solved = solve(listOf(credit("basicPay", 100000.0), debit("dsopSubscription", 20000.0)), gross = 100000.0, deductions = 20000.0, net = 70000.0)

        assertTrue(solved.reconciled.netResidual >= 2.0)
        assertTrue(solved.needsReview, "a net mismatch surfaces as needsReview, not a thrown failure")
    }
}
