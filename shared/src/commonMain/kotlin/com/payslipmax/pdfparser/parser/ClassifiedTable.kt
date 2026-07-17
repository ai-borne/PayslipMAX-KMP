package com.payslipmax.pdfparser.parser

/** Which side of the PCDA table an entry belongs to. */
enum class TableSide { CREDIT, DEBIT }

/**
 * One classified table line item. [standardKey] is non-null when the on-page [rawLabel] matched a
 * known credit/debit key in [PayslipPatternConfig]; otherwise the entry is an unknown that flows to
 * the same `rawEarnings`/`rawDeductions` channel the legacy pipeline uses.
 */
data class ClassifiedEntry(
    val rawLabel: String,
    val standardKey: String?,
    val amount: Double,
    /** Geometric side the entry physically sits on (the learned credit/debit label column). */
    val side: TableSide,
    val labelCenterX: Float,
    val centerY: Float,
    /**
     * The standardized key its label matched in [PayslipPatternConfig], regardless of which column it
     * physically sits in. Populated even when [standardKey] is null (i.e. a keyword match stranded in
     * the opposite column), so the Phase 4 [ReconciliationSolver] can route cross-column reversals
     * (a credit key in the debit column → recovery; a debit reversal in the credit column → adjustment).
     */
    val matchedKey: String? = null,
    /** Canonical side of [matchedKey] (the side that key normally belongs to), or null if unmatched. */
    val matchedSide: TableSide? = null,
)

/** The fully classified earnings/deductions table produced by the Phase 3 token engine. */
data class ClassifiedTable(
    val entries: List<ClassifiedEntry>,
) {
    val credits: List<ClassifiedEntry> get() = entries.filter { it.side == TableSide.CREDIT }
    val debits: List<ClassifiedEntry> get() = entries.filter { it.side == TableSide.DEBIT }

    /** Summed amounts per standardized key for matched entries on the given side. */
    fun standardizedCredits(): Map<String, Double> = sumByStandardKey(credits)

    fun standardizedDebits(): Map<String, Double> = sumByStandardKey(debits)

    /** Unmatched line items, summed under their raw on-page label (the raw earnings/deductions channel). */
    fun rawCredits(): Map<String, Double> = sumByRawLabel(credits)

    fun rawDeductions(): Map<String, Double> = sumByRawLabel(debits)

    private fun sumByStandardKey(items: List<ClassifiedEntry>): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        for (e in items) if (e.standardKey != null) out[e.standardKey] = (out[e.standardKey] ?: 0.0) + e.amount
        return out
    }

    private fun sumByRawLabel(items: List<ClassifiedEntry>): Map<String, Double> {
        val out = mutableMapOf<String, Double>()
        for (e in items) if (e.standardKey == null) out[e.rawLabel] = (out[e.rawLabel] ?: 0.0) + e.amount
        return out
    }
}
