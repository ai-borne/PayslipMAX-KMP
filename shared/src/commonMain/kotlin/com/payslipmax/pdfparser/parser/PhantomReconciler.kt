package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.ConfidenceThresholds
import com.payslipmax.pdfparser.parser.debug.FieldClassificationDump
import com.payslipmax.pdfparser.parser.debug.FieldStatus
import com.payslipmax.pdfparser.parser.debug.ParserDebugCollector
import kotlin.math.abs

/**
 * Phantom-numbers sprint, Phase 3 (#1) — the general, provable-removal guarantee. [VerticalBandFilter]
 * (Phase 1, geometry) and [RawLabelNoiseFilter] (Phase 2, label-shape) catch phantoms distinguishable by
 * *shape*; this catches the residual case where a phantom's label looks like real (if terse) content and
 * only the payslip's own arithmetic proves it doesn't belong — e.g. `01_jan_18`'s `R/oEtkt=1915` +
 * `TA Debit=22314`, whose sum exactly equals that fixture's deduction-side overshoot vs the printed
 * Total Deductions.
 *
 * Governing principle (asymmetric cost, same as [VerticalBandFilter]): only [ClassifiedTable]'s untrusted
 * raw/unmatched channel is ever a removal candidate — a clean, keyword-matched structured field is never
 * touched, however large the overshoot. When no subset of the raw channel explains the overshoot within
 * tolerance, nothing is removed; the residual is left for [SchemaValidator]/[ReconciliationSolver] to
 * flag via `needsReview` instead of guessing.
 */
internal object PhantomReconciler {
    /** Beyond this many raw items, subset-sum enumeration (worst case 2^n) is skipped — keep-and-flag instead. */
    private const val MAX_RAW_ITEMS_FOR_SEARCH = 16

    /**
     * Removes the smallest [rawEarnings] subset that exactly explains a credit-side overshoot vs
     * [trueGrossPay]. Callers must pass the *trusted* total — [ReconciliationEngine.reconcileTotals]'s
     * ledger-adjusted, self-corrected `realGross` (minus ledger carry), not the raw parsed field, which
     * can itself be a misparse (e.g. `totalDeductions` mis-OCR'd as a duplicate of `grossPay`) that
     * `reconcileTotals` only detects and fixes using the structured sums.
     */
    fun reconcileCredits(
        earningsMap: Map<String, Double>,
        rawEarnings: MutableMap<String, Double>,
        trueGrossPay: Double,
        debugCollector: ParserDebugCollector? = null,
    ) = removePhantomSubset(earningsMap.values.sum(), rawEarnings, trueGrossPay, TableSide.CREDIT, debugCollector)

    /** Debit-side counterpart of [reconcileCredits] — see its doc for why [trueTotalDeductions] must be the trusted total. */
    fun reconcileDebits(
        deductionsMap: Map<String, Double>,
        rawDeductions: MutableMap<String, Double>,
        trueTotalDeductions: Double,
        debugCollector: ParserDebugCollector? = null,
    ) = removePhantomSubset(deductionsMap.values.sum(), rawDeductions, trueTotalDeductions, TableSide.DEBIT, debugCollector)

    private fun removePhantomSubset(
        structuredSum: Double,
        raw: MutableMap<String, Double>,
        printedTotal: Double,
        side: TableSide,
        debugCollector: ParserDebugCollector?,
    ) {
        if (raw.isEmpty() || raw.size > MAX_RAW_ITEMS_FOR_SEARCH || printedTotal <= 0.0) return
        val overshoot = structuredSum + raw.values.sum() - printedTotal
        if (overshoot <= ConfidenceThresholds.ITEM_SUM_TOLERANCE) return

        val phantomKeys = smallestSubsetSumming(raw, overshoot) ?: return
        for (key in phantomKeys) {
            val amount = raw.remove(key) ?: continue
            debugCollector?.recordStage4Field(
                FieldClassificationDump(
                    fieldKeyOrLabel = key,
                    amount = amount,
                    status = FieldStatus.REJECTED,
                    reason =
                        "Totals-driven phantom removal: raw subset sums to the $side overshoot " +
                            "vs printed total ($printedTotal)",
                    side = side,
                    sourceTokens = key,
                ),
            )
        }
    }

    /** The fewest-item subset of [raw] whose amounts sum within tolerance of [target], or null if none exists. */
    private fun smallestSubsetSumming(
        raw: Map<String, Double>,
        target: Double,
    ): List<String>? {
        val entries = raw.entries.toList()
        for (size in 1..entries.size) {
            val match =
                combinationsOfIndices(entries.indices.toList(), size)
                    .firstOrNull { indices -> abs(indices.sumOf { entries[it].value } - target) <= ConfidenceThresholds.ITEM_SUM_TOLERANCE }
            if (match != null) return match.map { entries[it].key }
        }
        return null
    }

    private fun combinationsOfIndices(
        items: List<Int>,
        size: Int,
    ): Sequence<List<Int>> =
        sequence {
            if (size == 0) {
                yield(emptyList())
                return@sequence
            }
            for (i in items.indices) {
                val head = items[i]
                val rest = items.subList(i + 1, items.size)
                for (tail in combinationsOfIndices(rest, size - 1)) yield(listOf(head) + tail)
            }
        }
}
