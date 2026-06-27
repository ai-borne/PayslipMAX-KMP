package com.ssbmax.pdfparser.parser

import kotlin.math.abs

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

/**
 * Phase 3 — the pure-common credit/debit classifier. It is the third stage of
 * `tokens → 2D grid → (label, amount) rows → credit/debit classification`.
 *
 * Classification is **content-driven and geometry-learning**, never geometry-hardcoded:
 *  1. Match each label against [PayslipPatternConfig] credit/debit mappings (DRY — reuse, don't copy).
 *  2. From the *cleanly* matched labels, learn this document's credit-label and debit-label x-bands.
 *  3. Assign every remaining pair (ambiguous matches in the noisy "details" column, and genuinely
 *     unknown labels) to a side by proximity to those learned bands; anything far from both bands —
 *     transaction-detail text, page footers — is dropped rather than mis-booked.
 *
 * This permanently removes the brittle `xSplit` crop and the per-month geometry patches.
 */
object TokenTableClassifier {
    private val combinedKeys: List<Triple<String, TableSide, String>> =
        buildList {
            for ((key, std) in PayslipPatternConfig.creditKeysMapping) add(Triple(normalize(key), TableSide.CREDIT, std))
            for ((key, std) in PayslipPatternConfig.debitKeysMapping) add(Triple(normalize(key), TableSide.DEBIT, std))
        }.sortedByDescending { it.first.length }

    private val normalizedBlocklist: Set<String> = PayslipPatternConfig.blocklist.map { normalize(it) }.toSet()

    /** Full pipeline entry point: raw table-page tokens → classified credit/debit line items. */
    fun classify(tableTokens: List<PositionedToken>): ClassifiedTable {
        val pairs = RowPairing.pair(GridReconstructor.reconstruct(tableTokens))
        return classifyPairs(pairs)
    }

    internal fun classifyPairs(pairs: List<LabelAmount>): ClassifiedTable {
        val candidates = pairs.mapNotNull { toCandidate(it) }

        val creditBand = bandFrom(candidates, TableSide.CREDIT)
        val debitBand = bandFrom(candidates, TableSide.DEBIT)
        // Half the gap between the two label columns is a generous yet exclusive acceptance radius;
        // it keeps the right-hand "details" column out while tolerating per-document jitter.
        val acceptRadius =
            if (creditBand != null && debitBand != null) {
                abs(debitBand - creditBand) * 0.5f
            } else {
                DEFAULT_ACCEPT_RADIUS
            }

        val entries =
            candidates.mapNotNull { c ->
                val mappedBand = if (c.side == TableSide.CREDIT) creditBand else debitBand
                if (c.cleanMatch && c.side != null && withinBand(c.labelCenterX, mappedBand, acceptRadius)) {
                    // Trusted: a recognized key sitting in its own column. Keep its standardized key.
                    // Here geometric side == canonical side (the band check guaranteed it).
                    ClassifiedEntry(c.label, c.standardKey, c.amount, c.side, c.labelCenterX, c.centerY, c.standardKey, c.side)
                } else {
                    // Everything else — unknown labels, and keyword matches stranded in the opposite
                    // column — is placed by geometry alone and treated as a raw (unmatched) line item,
                    // but we retain any keyword match so the solver can route cross-column reversals.
                    // Anything too far from both label bands (detail text, footers) is dropped.
                    val side = assignSide(c.labelCenterX, creditBand, debitBand, acceptRadius) ?: return@mapNotNull null
                    ClassifiedEntry(c.label, standardKey = null, c.amount, side, c.labelCenterX, c.centerY, c.standardKey, c.side)
                }
            }
        return ClassifiedTable(entries)
    }

    private const val DEFAULT_ACCEPT_RADIUS = 30f

    private data class Candidate(
        val label: String,
        val amount: Double,
        val labelCenterX: Float,
        val centerY: Float,
        val side: TableSide?,
        val standardKey: String?,
        val cleanMatch: Boolean,
    )

    private fun toCandidate(pair: LabelAmount): Candidate? {
        val normalized = normalize(negateHindiTransliterations(pair.label))
        if (normalized.isBlank()) return null
        if (normalized in normalizedBlocklist) return null

        val match = combinedKeys.firstOrNull { (key, _, _) -> boundaryStartsWith(normalized, key) }
        val clean = match != null && isCleanMatch(normalized, match.first)
        return Candidate(
            label = pair.label.trim(),
            amount = pair.amount,
            labelCenterX = pair.labelCenterX,
            centerY = pair.centerY,
            side = match?.second,
            standardKey = match?.third,
            cleanMatch = clean,
        )
    }

    /** Learns a side's label column as the median x of its cleanly matched entries (details excluded). */
    private fun bandFrom(
        candidates: List<Candidate>,
        side: TableSide,
    ): Float? {
        val xs = candidates.filter { it.cleanMatch && it.side == side }.map { it.labelCenterX }.sorted()
        if (xs.isEmpty()) return null
        return xs[xs.size / 2]
    }

    private fun withinBand(
        x: Float,
        band: Float?,
        radius: Float,
    ): Boolean = band != null && abs(x - band) <= radius

    private fun assignSide(
        x: Float,
        creditBand: Float?,
        debitBand: Float?,
        radius: Float,
    ): TableSide? {
        val dCredit = creditBand?.let { abs(x - it) } ?: Float.MAX_VALUE
        val dDebit = debitBand?.let { abs(x - it) } ?: Float.MAX_VALUE
        val nearest = minOf(dCredit, dDebit)
        if (nearest > radius) return null
        return if (dCredit <= dDebit) TableSide.CREDIT else TableSide.DEBIT
    }

    /** True when [text] begins with [key] and the next character is a word boundary (not alphanumeric). */
    private fun boundaryStartsWith(
        text: String,
        key: String,
    ): Boolean {
        if (!text.startsWith(key)) return false
        val next = text.getOrNull(key.length) ?: return true
        return !next.isLetterOrDigit()
    }

    /**
     * A clean match has nothing meaningful after the key — only punctuation, whitespace, or a single
     * parenthetical qualifier such as the pay-band tag in "BPAY (12A)". This mirrors the legacy
     * `extractFromColumn` regex, which also skips an optional `(...)` group between key and amount.
     */
    private fun isCleanMatch(
        text: String,
        key: String,
    ): Boolean {
        val rest = text.drop(key.length).replace(Regex("\\([^)]*\\)"), " ")
        return rest.none { it.isLetterOrDigit() }
    }

    private fun normalize(text: String): String = text.lowercase().replace(Regex("\\s+"), " ").trim()
}
