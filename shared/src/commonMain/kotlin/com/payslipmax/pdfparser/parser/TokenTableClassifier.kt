package com.payslipmax.pdfparser.parser

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
    private val normalizedBlocklistPrefixes: Set<String> = PayslipPatternConfig.blocklistPrefixes.map { normalize(it) }.toSet()

    /** Full pipeline entry point: raw table-page tokens → classified credit/debit line items. */
    fun classify(
        tableTokens: List<PositionedToken>,
        debugCollector: com.payslipmax.pdfparser.parser.debug.ParserDebugCollector? = null,
    ): ClassifiedTable {
        val pairs = RowPairing.pair(GridReconstructor.reconstruct(tableTokens, debugCollector))
        return classifyPairs(pairs, debugCollector)
    }

    internal fun classifyPairs(
        pairs: List<LabelAmount>,
        debugCollector: com.payslipmax.pdfparser.parser.debug.ParserDebugCollector? = null,
    ): ClassifiedTable {
        val candidates = pairs.mapNotNull { toCandidate(it) }

        val creditBand = bandFrom(candidates, TableSide.CREDIT)
        val debitBand = bandFrom(candidates, TableSide.DEBIT)
        // Half the gap: generous for per-document jitter, exclusive enough to drop the details column.
        val acceptRadius = if (creditBand != null && debitBand != null) abs(debitBand - creditBand) * 0.5f else DEFAULT_ACCEPT_RADIUS
        // Single-column layout (bandSep < threshold): geometry cannot split sides; trust label keys.
        val bandSeparation = if (creditBand != null && debitBand != null) abs(debitBand - creditBand) else Float.MAX_VALUE
        val isSingleColumnLayout = bandSeparation < SINGLE_COLUMN_BAND_THRESHOLD

        val assignedDumps = mutableListOf<com.payslipmax.pdfparser.parser.debug.EntryAssignmentDump>()
        val droppedDumps = mutableListOf<com.payslipmax.pdfparser.parser.debug.EntryAssignmentDump>()

        val entries =
            candidates.mapNotNull { c ->
                val mappedBand = if (c.side == TableSide.CREDIT) creditBand else debitBand
                val isTrustedByLabel = isSingleColumnLayout && c.cleanMatch && c.side != null
                if (c.cleanMatch && c.side != null && (withinBand(c.labelCenterX, mappedBand, acceptRadius) || isTrustedByLabel)) {
                    val classificationReason =
                        if (isTrustedByLabel && !withinBand(c.labelCenterX, mappedBand, acceptRadius)) {
                            "Single-column layout (bandSep=${bandSeparation}pt): trusted ${c.side} label (x=${c.labelCenterX})"
                        } else {
                            "Clean match in ${c.side} column band (x=${c.labelCenterX})"
                        }
                    debugCollector?.recordStage4Field(
                        com.payslipmax.pdfparser.parser.debug.FieldClassificationDump(
                            fieldKeyOrLabel = c.standardKey ?: c.label,
                            amount = c.amount,
                            status = com.payslipmax.pdfparser.parser.debug.FieldStatus.RECOGNIZED,
                            reason = classificationReason,
                            side = c.side,
                            sourceTokens = c.label,
                        ),
                    )
                    assignedDumps.add(
                        com.payslipmax.pdfparser.parser.debug.EntryAssignmentDump(
                            rawLabel = c.label,
                            amount = c.amount,
                            side = c.side,
                            labelCenterX = c.labelCenterX,
                            centerY = c.centerY,
                            isCleanMatch = true,
                            matchedKey = c.standardKey,
                        ),
                    )
                    ClassifiedEntry(c.label, c.standardKey, c.amount, c.side, c.labelCenterX, c.centerY, c.standardKey, c.side)
                } else {
                    // Everything else — unknown labels, and keyword matches stranded in the opposite column.
                    val side = assignSide(c.labelCenterX, creditBand, debitBand, acceptRadius)
                    if (side != null) {
                        val reasonStr = if (c.standardKey != null) "Stranded key ${c.standardKey} (routed to raw)" else "Unrecognized raw label"
                        debugCollector?.recordStage4Field(
                            com.payslipmax.pdfparser.parser.debug.FieldClassificationDump(
                                fieldKeyOrLabel = c.standardKey ?: c.label,
                                amount = c.amount,
                                status = com.payslipmax.pdfparser.parser.debug.FieldStatus.REJECTED,
                                reason = reasonStr,
                                side = side,
                                sourceTokens = c.label,
                            ),
                        )
                        assignedDumps.add(
                            com.payslipmax.pdfparser.parser.debug.EntryAssignmentDump(
                                rawLabel = c.label,
                                amount = c.amount,
                                side = side,
                                labelCenterX = c.labelCenterX,
                                centerY = c.centerY,
                                isCleanMatch = false,
                                matchedKey = c.standardKey,
                            ),
                        )
                        ClassifiedEntry(c.label, standardKey = null, c.amount, side, c.labelCenterX, c.centerY, c.standardKey, c.side)
                    } else {
                        debugCollector?.recordStage4Field(
                            com.payslipmax.pdfparser.parser.debug.FieldClassificationDump(
                                fieldKeyOrLabel = c.standardKey ?: c.label,
                                amount = c.amount,
                                status = com.payslipmax.pdfparser.parser.debug.FieldStatus.REJECTED,
                                reason = "Outside acceptance radius ($acceptRadius) from both column bands",
                                side = null,
                                sourceTokens = c.label,
                            ),
                        )
                        if (c.side != null) {
                            droppedDumps.add(
                                com.payslipmax.pdfparser.parser.debug.EntryAssignmentDump(
                                    rawLabel = c.label,
                                    amount = c.amount,
                                    side = c.side,
                                    labelCenterX = c.labelCenterX,
                                    centerY = c.centerY,
                                    isCleanMatch = false,
                                    matchedKey = c.standardKey,
                                ),
                            )
                        }
                        null
                    }
                }
            }

        debugCollector?.stage3 =
            com.payslipmax.pdfparser.parser.debug.Stage3ColumnDump(
                creditBand = creditBand,
                debitBand = debitBand,
                acceptRadius = acceptRadius,
                assignedEntries = assignedDumps,
                droppedEntries = droppedDumps,
            )

        return ClassifiedTable(entries)
    }

    private const val DEFAULT_ACCEPT_RADIUS = 30f
    private const val SINGLE_COLUMN_BAND_THRESHOLD = 20f // two-column PCDA layouts have ≥140pt separation

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
        // Drop pseudo-labels that contain only digits/punctuation after Hindi words are removed —
        // this happens when the "Total" row's credit amount merges with the adjacent debit label
        // tokens in GridReconstructor (e.g. "271739 kuula kTaOtI" → "271739"). Valid payslip
        // codes always contain at least one letter.
        if (normalized.none { it.isLetter() }) return null
        if (normalized in normalizedBlocklist || normalizedBlocklistPrefixes.any { normalized.startsWith(it) }) return null
        val match = combinedKeys.firstOrNull { (key, _, _) -> boundaryStartsWith(normalized, key) }
        if (match == null && RawLabelNoiseFilter.isProseNoise(normalized)) return null
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
