package com.payslipmax.pdfparser.parser

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

    /**
     * Regression for the Feb-2025 cell-merge bug: when the credit-total amount token sits close
     * enough to the adjacent debit-label Hindi tokens that GridReconstructor groups them into one
     * cell, RowPairing creates a phantom pair whose label is "271739 kuula kTaOtI" (or similar).
     * After Hindi negation the label becomes "271739" — pure digits, no letters — and must be
     * dropped by toCandidate rather than booked as a raw deduction.
     */
    @Test
    fun hindiTotalsMergedWithAmountDropped() {
        // Mimics the Feb-2025 token layout at y=385: credit total (233016) sits 4 px to the left
        // of the debit-label "kuula kTaOtI", close enough to be merged into the same cell.
        val creditTotalRight = 145f + 33f // ≈ 178  (x of "233016" + its width)
        val debitLabelX = creditTotalRight + 4f // 4 px gap < cellGap → merges!
        val tokens =
            sampleTable() +
                listOf(
                    // Debit totals row: "kuula kTaOtI" label merged into the "233016" amount cell
                    tok("kuula", debitLabelX, 385f, w = 13f),
                    tok("kTaOtI", debitLabelX + 15f, 385f, w = 21f),
                    tok("109310", debitLabelX + 90f, 385f, w = 33f),
                )
        val table = TokenTableClassifier.classify(tokens)
        // The merged label "233016 kuula kTaOtI" → after Hindi removal = "233016" (no letters)
        // must be dropped; its amount 109310 must NOT appear as any line item.
        assertTrue(
            table.debits.none { it.amount == 109310.0 },
            "debit total row must not appear as a phantom deduction",
        )
        assertTrue(
            table.credits.none { it.amount == 109310.0 },
            "debit total must not bleed into credit channel either",
        )
    }

    /**
     * Regression for RC1 — single-column layout (iOS PDFKit coordinate collapse or legacy flat format).
     *
     * When PDFKit extracts all label tokens at the same x-position, [TokenTableClassifier] learns
     * creditBand ≈ debitBand (separation < 20 pt). The resulting acceptRadius ≈ 1 pt causes most
     * clean matches to miss [withinBand], and [assignSide] resolves every tie as CREDIT — so DSOP,
     * AGIF, and ITAX are either misclassified as raw deductions or dropped entirely.
     *
     * Fix: when bandSeparation < SINGLE_COLUMN_BAND_THRESHOLD, trust the label key's own canonical
     * side for all clean matches, bypassing the geometric check.
     */
    @Test
    fun singleColumnLayoutTrustsLabelSideForDebitKeys() {
        // All credit and debit labels at x = 11; widths differ by text length → label centers vary
        // by ≤ 5 pt, producing a bandSeparation << 20 pt that triggers the single-column path.
        val tokens =
            listOf(
                tok("BPAY", 11f, 50f), tok("136400", 115f, 50f),
                tok("DA", 11f, 65f), tok("45849", 115f, 65f),
                tok("MSP", 11f, 80f), tok("15500", 115f, 80f),
                tok("DSOP", 11f, 95f), tok("40000", 115f, 95f),
                tok("AGIF", 11f, 110f), tok("5000", 115f, 110f),
                tok("ITAX", 11f, 125f), tok("22000", 115f, 125f),
            )
        val table = TokenTableClassifier.classify(tokens)
        val credits = table.standardizedCredits()
        val debits = table.standardizedDebits()

        // Credit keys must survive even though their center-x overlaps the debit band.
        assertEquals(136400.0, credits["basicPay"], "BPAY must be CREDIT in single-column layout")
        assertEquals(45849.0, credits["dearnessAllowance"], "DA must be CREDIT in single-column layout")
        assertEquals(15500.0, credits["militaryServicePay"], "MSP must be CREDIT in single-column layout")

        // Debit keys must not be misclassified as CREDIT or dropped.
        assertEquals(40000.0, debits["dsopSubscription"], "DSOP must be DEBIT in single-column layout")
        assertEquals(5000.0, debits["agif"], "AGIF must be DEBIT in single-column layout")
        assertEquals(22000.0, debits["incomeTax"], "ITAX must be DEBIT in single-column layout")
    }

    @Test
    fun gridCellFontPropertiesCalculatedCorrectly() {
        val boldToken = PositionedToken("HEADER", 10f, 20f, 40f, 10f, fontSize = 14f, isBold = true)
        val normalToken = PositionedToken("SUB", 55f, 20f, 30f, 10f, fontSize = 10f, isBold = false)
        val cell = GridCell(listOf(boldToken, normalToken))

        assertTrue(cell.isBold, "Cell containing bold token must report isBold true")
        assertEquals(14f, cell.maxFontSize, "Cell maxFontSize must reflect maximum token font size")
    }

    /**
     * Regression for the real Mar-2025 payslip bug: a footer disclaimer sentence
     * ("This payslip is computer generated. Hence no signature is required. Page") gets paired by
     * RowPairing with a stray nearby page number, landing in the credit column band. Unlike a real
     * (if unrecognized) line item, this text is far longer than any legitimate PCDA label — it must
     * be dropped as noise, not booked to rawEarnings where it can eclipse a correctly itemized parse
     * downstream in the UI.
     */
    @Test
    fun proseFooterLabelMisPairedWithStrayNumberIsDropped() {
        val pairs =
            RowPairing.pair(GridReconstructor.reconstruct(sampleTable())) +
                LabelAmount(
                    label = "This payslip is computer generated. Hence no signature is required. Page",
                    amount = 1.0,
                    labelCenterX = 44f,
                    amountCenterX = 44f,
                    centerY = 420f,
                )
        val table = TokenTableClassifier.classifyPairs(pairs)

        assertTrue(table.credits.none { it.amount == 1.0 }, "footer noise must not appear as a credit line item")
        assertTrue(
            table.rawCredits().keys.none { it.startsWith("This payslip") },
            "footer noise must not land in rawEarnings",
        )
        assertEquals(999.0, table.rawCredits()["XYZALW"], "genuine short unmatched labels must still route to rawEarnings")
    }

    /**
     * Regression for a real on-device finding (Aug-2014 PCDA_TRANSITIONAL_7TH_CPC payslip, smoke-tested
     * during the Tier 6 diagnostic-mode rollout): unlike the footer-disclaimer case above, these bank-detail
     * and statement-header labels are *short* (2-5 words) — under RawLabelNoiseFilter's length guard — so
     * they were never rejected as prose noise. The real on-page labels also carry a trailing colon
     * ("BANK CODE :", "BANK A/C NO :", as printed on the statement), which the exact-match blocklist
     * would silently miss without stripping it in [normalize] first. Uncaught, a bank account number
     * gets booked as a deduction and inflates `SchemaValidator`'s debitsSum by orders of magnitude.
     */
    @Test
    fun bankDetailAndStatementHeaderLabelsDroppedFromDebits() {
        val pairs =
            RowPairing.pair(GridReconstructor.reconstruct(sampleTable())) +
                listOf(
                    LabelAmount("BANK CODE :", 6900047.0, 186f, 275f, 440f),
                    LabelAmount("BANK A/C NO :", 62400106755.0, 186f, 275f, 460f),
                    LabelAmount("STATEMENT OF ACCOUNT FOR AUG", 12345.0, 186f, 275f, 480f),
                )
        val table = TokenTableClassifier.classifyPairs(pairs)

        assertTrue(table.debits.none { it.amount == 6900047.0 }, "bank code must not appear as a debit line item")
        assertTrue(table.debits.none { it.amount == 62400106755.0 }, "bank account number must not appear as a debit line item")
        assertTrue(table.debits.none { it.amount == 12345.0 }, "statement-header noise must not appear as a debit line item")
        assertEquals(40000.0, table.standardizedDebits()["dsopSubscription"], "genuine debits on the same page must be unaffected")
    }

    /**
     * Regression for a real on-device finding: a January payslip's festive "PROSPEROUS NEW YEAR"
     * greeting text gets row-paired with the adjacent printed year (e.g. "2022"), which reads as a
     * clean amount and lands as a bogus credit line item — inflating `SchemaValidator`'s creditsSum by
     * exactly the calendar year.
     */
    @Test
    fun newYearGreetingDroppedFromCredits() {
        val pairs =
            RowPairing.pair(GridReconstructor.reconstruct(sampleTable())) +
                listOf(LabelAmount("PROSPEROUS NEW YEAR", 2022.0, 44f, 145f, 440f))
        val table = TokenTableClassifier.classifyPairs(pairs)

        assertTrue(table.credits.none { it.amount == 2022.0 }, "New Year greeting must not appear as a credit line item")
        assertEquals(140500.0, table.standardizedCredits()["basicPay"], "genuine credits on the same page must be unaffected")
    }
}
