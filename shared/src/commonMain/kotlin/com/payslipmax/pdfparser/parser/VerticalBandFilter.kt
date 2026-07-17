package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.ConfidenceThresholds
import com.payslipmax.pdfparser.parser.debug.FieldClassificationDump
import com.payslipmax.pdfparser.parser.debug.FieldStatus
import com.payslipmax.pdfparser.parser.debug.ParserDebugCollector
import kotlin.math.abs

/**
 * Phase 1 of the phantom-numbers sprint (see `docs/AI_INSIGHTS_PIPELINE.md`) — learns the earnings/
 * deductions table's real vertical body from this document's own geometry (never hardcoded) and lets
 * [apply] drop anything outside it. Nothing real ever prints above the first mandatory matched key
 * (BPAY) or below the printed Gross Pay / Total Deductions row, so this closes off the statement-title
 * year / address pin code (header) and the Next Increment Date / disclaimer (footer) in one geometric
 * rule, independent of the x-column classification [TokenTableClassifier] already does.
 *
 * Governing principle: fail toward keep-and-flag. When a bound cannot be learned from this document
 * (e.g. no cleanly matched key at all), that side of [Bounds] is `null` and [apply] filters nothing on
 * it, rather than guessing a value that could drop a real line item.
 */
object VerticalBandFilter {
    /** Learned top/bottom Y bounds of the real table body; either side may be unset (see class doc). */
    data class Bounds(val top: Float?, val bottom: Float?)

    private const val TOP_MARGIN = 1f
    private const val DEFAULT_ROW_HEIGHT = 20f

    /**
     * Learns [Bounds] for one table page.
     *
     * @param rows every row-local (label, amount) pair on the page, unfiltered — used only to locate the
     *   printed totals row by value and to estimate a fallback row height; never assumed to be real line
     *   items themselves.
     * @param cleanMatchCenterYs the Y-center of every candidate that cleanly matched a known
     *   [PayslipPatternConfig] key. The topmost of these is the hard floor: PCDA payslips always print
     *   BPAY (a strictly mandatory key) as the first real row.
     * @param grossPay the printed Gross Pay total (0.0 when unparsed — never matches).
     * @param totalDeductions the printed Total Deductions total (0.0 when unparsed — never matches).
     */
    fun computeBounds(
        rows: List<LabelAmount>,
        cleanMatchCenterYs: List<Float>,
        grossPay: Double,
        totalDeductions: Double,
    ): Bounds {
        val top = cleanMatchCenterYs.minOrNull()?.let { it - TOP_MARGIN }
        val bottom = totalsRowY(rows, grossPay, totalDeductions) ?: fallbackBottom(rows, cleanMatchCenterYs)
        return Bounds(top, bottom)
    }

    /** Keeps only items whose [centerY] falls within [bounds]; each drop is reported to [onDrop] for explainability. */
    fun <T> apply(
        items: List<T>,
        centerY: (T) -> Float,
        bounds: Bounds,
        onDrop: (T) -> Unit = {},
    ): List<T> =
        items.filter { item ->
            val y = centerY(item)
            val keep = (bounds.top == null || y >= bounds.top) && (bounds.bottom == null || y <= bounds.bottom)
            if (!keep) onDrop(item)
            keep
        }

    /** [apply] plus Stage-4 debug-dump recording for every drop, so callers stay a one-liner. */
    fun <T> applyWithDebug(
        items: List<T>,
        label: (T) -> String,
        amount: (T) -> Double,
        side: (T) -> TableSide?,
        centerY: (T) -> Float,
        bounds: Bounds,
        debugCollector: ParserDebugCollector?,
    ): List<T> =
        apply(items, centerY, bounds) { dropped ->
            debugCollector?.recordStage4Field(
                FieldClassificationDump(
                    fieldKeyOrLabel = label(dropped),
                    amount = amount(dropped),
                    status = FieldStatus.REJECTED,
                    reason = "Outside table-body vertical bounds (top=${bounds.top}, bottom=${bounds.bottom}, y=${centerY(dropped)})",
                    side = side(dropped),
                    sourceTokens = label(dropped),
                ),
            )
        }

    /** The bottommost row whose amount matches either printed total — the totals row is expected last. */
    private fun totalsRowY(
        rows: List<LabelAmount>,
        grossPay: Double,
        totalDeductions: Double,
    ): Float? =
        rows
            .filter { amountMatches(it.amount, grossPay) || amountMatches(it.amount, totalDeductions) }
            .maxOfOrNull { it.centerY }

    private fun amountMatches(
        amount: Double,
        target: Double,
    ): Boolean = target > 0.0 && abs(amount - target) <= ConfidenceThresholds.ITEM_SUM_TOLERANCE

    /** No printed total matched on-page (e.g. footer parse failed): bound by the last real row instead. */
    private fun fallbackBottom(
        rows: List<LabelAmount>,
        cleanMatchCenterYs: List<Float>,
    ): Float? {
        val maxClean = cleanMatchCenterYs.maxOrNull() ?: return null
        return maxClean + medianRowHeight(rows)
    }

    private fun medianRowHeight(rows: List<LabelAmount>): Float {
        val ys = rows.map { it.centerY }.distinct().sorted()
        if (ys.size < 2) return DEFAULT_ROW_HEIGHT
        val gaps = ys.zipWithNext { a, b -> b - a }.sorted()
        return gaps[gaps.size / 2]
    }
}
