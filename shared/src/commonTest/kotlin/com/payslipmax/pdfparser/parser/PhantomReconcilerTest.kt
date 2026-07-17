package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phantom-numbers sprint, Phase 3 (#1) — [PhantomReconciler] wired into [ReconciliationSolver.solve].
 * Drives the solver end-to-end (same harness as [ReconciliationSolverTest]) because the guarantee only
 * matters in terms of its effect on the solved table and the Tier 7 gate that consumes it: a raw entry
 * that exactly explains an overshoot vs the printed total must disappear from [SolvedTable.rawEarnings]/
 * [SolvedTable.rawDeductions], which in turn is what lets [SchemaValidator] (fed `solved.earningsMap.sum()
 * + solved.rawEarnings.sum()`, exactly mirroring [com.payslipmax.pdfparser.parser.pipeline.SharedParsingPipeline])
 * validate clean instead of flagging `needsReview` at Tier 7.
 */
class PhantomReconcilerTest {
    private fun credit(
        key: String,
        amount: Double,
    ) = ClassifiedEntry("L", key, amount, TableSide.CREDIT, 44f, 0f, key, TableSide.CREDIT)

    private fun debit(
        key: String,
        amount: Double,
    ) = ClassifiedEntry("L", key, amount, TableSide.DEBIT, 186f, 0f, key, TableSide.DEBIT)

    private fun rawCredit(
        label: String,
        amount: Double,
    ) = ClassifiedEntry(label, null, amount, TableSide.CREDIT, 44f, 0f)

    private fun rawDebit(
        label: String,
        amount: Double,
    ) = ClassifiedEntry(label, null, amount, TableSide.DEBIT, 186f, 0f)

    private fun solve(
        entries: List<ClassifiedEntry>,
        gross: Double,
        deductions: Double,
        net: Double,
    ) = ReconciliationSolver.solve(ClassifiedTable(entries), gross, deductions, net, fullText = "", filename = "test")

    /** Mirrors exactly what [com.payslipmax.pdfparser.parser.pipeline.SharedParsingPipeline] feeds Tier 7. */
    private fun schemaValidation(
        solved: SolvedTable,
        gross: Double,
        deductions: Double,
        net: Double,
    ) = SchemaValidator.validate(
        grossPay = gross,
        totalDeductions = deductions,
        netRemittance = net,
        creditsSum = solved.earningsMap.values.sum() + solved.rawEarnings.values.sum(),
        debitsSum = solved.deductionsMap.values.sum() + solved.rawDeductions.values.sum(),
    )

    @Test
    fun exactSingleRawPhantomOvershootIsRemovedAndTier7Validates() {
        // Structured side already equals the printed total; one raw line overshoots it by its own exact value.
        val entries =
            listOf(
                credit("basicPay", 70000.0),
                credit("dearnessAllowance", 15000.0),
                credit("militaryServicePay", 15000.0),
                debit("dsopSubscription", 15000.0),
                debit("agif", 5000.0),
                rawDebit("Stray Note", 1915.0),
            )
        val solved = solve(entries, gross = 100000.0, deductions = 20000.0, net = 80000.0)

        assertEquals(null, solved.rawDeductions["Stray Note"], "raw subset that exactly explains the overshoot is removed")
        assertTrue(
            schemaValidation(solved, gross = 100000.0, deductions = 20000.0, net = 80000.0).isValid,
            "with the phantom gone the printed totals reconcile — Tier 7 must not flag needsReview",
        )
    }

    @Test
    fun multiRawSubsetSummingExactlyToTheOvershootIsRemoved() {
        // 01_jan_18 shape: two raw deductions whose sum, not either alone, equals the overshoot.
        val entries =
            listOf(
                credit("basicPay", 70000.0),
                credit("dearnessAllowance", 15000.0),
                credit("militaryServicePay", 15000.0),
                debit("dsopSubscription", 15000.0),
                debit("agif", 5000.0),
                rawDebit("R/oEtkt", 1915.0),
                rawDebit("TA Debit", 22314.0),
            )
        val solved = solve(entries, gross = 100000.0, deductions = 20000.0, net = 80000.0)

        assertEquals(null, solved.rawDeductions["R/oEtkt"], "neither raw item alone matches the overshoot — only their sum does")
        assertEquals(null, solved.rawDeductions["TA Debit"])
    }

    @Test
    fun cleanStructuredFieldsAreNeverTouchedByPhantomRemoval() {
        val entries =
            listOf(
                credit("basicPay", 70000.0),
                credit("dearnessAllowance", 15000.0),
                credit("militaryServicePay", 15000.0),
                debit("dsopSubscription", 15000.0),
                debit("agif", 5000.0),
                rawDebit("Stray Note", 1915.0),
            )
        val solved = solve(entries, gross = 100000.0, deductions = 20000.0, net = 80000.0)

        assertEquals(70000.0, solved.earningsMap["basicPay"], "structured fields are never candidates for phantom removal")
        assertEquals(15000.0, solved.deductionsMap["dsopSubscription"])
        assertEquals(5000.0, solved.deductionsMap["agif"])
    }

    @Test
    fun unexplainedOvershootRemovesNothingAndTier7StaysFlagged() {
        // The lone raw credit (500) is nowhere near the 2000 overshoot — no subset can explain it.
        val entries =
            listOf(
                credit("basicPay", 97500.0),
                credit("dearnessAllowance", 1000.0),
                credit("militaryServicePay", 1000.0),
                rawCredit("Unrelated Note", 500.0),
                debit("dsopSubscription", 15000.0),
                debit("agif", 5000.0),
            )
        val solved = solve(entries, gross = 98000.0, deductions = 20000.0, net = 78000.0)

        assertEquals(500.0, solved.rawEarnings["Unrelated Note"], "an overshoot no raw subset explains must not remove anything")
        assertTrue(
            !schemaValidation(solved, gross = 98000.0, deductions = 20000.0, net = 78000.0).isValid,
            "the un-explained overshoot must stay visible to Tier 7 — never silently dropped (D4)",
        )
    }
}
