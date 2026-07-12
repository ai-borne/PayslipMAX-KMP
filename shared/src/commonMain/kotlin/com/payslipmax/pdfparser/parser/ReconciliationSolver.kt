package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.FieldSource

/**
 * The fully solved table: standardized earnings/deductions, the raw (unmatched) channel, the
 * reconciled totals, and the per-field confidence sidecar consumed by the Phase 5 correction UI.
 */
internal data class SolvedTable(
    val earningsMap: Map<String, Double>,
    val deductionsMap: Map<String, Double>,
    val rawEarnings: Map<String, Double>,
    val rawDeductions: Map<String, Double>,
    val reconciled: ReconciledTotals,
    val fieldConfidence: Map<String, Float>,
    val fieldSource: Map<String, FieldSource> = emptyMap(),
    val needsReview: Boolean,
    /** Human-readable reasons [needsReview] is true, for the Phase 5 correction UI (SSOT, never recomputed downstream). */
    val reviewReasons: List<String> = emptyList(),
)

/**
 * Phase 4 — turns the geometry-classified [ClassifiedTable] into the final earnings/deductions maps
 * and a confidence judgement, using the payslip's own arithmetic invariants
 * (`Gross = Σcredits`, `Total Deductions = Σdebits`, `Net = Gross − Deductions ± ledger`) as a
 * *solver and confidence signal* rather than a hard failure.
 *
 * It supersedes the legacy string pipeline's two brittle mechanisms:
 *  - historical per-month fudge factors — gone; any residual
 *    that can't be attributed to a line item is booked to `miscEarnings`/`miscDeductions` and lowers
 *    confidence, instead of being sprinkled onto Basic Pay/DA/MSP to force a balance, and
 *  - the hard-fail reconciliation that discarded the whole parse — replaced by [SolvedTable.needsReview].
 *
 * Cross-column routing reuses the SSOT key-sets in [PayslipPatternConfig] so recoveries (a credit key
 * sitting in the debit column) and reversals (a debit key in the credit column) are booked exactly as
 * the string path books them.
 */
internal object ReconciliationSolver {
    /** A field below this confidence is surfaced for user review in Phase 5 (SSOT in [ConfidenceThresholds]). */
    const val REVIEW_THRESHOLD = com.payslipmax.pdfparser.domain.ConfidenceThresholds.REVIEW_THRESHOLD

    /** Raw/ambiguous line items are inherently less certain than a clean keyword match in its column. */
    private const val RAW_PENALTY = 0.8f

    /** Net residual (rupees) at or above which the whole slip is flagged for review. Matches legacy. */
    private const val NET_TOLERANCE = 2.0

    fun solve(
        table: ClassifiedTable,
        grossPay: Double,
        totalDeductions: Double,
        netRemittance: Double,
        fullText: String,
        filename: String,
        debugCollector: com.payslipmax.pdfparser.parser.debug.ParserDebugCollector? = null,
    ): SolvedTable {
        val earningsMap = mutableMapOf<String, Double>()
        val deductionsMap = mutableMapOf<String, Double>()
        val rawEarnings = mutableMapOf<String, Double>()
        val rawDeductions = mutableMapOf<String, Double>()

        for (entry in table.entries) {
            route(entry, earningsMap, deductionsMap, rawEarnings, rawDeductions)
        }

        // reconcileTotals removes ledger entries from the maps, self-corrects a misparsed printed total
        // (e.g. totalDeductions mis-OCR'd as a duplicate of grossPay) and computes the misc residuals —
        // all from the structured maps only, so it's safe to run before Phase 3 touches the raw channel.
        val reconciled =
            reconcileTotals(
                earningsMap = earningsMap,
                deductionsMap = deductionsMap,
                fullText = fullText,
                grossPay = grossPay,
                totalDeductions = totalDeductions,
                netRemittance = netRemittance,
                filename = filename,
            )

        // Phase 3 (#1): strip any raw subset that exactly explains an overshoot vs the trusted,
        // ledger-adjusted totals reconcileTotals just computed — provable phantom removal.
        PhantomReconciler.reconcileCredits(earningsMap, rawEarnings, reconciled.trueGross, debugCollector)
        PhantomReconciler.reconcileDebits(deductionsMap, rawDeductions, reconciled.trueDeductions, debugCollector)

        val confidence = scoreConfidence(earningsMap, deductionsMap, rawEarnings, rawDeductions, reconciled).toMutableMap()
        val missingCredits = PayslipPatternConfig.strictlyMandatoryCredits.filter { (earningsMap[it] ?: 0.0) <= 0.0 }
        val missingDebits = PayslipPatternConfig.strictlyMandatoryDebits.filter { (deductionsMap[it] ?: 0.0) <= 0.0 }
        val hasMissingMandatory = missingCredits.isNotEmpty() || missingDebits.isNotEmpty()
        if (hasMissingMandatory) {
            for (key in confidence.keys) {
                confidence[key] = (confidence[key] ?: 1.0f) * 0.5f
            }
        }

        val reviewReasons = mutableListOf<String>()
        if (reconciled.netResidual >= NET_TOLERANCE) {
            reviewReasons.add("Net residual ${reconciled.netResidual} >= NET_TOLERANCE ($NET_TOLERANCE)")
        }
        val lowConfFields = confidence.filter { it.value < REVIEW_THRESHOLD }
        if (lowConfFields.isNotEmpty()) {
            reviewReasons.add("Low confidence fields (< $REVIEW_THRESHOLD): ${lowConfFields.keys.joinToString()}")
        }
        if (missingCredits.isNotEmpty()) {
            reviewReasons.add("Missing mandatory credits: ${missingCredits.joinToString()}")
        }
        if (missingDebits.isNotEmpty()) {
            reviewReasons.add("Missing mandatory debits: ${missingDebits.joinToString()}")
        }

        val needsReview =
            reconciled.netResidual >= NET_TOLERANCE || confidence.values.any { it < REVIEW_THRESHOLD } || hasMissingMandatory

        // Every field reaching this point came from the geometry solver — Tier 6 Gemma fallback (if any)
        // runs as a later pipeline step and overwrites the relevant keys to GEMMA_FALLBACK there.
        val fieldSource: Map<String, FieldSource> = confidence.keys.associateWith { FieldSource.GEOMETRY }

        debugCollector?.stage5 =
            com.payslipmax.pdfparser.parser.debug.Stage5ReconciliationDump(
                grossPay = grossPay,
                totalDeductions = totalDeductions,
                netRemittance = netRemittance,
                earningsSum = earningsMap.values.sum() + rawEarnings.values.sum(),
                deductionsSum = deductionsMap.values.sum() + rawDeductions.values.sum(),
                netResidual = reconciled.netResidual,
                needsReview = needsReview,
                reviewReasons = reviewReasons,
                fieldConfidence = confidence,
            )

        return SolvedTable(
            earningsMap = earningsMap,
            deductionsMap = deductionsMap,
            rawEarnings = rawEarnings,
            rawDeductions = rawDeductions,
            reconciled = reconciled,
            fieldConfidence = confidence,
            fieldSource = fieldSource,
            needsReview = needsReview,
            reviewReasons = reviewReasons,
        )
    }

    /** Books one classified entry into the right map, honoring cross-column recoveries and reversals. */
    private fun route(
        entry: ClassifiedEntry,
        earningsMap: MutableMap<String, Double>,
        deductionsMap: MutableMap<String, Double>,
        rawEarnings: MutableMap<String, Double>,
        rawDeductions: MutableMap<String, Double>,
    ) {
        val amount = entry.amount
        when (entry.side) {
            TableSide.CREDIT ->
                when {
                    // Trusted clean credit in its own column.
                    entry.standardKey != null && entry.matchedSide == TableSide.CREDIT ->
                        earningsMap.add(entry.standardKey, amount)
                    // A debit key stranded in the credit column → ledger entry or credit reversal.
                    entry.matchedSide == TableSide.DEBIT && entry.matchedKey != null ->
                        when (entry.matchedKey) {
                            in PayslipPatternConfig.ledgerDebitKeys -> deductionsMap.add(entry.matchedKey, amount)
                            in PayslipPatternConfig.creditReversalDebitKeys -> earningsMap.add("adjPayAndAllce", amount)
                            else -> Unit // a real deduction wrongly in the credit column — ignore, don't double-book
                        }
                    else -> rawEarnings.add(entry.rawLabel, amount)
                }
            TableSide.DEBIT ->
                when {
                    // Trusted clean debit in its own column.
                    entry.standardKey != null && entry.matchedSide == TableSide.DEBIT ->
                        deductionsMap.add(entry.standardKey, amount)
                    // A credit key stranded in the debit column → recovery of a previously paid allowance.
                    entry.matchedSide == TableSide.CREDIT && entry.matchedKey != null ->
                        deductionsMap.add(PayslipPatternConfig.recoveryTargetFor(entry.matchedKey), amount)
                    else -> rawDeductions.add(entry.rawLabel, amount)
                }
        }
    }

    /**
     * Assigns a 0..1 confidence to every populated field. A side that reconciles cleanly (small misc
     * residual relative to its trusted total) lends high confidence to its line items; a large residual
     * — the case the legacy fudge used to hide — lowers it. Raw/ambiguous items carry an extra penalty.
     */
    private fun scoreConfidence(
        earningsMap: Map<String, Double>,
        deductionsMap: Map<String, Double>,
        rawEarnings: Map<String, Double>,
        rawDeductions: Map<String, Double>,
        reconciled: ReconciledTotals,
    ): Map<String, Float> {
        val out = mutableMapOf<String, Float>()
        val creditBase = sideConfidence(reconciled.realGross, reconciled.miscEarnings, reconciled.ledger.openingCredit + reconciled.ledger.closingDebit)
        val debitBase = sideConfidence(reconciled.realDeductions, reconciled.miscDeductions, reconciled.ledger.openingDebit + reconciled.ledger.closingCredit)

        for (key in earningsMap.keys) out[key] = creditBase
        for (key in deductionsMap.keys) out[key] = debitBase
        for (key in rawEarnings.keys) out[key] = creditBase * RAW_PENALTY
        for (key in rawDeductions.keys) out[key] = debitBase * RAW_PENALTY
        if (reconciled.miscEarnings > 0.0) out["miscEarnings"] = creditBase * RAW_PENALTY
        if (reconciled.miscDeductions > 0.0) out["miscDeductions"] = debitBase * RAW_PENALTY
        return out
    }

    /** Confidence for one side = how small its unattributed residual is, relative to the trusted total. */
    private fun sideConfidence(
        printedTotal: Double,
        misc: Double,
        ledgerCarry: Double,
    ): Float {
        val trueTotal = (printedTotal - ledgerCarry).coerceAtLeast(0.0)
        if (trueTotal <= 0.0) return 1.0f
        return (1.0 - misc / trueTotal).coerceIn(0.0, 1.0).toFloat()
    }

    private fun MutableMap<String, Double>.add(
        key: String,
        amount: Double,
    ) {
        this[key] = (this[key] ?: 0.0) + amount
    }
}
