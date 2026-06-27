package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Phase 3 pure-common unit tests for the token table engine
 * (`GridReconstructor → RowPairing → TokenTableClassifier`). These drive the engine entirely from
 * hand-built [PositionedToken]s — no device, no PDF, no fixtures — so the grid/pairing/classification
 * logic is verified in isolation, and the "no hardcoded xSplit" guarantee is asserted directly by
 * shifting the whole layout and confirming identical classification.
 */
class TokenTableEngineTest {
    /** One table token with a fixed line height; width is approximated from text length. */
    private fun tok(
        text: String,
        x: Float,
        y: Float,
        w: Float = text.length * 5f,
        h: Float = 6f,
    ) = PositionedToken(text = text, x = x, y = y, width = w, height = h)

    /**
     * A miniature but faithful PCDA layout: credit labels at x≈44, credit amounts ≈145, debit labels
     * ≈186, debit amounts ≈280, plus a noisy right-hand "details" column whose stray number must be
     * ignored. `dx` shifts the entire table horizontally to prove geometry is learned, not hardcoded.
     */
    private fun sampleTable(dx: Float = 0f): List<PositionedToken> =
        listOf(
            // Row 1: BPAY 140500 | FUR 392 | 1. Recovery of LF ... to 878  (details number must drop)
            tok("BPAY", 44f + dx, 261f), tok("140500", 143f + dx, 261f),
            tok("FUR", 186f + dx, 261f), tok("392", 287f + dx, 261f),
            tok("Recovery", 339f + dx, 261f), tok("LF", 395f + dx, 261f),
            tok("to", 488f + dx, 261f), tok("878", 534f + dx, 261f),
            // Row 2: DA 71760 | LF 878
            tok("DA", 44f + dx, 281f), tok("71760", 149f + dx, 281f),
            tok("LF", 186f + dx, 281f), tok("878", 287f + dx, 281f),
            // Row 3: MSP 15500 | DSOP 40000
            tok("MSP", 44f + dx, 301f), tok("15500", 149f + dx, 301f),
            tok("DSOP", 186f + dx, 301f), tok("40000", 275f + dx, 301f),
            // Row 4: an unknown credit allowance | ITAX 40521
            tok("XYZALW", 44f + dx, 321f), tok("999", 150f + dx, 321f),
            tok("ITAX", 186f + dx, 321f), tok("40521", 275f + dx, 321f),
            // Totals row (hindi labels) must not be parsed as line items
            tok("kuula", 42f + dx, 385f), tok("Aaya", 58f + dx, 385f), tok("233016", 145f + dx, 385f),
        )

    @Test
    fun gridClustersRowsAndCells() {
        val grid = GridReconstructor.reconstruct(sampleTable())
        // 5 visual rows (3 line items + unknown row + totals row).
        assertEquals(5, grid.rows.size, "rows: ${grid.rows.map { r -> r.cells.map { it.text } }}")
        val firstRow = grid.rows.first()
        assertEquals("BPAY", firstRow.cells.first().text)
        assertTrue(firstRow.cells.any { it.text == "140500" })
    }

    @Test
    fun parseAmountRejectsNonAmounts() {
        assertEquals(140500.0, RowPairing.parseAmount("140500"))
        assertEquals(139604.0, RowPairing.parseAmount("1,39,604"))
        assertEquals(-392.0, RowPairing.parseAmount("-392"))
        assertNull(RowPairing.parseAmount("01/2024"))
        assertNull(RowPairing.parseAmount("1."))
        assertNull(RowPairing.parseAmount("Rs.1,39,604"))
    }

    @Test
    fun classifiesCreditsAndDebitsAndDropsDetailsColumn() {
        val table = TokenTableClassifier.classify(sampleTable())
        val credits = table.standardizedCredits()
        val debits = table.standardizedDebits()

        assertEquals(140500.0, credits["basicPay"])
        assertEquals(71760.0, credits["dearnessAllowance"])
        assertEquals(15500.0, credits["militaryServicePay"])

        assertEquals(392.0, debits["furnitureRent"])
        assertEquals(878.0, debits["licenseFee"], "details-column LF 878 must not double-count")
        assertEquals(40000.0, debits["dsopSubscription"])
        assertEquals(40521.0, debits["incomeTax"])

        // The hindi totals row contributes nothing.
        assertNull(credits["miscEarnings"])
        assertTrue(table.credits.none { it.amount == 233016.0 }, "gross total must not be a line item")
    }

    @Test
    fun unknownLabelRoutesToRawChannelBySide() {
        val table = TokenTableClassifier.classify(sampleTable())
        // XYZALW is unrecognized but sits in the credit column → raw earnings, not a standardized key.
        assertEquals(999.0, table.rawCredits()["XYZALW"])
        assertNull(table.standardizedCredits()["XYZALW"])
    }

    @Test
    fun classificationIsTranslationInvariant() {
        // Same logical table, shifted right by 120px: learned bands must yield identical results,
        // proving there is no hardcoded xSplit.
        val base = TokenTableClassifier.classify(sampleTable(dx = 0f))
        val shifted = TokenTableClassifier.classify(sampleTable(dx = 120f))
        assertEquals(base.standardizedCredits(), shifted.standardizedCredits())
        assertEquals(base.standardizedDebits(), shifted.standardizedDebits())
        assertEquals(base.rawCredits(), shifted.rawCredits())
    }
}
