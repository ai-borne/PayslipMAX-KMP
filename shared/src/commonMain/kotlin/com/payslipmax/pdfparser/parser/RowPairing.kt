package com.payslipmax.pdfparser.parser

/**
 * A single (label → amount) pair recovered from one table row, retaining the geometry needed by
 * [TokenTableClassifier] to learn the credit/debit column bands per-document.
 */
data class LabelAmount(
    val label: String,
    val amount: Double,
    val labelCenterX: Float,
    val amountCenterX: Float,
    val centerY: Float,
)

/**
 * Phase 3 — turns a reconstructed [Grid] into row-local (label, amount) pairs. Pairing is done within
 * a single row so it is robust to layout shifts: a PCDA table row commonly carries both a credit pair
 * (left) and a debit pair (middle) plus a noisy "details" column, all on the same baseline. We pair
 * each numeric cell with the nearest preceding text cell on its own row; numeric cells with no
 * preceding label (e.g. stray page numbers) are ignored here, and the details column is filtered later
 * by [TokenTableClassifier] using learned column geometry.
 */
object RowPairing {
    fun pair(grid: Grid): List<LabelAmount> {
        val pairs = mutableListOf<LabelAmount>()
        for (row in grid.rows) {
            val pendingLabels = mutableListOf<GridCell>()
            for (cell in row.cells) {
                val amount = parseAmount(cell.text)
                if (amount == null) {
                    // Text cell — becomes candidate label for next amount on this row.
                    pendingLabels.add(cell)
                } else {
                    if (pendingLabels.isNotEmpty()) {
                        val combinedText = pendingLabels.joinToString(" ") { it.text }
                        val firstLabel = pendingLabels.first()
                        pairs +=
                            LabelAmount(
                                label = combinedText,
                                amount = amount,
                                labelCenterX = firstLabel.centerX,
                                amountCenterX = cell.centerX,
                                centerY = row.centerY,
                            )
                        // An amount consumes its labels so the next number needs a fresh set.
                        pendingLabels.clear()
                    }
                }
            }
        }
        return pairs
    }

    /**
     * Parses a cell as a rupee amount, or null if it is not a clean number. Strips Indian-style
     * grouping commas and accepts an optional leading minus (credit reversals / adjustments). Rejects
     * dates ("01/2024"), list markers ("1."), and any token carrying other characters, so transaction
     * descriptions never masquerade as amounts.
     */
    internal fun parseAmount(text: String): Double? {
        val cleaned = text.replace(",", "").trim()
        if (!Regex("^-?\\d+$").matches(cleaned)) return null
        return cleaned.toDoubleOrNull()
    }
}
